# REST 与官方接口参考

`EhClient` 的全部接口。所有函数**同步阻塞**（异步版本请使用 `client.async`），返回统一的 `EhResult<T>`。

- `Cookie` 列：`是` 表示建议或必须携带 Cookie 请求头（访问 ExHentai 或带权限资源）。
- `路径`：相对于 `EhConfig.baseUrl`。
- 返回类型均在 `com.ehapi.objects.*`。
- `page` 默认为 `1`。

---

## 目录

- [源配置与健康检查](#源配置与健康检查)
- [漫画画廊检索与浏览](#漫画画廊检索与浏览)
- [漫画详情与章节图片](#漫画详情与章节图片)
- [图片代理与下载](#图片代理与下载)
- [官方 E-Hentai JSON 接口 (gdata)](#官方-e-hentai-json-接口-gdata)
- [ID 解析辅助方法](#id-解析辅助方法)

---

## 源配置与健康检查

| 函数 | HTTP | 路径 | Cookie | 返回 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `getConfig()` | GET | `config` | 否 | `EhResult<EhSourceConfig>` | 获取漫画源规范配置 |
| `health()` | GET | `health` | 可选 | `EhResult<EhHealthResponse>` | 服务健康检查及 Cookie 状态探测 |

```kotlin
// 获取源配置
val config = client.getConfig().getOrThrow()
println("源名称: ${config.name}, API: ${config.apiUrl}")

// 健康检查
val health = client.health().getOrThrow()
println("服务状态: ${health.status}, 携带有效 Cookie: ${health.isClientCookieProvided}")
```

---

## 漫画画廊检索与浏览

| 函数 | HTTP | 路径 | 说明 |
| --- | --- | --- | --- |
| `searchComics(query: String, page: Int = 1)` | GET | `search?q={query}&page={page}` | 快速关键词/表达式搜索 |
| `searchComics(query: EhQuery)` | GET | `search?q={...}&page={page}` | 使用流式 Builder 进行多条件检索 |

返回类型均为 `EhResult<EhSearchResponse>`。

### `EhQuery` 常用字段与用法

| 方法 | 说明 | 示例 |
| --- | --- | --- |
| `keyword(String)` | 关键字搜索 | `keyword("genshin")` |
| `page(Int)` | 页码（从 1 开始） | `page(2)` |
| `category(String)` | 分类过滤 | `category("manga")` |
| `language(String)` | 语言过滤 | `language("chinese")` |
| `addTag(namespace, name)` | 命名空间标签 | `addTag("female", "big breasts")` |
| `addTag(tag)` | 原始标签字符串 | `addTag("female:maid")` |
| `uploader(String)` | 上传者过滤 | `uploader("artist_name")` |
| `rawQuery(String)` | 直接使用原始表达式覆盖 | `rawQuery("language:chinese f_cats=1023")` |

```java
// Java 示例
EhResult<EhSearchResponse> res = client.searchComics(EhQuery.builder()
    .keyword("naruto")
    .language("chinese")
    .addTag("female", "maid")
    .page(1)
    .build());

res.onSuccess(data -> {
    System.out.println("共找到: " + data.getCount() + " 本画廊");
    for (EhComicItem item : data.getResults()) {
        System.out.println(item.getTitle() + " -> " + item.getComicId());
    }
});
```

---

## 漫画详情与章节图片

| 函数 | HTTP | 路径 | 返回 | 说明 |
| --- | --- | --- | --- | --- |
| `getComicDetail(comicId: String)` | GET | `comic/{comicId}` | `EhResult<EhComicDetail>` | 获取画廊详细信息（分类、标签、评分、总虚拟章节数） |
| `getComicImages(comicId: String, chapter: Int = 1)` | GET | `photo/{comicId}/{chapter}` | `EhResult<EhChapterImages>` | 获取指定虚拟章节（每章节 20 张）的图片列表 |
| `getAllChapterImages(comicId: String, totalChapters: Int)` | - | 多次请求合并 | `EhResult<List<EhImageItem>>` | 批量获取全本所有章节的所有图片链接 |

```kotlin
// Kotlin 示例
val detail = client.getComicDetail("3645215_4db836130d").getOrThrow()
println("画廊标题: ${detail.name}, 评分: ${detail.rate}, 虚拟章节总数: ${detail.totalChapters}")

// 读取第 1 章图片
val chapter1 = client.getComicImages(detail.itemId, 1).getOrThrow()
chapter1.images.forEach { img ->
    println("图片链接: ${img.url}")
}
```

---

## 图片代理与下载

| 函数 | 说明 |
| --- | --- |
| `fetchProxyImage(...)` | 请求服务端代理接口获取压缩/转码/裁剪后的二进制字节数组 `ByteArray` |
| `downloadProxyImage(url, targetFile, ...)` | 请求代理接口并**原子保存**到本地目标文件 |
| `downloader.downloadChapter(...)` | 并发多线程下载整个虚拟章节的全部图片，带实时进度监听 |
| `downloader.downloadImage(url, file)` | 下载任意单张网络图片并原子保存 |

```kotlin
// 进度监听下载章节
client.downloader.downloadChapter(
    images = chapter1.images,
    destinationDir = File("./downloads/chapter1"),
    filenamePrefix = "p_",
    concurrency = 4,
    listener = { progress ->
        println("下载进度: ${progress.current}/${progress.total} (${progress.percentage}%)")
    }
)
```

---

## 官方 E-Hentai JSON 接口 (gdata)

本库不仅支持自定义代理服务器，还内置了直连 E-Hentai 官方官方 JSON RPC `api.php` 接口的能力：

| 函数 | HTTP | 路径 | 返回 |
| --- | --- | --- | --- |
| `getGalleryMetadata(gid: Long, token: String)` | POST | `https://api.e-hentai.org/api.php` | `EhResult<EhGalleryMetadata>` |
| `getGalleryMetadataList(gidList: List<Pair<Long, String>>)` | POST | `https://api.e-hentai.org/api.php` | `EhResult<List<EhGalleryMetadata>>` |

```java
// Java 示例：直接查询官方元数据
client.getGalleryMetadata(3645215L, "4db836130d")
    .onSuccess(meta -> {
        System.out.println("官方标题: " + meta.getTitle());
        System.out.println("文件总数: " + meta.getFileCountInt());
        System.out.println("文件大小: " + meta.getFilesize() + " bytes");
        System.out.println("标签列表: " + meta.getTags());
    });
```

---

## ID 解析辅助方法

| 函数 | 说明 |
| --- | --- |
| `parseComicId(input: String): Pair<Long, String>?` | 提取 gid 与 token，支持 `"3645215_4db836130d"`、`"3645215/4db836130d"`、网页 URL `https://e-hentai.org/g/3645215/4db836130d/` |
| `EhComicItem.getGid()` / `getToken()` | 从条目 ID 自动拆解提取数字 ID 与 Token |
| `EhComicDetail.getGid()` / `getToken()` | 从详情 ID 自动拆解提取数字 ID 与 Token |
