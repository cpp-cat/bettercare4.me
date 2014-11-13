package com.nickelsoftware.bettercare4me.controllers

import scala.concurrent._
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import com.nickelsoftware.bettercare4me.actors.ClaimGeneratorActor
import com.nickelsoftware.bettercare4me.actors.SimpleActor
import com.nickelsoftware.bettercare4me.actors.SimpleActor.SimpleRequest
import com.nickelsoftware.bettercare4me.actors.SimpleActor.SimpleResponse
import com.nickelsoftware.bettercare4me.actors.SimpleActor.SimpleSparkRequest
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import scala.io.Source
import java.io.FileNotFoundException
import java.io.IOException
import play.Logger
import play.api.data.Form
import play.api.data.Forms._

// Define the form data classes
case class GeneratorConfigData(configTxt: String)

object Application extends Controller {

  // GeneratorConfigData Form binded
  val generatorConfigForm: Form[GeneratorConfigData] = Form(
    mapping(
      "configTxt" -> text)(GeneratorConfigData.apply)(GeneratorConfigData.unapply))

  // Really basic, gives you the version of Play on the page
  def index(msg: String) = Action {
    //*
    com.nickelsoftware.bettercare4me.utils.Yaml.test
    
    Ok(com.nickelsoftware.bettercare4me.views.html.index(msg))
  }

  // Return the claim generator configuration file for claim generation job submission (claim simulator)
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
        Ok(com.nickelsoftware.bettercare4me.views.html.claimGeneratorConfig(generatorConfigForm.fill(GeneratorConfigData(configTxt))))
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
  implicit val timeout = Timeout(10 seconds)

  // Claim generator job submission
  def claimGeneratorSubmit = Action.async { implicit request =>

    generatorConfigForm.bindFromRequest.fold(
      formWithErrors => {
        future {
          BadRequest(com.nickelsoftware.bettercare4me.views.html.claimGeneratorConfig(formWithErrors))
        }
      },
      generatorConfigData => {
        //**** Kick off the job here
        val fresponse: Future[GenerateClaimsCompleted] = ask(claimGeneratorActor, GenerateClaimsRequest(generatorConfigData.configTxt)).mapTo[GenerateClaimsCompleted]
        fresponse map { 
          case GenerateClaimsCompleted(0) => Redirect(routes.Application.index("Claim Generation Job Returned OK"))
          case GenerateClaimsCompleted(e) => Redirect(routes.Application.index("Claim Generation Job Returned ERROR "+e))
        }
      })
  }

  // Return the claim generator configuration file for report generation job submission (hedis report computation)
  def reportGenerator(fname: String) = Action {
    Ok(com.nickelsoftware.bettercare4me.views.html.index("Report generation job submission, using: " + fname))
  }

  // Using Akka Actor to perform the action
  import SimpleActor._
  val simpleActor = Akka.system.actorOf(Props[SimpleActor], name = "simpleActor")

  def actor = Action.async {

    // Next line would be simpleActor ? SimpleRequest("Hello World") but this return Future[Any]
    val futureSimpleResponse: Future[SimpleResponse] = ask(simpleActor, SimpleRequest("Hello World")).mapTo[SimpleResponse]
    futureSimpleResponse map { simpleResponse => Ok(simpleResponse.data) }
  }

  // Using the same Akka Actor to perform the Spark action!
  def spark = Action.async {

    // Basically the same as above, only change the message sent to the actor
    val futureSimpleResponse = simpleActor ? SimpleSparkRequest("conf/application.conf")
    futureSimpleResponse.mapTo[SimpleResponse] map { simpleResponse => Ok(simpleResponse.data) }
  }
}
