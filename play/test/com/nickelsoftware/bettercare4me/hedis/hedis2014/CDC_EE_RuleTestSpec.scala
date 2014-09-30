/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard

class CDC_EE_RuleTestSpec extends PlaySpec with OneAppPerSuite {
  
    "The CDC_EE_Rule class representing Diabetes Eye Exam HEDIS rule" must {
    
    "validate patient that meet the measure criteria" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCEE.name, 100, 0, 100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
    }
    
    "validate patient that does not meet the measure criteria and is not excluded" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCEE.name, 100, 0, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
  }
}