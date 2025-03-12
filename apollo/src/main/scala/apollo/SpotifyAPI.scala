package apollo 

import sttp.client4.quick._
import sttp.client4.Response
import sttp.model.Uri
import os._
import sttp.client4.DefaultSyncBackend
import java.util.UUID

// 1. Pegar o id de um artista
// 2. Pegar todos os albuns de um artista
// 3. Pegar todas as tracks de um album


// class SpotifyAPI(val client: String, val secret: String) {
object SpotifyAPI {
    var spotifyClient: String = ""
    var spotifySecret: String = ""   
    var spotifyToken: String = ""

    // Default request backend
    val requestBackend = DefaultSyncBackend()

    // A map that has the path to all subdirectories for the jsons files that will be created 
    val jsonPaths = Map(
        "albums" -> os.pwd / "data/albums",
        "tracks" -> os.pwd / "data/tracks",
        "searchArtist" -> os.pwd / "data/searchArtist",
    )

    private def saveJson(path: os.Path, json: ujson.Value): Unit = {
        os.write(path, ujson.write(json), createFolders = true)
    }
    // private def readJson(path: String): ujson.Value = {
    //     ujson.read(os.read(os.pwd / "raw.json"))
    // }

    def refreshToken(): String = {
        val body = s"grant_type=client_credentials&client_id=$spotifyClient&client_secret=$spotifySecret"
        val response: Response[String] = quickRequest
            .body(body)
            .post(uri"https://accounts.spotify.com/api/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .send(requestBackend)

        val json = ujson.read(response.body)

        spotifyToken = json("access_token").str
        json("access_token").str
    }
    
    def searchForArtistId(artistName: String): String = {
        val queryParams = Map(
            "type" -> "artist",
            "q" -> s"artist:$artistName",
            "limit" -> "1"
        )
        val url = uri"https://api.spotify.com/v1/search?$queryParams"
        val request = quickRequest
            .get(url)
            .auth.bearer(spotifyToken)

        val response: Response[String] = request.send(requestBackend)
        response.code match {
            case code if code.isSuccess => println(s"searchForArtist ($artistName): ${response.code} OK")
            case code if code == 401 => 
                println(s"searchForArtist ($artistName): ${response.code} NOK (Trying to refresh token)")
                refreshToken()
                return searchForArtistId(artistName)
            case _ => throw new RuntimeException(s"Erro: Código de resposta ${response.code}")
        }
        
        val json = ujson.read(response.body)
        val salt = UUID.randomUUID().toString.replace("-", "").take(8)

        saveJson(jsonPaths("searchArtist") / s"$artistName-$salt.json", json)
        json("artists")("items")(0)("id").str
    }
    
    def getArtistAlbums(artistId: String): List[String] = {
        val query = Map(
            "include_groups" -> "album,single,compilation,appears_on",
            "limit" -> "50",
            "offset" -> "0"
        )

        var nextUrl = uri"https://api.spotify.com/v1/artists/$artistId/albums?$query".toString
        var albums = List[String]()
        var runNum = 1
        while (nextUrl != null) {
            val request = quickRequest
                .get(uri"$nextUrl")
                .auth.bearer(spotifyToken)

            val response: Response[String] = request.send(requestBackend)
            response.code match {
                case code if code.isSuccess => println(s"getArtistAlbums ($artistId): ${response.code} OK")
                case code if code == 401 => 
                    println(s"getArtistAlbums ($artistId): ${response.code} NOK (Trying to refresh token)")
                    refreshToken()
                    return getArtistAlbums(artistId)
                case _ => throw new RuntimeException(s"Erro: Código de resposta ${response.code}")
            }

            val json = ujson.read(response.body)
            albums = albums ++ json("items").arr.map(item => item("id").str)

            saveJson(jsonPaths("albums") / s"$artistId-$runNum.json", json)
            
            // Checks if the pagination ended (returns a null value in the next field)
            nextUrl = if (json("next").isNull) null else json("next").str
            runNum += 1
        }
        albums
    }

    def getAlbumTracks(albumId: String): List[String] = {
        val query = Map(
            "limit" -> "50",
            "offset" -> "0"
        )

        val url = uri"https://api.spotify.com/v1/albums/$albumId/tracks?$query"
        val request = quickRequest
            .get(url)
            .auth.bearer(spotifyToken)

        val response: Response[String] = request.send(requestBackend)
        response.code match {
            case code if code.isSuccess => println(s"getAlbumTracks ($albumId): ${response.code} OK")
            case code if code == 401 => 
                println(s"getAlbumTracks ($albumId): ${response.code} NOK (Trying to refresh token)")
                refreshToken()
                return getAlbumTracks(albumId)
            case _ => throw new RuntimeException(s"Erro: Código de resposta ${response.code}")
        }

        val json = ujson.read(response.body)
        val tracks = json("items").arr.map(item => {
                item("artists")(0)("name").str + " - " + item("name").str
            }).toList   

        saveJson(jsonPaths("tracks") / s"$albumId.json", json)
        tracks
    }

    def getTracks(ids: List[String]): List[String] = {
        val randomHash = UUID.randomUUID().toString
        var chunkNum = 1

        // Every request can have up to 50 ids, so we chunk the list to send 
        ids.grouped(50).foreach(chunk => {
            val tracks = chunk.mkString(",")
            val url = uri"https://api.spotify.com/v1/tracks?ids=$tracks"
            val request = quickRequest
                .get(url)
                .auth.bearer(spotifyToken)

            val response: Response[String] = request.send(requestBackend)
            response.code match {
                case code if code.isSuccess => println(s"getTracks (Chunk $chunkNum): ${response.code} OK")
                case code if code == 401 => 
                    println(s"getTracks (Chunk $chunkNum): ${response.code} NOK (Trying to refresh token)")
                    refreshToken()
                    return getTracks(ids)
                case _ => throw new RuntimeException(s"Erro: Código de resposta ${response.code}")
            }

            val json = ujson.read(response.body)
            // Create a random hash to name the file
            saveJson(jsonPaths("tracks") / s"$randomHash-$chunkNum.json", json)

            chunkNum += 1
        })
        List()
    }

    def main(args: Array[String]): Unit = {
        spotifyClient = sys.env.get("SPOTIFY_CLIENT")
            .getOrElse(throw new RuntimeException("Cliente do Spotify não encontrado")) 

        spotifySecret = sys.env.get("SPOTIFY_SECRET")
            .getOrElse(throw new RuntimeException("Secret do Spotify não encontrado")) 

        refreshToken()
        val artistId = searchForArtistId("arcane")
        val albums = getArtistAlbums(artistId)
        val tracks = albums.flatMap(getAlbumTracks)

        print(tracks)
    }
}


