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

class ProviderTestSpec extends PlaySpec with OneAppPerSuite {

  "The Provider class" must {

    "be created with valid arguments" in {
      val provider = Provider("key1", "Michel", "Dufresne")

      provider.providerID mustBe "key1"
      provider.firstName mustBe "Michel"
      provider.lastName mustBe "Dufresne"
    }

    "put all atributes into a List" in {
      val provider = Provider("key1", "Michel", "Dufresne")

      provider.toList mustBe List("key1", "Michel", "Dufresne")
    }
    
    "create a Provider from a list of attributes" in {
      val provider = Provider("key1", "Michel", "Dufresne")

      ProviderParser.fromList(provider.toList) mustBe provider
    }
  }
  
  "The SimplePersistenceLayer class" must {
    
    "create Provider with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)
      
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-0", "Michel", "Dufresne")
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-1", "Michel", "Dufresne")
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-2", "Michel", "Dufresne")
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-3", "Michel", "Dufresne")
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-4", "Michel", "Dufresne")
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-5", "Michel", "Dufresne")
    }
    
    "create Provider with sequential keys (independent of Patient and Claim keys)" in {
      val persistenceLayer = new SimplePersistenceLayer(99)
      
      val dob = new LocalDate(1962, 7, 27).toDateTimeAtStartOfDay()
      val dos = new LocalDate(2014, 9, 5).toDateTimeAtStartOfDay()
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-0", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-0", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", dos, dos) mustBe Claim("claim-99-0", "patient.uuid", "provider.uuid", dos, dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-1", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-1", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", dos, dos) mustBe Claim("claim-99-1", "patient.uuid", "provider.uuid", dos, dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-2", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-2", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", dos, dos) mustBe Claim("claim-99-2", "patient.uuid", "provider.uuid", dos, dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-3", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-3", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", dos, dos) mustBe Claim("claim-99-3", "patient.uuid", "provider.uuid", dos, dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-4", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-4", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createClaim("patient.uuid", "provider.uuid", dos, dos) mustBe Claim("claim-99-4", "patient.uuid", "provider.uuid", dos, dos)
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-5", "Michel", "Dufresne")
    }
  }
}