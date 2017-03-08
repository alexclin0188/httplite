package alexclin.httplite.sample.frag;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.RequestInfo;
import com.example.Result;
import com.example.UserInfo;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.listener.Response;
//import alexclin.httplite.okhttp2.Ok2Lite;
import alexclin.httplite.okhttp3.Ok3Lite;
import alexclin.httplite.sample.App;
import alexclin.httplite.sample.TrustAllManager;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.RequestListener;
//import alexclin.httplite.rx.AndroidSchedulers;
//import alexclin.httplite.rx.RxCallAdapter;
import alexclin.httplite.sample.R;
import alexclin.httplite.sample.json.JacksonParser;
import alexclin.httplite.sample.model.ZhihuData;
import alexclin.httplite.sample.retrofit.ApiService;
import alexclin.httplite.sample.retrofit.ExRequestInfo;
import alexclin.httplite.sample.retrofit.MergeListener;
import alexclin.httplite.util.LogUtil;
//import rx.Observable;
//import rx.Subscriber;
//import rx.schedulers.Schedulers;

/**
 * RetrofitFrag
 *
 * @author alexclin 16/1/31 11:32
 */
public class RetrofitFrag extends Fragment implements View.OnClickListener{
    private View view;

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
//        TestRetrofit.test(httpLite);
        if(apiService==null) apiService = App.retrofit(getActivity()).create(ApiService.class);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_retrofit1:
                apiService.login("user_alexclin", "12345678", "sdfdsfdsfdsfsdf", this,new Callback<Result<UserInfo>>() {
                    @Override
                    public void onSuccess(Request req,Map<String, List<String>> headers,Result<UserInfo> result) {
                        LogUtil.e("Result:"+result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("Onfailed",e);
                    }
                });
                App.httpLite(getActivity()).cancel(this);
                break;
            case R.id.btn_retrofit2:
                apiService.testBaidu(new Callback<String>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers,String result) {
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
                    public void onSuccess(Request req, Map<String, List<String>> headers,ZhihuData result) {
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
                    public void onSuccess(Request req, Map<String, List<String>> headers,Result<RequestInfo> result) {
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
                apiService.downdloadFile("holder_123","12345","56789",saveDir,mergeListener,new Callback<File>(){

                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers,File result) {
                        LogUtil.e("Req:"+req);
                        LogUtil.e("Result:" + result);
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
                break;
            case R.id.btn_retrofit7:
                apiService.putJsonBody("JsonPath", "123", 3, 5.0,10, new Callback<ExRequestInfo>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers,ExRequestInfo result) {
                        LogUtil.e("Result:" + result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("Req:"+req);
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
//                Observable<ZhihuData> observable = apiService.testZhihu();
//                observable.subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(new Subscriber<ZhihuData>() {
//                    @Override
//                    public void onCompleted() {
//                        LogUtil.e("onCompleted");
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        LogUtil.e("Onfailed", e);
//                    }
//
//                    @Override
//                    public void onNext(ZhihuData zhihuData) {
//                        LogUtil.e("Result:" + zhihuData);
//                        LogUtil.e("Result:" + (Thread.currentThread()== Looper.getMainLooper().getThread()));
//                    }
//                });
                break;
        }
    }

}
