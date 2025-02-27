import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{VectorAssembler, StandardScaler}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.PipelineModel
import org.apache.log4j._


object MLTest {
    case class Data(features: org.apache.spark.ml.linalg.Vector, label: Double)

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
            .option("delimiter", ",")   
            .option("inferSchema", "true")
            .csv("Titanic.csv")

        // data.columns.filter(c => data.schema(c).dataType.toString == "IntegerType")
        val numbers = data.select("Survived", "Pclass", "Age", "SibSp", "Parch", "Fare")
        // println(numbers.describe().show())
        // println(numbers.summary().show())

        data.columns.foreach { col => 
            println(s"$col Nulls: " + data.filter(data(col).isNull)
                .alias(s"$col Nulls")
                .count()
            )
        }
        
        numbers.columns.foreach{ col =>
            println(s"Coluna: $col")
            // calcula a média de uma coluna
            data.agg(round(mean(data(col))))
                .alias(s"$col Mean")
                .show()       
            // distinct dos valores
            data.select(col)
                .distinct()
                .alias(s"$col Distincts")
                .show()
        }
    }
}

