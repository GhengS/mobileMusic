# Android 音乐播放器开发文档

## 1. 项目概述
本项目是一个运行在 Android 平台上的本地音乐播放器，支持扫描设备本地音频文件、播放控制（播放、暂停、切歌）、列表管理等功能。

## 2. 技术栈建议
- **开发语言**: Kotlin (1.9.0+)
- **UI 框架**: Jetpack Compose (现代化的声明式 UI)
- **核心引擎**: Jetpack Media3 (包含 ExoPlayer，是 Google 目前推荐的媒体播放解决方案)
- **架构模式**: MVVM (Model-View-ViewModel) + Clean Architecture 思想
- **依赖注入**: Hilt (可选，建议用于大型项目) 或 Koin
- **异步处理**: Kotlin Coroutines & Flow
- **本地存储**: Room (用于存储歌单、收藏状态等)

## 3. 核心功能模块

### 3.1 音乐扫描 (Media Scanner)
- **职责**: 扫描 Android `MediaStore` 数据库，获取设备上的音频文件。
- **关键技术**: `ContentResolver`, `MediaStore.Audio.Media`.

### 3.2 播放服务 (Playback Service)
- **职责**: 在后台运行，管理播放状态，确保切换应用时音乐不中断。
- **关键技术**: `MediaSessionService` (Media3), `ExoPlayer`.

### 3.3 UI 界面 (User Interface)
- **主界面**: 歌曲列表展示。
- **播放详情页**: 封面旋转、进度条控制、歌词显示（可选）。
- **通知栏控制**: 系统级媒体控制。

## 4. 数据流设计
1. **Repository**: 从 MediaStore 获取原始音频数据。
2. **ViewModel**: 负责状态管理（当前播放歌曲、播放状态、播放进度）。
3. **Compose UI**: 观察 ViewModel 中的 State 并进行渲染。

## 5. 权限申请
- `READ_EXTERNAL_STORAGE` (API 32 及以下)
- `READ_MEDIA_AUDIO` (API 33 及以上)
- `FOREGROUND_SERVICE` & `POST_NOTIFICATIONS`

## 6. 核心组件实现参考

### 6.1 播放服务 (PlaybackService.kt)
使用 Media3，你需要创建一个继承自 `MediaSessionService` 的类。

```kotlin
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
```

### 6.2 媒体扫描 (MusicRepository.kt)
通过 ContentResolver 访问本地音乐。

```kotlin
fun fetchLocalMusic(context: Context): List<MusicModel> {
    val musicList = mutableListOf<MusicModel>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA
    )
    
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        null, null, null
    )?.use { cursor ->
        // 遍历 cursor 填充 musicList
    }
    return musicList
}
```

## 7. 开发环境要求
- **Android Studio**: Flamingo (2022.2.1) 或更高版本。
- **Gradle**: 8.0+。
- **Min SDK**: 24 (Android 7.0)。

## 8. 下一步建议
1. 在 Android Studio 中创建一个新的 "Empty Compose Activity" 项目。
2. 在 `build.gradle` 中添加 `androidx.media3:media3-exoplayer` 和 `androidx.media3:media3-session` 依赖。
3. 按照文档中的结构开始编写核心服务代码。
