package com.ehapi.parser

import com.ehapi.objects.EhChapterImages
import com.ehapi.objects.EhComicDetail
import com.ehapi.objects.EhComicItem
import com.ehapi.objects.EhImageItem
import com.ehapi.objects.EhSearchResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * E-Hentai 原生 HTML 解析器。
 * Native HTML parser for E-Hentai and ExHentai web pages.
 *
 * 支持 E-Hentai 的各种列表模式（表格极简/扩展视图与缩略图网格视图）、画廊详情页与章节预览页。
 */
object EhParser {

    private val PATTERN_GALLERY_URL = Pattern.compile("/g/(\\d+)/([a-f0-9]+)/?")
    private val PATTERN_RATING = Pattern.compile("background-position:\\s*(-?\\d+)px")
    private val PATTERN_PAGES = Pattern.compile("(\\d+)\\s+pages?", Pattern.CASE_INSENSITIVE)
    private val PATTERN_NEXT_ID = Pattern.compile("[?&]next=(\\d+)")
    private val PATTERN_STYLE_URL = Pattern.compile("url\\((.+?)\\)")
    private val PATTERN_STYLE_SPRITE = Pattern.compile("width:(\\d+)px;height:(\\d+)px;.*background:.*?url\\(([^)]+)\\)\\s+(-?\\d+)px\\s+(-?\\d+)px")

    /**
     * 解析画廊列表页面（支持表格视图 Minimal/Compact/Extended 与网格视图 Thumbnail）。
     *
     * @param html 页面 HTML 源码
     * @param requestedPage 请求的当前页码
     * @return [EhSearchResponse]
     */
    fun parseGalleryList(html: String, requestedPage: Int = 1): EhSearchResponse {
        val doc = Jsoup.parse(html)
        val galleries = mutableListOf<EhComicItem>()

        // 1. 尝试解析表格模式 (table.itg - Minimal / Compact / Extended)
        val tableRows = doc.select("table.itg tr")
        for (row in tableRows) {
            val nameCell = row.selectFirst("td.glname, td.gl3c, td.gl3m") ?: continue
            val linkTag = nameCell.selectFirst("a[href*=/g/]") ?: continue
            val href = linkTag.attr("href")
            val (gid, token) = extractGidAndToken(href) ?: continue

            val titleElem = linkTag.selectFirst("div.glink") ?: linkTag
            val title = titleElem.text().trim()

            // 封面缩略图
            var thumbUrl: String? = null
            val thumbCell = row.selectFirst("td.gl2c, td.glthumb, td.gl1c")
            if (thumbCell != null) {
                val img = thumbCell.selectFirst("img")
                thumbUrl = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                    ?: img?.attr("src")?.takeIf { it.isNotBlank() }
            }

            // 页数
            var pages: Int? = null
            val textToSearch = row.text()
            val pagesMatcher = PATTERN_PAGES.matcher(textToSearch)
            if (pagesMatcher.find()) {
                pages = pagesMatcher.group(1).toIntOrNull()
            }

            val item = EhComicItem(
                "${gid}_$token",
                title,
                thumbUrl ?: "",
                pages ?: 0,
            )
            galleries.add(item)
        }

        // 2. 如果表格模式未匹配到任何结果，尝试解析缩略图网格视图 (div.itg.gld -> div.gl1t)
        if (galleries.isEmpty()) {
            val gridItems = doc.select("div.itg div.gl1t, div.gl1t")
            for (elem in gridItems) {
                val linkTag = elem.selectFirst("div.gl3t a[href*=/g/], a[href*=/g/]") ?: continue
                val href = linkTag.attr("href")
                val (gid, token) = extractGidAndToken(href) ?: continue

                val titleElem = elem.selectFirst("div.gl4t div.glink, div.glink")
                val title = titleElem?.text()?.trim()
                    ?: elem.selectFirst("img")?.attr("alt")?.trim()
                    ?: "Gallery $gid"

                val img = elem.selectFirst("div.gl3t img, img")
                val thumbUrl = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                    ?: img?.attr("src")?.takeIf { it.isNotBlank() }

                var pages: Int? = null
                val pagesMatcher = PATTERN_PAGES.matcher(elem.text())
                if (pagesMatcher.find()) {
                    pages = pagesMatcher.group(1).toIntOrNull()
                }

                val item = EhComicItem(
                    "${gid}_$token",
                    title,
                    thumbUrl ?: "",
                    pages ?: 0,
                )
                galleries.add(item)
            }
        }

        // 3. 判断是否有下一页 (分页导航 table.ptt 或 div.searchnav)
        var hasNext = false
        val ptt = doc.selectFirst("table.ptt, div.searchnav")
        if (ptt != null) {
            val nextLink = ptt.selectFirst("a#unext")
                ?: ptt.select("td").lastOrNull()?.selectFirst("a")
            if (nextLink != null && nextLink.hasAttr("href")) {
                val href = nextLink.attr("href")
                hasNext = href.contains("page=") || href.contains("next=") || href.contains("?p=")
            }
        }

        return EhSearchResponse(
            requestedPage,
            hasNext,
            galleries,
        )
    }

