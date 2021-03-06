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

import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.exception.DecodeException;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.exception.ParserException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ResponseParser;
import alexclin.httplite.util.Util;

/**
 * HttpCallback
 *
 * @author alexclin at 15/12/31 21:27
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
                postSuccess(parseResponse(response), response.headers());
            } else {
                handleFailedCode(response);
            }
        }catch (Exception e) {
            postFailed(e);
        }
    }

    @Override
    protected Type resultType() {
        return type;
    }

    @Override @SuppressWarnings("unchecked")
    T parseResponse(Response response) throws Exception{
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
                    Collection<ResponseParser> parsers = getLite().getParsers();
                    if(parsers.isEmpty()){
                        throw new ParserException("No ResponseParser has set in HttpLite, failed with type:"+type);
                    }
                    for(ResponseParser parser: getLite().getParsers()){
                        if(isCanceled)
                            throw new CanceledException("Canceled during parse");
                        if(parser.isSupported(type)){
                            try {
                                return parser.parseResponse(response, type);
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
            return decodeToString(response);
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

    static boolean isIgnoreStatus(Type type){
        return (type==Boolean.class)||(type==Integer.class);
    }
}
