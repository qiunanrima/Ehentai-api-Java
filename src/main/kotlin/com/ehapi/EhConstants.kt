package com.ehapi

/**
 * E-Hentai API 客户端核心常量。
 * Constants for E-Hentai API client.
 */
object EhConstants {
    /** 默认服务根地址：默认指向官方站，开箱即用无需依赖外部 Python 后端 / Default base URL */
    const val DEFAULT_BASE_URL = "https://e-hentai.org/"

    /** 本地 Python 代理后端默认服务地址 / Default local Python proxy service URL */
    const val DEFAULT_LOCAL_PROXY_URL = "http://127.0.0.1:8000/"

    /** 官方 E-Hentai JSON RPC API 接口地址 / Official E-Hentai JSON API endpoint */
    const val DEFAULT_OFFICIAL_API_URL = "https://api.e-hentai.org/api.php"

    /** E-Hentai 主站网页根地址 / E-Hentai main site */
    const val SITE_EHENTAI_URL = "https://e-hentai.org/"

    /** ExHentai 里站网页根地址 / ExHentai site */
    const val SITE_EXHENTAI_URL = "https://exhentai.org/"

    /** 默认用户代理，采用标准现代移动浏览器 UA，避免触发 Cloudflare WAF 对自定义/手表 UA 的拦截 */
    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    /** 手表/手环低功耗端历史 User-Agent */
    const val DEFAULT_WATCH_USER_AGENT = "wristcomic(1.8.0(18))/smartwatch/xiaomi/hyperos/1.0/1/zh/CN"

    /** EhTagTranslation 汉化数据库根地址 */
    const val TAG_TRANSLATION_BASE_URL = "https://raw.githubusercontent.com/EhTagTranslation/Database/master/database"

    // 请求头 / Headers
    const val HEADER_COOKIE = "Cookie"
    const val HEADER_USER_AGENT = "User-Agent"
    const val HEADER_ACCEPT = "Accept"
    const val HEADER_CONTENT_TYPE = "Content-Type"

    // 常用 MIME
    const val MIME_JSON = "application/json; charset=utf-8"

    // 请求路径 / Endpoints
    const val PATH_CONFIG = "config"
    const val PATH_SEARCH = "search"
    const val PATH_COMIC = "comic"
    const val PATH_PHOTO = "photo"
    const val PATH_IMAGE_PROXY = "image/proxy"
    const val PATH_HEALTH = "health"
}
