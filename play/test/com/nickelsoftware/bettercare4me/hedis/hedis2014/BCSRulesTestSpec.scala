/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer

class BCSRulesTestSpec extends PlaySpec with OneAppPerSuite {
  
  "The BCSRule class representing Breast Cancer Screening HEDIS rule" must {
    
    "validate rule creation and meta data" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName("BCS-HEDIS-2014")
      c.setEligibleRate(66)
      c.setMeetMeasureRate(77)
      c.setExclusionRate(88)
      val rule = HEDISRules.createRuleByName(c.getName)(c, new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay())

      rule.name mustBe "BCS-HEDIS-2014"
      rule.fullName mustBe "Breast Cancer Screening"
      rule.description mustBe "Breast Cancer Screening indicates whether a woman member, aged 42 to 69 years, had a mammogram done during the measurement year or the year prior to the measurement year. This excludes women who had a bilateral mastectomy or two unilateral mastectomies."
      rule.eligibleRate mustBe 100 // rule overrides this attribute
      rule.meetMeasureRate mustBe 77
      rule.exclusionRate mustBe 88
    }
    
    "validate patient's demographics correctly" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName("BCS-HEDIS-2014")
      c.setEligibleRate(100)
      c.setMeetMeasureRate(100)
      c.setExclusionRate(0)      
      val rule = HEDISRules.createRuleByName(c.getName)(c, new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay())
      val dob = new LocalDate(2014, 9, 12).toDateTimeAtStartOfDay()
      
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob)) mustBe false
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob)) mustBe false
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(42))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.plusYears(41))) mustBe false
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(52))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob.minusYears(52))) mustBe false
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(69))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(70))) mustBe false
    }
    
    "validate excluded patients criteria" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName("BCS-HEDIS-2014")
      c.setEligibleRate(100)
      c.setMeetMeasureRate(0)
      c.setExclusionRate(100)
      val rule = HEDISRules.createRuleByName(c.getName)(c, new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay())
      val dob = new LocalDate(1960, 9, 12).toDateTimeAtStartOfDay()
      val patient = persistenceLayer.createPatient("first", "last", "F", dob)
      val claims = rule.generateClaims(persistenceLayer, patient, persistenceLayer.createProvider("first", "last"))
      val patientHistory = PatientHistoryFactory.createPatientHistory(patient, claims)
      
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe true
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe false
    }
    
    "validate patient that meet the measure criteria" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName("BCS-HEDIS-2014")
      c.setEligibleRate(100)
      c.setMeetMeasureRate(100)
      c.setExclusionRate(0)
      val rule = HEDISRules.createRuleByName(c.getName)(c, new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay())
      val dob = new LocalDate(1960, 9, 12).toDateTimeAtStartOfDay()
      val patient = persistenceLayer.createPatient("first", "last", "F", dob)
      val claims = rule.generateClaims(persistenceLayer, patient, persistenceLayer.createProvider("first", "last"))
      val patientHistory = PatientHistoryFactory.createPatientHistory(patient, claims)
      
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe true
    }
    
    "validate patient that does not meet the measure criteria and is not excluded" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName("BCS-HEDIS-2014")
      c.setEligibleRate(100)		// not used, no claims generated
      c.setMeetMeasureRate(100)		// not used, no claims generated
      c.setExclusionRate(0)			// not used, no claims generated
      val rule = HEDISRules.createRuleByName(c.getName)(c, new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay())
      val dob = new LocalDate(1960, 9, 12).toDateTimeAtStartOfDay()
      val patient = persistenceLayer.createPatient("first", "last", "F", dob)
      val claims = List[Claim]()
      val patientHistory = PatientHistoryFactory.createPatientHistory(patient, claims)
      
      rule.isPatientMeetDemographic(patient) mustBe true
      rule.isPatientEligible(patient, patientHistory) mustBe true
      rule.isPatientExcluded(patient, patientHistory) mustBe false
      rule.isPatientMeetMeasure(patient, patientHistory) mustBe false
    }
  }
}
