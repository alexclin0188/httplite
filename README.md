# httplite
A android http library

# 重构记录

## 要做的调整(2016.11)

* 1. Request改为Builder模式

* 2. 移除不必要的Handle封装，网络请求只留一个同步和一个异步方法

* 3. Retrofit实现简化

* 4. Listener简化

* 5. 下载处理简化

## 说明

类库开发目的主要是以下三点

* 1.使用接口的方式定义API接口, 思路来源于[Retrofit](https://github.com/square/retrofit)
* 2.实现Http结果的自动解析（使用ResponseParser接口）
* 3.隔离对Http具体实现类库的依赖，方便替换（目前类库中有OkLite,URLConnectionLite两种实现，对应[okhttp](https://github.com/square/okhttp)和系统URLConnection实现）

## 使用

* Gradle

使用okhttp 2.7.5作为http实现

```
    compile 'alexclin.httplite:httplite-okhttp2:1.1.0'
```

使用okhttp 3.2.0作为http实现
```
    compile 'alexclin.httplite:httplite-okhttp3:1.1.0'
```

使用系统URLConnection作为http实现
```
    compile 'alexclin.httplite:httplite-url:1.1.0'
```

如需Rx扩展则还需要
```
    compile 'alexclin.httplite:httplite-rx:1.0.0'
    compile 'io.reactivex:rxjava:1.1.1'
```

或者直接使用releaselib中的jar包

* 1 okhttp2: httplite1.1.0.jar+httplite-ok2lite1.0.1.jar+okhttp 2.x.x版本jar包
* 2 okhttp3: httplite1.1.0.jar+httplite-ok3lite1.0.1.jar+okhttp 3.x.x版本jar包
* 3 url: httplite1.1.0.jar+httplite-urlite1.1.0.jar

### 使用接口定义API接口

使用接口+注解的方式来定义API接口, 思路来源于[Retrofit](https://github.com/square/retrofit)，具体实现稍有不同

Demo中的API接口定义

```
@BaseURL("http://192.168.99.238:10080/")
public interface ApiService {
    @POST("/login")
    void login(
            @JsonField("username") String userName,
            @JsonField("password")String password,
            @JsonField("token") String token,
            @Tag Object tag,
            Callback<Result<UserInfo>> callback
    );

    @GET("http://www.baidu.com")
    void testBaidu(Callback<String> callback);

    @GET("http://news-at.zhihu.com/api/4/news/latest")
    void testZhihu(Callback<ZhihuData> callback);

    @GET("/download/{test_holder}")
    void downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress ProgressListener progressListener,
            @Retry RetryListener retryListener,
            Callback<File> callback
    );

    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    ZhihuData syncZhihu(Clazz<ZhihuData> clazz) throws Exception;

    @HTTP(method = Method.POST,path = "/dosomething/{some_path}")
    void doSomething(
            @Path("some_path") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @Form("form_f1") String form_f1,
            @Tag Object tag,
            Callback<Result<RequestInfo>> callback
    );

    @HTTP(method = Method.POST,path = "/dosomething/{some_path}")
    Result<RequestInfo> doSomethingSync(
            @Path("some_path") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @Form("form_f1") String form_f1,
            @Tag Object tag,
            Clazz<Result<RequestInfo>> clazz
    ) throws Exception;

    @HTTP(method = Method.PUT,path = "put/{holde_test}")
    void putJsonBody(
            @Path("holde_test") String holder,
            @JsonField("field1") String field1,
            @JsonField("field2") int field2,
            @JsonField("field3") Double field3,
            @JsonField("field4") long field4,
            Callback<ExRequestInfo> callback
    );

    @GET("/download/{test_holder}")
    void downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress @Retry MergeCallback<File> callback
    );

    @GET("http://news-at.zhihu.com/api/4/news/latest")
    Observable<ZhihuData> testZhihu();

    @GET("http://news-at.zhihu.com/api/4/news/latest")
    Observable<alexclin.httplite.Result<ZhihuData>> testZhihuResult();
}
```

### 初始化Httplite并创建API接口的实例

配置并创建HttpLite

```java
        URLite.Builder urlBuilder = new alexclin.httplite.url.URLite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(app));//设置CookieStore;
                
        HttpLiteBuilder builder = new alexclin.httplite.url.URLite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(app))  //设置CookieStore,设置则启用Cookie,不设置则不启用
                .setConnectTimeout(30, TimeUnit.SECONDS)  //设置连接超时
                .setWriteTimeout(30, TimeUnit.SECONDS)  //设置写超时
                .setReadTimeout(30, TimeUnit.SECONDS)  //设置读超时
                .setMaxRetryCount(2)  //设置失败重试次数
                .setFollowRedirects(true)  //设置是否sFollowRedirects,默认false
                .setFollowSslRedirects(true) //设置是否setFollowSslRedirects
                .addResponseParser(new GsonParser())
                .setBaseUrl("https://192.168.99.238:10080/")
//                .setProxy(...)
//                .setProxySelector(...)
                .setMockHandler(handler)
                .setSocketFactory(SocketFactory.getDefault())
                .setSslSocketFactory(manager.getSocketFactory())
                .setHostnameVerifier(manager)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public Request interceptRequest(Request request, Type resultType) {
                        LogUtil.e("Request:" + request);
                        return request;
                    }
                });
        return builder.build();
```

创建API接口实例

```
    ApiService apiService = httpLite.retrofit(ApiService.class);
    //然后就可以使用API接口了，比如
    apiService.testBaidu(new Callback<String>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers,String result) {
                        LogUtil.e("BaiduResult:" + result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("OnFailed", e);
                    }
                });
```

http实现有okhttp2和okhttp3可选
```      
        //okhttp2的网络实现
        Ok2Lite.Builder ok2Builder = new Ok2Lite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(app));
        //okhttp3的网络实现
        Ok3Lite.Builder ok3Builder = new Ok3Lite.Builder()
                .setCookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        return null;
                    }
                });//okhttp3的cookie接口
```

详细使用指南请移步[《Android网络框架HttpLite使用指南》]()

或者文档[使用指南](./useage.md)



