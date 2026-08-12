package net.ccbluex.liquidbounce.utils

import org.apache.commons.io.IOUtils
import java.io.*

object FileUtils {
    fun readInputStream(inputStream: InputStream?): String? {
        val stringBuilder = StringBuilder()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) stringBuilder.append(line).append('\n')
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }

    @JvmStatic
    fun unpackFile(file: File, name: String) {
        val fos = FileOutputStream(file)
        IOUtils.copy(FileUtils::class.java.classLoader.getResourceAsStream(name), fos)
        fos.close()
    }

    fun writeFile(str: String, path: String) {
        val file = File(path)
        file.writeText(str, Charsets.UTF_8)
    }
}
