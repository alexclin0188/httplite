package alexclin.httplite.sample.frag;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.example.FileInfo;
import com.example.Result;
import com.example.util.EncryptUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import alexclin.httplite.HttpLite;
import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.sample.App;
import alexclin.httplite.sample.R;
import alexclin.httplite.sample.adapter.FileAdapter;
import alexclin.httplite.util.LogUtil;

/**
 * PostFrag
 *
 * @author alexclin
 * @date 16/1/10 11:38
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
                    HttpLite.runOnMainThread(new Runnable() {
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
            mHttpLite.url(String.format("/?hash=%s",info.hash)).post(MediaType.APPLICATION_STREAM,file).execute(new Callback<Result<String>>() {
                @Override
                public void onSuccess(Result<String> result, Map<String, List<String>> headers) {
                    LogUtil.e("Result:"+result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("onFailed:"+e);
                    e.printStackTrace();
                }
            });
            MediaType type = mHttpLite.parse(MediaType.MULTIPART_FORM+";charset=utf-8");
            RequestBody body = mHttpLite.createRequestBody(mHttpLite.parse(MediaType.APPLICATION_STREAM),file);
            mHttpLite.url("/").multipartType(type).multipart("早起早睡","身体好").multipart(info.fileName,info.hash).multipart(info.fileName,info.filePath,body).post().execute(new Callback<Result<String>>() {
                @Override
                public void onSuccess(Result<String> result, Map<String, List<String>> headers) {
                    LogUtil.e("Result:"+result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("onFailed:"+e);
                    e.printStackTrace();
                }
            });
            mHttpLite.url("/").post(MediaType.APPLICATION_JSON, JSON.toJSONString(info)).execute(new Callback<String>() {
                @Override
                public void onSuccess(String result, Map<String, List<String>> headers) {
                    LogUtil.e("Result:" + result);
                }

                @Override
                public void onFailed(Request req, Exception e) {
                    LogUtil.e("E:" + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
            mHttpLite.url("/").form("&test1","name&1").form("干撒呢","二逼").formEncoded(Uri.encode("test&2"),Uri.encode("name&2")).post().execute(new Callback<String>() {
                @Override
                public void onSuccess(String result, Map<String, List<String>> headers) {
                    LogUtil.e("Result:" + result);
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
