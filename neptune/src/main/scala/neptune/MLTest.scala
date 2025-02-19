package neptune

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{VectorAssembler, StandardScaler}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.PipelineModel
import org.apache.log4j._


object MLTest {
    def main(args: Array[String]): Unit = {
        Logger.getLogger("org").setLevel(Level.ERROR)
        
        // Criar a Spark Session
        val spark = SparkSession.builder()
            .appName("ML")
            .master("local[*]")
            .getOrCreate()

        // Carregar os dados
        val data = spark.read
            .option("header", "true")
            .option("delimiter", ";")   
            .option("inferSchema", "true")
            .csv("images-cellphone-1.csv")


        data.select("width", "height", "price").show()
        data.show()
       
        spark.stop()
    }
}

