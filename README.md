# 嘻嘻配音 - 开发文档

## 项目概述

**嘻嘻配音** 是一个 Android 原生语音克隆 APP，基于 MiMo-V2.5-TTS API 实现音色克隆和语音合成。

- **包名**: `com.linky.voiceclone`
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 36
- **架构**: 单 Activity + Compose Navigation

---

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 依赖注入 | Hilt |
| 本地存储 | Room (SQLite) + DataStore |
| 网络 | OkHttp (直连 MiMo REST API，无后端) |
| 录音 | AudioRecord PCM → WAV |
| 音频播放 | MediaPlayer + PlayerManager 单例 |

---

## 项目结构

```
app/src/main/java/com/linky/voiceclone/
├── VoiceCloneApp.kt          # Hilt Application
├── MainActivity.kt            # 单 Activity，Edge-to-Edge
├── api/
│   └── MiMoTtsApi.kt         # OkHttp 客户端，调用 MiMo REST API
├── data/
│   ├── Voice.kt              # Room Entity（音色数据）
│   ├── VoiceDao.kt           # Room DAO（音色增删改查）
│   ├── AppDatabase.kt        # Room 数据库（v1→v2 迁移）
│   └── SettingsDataStore.kt  # API Key / Base URL 持久化
├── di/
│   └── AppModule.kt          # Hilt DI 提供者
├── ui/
│   ├── AppTopBar.kt          # 统一 TopBar 组件
│   ├── Navigation.kt         # 底部 3-Tab 导航
│   ├── home/
│   │   └── HomeScreen.kt     # 合成页面（克隆/设计模式切换、音色选择、合成）
│   ├── voices/
│   │   └── VoicesScreen.kt   # 音色管理（录音/上传/编辑/删除/试听）
│   ├── history/
│   │   └── HistoryScreen.kt  # 生成记录（播放/分享/删除）
│   ├── settings/
│   │   └── SettingsScreen.kt # 设置页面（API 配置）
│   └── theme/
│       └── Theme.kt          # Material 3 主题（暗色）
├── util/
│   ├── PlayerManager.kt      # 全局音频播放单例（确保同时只播放一个）
│   └── WavRecorder.kt        # AudioRecord PCM → WAV 录音器
└── viewmodel/
    ├── SynthViewModel.kt     # 合成逻辑 ViewModel
    └── VoiceViewModel.kt     # 音色管理 ViewModel
```

---

## 核心模块设计

### 1. MiMo API 客户端 (`api/MiMoTtsApi.kt`)

**两种合成模式**:

| 模式 | API 端点 | 输入 | 用途 |
|------|----------|------|------|
| Clone | `/v1/audio/speech` | 音频样本 + 文本 | 复刻已有音色 |
| Design | `/v1/audio/speech` | 音色描述 + 文本 | 文本生成自定义音色 |

**音频格式限制**:
- ✅ 支持: WAV, MP3
- ❌ 不支持: M4A, AAC, MP4, OGG

**关键实现**:
```kotlin
// 音频样本必须转为 base64 DataURL
val dataUrl = "data:audio/wav;base64,${base64.encodeToString(bytes)}"

// OkHttp 连接池优化
ConnectionPool(2, 5, TimeUnit.MINUTES)
```

### 2. 录音模块 (`util/WavRecorder.kt`)

**为什么用 AudioRecord 而不是 MediaRecorder**:
- MediaRecorder 只能输出 M4A/AAC (MP4 容器)
- MiMo API 不支持 MP4 格式
- AudioRecord 录制 PCM 原始数据，手动封装 WAV 头

**录音参数**:
- 采样率: 16kHz
- 声道: 单声道
- 位深: 16-bit PCM
- 建议时长: 10-30 秒

### 3. 音频播放管理 (`util/PlayerManager.kt`)

**单例模式，确保全局同时只播放一个音频**:
```kotlin
object PlayerManager {
    private var current: MediaPlayer? = null

    fun play(player: MediaPlayer, onDone: () -> Unit) {
        current?.stop()  // 停止之前的播放
        current = player
        player.start()
    }

    fun pause(player: MediaPlayer) { player.pause() }
}
```

**问题**: MediaPlayer 通过 ComponentCallbacks 自动注册/注销导致异常
**解决**: 移除自动注册，改为显式调用 `play()` / `pause()`

### 4. Room 数据库 (`data/`)

