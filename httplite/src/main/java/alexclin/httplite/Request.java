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
import java.util.Map;

import alexclin.httplite.exception.IllegalOperationException;
import alexclin.httplite.impl.ProgressRequestBody;
import alexclin.httplite.impl.ProgressResponse;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.HttpMethod;
import alexclin.httplite.util.Util;

/**
 * Request
 *
 * @author alexclin 16/1/31 10:21
 */
public final class Request implements Cloneable{
    public static final int NO_CACHE = 0;
    public static final int FORCE_CACHE = -1;
    public static final int UNSPECIFIED_CACHE = -100;

    private static final Comparator<Pair<String,Pair<String,Boolean>>> paramComparable = new Comparator<Pair<String, Pair<String, Boolean>>>() {

        @Override
        public int compare(Pair<String, Pair<String, Boolean>> lhs, Pair<String, Pair<String, Boolean>> rhs) {
            if(lhs.first.equals(rhs.first)){
                return lhs.second.first.compareTo(rhs.second.first);
            }
            return lhs.first.compareTo(rhs.first);
        }
    };

    String baseUrl;
    String url;
    HttpMethod method;
    ProgressListener progressListener;
    RetryListener retryListener;
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

    Request(HttpLite lite, String url) {
        this.lite = lite;
        this.url = url;
    }

    public Request(HttpLite lite) {
        this.lite = lite;
    }

    public static boolean permitsRequestBody(HttpMethod method) {
        return requiresRequestBody(method)
                || method.name().equals("OPTIONS")
                || method.name().equals("DELETE")    // Permitted as spec is ambiguous.
                || method.name().equals("PROPFIND")  // (WebDAV) without body: call <allprop/>
                || method.name().equals("MKCOL")     // (WebDAV) may contain a body, but behaviour is unspecified
                || method.name().equals("LOCK");     // (WebDAV) body: create lock, without body: refresh lock
    }

    public static boolean requiresRequestBody(HttpMethod method) {
        return method.name().equals("POST")
                || method.name().equals("PUT")
                || method.name().equals("PATCH")
                || method.name().equals("PROPPATCH") // WebDAV
                || method.name().equals("REPORT");   // CalDAV/CardDAV (defined in WebDAV Versioning)
    }

    public Map<String,List<String>> getHeaders(){
        if(headers==null){
            headers = new HashMap<>();
        }
        return headers;
    }

    public Request header(String name, String value) {
        List<String> values = getHeaders().get(name);
        if(values==null){
            values = new ArrayList<>();
            values.add(value);
        }else{
            values.add(value);
        }
        getHeaders().put(name, values);
        return this;
    }

    public Request removeHeader(String name) {
        List<String> values = getHeaders().get(name);
        if(values!=null){
            values.remove(name);
        }
        return this;
    }

    public Request headers(Map<String,List<String>> headers) {
        this.headers = headers;
        return this;
    }

    public Request pathHolder(String key,String value){
        return pathHolder(key,value,false);
    }

    public Request pathHolder(String key,String value, boolean encoded){
        if(key!=null&&value!=null) getPathHolders().put(key, new Pair<>(value, encoded));
        return this;
    }

    public Request pathHolders(HashMap<String,String> pathHolders,boolean encoded){
        if(pathHolders!=null){
            for(String key:pathHolders.keySet()){
                getPathHolders().put(key,new Pair<String, Boolean>(pathHolders.get(key),encoded));
            }
        }
        return this;
    }

    public Request param(String name,String value){
        return param(name,value,false);
    }

    public Request param(String name,Object value){
        return param(name,value==null?null:value.toString(),false);
    }

    public Request param(String name,String value,boolean encoded){
        if(value==null) return this;
        if(params==null){
            params = new ArrayList<>();
        }
        params.add(new Pair<>(name, new Pair<>(value, encoded)));
        return this;
    }

    public Request form(String name,String value){
        return form(name,value,false);
    }

    public Request formEncoded(String name,String value){
        return form(name,value,true);
    }

