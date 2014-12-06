/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors

import scala.util.Random
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig

case class ClaimGeneratorCounts(nbrPatients: Long, nbrProviders: Long, nbrClaims: Long) {

  def +(rhs: ClaimGeneratorCounts) = ClaimGeneratorCounts(nbrPatients + rhs.nbrPatients, nbrProviders + rhs.nbrProviders, nbrClaims + rhs.nbrClaims)
}

/**
 * Base trait for claim generator helper object
 *
 * Implemented by ClaimFileGeneratorHelper and ClaimCassandraGeneratorHelper
 */
trait ClaimGeneratorHelper {

  def getOne[A](items: List[A]): A = items(Random.nextInt(items.size))

  /**
   * Generate claims using simulation parameters from `config
   *
   * @param igen Generation number
   * @param config the generator's configuration parameters
   */
  def generateClaims(igen: Int, config: ClaimGeneratorConfig): ClaimGeneratorCounts

  /**
   * Compute the HEDIS score and patient gaps
   */
  def processGeneratedClaims(igen: Int, config: ClaimGeneratorConfig): HEDISScoreSummary
}
