/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import play.Logger

/**
 * Helper object to distribute the workload of generating the patients, providers and claims
 * over a spark cluster.
 */
object ClaimGeneratorSparkHelper {

  import ClaimFileGeneratorHelper._

  /**
   * Generate all patients, providers, and claims based on config
   * @param config is the clain generator configuration
   * @returns triple with (nbr of patients, nbr providers, nbr claims) generated
   */
  def generateClaims(config: ClaimGeneratorConfig): ClaimGeneratorCounts = {

    val t1 = System.currentTimeMillis()
    
    val conf = new SparkConf()
      .setMaster("local[3]")
      .setAppName("Claim Generator Spark Helper")
    val sc = new SparkContext(conf)
    
    // broadcast the config so it is available to each node of the cluster
    val broadcastConfig = sc.broadcast(config)
    
    // create the nbrGen jobs to run, ensuring the rdd is sliced with one job per slice, ie nbrGen slices
    val rdd = sc.parallelize(1 to config.nbrGen, config.nbrGen) map { igen => ClaimFileGeneratorHelper.generateClaims(igen, broadcastConfig.value)}
    
    // combine the result of each job to get the total count of patients, providers and claims
    val result = rdd reduce { (a, b) => a + b}

    val t2 = System.currentTimeMillis() - t1
    Logger.info(s"ClaimGeneratorSparkHelper.generateClaims $result completed in $t2 msec.")
    
    result
  }
}