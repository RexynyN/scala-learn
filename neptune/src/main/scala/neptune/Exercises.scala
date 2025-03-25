package neptune

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object Exercises {
    def exercise1(): Unit = {
        val spark = SparkSession.builder()
            .appName("Exercises")
            .master("local[*]")
            .getOrCreate()

        import spark.implicits._
        val dept = Seq(
            ("50000.0#0#0#", "#"),
            ("0@1000.0@", "@"),
            ("1$", "$"),
            ("1000.00^Test_string", "^")).toDF("VALUES", "Delimiter")

        

        val extra = dept.withColumn("SPLIT", split($"VALUES", $"Delimiter"))

        extra.show()
    }

    def main(args: Array[String]): Unit = {
        exercise1()
    }
}