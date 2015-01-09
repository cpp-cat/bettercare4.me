/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models;

import java.io.File

import scala.collection.JavaConversions.asScalaBuffer
import scala.language.postfixOps
import scala.language.reflectiveCalls

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.github.tototoshi.csv.CSVReader

// -----------------------------------------------------------------------------------------------------------
// ModelsTestSpec Class: Testing models classes
// -----------------------------------------------------------------------------------------------------------

class GeneratorTestSpec extends PlaySpec {

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
      val gen = new PersonGenerator(LocalDate.parse("2015-01-01").toDateTimeAtStartOfDay(), new SimplePersistenceLayer(0))

      gen.seedToMonth(65) mustBe 60
      gen.seedToMonth(131) mustBe 120
      gen.seedToMonth(198) mustBe 180

      gen.seedToMonth(150) mustBe (120 + 17)
      gen.seedToMonth(1000) mustBe (90 * 12)
    }

    "Person given name must be consistent with gender and last name must be from last name pool" in {

      val f = fixture
      val gen = new PersonGenerator(LocalDate.parse("2015-01-01").toDateTimeAtStartOfDay(), new SimplePersistenceLayer(0))

      def testPatient(patient: Patient): Unit = {
        if (patient.gender == "M") f.maleNames.contains(patient.firstName) mustBe true
        else if (patient.gender == "F") f.femaleNames.contains(patient.firstName) mustBe true
        else fail("Invalid gender, got " + patient.gender)
        f.lastNames.contains(patient.lastName) mustBe true
        assert(patient.dob.compareTo(gen.hedisDate) <= 0)
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

  "The ClaimGeneratorConfig class" must {

    "load configuration from YAML text" in {

      val config = ClaimGeneratorConfig.loadConfig("""
          basePath: ./data/ClaimGenerator
          baseFname: HEDIS_sim
          nbrGen: 1
          nbrPatients: 100
          nbrProviders: 1
          hedisDateTxt: 2014-01-01T00:00:00.00-05:00
          """)

      config.basePath mustBe "./data/ClaimGenerator"
      config.baseFname mustBe "HEDIS_sim"
      config.nbrGen mustBe 1
      config.nbrPatients mustBe 100
      config.nbrProviders mustBe 1
      config.hedisDate mustBe new LocalDate(2014, 1, 1).toDateTimeAtStartOfDay()
      config.rulesConfig mustBe List()
    }

    "load configuration from YAML text including HEDISRule config" in {

      val config = ClaimGeneratorConfig.loadConfig("""
          basePath: ./data/ClaimGenerator
          baseFname: HEDIS_sim
          nbrGen: 1
          nbrPatients: 100
          nbrProviders: 1
          hedisDateTxt: 2014-01-01T00:00:00.00-05:00
          rulesConfig:
              - name: CDC1
                eligibleRate: 40
                meetMeasureRate: 92
                exclusionRate: 5
              - name: Rule2
                eligibleRate: 60
                meetMeasureRate: 85
                exclusionRate: 13
                otherParams:
                  key1: val1
                  key2: val2
          """)

      config.basePath mustBe "./data/ClaimGenerator"
      config.baseFname mustBe "HEDIS_sim"
      config.nbrGen mustBe 1
      config.nbrPatients mustBe 100
      config.nbrProviders mustBe 1
      config.hedisDate mustBe new LocalDate(2014, 1, 1).toDateTimeAtStartOfDay()

      config.rulesConfig.size mustBe 2

      for (ruleConfig <- config.rulesConfig) ruleConfig.name match {
        case "CDC1" =>
          ruleConfig.eligibleRate mustBe 40
          ruleConfig.meetMeasureRate mustBe 92
          ruleConfig.exclusionRate mustBe 5
          ruleConfig.otherParams.size mustBe 0

        case "Rule2" =>
          ruleConfig.eligibleRate mustBe 60
          ruleConfig.meetMeasureRate mustBe 85
          ruleConfig.exclusionRate mustBe 13
          ruleConfig.otherParams.size mustBe 2
          ruleConfig.otherParams.get("key1") mustBe Some("val1")
          ruleConfig.otherParams.get("key2") mustBe Some("val2")

        case _ => fail("Oops, unexpected rule name: " + ruleConfig.name)
      }
    }
  }
}
