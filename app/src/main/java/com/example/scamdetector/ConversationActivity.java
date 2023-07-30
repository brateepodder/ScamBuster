package com.example.scamdetector;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;

public class ConversationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);

        RecyclerView messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        MessageAdapter messageAdapter = new MessageAdapter(this);
        messagesRecyclerView.setAdapter(messageAdapter);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String phoneNumber = extras.getString("phoneNumber");
            ArrayList<String> messages = retrieveMessages(phoneNumber);
            ArrayList<Date> timestamps = retrieveTimestamps(phoneNumber);
            messageAdapter.setMessages(messages, timestamps);
            int lastItemPosition = messageAdapter.getItemCount() - 1;
            messagesRecyclerView.scrollToPosition(lastItemPosition);
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private ArrayList<String> retrieveMessages(String phoneNumber) {
        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = {"address", "body", "date"};
        String selection = "address = ?";
        String[] selectionArgs = {phoneNumber};
        String sortOrder = "date ASC";

        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        ArrayList<String> messages = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                messages.add(body);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return messages;
    }

    private ArrayList<Date> retrieveTimestamps(String phoneNumber) {
        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = {"address", "date"};
        String selection = "address = ?";
        String[] selectionArgs = {phoneNumber};
        String sortOrder = "date DESC";

        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        ArrayList<Date> timestamps = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                Date date = new Date(timestamp);
                timestamps.add(date);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return timestamps;
    }
}
