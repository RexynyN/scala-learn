import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import java.util.concurrent.Executors

object ThreadPool {
  // Define um ExecutionContext com um pool fixo de threads
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  // Função genérica para aplicar paralelamente uma função a um iterável
  def parallelMap[T, R](iterable: Iterable[T], f: T => R): Future[List[R]] = {
    val futures = iterable.map(el => Future { f(el) }) // Cria um Future para cada elemento
    Future.sequence(futures.toList) // Converte a sequência de Futures em um Future de sequência
  }

  def main(args: Array[String]): Unit = {
    val numbers = 1 to 10

    // Função exemplo: dobrar o número
    def double(x: Int): Int = {
      Thread.sleep(500) // Simula um processamento pesado
      x * 2
    }

    val resultFuture = parallelMap(numbers, double)

    resultFuture.onComplete {
      case Success(result) => println(s"Resultado: $result")
      case Failure(ex) => println(s"Erro: ${ex.getMessage}")
    }

    // Aguardar a conclusão (apenas para evitar que o programa termine antes)
    Await.result(resultFuture, 10.seconds)


    // Finalizar o pool de threads corretamente
    ec.asInstanceOf[ExecutionContextExecutorService].shutdown()
  }
}
