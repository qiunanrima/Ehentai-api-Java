package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Objects;

/**
 * 漫画/画廊检索结果条目。
 * Single gallery search result item.
 */
public class EhComicItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("comic_id")
    private String comicId;

    @SerializedName("title")
    private String title;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("pages")
    private int pages;

    public EhComicItem() {
    }

    public EhComicItem(String comicId, String title, String coverUrl, int pages) {
        this.comicId = comicId;
        this.title = title;
        this.coverUrl = coverUrl;
        this.pages = pages;
    }

    public String getComicId() {
        return comicId;
    }

    public void setComicId(String comicId) {
        this.comicId = comicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    /**
     * 从 comicId (gid_token) 中提取 gid。
     * Extracts gid from comicId (format: gid_token).
     */
    public Long getGid() {
        if (comicId == null) return null;
        int idx = comicId.indexOf('_');
        if (idx > 0) {
            try {
                return Long.parseLong(comicId.substring(0, idx));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * 从 comicId (gid_token) 中提取 token。
     * Extracts token from comicId (format: gid_token).
     */
    public String getToken() {
        if (comicId == null) return null;
        int idx = comicId.indexOf('_');
        if (idx >= 0 && idx < comicId.length() - 1) {
            return comicId.substring(idx + 1);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhComicItem that = (EhComicItem) o;
        return pages == that.pages &&
                Objects.equals(comicId, that.comicId) &&
                Objects.equals(title, that.title) &&
                Objects.equals(coverUrl, that.coverUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comicId, title, coverUrl, pages);
    }

    @Override
    public String toString() {
        return "EhComicItem{" +
                "comicId='" + comicId + '\'' +
                ", title='" + title + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", pages=" + pages +
                '}';
    }
}
