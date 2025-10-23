import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.{File, InputStream}
import javax.imageio.ImageIO
import scala.math._
import java.awt.Image
import org.bytedeco.javacv.{FFmpegFrameGrabber, Java2DFrameConverter}
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer
import java.awt.image.BufferedImage

object ImageHash {
  // Função para converter imagem para escala de cinza e redimensionar
  private def resizeAndGrayScale(image: BufferedImage, width: Int, height: Int): BufferedImage = {
    val resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    val g = resizedImage.createGraphics()
    g.drawImage(image, 0, 0, width, height, null)
    g.dispose()
    resizedImage
  }
  
  // Obter valor do pixel (como nível de cinza)
  private def getGrayPixel(image: BufferedImage, x: Int, y: Int): Int = {
    image.getRGB(x, y) & 0xFF
  }
  
  // Calcular distância de Hamming entre dois hashes
  def hammingDistance(hash1: String, hash2: String): Int = {
    if (hash1.length != hash2.length) {
      throw new IllegalArgumentException("Os hashes devem ter o mesmo comprimento")
    }
    
    hash1.zip(hash2).count { case (a, b) => a != b }
  }
  
  // Converter array de bits para string hexadecimal
  private def bitsToHexString(bits: Seq[Int]): String = {
    bits.grouped(4).map { group =>
      val value = group.zipWithIndex.map { case (bit, idx) => 
        if (bit == 1) 1 << (3 - idx) else 0 
      }.sum
      value.toHexString
    }.mkString
  }

  // Implementação do dHash de 64 bits (8x8)
  def dHash(image: BufferedImage): String = {
    // Redimensionar para 9x8 (para obter 8x8 diferenças)
    val width = 9
    val height = 8
    val img = resizeAndGrayScale(image, width, height)
    
    // Calcular hash baseado na diferença entre pixels adjacentes
    val bits = Array.ofDim[Int](width * height)
    var idx = 0
    
    for (y <- 0 until height) {
      for (x <- 0 until width - 1) {
        // Se o pixel da esquerda é mais brilhante que o da direita, bit = 1
        val left = getGrayPixel(img, x, y)
        val right = getGrayPixel(img, x + 1, y)
        bits(idx) = if (left > right) 1 else 0
        idx += 1
      }
    }
    
    // Converter para representação hexadecimal
    bitsToHexString(bits.take(64))
  }
  
  // Verificar similaridade usando distância de Hamming
  def isSimilar(hash1: String, hash2: String, threshold: Int = 5): Boolean = {
    hammingDistance(hash1, hash2) <= threshold
  }

  // Cálculo do hash perceptual
  def pHash(image: BufferedImage, size: Int = 32, smallerSize: Int = 8): String = {
    // Redimensionar e converter para escala de cinza
    val img = resizeAndGrayScale(image, size, size)
    
    // Preparar os valores para DCT
    val vals = Array.ofDim[Double](size, size)
    for (x <- 0 until size; y <- 0 until size) {
      vals(x)(y) = getGrayPixel(img, x, y).toDouble
    }
    
    // Aplicar DCT (Transformada Discreta de Cosseno)
    val dctVals = applyDCT(vals)
    
    // Calcular valor médio dos coeficientes DCT (ignorando DC)
    var total = 0.0
    for (x <- 0 until smallerSize; y <- 0 until smallerSize) {
      total += dctVals(x)(y)
    }
    total -= dctVals(0)(0)
    val avg = total / ((smallerSize * smallerSize) - 1)
    
    // Gerar hash - 1 se coeficiente > média, 0 caso contrário
    val bits = for {
      x <- 0 until smallerSize
      y <- 0 until smallerSize
    } yield if (dctVals(x)(y) > avg) 1 else 0
    
    bitsToHexString(bits)
  }
  
  // Implementação da DCT-II (Transformada Discreta de Cosseno tipo II)
  private def applyDCT(input: Array[Array[Double]]): Array[Array[Double]] = {
    val n = input.length
    val output = Array.ofDim[Double](n, n)
    
    for (u <- 0 until n; v <- 0 until n) {
      var sum = 0.0
      
      for (i <- 0 until n; j <- 0 until n) {
        sum += input(i)(j) * 
              cos((2 * i + 1) * u * Pi / (2 * n)) * 
              cos((2 * j + 1) * v * Pi / (2 * n))
      }
      
      // Ajuste para u=0 e v=0
      val alpha_u = if (u == 0) 1.0 / sqrt(2) else 1.0
      val alpha_v = if (v == 0) 1.0 / sqrt(2) else 1.0
      
      output(u)(v) = (2.0 / n) * alpha_u * alpha_v * sum
    }
    
    output
  }

