/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package alexclin.httplite.url.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.url.URLite;
import alexclin.httplite.util.Util;

/**
 * Utility methods for parsing HTTP headers.
 */
public class CacheEntryParser {

    private static final int ENTRY_METADATA = 0;
    private static final int ENTRY_DATA = 1;
    /**
     * Magic number for current version of cache file format.
     */
    private static final int CACHE_MAGIC = 0x20160221;

    public static CacheEntry parseCacheEntry(Response response) {
        long now = System.currentTimeMillis();

        Map<String, List<String>> headers = response.headers();

        long serverDate = 0;
        long lastModified = 0;
        long serverExpires = 0;
        long softExpire = 0;
        long finalExpire = 0;
        long maxAge = 0;
        long staleWhileRevalidate = 0;
        boolean hasCacheControl = false;
        boolean mustRevalidate = false;

        String serverEtag = null;
        String headerValue;

        List<String> headerValues = headers.get("Date");
        for (String value : headerValues) {
            headerValue = value;
            serverDate = parseDateAsEpoch(headerValue);
            if (serverDate != 0) break;
        }

        headerValues = headers.get("Cache-Control");
        if (headerValues != null && !headerValues.isEmpty()) {
            hasCacheControl = true;
            String[] tokens = headerValues.get(0).split(",");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (token.equals("no-cache") || token.equals("no-store")) {
                    return null;
                } else if (token.startsWith("max-age=")) {
                    try {
                        maxAge = Long.parseLong(token.substring(8));
                    } catch (Exception e) {
                    }
                } else if (token.startsWith("stale-while-revalidate=")) {
                    try {
                        staleWhileRevalidate = Long.parseLong(token.substring(23));
                    } catch (Exception e) {
                    }
                } else if (token.equals("must-revalidate") || token.equals("proxy-revalidate")) {
                    mustRevalidate = true;
                }
            }
        }

        headerValues = headers.get("Expires");
        for (String value : headerValues) {
            headerValue = value;
            serverExpires = parseDateAsEpoch(headerValue);
            if (serverExpires != 0) break;
        }

        headerValues = headers.get("Last-Modified");
        for (String value : headerValues) {
            headerValue = value;
            lastModified = parseDateAsEpoch(headerValue);
            if (lastModified != 0) break;
        }

        headerValues = headers.get("ETag");
        if (headerValues != null && !headerValues.isEmpty()) {
            serverEtag = headerValues.get(0);
        }

        // Cache-Control takes precedence over an Expires header, even if both exist and Expires
        // is more restrictive.
        if (hasCacheControl) {
            softExpire = now + maxAge * 1000;
            finalExpire = mustRevalidate
                    ? softExpire
                    : softExpire + staleWhileRevalidate * 1000;
        } else if (serverDate > 0 && serverExpires >= serverDate) {
            // Default semantic for Expire header in HTTP specification is softExpire.
            softExpire = now + (serverExpires - serverDate);
            finalExpire = softExpire;
        }

        CacheEntry entry = new CacheEntry();
        entry.setResponse(response);
        entry.setEtag(serverEtag);
        entry.setSoftTtl(softExpire);
        entry.setTtl(finalExpire);
        entry.setServerDate(serverDate);
        entry.setLastModified(lastModified);
        return entry;
    }

    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
    private static final String[] DEFAULT_PATTERNS = new String[]{PATTERN_RFC1036, PATTERN_RFC1123, PATTERN_ASCTIME};
    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    /**
     * Parse date in RFC1123 format, and return its value as epoch
     */
    public static long parseDateAsEpoch(String dateStr) {
        for (String pattern : DEFAULT_PATTERNS) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
                return dateFormat.parse(dateStr).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static String foramtDateAsEpoch(long time) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(PATTERN_RFC1123, Locale.ENGLISH);
            return dateFormat.format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static CacheEntry newEntry(DiskLruCache.Snapshot snapshot) throws IOException {
        CacheEntry entry = new CacheEntry();
        InputStream metaDataIn = snapshot.getInputStream(ENTRY_METADATA);
        IOUtil.readInt(metaDataIn);
        entry.setEtag(IOUtil.readString(metaDataIn));
        entry.setSoftTtl(IOUtil.readLong(metaDataIn));
        entry.setTtl(IOUtil.readLong(metaDataIn));
        entry.setServerDate(IOUtil.readLong(metaDataIn));
        entry.setLastModified(IOUtil.readLong(metaDataIn));
        return entry;
    }

    public static CacheEntry newEntry(DiskLruCache.Snapshot snapshot, ByteArrayPool pool, Request request) throws IOException {
        CacheEntry entry = newEntry(snapshot);
        InputStream dataIn = snapshot.getInputStream(ENTRY_DATA);
        int code = IOUtil.readInt(dataIn);
        String message = IOUtil.readString(dataIn);
        Map<String, List<String>> headers = IOUtil.readHeaders(dataIn);
        String mediaType = IOUtil.readString(dataIn);
        long contentLength = IOUtil.readLong(dataIn);
        byte[] bytes = IOUtil.streamToBytes(dataIn, (int) contentLength, pool);
        entry.setResponse(URLite.createResponse(code, message, headers, mediaType, contentLength,
                new PoolingStream(bytes, pool), request));
        return entry;
    }

    public static void writeEntryTo(CacheEntry entry, DiskLruCache.Editor editor) throws IOException {
        if (entry == null) return;
        OutputStream metaOut = editor.newOutputStream(ENTRY_METADATA);
        IOUtil.writeInt(metaOut, CACHE_MAGIC);
        IOUtil.writeString(metaOut, entry.getEtag() == null ? "" : entry.getEtag());
        IOUtil.writeLong(metaOut, entry.getSoftTtl());
        IOUtil.writeLong(metaOut, entry.getTtl());
        IOUtil.writeLong(metaOut, entry.getServerDate());
        IOUtil.writeLong(metaOut, entry.getLastModified());
        metaOut.flush();
        metaOut.close();

        OutputStream dataOut = editor.newOutputStream(ENTRY_DATA);
        Response response = entry.getResponse();
        IOUtil.writeInt(dataOut, response.code());
        IOUtil.writeString(dataOut, response.message() == null ? "" : response.message());
        IOUtil.writeHeaders(dataOut, response.headers());
        IOUtil.writeString(dataOut, response.body().contentType().toString());
        IOUtil.writeLong(dataOut, response.body().contentLength());
        Util.copy(response.body().stream(), dataOut);
        dataOut.flush();
        dataOut.close();
    }

    public static class PoolingStream extends ByteArrayInputStream {
        private ByteArrayPool pool;
        public PoolingStream(byte[] buf, ByteArrayPool pool) {
            super(buf);
            this.pool = pool;
        }

        @Override
        public void close() throws IOException {
            super.close();
            pool.returnBuf(this.buf);
        }
    }
}
