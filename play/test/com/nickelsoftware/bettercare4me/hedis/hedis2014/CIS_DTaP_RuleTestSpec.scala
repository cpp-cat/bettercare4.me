/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import org.joda.time.LocalDate

class CIS_DTaP_RuleTestSpec extends PlaySpec {
  
    "The CIS_DTaP_Rule class representing DTaP Vaccination HEDIS rule" must {
    
    "validate patient that meet the measure criteria" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CIS_DTaP.name, "F", new LocalDate(2012, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
   }
    
    "validate patient that does not meet the measure criteria and is not excluded" in {

      val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(CIS_DTaP.name, "M", new LocalDate(2012, 9, 12).toDateTimeAtStartOfDay(), 100, 0, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
  }
}