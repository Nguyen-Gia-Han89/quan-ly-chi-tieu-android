package com.example.quanlychitieu.view;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.CategoryController;
import com.example.quanlychitieu.controller.TransactionController;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {

    public static final String KEY_MODE = "mode";
    public static final String KEY_DATA = "transaction_data";
    public static final int MODE_ADD = 1;
    public static final int MODE_DETAIL = 2;
    public static final int MODE_EDIT = 3;

    private TextView txtTxTitle;
    private ImageView imgEditIcon;
    private EditText edtTxAmount, edtTxDate, edtTxNote;
    private Spinner spnTxCategory;
    private RadioGroup rgTransactionType;
    private RadioButton rbExpense, rbIncome;

    private LinearLayout layoutButtonsAdd, layoutButtonsDetail, layoutButtonsEdit;
    private Button btnConfirmAdd, btnEditState, btnDelete, btnSaveUpdate, btnCancelUpdate;

    private TransactionController transactionController;
    private CategoryController categoryController;
    private FirebaseAuth.AuthStateListener authStateListener;

    private int currentMode = MODE_ADD;
    private Transaction selectedTransaction;
    private List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<Category> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        transactionController = new TransactionController();
        categoryController = new CategoryController();

        initViews();
        setupCategorySpinner();
        setupDatePicker();
        setupAuthListener();

        if (getIntent() != null) {
            currentMode = getIntent().getIntExtra(KEY_MODE, MODE_ADD);
            selectedTransaction = (Transaction) getIntent().getSerializableExtra(KEY_DATA);
        }

        switchMode(currentMode);
        setupListeners();
    }

    private void initViews() {
        txtTxTitle = findViewById(R.id.txtTxTitle);
        imgEditIcon = findViewById(R.id.imgEditIcon);

        edtTxAmount = findViewById(R.id.edtTxAmount);
        edtTxNote = findViewById(R.id.edtTxNote);
        edtTxDate = findViewById(R.id.edtTxDate);
        spnTxCategory = findViewById(R.id.spnTxCategory);

        rgTransactionType = findViewById(R.id.rgTransactionType);
        rbExpense = findViewById(R.id.rbExpense);
        rbIncome = findViewById(R.id.rbIncome);

        layoutButtonsAdd = findViewById(R.id.layoutButtonsAdd);
        layoutButtonsDetail = findViewById(R.id.layoutButtonsDetail);
        layoutButtonsEdit = findViewById(R.id.layoutButtonsEdit);

        btnConfirmAdd = findViewById(R.id.btnConfirmAdd);
        btnEditState = findViewById(R.id.btnEditState);
        btnDelete = findViewById(R.id.btnDelete);
        btnSaveUpdate = findViewById(R.id.btnSaveUpdate);
        btnCancelUpdate = findViewById(R.id.btnCancelUpdate);
    }

    private void setupCategorySpinner() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTxCategory.setAdapter(categoryAdapter);

        categoryController.getAllCategories(new CategoryController.CategoryCallback() {
            @Override
            public void onLoaded(List<Category> categories) {
                categoryList.clear();
                categoryList.addAll(categories);
                categoryAdapter.notifyDataSetChanged();

                if (selectedTransaction != null) {
                    setSpinnerSelection(selectedTransaction.getCategory());
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(TransactionActivity.this, "Lỗi tải danh mục: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDatePicker() {
        edtTxDate.setOnClickListener(v -> {
            if (currentMode == MODE_DETAIL) return;

            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, month);
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        edtTxDate.setText(sdf.format(cal.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void switchMode(int mode) {
        this.currentMode = mode;
        if (mode == MODE_ADD) {
            txtTxTitle.setText("Thêm giao dịch");
            imgEditIcon.setImageResource(android.R.drawable.ic_menu_revert);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            edtTxDate.setText(sdf.format(Calendar.getInstance().getTime()));

            enableInputs(true);
            rbExpense.setChecked(true);

            layoutButtonsAdd.setVisibility(View.VISIBLE);
            layoutButtonsDetail.setVisibility(View.GONE);
            layoutButtonsEdit.setVisibility(View.GONE);

        } else if (mode == MODE_DETAIL) {
            txtTxTitle.setText("Chi tiết giao dịch");
            imgEditIcon.setImageResource(android.R.drawable.ic_menu_edit);

            fillDataInputs();
            enableInputs(false);

            layoutButtonsAdd.setVisibility(View.GONE);
            layoutButtonsDetail.setVisibility(View.VISIBLE);
            layoutButtonsEdit.setVisibility(View.GONE);

        } else if (mode == MODE_EDIT) {
            txtTxTitle.setText("Chỉnh sửa giao dịch");
            imgEditIcon.setImageResource(android.R.drawable.ic_menu_revert);

            fillDataInputs();
            enableInputs(true);

            layoutButtonsAdd.setVisibility(View.GONE);
            layoutButtonsDetail.setVisibility(View.GONE);
            layoutButtonsEdit.setVisibility(View.VISIBLE);
        }

        // VỊ TRÍ GỌI HÀM 1: Cập nhật giao diện Tab ngay khi vừa chuyển màn hình/chế độ
        updateRadioButtonStyles();
    }

    private void fillDataInputs() {
        if (selectedTransaction == null) return;

        DecimalFormat df = new DecimalFormat("#.##");
        edtTxAmount.setText(df.format(selectedTransaction.getAmount()));
        edtTxNote.setText(selectedTransaction.getNote());
        edtTxDate.setText(selectedTransaction.getDate());

        if ("INCOME".equals(selectedTransaction.getType())) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }

        setSpinnerSelection(selectedTransaction.getCategory());
    }

    private void setSpinnerSelection(String categoryName) {
        if (categoryName == null || categoryList.isEmpty()) return;
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getName().equalsIgnoreCase(categoryName)) {
                spnTxCategory.setSelection(i);
                break;
            }
        }
    }

    private void enableInputs(boolean isEnabled) {
        edtTxAmount.setEnabled(isEnabled);
        edtTxNote.setEnabled(isEnabled);
        edtTxDate.setEnabled(isEnabled);
        spnTxCategory.setEnabled(isEnabled);
        rbExpense.setEnabled(isEnabled);
        rbIncome.setEnabled(isEnabled);
    }

    private void setupListeners() {
        imgEditIcon.setOnClickListener(v -> {
            if (currentMode == MODE_DETAIL) {
                switchMode(MODE_EDIT);
            } else {
                finish();
            }
        });

        btnEditState.setOnClickListener(v -> switchMode(MODE_EDIT));
        btnCancelUpdate.setOnClickListener(v -> switchMode(MODE_DETAIL));

        btnConfirmAdd.setOnClickListener(v -> saveTransaction(true));
        btnSaveUpdate.setOnClickListener(v -> saveTransaction(false));

        btnDelete.setOnClickListener(v -> {
            if (currentMode == MODE_DETAIL) {
                new AlertDialog.Builder(this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteTransaction())
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        // VỊ TRÍ GỌI HÀM 2: Lắng nghe sự kiện chuyển khoản Thu/Chi đổi màu tức thì
        rgTransactionType.setOnCheckedChangeListener((group, checkedId) -> updateRadioButtonStyles());
    }

    // HÀM ĐỔI STYLE THU/CHI TOÀN DIỆN - KHÔNG LO MẤT CHỮ
    private void updateRadioButtonStyles() {
        int density = (int) getResources().getDisplayMetrics().density;
        int radius = 12 * density;

        if (rbExpense.isChecked()) {
            // Tab Khoản chi được tích chọn
            GradientDrawable selected = new GradientDrawable();
            selected.setColor(Color.WHITE);
            selected.setCornerRadius(radius);
            selected.setStroke(3 * density, Color.parseColor("#5E17EB")); // Viền tím đậm chuyên nghiệp
            rbExpense.setBackground(selected);
            rbExpense.setTextColor(Color.parseColor("#C62828")); // Chữ màu đỏ đậm cực kỳ rõ ràng

            // Tab Khoản thu ở trạng thái chờ
            GradientDrawable unselected = new GradientDrawable();
            unselected.setColor(Color.parseColor("#F0EEFF")); // Nền tím nhạt làm chìm nút đi
            unselected.setCornerRadius(radius);
            rbIncome.setBackground(unselected);
            rbIncome.setTextColor(Color.GRAY); // Chữ xám
        } else {
            // Tab Khoản thu được tích chọn
            GradientDrawable selected = new GradientDrawable();
            selected.setColor(Color.WHITE);
            selected.setCornerRadius(radius);
            selected.setStroke(3 * density, Color.parseColor("#5E17EB")); // Viền tím đậm chuyên nghiệp
            rbIncome.setBackground(selected);
            rbIncome.setTextColor(Color.parseColor("#2E7D32")); // Chữ màu xanh lá đậm cực kỳ rõ ràng

            // Tab Khoản chi ở trạng thái chờ
            GradientDrawable unselected = new GradientDrawable();
            unselected.setColor(Color.parseColor("#F0EEFF")); // Nền tím nhạt làm chìm nút đi
            unselected.setCornerRadius(radius);
            rbExpense.setBackground(unselected);
            rbExpense.setTextColor(Color.GRAY); // Chữ xám
        }
    }

    private void saveTransaction(boolean isNew) {
        String amountStr = edtTxAmount.getText().toString().trim();
        String note = edtTxNote.getText().toString().trim();
        String date = edtTxDate.getText().toString().trim();

        if (amountStr.isEmpty()) {
            edtTxAmount.setError("Vui lòng nhập số tiền!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            edtTxAmount.setError("Số tiền không hợp lệ!");
            return;
        }

        if (spnTxCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục!", Toast.LENGTH_SHORT).show();
            return;
        }

        Category selectedCat = (Category) spnTxCategory.getSelectedItem();
        String category = selectedCat.getName();
        String type = (rgTransactionType.getCheckedRadioButtonId() == R.id.rbIncome) ? "INCOME" : "EXPENSE";

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Tài khoản chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = user.getUid();

        if (isNew) {
            Transaction newTx = new Transaction("", uid, amount, category, date, note, type);
            transactionController.addTransaction(newTx, new TransactionController.TransactionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this, "Thêm giao dịch thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(TransactionActivity.this, "Lỗi thêm: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (selectedTransaction == null) return;

            selectedTransaction.setAmount(amount);
            selectedTransaction.setCategory(category);
            selectedTransaction.setDate(date);
            selectedTransaction.setNote(note);
            selectedTransaction.setType(type);

            transactionController.updateTransaction(selectedTransaction, new TransactionController.TransactionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(TransactionActivity.this, "Thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteTransaction() {
        if (selectedTransaction == null) return;
        transactionController.deleteTransaction(selectedTransaction.getId(), new TransactionController.TransactionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(TransactionActivity.this, "Xóa giao dịch thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(TransactionActivity.this, "Lỗi xóa: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAuthListener() {
        authStateListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null) {
                finish();
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }
}