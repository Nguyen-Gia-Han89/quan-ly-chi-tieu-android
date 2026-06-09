package com.example.quanlychitieu.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quanlychitieu.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.content.Intent;

public class ReportHistoryActivity extends AppCompatActivity {

    private ListView listView;
    private ImageView btnBack;
    private List<String> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_history);

        listView = findViewById(R.id.listViewReports);
        btnBack = findViewById(R.id.btnBackReportHistory);

        btnBack.setOnClickListener(v -> finish());

        loadFiles();
    }

    private void loadFiles() {

        SharedPreferences pref = getSharedPreferences("REPORT_HISTORY", MODE_PRIVATE);
        Set<String> filesSet = pref.getStringSet("files", new HashSet<>());

        fileList = new ArrayList<>(filesSet);

        if (fileList.isEmpty()) {
            Toast.makeText(this, "Chưa có báo cáo nào", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                fileList);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {

            String fileName = fileList.get(position);

            File file = new File(
                    getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    fileName);

            if (!file.exists()) {
                Toast.makeText(this, "File không tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, CsvViewerActivity.class);
            intent.putExtra("file_path", file.getAbsolutePath());
            startActivity(intent);
        });
    }
}