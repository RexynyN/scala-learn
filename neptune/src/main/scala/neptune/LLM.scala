import org.opencv.core._
import org.opencv.videoio._
import org.opencv.videoio.Videoio._
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.io._
import scala.util.{Success, Failure}

case class VideoHashes(path: String, hashes: List[Map[String, String]])

// Função para calcular o hash da imagem (como exemplo, utilizando uma biblioteca fictícia)
object ImageHashing {

  def averageHash(image: Mat): String =
    image.toString() // Placeholder, substitua com seu código de hash
  def cropResistantHash(image: Mat): String = image.toString() // Placeholder
  def whash(image: Mat): String = image.toString() // Placeholder
  def phash(image: Mat): String = image.toString() // Placeholder
  def dhash(image: Mat): String = image.toString() // Placeholder
  def colorhash(image: Mat): String = image.toString() // Placeholder
}

object VideoProcessor {
  val FRAME_INTERVAL = 1000 // 1 segundo
  val NUM_WORKERS = 30

  // Função para ler vídeos na pasta
  def listVideosInDirectory(directoryPath: String): List[String] = {
    val videoExtensions = Set(".mp4", ".avi", ".mov", ".mkv", ".m4v")
    val videoFiles = new java.io.File(directoryPath)
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
    Map(
      "avg" -> ImageHashing.averageHash(frame),
      "crop" -> ImageHashing.cropResistantHash(frame),
      "whash" -> ImageHashing.whash(frame),
      "phash" -> ImageHashing.phash(frame),
      "dhash" -> ImageHashing.dhash(frame),
      "color" -> ImageHashing.colorhash(frame)
    )
  }

  // Função para processar um vídeo e gerar seus hashes
  def videoHashes(path: String): List[Map[String, String]] = {
    val vidCap = new VideoCapture(path)

    val frameCount = vidCap.get(CAP_PROP_FRAME_COUNT).toLong
    println("Framecount: " + frameCount)
    val fps = vidCap.get(CAP_PROP_FPS)
    println("FPS: " + fps)
    val duration = (frameCount / fps * 1000).toLong
    println("Duration: " + duration)

    var count = 0
    var hashes = List[Map[String, String]]()

    while (count < duration) {
      vidCap.set(CAP_PROP_POS_MSEC, count.toDouble)
      val frame = new Mat()
      if (vidCap.read(frame)) {
        val hash = computeHashes(frame)
        hashes =
          hash + ("frame_order" -> (count / FRAME_INTERVAL).toString) :: hashes
      }
      count += FRAME_INTERVAL
    }

    hashes.foreach(println)
    hashes
  }

  // Função para gerar o arquivo CSV com os hashes
  def csvVideoHashes(path: String): Option[String] = {
    println(s"Processing: $path")
    val hashes = videoHashes(path)
    if (hashes.isEmpty)
      return None
    else {
      val row = s"$path;" + hashes.map(h => s"${h.mkString(",")}").mkString(";")
      Some(row)
    }
  }

  // Função para processar múltiplos vídeos paralelamente
  def runWorkers(
      paths: List[String],
      outputCsv: String = "output.csv"
  ): Unit = {
    val futures = paths.map(path => Future { csvVideoHashes(path) })
    val results = Future.sequence(futures).map(_.flatten)

    results.onComplete {
      case Success(rows) =>
        val writer = new PrintWriter(new File(outputCsv))
        writer.write("path;frame_order;avg;crop;whash;phash;dhash;color\n")
        rows.foreach(writer.write)
        writer.close()
      case Failure(exception) => println(s"An error occurred: $exception")
    }
  }

  // Função para iniciar o processo de hashing
  def startHashing(directoryPath: String): Unit = {
    val videoPaths = listVideosInDirectory(directoryPath)
    videoPaths.foreach(println)
    runWorkers(videoPaths)
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    // System.load(Core.NATIVE_LIBRARY_NAME) 
    // Caminho para a pasta dos vídeos
    // val directoryPath = "/mnt/c/users/breno/downloads/bingus"
    // VideoProcessor.startHashing(directoryPath)
    VideoProcessor.videoHashes("/mnt/c/users/breno/downloads/twitter.mp4")
  }
}