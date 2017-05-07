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

* httplite类库分为四部分：
  - httplite: httplite核心库，'alexclin.httplite:httplite:x.x.x'
  - urlite: 使用系统URLConnection作为网络底层实现，'alexclin.httplite:httplite-url:x.x.x'
  - okhttp2：使用okhttp2作为网络底层实现,'alexclin.httplite:httplite-okhttp2:x.x.x'
  - okhttp3: 使用okhttp3作为网络底层实现,'alexclin.httplite:httplite-okhttp3:x.x.x'

其中urlite/okhttp2/okkhttp3的gradle包是依赖于httplite，所以使用时只用添加三者其一为依赖即可。如使用URLite则添加依赖

```
    compile 'alexclin.httplite:httplite-url:2.0.0'
```

* 如果需要在JAVA接口定义API时使用RxJava,则需要添加以下依赖

```
    compile 'io.reactivex.rxjava2:rxandroid:2.0.1'//可选，在Android中使用会需要使用此类库
    compile 'io.reactivex.rxjava2:rxjava:2.1.0'
```

httplite核心库中自动做了RxJava类的检测，创建接口实例时会自动增加Obserble\<T\>类的支持

* okhttp2-httplite与okhttp3-httplite在使用时，需要另外加入okhttp对应版本的依赖

  - 使用okhttp 2.x作为http实现

```
    compile 'alexclin.httplite:httplite-okhttp2:2.0.0'
    compile 'com.squareup.okhttp:okhttp:2.x'
```

  - 使用okhttp 3.x作为http实现

```
    compile 'alexclin.httplite:httplite-okhttp3:2.0.0'
    compile 'com.squareup.okhttp3:okhttp:3.x'
```

PS:如需使用jar包可在项目releaselib目录下找到对应jar包

## 二、类库初始化

首先创建HttpLiteBuilder进行配置，目前有三种HTTP实现可选

对Builder进行配置并创建HttpLite实例

```
//使用系统URLConnection作为http实现
URLite.Builder urlBuilder = new alexclin.httplite.url.URLite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(app));//设置CookieStore;
                
//使用OkHttp2.x作为Http实现
Ok2Lite.Builder ok2Builder = new Ok2Lite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(app));
                
//使用OkHttp3.x作为Http实现
Ok3Lite.Builder ok3Builder = new Ok3Lite.Builder()
                .setCookieJar(new CookieJar() {...});//okhttp3的cookie接口
//任选上面三者之一
HttpLiteBuilder builder = urlBuilder
                .setConnectTimeout(30, TimeUnit.SECONDS)  //设置连接超时
                .setWriteTimeout(30, TimeUnit.SECONDS)  //设置写超时
                .setReadTimeout(30, TimeUnit.SECONDS)  //设置读超时
                .setMaxRetryCount(2)  //设置失败重试次数
                .setFollowRedirects(true)  //设置是否sFollowRedirects,默认false
                .setFollowSslRedirects(true) //设置是否setFollowSslRedirects
                .addResponseParser(new GsonParser())
                .setBaseUrl("https://192.168.99.238:10080/")
					//...设置其它支持的属性....
                .setMockHandler(handler)
                .setSocketFactory(SocketFactory.getDefault())
                .setSslSocketFactory(manager.getSocketFactory())
                .setHostnameVerifier(manager)
                .setRequestInterceptor(new RequestInterceptor() {...});
Httplite httplite = builder.build();
```

另外提供mock支持，需传入MockHandler

```
         MockHandler handler = new MockHandler() {
            @Override
            public <T> void mock(Request request, Mock<T> mock) throws Exception {
                LogUtil.e("mock Request:"+request);
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
                return request.getUrl().startsWith("http://198test");
            }
        };
builder = builder.setMockHandler(handler);
........

```

## 二、普通方式发起http请求

发起普通GET请求

```
        new Request.Builder(url).header("header","not chinese").header("test_header","2016-01-06")
                .header("double_header","header1").header("double_header","head2")
                .param("type","json").param("param2","You dog").param("param3", "中文").get().build()
                .enqueue(httpLite,new Callback<BaseResult<List<FileInfo>>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers,BaseResult<List<FileInfo>> result) {
                //TODO
                GetFrag.this.onSuccess(req,headers,result);
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
                GetFrag.this.onFailed(req,e);
            }
        });
```

