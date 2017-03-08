package alexclin.httplite.impl;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.Response;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.exception.ParserException;
import alexclin.httplite.listener.ResponseParser;
import alexclin.httplite.util.Util;

/**
 * ObjectParser
 *
 * @author alexclin  16/4/1 22:25
 */
public class ObjectParser {
    private final Collection<ResponseParser> parsers;
    private final ResponseParser defaultParser;

    public ObjectParser(Collection<ResponseParser> parsers) {
        this.parsers = new ArrayList<>();
        this.defaultParser = new DefaultParser();
        if(parsers!=null)
            this.parsers.addAll(parsers);
        this.parsers.add(defaultParser);
    }

    private static boolean isSuccess(Response response) {
        int code = response.code();
        return code >= 200 && code < 300;
    }

    private static HttpException responseToException(Response response) throws HttpException {
        String message = response.message();
        if (TextUtils.isEmpty(message)) {
            try {
                message = decodeToString(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new HttpException(response.code(), message);
    }

    static String decodeToString(Response response) throws IOException {
        MediaType mt = response.body().contentType();
        if (mt != null) {
            BufferedReader reader = null;
            StringBuilder stringBuilder;
            try {
                Charset cs = mt.charset(Util.UTF_8);
                reader = new BufferedReader(new InputStreamReader(response.body().stream(), cs));
                stringBuilder = new StringBuilder();
                String s;
                while ((s = reader.readLine()) != null) {
                    stringBuilder.append(s);
                }
                return stringBuilder.toString();
            } finally {
                Util.closeQuietly(reader);
            }
        }
        throw new RuntimeException("Not text response body,no Content-Type in response");
    }

    public <T> T parseObject(Response response, Type type) throws Exception{
        Handle cancelable = response.request().handle();
        if(!isBaseType(type)&&!isSuccess(response)) throw responseToException(response);
        if(isBaseType(type)||isStringType(type)) return defaultParser.parseResponse(response, type);
        for (ResponseParser parser : parsers) {
            if (cancelable.isCanceled())
                throw new CanceledException("Canceled during parse");
            if (parser.isSupported(type)) {
                try {
                    return parser.parseResponse(response, type);
                } catch (Exception e) {
                    throw new ParserException(e);
                } finally {
                    Util.closeQuietly(response.body());
                }
            }
        }
        throw new ParserException("There is no ResponseParser in HttpLite support type:" + type);
    }

    private static boolean isBaseType(Type type){
        return Boolean.class.equals(type)||Integer.class.equals(type)
                ||Response.class.equals(type);
    }

    private static boolean isStringType(Type type) {
        return String.class.equals(type) || CharSequence.class.equals(type);
    }

    private static class DefaultParser implements ResponseParser{

        @Override
        public boolean isSupported(Type type) {
            return isStringType(type)||isBaseType(type);
        }

        @Override @SuppressWarnings("unchecked")
        public <T> T parseResponse(Response response, Type type) throws Exception {
            int code = response.code();
            if(type==Boolean.class){
                return (T)Boolean.valueOf(isSuccess(response));
            }else if(type==Integer.class){
                return (T)Integer.valueOf(code);
            }else if(type == Response.class)
                return (T)response;
            if(!isSuccess(response)) throw responseToException(response);
            Request request = response.request();
            if (InputStream.class.equals(type)) {
                return (T) response.body().stream();
            } else if (isStringType(type)) {
                return (T)decodeToString(response);
            }
            Request.DownloadParams downloadParams = request.getDownloadParams();
            if (downloadParams == null) {
                throw new IllegalArgumentException("Parse File-Type result but there is no downloadParams in request(HFRequest) ");
            }
            File downloadFile = downloadParams.getTargetFile();
            File parentDir = downloadParams.getParentDir();
            String msg = "";
            boolean suc = parentDir==null || parentDir.exists() || parentDir.mkdirs();
            if (suc) {
                if (!downloadFile.exists()) {
                    suc = downloadFile.createNewFile();
                }
                if (suc) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    try {
                        inputStream = new BufferedInputStream(response.body().stream());
                        if (isSupportRange(request,response)) {//断点续传文件下载
                            outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile, true));
                        } else {//普通文件下载
                            outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
                        }
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = inputStream.read(buf)) != -1) {
                            outputStream.write(buf, 0, len);
                            if (request.handle().isCanceled()) {
                                throw new IOException("Request has been canceled!");
                            }
                        }
                        return (T) downloadFile;
                    } finally {
                        Util.closeQuietly(inputStream);
                        Util.closeQuietly(outputStream);
                    }
                } else {
                    msg = "file :" + downloadFile + " create failed";
                }
            } else {
                msg = "dir:" + parentDir + " create failed";
            }
            throw new IllegalStateException(msg);
        }

        private static boolean isSupportRange(Request request, Response response) {
            Map<String, List<String>> headers = request.getHeaders();
            if(headers==null) return false;
            if((headers.get("RANGE")==null
                    || headers.get("RANGE").isEmpty())&&(headers.get("range")==null
                    || headers.get("range").isEmpty()))
                return false;
            if (response == null) return false;
            String ranges = response.header("Accept-Ranges");
            if (ranges != null) {
                return ranges.contains("bytes");
            }
            ranges = response.header("Content-Range");
            return ranges != null && ranges.contains("bytes");
        }
    }
}
