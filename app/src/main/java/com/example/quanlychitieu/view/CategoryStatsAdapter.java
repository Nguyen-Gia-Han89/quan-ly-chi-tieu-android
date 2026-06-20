package com.example.quanlychitieu.view;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.controller.ReportController;
import java.text.DecimalFormat;
import java.util.List;

public class CategoryStatsAdapter extends RecyclerView.Adapter<CategoryStatsAdapter.StatsViewHolder> {

    private List<ReportController.CategoryReport> reportList;

    public CategoryStatsAdapter(List<ReportController.CategoryReport> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public StatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_stats, parent, false);
        return new StatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatsViewHolder holder, int position) {
        ReportController.CategoryReport item = reportList.get(position);

        holder.txtCategoryName.setText(item.category);

        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        holder.txtCategoryTotal.setText("-" + decimalFormat.format(item.total) + " Đ");

        int color = generateColorFromName(item.category);
        holder.viewCategoryColor.setBackgroundColor(color);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CategoryDetailActivity.class);
            intent.putExtra("CATEGORY_NAME", item.category);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    private int generateColorFromName(String name) {
        if (name == null || name.trim().isEmpty()) return Color.GRAY;
        int hash = name.trim().toLowerCase().hashCode();
        int r = (((hash & 0xFF0000) >> 16) + 255) / 2;
        int g = (((hash & 0x00FF00) >> 8) + 255) / 2;
        int b = ((hash & 0x0000FF) + 255) / 2;
        return Color.rgb(r, g, b);
    }

    static class StatsViewHolder extends RecyclerView.ViewHolder {
        View viewCategoryColor;
        TextView txtCategoryName, txtCategoryTotal;

        public StatsViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
            txtCategoryTotal = itemView.findViewById(R.id.txtCategoryTotal);
        }
    }
}