发起post请求，监听进度

```
    //multipart上传文件
    MediaType type = mHttpLite.parse(MediaType.MULTIPART_FORM+";charset=utf-8");
    RequestBody body = mHttpLite.createRequestBody(mHttpLite.parse(MediaType.APPLICATION_STREAM),file);
    new Request.Builder("/").multipartType(type).multipart("早起早睡","身体好").multipart(info.fileName,info.hash).multipart(info.fileName,info.filePath,body)
       .onProgress(new ProgressListener() {
            @Override
            public void onProgress(boolean out, long current, long total) {
                LogUtil.e("是否上传:"+out+",cur:"+current+",total:"+total);
            }
        }).post()
        .enqueue(httpLite,new Callback<BaseResult<String>>() {
                @Override
                public void onSuccess(Request req,Map<String, List<String>> headers,BaseResult<String> result) {
                    LogUtil.e("BaseResult:"+result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("onFailed:"+e);
                    e.printStackTrace();
                }
            });
    //post json
    new Request.Builder("/").post(MediaType.APPLICATION_JSON, JSON.toJSONString(info))
    	.enqueue(httpLite,new Callback<String>() {
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
    new Request.Builder("/").form("&test1","name&1").form("干撒呢","whatfuck").formEncoded(Uri.encode("test&2"),Uri.encode("name&2")).post()
    	.enqueue(httpLite,new Callback<String>() {
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
	new Request.Builder(url).intoFile(dir,name,true,true)
        .onProgress(new ProgressListener() {
            @Override
            public void onProgress(boolean out, long current, long total) {
                        //TODO
                    }
                })
        .get().build()
        .enqueue(httpLite,new Callback<File>() {
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

## 三、JavaInterface+注解的是使用方式

### 1.基础使用

* 1. 定义Api接口

```
@BaseURL("https://192.168.99.238:10080/")
public interface ApiService {
    @POST("/login")
    void login(
            @JsonField("username") String userName,
            @JsonField("password")String password,
            @JsonField("token") String token,
            @Tag Object tag,
            Callback<BaseResult<UserInfo>> callback
    );

    @GET("http://www.baidu.com")
    void testBaidu(Callback<String> callback);

    @GET("http://news-at.zhihu.com/api/4/news/latest")
    void testZhihu(Callback<ZhihuData> callback);

    @GET("/download/{test_holder}")
    @FixHeaders({"handle:GET"})
    void downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress ProgressListener progressListener,
            Callback<File> callback
    );

    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    Result<ZhihuData> syncZhihu();

    @HTTP(method = Request.Method.POST,path = "/dosomething/{some_path}")
    void doSomething(
            @Path("some_path") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @Form("form_f1") String form_f1,
            @Tag Object tag,
            Callback<BaseResult<RequestInfo>> callback
    );

    @HTTP(method = Request.Method.POST,path = "/dosomething/{some_path}")
    Result<RequestInfo> doSomethingSync(
            @Path("some_path") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @Form("form_f1") String form_f1,
            @Tag Object tag);

    @HTTP(method = Request.Method.PUT,path = "put/{holde_test}")
    void putJsonBody(
            @Path("holde_test") String holder,
            @JsonField("field1") String field1,
            @JsonField("field2") int field2,
            @JsonField("field3") Double field3,
            @JsonField("field4") long field4,
            Callback<ExRequestInfo> callback
    );

    @GET("/download/{test_holder}")
    Handle downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress MergeCallback<File> callback
    );
}
```

* 创建接口实例并使用API接口

```
		 Retrofit retrofit = new Retrofit(httpLite,releaseMode);
		 ApiService apiService = retrofit.create(ApiService.class);
		 //异步方法
		 apiService.login("user_alexclin", "12345678", "sdfdsfdsfdsfsdf", this,new Callback<BaseResult<UserInfo>>() {
		                    @Override
		                    public void onSuccess(Request req,Map<String, List<String>> headers,BaseResult<UserInfo> result) {
		                        LogUtil.e("BaseResult:"+result);
		                    }
		
		                    @Override
		                    public void onFailed(Request req, Exception e) {
		                        LogUtil.e("Onfailed",e);
		                    }
		                });
 		//同步方法
 		new Thread(){
                    @Override
                    public void run() {
                        LogUtil.e("SyncResult->start");
                        Result<ZhihuData> data = apiService.syncZhihu();
                        if(data.error()==null){
                            LogUtil.e("SyncResult:"+data);
                        }else {
                            LogUtil.e("SyncFailed", data.error());
                        }
                    }
                }.start();
       //下载
       String saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
       MergeListener mergeListener = new MergeListener();
       apiService.downdloadFile("holder_123","12345","56789",saveDir,mergeListener,new Callback<File>(){

                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers,File result) {
                        LogUtil.e("Req:"+req);
                        LogUtil.e("BaseResult:" + result);
                        for(String key:headers.keySet()){
                            for(String head:headers.get(key)){
                                LogUtil.e("head:"+key+","+head);
                            }
                        }
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("Req:"+req);
                        LogUtil.e("OnFailed", e);
                    }
                });
       ....
       //TODO 详细使用参考demo中RetrofitFrag类中使用
