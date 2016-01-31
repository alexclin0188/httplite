package com.example;

/**
 * FileInfo
 *
 * @author alexclin
 * @date 16/1/9 18:27
 */
public class FileInfo {
    public String fileName;
    public String filePath;
    public boolean isDir;
    public String hash;

    @Override
    public String toString() {
        return "FileInfo{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", isDir=" + isDir +
                ", hash='" + hash + '\'' +
                '}';
    }
}
