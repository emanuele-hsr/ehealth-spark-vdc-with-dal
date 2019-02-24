package com.ditas


import com.ditas.configuration.ServerConfiguration
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

import scala.collection.mutable.Stack

object EnforcementEngineResponseProcessor {
  private val LOGGER = LoggerFactory.getLogger("ProcessEnforcementEngineResponse")
  var response : String = ""
  var query: String = ""
  var tableNames: Stack[String] = new Stack[String]()
  var queryOnTables: String = ""
  var debugMode: Boolean = false

  def processResponse (spark: SparkSession, config: ServerConfiguration, response: String, debugMode: Boolean,
                       showDataFrameLength: Int): DataFrame = {
    this.debugMode = debugMode
    val json: JsValue = Json.parse(response)
    val table: String = new String("table")
    var index: Integer = 0;
    var cond = true;
    var tableKey: String = null
    val tables = (json \ "tables").as[List[JsValue]]
    for (table <- tables) {
      val tableName = (table \ "name").as[String]
      DataFrameUtils.addTableToSpark(spark, config, tableName, showDataFrameLength, debugMode)
    }
    val newQuery = (json \ "rewrittenQuery").validate[String]
    query = newQuery.get
    if (debugMode) {
      println("the re-written query: " + newQuery.get)
    }
    val bloodTestsDF: DataFrame = spark.sql(query).toDF().filter(row => DataFrameUtils.anyNotNull(row))
    if (debugMode) {
      println (query)
      bloodTestsDF.distinct().show(showDataFrameLength, false)
      bloodTestsDF.printSchema
      bloodTestsDF.explain(true)
    }
    bloodTestsDF
  }



}
