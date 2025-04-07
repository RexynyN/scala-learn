package apollo 


import org.apache.log4j.{Logger, Level}
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import upickle.default._
import sttp.client4.quick._
import sttp.client4.Response
import sttp.model.Uri
import sttp.client4.DefaultSyncBackend
import java.util.UUID
import scala.io.Source
import java.nio.file.FileAlreadyExistsException

case class Artist(
    name: String,
    id: String,
    genres: String,
)

case class Album(
    id: String,
    artist_id: String,
    feats: String, 
    feats_ids: String,
    total_tracks: Int,
    release_date: String,
    name: String,
    album_type: String
)

case class Track(
    id: String,
    explicit: Boolean,
    artist_id: String,
    artist_name: String,
    album_id: String,
    feats: String, 
    feats_ids: String,
    name: String,
    duration_ms: Int,
    track_number: Int,
    track_type: String
)


// class SpotifyAPI(val client: String, val secret: String) {
object SpotifyAPI {
    private var spotifyClient: String = ""
    private var spotifySecret: String = ""   
    private var spotifyToken: String = UUID.randomUUID().toString.replace("-", "").take(8) // Random token to initialize

    implicit val artistRW: ReadWriter[Artist] = macroRW[Artist]
    implicit val trackRW: ReadWriter[Track] = macroRW[Track]
    implicit val albumRW: ReadWriter[Album] = macroRW[Album]


    // Default request backend
    val requestBackend = DefaultSyncBackend()

    // A map that has the path to all subdirectories for the jsons files that will be created 
    val jsonPaths = Map(
        "albums" -> os.pwd / "data/albums",
        "tracks" -> os.pwd / "data/tracks",
        "searchArtist" -> os.pwd / "data/searchArtist",
    )

