package com.example.scamdetector;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class OnClickListener extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_message);

        TextView messageTextView = findViewById(R.id.messageTextView);
        TextView reviewFromHF = findViewById(R.id.reviewFromHF);
        Button ratingButton = findViewById(R.id.ratingbutton);

        // Create an instance of MessageOnClickListener (inner class)
        MessageOnClickListener onClickListener = new MessageOnClickListener(messageTextView, reviewFromHF);

        // Set the OnClickListener for the "rating" button
        ratingButton.setOnClickListener(onClickListener);
    }

    // Inner class for MessageOnClickListener
    private class MessageOnClickListener implements View.OnClickListener {
        private TextView messageTextView;
        private TextView reviewFromHF;

        public MessageOnClickListener(TextView messageTextView, TextView reviewFromHF) {
            this.messageTextView = messageTextView;
            this.reviewFromHF = reviewFromHF;
        }

        @Override
        public void onClick(View v) {
            // Copy the text from messageTextView
            String messageText = messageTextView.getText().toString();

            // Paste the text into reviewFromHF
            String existingReview = reviewFromHF.getText().toString();
            String newReview = existingReview + "\n" + messageText;
            reviewFromHF.setText(newReview);
        }
    }
}
