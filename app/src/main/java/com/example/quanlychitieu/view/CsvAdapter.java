package com.example.quanlychitieu.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

        h.date.setText(row.length > 0 ? row[0] : "");
        h.type.setText(row.length > 1 ? row[1] : "");

        if (row.length > 2) {
            try {
                double money = Double.parseDouble(row[2]);

                h.money.setText(moneyFormat.format(money) + " ₫");

            } catch (Exception e) {
                // nếu không phải số
                h.money.setText(row[2]);
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