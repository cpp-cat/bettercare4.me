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
import org.joda.time.DateTime
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import CDC.ndcAKey
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard

class CDCRuleBaseTestSpec extends PlaySpec with OneAppPerSuite {

  class CDCRuleBaseTest(config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {
    val name = "TEST"
    val fullName = "Test Rule"
    val description = "This rule is for testing."
    def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean = true
    def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
      scorecard.addScore("TEST", HEDISRule.meetMeasure, true)
  }

  // override to use CDCRuleBaseTest
  def setupTest(name: String, eligibleRate: Int, exclusionRate: Int, meetMeasureRate: Int): (Patient, PatientHistory, HEDISRule) = {
    val persistenceLayer = new SimplePersistenceLayer(88)
    val c = new RuleConfig
    c.setName(name)
    c.setEligibleRate(eligibleRate)
    c.setExclusionRate(exclusionRate)
    c.setMeetMeasureRate(meetMeasureRate)
    val rule = new CDCRuleBaseTest(c, new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay())
    val dob = new LocalDate(1960, 9, 12).toDateTimeAtStartOfDay()
    val patient = persistenceLayer.createPatient("first", "last", "F", dob)
    val claims = rule.generateClaims(persistenceLayer, patient, persistenceLayer.createProvider("first", "last"))
    val patientHistory = PatientHistoryFactory.createPatientHistory(patient, claims)
    (patient, patientHistory, rule)
  }
  
  import CDC._

  "The CDCRuleBase class identify patients in the denominator or meet the exclusion criteria for the Compehensive Diabetes Control HEDIS rules" must {

    "validate patient's demographics correctly" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName("TEST")
      c.setEligibleRate(100)
      c.setExclusionRate(0)
      c.setMeetMeasureRate(100)
      val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
      val rule = new CDCRuleBaseTest(c, hedisDate)
      val dob = new LocalDate(2014, 9, 12).toDateTimeAtStartOfDay()
      
      persistenceLayer.createPatient("first", "last", "M", dob).age(hedisDate) mustBe 0
      persistenceLayer.createPatient("first", "last", "M", dob.minusYears(18)).age(hedisDate) mustBe 18
      persistenceLayer.createPatient("first", "last", "M", dob.minusYears(75)).age(hedisDate) mustBe 75

      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob)) mustBe false
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob)) mustBe false
      
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(18))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(17))) mustBe false
      
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob.minusYears(18))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob.minusYears(17))) mustBe false
      
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(75))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusYears(76))) mustBe false
      
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob.minusYears(75))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob.minusYears(76))) mustBe false      
    }

    "validate patients in the denominator (eligible and not excluded)" in {

      val (patient, patientHistory, rule) = setupTest("TEST", 100, 0, 100)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe true
    }

    "validate patients not in the denominator (not eligible)" in {

      val (patient, patientHistory, rule) = setupTest("TEST", 0, 0, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe false
      rule.isPatientExcluded(scorecard) mustBe false
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }

    "validate excluded patients criteria (eligible and excluded)" in {

      val (patient, patientHistory, rule) = setupTest("TEST", 100, 100, 0)
      val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)
      
      rule.isPatientEligible(scorecard) mustBe true
      rule.isPatientExcluded(scorecard) mustBe true
      rule.isPatientMeetMeasure(scorecard) mustBe false
    }
  }
}
