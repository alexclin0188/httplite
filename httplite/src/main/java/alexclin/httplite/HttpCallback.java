package alexclin.httplite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;

import alexclin.httplite.exception.DecodeException;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.exception.ParserException;
import alexclin.httplite.util.IOUtil;
import alexclin.httplite.util.Util;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ResponseParser;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 15/12/31 21:27
 */
class HttpCallback<T> extends ResultCallback<T>{
    enum ParseType{
        RAW,Bitmap,String,Object
    }
    private Type type;
    private ParseType parseType;

    @SuppressWarnings("unchecked")
    HttpCallback(Callback<T> callback, HttpCall call,Type type) {
        super(callback,call);
        this.type = type;
        this.parseType = handleType(type);
    }

    @Override @SuppressWarnings("unchecked")
    protected void handleResponse(Response response) {
        try {
            if (isIgnoreStatus(type) || isSuccess(response)) {
                postSuccess(praseResponse(response), response.headers());
            } else {
                String message = response.message();
                if(TextUtils.isEmpty(message)){
                    try {
                        message = decodeResponseToString(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                throw new HttpException(response.code(),message);
            }
        } catch (Exception e) {
            postFailed(e);
        }
    }

    @Override
    protected Type resultType() {
        return type;
    }

    private boolean isSuccess(Response response) {
        int code = response.code();
        return code >= 200 && code < 300;
    }

    @Override @SuppressWarnings("unchecked")
    T praseResponse(Response response) throws Exception{
        int code = response.code();
        if(type==Boolean.class){
            return (T)Boolean.valueOf(code >= 200 && code < 300);
        }else if(type==Integer.class){
            return (T)Integer.valueOf(code);
        }else {
            switch (parseType){
                case RAW:
                    return (T)response;
                case Bitmap:
                    return (T)decodeBitmap(response);
                case String:
                    return (T)decodeString(response);
                case Object:
                    Collection<ResponseParser> prasers = getLite().getParsers();
                    if(prasers.isEmpty()){
                        throw new ParserException("No ResponseParser has set in HttpLise, failed with type:"+type);
                    }
                    for(ResponseParser parser: getLite().getParsers()){
                        if(parser.isSupported(type)){
                            try {
                                return parser.praseResponse(response,type);
                            } catch (Exception e) {
                                throw new ParserException(e);
                            }
                        }
                    }
                default:
                    throw new ParserException("There is no ResponseParser in HttpLite support type:"+type);
            }
        }
    }

    private ParseType handleType(Type type) {
        if(type==Response.class){
            return ParseType.RAW;
        }else if(type== Bitmap.class){
            return ParseType.Bitmap;
        }else if(type == String.class){
            return ParseType.String;
        }
        return ParseType.Object;
    }

    private String decodeString(Response response) throws Exception{
        try {
            return decodeResponseToString(response);
        } catch (Exception e) {
            throw new DecodeException("Decode String error",e);
        }
    }

    private Bitmap decodeBitmap(Response response) throws Exception{
        try {
            return BitmapFactory.decodeStream(response.body().stream());
        } catch (IOException e) {
            throw new DecodeException("Decode Bitmap error",e);
        }
    }

    static String decodeResponseToString(Response response) throws IOException{
        MediaType mt = response.body().contentType();
        if(mt!=null){
            Charset cs = mt.charset(Util.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().stream(),cs==null? Util.UTF_8:cs));
            StringBuilder stringBuilder = new StringBuilder();
            String s;
            while ((s=reader.readLine())!=null){
                stringBuilder.append(s);
            }
            IOUtil.closeQuietly(reader);
            return stringBuilder.toString();
        }
        throw new RuntimeException("Not text response body,no Content-Type in response");
    }

    static boolean isIgnoreStatus(Type type){
        return (type==Boolean.class)||(type==Integer.class);
    }
}
