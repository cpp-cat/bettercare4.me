/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import scala.beans.BeanProperty

import org.joda.time.LocalDate
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

object ClaimGeneratorConfig {

  def loadConfig(text: String): ClaimGeneratorConfig = {
    val yaml = new Yaml(new Constructor(classOf[ClaimGeneratorConfig]))
    yaml.load(text).asInstanceOf[ClaimGeneratorConfig]
  }

}

/**
 * Class holding the parameters of the simulation
 *
 * @param basePath base path for the output file. Simulator may add date to path and create new directory.
 * @param baseFname Base file name for the output files (claims, patients, and providers)
 * @param nGen number of generation to simulate
 * @param nPatients number of patients to simulate per generation
 * @param nProviders number of providers to simulate per generation
 * @param maleNamesFile file name for male given names used for person generation
 * @param femaleNamesFile file name for female given names used for person generation
 * @param lastNamesFile file name for last names used for person generation
 * @param hedisDate date to used for HEDIS report date for the simulation (use Jan 1 of following year, i.e., use 2015/01/01 for HEDIS 2014 reports
 * @param rulesConfig list of configuration parameter for each HEDISRule used in the simulation
 */
class ClaimGeneratorConfig {
  @BeanProperty var basePath: String = null
  @BeanProperty var baseFname: String = null
  @BeanProperty var nbrGen: Int = 0
  @BeanProperty var nbrPatients: Int = 0
  @BeanProperty var nbrProviders: Int = 0
  @BeanProperty var maleNamesFile: String = null
  @BeanProperty var femaleNamesFile: String = null
  @BeanProperty var lastNamesFile: String = null
  @BeanProperty var hedisDateTxt: String = null
  @BeanProperty var rulesConfig: java.util.ArrayList[RuleConfig] = new java.util.ArrayList()

  def hedisDate = LocalDate.parse(hedisDateTxt).toDateTimeAtStartOfDay()
}

/**
 * Class holding configuration parameters for HEDISRule
 *
 * @param name of the rule this configuration applies to
 * @param eligibleRate the rate at which the patients are eligible to the  measure
 * @param meetMeasureRate the rate at which the patients meet the measure, in %
 * @param exclusionRate the rate at which patients are excluded from measure, in %
 */
class RuleConfig {
  @BeanProperty var name: String = null
  @BeanProperty var eligibleRate: Int = 0
  @BeanProperty var meetMeasureRate: Int = 0
  @BeanProperty var exclusionRate: Int = 0
  @BeanProperty var otherParams: java.util.HashMap[String, String] = new java.util.HashMap() 
}
