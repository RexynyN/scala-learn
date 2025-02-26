package neptune

import org.apache.spark.sql.{SparkSession, DataFrame} 
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{VectorAssembler, StandardScaler}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.PipelineModel
import org.apache.log4j._

object MLTest {
    case class Data(features: org.apache.spark.ml.linalg.Vector, label: Double)

    def explorationAnalysis(data: DataFrame): DataFrame = {
        data.printSchema()

        // Seleciona todas as colunas do tipo string
        val strIndexes = data.columns.filter(c => data.schema(c).dataType.toString == "StringType")
        val strings = data.select(strIndexes.map(col): _*)
        strings.show()
        
        // Seleciona todas as colunas do tipo número
        val numIndexes = data.columns.filter(c => data.schema(c).dataType.toString == "IntegerType" || data.schema(c).dataType.toString == "DoubleType")
        val numbers = data.select(numIndexes.map(col): _*)
        numbers.describe().show()
        numbers.summary().show()

        val fillMap = Map(
            "Age" -> data.agg(round(median(data("Age")), 2).alias("median")).first().getDouble(0),
            "Fare" -> 0.0,
            "Embarked" -> 0.0
        )

        // data.columns.foreach { col => 
        //     println(s"$col Nulls: " + data.filter(data(col).isNull)
        //         .alias(s"$col Nulls")
        //         .count()
        //     )
        // }
        
        // numbers.columns.foreach{ col =>
        //     println(s"Coluna: $col")
        //     // calcula a média de uma coluna

        //     data.agg(round(mean(data(col))))
        //         .alias(s"$col Mean")
        //         .show()


        //     // distinct dos valores
        //     data.select(col)
        //         .distinct()
        //         .alias(s"$col Distincts")
        //         .show(50)
        // }

        data.na.fill(fillMap)
    }

    def main(args: Array[String]): Unit = {
        Logger.getLogger("org").setLevel(Level.ERROR)
        
        // Criar a Spark Session
        val spark = SparkSession.builder()
            .appName("ML")
            .master("local[*]")
            .getOrCreate()

        // Carregar os dados
        var data = spark.read
            .option("header", "true")
            .option("delimiter", ",")   
            .option("inferSchema", "true")
            .csv("Titanic.csv")


        // Análise exploratória e Limpeza dos dados
        data = explorationAnalysis(data)



        // Preparação dos dados
        val assembler = new VectorAssembler()
            .setInputCols(Array("Pclass", "Age", "SibSp", "Parch", "Fare"))
            .setOutputCol("features")
            

    }
}

