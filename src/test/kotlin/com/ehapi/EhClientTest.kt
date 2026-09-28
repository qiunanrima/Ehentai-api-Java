package com.ehapi

import com.ehapi.objects.EhImageFormat
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EhClientTest {

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun testConfigEndpoint() {
        val configJson = """
            {
              "E-Hentai": {
                "name": "E-Hentai",
                "apiUrl": "http://127.0.0.1:8000",
                "searchPath": "/search?q=<text>&page=<page>",
                "photoPath": "/photo/<id>/<chapter>",
                "detailPath": "/comic/<id>",
                "type": "ehentai"
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(configJson)
                .build()
        )

        val client = EhClient(EhConfig(baseUrl = server.url("/").toString()))
        val result = client.getConfig()

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()
        assertEquals("E-Hentai", config.name)
        assertEquals("http://127.0.0.1:8000", config.apiUrl)
        assertEquals("/photo/<id>/<chapter>", config.photoPath)
    }

    @Test
    fun testSearchComics() {
        val searchJson = """
            {
              "page": 1,
              "has_more": true,
              "results": [
                {
                  "comic_id": "3645215_4db836130d",
                  "title": "[Chinese] 画廊标题",
                  "cover_url": "http://test.com/cover.jpg",
                  "pages": 25
                }
              ]
            }
        """.trimIndent()

        server.enqueue(MockResponse.Builder().code(200).body(searchJson).build())

        val client = EhClient(EhConfig(baseUrl = server.url("/").toString()))
        val query = EhQuery.builder()
            .keyword("test")
            .language("chinese")
            .page(1)
            .build()

        val result = client.searchComics(query)
        assertTrue(result.isSuccess)
        val search = result.getOrThrow()
        assertEquals(1, search.page)
        assertTrue(search.isHasMore)
        assertEquals(1, search.results.size)
        assertEquals("3645215_4db836130d", search.results[0].comicId)
        assertEquals(3645215L, search.results[0].gid)
        assertEquals("4db836130d", search.results[0].token)

        val recorded = server.takeRequest()
        assertEquals("/search", recorded.url.encodedPath)
        assertEquals("test language:chinese", recorded.url.queryParameter("q"))
        assertEquals("1", recorded.url.queryParameter("page"))
    }

    @Test
    fun testComicImages() {
        val chapterJson = """
            {
              "title": "[Chinese] 画廊标题 - Part 1",
              "images": [
                { "url": "http://test.com/img1.jpg" },
                { "url": "http://test.com/img2.jpg" }
              ]
            }
        """.trimIndent()

        server.enqueue(MockResponse.Builder().code(200).body(chapterJson).build())

        val client = EhClient(EhConfig(baseUrl = server.url("/").toString()))
        val result = client.getComicImages("3645215_4db836130d", 1)

        assertTrue(result.isSuccess)
        val chapter = result.getOrThrow()
        assertEquals("[Chinese] 画廊标题 - Part 1", chapter.title)
        assertEquals(2, chapter.imageCount)
        assertEquals("http://test.com/img1.jpg", chapter.images[0].url)

        val recorded = server.takeRequest()
        assertEquals("/photo/3645215_4db836130d/1", recorded.url.encodedPath)
    }

    @Test
    fun testHealthEndpoint() {
        val healthJson = """
            {
              "status": "ok",
              "client_cookie_provided": true
            }
        """.trimIndent()

        server.enqueue(MockResponse.Builder().code(200).body(healthJson).build())

        val client = EhClient(EhConfig(baseUrl = server.url("/").toString(), cookie = "igneous=abc"))
        val result = client.health()

        assertTrue(result.isSuccess)
        val health = result.getOrThrow()
        assertTrue(health.isOk)
        assertTrue(health.isClientCookieProvided)

        val recorded = server.takeRequest()
        assertEquals("/health", recorded.url.encodedPath)
        assertEquals("igneous=abc", recorded.headers["Cookie"])
    }

    @Test
    fun testError404Handling() {
        server.enqueue(
            MockResponse.Builder()
                .code(404)
                .body("""{"error": "Gallery not found"}""")
                .build()
        )

        val client = EhClient(EhConfig(baseUrl = server.url("/").toString()))
        val result = client.getComicDetail("invalid_id")

        assertTrue(result.isFailure)
        assertEquals(404, result.getFailureOrNull()?.httpCode)
        assertEquals("Gallery not found", result.getFailureOrNull()?.message)
        assertThrows(EhException::class.java) {
            result.getOrThrow()
        }
    }

    @Test
    fun testParseComicIdHelper() {
        val client = EhClient()

        val parsed1 = client.parseComicId("3645215_4db836130d")
        assertNotNull(parsed1)
        assertEquals(3645215L, parsed1?.first)
        assertEquals("4db836130d", parsed1?.second)

        val parsed2 = client.parseComicId("https://e-hentai.org/g/3645215/4db836130d/")
        assertNotNull(parsed2)
        assertEquals(3645215L, parsed2?.first)
        assertEquals("4db836130d", parsed2?.second)

        val parsed3 = client.parseComicId("invalid_input_without_proper_format")
        assertNull(parsed3)
    }
}
