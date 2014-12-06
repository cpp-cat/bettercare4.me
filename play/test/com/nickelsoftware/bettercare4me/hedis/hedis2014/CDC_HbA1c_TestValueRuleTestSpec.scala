/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard

class CDCHbA1cTest8RuleTestSpec extends PlaySpec {

    "The CDC_HbA1c_TestValue_Rule class representing Diabetes HbA1c Test with value (less than 7%, less than 8%, more than 9%) HEDIS rule" must {
    
    "validate patient that meet the 'less than 7%' measure criteria" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTestValue.name7, 100, 0, 100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
    }
    
    "validate patient that does not meet the 'less than 7%' measure criteria and is not excluded" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTestValue.name7, 100, 0, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
    
    "validate patient that meet the 'less than 8%' measure criteria" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTestValue.name8, 100, 0, 100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
    }
    
    "validate patient that does not meet the 'less than 8%' measure criteria and is not excluded" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTestValue.name8, 100, 0, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
    
    "validate patient that meet the 'more than 9%' measure criteria" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTestValue.name9, 100, 0, 100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
    }
    
    "validate patient that does not meet the 'more than 9%' measure criteria and is not excluded" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTestValue.name9, 100, 0, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
  }
}