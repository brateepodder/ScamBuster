package com.example.scamdetector;

//import android.content.ClipData;
//import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<String> messages;
    private List<Date> timestamps;
    private final Context context;
    private final ApiService apiService;

    public MessageAdapter(Context context) {
        this.messages = new ArrayList<>();
        this.timestamps = new ArrayList<>();
        this.context = context;
        this.apiService = new ApiService();
    }

    public void setMessages(List<String> messages, List<Date> timestamps) {
        this.messages = messages;
        this.timestamps = timestamps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        String message = messages.get(position);
        Date timestamp = timestamps.get(position);

        holder.messageTextView.setText(message);
        holder.dateTextView.setText(formatDate(timestamp));
        holder.timeTextView.setText(formatTime(timestamp));

        holder.ratingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //copyMessageToClipboard(message);
                sendToHuggingface(message, holder.reviewFromHF);
            }
        });
    }
    private void sendToHuggingface(String message, TextView reviewFromHF) {
        apiService.makeRequest(message, new ApiService.Callback() {
            @Override
            public void onResponse(String result) {
                Log.d("HuggingFaceResponse", result);
                try {
                    JSONArray jsonArray = new JSONArray(result);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject labelInfo = jsonArray.getJSONObject(i);
                        String label = labelInfo.getString("label");
                        if (label.equals("LABEL_1")) {
                            double labelScore = labelInfo.getDouble("score");
                            // Convert the score to percentage
                            int scorePercentage = (int) (labelScore * 100);

                            // Set the result in reviewFromHF TextView
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String displayText = scorePercentage + "% scam";
                                    reviewFromHF.setText(displayText);
                                }
                            });

                            // Exit the loop once LABEL_1 is found
                            break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // This method will be called if the API call fails
                e.printStackTrace();
            }
        });
    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView dateTextView;
        TextView timeTextView;
        Button ratingButton;
        TextView reviewFromHF;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            ratingButton = itemView.findViewById(R.id.ratingbutton);
            reviewFromHF = itemView.findViewById(R.id.reviewFromHF);
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    //private void copyMessageToClipboard(String message) {
    //ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    //if (clipboardManager != null) {
    //  ClipData clipData = ClipData.newPlainText("message", message);
    //clipboardManager.setPrimaryClip(clipData);
    //Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
    //}
    //}
}
