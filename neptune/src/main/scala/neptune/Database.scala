import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.io._
import scala.util.{Success, Failure}

import neptune.ImageHasher

object ImageProcessor {
  val FRAME_INTERVAL = 1000 // 1 segundo
  val NUM_WORKERS = 30

  // Função para ler vídeos na pasta
  def listVideosInDirectory(directoryPath: String): List[String] = {
    val videoExtensions = Set(".gif", ".jfif", ".png", ".jpg", ".jpeg")
    val videoFiles = new File(directoryPath)
      .listFiles()
      .filter(file =>
        videoExtensions.exists(ext => file.getName.toLowerCase.endsWith(ext))
      )
      .map(file => file.getAbsolutePath)
      .toList

    naturalSort(videoFiles)
  }

  def naturalSort(l: List[String]): List[String] = {
    // Função de conversão de cada parte da string (número ou texto)
    val convert: String => Any = text =>
      if (text.matches("\\d+")) text.toLong else text.toLowerCase

    // Função que divide a string em partes (números e não-números) e faz a conversão
    val alphanumKey: String => List[Any] = key =>
      "\\D+|\\d+".r.findAllIn(key).map(convert).toList

    // Criando um Ordering explícito para a chave de ordenação
    implicit val ordering: Ordering[List[Any]] = new Ordering[List[Any]] {
      def compare(a: List[Any], b: List[Any]): Int = {
        val len = math.min(a.length, b.length)
        for (i <- 0 until len) {
          (a(i), b(i)) match {
            case (x: Int, y: Int)       => if (x != y) return x - y
            case (x: String, y: String) => if (x != y) return x.compareTo(y)
            case _ => // Caso de comparação mista entre String e Int
          }
        }
        a.length - b.length
      }
    }

    // Ordena a lista de strings usando a chave gerada por alphanumKey
    l.sortBy(alphanumKey)
  }

  // Função para calcular hashes para cada frame
  def computeHashes(frame: Mat): Map[String, String] = {
    val hashes = ImageHasher.computeHash(frame)

    Map(
      "avg" -> hashes.AverageHash,
      "block" -> hashes.BlockMeanHash,
      "radial" -> hashes.RadialVarianceHash,
      "phash" -> hashes.PHash,
      "marr" -> hashes.MarrHildrethHash,
      "color" -> hashes.ColorMomentHash
    )
  }
    
  // Função para gerar o arquivo CSV com os hashes
  def csvVideoHashes(path: String): Option[String] = {
    val img = imread(path)

    val hashes = computeHashes(img)
    if (hashes.isEmpty)
      return None
    else {
      val row = s"$path;" + hashes.mkString(";")
      println(s"$path => $row")
      Some(row)
    }
  }

  // Função para processar múltiplos vídeos paralelamente
  def runWorkers(paths: List[String], outputCsv: String = "output.csv"): Unit = {
    val futures = paths.map(path => Future { csvVideoHashes(path) })
    val results = Future.sequence(futures).map(_.flatten)

    results.onComplete {
      case Success(rows) =>
        val writer = new PrintWriter(new File(outputCsv))
        writer.write("path;avg;block;radial;phash;marr;color\n")
        rows.foreach(row => writer.write(row + "\n"))
        writer.close()
      case Failure(exception) => println(s"An error occurred: $exception")
    }
  }

  def startHashingAsync(directoryPath: String): Unit = {
    val imagePaths = listVideosInDirectory(directoryPath)
    imagePaths.foreach(println)
    runWorkers(imagePaths)
  }

  def startHashing(directoryPath: String): Unit = {
    val imagePaths = listVideosInDirectory(directoryPath)
    imagePaths.foreach(println)

    var row = ""
    val writer = new PrintWriter(new File("output.csv"))
    writer.write("path;avg;block;radial;phash;marr;color\n")

    for(img <- imagePaths) {
      row = csvVideoHashes(img).getOrElse("")

      if (!row.isEmpty())
        writer.write(row + "\n")
    }
    writer.close()
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    // Caminho para a pasta dos vídeos
    val directoryPath = "/home/breno/codes/cv/hash-similarity/data/original"
    ImageProcessor.startHashing(directoryPath)
    
    // VideoProcessor.videoHashes("twitter.mp4")
  }
}