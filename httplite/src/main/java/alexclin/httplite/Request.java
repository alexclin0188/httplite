package alexclin.httplite;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import alexclin.httplite.exception.IllegalOperationException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.util.Clazz;
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

    private static final Comparator<Pair<String,Pair<String,Boolean>>> paramComparable = new Comparator<Pair<String, Pair<String, Boolean>>>() {

        @Override
        public int compare(Pair<String, Pair<String, Boolean>> lhs, Pair<String, Pair<String, Boolean>> rhs) {
            if(lhs.first.equals(rhs.first)){
                return lhs.second.first.compareTo(rhs.second.first);
            }
            return lhs.first.compareTo(rhs.first);
        }
    };

    private final String mUrl;
    private String fullUrl;
    private String baseUrl;
    private final Map<String,Pair<String,Boolean>> pathHolders;
    private final Method method;
    private final ProgressListener progressListener;
    private final ProgressListener wrapListener;
    private final Map<String,List<String>> headers;
    private final List<Pair<String,Pair<String,Boolean>>> params;
    private final RequestBody requestBody;
    private final Object tag;
    private final int cacheExpiredTime;
    private final DownloadParams downloadParams;
    private final Handle handle;

    private Request(Builder builder) {
        this.mUrl = Util.isHttpPrefix(builder.url)?builder.url:Util.appendString(builder.baseUrl,builder.url);
        this.method = builder.method;
        this.progressListener = builder.progressListener;
        this.headers = builder.headers;
        this.params = builder.params;
        this.tag = builder.tag;
        this.cacheExpiredTime = builder.cacheExpiredTime;
        this.downloadParams = builder.downloadParams;
        this.pathHolders = builder.pathHolders==null?null:Collections.unmodifiableMap(builder.pathHolders);
        this.handle = new HandleImpl();
        this.requestBody = builder.body;
        if(this.progressListener!=null){
            this.wrapListener = new MainProgressListener(this.progressListener);
        }else{
            this.wrapListener = null;
        }
    }

    private String buildUrlAndParams(String baseUrl,String url,Map<String,Pair<String,Boolean>> pathHolders) {
        if(!Util.isHttpPrefix(url)&&TextUtils.isEmpty(baseUrl)){
            throw new IllegalArgumentException(String.format(Locale.getDefault(),"url:%s is not http prefix and setBaseUrl is empty",url));
        }else if(!Util.isHttpPrefix(url)&&!TextUtils.isEmpty(baseUrl)){
            url = Util.appendString(baseUrl,url);
        }
        if(url==null){
            throw new IllegalStateException("url is null for this request, set a url first");
        }
        if(TextUtils.isEmpty(url)){
            throw new IllegalStateException("Url is empty for this Request");
        }
        if(pathHolders!=null){
            String value;
            for (String key : pathHolders.keySet()){
                Pair<String,Boolean> pair = pathHolders.get(key);
                value = pair.second?pair.first:Uri.encode(pair.first,Util.UTF_8.name());
                url = url.replace("{" + key + "}",value);
            }
        }
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

    public String getUrl() {
        if(baseUrl==null){
            return mUrl;
        }else {
            if(fullUrl==null){
                fullUrl = buildUrlAndParams(baseUrl,mUrl,pathHolders);
            }
            return fullUrl;
        }
    }

    void setBaseUrl(String baseUrl){
        this.baseUrl = baseUrl;
    }

    public ProgressListener getWrapListener() {
        return wrapListener;
    }

    public Method getMethod() {
        return method;
    }

    public List<Pair<String,Pair<String,Boolean>>> getParams() {
        return params==null?null:Collections.unmodifiableList(params);
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public Object getTag() {
        return tag;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    public int getCacheExpiredTime() {
        return cacheExpiredTime;
    }

    public DownloadParams getDownloadParams() {
        return downloadParams;
    }

    public Map<String, List<String>> getHeaders() {
        return headers==null?null:Collections.unmodifiableMap(headers);
    }

    public <T> Result<T> execute(HttpLite lite, Type type){
        return lite.execute(this,type);
    }

    public <T> Result<T> execute(HttpLite lite, Clazz<T> clazz){
        return lite.execute(this,clazz);
    }

    public <T> void enqueue(HttpLite lite, Callback<T> callback){
        lite.enqueue(this,callback);
    }

    public Handle handle(){
        return handle;
    }

    private static class MainProgressListener implements ProgressListener{
        private ProgressListener listener;

        MainProgressListener(ProgressListener listener) {
            this.listener = listener;
        }

        @Override
        public void onProgress(final boolean out, final long current, final long total) {
            HttpLite.runOnMain(new Runnable() {
                @Override
                public void run() {
                    listener.onProgress(out,current,total);
                }
            });
        }
    }

    @Override
    public String toString() {
        boolean first;
        StringBuilder builder = new StringBuilder("Request{url='").append(mUrl).append('\'');
        if(!Util.isHttpPrefix(mUrl)){
            builder.append(", baseUrl='").append(baseUrl).append('\'');
        }
        builder.append(", method=").append(method);
        if(progressListener!=null){
            builder.append(", progress=").append(progressListener);
        }
        if(pathHolders!=null){
            builder.append(", paths=[");
            first = true;
            for(Map.Entry<String,Pair<String,Boolean>> entry:pathHolders.entrySet()){
                if(first){
                    first = false;
                }else{
                    builder.append(",");
                }
                builder.append(entry.getKey()).append(":").append(entry.getValue().first).append("-")
                        .append(entry.getValue().second);
            }
            builder.append("]");
        }
        if(headers!=null){
            builder.append(", headers=[");
            first = true;
            for(Map.Entry<String,List<String>> entry:headers.entrySet()){
                if(first){
                    first = false;
                }else{
                    builder.append(",");
                }
                builder.append(entry.getKey()).append(":");
                if(entry.getValue().size()>1){
                    for(String v:entry.getValue()){
                        builder.append(v).append(" ");
                    }
                }else{
                    builder.append(entry.getValue().get(0));
                }
            }
        }
        if(params!=null){
            builder.append(", , params=[");
            first = true;
            for(Pair<String,Pair<String,Boolean>> entry:params){
                if(first){
                    first = false;
                }else{
                    builder.append(",");
                }
                builder.append(entry.first).append(":").append(entry.second.first).append("-").append(entry.second.second);
            }
            builder.append("]");
        }
        if(requestBody!=null){
            builder.append(", body=").append(requestBody);
        }
        if(tag!=null){
            builder.append(", tag=").append(tag);
        }
        if(downloadParams!=null){
            builder.append(", download=").append(downloadParams);
        }
        builder.append(", cache=").append(cacheExpiredTime).append('}');
        return builder.toString();
    }

    public static final class Builder implements Cloneable{
        private String url;
        private String baseUrl;
        private Method method;
        private ProgressListener progressListener;
        private Map<String,List<String>> headers;
        private List<Pair<String,Pair<String,Boolean>>> params;
        private RequestBody body;
        private Object tag;
        private int cacheExpiredTime = UNSPECIFIED_CACHE;

        private RequestBody.FormBody formBody;
        private RequestBody.MultipartBody multipartBody;

        private HashMap<String,Pair<String,Boolean>> pathHolders;

        private DownloadParams downloadParams;

        public Builder(String url) {
            this.url = url;
        }

        public Builder(){}

        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        public Builder baseUrl(String baseUrl){
            this.baseUrl = baseUrl;
            return this;
        }

        public void url(String url) {
            this.url = url;
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
                formBody.addEncoded(name, value);
            else
                formBody.add(name, value);
            return this;
        }

        public Builder multipart(String name,String value){
            initMultiPartBuilder();
            multipartBody.add(name, value);
            return this;
        }

        public Builder multipartType(String mediaType){
            initMultiPartBuilder();
            multipartBody.setType(mediaType);
            return this;
        }

        public Builder multipart(RequestBody body){
            initMultiPartBuilder();
            multipartBody.add(body);
            return this;
        }

        public Builder multipart(Map<String,List<String>> headers,RequestBody body){
            initMultiPartBuilder();
            multipartBody.add(headers,body);
            return this;
        }

        public Builder multipart(String name, String fileName, RequestBody body){
            initMultiPartBuilder();
            multipartBody.add(name,fileName,body);
            return this;
        }

        public Builder multipart(String name, String fileName, File file){
//            RequestBody requestBody = lite.createRequestBody(Util.guessMediaType(lite, file), file);
            RequestBody body = RequestBody.createBody(file,null);
            return multipart(name, fileName, body);
        }

        public Builder multipart(String name, String fileName, File file,String mediaType){
//            RequestBody requestBody = lite.createRequestBody(lite.parse(mediaType), file);
            RequestBody body = RequestBody.createBody(file,mediaType);
            return multipart(name, fileName, body);
        }

        public Builder body(String mediaType,String content){
            if(content==null) return this;
//            this.requestBody = lite.createRequestBody(lite.parse(mediaType), content);
            this.body = RequestBody.createBody(content,mediaType);
            return this;
        }

        public Builder body(String mediaType,File file){
//            if(TextUtils.isEmpty(mediaType)){
//                this.requestBody = lite.createRequestBody(Util.guessMediaType(lite,file), file);
//            }else{
//                this.requestBody = lite.createRequestBody(lite.parse(mediaType), file);
//            }
            this.body = RequestBody.createBody(file,mediaType);
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

        private DownloadParams checkAndCreateDownload(String path,String fileName,boolean autoResume,boolean autoRename){
            DownloadParams params = DownloadParams.createParams(path, fileName, autoResume, autoRename,url);
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
//            return method(Method.POST, lite.createRequestBody(lite.parse(mediaType), content));
            return method(Method.POST, RequestBody.createBody(content,mediaType));
        }

        public Builder post(String mediaType,File file){
//            return method(Method.POST, lite.createRequestBody(lite.parse(mediaType), file));
            return method(Method.POST, RequestBody.createBody(file,mediaType));
        }

        public Builder delete(RequestBody body) {
            return method(Method.DELETE, body);
        }

        public Builder delete() {
//            return delete(lite.createRequestBody(null, new byte[0]));
            return delete(RequestBody.createBody(new byte[0],null));
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
                throw new IllegalArgumentException("method " + method + " must not have a call requestBody.");
            }
            if (isBodyNull && method.requiresRequestBody) {
                throw new IllegalArgumentException("method " + method + " must have a call requestBody.");
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

        private void initFormBuilder(){
            if(multipartBody !=null){
                throw new IllegalOperationException("You cannot call form-Method after you have called multipart method on call");
            }
            if(body!=null){
                throw new IllegalOperationException("You cannot call form-Method after you have set RequestBody on call");
            }
            if(formBody == null){
                formBody = new RequestBody.FormBody();
            }
        }

        private void initMultiPartBuilder(){
            if(formBody !=null){
                throw new IllegalOperationException("You cannot call multipart-method after you have called form-method on call");
            }
            if(body!=null){
                throw new IllegalOperationException("You cannot call multipart-method after you have set RequestBody on call");
            }
            if(multipartBody ==null){
                multipartBody = new RequestBody.MultipartBody();
            }
        }

        private void preWorkForTask(RequestBody body) {
            if((this.body!=null||body!=null)&&(formBody !=null|| multipartBody !=null)){
                throw new IllegalOperationException("You cannot not use multipart/from and raw RequestBody on the same call");
            }
            if(formBody !=null){
                this.body = formBody;
            }else if(multipartBody !=null){
                this.body = multipartBody;
            }
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
                if(body instanceof RequestBody.NotBody){
                    ((RequestBody.NotBody) body).setMediaType(contentType);
                }else{
                    body = RequestBody.wrapBody(body,contentType);
                }
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            Builder builder = (Builder) super.clone();
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
                    || method.name().equals("PROPFIND")  // (WebDAV) without requestBody: call <allprop/>
                    || method.name().equals("MKCOL")     // (WebDAV) may contain a requestBody, but behaviour is unspecified
                    || method.name().equals("LOCK");     // (WebDAV) requestBody: create lock, without requestBody: refresh lock
        }

        public static boolean requiresRequestBody(Method method) {
            return method.name().equals("POST")
                    || method.name().equals("PUT")
                    || method.name().equals("PATCH")
                    || method.name().equals("PROPPATCH") // WebDAV
                    || method.name().equals("REPORT");   // CalDAV/CardDAV (defined in WebDAV Versioning)
        }
    }

    public static class DownloadParams{
        public File parentDir;
        public File targetFile;
        public boolean autoResume;
        public boolean autoRename;

        DownloadParams(File parentDir, File targetFile, boolean autoResume, boolean autoRename) {
            this.parentDir = parentDir;
            this.targetFile = targetFile;
            this.autoResume = autoResume;
            this.autoRename = autoRename;
        }

        public File getParentDir() {
            return parentDir;
        }

        public File getTargetFile() {
            return targetFile;
        }

        public boolean isAutoResume() {
            return autoResume;
        }

        public boolean isAutoRename() {
            return autoRename;
        }

        private static DownloadParams createParams(String path, String fileName, boolean autoResume, boolean autoRename,String url) {
            if(TextUtils.isEmpty(path)){
                return null;
            }
            File parentDir = new File(path);
            if(TextUtils.isEmpty(fileName)){
                if(path.endsWith("/")){
                    autoRename = true;
                }else{
                    if(parentDir.exists()&&parentDir.isDirectory()){
                        autoRename = true;
                    }else{
                        int index = path.lastIndexOf("/");
                        if(index!=-1){
                            fileName = parentDir.getName();
                            parentDir = parentDir.getParentFile();
                        }else
                            return null;
                    }
                }
            }
            if(TextUtils.isEmpty(fileName)){
                fileName = createDefaultName(url);
            }
            File targetFile = new File(parentDir,fileName);
            if(!parentDir.exists()){
                if(!parentDir.mkdirs()){
                    return null;
                }
            }
            if(!parentDir.canWrite()){
                return null;
            }
            return new DownloadParams(parentDir,targetFile,autoResume,autoRename);
        }

        private static String createDefaultName(String url) {
            int index = url.lastIndexOf("/");
            if(index>-1&&index<url.length()-1){
                return url.substring(index+1);
            }
            return String.format(Locale.getDefault(),"download-%d.tmp", System.currentTimeMillis());
        }

        @Override
        public String toString() {
            return "{parentDir=" + parentDir +
                    ", targetFile=" + targetFile +
                    ", autoResume=" + autoResume +
                    ", autoRename=" + autoRename +
                    '}';
        }
    }
}

