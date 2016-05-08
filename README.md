# httplite
A android http library

## 说明

类库开发目的主要是以下三点

* 1.使用接口的方式定义API接口, 思路来源于[Retrofit](https://github.com/square/retrofit)
* 2.实现Http结果的自动解析（使用ResponseParser接口）
* 3.隔离对Http具体实现类库的依赖，方便替换（目前类库中有OkLite,URLConnectionLite两种实现，对应[okhttp](https://github.com/square/okhttp)和系统URLConnection实现）

## 使用

* Gradle

使用okhttp 2.7.5作为http实现

```
    compile 'alexclin.httplite:httplite-okhttp2:1.1.1'
    compile 'com.squareup.okhttp:okhttp:2.7.5'
```

使用okhttp 3.2.0作为http实现
```
    compile 'alexclin.httplite:httplite-okhttp3:1.1.1'
    compile 'com.squareup.okhttp3:okhttp:3.2.0'
```

使用系统URLConnection作为http实现
```
    compile 'alexclin.httplite:httplite-url:1.1.1'
```

如需Rx扩展则还需要
```
    compile 'alexclin.httplite:retrofit-rx:1.1.1'
    compile 'io.reactivex:rxjava:1.1.1'
```

或者直接使用releaselib中的jar包

* 1 okhttp2: httplite1.1.1.jar+httplite-ok2lite1.1.1.jar+okhttp 2.x.x版本jar包
* 2 okhttp3: httplite1.1.1.jar+httplite-ok3lite1.1.1.jar+okhttp 3.x.x版本jar包
* 3 url: httplite1.1.1.jar+httplite-urlite1.1.1.jar

* 4 使用rx扩展：httplite-retrofit-rx1.1.1.jar+rx1.x.x版本jar包

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
    Observable<alexclin.httplite.util.Result<ZhihuData>> testZhihuResult();
}
```

### 初始化Httplite并创建API接口的实例

替换依赖只用替换如下部分即可

```java
HttpLiteBuilder builder = Ok2Lite.create(); //okhttp2作为http实现类库，推荐
//或者
HttpLiteBuilder builder = Ok3Lite.create(); //okhttp3作为http实现类库，推荐
//或者
HttpLiteBuilder builder = URLite.create(); //使用URLConnection实现的http
```

配置并创建HttpLite

```java
builder = builder.setConnectTimeout(10, TimeUnit.SECONDS)  //设置连接超时
                .setWriteTimeout(10, TimeUnit.SECONDS)  //设置写超时
                .setReadTimeout(10, TimeUnit.SECONDS)  //设置读超时
                .setMaxRetryCount(2)  //设置失败重试次数
                .setFollowRedirects(true)  //设置是否sFollowRedirects,默认false
                .setFollowSslRedirects(true) //设置是否setFollowSslRedirects
                .addResponseParser(new GsonParser())
                .baseUrl("http://192.168.99.238:10080/")//BaseUrl
                .setProxy(...)//
                .setProxySelector(...)//
                .setSocketFactory(...)//
                .setSslSocketFactory(...)//
                .setHostnameVerifier(..)//
                .useCookie(...)  //设置CookieStore,设置则启用Cookie,不设置则不启用
                .addCallAdapter(new RxCallAdapter());//添加Rx支持
  //正常使用状态
  Httplite httpLite = builder.build();
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

详细使用指南请移步[《Android网络框架HttpLite使用指南》]()

或者文档[使用指南](./useage.md)



