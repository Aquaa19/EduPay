package com.aquaa.edupay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SeekBar; // Import SeekBar
import android.widget.TextView; // Import TextView
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.aquaa.edupay.database.DatabaseHelper;
import com.aquaa.edupay.models.Student;
import com.aquaa.edupay.utils.DateUtils;
import com.aquaa.edupay.utils.FeeTrackingManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Apache POI imports for Excel
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Iterator;
import android.database.Cursor;

public class AddStudentActivity extends AppCompatActivity {

    private static final String TAG = "AddStudentActivity";

    private EditText etStudentName, etMobileNumber, etGuardianMobileNumber;
    private RadioGroup rgGender;
    private Spinner spinnerMonthOfJoining;
    private SeekBar seekBarSemester;
    private TextView tvSemesterLabel;
    private Button btnSaveStudent, btnImportStudents, btnExportStudents;

    private DatabaseHelper dbHelper;
    private FeeTrackingManager feeTrackingManager;

    private int selectedSemester = 1;

    private ActivityResultLauncher<Intent> importStudentsLauncher;
    private ActivityResultLauncher<Intent> exportStudentsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        dbHelper = new DatabaseHelper(this);
        feeTrackingManager = new FeeTrackingManager(this);

        etStudentName = findViewById(R.id.etStudentName);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etGuardianMobileNumber = findViewById(R.id.etGuardianMobileNumber);
        rgGender = findViewById(R.id.rgGender);
        spinnerMonthOfJoining = findViewById(R.id.spinnerMonthOfJoining);
        seekBarSemester = findViewById(R.id.seekBarSemester);
        tvSemesterLabel = findViewById(R.id.tvSemesterLabel);
        btnSaveStudent = findViewById(R.id.btnSaveStudent);
        btnImportStudents = findViewById(R.id.btnImportStudents);
        btnExportStudents = findViewById(R.id.btnExportStudents);

