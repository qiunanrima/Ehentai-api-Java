# ehapi (Ehentai-api-Java)

独立的 **Kotlin / JVM API 客户端库**，专为 E-Hentai 与自定义漫画源服务打造，不含任何 Android 依赖。
**全面深度对齐 [PicACG-Api-Java](file:///F:/projects/Java/PicACG-Api-Java) 架构设计与调用规范**，提供开箱即用、极致便利的 Java 与 Kotlin 双端调用体验。

- **Gradle** 构建，纯 JVM（Java 17+）
- **OkHttp 5.x** 直连 REST，无 Retrofit
- **双端对齐架构**：与 `PicACG-Api-Java` 拥有完全一致的 API 设计语言（`Eh` 全局单例、`EhResult` 统一结果模型、`EhClient` 同步阻塞接口、`EhAsyncClient` 异步 CompletableFuture、`EhQuery` 流式查询构建器、`EhDownloader` 章节并发下载器）
- **全生态兼容**：既支持对接本项目内置/自建的 E-Hentai 高性能代理服务（`/config`、`/search`、`/comic`、`/photo`、`/image/proxy`），又内置 E-Hentai 官方 JSON RPC 接口直连（`gdata` 元数据获取）
- **Java 友好**：全接口具备 `@JvmOverloads`、Fluent Builder、Java 8 `Optional`、`Consumer`/`Function` 函数式链式回调
- **原子下载与低功耗支持**：Windows 非法文件名净化、`.tmp` 原子重命名防破损、LVGL / PNG / JPEG 预解码格式支持

---

## 目录

- [功能特性](#功能特性)
- [环境与构建](#环境与构建)
- [引入项目](#引入项目)
- [快速开始](#快速开始)
  - [Kotlin](#kotlin)
  - [Java](#java)
- [全局单例 `Eh`](#全局单例-eh)
- [结果处理 `EhResult`](#结果处理-ehresult)
- [配置 `EhConfig`](#配置-ehconfig)
- [查询构建 `EhQuery`](#查询构建-ehquery)
- [REST 与官方接口索引](#rest-与官方接口索引)
- [图片代理与下载器](#图片代理与下载器)
- [线程安全](#线程安全)
- [错误处理](#错误处理)
- [目录结构](#目录结构)
- [后端代理服务参考](#后端代理服务参考)
- [FAQ](#faq)

更多细节见：
- [docs/API.md](docs/API.md) —— 全部 REST 与官方接口签名及用法
- [docs/CONFIG.md](docs/CONFIG.md) —— 配置项完整说明与网络参数

---

## 功能特性

| 模块 | 说明 | 对齐 PicACG 设计 |
| --- | --- | --- |
| **全局单例** | `Eh.init()`、`Eh.client`、`Eh.async`、`Eh.cookie()`、持久化恢复 | 对齐 `Pica` |
| **统一结果包装** | `EhResult<T>`（`Success` / `Failure`，支持 `getOrThrow`、`toOptional`、链式回调） | 对齐 `PicaResult` |
| **同步/异步客户端** | 同步阻塞接口 `EhClient`，基于 `CompletableFuture` 的非阻塞接口 `EhAsyncClient` | 对齐 `PicaClient` / `PicaAsyncClient` |
| **漫画检索** | 关键字、语言、多命名空间标签、分类、页码筛选与表达式智能生成 | 对齐 `ComicQuery` |
| **漫画详情** | 获取画廊标题、评分、总虚拟章节数、封面图、汉化标签列表 | 对齐 `ComicDetail` |
| **章节与图片** | 获取虚拟章节（每章 20 页）图片列表，支持全本图片聚合拉取 | 对齐 `ComicEpisodes` / `ComicPages` |
| **图片代理** | 服务端动态缩放、裁剪雪碧图、压缩质量、JPEG / PNG / LVGL 格式输出 | 对齐 `PicaImages` |
| **多线程下载器** | 非法字符过滤、安全原子落盘、并发流水线下载、实时进度通知回调 | 对齐 `PicaDownloader` |
| **官方 JSON 直连** | 官方 `api.php` 的 `gdata` 元数据单本与批量查询 | 官方 JSON RPC 原生直连 |
| **源规范对齐** | 获取 `/config` 漫画源标准配置，支持一键载入漫画阅读器 | 自定义漫画源规范 |

---

## 环境与构建

要求：
- JDK 17+（`JAVA_HOME` 指向 JDK 17）
- 无需 Android SDK，纯 JVM 库

```bash
./gradlew build            # 编译 + 运行测试 + 打包
./gradlew test             # 单独执行单元测试
./gradlew jar              # 打包 jar
```

产物位于：
```
build/libs/ehapi-1.0.0.jar
build/libs/ehapi-1.0.0-sources.jar
```

---

## 引入项目

### 方式一：复合构建（推荐，直接用源码）

```groovy
// settings.gradle
includeBuild('../Ehentai-api-Java')

// build.gradle
dependencies {
    implementation 'com.ehapi:ehapi:1.0.0'
}
```

### 方式二：本地 jar

```groovy
dependencies {
    implementation files('libs/ehapi-1.0.0.jar')
    // 传递依赖需自行声明
    implementation 'com.squareup.okhttp3:okhttp:5.3.2'
    implementation 'com.squareup.okhttp3:logging-interceptor:5.3.2'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.2.21'
}
```

---

## 快速开始

### Kotlin

```kotlin
import com.ehapi.EhClient
import com.ehapi.EhQuery
import com.ehapi.objects.EhImageFormat
import java.io.File

// 1. 初始化客户端
val eh = EhClient {
    baseUrl = "https://your-api-domain.com/"
    cookie = "igneous=xxx; ipb_member_id=12345;"
    defaultImageQuality = 60
}

// 2. 搜索漫画：直接 getOrThrow() 或函数式处理
val search = eh.searchComics(
    EhQuery.builder()
        .keyword("genshin")
        .language("chinese")
        .addTag("female", "maid")
        .build()
).getOrThrow()

search.results.forEach { item ->
    println("${item.title} -> ${item.comicId} (Gid: ${item.gid}, Token: ${item.token})")
}

// 3. 读取漫画详情与虚拟章节
val detail = eh.getComicDetail(search.results[0].comicId).getOrThrow()
println("画廊名称: ${detail.name}, 评分: ${detail.rate}, 虚拟章节数: ${detail.totalChapters}")

val chapter = eh.getComicImages(detail.itemId, 1).getOrThrow()
println("第 1 章图片数: ${chapter.imageCount}")

// 4. 并发下载章节图片（带进度通知）
eh.downloader.downloadChapter(
    images = chapter.images,
    destinationDir = File("./downloads/${detail.gid}"),
    listener = { p -> println("已下载: ${p.current}/${p.total} (${p.percentage}%)") }
)

// 5. 异步调用 (返回 CompletableFuture)
eh.async.getComicDetail("3645215_4db836130d").thenAccept { res ->
    res.onSuccess { println("异步获取成功: ${it.name}") }
}
```

### Java

```java
import com.ehapi.*;
import com.ehapi.objects.*;
import java.io.File;

// 1. 支持 fluent Builder 构造配置
EhClient client = EhClient.create(EhConfig.builder()
    .baseUrl("https://your-api-domain.com/")
    .cookie("igneous=xxx; ipb_member_id=12345;")
    .defaultImageQuality(50)
    .enableLogging(true)
    .build());

// 2. 构造查询参数进行检索
EhResult<EhSearchResponse> searchResult = client.searchComics(
    EhQuery.builder()
        .keyword("naruto")
        .language("chinese")
        .page(1)
        .build()
);

// 3. 函数式链式回调，无需繁杂类型强转
searchResult
    .onSuccess(data -> {
        System.out.println("共找到: " + data.getCount() + " 本画廊");
        for (EhComicItem item : data.getResults()) {
            System.out.println(item.getTitle() + " -> " + item.getComicId());
        }
    })
    .onFailure(failure -> System.out.println("查询失败: " + failure.getMessage()));

// 4. 直接抛出异常风格调用
EhComicDetail detail = client.getComicDetail("3645215_4db836130d").getOrThrow();
System.out.println("详情: " + detail.getName() + ", 评分: " + detail.getRate());

// 5. 原生 CompletableFuture 异步支持，避免主线程网络阻塞
client.async().getComicImages(detail.getItemId(), 1).thenAccept(res -> {
    res.onSuccess(chapter -> {
        System.out.println("章节图片总数: " + chapter.getImageCount());
    });
});

// 6. 批量下载
client.getDownloader().downloadChapter(
    client.getComicImages(detail.getItemId(), 1).getOrThrow().getImages(),
    new File("./downloads/chapter1"),
    "page_",
    progress -> System.out.println("下载进度: " + progress.getCurrent() + "/" + progress.getTotal()),
    4,
    client.getRawHttpClient()
);
```

---

## 全局单例 `Eh`

`Eh` 提供跨页面共享的 `EhClient` 与 Cookie 持久化恢复，对齐 `PicACG-Api-Java` 中的 `Pica`：

```kotlin
import com.ehapi.Eh
import com.ehapi.FileEhCookieStore
import java.io.File

// Kotlin DSL 快速初始化单例
Eh.init(
    cookieStore = FileEhCookieStore(File("cookie.txt"))
) {
    baseUrl = "https://your-api-domain.com/"
}

// 任何页面直接使用单例
val result = Eh.searchComics("language:chinese")

// 保存与读取 Cookie
Eh.saveCookie("igneous=...;")
println(Eh.cookie())

// 清除 Cookie
Eh.clearCookie()
```

| 成员 | 说明 |
| --- | --- |
| `Eh.init(config?, cookieStore?)` | 初始化单例，并自动恢复 `cookieStore` 中保存的 Cookie |
| `Eh.client` | 全局 `EhClient`（未初始化时抛 `IllegalStateException`） |
| `Eh.async` / `Eh.getAsync()` | 全局异步客户端 `EhAsyncClient` |
| `Eh.downloader` | 全局下载器实例 `EhDownloader` |
| `Eh.isInitialized` | 是否已初始化 |
| `Eh.hasCookie` / `Eh.isLoggedIn` | 是否已持有有效 Cookie 凭据 |
| `Eh.saveCookie(cookie)` / `Eh.cookie()` | 保存 / 读取当前 Cookie |
| `Eh.clearCookie()` | 清除 Cookie |

---

## 结果处理 `EhResult`

所有接口统一返回 `EhResult<T>`，在 Java 与 Kotlin 下均具备一流的使用体验：

```kotlin
sealed class EhResult<out T> {
    data class Success<out T>(val data: T, val httpCode: Int = 200) : EhResult<T>()
    data class Failure(
        val httpCode: Int?,      // 请求未到达服务器（网络错误）时为 null
        val errorCode: String?,  // 业务错误码
        val message: String?,
        val rawBody: String?,
        val cause: Throwable?,
    ) : EhResult<Nothing>()
}
```

通用方法（Java / Kotlin 均可直接调用）：

| 方法 / 属性 | 说明 |
| --- | --- |
| `isSuccess` / `isSuccess()` | 是否成功 |
| `isFailure` / `isFailure()` | 是否失败 |
| `getOrNull()` | 成功返回数据，失败返回 `null` |
| `getFailureOrNull()` | 失败返回 `Failure` 结构，成功返回 `null`（Java 免强转） |
| `getOrDefault(default)` | 成功返回数据，失败返回默认值 |
| `getOrElse(fallback)` | 成功返回数据，失败通过 lambda / Function 计算备选值 |
| `getOrThrow()` | 成功返回数据，失败直接抛出 `EhException` |
| `toOptional()` | 转换为 Java 8 `Optional<T>` |
| `onSuccess(Consumer / Block)` | 成功回调，支持链式操作 |
| `onFailure(Consumer / Block)` | 失败回调，支持链式操作 |
| `map(Function / Block)` | 转换成功数据并保持封装 |
| `flatMap(Function / Block)` | 平铺转换 |
| `fold(onSuccess, onFailure)` | 双分支折叠为目标类型 |

---

## 配置 `EhConfig`

```kotlin
val config = EhConfig(
    baseUrl = "https://your-api-domain.com/",
    cookie = "igneous=...; ipb_member_id=...;",
    userAgent = "wristcomic(1.8.0(18))/smartwatch/xiaomi/hyperos/1.0/1/zh/CN",
    timeoutSeconds = 30L,
    enableLogging = false,
    disableSslVerification = false,
    dnsIps = listOf("1.1.1.1", "8.8.8.8"),
    proxyHost = "127.0.0.1",
    proxyPort = 7890,
    defaultImageQuality = 50,
    defaultImageWidth = 400,
    defaultImageFormat = EhImageFormat.JPEG,
)
val eh = EhClient(config)
```

支持运行时动态更新：
```kotlin
eh.updateConfig(newConfig)
eh.updateCookie("igneous=new_cookie;")
```

---

## 查询构建 `EhQuery`

专门为画廊复杂检索设计的构建器，能够自动根据多条件组装标准的 E-Hentai 检索语句：

```java
EhQuery query = EhQuery.builder()
    .keyword("genshin")
    .language("chinese")
    .category("manga")
    .addTag("female", "big breasts")
    .uploader("artist_name")
    .page(1)
    .build();

// 自动生成: genshin language:chinese category:manga uploader:artist_name female:"big breasts"
System.out.println(query.toSearchQuery());
```

---

## REST 与官方接口索引

完整接口列表与参数见 [docs/API.md](docs/API.md)。

```kotlin
// 1. 获取源配置与健康状态
eh.getConfig()
eh.health()

// 2. 检索画廊
eh.searchComics(query, page)
eh.searchComics(EhQuery)

// 3. 画廊详情与章节
eh.getComicDetail(comicId)
eh.getComicImages(comicId, chapter)
eh.getAllChapterImages(comicId, totalChapters)

// 4. 图片代理
eh.fetchProxyImage(rawUrl, width, quality, format)
eh.downloadProxyImage(rawUrl, targetFile, width, quality, format)

// 5. E-Hentai 官方 JSON RPC 接口直连
eh.getGalleryMetadata(gid, token)
eh.getGalleryMetadataList(gidList)
```

---

## 图片代理与下载器

### 图片代理地址拼装 `EhImages`

```kotlin
val proxyUrl = EhImages.buildProxyUrl(
    baseUrl = "https://your-api-domain.com/",
    rawImageUrl = "https://ehgt.org/cover.jpg",
    width = 300,
    quality = 50,
    format = EhImageFormat.PNG
)
```

### 下载器 `EhDownloader`
- **安全过滤**：`EhDownloader.sanitizeFilename("comic/title:test?*")` 自动将 Windows/Linux 非法字符替换为安全字符。
- **原子保存**：`EhDownloader.writeAtomically(file, bytes)` 写入同目录下的 `.tmp` 临时文件，写完后原子替换目标文件，断电/断网不会残留损坏文件。
- **章节批量下载**：`EhDownloader.downloadChapter(images, dir, listener)` 多线程并发下载，实时计算百分比通知。

---

## 线程安全

- 内部持有单个共享的 `OkHttpClient`，本身具备优秀的并发性能与线程安全性。
- 可变属性（如 `config`、`cookie`）均通过 `@Volatile` 保证多线程可见性。
- 所有 REST 接口均为**同步阻塞**调用，请勿在 Android 主线程直接执行；或使用 `client.async()` 异步调用。

```kotlin
// 协程环境下使用示例
suspend fun loadComics() = withContext(Dispatchers.IO) {
    Eh.searchComics("language:chinese").getOrThrow()
}
```

---

## 目录结构

```
Ehentai-api-Java/
├── build.gradle                 # Kotlin JVM 库配置，OkHttp + Gson + JUnit 5
├── settings.gradle              # Gradle 项目根设置
├── gradle.properties
├── docs/
│   ├── API.md                   # 完整 REST 接口与方法签名说明
│   └── CONFIG.md                # 完整网络与高级配置参考
├── src/main/kotlin/com/ehapi/
│   ├── Eh.kt                    # 全局单例管理器 (对齐 Pica.kt)
│   ├── EhClient.kt              # 核心同步 REST 客户端 (对齐 PicaClient.kt)
│   ├── EhAsyncClient.kt         # 异步客户端 (CompletableFuture)
│   ├── EhConfig.kt              # 不可变配置与 Fluent Builder
│   ├── EhConstants.kt           # 核心常量定义
│   ├── EhCookieStore.kt         # Cookie 持久化接口与实现
│   ├── EhDownloader.kt          # 图片与章节下载器 (原子写入/并发下载)
│   ├── EhImages.kt              # 图片代理与格式辅助工具
│   ├── EhNetworking.kt          # TLS / DNS / 代理底层策略
│   ├── EhQuery.kt               # 检索条件查询构建器 (对齐 ComicQuery.kt)
│   ├── EhResult.kt              # 统一结果封装与异常 (对齐 PicaResult.kt)
│   └── TypeAliases.kt           # 类型别名定义
├── src/main/java/com/ehapi/objects/
│   ├── EhComicDetail.java       # 画廊详情模型
│   ├── EhComicItem.java         # 检索条目模型
│   ├── EhSearchResponse.java    # 列表响应模型
│   ├── EhChapterImages.java     # 章节图片响应模型
│   ├── EhImageItem.java         # 单张图片模型
│   ├── EhSourceConfig.java      # 源规范配置模型
│   ├── EhHealthResponse.java    # 健康检查响应模型
│   ├── EhGalleryMetadata.java   # 官方 gdata 元数据模型
│   ├── EhImageFormat.java       # 图片格式枚举 (JPEG, PNG, LVGL)
│   └── GDataRequest.java        # 官方 gdata 请求模型
└── src/test/
    ├── java/com/ehapi/JavaInteropTest.java    # 纯 Java 互操作与流畅链式测试
    └── kotlin/com/ehapi/EhClientTest.kt       # MockWebServer 单元测试
```

---

## 后端代理服务参考

本项目保留了 Python 高性能代理服务服务端源码（`index.py`、`ecosystem.config.js`、`install.sh`），可按需自行部署至 Linux 服务器为低功耗快应用或手表端提供代理转码。

```bash
# 本地调试运行后端
pip install -r requirements.txt
python index.py
```

服务端提供 `/config`、`/search`、`/comic/<id>`、`/photo/<id>/<chapter>`、`/image/proxy`、`/health` 接口，Java / Kotlin 客户端无缝对接。

---

## FAQ

**Q：调用该库需要 Android 环境吗？**  
A：不需要，纯标准 JVM 库（Java 17+），完全兼容 Android、Spring Boot、Ktor、JavaFX、Compose Desktop 及各类命令行工具。

**Q：访问 ExHentai 或受限画廊需要什么？**  
A：只需在 `EhConfig` 中配置合法 Cookie 字符串（包含 `igneous`、`ipb_member_id`、`ipb_pass_hash`），所有请求将自动带上该 Cookie。

**Q：遇到 404 或网络问题如何捕获？**  
A：接口不会直接向外抛出未捕获的网络崩溃。统一返回 `EhResult.Failure`，可通过 `result.isFailure`、`result.getFailureOrNull()` 优雅处理，或调用 `result.getOrThrow()` 按需抛出 `EhException`。
