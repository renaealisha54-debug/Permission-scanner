package com.permissionscanner.app.scanner

import android.content.Context
import android.net.Uri
import java.util.zip.ZipInputStream

data class Finding(
    val file: String,
    val category: String,
    val matches: List<String>,
    val lineNumbers: List<Int>
)

private val PATTERNS = mapOf(
    "permission"  to Regex("""\b(uses-permission|permission)\b""", RegexOption.IGNORE_CASE),
    "network"     to Regex("""\b(INTERNET|CHANGE_NETWORK_STATE|ACCESS_WIFI_STATE)\b""", RegexOption.IGNORE_CASE),
    "camera"      to Regex("""\bCAMERA\b""", RegexOption.IGNORE_CASE),
    "storage"     to Regex("""\b(READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|MANAGE_EXTERNAL_STORAGE)\b""", RegexOption.IGNORE_CASE),
    "location"    to Regex("""\b(ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|ACCESS_BACKGROUND_LOCATION)\b""", RegexOption.IGNORE_CASE),
    "contacts"    to Regex("""\b(READ_CONTACTS|WRITE_CONTACTS)\b""", RegexOption.IGNORE_CASE),
    "phone"       to Regex("""\b(READ_PHONE_STATE|CALL_PHONE|READ_CALL_LOG)\b""", RegexOption.IGNORE_CASE),
    "sms"         to Regex("""\b(SEND_SMS|RECEIVE_SMS|READ_SMS)\b""", RegexOption.IGNORE_CASE),
    "microphone"  to Regex("""\bRECORD_AUDIO\b""", RegexOption.IGNORE_CASE),
    "execute"     to Regex("""\b(Runtime\.exec|ProcessBuilder|loadLibrary|dlopen)\b"""),
    "crypto"      to Regex("""\b(Cipher|KeyStore|SecretKey|MessageDigest)\b"""),
    "reflection"  to Regex("""\b(Class\.forName|getDeclaredMethod|setAccessible)\b"""),
)

private val SCAN_EXTENSIONS = setOf(".xml", ".java", ".kt", ".smali", ".json", ".gradle", ".properties")

fun scanUri(context: Context, uri: Uri): List<Finding> {
    val findings = mutableListOf<Finding>()
    val stream = context.contentResolver.openInputStream(uri) ?: return emptyList()

    ZipInputStream(stream.buffered()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            if (!entry.isDirectory && SCAN_EXTENSIONS.any { name.endsWith(it) }) {
                val text = zip.readBytes().toString(Charsets.UTF_8)
                val lines = text.lines()
                val fileHits = mutableMapOf<String, Pair<MutableSet<String>, MutableList<Int>>>()

                lines.forEachIndexed { idx, line ->
                    for ((category, pattern) in PATTERNS) {
                        val hits = pattern.findAll(line).map { it.value }.toList()
                        if (hits.isNotEmpty()) {
                            val (matches, lineNums) = fileHits.getOrPut(category) { mutableSetOf<String>() to mutableListOf() }
                            matches.addAll(hits)
                            lineNums.add(idx + 1)
                        }
                    }
                }

                for ((category, data) in fileHits) {
                    findings.add(Finding(
                        file = name,
                        category = category,
                        matches = data.first.sorted(),
                        lineNumbers = data.second
                    ))
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return findings
}

fun scanPath(path: String): List<Finding> {
    val findings = mutableListOf<Finding>()
    val file = java.io.File(path)
    if (!file.exists() || !file.canRead()) return emptyList()

    ZipInputStream(file.inputStream().buffered()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            if (!entry.isDirectory && SCAN_EXTENSIONS.any { name.endsWith(it) }) {
                val text = zip.readBytes().toString(Charsets.UTF_8)
                val lines = text.lines()
                val fileHits = mutableMapOf<String, Pair<MutableSet<String>, MutableList<Int>>>()

                lines.forEachIndexed { idx, line ->
                    for ((category, pattern) in PATTERNS) {
                        val hits = pattern.findAll(line).map { it.value }.toList()
                        if (hits.isNotEmpty()) {
                            val (matches, lineNums) = fileHits.getOrPut(category) { mutableSetOf<String>() to mutableListOf() }
                            matches.addAll(hits)
                            lineNums.add(idx + 1)
                        }
                    }
                }

                for ((category, data) in fileHits) {
                    findings.add(Finding(
                        file = name,
                        category = category,
                        matches = data.first.sorted(),
                        lineNumbers = data.second
                    ))
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return findings
}