  def aHash(image: BufferedImage, size: Int = 8): String = {
    // Redimensionar para tamanho desejado e converter para escala de cinza
    val img = resizeAndGrayScale(image, size, size)
    
    // Calcular média dos valores de pixels
    var sum = 0
    for (y <- 0 until size; x <- 0 until size) {
      sum += getGrayPixel(img, x, y)
    }
    val avg = sum / (size * size)
    
    // Gerar hash: 1 se pixel > média, 0 caso contrário
    val bits = for {
      y <- 0 until size
      x <- 0 until size
    } yield if (getGrayPixel(img, x, y) > avg) 1 else 0
    
    bitsToHexString(bits)
  }

  def cropResistantHash(image: BufferedImage): String = {
    // Primeiro, detectar e extrair regiões significativas da imagem
    val regions = detectSignificantRegions(image)
    
    // Processar cada região e calcular hashes individuais
    val regionHashes = regions.map { region =>
      // Normalizar orientação da região
      val normalizedRegion = normalizeOrientation(region)
      
      // Calcular hash da região normalizada (usando dHash)
      dHash(normalizedRegion)
    }
    
    // Combinar os hashes das regiões em um único hash
    val combinedHash = combineRegionHashes(regionHashes)
    
    combinedHash
  }
  
  // Detecta regiões significativas na imagem
  private def detectSignificantRegions(image: BufferedImage): List[BufferedImage] = {
    // Implementação simplificada: usa a imagem inteira como única região
    // Uma implementação real usaria algoritmos de segmentação de imagem
    List(image)
  }
  
  // Normaliza a orientação de uma região
  private def normalizeOrientation(region: BufferedImage): BufferedImage = {
    // Implementação simplificada: retorna a região sem modificação
    // Uma implementação real detectaria a orientação e rotacionaria a imagem
    region
  }
  
  // Combina hashes de múltiplas regiões
  private def combineRegionHashes(hashes: List[String]): String = {
    // Implementação simplificada: concatena os hashes
    // Uma implementação real poderia usar métodos mais sofisticados
    if (hashes.isEmpty) {
      return "0" * 16  // Hash vazio
    }
    hashes.mkString("")
  }

  def colorHash(image: BufferedImage, binBits: Int = 3): String = {
    val width = image.getWidth
    val height = image.getHeight
    
    // Contadores para faixas de cores HSV
    val hueCount = Array.fill(1 << binBits)(0)
    val satCount = Array.fill(1 << binBits)(0)
    val valCount = Array.fill(1 << binBits)(0)
    
    var blackCount = 0
    var whiteCount = 0
    var totalPixels = 0
    
    // Processar cada pixel
    for (y <- 0 until height; x <- 0 until width) {
      val rgb = image.getRGB(x, y)
      val r = (rgb >> 16) & 0xFF
      val g = (rgb >> 8) & 0xFF
      val b = rgb & 0xFF
      
      // Verificar se é preto ou branco
      if (r < 30 && g < 30 && b < 30) {
        blackCount += 1
      } else if (r > 225 && g > 225 && b > 225) {
        whiteCount += 1
      } else {
        // Converter RGB para HSV
        val hsv = rgbToHsv(r, g, b)
        val h = hsv._1
        val s = hsv._2
        val v = hsv._3
        
        // Discretizar e contar
        val hueIdx = (h * (1 << binBits)).toInt % (1 << binBits)
        val satIdx = (s * (1 << binBits)).toInt
        val valIdx = (v * (1 << binBits)).toInt
        
        hueCount(hueIdx) += 1
        satCount(satIdx) += 1
        valCount(valIdx) += 1
      }
      
      totalPixels += 1
    }
    
    // Calcular proporções
    val blackRatio = blackCount.toDouble / totalPixels
    val whiteRatio = whiteCount.toDouble / totalPixels
    
    // Gerar hash
    val bits = (
      hueCount.map(count => if (count > totalPixels / (2 << binBits)) 1 else 0) ++
      satCount.map(count => if (count > totalPixels / (2 << binBits)) 1 else 0) ++
      valCount.map(count => if (count > totalPixels / (2 << binBits)) 1 else 0) ++
      Array(if (blackRatio > 0.2) 1 else 0, if (whiteRatio > 0.2) 1 else 0)
    )
    
    bitsToHexString(bits)
  }
  
