/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.hedis.HEDISRulesTestSpec
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer
import scala.util.Random
import org.joda.time.Interval

class W15_RulesTestSpec extends PlaySpec with OneAppPerSuite {

  "The W15_Rule class representing Well-Child Visits in the First 15 Months of Life HEDIS rule" must {

    "validate patient's demographics correctly" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName(W15.name)
      c.setEligibleRate(100)
      c.setExclusionRate(0)
      c.setMeetMeasureRate(100)
      val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
      val rule = HEDISRules.createRuleByName(c.getName, c, hedisDate)
      val dob = hedisDate.minusMonths(15)

      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "M", dob)) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob)) mustBe true

      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusMonths(1))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.plusMonths(1))) mustBe false

      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusDays(1))) mustBe true
      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.plusDays(1))) mustBe false

      rule.isPatientMeetDemographic(persistenceLayer.createPatient("first", "last", "F", dob.minusMonths(11))) mustBe true
    }

    "validate patient that meet the measure criteria" in {

      import W15._
      for (i <- 1 to 20) {

        val days = new Interval(new LocalDate(2014, 1, 1).toDateTimeAtStartOfDay(), new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay()).toDuration().getStandardDays().toInt
        val hedisDate = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
        val dob = hedisDate.minusDays(Random.nextInt(days)).minusMonths(15)
        val (patient, patientHistory, rule) = HEDISRulesTestSpec.setupTest(name, "M", dob, 100, 0, 100)
        val scorecard = rule.scoreRule(Scorecard(), patient, patientHistory)

        rule.isPatientEligible(scorecard) mustBe true
        rule.isPatientExcluded(scorecard) mustBe false
        
        val meetCriteria = scorecard.hedisRuleMap(rule.name).meetMeasure.criteriaScore.keySet
        if (meetCriteria.size != 1) fail("Expecting a single criteria, got: " + meetCriteria)
        else {
          if (meetCriteria.intersect(Set(wellChildVisit0, wellChildVisit1, wellChildVisit2, wellChildVisit3, wellChildVisit4, wellChildVisit5, wellChildVisit6)).size != 1)
            fail("Unknown meet criteria predicate name, got: " + meetCriteria)
        }
      }
    }
  }
}
