package com.example.quanlychitieu.controller;

import android.os.Handler;
import android.os.Looper;

import com.example.quanlychitieu.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportController {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onDone(double income, double expense,
                    String trend, List<CategoryReport> categories);
    }

    public static class CategoryReport {
        public String category;
        public double total;
        public String type;

        public CategoryReport(String category, double total, String type) {
            this.category = category;
            this.total = total;
            this.type = type;
        }
    }

    public void generate(List<Transaction> list, Callback callback) {
        executor.execute(() -> {
            double income = 0;
            double expense = 0;

            Map<String, Double> map = new HashMap<>();

            if (list != null) {
                for (Transaction t : list) {

                    if (t == null || t.getType() == null) continue;
                    double amount = t.getAmount();
                    String type = t.getType();

                    if ("INCOME".equals(type)) {
                        income += amount;
                    } else if ("EXPENSE".equals(type)) {
                        expense += amount;
                    }

                    String category = (t.getCategory() == null || t.getCategory().trim().isEmpty())
                            ? "Khác"
                            : t.getCategory().trim();

                    String compositeKey = category + "_" + type;

                    map.put(compositeKey, map.getOrDefault(compositeKey, 0.0) + amount);
                }
            }

            List<CategoryReport> result = new ArrayList<>();
            for (Map.Entry<String, Double> e : map.entrySet()) {
                String compositeKey = e.getKey();
                double totalAmount = e.getValue();

                String[] parts = compositeKey.split("_");
                String categoryName = parts[0];
                String categoryType = parts.length > 1 ? parts[1] : "EXPENSE";

                result.add(new CategoryReport(categoryName, totalAmount, categoryType));
            }

            String trend;
            if (expense > income) {
                trend = "Chi vượt thu";
            } else if (expense > income * 0.7) {
                trend = "Chi cao";
            } else {
                trend = "Ổn định";
            }

            double fIncome = income;
            double fExpense = expense;
            String fTrend = trend;
            List<CategoryReport> fResult = result;

            handler.post(() -> callback.onDone(fIncome, fExpense, fTrend, fResult));
        });
    }
}