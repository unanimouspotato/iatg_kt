import okio.*
import okio.Path.Companion.toPath
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object YoutubeDL {

    fun download(youtubeDLArgs: YoutubeDLArgs): File? = download(
        youtubeDLArgs.url.toString(),
        youtubeDLArgs.startTimeSecs,
        youtubeDLArgs.duration,
        youtubeDLArgs.outputFormat,
    )

    private fun download(
        url: String,
        startSecs: Long,
        duration: Long,
        outputFormat: OutputFormat = OutputFormat.MP4
    ): File? {
        val keyframeBuffer = 30
        println("startSecs $startSecs")

        val bufferedStartSecs = (startSecs - keyframeBuffer).coerceAtLeast(0)
        val outputStartSecs = (bufferedStartSecs - startSecs).absoluteValue
        val bufferDurationSecs = outputStartSecs + duration

        val fileFormat = "%(title)s_${startSecs}_$duration.${outputFormat.extension}"
        val cmd = listOf(
            "yt-dlp",
            "-f",
            "mp4",
            "--recode",
            outputFormat.extension,
            "--external-downloader",
            "ffmpeg",
            "--external-downloader-args",
            "ffmpeg_i:-ss $bufferedStartSecs -t $bufferDurationSecs",
            "--external-downloader-args",
            outputFormat.args(outputStartSecs),
            "-o",
            fileFormat,
            url,
        )
        println("Running command: ${cmd.joinToString(" ")}")
        ProcessBuilder(*cmd.toTypedArray())
            .redirectErrorStream(true)
            .start()
            .printOutput()
            .waitFor()
        val filename = getFilename(fileFormat, url)
        val exists = filename?.let { FileSystem.SYSTEM.exists(it.toPath()) }
        println("exists? $exists")
        return filename?.let { File(it) }
    }

    private fun Process.printOutput(): Process {
        while (true) {
            val line = inputStream.source().buffer().readUtf8Line() ?: break
            println(line)
        }
        return this
    }

    private fun getFilename(fileFormat: String, url: String) = ProcessBuilder(
        "yt-dlp",
        "--get-filename",
        "-o",
        fileFormat,
        url
    ).start()
        .inputStream
        .source()
        .buffer()
        .readUtf8Line()

    enum class OutputFormat(val extension: String) {
        MP3("mp3"),
        MP4("mp4");
//        GIF("gif"); TODO Implement GIF support

        fun args(startSecs: Long) = when (this) {
            MP3 -> "ffmpeg_o:-ss $startSecs -map_metadata:g -1"
            MP4 -> "ffmpeg_o:-acodec copy -vcodec libx264 -preset veryfast -ss $startSecs -map_metadata:g -1"
        }
    }
}

fun String.formatToSeconds(): Long {
    // Supports hh:mm:ss or mm:ss
    val regex = Regex("^[0-9]+:[0-9]+(:[0-9]+)?$")
    if (!regex.matches(this)) return -1
    val parts = this.split(":").map { it.toLong() }
    return when (parts.size) {
        2 -> TimeUnit.MINUTES.toSeconds(parts[0]) + parts[1]
        3 -> TimeUnit.HOURS.toSeconds(parts[0]) + TimeUnit.MINUTES.toSeconds(parts[1]) + parts[2]
        else -> -1
    }
}