package alexclin.httplite.sample;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;
import alexclin.httplite.cookie.PersistentCookieStore;
import alexclin.httplite.listener.RequestListener;
import alexclin.httplite.okhttp3.Ok3Lite;
import alexclin.httplite.sample.json.GsonParser;
import alexclin.httplite.util.LogUtil;

/**
 * App
 *
 * @author alexclin 16/1/10 10:56
 */
public class App extends Application {
    private HttpLite httpLite;

    public static HttpLite httpLite(Context ctx){
        App app = (App) ctx.getApplicationContext();
        return app.httpLite;
    }

    public static App app(Context ctx){
        return (App) ctx.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.setDebug(true);
//        HttpLiteBuilder builder = Ok2Lite.create();
//        HttpLiteBuilder builder = Ok3Lite.create();

        TrustAllManager manager = new TrustAllManager();

        HttpLiteBuilder builder = Ok3Lite.create();
        builder = builder.setConnectTimeout(30, TimeUnit.SECONDS)  //设置连接超时
                .setWriteTimeout(30, TimeUnit.SECONDS)  //设置写超时
                .setReadTimeout(30, TimeUnit.SECONDS)  //设置读超时
                .setMaxRetryCount(2)  //设置失败重试次数
                .setFollowRedirects(true)  //设置是否sFollowRedirects,默认false
                .setFollowSslRedirects(true) //设置是否setFollowSslRedirects
                .addResponseParser(new GsonParser())
                .baseUrl("https://192.168.99.238:10080/")
//                .setProxy(...)
//                .setProxySelector(...)
                .setSocketFactory(SocketFactory.getDefault())
                .setSslSocketFactory(manager.getSocketFactory())
                .setHostnameVerifier(manager)
                .useCookie(PersistentCookieStore.getInstance(this))  //设置CookieStore,设置则启用Cookie,不设置则不启用
//                .addCallAdapter(new RxCallAdapter())
        ;//添加Rx支持

        builder.requestListener(new RequestListener() {
            @Override
            public void onRequestStart(Request request, Type resultType) {
                LogUtil.e("Request:"+request);
            }

            @Override
            public void onRequestEnd(Request request, Type resultType, Response response) {

            }
        });

        httpLite = builder.build();
//        httpLite = builder.mock(new MockHandler() {
//            @Override
//            public <T> void mock(Request request, Mock<T> mock) throws Exception {
//                  //模拟完整的http返回结果输入流
////                mock.mock(int code,String msg,Map<String, List<String>> headers, final InputStream stream,MediaType mediaType);
//                  //直接模拟结果
////                mock(T result, Map<String, List<String>> headers)；
//                  //模拟Json格式的结果
////                mock.mockJson(....);
//                  //以文件内容作为Http返回结果输入流
////                mock.mock(new File("...."))；
//            }
//
//            @Override
//            public boolean needMock(Request request) {
//                //TODO 判断该请求是否需要Mock
//                return true;
//            }
//        });
    }
}
