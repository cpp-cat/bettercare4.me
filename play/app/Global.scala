/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */

import play.api._
import com.nickelsoftware.bettercare4me.cassandra.Bettercare4me

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    Bettercare4me.connect()
  }  
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    Bettercare4me.close
  } 
}