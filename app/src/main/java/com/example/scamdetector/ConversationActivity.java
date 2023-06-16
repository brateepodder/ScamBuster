package com.example.scamdetector;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import android.provider.ContactsContract;
import java.util.List;


public class ConversationActivity extends AppCompatActivity {
    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Messages");
        actionBar.setDisplayHomeAsUpEnabled(true);

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter();
        messagesRecyclerView.setAdapter(messageAdapter);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String phoneNumber = extras.getString("phoneNumber");

            // Check if the received phoneNumber is a contact name
            if (isContactName(phoneNumber)) {
                phoneNumber = getPhoneNumber(phoneNumber);
            }
            ArrayList<String> messages = retrieveMessages(phoneNumber);
            ArrayList<Date> timestamps = retrieveTimestamps(phoneNumber);
            messageAdapter.setMessages(messages, timestamps);
        }
    }

    private boolean isContactName(String contactName) {
        // Query the Contacts database to check if the given string exists as a contact name
        String[] projection = {ContactsContract.Contacts.DISPLAY_NAME};
        String selection = ContactsContract.Contacts.DISPLAY_NAME + " = ?";
        String[] selectionArgs = {contactName};

        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        boolean isContact = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }

        return isContact;
    }

    private String getPhoneNumber(String contactName) {
        // Query the Contacts database to retrieve the phone number associated with the given contact name
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        String selection = ContactsContract.Contacts.DISPLAY_NAME + " = ?";
        String[] selectionArgs = {contactName};

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        String phoneNumber = null;
        if (cursor != null && cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
        }

        if (cursor != null) {
            cursor.close();
        }

        // If no matching phone number found, use contactName as the phone number
        if (phoneNumber == null) {
            phoneNumber = contactName;
        }

        return phoneNumber;
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private ArrayList<String> retrieveMessages(String number) {
        // Check if the number is a contact name
        String phoneNumber = getPhoneNumber(number);

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

    private ArrayList<Date> retrieveTimestamps(String number) {
        // Check if the number is a contact name
        String phoneNumber = getPhoneNumber(number);

        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = {"address", "date"};
        String selection = "address = ?";
        String[] selectionArgs = {phoneNumber};
        String sortOrder = "date ASC";

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

