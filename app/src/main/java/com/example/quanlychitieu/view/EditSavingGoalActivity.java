package com.example.quanlychitieu.view;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quanlychitieu.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class EditSavingGoalActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Spinner spCategory;
    private EditText edtTargetAmount;
    private GridView gvColorPalette;
    private Button btnUpdate;
    private String goalId, categoryName, currentColor, monthKey;
    private double currentAmount;
    private String selectedColor;
    private boolean isTotalMode = false;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ColorAdapter colorAdapter;
    private final String[] categories = {
            "Di chuyển", "Đồ uống", "Y tế", "Hóa đơn",
            "Mua sắm", "Giáo dục", "Đồ ăn", "Giải trí", "Chưa phân loại"
    };

    private final String[] colorPalette = {
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFC107", "#FF9800", "#FF5722"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_saving_goal);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btnBack);
        spCategory = findViewById(R.id.spCategory);
        edtTargetAmount = findViewById(R.id.edtTargetAmount);
        gvColorPalette = findViewById(R.id.gvColorPalette);
        btnUpdate = findViewById(R.id.btnUpdate);

        isTotalMode = getIntent().getBooleanExtra("IS_TOTAL", false);
        goalId = getIntent().getStringExtra("GOAL_ID");
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        currentAmount = getIntent().getDoubleExtra("TARGET_AMOUNT", 0);
        currentColor = getIntent().getStringExtra("COLOR");
        monthKey = getIntent().getStringExtra("MONTH_KEY");

        selectedColor = currentColor != null ? currentColor : colorPalette[0];

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        colorAdapter = new ColorAdapter();
        gvColorPalette.setAdapter(colorAdapter);

        // Hiển thị số tiền cũ
        edtTargetAmount.setText(String.valueOf((int) currentAmount));

        // NẾU LÀ SỬA TỔNG MỤC TIÊU ➔ ẨN CÁC PHẦN CHỌN CON
        if (isTotalMode) {
            spCategory.setVisibility(View.GONE);
            if (gvColorPalette != null) gvColorPalette.setVisibility(View.GONE);
            edtTargetAmount.setHint("Nhập ngân sách tổng mới");

            TextView tvAmountLabel = findViewById(R.id.tvAmountLabel);
            if (tvAmountLabel != null) {
                tvAmountLabel.setText("Tổng mục tiêu mới");
            }
            if (findViewById(R.id.tvCategoryTitle) != null) findViewById(R.id.tvCategoryTitle).setVisibility(View.GONE);
            if (findViewById(R.id.tvColorTitle) != null) findViewById(R.id.tvColorTitle).setVisibility(View.GONE);
        } else {
            // Tự động chọn đúng Danh mục cũ (Chỉ áp dụng khi sửa danh mục con)
            if (categoryName != null) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(categoryName.trim())) {
                        spCategory.setSelection(i);
                        break;
                    }
                }
            }
        }

        btnBack.setOnClickListener(v -> finish());
        btnUpdate.setOnClickListener(v -> updateGoalToFirestore());
    }

    private class ColorAdapter extends BaseAdapter {
        @Override
        public int getCount() { return colorPalette.length; }
        @Override
        public Object getItem(int position) { return colorPalette[position]; }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_color, parent, false);
            }
            View viewColor = convertView.findViewById(R.id.viewColor);
            String itemColor = colorPalette[position];

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(14);
            drawable.setColor(Color.parseColor(itemColor));

            if (itemColor.equalsIgnoreCase(selectedColor)) {
                drawable.setStroke(5, Color.BLACK);
            } else {
                drawable.setStroke(0, Color.TRANSPARENT);
            }
            viewColor.setBackground(drawable);

            convertView.setOnClickListener(v -> {
                selectedColor = itemColor;
                notifyDataSetChanged();
            });

            return convertView;
        }
    }

    private void updateGoalToFirestore() {
        String amountStr = edtTargetAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền mục tiêu mới", Toast.LENGTH_SHORT).show();
            return;
        }

        double newAmount = Double.parseDouble(amountStr);

        // Kiểm tra xem User đã đăng nhập chưa
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Tài khoản không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isTotalMode && (goalId == null || goalId.isEmpty())) {
            Toast.makeText(this, "Không tìm thấy thông tin mục tiêu cần sửa!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String currentMonthKey = (monthKey != null) ? monthKey : "06_2026";

        if (isTotalMode) {
            //CẬP NHẬT NGÂN SÁCH TỔNG CHO THÁNG (Ghi đè/Thêm mới trường totalBudget)
            Map<String, Object> totalUpdates = new HashMap<>();
            totalUpdates.put("totalBudget", newAmount);

            db.collection("users")
                    .document(uid)
                    .collection("saving_goals")
                    .document(currentMonthKey)
                    .set(totalUpdates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditSavingGoalActivity.this, "Cập nhật ngân sách tổng thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditSavingGoalActivity.this, "Lỗi cập nhật tổng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // CẬP NHẬT TỪNG MỤC TIÊU DANH MỤC CON
            String selectedCategory = spCategory.getSelectedItem().toString();
            Map<String, Object> updates = new HashMap<>();
            updates.put("categoryName", selectedCategory);
            updates.put("targetAmount", newAmount);
            updates.put("color", selectedColor);

            db.collection("users")
                    .document(uid)
                    .collection("saving_goals")
                    .document(currentMonthKey)
                    .collection("goals")
                    .document(goalId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditSavingGoalActivity.this, "Cập nhật mục tiêu thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditSavingGoalActivity.this, "Lỗi cập nhật danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}