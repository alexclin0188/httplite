# 前言

Http请求是做Android应用开发工作几乎必须要用到的东西。做Android开发这几年，从最开始仿照网上代码自己使用apache的DefaultHttpClient封装网络请求工具类，到后面开始使用GitHub上面的一些http框架，Afinal,xUtils到Volley,AsyncHttpClient等，网上这些http框架大多都还比较易用，但是做实际业务中还是感觉到业务和界面代码与Http请求的代码还是耦合性过高，特别是在服务器接口比较多的时候。所以自己在以前的项目中也一直在尝试做一些封装解耦，但是一直感觉达不到自己想要的效果，直到看到Retrofit这个类库。

在断断续续看了几个月的OkHttpClient和Retrofit源码，我终于决定尝试着封装一个自己的框架：[httplite](https://github.com/alexclin0188/httplite)

# 类库主要特性介绍
[httplite](https://github.com/alexclin0188/httplite)类库主要实现了以下特性
* 1.隔离了底层http实现，http实现可替换
     虽然okhttpclient的实现很好，但是有时候也会因为项目包大小等原因需要使用系统UrlConection来实现
* 2.建造者模式的流式调用和结果解析的解耦
    使用Request.url().param().header().***.async(Callback<T>)方式调用，可自定义多重ResponseParser来实现不同的http返回结果(json,protocolbuf等)
* 3.支持使用类似Retrofit的方式，使用java接口类来定义后后台API接口，并且支持RxJava，支持自定义注解

目前类库底层的http实现提供了okhttp2.x/okhttp3.x/URLConnection三种可选.

# 类库使用指南
## 一、添加依赖

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
            public void onProgress(boolean out, long current, long total) {
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
            public void onProgress(boolean out, long current, long total) {
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

只需定义自己的注解，在HttpLite的Retrofit实例中添加对应注解的处理器即可

定义注解和注解处理器,此处只列出GsonFieldProcesscor代码，详细参考Demo

```
public class GsonFieldProcesscor implements ParamMiscProcessor {
    public static final String BODY_TYPE = "gson_json_body";

    @Override
    public void process(Request request, Annotation[][] annotations, List<Pair<Integer, Integer>> list, Object... args) {
        //处理所有带有Gson注解的参数，list中存储的是所有Gson注解的位置
        JsonObject object = new JsonObject();
        for(Pair<Integer,Integer> pair:list){
            int argPos = pair.first;
            int annotationPos = pair.second;
            if(args[argPos]==null) continue;
            GsonField annotation = (GsonField) annotations[argPos][annotationPos];
            String key = annotation.value();
            if(args[argPos] instanceof String){
                object.addProperty(key,(String)args[argPos]);
            }else if(args[argPos] instanceof JsonElement){
                object.add(key,(JsonElement)args[argPos]);
            }else if(args[argPos] instanceof Boolean){
                object.addProperty(key,(Boolean)args[argPos]);
            }else if(args[argPos] instanceof Number){
                object.addProperty(key,(Number)args[argPos]);
            }
        }
        request.body(MediaType.APPLICATION_JSON,object.toString());
    }

    @Override
    public boolean support(Annotation annotation) {
        return annotation instanceof GsonField;
    }

    @Override
    public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
        //在此函数中检查参数类型是否定义正确
        if(!gsonSupportType(parameterType)){
            throw Util.methodError(method,"Annotation @GsonField only support parameter type String/JsonElement/Boolean/Number/int/long/double/short");
        }if(TextUtils.isEmpty(((GsonField)annotation).value())){
            throw Util.methodError(method,"The annotation {@GsonField(value) value} must not be null");
        }
    }

    private boolean gsonSupportType(Type type){
        return type==String.class || Util.isSubType(type,JsonElement.class) || type == int.class || type == long.class || type == double.class
                || type == short.class || Util.isSubType(type,Number.class) || type == boolean.class || type == Boolean.class;
    }
}
```


```
@BaseURL("http://192.168.99.238:10080/")
public interface CustomApi {
    @GET("/login")
    void login(
            @Query("username") String userName,
            @Query("password") String password,
            @Query("token") String token,
            @Tag Object tag,
            Callback<Result<UserInfo>> callback
    );

    @POST("/test")
    void testPost(@GsonField("param1") String param1,
                  @GsonField("param1")String param2,
                  Callback<Result<RequestInfo>> callback);
}
```

```
//添加自定义注解处理器
//普通的参数注解处理ParamterProcessor
Retrofit.registerParamterProcessor(new QueryProcessor());
//对个参数组合到一起的参数注解处理ParamMiscProcessor，如将多个参数组合成一个json字符串作为请求的BODY
Retrofit.registerParamMiscProcessor(new GsonFieldProcesscor());
//当注解处理的参数是用作Body时，还需要注册Body类型
Retrofit.basicAnnotationRule().registerBodyAnnotation(GsonField.class,
     GsonFieldProcesscor.BODY_TYPE,true);
```

```
        //创建实例
        CustomApi api = mHttplite.retrofit(CustomApi.class);
        //发起请求
        Object tag = new Object();
        api.login("user", "pass", "token", tag, new Callback<Result<UserInfo>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, Result<UserInfo> result) {
                //TODO
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
            }
        });
        api.testPost("test1", "test2", new Callback<Result<RequestInfo>>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers, Result<RequestInfo> result) {
                        //TODO
                        LogUtil.e("Result:"+result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        //TODO
                        LogUtil.e("onFailed",e);
                    }
                });
    }
```

### 4.RequestListener和MethodFilter的使用

HttpLite支持在创建API接口实例时传入RequestListener和MethodFilter

```
SampleApi api = mHttplite.retrofit(SampleApi.class,listener,filter);
```

* RequestListener主要用于监听接口中的请求，或者为请求添加一些通用参数

```
    RequestListener listener = new RequestListener() {
            @Override
            public void onRequest(HttpLite lite, Request request, Type resultType) {
                LogUtil.e("RequestUrl:"+request.rawUrl());
                //添加通用参数
                request.param("commonParam","1234");
            }
        };
```

* MethodFilter主要用于给某些请求加一些前置操作

```
    MethodFilter filter = new MethodFilter() {
            @Override
            public Object onMethod(HttpLite lite, final MethodInvoker invoker, final Object[] args) throws Throwable {
                String publicKey = ......
                if(TextUtils.isEmpty(publicKey)){
                    //publicKey是空，则先请求key
                    new Thread(){
                        @Override
                        public void run() {
                            //获取key
                            ......
                            //获取key成功后再发起真正的请求
                            invoker.invoke(args);
                        }
                    }.start();
                }else{
                    return invoker.invoke(args);
                }
            }
        };
```

## 四、配置ResponseParser

默认支持String的解析，但是类对象结果的解析需要使用httpLite.addResponseParser()添加支持该类型的解析器ResponseParser，可添加多个以便支持多种不同的结果解析

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