  // Converte RGB para HSV
  private def rgbToHsv(r: Int, g: Int, b: Int): (Double, Double, Double) = {
    val rf = r / 255.0
    val gf = g / 255.0
    val bf = b / 255.0
    
    val cmax = rf.max(gf).max(bf)
    val cmin = rf.min(gf).min(bf)
    val delta = cmax - cmin
    
    // Cálculo de H (matiz)
    val h = if (delta == 0) {
      0.0
    } else if (cmax == rf) {
      ((gf - bf) / delta) % 6.0
    } else if (cmax == gf) {
      (bf - rf) / delta + 2.0
    } else {
      (rf - gf) / delta + 4.0
    }
    
    val hue = h / 6.0  // Normalizar para [0,1]
    
    // Cálculo de S (saturação)
    val saturation = if (cmax == 0) 0.0 else delta / cmax
    
    // Cálculo de V (valor)
    val value = cmax
    
    (hue, saturation, value)
  }

  def wHash(image: BufferedImage, size: Int = 8): String = {
    // Redimensionar para potência de 2 superior ao tamanho final desejado
    val scaledSize = 64 // Precisa ser potência de 2 para wavelet
    val img = resizeAndGrayScale(image, scaledSize, scaledSize)
    
    // Converter para matriz de valores em escala de cinza
    val pixels = Array.ofDim[Double](scaledSize, scaledSize)
    for (y <- 0 until scaledSize; x <- 0 until scaledSize) {
      pixels(y)(x) = getGrayPixel(img, x, y).toDouble
    }
    
    // Aplicar transformada Haar wavelet (uma implementação simplificada)
    val coefficients = haarWavelet(pixels)
    
    // Usar coeficientes de baixa frequência para o hash
    val lowFreqCoeffs = Array.ofDim[Double](size * size)
    var idx = 0
    for (y <- 0 until size; x <- 0 until size) {
      lowFreqCoeffs(idx) = coefficients(y)(x)
      idx += 1
    }
    
    // Calcular média dos coeficientes (excluindo o primeiro - DC)
    val mean = (lowFreqCoeffs.sum - lowFreqCoeffs(0)) / (lowFreqCoeffs.length - 1)
    
    // Gerar hash: 1 se coeficiente > média, 0 caso contrário
    val bits = lowFreqCoeffs.map(c => if (c > mean) 1 else 0)
    
    bitsToHexString(bits)
  }
  
  // Implementação simplificada da transformada Haar wavelet
  private def haarWavelet(data: Array[Array[Double]]): Array[Array[Double]] = {
    val n = data.length
    val result = Array.ofDim[Double](n, n)
    
    // Copiar dados iniciais
    for (y <- 0 until n; x <- 0 until n) {
      result(y)(x) = data(y)(x)
    }
    
    // Aplicar wavelet horizontalmente e depois verticalmente
    var size = n
    while (size > 1) {
      val half = size / 2
      
      // Horizontalmente
      for (y <- 0 until size; x <- 0 until half) {
        val even = result(y)(2*x)
        val odd = result(y)(2*x + 1)
        
        result(y)(x) = (even + odd) / 2.0          // Média (baixa frequência)
        result(y)(x + half) = (even - odd) / 2.0   // Diferença (alta frequência)
      }
      
      // Verticalmente
      for (y <- 0 until half; x <- 0 until size) {
        val even = result(2*y)(x)
        val odd = result(2*y + 1)(x)
        
        result(y)(x) = (even + odd) / 2.0          // Média (baixa frequência)
        result(y + half)(x) = (even - odd) / 2.0   // Diferença (alta frequência)
      }
      
      size = half
    }
    
    result
  }
}


object VideoHashingSystem {
  // Configurações
  val frameSamplingRate = 1  // Capturar 1 frame a cada x segundos
  val similarityThreshold = 5  // Limiar de distância de Hamming para considerar similares
  val minMatchingFrames = 0.7  // Porcentagem mínima de frames correspondentes
  val frameInterval: Int = 1000 
  
