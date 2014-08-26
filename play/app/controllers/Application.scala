package controllers

import play.api._
import play.api.mvc._

// for using Akka
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.Props
import scala.language.postfixOps

// our SimpeActor
import actors.SimpleActor


object Application extends Controller {

  // Really basic, gives you the version of Play on the page
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
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