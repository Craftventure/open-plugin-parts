package net.craftventure.core.utils

import com.google.gson.Gson
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object GsonUtils {
    @Throws(FileNotFoundException::class)
    fun read(file: File?): String {
        val descriptorScanner = Scanner(file).useDelimiter("\\Z")
        val content = descriptorScanner.next()
        descriptorScanner.close()
        return content
    }

    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun <T> read(gson: Gson, file: File?, clazz: Class<T>?): T {
        return gson.fromJson(read(file), clazz)
    }

    @Throws(IOException::class)
    fun <T> readUtf(gson: Gson, file: File, clazz: Class<T>?): T {
        return gson.fromJson(
            java.lang.String.join(
                "\n",
                Files.readAllLines(Paths.get(file.path))
            ), clazz
        )
    }
}