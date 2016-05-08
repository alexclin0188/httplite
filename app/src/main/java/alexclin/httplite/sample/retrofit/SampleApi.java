package alexclin.httplite.sample.retrofit;

import com.example.Result;
import com.example.UserInfo;

import alexclin.httplite.Call;
import alexclin.httplite.annotation.BaseURL;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.sample.model.ZhihuData;
import alexclin.httplite.util.Clazz;

/**
 * SampleApi
 *
 * @author alexclin  16/5/5 22:41
 */
public interface SampleApi {
    //异步请求方法
    @POST("/login")
    void login(
            @JsonField("username") String userName,
            @JsonField("password")String password,
            @JsonField("token") String token,
            Callback<Result<UserInfo>> callback
    );
    //同步请求方法
    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    ZhihuData syncZhihu() throws Exception;

    //只生成Call对象
    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    Call zhihuCall();
}
