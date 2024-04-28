package net.craftventure.bukkit.ktx

import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.arguments.parser.ParserParameters
import cloud.commandframework.arguments.parser.StandardParameters
import cloud.commandframework.bukkit.CloudBukkitCapabilities
import cloud.commandframework.execution.CommandExecutionCoordinator
import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext
import cloud.commandframework.extra.confirmation.CommandConfirmationManager
import cloud.commandframework.meta.CommandMeta
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler
import cloud.commandframework.minecraft.extras.MinecraftHelp
import cloud.commandframework.paper.PaperCommandManager
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.logging.logcat
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.function.UnaryOperator


object MainCommandManager {
    lateinit var manager: PaperCommandManager<CommandSender>
    lateinit var confirmationManager: CommandConfirmationManager<CommandSender>
    lateinit var bukkitAudiences: BukkitAudiences
    lateinit var annotationParser: AnnotationParser<CommandSender>
    lateinit var minecraftHelp: MinecraftHelp<CommandSender>

    fun init(plugin: Plugin = PluginProvider.getInstance()) {
        manager = PaperCommandManager(
            plugin,
            CommandExecutionCoordinator.simpleCoordinator(),
            UnaryOperator.identity(),
            UnaryOperator.identity(),
        )

        bukkitAudiences = BukkitAudiences.create(plugin)

        this.minecraftHelp = MinecraftHelp(
            "/example help",
            { sender -> bukkitAudiences.sender(sender) },
            manager
        )

//        val audience = BukkitAudiences.create(this)

        if (this.manager.hasCapability(CloudBukkitCapabilities.BRIGADIER)) {
            try {
                this.manager.registerBrigadier();
            } catch (e: Exception) {
                e.printStackTrace()
                logcat(priority = LogPriority.WARN) { "Failed to initialised brigadier: ${e.message}" }
            }
        }

        if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions()
        }

        this.confirmationManager = CommandConfirmationManager(
            30L,
            TimeUnit.SECONDS,
            { context: CommandPostprocessingContext<CommandSender> ->
                context.commandContext.sender.sendMessage(
                    CVTextColor.serverNoticeAccent + "Confirmation required. Confirm using /example confirm."
                )
            },
            { sender: CommandSender ->
                sender.sendMessage(
                    CVTextColor.serverError + "You don't have any pending commands."
                )
            }
        )

        this.confirmationManager.registerConfirmationProcessor(this.manager)

        val commandMetaFunction: Function<ParserParameters, CommandMeta> = Function { params ->
            CommandMeta.simple()
                .with(CommandMeta.DESCRIPTION, params.get(StandardParameters.DESCRIPTION, "No description"))
                .build()
        }
        this.annotationParser = AnnotationParser(
            manager,
            CommandSender::class.java,
            commandMetaFunction
        )

        MinecraftExceptionHandler<CommandSender>()
            .withInvalidSyntaxHandler()
            .withInvalidSenderHandler()
            .withNoPermissionHandler()
            .withArgumentParsingHandler()
            .withCommandExecutionHandler()
//            .withDecorator { component ->
//                text()
//                    .append(text("[", NamedTextColor.DARK_GRAY))
//                    .append(text("Example", NamedTextColor.GOLD))
//                    .append(text("] ", NamedTextColor.DARK_GRAY))
//                    .append(component).build()
//            }
            .apply(this.manager, this.bukkitAudiences::sender)
    }

    abstract class CraftventureCommand(val plugin: Plugin) {
        abstract fun register()
    }
}