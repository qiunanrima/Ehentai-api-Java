package com.ehapi

import java.io.File

/**
 * Cookie 凭据持久化接口。应用可根据需要实现（如 Android 的 SharedPreferences / Room 等）。
 * Cookie persistence abstraction.
 */
interface EhCookieStore {
    /** 读取已保存的 Cookie 字符串，若无则返回 null。 / Loads saved cookie or null. */
    fun loadCookie(): String?

    /** 保存 Cookie 字符串；传入 null 或空白表示清除。 / Saves cookie; null or blank clears it. */
    fun saveCookie(cookie: String?)

    /** 是否已持有有效 Cookie。 / Whether a non-blank cookie exists. */
    fun hasCookie(): Boolean = !loadCookie().isNullOrBlank()
}

/**
 * 默认内存实现：仅保存在内存中，应用或进程重启后失效。
 * In-memory cookie store; lost on process restart.
 */
object MemoryEhCookieStore : EhCookieStore {
    @Volatile
    private var cookie: String? = null

    override fun loadCookie(): String? = cookie

    override fun saveCookie(cookie: String?) {
        this.cookie = cookie?.ifBlank { null }
    }
}

/**
 * 基于文件的 Cookie 持久化实现，适合 JVM 桌面应用或测试。
 * File-based cookie store, suitable for desktop/server JVM environments.
 */
class FileEhCookieStore(private val file: File) : EhCookieStore {

    override fun loadCookie(): String? = try {
        if (file.exists()) file.readText().trim().ifBlank { null } else null
    } catch (_: Exception) {
        null
    }

    override fun saveCookie(cookie: String?) {
        try {
            if (cookie.isNullOrBlank()) {
                if (file.exists()) file.delete()
            } else {
                file.parentFile?.mkdirs()
                file.writeText(cookie.trim())
            }
        } catch (_: Exception) {
            // 忽略写入失败，避免影响业务流程
        }
    }
}
