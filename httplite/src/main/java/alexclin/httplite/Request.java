package alexclin.httplite;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import alexclin.httplite.exception.IllegalOperationException;
import alexclin.httplite.impl.ProgressRequestBody;
import alexclin.httplite.impl.ProgressResponse;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.Response;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.Util;

/**
 * Request
 *
 * @author alexclin 16/1/31 10:21
 */
public final class Request{
    public static final int NO_CACHE = 0;
    public static final int FORCE_CACHE = -1;
    public static final int UNSPECIFIED_CACHE = -100;

    Builder mBuilder;

    private static final Comparator<Pair<String,Pair<String,Boolean>>> paramComparable = new Comparator<Pair<String, Pair<String, Boolean>>>() {

        @Override
        public int compare(Pair<String, Pair<String, Boolean>> lhs, Pair<String, Pair<String, Boolean>> rhs) {
            if(lhs.first.equals(rhs.first)){
                return lhs.second.first.compareTo(rhs.second.first);
            }
            return lhs.first.compareTo(rhs.first);
        }
    };

    private Request(Builder builder) {
        mBuilder = builder;
    }

    public String getUrl() {
        return mBuilder.buildUrlAndParams(mBuilder.baseUrl,mBuilder.url);
    }

    public Method getMethod() {
        return mBuilder.method;
    }

    public List<Pair<String,Pair<String,Boolean>>> getParams() {
        return mBuilder.params;
    }

    public RequestBody getBody() {
        if(mBuilder.progressListener!=null&&mBuilder.body!=null){
            if(mBuilder.progressBody==null||!mBuilder.progressBody.isWrappBody(mBuilder.body)){
                mBuilder.progressBody = new ProgressRequestBody(mBuilder.body,getMainProgressListener());
            }
            return mBuilder.progressBody;
        }
        return mBuilder.body;
    }

    Response handleResponse(Response response){
        if(mBuilder.progressListener!=null&&getDownloadParams()==null){
            return new ProgressResponse(response,getMainProgressListener());
        }
        return response;
    }

    private ProgressListener getMainProgressListener(){
        if(mBuilder.progressWrapper ==null){
            mBuilder.progressWrapper = new MainProgressListener(mBuilder.progressListener);
        }
        return mBuilder.progressWrapper;
    }

    public Object getTag() {
        return mBuilder.tag;
    }

    public ProgressListener getProgressListener() {
        return mBuilder.progressListener;
    }

    public RetryListener getRetryListener() {
        return mBuilder.retryListener;
    }

    public int getCacheExpiredTime() {
        return mBuilder.cacheExpiredTime;
    }

    public Handle download(Callback<File> callback){
        //TODO
//        return get().async(callback);
        return null;
    }

    public DownloadHandler.DownloadParams getDownloadParams() {
        return mBuilder.downloadParams;
    }

    public Object getMark(){
        return mBuilder.mark;
    }

    public Map<String, List<String>> getHeaders() {
        return mBuilder.headers;
    }

    private static class MainProgressListener implements ProgressListener{
        private ProgressListener listener;

        public MainProgressListener(ProgressListener listener) {
            this.listener = listener;
        }

        @Override
        public void onProgressUpdate(final boolean out,final long current,final long total) {
            HttpLite.postOnMain(new Runnable() {
                @Override
                public void run() {
                    listener.onProgressUpdate(out,current,total);
                }
            });
        }
    }

    HttpLite lite(){
        return mBuilder.lite;
    }

    public Call call(){
        return mBuilder.lite.makeCall(this.mBuilder);
    }

    @Override
    public String toString() {
        //TODO
        return null;
    }

    public static final class Builder implements Cloneable{
        private String baseUrl;
        private String url;
        Method method;
        private ProgressListener progressListener;
        private RetryListener retryListener;
        HttpLite lite;
        private Map<String,List<String>> headers;
        private List<Pair<String,Pair<String,Boolean>>> params;
        private RequestBody body;
        private ProgressRequestBody progressBody;
        private Object tag;
        private MainProgressListener progressWrapper;
        private int cacheExpiredTime = UNSPECIFIED_CACHE;

        private FormBuilder formBuilder;
        private MultipartBuilder multipartBuilder;

        private HashMap<String,Pair<String,Boolean>> pathHolders;

        private DownloadHandler.DownloadParams downloadParams;

        private Object mark;

        Builder(HttpLite lite) {
            this.lite = lite;
        }

