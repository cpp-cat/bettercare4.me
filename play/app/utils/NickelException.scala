/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */

package utils

/**
 * Root level exception of all application level exception
 */
case class NickelException(message: String) extends Exception(message)
