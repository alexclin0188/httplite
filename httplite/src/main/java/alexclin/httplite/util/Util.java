/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alexclin.httplite.util;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.MediaType;

/**
 * Junk drawer of utility methods.
 */
public final class Util {

    /**
     * A cheap and type-safe constant for the UTF-8 Charset.
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private Util() {
    }

    public static void checkOffsetAndCount(long arrayLength, long offset, long count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static <T> List<T> immutableList(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }

    public static boolean isHttpPrefix(String url){
        return !TextUtils.isEmpty(url)&&(url.startsWith("http://")||url.startsWith("https://"));
    }

    public static MediaType guessMediaType(HttpLite lite,File file){
        if(file==null||lite==null){
            return null;
        }
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentType = fileNameMap.getContentTypeFor(file.getAbsolutePath());
        if(contentType==null){
            contentType = MediaType.APPLICATION_STREAM;
        }
        MediaType type = lite.parse(contentType);
        if(type==null){
            type = lite.parse(MediaType.APPLICATION_STREAM);
        }
        return type;
    }

    //获取回调接口的结果类型T
    public static <T> Type type(Class<T> clazz, Object object, int index){
        Type[] types = object.getClass().getGenericInterfaces();
        for(Type type:types){
            if(!(type instanceof ParameterizedType)){
                continue;
            }
            ParameterizedType ptype = (ParameterizedType)type;
            if(ptype.getRawType()!=clazz){
                continue;
            }
            Type[] typeArgs = ptype.getActualTypeArguments();
            if(typeArgs.length>index){
                return typeArgs[index];
            }
        }
        return null;
    }

    //获取回调接口(类似Callback<T>,Clazz<T>)的结果类型T
    public static <T> Type type(Class<T> clazz, Object object){
        return type(clazz,object,0);
    }

    public static boolean isSubType(Type subType,Class superClazz){
        if(subType==superClazz) return true;
        if(subType instanceof Class){
            Type[] interfaces = ((Class)subType).getGenericInterfaces();
            for(Type type:interfaces){
                if (type instanceof ParameterizedType)
                    type = ((ParameterizedType) type).getRawType();
                if(type==superClazz) return true;
            }
            Type superC = ((Class)subType).getSuperclass();
            if(superC==superClazz) return true;
            if(superC!=Object.class) return isSubType(superC,superClazz);
        }else if(subType instanceof TypeVariable){
            TypeVariable t = (TypeVariable)subType;
            Type[] types = t.getBounds();
            for(Type type:types){
                if(type==superClazz) return true;
            }
        }else if(subType instanceof ParameterizedType){
            Type clazz = ((ParameterizedType) subType).getRawType();
            return isSubType(clazz,superClazz);
        }
        return false;
    }

    public static String appendString(String baseUrl,String url){
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0,baseUrl.length()-1);
        }
        if(url.startsWith("/")) {
            baseUrl = baseUrl+url;
        }else {
            baseUrl = baseUrl+"/"+url;
        }
        return baseUrl;
    }

    public static <T> void validateServiceInterface(Class<T> service) {
        if (!service.isInterface()) {
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }
        // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
        // Android (http://b.android.com/58753) but it forces composition of API declarations which is
        // the recommended pattern.
        if (service.getInterfaces().length > 0) {
            throw new IllegalArgumentException("API interfaces must not extend other interfaces.");
        }
    }

    public static String printArray(String foramt,Object[] array){
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for(Object object:array){
            if(isFirst){
                sb.append(object.toString());
                isFirst = false;
            }else{
                sb.append(object.toString()).append(",");
            }
        }
        return String.format(foramt, sb.toString());
    }

    public static boolean hasUnresolvableType(Type type) {
        if (type instanceof Class<?>) {
            return false;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                if (hasUnresolvableType(typeArgument)) {
                    return true;
                }
            }
            return false;
        }
        if (type instanceof GenericArrayType) {
            return hasUnresolvableType(((GenericArrayType) type).getGenericComponentType());
        }
        if (type instanceof TypeVariable) {
            return true;
        }
        if (type instanceof WildcardType) {
            return true;
        }
        String className = type == null ? "null" : type.getClass().getName();
        throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                + "GenericArrayType, but <" + type + "> is of type " + className);
    }

    // This method is copyright 2008 Google Inc. and is taken from Gson under the Apache 2.0 license.
    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // Type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
            // suspects some pathological case related to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) throw new IllegalArgumentException();
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
            // type that's more general than necessary is okay.
            return Object.class;

        } else if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);

        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                    + "GenericArrayType, but <" + type + "> is of type " + className);
        }
    }

    public static RuntimeException methodError(Method method, String message, Object... args) {
        return methodError(null, method, message, args);
    }

    public static RuntimeException methodError(Throwable cause, Method method, String message,
                                        Object... args) {
        message = String.format(message, args);
        IllegalArgumentException e = new IllegalArgumentException(message
                + "\n    for method ->"
                + method.getDeclaringClass().getSimpleName()
                + "."
                + method.getName()+"("+method.getGenericParameterTypes().length+" param)");
        e.initCause(cause);
        return e;
    }

    public static Type getTypeParameter(Type type,int index){
        if(type instanceof ParameterizedType){
            Type[] types = ((ParameterizedType)type).getActualTypeArguments();
            if(types.length>index) return types[index];
        }
        return null;
    }

    public static Type getTypeParameter(Type type){
        return getTypeParameter(type,0);
    }

    public static String md5Hex(String s) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5bytes = messageDigest.digest(s.getBytes("UTF-8"));
            return bytes2hex(md5bytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            LogUtil.e("md5 failed",e);
            return null;
        }
    }

    /**
     * 字节数组转HEX字符
     *
     * @param bytes 字节数组
     * @return HEX字符串
     */
    public static String bytes2hex(byte[] bytes){
        final String HEX = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes){
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));
        }
        return sb.toString();
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public static byte[] readBytes(InputStream in, long skip, long size) throws IOException {
        ByteArrayOutputStream out = null;
        try {
            if (skip > 0) {
                long skipSize;
                while (skip > 0 && (skipSize = in.skip(skip)) > 0) {
                    skip -= skipSize;
                }
            }
            out = new ByteArrayOutputStream();
            for (int i = 0; i < size; i++) {
                out.write(in.read());
            }
        } finally {
            closeQuietly(out);
        }
        return out.toByteArray();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        if (!(out instanceof BufferedOutputStream)) {
            out = new BufferedOutputStream(out);
        }
        int len;
        byte[] buffer = new byte[1024];
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        out.flush();
    }

    public static boolean deleteFileOrDir(File path) {
        if (path == null || !path.exists()) {
            return true;
        }
        if (path.isFile()) {
            return path.delete();
        }
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteFileOrDir(file);
            }
        }
        return path.delete();
    }
}
