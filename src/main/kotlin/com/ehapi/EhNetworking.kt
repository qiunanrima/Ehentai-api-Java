package com.ehapi

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * E-Hentai 网络底层策略封装：自定义 DNS、系统 TLS、跳过证书校验与 HTTP/SOCKS 代理配置。
 * Networking parameters for E-Hentai client.
 */
object EhNetworking {

    /**
     * 构造自定义 DNS 解析器。
     */
    @JvmStatic
    fun dns(ips: List<String>): Dns = Dns { hostname ->
        if (ips.isEmpty()) {
            Dns.SYSTEM.lookup(hostname)
        } else {
            ips.map { InetAddress.getByName(it) }
        }
    }

    /**
     * 应用 SSL 策略。
     */
    @JvmStatic
    fun applySslPolicy(builder: OkHttpClient.Builder, trustAll: Boolean): OkHttpClient.Builder {
        return if (trustAll) applyTrustAllSsl(builder) else builder
    }

    /**
     * 系统信任链 + TLSv1.2/1.3。
     */
    @JvmStatic
    fun applySystemTls(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        try {
            val factory = TlsSocketFactory()
            builder.sslSocketFactory(factory, factory.systemDefaultTrustManager())
        } catch (_: Throwable) {
            // Android 环境下如果提取自定义工厂受限，OkHttp 原生默认配置即为最佳系统信任链实现
        }
        return builder
    }

    /**
     * 信任所有证书与主机名。
     */
    @JvmStatic
    fun applyTrustAllSsl(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val trustAll = TrustAllManager()
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
        builder.sslSocketFactory(context.socketFactory, trustAll)
        builder.hostnameVerifier { _, _ -> true }
        return builder
    }

    /**
     * 配置网络代理与超时。
     */
    @JvmStatic
    fun applyProxy(builder: OkHttpClient.Builder, host: String?, port: Int?): OkHttpClient.Builder {
        if (!host.isNullOrBlank() && port != null && port > 0) {
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
        }
        return builder
    }

    @JvmStatic
    fun applyTimeouts(builder: OkHttpClient.Builder, timeoutSeconds: Long): OkHttpClient.Builder {
        builder.connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        builder.readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        builder.writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        return builder
    }

    private class TlsSocketFactory : SSLSocketFactory() {
        private val delegate: SSLSocketFactory

        init {
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf<TrustManager>(systemDefaultTrustManager()), null)
            delegate = context.socketFactory
        }

        fun systemDefaultTrustManager(): X509TrustManager {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            val managers = factory.trustManagers
            val x509 = managers?.filterIsInstance<X509TrustManager>()?.firstOrNull()
            return x509 ?: error("No X509TrustManager found in system default trust managers")
        }

        private fun configure(socket: Socket): Socket {
            if (socket is SSLSocket) {
                val supported = socket.supportedProtocols.toSet()
                val enabled = listOf("TLSv1.3", "TLSv1.2", "TLSv1.1").filter { it in supported }
                if (enabled.isNotEmpty()) socket.enabledProtocols = enabled.toTypedArray()
            }
            return socket
        }

        override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
        override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket =
            configure(delegate.createSocket(s, host, port, autoClose))
        override fun createSocket(host: String?, port: Int): Socket =
            configure(delegate.createSocket(host, port))
        override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
            configure(delegate.createSocket(host, port, localHost, localPort))
        override fun createSocket(host: InetAddress?, port: Int): Socket =
            configure(delegate.createSocket(host, port))
        override fun createSocket(
            address: InetAddress?,
            port: Int,
            localAddress: InetAddress?,
            localPort: Int,
        ): Socket = configure(delegate.createSocket(address, port, localAddress, localPort))
    }

    private class TrustAllManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