        public Builder header(String name, String value) {
            List<String> values = createHeaders().get(name);
            if(values==null){
                values = new ArrayList<>();
                values.add(value);
            }else{
                values.add(value);
            }
            createHeaders().put(name, values);
            return this;
        }

        private Map<String,List<String>> createHeaders(){
            if(headers==null){
                headers = new HashMap<>();
            }
            return headers;
        }

        public Builder removeHeader(String name) {
            List<String> values = createHeaders().get(name);
            if(values!=null){
                values.remove(name);
            }
            return this;
        }

        public Builder headers(Map<String,List<String>> headers) {
            this.headers = headers;
            return this;
        }

        public Builder pathHolder(String key,String value){
            return pathHolder(key,value,false);
        }

        public Builder pathHolder(String key,String value, boolean encoded){
            if(key!=null&&value!=null) getPathHolders().put(key, new Pair<>(value, encoded));
            return this;
        }

        public Builder pathHolders(HashMap<String,String> pathHolders,boolean encoded){
            if(pathHolders!=null){
                for(String key:pathHolders.keySet()){
                    getPathHolders().put(key,new Pair<String, Boolean>(pathHolders.get(key),encoded));
                }
            }
            return this;
        }

        public Builder param(String name,String value){
            return param(name,value,false);
        }

        public Builder param(String name,Object value){
            return param(name,value==null?null:value.toString(),false);
        }

        public Builder param(String name,String value,boolean encoded){
            if(value==null) return this;
            if(params==null){
                params = new ArrayList<>();
            }
            params.add(new Pair<>(name, new Pair<>(value, encoded)));
            return this;
        }

        public Builder form(String name,String value){
            return form(name,value,false);
        }

        public Builder formEncoded(String name,String value){
            return form(name,value,true);
        }

        public Builder form(String name,String value,boolean encoded){
            initFormBuilder();
            if(encoded)
                formBuilder.addEncoded(name, value);
            else
                formBuilder.add(name, value);
            return this;
        }

        public Builder multipart(String name,String value){
            initMultiPartBuilder();
            multipartBuilder.add(name, value);
            return this;
        }

        public Builder multipartType(MediaType type){
            initMultiPartBuilder();
            multipartBuilder.setType(type);
            return this;
        }

        public Builder multipart(RequestBody body){
            initMultiPartBuilder();
            multipartBuilder.add(body);
            return this;
        }

        public Builder multipart(Map<String,List<String>> headers,RequestBody body){
            initMultiPartBuilder();
            multipartBuilder.add(headers,body);
            return this;
        }

        public Builder multipart(String name, String fileName, RequestBody body){
            initMultiPartBuilder();
            multipartBuilder.add(name,fileName,body);
            return this;
        }

        public Builder multipart(String name, String fileName, File file){
            return multipart(name, fileName, lite.createRequestBody(Util.guessMediaType(lite, file), file));
        }

        public Builder multipart(String name, String fileName, File file,String mediaType){
            return multipart(name, fileName, lite.createRequestBody(lite.parse(mediaType), file));
        }

        public Builder body(String mediaType,String content){
            if(content==null) return this;
            this.body = lite.createRequestBody(lite.parse(mediaType), content);
            return this;
        }

        public Builder body(String mediaType,File file){
            if(TextUtils.isEmpty(mediaType)){
                this.body = lite.createRequestBody(Util.guessMediaType(lite,file), file);
            }else{
                this.body = lite.createRequestBody(lite.parse(mediaType), file);
            }
            return this;
        }

        public Builder body(RequestBody body){
            this.body = body;
            return this;
        }

        public Builder cacheExpire(int expire){
            this.cacheExpiredTime = expire;
            return this;
        }

        public Builder intoFile(String path,boolean autoResume){
            this.downloadParams = checkAndCreateDownload(path,null,autoResume,true);
            return this;
        }

        public Builder intoFile(String path, boolean autoResume,boolean autoRename){
            this.downloadParams = checkAndCreateDownload(path, null, autoResume, autoRename);
            return this;
        }

        public Builder intoFile(String path,String fileName,boolean autoResume,boolean autoRename){
            this.downloadParams = checkAndCreateDownload(path,fileName,autoResume,autoRename);
            return this;
        }

