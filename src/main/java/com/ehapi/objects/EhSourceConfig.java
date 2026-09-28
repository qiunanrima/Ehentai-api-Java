package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Objects;

/**
 * 漫画源配置信息。
 * Custom comic source configuration.
 */
public class EhSourceConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("name")
    private String name;

    @SerializedName("apiUrl")
    private String apiUrl;

    @SerializedName("searchPath")
    private String searchPath;

    @SerializedName("photoPath")
    private String photoPath;

    @SerializedName("detailPath")
    private String detailPath;

    @SerializedName("type")
    private String type;

    public EhSourceConfig() {
    }

    public EhSourceConfig(String name, String apiUrl, String searchPath, String photoPath, String detailPath, String type) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.searchPath = searchPath;
        this.photoPath = photoPath;
        this.detailPath = detailPath;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getSearchPath() {
        return searchPath;
    }

    public void setSearchPath(String searchPath) {
        this.searchPath = searchPath;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getDetailPath() {
        return detailPath;
    }

    public void setDetailPath(String detailPath) {
        this.detailPath = detailPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhSourceConfig that = (EhSourceConfig) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(apiUrl, that.apiUrl) &&
                Objects.equals(searchPath, that.searchPath) &&
                Objects.equals(photoPath, that.photoPath) &&
                Objects.equals(detailPath, that.detailPath) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, apiUrl, searchPath, photoPath, detailPath, type);
    }

    @Override
    public String toString() {
        return "EhSourceConfig{" +
                "name='" + name + '\'' +
                ", apiUrl='" + apiUrl + '\'' +
                ", searchPath='" + searchPath + '\'' +
                ", photoPath='" + photoPath + '\'' +
                ", detailPath='" + detailPath + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
