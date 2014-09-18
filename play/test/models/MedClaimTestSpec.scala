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

class MedClaimTestSpec extends PlaySpec with OneAppPerSuite {

  "The MedClaim class" must {

    "be created with valid arguments" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = MedClaim("claim 1", "patient.uuid", "provider.uuid",
        dos, dos, claimStatus="claimStatus", pcpFlag="Y", icdDPri="icd 1", icdD=Set("icd 1", "icd 2"), icdP=Set("icd p1"),
        hcfaPOS="hcfaPOS", drg="drg", tob="tob", ubRevenue="ubRevenue", 
        cpt="cpt", cptMod1="cptMod1", cptMod2="cptMod2", 
        hcpcs="hcpcs", hcpcsMod="hcpcsMod", dischargeStatus="dischargeStatus", daysDenied=5, roomBoardFlag="Y")

      claim.claimID mustBe "claim 1"
      claim.patientID mustBe "patient.uuid"
      claim.providerID mustBe "provider.uuid"
      claim.dos mustBe dos
      claim.dosThru mustBe dos
      claim.claimStatus mustBe "claimStatus"
      claim.pcpFlag mustBe "Y"
      claim.icdDPri mustBe "icd 1"
      claim.icdD mustBe Set("icd 1", "icd 2")
      claim.icdP mustBe Set("icd p1")
      claim.hcfaPOS mustBe "hcfaPOS"
      claim.drg mustBe "drg"
      claim.tob mustBe "tob"
      claim.ubRevenue mustBe "ubRevenue"
      claim.cpt mustBe "cpt"
      claim.cptMod1 mustBe "cptMod1"
      claim.cptMod2 mustBe "cptMod2"
      claim.hcpcs mustBe "hcpcs"
      claim.hcpcsMod mustBe "hcpcsMod"
      claim.dischargeStatus mustBe "dischargeStatus"
      claim.daysDenied mustBe 5
      claim.roomBoardFlag mustBe "Y"
    }

    "put all atributes into a List" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = MedClaim("claim 1", "patient.uuid", "provider.uuid",
        dos, dos, claimStatus="claimStatus", pcpFlag="Y", icdDPri="icd 1", icdD=Set("icd 1", "icd 2"), icdP=Set("icd p1"),
        hcfaPOS="hcfaPOS", drg="drg", tob="tob", ubRevenue="ubRevenue", 
        cpt="cpt", cptMod1="cptMod1", cptMod2="cptMod2", 
        hcpcs="hcpcs", hcpcsMod="hcpcsMod", dischargeStatus="dischargeStatus", daysDenied=5, roomBoardFlag="Y")

      val l = claim.toList
      val ans = List("MD", "claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "2014-09-05", "claimStatus", "Y", "icd 1",
        "icd 1", "icd 2", "", "", "", "", "", "", "", "",
        "icd p1", "", "", "", "", "", "", "", "", "",
        "hcfaPOS", "drg", "tob", "ubRevenue", "cpt", "cptMod1", "cptMod2", 
        "hcpcs", "hcpcsMod", "dischargeStatus", "5", "Y")

      l.size mustBe ans.size
      l mustBe ans
    }

    "put all atributes into a List (default values)" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = MedClaim("claim 1", "patient.uuid", "provider.uuid", dos, dos)

      val l = claim.toList
      val ans = List("MD", "claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "2014-09-05", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "", "", "", 
        "", "", "", "0", "N")

      l.size mustBe ans.size
      l mustBe ans
    }
    
    "parse a Claim from a list of attributes" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = MedClaim("claim 1", "patient.uuid", "provider.uuid",
        dos, dos, claimStatus="claimStatus", pcpFlag="Y", icdDPri="icd 1", icdD=Set("icd 1", "icd 2"), icdP=Set("icd p1"),
        hcfaPOS="hcfaPOS", drg="drg", tob="tob", ubRevenue="ubRevenue", 
        cpt="cpt", cptMod1="cptMod1", cptMod2="cptMod2", 
        hcpcs="hcpcs", hcpcsMod="hcpcsMod", dischargeStatus="dischargeStatus", daysDenied=5, roomBoardFlag="Y")
        
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