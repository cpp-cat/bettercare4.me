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

class CDCEERuleTestSpec extends PlaySpec with OneAppPerSuite {
  
    "The CDCEERule class representing Diabetes Eye Exam HEDIS rule" must {
    
    "validate patient that meet the measure criteria" in {

      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCEE.name, true)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe true
    }
    
    "validate patient that does not meet the measure criteria and is not excluded" in {

      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCEE.name, false)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe false
    }
  }
}