        setupSemesterSlider();
        setClickListeners();
        setupImportExportLaunchers();
    }

    private void setupSemesterSlider() {
        tvSemesterLabel.setText("Current Semester: " + selectedSemester);

        seekBarSemester.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedSemester = progress + 1;
                tvSemesterLabel.setText("Current Semester: " + selectedSemester);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setClickListeners() {
        btnSaveStudent.setOnClickListener(v -> saveStudent());
        btnImportStudents.setOnClickListener(v -> openFilePickerForImport());
        btnExportStudents.setOnClickListener(v -> openFileCreatorForExport());
    }

    private void setupImportExportLaunchers() {
        importStudentsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleImportFile(uri);
                        }
                    } else {
                        Toast.makeText(this, "Import cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        exportStudentsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            exportStudentsToUri(uri);
                        }
                    } else {
                        Toast.makeText(this, "Export cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openFilePickerForImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        String[] mimeTypes = {
                "text/csv",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        };

        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.setType("*/*");

        try {
            importStudentsLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker for import: " + e.getMessage());
            Toast.makeText(this, "Could not open file picker. Please ensure a file manager is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void openFileCreatorForExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "students_export_" + DateUtils.getCurrentDate() + ".csv");
        try {
            exportStudentsLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file creator for export: " + e.getMessage());
            Toast.makeText(this, "Could not open file creator. Please ensure a file manager is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleImportFile(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "Invalid file URI.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = null;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name from URI: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (fileName != null) {
            if (fileName.toLowerCase(Locale.getDefault()).endsWith(".csv")) {
                importStudentsFromCsv(uri);
            } else if (fileName.toLowerCase(Locale.getDefault()).endsWith(".xlsx")) {
                importStudentsFromExcel(uri);
            } else {
                Toast.makeText(this, "Unsupported file type. Please select a .csv or .xlsx file.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Could not determine file name or type. Please select a valid file.", Toast.LENGTH_LONG).show();
        }
    }

    private void importStudentsFromCsv(Uri uri) {
        new Thread(() -> {
            int importedCount = 0;
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        if (line.startsWith("Name,Gender,Mobile,GuardianMobile,MonthOfJoining,YearOfJoining,CurrentSemester")) {
                            continue;
                        }
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 7) {
                        try {
                            String name = parts[0].trim();
                            String gender = parts[1].trim();
                            String mobile = parts[2].trim();
                            String guardianMobile = parts[3].trim();
                            String monthOfJoiningStr = parts[4].trim();
                            int yearOfJoining = Integer.parseInt(parts[5].trim());
                            int currentSemester = Integer.parseInt(parts[6].trim());

                            int monthOfJoining = DateUtils.getMonthIndex(monthOfJoiningStr);
                            if (monthOfJoining == -1) {
                                Log.w(TAG, "Invalid month name in CSV: " + monthOfJoiningStr + " for student: " + name);
                                continue;
                            }

                            Student student = new Student();
                            student.setName(name);
                            student.setGender(gender);
                            student.setMobileNumber(mobile);
                            student.setGuardianMobileNumber(guardianMobile);
                            student.setMonthOfJoining(monthOfJoiningStr);
                            student.setYearOfJoining(yearOfJoining);
                            student.setCurrentSemester(currentSemester);
                            student.setStatus("active"); // Set default status for imported students

                            long studentId = dbHelper.addStudent(student);
                            if (studentId != -1) {
                                feeTrackingManager.initializeStudentFeeRecords((int) studentId, monthOfJoiningStr, yearOfJoining);
                                importedCount++;
                            } else {
                                Log.e(TAG, "Failed to add student from CSV: " + name);
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Number format error in CSV row: " + line + " - " + e.getMessage());
                        }
                    } else {
                        Log.w(TAG, "Skipping malformed CSV row (not enough parts): " + line);
                    }
                }

                int finalImportedCount = importedCount;
                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this,
                        finalImportedCount + " students imported successfully from CSV!", Toast.LENGTH_SHORT).show());

            } catch (IOException e) {
                Log.e(TAG, "Error importing CSV file: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this, "Failed to import CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during CSV import: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this, "Unexpected error during CSV import: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void importStudentsFromExcel(Uri uri) {
        new Thread(() -> {
            int importedCount = 0;
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                if (rowIterator.hasNext()) {
                    rowIterator.next();
                }

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (isRowEmpty(row)) {
                        continue;
                    }

                    try {
                        String name = getCellValueAsString(row.getCell(0));
                        String gender = getCellValueAsString(row.getCell(1));
                        String mobile = getCellValueAsString(row.getCell(2));
                        String guardianMobile = getCellValueAsString(row.getCell(3));
                        String monthOfJoiningStr = getCellValueAsString(row.getCell(4));
                        int yearOfJoining = (int) getCellValueAsNumeric(row.getCell(5));

                        int currentSemester = 1;
                        try {
                            currentSemester = (int) getCellValueAsNumeric(row.getCell(6));
                        } catch (Exception e) {
                            Log.w(TAG, "Could not parse CurrentSemester for student " + name + ". Defaulting to 1. Error: " + e.getMessage());
                        }

                        if (name.isEmpty() || mobile.isEmpty()) {
                            Log.w(TAG, "Skipping Excel row " + row.getRowNum() + " due to missing name or mobile.");
                            continue;
                        }

                        int monthOfJoiningIndex = DateUtils.getMonthIndex(monthOfJoiningStr);
                        if (monthOfJoiningIndex == -1) {
                            Log.w(TAG, "Invalid month name in Excel: " + monthOfJoiningStr + " for student: " + name + " at row: " + row.getRowNum());
                            continue;
                        }

                        Student student = new Student();
                        student.setName(name);
                        student.setGender(gender);
                        student.setMobileNumber(mobile);
                        student.setGuardianMobileNumber(guardianMobile);
                        student.setMonthOfJoining(monthOfJoiningStr);
                        student.setYearOfJoining(yearOfJoining);
                        student.setCurrentSemester(currentSemester);
                        student.setStatus("active"); // Set default status for imported students

                        long studentId = dbHelper.addStudent(student);
                        if (studentId != -1) {
                            feeTrackingManager.initializeStudentFeeRecords((int) studentId, monthOfJoiningStr, yearOfJoining);
                            importedCount++;
                        } else {
                            Log.e(TAG, "Failed to add student from Excel: " + name);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Excel row " + row.getRowNum() + ": " + e.getMessage(), e);
                    }
                }
                int finalImportedCount = importedCount;
                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this,
                        finalImportedCount + " students imported successfully from Excel!", Toast.LENGTH_LONG).show());

            } catch (IOException e) {
                Log.e(TAG, "Error importing Excel file: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this, "Failed to import Excel file: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return DateUtils.getMonthName(cell.getDateCellValue().getMonth());
            } else {
                return String.valueOf((long) cell.getNumericCellValue()).trim();
            }
        } else if (cellType == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue()).trim();
        } else if (cellType == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (IllegalStateException e) {
                return String.valueOf((long) cell.getNumericCellValue()).trim();
            }
        }
        return "";
    }

    private double getCellValueAsNumeric(Cell cell) {
        if (cell == null) {
            return 0.0;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Cannot parse string to numeric: " + cell.getStringCellValue());
                return 0.0;
            }
        }
        return 0.0;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }


    private void saveStudent() {
        String name = etStudentName.getText().toString().trim();
        String mobile = etMobileNumber.getText().toString().trim();
        String guardianMobile = etGuardianMobileNumber.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        RadioButton selectedGenderRadioButton = findViewById(selectedGenderId);
        String gender = selectedGenderRadioButton.getText().toString();

        String monthOfJoining = spinnerMonthOfJoining.getSelectedItem().toString();
        int yearOfJoining = DateUtils.getCurrentYear();

        if (name.isEmpty() || mobile.isEmpty() || guardianMobile.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Student student = new Student();
        student.setName(name);
        student.setGender(gender);
        student.setMobileNumber(mobile);
        student.setGuardianMobileNumber(guardianMobile);
        student.setCurrentSemester(selectedSemester);
        student.setMonthOfJoining(monthOfJoining);
        student.setYearOfJoining(yearOfJoining);
        student.setStatus("active"); // Explicitly set status to "active" for manually added students

        long studentId = dbHelper.addStudent(student);

        if (studentId != -1) {
            Toast.makeText(this, "Student added successfully!", Toast.LENGTH_SHORT).show();
            feeTrackingManager.initializeStudentFeeRecords((int) studentId, monthOfJoining, yearOfJoining);
            finish();
        } else {
            Toast.makeText(this, "Failed to add student.", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportStudentsToUri(Uri uri) {
        new Thread(() -> {
            List<Student> studentsToExport = dbHelper.getAllStudents();
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {

                writer.write("Name,Gender,Mobile,GuardianMobile,MonthOfJoining,YearOfJoining,CurrentSemester,Status\n"); // Updated header

                for (Student student : studentsToExport) {
                    String line = String.format(Locale.getDefault(), "%s,%s,%s,%s,%s,%d,%d,%s\n", // Added %s for status
                            escapeCsv(student.getName()),
                            escapeCsv(student.getGender()),
                            escapeCsv(student.getMobileNumber()),
                            escapeCsv(student.getGuardianMobileNumber()),
                            escapeCsv(student.getMonthOfJoining()),
                            student.getYearOfJoining(),
                            student.getCurrentSemester(),
                            escapeCsv(student.getStatus()) // Added status to export
                    );
                    writer.write(line);
                }

                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this,
                        studentsToExport.size() + " students exported successfully!", Toast.LENGTH_SHORT).show());

            } catch (IOException e) {
                Log.e(TAG, "Error exporting students: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this, "Failed to export students: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during export: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(AddStudentActivity.this, "Unexpected error during export: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
