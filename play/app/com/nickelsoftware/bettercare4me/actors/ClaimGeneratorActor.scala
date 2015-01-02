/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors

import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.utils.NickelException
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.cassandra.Bettercare4me

object ClaimGeneratorActor {

  /**
   * Request to trigger a claim generation job.
   *
   * @param configTxt configuration for claim generation simulator
   */
  case class GenerateClaimsRequest(configTxt: String)

  /**
   * Provide a status on the ongoing claim generation
   *
   * @param nbr_pt is the number of 1000 or block of patients generated thus far.
   */
  case class GenerateClaimsStatus(nbr_pt: Int)

  /**
   * Response to indicate the claim generation is completed
   *
   * @param counts indicates the total number of patients, providers, and claims generated
   * @param status: 0: OK, -1: Error (?)
   */
  case class GenerateClaimsCompleted(counts: ClaimGeneratorCounts, status: Int)

  /**
   * Process all claims, patient and provider files
   *
   * @param configText the YAML configuration for setting up the HEDIS rules
   */
  case class ProcessGenereatedClaims(configTxt: String)

  /**
   * Response to 'ProcessGenereatedFiles with the summary of the metrics
   *
   * @param ss the HEDISScoreSummary instance holding the result of processing all the files.
   */
  case class ProcessGenereatedFilesCompleted(ss: HEDISScoreSummary)
}

/**
 * Actor to to generate simulated claims, patients and providers
 *
 * Claims, patients and providers are stored either in CSV files or Cassandra database
 */
class ClaimGeneratorActor() extends Actor with ActorLogging {

  import ClaimGeneratorActor._

  def receive = {

    // Generate clinical data for simulation
    // ------------------------------------
    case GenerateClaimsRequest(configTxt) =>

      //      log.info(s"ClaimGeneratorActor: Received GenerateClaimsRequest, configuration is:\n $configTxt")
      try {
        val t1 = System.currentTimeMillis()
        val config = ClaimGeneratorConfig.loadConfig(configTxt)

        // save configuration to file in claim simulation directory
        val path = new File(config.basePath)
        path.mkdir()
        val f = new File(path, "claimSimulatorConfig.yaml")
        val fout = new BufferedWriter(new FileWriter(f))
        fout.write(configTxt)
        fout.close

        val counts = config.generator match {
          case "file" =>
            //TODO Use a pool of actors to generate the simulation files
            val c = for (igen <- 1 to config.nbrGen) yield ClaimFileGeneratorHelper.generateClaims(igen, configTxt)
            c.reduce((c1, c2) => c1 + c2)

          case "file-spark" => ClaimGeneratorSparkHelper.generateClaims(ClaimFileGeneratorHelper, configTxt)

          case "cassandra" => ClaimGeneratorSparkHelper.generateClaims(ClaimCassandraGeneratorHelper, configTxt)

          case _ => throw NickelException(s"ClaimGeneratorActor: Message GenerateClaimsRequest, unknown generator in config: ${config.generator}")
        }

        val t2 = System.currentTimeMillis() - t1
        log.info(s"GenerateClaimsRequest $counts completed in $t2 msec using ${config.generator}.")

        sender ! GenerateClaimsCompleted(counts, 0)

      } catch {
        case ex: FileNotFoundException => {
          log.error("FileNotFoundException, cannot save claim generator config " + ex.getMessage())
          sender ! GenerateClaimsCompleted(ClaimGeneratorCounts(0, 0, 0), 1)
        }
        case ex: IOException => {
          log.error("IOException, " + ex.getMessage())
          sender ! GenerateClaimsCompleted(ClaimGeneratorCounts(0, 0, 0), 1)
        }
        case ex: NickelException => {
          log.error("NickelException, " + ex.message)
          sender ! GenerateClaimsCompleted(ClaimGeneratorCounts(0, 0, 0), 1)
        }
      }

    // Process local file generated from simulation
    // ------------------------------------
    case ProcessGenereatedClaims(configTxt) =>

      try {
        val t1 = System.currentTimeMillis()
        val config = ClaimGeneratorConfig.loadConfig(configTxt)

        val hedisScoreSummary = config.generator match {
          case "file" =>
            //TODO Use a pool of actors to generate the simulation files
            val c = for (igen <- 1 to config.nbrGen) yield ClaimFileGeneratorHelper.processGeneratedClaims(igen, configTxt)
            c.reduce((c1, c2) => c1 + c2)

          case "file-spark" => ClaimGeneratorSparkHelper.processGeneratedClaims(ClaimFileGeneratorHelper, configTxt)

          case "cassandra" => ClaimGeneratorSparkHelper.processGeneratedClaims(ClaimCassandraGeneratorHelper, configTxt)

          case _ => throw NickelException(s"ClaimGeneratorActor: Message ProcessGenereatedClaims, unknown generator in config: ${config.generator}")
        }

        val t2 = System.currentTimeMillis() - t1
        log.info(s"ProcessGenereatedClaims completed in $t2 msec using ${config.generator}.")

        sender ! ProcessGenereatedFilesCompleted(hedisScoreSummary)

      } catch {
        case ex: FileNotFoundException => {
          log.error("FileNotFoundException, cannot save claim generator config " + ex.getMessage())
          sender ! akka.actor.Status.Failure(NickelException(ex.getMessage()))
        }
        case ex: IOException => {
          log.error("IOException, " + ex.getMessage())
          sender ! akka.actor.Status.Failure(NickelException(ex.getMessage()))
        }
        case ex: NickelException => {
          log.error("NickelException, " + ex.message)
          sender ! akka.actor.Status.Failure(NickelException(ex.getMessage()))
        }
      }
  }

}
