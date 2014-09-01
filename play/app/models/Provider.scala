/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

case class Provider(val uuid: String, val firstName: String, val lastName: String) {
  def toList: List[String] = List(uuid, firstName, lastName)
}



