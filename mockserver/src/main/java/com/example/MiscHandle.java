package com.example;

import com.alibaba.fastjson.JSON;
import com.example.utils.HttpUtil;
import com.example.utils.RecordedUpload;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.util.Streams;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


/**
 * MiscHandle
 *
 * @author alexclin
 * @date 16/1/31 11:42
 */
public class MiscHandle implements MethodHandle{
    @Override
    public MockResponse handle(RecordedRequest request, String root) {
        String path = request.getPath();
        RequestInfo requestInfo = new RequestInfo();
        int index = path.indexOf("?");
        if (index > -1) {
            String queryString = path.substring(index + 1);
            path = path.substring(0, index);
            requestInfo.params = HttpUtil.getParamsMap(queryString, Util.UTF_8.name());
        }
        if(path.startsWith("/download/")){
            MockResponse mr = handleDownload(request, root);
            mr.addHeader("RequestHeaders",request.getHeaders().toString());
            mr.addHeader("RequestParams",requestInfo.params);
            return mr;
        }
        requestInfo.method = request.getMethod();
        requestInfo.path = path;
        requestInfo.headers = request.getHeaders().toMultimap();
        String methodUp = requestInfo.method.toUpperCase();
        if(methodUp.equals("POST")||methodUp.equals("PUT")){
            requestInfo.bodyInfo = createBodyInfo(request);
        }
        Result<RequestInfo> result = new Result<>();
        result.requestMethod = requestInfo.method;
        result.requestPath = requestInfo.path;
        result.data = requestInfo;
        return new MockResponse().setStatus("HTTP/1.1 200").addHeader("content-type: application/json; charset=utf-8")
                .setBody(JSON.toJSONString(result));
    }

    private MockResponse handleDownload(RecordedRequest request, String root) {
        File rootDir = new File(root);
        File[] files = rootDir.listFiles();
        if(files!=null)
        try {
            for (File file:files){
                if(file.isFile()&&file.length()>500000){
                    return GetHandle.fileToResponse(file.getAbsolutePath(),file);
                }
            }
        } catch (Exception e) {
            return new MockResponse()
                    .setStatus("HTTP/1.1 500")
                    .addHeader("content-type: text/plain; charset=utf-8")
                    .setBody("SERVER ERROR: " + e);
        }
        return new MockResponse()
                .setStatus("HTTP/1.1 404")
                .addHeader("content-type: text/plain; charset=utf-8")
                .setBody("NOT FOUND: " + request.getPath());
    }

    private String createBodyInfo(RecordedRequest request) {
        if(HttpUtil.getMimeType(request).equals("application/json")){
            Charset charset = HttpUtil.getChartset(request);
            String json = request.getBody().readString(charset);
            System.out.println("createBodyInfo:"+json);
            return String.format("JsonBody charSet:%s,body:%s",charset.displayName(),json);
        }else if(HttpUtil.getMimeType(request).equals("application/x-www-form-urlencoded")){
            System.out.println("FormBody");
            String s;
            StringBuilder sb = new StringBuilder();
            try {
                while ((s = request.getBody().readUtf8Line())!=null){
                    sb.append(URLDecoder.decode(s, Util.UTF_8.name()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("createBodyInfo:"+sb.toString());
            return "FormBody:"+sb.toString();
        }else if(RecordedUpload.isMultipartContent(request)){
            return handleMultipart(request);
        }
        return HttpUtil.getMimeType(request);
    }

    private String handleMultipart(RecordedRequest request) {
        RecordedUpload upload = new RecordedUpload(request);
        Exception exception;
        try {
            Map<String,String> params = new HashMap<>();
            FileItemIterator iter = upload.getItemIterator();
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();
                if (item.isFormField()) {
                    String value = Streams.asString(stream);
                    System.out.println("Form field " + name + " with value "
                            + value + " detected.");
                    params.put(name,value);
                } else {
                    System.out.println("File field " + name + " with file name "
                            + item.getName() + " detected.");
                    params.put(name, "file->"+item.getName());
                }
            }
            return "Multipart:"+JSON.toJSONString(params);
        } catch (Exception e) {
            exception = e;
        }
        return "Multipart:error->"+exception;
    }
}
