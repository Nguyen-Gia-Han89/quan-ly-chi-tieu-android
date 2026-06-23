package com.example.quanlychitieu.view;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Notification;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.txtTitle.setText(notification.getTitle());
        holder.txtContent.setText(notification.getContent());
        holder.txtDate.setText(notification.getDate());

        if (notification.getTitle().contains("Vượt") || notification.getTitle().contains("Hết")) {
            holder.txtTitle.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            holder.txtTitle.setTextColor(android.graphics.Color.parseColor("#F57C00"));
        }

        if (notification.getIsRead() == false) {
            // Thông báo mới -> hiện badge "Mới"
            holder.txtBadgeNew.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.txtBadgeNew.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtContent, txtDate, txtBadgeNew;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtContent = itemView.findViewById(R.id.txtContent);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtBadgeNew = itemView.findViewById(R.id.txtBadgeNew);
        }
    }
}