**Voice Entity**:
```kotlin
@Entity(tableName = "voices")
data class Voice(
    @PrimaryKey val id: String,        // UUID
    val name: String,                   // 音色名称
    val description: String,            // 描述
    val sampleFileName: String,         // 样本文件名
    val sampleSize: Long,               // 文件大小
    val createdAt: Long,                // 创建时间
    val lastUsedAt: Long = 0            // 最近使用时间（v2 新增）
)
```

**数据库迁移 (v1 → v2)**:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE voices ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
    }
}
```

### 5. 依赖注入 (`di/AppModule.kt`)

Hilt 提供以下依赖:
- `AppDatabase` - Room 数据库单例
- `VoiceDao` - 音色 DAO
- `SettingsDataStore` - 设置存储

---

## UI 设计规范

### 主题

| 属性 | 值 |
|------|-----|
| 背景色 | `#0F1117` |
| 主色调 | `#3B82F6` (蓝色) |
| 模式 | 暗色主题 |

### 组件统一规范

| 组件 | 规范 |
|------|------|
| 卡片圆角 | 12dp |
| TopBar | 居中标题、粗体、Logo + 设置按钮 |
| 底部导航 | 3-Tab（合成/音色/记录），硬切换无动画 |
| 输入框圆角 | 12dp |
| WAV 标签 | 高度 16dp，圆角 3dp，描边风格 |

### 页面切换

```kotlin
// 硬切换，无动画（避免跨淡变延迟感）
enterTransition = { EnterTransition.None }
exitTransition = { ExitTransition.None }
```

---

## 踩坑记录

### 1. MiMo API 不支持 M4A/MP4

**问题**: MediaRecorder 默认输出 M4A (MP4 容器)，MiMo API 返回 "unsupported format"
**解决**: 改用 AudioRecord 录制 PCM，手动写 WAV 头
**代码**: `util/WavRecorder.kt`

### 2. API 参数大小写敏感

**问题**: 模型名 `MiMo-V2.5-TTS` 会返回 400 "Param Incorrect"
**解决**: 必须用全小写 `mimo-v2.5-tts`

### 3. ExposedDropdownMenuBox 遮罩问题

**问题**: Material 3 的 ExposedDropdownMenuBox 下拉菜单有默认遮罩
**解决**: 改用 `ModalBottomSheet` 替代下拉菜单

### 4. PlayerManager ComponentCallbacks 异常

**问题**: MediaPlayer 在 ComponentCallbacks 中自动注册/注销导致崩溃
**解决**: 移除 ComponentCallbacks 注册，改为显式管理播放状态

### 5. enableEdgeToEdge 双重 insets

**问题**: TopBar 和系统状态栏双重 insets 导致顶部空白
**解决**: TopBar 设置 `windowInsets = WindowInsets(0.dp)`，由 Scaffold 统一处理

### 6. adb install 失败 (-99 错误)

**问题**: ColorOS 16 安全策略阻止 adb install
**解决**: 通过飞书发送 APK 文件，手动安装

---

## 构建与发布

### Debug 构建

```bash
cd ~/workspace/voice-clone-app
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Release 构建

```bash
./gradlew assembleRelease
# 需要配置签名密钥
```

### 依赖版本

| 依赖 | 版本 |
|------|------|
| Kotlin | 2.3.21 |
| Compose BOM | 2025.06.01 |
| Hilt | 2.56.2 |
| Room | 2.7.2 |
| OkHttp | 4.12.0 |
| Material 3 | (BOM 管理) |

---

## 配置说明

### API 配置

在 APP 设置页面配置:

| 参数 | 默认值 | 说明 |
|------|--------|------|
| API Key | (空) | MiMo API 密钥 |
| Base URL | `https://token-plan-cn.xiaomimimo.com/v1` | API 端点 |

### 存储路径

| 类型 | 路径 |
|------|------|
| 音色样本 | `app.filesDir/audio/samples/` |
| 合成结果 | `app.filesDir/audio/output/` |
| API 配置 | DataStore (`settings.preferences`) |

---

## 未来优化方向

1. **批量合成** - 多段文本顺序合成
2. **音频格式转换** - 内置 M4A → WAV 转换
3. **云端备份** - 音色数据云同步
4. **更多 API** - 支持其他 TTS 服务商
5. **Widget** - 桌面小部件快速合成
