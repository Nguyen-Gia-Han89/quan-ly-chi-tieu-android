package com.example.quanlychitieu.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CsvAdapter extends RecyclerView.Adapter<CsvAdapter.ViewHolder> {

    private final List<String[]> data;

    private final NumberFormat moneyFormat =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    public CsvAdapter(List<String[]> data) {
        this.data = data;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView date, type, money;

        public ViewHolder(View v) {
            super(v);
            date = v.findViewById(R.id.txtDate);
            type = v.findViewById(R.id.txtType);
            money = v.findViewById(R.id.txtMoney);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_csv_row, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {

        String[] row = data.get(i);

        // Date nằm ở cột 3
        h.date.setText(row.length > 3 ? row[3] : "");

        // Type nằm ở cột 0
        String type = row.length > 0 ? row[0] : "";

        if ("INCOME".equalsIgnoreCase(type)) {

            h.type.setText("Thu");
            h.type.setTextColor(Color.parseColor("#2E7D32"));

        } else if ("EXPENSE".equalsIgnoreCase(type)) {

            h.type.setText("Chi");
            h.type.setTextColor(Color.parseColor("#C62828"));

        } else {

            h.type.setText(type);
            h.type.setTextColor(Color.BLACK);
        }

        // Amount nằm ở cột 1
        if (row.length > 1) {

            try {

                double amount = Double.parseDouble(row[1]);

                h.money.setText(
                        moneyFormat.format(amount) + " ₫"
                );

            } catch (Exception e) {

                h.money.setText(row[1]);
            }

        } else {

            h.money.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }
}