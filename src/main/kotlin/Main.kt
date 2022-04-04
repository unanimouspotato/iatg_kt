import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore

fun main(args: Array<String>) {
    println("Starting...")
    runBlocking {
        startBot()
    }
}

fun startBot() {
    val coroutineScope = CoroutineScope(Dispatchers.Main)
    val commands = SnipCommand.values().map { it.command }
        .plus(TextCommand.values().map { it.command }).map { "${it}_beta" }
    bot {
        token = System.getenv("TG_BOT_TOKEN")
        dispatch {
            commands.forEach { cmd ->
                command(cmd) {
                    val unbetaCmd = cmd.split("_beta")[0]
                    coroutineScope.runCommand(unbetaCmd, args, message, bot)
                }
            }
        }
    }.startPolling()
}

val jobLimiterSemaphore = Semaphore(2)

fun CoroutineScope.runCommand(command: String, args: List<String>, message: Message, bot: Bot) {
    launch(Dispatchers.IO) {
        if (!jobLimiterSemaphore.tryAcquire()) {
            bot.sendMessage(ChatId.fromId(message.chat.id), text = "yo chill out lmao!")
        } else {
            handleCommand(command, args, message, bot)
            jobLimiterSemaphore.release()
        }
    }
}

fun handleCommand(commandString: String, args: List<String>, message: Message, bot: Bot) {
    val command = getCommand(commandString)
    val chatId = ChatId.fromId(message.chat.id)
    when (command) {
        is SnipCommand -> handleSnipCommand(command, args, message, bot)
        is TextCommand -> bot.sendMessage(chatId, text = command.output)
        else -> bot.sendMessage(chatId, text = "Unknown command :(")
    }
}
