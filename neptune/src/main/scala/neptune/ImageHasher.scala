package neptune

import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_img_hash._
import org.bytedeco.opencv.opencv_core._
import java.io.File

object ImageHasher {
    case class ImageHash(PHash: String, AverageHash: String, BlockMeanHash: String, ColorMomentHash: String, MarrHildrethHash: String, RadialVarianceHash: String)

    def listFiles(directory: String): Array[String] = {
        val dir = new File(directory)
        if (dir.exists && dir.isDirectory) {
            dir.listFiles.filter(_.isFile).map(_.getAbsolutePath)
        } else {
            Array[String]()
        }
    }

    def listFilesRecursively(directory: String): Array[String] = {
        def listFilesHelper(dir: File): Array[File] = {
            val files = dir.listFiles
            files ++ files.filter(_.isDirectory).flatMap(listFilesHelper)
        }

        val dir = new File(directory)
        if (dir.exists && dir.isDirectory) {
            listFilesHelper(dir).filter(_.isFile).map(_.getAbsolutePath)
        } else {
            Array[String]()
        }
    }


    def computeHashFromPath(image: String): ImageHash = {
        val img = imread(image)
        computeHash(img)
    }

    def computeHash(image: Mat): ImageHash = {
        if (image.empty()) {
            throw new IllegalArgumentException("The given image is empty")
        }

        // Criar um objeto para armazenar o hash
        val hash = new Mat()

        AverageHash.create().compute(image, hash)
        val avg = hash.data().getStringBytes().map("%02x".format(_)).mkString

        BlockMeanHash.create().compute(image, hash)
        val block = hash.data().getStringBytes().map("%02x".format(_)).mkString

        ColorMomentHash.create().compute(image, hash)
        val color = hash.data().getStringBytes().map("%02x".format(_)).mkString

        MarrHildrethHash.create().compute(image, hash)
        val marr = hash.data().getStringBytes().map("%02x".format(_)).mkString

        PHash.create().compute(image, hash)
        val pHash = hash.data().getStringBytes().map("%02x".format(_)).mkString

        RadialVarianceHash.create().compute(image, hash)
        val radial = hash.data().getStringBytes().map("%02x".format(_)).mkString

        ImageHash(pHash, avg, block, color, marr, radial)
    }
}