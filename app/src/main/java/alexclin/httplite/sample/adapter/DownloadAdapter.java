package alexclin.httplite.sample.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import alexclin.httplite.sample.R;
import alexclin.httplite.sample.manager.DownloadTask;
import alexclin.httplite.sample.manager.TaskStateListener;

/**
 * DownloadAdapter
 *
 * @author alexclin 16/1/10 16:22
 */
public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {

    private List<DownloadTask> mList;

    public DownloadAdapter(List<DownloadTask> mList) {
        this.mList = mList;
    }

    @Override
    public DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_download, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DownloadViewHolder holder, int position) {
        DownloadTask task = mList.get(position);
        holder.onStateChanged(task);
        task.setStateListener(holder);
    }

    @Override
    public int getItemCount() {
        if(mList==null) return 0;
        return mList.size();
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder implements TaskStateListener,View.OnClickListener{

        private TextView mNameTv;
        private TextView mPathTv;
        private TextView mStateInfoTv;
        private TextView mProgressTv;
        private ProgressBar mProgressBar;

        private DownloadTask mTask;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            mNameTv = (TextView) itemView.findViewById(R.id.tv_file_name);
            mPathTv = (TextView) itemView.findViewById(R.id.tv_file_path);
            mStateInfoTv = (TextView) itemView.findViewById(R.id.tv_state_info);
            mProgressTv = (TextView) itemView.findViewById(R.id.tv_progress);
            mProgressBar = (ProgressBar) itemView.findViewById(R.id.progress_download);
            mStateInfoTv.setOnClickListener(this);
        }

        @Override
        public void onProgressUpdate(long current, long total) {
            if(total!=0){
                int progress = (int) (current*100/total);
                mProgressBar.setProgress(progress);
            }else{
                mProgressBar.setProgress(0);
            }
            mProgressTv.setText(String.format(Locale.getDefault(),"%d/%d",current,total));
        }

        @Override
        public void onStateChanged(DownloadTask task) {
            mTask = task;
            DownloadViewHolder holder = this;
            holder.mNameTv.setText(task.getName());
            holder.mPathTv.setText(task.getPath());
            if(task.getState()== DownloadTask.State.Downloading){
                holder.mProgressBar.setVisibility(View.VISIBLE);
                holder.mProgressTv.setVisibility(View.VISIBLE);
                onProgressUpdate(task.getCurrent(),task.getTotal());
            }else {
                holder.mStateInfoTv.setVisibility(View.VISIBLE);
                holder.mProgressBar.setVisibility(View.GONE);
                holder.mProgressTv.setVisibility(View.GONE);
            }
            String info = null;
            switch (task.getState()){
                case IDLE:
                    info = "未开启";
                    break;
                case Failed:
                    info = "失败";
                    break;
                case Canceled:
                    info = "已取消";
                    break;
                case Finished:
                    info = "成功";
                    break;
                case Downloading:
                    info = "下载中";
                    break;
            }
            holder.mStateInfoTv.setText(info);
        }

        @Override
        public void onClick(View v) {
            if(mTask!=null){
                if(mTask.getState().startAble)
                    mTask.resume(v.getContext());
                else
                    mTask.cancel();
            }
        }
    }
}
