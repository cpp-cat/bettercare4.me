/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer
import com.nickelsoftware.bettercare4me.utils.NickelException
import org.joda.time.DateTime
import org.joda.time.Interval
import com.nickelsoftware.bettercare4me.utils.Utils
import scala.util.Random

object HEDISRulesTestSpec {

  def setupTest(name: String, eligibleRate: java.lang.Integer, exclusionRate: java.lang.Integer, meetMeasureRate: java.lang.Integer): (Patient, PatientHistory, HEDISRule) = {
    val dob = new LocalDate(1960, 9, 12).toDateTimeAtStartOfDay()
    setupTest(name, "F", dob, eligibleRate, exclusionRate, meetMeasureRate)
  }

  def setupTest(name: String, gender: String, dob: DateTime, eligibleRate: java.lang.Integer, exclusionRate: java.lang.Integer, meetMeasureRate: java.lang.Integer): (Patient, PatientHistory, HEDISRule) = {
    val persistenceLayer = new SimplePersistenceLayer(88)
    val c = new RuleConfig(Map("name" -> name, "eligibleRate" -> eligibleRate, "exclusionRate" -> exclusionRate, "meetMeasureRate" -> meetMeasureRate))
    val rule = HEDISRules.createRuleByName(c.name, c, new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay())
    val patient = persistenceLayer.createPatient("first", "last", gender, dob)
    val claims = rule.generateClaims(persistenceLayer, patient, persistenceLayer.createProvider("first", "last"), Random.nextInt(100), Random.nextInt(100), Random.nextInt(100))
    val patientHistory = PatientHistoryFactory.createPatientHistory(patient, claims)
    (patient, patientHistory, rule)
  }
}

class HEDISRulesTestSpec extends PlaySpec with OneAppPerSuite {

  "The DateTime class" must {
    
    "compute age from an HEDIS date" in {
      
      val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
      val dobMin = new LocalDate(2012, 1, 1).toDateTimeAtStartOfDay()
      val dobMax = new LocalDate(2012, 12, 31).toDateTimeAtStartOfDay()
      Patient("key1", "Michel", "Dufresne", "M", dobMin).age(hedisDate) mustBe 2
      Patient("key1", "Michel", "Dufresne", "M", dobMax).age(hedisDate) mustBe 2
      
      Patient("key1", "Michel", "Dufresne", "M", dobMin).ageInMonths(hedisDate) mustBe 35
      Patient("key1", "Michel", "Dufresne", "M", dobMax).ageInMonths(hedisDate) mustBe 24
    }
    
    "compute intervale from an HEDIS date" in {
      
      val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()      
      val dobMin = new LocalDate(2014, 1, 1).toDateTimeAtStartOfDay()
      val dobMax = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
      
      Utils.getIntervalFromMonths(12, hedisDate).contains(dobMin) mustBe true
      Utils.getIntervalFromMonths(12, hedisDate).contains(dobMax) mustBe true
      
      Utils.getIntervalFromMonths(12, hedisDate).contains(dobMin.minusDays(1)) mustBe false
      Utils.getIntervalFromMonths(12, hedisDate).contains(dobMax.minusDays(1)) mustBe true
      
      Utils.getIntervalFromMonths(12, hedisDate).contains(dobMin.plusDays(1)) mustBe true
      Utils.getIntervalFromMonths(12, hedisDate).contains(dobMax.plusDays(1)) mustBe false
      
      Utils.getIntervalFromDays(10, hedisDate).contains(new LocalDate(2014, 12, 22).toDateTimeAtStartOfDay()) mustBe true
      Utils.getIntervalFromDays(10, hedisDate).contains(new LocalDate(2014, 12, 21).toDateTimeAtStartOfDay()) mustBe false
      Utils.getIntervalFromDays(10, hedisDate).contains(dobMax) mustBe true
    }
  }
  
  "The HEDISRules class" must {

    "create a TestRule properly from config" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig(Map("name" -> "TEST", "eligibleRate" -> new java.lang.Integer(40), "exclusionRate" -> new java.lang.Integer(5), "meetMeasureRate" -> new java.lang.Integer(92)))
      
      val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
      val rule = HEDISRules.createRuleByName(c.name, c, hedisDate)
      
      rule.name mustBe "TEST"
      rule.fullName mustBe "Test Rule"
      rule.description mustBe "This rule is for testing."
      val patient = persistenceLayer.createPatient("M", "D", "M", new LocalDate(1962, 7, 27).toDateTimeAtStartOfDay())
      val provider = persistenceLayer.createProvider("M", "D")
      val claims = rule.generateClaims(persistenceLayer, patient, provider, Random.nextInt(100), Random.nextInt(100), Random.nextInt(100))
      claims.size mustBe 1
      claims(0) match {
        case claim: MedClaim =>
          claim.patientID mustBe patient.patientID
          claim.providerID mustBe provider.providerID
        case _ => fail("Invalid claim class type!")
      }
    }
    
    "contains a date withing a specified interval" in {
      
      val c = new RuleConfig(Map("name" -> "TEST", "eligibleRate" -> new java.lang.Integer(40), "exclusionRate" -> new java.lang.Integer(5), "meetMeasureRate" -> new java.lang.Integer(92)))
      
      val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
      val rule = new TestRule(c, hedisDate)
      
      rule.getIntervalFromMonths(6).contains(new LocalDate(2014, 7, 1).toDateTimeAtStartOfDay()) mustBe true
      rule.getIntervalFromDays(31).contains(new LocalDate(2014, 12, 1).toDateTimeAtStartOfDay()) mustBe true
      
      rule.getIntervalFromMonths(6).contains(new LocalDate(2014, 6, 30).toDateTimeAtStartOfDay()) mustBe false
      rule.getIntervalFromDays(31).contains(new LocalDate(2014, 11, 30).toDateTimeAtStartOfDay()) mustBe false
      
    }

    "throw NickelException when try to create a rule with an unknown name" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig(Map("name" -> "Unknown Name", "eligibleRate" -> new java.lang.Integer(40), "exclusionRate" -> new java.lang.Integer(5), "meetMeasureRate" -> new java.lang.Integer(92)))
      val hedisDate = new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay()
      
      a[NickelException] should be thrownBy {
        HEDISRules.createRuleByName(c.name, c, hedisDate)
      }
    }
    
  }
}
