package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 漫画搜索与列表响应。
 * Comic list / search response.
 */
public class EhSearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("page")
    private int page = 1;

    @SerializedName("has_more")
    private boolean hasMore;

    @SerializedName("results")
    private List<EhComicItem> results = new ArrayList<>();

    public EhSearchResponse() {
    }

    public EhSearchResponse(int page, boolean hasMore, List<EhComicItem> results) {
        this.page = page;
        this.hasMore = hasMore;
        this.results = results != null ? results : new ArrayList<>();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<EhComicItem> getResults() {
        return results;
    }

    public void setResults(List<EhComicItem> results) {
        this.results = results != null ? results : new ArrayList<>();
    }

    public int getCount() {
        return results != null ? results.size() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhSearchResponse that = (EhSearchResponse) o;
        return page == that.page &&
                hasMore == that.hasMore &&
                Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, hasMore, results);
    }

    @Override
    public String toString() {
        return "EhSearchResponse{" +
                "page=" + page +
                ", hasMore=" + hasMore +
                ", resultsCount=" + (results != null ? results.size() : 0) +
                '}';
    }
}
