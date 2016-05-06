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
    compile 'alexclin.httplite:httplite-okhttp2:1.0.1'
```

使用okhttp 3.2.0作为http实现
```
    compile 'alexclin.httplite:httplite-okhttp3:1.0.1'
```

使用系统URLConnection作为http实现
```
    compile 'alexclin.httplite:httplite-url:1.0.1'
```

或者直接使用releaselib中的jar包

* 1 okhttp2: httplite1.0.1.jar+httplite-ok2lite1.0.1.jar+okhttp 2.x.x版本jar包
* 2 okhttp3: httplite1.0.1.jar+httplite-ok3lite1.0.1.jar+okhttp 3.x.x版本jar包
* 3 url: httplite1.0.1.jar+httplite-urlite1.0.1.jar

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

//    @HTTP(path = "test2",method = Method.POST)
//    void test2(
//            @Progress @Retry @Cancel @Tag MergeListener listener,
//            @Param("123") TestModel[] array,
//            @Header("test1") List<String> list,
//            @Headers Map<String,String> map,
//            @IntoFile String str,
//            @Multipart MultiPart multiPart,
//            Clazz<TestModel> clazz
//    );

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
  builder  = builder.setConnectTimeout(3, TimeUnit.SECONDS)  //设置连接超时
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
                .setRelease(false)   //设置是否是Release状态，是Release状态会关闭对接口函数定义的检查，提升效率
                .addResponseParser(new JacksonParser()); //添加ResponseParser实现结果解析
                .requestFilter(new RequestFilter() {
                                    @Override
                                    public void onRequest(HttpLite lite,Request request, Type type) {
                                        request.header("handle","misc");
                                    }
                                })；      //对所有请求进行监听，做某些处理
  //正常使用状态
  Httplite httpLite = builder.build();
  //本地模拟模式
        httpLite = builder.mock(new MockHandler() {
            @Override
            public <T> void mock(Request request, Mock<T> mock) throws Exception {
//                mock.mockProgress(long current,long total); //模拟进度调用
//                mock.mockRetry(long current,long max);  //模拟重试调用
                  //TODO 模拟网络数据
//                mock.mock(Response response);//模拟原生Response
//                mock.mockJson(....);
//                mock.mock(new File("...."))；
//                mock.mock(T result,Map<String, List<String>> headers);//直接模拟解析结果
            }

            @Override
            public boolean needMock(Request request) {
                //TODO 判断该请求是否需要Mock
                return true;
            }
        });
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

### Retrofit功能使用需要注意的地方

* 1.使用注解定义的API接口函数在调用时，会忽略掉所有传入的null参数，建议develop时对方法调用和参数做监听，可以通过设置MethodListener来实现

```
    httplite.getRetrofit().setMethodListener(MethodListener methodListener);
