/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import com.nickelsoftware.bettercare4me.utils.NickelException
import org.joda.time.LocalDate
import com.nickelsoftware.bettercare4me.models.MedClaim

class ScorecardTestSpec extends PlaySpec {

  "The Scorecard class" must {

    "update the scorecard when a criteria is set with an override for a measure" in {

      val scorecard = Scorecard().addScore("TEST", HEDISRule.meetDemographic, true)
      scorecard.isPatientMeetDemographic("TEST") mustBe true
      scorecard.isPatientEligible("TEST") mustBe false
      scorecard.isPatientExcluded("TEST") mustBe false
      scorecard.isPatientMeetMeasure("TEST") mustBe false
      
      val s2 = scorecard.addScore("TEST", HEDISRule.eligible, true)
      s2.isPatientMeetDemographic("TEST") mustBe true
      s2.isPatientEligible("TEST") mustBe true
      s2.isPatientExcluded("TEST") mustBe false
      s2.isPatientMeetMeasure("TEST") mustBe false
      
      val s3 = s2.addScore("TEST", HEDISRule.excluded, true)
      s3.isPatientMeetDemographic("TEST") mustBe true
      s3.isPatientEligible("TEST") mustBe true
      s3.isPatientExcluded("TEST") mustBe true
      s3.isPatientMeetMeasure("TEST") mustBe false
      
      val s4 = s2.addScore("TEST", HEDISRule.meetMeasure, true)
      s4.isPatientMeetDemographic("TEST") mustBe true
      s4.isPatientEligible("TEST") mustBe true
      s4.isPatientExcluded("TEST") mustBe false
      s4.isPatientMeetMeasure("TEST") mustBe true
      
      s4.isPatientMeetDemographic("TEST2") mustBe false
      s4.isPatientMeetMeasure("TEST2") mustBe false
    }
    
    "update the scorecard for a unknown criteria throws and exception" in {
      
      a[NickelException] should be thrownBy {
        Scorecard().addScore("TEST", "unknown", true)
      }
    }
    
    "update the scorecard when a criteria is set with a list of claims" in {
      
      val s1 = Scorecard().addScore("TEST", HEDISRule.eligible, "predicate 1", List())
      s1.isPatientMeetDemographic("TEST") mustBe false
      s1.isPatientEligible("TEST") mustBe false
      s1.isPatientExcluded("TEST") mustBe false
      s1.isPatientMeetMeasure("TEST") mustBe false
      
      val dos = new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay()
      val s2 = s1.addScore("TEST", HEDISRule.eligible, "predicate 1", List(MedClaim("c", "p", "p", dos, dos)))
      s2.isPatientEligible("TEST") mustBe true
    }
  }
}