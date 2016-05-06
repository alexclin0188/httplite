package alexclin.httplite.sample.retrofit.custom;

import com.example.RequestInfo;
import com.example.Result;
import com.example.UserInfo;

import alexclin.httplite.annotation.BaseURL;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.annotation.Tag;
import alexclin.httplite.listener.Callback;

/**
 * CustomApi
 *
 * @author alexclin  16/5/6 19:19
 */
@BaseURL("http://192.168.99.238:10080/")
public interface CustomApi {
    @GET("/login")
    void login(
            @Query("username") String userName,
            @Query("password") String password,
            @Query("token") String token,
            @Tag Object tag,
            Callback<Result<UserInfo>> callback
    );

    @POST("/test")
    void testPost(@GsonField("param1") String param1,
                  @GsonField("param1")String param2,
                  Callback<RequestInfo> callback);
}
