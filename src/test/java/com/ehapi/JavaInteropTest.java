package com.ehapi;

import com.ehapi.objects.*;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class JavaInteropTest {

    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.close();
    }

    @Test
    public void testEhConfigBuilder() {
        EhConfig config = EhConfig.builder()
                .baseUrl("http://127.0.0.1:8080/")
                .cookie("igneous=abc; ipb_member_id=123;")
                .userAgent("custom-ua")
                .enableLogging(true)
                .disableSslVerification(true)
                .dnsIps("1.1.1.1", "8.8.8.8")
                .defaultImageQuality(60)
                .defaultImageWidth(500)
                .defaultImageFormat(EhImageFormat.PNG)
                .build();

        assertEquals("http://127.0.0.1:8080/", config.getBaseUrl());
        assertEquals("igneous=abc; ipb_member_id=123;", config.getCookie());
        assertEquals("custom-ua", config.getUserAgent());
        assertTrue(config.getEnableLogging());
        assertTrue(config.getDisableSslVerification());
        assertEquals(List.of("1.1.1.1", "8.8.8.8"), config.getDnsIps());
        assertEquals(60, config.getDefaultImageQuality());
        assertEquals(500, config.getDefaultImageWidth());
        assertEquals(EhImageFormat.PNG, config.getDefaultImageFormat());

        EhConfig modified = config.toBuilder()
                .defaultImageQuality(40)
                .build();
        assertEquals(40, modified.getDefaultImageQuality());
        assertTrue(modified.getEnableLogging());
    }

    @Test
    public void testEhResultJavaInterop() {
        EhResult<String> success = EhResult.success("hello", 200);
        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertEquals("hello", success.getOrNull());
        assertEquals("hello", success.getOrDefault("world"));
        assertEquals("hello", success.getOrThrow());
        assertNull(success.getFailureOrNull());
        assertNull(success.exceptionOrNull());

        Optional<String> optional = Optional.ofNullable(success.getOrNull());
        assertTrue(optional.isPresent());
        assertEquals("hello", optional.get());

        AtomicReference<String> callbackValue = new AtomicReference<>();
        success.onSuccess(callbackValue::set)
                .onFailure(f -> fail("Should not be called"));
        assertEquals("hello", callbackValue.get());

        EhResult<Integer> mapped = success.map(String::length);
        assertEquals(5, mapped.getOrNull());

        EhResult<Integer> flatMapped = success.flatMap(s -> EhResult.success(s.length() * 2));
        assertEquals(10, flatMapped.getOrNull());

        String folded = success.fold(
                val -> "Success: " + val,
                err -> "Failure: " + err.getMessage()
        );
        assertEquals("Success: hello", folded);

        // Failure tests
        EhResult<String> failure = EhResult.failure(404, "NOT_FOUND", "Comic not found", null, null);
        assertFalse(failure.isSuccess());
        assertTrue(failure.isFailure());
        assertNull(failure.getOrNull());
        assertEquals("default", failure.getOrDefault("default"));
        assertEquals("fallback", failure.getOrElse(f -> "fallback"));
        assertNotNull(failure.getFailureOrNull());
        assertEquals("NOT_FOUND", failure.getFailureOrNull().getErrorCode());

        AtomicBoolean failureCalled = new AtomicBoolean(false);
        failure.onSuccess(d -> fail("Should not be called"))
                .onFailure(f -> failureCalled.set(true));
        assertTrue(failureCalled.get());

        assertThrows(EhException.class, failure::getOrThrow);
    }

    @Test
    public void testEhQueryBuilder() {
        EhQuery query = EhQuery.builder()
                .keyword("naruto")
                .language("chinese")
                .addTag("female", "big breasts")
                .page(2)
                .build();

        assertEquals("naruto", query.getKeyword());
        assertEquals("chinese", query.getLanguage());
        assertEquals(2, query.getPage());
        assertTrue(query.toSearchQuery().contains("naruto"));
        assertTrue(query.toSearchQuery().contains("language:chinese"));
        assertTrue(query.toSearchQuery().contains("female:\"big breasts\""));
        assertEquals(2, query.toQueryMap().get("page"));
    }

    @Test
    public void testEhClientOverloadsInJava() throws InterruptedException {
        String detailJson = "{\n" +
                "  \"item_id\": \"3645215_4db836130d\",\n" +
                "  \"name\": \"[Chinese] 画廊标题\",\n" +
                "  \"page_count\": 25,\n" +
                "  \"rate\": 4.5,\n" +
                "  \"cover\": \"http://example.com/cover.jpg\",\n" +
                "  \"tags\": [\"漫画\", \"汉语\"],\n" +
                "  \"total_chapters\": 2\n" +
                "}";

        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body(detailJson)
                .build());

        EhClient client = EhClient.create(
                EhConfig.builder().baseUrl(server.url("/").toString()).build()
        );

        EhResult<EhComicDetail> result = client.getComicDetail("3645215_4db836130d");
        assertTrue(result.isSuccess());
        EhComicDetail detail = result.getOrThrow();
        assertEquals("3645215_4db836130d", detail.getItemId());
        assertEquals(3645215L, detail.getGid());
        assertEquals("4db836130d", detail.getToken());
        assertEquals(25, detail.getPageCount());

        var recorded = server.takeRequest();
        assertEquals("/comic/3645215_4db836130d", recorded.getUrl().encodedPath());
    }

    @Test
    public void testAsyncClientInJava() throws ExecutionException, InterruptedException {
        String searchJson = "{\n" +
                "  \"page\": 1,\n" +
                "  \"has_more\": true,\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"comic_id\": \"3645215_4db836130d\",\n" +
                "      \"title\": \"Gallery Title\",\n" +
                "      \"cover_url\": \"http://example.com/c.jpg\",\n" +
                "      \"pages\": 20\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body(searchJson)
                .build());

        EhClient client = new EhClient(
                EhConfig.builder().baseUrl(server.url("/").toString()).build()
        );

        CompletableFuture<EhResult<EhSearchResponse>> future = client.async().searchComics("naruto", 1);
        EhResult<EhSearchResponse> result = future.get();
        assertTrue(result.isSuccess());
        EhSearchResponse search = result.getOrThrow();
        assertEquals(1, search.getPage());
        assertTrue(search.isHasMore());
        assertEquals(1, search.getResults().size());
        assertEquals(3645215L, search.getResults().get(0).getGid());
    }

    @Test
    public void testEhImagesHelpers() {
        String proxyUrl = EhImages.buildProxyUrl(
                "http://127.0.0.1:8000/",
                "https://raw.image.com/test.jpg",
                300,
                50,
                EhImageFormat.PNG,
                0,
                0,
                100,
                100
        );

        assertTrue(proxyUrl.contains("url=https%3A%2F%2Fraw.image.com%2Ftest.jpg"));
        assertTrue(proxyUrl.contains("w=300"));
        assertTrue(proxyUrl.contains("q=50"));
        assertTrue(proxyUrl.contains("ifPNG=1"));
        assertTrue(proxyUrl.contains("crop_x=0"));

        EhImageItem item = new EhImageItem("https://test.com/img.jpg");
        String itemUrl = EhImages.toProxyUrl(item, "http://127.0.0.1:8000/", 400, 50, EhImageFormat.JPEG);
        assertTrue(itemUrl.contains("w=400"));
    }

    @Test
    public void testEhDownloaderJavaInterop() {
        String safe = EhDownloader.sanitizeFilename("comic/test:name?*<>|");
        assertEquals("comic_test_name_____", safe);

        assertNotNull(EhDownloader.INSTANCE);
        EhClient client = new EhClient();
        assertNotNull(client.getDownloader());
        assertNotNull(Eh.getDownloader());
    }

    @Test
    public void testEhSingleton() {
        Eh.init(EhConfig.builder()
                .cookie("test_cookie")
                .baseUrl("http://127.0.0.1:8000/")
                .build());

        assertTrue(Eh.isInitialized());
        assertTrue(Eh.hasCookie());
        assertEquals("test_cookie", Eh.cookie());

        Eh.clearCookie();
        assertFalse(Eh.hasCookie());
        assertNull(Eh.cookie());
    }

    @Test
    public void testOfficialGDataApiInJava() throws InterruptedException {
        String gdataResponse = "{\n" +
                "  \"gmetadata\": [\n" +
                "    {\n" +
                "      \"gid\": 3645215,\n" +
                "      \"token\": \"4db836130d\",\n" +
                "      \"title\": \"Sample Gallery\",\n" +
                "      \"category\": \"Manga\",\n" +
                "      \"uploader\": \"artist\",\n" +
                "      \"filecount\": \"42\",\n" +
                "      \"rating\": \"4.80\",\n" +
                "      \"tags\": [\"language:chinese\", \"female:maid\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body(gdataResponse)
                .build());

        EhClient client = EhClient.create(
                EhConfig.builder().officialApiUrl(server.url("/api.php").toString()).build()
        );

        EhResult<EhGalleryMetadata> result = client.getGalleryMetadata(3645215L, "4db836130d");
        assertTrue(result.isSuccess());
        EhGalleryMetadata meta = result.getOrThrow();
        assertEquals(3645215L, meta.getGid());
        assertEquals("Sample Gallery", meta.getTitle());
        assertEquals(42, meta.getFileCountInt());
        assertEquals(4.80, meta.getRatingDouble(), 0.01);
        assertEquals(2, meta.getTags().size());
        assertEquals("3645215_4db836130d", meta.getComicId());

        var recorded = server.takeRequest();
        assertEquals("/api.php", recorded.getUrl().encodedPath());
        assertTrue(recorded.getBody().utf8().contains("gdata"));
    }
}
