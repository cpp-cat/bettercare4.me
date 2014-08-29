/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package actors

import akka.actor.Actor
import akka.actor.ActorLogging

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object ClaimGeneratorActor {

  /**
   * Request to trigger a claim generation job.
   * 
   * Nbr_pt is the number of 1000 patients to include in the generation
   */
  case class GenerateClaimsRequest(nbr_pt: Int)
  
  /**
   * Provide a status on the ongoing claim generation
   * 
   * Nbr_pt is the number of 1000 patients generate thus far.
   */
  case class GenerateClaimsStatus(nbr_pt: Int)
  
  /**
   * Response to indicate the claim generation is completed
   * 
   * status: 0: OK, -1: Error (?)
   */
  case class GenerateClaimsResponse(status: Int)
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
    case GenerateClaimsRequest(nbr_pt) =>
      
      log.info(s"ClaimGeneratorActor: Received GenerateClaimsRequest, with Nbr of 1000 patients of $nbr_pt")
      
      //TODO Generate claims, patients and providers
      
      sender ! GenerateClaimsResponse(0)

  }

}