```

* 2.Release版本请设置HttpLiteBuilder.setRelease(true)，创建的HttpLite和对应的Retrofit会关闭接口Annotation的定义检查（develop时请打开），减少因对接口注解使用正确性检查行带来的性能消耗

### 支持自定义注解的使用

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

### 直接使用HttpLite的http方法

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

# 前言

Http请求是做Android应用开发工作几乎必须要用到的东西。做Android开发这几年，从最开始仿照网上代码自己使用apache的DefaultHttpClient封装网络请求工具类，到后面开始使用GitHub上面的一些http框架，Afinal,xUtils到Volley,AsyncHttpClient等，网上这些http框架大多都还比较易用，但是做实际业务中还是感觉到业务和界面代码与Http请求的代码还是耦合性过高，特别是在服务器接口比较多的时候。所以自己在以前的项目中也一直在尝试做一些封装解耦，但是一直感觉达不到自己想要的效果，直到看到Retrofit这个类库。

在我的上一篇文章中，简单介绍了一下Retrofit的实现原理。

在断断续续看了几个月的OkHttpClient和Retrofit源码，我终于决定尝试着封装一个自己的框架：[httplite](https://github.com/alexclin0188/httplite)

# 类库主要特性介绍
[httplite](https://github.com/alexclin0188/httplite)类库主要实现了以下特性
* 1.隔离了底层http实现，http实现可替换
     虽然okhttpclient的实现很好，但是有时候也会因为项目包大小等原因需要使用系统UrlConection来实现
* 2.建造者模式的流式调用和结果解析的解耦
    使用Request.url().param().header().***.async(Callback<T>)方式调用，可自定义多重ResponseParser来实现不同的http返回结果(json,protocolbuf等)
* 3.支持使用类似Retrofit的方式，使用java接口类来定义后后台API接口，并且支持RxJava，支持自定义注解

目前类库底层的http实现提供了okhttp2.x/okhttp3.x/URLConnection三种可选。

# 类库使用指南
## 一、添加依赖
* Gradle使用okhttp 2.7.5作为http实现
```
compile 'alexclin.httplite:httplite-okhttp2:1.0.1'
```
使用okhttp 3.2.0作为http实现
```
compile 'alexclin.httplite:httplite-okhttp3:1.0.1'
```
使用系统URLConnection作为http实现
```
compile 'alexclin.httplite:httplite-url:1.0.1'
```
或者直接使用releaselib中的jar包
* 1 okhttp2: httplite1.0.1.jar+httplite-ok2lite1.0.1.jar+okhttp 2.x.x版本jar包
* 2 okhttp3: httplite1.0.1.jar+httplite-ok3lite1.0.1.jar+okhttp 3.x.x版本jar包
* 3 url: httplite1.0.1.jar+httplite-urlite1.0.1.jar

## 二、类库初始化
或者也可以直接使用jar包
首先创建HttpLiteBuilder进行配置，目前有三种HTTP实现可选
```
//使用OkHttp2.x作为Http实现
HttpLiteBuilder builder = Ok2Lite.create();
//使用OkHttp3.x作为Http实现
HttpLiteBuilder builder = Ok3Lite.create();
//使用系统URLConnection作为http实现
HttpLiteBuilder builder = URLite.create();
```
对Builder进行配置
```
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
```
创建HttpLite实例
```
HttpLite httpLite = builder.build();
```
另外提供mock支持，需传入MockHandler
```
httpLite = builder.mock(new MockHandler() {
                      @Override
                      public <T> void mock(Request request, Mock<T> mock) throws Exception {
                          //模拟完整的http返回结果输入流
                          mock.mock(int code,String msg,Map<String, List<String>> headers, final InputStream stream,MediaType mediaType);
                          //直接模拟结果
                          mock(T result, Map<String, List<String>> headers)；
                          //模拟Json格式的结果
                          mock.mockJson(....);
                          //以文件内容作为Http返回结果输入流
                          mock.mock(new File("...."))；
                      }
                      @Override
                      public boolean needMock(Request request) {
                          //TODO 判断该请求是否需要Mock
                          return true;
                      }
            });
```
## 二、普通方式发起http请求

发起普通GET请求

```
    mHttpLite.url(url).header("header","not chinese").header("test_header","2016-01-06")
                .header("double_header","header1").addHeader("double_header","head2")
                .param("type","json").param("param2","You dog").param("param3", "中文")
                .get().async(new Callback<Result<List<FileInfo>>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers,Result<List<FileInfo>> result) {
                //TODO
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
            }
        });
