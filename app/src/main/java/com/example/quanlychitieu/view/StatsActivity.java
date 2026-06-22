package com.example.quanlychitieu.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.ReportController;
import com.example.quanlychitieu.model.Transaction;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    private ImageView btnBackStats;
    private ImageView btnSwitchToMonthly;
    private PieChart pieChart;
    private TextView txtNoData;
    private RecyclerView rvStatsCategories;

    private LinearLayout layoutLeftContainer;
    private LinearLayout layoutRightContainer;

    private ImageButton btnPreviousMonth, btnNextMonth;
    private TextView txtCurrentMonthYear, txtChartTitle;
    private LinearLayout tabExpense, tabIncome;
    private TextView txtTabTotalExpense, txtTabTotalIncome;

    private CategoryStatsAdapter statsAdapter;
    private ReportController reportController;

    private List<Transaction> allTransactionsOfMonth = new ArrayList<>();
    private Calendar currentCalendar = Calendar.getInstance();
    private boolean isExpenseTabSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        reportController = new ReportController();

        initViews();
        setupRecyclerView();

        if (getIntent() != null && getIntent().hasExtra("SELECTED_MONTH")) {
            int targetMonth = getIntent().getIntExtra("SELECTED_MONTH", currentCalendar.get(Calendar.MONTH) + 1);
            int targetYear = getIntent().getIntExtra("SELECTED_YEAR", currentCalendar.get(Calendar.YEAR));

            currentCalendar.set(Calendar.YEAR, targetYear);
            currentCalendar.set(Calendar.MONTH, targetMonth - 1);

            String selectedType = getIntent().getStringExtra("SELECTED_TYPE");
            if (selectedType != null) {
                if (selectedType.equalsIgnoreCase("thu")) {
                    isExpenseTabSelected = false;
                } else if (selectedType.equalsIgnoreCase("chi")) {
                    isExpenseTabSelected = true;
                }
            }
        }

        setupMonthPickerAndTabs();
        loadDataForSelectedMonth();
    }

    private void initViews() {
        btnBackStats = findViewById(R.id.btnBackStats);
        btnSwitchToMonthly = findViewById(R.id.btnSwitchToMonthly);
        pieChart = findViewById(R.id.pieChartStats);
        txtNoData = findViewById(R.id.txtNoData);
        rvStatsCategories = findViewById(R.id.rvStatsCategories);

        layoutLeftContainer = findViewById(R.id.layoutLeftContainer);
        layoutRightContainer = findViewById(R.id.layoutRightContainer);

        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        txtCurrentMonthYear = findViewById(R.id.txtCurrentMonthYear);
        txtChartTitle = findViewById(R.id.txtChartTitle);
        tabExpense = findViewById(R.id.tabExpense);
        tabIncome = findViewById(R.id.tabIncome);
        txtTabTotalExpense = findViewById(R.id.txtTabTotalExpense);
        txtTabTotalIncome = findViewById(R.id.txtTabTotalIncome);

        btnBackStats.setOnClickListener(v -> finish());

        if (btnSwitchToMonthly != null) {
            btnSwitchToMonthly.setOnClickListener(v -> {
                Intent intent = new Intent(StatsActivity.this, MonthlyStatsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        rvStatsCategories.setLayoutManager(new LinearLayoutManager(this));
        rvStatsCategories.setHasFixedSize(true);
    }

    private void setupMonthPickerAndTabs() {
        updateMonthYearDisplay();

        btnPreviousMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthYearDisplay();
            loadDataForSelectedMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthYearDisplay();
            loadDataForSelectedMonth();
        });

        tabExpense.setOnClickListener(v -> {
            if (!isExpenseTabSelected) {
                isExpenseTabSelected = true;
                updateTabUi();
                loadReport();
            }
        });

        tabIncome.setOnClickListener(v -> {
            if (isExpenseTabSelected) {
                isExpenseTabSelected = false;
                updateTabUi();
                loadReport();
            }
        });

        updateTabUi();
    }

    private void updateMonthYearDisplay() {
        int month = currentCalendar.get(Calendar.MONTH) + 1;
        int year = currentCalendar.get(Calendar.YEAR);
        txtCurrentMonthYear.setText("Tháng " + month + "/" + year);
    }

    private void updateTabUi() {
        if (isExpenseTabSelected) {
            tabExpense.setBackgroundResource(R.drawable.bg_tab_expense_selected);
            tabIncome.setBackgroundResource(R.drawable.bg_tab_right_unselected);
            txtChartTitle.setText("Cơ cấu Khoản chi");
        } else {
            tabExpense.setBackgroundResource(R.drawable.bg_tab_left_unselected);
            tabIncome.setBackgroundResource(R.drawable.bg_tab_income_selected);
            txtChartTitle.setText("Cơ cấu Khoản thu");
        }
    }

    private void loadDataForSelectedMonth() {
        new Handler(Looper.getMainLooper()).postDelayed(this::getTransactionsFromFirebase, 200);
    }

    private void getTransactionsFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        int selectedMonth = currentCalendar.get(Calendar.MONTH) + 1;
        int selectedYear = currentCalendar.get(Calendar.YEAR);

        FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTransactionsOfMonth.clear();
                    double totalIncome = 0;
                    double totalExpense = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if (transaction != null) {
                            int transactionMonth = 0;
                            int transactionYear = 0;

                            if (transaction.getDate() != null && transaction.getDate().contains("/")) {
                                try {
                                    String[] parts = transaction.getDate().split("/");
                                    transactionMonth = Integer.parseInt(parts[1]);
                                    transactionYear = Integer.parseInt(parts[2]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (transactionMonth == selectedMonth && transactionYear == selectedYear) {
                                allTransactionsOfMonth.add(transaction);

                                if (transaction.getType() != null &&
                                        (transaction.getType().equalsIgnoreCase("thu") || transaction.getType().equalsIgnoreCase("income"))) {
                                    totalIncome += transaction.getAmount();
                                } else {
                                    totalExpense += transaction.getAmount();
                                }
                            }
                        }
                    }

                    DecimalFormat formatter = new DecimalFormat("#,### đ");
                    txtTabTotalExpense.setText(formatter.format(totalExpense));
                    txtTabTotalIncome.setText(formatter.format(totalIncome));

                    loadReport();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi kết nối dữ liệu!", Toast.LENGTH_SHORT).show());
    }

    private void loadReport() {
        reportController.generate(allTransactionsOfMonth, (income, expense, trend, categories) -> fillPieChart(categories));
    }

    private void fillPieChart(List<ReportController.CategoryReport> list) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        ArrayList<ReportController.CategoryReport> filteredList = new ArrayList<>();

        double totalAmountSelectedTab = 0;
        String targetType = isExpenseTabSelected ? "EXPENSE" : "INCOME";

        if (list != null) {
            for (ReportController.CategoryReport item : list) {
                if (item.total > 0 && targetType.equalsIgnoreCase(item.type)) {
                    entries.add(new PieEntry((float) item.total, ""));
                    colors.add(generateColorFromName(item.category));
                    filteredList.add(item);
                    totalAmountSelectedTab += item.total;
                }
            }
        }

        layoutLeftContainer.removeAllViews();
        layoutRightContainer.removeAllViews();

        if (entries.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            txtNoData.setVisibility(View.VISIBLE);
            rvStatsCategories.setVisibility(View.GONE);
            return;
        } else {
            pieChart.setVisibility(View.VISIBLE);
            txtNoData.setVisibility(View.GONE);
            rvStatsCategories.setVisibility(View.VISIBLE);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setDrawValues(false);

        pieChart.setDrawEntryLabels(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(60f);
        pieChart.setExtraOffsets(6f, 0f, 6f, 6f);
        pieChart.setRotationAngle(270f);
        pieChart.setRotationEnabled(false);
        pieChart.setMarker(null);
        pieChart.animateY(600);
        pieChart.invalidate();

        float[] drawAngles = pieChart.getDrawAngles();
        float[] absoluteAngles = pieChart.getAbsoluteAngles();
        float rotationAngle = pieChart.getRotationAngle();

        for (int i = 0; i < filteredList.size(); i++) {
            ReportController.CategoryReport item = filteredList.get(i);
            int percent = (int) Math.round((item.total / totalAmountSelectedTab) * 100);
            int categoryColor = colors.get(i);

            LinearLayout itemRowLayout = new LinearLayout(this);
            itemRowLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemRowLayout.setPadding(0, 15, 0, 15);

            LinearLayout textGroupVertical = new LinearLayout(this);
            textGroupVertical.setOrientation(LinearLayout.VERTICAL);

            TextView txtPercent = new TextView(this);
            txtPercent.setText(percent + "%");
            txtPercent.setTextSize(15f);
            txtPercent.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            txtPercent.setTextColor(Color.parseColor("#2C2C2C"));

            TextView txtName = new TextView(this);
            txtName.setText(item.category);
            txtName.setTextSize(12f);
            txtName.setTextColor(Color.parseColor("#7E7E7E"));

            textGroupVertical.addView(txtPercent);
            textGroupVertical.addView(txtName);

            View colorIndicator = new View(this);
            LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(28, 28);
            colorParams.gravity = Gravity.CENTER_VERTICAL;

            GradientDrawable circleShape = new GradientDrawable();
            circleShape.setShape(GradientDrawable.OVAL);
            circleShape.setColor(categoryColor);
            colorIndicator.setBackground(circleShape);

            float currentAbs = (i == 0) ? 0f : absoluteAngles[i - 1];
            float currentAngle = rotationAngle + currentAbs + (drawAngles[i] / 2f);
            float cosValue = (float) Math.cos(Math.toRadians(currentAngle));

            if (cosValue > 0) {
                txtPercent.setGravity(Gravity.END);
                txtName.setGravity(Gravity.END);
                itemRowLayout.setGravity(Gravity.END);
                colorParams.setMargins(16, 0, 0, 0);
                itemRowLayout.addView(textGroupVertical);
                itemRowLayout.addView(colorIndicator, colorParams);
                layoutRightContainer.addView(itemRowLayout);
            } else {
                txtPercent.setGravity(Gravity.START);
                txtName.setGravity(Gravity.START);
                itemRowLayout.setGravity(Gravity.START);
                colorParams.setMargins(0, 0, 16, 0);
                itemRowLayout.addView(colorIndicator, colorParams);
                itemRowLayout.addView(textGroupVertical);
                layoutLeftContainer.addView(itemRowLayout);
            }
        }

        statsAdapter = new CategoryStatsAdapter(filteredList);
        rvStatsCategories.setAdapter(statsAdapter);
    }

    private int generateColorFromName(String name) {
        if (name == null || name.trim().isEmpty()) return Color.GRAY;
        int hash = name.trim().toLowerCase().hashCode();
        int r = (((hash & 0xFF0000) >> 16) + 255) / 2;
        int g = (((hash & 0x00FF00) >> 8) + 255) / 2;
        int b = ((hash & 0x0000FF) + 255) / 2;
        return Color.rgb(r, g, b);
    }
}