package com.example.quanlychitieu.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Transaction;
import java.text.DecimalFormat;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactionList, OnItemClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    public void updateList(List<Transaction> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction tx = transactionList.get(position);

        holder.txtItemCategory.setText(tx.getCategory());
        holder.txtItemDate.setText(tx.getDate());

        if (tx.getNote() != null && !tx.getNote().isEmpty()) {
            holder.txtItemNote.setText(tx.getNote());
            holder.txtItemNote.setVisibility(View.VISIBLE);
        } else {
            holder.txtItemNote.setVisibility(View.GONE);
        }

        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedAmount = decimalFormat.format(tx.getAmount());

        if ("INCOME".equals(tx.getType())) {
            holder.txtItemAmount.setText("+" + formattedAmount + " Đ");
            holder.txtItemAmount.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.txtItemAmount.setText("-" + formattedAmount + " Đ");
            holder.txtItemAmount.setTextColor(Color.parseColor("#C62828"));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(tx));
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView txtItemCategory, txtItemNote, txtItemDate, txtItemAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtItemCategory = itemView.findViewById(R.id.txtItemCategory);
            txtItemNote = itemView.findViewById(R.id.txtItemNote);
            txtItemDate = itemView.findViewById(R.id.txtItemDate);
            txtItemAmount = itemView.findViewById(R.id.txtItemAmount);
        }
    }
}