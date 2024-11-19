package fr.umontpellier.ig5

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, explode}
import com.mongodb.{ServerApi, ServerApiVersion}
import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings}
import org.mongodb.scala.bson.Document

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Using
import org.mongodb.scala._

import scala.io.Source
import scala.util.{Try, Using}

object CVEJsonMongoDB {
    def main(args: Array[String]): Unit = {
        val connectionString = "mongodb+srv://user0:@cluster0.nrkl0.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
        // Construct a ServerApi instance using the ServerApi.builder() method
        val serverApi = ServerApi.builder.version(ServerApiVersion.V1).build()
        val settings = MongoClientSettings
          .builder()
          .applyConnectionString(ConnectionString(connectionString))
          .serverApi(serverApi)
          .build()
        // Create a new client and connect to the server
        Using(MongoClient(settings)) { mongoClient =>
            // Send a ping to confirm a successful connection
            val database = mongoClient.getDatabase("cve-database")
            val ping = database.runCommand(Document("ping" -> 1)).head()
            Await.result(ping, 10.seconds)
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!")
        }

        Using(MongoClient(settings)) { mongoClient =>
            val database = mongoClient.getDatabase("cve-database")
            val collectionName = "cve-files" // Name of the MongoDB collection

            // Ping the database to ensure the connection is successful
            val ping = database.runCommand(Document("ping" -> 1)).head()
            Await.result(ping, 10.seconds)
            println("Pinged your deployment. Successfully connected to MongoDB!")

            val dataFolder = "data" // Path to the folder with JSON files
            val files = new java.io.File(dataFolder).listFiles.filter(_.getName.endsWith(".json"))

            files.foreach { file =>
                val year = file.getName.stripSuffix(".json") // Extract year from the filename
                println(s"Processing file: ${file.getName}")

                // Read and parse the JSON file
                val fileContent = Using(Source.fromFile(file))(_.mkString).getOrElse("")
                if (fileContent.nonEmpty) {
                    val collection = database.getCollection(collectionName)
                    val document = Document("year" -> year, "data" -> Document(fileContent))

                    // Insert the document into the MongoDB collection
                    val result = collection.insertOne(document).toFuture()
                    Await.result(result, 10.seconds)
                    println(s"Inserted document for year $year successfully!")
                } else {
                    println(s"Failed to read data from ${file.getName}")
                }
            }
        } match {
            case scala.util.Success(_) => println("All files processed successfully.")
            case scala.util.Failure(exception) => println(s"Error: ${exception.getMessage}")
        }
    }
}
