package com.example.musicplayer.ui

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.Lyrics
import com.example.musicplayer.data.MusicModel
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 播放模式枚举
 * 顺序播放 → 单曲循环 → 列表循环 → 随机播放 → 顺序播放
 */
enum class RepeatMode {
    /** 顺序播放：播完列表即停止 */
    SEQUENTIAL,
    /** 单曲循环：反复播放当前歌曲 */
    SINGLE_LOOP,
    /** 列表循环：播完列表后从头重新开始 */
    LIST_LOOP,
    /** 随机播放：列表循环 + 随机顺序 */
    SHUFFLE
}

class MainViewModel(private val repository: MusicRepository) : ViewModel() {

    private val _musicList = MutableStateFlow<List<MusicModel>>(emptyList())
    val musicList: StateFlow<List<MusicModel>> = _musicList

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentMusic = MutableStateFlow<MusicModel?>(null)
    val currentMusic: StateFlow<MusicModel?> = _currentMusic

    private val _lyrics = MutableStateFlow(Lyrics.Empty)
    val lyrics: StateFlow<Lyrics> = _lyrics

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _repeatMode = MutableStateFlow(RepeatMode.SEQUENTIAL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // 位置更新轮询 Job
    private var positionUpdateJob: kotlinx.coroutines.Job? = null

    fun loadMusic() {
        viewModelScope.launch {
            _musicList.value = repository.fetchLocalMusic()
        }
    }

    fun initController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startPositionUpdates()
                    } else {
                        stopPositionUpdates()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // 当歌曲切换时（自动或手动），更新当前播放的音乐信息并加载歌词
                    val index = mediaController?.currentMediaItemIndex ?: -1
                    if (index != -1 && index < _musicList.value.size) {
                        val music = _musicList.value[index]
                        _currentMusic.value = music
                        loadLyrics(music)
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun playMusic(music: MusicModel) {
        mediaController?.let { controller ->
            val index = _musicList.value.indexOf(music)
            if (index != -1) {
                // 将整个列表设置为播放队列
                val mediaItems = _musicList.value.map { MediaItem.fromUri(it.contentUri) }
                controller.setMediaItems(mediaItems, index, 0L)
                controller.prepare()
                controller.play()
                _currentMusic.value = music
                loadLyrics(music)
            }
        }
    }

    fun playNext() {
        mediaController?.seekToNext()
    }

    fun playPrevious() {
        mediaController?.seekToPrevious()
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    /**
     * 循环切换播放模式：顺序 → 单曲循环 → 列表循环 → 随机 → 顺序
     */
    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.SEQUENTIAL -> RepeatMode.SINGLE_LOOP
            RepeatMode.SINGLE_LOOP -> RepeatMode.LIST_LOOP
            RepeatMode.LIST_LOOP -> RepeatMode.SHUFFLE
            RepeatMode.SHUFFLE -> RepeatMode.SEQUENTIAL
        }
        _repeatMode.value = nextMode
        applyRepeatMode(nextMode)
    }

    /**
     * 将播放模式应用到 MediaController
     */
    private fun applyRepeatMode(mode: RepeatMode) {
        mediaController?.let { controller ->
            when (mode) {
                RepeatMode.SEQUENTIAL -> {
                    controller.repeatMode = Player.REPEAT_MODE_OFF
                    controller.shuffleModeEnabled = false
                }
                RepeatMode.SINGLE_LOOP -> {
                    controller.repeatMode = Player.REPEAT_MODE_ONE
                    controller.shuffleModeEnabled = false
                }
                RepeatMode.LIST_LOOP -> {
                    controller.repeatMode = Player.REPEAT_MODE_ALL
                    controller.shuffleModeEnabled = false
                }
                RepeatMode.SHUFFLE -> {
                    controller.repeatMode = Player.REPEAT_MODE_ALL
                    controller.shuffleModeEnabled = true
                }
            }
        }
    }

    private fun loadLyrics(music: MusicModel) {
        _currentLyricIndex.value = -1
        _currentPositionMs.value = 0L
        viewModelScope.launch {
            _lyrics.value = repository.loadLyrics(music)
        }
    }

    /**
     * 启动播放位置轮询，定期更新当前歌词行索引
     */
    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        val pos = controller.currentPosition
                        _currentPositionMs.value = pos
                        _currentLyricIndex.value = _lyrics.value.findCurrentLineIndex(pos)
                    }
                }
                kotlinx.coroutines.delay(100) // 每100ms更新一次
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
