/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

case class Provider(uuid: String, firstName: String, lastName: String) {
  def toList: List[String] = List(uuid, firstName, lastName)
}



