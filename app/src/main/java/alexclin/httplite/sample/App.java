package alexclin.httplite.sample;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.Request;
import alexclin.httplite.internal.Mock;
import alexclin.httplite.listener.MockHandler;
import alexclin.httplite.okhttp2.Ok2Lite;
import alexclin.httplite.okhttp3.Ok3Lite;
import alexclin.httplite.sample.json.GsonParser;
import alexclin.httplite.url.URLite;
import alexclin.httplite.util.LogUtil;

/**
 * App
 *
 * @author alexclin
 * @date 16/1/10 10:56
 */
public class App extends Application {
    private HttpLite httpLite;
    private String baseUrl;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.setDebug(true);
        baseUrl = "http://192.168.99.238:10080/";
//        HttpLiteBuilder builder = Ok2Lite.create();
//        HttpLiteBuilder builder = Ok3Lite.create();
        HttpLiteBuilder builder = URLite.create();
        builder = builder.setConnectTimeout(10, TimeUnit.SECONDS)  //设置连接超时
                .setWriteTimeout(10, TimeUnit.SECONDS)  //设置写超时
                .setReadTimeout(10, TimeUnit.SECONDS)  //设置读超时
                .setMaxRetryCount(2)  //设置失败重试次数
                .setFollowRedirects(true)  //设置是否sFollowRedirects,默认false
                .setFollowSslRedirects(true) //设置是否setFollowSslRedirects
                .addResponseParser(new GsonParser())
                .baseUrl(baseUrl);
//                .setProxy(...)
//                .setProxySelector(...)
//                .setSocketFactory(...)
//                .setSslSocketFactory(...)
//                .setHostnameVerifier(..)
//                .baseUrl("http://xxx.xxx.xxx")  //BaseUrl,用于拼接完整的Url
//                .useCookie(...)  //设置CookieStore,设置则启用Cookie,不设置则不启用
//        httpLite = builder.build();
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
    }

    public static HttpLite httpLite(Context ctx){
        App app = (App) ctx.getApplicationContext();
        return app.httpLite;
    }

    public static App app(Context ctx){
        return (App) ctx.getApplicationContext();
    }

    public String getBaseUrl(){
        return baseUrl;
    }
}
