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
  def generateClaims(generator: ClaimGeneratorHelper, configTxt: String): ClaimGeneratorCounts = {

    val conf = new SparkConf()
      .setMaster("local[3]")
      .setAppName("Claim Generator Spark Helper")
    val sc = new SparkContext(conf)

    // broadcast the config so it is available to each node of the cluster
    val broadcastConfigTxt = sc.broadcast(configTxt)
    val broadcastGenerator = sc.broadcast(generator)
    
    val config = ClaimGeneratorConfig.loadConfig(configTxt)

    // create the nbrGen jobs to run, ensuring the rdd is sliced with one job per slice, ie nbrGen slices
    val rdd = sc.parallelize(1 to config.nbrGen, config.nbrGen) map { igen => broadcastGenerator.value.generateClaims(igen, broadcastConfigTxt.value) }

    // combine the result of each job to get the total count of patients, providers and claims
    val result = rdd reduce { (a, b) => a + b }
    
    sc.stop
    result
  }

  def processGeneratedClaims(generator: ClaimGeneratorHelper, configTxt: String): HEDISScoreSummary = {

    val conf = new SparkConf()
      .setMaster("local[3]")
      .setAppName("Claim Generator Spark Helper")
    val sc = new SparkContext(conf)

    // broadcast the config so it is available to each node of the cluster
    val broadcastConfigTxt = sc.broadcast(configTxt)
    val broadcastGenerator = sc.broadcast(generator)

    val config = ClaimGeneratorConfig.loadConfig(configTxt)
    
    // create the nbrGen jobs to run, ensuring the rdd is sliced with one job per slice, ie nbrGen slices
    val rdd = sc.parallelize(1 to config.nbrGen, config.nbrGen) map { igen => broadcastGenerator.value.processGeneratedClaims(igen, broadcastConfigTxt.value) }

    // combine the result of each job to get the total count of patients, providers and claims
    val result = rdd reduce { (a, b) => a + b }
    
    // create the paginated list of patients from rule_scorecards table to rule_scorecards_paginated table - Cassandra only
    sc.parallelize(config.rulesConfig map (_.name), config.rulesConfig.size) foreach { ruleName => broadcastGenerator.value.paginateRuleScorecards(ruleName, broadcastConfigTxt.value) }
    
    sc.stop
    result
  }
}