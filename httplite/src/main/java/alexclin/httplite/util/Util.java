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
import java.net.IDN;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

import alexclin.httplite.HttpLite;
import alexclin.httplite.MediaType;
import alexclin.httplite.listener.Callback;

/**
 * Junk drawer of utility methods.
 */
public final class Util {
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * A cheap and type-safe constant for the UTF-8 Charset.
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * GMT and UTC are equivalent for our purposes.
     */
    public static final TimeZone UTC = TimeZone.getTimeZone("GMT");

    /**
     * Quick and dirty pattern to differentiate IP addresses from hostnames. This is an approximation
     * of Android's private InetAddress#isNumeric API.
     * <p>
     * <p>This matches IPv6 addresses as a hex string containing at least one colon, and possibly
     * including dots after the first colon. It matches IPv4 addresses as strings containing only
     * decimal digits and dots. This pattern matches strings like "a:.23" and "54" that are neither IP
     * addresses nor hostnames; they will be verified as IP addresses (which is a more strict
     * verification).
     */
    private static final Pattern VERIFY_AS_IP_ADDRESS = Pattern.compile(
            "([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");

    private Util() {
    }

    public static void checkOffsetAndCount(long arrayLength, long offset, long count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Returns true if two possibly-null objects are equal.
     */
    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Returns an immutable copy of {@code list}.
     */
    public static <T> List<T> immutableList(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /**
     * Returns an immutable list containing {@code elements}.
     */
    public static <T> List<T> immutableList(T... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements.clone()));
    }

    /**
     * Returns an immutable copy of {@code map}.
     */
    public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
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

    /**
     * Returns an array containing containing only elements found in {@code first}  and also in {@code
     * second}. The returned elements are in the same order as in {@code first}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] intersect(Class<T> arrayType, T[] first, T[] second) {
        List<T> result = intersect(first, second);
        return result.toArray((T[]) Array.newInstance(arrayType, result.size()));
    }

    /**
     * Returns a list containing containing only elements found in {@code first}  and also in {@code
     * second}. The returned elements are in the same order as in {@code first}.
     */
    private static <T> List<T> intersect(T[] first, T[] second) {
        List<T> result = new ArrayList<>();
        for (T a : first) {
            for (T b : second) {
                if (a.equals(b)) {
                    result.add(b);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Returns true if {@code e} is due to a firmware bug fixed after Android 4.2.2.
     * https://code.google.com/p/android/issues/detail?id=54072
     */
    public static boolean isAndroidGetsocknameError(AssertionError e) {
        return e.getCause() != null && e.getMessage() != null
                && e.getMessage().contains("getsockname failed");
    }

    public static boolean contains(String[] array, String value) {
        return Arrays.asList(array).contains(value);
    }

    public static String[] concat(String[] array, String value) {
        String[] result = new String[array.length + 1];
        System.arraycopy(array, 0, result, 0, array.length);
        result[result.length - 1] = value;
        return result;
    }

    /**
     * Increments {@code pos} until {@code input[pos]} is not ASCII whitespace. Stops at {@code
     * limit}.
     */
    public static int skipLeadingAsciiWhitespace(String input, int pos, int limit) {
        for (int i = pos; i < limit; i++) {
            switch (input.charAt(i)) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case ' ':
                    continue;
                default:
                    return i;
            }
        }
        return limit;
    }

    /**
     * Decrements {@code limit} until {@code input[limit - 1]} is not ASCII whitespace. Stops at
     * {@code pos}.
     */
    public static int skipTrailingAsciiWhitespace(String input, int pos, int limit) {
        for (int i = limit - 1; i >= pos; i--) {
            switch (input.charAt(i)) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case ' ':
                    continue;
                default:
                    return i + 1;
            }
        }
        return pos;
    }

    /**
     * Equivalent to {@code string.substring(pos, limit).trim()}.
     */
    public static String trimSubstring(String string, int pos, int limit) {
        int start = skipLeadingAsciiWhitespace(string, pos, limit);
        int end = skipTrailingAsciiWhitespace(string, start, limit);
        return string.substring(start, end);
    }

    /**
     * Returns the index of the first character in {@code input} that contains a character in {@code
     * delimiters}. Returns limit if there is no such character.
     */
    public static int delimiterOffset(String input, int pos, int limit, String delimiters) {
        for (int i = pos; i < limit; i++) {
            if (delimiters.indexOf(input.charAt(i)) != -1) return i;
        }
        return limit;
    }

    /**
     * Returns the index of the first character in {@code input} that is {@code delimiter}. Returns
     * limit if there is no such character.
     */
    public static int delimiterOffset(String input, int pos, int limit, char delimiter) {
        for (int i = pos; i < limit; i++) {
            if (input.charAt(i) == delimiter) return i;
        }
        return limit;
    }

    /**
     * Performs IDN ToASCII encoding and canonicalize the result to lowercase. e.g. This converts
     * {@code ☃.net} to {@code xn--n3h.net}, and {@code WwW.GoOgLe.cOm} to {@code www.google.com}.
     * {@code null} will be returned if the input cannot be ToASCII encoded or if the result
     * contains unsupported ASCII characters.
     */
    public static String domainToAscii(String input) {
        try {
            String result = IDN.toASCII(input).toLowerCase(Locale.US);
            if (result.isEmpty()) return null;

            // Confirm that the IDN ToASCII result doesn't contain any illegal characters.
            if (containsInvalidHostnameAsciiCodes(result)) {
                return null;
            }
            // TODO: implement all label limits.
            return result;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean containsInvalidHostnameAsciiCodes(String hostnameAscii) {
        for (int i = 0; i < hostnameAscii.length(); i++) {
            char c = hostnameAscii.charAt(i);
            // The WHATWG Host parsing rules accepts some character codes which are invalid by
            // definition for OkHttp's host header checks (and the WHATWG Host syntax definition). Here
            // we rule out characters that would cause problems in host headers.
            if (c <= '\u001f' || c >= '\u007f') {
                return true;
            }
            // Check for the characters mentioned in the WHATWG Host parsing spec:
            // U+0000, U+0009, U+000A, U+000D, U+0020, "#", "%", "/", ":", "?", "@", "[", "\", and "]"
            // (excluding the characters covered above).
            if (" #%/:?@[\\]".indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code host} is not a host name and might be an IP address.
     */
    public static boolean verifyAsIpAddress(String host) {
        return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
    }

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
    }

    public static boolean isHttpPrefix(String url){
        if(TextUtils.isEmpty(url)){
            return false;
        }
        return url.startsWith("http://")||url.startsWith("https://");
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
        return String.format(foramt,sb.toString());
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
                + method.getName()+"()");
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
     * 方式三
     *
     * @param bytes
     * @return
     */
    public static String bytes2hex(byte[] bytes)
    {
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
}
