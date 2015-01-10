/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

class PatientScorecardResultTestSpec extends PlaySpec {

  "The CriteriaResultDetail class" must {

    "serialize to a csv string" in {
      
      val c1 = CriteriaResultDetail("claimId", "providerLName", "providerFName", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay(), "reason")
      c1.toCSVString mustBe "claimId,providerLName,providerFName,1962-07-27T00:00:00.000-04:00,reason\r\n"
      
      val c2 = CriteriaResultDetail("claimId", "provider, LName", "providerFName", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay(), "reason")
      c2.toCSVString mustBe "claimId,\"provider, LName\",providerFName,1962-07-27T00:00:00.000-04:00,reason\r\n"
    }

    "de-serialize from a csv string" in {
      
      val c1 = CriteriaResultDetail("claimId", "providerLName", "providerFName", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay(), "reason")
      CriteriaResultDetail("claimId,providerLName,providerFName,1962-07-27T00:00:00.000-04:00,reason\r\n") mustEqual c1
      
      val c2 = CriteriaResultDetail("claimId", "provider, LName", "providerFName", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay(), "reason")
      CriteriaResultDetail("claimId,\"provider, LName\",providerFName,1962-07-27T00:00:00.000-04:00,reason\r\n") mustEqual c2
    }
  }
}
