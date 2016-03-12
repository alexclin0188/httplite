package alexclin.httplite.sample;

import android.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * LeftMenuAdapter
 *
 * @author alexclin 16/1/10 11:44
 */
public class LeftMenuAdapter extends RecyclerView.Adapter<LeftMenuAdapter.MenuViewHolder> {

    public interface OnItemClickListener{
        void onItemClick(MenuItem item);
    }

    private List<MenuItem> mList;
    private OnItemClickListener mItemClickListener;

    public LeftMenuAdapter(List<MenuItem> mList, OnItemClickListener mItemClickListener) {
        this.mList = mList;
        this.mItemClickListener = mItemClickListener;
    }

    @Override
    public MenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        textView.setTextColor(parent.getResources().getColor(R.color.black));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(0,10,0,10);
        final MenuViewHolder holder = new MenuViewHolder(textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.onItemClick(mList.get(holder.postion));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(MenuViewHolder holder, int position) {
        holder.mTextView.setText(mList.get(position).name);
        holder.postion = position;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public MenuItem getItem(int postion){
        if(mList==null||mList.size()<=postion){
            return null;
        }
        return mList.get(postion);
    }

    public static class MenuViewHolder extends RecyclerView.ViewHolder{

        private TextView mTextView;
        private int postion;
        private OnItemClickListener listener;

        public MenuViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView;
        }
    }

    public static class MenuItem{
        public MenuItem(String name, Class<? extends Fragment> clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        public String name;
        public Class<? extends Fragment> clazz;
        public Fragment frag;
    }
}
