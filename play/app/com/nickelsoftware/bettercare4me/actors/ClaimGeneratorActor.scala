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
   * @param status: 0: OK, -1: Error (?)
   */
  case class GenerateClaimsCompleted(status: Int)

  /**
   * Process all claims, patient and provider files
   *
   * @param configText the YAML configuration for setting up the HEDIS rules
   */
  case class ProcessGenereatedFiles(configTxt: String)

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

    // Generate local file for simulation
    // ------------------------------------
    case GenerateClaimsRequest(configTxt) =>

      //      log.info(s"ClaimGeneratorActor: Received GenerateClaimsRequest, configuration is:\n $configTxt")
      try {
        val config = ClaimGeneratorConfig.loadConfig(configTxt)

        // save configuration to file in claim simulation directory
        val path = new File(config.basePath)
        path.mkdir()
        val f = new File(path, "claimSimulatorConfig.yaml")
        val fout = new BufferedWriter(new FileWriter(f))
        fout.write(configTxt)
        fout.close

        //TODO Use a pool of actors to generate the simulation files
        for (igen <- 1 to config.nbrGen) ClaimFileGeneratorHelper.generateClaims(igen, config)

        sender ! GenerateClaimsCompleted(0)
        
      } catch {
        case ex: FileNotFoundException => {
          log.error("FileNotFoundException, cannot save claim generator config "+ex.getMessage())
          sender ! GenerateClaimsCompleted(1)
        }
        case ex: IOException => {
          log.error("IOException, "+ex.getMessage())
          sender ! GenerateClaimsCompleted(1)
        }
        case ex: NickelException => {
          log.error("NickelException, "+ex.message)
          sender ! GenerateClaimsCompleted(1)
        }
      }

    // Process local file generated from simulation
    // ------------------------------------
    case ProcessGenereatedFiles(configTxt) =>

      val config = ClaimGeneratorConfig.loadConfig(configTxt)

      def loop(scoreSummary: HEDISScoreSummary, igen: Int): HEDISScoreSummary = {
        if (igen == 0) scoreSummary
        else {
          // Must combine with scoreSummary
          scoreSummary.addHEDISScoreSummary(loop(ClaimFileGeneratorHelper.processGeneratedFiles(igen, config), igen - 1))
        }
      }

      if (config.nbrGen > 0) {

        val ss = loop(ClaimFileGeneratorHelper.processGeneratedFiles(config.nbrGen, config), config.nbrGen - 1)
        sender ! ProcessGenereatedFilesCompleted(ss)

      } else {

        val msg = "ClaimGeneratorActor: Received ProcessGenereatedFiles with invalid configuration (nbrGen must be > 0) is: " + config.nbrGen
        log.error(msg)
        sender ! akka.actor.Status.Failure(NickelException(msg))
      }
  }

}
