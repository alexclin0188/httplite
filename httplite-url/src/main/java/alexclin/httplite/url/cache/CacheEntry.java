package alexclin.httplite.url.cache;

import alexclin.httplite.Response;

public class CacheEntry {
    private Response response;
    private String etag;
    private long softTtl;
    private long ttl;
    private long serverDate;
    private long lastModified;

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getSoftTtl() {
        return softTtl;
    }

    public void setSoftTtl(long softTtl) {
        this.softTtl = softTtl;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getServerDate() {
        return serverDate;
    }

    public void setServerDate(long serverDate) {
        this.serverDate = serverDate;
    }

    /** True if the entry is expired. */
    public boolean isExpired() {
        return this.ttl < System.currentTimeMillis();
    }

    /** True if a refresh is needed from the original data source. */
    public boolean refreshNeeded() {
        return this.softTtl < System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "CacheEntry{" +
                "response=" + response +
                ", etag='" + etag + '\'' +
                ", softTtl=" + softTtl +
                ", ttl=" + ttl +
                ", serverDate=" + serverDate +
                ", lastModified=" + lastModified +
                '}';
    }
}
