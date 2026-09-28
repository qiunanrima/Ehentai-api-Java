# 配置参考

---

## 目录

- [EhConfig 核心配置项](#ehconfig-核心配置项)
- [默认常量](#默认常量)
- [网络参数与代理](#网络参数与代理)
- [图片参数与输出格式](#图片参数与输出格式)
- [全局单例 Eh](#全局单例-eh)
- [Cookie 持久化 EhCookieStore](#cookie-持久化-ehcookiestore)

---

## EhConfig 核心配置项

`com.ehapi.EhConfig`，不可变数据类，所有参数均提供开箱即用的默认值。

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `baseUrl` | `String` | `http://127.0.0.1:8000/` | API 主机服务地址，会自动规范化以 `/` 结尾 |
| `cookie` | `String?` | `null` | 访问 ExHentai 或带权限资源所需的 HTTP Cookie 字符串 |
| `userAgent` | `String` | `wristcomic(1.8.0(18))/...` | 客户端 User-Agent，包含中文标识以便服务端自动汉化 |
| `timeoutSeconds` | `Long` | `30` | 网络连接与读写超时（秒） |
| `enableLogging` | `Boolean` | `false` | 是否开启 OkHttp 的 HTTP 完整请求/响应 Body 日志打印 |
| `disableSslVerification` | `Boolean` | `false` | 是否跳过 SSL 证书与主机名验证（用于自签名或测试环境） |
| `dnsIps` | `List<String>` | `[]` | 自定义 DNS IP 解析列表，非空时强制将域名解析至指定 IP |
| `proxyHost` | `String?` | `null` | HTTP 代理服务器地址 |
| `proxyPort` | `Int?` | `null` | HTTP 代理服务器端口 |
| `officialApiUrl` | `String` | `https://api.e-hentai.org/api.php` | 官方 E-Hentai JSON API 接口地址 |
| `defaultImageQuality` | `Int` | `50` | 默认图片压缩质量 (1-100) |
| `defaultImageWidth` | `Int` | `400` | 默认图片最大宽度 (px) |
| `defaultImageFormat` | `EhImageFormat` | `JPEG` | 默认图片格式 (`JPEG`、`PNG`、`LVGL`) |

---

## 默认常量

在 `EhConstants` 与 `EhConfig.Companion` 中维护：

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `DEFAULT_BASE_URL` | `http://127.0.0.1:8000/` | 默认代理服务地址 |
| `DEFAULT_OFFICIAL_API_URL` | `https://api.e-hentai.org/api.php` | E-Hentai 官方 JSON RPC 接口 |
| `SITE_EHENTAI_URL` | `https://e-hentai.org/` | E-Hentai 主站地址 |
| `SITE_EXHENTAI_URL` | `https://exhentai.org/` | ExHentai 里站地址 |
| `DEFAULT_USER_AGENT` | `wristcomic(1.8.0(18))/smartwatch/xiaomi/hyperos/1.0/1/zh/CN` | 符合 1.8 规范的 UA |

---

## 示例配置与使用

### Kotlin DSL

```kotlin
import com.ehapi.EhClient
import com.ehapi.objects.EhImageFormat

val client = EhClient {
    baseUrl = "https://your-api-domain.com/"
    cookie = "igneous=xxx; ipb_member_id=12345; ipb_pass_hash=yyy;"
    defaultImageQuality = 60
    defaultImageWidth = 500
    defaultImageFormat = EhImageFormat.PNG
    enableLogging = true
    timeoutSeconds = 45
}
```

### Java Fluent Builder

```java
import com.ehapi.EhClient;
import com.ehapi.EhConfig;
import com.ehapi.objects.EhImageFormat;

EhConfig config = EhConfig.builder()
    .baseUrl("https://your-api-domain.com/")
    .cookie("igneous=xxx; ipb_member_id=12345;")
    .defaultImageQuality(60)
    .defaultImageWidth(480)
    .defaultImageFormat(EhImageFormat.JPEG)
    .enableLogging(true)
    .dnsIps("1.1.1.1", "8.8.8.8")
    .proxy("127.0.0.1", 7890)
    .build();

EhClient client = EhClient.create(config);
```

### 运行时动态更新

```kotlin
// 切换 Cookie
client.updateCookie("igneous=new_value;")

// 更新整体配置
client.updateConfig(newConfig)
```

---

## 全局单例 Eh

与 `PicACG-Api-Java` 的 `Pica` 完全对齐，`Eh` 提供了跨界面共享单例的能力：

```kotlin
import com.ehapi.Eh
import com.ehapi.FileEhCookieStore
import java.io.File

// 应用启动时初始化单例，并持久化到本地文件
Eh.init(
    config = EhConfig.builder()
        .baseUrl("https://your-api-domain.com/")
        .build(),
    cookieStore = FileEhCookieStore(File(context.filesDir, "eh_cookie.txt"))
)

// 判断是否已持有 Cookie
if (Eh.hasCookie) {
    println("已载入 Cookie: ${Eh.cookie()}")
}

// 退出或清除 Cookie
Eh.clearCookie()
```
