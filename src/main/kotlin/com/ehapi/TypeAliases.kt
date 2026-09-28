package com.ehapi

import com.ehapi.objects.EhChapterImages
import com.ehapi.objects.EhComicDetail
import com.ehapi.objects.EhGalleryMetadata
import com.ehapi.objects.EhHealthResponse
import com.ehapi.objects.EhSearchResponse
import com.ehapi.objects.EhSourceConfig

/**
 * 常用操作返回类型的别名定义，简化复杂泛型声明。
 * Type aliases for common EhResult generic types.
 */
typealias EhSearchResult = EhResult<EhSearchResponse>
typealias EhDetailResult = EhResult<EhComicDetail>
typealias EhImagesResult = EhResult<EhChapterImages>
typealias EhConfigResult = EhResult<EhSourceConfig>
typealias EhHealthResult = EhResult<EhHealthResponse>
typealias EhMetadataResult = EhResult<EhGalleryMetadata>
typealias EhMetadataListResult = EhResult<List<EhGalleryMetadata>>
typealias EhBytesResult = EhResult<ByteArray>
