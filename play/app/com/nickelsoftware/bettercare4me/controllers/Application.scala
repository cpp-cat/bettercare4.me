package com.nickelsoftware.bettercare4me.controllers

import java.io.FileNotFoundException
import java.io.IOException
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.future
import scala.io.Source
import scala.language.postfixOps
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import com.nickelsoftware.bettercare4me.actors.ClaimGeneratorActor
import com.nickelsoftware.bettercare4me.actors.ClaimGeneratorActor.GenerateClaimsCompleted
import com.nickelsoftware.bettercare4me.actors.ClaimGeneratorActor.GenerateClaimsRequest
import com.nickelsoftware.bettercare4me.actors.ClaimGeneratorActor.ProcessGenereatedClaims
import com.nickelsoftware.bettercare4me.actors.ClaimGeneratorActor.ProcessGenereatedFilesCompleted
import com.nickelsoftware.bettercare4me.actors.ClaimGeneratorCounts
import com.nickelsoftware.bettercare4me.actors.SimpleActor
import com.nickelsoftware.bettercare4me.actors.SimpleActor.SimpleRequest
import com.nickelsoftware.bettercare4me.actors.SimpleActor.SimpleResponse
import com.nickelsoftware.bettercare4me.actors.SimpleActor.SimpleSparkRequest
import com.nickelsoftware.bettercare4me.cassandra.Bettercare4me
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.views.html.claimGeneratorConfig
import com.nickelsoftware.bettercare4me.views.html.hedisReport
import com.nickelsoftware.bettercare4me.views.html.patientList
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import play.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import org.joda.time.LocalDate

// Define the form data classes
case class GeneratorConfigData(configTxt: String)

object Application extends Controller {

  // GeneratorConfigData Form binded
  val generatorConfigForm: Form[GeneratorConfigData] = Form(
    mapping(
      "configTxt" -> text)(GeneratorConfigData.apply)(GeneratorConfigData.unapply))

  // Really basic, gives you the index page
  // ---------------------------------------
  def index(msg: String) = Action {
    Ok(com.nickelsoftware.bettercare4me.views.html.index(msg))
  }

  // Return the claim generator configuration file for claim generation job submission (claim simulator)
  // ---------------------------------------------------------------------------------------------------
  def claimGenerator(fname: String) = Action {

    // Open file, making sure it exists
    val sourceOpt = try {
      Some(Source.fromFile(fname))
    } catch {
      case ex: FileNotFoundException => {
        Logger.error("Oops, file not found: " + fname)
        None
      }
    }

    // read yaml config file and return the view 
    sourceOpt match {
      case Some(source) => try {
        val configTxt = source.mkString
        Ok(claimGeneratorConfig(generatorConfigForm.fill(GeneratorConfigData(configTxt))))
      } catch {
        case ex: IOException => {
          Logger.error("Oops, IOException while reading file: " + fname)
          Ok(com.nickelsoftware.bettercare4me.views.html.index("Oops, IOException while reading file: " + fname))
        }
      } finally {
        source.close
      }

      case None => Ok(com.nickelsoftware.bettercare4me.views.html.index("Oops, file not found: " + fname))
    }
  }

  // Using Akka Actor to perform the action
  import ClaimGeneratorActor._
  val claimGeneratorActor = Akka.system.actorOf(Props[ClaimGeneratorActor], name = "claimGeneratorActor")
  implicit val timeout = Timeout(240 seconds)

  // Claim generator job submission
  // -------------------------------
  def claimGeneratorSubmit = Action.async { implicit request =>

    generatorConfigForm.bindFromRequest.fold(
      formWithErrors => {
        future {
          BadRequest(claimGeneratorConfig(formWithErrors))
        }
      },
      generatorConfigData => {
        //**** Kick off the job here
        //        val fresponse: Future[GenerateClaimsCompleted] = ask(claimGeneratorActor, GenerateClaimsRequest(generatorConfigData.configTxt)).mapTo[GenerateClaimsCompleted]
        val fresponse: Future[GenerateClaimsCompleted] = (claimGeneratorActor ? GenerateClaimsRequest(generatorConfigData.configTxt)).mapTo[GenerateClaimsCompleted]
        fresponse map {
          case GenerateClaimsCompleted(ClaimGeneratorCounts(pa, pr, c), 0) => Redirect(routes.Application.index(s"Claim Generation Job Returned OK, generated $pa patients, $pr providers, and $c claims"))
          case GenerateClaimsCompleted(_, e) => Redirect(routes.Application.index("Claim Generation Job Returned ERROR " + e))
        }
      })
  }

  // Action to ask the user to provide a claim generator configuration file that was used
  // to generate claims. This configuration file will be used for hedis report generation
  // from the simulation data
  // -------------------------------------------------------------------------------------
  def reportGenerator = Action {
    Ok(com.nickelsoftware.bettercare4me.views.html.reportGenerator("bogus message"))
  }

