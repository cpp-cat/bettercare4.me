/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors

import java.io.File
import scala.collection.JavaConversions.asScalaBuffer
import scala.util.Random
import org.joda.time.LocalDate
import com.github.tototoshi.csv.CSVWriter
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.models.ClaimParser
import com.nickelsoftware.bettercare4me.models.PersonGenerator
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer
import scala.collection.mutable.HashMap
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import com.github.tototoshi.csv.CSVReader
import com.nickelsoftware.bettercare4me.models.PatientParser
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.hedis.Scorecard

/**
 * Class for generating patients, providers, and claims for a given \c igen generation
 *
 * @param config Simulation parameters
 */
object ClaimFileGeneratorHelper {
  
  case class ClaimGeneratorCounts(nbrPatients: Long, nbrProviders: Long, nbrClaims: Long) {
    
    def +(rhs: ClaimGeneratorCounts) = ClaimGeneratorCounts(nbrPatients+rhs.nbrPatients, nbrProviders+rhs.nbrProviders, nbrClaims+rhs.nbrClaims)
  }

  def getOne[A](items: IndexedSeq[A]): A = items(Random.nextInt(items.size))

  /**
   * Generate claims using simulation parameters from `config
   *
   * Generate the simulated `Patients, `Providers, and `Claims to CSV files.
   * This simulator uses `SimplePersistenceLayer for created the entities `UUIDs
   *
   * @param igen Generation number
   * @param config the generator's configuration parameters
   */
  def generateClaims(igen: Int, config: ClaimGeneratorConfig): ClaimGeneratorCounts = {

    // The persistence layer provides an abstraction level to the UUID generation
    val persistenceLayer = new SimplePersistenceLayer(igen)

    // Make a directory to hold generated files
    val pathName = config.basePath
    (new File(pathName)).mkdir()

    val fnameBase = pathName + "/" + config.baseFname
    val patientsWriter = CSVWriter.open(new File(fnameBase + "_patients_" + igen.toString + ".csv"))
    val providersWriter = CSVWriter.open(new File(fnameBase + "_providers_" + igen.toString + ".csv"))
    val claimsWriter = CSVWriter.open(new File(fnameBase + "_claims_" + igen.toString + ".csv"))

    // Person generator class
    val personGenerator = new PersonGenerator(config.maleNamesFile, config.femaleNamesFile, config.lastNamesFile, config.hedisDate, persistenceLayer)

    // create and configure the rules to use for the simulation
    val hedisDate = config.hedisDate
    val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, hedisDate) }.toList

    // generate the providers
    val providers = for (i <- 1 to config.nbrProviders) yield personGenerator.generateProvider

    // write them to file
    providers.foreach { p => providersWriter.writeRow(p.toList) }

    // generate the patients
    val patients = for (i <- 1 to config.nbrPatients) yield personGenerator.generatePatient

    // write them to file
    patients.foreach { p => patientsWriter.writeRow(p.toList) }

    // generate the claims
    var simScores = HashMap[String, (Int, Int, Int)]()
    var nbrClaims = 0L
    for {
      patient <- patients
      provider = getOne(providers)
    } {
      simScores.clear
      for {
        rule <- rules
        simScoreTpl = simScores.getOrElseUpdate(rule.config.simParityRuleName, (Random.nextInt(100), Random.nextInt(100), Random.nextInt(100)))
        claim <- rule.generateClaims(persistenceLayer, patient, provider, simScoreTpl._1, simScoreTpl._2, simScoreTpl._3)
      } {claimsWriter.writeRow(ClaimParser.toList(claim)); nbrClaims = nbrClaims + 1}
    }

    // that's it, close all files
    patientsWriter.close
    providersWriter.close
    claimsWriter.close
    
    ClaimGeneratorCounts(patients.size.toLong, providers.size.toLong, nbrClaims)
  }
  
  
  def processGeneratedFiles(igen: Int, config: ClaimGeneratorConfig): HEDISScoreSummary = {

    val fnameBase = config.basePath + "/" + config.baseFname
    val allPatients = CSVReader.open(new File(fnameBase + "_patients_" + igen.toString + ".csv")).all() map { PatientParser.fromList(_) }
    
    val allClaims = CSVReader.open(new File(fnameBase + "_claims_" + igen.toString + ".csv")).all() map { ClaimParser.fromList(_) }
    val claimsMap = allClaims groupBy { _.patientID }

    // create and configure the rules to use for the simulation
    val hedisDate = config.hedisDate
    val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, hedisDate) }.toList
    
    // compute the scorecard for each patient
    val patientScorecards = for {
      patient <- allPatients
      claims = claimsMap.getOrElse(patient.patientID, List.empty)
      ph = PatientHistoryFactory.createPatientHistory(patient, claims)
    } yield { rules.foldLeft(Scorecard())({ (scorecard, rule) => rule.scoreRule(scorecard, patient, ph) }) }
    
    //*** Save each patient scorecard
    
    // fold the scorecards into a HEDISScoreSummary and return it
    patientScorecards.foldLeft(HEDISScoreSummary(rules))({ (scoreSummary, scorecard) => scoreSummary.addScoreCard(scorecard) })
  }
}