```

发起post请求，监听进度

```
    //multipart上传文件
    MediaType type = mHttpLite.parse(MediaType.MULTIPART_FORM+";charset=utf-8");
    RequestBody body = mHttpLite.createRequestBody(mHttpLite.parse(MediaType.APPLICATION_STREAM),file);
    mHttpLite.url("/").multipartType(type).multipart("早起早睡","身体好").multipart(info.fileName,info.hash).multipart(info.fileName,info.filePath,body)
       .onProgress(new ProgressListener() {
            @Override
            public void onProgressUpdate(boolean out, long current, long total) {
                LogUtil.e("是否上传:"+out+",cur:"+current+",total:"+total);
            }
        })
        .post().async(new Callback<Result<String>>() {
            @Override
            public void onSuccess(Request req,Map<String, List<String>> headers,Result<String> result) {
                LogUtil.e("Result:"+result);
            }
            @Override
            public void onFailed(Request req, Exception e) {
                LogUtil.e("onFailed:"+e);
                e.printStackTrace();
           }
    });
    //post json
    mHttpLite.url("/").post(MediaType.APPLICATION_JSON, JSON.toJSONString(info)).async(new Callback<String>() {
        @Override
        public void onSuccess(Request req,Map<String, List<String>> headers,String result) {
            LogUtil.e("Result:" + result);
        }
        @Override
        public void onFailed(Request req, Exception e) {
            LogUtil.e("E:" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    });
    //post form表单
    mHttpLite.url("/").form("&test1","name&1").form("干撒呢","whatfuck").formEncoded(Uri.encode("test&2"),Uri.encode("name&2")).post().async(new Callback<String>() {
        @Override
       public void onSuccess(Request req,Map<String, List<String>> headers,String result) {
            LogUtil.e("Result:" + result);
        }
        @Override
        public void onFailed(Request req, Exception e) {
            LogUtil.e("E:" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    });
```

下载文件

```
mHttpLite.url(url).intoFile(dir,name,true,true)
        .onProgress(new ProgressListener() {
            @Override
            public void onProgressUpdate(boolean out, long current, long total) {
                        //TODO
                    }
                })
                .download(new Callback<File>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers, File result) {
                        //TODO
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        //TODO
                    }
                });
```

## 三、使用java接口定义API接口(类似Retrofit的功能)

### 1.基础使用

```
        //生成API接口实例
        final SampleApi api = mHttplite.retrofit(SampleApi.class);
        //调用异步方法
        api.login("user", "pass", "token", new Callback<Result<UserInfo>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, Result<UserInfo> result) {
                //TODO
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
            }
        });
        //调用异步方法
        new Thread(){
            @Override
            public void run() {
                //获取知乎主页数据
                try {
                    ZhihuData data = api.syncZhihu();
                    //TODO
                } catch (Exception e) {
                    //TODO
                }
            }
        }.start();
        //生成Call
        final Call call = api.zhihuCall();
        //异步调用Call
        call.async(new Callback<ZhihuData>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, ZhihuData result) {
                //TODO
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
            }
        });
        //或者同步调用Call
        new Thread(){
            @Override
            public void run() {
                //获取知乎主页数据
                try {
                    ZhihuData data = call.sync(new Clazz<ZhihuData>(){});
                    //TODO
                } catch (Exception e) {
                    //TODO
                }
            }
        }.start();
```

### 2.RxJava的支持

支持RxJava需要在配置HttpLiteBuilder时添加RxCallAdapter

```
HttpLiteBuilder builder = ....
.....
builder.addCallAdapter(new RxCallAdapter());
.....

```
定义返回Obserable的API函数

```
@GET("http://news-at.zhihu.com/api/4/news/latest")
Observable<ZhihuData> testZhihu();
```
使用返回的Obserable

```
Observable<ZhihuData> observable = apiService.testZhihu();
observable.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Subscriber<ZhihuData>() {
       @Override
       public void onCompleted() {
           LogUtil.e("onCompleted");
       }
       @Override
       public void onError(Throwable e) {
           LogUtil.e("Onfailed", e);
       }
       @Override
       public void onNext(ZhihuData zhihuData) {
           LogUtil.e("Result:" + zhihuData);
           LogUtil.e("Result:" + (Thread.currentThread()== Looper.getMainLooper().getThread()));
       }
    });
```

### 3.自定义注解的使用

自定义注解支持**方法注解**和**参数注解**

只需在HttpLite的Retrofit实例中添加对应注解的处理器即可

```
```

### 4.RequestListener和MethodFilter的使用



## 四、配置ResponseParser

