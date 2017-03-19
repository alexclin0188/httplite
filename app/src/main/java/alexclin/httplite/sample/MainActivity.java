package alexclin.httplite.sample;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.RequestInfo;
import com.example.BaseResult;

import java.util.ArrayList;
import java.util.List;

import alexclin.httplite.Result;
import alexclin.httplite.sample.event.ChangeFragEvent;
import alexclin.httplite.sample.frag.RequestFrag;
import alexclin.httplite.sample.frag.DownloadFrag;
import alexclin.httplite.sample.frag.GetFrag;
import alexclin.httplite.sample.frag.PostFrag;
import alexclin.httplite.sample.frag.RetrofitFrag;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Util;
import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,LeftMenuAdapter.OnItemClickListener{

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.left_drawer)
    RecyclerView mRecyclerView;

    private Toolbar mToolbar;
    private LeftMenuAdapter leftMenuAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setSubtitle("点击左侧机器人切换");

        mToolbar.setNavigationIcon(R.mipmap.ic_launcher);
        mToolbar.setNavigationOnClickListener(this);

        List<LeftMenuAdapter.MenuItem> list = new ArrayList<>();
        list.add(new LeftMenuAdapter.MenuItem("自定义请求", RequestFrag.class));
        list.add(new LeftMenuAdapter.MenuItem("下载上传管理", DownloadFrag.class));
        list.add(new LeftMenuAdapter.MenuItem("Retrofit", RetrofitFrag.class));
        list.add(new LeftMenuAdapter.MenuItem("获取文件列表", GetFrag.class));
        list.add(new LeftMenuAdapter.MenuItem("列表选择上传", PostFrag.class));
        leftMenuAdapter = new LeftMenuAdapter(list,this);
        mRecyclerView.addItemDecoration(new RecycleViewDivider(this,LinearLayoutManager.HORIZONTAL));
        mRecyclerView.setAdapter(leftMenuAdapter);
        EventBus.getDefault().register(this);
        onItemClick(list.get(0));
//        TestRetrofit.testCustom(App.httpLite(this));
//        TestRetrofit.testFilter(App.httpLite(this));
//        TestRetrofit.testSampleApi(App.httpLite(this));
        Util.isSubType(new Clazz<Result<RequestInfo>>(){}.type(),Result.class);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(mDrawerLayout.isDrawerOpen(mRecyclerView)){
            mDrawerLayout.closeDrawer(mRecyclerView);
        }else{
            mDrawerLayout.openDrawer(mRecyclerView);
        }
    }

    @Override
    public void onItemClick(LeftMenuAdapter.MenuItem item) {
        if(item.frag==null){
            try {
                item.frag = item.clazz.newInstance();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.content_layout,item.frag).commit();
        mToolbar.setTitle(item.name);
        mDrawerLayout.closeDrawer(mRecyclerView);
    }

    public void onEvent(ChangeFragEvent event){
        onItemClick(leftMenuAdapter.getItem(event.toIndex));
    }
}
