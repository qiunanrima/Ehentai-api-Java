package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 官方 E-Hentai gdata 请求体。
 * Request payload for official api.e-hentai.org/api.php gdata method.
 */
public class GDataRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("method")
    private String method = "gdata";

    @SerializedName("gidlist")
    private List<List<Object>> gidlist = new ArrayList<>();

    @SerializedName("namespace")
    private int namespace = 1;

    public GDataRequest() {
    }

    public GDataRequest(List<List<Object>> gidlist) {
        this.gidlist = gidlist;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<List<Object>> getGidlist() {
        return gidlist;
    }

    public void setGidlist(List<List<Object>> gidlist) {
        this.gidlist = gidlist;
    }

    public int getNamespace() {
        return namespace;
    }

    public void setNamespace(int namespace) {
        this.namespace = namespace;
    }

    public void addGallery(long gid, String token) {
        List<Object> item = new ArrayList<>(2);
        item.add(gid);
        item.add(token);
        gidlist.add(item);
    }
}
