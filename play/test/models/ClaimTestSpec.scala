/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models;

import play.api.Play.current
import org.scalatest._
import org.scalatestplus.play._
import java.io.File
import org.joda.time.LocalDate
import play.api.Logger

class ClaimTestSpec extends PlaySpec with OneAppPerSuite {

  "The Claim class" must {

    "be created with valid arguments" in {
      val claim = Claim("claim 1", "patient.uuid", "provider.uuid",
        LocalDate.parse("2014-09-05"), "icd 1", Set("icd 1", "icd 2"), Set("icd p1"),
        "hcfaPOS", "ubRevenue", "cpt", "hcpcs")

      claim.uuid mustBe "claim 1"
      claim.patientUuid mustBe "patient.uuid"
      claim.providerUuid mustBe "provider.uuid"
      claim.dos mustBe new LocalDate(2014, 9, 5)
      claim.icd_d_pri mustBe "icd 1"
      claim.icd_d mustBe Set("icd 1", "icd 2")
      claim.icd_p mustBe Set("icd p1")
      claim.hcfaPOS mustBe "hcfaPOS"
      claim.ubRevenue mustBe "ubRevenue"
      claim.cpt mustBe "cpt"
      claim.hcpcs mustBe "hcpcs"
    }

    "put all atributes into a List" in {
      val claim = Claim("claim 1", "patient.uuid", "provider.uuid",
        LocalDate.parse("2014-09-05"), "icd 1", Set("icd 1", "icd 2"), Set("icd p1"),
        "hcfaPOS", "ubRevenue", "cpt", "hcpcs")

      val l = claim.toList
      val ans = List("claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "icd 1",
        "icd 1", "icd 2", "", "", "", "", "", "", "", "",
        "icd p1", "", "", "", "", "", "", "", "", "",
        "hcfaPOS", "ubRevenue", "cpt", "hcpcs")

      l.size mustBe ans.size
      l.slice(0, 5) mustBe ans.slice(0, 5)
      l.slice(7, ans.size-1) mustBe ans.slice(7, ans.size-1)
    }

    "put all atributes into a List (boundary case)" in {
      val claim = Claim("claim 1", "patient.uuid", "provider.uuid",
        LocalDate.parse("2014-09-05"), "icd 1", Set("icd 1"), Set(),
        "hcfaPOS", "ubRevenue", "cpt", "hcpcs")

      val l = claim.toList
      val ans = List("claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "icd 1",
        "icd 1", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        "hcfaPOS", "ubRevenue", "cpt", "hcpcs")

      l.size mustBe ans.size
      l mustBe ans
    }
  }
  
  "The SimplePersistenceLayer class" must {
    
    "create Claims with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)
      
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs") mustBe Claim("claim-99-0", "patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs")
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs") mustBe Claim("claim-99-1", "patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs")
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs") mustBe Claim("claim-99-2", "patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs")
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs") mustBe Claim("claim-99-3", "patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs")
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs") mustBe Claim("claim-99-4", "patient.uuid", "provider.uuid", new LocalDate(2014, 9, 5), "icd 1", Set("icd 1"), Set(), "hcfaPOS", "ubRevenue", "cpt", "hcpcs")
    }
  }
}