    private def saveJson(path: os.Path, json: String): Unit = {
        try {
            os.write(path, json, createFolders = true)
        } catch {
            case already: FileAlreadyExistsException =>
                println(s"File $path already exists, skipping.")
            case _: Throwable => println(s"File $path not found")
        }
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

    def artistJson(path: os.Path, json: ujson.Value): Unit = {
        val items = json("artists")("items")(0)
        val data: Artist = Artist(
            items("name").str,
            items("id").str,
            items("genres").arr.map(x => x.str).mkString(", "),
        )
        saveJson(path, upickle.default.write(data))  
    }

    def albumsJson(path: os.Path, json: ujson.Value): Unit = {
        val items = json("items").arr

        val data: List[Album] = items.map(item => {
            val feats = item("artists").arr.tail.arr
            Album(
                item("id").str,
                item("artists").arr(0)("id").str,
                feats.map(x=> x("name").str).mkString(", "),
                feats.map(x=> x("id").str).mkString(", "),
                item("total_tracks").num.toInt,
                item("release_date").str,
                item("name").str,
                item("album_type").str
            )
        }).toList

        saveJson(path, upickle.default.write(data))  
    }


    def trackJson(path: os.Path, json: ujson.Value, include: String=null): Unit = {
        val items = json("items").arr
        val album_id = json("href").str.split("/")(5)
        val raw: List[Track] = items.map(item => {
            val feats = item("artists").arr.tail.arr
            Track(
                item("id").str,
                item("explicit").bool,
                item("artists").arr(0)("id").str,
                item("artists").arr(0)("name").str,
                album_id,
                feats.map(x=> x("name").str).mkString(", "),
                feats.map(x=> x("id").str).mkString(", "),
                item("name").str,
                item("duration_ms").num.toInt,
                item("track_number").num.toInt,
                item("type").str
            )
        }).toList

        val data = raw.filter(x => {
            if (include == null) true
            else x.artist_id.toLowerCase.contains(include.toLowerCase) ||
                x.feats_ids.toLowerCase.contains(include.toLowerCase)
        })
        
        saveJson(path, upickle.default.write(data))  
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

        val response: Response[String] = request.response(asStringAlways).send(requestBackend)
        response.code match {
            case code if code.isSuccess => println(s"searchForArtist ($artistName): ${response.code} OK")
            case code if code.code == 401 => 
                println(s"searchForArtist ($artistName): ${response.code} NOK (Trying to refresh token)")
                refreshToken()
                return searchForArtistId(artistName)
            case code if code.isServerError => 
                println(s"searchForArtist ($artistName): ${response.code} NOK (Sleeping and Trying again)")
                Thread.sleep(1000 * 60)
                refreshToken()
                return searchForArtistId(artistName)
            case _ => throw new RuntimeException(s"Error: Response code ${response.code}\n${response.body}")
        }
        
        val json = ujson.read(response.body)
        val salt = UUID.randomUUID().toString.replace("-", "").take(8)

        artistJson(jsonPaths("searchArtist") / s"$artistName-$salt.json", json)
        json("artists")("items")(0)("id").str
    }
    
    def getArtistAlbums(artistId: String): List[String] = {
        val groups = List(
            "album", 
            "single", 
            // "compilation", 
            "appears_on"
        ).mkString(",")

        val query = Map(
            "include_groups" -> groups,
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

            val response: Response[String] = request.response(asStringAlways).send(requestBackend)
            response.code match {
                case code if code.isSuccess => println(s"getArtistAlbums ($artistId): ${response.code} OK")
                case code if code.code == 401 => 
                    println(s"getArtistAlbums ($artistId): ${response.code} NOK (Trying to refresh token)")
                    refreshToken()
                    return getArtistAlbums(artistId)
                case code if code.isServerError => 
                    println(s"getArtistAlbums ($artistId): ${response.code} NOK (Sleeping and Trying again)")
                    Thread.sleep(1000 * 60)
                    refreshToken()
                    return getArtistAlbums(artistId)
                case _ => throw new RuntimeException(s"Error: Response code ${response.code}\n${response.body}")
            }

            val json = ujson.read(response.body)
            albums = albums ++ json("items").arr.map(item => item("id").str)

            albumsJson(jsonPaths("albums") / s"$artistId-$runNum.json", json)
            
            // Checks if the pagination ended (returns a null value in the next field)
            nextUrl = if (json("next").isNull) null else json("next").str
            runNum += 1
        }
        albums
    }

    def getAlbumTracks(albumId: String, filterArtist: String=null): List[String] = {
        val query = Map(
            "limit" -> "50",
            "offset" -> "0"
        )

        val url = uri"https://api.spotify.com/v1/albums/$albumId/tracks?$query"
        val request = quickRequest
            .get(url)
            .auth.bearer(spotifyToken)

        val response: Response[String] = request.response(asStringAlways).send(requestBackend)
        response.code match {
            case code if code.isSuccess => println(s"getAlbumTracks ($albumId): ${response.code} OK")
            case code if code.code == 401 => 
                println(s"getAlbumTracks ($albumId): ${response.code} NOK (Trying to refresh token)")
                refreshToken()
                return getAlbumTracks(albumId, filterArtist)
            case code if code.isServerError => 
                println(s"getAlbumTracks ($albumId): ${response.code} NOK (Sleeping and Trying again)")
                Thread.sleep(1000 * 60)
                refreshToken()
                return getAlbumTracks(albumId, filterArtist)
            case _ => throw new RuntimeException(s"Error: Response code ${response.code}\n${response.body}")
        }

        val json = ujson.read(response.body)
        val tracks = json("items").arr.map(item => {
                item("artists")(0)("name").str + " - " + item("name").str
            }).toList   

        trackJson(jsonPaths("tracks") / s"$albumId.json", json, include=filterArtist)
        tracks.filter(x => {
            if (filterArtist == null) true
            else x.toLowerCase.contains(filterArtist.toLowerCase)
        })
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

            val response: Response[String] = request.response(asStringAlways).send(requestBackend)
            response.code match {
                case code if code.isSuccess => println(s"getTracks (Chunk $chunkNum): ${response.code} OK")
                case code if code.code == 401 => 
                    println(s"getTracks (Chunk $chunkNum): ${response.code} NOK (Trying to refresh token)")
                    refreshToken()
                    return getTracks(ids)
                case code if code.isServerError => 
                    println(s"getTracks (Chunk $chunkNum): ${response.code} NOK (Sleeping and Trying again)")
                    Thread.sleep(1000 * 60)
                    refreshToken()
                    return getTracks(ids)
                case _ => throw new RuntimeException(s"Error: Response code ${response.code}\n${response.body}")
            }

            val json = ujson.read(response.body)
            // Create a random hash to name the file
            trackJson(jsonPaths("tracks") / s"$randomHash-$chunkNum.json", json)

            chunkNum += 1
        })
        List()
    }

    def main(args: Array[String]): Unit = {
        spotifyClient = sys.env.get("SPOTIFY_CLIENT")
            .getOrElse(throw new RuntimeException("Spotify client not found")) 

        spotifySecret = sys.env.get("SPOTIFY_SECRET")
            .getOrElse(throw new RuntimeException("Spotify secret not found")) 

        val artistsDir = os.list(jsonPaths("searchArtist"))
        val artists = Source.fromFile("artists.playlist").getLines().toList.filter(x => {
            if (artistsDir.exists(_.toString.contains(x.strip()))) {
                println(s"Skipping artist: ${x.strip()} (already processed)")
                false
            } else {
                true
            }
        })

        artists.foreach(artist => {
            if (os.list(jsonPaths("searchArtist")).exists(_.toString.contains(artist.strip()))) {
                println(s"Skipping artist: ${artist.strip()} (already processed)")
                
            }

            val artistId = searchForArtistId(artist.strip())
            val albums = getArtistAlbums(artistId)
            
            var tracks = List[String]()
            albums.foreach(album => {
                tracks ++= getAlbumTracks(album)
                Thread.sleep(1000 * 2) // 2 seconds
            })

            print(tracks)
            Thread.sleep(1000 * 300) // 6 Minutes
        })
    }
}
