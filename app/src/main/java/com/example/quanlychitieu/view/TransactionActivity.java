package com.example.quanlychitieu.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.example.quanlychitieu.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;
    private com.google.mlkit.vision.codescanner.GmsBarcodeScanner qrScanner;
    private com.google.mlkit.vision.text.TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

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
        btnDelete = findViewById(R.id.btnDelete);
        btnSaveUpdate = findViewById(R.id.btnSaveUpdate);
        btnCancelUpdate = findViewById(R.id.btnCancelUpdate);

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
                        .setTitle("Xác nano xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteTransaction())
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        // VỊ TRÍ GỌI HÀM 2: Lắng nghe sự kiện chuyển khoản Thu/Chi đổi màu tức thì
        rgTransactionType.setOnCheckedChangeListener((group, checkedId) -> updateRadioButtonStyles());

        imgScanQR.setOnClickListener(v -> {
            if (currentMode == MODE_DETAIL) return; // Không cho quét khi đang xem chi tiết

            // Tạo menu lựa chọn phương thức quét
            String[] options = {"Quét mã QR / Chuyển khoản (VietQR)", "Chụp ảnh & Quét Bill giấy (OCR)"};

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Chọn phương thức quét")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // Người dùng chọn quét QR
                            startQrScanning();
                        } else if (which == 1) {
                            // Người dùng chọn chụp ảnh Bill giấy
                            startBillCamera();
                        }
                    })
                    .show();
        });
    }

    // HÀM ĐỔI STYLE THU/CHI TOÀN DIỆN - KHÔNG LO MẤT CHỮ
    private void updateRadioButtonStyles() {
        int density = (int) getResources().getDisplayMetrics().density;
        int radius = 12 * density;

        if (rbExpense == null || rbIncome == null) return;

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
        if (authStateListener != null && FirebaseAuth.getInstance() != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }

    // HÀM 1: Kích hoạt quét QR Code
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

    // HÀM 2: Mở camera hệ thống để chụp ảnh Bill giấy
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
            Intent takePictureIntent =
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            startActivityForResult(
                    takePictureIntent,
                    REQUEST_IMAGE_CAPTURE);

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            Log.e("CAMERA_ERROR", "Camera error", e);
        }
    }

    // Phân tích dữ liệu từ mã QR quét được
    private void parseQrCodeData(String qrData) {
        try {
            if (qrData.startsWith("000201")) { // Định dạng VietQR
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
                rbExpense.setChecked(true);
                updateRadioButtonStyles();
                Toast.makeText(this, "Đã nhập dữ liệu từ VietQR!", Toast.LENGTH_SHORT).show();
            } else {
                edtTxNote.setText(qrData);
                Toast.makeText(this, "Đã lưu nội dung QR vào Ghi chú!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            edtTxNote.setText(qrData);
        }
    }

    // Phân tích văn bản từ ảnh chụp Bill giấy để mò Số tiền
    private void analyzeBillText(String billText) {
        String[] lines = billText.split("\n");
        String detectedAmount = "";

        // Tìm kiếm số tiền theo từ khóa thông dụng trên Bill Việt Nam
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("tổng cộng") || lowerLine.contains("thành tiền")
                    || lowerLine.contains("thanh toán") || lowerLine.contains("total")) {

                String numberOnly = line.replaceAll("[^0-9]", "");
                if (!numberOnly.isEmpty()) {
                    detectedAmount = numberOnly;
                    break;
                }
            }
        }

        // Thuật toán phụ: Nếu không thấy từ khóa, lấy số lớn nhất hợp lệ trên bill
        if (detectedAmount.isEmpty()) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{1,3}(?:[.,]\\d{3})*\\b");
            java.util.regex.Matcher matcher = pattern.matcher(billText);
            long maxAmount = 0;
            while (matcher.find()) {
                try {
                    String numStr = matcher.group().replaceAll("[.,]", "");
                    long val = Long.parseLong(numStr);
                    if (val > maxAmount && val > 1000 && val < 50000000) { // Từ 1k đến 50 triệu
                        maxAmount = val;
                    }
                } catch (Exception ignored) {}
            }
            if (maxAmount > 0) detectedAmount = String.valueOf(maxAmount);
        }

        // Đổ dữ liệu lên giao diện người dùng
        if (!detectedAmount.isEmpty()) {
            edtTxAmount.setText(detectedAmount);
            if (lines.length > 0) edtTxNote.setText("Bill: " + lines[0].trim());
            Toast.makeText(this, "Đã tự động điền số tiền từ hóa đơn!", Toast.LENGTH_SHORT).show();
        } else {
            edtTxNote.setText(billText);
            Toast.makeText(this, "Không nhận diện rõ số tiền, đã lưu tạm chữ vào Ghi chú!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE
                && resultCode == RESULT_OK
                && data != null) {

            Bundle extras = data.getExtras();

            if (extras == null) {
                Toast.makeText(this,
                        "extras = null",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if (imageBitmap == null) {
                Toast.makeText(this,
                        "imageBitmap = null",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this,
                    "Đang phân tích hóa đơn...",
                    Toast.LENGTH_SHORT).show();

            InputImage image =
                    InputImage.fromBitmap(imageBitmap, 0);

            textRecognizer.process(image)
                    .addOnSuccessListener(visionText ->
                            analyzeBillText(visionText.getText()))
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "OCR lỗi: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        }
    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startBillCamera();

            } else {

                Toast.makeText(
                        this,
                        "Bạn cần cấp quyền Camera để chụp hóa đơn",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}