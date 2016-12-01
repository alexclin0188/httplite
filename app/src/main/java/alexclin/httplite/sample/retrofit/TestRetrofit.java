package alexclin.httplite.sample.retrofit;

import android.text.TextUtils;

import com.example.FileInfo;
import com.example.RequestInfo;
import com.example.Result;
import com.example.UserInfo;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Call;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.RequestListener;
import alexclin.httplite.listener.Response;
import alexclin.httplite.retrofit.MethodFilter;
import alexclin.httplite.retrofit.MethodInvoker;
import alexclin.httplite.retrofit.Retrofit;
import alexclin.httplite.sample.model.ZhihuData;
import alexclin.httplite.sample.retrofit.custom.CustomApi;
import alexclin.httplite.sample.retrofit.custom.GsonField;
import alexclin.httplite.sample.retrofit.custom.GsonFieldProcesscor;
import alexclin.httplite.sample.retrofit.custom.QueryProcessor;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * TestRetrofit
 *
 * @author alexclin 16/1/30 19:08
 */
public class TestRetrofit {
    public static void test(HttpLite lite){
//        Class<ApiService> clazz = ApiService.class;
//        Method[] methods = clazz.getDeclaredMethods();
//        for(Method method:methods){
//            Type[] methodParamTypes = method.getGenericParameterTypes();
//           for(Type type:methodParamTypes){
//               LogUtil.e("Type:"+type+",p1:"+(type instanceof ParameterizedType));
//               LogUtil.e("Type:"+type+",p1:"+(type instanceof GenericArrayType));
//               LogUtil.e("Type:"+type+",p1:"+(type instanceof TypeVariable));
//               LogUtil.e("Type:"+type+",p1:"+(type instanceof WildcardType));
//           }
//        }
//        for(Method m:methods){
//            if(m.getName().equals("doSomethingSync")){
//                Type[] methodParamTypes = m.getGenericParameterTypes();
//                Type returnType = m.getGenericReturnType();
//                Type typeParam = Util.getTypeParameter(methodParamTypes[methodParamTypes.length - 1]);
//                LogUtil.e("ReturnType:"+returnType);
//                LogUtil.e("LastTypeParam:"+typeParam);
//                LogUtil.e("IsSame:"+returnType.equals(typeParam));
//            }
//        }

//        boolean isSub = Util.isSubType(methodParamTypes[0], ProgressListener.class);
//        boolean isSub2 = Util.isSubType(methodParamTypes[1], ProgressListener.class);
//        LogUtil.e("isSub:"+isSub+",isSub2:"+isSub2);
//        LogUtil.e("Method:"+methods[0]);

//
//        Type parameterType = new Clazz<Map<String,TestModel>>(){}.type();
//
//        boolean isMapStr = false;
//        if(parameterType instanceof ParameterizedType){
//            if(Util.isSubType(parameterType, Map.class)){
//                Type[] typeParams = ((ParameterizedType)parameterType).getActualTypeArguments();
//                if(typeParams.length>0&&typeParams[0]==String.class){
//                    isMapStr = true;
//                }
//            }
//        }
//        LogUtil.e("isMap:"+isMapStr);
//        Callback<TestModel> callback = new Callback<TestModel>() {
//            @Override
//            public void onSuccess(TestModel result, Map<String, List<String>> headers) {
//
//            }
//
//            @Override
//            public void onFailed(Request req, Exception e) {
//
//            }
//        };
//        LogUtil.e("Type:"+Util.type(Callback.class,callback));
//        LogUtil.e("Type:"+Clazz.of(callback).type());
//        TestModel[] arrray = new TestModel[10];
//        Object[] testObj = arrray;
//        ApiService service = lite.retrofit(ApiService.class);

//        LogUtil.e("IsSub:"+Util.isSubType(ExMergeCallback.class, Callback.class));
    }

