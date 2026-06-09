package com.example.quanlychitieu.view;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CsvViewerActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csv_viewer);

        rv = findViewById(R.id.rvCsv);
        rv.setLayoutManager(new LinearLayoutManager(this));

        btnBack = findViewById(R.id.btnBackCsv);

        btnBack.setOnClickListener(v -> finish());

        String path = getIntent().getStringExtra("file_path");

        if (path != null) {
            loadCsv(path);
        } else {
            Toast.makeText(this, "Không tìm thấy file", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCsv(String path) {

        List<String[]> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.toLowerCase().contains("date") ||
                        line.toLowerCase().contains("ngày")) {
                    continue;
                }

                String[] parts = line.split(",");
                list.add(parts);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi đọc CSV", Toast.LENGTH_SHORT).show();
        }

        rv.setAdapter(new CsvAdapter(list));
    }
}