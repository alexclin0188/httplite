package alexclin.httplite.sample.frag;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.example.BaseResult;
import com.example.FileInfo;
import com.example.util.EncryptUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.sample.App;
import alexclin.httplite.sample.R;
import alexclin.httplite.sample.RecycleViewDivider;
import alexclin.httplite.sample.adapter.FileAdapter;
import alexclin.httplite.util.LogUtil;

/**
 * PostFrag
 *
 * @author alexclin 16/1/10 11:38
 */
public class PostFrag extends Fragment implements FileAdapter.OnFileClickListener,View.OnClickListener{
    private RecyclerView mRecyclerView;
    private FileAdapter mAdapter;

    private Button mBackUpBtn;

    private TextView mPathTv;

    private String basePath;

    private String currentPath;

    private HttpLite mHttpLite;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_get,container,false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.file_list);
        mPathTv = (TextView) view.findViewById(R.id.tv_request_path);
        mAdapter = new FileAdapter(null,this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new RecycleViewDivider(getContext(), LinearLayoutManager.HORIZONTAL));
        mBackUpBtn = (Button) view.findViewById(R.id.btn_back_up);
        mBackUpBtn.setOnClickListener(this);
        if(basePath==null){
            File file = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if(file==null){
                file = getActivity().getCacheDir();
            }
            basePath = file.getAbsolutePath();
        }
        if(currentPath==null){
            currentPath = basePath;
        }
        mHttpLite = App.httpLite(getActivity());
        loadFiles(currentPath);
        return view;
    }

    @Override
    public void onClick(View v) {
        if(!currentPath.equals(basePath)){
            int index = currentPath.lastIndexOf("/");
            currentPath = currentPath.substring(0,index);
            loadFiles(currentPath);
        }
    }

    private void loadFiles(final String currentPath) {
        new Thread(){
            @Override
            public void run() {
                File file = new File(currentPath);
                if(file.isDirectory()){
                    File[] childs = file.listFiles();
                    final List<FileInfo> list = new ArrayList<>();
                    if (childs != null)
                        for (File child : childs) {
                            FileInfo fileInfo = new FileInfo();
                            fileInfo.fileName = child.getName();
                            fileInfo.filePath = child.getAbsolutePath().replace(basePath,"");
                            fileInfo.isDir = child.isDirectory();
                            if (child.isFile()) {
                                fileInfo.hash = EncryptUtil.hash(child);
                            }
                            list.add(fileInfo);
                        }
                    HttpLite.runOnMain(new Runnable() {
                        @Override
                        public void run() {
                            PostFrag.this.currentPath = currentPath;
                            mPathTv.setText(currentPath);
                            mAdapter.update(list);
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public void onFileClick(FileInfo info) {
        LogUtil.e("info:" + info);
        if(info.isDir){
            loadFiles(info.filePath);
        }else{
            File file = new File(basePath+info.filePath);
            new Request.Builder(String.format("/?hash=%s",info.hash)).post(MediaType.APPLICATION_STREAM,file).build().enqueue(mHttpLite,new Callback<BaseResult<String>>() {
                @Override
                public void onSuccess(Request req, Map<String, List<String>> headers,BaseResult<String> result) {
                    LogUtil.e("BaseResult:"+result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("onFailed:"+e);
                    e.printStackTrace();
                }
            });
            String type = MediaType.MULTIPART_FORM+";charset=utf-8";
            RequestBody body = RequestBody.createBody(file,MediaType.APPLICATION_STREAM);
            new Request.Builder("/").multipartType(type).multipart("早起早睡","身体好").multipart(info.fileName,info.hash).multipart(info.fileName,info.filePath,body)
                    .onProgress(new ProgressListener() {
                        @Override
                        public void onProgress(boolean out, long current, long total) {
                            LogUtil.e("是否上传:"+out+",cur:"+current+",total:"+total);
                        }
                    })
                    .post().build().enqueue(mHttpLite,new Callback<BaseResult<String>>() {
                @Override
                public void onSuccess(Request req,Map<String, List<String>> headers,BaseResult<String> result) {
                    LogUtil.e("BaseResult:"+result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("onFailed:"+e);
                    e.printStackTrace();
                }
            });
            new Request.Builder("/").post(MediaType.APPLICATION_JSON, JSON.toJSONString(info)).build().enqueue(mHttpLite,new Callback<String>() {
                @Override
                public void onSuccess(Request req,Map<String, List<String>> headers,String result) {
                    LogUtil.e("BaseResult:" + result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("E:" + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
            new Request.Builder("/").form("&test1","name&1").form("干撒呢","whatfuck").formEncoded(Uri.encode("test&2"),Uri.encode("name&2")).post().build().enqueue(mHttpLite,new Callback<String>() {
                @Override
                public void onSuccess(Request req,Map<String, List<String>> headers,String result) {
                    LogUtil.e("BaseResult:" + result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("E:" + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}
