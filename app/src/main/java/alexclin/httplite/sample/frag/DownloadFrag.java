package alexclin.httplite.sample.frag;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import alexclin.httplite.sample.R;
import alexclin.httplite.sample.adapter.DownloadAdpater;
import alexclin.httplite.sample.manager.DownloadManager;

/**
 * DownloadFrag
 *
 * @author alexclin
 * @date 16/1/10 11:57
 */
public class DownloadFrag extends Fragment{
    private RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_download,container,false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.download_list);
        mRecyclerView.setAdapter(DownloadManager.getDownloadAdapter());
        return view;
    }
}
