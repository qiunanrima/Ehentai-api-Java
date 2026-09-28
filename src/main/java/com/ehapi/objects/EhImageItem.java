package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Objects;

/**
 * 章节单张图片信息。
 * Single image item in a chapter.
 */
public class EhImageItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("url")
    private String url;

    @SerializedName("crop_x")
    private Integer cropX;

    @SerializedName("crop_y")
    private Integer cropY;

    @SerializedName("crop_w")
    private Integer cropW;

    @SerializedName("crop_h")
    private Integer cropH;

    public EhImageItem() {
    }

    public EhImageItem(String url) {
        this.url = url;
    }

    public EhImageItem(String url, Integer cropX, Integer cropY, Integer cropW, Integer cropH) {
        this.url = url;
        this.cropX = cropX;
        this.cropY = cropY;
        this.cropW = cropW;
        this.cropH = cropH;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getCropX() {
        return cropX;
    }

    public void setCropX(Integer cropX) {
        this.cropX = cropX;
    }

    public Integer getCropY() {
        return cropY;
    }

    public void setCropY(Integer cropY) {
        this.cropY = cropY;
    }

    public Integer getCropW() {
        return cropW;
    }

    public void setCropW(Integer cropW) {
        this.cropW = cropW;
    }

    public Integer getCropH() {
        return cropH;
    }

    public void setCropH(Integer cropH) {
        this.cropH = cropH;
    }

    public boolean hasCrop() {
        return cropX != null && cropY != null && cropW != null && cropH != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhImageItem that = (EhImageItem) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(cropX, that.cropX) &&
                Objects.equals(cropY, that.cropY) &&
                Objects.equals(cropW, that.cropW) &&
                Objects.equals(cropH, that.cropH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, cropX, cropY, cropW, cropH);
    }

    @Override
    public String toString() {
        return "EhImageItem{" +
                "url='" + url + '\'' +
                ", cropX=" + cropX +
                ", cropY=" + cropY +
                ", cropW=" + cropW +
                ", cropH=" + cropH +
                '}';
    }
}
