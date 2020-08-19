package alexclin.httplite.retrofit;

import android.text.TextUtils;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import alexclin.httplite.listener.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.annotation.Body;
import alexclin.httplite.annotation.Form;
import alexclin.httplite.annotation.Forms;
import alexclin.httplite.annotation.Header;
import alexclin.httplite.annotation.Headers;
import alexclin.httplite.annotation.IntoFile;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.Multipart;
import alexclin.httplite.annotation.Param;
import alexclin.httplite.annotation.Params;
import alexclin.httplite.annotation.Path;
import alexclin.httplite.annotation.Paths;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * BasicProcessors
 *
 * @author alexclin 16/1/30 10:06
 */
class BasicProcessors {

    static class FormProcessor extends ProcessorFactory.ObjectsProcessor{

        @Override
        protected void performProcess(Annotation annotation, Request.Builder request, Object value) {
            request.form(((Form)annotation).value(),value.toString(),((Form)annotation).encoded());
        }

        @Override
        protected String value(Annotation annotation) {
            return ((Form)annotation).value();
        }

        @Override
        public boolean support(Annotation annotation) {
            return (annotation instanceof Form);
        }
    }

    static class FormsProcessor extends ProcessorFactory.MapProcessor{

        @Override
        void performProcess(Annotation annotation, Request.Builder request, String key, Object value) {
            request.form(key, value.toString(), ((Forms) annotation).encoded());
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Forms;
        }
    }

    static class HeaderProcessor extends ProcessorFactory.ObjectsProcessor{

        @Override
        protected void performProcess(Annotation annotation, Request.Builder request, Object value) {
            request.header(((Header)annotation).value(),value.toString());
        }

        @Override
        protected String value(Annotation annotation) {
            return ((Header)annotation).value();
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Header;
        }
    }

    static class HeadersProcessor extends ProcessorFactory.MapProcessor{

        @Override
        void performProcess(Annotation annotation, Request.Builder request, String key, Object value) {
            request.header(key,value.toString());
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Headers;
        }
    }

    static class ParamProcessor extends ProcessorFactory.ObjectsProcessor{

        @Override
        protected void performProcess(Annotation annotation, Request.Builder request, Object value) {
            request.param(((Param)annotation).value(),value.toString(),((Param)annotation).encoded());
        }

        @Override
        protected String value(Annotation annotation) {
            return ((Param)annotation).value();
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Param;
        }
    }

    static class ParamsProcessor extends ProcessorFactory.MapProcessor{

        @Override
        void performProcess(Annotation annotation, Request.Builder request, String key, Object value) {
            request.param(key,value.toString(),((Params)annotation).encoded());
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Params;
        }
    }

    static class PathProcessor implements ParameterProcessor{

        @Override
        public void process(Annotation annotation, Request.Builder request, Object value) {
            if(value==null) return;
            request.pathHolder(((Path)annotation).value(),value.toString(),((Path)annotation).encoded());
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Path;
        }

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            if(parameterType!=String.class){
                throw Util.methodError(method,"Parameter with @Path must be type String");
            }else if(parameterType==String.class&&(TextUtils.isEmpty(((Path) annotation).value()))){
                throw Util.methodError(method,"The annotation {@Path(value) value} must not be null when @Path use with type String");
            }
        }
    }

    static class PathsProcessor extends ProcessorFactory.MapProcessor{

        @Override
        void performProcess(Annotation annotation, Request.Builder request, String key, Object value) {
            request.pathHolder(key,value.toString(),((Paths)annotation).encoded());
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Paths;
        }
    }

    static class MultipartProcessor implements ParameterProcessor{

        @Override
        public void process(Annotation annotation, Request.Builder request, Object value) {
            if(value==null) return;
            if(value instanceof String){
                request.multipart(((Multipart) annotation).value(),value.toString());
            }else if(value instanceof MultiPart){
                ((MultiPart) value).addTo(request);
            }
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Multipart;
        }

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            //String/MultiPart
            if(parameterType!=String.class && parameterType!= MultiPart.class){
                throw Util.methodError(method,"Annotation @Multipart only support parameter type String/MultiPart");
            }else if(parameterType==String.class&&TextUtils.isEmpty(((Multipart)annotation).value())){
                throw Util.methodError(method,"The annotation {@Multipart(value) value} must not be null when @Multipart use with type String");
            }
        }
    }

    static class IntoFileProcessor implements ParameterProcessor{

        @Override
        public void process(Annotation annotation, Request.Builder request, Object value) {
            if(value==null) return;
            request.intoFile((String)value,((IntoFile) annotation).resume(),((IntoFile) annotation).rename());
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof IntoFile;
        }

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            //必须为String
            if(parameterType!=String.class){
                throw Util.methodError(method,"the parameter with @IntoFile must be type String");
            }
        }
    }

    static class BodyProcessor implements ParameterProcessor{
        @Override
        public void process(Annotation annotation, Request.Builder request, Object value) {
            if(value==null) return;
            String mediaType = ((Body) annotation).value();
            if(value instanceof String){
                if(TextUtils.isEmpty(mediaType)) mediaType = MediaType.APPLICATION_JSON;
                request.body(mediaType,(String)value);
            }else if(value instanceof File){
                request.body(mediaType,(File)value);
            }else if(value instanceof RequestBody){
                request.body((RequestBody)value);
            }
        }

        @Override
        public boolean support(Annotation annotation) {
            return annotation instanceof Body;
        }

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            //String/File/RequestBody
            if(parameterType!=String.class && parameterType!= File.class && parameterType!= RequestBody.class){
                throw Util.methodError(method,"Annoation @Body only support parameter type String/File/RequestBody");
            }
        }
    }
}
