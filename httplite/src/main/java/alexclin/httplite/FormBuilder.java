package alexclin.httplite;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 17:36
 */
class FormBuilder {
    private List<Pair<String,String>> paramList;
    private List<Pair<String,String>> encodedParamList;

    FormBuilder() {
    }

    public FormBuilder add(String name, String value){
        if(paramList==null){
            paramList = new ArrayList<>();
        }
        paramList.add(new Pair<String, String>(name,value));
        return this;
    }

    public FormBuilder addEncoded(String name,String value){
        if(encodedParamList==null){
            encodedParamList = new ArrayList<>();
        }
        encodedParamList.add(new Pair<String, String>(name,value));
        return this;
    }

    public RequestBody build(LiteClient client){
        return client.createFormBody(paramList,encodedParamList);
    }
}
