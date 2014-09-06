/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models;

import play.api.Play.current
import org.scalatest._
import org.scalatestplus.play._
import org.joda.time.LocalDate
import play.api.Logger

class PatientTestSpec extends PlaySpec with OneAppPerSuite {

  "The Patient class" must {

    "be created with valid arguments" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27"))

      patient.uuid mustBe "key1"
      patient.firstName mustBe "Michel"
      patient.lastName mustBe "Dufresne"
      patient.gender mustBe "M"
      patient.dob mustBe new LocalDate(1962, 7, 27)
    }

    "compute age correctly" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27"))

      patient.age(new LocalDate(2014, 7, 27)) mustBe 52
      patient.age(new LocalDate(2014, 9, 30)) mustBe 52
      patient.age(new LocalDate(2014, 6, 1)) mustBe 51
    }

    "put all atributes into a List" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27"))

      patient.toList mustBe List("key1", "Michel", "Dufresne", "M", "1962-07-27")
    }
  }
  
  "The SimplePersistenceLayer class" must {
    
    "create Patient with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)
      
      persistenceLayer.createPatient("Michel", "Dufresne", "M", new LocalDate(1962, 7, 27)) mustBe Patient("patient-99-0", "Michel", "Dufresne", "M", new LocalDate(1962, 7, 27))
      persistenceLayer.createPatient("Michel", "Dufresne", "M", new LocalDate(1962, 7, 27)) mustBe Patient("patient-99-1", "Michel", "Dufresne", "M", new LocalDate(1962, 7, 27))
      persistenceLayer.createPatient("Michel", "Dufresne", "M", new LocalDate(1962, 7, 27)) mustBe Patient("patient-99-2", "Michel", "Dufresne", "M", new LocalDate(1962, 7, 27))
      persistenceLayer.createPatient("Michel", "Dufresne", "M", new LocalDate(1962, 7, 27)) mustBe Patient("patient-99-3", "Michel", "Dufresne", "M", new LocalDate(1962, 7, 27))
      persistenceLayer.createPatient("Michel", "Dufresne", "M", new LocalDate(1962, 7, 27)) mustBe Patient("patient-99-4", "Michel", "Dufresne", "M", new LocalDate(1962, 7, 27))
    }
  }
}