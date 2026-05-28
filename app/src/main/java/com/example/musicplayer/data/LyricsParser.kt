package com.example.musicplayer.data

import java.io.BufferedReader
import java.io.StringReader

/**
 * LRC 格式歌词解析器
 * 支持的格式示例：
 *   [00:12.00]歌词文本
 *   [00:12.00][00:45.30]重复时间标签
 *   [ar:艺术家] 元数据标签（会被忽略）
 */
object LyricsParser {

    // 匹配时间标签，如 [00:12.00] 或 [00:12.001]
    private val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})]""")

    // 元数据标签前缀
    private val metaPrefixes = setOf("ar:", "ti:", "al:", "by:", "offset:", "length:")

    /**
     * 解析 LRC 歌词文本为 Lyrics 对象
     */
    fun parse(lrcText: String): Lyrics {
        val lines = mutableListOf<LyricsLine>()
        val reader = BufferedReader(StringReader(lrcText))

        reader.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachLine

            // 跳过元数据行，如 [ar:Artist]
            val contentInBrackets = trimmed.substringAfter("[", "").substringBefore("]", "")
            if (metaPrefixes.any { contentInBrackets.startsWith(it, ignoreCase = true) }) {
                return@forEachLine
            }

            // 提取所有时间标签
            val timeMatches = timeRegex.findAll(trimmed).toList()
            if (timeMatches.isEmpty()) return@forEachLine

            // 提取歌词文本（去掉所有时间标签后的内容）
            val text = timeRegex.replace(trimmed, "").trim()
            if (text.isEmpty()) return@forEachLine

            // 每个时间标签都对应同一行歌词（处理多时间标签）
            for (match in timeMatches) {
                val (min, sec, ms) = match.destructured
                val timeMs = min.toLong() * 60_000 +
                        sec.toLong() * 1_000 +
                        if (ms.length == 2) ms.toLong() * 10 else ms.toLong()
                lines.add(LyricsLine(timeMs, text))
            }
        }

        // 按时间排序
        lines.sortBy { it.timeMs }
        return Lyrics(lines)
    }

    /**
     * 判断是否为有效的歌词内容（非空且至少有一行带时间的歌词）
     */
    fun isValid(lrcText: String?): Boolean {
        if (lrcText.isNullOrBlank()) return false
        return timeRegex.containsMatchIn(lrcText)
    }
}
