package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * E-Hentai 官方 gdata 接口画廊元数据实体。
 * Official E-Hentai JSON API metadata entity (from api.e-hentai.org/api.php).
 */
public class EhGalleryMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("gid")
    private long gid;

    @SerializedName("token")
    private String token;

    @SerializedName("archiver_key")
    private String archiverKey;

    @SerializedName("title")
    private String title;

    @SerializedName("title_jpn")
    private String titleJpn;

    @SerializedName("thumb")
    private String thumb;

    @SerializedName("category")
    private String category;

    @SerializedName("uploader")
    private String uploader;

    @SerializedName("posted")
    private String posted;

    @SerializedName("filecount")
    private String filecount;

    @SerializedName("filesize")
    private long filesize;

    @SerializedName("expunged")
    private boolean expunged;

    @SerializedName("rating")
    private String rating;

    @SerializedName("torrentcount")
    private String torrentcount;

    @SerializedName("tags")
    private List<String> tags = new ArrayList<>();

    public EhGalleryMetadata() {
    }

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getArchiverKey() {
        return archiverKey;
    }

    public void setArchiverKey(String archiverKey) {
        this.archiverKey = archiverKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleJpn() {
        return titleJpn;
    }

    public void setTitleJpn(String titleJpn) {
        this.titleJpn = titleJpn;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public String getPosted() {
        return posted;
    }

    public void setPosted(String posted) {
        this.posted = posted;
    }

    public String getFilecount() {
        return filecount;
    }

    public void setFilecount(String filecount) {
        this.filecount = filecount;
    }

    public int getFileCountInt() {
        try {
            return Integer.parseInt(filecount);
        } catch (Exception e) {
            return 0;
        }
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public boolean isExpunged() {
        return expunged;
    }

    public void setExpunged(boolean expunged) {
        this.expunged = expunged;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public double getRatingDouble() {
        try {
            return Double.parseDouble(rating);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public String getTorrentcount() {
        return torrentcount;
    }

    public void setTorrentcount(String torrentcount) {
        this.torrentcount = torrentcount;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getComicId() {
        return gid + "_" + token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhGalleryMetadata that = (EhGalleryMetadata) o;
        return gid == that.gid &&
                filesize == that.filesize &&
                expunged == that.expunged &&
                Objects.equals(token, that.token) &&
                Objects.equals(title, that.title) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gid, token, title, category, filesize, expunged);
    }

    @Override
    public String toString() {
        return "EhGalleryMetadata{" +
                "gid=" + gid +
                ", token='" + token + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", filecount='" + filecount + '\'' +
                ", rating='" + rating + '\'' +
                '}';
    }
}