        private DownloadHandler.DownloadParams checkAndCreateDownload(String path,String fileName,boolean autoResume,boolean autoRename){
            DownloadHandler.DownloadParams params = DownloadHandler.createParams(path, fileName, autoResume, autoRename,url);
            if(params==null){
                String info = String.format("call intoFile() with wrong params->path:%s,fileName:%s,resume:%b,rename:%b",path,fileName,autoResume,autoRename);
                throw new IllegalArgumentException(info);
            }
            return params;
        }

        public Builder get() {
            return method(Method.GET, null);
        }

        public Builder head() {
            return method(Method.HEAD, null);
        }

        public Builder post(RequestBody body) {
            return method(Method.POST, body);
        }

        public Builder post(){
            return method(Method.POST, null);
        }

        public Builder post(String mediaType,String content){
            return method(Method.POST, lite.createRequestBody(lite.parse(mediaType), content));
        }

        public Builder post(String mediaType,File file){
            return method(Method.POST, lite.createRequestBody(lite.parse(mediaType), file));
        }

        public Builder delete(RequestBody body) {
            return method(Method.DELETE, body);
        }

        public Builder delete() {
            return delete(lite.createRequestBody(null, new byte[0]));
        }

        public Builder put(RequestBody body) {
            return method(Method.PUT, body);
        }

        public Builder patch(RequestBody body) {
            return method(Method.PATCH, body);
        }

        public Builder method(Method method, RequestBody body) {
            preWorkForTask(body);
            boolean isBodyNull = body==null&&this.body==null;
            if (method == null) {
                throw new IllegalArgumentException("method == null");
            }
            if (!isBodyNull && !method.permitsRequestBody) {
                throw new IllegalArgumentException("method " + method + " must not have a call body.");
            }
            if (isBodyNull && method.requiresRequestBody) {
                throw new IllegalArgumentException("method " + method + " must have a call body.");
            }
            this.method = method;
            if(body!=null){
                this.body = body;
            }
            checkContentType();
            return this;
        }

        public Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Builder onProgress(ProgressListener listener){
            this.progressListener = listener;
            return this;
        }

        public Builder onRetry(RetryListener listener){
            this.retryListener = listener;
            return this;
        }

        private void initFormBuilder(){
            if(multipartBuilder!=null){
                throw new IllegalOperationException("You cannot call form-Method after you have called multipart method on call");
            }
            if(body!=null){
                throw new IllegalOperationException("You cannot call form-Method after you have set RequestBody on call");
            }
            if(formBuilder == null){
                formBuilder = new FormBuilder();
            }
        }

        private void initMultiPartBuilder(){
            if(formBuilder!=null){
                throw new IllegalOperationException("You cannot call multipart-method after you have called form-method on call");
            }
            if(body!=null){
                throw new IllegalOperationException("You cannot call multipart-method after you have set RequestBody on call");
            }
            if(multipartBuilder==null){
                multipartBuilder = new MultipartBuilder();
            }
        }

        private void preWorkForTask(RequestBody body) {
            if((this.body!=null||body!=null)&&(formBuilder!=null||multipartBuilder!=null)){
                throw new IllegalOperationException("You cannot not use multipart/from and raw RequestBody on the same call");
            }
            if(formBuilder!=null){
                this.body = formBuilder.build(lite.getClient());
            }else if(multipartBuilder!=null){
                this.body = multipartBuilder.build(lite.getClient());
            }
        }

        private void checkUrl(String baseUrl,String url){
            if(url==null){
                throw new NullPointerException("Url is null for this Request");
            }
            if(!Util.isHttpPrefix(url)&&TextUtils.isEmpty(baseUrl)&&TextUtils.isEmpty(lite.getBaseUrl())){
                throw new IllegalArgumentException(String.format(Locale.getDefault(),"url:%s is not http prefix and baseUrl is empty",url));
            }
        }

