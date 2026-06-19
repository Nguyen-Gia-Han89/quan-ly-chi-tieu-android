package com.example.quanlychitieu.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.SavingGoalController;
import com.example.quanlychitieu.controller.TransactionController;
import com.example.quanlychitieu.model.SavingGoal;
import com.example.quanlychitieu.model.Transaction;
import java.text.NumberFormat;
import java.util.*;

public class SavingGoalActivity extends AppCompatActivity {

    private TextView tvTotalTarget, tvTotalPercent, tvTotalRemaining, tvTotalSpent, tvTitle;
    private ProgressBar progressTotal;
    private RecyclerView recyclerView;
    private SavingGoalAdapter adapter;
    private ImageButton btnAddGoal, btnBack, btnPrevMonth, btnNextMonth;
    private SavingGoalController controller;
    private TransactionController transactionController;
    private Calendar selectedMonth;
    private double totalTargetValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saving_goal);

        initViews();

        controller = new SavingGoalController();
        transactionController = new TransactionController();
        selectedMonth = Calendar.getInstance();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavingGoalAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnPrevMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, -1);
            loadGoals();
        });

        btnNextMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, 1);
            loadGoals();
        });

        btnAddGoal.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSavingGoalActivity.class);
            intent.putExtra("monthKey", getMonthKey());
            startActivity(intent);
        });
        loadGoals();
    }

    private void initViews() {
        tvTotalTarget = findViewById(R.id.tvTotalTarget);
        tvTotalPercent = findViewById(R.id.tvTotalPercent);
        tvTotalRemaining = findViewById(R.id.tvTotalRemaining);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvTitle = findViewById(R.id.tvTitle);

        progressTotal = findViewById(R.id.progressTotal);
        recyclerView = findViewById(R.id.recyclerView);
        btnAddGoal = findViewById(R.id.btnAddGoal);

        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        progressTotal.getProgressDrawable().setTint(Color.parseColor("#5E17EB"));
    }

    private void loadGoals() {
        getCurrentMonthYear();
        String monthKey = getMonthKey();
        String monthYear = monthKey.replace("_", "/");

        controller.getSavingGoals(monthKey, new SavingGoalController.SavingGoalCallback() {
            @Override
            public void onLoaded(List<SavingGoal> goals) {
                if (goals == null) goals = new ArrayList<>();

                adapter.updateData(goals);

                totalTargetValue = 0;
                for (SavingGoal g : goals) {
                    totalTargetValue += g.getTargetAmount();
                }

                final List<SavingGoal> finalGoals = goals;

                transactionController.getTransactionsByMonth(monthYear,
                        new TransactionController.TransactionListCallback() {
                            @Override
                            public void onLoaded(List<Transaction> transactions, double totalIncome, double totalExpense) {
                                Map<String, Double> spentMap = new HashMap<>();

                                // Gom nhóm tổng chi tiêu thực tế theo từng danh mục
                                for (Transaction t : transactions) {
                                    if ("EXPENSE".equalsIgnoreCase(t.getType())) {
                                        String key = normalize(t.getCategory());
                                        double amount = t.getAmount();

                                        double oldAmount = spentMap.containsKey(key) ? spentMap.get(key) : 0;
                                        spentMap.put(key, oldAmount + amount);
                                    }
                                }

                                // Gửi bản đồ chi tiêu thực tế đã nhóm xuống Adapter
                                adapter.updateSpentMap(spentMap);

                                // LOGIC TÍNH TỔNG CHI TIÊU THỰC TẾ TRÊN TOÀN BỘ MỤC TIÊU
                                double totalTargetSpent = 0;
                                for (SavingGoal g : finalGoals) {
                                    String goalKey = normalize(g.getCategoryName());
                                    if (spentMap.containsKey(goalKey)) {
                                        totalTargetSpent += spentMap.get(goalKey);
                                    }
                                }

                                // Cập nhật UI với tổng ngân sách và tổng chi tiêu thực tế của các mục tiêu
                                updateUI(totalTargetValue, totalTargetSpent);
                            }

                            @Override
                            public void onFailure(String message) {
                                Toast.makeText(SavingGoalActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(SavingGoalActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(double totalTarget, double totalSpent) {
        NumberFormat f = NumberFormat.getInstance(new Locale("vi", "VN"));
        double remaining = totalTarget - totalSpent;
        tvTotalTarget.setText("Ngân sách: " + f.format(totalTarget) + " đ");
        tvTotalSpent.setText("Chi tiêu: " + f.format(totalSpent) + " đ");
        tvTotalRemaining.setText("Còn lại: " + f.format(remaining) + " đ");

        int percent = totalTarget > 0 ? (int) ((totalSpent * 100) / totalTarget) : 0;
        progressTotal.setProgress(percent);
        tvTotalPercent.setText(percent + "%");
    }

    private String normalize(String s) {
        return (s == null) ? "" : s.trim().toLowerCase();
    }

    private String getMonthKey() {
        int m = selectedMonth.get(Calendar.MONTH) + 1;
        int y = selectedMonth.get(Calendar.YEAR);
        return String.format("%02d_%d", m, y);
    }

    private String getCurrentMonthYear() {
        int m = selectedMonth.get(Calendar.MONTH) + 1;
        int y = selectedMonth.get(Calendar.YEAR);
        tvTitle.setText("Tổng ngân sách tháng " + m + "/" + y);
        return "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGoals();
    }
}