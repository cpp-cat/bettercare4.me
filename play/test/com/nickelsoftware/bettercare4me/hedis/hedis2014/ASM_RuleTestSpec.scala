/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import org.joda.time.LocalDate

class ASM_RuleTestSpec extends PlaySpec with OneAppPerSuite {

  "The ASM_Rule class representing Asthma Medication Management (5 - 11) HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name5, "F", new LocalDate(2014 - 7, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
      }
    }

    "validate patient that are excluded" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name5, "F", new LocalDate(2014 - 7, 9, 12).toDateTimeAtStartOfDay(), 100, 100, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe true
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name5, "M", new LocalDate(2014 - 7, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }
  }

  "The ASM_Rule class representing Asthma Medication Management (12 - 18) HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name12, "F", new LocalDate(2014 - 15, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
      }
    }

    "validate patient that are excluded" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name12, "F", new LocalDate(2014 - 15, 9, 12).toDateTimeAtStartOfDay(), 100, 100, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe true
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 100) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name12, "F", new LocalDate(2014 - 15, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }
  }

  "The ASM_Rule class representing Asthma Medication Management (19 - 50) HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name19, "F", new LocalDate(2014 - 22, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
      }
    }

    "validate patient that are excluded" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name19, "M", new LocalDate(2014 - 22, 9, 12).toDateTimeAtStartOfDay(), 100, 100, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe true
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 100) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name19, "F", new LocalDate(2014 - 22, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }
  }

  "The ASM_Rule class representing Asthma Medication Management (51 - 64) HEDIS rule" must {

    "validate patient that meet the measure criteria" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name51, "M", new LocalDate(2014 - 55, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe true
      }
    }

    "validate patient that are excluded" in {

      for (i <- 1 to 25) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name51, "F", new LocalDate(2014 - 55, 9, 12).toDateTimeAtStartOfDay(), 100, 100, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe true
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }

    "validate patient that does not meet the measure criteria and is not excluded" in {

      for (i <- 1 to 100) {
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(ASM.name51, "F", new LocalDate(2014 - 55, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 0)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        rule.isPatientMeetMeasure(scorecard) mustBe false
      }
    }
  }
}