package io.sozoo.plugins.chunkupload;

import android.net.Uri;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.BufferedInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@CapacitorPlugin(name = "ChunkUpload")
public class ChunkUploadPlugin extends Plugin {
    public static final String TAG = "ChunkUpload";
    public static final String ERROR_PATH_MISSING = "path must be provided.";
    public static final String ERROR_URL_MISSING = "url must be provided.";

    private ChunkUpload implementation = new ChunkUpload();

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void uploadFile(PluginCall call) {
        try{
            String url = call.getString("url");
            if (url == null) {
                call.reject(ERROR_URL_MISSING);
                return;
            }
            String path = call.getString("path");
            if (path == null) {
                call.reject(ERROR_PATH_MISSING);
                return;
            }
            Map<String,String> headers =  new HashMap<String,String>();
            JSObject headersObject = call.getObject("headers");
            for (Iterator<String> it = headersObject.keys(); it.hasNext(); ) {
                String key = it.next();
                String value = headersObject.getString(key);
                headers.put(key,value);
            }
            JSObject dumpDict = new JSObject();
            notifyListeners("uploadStarted",dumpDict);
            BufferedInputStream bis = new BufferedInputStream(this.getContext().getContentResolver().openInputStream(Uri.parse(path)));
            implementation.uploadFile(bis,
                    url,
                    headers,
                    new UploadFileResultCallback() {
                        @Override
                        public void success() {
                            call.resolve();
                        }
                        @Override
                        public void progress(Double percentage) {
                            JSObject ret = new JSObject();
                            ret.put("percentage", percentage);
                            notifyListeners("uploadProgressChanged", ret);
                        }
                        @Override
                        public void error(String message) {
                            call.reject(message);
                        }
                    }
            );
        }
        catch (Exception exception) {
            call.reject(exception.getMessage());
            Logger.error(TAG, exception.getMessage(), exception);
        }
    }
}
