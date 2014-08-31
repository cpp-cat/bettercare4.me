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
import scala.language.postfixOps
import scala.language.reflectiveCalls
import com.github.tototoshi.csv.CSVReader
import org.scalactic.Or
import org.scalactic.One
import utils.NickelException

// -----------------------------------------------------------------------------------------------------------
// ModelsTestSpec Class: Testing models classes
// -----------------------------------------------------------------------------------------------------------

class ModelsTestSpec extends PlaySpec with OneAppPerSuite {

  def fixture =
    new {
      val maleNameFile = "./data/male-names.csv"
      val femaleNameFile = "./data/female-names.csv"
      val lastNameFile = "./data/last-names.csv"
        
      val maleNames: Set[String] = CSVReader.open(new File(maleNameFile)).all().flatten.toSet
      val femaleNames: Set[String] = CSVReader.open(new File(femaleNameFile)).all().flatten.toSet
      val lastNames: Set[String] = CSVReader.open(new File(lastNameFile)).all().flatten.toSet
    }

  "The PersonGenerator class" must {

    "map a seed (range 0 to 1000) to month based on US population age distribution" in {
      val f = fixture
      val gen = new PersonGenerator(f.maleNameFile, f.femaleNameFile, f.lastNameFile, LocalDate.parse("2015-01-01"))
      
      gen.seedToMonth(65) mustBe 60
      gen.seedToMonth(131) mustBe 120
      gen.seedToMonth(198) mustBe 180
      
      gen.seedToMonth(150) mustBe (120 + 17)
      gen.seedToMonth(1000) mustBe (90*12)
    }
    
    "Person given name must be consistent with gender and last name must be from last name pool" in {
      
      val f = fixture
      val gen = new PersonGenerator(f.maleNameFile, f.femaleNameFile, f.lastNameFile, LocalDate.parse("2015-01-01"))
      
      def testPatient(patient: Patient): Unit = {
	      if (patient.gender == "M") f.maleNames.contains(patient.firstName) mustBe true
	      else if(patient.gender == "F") f.femaleNames.contains(patient.firstName) mustBe true
	      else fail("Invalid gender, got " + patient.gender)
	      f.lastNames.contains(patient.lastName) mustBe true
	      assert(patient.dob.compareTo(gen.hedisDate) < 0)
	      assert(gen.hedisDate.minusMonths(1081).compareTo(patient.dob) < 0)
      }
      
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
      testPatient(gen.generatePatient)
    }

  }
}
