package com.aquaa.edupay.adapters;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aquaa.edupay.R;
import com.aquaa.edupay.models.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromoteStudentAdapter extends ArrayAdapter<Student> {

    private Context context;
    private List<Student> students;
    private SparseBooleanArray itemCheckedStates; // Stores checked state by position
    private OnStudentCheckedChangeListener listener;

    // Interface for callback to the Activity
    public interface OnStudentCheckedChangeListener {
        void onStudentCheckedChanged(int position, boolean isChecked);
        void onAllStudentsCheckedStateChanged(); // New callback for "Select All" logic
    }

    public void setOnStudentCheckedChangeListener(OnStudentCheckedChangeListener listener) {
        this.listener = listener;
    }

    public PromoteStudentAdapter(@NonNull Context context, List<Student> students) {
        super(context, 0, students);
        this.context = context;
        this.students = students;
        this.itemCheckedStates = new SparseBooleanArray();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_promote_student, parent, false);
        }

        Student currentStudent = students.get(position);

        TextView tvStudentName = listItem.findViewById(R.id.tvPromoteStudentName);
        CheckBox cbPromoteStudent = listItem.findViewById(R.id.cbPromoteStudent);

        tvStudentName.setText(String.format(Locale.getDefault(), "%s (Sem %d)",
                currentStudent.getName(), currentStudent.getCurrentSemester()));

        // Set the checkbox state based on stored states
        cbPromoteStudent.setChecked(itemCheckedStates.get(position, false));

        // Set OnCheckedChangeListener
        cbPromoteStudent.setOnCheckedChangeListener(null); // Clear previous listener to prevent unwanted triggers
        cbPromoteStudent.setOnClickListener(v -> {
            boolean isChecked = ((CheckBox) v).isChecked();
            itemCheckedStates.put(position, isChecked);
            if (listener != null) {
                listener.onStudentCheckedChanged(position, isChecked);
                listener.onAllStudentsCheckedStateChanged(); // Notify activity to check "Select All" state
            }
        });

        return listItem;
    }

    /**
     * Sets the checked state for all visible students.
     * @param isChecked True to check all, false to uncheck all.
     */
    public void setAllChecked(boolean isChecked) {
        itemCheckedStates.clear(); // Clear existing states
        for (int i = 0; i < students.size(); i++) {
            itemCheckedStates.put(i, isChecked);
        }
        notifyDataSetChanged(); // Refresh the list view
    }

    /**
     * Gets a list of students whose checkboxes are currently checked.
     * @return List of selected Student objects.
     */
    public List<Student> getSelectedStudents() {
        List<Student> selected = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            if (itemCheckedStates.get(i, false)) { // Get state, default to false if not found
                selected.add(students.get(i));
            }
        }
        return selected;
    }

    /**
     * Checks if all students in the current list are checked.
     * @return True if all are checked, false otherwise.
     */
    public boolean areAllStudentsChecked() {
        if (students.isEmpty()) {
            return false; // If no students, "Select All" should be unchecked
        }
        for (int i = 0; i < students.size(); i++) {
            if (!itemCheckedStates.get(i, false)) {
                return false; // Found at least one unchecked
            }
        }
        return true; // All are checked
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        // After data set changes (e.g., filtering), reset checked states for new items
        // or ensure existing checked items are still valid.
        // For simplicity, we'll clear and re-evaluate if needed by the activity.
        // The activity will call setAllChecked(false) or update individually.
    }

    public void clearCheckedStates() {
        itemCheckedStates.clear();
        notifyDataSetChanged();
    }
}