    public Request form(String name,String value,boolean encoded){
        initFormBuilder();
        if(encoded)
            formBuilder.addEncoded(name, value);
        else
            formBuilder.add(name, value);
        return this;
    }

    public Request multipart(String name,String value){
        initMultiPartBuilder();
        multipartBuilder.add(name, value);
        return this;
    }

    public Request multipartType(MediaType type){
        initMultiPartBuilder();
        multipartBuilder.setType(type);
        return this;
    }

    public Request multipart(RequestBody body){
        initMultiPartBuilder();
        multipartBuilder.add(body);
        return this;
    }

    public Request multipart(Map<String,List<String>> headers,RequestBody body){
        initMultiPartBuilder();
        multipartBuilder.add(headers,body);
        return this;
    }

    public Request multipart(String name, String fileName, RequestBody body){
        initMultiPartBuilder();
        multipartBuilder.add(name,fileName,body);
        return this;
    }

    public Request multipart(String name, String fileName, File file){
        return multipart(name, fileName, lite.createRequestBody(Util.guessMediaType(lite, file), file));
    }

    public Request multipart(String name, String fileName, File file,String mediaType){
        return multipart(name, fileName, lite.createRequestBody(lite.parse(mediaType), file));
    }

    public Request body(String mediaType,String content){
        if(content==null) return this;
        this.body = lite.createRequestBody(lite.parse(mediaType), content);
        return this;
    }

    public Request body(String mediaType,File file){
        if(TextUtils.isEmpty(mediaType)){
            this.body = lite.createRequestBody(Util.guessMediaType(lite,file), file);
        }else{
            this.body = lite.createRequestBody(lite.parse(mediaType), file);
        }
        return this;
    }

    public Request body(RequestBody body){
        this.body = body;
        return this;
    }

    public Request cacheExpire(int expire){
        this.cacheExpiredTime = expire;
        return this;
    }

    public Call get() {
        return method(HttpMethod.GET, null);
    }

    public Call head() {
        return method(HttpMethod.HEAD, null);
    }

    public Call post(RequestBody body) {
        return method(HttpMethod.POST, body);
    }

    public Call post(){
        return method(HttpMethod.POST, null);
    }

    public Call post(String mediaType,String content){
        return method(HttpMethod.POST, lite.createRequestBody(lite.parse(mediaType), content));
    }

    public Call post(String mediaType,File file){
        return method(HttpMethod.POST, lite.createRequestBody(lite.parse(mediaType), file));
    }

    public Call delete(RequestBody body) {
        return method(HttpMethod.DELETE, body);
    }

    public Call delete() {
        return delete(lite.createRequestBody(null, new byte[0]));
    }

    public Call put(RequestBody body) {
        return method(HttpMethod.PUT, body);
    }

    public Call patch(RequestBody body) {
        return method(HttpMethod.PATCH, body);
    }

    public Call method(HttpMethod method, RequestBody body) {
        preWorkForTask(body);
        boolean isBodyNull = body==null&&this.body==null;
        if (method == null) {
            throw new IllegalArgumentException("method == null");
        }
        if (!isBodyNull && !permitsRequestBody(method)) {
            throw new IllegalArgumentException("method " + method + " must not have a call body.");
        }
        if (isBodyNull && requiresRequestBody(method)) {
            throw new IllegalArgumentException("method " + method + " must have a call body.");
        }
        this.method = method;
        if(body!=null){
            this.body = body;
        }
        checkContentType();
        return lite.makeCall(this);
    }

    public Request tag(Object tag) {
        this.tag = tag;
        return this;
    }

    public Request onProgress(ProgressListener listener){
        this.progressListener = listener;
        return this;
    }

