package com.aquaa.edupay;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnAddStudent, btnCheckFeesStatus, btnStudentProfiles, btnPromoteStudent;
    private TextView tvAppTitle; // Re-added for the app title in the main layout

    private static final String PREFS_NAME = "EduPayPrefs";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_IS_FIRST_LAUNCH = "isFirstLaunch"; // New key for tutorial

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize TextView for app title
        tvAppTitle = findViewById(R.id.tvAppTitle); // Initialize the new TextView

        // Initialize buttons
        btnAddStudent = findViewById(R.id.btnAddStudent);
        btnCheckFeesStatus = findViewById(R.id.btnCheckFeesStatus);
        btnStudentProfiles = findViewById(R.id.btnStudentProfiles);
        btnPromoteStudent = findViewById(R.id.btnPromoteStudent);

        setClickListeners();
        checkFirstLaunchAndShowTutorial(); // New method to handle tutorial
        displayGreeting(); // This will now update the ActionBar title
    }

    private void setClickListeners() {
        btnAddStudent.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddStudentActivity.class)));
        btnCheckFeesStatus.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CheckFeesStatusActivity.class)));
        btnStudentProfiles.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StudentProfilesActivity.class)));
        btnPromoteStudent.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PromoteStudentActivity.class)));
    }

    private void checkFirstLaunchAndShowTutorial() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true); // Default to true

        if (isFirstLaunch) {
            // Mark as not first launch for next time
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_IS_FIRST_LAUNCH, false);
            editor.apply();

            // Launch tutorial activity
            Intent tutorialIntent = new Intent(MainActivity.this, TutorialActivity.class);
            startActivity(tutorialIntent);
            // Optionally finish MainActivity if you want tutorial to be the very first screen
            // finish();
        }
    }

    private void displayGreeting() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userName = prefs.getString(KEY_USER_NAME, null);

        if (userName == null || userName.isEmpty()) {
            showNameInputDialog();
        } else {
            updateGreetingText(userName);
        }
    }

    private void showNameInputDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom_name_input_dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        EditText etUserName = dialog.findViewById(R.id.etUserName);
        Button btnSaveName = dialog.findViewById(R.id.btnSaveName);
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialog.findViewById(R.id.dialogMessage);

        if (dialogTitle != null) dialogTitle.setTextColor(getResources().getColor(R.color.light_on_surface, getTheme()));
        if (dialogMessage != null) dialogMessage.setTextColor(getResources().getColor(R.color.light_on_surface, getTheme()));

        btnSaveName.setOnClickListener(v -> {
            String name = etUserName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter your name.", Toast.LENGTH_SHORT).show();
            } else {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_USER_NAME, name);
                editor.apply();
                updateGreetingText(name);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void updateGreetingText(String userName) {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay >= 5 && hourOfDay < 12) {
            greeting = "Good Morning";
        } else if (hourOfDay >= 12 && hourOfDay < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(String.format(Locale.getDefault(), "%s, %s", userName, greeting));
        }
    }
}
