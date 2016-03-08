package alexclin.httplite;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MultipartBuilder
 *
 * @author alexclin  16/1/1 17:18
 */
class MultipartBuilder {
    private String boundary;
    private MediaType type;
    private List<RequestBody> bodyList;
    private List<Pair<Map<String,List<String>>,RequestBody>> headBodyList;
    private List<Pair<String,String>> paramList;
    private List<Pair<String,Pair<String,RequestBody>>> fileList;

    MultipartBuilder() {
    }

    public MultipartBuilder boundary(String boundary){
        this.boundary = boundary;
        return this;
    }

    public MultipartBuilder setType(MediaType type){
        this.type = type;
        return this;
    }

    public MultipartBuilder add(RequestBody body){
        if(bodyList==null){
            bodyList = new ArrayList<>();
        }
        this.bodyList.add(body);
        return this;
    }

    public MultipartBuilder add(Map<String,List<String>> headers, RequestBody body){
        if(headBodyList==null){
            headBodyList = new ArrayList<>();
        }
        headBodyList.add(new Pair<Map<String, List<String>>, RequestBody>(headers,body));
        return this;
    }

    public MultipartBuilder add(String name, String value){
        if(paramList==null){
            paramList = new ArrayList<>();
        }
        paramList.add(new Pair<String, String>(name,value));
        return this;
    }

    public MultipartBuilder add(String name, String fileName, RequestBody body){
        if(fileList==null){
            fileList = new ArrayList<>();
        }
        fileList.add(new Pair<String, Pair<String, RequestBody>>(name,new Pair<String, RequestBody>(fileName,body)));
        return this;
    }

    public RequestBody build(LiteClient client){
        if(type==null){
            type = client.parse(MediaType.MULTIPART_FORM);
        }
        return client.createMultipartBody(boundary,type,bodyList,headBodyList,paramList,fileList);
    }
}
