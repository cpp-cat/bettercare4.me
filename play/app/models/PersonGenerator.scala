/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.DateTime
import com.github.tototoshi.csv.CSVReader
import java.io.File
import scala.util.Random
import org.joda.time.LocalDate

class PersonGenerator(maleNameFile: String, femaleNameFile: String, lastNameFile: String, val hedisDate: LocalDate) {

  val ageDistribution = List(0, 65, 131, 198, 269, 339, 407, 472, 537, 605, 679, 751, 815, 869, 909, 939, 963, 982, 1000)
  val maleNames: List[String] = CSVReader.open(new File(maleNameFile)).all().flatten
  val femaleNames: List[String] = CSVReader.open(new File(femaleNameFile)).all().flatten
  val lastNames: List[String] = CSVReader.open(new File(lastNameFile)).all().flatten

  def getOne[A](items: List[A]): A = items(Random.nextInt(items.size))

  /**
   * Return age in month by converting seed (seed>0 and seed<=1000) into nbr of months
   * based on ageDistribution
   */
  def seedToMonth(seed: Int): Int = {

    // Use a simple tail recursion function to find where r is in ageDistribution (find the age group)
    def findIt(i: Int): Int = {
      if (seed > ageDistribution(i) && seed <= ageDistribution(i + 1))
        i
      else
        findIt(i + 1)
    }

    val ageGroupIdx = findIt(0)
    val f = 60.0 * (seed - ageDistribution(ageGroupIdx)) / (ageDistribution(ageGroupIdx + 1) - ageDistribution(ageGroupIdx))
    60 * ageGroupIdx + f.toInt
  }

  def monthToDOB(nbrMo: Int): LocalDate = hedisDate.minusMonths(nbrMo)

  def generatePatient(): Patient = {

    val r = Random.nextInt(1000) + 1
    val nbrMo = seedToMonth(r)

    val dob = monthToDOB(nbrMo)
    val gender = if (Random.nextInt(2) > 0) "F" else "M"

    val firstName = if (gender == "F") getOne(femaleNames) else getOne(maleNames)

    Patient("key", firstName, getOne(lastNames), gender, dob)
  }

  def generateProvider(): Provider = {

    val firstName = if (Random.nextInt(2) > 0) getOne(femaleNames) else getOne(maleNames)
    
    Provider("key", firstName, getOne(lastNames))
  }
}