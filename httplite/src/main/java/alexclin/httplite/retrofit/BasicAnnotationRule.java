package alexclin.httplite.retrofit;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.Request;
import alexclin.httplite.annotation.Body;
import alexclin.httplite.annotation.Form;
import alexclin.httplite.annotation.Forms;
import alexclin.httplite.annotation.IntoFile;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.Multipart;
import alexclin.httplite.util.Util;

/**
 * BasicAnnotationRule
 *
 * @author alexclin
 * @date 16/2/1 19:49
 */
public class BasicAnnotationRule implements AnnotationRule {

    @Override
    public void checkMethod(Method checkMethod) throws RuntimeException {
        Type[] methodParameterTypes = checkMethod.getGenericParameterTypes();
        Annotation[][] methodParameterAnnotationArrays = checkMethod.getParameterAnnotations();
        Annotation[] methodAnnotations = checkMethod.getAnnotations();
        boolean hasHttpAnnotation = false;
        alexclin.httplite.Method method = null;
        for (Annotation annotation : methodAnnotations) {
            boolean isBasicHttp = ProcessorFactory.isBasicHttpAnnoation(annotation);
            method = ProcessorFactory.getHttpAnnoationMethod(annotation);
            if (!isBasicHttp) continue;
            if (hasHttpAnnotation) {
                String info = Util.printArray("You can use only one http annotation on each method but threre is:%s", methodAnnotations);
                throw Util.methodError(checkMethod,info);
            } else {
                hasHttpAnnotation = true;
            }
        }
        if (!hasHttpAnnotation) {
            String info = Util.printArray("You must set one http annotation on each method but threre is:%s", methodAnnotations);
            throw Util.methodError(checkMethod,info);
        }

        boolean isFileReturnOrCallback = Util.getTypeParameter(methodParameterTypes[methodParameterTypes.length-1])== File.class;
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
                        throw Util.methodError(checkMethod, "You can use @Body annoation on method not more than once");
                    }else if (!allowBody) {
                        throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasBodyAnnotation = true;
                } else if ((annotation instanceof Form) || (annotation instanceof Forms)) {
                    if (!allowBody) {
                        throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasFormAnnotation = true;
                } else if ((annotation instanceof Multipart)) {
                    if (!allowBody) {
                        throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasMultipartAnnotation = true;
                } else if (annotation instanceof IntoFile) {
                    if (!isFileReturnOrCallback)
                        throw Util.methodError(checkMethod,"Use @InfoFile must with last parameter (Callback<File>) or (Clazz<File> and return file)");
                    hasIntoFile = true;
                } else if (annotation instanceof JsonField){
                    if (!allowBody) {
                        throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                    }
                    hasJsonField = true;
                }
                if (hasFormAnnotation&&hasBodyAnnotation) {
                    throw Util.methodError(checkMethod,"You can not use @Body and @Form/Forms on on the same one method");
                } else if (hasMultipartAnnotation&&hasBodyAnnotation) {
                    throw Util.methodError(checkMethod,"You can not use @Body and @Multipart on on the same one method");
                } else if(hasJsonField&&hasBodyAnnotation){
                    throw Util.methodError(checkMethod,"You can not use @Body and @JsonField on on the same one method");
                } else if(hasFormAnnotation&&hasMultipartAnnotation){
                    throw Util.methodError(checkMethod,"You can not use @Form/Forms and @Multipart on on the same one method");
                } else if(hasFormAnnotation&&hasJsonField){
                    throw Util.methodError(checkMethod,"You can not use @JsonFile and @Multipart on on the same one method");
                } else if(hasMultipartAnnotation&&hasJsonField){
                    throw Util.methodError(checkMethod,"You can not use @Form/Forms and @JsonField on on the same one method");
                }
            }
        }

        if(isFileReturnOrCallback&&!hasIntoFile){
            throw Util.methodError(checkMethod,"Use last parameter (Callback<File>) or (Clazz<File> and return file), method must has @IntoFile paramter");
        }

        if(requireBody && !(hasBodyAnnotation||hasFormAnnotation||hasMultipartAnnotation||hasJsonField)){
            throw Util.methodError(checkMethod,"Http method:%s must has body parameter such as:@Form/Forms @Multipart @Body @JsonField",method);
        }
    }
}
