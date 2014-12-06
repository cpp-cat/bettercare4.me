/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import play.api.Logger
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import scala.concurrent.Await

/**
 * Helper object to distribute the workload of generating the patients, providers and claims
 * over a spark cluster.
 */
object ClaimGeneratorSparkHelper {

  /**
   * Generate all patients, providers, and claims based on config
   * @param config is the claim generator configuration
   * @returns triple with (nbr of patients, nbr providers, nbr claims) generated
   */
  def generateClaims(generator: ClaimGeneratorHelper, config: ClaimGeneratorConfig): ClaimGeneratorCounts = {

    val conf = new SparkConf()
      .setMaster("local[3]")
      .setAppName("Claim Generator Spark Helper")
    val sc = new SparkContext(conf)

    // broadcast the config so it is available to each node of the cluster
    val broadcastConfig = sc.broadcast(config)
    val broadcastGenerator = sc.broadcast(generator)

    // create the nbrGen jobs to run, ensuring the rdd is sliced with one job per slice, ie nbrGen slices
    val rdd = sc.parallelize(1 to config.nbrGen, config.nbrGen) map { igen => broadcastGenerator.value.generateClaims(igen, broadcastConfig.value) }

    // combine the result of each job to get the total count of patients, providers and claims
    rdd reduce { (a, b) => a + b }
  }

  def processGeneratedClaims(generator: ClaimGeneratorHelper, config: ClaimGeneratorConfig): HEDISScoreSummary = {

    val conf = new SparkConf()
      .setMaster("local[3]")
      .setAppName("Claim Generator Spark Helper")
    val sc = new SparkContext(conf)

    // broadcast the config so it is available to each node of the cluster
    val broadcastConfig = sc.broadcast(config)
    val broadcastGenerator = sc.broadcast(generator)

    // create the nbrGen jobs to run, ensuring the rdd is sliced with one job per slice, ie nbrGen slices
    val rdd = sc.parallelize(1 to config.nbrGen, config.nbrGen) map { igen => broadcastGenerator.value.processGeneratedClaims(igen, broadcastConfig.value) }

    // combine the result of each job to get the total count of patients, providers and claims
    rdd reduce { (a, b) => a + b }
  }
}