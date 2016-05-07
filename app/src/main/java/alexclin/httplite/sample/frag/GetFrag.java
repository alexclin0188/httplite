package alexclin.httplite.sample.frag;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.FileInfo;
import com.example.Result;

import java.util.List;
import java.util.Map;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.sample.App;
import alexclin.httplite.sample.R;
import alexclin.httplite.sample.adapter.FileAdapter;
import alexclin.httplite.sample.event.ChangeFragEvent;
import alexclin.httplite.sample.manager.DownloadManager;
import alexclin.httplite.util.LogUtil;
import de.greenrobot.event.EventBus;

/**
 * GetFrag
 *
 * @author alexclin 16/1/10 11:38
 */
public class GetFrag extends Fragment implements FileAdapter.OnFileClickListener,Callback<Result<List<FileInfo>>>,View.OnClickListener{
    private RecyclerView mRecyclerView;
    private FileAdapter mAdapter;

    private Button mBackUpBtn;

    private TextView mPathTv;

    private String url = "";

    private HttpLite mHttpLite;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_get,container,false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.file_list);
        mPathTv = (TextView) view.findViewById(R.id.tv_request_path);
        mAdapter = new FileAdapter(null,this);
        mRecyclerView.setAdapter(mAdapter);

        mBackUpBtn = (Button) view.findViewById(R.id.btn_back_up);
        mBackUpBtn.setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHttpLite = App.httpLite(getActivity());
        mHttpLite.url(url).header("header","not chinese").header("test_header","2016-01-06")
                .header("double_header","header1").header("double_header","head2")
                .param("type","json").param("param2","You dog").param("param3", "中文")
                .get().async(new Callback<Result<List<FileInfo>>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers,Result<List<FileInfo>> result) {
                //TODO
                GetFrag.this.onSuccess(req,headers,result);
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
                GetFrag.this.onFailed(req,e);
            }
        });
    }

    @Override
    public void onFileClick(FileInfo info) {
        if(info.isDir){
            url = info.filePath;
            requestPath(url);
        }else{
            DownloadManager.download(getActivity(),info.filePath,
                    getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),info.fileName,info.hash);
            EventBus.getDefault().post(new ChangeFragEvent(3));
        }
    }

    private void requestPath(String url) {
        mHttpLite.url(url).header("header","not chinese").header("test_header","2016-01-06")
                .header("double_header","header1").header("double_header","head2")
                .param("type","json").param("param2","You dog").param("param3", "中文")
                .get().async(this);
    }

    @Override
    public void onSuccess(Request req,Map<String,List<String>> headers,Result<List<FileInfo>> result) {
        LogUtil.e("Succuess:" + result);
        url = result.requestPath;
        mBackUpBtn.setVisibility((url.equals("")||url.equals("/"))?View.GONE:View.VISIBLE);
        mPathTv.setText(result.requestPath);
        if(result.code==200){
            mAdapter.update(result.data);
        }
    }

    @Override
    public void onFailed(Request req, Exception e) {
        LogUtil.e("Filed:"+e);
        e.printStackTrace();
    }

    @Override
    public void onClick(View v) {
        int index = url.lastIndexOf("/");
        if(index>-1){
            url = url.substring(0,index+1);
            requestPath(url);
        }
    }
}
