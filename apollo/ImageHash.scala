// import java.awt.image.BufferedImage
// import java.awt.{Color, Image}
// import javax.imageio.ImageIO
// import java.io.File
// import scala.math._
// import org.apache.commons.math3.transform.{DctNormalization, FastCosineTransformer, TransformType}

// object ImageHashing {
//     case class ImageHash(hash: Array[Array[Boolean]]) {
//         override def toString: String = {
//             hash.flatten.zipWithIndex.map { case (v, i) =>
//                 if (v) 1 << (i % 8) else 0
//             }.grouped(8).map(_.sum).map(h => f"$h%02x").mkString
//         }

//         def -(other: ImageHash): Int = {
//             if (hash.length != other.hash.length || hash.head.length != other.hash.head.length) {
//                 throw new IllegalArgumentException("ImageHashes must be of the same shape.")
//             }
//             hash.flatten.zip(other.hash.flatten).count { case (a, b) => a != b }
//         }

//         override def equals(obj: Any): Boolean = obj match {
//             case other: ImageHash => hash.flatten.sameElements(other.hash.flatten)
//             case _ => false
//         }

//         override def hashCode(): Int = {
//             hash.flatten.zipWithIndex.collect {
//                 case (v, i) if v => 1 << (i % 8)
//             }.sum
//         }
//     }

//     def phash(image: BufferedImage, hashSize: Int = 8, highFreqFactor: Int = 4): ImageHash = {
//         val imgSize = hashSize * highFreqFactor
//         val resized = resizeImage(image, imgSize, imgSize)
//         val pixels = getGrayscalePixels(resized).grouped(imgSize).toArray

//         val transformer = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I)
//         val dct = pixels.map(row => transformer.transform(row, TransformType.FORWARD))

//         val dctLowFreq = dct.take(hashSize).map(_.slice(1, hashSize + 1))
//         val avg = dctLowFreq.flatten.sum / (hashSize * hashSize)
//         val diff = dctLowFreq.map(_.map(_ > avg))

//         ImageHash(diff)
//     }

//     private def resizeImage(image: BufferedImage, width: Int, height: Int): BufferedImage = {
//         val resized = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
//         val buffered = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
//         val g = buffered.getGraphics
//         g.drawImage(resized, 0, 0, null)
//         g.dispose()
//         buffered
//     }

//     private def getGrayscalePixels(image: BufferedImage): Array[Double] = {
//         val width = image.getWidth
//         val height = image.getHeight
//         val pixels = new Array[Double](width * height)
//         for (y <- 0 until height; x <- 0 until width) {
//             val color = new Color(image.getRGB(x, y))
//             pixels(y * width + x) = (color.getRed + color.getGreen + color.getBlue) / 3.0
//         }
//         pixels
//     }

//     def main(args: Array[String]): Unit = {
//         val image = ImageIO.read(new File("1.png"))
//         val hash = phash(image)
//         println(hash)
//     }
// }
