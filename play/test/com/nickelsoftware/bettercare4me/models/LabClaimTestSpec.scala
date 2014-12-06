/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models;

import scala.math.BigDecimal.double2bigDecimal

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

class LabClaimTestSpec extends PlaySpec {

  "The LabClaim class" must {

    "be created with valid arguments" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = LabClaim("claim 1", "patient.uuid", "provider.uuid",
        dos, claimStatus="claimStatus", cpt="cpt", loinc="loinc", result=60.0123456789, posNegResult="1")

      claim.claimID mustBe "claim 1"
      claim.patientID mustBe "patient.uuid"
      claim.providerID mustBe "provider.uuid"
      claim.dos mustBe dos
      claim.claimStatus mustBe "claimStatus"
      claim.cpt mustBe "cpt"
      claim.loinc mustBe "loinc"
      claim.result mustBe BigDecimal(60.0123456789)
      claim.posNegResult mustBe "1"
    }

    "put all atributes into a List" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = LabClaim("claim 1", "patient.uuid", "provider.uuid",
        dos, claimStatus="claimStatus", cpt="cpt", loinc="loinc", result=60.0123456789, posNegResult="1")

      val l = claim.toList
      val ans = List("LC", "claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "claimStatus", "cpt", "loinc", "60.0123456789", "1")

      l.size mustBe ans.size
      l mustBe ans
    }

    "put all atributes into a List (default values)" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = LabClaim("claim 1", "patient.uuid", "provider.uuid", dos)

      val l = claim.toList
      val ans = List("LC", "claim 1", "patient.uuid", "provider.uuid",
        "2014-09-05", "", "", "", "0.0", "")

      l.size mustBe ans.size
      l mustBe ans
    }
    
    "parse a Claim from a list of attributes" in {
      val dos = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val claim = LabClaim("claim 1", "patient.uuid", "provider.uuid",
        dos, claimStatus="claimStatus", cpt="cpt", loinc="loinc", result=60.0123456789, posNegResult="1")
        
        ClaimParser.fromList(claim.toList) mustBe claim
    }
  }
  
  "The SimplePersistenceLayer class" must {
    
    "create LabClaims with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)
      
      val dos = new LocalDate(2014, 9, 5).toDateTimeAtStartOfDay()
      persistenceLayer.createLabClaim("patient.uuid", "provider.uuid", dos) mustBe LabClaim("c-lc-99-0", "patient.uuid", "provider.uuid", dos)
      persistenceLayer.createLabClaim("patient.uuid", "provider.uuid", dos) mustBe LabClaim("c-lc-99-1", "patient.uuid", "provider.uuid", dos)
      persistenceLayer.createLabClaim("patient.uuid", "provider.uuid", dos) mustBe LabClaim("c-lc-99-2", "patient.uuid", "provider.uuid", dos)
      persistenceLayer.createLabClaim("patient.uuid", "provider.uuid", dos) mustBe LabClaim("c-lc-99-3", "patient.uuid", "provider.uuid", dos)
      persistenceLayer.createLabClaim("patient.uuid", "provider.uuid", dos) mustBe LabClaim("c-lc-99-4", "patient.uuid", "provider.uuid", dos)
    }
  }
}
