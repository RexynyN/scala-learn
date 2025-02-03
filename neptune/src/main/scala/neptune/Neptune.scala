// import org.bytedeco.opencv.global.opencv_imgcodecs._
// import org.bytedeco.opencv.opencv_img_hash._
// import org.bytedeco.opencv.opencv_core._
// import org.apache.log4j.{Level, Logger}
// import java.io.File

// object Neptune {
//     case class ImageHash(path: String, PHash: String, AverageHash: String, BlockMeanHash: String, ColorMomentHash: String, MarrHildrethHash: String, RadialVarianceHash: String)

//     def listFiles(directory: String): Array[String] = {
//         val dir = new File(directory)
//         if (dir.exists && dir.isDirectory) {
//             dir.listFiles.filter(_.isFile).map(_.getAbsolutePath)
//         } else {
//             Array[String]()
//         }
//     }

//     def listFilesRecursively(directory: String): Array[String] = {
//         def listFilesHelper(dir: File): Array[File] = {
//             val files = dir.listFiles
//             files ++ files.filter(_.isDirectory).flatMap(listFilesHelper)
//         }

//         val dir = new File(directory)
//         if (dir.exists && dir.isDirectory) {
//             listFilesHelper(dir).filter(_.isFile).map(_.getAbsolutePath)
//         } else {
//             Array[String]()
//         }
//     }

//     def computeHash(imagePath: String): ImageHash = {
//         // Carregar a imagem
//         val image = imread(imagePath, IMREAD_GRAYSCALE)
//         if (image.empty()) {
//             throw new IllegalArgumentException(s"Não foi possível carregar a imagem: $imagePath")
//         }

//         // Criar um objeto para armazenar o hash
//         val hash = new Mat()

//         AverageHash.create().compute(image, hash)
//         val avg = hash.data().getStringBytes().map("%02x".format(_)).mkString

//         BlockMeanHash.create().compute(image, hash)
//         val block = hash.data().getStringBytes().map("%02x".format(_)).mkString

//         ColorMomentHash.create().compute(image, hash)
//         val color = hash.data().getStringBytes().map("%02x".format(_)).mkString

//         MarrHildrethHash.create().compute(image, hash)
//         val marr = hash.data().getStringBytes().map("%02x".format(_)).mkString

//         PHash.create().compute(image, hash)
//         val pHash = hash.data().getStringBytes().map("%02x".format(_)).mkString

//         RadialVarianceHash.create().compute(image, hash)
//         val radial = hash.data().getStringBytes().map("%02x".format(_)).mkString

//         ImageHash(imagePath, pHash, avg, block, color, marr, radial)
//     }


//     // def main(args: Array[String]): Unit = {
//     //     // STOP LOGGIN' EVER'THANG
//     //     Logger.getLogger("org").setLevel(Level.ERROR)

//     //     val dir = "/mnt/f/Back-Up/Downloads/folder/weeb/weeb"
        
//     //     try {
//     //         val hash = computeHash("image.jpg")
//     //         println(s"Hash da imagem: $hash")
//     //     } catch {
//     //         case e: Exception => println(s"Erro ao calcular hash: ${e.getMessage}")
//     //     }
//     // }
// }
