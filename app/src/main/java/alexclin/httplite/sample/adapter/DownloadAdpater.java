package alexclin.httplite.sample.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import alexclin.httplite.sample.R;
import alexclin.httplite.sample.manager.DownloadManager;
import alexclin.httplite.sample.manager.DownloadTask;

/**
 * DownloadAdpater
 *
 * @author alexclin
 * @date 16/1/10 16:22
 */
public class DownloadAdpater extends RecyclerView.Adapter<DownloadAdpater.DownloadViewHolder> implements DownloadManager.DownloadListener {

    private List<DownloadTask> mList;

    public DownloadAdpater(List<DownloadTask> mList) {
        this.mList = mList;
    }

    @Override
    public DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_download, parent, false);
        DownloadViewHolder holder = new DownloadViewHolder(view);
        return holder;
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

    @Override
    public void onTaskChanged(List<DownloadTask> taskList) {
        mList = taskList;
        notifyDataSetChanged();
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder implements DownloadTask.TaskStateListener{

        private TextView mNameTv;
        private TextView mPathTv;
        private TextView mStateInfoTv;
        private TextView mProgressTv;
        private ProgressBar mProgressBar;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            mNameTv = (TextView) itemView.findViewById(R.id.tv_file_name);
            mPathTv = (TextView) itemView.findViewById(R.id.tv_file_path);
            mStateInfoTv = (TextView) itemView.findViewById(R.id.tv_state_info);
            mProgressTv = (TextView) itemView.findViewById(R.id.tv_progress);
            mProgressBar = (ProgressBar) itemView.findViewById(R.id.progress_download);
        }

        @Override
        public void onProgressUpdate(long current, long total) {
            if(total!=0){
                int progress = (int) (current*100/total);
                mProgressBar.setProgress(progress);
            }else{
                mProgressBar.setProgress(0);
            }
            mProgressTv.setText(String.format("%d/%d",current,total));
        }

        @Override
        public void onStateChanged(DownloadTask task) {
            DownloadViewHolder holder = this;
            boolean isDownloading = !task.isFinished()&&!task.isCanceled()&&!task.isFailed();
            holder.mNameTv.setText(task.getName());
            holder.mPathTv.setText(task.getPath());
            if(isDownloading){
                holder.mStateInfoTv.setVisibility(View.GONE);
                holder.mProgressBar.setVisibility(View.VISIBLE);
                holder.mProgressTv.setVisibility(View.VISIBLE);
                onProgressUpdate(task.getCurrent(),task.getTotal());
            }else {
                holder.mStateInfoTv.setVisibility(View.VISIBLE);
                holder.mProgressBar.setVisibility(View.GONE);
                holder.mProgressTv.setVisibility(View.GONE);
                if(task.isFinished()){
                    holder.mStateInfoTv.setText(String.format("下载成功，hash校验：%b",task.isValidHash()));
                }else if(task.isCanceled()){
                    holder.mStateInfoTv.setText("下载已被取消");
                }else if(task.isFailed()){
                    holder.mStateInfoTv.setText("下载失败");
                }
            }
        }
    }
}
