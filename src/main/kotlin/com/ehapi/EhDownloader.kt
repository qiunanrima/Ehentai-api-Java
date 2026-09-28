package com.ehapi

import com.ehapi.objects.EhImageItem
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * 下载进度状态实体。
 * Download progress state.
 */
data class DownloadProgress(
    val current: Int,
    val total: Int,
    val currentFile: File? = null,
) {
    val percentage: Float
        get() = if (total > 0) (current.toFloat() / total) * 100f else 0f

    val isFinished: Boolean
        get() = total > 0 && current >= total
}

/**
 * 下载进度监听函数式接口。
 * Download progress callback listener.
 */
fun interface DownloadProgressListener {
    fun onProgress(progress: DownloadProgress)
}

/**
 * E-Hentai 图片与章节批量下载工具，与 PicACG-Api-Java 的 PicaDownloader 完全对齐：
 * <ul>
 *   <li>跨平台（特别是 Windows）非法字符净化 [sanitizeFilename]；</li>
 *   <li>安全原子写入 [writeAtomically]（临时 `.tmp` 写入后原子替换，防止意外中断文件损坏）；</li>
 *   <li>单图原始数据抓取 [fetchImageBytes] 与文件保存 [downloadImage]；</li>
 *   <li>章节与画廊并发流水线多线程下载 [downloadChapter] 与实时进度回调。</li>
 * </ul>
 */
object EhDownloader {

    private val ILLEGAL_CHARACTERS_PATTERN = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]")

    private val defaultHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    /**
     * 过滤净化文件名中的非法字符，防止路径穿越及系统无法创建文件。
     */
    @JvmStatic
    @JvmOverloads
    fun sanitizeFilename(input: String?, replacement: String = "_"): String {
        if (input.isNullOrEmpty()) return ""
        return ILLEGAL_CHARACTERS_PATTERN.matcher(input).replaceAll(replacement).trim()
    }

    /**
     * 原子保存二进制数据至目标文件。
     */
    @JvmStatic
    fun writeAtomically(targetFile: File, bytes: ByteArray) {
        val parent = targetFile.parentFile ?: File(".")
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val tmpFile = File(parent, "${targetFile.name}.${UUID.randomUUID().toString().substring(0, 8)}.tmp")
        try {
            tmpFile.outputStream().use { it.write(bytes) }
            moveTmpToTarget(tmpFile, targetFile)
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    /**
     * 原子保存输入流至目标文件。
     */
    @JvmStatic
    fun writeAtomically(targetFile: File, inputStream: InputStream) {
        val parent = targetFile.parentFile ?: File(".")
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val tmpFile = File(parent, "${targetFile.name}.${UUID.randomUUID().toString().substring(0, 8)}.tmp")
        try {
            tmpFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }
            moveTmpToTarget(tmpFile, targetFile)
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    private fun moveTmpToTarget(tmpFile: File, targetFile: File) {
        try {
            Files.move(
                tmpFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                tmpFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    /**
     * 下载获取单张图片的二进制字节数组。
     */
    @JvmStatic
    @JvmOverloads
    fun fetchImageBytes(
        url: String,
        headers: Map<String, String>? = null,
        client: OkHttpClient = defaultHttpClient,
    ): ByteArray {
        val reqBuilder = Request.Builder().url(url)
        headers?.forEach { (k, v) -> reqBuilder.header(k, v) }
        val call = client.newCall(reqBuilder.build())
        val response = call.execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw IOException("Download failed with HTTP $code for url: $url")
        }
        return response.body?.bytes() ?: throw IOException("Empty response body for url: $url")
    }

    /**
     * 下载单张图片并原子写入指定目标文件。
     */
    @JvmStatic
    @JvmOverloads
    fun downloadImage(
        url: String,
        targetFile: File,
        headers: Map<String, String>? = null,
        client: OkHttpClient = defaultHttpClient,
    ): File {
        val bytes = fetchImageBytes(url, headers, client)
        writeAtomically(targetFile, bytes)
        return targetFile
    }

    /**
     * 并发下载虚拟章节的所有图片。
     *
     * @param images 章节图片列表
     * @param destinationDir 存储目录
     * @param filenamePrefix 文件名前缀，默认为 "page_"
     * @param listener 进度回调
     * @param concurrency 并发线程数，默认为 4
     * @param client 自定义 OkHttpClient 客户端
     * @return 按原顺序排序的下载后本地文件列表
     */
    @JvmStatic
    @JvmOverloads
    fun downloadChapter(
        images: List<EhImageItem>,
        destinationDir: File,
        filenamePrefix: String = "page_",
        listener: DownloadProgressListener? = null,
        concurrency: Int = 4,
        client: OkHttpClient = defaultHttpClient,
    ): List<File> {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        val total = images.size
        val completedCount = AtomicInteger(0)
        val fileMap = ConcurrentHashMap<Int, File>()
        val executor = Executors.newFixedThreadPool(concurrency.coerceIn(1, 16))

        try {
            val futures = images.mapIndexed { index, item ->
                CompletableFuture.runAsync({
                    val extension = inferExtension(item.url)
                    val filename = String.format("%s%03d.%s", filenamePrefix, index + 1, extension)
                    val targetFile = File(destinationDir, filename)
                    downloadImage(item.url, targetFile, client = client)
                    fileMap[index] = targetFile

                    val current = completedCount.incrementAndGet()
                    listener?.onProgress(DownloadProgress(current, total, targetFile))
                }, executor)
            }
            CompletableFuture.allOf(*futures.toTypedArray()).join()
        } finally {
            executor.shutdown()
        }

        return images.indices.mapNotNull { fileMap[it] }
    }

    private fun inferExtension(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val ext = cleanUrl.substringAfterLast('.', "")
        return if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else "jpg"
    }
}
