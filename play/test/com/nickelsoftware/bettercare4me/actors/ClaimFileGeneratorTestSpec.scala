/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors;

import java.io.File

import scala.collection.JavaConversions.asScalaBuffer
import scala.language.postfixOps
import scala.language.reflectiveCalls

import org.scalatest.Suite
import org.scalatest.SuiteMixin
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.github.tototoshi.csv.CSVReader
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.models.ClaimParser
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.models.PatientParser
import com.nickelsoftware.bettercare4me.models.ProviderParser

trait TestFileGenerator extends SuiteMixin { this: Suite =>

  val pathName = "./data/temp-test"

  abstract override def withFixture(test: NoArgTest) = {

    // pre test
    val dir = new File(pathName)
    dir.mkdir()

    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally {

      // post test
      for (f <- dir.listFiles()) f.delete()
      dir.delete()
    }
  }
}

class ClaimFileGeneratorTestSpec extends PlaySpec with OneAppPerSuite with TestFileGenerator {

  "The ClaimFileGeneratorHelper class" must {

    "generate Patients, Providers and Claims based on configuration, single entity" in {

      val config = ClaimGeneratorConfig.loadConfig("""
        basePath: """ + pathName + """
        baseFname: ClaimGenerator
        nbrGen: 1
        nbrPatients: 1
        nbrProviders: 1
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-01-01T00:00:00.00-05:00
        rulesConfig:
          - name: TEST
            eligibleRate: 100
            meetMeasureRate: 100
            exclusionRate: 0
        """)

      ClaimFileGeneratorHelper.generateClaims(0, config)

      // validate the created files
      val fnameBase = pathName + "/" + config.baseFname
      val patients = CSVReader.open(new File(fnameBase + "_patients_0.csv")).all()
      val providers = CSVReader.open(new File(fnameBase + "_providers_0.csv")).all()
      val claims = CSVReader.open(new File(fnameBase + "_claims_0.csv")).all()

      patients.size mustBe 1
      providers.size mustBe 1
      claims.size mustBe 1

      val patient = PatientParser.fromList(patients(0))
      patient.age(config.hedisDate) must be >= 0
      patient.age(config.hedisDate) must be < 100

      val provider = ProviderParser.fromList(providers(0))
      ClaimParser.fromList(claims(0)) match {
        case claim: MedClaim =>
          claim.patientID mustBe patient.patientID
          claim.providerID mustBe provider.providerID
        case _ => fail("Invalid claim class type!")
      }
    }

    "generate Patients, Providers and Claims based on configuration, multiple entities" in {

      val config = ClaimGeneratorConfig.loadConfig("""
        basePath: """ + pathName + """
        baseFname: ClaimGenerator
        nbrGen: 1
        nbrPatients: 5
        nbrProviders: 3
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-01-01T00:00:00.00-05:00
        rulesConfig:
          - name: TEST
            eligibleRate: 100
            meetMeasureRate: 100
            exclusionRate: 0
        """)

      ClaimFileGeneratorHelper.generateClaims(0, config)

      // validate the created files
      val fnameBase = pathName + "/" + config.baseFname
      val patients = CSVReader.open(new File(fnameBase + "_patients_0.csv")).all()
      val providers = CSVReader.open(new File(fnameBase + "_providers_0.csv")).all()
      val claims = CSVReader.open(new File(fnameBase + "_claims_0.csv")).all()

      patients.size mustBe 5
      providers.size mustBe 3
      claims.size mustBe 5
    }

    "generate HEDIS measures simulation data, testing simulationParity in RuleConfig" in {

      val config = ClaimGeneratorConfig.loadConfig("""
        basePath: """ + pathName + """
        baseFname: ClaimGenerator
        nbrGen: 1
        nbrPatients: 1000
        nbrProviders: 1
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-12-31T00:00:00.00-05:00
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
        """)

      // generate the simulation data
      val igen = 0
      ClaimFileGeneratorHelper.generateClaims(igen, config)

      // validate the created files
      // compute the HEDIS scores
      val fnameBase = config.basePath + "/" + config.baseFname
      val allPatients = CSVReader.open(new File(fnameBase + "_patients_" + igen.toString + ".csv")).all() map { PatientParser.fromList(_) }

      val allClaims = CSVReader.open(new File(fnameBase + "_claims_" + igen.toString + ".csv")).all() map { ClaimParser.fromList(_) }
      val claimsMap = allClaims groupBy { _.patientID }

      val nbrPatients = config.nbrPatients
      allPatients.size mustBe nbrPatients

      // create and configure the rules to use for the simulation
      val hedisDate = config.hedisDate
      val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, hedisDate) }.toList

      // compute the scorecard for each patient
      val patientScorecards = for {
        patient <- allPatients
        claims = claimsMap.getOrElse(patient.patientID, List.empty)
        ph = PatientHistoryFactory.createPatientHistory(patient, claims)
      } yield { rules.foldLeft(Scorecard())({ (scorecard, rule) => rule.scoreRule(scorecard, patient, ph) }) }

      // should have a scorecard for each patient
      patientScorecards.size mustBe nbrPatients

      // each patient should have matching score result for all rules
      patientScorecards foreach { scorecard =>
        val baseRule = rules.head
        val rs = scorecard.hedisRuleMap.get(baseRule.name).get
        val testValue = (rs.meetDemographic.isCriteriaMet, rs.eligible.isCriteriaMet, rs.excluded.isCriteriaMet, rs.meetMeasure.isCriteriaMet)
        //        println("---\n" + baseRule.name + ": " + testValue)
        val test = rules.tail forall { r =>
          val rs = scorecard.hedisRuleMap.get(r.name).get
          val tv = (rs.meetDemographic.isCriteriaMet, rs.eligible.isCriteriaMet, rs.excluded.isCriteriaMet, rs.meetMeasure.isCriteriaMet)
          //          println(r.name + ": " + tv)
          val b = testValue == tv
          if (!b) fail("Scorecard from " + r.name + " does not match value of base " + baseRule.name)
          else b
        }
      }

      // compute the overall score summary
      val scoreSummary = patientScorecards.foldLeft(HEDISScoreSummary(rules))({ (scoreSummary, scorecard) => scoreSummary.addScoreCard(scorecard) })

      //      println("\n Overall score summary over " + scoreSummary.patientCount + " patients")
      scoreSummary.ruleScoreSummaries.values foreach { rss =>
        if (rss.eligible2MeetDemographics < 45.0 || rss.eligible2MeetDemographics > 55.0) fail(rss.ruleInfo.name + " ratio of eligible to meet demographics is ("+rss.eligible2MeetDemographics+") not in the 45 - 55 range!")
        if (rss.excluded2eligible < -0.01 || rss.excluded2eligible > 0.01) fail(rss.ruleInfo.name + " ratio of excluded to eligible is ("+rss.excluded2eligible+") not in the -0.01 to 0.01 range!")
        if (rss.meetMeasure2eligible < 40.0 || rss.meetMeasure2eligible > 60.0) fail(rss.ruleInfo.name + " ratio of meet measure to eligible is ("+rss.meetMeasure2eligible+") not in the 40 - 60 range!")
        //        println(rss.ruleInfo.name + ": " + rss.meetDemographics + ", " + rss.eligible + " (" + rss.eligible2MeetDemographics + "), " + rss.excluded + " (" + rss.excluded2eligible + "), " + rss.meetMeasure + " (" + rss.meetMeasure2eligible + ")")
      }
    }
  }
}
