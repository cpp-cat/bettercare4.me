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
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay())

      patient.patientID mustBe "key1"
      patient.firstName mustBe "Michel"
      patient.lastName mustBe "Dufresne"
      patient.gender mustBe "M"
      patient.dob mustBe new LocalDate(1962, 7, 27).toDateTimeAtStartOfDay()
    }

    "compute age in years correctly" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay())

      patient.age(new LocalDate(2014, 7, 27).toDateTimeAtStartOfDay()) mustBe 52
      patient.age(new LocalDate(2014, 9, 30).toDateTimeAtStartOfDay()) mustBe 52
      patient.age(new LocalDate(2014, 6, 1).toDateTimeAtStartOfDay()) mustBe 51
    }

    "compute age in months correctly" in {
      val patient = Patient("key1", "Sophie", "Dufresne", "F", LocalDate.parse("2000-07-27").toDateTimeAtStartOfDay())

      patient.ageInMonths(new LocalDate(2000, 9, 27).toDateTimeAtStartOfDay()) mustBe 2
      patient.ageInMonths(new LocalDate(2001, 7, 26).toDateTimeAtStartOfDay()) mustBe 11
      patient.ageInMonths(new LocalDate(2001, 7, 27).toDateTimeAtStartOfDay()) mustBe 12
      patient.ageInMonths(new LocalDate(2001, 8, 28).toDateTimeAtStartOfDay()) mustBe 13
    }

    "put all atributes into a List" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay())

      patient.toList mustBe List("key1", "Michel", "Dufresne", "M", "1962-07-27")
    }

    "create a Patient from a list of attributes" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay())

      PatientParser.fromList(patient.toList) mustBe patient
    }
  }

  "The PatientHistoryFactory class" must {

    "create PatientHistory from empty list of claims" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)
      PatientHistoryFactory.createPatientHistory(patient, List()) mustBe PatientHistory(Map(), Map(), Map(), Map(), Map())
    }

    "create PatientHistory from a single claim with no codes" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)
      PatientHistoryFactory.createPatientHistory(patient, List(Claim("c1", "p1", "p2", date, date))) mustBe PatientHistory(Map(), Map(), Map(), Map(), Map())
    }

    "create PatientHistory from a single claim with codes" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)

      val cl = List(Claim("c1", "p1", "p2", date, date,
        icdDPri = "icd 1", icdD = Set("icd 2", "icd 3"), icdP = Set("icd p1"),
        ubRevenue = "ubRevenue", cpt = "cpt", hcpcs = "hcpcs"))

      PatientHistoryFactory.createPatientHistory(patient, cl) mustBe
        PatientHistory(Map(("icd 1" -> cl), ("icd 2" -> cl), ("icd 3" -> cl)), Map(("icd p1" -> cl)),
          Map(("ubRevenue" -> cl)), Map(("cpt" -> cl)), Map(("hcpcs" -> cl)))
    }

    "create PatientHistory from multiple claims" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)

      val c1 = Claim("c1", "p1", "p2", date, date, icdDPri = "icd 1", icdD = Set("icd 2", "icd 3"), icdP = Set("icd p1"), ubRevenue = "ubRevenue", cpt = "cpt1", hcpcs = "hcpcs1")
      val c2 = Claim("c2", "p1", "p2", date, date, icdDPri = "icd 1", icdD = Set("icd 2", "icd 3"), icdP = Set("icd p1"), ubRevenue = "ubRevenue", cpt = "cpt2", hcpcs = "hcpcs")
      val c3 = Claim("c3", "p1", "p2", date, date, icdDPri = "icd 4", icdD = Set(),                 icdP = Set("icd p1"), ubRevenue = "ubRevenue", cpt = "cpt3", hcpcs = "hcpcs")

      val cl1 = List(c1)
      val cl2 = List(c2)
      val cl3 = List(c3)
      val cl12 = List(c2, c1)
      val cl23 = List(c3, c2)
      val cl123 = List(c3, c2, c1)

      val ph = PatientHistoryFactory.createPatientHistory(patient, List(c1, c2, c3))
      ph.icdD      mustBe Map(("icd 1" -> cl12), ("icd 2" -> cl12), ("icd 3" -> cl12), ("icd 4" -> cl3))
      ph.claims4ICDD("icd 1") mustBe cl12
      ph.claims4ICDD("xxxx") mustBe List[Claim]()
      
      ph.icdP      mustBe Map(("icd p1" -> cl123))
      ph.claims4ICDP("icd p1") mustBe cl123
      ph.claims4ICDP("xxxx") mustBe List[Claim]()
      
      ph.ubRevenue mustBe Map(("ubRevenue" -> cl123))
      ph.claims4UBRev("ubRevenue") mustBe cl123
      ph.claims4UBRev("xxxx") mustBe List[Claim]()
      
      ph.cpt       mustBe Map(("cpt1" -> cl1), ("cpt2" -> cl2), ("cpt3" -> cl3))
      ph.claims4CPT("cpt1") mustBe cl1
      ph.claims4CPT("xxxx") mustBe List[Claim]()
      
      ph.hcpcs     mustBe Map(("hcpcs1" -> cl1), ("hcpcs" -> cl23))
      ph.claims4HCPCS("hcpcs") mustBe cl23
      ph.claims4HCPCS("xxxx") mustBe List[Claim]()
      
    }
  }

  "The SimplePersistenceLayer class" must {

    "create Patient with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)

      val dob = new LocalDate(1962, 7, 27).toDateTimeAtStartOfDay()
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-0", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-1", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-2", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-3", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-4", "Michel", "Dufresne", "M", dob)
    }
  }
}