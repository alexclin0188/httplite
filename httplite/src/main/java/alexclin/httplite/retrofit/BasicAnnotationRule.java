package alexclin.httplite.retrofit;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.Request;
import alexclin.httplite.annotation.Body;
import alexclin.httplite.annotation.Form;
import alexclin.httplite.annotation.Forms;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.HTTP;
import alexclin.httplite.annotation.IntoFile;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.Multipart;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Util;

/**
 * BasicAnnotationRule
 *
 * @author alexclin
 * @date 16/2/1 19:49
 */
public class BasicAnnotationRule implements AnnotationRule {

    @Override
    public void checkMethod(Method interfaceMethod,boolean isFileResult) throws RuntimeException {
        Type[] methodParameterTypes = interfaceMethod.getGenericParameterTypes();
        Annotation[][] methodParameterAnnotationArrays = interfaceMethod.getParameterAnnotations();
        alexclin.httplite.Method method = null;
        GET get = interfaceMethod.getAnnotation(GET.class);
        if(get!=null){
            method = alexclin.httplite.Method.GET;
        }
        if(method==null){
            POST post = interfaceMethod.getAnnotation(POST.class);
            if(post!=null) method = alexclin.httplite.Method.POST;
        }
        if(method==null){
            HTTP http = interfaceMethod.getAnnotation(HTTP.class);
            if(http!=null) method = http.method();
        }
        if (method==null) {
            String info = Util.printArray("You must set one http annotation on each method but there is:%s", interfaceMethod.getAnnotations());
            throw Util.methodError(interfaceMethod,info);
        }

        boolean allowBody = Request.permitsRequestBody(method);
        boolean requireBody = Request.requiresRequestBody(method);
        boolean hasBodyAnnotation = false;
        boolean hasFormAnnotation = false;
        boolean hasMultipartAnnotation = false;
        boolean hasIntoFile = false;
        boolean hasJsonField = false;
        for (Annotation[] annotations : methodParameterAnnotationArrays) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof Body) {
                    if (hasBodyAnnotation) {
                        throw Util.methodError(interfaceMethod, "You can use @Body annotation on method not more than once");
                    }else if (!allowBody) {
                        throw Util.methodError(interfaceMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasBodyAnnotation = true;
                } else if ((annotation instanceof Form) || (annotation instanceof Forms)) {
                    if (!allowBody) {
                        throw Util.methodError(interfaceMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasFormAnnotation = true;
                } else if ((annotation instanceof Multipart)) {
                    if (!allowBody) {
                        throw Util.methodError(interfaceMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasMultipartAnnotation = true;
                } else if (annotation instanceof IntoFile) {
                    if (!isFileResult)
                        throw Util.methodError(interfaceMethod,"Use @InfoFile must with last parameter (Callback<File>) or (Clazz<File> and return file)");
                    hasIntoFile = true;
                } else if (annotation instanceof JsonField){
                    if (!allowBody) {
                        throw Util.methodError(interfaceMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasJsonField = true;
                }
                if (hasFormAnnotation&&hasBodyAnnotation) {
                    throw Util.methodError(interfaceMethod,"You can not use @Body and @Form/Forms on on the same one method");
                } else if (hasMultipartAnnotation&&hasBodyAnnotation) {
                    throw Util.methodError(interfaceMethod,"You can not use @Body and @Multipart on on the same one method");
                } else if(hasJsonField&&hasBodyAnnotation){
                    throw Util.methodError(interfaceMethod,"You can not use @Body and @JsonField on on the same one method");
                } else if(hasFormAnnotation&&hasMultipartAnnotation){
                    throw Util.methodError(interfaceMethod,"You can not use @Form/Forms and @Multipart on on the same one method");
                } else if(hasFormAnnotation&&hasJsonField){
                    throw Util.methodError(interfaceMethod,"You can not use @JsonFile and @Multipart on on the same one method");
                } else if(hasMultipartAnnotation&&hasJsonField){
                    throw Util.methodError(interfaceMethod,"You can not use @Form/Forms and @JsonField on on the same one method");
                }
            }
        }

        if(isFileResult&&!hasIntoFile){
            throw Util.methodError(interfaceMethod,"Use last parameter (Callback<File>) or (Clazz<File> and return file), method must has @IntoFile parameter");
        }

        if(requireBody && !(hasBodyAnnotation||hasFormAnnotation||hasMultipartAnnotation||hasJsonField)){
            throw Util.methodError(interfaceMethod,"Http method:%s must has body parameter such as:@Form/Forms @Multipart @Body @JsonField",method);
        }
    }
}
