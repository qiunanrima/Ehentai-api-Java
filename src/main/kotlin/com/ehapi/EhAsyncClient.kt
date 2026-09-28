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
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * [EhClient] 的异步非阻塞客户端封装。
 * 所有方法均返回 [CompletableFuture]，完全对齐 PicACG-Api-Java 的 PicaAsyncClient。
 * 适用于 Android 避免在主线程发起同步网络请求，以及 Java 8+ / 17 异步响应式编程。
 */
class EhAsyncClient internal constructor(private val client: EhClient) {

    private val gson = Gson()

    /**
     * 1. 异步获取漫画源配置。
     */
    fun getConfig(): CompletableFuture<EhResult<EhSourceConfig>> {
        val url = client.config.normalizedBaseUrl() + EhConstants.PATH_CONFIG
        val request = Request.Builder().url(url).get().build()
        return client.executeRequestAsync(request, EhSourceConfig::class.java) { body ->
            val element = JsonParser.parseString(body)
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has("E-Hentai")) {
                    gson.fromJson(obj.get("E-Hentai"), EhSourceConfig::class.java)
                } else {
                    gson.fromJson(obj, EhSourceConfig::class.java)
                }
            } else {
                error("Unexpected config format")
            }
        }
    }

    /**
     * 2. 异步检索漫画列表。
     */
    /**
     * 2. 异步检索漫画列表。
     */
    @JvmOverloads
    fun searchComics(query: String = "", page: Int = 1): CompletableFuture<EhResult<EhSearchResponse>> {
        return CompletableFuture.supplyAsync { client.searchComics(query, page) }
    }

    /**
     * 使用 [EhQuery] 异步检索漫画列表。
     */
    fun searchComics(query: EhQuery): CompletableFuture<EhResult<EhSearchResponse>> {
        return searchComics(query.toSearchQuery(), query.page)
    }

    /**
     * 3. 异步获取画廊详情。
     */
    fun getComicDetail(comicId: String): CompletableFuture<EhResult<EhComicDetail>> {
        return CompletableFuture.supplyAsync { client.getComicDetail(comicId) }
    }

    /**
     * 4. 异步获取虚拟章节图片列表。
     */
    @JvmOverloads
    fun getComicImages(comicId: String, chapter: Int = 1): CompletableFuture<EhResult<EhChapterImages>> {
        return CompletableFuture.supplyAsync { client.getComicImages(comicId, chapter) }
    }

    /**
     * 异步解析单页阅读器大图真实下载地址。
     */
    fun resolveViewerImage(viewerUrl: String): CompletableFuture<EhResult<String>> {
        return CompletableFuture.supplyAsync { client.resolveViewerImage(viewerUrl) }
    }

    /**
     * 异步获取全本漫画所有虚拟章节的所有图片列表。
     */
    fun getAllChapterImages(comicId: String, totalChapters: Int): CompletableFuture<EhResult<List<EhImageItem>>> {
        return CompletableFuture.supplyAsync {
            client.getAllChapterImages(comicId, totalChapters)
        }
    }

    /**
     * 5. 异步健康检查。
     */
    fun health(): CompletableFuture<EhResult<EhHealthResponse>> {
        val url = client.config.normalizedBaseUrl() + EhConstants.PATH_HEALTH
        val request = Request.Builder().url(url).get().build()
        return client.executeRequestAsync(request, EhHealthResponse::class.java)
    }

    /**
     * 6. 异步请求图片代理服务获取二进制字节。
     */
    @JvmOverloads
    fun fetchProxyImage(
        rawImageUrl: String,
        width: Int? = client.config.defaultImageWidth,
        quality: Int? = client.config.defaultImageQuality,
        format: EhImageFormat? = client.config.defaultImageFormat,
        cropX: Int? = null,
        cropY: Int? = null,
        cropW: Int? = null,
        cropH: Int? = null,
    ): CompletableFuture<EhResult<ByteArray>> {
        return CompletableFuture.supplyAsync {
            client.fetchProxyImage(rawImageUrl, width, quality, format, cropX, cropY, cropW, cropH)
        }
    }

    /**
     * 异步下载代理图片并保存至本地文件。
     */
    @JvmOverloads
    fun downloadProxyImage(
        rawImageUrl: String,
        targetFile: File,
        width: Int? = client.config.defaultImageWidth,
        quality: Int? = client.config.defaultImageQuality,
        format: EhImageFormat? = client.config.defaultImageFormat,
        cropX: Int? = null,
        cropY: Int? = null,
        cropW: Int? = null,
        cropH: Int? = null,
    ): CompletableFuture<EhResult<File>> {
        return CompletableFuture.supplyAsync {
            client.downloadProxyImage(rawImageUrl, targetFile, width, quality, format, cropX, cropY, cropW, cropH)
        }
    }

    /**
     * 官方 E-Hentai gdata 异步查询单个画廊元数据。
     */
    fun getGalleryMetadata(gid: Long, token: String): CompletableFuture<EhResult<EhGalleryMetadata>> {
        return getGalleryMetadataList(listOf(gid to token)).thenApply { listRes ->
            when (listRes) {
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
    }

    /**
     * 官方 E-Hentai gdata 异步批量查询元数据。
     */
    fun getGalleryMetadataList(gidList: List<Pair<Long, String>>): CompletableFuture<EhResult<List<EhGalleryMetadata>>> {
        val requestBody = GDataRequest().apply {
            gidList.forEach { (gid, token) -> addGallery(gid, token) }
        }
        val jsonPayload = gson.toJson(requestBody)
        val mediaType = EhConstants.MIME_JSON.toMediaType()
        val request = Request.Builder()
            .url(client.config.officialApiUrl)
            .post(jsonPayload.toRequestBody(mediaType))
            .build()

        val type = object : TypeToken<List<EhGalleryMetadata>>() {}.type
        return client.executeRequestAsync(request, type) { body ->
            val root = JsonParser.parseString(body).asJsonObject
            if (root.has("error")) {
                throw IOException(root.get("error").asString)
            }
            val array = root.getAsJsonArray("gmetadata")
            gson.fromJson(array, type)
        }
    }
}
