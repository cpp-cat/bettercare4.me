/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer

class CIS_RuleBaseTestSpec extends PlaySpec {

  class CIS_RuleBaseTest(config: RuleConfig, hedisDate: DateTime) extends CIS_RuleBase(config, hedisDate) {
    val name = "TEST"
    val fullName = "Test Rule"
    val description = "This rule is for testing."
    def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean = true
    def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
      scorecard.addScore("TEST", "TEST", HEDISRule.meetMeasure, true)
  }

  def setupTest(exclusionRate: java.lang.Integer): (Patient, PatientHistory, HEDISRule) = {
    val persistenceLayer = new SimplePersistenceLayer(88)
    val c = new RuleConfig(Map("name" -> "TEST", "eligibleRate" -> new java.lang.Integer(100), "exclusionRate" -> exclusionRate, "meetMeasureRate" -> new java.lang.Integer(100)))
    val rule = new CIS_RuleBaseTest(c, new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay())
    val dob = new LocalDate(2012, 9, 12).toDateTimeAtStartOfDay()
    val patient = persistenceLayer.createPatient("first", "last", "F", dob)
    val claims = rule.generateClaims(persistenceLayer, patient, persistenceLayer.createProvider("first", "last"), Random.nextInt(100), Random.nextInt(100), Random.nextInt(100))
    val patientHistory = PatientHistoryFactory.createPatientHistory(patient, claims)
    (patient, patientHistory, rule)
  }
  
  import CIS._

  "The CIS_RuleBase class identify patients in the denominator or meet the exclusion criteria for the Immunization Base HEDIS rules" must {

    "validate patient's demographics correctly" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig(Map("name" -> "TEST", "eligibleRate" -> new java.lang.Integer(100), "exclusionRate" -> new java.lang.Integer(0), "meetMeasureRate" -> new java.lang.Integer(100)))
      val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
      val rule = new CIS_RuleBaseTest(c, hedisDate)
      
      val dobMin = new LocalDate(2012, 1, 1).toDateTimeAtStartOfDay()
      val dobMax = new LocalDate(2012, 12, 31).toDateTimeAtStartOfDay()
      persistenceLayer.createPatient("first", "last", "M", dobMin).age(hedisDate) mustBe 2
      persistenceLayer.createPatient("first", "last", "M", dobMax).age(hedisDate) mustBe 2
      persistenceLayer.createPatient("first", "last", "M", dobMin).ageInMonths(hedisDate) mustBe 35
      persistenceLayer.createPatient("first", "last", "M", dobMax).ageInMonths(hedisDate) mustBe 24

      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dobMin)) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dobMax)) mustBe true
      
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dobMin.minusDays(1))) mustBe false
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dobMax.minusDays(10))) mustBe true
      
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dobMin.plusDays(10))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dobMax.plusDays(1))) mustBe false
    }

    "validate patients in the denominator (eligible and not excluded)" in {

      val (patient, patientHistory, rule) = setupTest(0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
    }

    "validate excluded patients criteria (eligible and excluded)" in {

      val (patient, patientHistory, rule) = setupTest(100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe true
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
  }
}
