/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright © 2020 Skyline Team and Contributors (https://github.com/skyline-emu/)
 */
package emu.skyline

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object KeyReader {
    private val Tag = KeyReader::class.java.simpleName

    enum class ImportResult {
        Success,
        InvalidInputPath,
        InvalidKeys,
        DeletePreviousFailed,
        MoveFailed,
    }

    enum class KeyType(val keyName: String, val fileName: String) {
        Title("title_keys", "title.keys"), Prod("prod_keys", "prod.keys");

        companion object {
            fun parse(keyName: String) = values().first { it.keyName == keyName }
            fun parse(file: File) = values().first { it.fileName == file.name }
            fun parseOrNull(file: File) = values().find { it.fileName == file.name }
        }
    }

    fun importFromLocation(context: Context, searchLocation: String) =
        importFromDirectory(File(searchLocation), context)

    private fun importFromDirectory(directory: File, context: Context) {
        directory.listFiles()?.forEach { file ->
            if (!file.isDirectory) {
                KeyType.parseOrNull(file)?.let { import(file, it, context) }
            }
        }
    }

    /**
     * Reads keys file, trims and writes to internal app data storage, it makes sure file is properly formatted
     */
    fun import(file: File, keyType: KeyType, context: Context): ImportResult {
        Log.i(Tag, "Parsing ${keyType.name} ${file.absolutePath}")

        if (!file.exists())
            return ImportResult.InvalidInputPath

        val outputFile = File(context.filesDir, keyType.fileName)
        val tmpOutputFile = File(outputFile.absolutePath + ".tmp")
        var valid = false

        context.assets.open(keyType.fileName).bufferedReader().useLines { lines ->
            tmpOutputFile.bufferedWriter().use { writer ->
                valid = lines.all { line ->
                    if (line.startsWith(";") || line.isBlank()) return@all true

                    val pair = line.split("=")
                    if (pair.size != 2)
                        return@all false

                    val key = pair[0].trim()
                    val value = pair[1].trim()
                    when (keyType) {
                        KeyType.Title -> {
                            if (key.length != 32 && !isHexString(key))
                                return@all false
                            if (value.length != 32 && !isHexString(value))
                                return@all false
                        }
                        KeyType.Prod -> {
                            if (!key.contains("_"))
                                return@all false
                            if (!isHexString(value))
                                return@all false
                        }
                    }

                    writer.append("$key=$value\n")
                    true
                }
            }
        }

        val cleanup = {
            try {
                tmpOutputFile.delete()
            } catch (_: Exception) {
            }
        }

        if (!valid) {
            cleanup()
            return ImportResult.InvalidKeys
        }

        FileOutputStream(outputFile).use { output ->
            tmpOutputFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        if (!tmpOutputFile.delete()) {
            cleanup()
            return ImportResult.MoveFailed
        }

        return ImportResult.Success
    }

    private fun isHexString(str: String): Boolean {
        for (c in str)
            if (!(c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F')) return false
        return true
    }
}