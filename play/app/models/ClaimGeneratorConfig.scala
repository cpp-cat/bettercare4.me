/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import scala.beans.BeanProperty
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
 * @param baseFname Base file name and path for the output files (claims, patients, and providers)
 * @param nGen number of generation to simulate
 * @param nPatients number of patients to simulate per generation
 * @param nProviders number of providers to simulate per generation
 * @param maleNamesFile file name for male given names used for person generation
 * @param femaleNamesFile file name for female given names used for person generation
 * @param lastNamesFile file name for last names used for person generation
 * @param hedisDate date to used for HEDIS report date for the simulation (use Jan 1 of following year, i.e., use 2015/01/01 for HEDIS 2014 reports
 */
class ClaimGeneratorConfig {
    @BeanProperty var baseFname: String = null
    @BeanProperty var nbrGen: Int = 0
    @BeanProperty var nbrPatients: Int = 0
    @BeanProperty var nbrProviders: Int = 0
    @BeanProperty var maleNamesFile: String = null
    @BeanProperty var femaleNamesFile: String = null
    @BeanProperty var lastNamesFile: String = null
    @BeanProperty var hedisDateTxt: String = null
    
    def hedisDate = LocalDate.parse(hedisDateTxt)
  }