    public static void testSampleApi(HttpLite mHttplite){
        //生成API接口实例
        final SampleApi api = mHttplite.retrofit(SampleApi.class);
        //调用异步方法
        api.login("user", "pass", "token", new Callback<Result<UserInfo>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, Result<UserInfo> result) {
                //TODO
                LogUtil.e("Result:"+result);
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
                LogUtil.e("onFailed",e);
            }
        });
        //调用异步方法
        new Thread(){
            @Override
            public void run() {
                //获取知乎主页数据
                try {
                    ZhihuData data = api.syncZhihu();
                    //TODO
                    LogUtil.e("Result:"+data);
                } catch (Exception e) {
                    //TODO
                    LogUtil.e("onFailed",e);
                }
            }
        }.start();
        //生成Call
        final Call call = api.zhihuCall();
        //异步调用Call
        call.async(new Callback<ZhihuData>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, ZhihuData result) {
                //TODO
                LogUtil.e("Result:"+result);
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
                LogUtil.e("onFailed",e);
            }
        });
        //或者同步调用Call
        new Thread(){
            @Override
            public void run() {
                //获取知乎主页数据
                try {
                    ZhihuData data = call.sync(new Clazz<ZhihuData>(){});
                    //TODO
                    LogUtil.e("Result:"+data);
                } catch (Exception e) {
                    //TODO
                    LogUtil.e("onFailed",e);
                }
            }
        }.start();
    }

    public static void testCustom(HttpLite mHttplite){
        //添加自定义注解处理器
        //普通的参数注解处理ParamterProcessor
        Retrofit.registerParamterProcessor(new QueryProcessor());
        //对个参数组合到一起的参数注解处理ParamMiscProcessor，如将多个参数组合成一个json字符串作为请求的BODY
        Retrofit.registerParamMiscProcessor(new GsonFieldProcesscor());
        //当注解处理的参数是用作Body时，还需要注册Body类型
        Retrofit.registerBodyAnnotation(GsonField.class,GsonFieldProcesscor.BODY_TYPE,true);
        //创建实例
        CustomApi api = mHttplite.retrofit(CustomApi.class);
        //发起请求
        Object tag = new Object();
        api.login("user", "pass", "token", tag, new Callback<Result<UserInfo>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, Result<UserInfo> result) {
                //TODO
                LogUtil.e("Result:"+result);
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
                LogUtil.e("onFailed",e);
            }
        });
        api.testPost("test1", "test2", new Callback<Result<RequestInfo>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, Result<RequestInfo> result) {
                //TODO
                LogUtil.e("Result:"+result);
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
                LogUtil.e("onFailed",e);
            }
        });
    }

    public static void testFilter(HttpLite mHttplite){
        RequestListener listener = new RequestListener() {

            @Override
            public void onRequestStart(Request request, Type resultType) {

            }

            @Override
            public void onRequestEnd(Request request, Type resultType, Response response) {

            }
        };
        MethodFilter filter = new MethodFilter() {
            @Override
            public Object onMethod(HttpLite lite, final MethodInvoker invoker, final Object[] args) throws Throwable {
                LogUtil.e("methodFilter:"+invoker);
//                String publicKey = ......
//                if(TextUtils.isEmpty(publicKey)){
//                    new Thread(){
//                        @Override
//                        public void run() {
//                            //获取key
//                            ......
//                            invoker.invoke(args);
//                        }
//                    }.start();
//                }else{
                    return invoker.invoke(args);
//                }
            }
        };
        SampleApi api = mHttplite.retrofit(SampleApi.class,listener,filter);

        api.login("user", "pass", "test", new Callback<Result<UserInfo>>() {
            @Override
            public void onSuccess(Request req, Map<String, List<String>> headers, Result<UserInfo> result) {
                LogUtil.e("Result:"+result);
            }

            @Override
            public void onFailed(Request req, Exception e) {
                LogUtil.e("onFailed",e);
            }
        });
    }
}
