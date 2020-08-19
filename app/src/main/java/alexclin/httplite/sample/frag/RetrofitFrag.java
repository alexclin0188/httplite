package alexclin.httplite.sample.frag;

import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.BaseResult;
import com.example.RequestInfo;
import com.example.UserInfo;

import java.io.File;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Handle;
import alexclin.httplite.Result;
import alexclin.httplite.sample.App;
import alexclin.httplite.sample.retrofit.ExMergeCallback;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.sample.R;
import alexclin.httplite.sample.model.ZhihuData;
import alexclin.httplite.sample.retrofit.ApiService;
import alexclin.httplite.sample.retrofit.ExRequestInfo;
import alexclin.httplite.sample.retrofit.MergeListener;
import alexclin.httplite.util.LogUtil;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
                apiService.doSomething("IamHolder", "12345", "67890","qwert", this, new Callback<BaseResult<RequestInfo>>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers,BaseResult<RequestInfo> result) {
                        LogUtil.e("BaseResult:" + result);
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
                        LogUtil.e("SyncResult->start");
                        Result<ZhihuData> data = apiService.syncZhihu();
                        if(data.error()==null){
                            LogUtil.e("SyncResult:"+data);
                        }else {
                            LogUtil.e("SyncFailed", data.error());
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
                break;
            case R.id.btn_retrofit7:
                apiService.putJsonBody("JsonPath", "123", 3, 5.0,10, new Callback<ExRequestInfo>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers,ExRequestInfo result) {
                        LogUtil.e("BaseResult:" + result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        LogUtil.e("Req:"+req);
                        LogUtil.e("OnFailed", e);
                    }
                });
                break;
            case R.id.btn_retrofit8:
                String saveDir1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                ExMergeCallback callback = new ExMergeCallback();
                final Handle handle1 = apiService.downdloadFile("holder_123","12345","56789",saveDir1,callback);
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handle1.cancel();
                    }
                },2);
                Observable<ZhihuData> observable = apiService.testZhihu();
                observable.subscribeOn(Schedulers.io())
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
                break;
        }
    }

}
