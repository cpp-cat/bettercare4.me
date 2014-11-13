package com.nickelsoftware.bettercare4me.utils

import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import scala.io.Source

object Yaml {

  def test = {

    val configTxt = """
        basePath: ./data/hedis-2014-test
        baseFname: ClaimGenerator
        nbrGen: 1
        nbrPatients: 10
        nbrProviders: 1
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-12-31
        rulesConfig:
          - name: CDC-LDL-C-HEDIS-2014
            eligibleRate: 50
            exclusionRate: 0
            meetMeasureRate: 50
        
          - name: CDC-LDL-C-Value-HEDIS-2014
            eligibleRate: 50
            exclusionRate: 0
            meetMeasureRate: 50
            simulationParity: CDC-LDL-C-HEDIS-2014
        
          - name: CMC-LDL-C-Test-HEDIS-2014
            eligibleRate: 50
            exclusionRate: 0
            meetMeasureRate: 50
            simulationParity: CDC-LDL-C-HEDIS-2014
        
          - name: CMC-LDL-C-Test-Value-HEDIS-2014
            eligibleRate: 50
            exclusionRate: 0
            meetMeasureRate: 50
            simulationParity: CDC-LDL-C-HEDIS-2014 
        """

    //Source.fromFile(fname)
    val config = ClaimGeneratorConfig.loadConfig(configTxt)
    println("Yaml Test, got config object - basePath: "+config.basePath)
  }
}