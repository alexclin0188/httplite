package alexclin.httplite;


import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.impl.ObjectParser;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * DownloadHandler
 *
 * @author alexclin at 16/1/1 19:12
 */
class DownloadHandler extends ResponseHandler<File>{
    private static final int CHECK_SIZE = 512;
    private static final int MAX_DOWNLOAD_RETRY = 2;
    private int downloadRetryCount;

    private DownloadParams params;

    private ThreadLocal<Boolean> threadCancel;

    private volatile boolean isExecuted = false;

    private Executable httpHandle;

    public DownloadHandler(Callback<File> mCallback, HttpCall call, DownloadParams params, boolean callOnMain) {
        super(mCallback,call,File.class,callOnMain);
        this.params = params;
    }

    private static boolean isSupportRange(Response response) {
        if (response == null) return false;
        String ranges = response.header("Accept-Ranges");
        if (ranges != null) {
            return ranges.contains("bytes");
        }
        ranges = response.header("Content-Range");
        return ranges != null && ranges.contains("bytes");
    }

    static DownloadParams createParams(String path, String fileName, boolean autoResume, boolean autoRename,String url) {
        if(TextUtils.isEmpty(path)){
            return null;
        }
        File parentDir = new File(path);
        if(TextUtils.isEmpty(fileName)){
            if(path.endsWith("/")){
                autoRename = true;
            }else{
                if(parentDir.exists()&&parentDir.isDirectory()){
                    autoRename = true;
                }else{
                    int index = path.lastIndexOf("/");
                    if(index!=-1){
                        fileName = parentDir.getName();
                        parentDir = parentDir.getParentFile();
                    }else
                        return null;
                }
            }
        }
        if(TextUtils.isEmpty(fileName)){
            fileName = createDefaultName(url);
        }
        File targetFile = new File(parentDir,fileName);
        if(!parentDir.exists()){
            if(!parentDir.mkdirs()){
                return null;
            }
        }
        if(!parentDir.canWrite()){
            return null;
        }
        return new DownloadParams(parentDir,targetFile,autoResume,autoRename);
    }

    private static String createDefaultName(String url) {
        int index = url.lastIndexOf("/");
        if(index>-1&&index<url.length()-1){
            return url.substring(index+1);
        }
        return String.format(Locale.getDefault(),"lite%d.tmp", System.currentTimeMillis());
    }

