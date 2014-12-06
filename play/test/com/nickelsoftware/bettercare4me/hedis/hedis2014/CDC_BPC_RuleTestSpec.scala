/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard

class CDC_BPC_RuleTestSpec extends PlaySpec {

  "The CDC_BPC_T_Rule class representing Diabetes BP Test Performed HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 20) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDC_BPC.nameTest, 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
        
        val predicateName = scorecard.hedisRuleMap(rule.name).meetMeasure.criteriaScore.keySet.head
        predicateName mustBe CDC_BPC.bpTest
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 100) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDC_BPC.nameTest, 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false

        if (rule.isPatientMeetMeasure(scorecard)) {
          fail("Meet Measure should have failed, but rule fired: " + scorecard.hedisRuleMap(rule.name).meetMeasure.criteriaScore.keySet)
        }
      }
    }
  }

  "The CDC_BPC_C1_Rule class representing Diabetes BP Test <140/80 mmHg Performed HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 20) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDC_BPC.nameC1, 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
        
        val predicateName = scorecard.hedisRuleMap(rule.name).meetMeasure.criteriaScore.keySet.head
        predicateName mustBe CDC_BPC.bpTestC1
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 100) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDC_BPC.nameC1, 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false

        if (rule.isPatientMeetMeasure(scorecard)) {
          fail("Meet Measure should have failed, but rule fired: " + scorecard.hedisRuleMap(rule.name).meetMeasure.criteriaScore.keySet)
        }
      }
    }
  }

  "The CDC_BPC_C2_Rule class representing Diabetes BP Test <140/90 mmHg Performed HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 20) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDC_BPC.nameC2, 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
        
        val predicateName = scorecard.hedisRuleMap(rule.name).meetMeasure.criteriaScore.keySet.head
        predicateName mustBe CDC_BPC.bpTestC2
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 100) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDC_BPC.nameC2, 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false

        if (rule.isPatientMeetMeasure(scorecard)) {
          fail("Meet Measure should have failed, but rule fired: " + scorecard.hedisRuleMap(rule.name).meetMeasure.criteriaScore.keySet)
        }
      }
    }
  }
}