    public Request onRetry(RetryListener listener){
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

    private String buildUrlAndParams(String baseUrl,String url) {
        if(url==null){
            throw new NullPointerException("Url is null for this Request");
        }
        if(!Util.isHttpPrefix(url)&&!TextUtils.isEmpty(lite.getBaseUrl())){
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

    public String getUrl() {
        return buildUrlAndParams(baseUrl,url);
    }

    public String rawUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public List<Pair<String,Pair<String,Boolean>>> getParams() {
        return params;
    }

    public RequestBody getBody() {
        if(progressListener!=null&&body!=null){
            if(progressBody==null||!progressBody.isWrappBody(body)){
                progressBody = new ProgressRequestBody(body,getMainProgressListener());
            }
            return progressBody;
        }
        return body;
    }

    Response handleResponse(Response response){
        if(progressListener!=null&&getDownloadParams()==null){
            return new ProgressResponse(response,getMainProgressListener());
        }
        return response;
    }

    private ProgressListener getMainProgressListener(){
        if(progressWrapper ==null){
            progressWrapper = new MainProgressListener(progressListener);
        }
        return progressWrapper;
    }

    public Object getTag() {
        return tag;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    public RetryListener getRetryListener() {
        return retryListener;
    }

    public int getCacheExpiredTime() {
        return cacheExpiredTime;
    }

    public Request intoFile(String path,boolean autoResume){
        this.downloadParams = checkAndCreateDownload(path,null,autoResume,true);
        return this;
    }

    public Request intoFile(String path, boolean autoResume,boolean autoRename){
        this.downloadParams = checkAndCreateDownload(path, null, autoResume, autoRename);
        return this;
    }

    public Request intoFile(String path,String fileName,boolean autoResume,boolean autoRename){
        this.downloadParams = checkAndCreateDownload(path,null,autoResume,autoRename);
        return this;
    }

    public void download(Callback<File> callback){
        get().async(callback);
    }

    private DownloadHandler.DownloadParams checkAndCreateDownload(String path,String fileName,boolean autoResume,boolean autoRename){
        DownloadHandler.DownloadParams params = DownloadHandler.createParams(path, fileName, autoResume, autoRename);
        if(params==null){
            String info = String.format("call intoFile() with wrong params->path:%s,fileName:%s,resume:%b,rename:%b",path,fileName,autoResume,autoRename);
            throw new IllegalArgumentException(info);
        }
        return params;
    }

    public DownloadHandler.DownloadParams getDownloadParams() {
        return downloadParams;
    }

    public Request mark(Object mark){
        this.mark = mark;
        return this;
    }

    public Object getMark(){
        return mark;
    }

    @Override
    public Request clone() throws CloneNotSupportedException {
        Request request = (Request) super.clone();
        if(this.headers!=null){
            request.headers = new HashMap<>();
            for(Map.Entry<String,List<String>> entry:this.headers.entrySet()){
                request.headers.put(entry.getKey(),new ArrayList<>(entry.getValue()));
            }
        }
        request.params = this.params!=null?new ArrayList<>(this.params):null;
        request.pathHolders = this.pathHolders!=null?new HashMap<>(this.pathHolders):null;
//        request.lite = this.lite;
//        request.url = this.url;
//        request.baseUrl = this.baseUrl;
//        request.method = this.method;
//        request.progressListener = this.progressListener;
//        request.retryListener = this.retryListener;
//        request.body = this.body;
//        request.progressBody = this.progressBody;
//        request.tag = this.tag;
//        request.progressWrapper = this.progressWrapper;
//        request.cacheExpiredTime = this.cacheExpiredTime;
//        request.formBuilder = this.formBuilder;
//        request.multipartBuilder = this.multipartBuilder;
//        request.downloadParams = this.downloadParams;
//        request.mark = this.mark;
        return request;
    }

    public static class MainProgressListener implements ProgressListener{
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

    @Override
    public String toString() {
        return "Request{" +
                "baseUrl='" + baseUrl + '\'' +
                ", url='" + url + '\'' +
                ", method=" + method +
                ", progressListener=" + progressListener +
                ", retryListener=" + retryListener +
                ", headers=" + headers +
                ", params=" + params +
                ", body=" + body +
                ", tag=" + tag +
                ", cacheExpiredTime=" + cacheExpiredTime +
                ", formBuilder=" + formBuilder +
                ", multipartBuilder=" + multipartBuilder +
                ", pathHolders=" + pathHolders +
                ", downloadParams=" + downloadParams +
                ", mark=" + mark +
                '}';
    }
}

