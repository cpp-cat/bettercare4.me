/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.DateTime

case class Claim(
    val uuid: String, val patientUuid: String, val providerUuid: String, 
    val gender: String, val dob: DateTime
    )



