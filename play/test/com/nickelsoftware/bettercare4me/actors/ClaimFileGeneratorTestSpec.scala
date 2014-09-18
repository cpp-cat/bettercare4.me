/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors;

import java.io.File

import scala.language.postfixOps
import scala.language.reflectiveCalls

import org.joda.time.LocalDate
import org.scalatest.Suite
import org.scalatest.SuiteMixin
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.github.tototoshi.csv.CSVReader
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.models.ClaimParser
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.PatientParser
import com.nickelsoftware.bettercare4me.models.ProviderParser

trait TestFileGenerator extends SuiteMixin { this: Suite =>

  val basePath = "./data/temp-test-"
  val pathName = basePath + LocalDate.now().toString()

  abstract override def withFixture(test: NoArgTest) = {

    // pre test
    val dir = new File(pathName)
    dir.mkdir()

    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally {

      // post test
      for (f <- dir.listFiles()) f.delete()
      dir.delete()
    }
  }
}

class ClaimFileGeneratorTestSpec extends PlaySpec with OneAppPerSuite with TestFileGenerator {

  "The ClaimFileGeneratorHelper class" must {

    "generate Patients, Providers and Claims based on configuration, single entity" in {

      val config = ClaimGeneratorConfig.loadConfig("""
        basePath: """ + basePath + """
        baseFname: ClaimGenerator
        nbrGen: 1
        nbrPatients: 1
        nbrProviders: 1
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-01-01
        rulesConfig:
          - name: TEST
            eligibleRate: 100
            meetMeasureRate: 100
            exclusionRate: 0
        """)

      ClaimFileGeneratorHelper.generateClaims(0, config)

      // validate the created files
      val fnameBase = pathName + "/" + config.baseFname
      val patients = CSVReader.open(new File(fnameBase + "_patients_0.csv")).all()
      val providers = CSVReader.open(new File(fnameBase + "_providers_0.csv")).all()
      val claims = CSVReader.open(new File(fnameBase + "_claims_0.csv")).all()

      patients.size mustBe 1
      providers.size mustBe 1
      claims.size mustBe 1

      val patient = PatientParser.fromList(patients(0))
      patient.age(config.hedisDate) must be >= 0
      patient.age(config.hedisDate) must be < 100

      val provider = ProviderParser.fromList(providers(0))
      ClaimParser.fromList(claims(0)) match {
        case claim: MedClaim =>
          claim.patientID mustBe patient.patientID
          claim.providerID mustBe provider.providerID
        case _ => fail("Invalid claim class type!")
      }
    }

    "generate Patients, Providers and Claims based on configuration, multiple entities" in {

      val config = ClaimGeneratorConfig.loadConfig("""
        basePath: """ + basePath + """
        baseFname: ClaimGenerator
        nbrGen: 1
        nbrPatients: 5
        nbrProviders: 3
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-01-01
        rulesConfig:
          - name: TEST
            eligibleRate: 100
            meetMeasureRate: 100
            exclusionRate: 0
        """)

      ClaimFileGeneratorHelper.generateClaims(0, config)

      // validate the created files
      val fnameBase = pathName + "/" + config.baseFname
      val patients = CSVReader.open(new File(fnameBase + "_patients_0.csv")).all()
      val providers = CSVReader.open(new File(fnameBase + "_providers_0.csv")).all()
      val claims = CSVReader.open(new File(fnameBase + "_claims_0.csv")).all()

      patients.size mustBe 5
      providers.size mustBe 3
      claims.size mustBe 5
    }
  }
}
