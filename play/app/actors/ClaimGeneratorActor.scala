/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import models.ClaimGeneratorConfig

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
   * Nbr_pt is the number of 1000 or block of patients generated thus far.
   */
  case class GenerateClaimsStatus(nbr_pt: Int)
  
  /**
   * Response to indicate the claim generation is completed
   * 
   * status: 0: OK, -1: Error (?)
   */
  case class GenerateClaimsCompleted(status: Int)
}

/**
 * Actor to to generate simulated claims, patients and providers
 * 
 * Claims, patients and providers are stored either in CSV files or Cassandra database
 */
class ClaimGeneratorActor() extends Actor with ActorLogging {

  import ClaimGeneratorActor._
  
  def receive = {

    // Real simple request
    case GenerateClaimsRequest(configTxt) =>
      
      log.info(s"ClaimGeneratorActor: Received GenerateClaimsRequest, configuration is:\n $configTxt")
      
      val config = ClaimGeneratorConfig.loadConfig(configTxt)
      
      //TODO Use a pool of actors to generate the simulation files
      for(igen <- 1 to config.nbrGen) ClaimFileGeneratorHelper.generateClaims(igen, config)
      
      sender ! GenerateClaimsCompleted(0)
  }

}