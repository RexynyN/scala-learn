package apollo

import org.apache.log4j.{Logger, Level}
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

object JsonDatasetter {
    // Game fuckin' changer TBH
    // https://jay-reddy.medium.com/how-to-handle-nested-json-with-apache-spark-c3801195dcc8
    def flattenDf(df: DataFrame): DataFrame = {
        val fields = df.schema.fields
        val fieldNames = fields.map(x => x.name)
        for (i <- fields.indices) {
            val field = fields(i)
            val fieldtype = field.dataType
            val fieldName = field.name

            fieldtype match {
                case aType: ArrayType =>
                    val firstFieldName = fieldName
                    val fieldNamesExcludingArrayType = fieldNames.filter(_ != firstFieldName)
                    
                    val explodeFieldNames = fieldNamesExcludingArrayType ++ Array(s"explode_outer($firstFieldName) as $firstFieldName")
                    val explodedDf = df.selectExpr(explodeFieldNames: _*)
                    return flattenDf(explodedDf)

                case sType: StructType =>
                    val childFieldnames = sType.fieldNames.map(childname => fieldName + "." + childname)
                    val newfieldNames = fieldNames.filter(_ != fieldName) ++ childFieldnames
                    val renamedcols = newfieldNames.map(x =>
                        (col(x.toString()).as(
                            x.toString()
                                .replace(".", "_")
                                .replace("$", "_")
                                .replace("__", "_")
                                .replace(" ", "")
                                .replace("-", "")
                        ))
                )
                val explodedf = df.select(renamedcols: _*)
                return flattenDf(explodedf)
                case _ =>
            }
        }
        df
    }

    def main(args: Array[String]): Unit = {
        Logger.getLogger("org").setLevel(Level.ERROR)
        val spark = SparkSession.builder
            .appName("JsonDatasetter")
            .master("local[*]")
            .config("spark.driver.bindAddress", "127.0.0.1")
            .getOrCreate()

        spark.sparkContext.setLogLevel("FATAL")
        import spark.implicits._
        // Load json into spark dataframe
        val tracksDf = spark.read
            .option("multiLine", true)
            .option("mode", "PERMISSIVE")
            .option("inferSchema", true)
            .json("data/tracks/*.json")
        
        val artistDf = spark.read
            .option("multiLine", true)
            .option("mode", "PERMISSIVE")
            .option("inferSchema", true)
            .json("data/searchArtist/*.json")

        
        val artists = artistDf.select("id").distinct().collect().toList.map(_.getString(0))

        var shape = (tracksDf.count(), tracksDf.columns.length)
        println("Shape: " + shape)
        
        // 1. Find the Distinct Tracks
        // val types = df.select("type").distinct()   
        // types.show()

        // val featsIds = split(col("feats_ids"), ",")
        // val featsFilter: Column = array_intersect(featsIds, array(artists.map(lit): _*)).isNotNull 
        //               && size(array_intersect(featsIds, array(artists.map(lit): _*))) > 0

        val splitDf = tracksDf.withColumn("feats_ids", split($"feats_ids", ","))
        val distinctTracks = splitDf.filter(
            splitDf("artist_id").isin(artists: _*) || 
            (
                array_intersect(splitDf("feats_ids"), array(artists.map(lit): _*)).isNotNull && 
                size(array_intersect(splitDf("feats_ids"), array(artists.map(lit): _*))) > 0
            )
        )
     
         
        // 2. Filter Out Artists that are not in the list
        // distinctTracks.show(distinctTracks.count().toInt) // .show(100)
        distinctTracks.show(100)
        shape = (distinctTracks.count(), distinctTracks.columns.length)
        println("Shape: " + shape)
 
        distinctTracks.select("artist_name").distinct().show(30) 
        distinctTracks.select("feats").distinct().show(30)  
    }

    def toyingJson(): Unit = {
        Logger.getLogger("org").setLevel(Level.ERROR)
        val spark = SparkSession.builder
            .appName("JsonDatasetter")
            .master("local[*]")
            .getOrCreate()

        spark.sparkContext.setLogLevel("FATAL")

        import spark.implicits._
        // Load json into spark dataframe
        val df = spark.read
            .option("multiLine", true)
            .option("mode", "PERMISSIVE")
            .option("inferSchema", true)
            .json("data/tracks/*.json")

        // Find the array of items and remove the "artists" struct from the dataframe
        val itemsDf = df.select(explode_outer($"items").as("item"))
        itemsDf.show()

        val cleanDf = itemsDf.select($"item.*")
            .withColumn("artist", expr("transform(artists, x -> x.name)"))
            .drop("available_markets", "external_urls", "artists")

        // df.select($"items.artists").show()
        // Show the dataframe
        // df.show()


        // Flatten the dataframe
        val flatDf = flattenDf(cleanDf)

        flatDf.printSchema()
        println("\n\n")
        flatDf.show()
        println("\n\n")
        // println("Shape: ", (flatDf.count(), flatDf.columns.length))

        val dropCols = flatDf.columns.filter(_.contains("artists"))
        val finalDf = flatDf
            .drop(dropCols: _*).distinct()
            .drop("disc_number", "is_local", "preview_url", "type", "uri")
            .withColumnRenamed("id", "spotify_id")
            .withColumnRenamed("uri", "spotify_uri")
            .withColumnRenamed("name", "name")

        // println("Shape: ", (finalDf.count(), finalDf.columns.length))

        finalDf.printSchema()
        spark.close()
    }
}
