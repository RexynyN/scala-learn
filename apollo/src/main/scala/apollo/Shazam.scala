// package apollo

// // import apollo.{ApacheFFT,  CooleyTurkeyFFT, Complex}
// import java.io.{ByteArrayOutputStream, IOException, OutputStream, File}
// import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, TargetDataLine}
// import javax.sound.sampled.{AudioInputStream, AudioSystem}
// import org.apache.commons.math3.transform.{FastFourierTransformer, DftNormalization, TransformType}
// import org.apache.commons.math3.complex.Complex

// private case class DataPoint(songId: Int, time: Int)

// object Shazam {
//     private val RANGE: Array[Int] = Array(40, 80, 120, 180, 300)
//     private val FUZ_FACTOR = 2

//     private def getFormat(): AudioFormat = {
//         val sampleRate: Float = 44100
//         val sampleSizeInBits: Int = 16
//         val channels: Int = 1          // mono
//         val signed: Boolean = true     // Indicates whether the data is signed or unsigned
//         val bigEndian: Boolean = true  // Indicates whether the audio data is stored in big-endian or little-endian order
//         return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)
//     }


//     // Get the audio file total byte size
//     def getWavFileSize(filePath: String): Long = {
//         val file = new File(filePath)
//         val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(file)
//         val byteArrayOutputStream = new ByteArrayOutputStream()
//         val buffer = new Array[Byte](4096)
//         var bytesRead: Int = 0
//         var totalBytesRead: Long = 0L
//         while ({ bytesRead = audioInputStream.read(buffer); bytesRead } != -1) {
//             totalBytesRead += bytesRead
//         }

//         audioInputStream.close()
//         totalBytesRead
//     }
    
//     def readWavFileChunk(filePath: String): Array[Byte] = {
//         val file = new File(filePath)
//         val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(file)
//         val byteArrayOutputStream = new ByteArrayOutputStream()
//         val buffer = new Array[Byte](4096)
//         var bytesRead: Int = 0
        
//         while ({ bytesRead = audioInputStream.read(buffer); bytesRead } != -1) {
//             byteArrayOutputStream.write(buffer, 0, bytesRead)
//         }

//         audioInputStream.close()
//         byteArrayOutputStream.toByteArray
//     }

//     def readWavFileChunk(filePath: String, offset: Long, readLimit: Long): Array[Byte] = {
//         val file = new File(filePath)
//         val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(file)
//         val byteArrayOutputStream = new ByteArrayOutputStream()
//         val buffer = new Array[Byte](4096)
//         var bytesRead: Int = 0

//         // Skip the offset bytes
//         var skippedBytes: Long = 0L
//         while (skippedBytes < offset) {
//             val skipped = audioInputStream.skip(offset - skippedBytes)
//             if (skipped <= 0) {
//                 throw new IOException("Unable to skip the required number of bytes")
//             }
//             skippedBytes += skipped
//         }

//         println("Skipped bytes: " + skippedBytes)
//         var totalBytesRead: Long = 0L
//         while ({ bytesRead = audioInputStream.read(buffer); bytesRead } != -1) {
//             byteArrayOutputStream.write(buffer, 0, bytesRead)
//             totalBytesRead += bytesRead

//             // println("Read bytes: " + bytesRead)
//             if (totalBytesRead >= readLimit) {
//                 println(totalBytesRead)
//                 audioInputStream.close()
//                 return byteArrayOutputStream.toByteArray
//             }
//         }

//         println(totalBytesRead)
//         audioInputStream.close()
//         byteArrayOutputStream.toByteArray
//     }


//     def readMicrophone(): Array[Byte] = {
//         // Read from microphone
//         val format = getFormat() // Fill AudioFormat with the settings
//         val info = new DataLine.Info(classOf[TargetDataLine], format)
//         val line = AudioSystem.getLine(info).asInstanceOf[TargetDataLine]
//         line.open(format)
//         line.start()

//         val buffer: Array[Byte] = new Array[Byte](4096)
//         val out: OutputStream = new ByteArrayOutputStream()
//         var running = true
//         try {
//             while (running) {
//                 val count = line.read(buffer, 0, buffer.length)
//                 if (count > 0) {
//                     out.write(buffer, 0, count)
//                 }
//             }
//             out.close()
//         } catch {
//             case e: IOException =>
//                 println("I/O problems: " + e)
//                 System.exit(-1)
//         }
    
//         val audio: Array[Byte] = out.asInstanceOf[ByteArrayOutputStream].toByteArray
//         line.stop()
//         line.close()
//         audio
//     }

//     def main(args: Array[String]): Unit = { 
//         // Example usage in the main function
//         val filePath = "90210.wav"
//         val audioSizeChunk: Long = 10 * 1024 * 1024 // 10MB chunk size
//         val audioSize: Long = getWavFileSize(filePath) 
//         val numChunks = (audioSize / audioSizeChunk).toInt
        
//         for (i <- 0 until numChunks) {
//             println("Chunk " + i)
//             val audio: Array[Byte] = readWavFileChunk(filePath, audioSizeChunk * i, audioSizeChunk)
//             val totalSize: Int = audio.length
//             val chunkSize: Int = 4096 
//             val sampledChunkSize: Int = totalSize / chunkSize
//             val result: Array[Array[Complex]] = Array.ofDim[Complex](sampledChunkSize, chunkSize)

//             val fftTranformer = new FastFourierTransformer(DftNormalization.STANDARD)
//             for (j <- 0 until sampledChunkSize) {
//                 val arr: Array[Double] = Array.ofDim[Double](chunkSize)

//                 for (i <- 0 until chunkSize) {
//                     arr(i) = audio((j * chunkSize) + i).toDouble
//                 }
//                 result(j) = fftTranformer.transform(arr, TransformType.FORWARD)
//             }

//             val highscores: Array[Array[Double]] = Array.fill(result.length, RANGE.length)(Double.MinValue)
//             val points: Array[Array[Int]] = Array.ofDim[Int](result.length, RANGE.length)

//             for (t <- result.indices) {
//                 for (freq <- 40 until 300) {
//                     val mag = math.log(result(t)(freq).abs + 1)
//                     val index = getIndex(freq)

//                     if (mag > highscores(t)(index)) {
//                         highscores(t)(index) = mag
//                         points(t)(index) = freq
//                     }
//                 }

//                 val h = hash(points(t)(0), points(t)(1), points(t)(2), points(t)(3))
//                 println(h)
//             }
//         }
//     }


//     // Find out in which range is frequency
//     private def getIndex(freq: Int): Int = {
//         var i = 0
//         while (i < RANGE.length && RANGE(i) < freq) {
//             i += 1  
//         }
//         i
//     }

//     private def hash(p1: Int, p2: Int, p3: Int, p4: Int): Long = {
//         ((p4 - (p4 % FUZ_FACTOR)).toLong * 100000000L) +
//         ((p3 - (p3 % FUZ_FACTOR)).toLong * 100000L) +
//         ((p2 - (p2 % FUZ_FACTOR)).toLong * 100L) +
//         (p1 - (p1 % FUZ_FACTOR)).toLong
//     }
// }




