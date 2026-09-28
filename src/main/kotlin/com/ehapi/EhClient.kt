package com.ehapi

import com.ehapi.objects.EhChapterImages
import com.ehapi.objects.EhComicDetail
import com.ehapi.objects.EhGalleryMetadata
import com.ehapi.objects.EhHealthResponse
import com.ehapi.objects.EhImageFormat
import com.ehapi.objects.EhImageItem
import com.ehapi.objects.EhSearchResponse
import com.ehapi.objects.EhSourceConfig
import com.ehapi.objects.GDataRequest
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

/**
 * E-Hentai API 的线程安全 REST 与官方接口客户端。
 * Thread-safe REST client for E-Hentai API.
 *
 * 与 PicACG-Api-Java 的 PicaClient 保持全面对齐：
 * <ul>
 *   <li>支持 Kotlin 与 Java 双端无缝互操作；</li>
 *   <li>所有接口均提供 `@JvmOverloads` 便捷默认重载；</li>
 *   <li>统一使用 [EhResult] 返回类型，免去冗长繁琐的异常捕获样板代码；</li>
 *   <li>提供 [async] 异步客户端，完整返回 [CompletableFuture]；</li>
 *   <li>内置 [EhDownloader] 下载引擎与图片处理 [EhImages]；</li>
 *   <li>支持同时对接自建/公共代理服务与 E-Hentai 官方 JSON RPC 接口。</li>
 * </ul>
 */
