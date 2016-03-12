package alexclin.httplite.retrofit;


import android.util.Pair;

import java.io.File;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;

/**
 * MultiPart
 *
 * @author alexclin 16/1/20 20:59
 */
public class MultiPart {
    private RequestBody requestBody;
    private Pair<Map<String,List<String>>,RequestBody> headersBody;
    private Pair<Pair<String,String>,RequestBody> nameFileBody;
    private Pair<Pair<String,String>,File> nameFile;
    private Pair<Pair<String,String>,Pair<File,String>> nameFileType;

    public MultiPart(RequestBody body){
        this.requestBody = body;
    }

    public MultiPart(Map<String,List<String>> headers,RequestBody body){
        this.headersBody = new Pair<>(headers,body);
    }

    public MultiPart(String name, String fileName, RequestBody body){
        this.nameFileBody = new Pair<>(new Pair<>(name,fileName),body);
    }

    public MultiPart(String name, String fileName, File file){
        this.nameFile = new Pair<>(new Pair<>(name,fileName),file);
    }

    public MultiPart(String name, String fileName, File file,String mediaType){
        this.nameFileType = new Pair<>(new Pair<>(name,fileName),new Pair<>(file,mediaType));
    }

    void addTo(Request request){
        if(requestBody!=null){
            request.multipart(requestBody);
        }else if(headersBody!=null){
            request.multipart(headersBody.first,headersBody.second);
        }else if(nameFileBody!=null){
            request.multipart(nameFileBody.first.first,nameFileBody.first.second,nameFileBody.second);
        }else if(nameFile!=null){
            request.multipart(nameFile.first.first,nameFile.first.second,nameFile.second);
        }else if(nameFileType!=null){
            request.multipart(nameFileType.first.first,nameFileType.first.second,nameFileType.second.first,nameFileType.second.second);
        }
    }
}
