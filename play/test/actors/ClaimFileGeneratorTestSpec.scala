/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package actors;

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
import scala.collection.JavaConversions._
import models.ClaimGeneratorConfig
import com.github.tototoshi.csv.CSVWriter
import models.PatientParser
import models.ProviderParser
import models.ClaimParser

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
      val claim = ClaimParser.fromList(claims(0))

      claim.patientID mustBe patient.patientID
      claim.providerID mustBe provider.providerID
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