```

### 2.RxJava的支持

在项目添加RxAndroid和RxJava的依赖后，即可在Interface定义中使用RxJava

```
    compile 'io.reactivex.rxjava2:rxandroid:2.0.1'
    compile 'io.reactivex.rxjava2:rxjava:2.1.0'
```

定义返回Obserable的API函数

```
	@GET("http://news-at.zhihu.com/api/4/news/latest")
    Observable<ZhihuData> testZhihu();

    @GET("http://news-at.zhihu.com/api/4/news/latest")
    Observable<alexclin.httplite.Result<ZhihuData>> testZhihuResult();
```
使用返回的Obserable

```
    apiService.testZhihu().subscribeOn(Schedulers.io())
           .observeOn(AndroidSchedulers.mainThread())
           .subscribe(new Observer<ZhihuData>() {
              @Override
              public void onSubscribe(@NonNull Disposable d) {
                  LogUtil.e("onSubscribe d:"+d.isDisposed());
              }

              @Override
              public void onError(Throwable e) {
                  LogUtil.e("Onfailed", e);
              }

              @Override
              public void onComplete() {
                  LogUtil.e("onCompleted");
              }

              @Override
              public void onNext(ZhihuData zhihuData) {
                  LogUtil.e("onNext Result:" + zhihuData);
                  LogUtil.e("onNext Result isMain:" + (Thread.currentThread()== Looper.getMainLooper().getThread()));
              }
          });
```

### 3.自定义注解的使用

自定义注解支持**方法注解**和**参数注解**

只需定义自己的注解，在HttpLite的Retrofit实例中添加对应注解的处理器即可

* 定义注解和注解处理器,此处只列出GsonFieldProcesscor代码，详细参考Demo

```
public class GsonFieldProcesscor implements ParamMiscProcessor {
    public static final String BODY_TYPE = "gson_json_body";

    @Override
    public void process(Request.Builder request, Annotation[][] annotations, List<Pair<Integer, Integer>> list, Object... args) {
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
* 在接口定义中使用自定义注解

```
@BaseURL("https://192.168.99.238:10080/")
public interface CustomApi {
    @GET("/login")
    @FixHeaders({
            "abcded:tests123",
            "headerFix:headerValue"
    })
    void login(
            @Query("username") String userName,
            @Query("password") String password,
            @Query("token") String token,
            @Tag Object tag,
            Callback<BaseResult<UserInfo>> callback
    );

    @POST("/test")
    void testPost(@GsonField("param1") String param1,
                  @GsonField("param2")String param2,
                  Callback<BaseResult<RequestInfo>> callback);
}
```

* 为自定义注解注册处理器和和做必要的Body类型注册

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

* 创建接口实例使用API

```
        //创建实例
        CustomApi api = retrofit.create(CustomApi.class);
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
    public final <T> T parseResponse(Response response, Type type) throws Exception{
        return parseResponse(ObjectParser.decodeToString(response), type);
    }

    public abstract <T> T parseResponse(String content, Type type) throws Exception;
}
```
