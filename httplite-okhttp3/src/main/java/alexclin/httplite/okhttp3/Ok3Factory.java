package alexclin.httplite.okhttp3;

import android.util.Pair;

import java.io.File;
import java.util.List;
import java.util.Map;

import alexclin.httplite.ILite;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * @author xiehonglin429 on 2017/3/2.
 */

public class Ok3Factory implements ILite.RequestBodyFactory<RequestBody> {
    @Override
    public RequestBody createRequestBody(alexclin.httplite.RequestBody requestBody, String mediaType) {
        return OkRequestBody.wrapperLite(requestBody,mediaType);
    }

    @Override
    public RequestBody createRequestBody(File file, String mediaType) {
        return RequestBody.create(MediaType.parse(mediaType),file);
    }

    @Override
    public RequestBody createRequestBody(String content, String mediaType) {
        return RequestBody.create(MediaType.parse(mediaType),content);
    }

    @Override
    public RequestBody createRequestBody(byte[] content, String mediaType, int offset, int byteCount) {
        return RequestBody.create(MediaType.parse(mediaType),content,offset,byteCount);
    }

    @Override
    public RequestBody createRequestBody(List<Pair<String, String>> paramList, List<Pair<String, String>> encodedParamList) {
        FormBody.Builder builder = new FormBody.Builder();
        if(paramList!=null){
            for(Pair<String,String> param:paramList){
                builder.add(param.first,param.second);
            }
        }
        if(encodedParamList!=null){
            for(Pair<String,String> param:encodedParamList){
                builder.addEncoded(param.first,param.second);
            }
        }
        return builder.build();
    }

    @Override
    public RequestBody createRequestBody(String boundary, String type, List<alexclin.httplite.RequestBody> bodyList, List<Pair<Map<String, List<String>>, alexclin.httplite.RequestBody>> headBodyList, List<Pair<String, String>> paramList, List<Pair<String, Pair<String, alexclin.httplite.RequestBody>>> fileList) {
        MultipartBody.Builder builder;
        if(boundary==null){
            builder = new MultipartBody.Builder().setType(MediaType.parse(type));
        }else {
            builder = new MultipartBody.Builder(boundary).setType(MediaType.parse(type));
        }
        if(bodyList!=null){
            for(alexclin.httplite.RequestBody body:bodyList){
                builder.addPart(OkRequestBody.wrapperLite(body));
            }
        }
        if(headBodyList!=null){
            for(Pair<Map<String,List<String>>, alexclin.httplite.RequestBody> bodyPair:headBodyList){
                builder.addPart(createHeader(bodyPair.first), OkRequestBody.wrapperLite(bodyPair.second));
            }
        }
        if(paramList!=null){
            for(Pair<String,String> pair:paramList){
                builder.addFormDataPart(pair.first, pair.second);
            }
        }
        if(fileList!=null){
            for(Pair<String,Pair<String, alexclin.httplite.RequestBody>> pair:fileList){
                alexclin.httplite.RequestBody liteBody = pair.second.second;
                RequestBody body;
                if(liteBody instanceof alexclin.httplite.RequestBody.NotBody){
                    body = ((alexclin.httplite.RequestBody.NotBody) liteBody).createReal(this);
                }else{
                    body = OkRequestBody.wrapperLite(liteBody);
                }
                builder.addFormDataPart(pair.first, pair.second.first, body);
            }
        }
        return builder.build();
    }

    private static Headers createHeader(Map<String, List<String>> headers){
        if(headers!=null&&!headers.isEmpty()){
            Headers.Builder hb = new Headers.Builder();
            for(String key:headers.keySet()){
                List<String> values = headers.get(key);
                for(String value:values){
                    hb.add(key,value);
                }
            }
            return hb.build();
        }
        return null;
    }
}
