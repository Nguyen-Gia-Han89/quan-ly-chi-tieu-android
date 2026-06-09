package com.example.quanlychitieu.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import com.example.quanlychitieu.model.Transaction;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportExporter {

    public static void exportToCSV(Context context, List<Transaction> list) {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {

                // tạo tên file theo thời gian
                String timeStamp = new SimpleDateFormat("MM_yyyy_HH_mm_ss", Locale.getDefault())
                        .format(new Date());

                String fileName = "report_" + timeStamp + ".csv";

                // lưu vào thư mục Documents
                File folder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (folder != null && !folder.exists()) {
                    folder.mkdirs();
                }

                File file = new File(folder, fileName);

                FileWriter writer = new FileWriter(file);

                // header CSV
                writer.append("Type,Amount,Category,Date,Note\n");

                if (list != null) {
                    for (Transaction t : list) {
                        writer.append(String.valueOf(t.getType())).append(",")
                                .append(String.valueOf(t.getAmount())).append(",")
                                .append(String.valueOf(t.getCategory())).append(",")
                                .append(String.valueOf(t.getDate())).append(",")
                                .append(String.valueOf(t.getNote())).append("\n");
                    }
                }

                writer.flush();
                writer.close();

                // lưu lịch sử file đã export
                SharedPreferences pref = context.getSharedPreferences(
                        "REPORT_HISTORY",
                        Context.MODE_PRIVATE
                );

                Set<String> files = pref.getStringSet("files", new HashSet<>());

                // clone set để tránh lỗi immutable set
                Set<String> newFiles = new HashSet<>(files);
                newFiles.add(fileName);

                pref.edit()
                        .putStringSet("files", newFiles)
                        .apply();

                // thông báo UI
                new Handler(context.getMainLooper()).post(() ->
                        Toast.makeText(
                                context,
                                "Xuất báo cáo thành công\n" + file.getAbsolutePath(),
                                Toast.LENGTH_LONG
                        ).show());

            } catch (Exception e) {
                e.printStackTrace();

                new Handler(context.getMainLooper()).post(() ->
                        Toast.makeText(
                                context,
                                "Xuất báo cáo thất bại!",
                                Toast.LENGTH_SHORT
                        ).show());
            }
        });
    }
}