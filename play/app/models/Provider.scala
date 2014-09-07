/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

object ProviderParser {
  
  def fromList(l: List[String]): Provider = Provider(l(0), l(1), l(2))
}

case class Provider(uuid: String, firstName: String, lastName: String) {
  def toList: List[String] = List(uuid, firstName, lastName)
}



