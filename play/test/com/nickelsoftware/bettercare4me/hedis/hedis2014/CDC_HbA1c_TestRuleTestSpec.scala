/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer
import CDC.ndcAKey
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard

class CDCHbA1cTestRuleTestSpec extends PlaySpec with OneAppPerSuite {
  
    "The CDC_HbA1c_TestRule class representing Diabetes HbA1c Test HEDIS rule" must {
    
    "validate patient that meet the measure criteria" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTest.name, 100, 0, 100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
    }
    
    "validate patient that does not meet the measure criteria and is not excluded" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CDCHbA1cTest.name, 100, 0, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
  }
}