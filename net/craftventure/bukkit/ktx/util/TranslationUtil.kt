package net.craftventure.bukkit.ktx.util

import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.ktx.util.Logger
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.*
import java.util.*
import kotlin.system.exitProcess

object TranslationUtil {
    private var translations: Map<String, Map<String, String>> = HashMap()

    fun load(plugin: Plugin) {
        val translations = HashMap<String, Map<String, String>>()

        val files =
            File(plugin.dataFolder, "/translations/").listFiles(FileFilter { it.isFile })
        if (files == null) {
            Logger.severe("Failed to find any translation files")
            exitProcess(0)
        } else {
            for (file in files) {
                val properties = Properties()
                var inputStream: InputStream? = null
                var inputStreamReader: InputStreamReader? = null
                try {
                    inputStream = FileInputStream(file)
                    inputStreamReader = InputStreamReader(inputStream, "UTF8")
                    properties.load(inputStreamReader)
                    val translation = HashMap<String, String>()

                    for (name in properties.stringPropertyNames()) {
                        translation[name] = properties.getProperty(name).replace("''", "'")
                    }

                    translations[file.nameWithoutExtension.replace("language_", "")] = translation
                } catch (e: IOException) {
                    Logger.capture(e)
                } finally {
                    try {
                        inputStreamReader?.close()
                        inputStream?.close()
                    } catch (e: IOException) {
                        Logger.capture(e)
                    }

                }
            }
        }

        TranslationUtil.translations = translations

        val enEnum = StringBuilder()
        for (language in TranslationUtil.translations.keys) {
            val languageTranslations = TranslationUtil.translations[language]
            val keys = ArrayList(languageTranslations!!.keys).sortedBy { it }
            for (key in keys) {
                //                Logger.console(String.format("%s: %s > %s", language, key, languageTranslations.get(key)));
                if ("en" == language) {
                    enEnum.append(key.toUpperCase().replace(".", "_")).append("(\"").append(key).append("\"),\n")
                }
            }
        }

        //        Logger.console(enEnum);
    }

    fun getTranslation(key: String): Component? {
        val locale = "en"
        //        Logger.console("Finding translation for language " + locale);
        var language: Map<String, String>? = translations[locale]
        if (language == null) {
            language = translations["en"]
            Logger.severe("Didn't find EN translation")
        }
        return if (language != null) {
            //            Logger.console("Found " + language.get(key) + " for " + key);
            language[key]?.parseWithCvMessage()
        } else null
    }

    fun getRawTranslation(player: Player?, key: String): String? {
        val localeFull = player?.locale ?: "en_us"
        var locale: String? = localeFull
        var country: String? = null
        try {
            if (locale != null) {
                val localeSplitted = locale.split("_").dropLastWhile { it.isEmpty() }.toTypedArray()
                country = if (localeSplitted.size >= 2) localeSplitted[1] else null
                locale = if (localeSplitted.isNotEmpty()) localeSplitted[0] else null
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }

//        Logger.info("Finding translation for language $localeFull ${TranslationUtil.translations[localeFull] != null} " +
//                "$locale ${TranslationUtil.translations[locale] != null} " +
//                "$country ${TranslationUtil.translations[country] != null} " +
//                "in ${translations.keys.joinToString("|")}")
        val language: Map<String, String>? = translations[localeFull]
            ?: translations[locale] ?: translations[country]
        if (language != null) {
//            Logger.info("Language $key ${language[key]}")
            val translation = language[key]
            if (translation != null)
                return translation
        }
        val translation = translations["en"]?.get(key)
        if (translation != null)
            return translation

//        Logger.info("Language not found $locale and no fallbacks found")
        return null
    }

    fun getTranslation(player: Player?, key: String, vararg params: Any?): Component? {
//        logcat { "Getting ${getRawTranslation(player, key)} (vs ${getRawTranslation(player, key)?.format(*params)} with params ${params})" }
        return getRawTranslation(player, key)?.format(*params)?.parseWithCvMessage()
    }
}
