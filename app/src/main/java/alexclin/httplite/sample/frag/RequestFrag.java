package alexclin.httplite.sample.frag;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.BaseResult;
import com.example.RequestInfo;

import java.util.List;
import java.util.Map;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.sample.App;
import alexclin.httplite.sample.R;
import alexclin.httplite.sample.model.ZhihuData;
import alexclin.httplite.util.LogUtil;

/**
 * RequestFrag
 *
 * @author alexclin 16/1/10 11:37
 */
public class RequestFrag extends Fragment implements View.OnClickListener{

    private EditText mSearchEdt;
    private TextView mRequestInfo;
    private TextView mReturnInfo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frag_request,null);
        root.findViewById(R.id.btn_test1).setOnClickListener(this);
        root.findViewById(R.id.btn_test2).setOnClickListener(this);
        root.findViewById(R.id.btn_test3).setOnClickListener(this);
        root.findViewById(R.id.btn_search).setOnClickListener(this);
        mSearchEdt = (EditText) root.findViewById(R.id.edt_search);
        mRequestInfo = (TextView) root.findViewById(R.id.tv_request);
        mReturnInfo= (TextView) root.findViewById(R.id.tv_return);
        return root;
    }

    public void onClick(View view){
        HttpLite lite = App.httpLite(getActivity());
        switch (view.getId()){
            case R.id.btn_test1:
                new Request.Builder("https://192.168.99.238:10080/test").header("header","not chinese").header("test_header","2016-01-06")
                        .header("double_header","header1").header("double_header","head2")
                        .param("param1","I'm god").param("param2","You dog").param("param3","中文").get().build().enqueue(lite,new Callback<String>() {
                    @Override
                    public void onSuccess(Request req,Map<String,List<String>> headers,String result) {
                        mRequestInfo.setText(req.toString());
                        mReturnInfo.setText(result);
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        mRequestInfo.setText(req.toString());
                        mReturnInfo.setText(e.getLocalizedMessage());
                    }
                });
                break;
            case R.id.btn_test2:
                new Request.Builder("http://news-at.zhihu.com/api/4/news/latest").get().build().enqueue(lite,new Callback<ZhihuData>() {
                    @Override
                    public void onSuccess(Request req,Map<String,List<String>> headers,ZhihuData result) {
                        mRequestInfo.setText(req.toString());
                        LogUtil.e("BaseResult:" + result);
                        mReturnInfo.setText(result.toString());
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        mRequestInfo.setText(req.toString());
                        e.printStackTrace();
                        mReturnInfo.setText(e.getLocalizedMessage());
                    }
                });
                break;
            case R.id.btn_test3:
                new Request.Builder("http://192.168.99.238:10080/abcde").header("header","not chinese").header("test_header","2016-01-06")
                        .header("double_header","header1").header("double_header","head2")
                        .param("type","json").param("param2","You dog").param("param3", "中文").get().build().enqueue(lite,new Callback<BaseResult<RequestInfo>>() {
                    @Override
                    public void onSuccess(Request req,Map<String,List<String>> headers,BaseResult<RequestInfo> result) {
                        mRequestInfo.setText(req.toString());
                        mReturnInfo.setText(result.toString());
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        mRequestInfo.setText(req.toString());
                        mReturnInfo.setText(e.getLocalizedMessage());
                    }
                });
                break;
            case R.id.btn_search:
                String keyword = mSearchEdt.getText().toString();
                if(TextUtils.isEmpty(keyword)){
                    Toast.makeText(view.getContext(),"关键字不能为空",Toast.LENGTH_SHORT).show();
                }else{
                    new Request.Builder("http://www.baidu.com/s?pn=0&rn=10&tn=json").param("wd",keyword,false).get().build().enqueue(lite,new Callback<String>() {
                        @Override
                        public void onSuccess(Request req, Map<String, List<String>> headers, String result) {
                            mRequestInfo.setText(req.toString());
                            mReturnInfo.setText(result);
                            mSearchEdt.setText("");
                        }

                        @Override
                        public void onFailed(Request req, Exception e) {
                            mRequestInfo.setText(req.toString());
                            mReturnInfo.setText(e.getLocalizedMessage());
                        }
                    });
                }
                break;
        }

    }

    private void clearInfo(){
        mRequestInfo.setText("请求中...");
        mReturnInfo.setText("请求中...");
    }
}
