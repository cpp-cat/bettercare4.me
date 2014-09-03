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
import models.ClaimGeneratorConfig


/**
 * Class for generating patients, providers, and claims for a given \c igen generation
 *
 * @param config Simulation parameters
 */
class ClaimFileGeneratorHelper(val config: ClaimGeneratorConfig) {

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
    val personGenerator = new PersonGenerator(config.maleNamesFile, config.femaleNamesFile, config.lastNamesFile, config.hedisDate)
    
    // generate the providers
    val providers = for(i <- 1 to config.nbrProviders) yield personGenerator.generateProvider
    
    // write them to file
    providers.foreach {p => providersWriter.writeRow(p.toList)}
    
    // generate the patients
    val patients = for(i <- 1 to config.nbrPatients) yield personGenerator.generatePatient
    
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