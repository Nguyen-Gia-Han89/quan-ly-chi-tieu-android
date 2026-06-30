package com.example.quanlychitieu.view;

import com.example.quanlychitieu.BuildConfig;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import com.example.quanlychitieu.model.Notification;
import com.example.quanlychitieu.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;

import android.text.Editable;
import android.text.TextWatcher;

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
    private ImageView imgScanQR;
    private ImageView btnPickDate;

    private ImageView btnManageCategory;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;
    private static final int REQUEST_PICK_IMAGE = 103;

    private com.google.mlkit.vision.codescanner.GmsBarcodeScanner qrScanner;
    private com.google.mlkit.vision.text.TextRecognizer textRecognizer;
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        View root = findViewById(android.R.id.content);

        root.setOnApplyWindowInsetsListener((v, insets) -> {

            int bottom = insets.getSystemWindowInsetBottom();

            LinearLayout bottomButtons =
                    findViewById(R.id.bottomButtonsContainer);

            bottomButtons.setPadding(
                    bottomButtons.getPaddingLeft(),
                    bottomButtons.getPaddingTop(),
                    bottomButtons.getPaddingRight(),
                    bottom + 24
            );

            return insets;
        });

        transactionController = new TransactionController();
        categoryController = new CategoryController();

        initViews();
        setupCategorySpinner();
        setupDatePicker();

        if (getIntent() != null) {
            currentMode = getIntent().getIntExtra(KEY_MODE, MODE_ADD);
            selectedTransaction = (Transaction) getIntent().getSerializableExtra(KEY_DATA);
        }

        switchMode(currentMode);
        setupListeners();
    }

    private void initViews() {
        imgEditIcon = findViewById(R.id.imgEditIcon);
        txtTxTitle = findViewById(R.id.txtTxTitle);

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
        btnSaveUpdate = findViewById(R.id.btnSaveUpdate);
        btnCancelUpdate = findViewById(R.id.btnCancelUpdate);
        btnDelete = findViewById(R.id.btnDelete);

        btnManageCategory = findViewById(R.id.btnManageCategory);
        Log.d("TEST", String.valueOf(btnManageCategory));
        btnPickDate = findViewById(R.id.btnPickDate);
        imgScanQR = findViewById(R.id.imgScanQR);

        // Khởi tạo bộ quét QR Code
        com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions options =
                new com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                        .build();
        qrScanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(this, options);

        // Khởi tạo bộ nhận diện chữ viết OCR (Quét Bill)
        textRecognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
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
            imgEditIcon.setImageResource(android.R.drawable.ic_menu_revert);


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

        if (rbExpense != null && rbIncome != null) {
            updateRadioButtonStyles();
        }
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
        imgEditIcon.setOnClickListener(v -> finish());

        btnEditState.setOnClickListener(v -> switchMode(MODE_EDIT));
        btnCancelUpdate.setOnClickListener(v -> switchMode(MODE_DETAIL));

        btnConfirmAdd.setOnClickListener(v -> saveTransaction(true));
        btnSaveUpdate.setOnClickListener(v -> saveTransaction(false));

        rgTransactionType.setOnCheckedChangeListener((group, checkedId) -> updateRadioButtonStyles());

        btnManageCategory.setOnClickListener(v -> showCategoryMenu());

        btnPickDate.setOnClickListener(v -> edtTxDate.performClick());
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa giao dịch")
                    .setMessage("Bạn có chắc muốn xóa giao dịch này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {

                        transactionController.deleteTransaction(
                                selectedTransaction.getId(),
                                new TransactionController.TransactionCallback() {

                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(
                                                TransactionActivity.this,
                                                "Đã xóa giao dịch",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Toast.makeText(
                                                TransactionActivity.this,
                                                errorMessage,
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        imgScanQR.setOnClickListener(v -> {
            if (currentMode == MODE_DETAIL) return;

            // Bước 3: Sửa đổi hiển thị thực đơn bao gồm cả Chọn ảnh hóa đơn
            String[] options = {"Quét mã QR", "Chụp hóa đơn", "Chọn ảnh hóa đơn"};

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Chọn phương thức quét")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startQrScanning();
                        } else if (which == 1) {
                            startBillCamera();
                        } else if (which == 2) {
                            pickBillImage(); // Bước 4: Gọi hàm chọn ảnh
                        }
                    })
                    .show();
        });

        edtTxAmount.addTextChangedListener(new TextWatcher() {

            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {

                if (s.toString().equals(current))
                    return;

                edtTxAmount.removeTextChangedListener(this);

                String clean = s.toString()
                        .replace(".", "")
                        .replace(",", "");

                if (!clean.isEmpty()) {
                    try {
                        long value = Long.parseLong(clean);

                        DecimalFormat formatter =
                                new DecimalFormat("#,###");

                        String formatted =
                                formatter.format(value)
                                        .replace(",", ".");

                        current = formatted;
                        edtTxAmount.setText(formatted);
                        edtTxAmount.setSelection(formatted.length());

                    } catch (Exception ignored) {}
                }

                edtTxAmount.addTextChangedListener(this);
            }
        });
    }

    private void showCategoryMenu() {

        String[] options = {
                "➕ Thêm danh mục",
                "✏️ Sửa danh mục đang chọn",
                "🗑 Xóa danh mục đang chọn"
        };

        new AlertDialog.Builder(this)
                .setTitle("Quản lý danh mục")
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {
                        showAddCategoryDialog();
                    }
                    else if (which == 1) {
                        showEditCategoryDialog();
                    }
                    else {
                        showDeleteCategoryDialog();
                    }

                })
                .show();
    }

    private void showAddCategoryDialog() {

        EditText input = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Thêm danh mục")
                .setView(input)

                .setPositiveButton("Thêm", (d, w) -> {

                    String name =
                            input.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Tên không được để trống",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    categoryController.addCategory(
                            name,
                            new CategoryController.ActionCallback() {

                                @Override
                                public void onSuccess() {

                                    Toast.makeText(
                                            TransactionActivity.this,
                                            "Thêm thành công",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    setupCategorySpinner();
                                }

                                @Override
                                public void onFailure(String errorMessage) {

                                    Toast.makeText(
                                            TransactionActivity.this,
                                            errorMessage,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });
                })

                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditCategoryDialog() {

        Category selected =
                (Category) spnTxCategory.getSelectedItem();

        if (selected == null) {
            return;
        }

        if ("SYSTEM".equalsIgnoreCase(selected.getUserId())) {

            Toast.makeText(
                    this,
                    "Không thể sửa danh mục hệ thống",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        EditText input = new EditText(this);
        input.setText(selected.getName());

        new AlertDialog.Builder(this)
                .setTitle("Sửa danh mục")
                .setView(input)

                .setPositiveButton("Lưu", (d, w) -> {

                    String newName =
                            input.getText().toString().trim();

                    if (newName.isEmpty()) {
                        return;
                    }

                    selected.setName(newName);

                    categoryController.updateCategory(
                            selected,
                            new CategoryController.ActionCallback() {

                                @Override
                                public void onSuccess() {

                                    Toast.makeText(
                                            TransactionActivity.this,
                                            "Đã cập nhật",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    setupCategorySpinner();
                                }

                                @Override
                                public void onFailure(String errorMessage) {

                                    Toast.makeText(
                                            TransactionActivity.this,
                                            errorMessage,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });

                })

                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteCategoryDialog() {

        Category selected =
                (Category) spnTxCategory.getSelectedItem();

        if (selected == null) {
            return;
        }

        if ("SYSTEM".equalsIgnoreCase(selected.getUserId())) {

            Toast.makeText(
                    this,
                    "Không thể xóa danh mục hệ thống",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục")
                .setMessage(
                        "Xóa danh mục \"" +
                                selected.getName() +
                                "\" ?"
                )

                .setPositiveButton("Xóa", (d, w) -> {

                    categoryController.deleteCategory(
                            selected,
                            new CategoryController.ActionCallback() {

                                @Override
                                public void onSuccess() {

                                    Toast.makeText(
                                            TransactionActivity.this,
                                            "Đã xóa",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    setupCategorySpinner();
                                }

                                @Override
                                public void onFailure(String errorMessage) {

                                    Toast.makeText(
                                            TransactionActivity.this,
                                            errorMessage,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });

                })

                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateRadioButtonStyles() {
        int density = (int) getResources().getDisplayMetrics().density;
        int radius = 12 * density;

        if (rbExpense == null || rbIncome == null) return;

        if (rbExpense.isChecked()) {
            GradientDrawable selected = new GradientDrawable();
            selected.setColor(Color.WHITE);
            selected.setCornerRadius(radius);
            selected.setStroke(3 * density, Color.parseColor("#5E17EB"));
            rbExpense.setBackground(selected);
            rbExpense.setTextColor(Color.parseColor("#C62828"));

            GradientDrawable unselected = new GradientDrawable();
            unselected.setColor(Color.parseColor("#F0EEFF"));
            unselected.setCornerRadius(radius);
            rbIncome.setBackground(unselected);
            rbIncome.setTextColor(Color.GRAY);
        } else {
            GradientDrawable selected = new GradientDrawable();
            selected.setColor(Color.WHITE);
            selected.setCornerRadius(radius);
            selected.setStroke(3 * density, Color.parseColor("#5E17EB"));
            rbIncome.setBackground(selected);
            rbIncome.setTextColor(Color.parseColor("#2E7D32"));

            GradientDrawable unselected = new GradientDrawable();
            unselected.setColor(Color.parseColor("#F0EEFF"));
            unselected.setCornerRadius(radius);
            rbExpense.setBackground(unselected);
            rbExpense.setTextColor(Color.GRAY);
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

        // Loại bỏ các ký tự dấu phân cách hàng nghìn/thập phân để tránh lỗi ép kiểu chuỗi sang Double
        amountStr = amountStr.replace(".", "").replace(",", "").trim();

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
                    String[] parts = date.split("/");
                    String mKey = parts[1] + "_" + parts[2];
                    String mYear = parts[1] + "/" + parts[2];
                    checkBudgetAndNotify(mKey, mYear, category, date);
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
        if (authStateListener != null && FirebaseAuth.getInstance() != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }

    private void startQrScanning() {
        qrScanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        parseQrCodeData(rawValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Quét mã thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void startBillCamera() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);

            return;
        }

        try {

            File photoFile = createImageFile();

            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    photoFile
            );

            Intent intent =
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            intent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    photoUri
            );

            startActivityForResult(
                    intent,
                    REQUEST_IMAGE_CAPTURE
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        String fileName = "bill_" + System.currentTimeMillis();

        File storageDir = getExternalFilesDir("Pictures");

        return File.createTempFile(
                fileName,
                ".jpg",
                storageDir
        );
    }

    // Bước 2: Viết hàm pickBillImage kích hoạt Intent chọn ảnh từ thư viện máy
    private void pickBillImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void parseQrCodeData(String qrData) {
        try {
            if (qrData.startsWith("000201")) {
                if (qrData.contains("54")) {
                    int index54 = qrData.indexOf("54");
                    int length = Integer.parseInt(qrData.substring(index54 + 2, index54 + 4));
                    String amount = qrData.substring(index54 + 4, index54 + 4 + length);
                    edtTxAmount.setText(amount);
                }
                if (qrData.contains("62")) {
                    int index62 = qrData.indexOf("62");
                    int totalLength = Integer.parseInt(qrData.substring(index62 + 2, index62 + 4));
                    String subFields = qrData.substring(index62 + 4, index62 + 4 + totalLength);
                    if (subFields.contains("08")) {
                        int index08 = subFields.indexOf("08");
                        int subLength = Integer.parseInt(subFields.substring(index08 + 2, index08 + 4));
                        edtTxNote.setText(subFields.substring(index08 + 4, index08 + 4 + subLength));
                    } else {
                        edtTxNote.setText("Thanh toán mã VietQR");
                    }
                }

                // Kiểm tra từ khóa trong dữ liệu VietQR để tự xác định trạng thái thu nhập
                String lower = qrData.toLowerCase();
                if (lower.contains("salary") || lower.contains("luong") || lower.contains("thuong") || lower.contains("nop tien")) {
                    rbIncome.setChecked(true);
                } else {
                    rbExpense.setChecked(true);
                }
                detectCategoryWithAI(qrData);
                updateRadioButtonStyles();
                Toast.makeText(this, "Đã nhập dữ liệu từ VietQR!", Toast.LENGTH_SHORT).show();
            } else {
                edtTxNote.setText(qrData);

                String lower = qrData.toLowerCase();
                if (lower.contains("salary") || lower.contains("luong") || lower.contains("thuong") || lower.contains("nop tien") || lower.contains("chuyen khoan den")) {
                    rbIncome.setChecked(true);
                } else {
                    rbExpense.setChecked(true);
                }
                detectCategoryWithAI(qrData);
                updateRadioButtonStyles();
                Toast.makeText(this, "Đã lưu nội dung QR vào Ghi chú!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            edtTxNote.setText(qrData);
        }
    }

    /**
     * Đồng thời trích xuất Số hóa đơn, Ngày lập bill, Số tiền tổng và sử dụng Gemini AI
     * để bóc tách sạch sẽ danh sách: Tên món | SL | Giá đưa vào Ghi chú.
     */
    private void analyzeBillText(String billText) {
        String[] lines = billText.split("\n");
        String detectedAmount = "";
        String detectedDate = "";

        // 1. Vẫn tự động nhận diện Danh mục bằng AI như cũ
        if (!billText.isEmpty()) {
            detectCategoryWithAI(billText);
        }

        // --- 2. THUẬT TOÁN TÌM NGÀY THÁNG TRÊN HÓA ĐƠN ---
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(
                "\\b(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})\\b");
        java.util.regex.Matcher dateMatcher = datePattern.matcher(billText);

        if (dateMatcher.find()) {
            String day = dateMatcher.group(1);
            String month = dateMatcher.group(2);
            String year = dateMatcher.group(3);

            if (day.length() == 1) day = "0" + day;
            if (month.length() == 1) month = "0" + month;
            if (year.length() == 2) year = "20" + year;

            int d = Integer.parseInt(day);
            int m = Integer.parseInt(month);
            if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                detectedDate = day + "/" + month + "/" + year;
            }
        }

        // --- 3. PHÂN TÍCH SỐ TIỀN TỔNG CỘNG THEO TỪ KHÓA ---
        for (String line : lines) {
            String lowerLine = line.toLowerCase().trim();
            if (detectedAmount.isEmpty()) {
                if (lowerLine.contains("tổng cộng") || lowerLine.contains("thành tiền")
                        || lowerLine.contains("thanh toán") || lowerLine.contains("total")
                        || lowerLine.contains("lương") || lowerLine.contains("thưởng")
                        || lowerLine.contains("cộng tiền") || lowerLine.contains("số tiền nhận")) {

                    String numberOnly = line.replaceAll("[^0-9]", "");
                    if (!numberOnly.isEmpty()) {
                        detectedAmount = numberOnly;
                    }
                }
            }
        }

        // Thuật toán phụ tìm số lớn nhất làm tổng tiền (nếu không bắt được từ khóa)
        if (detectedAmount.isEmpty()) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{1,3}(?:[.,]\\d{3})*\\b");
            java.util.regex.Matcher matcher = pattern.matcher(billText);
            long maxAmount = 0;
            while (matcher.find()) {
                try {
                    String numStr = matcher.group().replaceAll("[.,]", "");
                    long val = Long.parseLong(numStr);
                    if (val > maxAmount && val > 1000 && val < 50000000) {
                        maxAmount = val;
                    }
                } catch (Exception ignored) {
                }
            }
            if (maxAmount > 0) detectedAmount = String.valueOf(maxAmount);
        }

        // --- 4. CẬP NHẬT NGÀY THÁNG VÀ SỐ TIỀN LÊN GIAO DIỆN TRƯỚC ---
        if (!detectedDate.isEmpty()) {
            edtTxDate.setText(detectedDate);
        }

        if (!detectedAmount.isEmpty()) {
            edtTxAmount.setText(detectedAmount);
        }

        // --- Ghi chú bỏ trong ---
        StringBuilder cleanNote = new StringBuilder();

        for (String line : lines) {
            String l = line.toLowerCase().trim();

            // loại bỏ rác
            if (l.contains("tổng") || l.contains("total") ||
                    l.contains("thanh toán") || l.contains("mã hd") ||
                    l.contains("giờ") || l.contains("wifi") ||
                    l.contains("cảm ơn") || l.contains("powered") ||
                    l.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) {
                continue;
            }

            // giữ dòng có chữ món ăn
            if (l.matches(".*[a-zA-ZÀ-ỹ].*") && l.length() > 3) {
                cleanNote.append(line).append("\n");
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Xử lý Chụp ảnh Camera
        if (requestCode == REQUEST_IMAGE_CAPTURE
                && resultCode == RESULT_OK) {

            try {

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(),
                        photoUri
                );

                Log.d("TEST_OCR", "Bitmap loaded");

                InputImage image =
                        InputImage.fromBitmap(bitmap, 0);

                textRecognizer.process(image)
                        .addOnSuccessListener(visionText -> {

                            Log.d("TEST_OCR", "OCR success");

                            String text = visionText.getText();

                            Log.d("OCR_TEXT", text);

                            analyzeBillText(text);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TEST_OCR", "OCR fail", e);
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Bước 5: Đón nhận kết quả trả về khi người dùng chọn một ảnh từ Gallery máy
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                InputImage image = InputImage.fromFilePath(this, data.getData());
                Toast.makeText(this, "Đang phân tích hóa đơn...", Toast.LENGTH_SHORT).show();

                textRecognizer.process(image)
                        .addOnSuccessListener(visionText -> analyzeBillText(visionText.getText()))
                        .addOnFailureListener(e -> Toast.makeText(this, "OCR lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi đọc file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBillCamera();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền Camera để chụp hóa đơn", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void detectCategoryWithAI(String billText) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                StringBuilder categoryPrompt = new StringBuilder();

                for (Category category : categoryList) {
                    categoryPrompt.append("- ")
                            .append(category.getName())
                            .append("\n");
                }

                // Nâng cấp Prompt: Yêu cầu AI trả về cả Loại giao dịch (INCOME/EXPENSE) và tên danh mục theo format chuẩn
                String prompt =
                        "Hãy phân tích nội dung hóa đơn hoặc giao dịch sau.\n" +

                                "Chỉ trả về duy nhất theo định dạng:\n" +
                                "TYPE|CATEGORY|AMOUNT\n\n" +

                                "TYPE chỉ được là:\n" +
                                "- INCOME\n" +
                                "- EXPENSE\n\n" +

                                "CATEGORY phải khớp CHÍNH XÁC với một trong các danh mục sau:\n" +
                                categoryPrompt.toString() +

                                "\nAMOUNT là số tiền thực tế của giao dịch.\n" +
                                "Nếu hóa đơn có tiền khách đưa và tiền thối thì chỉ lấy tổng tiền phải trả.\n" +
                                "Không lấy tiền khách đưa.\n" +
                                "Không lấy tiền thối.\n" +
                                "AMOUNT chỉ chứa chữ số.\n\n" +

                                "Nội dung:\n" +
                                billText;

                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();

                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                body.put("contents", contents);

                Request request = new Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .build();

                Response response = client.newCall(request).execute();
                String json = response.body().string();
                JSONObject root = new JSONObject(json);
                String category = "EXPENSE|Chưa phân loại";

                try {
                    category = root.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();
                } catch (Exception ignored) {
                }

                // Dọn dẹp dấu định dạng code markdown ```json nếu AI tự thêm vào
                String cleanedResult = category.replace("```", "").replace("json", "").trim();

                runOnUiThread(() -> {

                    if (cleanedResult.contains("|")) {

                        String[] pieces = cleanedResult.split("\\|");

                        if (pieces.length >= 3) {

                            String typePart = pieces[0].trim();
                            String categoryPart = pieces[1].trim();
                            String amountPart = pieces[2].trim();

                            if ("INCOME".equalsIgnoreCase(typePart)) {
                                rbIncome.setChecked(true);
                            } else {
                                rbExpense.setChecked(true);
                            }

                            updateRadioButtonStyles();

                            autoSelectCategory(categoryPart);

                            amountPart = amountPart.replaceAll("[^0-9]", "");

                            if (!amountPart.isEmpty()) {
                                edtTxAmount.setText(amountPart);
                            }

                            Toast.makeText(
                                    this,
                                    "AI: " + typePart + " - " + categoryPart + " - " + amountPart,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }


    private void autoSelectCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "Chưa phân loại";
        }
        // Làm sạch chuỗi triệt để
        final String cleanCategoryName = categoryName.replaceAll("[\\r\\n]", "").trim().toLowerCase();

        // Vòng lặp 1: Tìm kiếm chính xác tuyệt đối
        for (int i = 0; i < categoryList.size(); i++) {
            String currentName = categoryList.get(i).getName().trim().toLowerCase();
            if (currentName.equals(cleanCategoryName)) {
                spnTxCategory.setSelection(i);
                return;
            }
        }

        // Vòng lặp 2: Tìm kiếm tương đối (Ví dụ: "cafe" nằm trong "Cà phê/Cafe")
        for (int i = 0; i < categoryList.size(); i++) {
            String currentName = categoryList.get(i).getName().trim().toLowerCase();
            if (currentName.contains(cleanCategoryName) || cleanCategoryName.contains(currentName)) {
                spnTxCategory.setSelection(i);
                return;
            }
        }

        // Vòng lặp 3: Dự phòng nếu hoàn toàn không match
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getName().equalsIgnoreCase("Chưa phân loại")) {
                spnTxCategory.setSelection(i);
                return;
            }
        }
    }

    private void saveNotificationToFirestore(String title, String message, String fullDate) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String id = db.collection("users").document(uid).collection("notifications").document().getId();
        Notification notify = new Notification(id, title, message, false, fullDate, System.currentTimeMillis());
        db.collection("users").document(uid).collection("notifications").document(id).set(notify);
    }

    private void checkBudgetAndNotify(String monthKey, String monthYear, String currentCategory, String fullDate) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String normalizedCat = currentCategory.trim().toLowerCase();

        // kiểm tra tổng ngân sách để thông báo nếu vượt mức mục tiêu
        db.collection("users").document(uid).collection("saving_goals").document(monthKey).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("totalBudget")) {
                        double totalTarget = doc.getDouble("totalBudget");
                        transactionController.getTransactionsByMonth(monthYear, new TransactionController.TransactionListCallback() {
                            @Override
                            public void onLoaded(List<Transaction> transactions, double income, double expense) {
                                int percent = (int) ((expense * 100) / totalTarget);
                                if (percent >= 100) {
                                    saveNotificationToFirestore("Vượt hạn mức tổng", "Tháng này bạn đã tiêu " + percent + "% ngân sách.", fullDate);
                                } else if (percent >= 85) {
                                    saveNotificationToFirestore("Sắp hết ngân sách tổng", "Bạn đã chi " + percent + "% tổng ngân sách tháng.", fullDate);
                                }
                            }
                            @Override public void onFailure(String e) {}
                        });
                    }

                    // Kiểm tra hạn mức từng danh mục trong trang mục tiêu
                    db.collection("users").document(uid).collection("saving_goals").document(monthKey).collection("goals").get()
                            .addOnSuccessListener(query -> {
                                for (DocumentSnapshot d : query.getDocuments()) {
                                    String catName = d.getString("categoryName");
                                    if (catName != null && catName.trim().toLowerCase().equals(normalizedCat)) {
                                        double catTarget = d.getDouble("targetAmount");
                                        transactionController.getTransactionsByMonth(monthYear, new TransactionController.TransactionListCallback() {
                                            @Override
                                            public void onLoaded(List<Transaction> transactions, double inc, double exp) {
                                                double catExp = 0;
                                                for (Transaction t : transactions) {
                                                    if ("EXPENSE".equals(t.getType()) && t.getCategory().trim().toLowerCase().equals(normalizedCat)) catExp += t.getAmount();
                                                }
                                                int p = (int) ((catExp * 100) / catTarget);
                                                if (p >= 100) saveNotificationToFirestore("Hết ngân sách của [" + currentCategory + "]", "Danh mục này đã tiêu quá " + p + "%.", fullDate);
                                                else if (p >= 85) saveNotificationToFirestore("Cảnh báo [" + currentCategory + "]", "Danh mục chạm ngưỡng " + p + "%.", fullDate);
                                            }
                                            @Override public void onFailure(String e) {}
                                        });
                                    }
                                }
                            });
                });
    }
}