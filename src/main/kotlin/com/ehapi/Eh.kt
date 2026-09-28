package com.ehapi

import com.ehapi.objects.EhChapterImages
import com.ehapi.objects.EhComicDetail
import com.ehapi.objects.EhSearchResponse

/**
 * 全局单例管理器，跨页面与组件共享同一个 [EhClient] 实例与 Cookie 状态。
 * Global singleton manager that shares one [EhClient] and Cookie state across screens.
 *
 * 与 PicACG-Api-Java 的 Pica 单例完全对齐：
 * <ul>
 *   <li>线程安全单例初始化与 Cookie 自动恢复；</li>
 *   <li>静态方法便捷访问，Kotlin 与 Java 双端友好；</li>
 *   <li>支持自定义 CookieStore（如文件或内存持久化）；</li>
 *   <li>提供常用 API 便捷代理转发。</li>
 * </ul>
 *
 * ### 用法 / Usage
 * ```kotlin
 * // 启动时初始化一次 / initialize once at startup
 * Eh.init(EhConfig(cookie = "igneous=...;"), FileEhCookieStore(File("cookie.txt")))
 *
 * // 检索漫画 / search
 * val res = Eh.searchComics("language:chinese")
 *
 * // 获取详情 / detail
 * val detail = Eh.getComicDetail("3645215_4db836130d").getOrThrow()
 * ```
 */
object Eh {

    @Volatile
    private var _client: EhClient? = null

    @Volatile
    private var cookieStore: EhCookieStore = MemoryEhCookieStore

    /** 是否已完成初始化。 / Whether [init] has been called. */
    @JvmStatic
    val isInitialized: Boolean
        get() = _client != null

    /** 是否已持有有效 Cookie / 是否已登录。 / Whether a non-blank cookie exists. */
    @JvmStatic
    val isLoggedIn: Boolean
        get() = isInitialized && !_client?.cookie.isNullOrBlank()

    /** 是否已持有有效 Cookie（Java 端支持 Eh.hasCookie()）。 / Whether a non-blank cookie exists. */
    @JvmStatic
    @get:JvmName("hasCookie")
    val hasCookie: Boolean
        get() = isLoggedIn

    /** 全局同步客户端；若未显式初始化则自动使用默认配置初始化兜底，防止 Android 冷启动崩溃。 / Global client. */
    @JvmStatic
    val client: EhClient
        get() = _client ?: synchronized(this) {
            _client ?: init()
        }

    /** 全局异步客户端。 / Global async client. */
    @JvmStatic
    val async: EhAsyncClient
        get() = client.async

    /** 全局下载器。 / Global downloader utility. */
    @JvmStatic
    val downloader: EhDownloader
        get() = EhDownloader

    /**
     * 初始化全局单例。会自动从 [cookieStore] 中恢复持久化 Cookie。
     *
     * @return 新建的全局客户端
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        config: EhConfig = EhConfig(),
        cookieStore: EhCookieStore = MemoryEhCookieStore,
    ): EhClient {
        synchronized(this) {
            this.cookieStore = cookieStore
            val restoredCookie = cookieStore.loadCookie() ?: config.cookie
            val finalConfig = if (restoredCookie != config.cookie) {
                config.toBuilder().cookie(restoredCookie).build()
            } else {
                config
            }
            val newClient = EhClient(finalConfig)
            _client = newClient
            return newClient
        }
    }

    /**
     * Kotlin DSL: 使用配置块快速初始化单例。
     */
    inline fun init(
        cookieStore: EhCookieStore = MemoryEhCookieStore,
        configBlock: EhConfig.Builder.() -> Unit,
    ): EhClient = init(EhConfig.build(configBlock), cookieStore)

    /**
     * 保存 Cookie 并同步更新到内存中的客户端。
     */
    @JvmStatic
    fun saveCookie(cookie: String?) {
        cookieStore.saveCookie(cookie)
        _client?.updateCookie(cookie)
    }

    /**
     * 读取当前 Cookie 凭据。
     */
    @JvmStatic
    fun cookie(): String? = _client?.cookie

    /**
     * 清除当前 Cookie。
     */
    @JvmStatic
    fun clearCookie() {
        saveCookie(null)
    }

    // region 便捷静态方法转发 / Convenient Delegate Methods -----------------------------------------

    @JvmStatic
    @JvmOverloads
    fun searchComics(query: String = "", page: Int = 1): EhResult<EhSearchResponse> =
        client.searchComics(query, page)

    @JvmStatic
    fun searchComics(query: EhQuery): EhResult<EhSearchResponse> =
        client.searchComics(query)

    @JvmStatic
    fun getComicDetail(comicId: String): EhResult<EhComicDetail> =
        client.getComicDetail(comicId)

    @JvmStatic
    @JvmOverloads
    fun getComicImages(comicId: String, chapter: Int = 1): EhResult<EhChapterImages> =
        client.getComicImages(comicId, chapter)

    // endregion
}
