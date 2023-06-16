package com.example.scamdetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_SMS = 123;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 456;

    private ListView phoneNumberListView;
    private ArrayAdapter<String> phoneNumberAdapter;
    private List<String> phoneNumberList;

    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberListView = findViewById(R.id.phoneNumberListView);

        // Initialize EditText
        searchEditText = findViewById(R.id.searchEditText);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("ScamBuster");
        actionBar.setDisplayHomeAsUpEnabled(false);

        // Request necessary permissions if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSIONS_REQUEST_READ_SMS);
        } else {
            // Permission already granted, proceed to retrieve phone numbers
            retrievePhoneNumbers();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSIONS_REQUEST_READ_SMS);
        } else {
            // Permission already granted, proceed to check READ_CONTACTS permission
            checkReadContactsPermission();
        }

        // Set up click listener for the phone number list items
        phoneNumberListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedContactOrNumber = phoneNumberList.get(position);

                // Check if the selected item is a contact name or a phone number
                if (isContactName(selectedContactOrNumber)) {
                    // Retrieve the corresponding phone number for the contact name
                    String phoneNumber = getPhoneNumber(selectedContactOrNumber);

                    // Pass the phone number to the ConversationActivity
                    Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                    intent.putExtra("phoneNumber", phoneNumber);
                    startActivity(intent);
                } else {
                    // Pass the selected phone number to the ConversationActivity
                    Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                    intent.putExtra("phoneNumber", selectedContactOrNumber);
                    startActivity(intent);
                }
            }
        });

    // Set up text watcher for the search EditText
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Not needed
            }
            @Override
            public void afterTextChanged(Editable s) {
                String searchQuery = s.toString().trim();
                // Check if input is empty and update the phone number list accordingly
                if (s.length() == 0) {
                    retrievePhoneNumbers();
                }
                searchNumbers(searchQuery);
            }
        });

    }

    private void checkReadContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            // Permission already granted, proceed to retrieve phone numbers
            retrievePhoneNumbers();
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

    private String getContactName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        String contactName = null;
        if (cursor != null && cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
            cursor.close();
        }

        return contactName;
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
            cursor.close();
        }

        return phoneNumber;
    }

    private void searchNumbers(String searchQuery) {
        ArrayList<String> filteredList = new ArrayList<>();

        // If search query is empty, show all phone numbers
        if (searchQuery == "") {
            retrievePhoneNumbers();
        } else {
            // Filter phone numbers based on search query
            for (String phoneNumber : phoneNumberList) {
                if (phoneNumber.contains(searchQuery)) {
                    filteredList.add(phoneNumber);
                }
            }
        }

        // Update the adapter with the filtered list
        phoneNumberAdapter.clear();
        phoneNumberAdapter.addAll(filteredList);
        phoneNumberAdapter.notifyDataSetChanged();
    }

    private void retrievePhoneNumbers() {
        List<String> newPhoneNumberList = new ArrayList<>();

        Uri uri = Telephony.Sms.CONTENT_URI;
        String[] projection = {Telephony.Sms.ADDRESS};
        String selection = Telephony.Sms.TYPE + " = " + Telephony.Sms.MESSAGE_TYPE_INBOX;
        String[] selectionArgs = null;
        String sortOrder = Telephony.Sms.DEFAULT_SORT_ORDER;

        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));

                // Check if the phone number is already present in the newPhoneNumberList
                if (!newPhoneNumberList.contains(phoneNumber)) {
                    String contactName = getContactName(phoneNumber);
                    if (contactName != null) {
                        if (!newPhoneNumberList.contains(contactName)) {
                            newPhoneNumberList.add(contactName);
                        }
                    } else {
                        newPhoneNumberList.add(phoneNumber);
                    }
                }
            }

            cursor.close();
        }

        phoneNumberList = newPhoneNumberList;
        phoneNumberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, phoneNumberList);
        phoneNumberListView.setAdapter(phoneNumberAdapter);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_SMS permission granted, check READ_CONTACTS permission
                checkReadContactsPermission();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_CONTACTS permission granted, proceed to retrieve phone numbers
                retrievePhoneNumbers();
            }
        }
    }
}
