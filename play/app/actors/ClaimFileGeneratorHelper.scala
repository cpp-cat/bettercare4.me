/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package actors

import java.io.File
import com.github.tototoshi.csv.CSVWriter


object ClaimFileGeneratorHelper {
  
  def init(fname: String): ClaimFileGeneratorHelper = {
    
    val patients_writer = CSVWriter.open(new File(fname + "_patients.csv"))
    val providers_writer = CSVWriter.open(new File(fname + "_providers.csv"))
    val claims_writer = CSVWriter.open(new File(fname + "_claims.csv"))
    
    new ClaimFileGeneratorHelper(patients_writer, providers_writer, claims_writer)
  }
}


class ClaimFileGeneratorHelper(patients_writer: CSVWriter, providers_writer: CSVWriter, claims_writer: CSVWriter) {
  
  /**
   * Generate claims for a generation of 1000 patients.
   * 
   * @param ngen Generation number
   */
  def generateClaims(ngen: Int): Unit = {
    
  }
}