package com.example.scamdetector;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class ApiService implements Callback {

    private static final String BASE_URL = "https://api-inference.huggingface.co/models/TaskeenJafri/ScamBuster-Bert";

    public interface Callback {
        void onResponse(String result);

        void onFailure(Exception e);
    }

    public void makeRequest(String requestText, final Callback callback) {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"text\": \"" + requestText + "\"}");
        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseData = response.body().string();
                if (callback != null) {
                    callback.onResponse(responseData);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        // Not used in this implementation
    }

    @Override
    public void onFailure(Call call, IOException e) {
        // Not used in this implementation
    }
}
