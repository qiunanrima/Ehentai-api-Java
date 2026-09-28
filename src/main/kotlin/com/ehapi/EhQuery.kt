package com.ehapi

/**
 * 漫画画廊检索查询参数实体。
 * Query parameters for comic searching and listing.
 *
 * 与 PicACG-Api-Java 的 ComicQuery 对齐，提供流畅的 Builder 与智能查询字符串组装。
 */
data class EhQuery @JvmOverloads constructor(
    /** 关键字（标题、作品名等）。 / Keyword query. */
    val keyword: String = "",
    /** 页码，从 1 开始。 / Page number, starting at 1. */
    val page: Int = 1,
    /** 分类名称（如 doujinshi, manga, artistcg, etc.）。 / Category filter. */
    val category: String? = null,
    /** 标签列表（如 female:big breasts, male:shotacon 等）。 / Tags filter. */
    val tags: List<String> = emptyList(),
    /** 上传者名称。 / Uploader filter. */
    val uploader: String? = null,
    /** 语言过滤（如 chinese, japanese, english）。 / Language filter. */
    val language: String? = null,
    /** 显式指定的原生查询串（若非空则优先使用）。 / Raw query override. */
    val rawQuery: String? = null,
) {
    /**
     * 将各过滤条件组装为 E-Hentai 的 `q` 检索表达式。
     * Builds standard E-Hentai search query string.
     */
    fun toSearchQuery(): String {
        if (!rawQuery.isNullOrBlank()) {
            return rawQuery.trim()
        }
        val parts = mutableListOf<String>()
        if (keyword.isNotBlank()) {
            parts.add(keyword.trim())
        }
        if (!language.isNullOrBlank()) {
            parts.add("language:${language.trim()}")
        }
        if (!category.isNullOrBlank()) {
            parts.add("category:${category.trim()}")
        }
        if (!uploader.isNullOrBlank()) {
            parts.add("uploader:${uploader.trim()}")
        }
        for (tag in tags) {
            val trimmed = tag.trim()
            if (trimmed.isNotEmpty()) {
                if (trimmed.contains(' ') && !trimmed.startsWith("\"") && !trimmed.endsWith("\"")) {
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0) {
                        val ns = trimmed.substring(0, colonIdx)
                        val name = trimmed.substring(colonIdx + 1)
                        parts.add("$ns:\"$name\"")
                    } else {
                        parts.add("\"$trimmed\"")
                    }
                } else {
                    parts.add(trimmed)
                }
            }
        }
        return parts.joinToString(" ")
    }

    /** 转换为 HTTP 请求参数映射。 / Converts to query map. */
    fun toQueryMap(): Map<String, Any?> = mapOf(
        "q" to toSearchQuery(),
        "page" to page,
    )

    fun toBuilder(): Builder = Builder()
        .keyword(keyword)
        .page(page)
        .category(category)
        .tags(tags)
        .uploader(uploader)
        .language(language)
        .rawQuery(rawQuery)

    class Builder {
        var keyword: String = ""
        var page: Int = 1
        var category: String? = null
        var tags: MutableList<String> = mutableListOf()
        var uploader: String? = null
        var language: String? = null
        var rawQuery: String? = null

        fun keyword(keyword: String) = apply { this.keyword = keyword }
        fun page(page: Int) = apply { this.page = page }
        fun category(category: String?) = apply { this.category = category }
        fun uploader(uploader: String?) = apply { this.uploader = uploader }
        fun language(language: String?) = apply { this.language = language }
        fun rawQuery(rawQuery: String?) = apply { this.rawQuery = rawQuery }

        fun addTag(tag: String) = apply {
            if (tag.isNotBlank()) this.tags.add(tag)
        }

        fun addTag(namespace: String, tag: String) = apply {
            if (tag.isNotBlank()) this.tags.add("$namespace:$tag")
        }

        fun tag(tag: String) = addTag(tag)

        fun tags(tags: List<String>) = apply {
            this.tags.clear()
            this.tags.addAll(tags)
        }

        fun tags(vararg tags: String) = apply {
            this.tags.clear()
            this.tags.addAll(tags)
        }

        fun build(): EhQuery = EhQuery(
            keyword = keyword,
            page = page,
            category = category,
            tags = tags.toList(),
            uploader = uploader,
            language = language,
            rawQuery = rawQuery,
        )
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        inline fun build(block: Builder.() -> Unit): EhQuery = Builder().apply(block).build()
    }
}
