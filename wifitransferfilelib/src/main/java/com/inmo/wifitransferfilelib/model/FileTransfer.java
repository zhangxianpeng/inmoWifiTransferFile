package com.inmo.wifitransferfilelib.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class FileTransfer implements Serializable {

    // 文件类型
    private String fileType;

    //文件名
    private String fileName;

    //文件路径
    private String filePath;

    //文件大小
    private long fileSize;

    public FileTransfer() {

    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    @NonNull
    @Override
    public String toString() {
        return "FileTransfer{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                '}';
    }

}
