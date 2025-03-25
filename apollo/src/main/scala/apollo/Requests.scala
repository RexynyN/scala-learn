// package apollo 

// import sttp.client3._
// import io.circe.parser._
// import io.circe.Json
// import io.circe.syntax._

// object Requests {
//     val requestsBackend = HttpURLConnectionBackend()

//     def get(url: String, token: Option[String] = None): Either[String, Map[String, Any]] = {
//         val request = basicRequest.get(uri"$url").auth.bearer(token.getOrElse(""))
//         val response = request.send(requestsBackend)

//         response.body match {
//             case Right(jsonString) =>
//                 parse(jsonString) match {
//                     case Right(json) => Right(jsonToMap(json))
//                     case Left(error) => Left(s"Erro ao parsear JSON: $error")
//                 }
//             case Left(error) => Left(s"Erro na requisição: $error")
//         }
//     }

//     def post(url: String, data: Map[String, Any], token: Option[String] = None): Either[String, Map[String, Any]] = {
//         val jsonData = data.asJson.noSpaces
//         val request = basicRequest.post(uri"$url").body(jsonData).auth.bearer(token.getOrElse(""))
//         val response = request.send(requestsBackend)

//         response.body match {
//             case Right(jsonString) =>
//                 parse(jsonString) match {
//                     case Right(json) => Right(jsonToMap(json))
//                     case Left(error) => Left(s"Erro ao parsear JSON: $error")
//                 }
//             case Left(error) => Left(s"Erro na requisição: $error")
//         }
//     }

//     def put(url: String, data: Map[String, Any], token: Option[String] = None): Either[String, Map[String, Any]] = {
//         val jsonData = data.asJson.noSpaces
//         val request = basicRequest.put(uri"$url").body(jsonData).auth.bearer(token.getOrElse(""))
//         val response = request.send(requestsBackend)

//         response.body match {
//             case Right(jsonString) =>
//                 parse(jsonString) match {
//                     case Right(json) => Right(jsonToMap(json))
//                     case Left(error) => Left(s"Erro ao parsear JSON: $error")
//                 }
//             case Left(error) => Left(s"Erro na requisição: $error")
//         }
//     }

//     def delete(url: String, token: Option[String] = None): Either[String, String] = {
//         val request = basicRequest.delete(uri"$url").auth.bearer(token.getOrElse(""))
//         val response = request.send(requestsBackend)

//         response.body match {
//             case Right(_) => Right("Recurso deletado com sucesso")
//             case Left(error) => Left(s"Erro na requisição: $error")
//         }
//     }

//     // Converte JSON para Map[String, Any]
//     def jsonToMap(json: Json): Map[String, Any] = {
//         json.asObject match {
//             case Some(obj) =>
//                 obj.toMap.mapValues {
//                     case j if j.isString  => j.asString.get
//                     case j if j.isNumber  => j.asNumber.get.toDouble
//                     case j if j.isBoolean => j.asBoolean.get
//                     case j if j.isArray   => j.asArray.get.map(jsonToMap).toList
//                     case j if j.isObject  => jsonToMap(j)
//                     case _ => null
//                 }
//             case None => Map.empty
//         }
//     }

//     def main(args: Array[String]): Unit = {
//         println(sys.env)

//         // val apiUrl = "https://jsonplaceholder.typicode.com/todos/1"
//         // get(apiUrl, Some("your_bearer_token")) match {
//         //     case Right(map) => println(s"Resposta da API como Map: $map")
//         //     case Left(error) => println(s"Erro: $error")
//         // }
//     }
// }