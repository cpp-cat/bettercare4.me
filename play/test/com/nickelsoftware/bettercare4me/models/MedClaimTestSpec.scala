/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

object MedClaimTestSpecHelper {
  val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()

  def mkClaim = MedClaim("claimID", "patientID", "providerID", dos, dos,
    MHead("claimStatus", "Y", "specialtyCde", "hcfaPOS", "dischargeStatus", 5, "Y"),
    MCodes("icdDPri", Set("icd 1", "icd 2"), Set("icd p1"), "drg", "cpt", "cptMod1", "cptMod2"),
    MBill("tob", "ubRevenue", "hcpcs", "hcpcsMod"))

  // default values
  def mkClaim0 = MedClaim("claimID", "patientID", "providerID", dos, dos)

}

class MedClaimTestSpec extends PlaySpec {

  import MedClaimTestSpecHelper._

  "The MedClaim class" must {

    "be created with valid arguments" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = mkClaim

      claim.claimID mustBe "claimID"
      claim.patientID mustBe "patientID"
      claim.providerID mustBe "providerID"
      claim.dos mustBe dos
      claim.dosThru mustBe dos
      claim.claimStatus mustBe "claimStatus"
      claim.pcpFlag mustBe "Y"
      claim.specialtyCde mustBe "specialtyCde"
      claim.hcfaPOS mustBe "hcfaPOS"
      claim.dischargeStatus mustBe "dischargeStatus"
      claim.daysDenied mustBe 5
      claim.roomBoardFlag mustBe "Y"
      claim.icdDPri mustBe "icdDPri"
      claim.icdD mustBe Set("icd 1", "icd 2")
      claim.icdP mustBe Set("icd p1")
      claim.drg mustBe "drg"
      claim.cpt mustBe "cpt"
      claim.cptMod1 mustBe "cptMod1"
      claim.cptMod2 mustBe "cptMod2"
      claim.tob mustBe "tob"
      claim.ubRevenue mustBe "ubRevenue"
      claim.hcpcs mustBe "hcpcs"
      claim.hcpcsMod mustBe "hcpcsMod"
    }

    "put all atributes into a List" in {
      val claim = mkClaim

      val l = claim.toList
      val ans = List("MD", "claimID", "patientID", "providerID", "2014-09-05", "2014-09-05",
        "claimStatus", "Y", "specialtyCde", "hcfaPOS", "dischargeStatus", "5", "Y",
        "icdDPri",
        "icd 1", "icd 2", "", "", "", "", "", "", "", "",
        "icd p1", "", "", "", "", "", "", "", "", "",
        "drg", "cpt", "cptMod1", "cptMod2",
        "tob", "ubRevenue", "hcpcs", "hcpcsMod")

      l.size mustBe ans.size
      l mustBe ans
    }

    "put all atributes into a List (default values)" in {
      val claim = mkClaim0

      val l = claim.toList
      val ans = List("MD", "claimID", "patientID", "providerID", "2014-09-05", "2014-09-05",
        "", "", "", "", "", "0", "N",
        "",
        "", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "")

      l.size mustBe ans.size
      l mustBe ans
    }

    "parse a Claim from a list of attributes" in {
      val claim = mkClaim
      ClaimParser.fromList(claim.toList) mustBe claim
    }
  }

  "The SimplePersistenceLayer class" must {

    "create MedClaims with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)

      val dos = new LocalDate(2014, 9, 5).toDateTimeAtStartOfDay()
      persistenceLayer.createMedClaim("patient.uuid", "provider.uuid", dos, dos) mustBe MedClaim("c-md-99-0", "patient.uuid", "provider.uuid", dos, dos)
      persistenceLayer.createMedClaim("patient.uuid", "provider.uuid", dos, dos) mustBe MedClaim("c-md-99-1", "patient.uuid", "provider.uuid", dos, dos)
      persistenceLayer.createMedClaim("patient.uuid", "provider.uuid", dos, dos) mustBe MedClaim("c-md-99-2", "patient.uuid", "provider.uuid", dos, dos)
      persistenceLayer.createMedClaim("patient.uuid", "provider.uuid", dos, dos) mustBe MedClaim("c-md-99-3", "patient.uuid", "provider.uuid", dos, dos)
      persistenceLayer.createMedClaim("patient.uuid", "provider.uuid", dos, dos) mustBe MedClaim("c-md-99-4", "patient.uuid", "provider.uuid", dos, dos)
    }
  }
}
