package alexclin.httplite.sample.retrofit.custom;

import com.example.BaseResult;
import com.example.RequestInfo;
import com.example.UserInfo;

import alexclin.httplite.annotation.BaseURL;
import alexclin.httplite.annotation.FixHeaders;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.annotation.Tag;
import alexclin.httplite.listener.Callback;

/**
 * CustomApi
 *
 * @author alexclin  16/5/6 19:19
 */
@BaseURL("https://192.168.99.238:10080/")
public interface CustomApi {
    @GET("/login")
    @FixHeaders({
            "abcded:tests123",
            "headerFix:headerValue"
    })
    void login(
            @Query("username") String userName,
            @Query("password") String password,
            @Query("token") String token,
            @Tag Object tag,
            Callback<BaseResult<UserInfo>> callback
    );

    @POST("/test")
    void testPost(@GsonField("param1") String param1,
                  @GsonField("param2")String param2,
                  Callback<BaseResult<RequestInfo>> callback);
}