    private static String getResponseFileName(Response response) {
        if (response == null) return null;
        String disposition = response.header("Content-Disposition");
        if (!TextUtils.isEmpty(disposition)) {
            int startIndex = disposition.indexOf("filename=");
            if (startIndex > 0) {
                startIndex += 9; // "filename=".length()
                int endIndex = disposition.indexOf(";", startIndex);
                if (endIndex < 0) {
                    endIndex = disposition.length();
                }
                if (endIndex > startIndex) {
                    try {
                        return URLDecoder.decode(
                                disposition.substring(startIndex, endIndex),
                                response.body().contentType().charset(Util.UTF_8).name());
                    } catch (UnsupportedEncodingException ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    @Override
    File parseResponse(Response response) throws Exception {
        try {
            if(ObjectParser.isSuccess(response)){
                checkCanceled();
                if(params.autoResume){
                    params.autoResume = isSupportRange(response);
                }
                if(params.autoRename){
                    String name = getResponseFileName(response);
                    if(!TextUtils.isEmpty(name)&&!name.equals(params.targetFile.getName())){
                        File newFile = new File(params.parentDir,name);
                        if(!params.autoResume||!newFile.exists()||newFile.length()<CHECK_SIZE){
                            params.targetFile = renameTargetFile(name,params.targetFile);
                        }else{
                            //需要重新发起请求
                            params.targetFile = newFile;
                            doResumeWork();
                            response = call.sync();
                        }
                    }
                }

                saveToFile(response);
                return params.targetFile;
            }else{
                throw ObjectParser.responseToException(response);
            }
        }catch (Exception e){
            if(isCanceled()) e = new CanceledException(e);
            throw e;
        }finally {
            isExecuted = true;
        }
    }

    private File renameTargetFile(String newName,File oldTargetFile) {
        if(TextUtils.isEmpty(newName)) {
            return oldTargetFile;
        }
        File newFile = new File(params.parentDir, newName);
        if(oldTargetFile.getAbsolutePath().equals(newFile.getAbsolutePath())){
            return newFile;
        }else if (oldTargetFile.exists() && !TextUtils.isEmpty(newName)) {
            while (newFile.exists()) {
                newFile = new File(oldTargetFile.getParent(), System.currentTimeMillis() + newName);
            }
            return oldTargetFile.renameTo(newFile) ? newFile : oldTargetFile;
        }else{
            if(newFile.exists()){
                Util.deleteFileOrDir(newFile);
            }
            return newFile;
        }
    }

    private void processHeaders(Map<String, List<String>> headers){
        if(!params.targetFile.exists()||!params.autoResume){
            params.autoResume = false;
            return;
        }
        long range;
        long fileLen = params.targetFile.length();
        if (fileLen <= CHECK_SIZE) {
            params.autoResume = false;
            return;
        } else {
            range = fileLen - CHECK_SIZE;
        }
        // retry 时需要覆盖RANGE参数
        if(headers==null){
            headers = new HashMap<>();
        }
        headers.put("RANGE", Collections.singletonList("bytes=" + range + "-"));
    }

    private void saveToFile(Response response) throws Exception{
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            if (params.targetFile.isDirectory()) {
                // 防止文件正在写入时, 父文件夹被删除, 继续写入时造成偶现文件节点异常问题.
                Util.deleteFileOrDir(params.targetFile);
            }
            checkCanceled();
            if (!params.targetFile.exists()) {
                params.targetFile.createNewFile();
            }
            checkCanceled();
            // 处理[断点逻辑2](见文件头doc)
            long targetFileLen = params.targetFile.length();
            if (params.autoResume && targetFileLen > 0) {
                FileInputStream fis = null;
                try {
                    long filePos = targetFileLen - CHECK_SIZE;
                    if (filePos > 0) {
                        fis = new FileInputStream(params.targetFile);
                        byte[] fileCheckBuffer = Util.readBytes(fis, filePos, CHECK_SIZE);
                        byte[] checkBuffer = Util.readBytes(response.body().stream(), 0, CHECK_SIZE);
                        if (!Arrays.equals(checkBuffer, fileCheckBuffer)) {
                            Util.closeQuietly(fis); // 先关闭文件流, 否则文件删除会失败.
                            Util.deleteFileOrDir(params.targetFile);
                            retryDownload(new RuntimeException("autoResume but file is changed"));
                            return;
                        }
                    } else {
                        Util.deleteFileOrDir(params.targetFile);
                        retryDownload(new RuntimeException("autoResume but local file large then server file length"));
                        return;
                    }
                } finally {
                    Util.closeQuietly(fis);
                }
            }
            checkCanceled();
            // 开始下载
            long current = 0;
            FileOutputStream fileOutputStream;
            if (params.autoResume) {
                current = targetFileLen;
                fileOutputStream = new FileOutputStream(params.targetFile, true);
            } else {
                fileOutputStream = new FileOutputStream(params.targetFile);
            }
            checkCanceled();
            long total = response.body().contentLength() + current;
            bis = new BufferedInputStream(response.body().stream());
            bos = new BufferedOutputStream(fileOutputStream);
            checkCanceled();
            byte[] tmp = new byte[4096];
            int len;
            while ((len = bis.read(tmp)) != -1) {
                // 防止父文件夹被其他进程删除, 继续写入时造成父文件夹变为0字节文件的问题.
                if (!params.targetFile.getParentFile().exists()) {
                    params.targetFile.getParentFile().mkdirs();
                    throw new IOException("parent be deleted!");
                }
                checkCanceled();
                bos.write(tmp, 0, len);
                current += len;
                onProgress(current,total);
            }
            bos.flush();
            onProgress(current,total);
        } finally {
            Util.closeQuietly(bis);
            Util.closeQuietly(bos);
        }
    }

    final void onProgress(final long current, final long total) {
        HttpLite.postOnMain(new Runnable() {
            @Override
            public void run() {
                ProgressListener listener = call.request.progressListener;
                if (listener != null) {
                    listener.onProgressUpdate(false,current, total);
                }
            }
        });
    }

    private boolean isThreadCanceled(){
        return threadCancel!=null&&threadCancel.get();
    }

    private void setThreadCanceled(boolean cancel){
        if(threadCancel==null){
            threadCancel = new ThreadLocal<>();
        }
        threadCancel.set(cancel);
    }

    private void checkCanceled() throws CanceledException {
        if(isCanceled() || isThreadCanceled()){
            setThreadCanceled(true);
            throw new CanceledException("Download is canceled");
        }
    }

    private void retryDownload(Throwable throwable) throws Exception{
        checkCanceled();
        downloadRetryCount++;
        if(downloadRetryCount>MAX_DOWNLOAD_RETRY){
            throw new RuntimeException(String.format(Locale.getDefault(),
                    "Download retry over limit count:%d",downloadRetryCount),throwable);
        }
        try {
            params.targetFile = parseResponse(call.sync());
        } catch (Exception e) {
            retryDownload(throwable);
        }
    }

    public void doResumeWork(){
        processHeaders(call.request.getHeaders());
    }

    public boolean isCanceled() {
        return (httpHandle!=null&&httpHandle.isCanceled())||isCanceled;
    }

    public Executable wrap(Executable handle) {
        this.httpHandle = new ExecutableWrapper(handle);
        return httpHandle;
    }

    public static class DownloadParams{
        private File parentDir;
        private File targetFile;
        private boolean autoResume;
        private boolean autoRename;

        public DownloadParams(File parentDir, File targetFile, boolean autoResume, boolean autoRename) {
            this.parentDir = parentDir;
            this.targetFile = targetFile;
            this.autoResume = autoResume;
            this.autoRename = autoRename;
        }

        public File getParentDir() {
            return parentDir;
        }

        public File getTargetFile() {
            return targetFile;
        }

        public boolean isAutoResume() {
            return autoResume;
        }

        public boolean isAutoRename() {
            return autoRename;
        }
    }

    private class ExecutableWrapper implements Executable{
        private Executable real;

        public ExecutableWrapper(Executable real) {
            this.real = real;
        }

        @Override
        public Response execute() throws Exception {
            return real.execute();
        }

        @Override
        public void enqueue(ResponseHandler responseHandler) {
            real.enqueue(responseHandler);
        }

        @Override
        public void cancel() {
            isCanceled = true;
            real.cancel();
        }

        @Override
        public boolean isExecuted() {
            return real.isExecuted()&&isExecuted;
        }

        @Override
        public boolean isCanceled() {
            return real.isCanceled()||isCanceled;
        }
    }
}
