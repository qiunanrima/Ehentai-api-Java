package com.ehapi.objects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Objects;

/**
 * 服务健康检查响应。
 * Health check response.
 */
public class EhHealthResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("status")
    private String status;

    @SerializedName("client_cookie_provided")
    private boolean clientCookieProvided;

    public EhHealthResponse() {
    }

    public EhHealthResponse(String status, boolean clientCookieProvided) {
        this.status = status;
        this.clientCookieProvided = clientCookieProvided;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isClientCookieProvided() {
        return clientCookieProvided;
    }

    public void setClientCookieProvided(boolean clientCookieProvided) {
        this.clientCookieProvided = clientCookieProvided;
    }

    public boolean isOk() {
        return "ok".equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EhHealthResponse that = (EhHealthResponse) o;
        return clientCookieProvided == that.clientCookieProvided &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, clientCookieProvided);
    }

    @Override
    public String toString() {
        return "EhHealthResponse{" +
                "status='" + status + '\'' +
                ", clientCookieProvided=" + clientCookieProvided +
                '}';
    }
}
