package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 漫画画廊详情。
 * Comic / gallery detailed information.
 */
public class EhComicDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("item_id")
    private String itemId;

    @SerializedName("name")
    private String name;

    @SerializedName("page_count")
    private int pageCount;

    @SerializedName("rate")
    private double rate;

    @SerializedName("cover")
    private String cover;

    @SerializedName("tags")
    private List<String> tags = new ArrayList<>();

    @SerializedName("total_chapters")
    private int totalChapters;

    public EhComicDetail() {
    }

    public EhComicDetail(String itemId, String name, int pageCount, double rate, String cover, List<String> tags, int totalChapters) {
        this.itemId = itemId;
        this.name = name;
        this.pageCount = pageCount;
        this.rate = rate;
        this.cover = cover;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.totalChapters = totalChapters;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public int getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }

    /**
     * 从 itemId (gid_token) 中提取 gid。
     * Extracts gid from itemId (format: gid_token).
     */
    public Long getGid() {
        if (itemId == null) return null;
        int idx = itemId.indexOf('_');
        if (idx > 0) {
            try {
                return Long.parseLong(itemId.substring(0, idx));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * 从 itemId (gid_token) 中提取 token。
     * Extracts token from itemId (format: gid_token).
     */
    public String getToken() {
        if (itemId == null) return null;
        int idx = itemId.indexOf('_');
        if (idx >= 0 && idx < itemId.length() - 1) {
            return itemId.substring(idx + 1);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhComicDetail that = (EhComicDetail) o;
        return pageCount == that.pageCount &&
                Double.compare(that.rate, rate) == 0 &&
                totalChapters == that.totalChapters &&
                Objects.equals(itemId, that.itemId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(cover, that.cover) &&
                Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, name, pageCount, rate, cover, tags, totalChapters);
    }

    @Override
    public String toString() {
        return "EhComicDetail{" +
                "itemId='" + itemId + '\'' +
                ", name='" + name + '\'' +
                ", pageCount=" + pageCount +
                ", rate=" + rate +
                ", cover='" + cover + '\'' +
                ", tagsCount=" + (tags != null ? tags.size() : 0) +
                ", totalChapters=" + totalChapters +
                '}';
    }
}
