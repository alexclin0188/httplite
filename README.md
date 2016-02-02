# httplite
A android http library

## 说明

类库开发目的主要是以下三点

* 1.使用接口的方式定义API接口, 思路来源于[Retrofit](https://github.com/square/retrofit)
* 2.实现Http结果的自动解析（使用ResponseParser接口）
* 3.隔离对Http具体实现类库的依赖，方便替换（目前类库中有OkLite,URLConnectionLite两种实现，对应[okhttp](https://github.com/square/okhttp)和系统URLConnection实现）

## 使用

### 使用接口定义API接口

使用接口+注解的方式来定义API接口, 思路来源于[Retrofit](https://github.com/square/retrofit)，具体实现稍有不同

Demo中的API接口定义

```
public interface ApiService {
    @POST("/login")
    void login(
            @Param("username") String userName,
            @Param("password")String password,
            @Param("token") String token,
            Callback<Result<UserInfo>> callback
    );

    @GET("http://www.baidu.com")
    void testBaidu(Callback<String> callback);

    @GET("http://news-at.zhihu.com/api/4/news/latest")
    void testZhihu(Callback<ZhihuData> callback);

    @GET("/download/{test_holder}")
    DownloadHandle downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress ProgressListener progressListener,
            @Cancel CancelListener cancelListener,
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
            @JsonField("field2") String field2,
            @JsonField("field3") String field3,
            Callback<ExRequestInfo> callback
    );

    @GET("/download/{test_holder}")
    DownloadHandle downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress @Cancel @Retry MergeCallback<File> callback
    );
}
```

### 初始化Httplite并创建API接口的实例

替换依赖只用替换如下部分即可

```java
HttpLiteBuilder builder = OkLite.create(); //okhttp作为http实现类库，推荐
//或者
HttpLiteBuilder builder = URLConnectionLite.create(); //使用URLConnection实现的http
//另外提供一个本地mock模拟功能
HttpLiteBuilder  builder = MockLite.mock(new MockHandler() {
            @Override
            public <T> void mock(Request request, MockResponse<T> response) throws Exception {
                //TODO 模拟数据
                //response.mock(T result,Map<String, List<String>> headers);//模拟解析结果
                //response.mock(Response response);//模拟原生Response 仅用于Sync执行并返回原生Response的情况
                //response.mockCancel(); //模拟取消
                //response.mockProgress(long current,long total); //模拟进度调用
                //response.mockRetry(long current,long max);  //模拟重试调用
            }
        });
```

配置并创建HttpLite

```java
    httpLite = builder.setConnectTimeout(3, TimeUnit.SECONDS)  //设置连接超时
                .setWriteTimeout(3, TimeUnit.SECONDS)  //设置写超时
                .setReadTimeout(3, TimeUnit.SECONDS)  //设置读超市
                .setMaxRetryCount(2)  //设置失败重试次数
                .setFollowRedirects(true)  //设置是否sFollowRedirects,默认false
                .setFollowSslRedirects(true) //设置是否setFollowSslRedirects
                .setProxy(...)  
                .setProxySelector(...)
                .setSocketFactory(...)
                .setSslSocketFactory(...)
                .setHostnameVerifier(..)
                .baseUrl("http://xxx.xxx.xxx")  //BaseUrl,用于拼接完整的Url
                .useCookie(...)  //设置CookieStore,设置则启用Cookie,不设置则不启用
                .setRelease(true)   //设置是否是Release状态，是Release状态会关闭对接口函数定义的检查，提升效率
                .build();
    httpLite.addResponseParser(new JacksonParser()); //添加ResponseParser实现结果解析
    httpLite.setBaseUrl("http://192.168.99.238:10080/");
```

创建API接口实例

```
    ApiService apiService = httpLite.retrofit(ApiService.class);
    //然后就可以使用API接口了，比如
    apiService.testBaidu(new Callback<String>() {
                    @Override
                    public void onSuccess(String result, Map<String, List<String>> headers) {
                        LogUtil.e("BaiduResult:" + result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("OnFailed", e);
                    }
                });
```

具体测试代码详见代码中RetrofitFrag类

### Retrofit使用需要注意的地方

* 1.使用注解定义的API接口函数在调用时，会忽略掉所有传入的null参数，建议develop时对方法调用和参数做监听，可以通过设置MethodListener来实现

```
    httplite.getRetrofit().setMethodListener(MethodListener methodListener);
```

* 2.Release版本请设置HttpLiteBuilder.setRelease(true)，创建的HttpLite和对应的Retrofit会关闭接口Annotation的定义检查（develop时请打开），减少反射带来的性能消耗

### 自定义注解的使用

ParameterProcessor/ParamMiscProcessor 实现对参数注解的检查和处理

MethodProcessor 实现对方法注解的检查和处理

AnnotationRule 定义对整个方法的注解和参数的检查处理

```
    Retrofit.registerParamterProcessor(ParameterProcessor processor)；
    Retrofit.registerParamMiscProcessor(ParamMiscProcessor processor);
    Retrofit.registerMethodProcessor(MethodProcessor processor);
    Retrofit.registerAnnotationRule(AnnotationRule rule);
```

具体自定义使用请参考BasicAnnotationRule/BasicProcessors/ProcessorFactory源码

### 直接使用http方法

解析String返回结果

```java
httpLite.url("http://www.baidu.com").get().execute(new Callback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        mInfoTv.setText(result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        mInfoTv.setText("Error:"+e);
                    }
                });
```

解析类对象

```java
httpLite.url("http://news-at.zhihu.com/api/4/news/latest").get().execute(new Callback<ZhihuData>() {
                    @Override
                    public void onSuccess(ZhihuData result) {
                        LogUtil.e("Result:"+result);
                        mInfoTv.setText("Rsult:"+result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        e.printStackTrace();
                        mInfoTv.setText("Error:"+e);
                    }
                });
```

### 关于解析结果的说明

* 1.默认支持String的解析，但是类对象结果的解析需要使用httpLite.addResponseParser()提供至少一个解析器
* 2.当传入Callback\<Response\>时，Callback的调用是工作线程，其他情况下均为主线程，可以直接进行设置View显示等操作

ResponseParser接口定义如下：

```java
public interface ResponseParser {
    boolean isSupported(Type type);
    <T> T praseResponse(Response response, Type type) throws Exception;
}
```

可以通过实现此接口解析Json,XML或者二进制流为对象的功能

demo模块app中分别有使用Jackson,FastJson,Gson实现Json解析，通过继承StringParser实现。

```java
public abstract class StringParser implements ResponseParser{

    @Override
    public final <T> T praseResponse(Response response, Type type) throws Exception{
        return praseResponse(HttpCallback.decodeResponseToString(response),type);
    }

    public abstract <T> T praseResponse(String content, Type type) throws Exception;
}
```

