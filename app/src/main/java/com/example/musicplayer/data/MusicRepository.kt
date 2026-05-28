package com.example.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 歌词查找策略：
 * 1. 与音乐文件同目录、同文件名但扩展名为 .lrc
 * 2. 同目录下 子目录 lyrics/ 中同文件名 .lrc
 * 3. 应用内部存储 lyrics/ 目录中以 title.lrc 命名
 */
class MusicRepository(private val context: Context) {

    suspend fun fetchLocalMusic(): List<MusicModel> = withContext(Dispatchers.IO) {
        val musicList = mutableListOf<MusicModel>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // 只扫描时长大于 30 秒的音频，过滤掉通知音等
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("30000")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val data = cursor.getString(dataColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                musicList.add(
                    MusicModel(id, title, artist, data, duration, albumId, contentUri)
                )
            }
        }
        musicList
    }

    /**
     * 加载指定音乐的歌词
     * @param music 音乐模型
     * @return 解析后的 Lyrics 对象，如果找不到歌词文件则返回 Lyrics.Empty
     */
    suspend fun loadLyrics(music: MusicModel): Lyrics = withContext(Dispatchers.IO) {
        val lrcFile = findLrcFile(music)
        if (lrcFile != null && lrcFile.exists() && lrcFile.canRead()) {
            val lrcText = lrcFile.readText(charset("UTF-8"))
            if (LyricsParser.isValid(lrcText)) {
                return@withContext LyricsParser.parse(lrcText)
            }
        }
        Lyrics.Empty
    }

    /**
     * 查找与音乐文件对应的 .lrc 歌词文件
     */
    private fun findLrcFile(music: MusicModel): File? {
        val musicFile = File(music.data)
        val musicDir = musicFile.parentFile ?: return null
        val baseName = musicFile.nameWithoutExtension

        // 策略1: 同目录同名 .lrc
        val sameDirLrc = File(musicDir, "$baseName.lrc")
        if (sameDirLrc.exists()) return sameDirLrc

        // 策略2: 同目录下 lyrics/ 子目录
        val lyricsSubDir = File(musicDir, "lyrics")
        val subDirLrc = File(lyricsSubDir, "$baseName.lrc")
        if (subDirLrc.exists()) return subDirLrc

        // 策略3: 应用内部存储 lyrics/title.lrc
        val appLyricsDir = File(context.filesDir, "lyrics")
        val appLrc = File(appLyricsDir, "${music.title}.lrc")
        if (appLrc.exists()) return appLrc

        return null
    }
}
