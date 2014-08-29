/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.DateTime

case class Patient(val uuid: String, val firstName: String, val lastName: String, val gender: String, val dob: DateTime)

case class PatientHistory(val cPT: Map[String, DateTime])

