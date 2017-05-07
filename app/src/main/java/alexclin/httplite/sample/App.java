package alexclin.httplite.sample;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.Mock;
import alexclin.httplite.Request;
import alexclin.httplite.listener.MockHandler;
import alexclin.httplite.cookie.PersistentCookieStore;
import alexclin.httplite.listener.RequestInterceptor;
import alexclin.httplite.okhttp2.Ok2Lite;
import alexclin.httplite.okhttp3.Ok3Lite;
import alexclin.httplite.retrofit.Retrofit;
import alexclin.httplite.sample.json.GsonParser;
import alexclin.httplite.url.URLite;
import alexclin.httplite.url.cache.CacheHandler;
import alexclin.httplite.util.LogUtil;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * App
 *
 * @author alexclin 16/1/10 10:56
 */
public class App extends Application {
    private HttpLite httpLite;
    private Retrofit retrofit;

    public static HttpLite httpLite(Context ctx){
        App app = (App) ctx.getApplicationContext();
        if(app.httpLite==null){
            app.httpLite = createHttp(app);
        }
        return app.httpLite;
    }

    public static Retrofit retrofit(Context ctx){
        App app = (App) ctx.getApplicationContext();
        if(app.retrofit==null){

            app.retrofit = new Retrofit(httpLite(ctx),false);
        }
        return app.retrofit;
    }

    public static App app(Context ctx){
        return (App) ctx.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.setDebug(true);
    }

    private static HttpLite createHttp(App app){
        //        HttpLiteBuilder builder = Ok2Lite.create();
//        HttpLiteBuilder builder = Ok3Lite.create();

        TrustAllManager manager = new TrustAllManager();

        MockHandler handler = new MockHandler() {
            @Override
            public <T> void mock(Request request, Mock<T> mock) throws Exception {
                LogUtil.e("mock Request:"+request);
                  //模拟完整的http返回结果输入流
//                mock.mock(int code,String msg,Map<String, List<String>> headers, final InputStream stream,MediaType mediaType);
                  //直接模拟结果
//                mock(T result, Map<String, List<String>> headers)；
                  //模拟Json格式的结果
//                mock.mockJson(....);
                  //以文件内容作为Http返回结果输入流
//                mock.mock(new File("...."))；
            }

            @Override
            public boolean needMock(Request request) {
                //TODO 判断该请求是否需要Mock
                return request.getUrl().startsWith("http://198test");
            }
        };

        URLite.Builder urlBuilder = new alexclin.httplite.url.URLite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(app));//设置CookieStore;

        Ok2Lite.Builder ok2Builder = new Ok2Lite.Builder()
                .setCookieStore(PersistentCookieStore.getInstance(app));

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



        HttpLiteBuilder builder = urlBuilder
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
                        //支持对Request做一些处理操作
                        //Request.Builder newBuilder = request.newBuilder();
                        LogUtil.e("Request:" + request);
                        return request;
                    }
                });
        return builder.build();
    }

}
