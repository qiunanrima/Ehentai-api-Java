package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 虚拟章节图片列表响应。
 * Images in a virtual chapter.
 */
public class EhChapterImages implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("title")
    private String title;

    @SerializedName("images")
    private List<EhImageItem> images = new ArrayList<>();

    public EhChapterImages() {
    }

    public EhChapterImages(String title, List<EhImageItem> images) {
        this.title = title;
        this.images = images != null ? images : new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<EhImageItem> getImages() {
        return images;
    }

    public void setImages(List<EhImageItem> images) {
        this.images = images != null ? images : new ArrayList<>();
    }

    public int getImageCount() {
        return images != null ? images.size() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhChapterImages that = (EhChapterImages) o;
        return Objects.equals(title, that.title) && Objects.equals(images, that.images);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, images);
    }

    @Override
    public String toString() {
        return "EhChapterImages{" +
                "title='" + title + '\'' +
                ", images=" + images +
                '}';
    }
}
