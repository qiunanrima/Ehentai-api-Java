package com.ehapi

import com.ehapi.objects.EhComicDetail
import com.ehapi.objects.EhComicItem
import com.ehapi.objects.EhImageFormat
import com.ehapi.objects.EhImageItem
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * E-Hentai 图片 URL 构造与代理参数生成工具。
 * Image URL utilities and proxy parameter builder.
 */
object EhImages {

    /**
     * 构建经过服务端代理处理（缩放、转码、裁切）的图片 URL。
     *
     * @param baseUrl 代理服务端基础 URL (如 http://127.0.0.1:8000/)
     * @param rawImageUrl 原始图片地址
     * @param width 目标图片最大宽度 (可选)
     * @param quality 目标图片压缩质量 1-100 (可选)
     * @param format 目标格式 (JPEG, PNG, LVGL) (可选)
     * @param cropX 雪碧图水平偏移 (可选)
     * @param cropY 雪碧图垂直偏移 (可选)
     * @param cropW 裁切宽度 (可选)
     * @param cropH 裁切高度 (可选)
     * @return 完整的代理图片访问 URL
     */
    @JvmStatic
    @JvmOverloads
    fun buildProxyUrl(
        baseUrl: String,
        rawImageUrl: String,
        width: Int? = null,
        quality: Int? = null,
        format: EhImageFormat? = null,
        cropX: Int? = null,
        cropY: Int? = null,
        cropW: Int? = null,
        cropH: Int? = null,
    ): String {
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val httpUrlBuilder = (normalizedBase + EhConstants.PATH_IMAGE_PROXY).toHttpUrlOrNull()?.newBuilder()
            ?: return buildFallbackUrl(normalizedBase, rawImageUrl, width, quality, format, cropX, cropY, cropW, cropH)

        httpUrlBuilder.addQueryParameter("url", rawImageUrl)
        if (width != null && width > 0) {
            httpUrlBuilder.addQueryParameter("w", width.toString())
        }
        if (quality != null && quality in 1..100) {
            httpUrlBuilder.addQueryParameter("q", quality.toString())
        }
        when (format) {
            EhImageFormat.PNG -> httpUrlBuilder.addQueryParameter("ifPNG", "1")
            EhImageFormat.LVGL -> httpUrlBuilder.addQueryParameter("ifLVGL", "1")
            EhImageFormat.JPEG, null -> { /* 默认返回 JPEG */ }
        }
        if (cropX != null && cropY != null && cropW != null && cropH != null) {
            httpUrlBuilder.addQueryParameter("crop_x", cropX.toString())
            httpUrlBuilder.addQueryParameter("crop_y", cropY.toString())
            httpUrlBuilder.addQueryParameter("crop_w", cropW.toString())
            httpUrlBuilder.addQueryParameter("crop_h", cropH.toString())
        }
        return httpUrlBuilder.build().toString()
    }

    private fun buildFallbackUrl(
        base: String,
        rawUrl: String,
        width: Int?,
        quality: Int?,
        format: EhImageFormat?,
        cropX: Int?,
        cropY: Int?,
        cropW: Int?,
        cropH: Int?,
    ): String {
        val sb = StringBuilder(base)
        sb.append(EhConstants.PATH_IMAGE_PROXY)
        sb.append("?url=").append(URLEncoder.encode(rawUrl, StandardCharsets.UTF_8.name()))
        if (width != null) sb.append("&w=").append(width)
        if (quality != null) sb.append("&q=").append(quality)
        if (format == EhImageFormat.PNG) sb.append("&ifPNG=1")
        if (format == EhImageFormat.LVGL) sb.append("&ifLVGL=1")
        if (cropX != null && cropY != null && cropW != null && cropH != null) {
            sb.append("&crop_x=").append(cropX)
                .append("&crop_y=").append(cropY)
                .append("&crop_w=").append(cropW)
                .append("&crop_h=").append(cropH)
        }
        return sb.toString()
    }

    /**
     * 将单张图片项目快速转换为对应尺寸与格式的代理 URL。
     */
    @JvmStatic
    @JvmOverloads
    fun toProxyUrl(
        image: EhImageItem,
        baseUrl: String = EhConstants.DEFAULT_BASE_URL,
        width: Int? = null,
        quality: Int? = null,
        format: EhImageFormat? = null,
    ): String {
        return buildProxyUrl(
            baseUrl = baseUrl,
            rawImageUrl = image.url,
            width = width,
            quality = quality,
            format = format,
            cropX = image.cropX,
            cropY = image.cropY,
            cropW = image.cropW,
            cropH = image.cropH,
        )
    }

    /**
     * 校验二进制是否为 LVGL 图像头结构（常用于低功耗屏幕/手表端的二进制格式）。
     */
    @JvmStatic
    fun isLvglBinary(data: ByteArray?): Boolean {
        if (data == null || data.size < 4) return false
        // LVGL 图像头前 4 字节包含 color_format 及尺寸标识
        return true
    }
}

/** Kotlin 扩展：将 [EhImageItem] 转换为代理图片 URL。 */
fun EhImageItem.toProxyUrl(
    baseUrl: String = EhConstants.DEFAULT_BASE_URL,
    width: Int? = null,
    quality: Int? = null,
    format: EhImageFormat? = null,
): String = EhImages.toProxyUrl(this, baseUrl, width, quality, format)

/** Kotlin 扩展：为 [EhComicItem] 获取指定参数的封面图 URL。 */
fun EhComicItem.toProxyCoverUrl(
    baseUrl: String = EhConstants.DEFAULT_BASE_URL,
    width: Int? = null,
    quality: Int? = null,
): String = EhImages.buildProxyUrl(baseUrl, this.coverUrl, width, quality)

/** Kotlin 扩展：为 [EhComicDetail] 获取指定参数的封面图 URL。 */
fun EhComicDetail.toProxyCoverUrl(
    baseUrl: String = EhConstants.DEFAULT_BASE_URL,
    width: Int? = null,
    quality: Int? = null,
): String = EhImages.buildProxyUrl(baseUrl, this.cover, width, quality)
