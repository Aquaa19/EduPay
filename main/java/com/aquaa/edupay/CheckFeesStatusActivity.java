package com.aquaa.edupay;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aquaa.edupay.adapters.FeeStatusAdapter;
import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.FeePayment;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.Constants;
import com.aquaa.edupay.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class CheckFeesStatusActivity extends AppCompatActivity implements FeeStatusAdapter.FeeStatusUpdateListener {

    private Spinner spinnerMonth, spinnerSemester;
    private RadioGroup rgFilter; // For Paid/Unpaid/All
    private RadioGroup rgStudentStatusFilter; // For Active/Graduated/All
    private ListView lvFeeStatus;
    private CheckBox cbSelectAll;
    private EditText etSearchStudent;
    private TextView tvSelectedMonth, tvSelectedSemester;
    private DatabaseHelper dbHelper;
    private FeeStatusAdapter adapter;

    private List<Student> allStudents; // This will now contain both active and graduated students
    private Map<Integer, FeePayment> studentPaymentMap;

    private boolean isUpdatingAll = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_fees_status);

        dbHelper = new DatabaseHelper(this);

        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        rgFilter = findViewById(R.id.rgFilter);
        rgStudentStatusFilter = findViewById(R.id.rgStudentStatusFilter);
        lvFeeStatus = findViewById(R.id.lvFeeStatus);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        etSearchStudent = findViewById(R.id.etSearchStudent);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvSelectedSemester = findViewById(R.id.tvSelectedSemester);

        setupSpinners();
        // Load all students (active and graduated) once
        allStudents = dbHelper.getAllStudents();

        AdapterView.OnItemSelectedListener refreshListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getId() == R.id.spinnerMonth) {
                    tvSelectedMonth.setText(parent.getItemAtPosition(position).toString());
                } else if (parent.getId() == R.id.spinnerSemester) {
                    tvSelectedSemester.setText(parent.getItemAtPosition(position).toString());
                }
                refreshFeeStatusList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        };

        spinnerMonth.setOnItemSelectedListener(refreshListener);
        spinnerSemester.setOnItemSelectedListener(refreshListener);
        rgFilter.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                refreshFeeStatusList();
            }
        });
        rgStudentStatusFilter.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                refreshFeeStatusList();
            }
        });

        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingAll) {
                updateAllStudentsStatus(isChecked);
            }
        });

        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshFeeStatusList();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        refreshFeeStatusList();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(this,
                R.array.months, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(DateUtils.getCurrentMonth());
        tvSelectedMonth.setText(monthAdapter.getItem(DateUtils.getCurrentMonth()));

        ArrayAdapter<CharSequence> semesterAdapter = ArrayAdapter.createFromResource(this,
                R.array.semesters, android.R.layout.simple_spinner_item);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);
        spinnerSemester.setSelection(0);
        tvSelectedSemester.setText(semesterAdapter.getItem(0));
    }

    private void refreshFeeStatusList() {
        int selectedYear = DateUtils.getCurrentYear();
        int selectedMonth = spinnerMonth.getSelectedItemPosition();
        int selectedSemester = spinnerSemester.getSelectedItemPosition() + 1; // 1-indexed semester
        String searchQuery = etSearchStudent.getText().toString().trim().toLowerCase(Locale.getDefault());

        String feeFilterStatus = Constants.STATUS_ALL;
        int checkedFeeFilterId = rgFilter.getCheckedRadioButtonId();
        if (checkedFeeFilterId == R.id.rbFilterPaid) {
            feeFilterStatus = Constants.STATUS_PAID;
        } else if (checkedFeeFilterId == R.id.rbFilterUnpaid) {
            feeFilterStatus = Constants.STATUS_UNPAID;
        }

        String studentStatusFilter = Constants.STATUS_ACTIVE;
        int checkedStudentStatusFilterId = rgStudentStatusFilter.getCheckedRadioButtonId();
        if (checkedStudentStatusFilterId == R.id.rbStatusGraduated) {
            studentStatusFilter = Constants.STATUS_GRADUATED;
        } else if (checkedStudentStatusFilterId == R.id.rbStatusAll) {
            studentStatusFilter = Constants.STATUS_ALL;
        }


        List<Student> studentsToDisplay = new ArrayList<>();
        studentPaymentMap = new HashMap<>();

        List<FeePayment> paymentsForPeriod = dbHelper.getFeePaymentsByPeriodAndStatus(selectedYear, selectedMonth, selectedSemester, Constants.STATUS_ALL);

        Map<Integer, FeePayment> currentPaymentsMap = new HashMap<>();
        for (FeePayment payment : paymentsForPeriod) {
            currentPaymentsMap.put(payment.getStudentId(), payment);
        }

        boolean allStudentsPaid = true;

        for (Student student : allStudents) {
            // 1. Filter by student status (Active/Graduated/All)
            if (!studentStatusFilter.equals(Constants.STATUS_ALL) && !student.getStatus().equalsIgnoreCase(studentStatusFilter)) {
                continue;
            }

            // 2. Filter by selected semester (using student's current_semester directly)
            // This is the key change to correctly filter by the student's current enrollment
            if (student.getCurrentSemester() != selectedSemester) {
                continue;
            }

            // 3. Filter by search query
            if (!searchQuery.isEmpty() && !student.getName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                continue;
            }

            // 4. Apply fee payment status filter (Paid/Unpaid/All)
            FeePayment payment = currentPaymentsMap.get(student.getStudentId());
            boolean isPaid = (payment != null && Constants.STATUS_PAID.equals(payment.getStatus()));

            boolean shouldAdd = false;
            if (feeFilterStatus.equals(Constants.STATUS_ALL)) {
                shouldAdd = true;
            } else if (feeFilterStatus.equals(Constants.STATUS_PAID)) {
                if (isPaid) {
                    shouldAdd = true;
                }
            } else if (feeFilterStatus.equals(Constants.STATUS_UNPAID)) {
                if (!isPaid) {
                    shouldAdd = true;
                }
            }

            if (shouldAdd) {
                studentsToDisplay.add(student);
                if (!isPaid) {
                    allStudentsPaid = false;
                }
                studentPaymentMap.put(student.getStudentId(), payment);
            }
        }

        Collections.sort(studentsToDisplay, new Comparator<Student>() {
            @Override
            public int compare(Student s1, Student s2) {
                return s1.getName().compareToIgnoreCase(s2.getName());
            }
        });

        adapter = new FeeStatusAdapter(this, studentsToDisplay, studentPaymentMap, selectedYear, selectedMonth, selectedSemester);
        adapter.setFeeStatusUpdateListener(this);
        lvFeeStatus.setAdapter(adapter);

        isUpdatingAll = true;
        cbSelectAll.setChecked(allStudentsPaid && !studentsToDisplay.isEmpty());
        cbSelectAll.setEnabled(!studentsToDisplay.isEmpty());
        isUpdatingAll = false;
    }

    private void updateAllStudentsStatus(boolean markAsPaid) {
        int selectedYear = DateUtils.getCurrentYear();
        int selectedMonth = spinnerMonth.getSelectedItemPosition();
        int selectedSemester = spinnerSemester.getSelectedItemPosition() + 1;

        String status = markAsPaid ? Constants.STATUS_PAID : Constants.STATUS_UNPAID;
        String paymentDate = markAsPaid ? DateUtils.getCurrentDate() : "";

        List<Student> currentDisplayedStudents = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            currentDisplayedStudents.add(adapter.getItem(i));
        }

        if (currentDisplayedStudents.isEmpty()) {
            Toast.makeText(this, "No students to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Student student : currentDisplayedStudents) {
            // When updating payment status, we still need the correct semester for the payment record.
            // This semester for payment should be the one the student was *actually* in during that month/year,
            // which is correctly determined by getActiveSemesterForStudent.
            int semesterForPayment = dbHelper.getActiveSemesterForStudent(student.getStudentId(), selectedYear, selectedMonth);

            dbHelper.updateOrInsertFeePaymentStatus(
                    student.getStudentId(),
                    DateUtils.getMonthYearString(selectedMonth, selectedYear),
                    semesterForPayment,
                    status,
                    0.0,
                    paymentDate
            );
        }
        Toast.makeText(this, "All displayed students marked as " + status + "!", Toast.LENGTH_SHORT).show();
        refreshFeeStatusList();
    }

    @Override
    public void onFeeStatusUpdated() {
        refreshFeeStatusList();
    }
}
