package com.example.quanlychitieu.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.ReportController;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private final List<ReportController.CategoryReport> list;

    public ReportAdapter(List<ReportController.CategoryReport> list) {
        this.list = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory;
        TextView txtTotal;

        public ViewHolder(View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtTotal = itemView.findViewById(R.id.txtTotal);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ReportController.CategoryReport item = list.get(position);

        holder.txtCategory.setText(item.category);

        NumberFormat format =
                NumberFormat.getInstance(new Locale("vi", "VN"));

        String money = format.format(item.total) + " ₫";

        if ("INCOME".equals(item.type)) {

            holder.txtTotal.setText("+" + money);
            holder.txtTotal.setTextColor(Color.parseColor("#2E7D32")); // xanh

        } else {

            holder.txtTotal.setText("-" + money);
            holder.txtTotal.setTextColor(Color.parseColor("#C62828")); // đỏ
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }
}