  // Função principal para comparar dois vídeos
  def compareVideos(video1Path: String, video2Path: String, hashAlgorithm: String = "dhash"): Double = {
    // Extrair hashes de frames dos vídeos
    val video1Hashes = extractVideoHashes(video1Path)
    val video2Hashes = extractVideoHashes(video2Path)
    
    // Comparar sequências de hashes (implementação simplificada)
    val matchCount = findMatchingFrames(video1Hashes, video2Hashes)
    
    // Calcular pontuação de similaridade
    val similarityScore = matchCount.toDouble / Math.min(video1Hashes.length, video2Hashes.length)
    
    similarityScore
  }
  
  def calculateAllHashes(frame: BufferedImage): Map[String, String] = {
    Map(
      "dhash" -> ImageHash.dHash(frame),
      "phash" -> ImageHash.pHash(frame),
      "avghash" -> ImageHash.aHash(frame),
      "whash" -> ImageHash.wHash(frame),
      "colorhash" -> ImageHash.colorHash(frame),
      "crophash" -> ImageHash.cropResistantHash(frame),
    )
  }

  def extractVideoHashes(videoPath: String): List[Map[String, Any]] = {
    val grabber = new FFmpegFrameGrabber(new File(videoPath))
    val hashes = ListBuffer[Map[String, String]]()
    val converter = new Java2DFrameConverter()
    
    try {
      grabber.start()
      // Duração total do vídeo em milissegundos
      val durationMs = grabber.getLengthInTime()
      var currentTime = 0L
      while (currentTime <= durationMs) {
        // Posiciona no tempo desejado
        grabber.setTimestamp(currentTime, true)
        
        // Captura o frame
        val frame = grabber.grab()
        if (frame != null && frame.image != null) {
          // Converte para BufferedImage
          val image: BufferedImage = converter.convert(frame)
          
          // Calcula hashes usando implementações anteriores
          var hashesMap = calculateAllHashes(image)
          val aux = Map("frame_order" -> (currentTime / frameInterval).toString)
          hashesMap = aux ++ hashesMap 
          
          hashes += hashesMap
        }
        
        currentTime += frameInterval
      }
    } catch {
      case e: Exception =>
        println(s"Erro ao processar vídeo: ${e.getMessage}")
    } finally {
      grabber.stop()
      converter.close()
    }
    
    hashes.toList
  }

  // Encontra o número de frames correspondentes entre dois vídeos
  private def findMatchingFrames(hashes1: List[Map[String,Any]], hashes2: List[Map[String,Any]]): Int = {
    var matchCount = 0
    
    // Para cada hash no primeiro vídeo
    for (hash1 <- hashes1) {
      // Verificar se existe um hash correspondente no segundo vídeo
      val hasMatch = hashes2.exists(hash2 => 
        ImageHash.hammingDistance(hash1, hash2) <= similarityThreshold
      )
      
      if (hasMatch) {
        matchCount += 1
      }
    }
    
    matchCount
  }
  
  // Função auxiliar para calcular hash para um frame específico
  def calculateFrameHash(frame: BufferedImage, algorithm: String): String = {
    algorithm.toLowerCase match {
      case "dhash" => ImageHash.dHash(frame)
      case "phash" => ImageHash.pHash(frame)
      case "ahash" => ImageHash.aHash(frame)
      case "whash" => ImageHash.wHash(frame)
      case "colorhash" => ImageHash.colorHash(frame)
      case "cropresistant" => ImageHash.cropResistantHash(frame)
      case _ => throw new IllegalArgumentException(s"Algoritmo de hash desconhecido: $algorithm")
    }
  }
}

object VideoHashingDemo {
  def main(args: Array[String]): Unit = {
    val video1Path = "caminho/para/video1.mp4"
    val video2Path = "caminho/para/video2.mp4"
    
    // Comparar usando diferentes algoritmos
    val algorithms = List("dhash", "phash", "ahash", "whash", "colorhash", "cropresistant")
    
    println("Comparação de vídeos:")
    for (algorithm <- algorithms) {
      try {
        val similarityScore = VideoHashingSystem.compareVideos(video1Path, video2Path, algorithm)
        println(f"$algorithm: ${similarityScore * 100}%.2f%% similar")
      } catch {
        case e: Exception =>
          println(s"Erro ao processar com $algorithm: ${e.getMessage}")
      }
    }
  }
}
