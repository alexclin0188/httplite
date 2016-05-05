package alexclin.httplite.sample.retrofit;

import com.example.FileInfo;
import com.example.Result;
import com.example.UserInfo;

import java.util.List;
import java.util.Map;

import alexclin.httplite.Call;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.sample.model.ZhihuData;
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
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
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
                } catch (Exception e) {
                    //TODO
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
            }

            @Override
            public void onFailed(Request req, Exception e) {
                //TODO
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
                } catch (Exception e) {
                    //TODO
                }
            }
        }.start();
    }
}
