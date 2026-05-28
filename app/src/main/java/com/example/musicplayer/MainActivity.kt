package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.musicplayer.data.Lyrics
import com.example.musicplayer.data.MusicModel
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadMusic()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 简单手动实例化 (实际项目建议用 Hilt)
        val repository = MusicRepository(applicationContext)
        viewModel = MainViewModel(repository)
        viewModel.initController(this)

        checkAndRequestPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayerScreen(viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadMusic()
            }

            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
fun MusicPlayerScreen(viewModel: MainViewModel) {
    val musicList by viewModel.musicList.collectAsState()
    val currentMusic by viewModel.currentMusic.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    // 是否展开歌词面板
    var showLyrics by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "我的音乐",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(musicList) { _, music ->
                MusicItem(
                    music = music,
                    isCurrent = music == currentMusic,
                    onClick = { viewModel.playMusic(music) }
                )
            }
        }

        // 底部播放控制条
        if (currentMusic != null) {
            BottomControlBar(
                music = currentMusic!!,
                isPlaying = isPlaying,
                hasLyrics = lyrics.lines.isNotEmpty(),
                showLyrics = showLyrics,
                onTogglePlay = { viewModel.togglePlayPause() },
                onToggleLyrics = { showLyrics = !showLyrics },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() }
            )
        }
    }

    // 歌词面板（覆盖在主界面上方）
    if (currentMusic != null && showLyrics) {
        LyricsOverlay(
            music = currentMusic!!,
            lyrics = lyrics,
            currentLyricIndex = currentLyricIndex,
            isPlaying = isPlaying,
            onTogglePlay = { viewModel.togglePlayPause() },
            onPrevious = { viewModel.playPrevious() },
            onNext = { viewModel.playNext() },
            onClose = { showLyrics = false }
        )
    }
}

@Composable
fun MusicItem(music: MusicModel, isCurrent: Boolean, onClick: () -> Unit) {
    val textColor = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = music.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
            Text(text = music.artist, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControlBar(
    music: MusicModel,
    isPlaying: Boolean,
    hasLyrics: Boolean,
    showLyrics: Boolean,
    onTogglePlay: () -> Unit,
    onToggleLyrics: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = music.title, fontSize = 14.sp, maxLines = 1)
                Text(text = music.artist, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 歌词按钮
                if (hasLyrics) {
                    IconButton(onClick = onToggleLyrics) {
                        Icon(
                            imageVector = if (showLyrics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "歌词"
                        )
                    }
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一首"
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying)
                            Icons.Default.Close
                        else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一首"
                    )
                }
            }
        }
    }
}

/**
 * 歌词全屏覆盖面板
 */
@Composable
fun LyricsOverlay(
    music: MusicModel,
    lyrics: Lyrics,
    currentLyricIndex: Int,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部：歌曲信息 + 关闭按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = music.artist,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "关闭歌词"
                    )
                }
            }

            // 中部：歌词滚动区域
            if (lyrics.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无歌词",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LyricsScroller(
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    modifier = Modifier.weight(1f)
                )
            }

            // 底部：播放控制
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

/**
 * 歌词滚动组件，自动滚动到当前播放行并高亮
 */
@Composable
fun LyricsScroller(
    lyrics: Lyrics,
    currentLyricIndex: Int,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 当前行变化时自动滚动，将当前行滚动到屏幕中央偏上位置
    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex >= 0) {
            // 计算目标位置，将当前歌词行显示在视口约 1/3 处
            val targetIndex = (currentLyricIndex - 3).coerceAtLeast(0)
            if (listState.firstVisibleItemIndex != targetIndex ||
                kotlin.math.abs(listState.firstVisibleItemScrollOffset) > 50
            ) {
                listState.animateScrollToItem(
                    index = targetIndex
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 200.dp) // 上下留白，让首尾行能滚到中间
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isCurrentLine = index == currentLyricIndex
            val scale by animateFloatAsState(
                targetValue = if (isCurrentLine) 1.15f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "lyricScale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isCurrentLine) 1f else 0.45f,
                animationSpec = tween(durationMillis = 300),
                label = "lyricAlpha"
            )

            Text(
                text = line.text,
                fontSize = 18.sp,
                fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentLine) MaterialTheme.colorScheme.primary else Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 10.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
    }
}
