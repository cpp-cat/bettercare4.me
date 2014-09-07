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
import models.HEDISRule
import models.ClaimGeneratorConfig
import scala.collection.JavaConversions._
import models.HEDISRules
import models.SimplePersistenceLayer


/**
 * Class for generating patients, providers, and claims for a given \c igen generation
 *
 * @param config Simulation parameters
 */
object ClaimFileGeneratorHelper {

  def getOne[A](items: IndexedSeq[A]): A = items(Random.nextInt(items.size))
  
  /**
   * Generate claims using simulation parameters from \c config
   * 
   * Generate the simulated \c Patients, \c Providers, and \c Claims to CSV files. 
   * This simulator uses \c SimplePersistenceLayer for created the entities \c UUIDs
   *
   * @param igen Generation number
   */
  def generateClaims(igen: Int, config: ClaimGeneratorConfig): Unit = {

    // The persistence layer provides an abstraction level to the UUID generation
    val persistenceLayer = new SimplePersistenceLayer(igen)

    // Make a unique directory name based on today's date and create the directory if does not already exist.
    val pathName = config.basePath + LocalDate.now().toString()
    (new File(pathName)).mkdir()
    
    val fnameBase = pathName + "/" +config.baseFname
    val patientsWriter = CSVWriter.open(new File(fnameBase + "_patients_" + igen.toString + ".csv"))
    val providersWriter = CSVWriter.open(new File(fnameBase + "_providers_" + igen.toString + ".csv"))
    val claimsWriter = CSVWriter.open(new File(fnameBase + "_claims_" + igen.toString + ".csv"))

    // Person generator class
    val personGenerator = new PersonGenerator(config.maleNamesFile, config.femaleNamesFile, config.lastNamesFile, config.hedisDate, persistenceLayer)
    
    // create and configure the rules to use for the simulation
    val hedisDate = config.hedisDate
    val rules: List[HEDISRule] = config.getRulesConfig().map{c => HEDISRules.createRuleByName(c.name)(c, hedisDate)}.toList
    
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
        rule <- rules
        claim <- rule.generateClaims(persistenceLayer, patient, provider)
    } claimsWriter.writeRow(claim.toList)
    
    // that's it, close all files
    patientsWriter.close
    providersWriter.close
    claimsWriter.close
  }
}