package alexclin.httplite.sample.retrofit;

import com.example.BaseResult;
import com.example.UserInfo;

import alexclin.httplite.Result;
import alexclin.httplite.annotation.BaseURL;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.sample.model.ZhihuData;

/**
 * SampleApi
 *
 * @author alexclin  16/5/5 22:41
 */
public interface SampleApi {

    //同步请求方法
    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    Result<ZhihuData> syncZhihu();

    //异步请求方法
    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    void asyncZhihu(Callback<ZhihuData> callback);
}
