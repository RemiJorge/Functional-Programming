package fr.umontpellier.ig5
import scala.io.Source

object PeopleExo {

  case class User(id: Int, name: String, age: Int, city: String)

  def main(args: Array[String]): Unit = {
    // Load the data from CSV file
    val source = Source.fromFile("data/people.csv")

    // Skip the header row
    val lines = source.getLines().drop(1).toList

    // Map the remaining lines to users
    val users = lines.flatMap { line =>
      // Split the line into fields and make sure it's well formatted
      val fields = line.split(",").map(_.trim)
      if (fields.length == 4) {
        try {
          val id = fields(0).toInt
          val name = fields(1)
          val age = fields(2).toInt
          val city = fields(3)
          Some(User(id, name, age, city)) // Return a valid User object
        } catch {
          case e: NumberFormatException =>
            println(s"Skipping invalid line: $line")
            None // Return None for invalid data
        }
      } else {
        println(s"Skipping invalid line: $line")
        None // Return None for lines with incorrect number of fields
      }
    }
    source.close()

    // Filter users aged 25 and above
    val filteredUsers = users.filter(_.age >= 25)

    // Transform the data to extract names and cities
    val transformedData = filteredUsers.map(user => (user.name, user.city))

    // Group users by city
    val groupedByCity = transformedData.groupBy(_._2)  // Group by the city (second element of the tuple)

    // Print the grouped data by city
    groupedByCity.foreach { case (city, usersInCity) =>
      println(s"City: $city")
      usersInCity.foreach { case (name, _) =>
        println(s"  Name: $name")
      }
    }
  }
}
