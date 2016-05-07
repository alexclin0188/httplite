package alexclin.httplite.sample.frag;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import alexclin.httplite.sample.R;
import alexclin.httplite.sample.RecycleViewDivider;
import alexclin.httplite.sample.manager.DownloadManager;
import alexclin.httplite.util.Util;

/**
 * DownloadFrag
 *
 * @author alexclin 16/1/10 11:57
 */
public class DownloadFrag extends Fragment implements View.OnClickListener,DialogInterface.OnClickListener{
    private RecyclerView mRecyclerView;
    private DownloadManager mDownloadManager;

    private String downloadPath;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_download,container,false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.download_list);
        view.findViewById(R.id.btn_clear_file).setOnClickListener(this);
        mDownloadManager = new DownloadManager();
        mRecyclerView.setAdapter(mDownloadManager.getDownloadAdapter());
        mRecyclerView.addItemDecoration(new RecycleViewDivider(getContext(), LinearLayoutManager.HORIZONTAL));
        insertTask();
        return view;
    }

    private void insertTask(){
        File file = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if(file!=null) downloadPath = file.getAbsolutePath()+"/test/";
        List<String> list = new ArrayList<>();
        list.add("http://dl.360safe.com/se/360se_setup.exe");
        int i = 1;
        for(String url:list){
            mDownloadManager.addTask(url,downloadPath,null);
            i++;
        }
    }

    @Override
    public void onDestroyView() {
        mDownloadManager.release();
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        new AlertDialog.Builder(v.getContext()).setTitle("提示").setPositiveButton("清除",this)
                .setNegativeButton("取消",this).setMessage("是否停止所有任务并删除所有下载的文件").create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which==DialogInterface.BUTTON_POSITIVE){
            mDownloadManager.stopAll();
            mDownloadManager.getDownloadAdapter().notifyDataSetChanged();
            Util.deleteFileOrDir(new File(downloadPath));
        }
    }
}
