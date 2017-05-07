package com.example;

import com.alibaba.fastjson.JSON;
import com.example.util.EncryptUtil;
import com.example.utils.HttpUtil;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okio.Buffer;
import okio.Okio;

/**
 * com.example
 *
 * @author alexclin
 * @date 16/1/3 22:57
 */
public class GetHandle implements MethodHandle {
    public MockResponse handle(RecordedRequest request, String root) {
        String path = request.getPath();
        int index = path.indexOf("?");
        if (index > -1) {
            String queryString = path.substring(index + 1);
            path = path.substring(0, index);
            Map<String, String[]> paramMap = HttpUtil.getParamsMap(queryString, Util.UTF_8.name());
            System.out.println("Params:" + JSON.toJSONString(paramMap));
            String[] types = paramMap.get("type");
            if (types != null) {
                for (String type : types) {
                    if ("json".equals(type.toLowerCase())) {
                        return responseJson(root, path, paramMap);
                    }
                }
            }
        }
        try {
            if (!path.startsWith("/") || path.contains("..")) throw new FileNotFoundException();
            path = URLDecoder.decode(path, Util.UTF_8.name());
            File file = new File(root + path);
            return file.isDirectory()
                    ? directoryToResponse(path, file)
                    : fileToResponse(path, file);
        } catch (FileNotFoundException e) {
            return new MockResponse()
                    .setStatus("HTTP/1.1 404")
                    .addHeader("content-type: text/plain; charset=utf-8")
                    .setBody("NOT FOUND: " + path);
        } catch (IOException e) {
            return new MockResponse()
                    .setStatus("HTTP/1.1 500")
                    .addHeader("content-type: text/plain; charset=utf-8")
                    .setBody("SERVER ERROR: " + e);
        }
    }

    private MockResponse directoryToResponse(String basePath, File directory) {
        if (!basePath.endsWith("/")) basePath += "/";

        StringBuilder response = new StringBuilder();
        response.append(String.format("<html><head><title>%s</title></head><body>", basePath));
        response.append(String.format("<h1>%s</h1>", basePath));
        for (String file : directory.list()) {
            response.append(String.format("<div class='file'><a href='%s'>%s</a></div>",
                    basePath + file, file));
        }
        response.append("</body></html>");

        return new MockResponse()
                .setStatus("HTTP/1.1 200")
                .addHeader("content-type: text/html; charset=utf-8")
                .setBody(response.toString());
    }

    public static MockResponse fileToResponse(String path, File file) throws IOException {
        return new MockResponse()
                .setStatus("HTTP/1.1 200")
                .setBody(fileToBytes(file))
                .addHeader("content-type: " + contentType(path));
    }

    public static Buffer fileToBytes(File file) throws IOException {
        Buffer result = new Buffer();
        result.writeAll(Okio.source(file));
        return result;
    }

    public static String contentType(String path) {
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg")) return "image/jpeg";
        if (path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".txt")) return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }

    private MockResponse responseJson(String root, String path, Map<String, String[]> paramMap) {
        BaseResult<Object> result = new BaseResult<>();
        result.requestPath = path;
        result.requestMethod = "GET";
        if (!path.startsWith("/") || path.contains("..")) {
            result.code = 404;
            result.errorMsg = "FileInfo:" + path + " not found";
        } else {
            File file = new File(root + path);
            if (file.isDirectory()) {
                File[] childs = file.listFiles();
                List<FileInfo> list = new ArrayList<>();
                if (childs != null)
                    for (File child : childs) {
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.fileName = child.getName();
                        fileInfo.filePath = child.getAbsolutePath();
                        fileInfo.isDir = child.isDirectory();
                        if (fileInfo.filePath.startsWith(root)) {
                            fileInfo.filePath = fileInfo.filePath.substring(root.length());
                        }
                        if (child.isFile()) {
                            fileInfo.hash = EncryptUtil.hash(child);
                        }
                        list.add(fileInfo);
                    }
                result.data = list;
                result.code = 200;
            } else if (file.isFile()) {
                try {
                    return fileToResponse(path, file);
                } catch (IOException e) {
                    result.code = 500;
                    result.errorMsg = "Server code:" + e;
                }
            }
        }
        return new MockResponse().setStatus("HTTP/1.1 200").addHeader("content-type: application/json; charset=utf-8")
                .setBody(JSON.toJSONString(result));
    }
}
