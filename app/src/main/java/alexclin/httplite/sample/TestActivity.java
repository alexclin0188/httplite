package alexclin.httplite.sample;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.Result;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.retrofit.Retrofit;
import alexclin.httplite.sample.model.ZhihuData;
import alexclin.httplite.sample.retrofit.SampleApi;

/**
 * TestActivity
 *
 * @author alexclin  2017/5/7 12:25
 */

public class TestActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HttpLite httpLite = App.httpLite(this);
        boolean releaseMode = false;
        //创建Retrofit实例
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
    }
}
