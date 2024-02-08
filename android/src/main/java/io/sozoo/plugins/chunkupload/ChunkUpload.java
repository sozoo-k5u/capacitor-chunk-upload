
package io.sozoo.plugins.chunkupload;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChunkUpload {
    private String TAG = "CAPACITOR_CHUNK_UPLOAD";

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
    public void uploadFile(
            BufferedInputStream inputStream,
            String url,
            Map<String,String> headers,
            UploadFileResultCallback uploadFileCallback
    ) {
        List<String> blockIds = new ArrayList<>();
        final int MAX_SUB_SIZE = 1024 * 1024;// 4194304; // 4*1024*1024 == 4MB
        try {
            long fileSize = inputStream.available();
            int totalChunks = (int) Math.ceil((double) fileSize / MAX_SUB_SIZE);
            byte[] buffer = new byte[MAX_SUB_SIZE];
            int chunk = 0;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Generate a random ID for the chunk,Encode the random ID in Base64 and add it to the blockIds array
                String blockId = Base64.encodeToString(generateRandomId().getBytes(), Base64.NO_WRAP);
                blockIds.add(blockId);
                String chunkUploadUri = url + "&comp=block&blockid=" + blockId; //??todo put in the option
                // Create a new URL connection for each chunk
                HttpURLConnection connection = (HttpURLConnection) new URL(chunkUploadUri).openConnection();
                connection.setRequestMethod("PUT"); //??todo put in the option
                // Set headers from the map
                if (headers != null && !headers.isEmpty()) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("Content-Length", String.valueOf(bytesRead));
                connection.setDoOutput(true);
                // Write the chunk data to the server
                //todo there was a performance issue with this approach, might find another approach
                try (BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                // Handle the server response if needed
                int responseCode = connection.getResponseCode();
                chunk +=1;
                if (responseCode >= 200 && responseCode < 300) {
                    connection.disconnect();
                    double progressPercentage = (double) ((chunk * 100) / totalChunks);
                    // The request was successful
                    Log.d(TAG, "Chunk " + chunk + " uploaded successfully with response code: " + responseCode);
                    uploadFileCallback.progress(progressPercentage);

                } else {
                    connection.disconnect();
                    // The request failed, handle the error
                    Log.e(TAG, "Error uploading chunk " + chunk + " with response code: " + responseCode);
                    return;
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        //commit the changes, //todo could be an option?
        String commitChunksUri = url + "&comp=blocklist";
        commitChunks(commitChunksUri,blockIds, headers);
        uploadFileCallback.success();

    }
    private String generateRandomId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
    private static void commitChunks(String apiUrl, List<String> chunkIds, Map<String,String> headers) {
        try {
            // Create a new URL connection for the commit operation
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            // Set headers for the commit operation
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            // Convert the list of uploaded chunks to a JSON array
            JSONArray jsonArray = new JSONArray(chunkIds);
            String body = jsonArray.toString();
            byte[] dataBytes = body.getBytes("UTF-8");
            // Set headers from the map
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", String.valueOf(dataBytes.length));
            // Write the JSON array to the server in the request body
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(dataBytes);
                outputStream.flush();
            }
            // Handle the server response for the commit operation
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("Chunks committed successfully with response code: " + responseCode);
            } else {
                System.err.println("Error committing chunks with response code: " + responseCode);
            }
            // Close the connection
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
