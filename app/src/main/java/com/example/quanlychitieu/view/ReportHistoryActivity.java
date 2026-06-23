package com.example.quanlychitieu.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quanlychitieu.R;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ReportHistoryActivity extends AppCompatActivity {
    private ListView listView;
    private ImageView btnBack;
    private TextView txtTotalReports, txtLatestReport;
    private Button btnSort;
    private Button btnDelete;
    private TextView btnSelectMonth;
    private List<String> fileList = new ArrayList<>();
    private List<String> allFiles = new ArrayList<>();
    private List<String> selectedFiles = new ArrayList<>();
    private boolean isDeleteMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_history);

        initViews();
        setListeners();
        loadFiles();
    }

    private void initViews() {
        listView = findViewById(R.id.listViewReports);
        btnBack = findViewById(R.id.btnBackReportHistory);
        txtTotalReports = findViewById(R.id.txtTotalReports);
        txtLatestReport = findViewById(R.id.txtLatestReport);
        btnSort = findViewById(R.id.btnSort);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSort.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);

            popup.getMenu().add("Gần nhất");
            popup.getMenu().add("Xa nhất");
            popup.getMenu().add("Tất cả");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getTitle().toString()) {
                    case "Gần nhất":
                        sortNewestFirst();
                        break;
                    case "Xa nhất":
                        sortOldestFirst();
                        break;
                    case "Tất cả":
                        resetFilter();
                        break;
                }
                bindList();
                return true;
            });
            popup.show();
        });

        // nút xoá
        btnDelete.setOnClickListener(v -> {

            // TH1: chưa vào delete mode
            if (!isDeleteMode) {
                isDeleteMode = true;
                selectedFiles.clear();
                bindList();
                return;
            }

            // TH2: đang delete mode nhưng chưa chọn file
            if (selectedFiles.isEmpty()) {
                isDeleteMode = false;
                selectedFiles.clear();
                bindList();
                return;
            }

            // TH3: đang delete mode + có chọn file
            showDeleteDialog();
        });
    }

    private void loadFiles() {
        SharedPreferences pref =
                getSharedPreferences("REPORT_HISTORY", MODE_PRIVATE);

        Set<String> set = pref.getStringSet("files", new HashSet<>());

        allFiles = new ArrayList<>(set);
        fileList = new ArrayList<>(allFiles);

        sortNewestFirst();

        txtTotalReports.setText(String.valueOf(fileList.size()));
        showLatestReport();

        if (fileList.isEmpty()) {
            Toast.makeText(this, "Chưa có báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        bindList();
    }

    //sắp xêp
    private void sortNewestFirst() {
        Collections.sort(fileList, (a, b) ->
                extractDate(b).compareTo(extractDate(a)));
    }

    private void sortOldestFirst() {
        Collections.sort(fileList, (a, b) ->
                extractDate(a).compareTo(extractDate(b)));
    }

    private void resetFilter() {
        fileList = new ArrayList<>(allFiles);
        sortNewestFirst();
        txtTotalReports.setText(String.valueOf(fileList.size()));
        bindList();
    }

    private void bindList() {
        if (isDeleteMode) {

            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            DeleteAdapter adapter = new DeleteAdapter();
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {

                String file = fileList.get(position);

                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file);
                } else {
                    selectedFiles.add(file);
                }

                listView.setItemChecked(position, selectedFiles.contains(file));
            });

    } else {

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    fileList);

            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {

                String fileName = fileList.get(position);

                File file = new File(
                        getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
                        fileName);

                if (!file.exists()) return;

                Intent intent = new Intent(this, CsvViewerActivity.class);
                intent.putExtra("file_path", file.getAbsolutePath());
                startActivity(intent);
            });
        }
    }

    // delete dialog
    private void showDeleteDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Xoá báo cáo")
                .setMessage("Bạn có chắc muốn xoá " + selectedFiles.size() + " file?")
                .setPositiveButton("Xoá", (d, w) -> deleteFiles())

                .setNegativeButton("Huỷ", (d, w) -> {

                    // HUỶ -> thoát delete mode
                    isDeleteMode = false;
                    selectedFiles.clear();
                    bindList();

                    d.dismiss();
                })
                .show();
    }

    private void deleteFiles() {

        File folder = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);

        SharedPreferences pref =
                getSharedPreferences("REPORT_HISTORY", MODE_PRIVATE);

        Set<String> saved = new HashSet<>(pref.getStringSet("files", new HashSet<>()));

        for (String f : selectedFiles) {

            File file = new File(folder, f);

            if (file.exists()) file.delete();

            saved.remove(f);
        }

        pref.edit().putStringSet("files", saved).apply();

        Toast.makeText(this, "Đã xoá", Toast.LENGTH_SHORT).show();

        isDeleteMode = false;
        selectedFiles.clear();

        loadFiles();
    }

    private void showLatestReport() {

        if (fileList.isEmpty()) return;

        String latest = fileList.get(0)
                .replace("report_", "")
                .replace(".csv", "");

        String[] p = latest.split("_");

        if (p.length >= 5) {
            txtLatestReport.setText(
                    p[0] + "/" + p[1] + "/" + p[2] +
                            " " + p[3] + ":" + p[4]
            );
        }
    }

    //date
    private Date extractDate(String fileName) {

        try {
            String clean = fileName
                    .replace("report_", "")
                    .replace(".csv", "");

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.getDefault());

            return sdf.parse(clean);

        } catch (Exception e) {
            return new Date(0);
        }
    }

    //delete adapter
    private class DeleteAdapter extends ArrayAdapter<String> {
        DeleteAdapter() {
            super(ReportHistoryActivity.this,
                    android.R.layout.simple_list_item_multiple_choice,
                    fileList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            String file = fileList.get(position);
            TextView text = v.findViewById(android.R.id.text1);
            text.setText(file);
            listView.setItemChecked(position, selectedFiles.contains(file));

            return v;
        }
    }
}