package alexclin.httplite.sample.frag;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.RequestInfo;
import com.example.Result;
import com.example.UserInfo;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.okhttp2.Ok2Lite;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.rx.AndroidSchedulers;
import alexclin.httplite.rx.RxInvoker;
import alexclin.httplite.sample.R;
import alexclin.httplite.sample.json.JacksonParser;
import alexclin.httplite.sample.model.ZhihuData;
import alexclin.httplite.sample.retrofit.ApiService;
import alexclin.httplite.sample.retrofit.ExRequestInfo;
import alexclin.httplite.sample.retrofit.MergeListener;
import alexclin.httplite.util.LogUtil;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * RetrofitFrag
 *
 * @author alexclin 16/1/31 11:32
 */
public class RetrofitFrag extends Fragment implements View.OnClickListener{
    private View view;
    private HttpLite httpLite;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view==null) view = inflater.inflate(R.layout.frag_retrofit,container,false);
        view.findViewById(R.id.btn_retrofit1).setOnClickListener(this);
        view.findViewById(R.id.btn_retrofit2).setOnClickListener(this);
        view.findViewById(R.id.btn_retrofit3).setOnClickListener(this);
        view.findViewById(R.id.btn_retrofit4).setOnClickListener(this);
        view.findViewById(R.id.btn_retrofit5).setOnClickListener(this);
        view.findViewById(R.id.btn_retrofit6).setOnClickListener(this);
        view.findViewById(R.id.btn_retrofit7).setOnClickListener(this);
        view.findViewById(R.id.btn_retrofit8).setOnClickListener(this);
        httpLite = initHttpLite();
//        TestRetrofit.test(httpLite);
        if(apiService==null) apiService = httpLite.retrofit(ApiService.class);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_retrofit1:
                apiService.login("user_alexclin", "12345678", "sdfdsfdsfdsfsdf", this,new Callback<Result<UserInfo>>() {
                    @Override
                    public void onSuccess(Result<UserInfo> result, Map<String, List<String>> headers) {
                        LogUtil.e("Result:"+result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("Onfailed",e);
                    }
                });
                httpLite.cancel(this);
                break;
            case R.id.btn_retrofit2:
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
                break;
            case R.id.btn_retrofit3:
                apiService.testZhihu(new Callback<ZhihuData>() {
                    @Override
                    public void onSuccess(ZhihuData result, Map<String, List<String>> headers) {
                        LogUtil.e("Zhihu:" + result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("OnFailed", e);
                    }
                });
                break;
            case R.id.btn_retrofit4:
                apiService.doSomething("IamHolder", "12345", "67890","qwert", this, new Callback<Result<RequestInfo>>() {
                    @Override
                    public void onSuccess(Result<RequestInfo> result, Map<String, List<String>> headers) {
                        LogUtil.e("Result:" + result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("OnFailed", e);
                    }
                });
                break;
            case R.id.btn_retrofit5:
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            LogUtil.e("SyncResult->start");
                            ZhihuData data = apiService.syncZhihu(new Clazz<ZhihuData>() {});
                            LogUtil.e("SyncResult:"+data);
                        }catch (Exception e){
                            LogUtil.e("SyncFailed", e);
                        }
                    }
                }.start();
                break;
            case R.id.btn_retrofit6:
                String saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                MergeListener mergeListener = new MergeListener();
                Handle handle = apiService.downdloadFile("holder_123","12345","56789",saveDir,mergeListener,mergeListener,new Callback<File>(){

                    @Override
                    public void onSuccess(File result, Map<String, List<String>> headers) {
                        LogUtil.e("Result:" + result);
                        for(String key:headers.keySet()){
                            for(String head:headers.get(key)){
                                LogUtil.e("head:"+key+","+head);
                            }
                        }
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("OnFailed", e);
                    }
                });
                break;
            case R.id.btn_retrofit7:
                apiService.putJsonBody("JsonPath", "123", 3, 5.0,10, new Callback<ExRequestInfo>() {
                    @Override
                    public void onSuccess(ExRequestInfo result, Map<String, List<String>> headers) {
                        LogUtil.e("Result:" + result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("OnFailed", e);
                    }
                });
                break;
            case R.id.btn_retrofit8:
//                String saveDir1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
//                ExMergeCallback callback = new ExMergeCallback();
//                final Handle handle1 = apiService.downdloadFile("holder_123","12345","56789",saveDir1,callback);
//                callback.setHandle(handle1);
//                view.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        handle1.cancel();
//                    }
//                },2);
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
                break;
        }
    }

    private HttpLite initHttpLite(){
        if(this.httpLite!=null) return this.httpLite;
//        String baseUrl = "http://192.168.99.238:10080/";
        HttpLiteBuilder builder = Ok2Lite.create();//URLite.create();
        HttpLite lite = builder.setConnectTimeout(10, TimeUnit.SECONDS)  //设置连接超时
                .setWriteTimeout(10, TimeUnit.SECONDS)  //设置写超时
                .setReadTimeout(10, TimeUnit.SECONDS)  //设置读超时
                .setMaxRetryCount(2)  //设置失败重试次数
                .setFollowRedirects(true)  //设置是否sFollowRedirects,默认false
                .setFollowSslRedirects(true) //设置是否setFollowSslRedirects
                .setCache(getActivity().getCacheDir(), 10 * 1024 * 1024)
//                .baseUrl(baseUrl)
                .addResponseParser(new JacksonParser())
                .requestFilter(new RequestFilter() {
                    @Override
                    public void onRequest(HttpLite lite, Request request, Type type) {
                        request.header("handle", "misc");
                        request.cacheExpire(Request.NO_CACHE);
                        request.onProgress(new ProgressListener() {
                            @Override
                            public void onProgressUpdate(boolean out, long current, long total) {
                                LogUtil.e(String.format(Locale.ENGLISH,"Out:%b,cur:%d,total:%d",out,current,total));
                            }
                        });
                    }
                })
                .addRetrofitInvoker(new RxInvoker())
                .build();
        return lite;
    }
}
