package com.example.musicplayer.data

/**
 * 单行歌词
 * @param timeMs 该行歌词的起始时间（毫秒）
 * @param text 歌词文本内容
 */
data class LyricsLine(
    val timeMs: Long,
    val text: String
)

/**
 * 歌词数据
 * @param lines 按时间排序的歌词行列表
 */
data class Lyrics(
    val lines: List<LyricsLine>
) {
    /**
     * 根据当前播放进度查找当前应高亮的歌词行索引
     * 返回最后一行 timeMs <= positionMs 的索引，如果没有匹配则返回 -1
     */
    fun findCurrentLineIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        // 二分查找：找到最后一个 timeMs <= positionMs 的行
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    companion object {
        val Empty = Lyrics(emptyList())
    }
}
