package com.example.quanlychitieu.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.SavingGoal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavingGoalAdapter extends RecyclerView.Adapter<SavingGoalAdapter.ViewHolder> {

    private List<SavingGoal> list = new ArrayList<>();
    private double totalExpense = 0;
    private Map<String, Double> spentMap = new HashMap<>();
    private Context context;
    private String monthKey;
    public SavingGoalAdapter(List<SavingGoal> list) {
        if (list != null) this.list = list;
    }

    public void setMonthKey(String monthKey) {
        if (monthKey != null && !monthKey.isEmpty()) {
            this.monthKey = monthKey;
        }
    }
    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
        notifyDataSetChanged();
    }

    public void updateData(List<SavingGoal> goals) {
        this.list.clear();
        if (goals != null) {
            this.list.addAll(goals);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvRemaining, tvSpent, tvBudget, tvPercent;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvSpent = itemView.findViewById(R.id.tvSpent);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressGoal);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saving_goal, parent, false);
        return new ViewHolder(view);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SavingGoal goal = list.get(position);
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        double budget = goal.getTargetAmount();
        double spent = 0;
        String key = normalize(goal.getCategoryName());

        if (spentMap.containsKey(key)) {
            spent = spentMap.get(key);
        }

        double remaining = budget - spent;
        holder.tvCategory.setText(goal.getCategoryName());
        holder.tvBudget.setText("Ngân sách: " + formatter.format(budget) + " đ");
        holder.tvSpent.setText("Chi tiêu: " + formatter.format(spent) + " đ");
        holder.tvRemaining.setText("Còn lại: " + formatter.format(remaining) + " đ");

        int percent = 0;
        if (budget > 0) {
            percent = (int) ((spent * 100) / budget);
        }

        holder.tvPercent.setText(percent + "%");
        holder.progressBar.setMax(100);
        holder.progressBar.setProgress(Math.min(percent, 100));

        try {
            holder.progressBar.getProgressDrawable()
                    .setTint(Color.parseColor(goal.getColor()));
        } catch (Exception e) {
            holder.progressBar.getProgressDrawable()
                    .setTint(Color.BLUE);
        }

        //sự kiện click vào dấu ">" hoặc cả item để chuyển sang màn hình sửa
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditSavingGoalActivity.class);

            // Truyền các thông tin cần thiết của mục tiêu hiện tại sang màn hình sửa
            intent.putExtra("GOAL_ID", goal.getId());
            intent.putExtra("CATEGORY_NAME", goal.getCategoryName());
            intent.putExtra("TARGET_AMOUNT", goal.getTargetAmount());
            intent.putExtra("COLOR", goal.getColor());
            intent.putExtra("MONTH_KEY", this.monthKey);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateSpentMap(Map<String, Double> map) {
        this.spentMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }
}