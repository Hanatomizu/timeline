# Timeline

一款基于 Kotlin + Jetpack Compose 构建的 Android 时间轴应用，支持多条时间线管理，为每个时间点记录文字、图片、颜色标签与精确时间。

## 功能

- **时间线管理** — 创建多条独立的时间线，支持封面图片，长按删除（级联删除所有时间点）
- **时间点记录** — 以卡片列表形式展示，每张卡片显示彩色标签、日期时间、内容和缩略图
- **编辑/新建弹窗** — 点击卡片弹出编辑对话框，支持内容、图片、标签颜色、日期时间（精确到分钟）
- **排序切换** — 右上角一键切换升序/降序
- **图片处理** — 从相册选取图片，自动复制到应用内部存储（兼容分区存储）
- **深色主题** — 跟随系统自动切换 Material 3 深色/浅色主题

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Kotlin 2.1.20 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM（ViewModel + Repository） |
| 数据库 | Room 2.7.2（KSP 编译处理器） |
| 图片加载 | Coil 2.6.0 |
| 导航 | Compose Navigation |
| 并发 | Kotlin Coroutines + Flow |
| 构建 | AGP 9.1.0 + Gradle 9.3.1 |

## 快速开始

### 环境要求

- Android Studio Ladybug 或更新版本
- JDK 17 or higher (Temurin JDK 21 recommended)
- Android SDK 34+
- Gradle 9.3.1（由 wrapper 自动管理）

### 构建运行

```bash
git clone https://github.com/hanatomizu/timeline.git
cd Timeline
./gradlew assembleDebug
```

或在 Android Studio 中打开项目根目录，等待 Gradle Sync 完成后直接运行。

## 项目结构

```
app/src/main/java/moe/hanatomizu/timeline/
├── MainActivity.kt                 # 入口 + Compose Navigation
├── data/
│   ├── entity/
│   │   ├── TimelineEntity.kt       # 时间线实体
│   │   └── TimelineEventEntity.kt  # 时间点实体（外键级联删除）
│   ├── dao/
│   │   ├── TimelineDao.kt
│   │   └── TimelineEventDao.kt
│   ├── AppDatabase.kt              # Room 数据库（单例）
│   └── TimelineRepository.kt       # 统一数据仓库
├── viewmodel/
│   ├── TimelineListViewModel.kt    # 列表界面
│   └── TimelineDetailViewModel.kt  # 详情界面
├── ui/
│   ├── theme/Theme.kt              # Material 3 主题（Dynamic Color）
│   └── screens/
│       ├── TimelineListScreen.kt   # 时间线列表
│       └── TimelineDetailScreen.kt # 详情（卡片列表 + 编辑弹窗）
└── util/
    ├── ImageFileHelper.kt          # 图片复制到内部存储
    └── ColorPickerDialog.kt        # 预设颜色选择器
```



## License

MIT License — 详见 [LICENSE](LICENSE)。

## Acknowledgements

See [NOTICE](NOTICE)

and AI assistants:

- Claude Code CLI
- DeepSeek-v4-flash
