package com.example.quanlychitieu.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CategoryDetailActivity extends AppCompatActivity {

    private ImageView btnBackDetail;
    private TextView txtTitleDetail;
    private RecyclerView rvCategoryTransactions;
    private TextView txtNoTransaction;

    private TextView txtTotalAmountDetail;
    private TextView txtTransactionCountDetail;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> filteredList = new ArrayList<>();
    private String categoryName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        if (getIntent() != null) {
            categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        }

        initViews();
        setupRecyclerView();
        getFilteredTransactionsFromFirebase();
    }

    private void initViews() {
        btnBackDetail = findViewById(R.id.btnBackDetail);
        txtTitleDetail = findViewById(R.id.txtTitleDetail);
        rvCategoryTransactions = findViewById(R.id.rvCategoryTransactions);
        txtNoTransaction = findViewById(R.id.txtNoTransaction);

        txtTotalAmountDetail = findViewById(R.id.txtTotalAmountDetail);
        txtTransactionCountDetail = findViewById(R.id.txtTransactionCountDetail);

        txtTitleDetail.setText(categoryName);
        btnBackDetail.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvCategoryTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryTransactions.setHasFixedSize(true);

        transactionAdapter = new TransactionAdapter(filteredList, transaction -> {
            Intent intent = new Intent(CategoryDetailActivity.this, TransactionActivity.class);

            intent.putExtra("TRANSACTION_ID", transaction.getId());

            startActivity(intent);
        });

        rvCategoryTransactions.setAdapter(transactionAdapter);
    }

    private void getFilteredTransactionsFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", categoryName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    filteredList.clear();
                    double totalAmount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());

                            filteredList.add(transaction);
                            totalAmount += transaction.getAmount();
                        }
                    }

                    DecimalFormat formatter = new DecimalFormat("#,### đ");
                    txtTotalAmountDetail.setText(formatter.format(totalAmount));
                    txtTransactionCountDetail.setText(String.valueOf(filteredList.size()));

                    if (filteredList.isEmpty()) {
                        rvCategoryTransactions.setVisibility(View.GONE);
                        txtNoTransaction.setVisibility(View.VISIBLE);
                    } else {
                        rvCategoryTransactions.setVisibility(View.VISIBLE);
                        txtNoTransaction.setVisibility(View.GONE);
                        transactionAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải dữ liệu chi tiết!", Toast.LENGTH_SHORT).show());
    }
}