class EhClient @JvmOverloads constructor(
    config: EhConfig = EhConfig(),
    baseClient: OkHttpClient? = null,
) {

    /** 当前配置（可运行时动态替换）。 / Current configuration. */
    @Volatile
    var config: EhConfig = config
        private set

    /** 当前生效的 Cookie 请求头。 / Current Cookie header. */
    @Volatile
    var cookie: String? = config.cookie

    @Volatile
    private var baseClient: OkHttpClient? = baseClient

    private val gson = Gson()

    @Volatile
    private var httpClient: OkHttpClient = buildHttpClient(config, baseClient)

    /** 获取底层 OkHttpClient 实例。 / Underlying OkHttpClient instance. */
    val rawHttpClient: OkHttpClient
        get() = httpClient

    /** 内置的图片与章节下载器。 / Built-in downloader utility. */
    val downloader: EhDownloader
        get() = EhDownloader

    /** 异步调用客户端，所有方法均返回 [CompletableFuture]。 / Async client returning [CompletableFuture]. */
    val async: EhAsyncClient by lazy { EhAsyncClient(this) }

    /** 供 Java 友好调用的异步客户端获取方法。 / Java-friendly getter for async client. */
    @JvmName("async")
    fun async(): EhAsyncClient = async

    /** 替换当前配置并重新初始化 HTTP 传输层。 / Updates configuration. */
    fun updateConfig(newConfig: EhConfig) {
        this.config = newConfig
        this.cookie = newConfig.cookie
        this.httpClient = buildHttpClient(newConfig, this.baseClient)
    }

    /** 更新 Cookie 凭据。 / Updates cookie. */
    fun updateCookie(cookie: String?) {
        this.cookie = cookie
    }

    /**
     * 计算当前直接模式下实际生效的基础 URL。
     * 若配置了 exhentai.org 但缺少有效 igneous Cookie，自动回退到 e-hentai.org 防止白屏/Sad Panda。
     */
    fun getEffectiveBaseUrl(): String {
        val base = config.normalizedBaseUrl()
        val currentCookie = this.cookie
        val hasIgneous = currentCookie != null && currentCookie.contains("igneous") && !currentCookie.contains("igneous=mystery")
        if (base.contains("exhentai.org") && !hasIgneous) {
            return "https://e-hentai.org/"
        }
        return base
    }

    private fun buildHttpClient(cfg: EhConfig, base: OkHttpClient? = null): OkHttpClient {
        val builder = base?.newBuilder() ?: OkHttpClient.Builder()
        if (cfg.dnsIps.isNotEmpty()) {
            builder.dns(EhNetworking.dns(cfg.dnsIps))
        }
        EhNetworking.applySslPolicy(builder, cfg.disableSslVerification)
        if (!cfg.proxyHost.isNullOrBlank() && cfg.proxyPort != null && cfg.proxyPort > 0) {
            EhNetworking.applyProxy(builder, cfg.proxyHost, cfg.proxyPort)
        }
        EhNetworking.applyTimeouts(builder, cfg.timeoutSeconds)

        if (cfg.enableLogging) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        // 添加统一基础请求头 (UA、Accept、Accept-Language、Referer、Cookie)
        builder.addInterceptor { chain ->
            val original = chain.request()
            val reqBuilder = original.newBuilder()

            // 1. User-Agent
            if (original.header(EhConstants.HEADER_USER_AGENT) == null) {
                reqBuilder.header(EhConstants.HEADER_USER_AGENT, config.userAgent)
            }
            // 2. Accept
            if (original.header(EhConstants.HEADER_ACCEPT) == null) {
                reqBuilder.header(EhConstants.HEADER_ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            }
            // 3. Accept-Language
            if (original.header("Accept-Language") == null) {
                reqBuilder.header("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7,ja;q=0.6")
            }
            // 4. Referer: 针对 E-Hentai 站群与图片 CDN 自动注入防盗链 Referer
            val host = original.url.host.lowercase()
            if (original.header("Referer") == null) {
                if (host.contains("e-hentai.org") || host.contains("exhentai.org") || host.contains("ehgt.org") || host.contains("hath.network")) {
                    val defaultReferer = if (host.contains("exhentai")) "https://exhentai.org/" else "https://e-hentai.org/"
                    reqBuilder.header("Referer", defaultReferer)
                }
            }
            // 5. Cookie: 直连模式下自动追加 nw=1 绕过警告，并合并用户 Cookie
            val customCookie = this.cookie
            val existingCookie = original.header(EhConstants.HEADER_COOKIE)
            if (config.isDirectSite() || host.contains("e-hentai.org") || host.contains("exhentai.org")) {
                val mergedCookie = buildDirectCookie(customCookie ?: existingCookie)
                reqBuilder.header(EhConstants.HEADER_COOKIE, mergedCookie)
            } else if (!customCookie.isNullOrBlank() && existingCookie == null) {
                reqBuilder.header(EhConstants.HEADER_COOKIE, customCookie)
            }

            chain.proceed(reqBuilder.build())
        }

        return builder.build()
    }

    private fun buildDirectCookie(userCookie: String?): String {
        val cookies = mutableMapOf<String, String>()
        cookies["nw"] = "1"
        if (!userCookie.isNullOrBlank()) {
            val parts = userCookie.split(';')
            for (p in parts) {
                val trimmed = p.trim()
                val eq = trimmed.indexOf('=')
                if (eq > 0) {
                    val k = trimmed.substring(0, eq).trim()
                    val v = trimmed.substring(eq + 1).trim()
                    if (k.isNotBlank()) cookies[k] = v
                }
            }
        }
        return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    // region 核心网络请求执行与解析 / Core Request Execution -----------------------------------------

    internal fun <T> executeRequest(
        request: Request,
        type: Type,
        customParser: ((String) -> T)? = null,
    ): EhResult<T> {
        return try {
            val response = httpClient.newCall(request).execute()
            parseResponse(response, type, customParser)
        } catch (e: Exception) {
            EhResult.failure(cause = e)
        }
    }

    internal fun <T> executeRequestAsync(
        request: Request,
        type: Type,
        customParser: ((String) -> T)? = null,
    ): CompletableFuture<EhResult<T>> {
        val future = CompletableFuture<EhResult<T>>()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.complete(EhResult.failure(cause = e))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = parseResponse(response, type, customParser)
                future.complete(result)
            }
        })
        return future
    }

    private fun <T> parseResponse(
        response: Response,
        type: Type,
        customParser: ((String) -> T)? = null,
    ): EhResult<T> {
        response.use { resp ->
            val code = resp.code
            val bodyString = resp.body?.string() ?: ""

            if (!resp.isSuccessful) {
                var errorMsg: String? = null
                try {
                    val json = JsonParser.parseString(bodyString).asJsonObject
                    if (json.has("error")) errorMsg = json.get("error").asString
                    else if (json.has("message")) errorMsg = json.get("message").asString
                } catch (_: Exception) {
                }
                return EhResult.failure(
                    httpCode = code,
                    message = errorMsg ?: "HTTP $code: ${resp.message}",
                    rawBody = bodyString,
                )
            }

            return try {
                val parsed: T = if (customParser != null) {
                    customParser(bodyString)
                } else {
                    gson.fromJson(bodyString, type)
                }
                EhResult.success(parsed, code)
            } catch (e: Exception) {
                EhResult.failure(
                    httpCode = code,
                    message = "Failed to parse response: ${e.message}",
                    rawBody = bodyString,
                    cause = e,
                )
            }
        }
    }

    // endregion

    // region 公共 API 接口 / Public Endpoints --------------------------------------------------------

    /**
     * 1. 获取漫画源配置信息。
     * GET /config
     */
    fun getConfig(): EhResult<EhSourceConfig> {
        val url = config.normalizedBaseUrl() + EhConstants.PATH_CONFIG
        val request = Request.Builder().url(url).get().build()
        return executeRequest(request, EhSourceConfig::class.java) { body ->
            parseSourceConfig(body)
        }
    }

    private fun parseSourceConfig(body: String): EhSourceConfig {
        val element = JsonParser.parseString(body)
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("E-Hentai")) {
                return gson.fromJson(obj.get("E-Hentai"), EhSourceConfig::class.java)
            }
            for ((_, v) in obj.entrySet()) {
                if (v.isJsonObject) {
                    val child = v.asJsonObject
                    if (child.has("apiUrl") || child.has("searchPath")) {
                        return gson.fromJson(child, EhSourceConfig::class.java)
                    }
                }
            }
            return gson.fromJson(obj, EhSourceConfig::class.java)
        }
        error("Unexpected config format: $body")
    }

    /**
     * 2. 搜索/浏览画廊列表。
     * - 直连官方模式：直接请求 E-Hentai / ExHentai 并解析 HTML 列表。
     * - 代理中转模式：请求 GET /search?q={query}&page={page}。
     *
     * @param query 搜索关键词或表达式 (如 language:chinese，为空时浏览最新画廊)
     * @param page 页码，从 1 开始
     */
    @JvmOverloads
    fun searchComics(query: String = "", page: Int = 1): EhResult<EhSearchResponse> {
        return if (config.isDirectSite()) {
            searchDirect(query, page)
        } else {
            searchProxy(query, page)
        }
    }

    private fun searchProxy(query: String, page: Int): EhResult<EhSearchResponse> {
        val httpUrl = (config.normalizedBaseUrl() + EhConstants.PATH_SEARCH).toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("page", page.toString())
            ?.build()
            ?: return EhResult.failure(message = "Invalid URL")

        val request = Request.Builder().url(httpUrl).get().build()
        return executeRequest(request, EhSearchResponse::class.java)
    }

    private fun searchDirect(query: String, page: Int): EhResult<EhSearchResponse> {
        val targetPage = (page - 1).coerceAtLeast(0)
        val httpUrl = getEffectiveBaseUrl().toHttpUrlOrNull()
            ?.newBuilder()
            ?.apply {
                if (query.isNotBlank()) {
                    addQueryParameter("f_search", query.trim())
                }
                if (targetPage > 0) {
                    addQueryParameter("page", targetPage.toString())
                }
            }
            ?.build()
            ?: return EhResult.failure(message = "Invalid URL")

        val request = Request.Builder().url(httpUrl).get().build()
        return executeRequest(request, EhSearchResponse::class.java) { html ->
            com.ehapi.parser.EhParser.parseGalleryList(html, requestedPage = page)
        }
    }

    /**
     * 使用 [EhQuery] 构建器搜索/浏览画廊列表。
     */
    fun searchComics(query: EhQuery): EhResult<EhSearchResponse> {
        return searchComics(query.toSearchQuery(), query.page)
    }

    /**
     * 3. 获取漫画详情。
     * - 直连官方模式：调用官方 JSON RPC api.php (gdata)，回退到抓取画廊 HTML 详情页。
     * - 代理中转模式：请求 GET /comic/{comicId}。
     *
     * @param comicId 漫画 ID，格式为 gid_token（如 "3645215_4db836130d"）或完整 URL
     */
    fun getComicDetail(comicId: String): EhResult<EhComicDetail> {
        return if (config.isDirectSite()) {
            getComicDetailDirect(comicId)
        } else {
            getComicDetailProxy(comicId)
        }
    }

    private fun getComicDetailProxy(comicId: String): EhResult<EhComicDetail> {
        val cleanId = comicId.trim().trim('/')
        val url = config.normalizedBaseUrl() + EhConstants.PATH_COMIC + "/" + cleanId
        val request = Request.Builder().url(url).get().build()
        return executeRequest(request, EhComicDetail::class.java)
    }

    private fun getComicDetailDirect(comicId: String): EhResult<EhComicDetail> {
        val parsed = parseComicId(comicId)
            ?: return EhResult.failure(message = "Invalid comicId format: $comicId (expected gid_token)")
        val (gid, token) = parsed

        // 1. 优先调用官方 JSON API (api.e-hentai.org/api.php)
        val metaRes = getGalleryMetadata(gid, token)
        if (metaRes is EhResult.Success) {
            val meta = metaRes.data
            val detail = EhComicDetail()
            detail.itemId = "${meta.gid}_${meta.token}"
            detail.name = meta.title ?: meta.titleJpn ?: "Gallery $gid"
            detail.cover = meta.thumb ?: ""
            detail.rate = meta.rating?.toDoubleOrNull() ?: 0.0
            val pCount = meta.filecount?.toIntOrNull() ?: 0
            detail.pageCount = pCount
            detail.totalChapters = if (pCount > 0) Math.ceil(pCount / 20.0).toInt().coerceAtLeast(1) else 1
            detail.tags = meta.tags ?: emptyList()
            return EhResult.success(detail)
        }

        // 2. 回退到直接抓取画廊页面
        val detailUrl = "${getEffectiveBaseUrl()}g/$gid/$token/"
        val request = Request.Builder().url(detailUrl).get().build()
        return executeRequest(request, EhComicDetail::class.java) { html ->
            com.ehapi.parser.EhParser.parseGalleryDetail(html, gid, token)
        }
    }

    /**
     * 4. 获取漫画指定虚拟章节的图片列表。
     * - 直连官方模式：抓取画廊预览页并解析图片链接。
     * - 代理中转模式：请求 GET /photo/{comicId}/{chapter}。
     *
     * @param comicId 漫画 ID，格式为 gid_token
     * @param chapter 虚拟章节序号，从 1 开始
     */
    @JvmOverloads
    fun getComicImages(comicId: String, chapter: Int = 1): EhResult<EhChapterImages> {
        return if (config.isDirectSite()) {
            getComicImagesDirect(comicId, chapter)
        } else {
            getComicImagesProxy(comicId, chapter)
        }
    }

    private fun getComicImagesProxy(comicId: String, chapter: Int = 1): EhResult<EhChapterImages> {
        val cleanId = comicId.trim().trim('/')
        val url = "${config.normalizedBaseUrl()}${EhConstants.PATH_PHOTO}/$cleanId/$chapter"
        val request = Request.Builder().url(url).get().build()
        return executeRequest(request, EhChapterImages::class.java)
    }

    private fun getComicImagesDirect(comicId: String, chapter: Int = 1): EhResult<EhChapterImages> {
        val parsed = parseComicId(comicId)
            ?: return EhResult.failure(message = "Invalid comicId format: $comicId (expected gid_token)")
        val (gid, token) = parsed
        val pageIndex = (chapter - 1).coerceAtLeast(0)
        val previewUrl = "${getEffectiveBaseUrl()}g/$gid/$token/?p=$pageIndex"
        val request = Request.Builder().url(previewUrl).get().build()
        return executeRequest(request, EhChapterImages::class.java) { html ->
            val previewItems = com.ehapi.parser.EhParser.parsePreviewImages(html)
            EhChapterImages("Part $chapter", previewItems)
        }
    }

    /**
     * 解析阅读器页面的大图真实下载地址。
     *
     * @param viewerUrl 单页阅读器网页链接 (如 https://e-hentai.org/s/xxx/yyy-z)
     */
    fun resolveViewerImage(viewerUrl: String): EhResult<String> {
        val request = Request.Builder().url(viewerUrl).get().build()
        return executeRequest(request, String::class.java) { html ->
            com.ehapi.parser.EhParser.parseViewerImageUrl(html)
                ?: error("Failed to parse full image from viewer page: $viewerUrl")
        }
    }

    /**
     * 一次性获取全本漫画所有虚拟章节的所有图片列表。
     *
     * @param comicId 漫画 ID
     * @param totalChapters 总章节数（可从 [EhComicDetail.getTotalChapters] 获得）
     */
    fun getAllChapterImages(comicId: String, totalChapters: Int): EhResult<List<EhImageItem>> {
        val allImages = mutableListOf<EhImageItem>()
        for (ch in 1..totalChapters) {
            val res = getComicImages(comicId, ch)
            if (res is EhResult.Failure) {
                return res
            }
            if (res is EhResult.Success) {
                allImages.addAll(res.data.images)
            }
        }
        return EhResult.success(allImages)
    }

    /**
     * 5. 服务端健康检查。
     * GET /health
     */
    fun health(): EhResult<EhHealthResponse> {
        val url = config.normalizedBaseUrl() + EhConstants.PATH_HEALTH
        val request = Request.Builder().url(url).get().build()
        return executeRequest(request, EhHealthResponse::class.java)
    }

    /**
     * 6. 请求图片代理服务获取二进制数据。
     * GET /image/proxy?url={rawUrl}&w={w}&q={q}&ifPNG={ifPNG}&ifLVGL={ifLVGL}
     */
    @JvmOverloads
    fun fetchProxyImage(
        rawImageUrl: String,
        width: Int? = config.defaultImageWidth,
        quality: Int? = config.defaultImageQuality,
        format: EhImageFormat? = config.defaultImageFormat,
        cropX: Int? = null,
        cropY: Int? = null,
        cropW: Int? = null,
        cropH: Int? = null,
    ): EhResult<ByteArray> {
        val proxyUrl = EhImages.buildProxyUrl(
            baseUrl = config.baseUrl,
            rawImageUrl = rawImageUrl,
            width = width,
            quality = quality,
            format = format,
            cropX = cropX,
            cropY = cropY,
            cropW = cropW,
            cropH = cropH,
        )
        return try {
            val bytes = downloader.fetchImageBytes(proxyUrl, client = httpClient)
            EhResult.success(bytes)
        } catch (e: Exception) {
            EhResult.failure(cause = e)
        }
    }

    /**
     * 请求图片代理服务并将结果原子保存至本地文件。
     */
    @JvmOverloads
    fun downloadProxyImage(
        rawImageUrl: String,
        targetFile: File,
        width: Int? = config.defaultImageWidth,
        quality: Int? = config.defaultImageQuality,
        format: EhImageFormat? = config.defaultImageFormat,
        cropX: Int? = null,
        cropY: Int? = null,
        cropW: Int? = null,
        cropH: Int? = null,
    ): EhResult<File> {
        val res = fetchProxyImage(rawImageUrl, width, quality, format, cropX, cropY, cropW, cropH)
        return when (res) {
            is EhResult.Success -> {
                try {
                    downloader.writeAtomically(targetFile, res.data)
                    EhResult.success(targetFile)
                } catch (e: Exception) {
                    EhResult.failure(cause = e)
                }
            }
            is EhResult.Failure -> res
        }
    }

    // endregion

    // region 官方 E-Hentai JSON API (gdata) ---------------------------------------------------------

    /**
     * 调用 E-Hentai 官方 JSON API 查询单个画廊的详细元数据。
     * POST https://api.e-hentai.org/api.php
     *
     * @param gid 画廊 ID
     * @param token 画廊 token
     */
    fun getGalleryMetadata(gid: Long, token: String): EhResult<EhGalleryMetadata> {
        val listRes = getGalleryMetadataList(listOf(gid to token))
        return when (listRes) {
            is EhResult.Success -> {
                val item = listRes.data.firstOrNull()
                if (item != null) {
                    EhResult.success(item, listRes.httpCode)
                } else {
                    EhResult.failure(message = "Gallery metadata not found for gid: $gid")
                }
            }
            is EhResult.Failure -> listRes
        }
    }

    /**
     * 批量查询画廊元数据。
     * POST https://api.e-hentai.org/api.php
     *
     * @param gidList 画廊 gid 与 token 对列表
     */
    fun getGalleryMetadataList(gidList: List<Pair<Long, String>>): EhResult<List<EhGalleryMetadata>> {
        val requestBody = GDataRequest().apply {
            gidList.forEach { (gid, token) -> addGallery(gid, token) }
        }
        val jsonPayload = gson.toJson(requestBody)
        val mediaType = EhConstants.MIME_JSON.toMediaType()
        val request = Request.Builder()
            .url(config.officialApiUrl)
            .post(jsonPayload.toRequestBody(mediaType))
            .build()

        val type = object : TypeToken<List<EhGalleryMetadata>>() {}.type
        return executeRequest(request, type) { body ->
            val root = JsonParser.parseString(body).asJsonObject
            if (root.has("error")) {
                throw IOException(root.get("error").asString)
            }
            val array = root.getAsJsonArray("gmetadata")
            gson.fromJson(array, type)
        }
    }

    /**
     * 辅助方法：从 ID 或 URL 中解析 gid 与 token。
     * 例如 "3645215_4db836130d" 或 "https://e-hentai.org/g/3645215/4db836130d/"。
     */
    fun parseComicId(input: String): Pair<Long, String>? {
        val trimmed = input.trim()
        if (trimmed.contains('_')) {
            val parts = trimmed.split('_')
            if (parts.size == 2) {
                val gid = parts[0].toLongOrNull()
                if (gid != null && parts[1].isNotBlank()) {
                    return gid to parts[1]
                }
            }
        }
        val matcher = GALLERY_URL_PATTERN.matcher(trimmed)
        if (matcher.find()) {
            val gid = matcher.group(1).toLongOrNull()
            val token = matcher.group(2)
            if (gid != null && !token.isNullOrBlank()) {
                return gid to token
            }
        }
        return null
    }

    companion object {
        private val GALLERY_URL_PATTERN = Pattern.compile("/g/(\\d+)/([a-f0-9]+)/?")

        /** 快速创建 [EhClient] 客户端。 / Factory method for Java callers. */
        @JvmStatic
        @JvmOverloads
        fun create(config: EhConfig = EhConfig()): EhClient = EhClient(config)
    }
}
