package apollo

import org.apache.log4j.{Logger, Level}
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import upickle.default._




case class Artist(
    name: String,
    id: String,
    followers: String,
    genres: List[String],
)


case class Track(
    artists: String,
    name: String,
    duration_ms: String,
    id: String,
    item_number: String
)


object JsonDatasetter {
    implicit val artistRW: ReadWriter[Artist] = macroRW[Artist]

    implicit val trackRW: ReadWriter[Artist] = macroRW[Artist]
    implicit val albumRW: ReadWriter[Artist] = macroRW[Artist]


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

    def artistJson(path: String): Unit = {

    }

    def albumsJson(path: String): Unit = {
        val json = ujson.read(os.read(os.pwd / path))

        val items = json("items").arr


        ujson.Value(items.map(item => {
            val artists = item("artists").arr.map(artist => artist("name").str).mkString(", ")
            val name = item("name").str
            val release_date = item("release_date").str
            val total_tracks = item("total_tracks").num
            val id = item("id").str
            val type_ = item("type").str

            Map(
                "artists" -> artists,
                "nase" -> name,
                "release_date" -> release_date,
                "total_tracks" -> total_tracks,
                "id" -> id,
                "type" -> type_
            )
        }).toList)


    }





    def trackJson(path: String): Unit = {
        val json = ujson.read(os.read(os.pwd / path))

        val items = json("items").arr

        val data: List[Map[String, String]] = items.map(item => {
            val artists = item("artists").arr.map(artist => artist("name").str).mkString(", ")
            val name = item("name").str
            val duration_ms = item("duration_ms").str
            val id = item("id").str
            val item_number = item("item_number").str
            val type_ = item("type").str

            Map(
                "artists" -> artists,
                "name" -> name,
                "duration_ms" -> duration_ms,
                "id" -> id,
                "item_number" -> item_number,
                "type" -> type_
            )
        }).toList

        os.write(os.pwd / "raw-updated.json", upickle.default.write(data))
    }

    def main(args: Array[String]): Unit = {
        Logger.getLogger("org").setLevel(Level.ERROR)
        println("Hello, world!")

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

        // df.printSchema()

        // Find the array of items and remove the "artists" struct from the dataframe
        val itemsDf = df.select(explode_outer($"items").as("item"))
        itemsDf.show()

        val cleanDf = itemsDf.select($"item.*")
            .withColumn("artist", expr("transform(artists, x -> x.name)"))
            .drop("available_markets", "external_urls", "artists")


        // df.select($"items.artists").show()

        // Show the dataframe
        // df.show()


        // // Flatten the dataframe
        val flatDf = flattenDf(cleanDf)


        flatDf.printSchema()
        println("\n\n")
        flatDf.show()
        println("\n\n")


        println("Shape: ", (flatDf.count(), flatDf.columns.length))



        val dropCols = flatDf.columns.filter(_.contains("artists"))

        dropCols.foreach(println)
        val finalDf = flatDf
            .drop(dropCols: _*).distinct()
            .drop("disc_number", "is_local", "preview_url", "type", "uri")
            .withColumnRenamed("id", "spotify_id")
            .withColumnRenamed("uri", "spotify_uri")
            .withColumnRenamed("name", "name")
            




        println("Shape: ", (finalDf.count(), finalDf.columns.length))



        finalDf.printSchema()

        spark.close()
    }
}
