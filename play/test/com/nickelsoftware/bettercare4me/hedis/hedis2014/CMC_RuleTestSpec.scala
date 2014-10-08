/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard

class CMC_RuleTestSpec extends PlaySpec with OneAppPerSuite {

  "The CMC_LDL_C_TestRule class representing Lipid Test for Cardiovascular Patients HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CMC.nameTest, 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CMC.nameTest, 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }
  }

  "The CMC_LDL_C_TestValueRule class representing Lipid Test <100 mg/dL for Cardiovascular Patients HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CMC.nameTestValue, 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CMC.nameTestValue, 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }
  }
}