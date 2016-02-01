package alexclin.httplite.sample.retrofit;

import com.example.RequestInfo;
import com.example.Result;
import com.example.UserInfo;

import java.io.File;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Clazz;
import alexclin.httplite.DownloadHandle;
import alexclin.httplite.Method;
import alexclin.httplite.annotation.Cancel;
import alexclin.httplite.annotation.Form;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.HTTP;
import alexclin.httplite.annotation.Header;
import alexclin.httplite.annotation.Headers;
import alexclin.httplite.annotation.IntoFile;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.Multipart;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.annotation.Param;
import alexclin.httplite.annotation.Path;
import alexclin.httplite.annotation.Progress;
import alexclin.httplite.annotation.Retry;
import alexclin.httplite.annotation.Tag;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.CancelListener;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.retrofit.MultiPart;
import alexclin.httplite.sample.model.TestModel;
import alexclin.httplite.sample.model.ZhihuData;

/**
 * ApiService
 *
 * @author alexclin
 * @date 16/1/30 18:59
 */
public interface ApiService {
    @POST("/login")
    void login(
            @Param("username") String userName,
            @Param("password")String password,
            @Param("token") String token,
            Callback<Result<UserInfo>> callback
    );

    @GET("http://www.baidu.com")
    void testBaidu(Callback<String> callback);

    @GET("http://news-at.zhihu.com/api/4/news/latest")
    void testZhihu(Callback<ZhihuData> callback);

    @GET("/download/{test_holder}")
    DownloadHandle downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress ProgressListener progressListener,
            @Cancel CancelListener cancelListener,
            @Retry RetryListener retryListener,
            Callback<File> callback
    );

    @GET( "http://news-at.zhihu.com/api/4/news/latest")
    ZhihuData syncZhihu(Clazz<ZhihuData> clazz) throws Exception;

    @HTTP(method = Method.POST,path = "/dosomething/{some_path}")
    void doSomething(
            @Path("some_path") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @Form("form_f1") String form_f1,
            @Tag Object tag,
            Callback<Result<RequestInfo>> callback
    );

    @HTTP(method = Method.POST,path = "/dosomething/{some_path}")
    Result<RequestInfo> doSomethingSync(
            @Path("some_path") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @Form("form_f1") String form_f1,
            @Tag Object tag,
            Clazz<Result<RequestInfo>> clazz
    ) throws Exception;

    @HTTP(method = Method.PUT,path = "put/{holde_test}")
    void putJsonBody(
            @Path("holde_test") String holder,
            @JsonField("field1") String field1,
            @JsonField("field2") int field2,
            @JsonField("field3") Double field3,
            @JsonField("field4") long field4,
            Callback<ExRequestInfo> callback
    );

    @GET("/download/{test_holder}")
    DownloadHandle downdloadFile(
            @Path("test_holder") String holder,
            @Param("param1") String param1,
            @Param("param2") String param2,
            @IntoFile String path,
            @Progress @Cancel @Retry MergeCallback<File> callback
    );

//    @HTTP(path = "test2",method = Method.POST)
//    void test2(
//            @Progress @Retry @Cancel @Tag MergeListener listener,
//            @Param("123") TestModel[] array,
//            @Header("test1") List<String> list,
//            @Headers Map<String,String> map,
//            @IntoFile String str,
//            @Multipart MultiPart multiPart,
//            Clazz<TestModel> clazz
//    );
}
