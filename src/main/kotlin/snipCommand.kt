import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.TelegramFile
import java.net.MalformedURLException
import java.net.URL

fun handleSnipCommand(snipCommand: SnipCommand, args: List<String>, message: Message, bot: Bot) {
    val chatId = ChatId.fromId(message.chat.id)
    val youtubeDLArgs = YoutubeDLArgs.parse(args, snipCommand.outputFormat)
    if (youtubeDLArgs == null) {
        bot.sendMessage(chatId, text = "arguments: <URL> <mm:ss or hh:mm:ss> <num seconds to snip>")
        return
    }

    val messageId = bot.sendMessage(chatId, text = "One HOT ${snipCommand.outputFormat.extension}, coming right up!")
        .first
        ?.body()
        ?.result
        ?.messageId ?: run {
        println("Error sending message!")
        return
    }

    val file = YoutubeDL.download(youtubeDLArgs)
    if (file == null) {
        bot.editMessageText(chatId, messageId, text = "Cold snip : (")
        return
    }
    val tgFile = TelegramFile.ByFile(file)

    bot.editMessageText(chatId, messageId, text = "Almost done...")

    when (snipCommand) {
        SnipCommand.HOTSNIP -> bot.sendAudio(chatId, tgFile)
        SnipCommand.HOTMP4 -> bot.sendVideo(chatId, tgFile)
    }

    file.delete()
    bot.deleteMessage(chatId, messageId)
}

data class YoutubeDLArgs(
    val url: URL,
    val startTimeSecs: Long = 0L,
    val duration: Long = 0L,
    val outputFormat: YoutubeDL.OutputFormat,
) {
    companion object {
        fun parse(args: List<String>, outputFormat: YoutubeDL.OutputFormat): YoutubeDLArgs? {
            val url = try {
                URL(args.getOrNull(0))
            } catch (e: MalformedURLException) {
                return YoutubeDLArgs(URL("https://www.youtube.com/watch?v=zILpjFqlOak"), 0L, 5L, YoutubeDL.OutputFormat.MP4)
            }
            return YoutubeDLArgs(
                url = url,
                startTimeSecs = args.getOrElse(1) { "0:00" }.formatToSeconds(),
                duration = args.getOrElse(2) { "9999" }.toLongOrNull() ?: 9999L,
                outputFormat = outputFormat
            )
        }
    }
}
