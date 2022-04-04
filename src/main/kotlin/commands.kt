enum class TextCommand(val command: String, val output: String) {
    HELP("help", "hotsnip/hotmp4 args: <URL> <mm:ss or hh:mm:ss> <num seconds to snip>");

    companion object {
        fun fromValue(value: String): TextCommand? = values().associateBy { it.command }[value]
    }
}

enum class SnipCommand(val command: String, val outputFormat: YoutubeDL.OutputFormat) {
    HOTSNIP("hotsnip", YoutubeDL.OutputFormat.MP3),
    HOTMP4("hotmp4", YoutubeDL.OutputFormat.MP4);

    companion object {
        fun fromValue(value: String): SnipCommand? = values().associateBy { it.command }[value]
    }
}

// TODO: Use a sealed class or something else for this
fun getCommand(command: String): Any? = SnipCommand.fromValue(command) ?: TextCommand.fromValue(command)