  // Generate the report using the uploaded configuration file, which defines claim simulator data
  // --------------------------------------------------------------------------------------------
  def reportGeneratorSubmit = Action.async(parse.multipartFormData) { implicit request =>

    val result = request.body.file("configTxt").map { file =>

      val filename = file.filename
      val contentType = file.contentType

      contentType match {

        case Some("application/x-yaml") =>
          val configTxt = Source.fromFile(file.ref.file).mkString
          (Some(configTxt), None)

        case _ =>
          (None, Some("Error, File with invalid content type, must be YAML configuration file"))
      }
    }

    result match {

      case Some((Some(configTxt), _)) =>
        val fresponse: Future[ProcessGenereatedFilesCompleted] = (claimGeneratorActor ? ProcessGenereatedClaims(configTxt)).mapTo[ProcessGenereatedFilesCompleted]
        fresponse map { processGenereatedFilesCompleted =>
          val config = ClaimGeneratorConfig.loadConfig(configTxt)
          //*** use report view with config and ss
          Ok(com.nickelsoftware.bettercare4me.views.html.hedisReport(config, processGenereatedFilesCompleted.ss))
        }

      case Some((None, Some(err))) =>
        future {
          Redirect(routes.Application.index(err))
        }

      case None =>
        future {
          Redirect(routes.Application.index("Error, Missing File"))
        }

      case Some(_) =>
        future {
          Redirect(routes.Application.index("humm, unknown error!"))
        }
    }
  }

  // -------------------------------------------------------------------------------------------
  // Action loading data from Cassandra
  // ===========================================================================================
  //
  // List the available dashboards from hedis_summary table
  // ------------------------------------------------------------
  def hedisDashboard = Action.async {

    val futureConfigList = Bettercare4me.queryHEDISSummary map { x =>
      x map { case (_, c) => ClaimGeneratorConfig.loadConfig(c) } toList
    }
    futureConfigList map { cl =>
      Ok(com.nickelsoftware.bettercare4me.views.html.hedisDashboardView(cl))
    }
  }

  // Return an hedis report from hedis_summary table
  // ------------------------------------------------------------
  def hedisReport(name: String, date: String) = Action.async {

    val hedisDate = LocalDate.parse(date).toDateTimeAtStartOfDay()
    Bettercare4me.queryHEDISReport(name, hedisDate) map {
      case (hedisScoreSummary, configTxt) =>
        val config = ClaimGeneratorConfig.loadConfig(configTxt)
        Ok(com.nickelsoftware.bettercare4me.views.html.hedisReport(config, hedisScoreSummary))
    }
  }

  // Return the list of patient for a given hedis measure
  // ------------------------------------------------------------
  def ruleScorecard(ruleName: String, date: String) = Action.async {

    val hedisDate = LocalDate.parse(date).toDateTimeAtStartOfDay()
    Bettercare4me.queryRuleScorecard(ruleName, hedisDate) map { tuples =>
      Ok(com.nickelsoftware.bettercare4me.views.html.ruleScorecard(ruleName, date, tuples.toList))
    }
  }

  // ---------------- SIMPLE TEST STUFF ----------------------------------------------------
  // Using Akka Actor to perform the action
  import SimpleActor._
  val simpleActor = Akka.system.actorOf(Props[SimpleActor], name = "simpleActor")

  def actor = Action.async {

    // ask the actor. . .
    val futureSimpleResponse = (simpleActor ? SimpleRequest("Hello World")).mapTo[SimpleResponse]
    futureSimpleResponse map { simpleResponse => Ok(simpleResponse.data) }
  }

  // Using the same Akka Actor to perform the Spark action!
  def spark = Action.async {

    // Basically the same as above, only change the message sent to the actor
    val futureSimpleResponse = simpleActor ? SimpleSparkRequest("conf/application.conf")
    futureSimpleResponse.mapTo[SimpleResponse] map { simpleResponse => Ok(simpleResponse.data) }
  }

  // --Simple Cassandra test ----------------------------------------------------------------------
  // Assumes we ran the ./data/bettercare4me.cql already and the database is running on localhost
  def cassandra = Action {

    //    val patients = future { new SimpleClient("127.0.0.1").queryPatients }
    //    patients map { p => Ok(patientList(p)) }

    // Using Cassandra directly, specifying batch 1
    //    Bettercare4me.queryPatients(1) map { patients => Ok(patientList(patients)) }

    // Using spark on cassandra for all 3 batches
    import org.apache.spark.SparkConf
    import org.apache.spark.SparkContext
    val conf = new SparkConf()
      .setMaster("local[3]")
      .setAppName("Spark on Cassandra Test")
    val sc = new SparkContext(conf)

    // create the nbrGen jobs to run, ensuring the rdd is sliced with one job per slice, ie nbrGen slices
    val rdd = sc.parallelize(1 to 3, 3) map { igen =>
      val f = Bettercare4me.queryPatients(igen)
      Await.result(f, Duration.Inf)
    }

    // combine the result of each job to get the total count of patients, providers and claims
    Ok(patientList(rdd.collect().flatten.toList))
  }
}