        private String buildUrlAndParams(String baseUrl,String url) {
            checkUrl(baseUrl,url);
            if(!Util.isHttpPrefix(url)){
                if(!TextUtils.isEmpty(lite.getBaseUrl())){
                    if(!TextUtils.isEmpty(baseUrl)){
                        if(Util.isHttpPrefix(baseUrl)){
                            url = Util.appendString(baseUrl,url);
                        }else{
                            baseUrl = Util.appendString(lite.getBaseUrl(),baseUrl);
                            url = Util.appendString(baseUrl,url);
                        }
                    }else{
                        url = Util.appendString(lite.getBaseUrl(), url);
                    }
                }else{
                    if(!TextUtils.isEmpty(baseUrl)){
                        url = Util.appendString(baseUrl,url);
                    }
                }
            }
            url = processPathHolders(url, pathHolders);
            StringBuilder sb = new StringBuilder(url);
            if (params != null && !params.isEmpty()){
                int index = url.indexOf("?");
                if(index == -1){
                    sb.append("?");
                }else if(index<url.length()-1){
                    if(!url.endsWith("&")){
                        sb.append("&");
                    }
                }
                Collections.sort(params,paramComparable);
                boolean first = true;
                String value;
                for (Pair<String,Pair<String,Boolean>> pair : params){
                    Pair<String,Boolean> pairValue = pair.second;
                    String key = pairValue.second?pair.first:Uri.encode(pair.first,Util.UTF_8.name());
                    value = pairValue.second?pairValue.first:Uri.encode(pairValue.first,Util.UTF_8.name());
                    if(first){
                        sb.append(key).append("=").append(value);
                        first = false;
                    }else{
                        sb.append("&").append(key).append("=").append(value);
                    }
                }
            }
            return sb.toString();
        }

        private String processPathHolders(String url, Map<String, Pair<String,Boolean>> pathHolders) {
            if(pathHolders!=null){
                String value;
                for (String key : pathHolders.keySet()){
                    Pair<String,Boolean> pair = pathHolders.get(key);
                    value = pair.second?pair.first:Uri.encode(pair.first,Util.UTF_8.name());
                    url = url.replace("{" + key + "}",value);
                }
            }
            return url;
        }

        private HashMap<String,Pair<String,Boolean>> getPathHolders(){
            if(pathHolders==null){
                pathHolders = new HashMap<>();
            }
            return pathHolders;
        }

        private void checkContentType() {
            if(headers==null||body==null) return;
            String contentType = null;
            for(String key:headers.keySet()){
                if("Content-Type".equalsIgnoreCase(key)){
                    contentType = key;
                    break;
                }
            }
            if(contentType!=null){
                headers.remove(contentType);
                final MediaType type = lite.parse(contentType);
                final RequestBody tmp = body;
                body = new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return type;
                    }

                    @Override
                    public long contentLength() throws IOException {
                        return tmp.contentLength();
                    }

                    @Override
                    public void writeTo(OutputStream sink) throws IOException {
                        tmp.writeTo(sink);
                    }
                };
            }
        }

        public Builder mark(Object mark){
            this.mark = mark;
            return this;
        }



        Builder setUrl(String url){
            checkUrl(baseUrl,url);
            this.url = url;
            return this;
        }

        Builder setBaseUrl(String baseUrl){
            if(TextUtils.isEmpty(lite.getBaseUrl())&&!Util.isHttpPrefix(baseUrl)){
                throw new IllegalArgumentException("Global BaseUrl is empty, you must set a baseUrl(@BaseURL) with http/https prefix for this request");
            }
            this.baseUrl = baseUrl;
            return this;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            Builder builder = (Builder) super.clone();
            builder.lite = this.lite;
            return builder;
        }

        public Request build(){
            return new Request(this);
        }
    }

    public enum Method {
        GET(false,false),POST(true,true),PUT(true,true),DELETE(true,false),HEAD(false,false),PATCH(true,true);

        public final boolean permitsRequestBody;
        public final boolean requiresRequestBody;

        Method(boolean permitsRequestBody, boolean requiresRequestBody) {
            this.permitsRequestBody = permitsRequestBody;
            this.requiresRequestBody = requiresRequestBody;
        }

        public static boolean permitsRequestBody(Method method) {
            return requiresRequestBody(method)
                    || method.name().equals("OPTIONS")
                    || method.name().equals("DELETE")    // Permitted as spec is ambiguous.
                    || method.name().equals("PROPFIND")  // (WebDAV) without body: call <allprop/>
                    || method.name().equals("MKCOL")     // (WebDAV) may contain a body, but behaviour is unspecified
                    || method.name().equals("LOCK");     // (WebDAV) body: create lock, without body: refresh lock
        }

        public static boolean requiresRequestBody(Method method) {
            return method.name().equals("POST")
                    || method.name().equals("PUT")
                    || method.name().equals("PATCH")
                    || method.name().equals("PROPPATCH") // WebDAV
                    || method.name().equals("REPORT");   // CalDAV/CardDAV (defined in WebDAV Versioning)
        }
    }
}

