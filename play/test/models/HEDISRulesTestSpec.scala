/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models;

import play.api.Play.current
import org.scalatest._
import org.scalatestplus.play._
import org.joda.time.LocalDate
import play.api.Logger

class HEDISRulesTestSpec extends PlaySpec with OneAppPerSuite {

  "The HEDISRules class" must {

    "create a TestRule properly from config" in {

      val persistenceLayer = new SimplePersistenceLayer(88)
      val c = new RuleConfig
      c.setName("TEST")
      c.setEligibleRate(40)
      c.setMeetMeasureRate(92)
      c.setExclusionRate(5)
      
      val hedisDate = new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay()
      val rule = HEDISRules.createRuleByName(c.getName)(c, hedisDate)

      rule.name mustBe "TEST"
      rule.fullName mustBe "Test Rule"
      rule.description mustBe "This rule is for testing."
      val patient = persistenceLayer.createPatient("M", "D", "M", new LocalDate(1962, 7, 27).toDateTimeAtStartOfDay())
      val provider = persistenceLayer.createProvider("M", "D")
      val claims = rule.generateClaims(persistenceLayer, patient, provider)
      claims.size mustBe 1
      claims(0) match {
        case claim: MedClaim =>
          claim.patientID mustBe patient.patientID
          claim.providerID mustBe provider.providerID
        case _ => fail("Invalid claim class type!")
      }
    }
  }
}