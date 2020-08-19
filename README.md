# httplite
A android http library

# 版本修改记录 
## 1.0
* 完成版本发布
 
## 2.0修改记录
*  Request改为Builder模式
*  简化类库逻辑，去除不必要的封装
*  下载处理逻辑简化
## 2.0.1
* 修复设置Request拦截器后，request没有baseUrl的bug
## 2.0.2
* @JsonField注解支持Bean类型



## 说明

类库主要功能

* 1.实现Http结果的自动解析（使用ResponseParser接口）
* 2.隔离对Http具体实现类库的依赖，方便替换（目前类库中有OkLite,URLConnectionLite两种实现，对应[okhttp](https://github.com/square/okhttp)和系统URLConnection实现）
* 3.支持使用JAVA接口的方式定义API接口, 思路来源于[Retrofit](https://github.com/square/retrofit)

## 使用

### 添加Gradle依赖

使用系统URLConnection作为http实现

```
    compile 'alexclin.httplite:httplite-url:2.0.0'
```

如果需要在JAVA接口定义API时使用RxJava,则需要添加以下依赖

```
    compile 'io.reactivex.rxjava2:rxandroid:2.0.1'//可选，在Android中使用会需要使用此类库
    compile 'io.reactivex.rxjava2:rxjava:2.1.0'
```

PS:如需使用jar包可在项目releaselib目录下找到对应jar包

### 初始化Httplite

配置并创建HttpLite

```java
public HttpLite create(){
	  return new URLite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(context))//设置CookieStore
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
                .setRequestInterceptor(new RequestInterceptor() {...})
                .build();
}
```
### 发起网络请求
#### 普通方式

```
		new Request.Builder("http://news-at.zhihu.com/api/4/news/latest").get()
                .build().enqueue(httpLite,new Callback<ZhihuData>(){
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, ZhihuData result) {
                //.....
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //.....
            }
        });
```
#### Java Interface+注解的使用方式

支持使用Java Interface+注解的方式来定义API接口

* 1. 使用Interface定义API接口

```
@BaseURL("https://192.168.99.238:10080/")
public interface SampleApi {
    //异步请求方法
    @POST("/login")
    void login(
            @JsonField("username") String userName,
            @JsonField("password")String password,
            @JsonField("token") String token,
            Callback<BaseResult<UserInfo>> callback
    );
    //同步请求方法
    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    Result<ZhihuData> syncZhihu();
}
```

* 2. 创建API接口实例并发起请求

```
		  创建Retrofit实例
		  Retrofit retrofit = new Retrofit(httpLite,releaseMode);
		  //创建接口实例
        SampleApi sampleApi = retrofit.create(SampleApi.class);
        //使用接口实例发起同步网络请求
        Result<ZhihuData> result = sampleApi.syncZhihu();
        if(result.isSuccessful()){
            ZhihuData data = result.result();
            Map<String,List<String>> headers = result.headers();
            //.....
        }else{
            Exception exception = result.error();
            //.....
        }
        //使用接口发起异步网络请求
        sampleApi.asyncZhihu(new Callback<ZhihuData>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, ZhihuData result) {
                //.....
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //.....
            }
        });
```

### 其它可选的httplite网络实现

* http实现有okhttp2和okhttp3可选

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

使用okhttp2或okhttp3作为网络实现时需在build.gradle中添加以下依赖

```         
    //使用okhttp 2.7.5作为http实现
    compile 'alexclin.httplite:httplite-okhttp2:2.0.0'
    compile 'com.squareup.okhttp:okhttp:2.7.5'

    //使用okhttp 3.2.0作为http实现
    compile 'alexclin.httplite:httplite-okhttp3:2.0.0'
    compile 'com.squareup.okhttp3:okhttp:3.2.0'
```

PS:如需使用jar包可在项目releaselib目录下找到对应jar包

### 详细使用指南请移步[使用指南](./useage.md)

