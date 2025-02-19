package neptune

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{VectorAssembler, StandardScaler}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.PipelineModel


object MLProject {
    def manin(args: Array[String]): Unit = {
        // Criar a Spark Session
        val spark = SparkSession.builder()
        .appName("Regressao Linear Spark ML")
        .master("local[*]")
        .getOrCreate()

        // Carregar os dados
        val data = spark.read
            .option("header", "true")   
            .option("inferSchema", "true")
            .csv("dados.csv")

        data.show()

        data.describe().show()

        // Seleção de atributos
        val colunasSelecionadas = Array("feature1", "feature2", "feature3", "target")
        val dadosSelecionados = data.select(colunasSelecionadas.map(col): _*)

        dadosSelecionados.summary().show()

        // Tratamento de valores nulos
        dadosSelecionados.na.drop().show()

        // Criando vetores de features
        val assembler = new VectorAssembler()
            .setInputCols(Array("feature1", "feature2", "feature3"))
            .setOutputCol("features")

        val dadosTransformados = assembler.transform(dadosSelecionados).select("features", "target")

        dadosTransformados.show()

        // Normalização das features
        val scaler = new StandardScaler()
            .setInputCol("features")
            .setOutputCol("scaledFeatures")

        val scalerModel = scaler.fit(dadosTransformados)
        val dadosNormalizados = scalerModel.transform(dadosTransformados).select("scaledFeatures", "target")

        dadosNormalizados.show()

        // Divisão em treino e teste
        val Array(dadosTreino, dadosTeste) = dadosNormalizados.randomSplit(Array(0.8, 0.2), seed = 1234)

        // Criar e treinar o modelo
        val lr = new LinearRegression()
        .setFeaturesCol("scaledFeatures")
        .setLabelCol("target")

        val modelo = lr.fit(dadosTreino)

        // Avaliação do modelo
        val predicoes = modelo.transform(dadosTeste)
        predicoes.show()

        val evaluator = new RegressionEvaluator()
            .setLabelCol("target")
            .setPredictionCol("prediction")
            .setMetricName("rmse")

        val rmse = evaluator.evaluate(predicoes)
        println(s"Root Mean Squared Error (RMSE): $rmse")

        // Salvar o modelo
        modelo.write.overwrite().save("modelo_regressao_linear")

        println("Modelo salvo com sucesso!")

        spark.stop()

    }
}

