package com.aquaa.edupay;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioGroup; // Import RadioGroup
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aquaa.edupay.adapters.PromoteStudentAdapter;
import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.Constants; // Import Constants
import com.aquaa.edupay.utils.DateUtils;
import com.aquaa.edupay.utils.PromotionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.List;

public class PromoteStudentActivity extends AppCompatActivity implements PromoteStudentAdapter.OnStudentCheckedChangeListener {

    private static final String TAG = "PromoteStudentActivity";

    private Spinner spinnerFilterSemester, spinnerPromotionMonth;
    private RadioGroup rgStudentStatusFilter; // NEW: Declare RadioGroup for student status filter
    private ListView lvPromoteStudents;
    private CheckBox cbSelectAllStudents;
    private Button btnPromoteSelected;

    private DatabaseHelper dbHelper;
    private PromotionManager promotionManager;
    private List<Student> allStudents; // This will now contain both active and graduated students
    private PromoteStudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_student);

        dbHelper = new DatabaseHelper(this);
        promotionManager = new PromotionManager(this);

        spinnerFilterSemester = findViewById(R.id.spinnerFilterSemester);
        lvPromoteStudents = findViewById(R.id.lvPromoteStudents);
        cbSelectAllStudents = findViewById(R.id.cbSelectAllStudents);
        spinnerPromotionMonth = findViewById(R.id.spinnerPromotionMonth);
        btnPromoteSelected = findViewById(R.id.btnPromoteSelected);
        rgStudentStatusFilter = findViewById(R.id.rgStudentStatusFilter); // Initialize RadioGroup

        // Load all students (active and graduated) once
        allStudents = dbHelper.getAllStudents();

        setupSpinners();
        setupListViewAndAdapter();

        spinnerFilterSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAndDisplayStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // NEW: Listener for student status filter
        rgStudentStatusFilter.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                filterAndDisplayStudents();
            }
        });

        cbSelectAllStudents.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.setAllChecked(isChecked);
            }
        });

        btnPromoteSelected.setOnClickListener(v -> promoteSelectedStudents());

        filterAndDisplayStudents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload all students on resume, in case status or other details changed elsewhere
        allStudents = dbHelper.getAllStudents();
        filterAndDisplayStudents();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> filterSemesterAdapter = ArrayAdapter.createFromResource(this,
                R.array.semesters, android.R.layout.simple_spinner_item);
        filterSemesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterSemester.setAdapter(filterSemesterAdapter);
        spinnerFilterSemester.setSelection(0);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(this,
                R.array.months, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPromotionMonth.setAdapter(monthAdapter);
        spinnerPromotionMonth.setSelection(DateUtils.getCurrentMonth());
    }

    private void setupListViewAndAdapter() {
        adapter = new PromoteStudentAdapter(this, new ArrayList<>());
        adapter.setOnStudentCheckedChangeListener(this);
        lvPromoteStudents.setAdapter(adapter);
    }

    private void filterAndDisplayStudents() {
        List<Student> filteredStudents = new ArrayList<>();
        int selectedFilterSemester = spinnerFilterSemester.getSelectedItemPosition() + 1;

        // Determine student status filter
        String studentStatusFilter;
        int checkedStudentStatusFilterId = rgStudentStatusFilter.getCheckedRadioButtonId();
        if (checkedStudentStatusFilterId == R.id.rbStatusGraduated) {
            studentStatusFilter = Constants.STATUS_GRADUATED;
        } else { // Default or if rbStatusActive is checked
            studentStatusFilter = Constants.STATUS_ACTIVE;
        }
        // No "All" option for promote screen, as we only want to promote active/graduated students


        for (Student student : allStudents) {
            // 1. Filter by student status
            if (!student.getStatus().equalsIgnoreCase(studentStatusFilter)) {
                continue; // Skip if status doesn't match the active/graduated filter
            }

            // 2. Filter by semester
            // Students in Semester 6 are not "promotable" to a next semester,
            // but they will be "graduated" if selected and in Semester 6.
            // So, for filtering for promotion, we want to show active students who are NOT yet graduated.
            // If the filter is set to "Graduated", we show graduated students regardless of semester.
            if (studentStatusFilter.equals(Constants.STATUS_ACTIVE) && student.getCurrentSemester() != selectedFilterSemester) {
                continue; // For active students, apply semester filter
            }
            // If status filter is "graduated", we don't need to filter by semester for display purposes,
            // as all graduated students are considered "past" semester 6, or directly set to graduated.
            // However, it makes more sense for the "promote" screen to only show ACTIVE students in a specific semester
            // for promotion, and *potentially* show graduated students for review, but not for direct "promotion" action.
            // The existing logic already makes sure graduated students are not promoted if the flag is active.
            // So, for Promote Students, it's most logical to always filter by the selected semester AND the selected status.

            filteredStudents.add(student);
        }

        Collections.sort(filteredStudents, new Comparator<Student>() {
            @Override
            public int compare(Student s1, Student s2) {
                return s1.getName().compareToIgnoreCase(s2.getName());
            }
        });

        adapter.clear();
        adapter.addAll(filteredStudents);
        adapter.clearCheckedStates();
        adapter.notifyDataSetChanged();

        cbSelectAllStudents.setChecked(adapter.areAllStudentsChecked());
        cbSelectAllStudents.setEnabled(!filteredStudents.isEmpty());
    }

    private void promoteSelectedStudents() {
        List<Student> selectedStudents = adapter.getSelectedStudents();

        if (selectedStudents.isEmpty()) {
            Toast.makeText(this, "Please select at least one student to promote.", Toast.LENGTH_SHORT).show();
            return;
        }

        int promotionYear = DateUtils.getCurrentYear();
        int promotionMonth = spinnerPromotionMonth.getSelectedItemPosition();

        int promotedCount = 0;
        int graduatedCount = 0;
        for (Student student : selectedStudents) {
            boolean success = promotionManager.promoteStudent(student.getStudentId(), promotionMonth, promotionYear);
            if (success) {
                // Re-fetch the student to check their *new* status if promoted from Sem 6
                Student updatedStudent = dbHelper.getStudentById(student.getStudentId());
                if (updatedStudent != null && Constants.STATUS_GRADUATED.equals(updatedStudent.getStatus())) {
                    graduatedCount++;
                } else {
                    promotedCount++;
                }
            } else {
                Log.w(TAG, "Failed to promote/graduate " + student.getName() + " or promotion not applicable (e.g., already in final semester and not 6).");
            }
        }

        if (promotedCount > 0 && graduatedCount > 0) {
            Toast.makeText(this, promotedCount + " student(s) promoted and " + graduatedCount + " student(s) graduated!", Toast.LENGTH_LONG).show();
        } else if (promotedCount > 0) {
            Toast.makeText(this, promotedCount + " student(s) promoted successfully!", Toast.LENGTH_SHORT).show();
        } else if (graduatedCount > 0) {
            Toast.makeText(this, graduatedCount + " student(s) graduated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No students were promoted or graduated.", Toast.LENGTH_SHORT).show();
        }

        allStudents = dbHelper.getAllStudents(); // Refresh the list of all students from DB
        filterAndDisplayStudents();
    }

    @Override
    public void onStudentCheckedChanged(int position, boolean isChecked) {
        cbSelectAllStudents.setChecked(adapter.areAllStudentsChecked());
    }

    @Override
    public void onAllStudentsCheckedStateChanged() {
        cbSelectAllStudents.setChecked(adapter.areAllStudentsChecked());
    }
}
