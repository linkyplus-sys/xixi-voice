# 嘻嘻配音

一款 Android 原生 AI 配音应用，使用小米 MiMo V2.5 TTS API 完成音色克隆、音色设计、语音合成和本地历史管理。

## 功能

- 录制 WAV 样本或导入 WAV / MP3 音频。
- 根据真实文件头校验格式，不通过修改扩展名伪装格式。
- 使用 `mimo-v2.5-tts-voiceclone` 复刻音色。
- 使用 `mimo-v2.5-tts-voicedesign` 根据文字描述设计音色。
- 可取消的分阶段生成状态与分类错误提示。
- 单实例音频播放、试听、保存到文件夹、分享和删除。
- Room 持久化音色与生成历史。
- 深色声场视觉系统和自适应液态玻璃底部导航。

## MiMo 音频要求

音色克隆样本只接受：

- WAV
- MP3

样本经过 Base64 编码后不能超过 10 MiB。应用会在导入阶段检查文件头和编码后大小，M4A、AAC、MP4、OGG 等格式不会被错误重命名为 WAV。

当前请求使用：

```text
POST {baseUrl}/chat/completions
Header: api-key: <API_KEY>
```

默认 Base URL：

```text
https://token-plan-cn.xiaomimimo.com/v1
```

## 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Kotlin 2.3.21 / Java 17 |
| UI | Jetpack Compose + Material 3 |
| 依赖注入 | Hilt 2.58 |
| 数据 | Room 2.8.4 + DataStore |
| 网络 | OkHttp 4.12.0 |
| 录音 | AudioRecord，44.1kHz / 单声道 / PCM 16-bit WAV |
| 最低系统 | Android 8.0 / API 26 |
| 目标系统 | API 36 |

## 代码结构

```text
app/src/main/java/com/linky/voiceclone/
├── api/             MiMo 请求、重试和错误映射
├── audio/           音频导入、文件头识别和大小校验
├── data/            Room Entity / DAO / DataStore
├── di/              Hilt 依赖提供
├── ui/
│   ├── components/  玻璃容器、音色头像、波形等通用组件
│   ├── home/        合成工作台
│   ├── voices/      录音、导入和音色管理
│   ├── history/     生成历史
│   ├── settings/    API 配置与连接测试
│   └── theme/       颜色、字体、圆角和主题
├── util/            播放器和 WAV 录音器
└── viewmodel/       页面状态与业务流程
```

## 本地存储

| 数据 | 路径 |
|---|---|
| 音色样本 | `filesDir/audio/samples/` |
| 生成结果 | `filesDir/audio/output/` |
| 音色与历史元数据 | Room `voiceclone.db` |
| API 配置 | DataStore `settings.preferences_pb` |

生成结果会先保存在应用私有目录；结果页和历史记录中的“保存到文件夹”按钮可通过 Android 系统文件选择器导出到用户指定位置，不依赖系统分享面板是否提供文件管理器入口。

API Key 仅保存在当前设备，不应写入源码或提交到 Git。如果未来使用应用方统一密钥，应改为后端代理，不能打包到 APK。

## 构建

1. 安装 Android Studio，并使用其自带 JDK 17+。
2. 在未纳入 Git 的 `local.properties` 中配置 SDK：

```properties
sdk.dir=C\:\\Users\\<用户名>\\AppData\\Local\\Android\\Sdk
```

3. 构建 Debug APK：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

## 正式签名

正式 APK 通过以下环境变量读取签名，不应把签名文件或密码提交到仓库：

```text
XIXI_RELEASE_STORE_FILE
XIXI_RELEASE_STORE_PASSWORD
XIXI_RELEASE_KEY_ALIAS
XIXI_RELEASE_KEY_PASSWORD
```

配置完成后执行 `assembleRelease`，产物位于 `app/build/outputs/apk/release/`。长期发布必须始终使用同一份签名文件，否则用户无法覆盖升级。

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

项目启用了 `android.overridePathCheck=true` 以允许当前中文目录完成 Android 构建。部分 Windows JDK/Gradle 单元测试 worker 对非 ASCII 路径仍可能存在编码问题；运行测试时推荐将项目放在纯英文路径。

## 测试

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

当前单元测试覆盖 WAV/MP3 文件头识别、非法容器拦截和 Base64 大小计算。

## 数据库版本

- v1：音色表。
- v2：增加音色最近使用时间。
- v3：增加生成历史表和创建时间索引。

所有迁移均通过显式 `Migration` 注册，升级不会清空已有音色数据。
