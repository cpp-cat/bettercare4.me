/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

class ProviderTestSpec extends PlaySpec {

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
      persistenceLayer.createMedClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos) mustBe MedClaim("c-md-99-0", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-1", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-1", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createMedClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos) mustBe MedClaim("c-md-99-1", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-2", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-2", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createMedClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos) mustBe MedClaim("c-md-99-2", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-3", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-3", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createMedClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos) mustBe MedClaim("c-md-99-3", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos)
      persistenceLayer.createRxClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos) mustBe RxClaim("c-rx-99-4", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos)
      persistenceLayer.createLabClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos) mustBe LabClaim("c-lc-99-5", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos)

      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-4", "Michel", "Dufresne")
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-4", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createMedClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos) mustBe MedClaim("c-md-99-6", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos, dos)
      persistenceLayer.createProvider("Michel", "Dufresne") mustBe Provider("provider-99-5", "Michel", "Dufresne")
      persistenceLayer.createRxClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos) mustBe RxClaim("c-rx-99-7", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos)
      persistenceLayer.createLabClaim("patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos) mustBe LabClaim("c-lc-99-8", "patient.uuid", "patient.first", "patient.last", "provider.uuid", "provider.first", "provider.last", dos)
    }
  }
}
