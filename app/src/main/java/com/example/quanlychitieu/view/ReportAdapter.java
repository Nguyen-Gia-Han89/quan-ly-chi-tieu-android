package com.example.quanlychitieu.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.ReportController;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private final List<ReportController.CategoryReport> list;
    private final NumberFormat format =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    public ReportAdapter(List<ReportController.CategoryReport> list) {
        this.list = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtCategory;
        TextView txtTotal;

        public ViewHolder(View view) {
            super(view);
            txtCategory = view.findViewById(android.R.id.text1);
            txtTotal = view.findViewById(android.R.id.text2);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ReportController.CategoryReport item = list.get(position);

        // Tên danh mục
        holder.txtCategory.setText(item.category);

        // Số tiền
        holder.txtTotal.setText(
                format.format(item.total) + " ₫");
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }
}