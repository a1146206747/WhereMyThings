package com.example.wheremythings;

import android.view.LayoutInflater;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notificationList;

 public NotificationAdapter(Context context, List<Notification> notificationList) {
 this.context = context;
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

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedTimestamp = sdf.format(notification.getTimestamp());

        holder.timestampTextView.setText(formattedTimestamp);
        holder.reportTypeTextView.setText("Matched Report Type: " + notification.getMatchedReportType());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle item click
                Notification clickedNotification = notificationList.get(holder.getAdapterPosition());
                if (clickedNotification.getMatchedReportId() != null) {
                    android.content.Intent intent = new android.content.Intent(context, ReportDetailActivity.class);
                    intent.putExtra("reportId", clickedNotification.getMatchedReportId());
                    context.startActivity(intent);
                }
            }
        });
        // You might want to add more details here based on your Notification class
    }
    @Override
    public int getItemCount() {
        return notificationList.size();
    }
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView;
        TextView reportTypeTextView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            reportTypeTextView = itemView.findViewById(R.id.reportTypeTextView);
        }
    }

    public void setNotificationList(List<Notification> notificationList) {
        this.notificationList = notificationList;
        notifyDataSetChanged();
    }
}