package com.example.wheremythings;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private Context context;
    private ArrayList<Report> reportList;

    public ReportAdapter(Context context, ArrayList<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.report_item, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.reportType.setText(report.getReportType());
        holder.predictedClass.setText("Item: " + report.getPredictedClass());
        holder.location.setText("Location: " + report.getLocation());
        holder.description.setText("Description: " + report.getDescription());

        // Load image using Picasso
        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            Picasso.get().load(report.getImageUrl()).into(holder.reportImage);
        }

        // Optional: Add click listener to view report details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReportDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        ImageView reportImage;
        TextView reportType, predictedClass, location, description;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            reportImage = itemView.findViewById(R.id.reportImage);
            reportType = itemView.findViewById(R.id.reportType);
            predictedClass = itemView.findViewById(R.id.predictedClass);
            location = itemView.findViewById(R.id.location);
            description = itemView.findViewById(R.id.description);
        }
    }
}