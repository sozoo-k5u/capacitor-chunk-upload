package io.sozoo.plugins.chunkupload;

public interface UploadFileResultCallback {
    void success();
    void error(String message);
    void progress(Double percentage);
}