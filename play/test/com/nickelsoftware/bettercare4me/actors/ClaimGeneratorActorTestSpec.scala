/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors;

import java.io.File
import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import com.github.tototoshi.csv.CSVReader
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.utils.NickelException
import ClaimGeneratorActor.GenerateClaimsCompleted
import ClaimGeneratorActor.GenerateClaimsRequest
import ClaimGeneratorActor.ProcessGenereatedClaims
import ClaimGeneratorActor.ProcessGenereatedFilesCompleted
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.util.Timeout
import play.api.test.WithApplication
import play.api.test.FakeApplication

class ClaimGeneratorTestSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  import ClaimGeneratorActor._

  val claimGeneratorActor = system.actorOf(Props[ClaimGeneratorActor])

  def this() = this(ActorSystem("BetterCare4meSpec"))
  val pathName = "./data/temp-actor-test"

  override def afterAll {

    // post test
    val dir = new File(pathName)
    for (f <- dir.listFiles()) f.delete()
    dir.delete()

    TestKit.shutdownActorSystem(system)
  }

  "A ClaimGeneratorActor actor" must {

    "generate Patients, Providers and Claims based on configuration triggered by a message" in new WithApplication {

      val baseFname = "ClaimGenerator"
      val nbrGen = 2
      claimGeneratorActor ! GenerateClaimsRequest("""
        basePath: """ + pathName + """
        baseFname: """ + baseFname + """
        nbrGen: """ + nbrGen + """
        nbrPatients: 1
        nbrProviders: 1
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-01-01T00:00:00.00-05:00
        rulesConfig:
          - name: TEST
            eligibleRate: 100
            meetMeasureRate: 100
            exclusionRate: 0
          """)

      expectMsg(GenerateClaimsCompleted(ClaimGeneratorCounts(2, 2, 2), 0))

      (new File(pathName)).listFiles() should have length 7

      val fnameBase = pathName + "/" + baseFname

      for (igen <- 1 to nbrGen) {

        CSVReader.open(new File(fnameBase + "_patients_" + igen + ".csv")).all() should have length 1
        CSVReader.open(new File(fnameBase + "_providers_" + igen + ".csv")).all() should have length 1
        CSVReader.open(new File(fnameBase + "_claims_" + igen + ".csv")).all() should have length 1
      }
    }

    "generate ALL HEDIS measures simulation data and process the measures to return an overall score summary result" in new WithApplication {

      val baseFname = "ClaimGenerator"
      val nbrGen = 2
      val configTxt = """
        basePath: """ + pathName + """
        baseFname: """ + baseFname + """
        nbrGen: """ + nbrGen + """
        nbrPatients: 10
        nbrProviders: 1
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-12-31T00:00:00.00-05:00
        rulesConfig:
          - name: BCS-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 0
            meetMeasureRate: 100
        
          - name: CCS-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 10
            meetMeasureRate: 95
        
          - name: CHL-16-20-HEDIS-2014
            eligibleRate: 80
            exclusionRate: 15
            meetMeasureRate: 95
        
          - name: CHL-21-24-HEDIS-2014
            eligibleRate: 97
            exclusionRate: 18
            meetMeasureRate: 95
        
          - name: COL-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 30
            meetMeasureRate: 95
        
          - name: CIS-VZV-C-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: CIS-DTaP-C-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: CIS-HB-C-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: CIS-HiB-C-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: CIS-MMR-C-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: CIS-PC-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: CIS-IPV-C-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: W15-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 0
            meetMeasureRate: 100
        
          - name: W34-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 0
            meetMeasureRate: 95
        
          - name: AWC-HEDIS-2014
            eligibleRate: 100
            exclusionRate: 0
            meetMeasureRate: 95
        
          - name: CDC-HbA1c-Test-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 90
        
          - name: CDC-HbA1c-Test-LT7-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 40
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-HbA1c-Test-LT8-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 40
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-HbA1c-Test-GT9-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 20
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-EE-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 90
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-LDL-C-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 95
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-LDL-C-Value-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 95
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-MAN-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 85
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-BPC-Test-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 85
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-BPC-C1-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 40
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: CDC-BPC-C2-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 20
            meetMeasureRate: 40
            simulationParity: CDC-HbA1c-Test-HEDIS-2014
        
          - name: ASM-5-11-HEDIS-2014
            eligibleRate: 35
            exclusionRate: 10
            meetMeasureRate: 85
        
          - name: ASM-12-18-HEDIS-2014
            eligibleRate: 35
            exclusionRate: 10
            meetMeasureRate: 85
        
          - name: ASM-19-50-HEDIS-2014
            eligibleRate: 35
            exclusionRate: 10
            meetMeasureRate: 85
        
          - name: ASM-51-64-HEDIS-2014
            eligibleRate: 35
            exclusionRate: 10
            meetMeasureRate: 85
        
          - name: CMC-LDL-C-Test-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 0
            meetMeasureRate: 95
            simulationParity: CDC-LDL-C-HEDIS-2014
        
          - name: CMC-LDL-C-Test-Value-HEDIS-2014
            eligibleRate: 40
            exclusionRate: 0
            meetMeasureRate: 95
            simulationParity: CDC-LDL-C-HEDIS-2014 
        
          - name: MPM-ACE-ARB-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: MPM-Digoxin-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: MPM-Diuretics-ARB-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: MPM-AC-Carbamazepine-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: MPM-AC-Phenobarbital-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: MPM-AC-Phenytoin-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: MPM-AC-Valproic-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: CWP-HEDIS-2014
            eligibleRate: 35
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: URI-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 5
            meetMeasureRate: 95
        
          - name: AAB-HEDIS-2014
            eligibleRate: 35
            exclusionRate: 20
            meetMeasureRate: 90
        
          - name: LBP-HEDIS-2014
            eligibleRate: 30
            exclusionRate: 15
            meetMeasureRate: 90
          """
      implicit val timeout = Timeout(5 seconds) // needed for `?` below

      claimGeneratorActor ! GenerateClaimsRequest(configTxt)

      //expectMsg(GenerateClaimsCompleted(0))

      // get the list of rule names for printing out
      val config = ClaimGeneratorConfig.loadConfig(configTxt)
      val ruleNames = config.rulesConfig map { _.name } toList
      
      val future = (claimGeneratorActor ? ProcessGenereatedClaims(configTxt)).mapTo[ProcessGenereatedFilesCompleted]
      future onComplete {
        
        case Success(ProcessGenereatedFilesCompleted(scoreSummary)) => 
          info(scoreSummary.toString(ruleNames))
          scoreSummary.patientCount should be (config.nbrGen * config.nbrPatients)
          
        case Failure(NickelException(msg)) => fail(msg)
        case Failure(_) => fail("Unknown error")
      }
      
      // make sure it completes the future before shutting down the test case
      Await.ready(future, 5 seconds)
    }

  }
}
