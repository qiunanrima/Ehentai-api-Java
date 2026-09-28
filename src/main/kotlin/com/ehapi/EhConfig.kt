package com.ehapi

import com.ehapi.objects.EhImageFormat

/**
 * [EhClient] 的不可变配置实体。
 * Immutable configuration for [EhClient].
 *
 * 与 PicACG-Api-Java 结构完全对齐，支持 Fluent Builder 与 Kotlin DSL。
 */
data class EhConfig @JvmOverloads constructor(
    /** API 服务端根地址，必须以斜杠结尾。 / API host, ending with a slash. */
    val baseUrl: String = EhConstants.DEFAULT_BASE_URL,
    /** 可选的 Cookie 请求头，访问 ExHentai 或带权限资源时必须提供。 / Optional Cookie header. */
    val cookie: String? = null,
    /** `User-Agent` 请求头。 / `User-Agent` header. */
    val userAgent: String = EhConstants.DEFAULT_USER_AGENT,
    /** 请求超时时间（秒）。 / Timeout in seconds. */
    val timeoutSeconds: Long = 30L,
    /** 是否通过 HttpLoggingInterceptor 打印完整 HTTP 请求/响应日志。 / Log full request/response. */
    val enableLogging: Boolean = false,
    /** 是否跳过 SSL 证书与主机名校验。 / Skip SSL certificate and hostname verification. */
    val disableSslVerification: Boolean = false,
    /** 自定义 DNS IP 解析列表。 / Custom DNS IP list. */
    val dnsIps: List<String> = emptyList(),
    /** 代理服务器主机。 / HTTP/SOCKS Proxy host. */
    val proxyHost: String? = null,
    /** 代理服务器端口。 / Proxy port. */
    val proxyPort: Int? = null,
    /** 官方 E-Hentai JSON API 接口地址。 / Official E-Hentai API endpoint. */
    val officialApiUrl: String = EhConstants.DEFAULT_OFFICIAL_API_URL,
    /** 默认图片质量 (1-100)。 / Default image quality. */
    val defaultImageQuality: Int = 50,
    /** 默认图片宽度。 / Default image width. */
    val defaultImageWidth: Int = 400,
    /** 默认图片格式。 / Default image format. */
    val defaultImageFormat: EhImageFormat = EhImageFormat.JPEG,
) {
    /** 规范化 baseUrl，补全协议头并确保以斜杠结尾。 / Normalizes [baseUrl] so it always has scheme and ends with a slash. */
    fun normalizedBaseUrl(): String {
        var url = baseUrl.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }
        return if (url.endsWith("/")) url else "$url/"
    }

    /** 判断当前是否直接面向 E-Hentai / ExHentai 官方站（走原生解析而不是中转 REST 代理）。 */
    fun isDirectSite(): Boolean {
        val lower = normalizedBaseUrl().lowercase()
        return lower.contains("e-hentai.org") || lower.contains("exhentai.org")
    }

    /** 基于当前配置创建 Builder。 / Creates a [Builder] pre-populated with current settings. */
    fun toBuilder(): Builder = Builder()
        .baseUrl(baseUrl)
        .cookie(cookie)
        .userAgent(userAgent)
        .timeoutSeconds(timeoutSeconds)
        .enableLogging(enableLogging)
        .disableSslVerification(disableSslVerification)
        .dnsIps(dnsIps)
        .proxy(proxyHost, proxyPort)
        .officialApiUrl(officialApiUrl)
        .defaultImageQuality(defaultImageQuality)
        .defaultImageWidth(defaultImageWidth)
        .defaultImageFormat(defaultImageFormat)

    /** 供 Java 和 Kotlin 使用的流式配置构建器。 / Fluent builder for Java and Kotlin callers. */
    class Builder {
        var baseUrl: String = EhConstants.DEFAULT_BASE_URL
        var cookie: String? = null
        var userAgent: String = EhConstants.DEFAULT_USER_AGENT
        var timeoutSeconds: Long = 30L
        var enableLogging: Boolean = false
        var disableSslVerification: Boolean = false
        var dnsIps: List<String> = emptyList()
        var proxyHost: String? = null
        var proxyPort: Int? = null
        var officialApiUrl: String = EhConstants.DEFAULT_OFFICIAL_API_URL
        var defaultImageQuality: Int = 50
        var defaultImageWidth: Int = 400
        var defaultImageFormat: EhImageFormat = EhImageFormat.JPEG

        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun cookie(cookie: String?) = apply { this.cookie = cookie }
        fun userAgent(userAgent: String) = apply { this.userAgent = userAgent }
        fun timeoutSeconds(timeoutSeconds: Long) = apply { this.timeoutSeconds = timeoutSeconds }
        fun enableLogging(enableLogging: Boolean) = apply { this.enableLogging = enableLogging }
        fun disableSslVerification(disableSslVerification: Boolean) = apply { this.disableSslVerification = disableSslVerification }
        fun dnsIps(dnsIps: List<String>) = apply { this.dnsIps = dnsIps }
        fun dnsIps(vararg dnsIps: String) = apply { this.dnsIps = dnsIps.toList() }
        fun proxy(host: String?, port: Int?) = apply {
            this.proxyHost = host
            this.proxyPort = port
        }
        fun officialApiUrl(officialApiUrl: String) = apply { this.officialApiUrl = officialApiUrl }
        fun defaultImageQuality(defaultImageQuality: Int) = apply { this.defaultImageQuality = defaultImageQuality }
        fun defaultImageWidth(defaultImageWidth: Int) = apply { this.defaultImageWidth = defaultImageWidth }
        fun defaultImageFormat(defaultImageFormat: EhImageFormat) = apply { this.defaultImageFormat = defaultImageFormat }

        fun build(): EhConfig = EhConfig(
            baseUrl = baseUrl,
            cookie = cookie,
            userAgent = userAgent,
            timeoutSeconds = timeoutSeconds,
            enableLogging = enableLogging,
            disableSslVerification = disableSslVerification,
            dnsIps = dnsIps,
            proxyHost = proxyHost,
            proxyPort = proxyPort,
            officialApiUrl = officialApiUrl,
            defaultImageQuality = defaultImageQuality,
            defaultImageWidth = defaultImageWidth,
            defaultImageFormat = defaultImageFormat,
        )
    }

    companion object {
        /** 创建 Builder 实例。 / Creates a [Builder]. */
        @JvmStatic
        fun builder(): Builder = Builder()

        /** 获取默认配置。 / Creates a default [EhConfig]. */
        @JvmStatic
        fun create(): EhConfig = EhConfig()

        /** Kotlin DSL 构建配置。 / Kotlin DSL configuration builder. */
        inline fun build(block: Builder.() -> Unit): EhConfig = Builder().apply(block).build()
    }
}

/** Kotlin DSL 便捷顶层函数。 / Convenient Kotlin DSL top-level builder. */
inline fun ehConfig(block: EhConfig.Builder.() -> Unit): EhConfig = EhConfig.build(block)
