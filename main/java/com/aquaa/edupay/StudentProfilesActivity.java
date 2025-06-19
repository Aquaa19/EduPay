package com.aquaa.edupay;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.aquaa.edupay.adapters.StudentAdapter;
import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.Constants;
import com.aquaa.edupay.utils.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StudentProfilesActivity extends AppCompatActivity implements StudentAdapter.OnStudentActionListener {

    private static final String TAG = "StudentProfilesActivity";

    private ListView lvStudentProfiles;
    private Spinner spinnerSemester;
    private EditText etSearchStudentProfile;
    private ImageView ivHelpIcon;
    private RadioGroup rgStudentStatusFilter;
    private StudentAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Student> allStudents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profiles);

        dbHelper = new DatabaseHelper(this);
        lvStudentProfiles = findViewById(R.id.lvStudentProfiles);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        etSearchStudentProfile = findViewById(R.id.etSearchStudentProfile);
        ivHelpIcon = findViewById(R.id.ivHelpIcon);
        rgStudentStatusFilter = findViewById(R.id.rgStudentStatusFilter);

        setupSemesterSpinner();

        allStudents = dbHelper.getAllStudents(); // Load all students initially
        adapter = new StudentAdapter(this, new ArrayList<>(), DateUtils.getCurrentYear());
        adapter.setOnStudentActionListener(this);
        lvStudentProfiles.setAdapter(adapter);

        etSearchStudentProfile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplayStudents();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        rgStudentStatusFilter.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                filterAndDisplayStudents();
            }
        });

        ivHelpIcon.setOnClickListener(v -> showColorLegendDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-fetch all students on resume, in case status or other details changed
        allStudents = dbHelper.getAllStudents();
        Log.d(TAG, "onResume: Total students fetched from DB: " + allStudents.size());
        filterAndDisplayStudents();
    }

    private void setupSemesterSpinner() {
        ArrayAdapter<CharSequence> semesterAdapter = ArrayAdapter.createFromResource(
                this, R.array.semesters, android.R.layout.simple_spinner_item);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        spinnerSemester.setSelection(0);

        spinnerSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Semester selected: " + (position + 1));
                filterAndDisplayStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }


    private void filterAndDisplayStudents() {
        List<Student> filteredStudents = new ArrayList<>();

        int selectedSemester = spinnerSemester.getSelectedItemPosition() + 1;
        String searchQuery = etSearchStudentProfile.getText().toString().trim().toLowerCase(Locale.getDefault());

        String studentStatusFilter = Constants.STATUS_ACTIVE;
        int checkedStudentStatusFilterId = rgStudentStatusFilter.getCheckedRadioButtonId();
        if (checkedStudentStatusFilterId == R.id.rbStatusGraduated) {
            studentStatusFilter = Constants.STATUS_GRADUATED;
        } else if (checkedStudentStatusFilterId == R.id.rbStatusAll) {
            studentStatusFilter = Constants.STATUS_ALL;
        }

        Log.d(TAG, "Filtering students for selected semester: " + selectedSemester +
                ", search query: '" + searchQuery + "', status: " + studentStatusFilter);

        for (Student student : allStudents) {
            // 1. Filter by student status (Active/Graduated/All)
            if (!studentStatusFilter.equals(Constants.STATUS_ALL) && !student.getStatus().equalsIgnoreCase(studentStatusFilter)) {
                continue;
            }

            // 2. Apply semester filter ONLY if student is active (or 'all' status but currently active)
            // Graduated students are not filtered by semester as they are no longer "in" a semester.
            if (studentStatusFilter.equals(Constants.STATUS_ACTIVE) || (studentStatusFilter.equals(Constants.STATUS_ALL) && Constants.STATUS_ACTIVE.equals(student.getStatus()))) {
                if (student.getCurrentSemester() != selectedSemester) {
                    continue;
                }
            } else if (studentStatusFilter.equals(Constants.STATUS_GRADUATED)) {
                // If filtering specifically for graduated students, we ignore the semester spinner value.
                // We just need to ensure the student's status IS 'graduated'.
                if (!Constants.STATUS_GRADUATED.equals(student.getStatus())) {
                    continue; // This should ideally be caught by the first status filter, but good for robustness.
                }
            }


            // 3. Filter by search query
            if (!searchQuery.isEmpty() && !student.getName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                continue;
            }

            filteredStudents.add(student);
            Log.d(TAG, "Added filtered student: " + student.getName() + " (DB Semester: " + student.getCurrentSemester() + ", Status: " + student.getStatus() + ")");
        }

        Collections.sort(filteredStudents, new Comparator<Student>() {
            @Override
            public int compare(Student s1, Student s2) {
                return s1.getName().compareToIgnoreCase(s2.getName());
            }
        });

        Log.d(TAG, "Number of students after filtering: " + filteredStudents.size());

        if (adapter == null) {
            adapter = new StudentAdapter(this, filteredStudents, DateUtils.getCurrentYear());
            adapter.setOnStudentActionListener(this);
            lvStudentProfiles.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(filteredStudents);
            adapter.setSelectedYear(DateUtils.getCurrentYear());
            adapter.notifyDataSetChanged();
        }

        if (filteredStudents.isEmpty()) {
            Toast.makeText(this, "No students found for Semester " + selectedSemester + " with current filters.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showColorLegendDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_color_legend_dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        LinearLayout llLegendContainer = dialog.findViewById(R.id.llLegendContainer);
        Button btnClose = dialog.findViewById(R.id.btnCloseLegend);

        for (int i = 1; i <= 6; i++) {
            View legendItem = LayoutInflater.from(this).inflate(R.layout.item_color_legend, llLegendContainer, false);
            View colorCircle = legendItem.findViewById(R.id.colorCircle);
            TextView tvLegendText = legendItem.findViewById(R.id.tvLegendText);

            int semesterColor = Constants.getSemesterColor(i);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(semesterColor);
            colorCircle.setBackground(drawable);

            tvLegendText.setText("Semester " + i);
            tvLegendText.setTextColor(ContextCompat.getColor(this, R.color.light_on_surface));

            llLegendContainer.addView(legendItem);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onDeleteStudent(int studentId) {
        dbHelper.deleteStudent(studentId);
        Toast.makeText(this, "Student deleted successfully!", Toast.LENGTH_SHORT).show();
        allStudents = dbHelper.getAllStudents();
        filterAndDisplayStudents();
    }
}
