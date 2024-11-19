package fr.umontpellier.ig5

import org.apache.spark.sql.{SaveMode, SparkSession, functions => F}

object Neo4jCVEGraph {

  def main(args: Array[String]): Unit = {
    // Replace with the actual connection URI and credentials
    val url = "neo4j://localhost:7687"
    val username = "neo4j"
    val password = "password"
    val dbname = "neo4j"

    val spark = SparkSession.builder
      .config("neo4j.url", url)
      .config("neo4j.authentication.basic.username", username)
      .config("neo4j.authentication.basic.password", password)
      .config("neo4j.database", dbname)
      .appName("Neo4j CVE Graph")
      .master("local[*]")
      .getOrCreate()

    // Load JSON data
    val data = spark.read.json("data/extractedcve-2024-min.json")

    // Filter out rows where Description or impactScore is null
    val filteredData = data.filter(F.col("Description").isNotNull && F.col("impactScore").isNotNull)

    // Write CVE nodes
    filteredData.select(F.col("ID").alias("cve_id"))
      .write
      .format("org.neo4j.spark.DataSource")
      .mode(SaveMode.Overwrite)
      .option("labels", "CVE")
      .option("node.keys", "cve_id")
      .save()

    // Write Description nodes
    filteredData.select(F.col("ID").alias("cve_id"), F.col("Description").alias("desc_text"))
      .write
      .format("org.neo4j.spark.DataSource")
      .mode(SaveMode.Overwrite)
      .option("labels", "Description")
      .option("node.keys", "desc_text")
      .save()

    // Write ImpactScore nodes
    filteredData.select(F.col("ID").alias("cve_id"), F.col("impactScore").alias("impact_value"))
      .write
      .format("org.neo4j.spark.DataSource")
      .mode(SaveMode.Overwrite)
      .option("labels", "ImpactScore")
      .option("node.keys", "impact_value")
      .save()

    // Create relationships to Description
    filteredData
      .select(
        F.col("ID").alias("source.cve_id"), // Source node property
        F.col("Description").alias("target.desc_text") // Target node property
      )
      .write
      .format("org.neo4j.spark.DataSource")
      .mode(SaveMode.Overwrite)
      .option("relationship", "HAS")
      .option("relationship.source.labels", "CVE") // Source node label
      .option("relationship.source.keys", "cve_id") // Source node key
      .option("relationship.target.labels", "Description") // Target node label
      .option("relationship.target.keys", "desc_text") // Target node key
      .save()


    // Create relationships to ImpactScore
    filteredData
      .select(
        F.col("ID").alias("source.cve_id"), // Source node property
        F.col("impactScore").alias("target.impact_value") // Target node property
      )
      .write
      .format("org.neo4j.spark.DataSource")
      .mode(SaveMode.Overwrite)
      .option("relationship", "HAS")
      .option("relationship.source.labels", "CVE") // Source node label
      .option("relationship.source.keys", "cve_id") // Source node key
      .option("relationship.target.labels", "ImpactScore") // Target node label
      .option("relationship.target.keys", "impact_value") // Target node key
      .save()

    println("Data successfully written to Neo4j")
  }
}
