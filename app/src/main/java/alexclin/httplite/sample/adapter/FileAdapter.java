package alexclin.httplite.sample.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.FileInfo;

import java.util.List;

import alexclin.httplite.sample.R;

/**
 * FileAdapter
 *
 * @author alexclin 16/1/10 12:20
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {



    public interface OnFileClickListener {
        void  onFileClick(FileInfo info);
    }

    private List<FileInfo> mFiles;
    private OnFileClickListener mListener;

    public FileAdapter(List<FileInfo> mFiles,OnFileClickListener listener) {
        this.mFiles = mFiles;
        this.mListener = listener;
    }

    public void update(List<FileInfo> result) {
        this.mFiles = result;
        notifyDataSetChanged();
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_file,parent,false);
        final FileViewHolder holder = new FileViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onFileClick(mFiles.get(holder.postion));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int position) {
        FileInfo info = mFiles.get(position);
        holder.mTypeTv.setText(info.isDir?"[文件夹]":"[文件]");
        holder.mNameTv.setText(info.fileName);
        holder.mHashTv.setText(info.hash==null?"":String.format("[hash:%s]",info.hash));
        holder.mHashTv.setVisibility(info.hash == null?View.GONE:View.VISIBLE);
        holder.postion = position;
    }

    @Override
    public int getItemCount() {
        if(mFiles==null){
            return 0;
        }
        return mFiles.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder{

        private TextView mTypeTv;
        private TextView mNameTv;
        private TextView mHashTv;

        private int postion;

        public FileViewHolder(View itemView) {
            super(itemView);
            mTypeTv = (TextView) itemView.findViewById(R.id.tv_file_type);
            mNameTv = (TextView) itemView.findViewById(R.id.tv_file_name);
            mHashTv = (TextView) itemView.findViewById(R.id.tv_file_hash);
        }
    }
}
