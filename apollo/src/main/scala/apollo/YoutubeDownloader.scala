package apollo 

import scala.sys.process._

object YoutubeDownloader {
    def audioFromName(query: String, format: String, filename: String): String = {
        if (query.isEmpty || query == null) 
            return ""

        val finalFormat = if(format.isEmpty || format == null) "wav" else format
        var cmd = ""
        if (filename.isEmpty)
            cmd = s"yt-dlp -f bestaudio --extract-audio --audio-format $finalFormat --audio-quality 0 \'ytsearch:$query\'"
        else 
            cmd = s"yt-dlp -f bestaudio --extract-audio --audio-format $finalFormat --audio-quality 0 \'ytsearch:$query\' -o $filename"
        
        cmd.!!
    }

    def audioFromURL(url: String, format: String, filename: String): String = {
        if (url.isEmpty || url == null) 
            return ""
        
        val finalFormat = if(format.isEmpty || format == null) "wav" else format
        var cmd = ""
        if (filename.isEmpty)
            cmd = s"yt-dlp -f bestaudio --extract-audio --audio-format $finalFormat --audio-quality 0 \'$url\'"
        else 
            cmd = s"yt-dlp -f bestaudio --extract-audio --audio-format $finalFormat --audio-quality 0 \'$url\' -o $filename"
        
        cmd.!!
    }

    def videoFromName(query: String, format: String, filename: String): String = {
        if (query.isEmpty || query == null) 
            return ""
        
        val finalFormat = if(format.isEmpty || format == null) "mp4" else format
        var cmd = ""
        if (filename.isEmpty)
            cmd = s"yt-dlp -f bestvideo+bestaudio --merge-output-format $finalFormat \'ytsearch:$query\'"
        else 
            cmd = s"yt-dlp -f bestvideo+bestaudio --merge-output-format $finalFormat \'ytsearch:$query\' -o $filename"
        
        cmd.!!
    }

    def videoFromURL(url: String, format: String, filename: String): String = {
        if (url.isEmpty || url == null) 
            return ""

        val finalFormat = if(format.isEmpty || format == null) "mp4" else format
        var cmd = ""
        if (filename.isEmpty)
            cmd = s"yt-dlp -f bestvideo+bestaudio --merge-output-format $finalFormat \'$url\'"
        else 
            cmd = s"yt-dlp -f bestvideo+bestaudio --merge-output-format $finalFormat \'$url\' -o $filename"
        
        cmd.!!
    }
}