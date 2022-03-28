import okio.*
import okio.Path.Companion.toPath
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object YoutubeDL {

    fun download(
        url: String,
        start: String,
        duration: Long,
        outputFormat: OutputFormat = OutputFormat.MP4
    ) {
        val keyframeBuffer = 30
        val startSecs = start.toSeconds()
        println("startSecs $startSecs")

        val bufferStartSecs = (startSecs - keyframeBuffer).coerceAtLeast(0)
        val outputStartSecs = (bufferStartSecs - startSecs).absoluteValue
        val bufferDurationSecs = outputStartSecs + duration

        val fileFormat = "%(title)s.${outputFormat.extension}"
        val cmd = listOf(
            "yt-dlp",
            "-f",
            "mp4",
            "--recode",
            outputFormat.extension,
            "--external-downloader",
            "ffmpeg",
            "--external-downloader-args",
            "ffmpeg_i:-ss $bufferStartSecs -t $bufferDurationSecs",
            "--external-downloader-args",
            "ffmpeg_o:-acodec copy -vcodec libx264 -preset veryfast -ss $outputStartSecs -map_metadata:g -1",
            
            "-o",
            fileFormat,
            url,
        )
        println("Running command: ${cmd.joinToString(" ")}")
        val p = ProcessBuilder(*cmd.toTypedArray())
            .redirectErrorStream(true)
            .start()
        while (true) {
            val line = p.inputStream.source().buffer().readUtf8Line() ?: break
            println(line)
        }
        p.waitFor()
        val filename = ProcessBuilder("yt-dlp", "--get-filename", "-o", fileFormat, url)
            .start()
            .inputStream
            .source()
            .buffer()
            .readUtf8Line()
        val exists = FileSystem.SYSTEM.exists(filename!!.toPath())
        println("$filename exists? $exists")
    }

    fun String.toSeconds(): Long {
        // Supports hh:mm:ss or mm:ss
        val regex = Regex("^[0-9]+:[0-9]+(:[0-9]+)?$")
        if (!regex.matches(this)) return -1
        val parts = this.split(":").map { it.toLong() }
        println("num parts ${parts.size}")
        println("${parts[0]} ${parts[1]}")
        return when (parts.size) {
            2 -> TimeUnit.MINUTES.toSeconds(parts[0]) + parts[1]
            3 -> TimeUnit.HOURS.toSeconds(parts[0]) + TimeUnit.MINUTES.toSeconds(parts[1]) + parts[2]
            else -> -1
        }
    }

    enum class OutputFormat(val extension: String) {
        MP3("mp3"),
        MP4("mp4")
    }
}