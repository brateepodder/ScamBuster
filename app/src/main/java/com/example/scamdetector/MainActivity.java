package com.example.scamdetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_SMS = 123;

    private ListView phoneNumberListView;
    private ArrayAdapter<String> phoneNumberAdapter;
    private List<String> phoneNumberList;
    private EditText searchEditText;
    private Button clearButton;
    private TextView textViewResult;
    private TextView NoResultsFound;
    private EditText editTextRequest;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        apiService = new ApiService();

        // Initializing
        phoneNumberListView = findViewById(R.id.phoneNumberListView);
        searchEditText = findViewById(R.id.searchEditText);
        NoResultsFound = findViewById(R.id.NoResultsFound);
        NoResultsFound.setVisibility(View.GONE);
        textViewResult = findViewById(R.id.textViewResult);

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

        // Set up click listener for the phone number list items
        phoneNumberListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedNumber = phoneNumberList.get(position);
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            intent.putExtra("phoneNumber", selectedNumber);
            startActivity(intent);
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

        clearButton = findViewById(R.id.button);

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearButton.setVisibility(View.VISIBLE);
            } else {
                clearButton.setVisibility(View.GONE);
            }
        });

        clearButton.setOnClickListener(v -> searchEditText.setText(""));
        // Initialize the EditText and Button for the request
        editTextRequest = findViewById(R.id.EditText);
        Button buttonSendRequest = findViewById(R.id.buttonSendRequest);

        buttonSendRequest.setOnClickListener(v -> {
            String requestText = editTextRequest.getText().toString();
            sendToHuggingface(requestText);
        });
    }

    private void searchNumbers(String searchQuery) {
        ArrayList<String> filteredList = new ArrayList<>();

        // If search query is empty, show all phone numbers
        if (searchQuery.isEmpty()) {
            retrievePhoneNumbers();
        } else {
            // Filter phone numbers based on search query
            for (String phoneNumber : phoneNumberList) {
                if (phoneNumber.contains(searchQuery)) {
                    filteredList.add(phoneNumber);
                }
            }
            if (filteredList.size() == 0) {
                NoResultsFound.setVisibility(View.VISIBLE);
            } else {
                NoResultsFound.setVisibility(View.GONE);
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
                    newPhoneNumberList.add(phoneNumber);
                }
            }

            cursor.close();
        }

        phoneNumberList = newPhoneNumberList;
        phoneNumberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, phoneNumberList);
        phoneNumberListView.setAdapter(phoneNumberAdapter);
    }

    private void sendToHuggingface(String message) {
        apiService.makeRequest(message, new ApiService.Callback() {
            @Override
            public void onResponse(String result) {
                // This method will be called when the API call is successful
                Log.d("HuggingFaceResponse", result); // Add this log statement to display the full API response

                try {
                    JSONArray jsonArray = new JSONArray(result);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject labelInfo = jsonArray.getJSONObject(i);
                        String label = labelInfo.getString("label");
                        if (label.equals("LABEL_1")) {
                            double labelScore = labelInfo.getDouble("score");
                            // Convert the score to percentage
                            int scorePercentage = (int) (labelScore * 100);

                            // Set the result in textViewResult
                            runOnUiThread(() -> {
                                String displayText = scorePercentage + "% scam/spam";
                                textViewResult.setText(displayText);
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
                // Handle API call failure here if needed
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to retrieve phone numbers
                retrievePhoneNumbers();
            }
        }
    }
}
