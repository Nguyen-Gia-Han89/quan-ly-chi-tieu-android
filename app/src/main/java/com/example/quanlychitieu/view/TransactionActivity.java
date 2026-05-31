package com.example.quanlychitieu.view;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.CategoryController;
import com.example.quanlychitieu.controller.TransactionController;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TransactionActivity extends AppCompatActivity {

    public static final String KEY_MODE = "mode";
    public static final String KEY_DATA = "transaction_data";
    public static final int MODE_ADD = 1;
    public static final int MODE_DETAIL = 2;
    public static final int MODE_EDIT = 3;

    private TextView txtTxTitle, txtSubTitleState;
    private EditText edtTxAmount, edtTxDate, edtTxNote;
    private Spinner spnTxCategory;
    private ImageView btnPickDate, imgEditIcon;

    private LinearLayout layoutButtonsAdd, layoutButtonsDetail, layoutButtonsEdit;
    private Button btnConfirmAdd, btnCancelAdd, btnEditState, btnDelete, btnSaveUpdate, btnCancelUpdate;

    private TransactionController transactionController;
    private CategoryController categoryController;

    private int currentMode = MODE_ADD;
    private Transaction selectedTransaction;
    private List<Category> dynamicCategories = new ArrayList<>();

    // Khai báo listener để quản lý vòng đời đăng nhập của Firebase
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        transactionController = new TransactionController();
        categoryController = new CategoryController();

        initUI();

        if (getIntent() != null) {
            currentMode = getIntent().getIntExtra(KEY_MODE, MODE_ADD);
            selectedTransaction = (Transaction) getIntent().getSerializableExtra(KEY_DATA);
        }

        // 🌟 GIẢI PHÁP TRIỆT ĐỂ: Lắng nghe khi nào Token của tài khoản thật sẵn sàng
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // Tài khoản thật đã đồng bộ thành công -> Tiến hành load dữ liệu Spinner an toàn
                    setupSpinner();
                    // Ngắt lắng nghe ngay sau khi đã xác thực xong để tránh chạy lại nhiều lần
                    FirebaseAuth.getInstance().removeAuthStateListener(this);
                }
            }
        };
        // Kích hoạt bộ lắng nghe dữ liệu user
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);

        setupDatePicker();
        switchMode(currentMode);
        setupClickListeners();
    }

    private void initUI() {
        txtTxTitle = findViewById(R.id.txtTxTitle);
        txtSubTitleState = findViewById(R.id.txtSubTitleState);
        edtTxAmount = findViewById(R.id.edtTxAmount);
        edtTxDate = findViewById(R.id.edtTxDate);
        edtTxNote = findViewById(R.id.edtTxNote);
        spnTxCategory = findViewById(R.id.spnTxCategory);
        btnPickDate = findViewById(R.id.btnPickDate);
        imgEditIcon = findViewById(R.id.imgEditIcon);

        layoutButtonsAdd = findViewById(R.id.layoutButtonsAdd);
        layoutButtonsDetail = findViewById(R.id.layoutButtonsDetail);
        layoutButtonsEdit = findViewById(R.id.layoutButtonsEdit);

        btnConfirmAdd = findViewById(R.id.btnConfirmAdd);
        btnCancelAdd = findViewById(R.id.btnCancelAdd);
        btnEditState = findViewById(R.id.btnEditState);
        btnDelete = findViewById(R.id.btnDelete);
        btnSaveUpdate = findViewById(R.id.btnSaveUpdate);
        btnCancelUpdate = findViewById(R.id.btnCancelUpdate);
    }

    private void setupSpinner() {
        if (isFinishing() || isDestroyed()) return;

        categoryController.getAllCategories(new CategoryController.CategoryListCallback() {
            @Override
            public void onLoaded(List<Category> categories) {
                dynamicCategories = categories;

                ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                        TransactionActivity.this,
                        android.R.layout.simple_spinner_item,
                        dynamicCategories
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnTxCategory.setAdapter(adapter);

                if (selectedTransaction != null) {
                    for (int i = 0; i < dynamicCategories.size(); i++) {
                        if (dynamicCategories.get(i).getName().equals(selectedTransaction.getCategory())) {
                            spnTxCategory.setSelection(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(TransactionActivity.this, "Lỗi danh mục: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDatePicker() {
        btnPickDate.setOnClickListener(v -> {
            if (currentMode == MODE_DETAIL) return;
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                String formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, (month1 + 1), year1);
                edtTxDate.setText(formattedDate);
            }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void switchMode(int mode) {
        currentMode = mode;
        boolean isEditable = (mode == MODE_ADD || mode == MODE_EDIT);

        edtTxAmount.setEnabled(isEditable);
        spnTxCategory.setEnabled(isEditable);
        edtTxNote.setEnabled(isEditable);
        if (btnPickDate != null) {
            btnPickDate.setEnabled(isEditable);
        }

        if (mode == MODE_ADD) {
            txtTxTitle.setText("Thêm khoản chi");
            txtSubTitleState.setVisibility(View.GONE);
            layoutButtonsAdd.setVisibility(View.VISIBLE);
            layoutButtonsDetail.setVisibility(View.GONE);
            layoutButtonsEdit.setVisibility(View.GONE);
        } else if (mode == MODE_DETAIL) {
            txtTxTitle.setText("Thêm khoản chi");
            txtSubTitleState.setText("Chi tiết khoản chi");
            txtSubTitleState.setVisibility(View.VISIBLE);
            fillDataToFields();
            layoutButtonsAdd.setVisibility(View.GONE);
            layoutButtonsDetail.setVisibility(View.VISIBLE);
            layoutButtonsEdit.setVisibility(View.GONE);
        } else if (mode == MODE_EDIT) {
            txtTxTitle.setText("Thêm khoản chi");
            txtSubTitleState.setText("Chỉnh sửa khoản chi");
            txtSubTitleState.setVisibility(View.VISIBLE);
            fillDataToFields();
            layoutButtonsAdd.setVisibility(View.GONE);
            layoutButtonsDetail.setVisibility(View.GONE);
            layoutButtonsEdit.setVisibility(View.VISIBLE);
        }
    }

    private void fillDataToFields() {
        if (selectedTransaction != null) {
            edtTxAmount.setText(String.format("%.0f", selectedTransaction.getAmount()));
            edtTxDate.setText(selectedTransaction.getDate());
            edtTxNote.setText(selectedTransaction.getNote());
        }
    }

    private void setupClickListeners() {
        if (imgEditIcon != null) {
            imgEditIcon.setOnClickListener(v -> finish());
        }

        btnConfirmAdd.setOnClickListener(v -> executeSaveTransaction(true));
        btnCancelAdd.setOnClickListener(v -> finish());
        btnEditState.setOnClickListener(v -> switchMode(MODE_EDIT));

        btnDelete.setOnClickListener(v -> {
            if (selectedTransaction != null) {
                transactionController.deleteTransaction(selectedTransaction.getId(), new TransactionController.TransactionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(TransactionActivity.this, "Xóa giao dịch thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(TransactionActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnSaveUpdate.setOnClickListener(v -> executeSaveTransaction(false));
        btnCancelUpdate.setOnClickListener(v -> switchMode(MODE_DETAIL));
    }

    private void executeSaveTransaction(boolean isNew) {
        String amountStr = edtTxAmount.getText().toString().trim();
        String date = edtTxDate.getText().toString().trim();
        String note = edtTxNote.getText().toString().trim();

        Category selectedCat = (Category) spnTxCategory.getSelectedItem();
        String category = (selectedCat != null) ? selectedCat.getName() : "Khác";

        if (amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        // 🌟 LẤY UID THẬT 100%: Bắt buộc lấy đúng UID của tài khoản đang đăng nhập để lưu trữ thông tin chuẩn xác
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi xác thực: Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        if (isNew) {
            String id = FirebaseFirestore.getInstance().collection("transactions").document().getId();
            Transaction tx = new Transaction(id, uid, amount, category, date, note, "EXPENSE");

            transactionController.addTransaction(tx, new TransactionController.TransactionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TransactionActivity.this, "Thêm khoản chi thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(TransactionActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (selectedTransaction == null) return;
            selectedTransaction.setAmount(amount);
            selectedTransaction.setCategory(category);
            selectedTransaction.setDate(date);
            selectedTransaction.setNote(note);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy bỏ lắng nghe khi hủy Activity để tránh rò rỉ bộ nhớ (Memory Leak)
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }
}