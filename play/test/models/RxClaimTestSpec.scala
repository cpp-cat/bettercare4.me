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

class RxClaimTestSpec extends PlaySpec with OneAppPerSuite {

  "The RxClaim class" must {

    "be created with valid arguments" in {
      val fillD = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = RxClaim("claim 1", "patient.uuid", "provider.uuid",
        fillD, claimStatus="claimStatus", ndc="ndc", daysSupply=30, qty=60, supplyF="Y")

      claim.claimID mustBe "claim 1"
      claim.patientID mustBe "patient.uuid"
      claim.providerID mustBe "provider.uuid"
      claim.fillD mustBe fillD
      claim.claimStatus mustBe "claimStatus"
      claim.ndc mustBe "ndc"
      claim.daysSupply mustBe 30
      claim.qty mustBe 60
      claim.supplyF mustBe "Y"
    }

    "put all atributes into a List" in {
      val fillD = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = RxClaim("claim 1", "patient.uuid", "provider.uuid",
        fillD, claimStatus="claimStatus", ndc="ndc", daysSupply=30, qty=60, supplyF="Y")

      val l = claim.toList
      val ans = List("RX", "claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "claimStatus", "ndc", "30", "60", "Y")

      l.size mustBe ans.size
      l mustBe ans
    }

    "put all atributes into a List (default values)" in {
      val fillD = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = RxClaim("claim 1", "patient.uuid", "provider.uuid", fillD)

      val l = claim.toList
      val ans = List("RX", "claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "", "", "1", "0", "N")

      l.size mustBe ans.size
      l mustBe ans
    }
    
    "parse a Claim from a list of attributes" in {
      val fillD = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = RxClaim("claim 1", "patient.uuid", "provider.uuid",
        fillD, claimStatus="claimStatus", ndc="ndc", daysSupply=30, qty=60, supplyF="Y")
        
        ClaimParser.fromList(claim.toList) mustBe claim
    }
  }
  
  "The SimplePersistenceLayer class" must {
    
    "create RxClaims with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)
      
      val fillD = new LocalDate(2014, 9, 5).toDateTimeAtStartOfDay()
      persistenceLayer.createRxClaim("patient.uuid", "provider.uuid", fillD) mustBe RxClaim("c-rx-99-0", "patient.uuid", "provider.uuid", fillD)
      persistenceLayer.createRxClaim("patient.uuid", "provider.uuid", fillD) mustBe RxClaim("c-rx-99-1", "patient.uuid", "provider.uuid", fillD)
      persistenceLayer.createRxClaim("patient.uuid", "provider.uuid", fillD) mustBe RxClaim("c-rx-99-2", "patient.uuid", "provider.uuid", fillD)
      persistenceLayer.createRxClaim("patient.uuid", "provider.uuid", fillD) mustBe RxClaim("c-rx-99-3", "patient.uuid", "provider.uuid", fillD)
      persistenceLayer.createRxClaim("patient.uuid", "provider.uuid", fillD) mustBe RxClaim("c-rx-99-4", "patient.uuid", "provider.uuid", fillD)
    }
  }
}