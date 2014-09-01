/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package actors

import java.io.File
import com.github.tototoshi.csv.CSVWriter
import org.joda.time.LocalDate
import models.PersonGenerator
import models.Provider
import scala.util.Random
import models.HEDISRules

/**
 * Class holding the parameters of the simulation
 *
 * @param baseFname Base file name and path for the output files (claims, patients, and providers)
 * @param nGen number of generation to simulate
 * @param nPatients number of patients to simulate per generation
 * @param nProviders number of providers to simulate per generation
 * @param maleNameFile file name for male given names used for person generation
 * @param femaleNameFile file name for female given names used for person generation
 * @param lastNameFile file name for last names used for person generation
 * @param hedisDate date to used for HEDIS report date for the simulation (use Jan 1 of following year, i.e., use 2015/01/01 for HEDIS 2014 reports
 */
case class ClaimFileGeneratorConfig(
  val baseFname: String, val nGen: Int, val nPatients: Int, val nProviders: Int,
  val maleNameFile: String, val femaleNameFile: String, val lastNameFile: String, val hedisDate: LocalDate)

/**
 * Class for generating patients, providers, and claims for a given \c igen generation
 *
 * @param config Simulation parameters
 */
class ClaimFileGeneratorHelper(val config: ClaimFileGeneratorConfig) {

  def getOne[A](items: IndexedSeq[A]): A = items(Random.nextInt(items.size))
  
  /**
   * Generate claims using simulation parameters from \c config
   *
   * @param igen Generation number
   */
  def generateClaims(igen: Int): Unit = {

    val patientsWriter = CSVWriter.open(new File(config.baseFname + "_patients_" + igen.toString + ".csv"))
    val providersWriter = CSVWriter.open(new File(config.baseFname + "_providers_" + igen.toString + ".csv"))
    val claimsWriter = CSVWriter.open(new File(config.baseFname + "_claims_" + igen.toString + ".csv"))

    // Person generator class
    val personGenerator = new PersonGenerator(config.maleNameFile, config.femaleNameFile, config.lastNameFile, config.hedisDate)
    
    // generate the providers
    val providers = for(i <- 1 to config.nProviders) yield personGenerator.generateProvider
    
    // write them to file
    providers.foreach {p => providersWriter.writeRow(p.toList)}
    
    // generate the patients
    val patients = for(i <- 1 to config.nPatients) yield personGenerator.generatePatient
    
    // write them to file
    patients.foreach {p => patientsWriter.writeRow(p.toList)}
    
    // generate the claims
    for {
        patient <- patients
        provider = getOne(providers)
        rule <- HEDISRules.all
        claim <- rule.generateClaims(patient, provider)
    } claimsWriter.writeRow(claim.toList)
    
    // that's it, close all files
    patientsWriter.close
    providersWriter.close
    claimsWriter.close
  }
}