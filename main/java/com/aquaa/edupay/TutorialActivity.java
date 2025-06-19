package com.aquaa.edupay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.aquaa.edupay.adapters.TutorialPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager2 viewPagerTutorial;
    private TutorialPagerAdapter adapter;
    private Button btnSkip, btnNext, btnDone;
    private LinearLayout layoutDots;

    private int[] layouts; // Array to hold layout IDs for slides

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Hide ActionBar for tutorial
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        viewPagerTutorial = findViewById(R.id.viewPagerTutorial);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);
        btnDone = findViewById(R.id.btnDone);
        layoutDots = findViewById(R.id.layoutDots);

        // Define your tutorial slide layouts (you can use item_tutorial_slide.xml with different content)
        // For simplicity, we'll use a single layout and populate content dynamically in adapter
        layouts = new int[]{
                R.layout.item_tutorial_slide, // Slide 1: Welcome
                R.layout.item_tutorial_slide, // Slide 2: Add Students
                R.layout.item_tutorial_slide, // Slide 3: Track Fees
                R.layout.item_tutorial_slide, // Slide 4: Student Profiles
                R.layout.item_tutorial_slide  // Slide 5: Promote Students
        };

        // Prepare slide data (title, description, image resource)
        List<TutorialPagerAdapter.SlideData> slideDataList = new ArrayList<>();
        slideDataList.add(new TutorialPagerAdapter.SlideData(
                "Welcome to EduPay!",
                "Your comprehensive solution for managing student fees and academic progress.",
                R.drawable.ic_launcher_foreground // Replace with a more relevant image
        ));
        slideDataList.add(new TutorialPagerAdapter.SlideData(
                "Add & Manage Students",
                "Easily add new students, view their profiles, and import/export data.",
                R.drawable.ic_add_student // You'll need to add this drawable
        ));
        slideDataList.add(new TutorialPagerAdapter.SlideData(
                "Track Fees Status",
                "Monitor monthly fee payments, mark paid/unpaid, and filter by status.",
                R.drawable.ic_check_fees // You'll need to add this drawable
        ));
        slideDataList.add(new TutorialPagerAdapter.SlideData(
                "Student Profiles",
                "View detailed student information, including their current semester and fee status at a glance.",
                R.drawable.ic_student_profiles // You'll need to add this drawable
        ));
        slideDataList.add(new TutorialPagerAdapter.SlideData(
                "Promote Students",
                "Effortlessly promote students to the next semester based on academic periods.",
                R.drawable.ic_promote_student // You'll need to add this drawable
        ));


        adapter = new TutorialPagerAdapter(slideDataList);
        viewPagerTutorial.setAdapter(adapter);

        addDotsIndicator(0); // Add dots for the first slide

        viewPagerTutorial.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                addDotsIndicator(position);
                updateButtons(position);
            }
        });

        btnSkip.setOnClickListener(v -> launchHomeScreen());
        btnNext.setOnClickListener(v -> {
            int current = viewPagerTutorial.getCurrentItem() + 1;
            if (current < adapter.getItemCount()) {
                viewPagerTutorial.setCurrentItem(current);
            } else {
                launchHomeScreen();
            }
        });
        btnDone.setOnClickListener(v -> launchHomeScreen());
    }

    private void addDotsIndicator(int currentPage) {
        layoutDots.removeAllViews();
        TextView[] dots = new TextView[adapter.getItemCount()];
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText("•"); // Bullet point character
            dots[i].setTextSize(35);
            dots[i].setTextColor(ContextCompat.getColor(this, R.color.gray)); // Inactive dot color
            layoutDots.addView(dots[i]);
        }
        if (dots.length > 0) {
            // Corrected: Use the direct color resource that colorPrimary is set to in themes.xml
            dots[currentPage].setTextColor(ContextCompat.getColor(this, R.color.app_primary_color)); // Active dot color
        }
    }

    private void updateButtons(int position) {
        if (position == adapter.getItemCount() - 1) { // Last slide
            btnNext.setVisibility(View.GONE);
            btnSkip.setVisibility(View.GONE);
            btnDone.setVisibility(View.VISIBLE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
            btnSkip.setVisibility(View.VISIBLE);
            btnDone.setVisibility(View.GONE);
        }
    }

    private void launchHomeScreen() {
        startActivity(new Intent(TutorialActivity.this, MainActivity.class));
        finish(); // Finish tutorial activity so user can't go back
    }
}
