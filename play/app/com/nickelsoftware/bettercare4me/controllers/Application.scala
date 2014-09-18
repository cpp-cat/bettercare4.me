package com.nickelsoftware.bettercare4me.controllers

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

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


object Application extends Controller {

  // Really basic, gives you the version of Play on the page
  def index = Action {
    Ok(com.nickelsoftware.bettercare4me.views.html.index("Your new application is ready."))
  }

  // Using Akka Actor to perform the action
  import SimpleActor._
  val simpleActor = Akka.system.actorOf(Props[SimpleActor], name = "simpleActor")
  implicit val timeout = Timeout(10 seconds)

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
