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

class CDCHbA1cTest8RuleTestSpec extends PlaySpec with OneAppPerSuite {

    "The CDCHbA1cTestValueRule class representing Diabetes HbA1c Test with value (less than 7%, less than 8%, more than 9%) HEDIS rule" must {
    
    "validate patient that meet the 'less than 7%' measure criteria" in {
      
      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCHbA1cTestValue.name7, true)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe true
    }
    
    "validate patient that does not meet the 'less than 7%' measure criteria and is not excluded" in {
      
      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCHbA1cTestValue.name7, false)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe false
    }
    
    "validate patient that meet the 'less than 8%' measure criteria" in {
      
      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCHbA1cTestValue.name8, true)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe true
    }
    
    "validate patient that does not meet the 'less than 8%' measure criteria and is not excluded" in {
      
      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCHbA1cTestValue.name8, false)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe false
    }
    
    "validate patient that meet the 'more than 9%' measure criteria" in {
      
      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCHbA1cTestValue.name9, true)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe true
    }
    
    "validate patient that does not meet the 'more than 9%' measure criteria and is not excluded" in {
      
      val (patient, patientHistory, rule) = CDCMeetCriteriaTest.meetCriteria(CDCHbA1cTestValue.name9, false)
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe false
    }
  }
}