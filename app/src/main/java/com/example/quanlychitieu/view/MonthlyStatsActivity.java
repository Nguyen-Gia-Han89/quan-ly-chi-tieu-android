package com.example.quanlychitieu.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Transaction;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MonthlyStatsActivity extends AppCompatActivity {

    private ImageView btnBackMonthly;
    private ImageButton btnPreviousYear, btnNextYear;
    private TextView txtCurrentYear, txtMonthlyNoData;
    private TextView txtStatsTitle, txtMaxExpense, txtMaxIncome;
    private BarChart barChartMonthly;

    private int currentYear = Calendar.getInstance().get(Calendar.YEAR);

    private double[] monthlyIncome = new double[12];
    private double[] monthlyExpense = new double[12];

    private double[] maxExpenseInMonth = new double[12];
    private double[] minExpenseInMonth = new double[12];
    private double[] maxIncomeInMonth = new double[12];
    private double[] minIncomeInMonth = new double[12];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_stats);

        initViews();
        setupYearPicker();
        loadYearlyDataFromFirebase();
    }

    private void initViews() {
        btnBackMonthly = findViewById(R.id.btnBackMonthly);
        btnPreviousYear = findViewById(R.id.btnPreviousYear);
        btnNextYear = findViewById(R.id.btnNextYear);
        txtCurrentYear = findViewById(R.id.txtCurrentYear);
        txtMonthlyNoData = findViewById(R.id.txtMonthlyNoData);
        txtStatsTitle = findViewById(R.id.txtStatsTitle);
        txtMaxExpense = findViewById(R.id.txtMaxExpense);
        txtMaxIncome = findViewById(R.id.txtMaxIncome);
        barChartMonthly = findViewById(R.id.barChartMonthly);

        btnBackMonthly.setOnClickListener(v -> finish());
    }

    private void setupYearPicker() {
        txtCurrentYear.setText("Năm " + currentYear);

        btnPreviousYear.setOnClickListener(v -> {
            currentYear--;
            txtCurrentYear.setText("Năm " + currentYear);
            loadYearlyDataFromFirebase();
        });

        btnNextYear.setOnClickListener(v -> {
            currentYear++;
            txtCurrentYear.setText("Năm " + currentYear);
            loadYearlyDataFromFirebase();
        });
    }

    private void loadYearlyDataFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (int i = 0; i < 12; i++) {
                        monthlyIncome[i] = 0;
                        monthlyExpense[i] = 0;
                        maxExpenseInMonth[i] = 0;
                        minExpenseInMonth[i] = Double.MAX_VALUE;
                        maxIncomeInMonth[i] = 0;
                        minIncomeInMonth[i] = Double.MAX_VALUE;
                    }

                    boolean hasData = false;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if (transaction != null && transaction.getDate() != null && transaction.getDate().contains("/")) {
                            try {
                                String[] parts = transaction.getDate().split("/");
                                int tMonth = Integer.parseInt(parts[1]);
                                int tYear = Integer.parseInt(parts[2]);

                                if (tYear == currentYear) {
                                    hasData = true;
                                    int monthIndex = tMonth - 1;
                                    double amount = transaction.getAmount();

                                    if (transaction.getType() != null &&
                                            (transaction.getType().equalsIgnoreCase("thu") || transaction.getType().equalsIgnoreCase("income"))) {
                                        monthlyIncome[monthIndex] += amount;
                                        if (amount > maxIncomeInMonth[monthIndex]) maxIncomeInMonth[monthIndex] = amount;
                                        if (amount < minIncomeInMonth[monthIndex]) minIncomeInMonth[monthIndex] = amount;
                                    } else {
                                        monthlyExpense[monthIndex] += amount;
                                        if (amount > maxExpenseInMonth[monthIndex]) maxExpenseInMonth[monthIndex] = amount;
                                        if (amount < minExpenseInMonth[monthIndex]) minExpenseInMonth[monthIndex] = amount;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    for (int i = 0; i < 12; i++) {
                        if (minExpenseInMonth[i] == Double.MAX_VALUE) minExpenseInMonth[i] = 0;
                        if (minIncomeInMonth[i] == Double.MAX_VALUE) minIncomeInMonth[i] = 0;
                    }

                    if (hasData) {
                        barChartMonthly.setVisibility(View.VISIBLE);
                        txtMonthlyNoData.setVisibility(View.GONE);
                        calculateRecords();
                        buildBarChart();
                    } else {
                        barChartMonthly.setVisibility(View.GONE);
                        txtMonthlyNoData.setVisibility(View.VISIBLE);
                        txtStatsTitle.setText("Thống kê kỷ lục trong năm");
                        txtMaxExpense.setText("Chi cao nhất:\n--");
                        txtMaxIncome.setText("Thu cao nhất:\n--");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải dữ liệu năm!", Toast.LENGTH_SHORT).show());
    }

    private void calculateRecords() {
        DecimalFormat formatter = new DecimalFormat("#,### đ");
        txtStatsTitle.setText("Thống kê kỷ lục trong năm " + currentYear);

        int maxExpMonth = 0;
        double maxExpVal = monthlyExpense[0];
        int maxIncMonth = 0;
        double maxIncVal = monthlyIncome[0];

        for (int i = 1; i < 12; i++) {
            if (monthlyExpense[i] > maxExpVal) {
                maxExpVal = monthlyExpense[i];
                maxExpMonth = i;
            }
            if (monthlyIncome[i] > maxIncVal) {
                maxIncVal = monthlyIncome[i];
                maxIncMonth = i;
            }
        }

        txtMaxExpense.setText("Chi cao nhất năm:\nT" + (maxExpMonth + 1) + ": " + formatter.format(maxExpVal));
        txtMaxIncome.setText("Thu cao nhất năm:\nT" + (maxIncMonth + 1) + ": " + formatter.format(maxIncVal));
    }

    private void buildBarChart() {
        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            incomeEntries.add(new BarEntry(i, (float) monthlyIncome[i]));
            expenseEntries.add(new BarEntry(i, (float) monthlyExpense[i]));
        }

        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, "Thu nhập");
        incomeDataSet.setColor(Color.parseColor("#2E7D32"));
        incomeDataSet.setDrawValues(false);

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseDataSet.setColor(Color.parseColor("#C62828"));
        expenseDataSet.setDrawValues(false);

        BarData barData = new BarData(incomeDataSet, expenseDataSet);

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        barData.setBarWidth(barWidth);
        barChartMonthly.setData(barData);

        String[] months = new String[]{"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"};
        XAxis xAxis = barChartMonthly.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(12f);
        xAxis.setLabelRotationAngle(-45f);

        barChartMonthly.groupBars(0f, groupSpace, barSpace);

        barChartMonthly.getDescription().setEnabled(false);
        barChartMonthly.getAxisRight().setEnabled(false);
        barChartMonthly.setExtraBottomOffset(30f);
        barChartMonthly.setDoubleTapToZoomEnabled(false);
        barChartMonthly.setAutoScaleMinMaxEnabled(false);

        barChartMonthly.post(new Runnable() {
            @Override
            public void run() {
                if (barChartMonthly == null) return;
                barChartMonthly.fitScreen();

                barChartMonthly.setVisibleXRangeMaximum(12f);
                barChartMonthly.setVisibleXRangeMinimum(12f);

                barChartMonthly.moveViewToX(0f);
                barChartMonthly.invalidate();
            }
        });

        barChartMonthly.animateY(800);

        barChartMonthly.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int monthIndex = (int) h.getX();
                if (monthIndex < 0 || monthIndex > 11) return;

                int dataSetIndex = h.getDataSetIndex();
                DecimalFormat formatter = new DecimalFormat("#,### đ");

                if (dataSetIndex == 0) {
                    txtStatsTitle.setText("Thống kê Thu nhập Tháng " + (monthIndex + 1));
                    txtMaxIncome.setText("Thu cao nhất:\n" + formatter.format(maxIncomeInMonth[monthIndex]));
                    txtMaxExpense.setText("Thu thấp nhất:\n" + formatter.format(minIncomeInMonth[monthIndex]));
                } else {
                    txtStatsTitle.setText("Thống kê Chi tiêu Tháng " + (monthIndex + 1));
                    txtMaxIncome.setText("Chi cao nhất:\n" + formatter.format(maxExpenseInMonth[monthIndex]));
                    txtMaxExpense.setText("Chi thấp nhất:\n" + formatter.format(minExpenseInMonth[monthIndex]));
                }
            }

            @Override
            public void onNothingSelected() {
                calculateRecords();
            }
        });

        barChartMonthly.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                Highlight h = barChartMonthly.getHighlightByTouchPoint(me.getX(), me.getY());
                if (h != null) {
                    int monthIndex = (int) h.getX();
                    int dataSetIndex = h.getDataSetIndex();

                    if (monthIndex >= 0 && monthIndex <= 11) {
                        Intent intent = new Intent(MonthlyStatsActivity.this, StatsActivity.class);
                        intent.putExtra("SELECTED_MONTH", monthIndex + 1);
                        intent.putExtra("SELECTED_YEAR", currentYear);

                        if (dataSetIndex == 0) {
                            intent.putExtra("SELECTED_TYPE", "thu");
                        } else {
                            intent.putExtra("SELECTED_TYPE", "chi");
                        }

                        startActivity(intent);
                        finish();
                    }
                }
            }

            @Override public void onChartSingleTapped(MotionEvent me) {}
            @Override public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}
            @Override public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
            @Override public void onChartTranslate(MotionEvent me, float dX, float dY) {}
            @Override public void onChartLongPressed(MotionEvent me) {}
            @Override public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastGesture) {}
            @Override public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastGesture) {}
        });

        barChartMonthly.invalidate();
    }
}