package com.example.musicplayer.data

import android.net.Uri

data class MusicModel(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String, // 文件路径
    val duration: Long,
    val albumId: Long,
    val contentUri: Uri
)
