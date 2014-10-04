/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard

class CIS_HB_RuleTestSpec extends PlaySpec with OneAppPerSuite {

  "The CIS_HB_Rule class representing Hep B Vaccine HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 20) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CIS_HB.name, "F", new LocalDate(2012, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 20) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CIS_HB.name, "M", new LocalDate(2012, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 0)
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