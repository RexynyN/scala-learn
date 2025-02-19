import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import java.util.concurrent.Executors
// import ExecutionContext.Implicits.global
import java.io._
import scala.util.{Success, Failure}

import neptune.ImageHasher

object ImageProcessor {
  val FRAME_INTERVAL = 1000 // 1 segundo
  val NUM_WORKERS = 30

  // Define um ExecutionContext com um pool fixo de threads
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

  // Função genérica para aplicar paralelamente uma função a um iterável
  def parallelMap[T, R](iterable: Iterable[T], f: T => R): Future[List[R]] = {
    val futures =
      iterable.map(el => Future { f(el) }) // Cria um Future para cada elemento
    Future.sequence(
      futures.toList
    ) // Converte a sequência de Futures em um Future de sequência
  }

  def listFilesRecursively(directory: String): List[String] = {
    def listFilesHelper(dir: File): List[File] = {
      val files = dir.listFiles.toList
      files ++ files.filter(_.isDirectory).flatMap(listFilesHelper)
    }

    val dir = new File(directory)
    if (dir.exists && dir.isDirectory) {
      val videoExtensions = Set(".gif", ".jfif", ".png", ".jpg", ".jpeg")
      listFilesHelper(dir)
        .filter(_.isFile)
        .filter(file =>
          videoExtensions.exists(ext => file.getName.toLowerCase.endsWith(ext))
        )
        .map(_.getAbsolutePath)
    } else {
      List[String]()
    }
  }

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
    println(path)
    if (img.empty())
      return None

    val hashes = computeHashes(img)
    if (hashes.isEmpty)
      return None
    else {
      val row = s"$path;" + hashes.map(_._2).mkString(";")
      Some(row)
    }
  }

  def createCsvFile(rows: List[String]): Unit = {
    val writer = new PrintWriter(new File("output-async.csv"))
    writer.write("path;avg;block;radial;phash;marr;color\n")

    for (img <- rows) {
      if (!img.isEmpty())
        writer.write(img + "\n")
    }
    writer.close()
    println("Arquivo CSV gerado com sucesso")
  }

  // Função para processar múltiplos vídeos paralelamente
  def startHashingAsync(directoryPath: String): Unit = {
    val imagePaths = listFilesRecursively(directoryPath)
    println(s"${imagePaths.length} imagens encontradas")
    // imagePaths.foreach(println)

    val resultFuture = parallelMap(imagePaths, csvVideoHashes)

    resultFuture.onComplete {
      case Success(result) => createCsvFile(result.map(_.getOrElse("")))
      case Failure(ex)     => println(s"Erro: ${ex.getMessage}")
    }

    Await.result(resultFuture, 72.hour)
  }

  def startHashing(directoryPath: String): Unit = {
    val imagePaths = listFilesRecursively(directoryPath)
    imagePaths.foreach(println)

    var row = ""
    val writer = new PrintWriter(new File("output.csv"))
    writer.write("path;avg;block;radial;phash;marr;color\n")

    for (img <- imagePaths) {
      row = csvVideoHashes(img).getOrElse("")

      if (!row.isEmpty())
        writer.write(row + "\n")
    }
    writer.close()
    println("Arquivo CSV gerado com sucesso")
  }
}

object Main {
  def manin(args: Array[String]): Unit = {
    // Caminho para a pasta dos vídeos
    // val directoryPath = "/mnt/f/Back-Up/Downloads/folder/weeb/weeb"
    val directoryPath = "/mnt/f/Back-Up/Redmi Backup 20241129"

    // Set(ImageProcessor.listFilesRecursively(directoryPath)).toList
    //   .foreach(println)
    ImageProcessor.startHashingAsync(directoryPath)
  }
}