    /**
     * 解析画廊详情页面 (https://e-hentai.org/g/{gid}/{token}/)。
     */
    fun parseGalleryDetail(html: String, gid: Long, token: String): EhComicDetail {
        val doc = Jsoup.parse(html)
        val comicId = "${gid}_$token"

        val title = doc.selectFirst("#gn")?.text()?.trim()
            ?: doc.selectFirst("h1")?.text()?.trim()
            ?: "Gallery $gid"
        val titleJpn = doc.selectFirst("#gj")?.text()?.trim()

        // 封面图
        var coverUrl: String? = null
        val thumbDiv = doc.selectFirst("#gd1 div")
        if (thumbDiv != null) {
            val style = thumbDiv.attr("style")
            val m = PATTERN_STYLE_URL.matcher(style)
            if (m.find()) {
                coverUrl = m.group(1).trim('\'', '"')
            }
        }
        if (coverUrl.isNullOrBlank()) {
            coverUrl = doc.selectFirst("#gd1 img")?.attr("src")
        }

        // 分类
        val category = doc.selectFirst("#gdc a, #gdc")?.text()?.trim() ?: "Misc"

        // 评分
        var rating = 0.0
        val ratingElem = doc.selectFirst("#rating_label")
        if (ratingElem != null) {
            val text = ratingElem.text()
            val m = Pattern.compile("([\\d.]+)").matcher(text)
            if (m.find()) {
                rating = m.group(1).toDoubleOrNull() ?: 0.0
            }
        }

        // 页数
        var pageCount = 0
        val gddRows = doc.select("#gdd tr")
        for (row in gddRows) {
            val label = row.selectFirst("td.gdt1")?.text()?.trim() ?: ""
            val value = row.selectFirst("td.gdt2")?.text()?.trim() ?: ""
            if (label.contains("Length", ignoreCase = true)) {
                val m = Pattern.compile("(\\d+)").matcher(value)
                if (m.find()) {
                    pageCount = m.group(1).toIntOrNull() ?: 0
                }
                break
            }
        }

        // 标签解析
        val tags = mutableListOf<String>()
        val tagRows = doc.select("#taglist tr")
        for (tagRow in tagRows) {
            val namespace = tagRow.selectFirst("td.tc")?.text()?.trim()?.removeSuffix(":") ?: ""
            val tagLinks = tagRow.select("td div a")
            for (link in tagLinks) {
                val rawTag = link.text().trim()
                if (rawTag.isNotEmpty()) {
                    val fullTag = if (namespace.isNotEmpty()) "$namespace:$rawTag" else rawTag
                    tags.add(fullTag)
                }
            }
        }

        val totalChapters = if (pageCount > 0) ceil(pageCount / 20.0).toInt().coerceAtLeast(1) else 1

        val detail = EhComicDetail()
        detail.itemId = comicId
        detail.name = title
        detail.cover = coverUrl ?: ""
        detail.rate = rating
        detail.pageCount = pageCount
        detail.totalChapters = totalChapters
        detail.tags = tags
        return detail
    }

    /**
     * 解析画廊缩略图预览列表 (#gdt)。
     *
     * @param html 页面 HTML
     * @return 预览图项列表，若包含雪碧图切割参数则填充 crop 信息
     */
    fun parsePreviewImages(html: String): List<EhImageItem> {
        val doc = Jsoup.parse(html)
        val items = mutableListOf<EhImageItem>()
        val gdt = doc.selectFirst("#gdt") ?: return items

        val links = gdt.select("a[href*=/s/]")
        for (a in links) {
            val viewerPageUrl = a.attr("href")
            val div = a.selectFirst("div")
            if (div != null && div.hasAttr("style")) {
                val style = div.attr("style")
                val spriteMatcher = PATTERN_STYLE_SPRITE.matcher(style)
                if (spriteMatcher.find()) {
                    val w = spriteMatcher.group(1).toIntOrNull() ?: 100
                    val h = spriteMatcher.group(2).toIntOrNull() ?: 100
                    val thumbUrl = spriteMatcher.group(3)
                    val x = abs(spriteMatcher.group(4).toIntOrNull() ?: 0)
                    val y = abs(spriteMatcher.group(5).toIntOrNull() ?: 0)
                    items.add(EhImageItem(viewerPageUrl, x, y, w, h))
                    continue
                }
            }

            // 普通独立预览图片
            val img = a.selectFirst("img")
            val thumbUrl = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("src")?.takeIf { it.isNotBlank() }
            items.add(EhImageItem(viewerPageUrl ?: thumbUrl ?: ""))
        }

        return items
    }

    /**
     * 解析单张大图阅读页面 (#i3)，提取真实原图或全屏显示图片 URL。
     */
    fun parseViewerImageUrl(html: String): String? {
        val doc = Jsoup.parse(html)
        val img = doc.selectFirst("#i3 img#img, #i3 img, img#img")
        return img?.attr("src")?.takeIf { it.isNotBlank() }
    }

    /**
     * 从 URL 中提取 gid 与 token。
     */
    fun extractGidAndToken(url: String): Pair<Long, String>? {
        val m = PATTERN_GALLERY_URL.matcher(url)
        return if (m.find()) {
            val gid = m.group(1).toLongOrNull() ?: return null
            val token = m.group(2)
            gid to token
        } else {
            null
        }
    }
}
