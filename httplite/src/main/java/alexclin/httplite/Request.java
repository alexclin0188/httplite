package alexclin.httplite;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
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
import alexclin.httplite.util.Method;
import alexclin.httplite.util.Util;

/**
 * Request
 *
 * @author alexclin 16/1/31 10:21
 */
public final class Request {
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
    Method method;
    private Map<String,List<String>> headers;
    private List<Pair<String,Pair<String,Boolean>>> params;
    private RequestBody body;
    private ProgressRequestBody progressBody;

    private Object tag;

    ProgressListener progressListener;
    RetryListener retryListener;

    private MainProgressListener progressWrapper;

    HttpLite lite;

    private int cacheExpiredTime = UNSPECIFIED_CACHE;

    private FormBuilder formBuilder;
    private MultipartBuilder multipartBuilder;

    private HashMap<String,Pair<String,Boolean>> pathHolders;

    private DownloadHandler.DownloadParams downloadParams;

    private Object mark;

    Request(HttpLite lite,String url) {
        this.lite = lite;
        this.url = url;
    }

    public Request(HttpLite lite) {
        this.lite = lite;
    }

    public Map<String,List<String>> getHeaders(){
        if(headers==null){
            headers = new HashMap<>();
        }
        return headers;
    }

    public Request header(String name, String value) {
        ArrayList<String> list = new ArrayList<>();
        list.add(value);
        getHeaders().put(name, list);
        return this;
    }

    public Request addHeader(String name, String value) {
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
        return method(Method.GET, null);
    }

    public Call head() {
        return method(Method.HEAD, null);
    }

    public Call post(RequestBody body) {
        return method(Method.POST, body);
    }

    public Call post(){
        return method(Method.POST, null);
    }

    public Call post(String mediaType,String content){
        return method(Method.POST, lite.createRequestBody(lite.parse(mediaType), content));
    }

    public Call post(String mediaType,File file){
        return method(Method.POST, lite.createRequestBody(lite.parse(mediaType), file));
    }

    public Call delete(RequestBody body) {
        return method(Method.DELETE, body);
    }

    public Call delete() {
        return delete(lite.createRequestBody(null, new byte[0]));
    }

    public Call put(RequestBody body) {
        return method(Method.PUT, body);
    }

    public Call patch(RequestBody body) {
        return method(Method.PATCH, body);
    }

    public Call method(Method method, RequestBody body) {
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
        if(!TextUtils.isEmpty(baseUrl)&&Util.isHttpPrefix(baseUrl)&&!Util.isHttpPrefix(url)){
            url = Util.appendString(baseUrl, url);
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

    public String getUrl() {
        return buildUrlAndParams(TextUtils.isEmpty(baseUrl)?lite.getBaseUrl():baseUrl,url);
    }

    public String rawUrl() {
        return url;
    }

    public Method getMethod() {
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

    public Handle download(Callback<File> callback){
        return get().async(callback);
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

    public boolean canCache(){
        return method==Method.GET && cacheExpiredTime!=NO_CACHE;
    }

    public Request mark(Object mark){
        this.mark = mark;
        return this;
    }

    public Object getMark(){
        return mark;
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
}

