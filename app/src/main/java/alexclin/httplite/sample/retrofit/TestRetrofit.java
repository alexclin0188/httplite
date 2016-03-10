package alexclin.httplite.sample.retrofit;

import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * TestRetrofit
 *
 * @author alexclin
 * @date 16/1/30 19:08
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

        LogUtil.e("IsSub:"+Util.isSubType(ExMergeCallback.class, Callback.class));
    }
}
