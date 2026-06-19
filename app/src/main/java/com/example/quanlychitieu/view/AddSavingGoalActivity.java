package com.example.quanlychitieu.view;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.SavingGoalController;
import com.example.quanlychitieu.model.SavingGoal;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddSavingGoalActivity extends AppCompatActivity {
    private Spinner spCategory;
    private EditText edtTargetAmount;
    private Button btnSave;
    private ImageButton btnBack;
    private String monthKey;
    private GridView gvColorPalette;
    private String selectedColor = "#2196F3";
    private int selectedPosition = 5;
    private final String[] colorPalette = {
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFC107", "#FF9800", "#FF5722"
    };
    private final String[] categories = {
            "Di chuyển", "Đồ uống", "Y tế", "Hóa đơn",
            "Mua sắm", "Giáo dục", "Đồ ăn", "Giải trí", "Chưa phân loại"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_saving_goal);

        spCategory = findViewById(R.id.spCategory);
        edtTargetAmount = findViewById(R.id.edtTargetAmount);
        gvColorPalette = findViewById(R.id.gvColorPalette);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        monthKey = getIntent().getStringExtra("monthKey");
        if (monthKey == null || monthKey.isEmpty()) {
            // Lấy thời gian hiện tại của thiết bị định dạng yyyy_MM (Ví dụ: "2026_06")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM", Locale.getDefault());
            monthKey = sdf.format(new Date());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        // Xử lý hiển thị bảng màu Lưới (GridView)
        BaseAdapter colorAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return colorPalette.length;
            }

            @Override
            public Object getItem(int position) {
                return colorPalette[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = new View(AddSavingGoalActivity.this);
                    int size = (int) (40 * getResources().getDisplayMetrics().density);
                    view.setLayoutParams(new GridView.LayoutParams(size, size));
                }

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setCornerRadius(15);
                shape.setColor(Color.parseColor(colorPalette[position]));

                // Tạo viền đen nổi bật bao quanh ô màu được chọn
                if (position == selectedPosition) {
                    shape.setStroke(6, Color.parseColor("#222222"));
                } else {
                    shape.setStroke(0, Color.TRANSPARENT);
                }

                view.setBackground(shape);
                return view;
            }
        };
        gvColorPalette.setAdapter(colorAdapter);

        // Click chọn màu trong bảng lưới
        gvColorPalette.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            selectedColor = colorPalette[position];
            colorAdapter.notifyDataSetChanged();
        });

        btnBack.setOnClickListener(v -> {
            finish();
        });
        btnSave.setOnClickListener(v -> saveGoal());
        monthKey = getIntent().getStringExtra("monthKey");
    }

    private void saveGoal() {
        String category = spCategory.getSelectedItem().toString();
        String targetStr = edtTargetAmount.getText().toString().trim();

        if (targetStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ngân sách", Toast.LENGTH_SHORT).show();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        double targetAmount = Double.parseDouble(targetStr);

        SavingGoal goal = new SavingGoal("", category, targetAmount, selectedColor);
        SavingGoalController controller = new SavingGoalController();
        controller.addSavingGoal(monthKey, goal, new SavingGoalController.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddSavingGoalActivity.this,
                        "Cập nhật mục tiêu thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(AddSavingGoalActivity.this,
                        "Lỗi: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
