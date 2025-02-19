import java.io._
import java.util.zip._

object ZipExample {
  def zipFiles(zipFileName: String, files: List[String]): Unit = {
    val zipFile = new File(zipFileName)
    val fos = new FileOutputStream(zipFile)
    val zipOut = new ZipOutputStream(fos)

    files.foreach { file =>
      val fileToZip = new File(file)
      val fis = new FileInputStream(fileToZip)
      val zipEntry = new ZipEntry(fileToZip.getName)
      
      zipOut.putNextEntry(zipEntry)
      
      val buffer = new Array[Byte](1024)
      var length = fis.read(buffer)
      while (length >= 0) {
        zipOut.write(buffer, 0, length)
        length = fis.read(buffer)
      }

      fis.close()
      zipOut.closeEntry()
    }

    zipOut.close()
    fos.close()
  }

  // def main(args: Array[String]): Unit = {
  //   val files = List("arquivo1.txt", "arquivo2.txt") // Altere para os arquivos que deseja compactar
  //   zipFiles("meu_arquivo.zip", files)
  //   println("Arquivo ZIP criado com sucesso!")
  // }
}