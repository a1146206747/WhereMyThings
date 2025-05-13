package com.example.wheremythings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private List<Reply> replyList;

    public ReplyAdapter(List<Reply> replyList) {
        this.replyList = replyList;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Reply reply = replyList.get(position);
        holder.replyText.setText(reply.getText());
        holder.replyUsername.setText(reply.getUsername());
        holder.replyTimestamp.setText(formatTimestamp(reply.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView replyText, replyUsername, replyTimestamp;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            replyText = itemView.findViewById(R.id.replyText);
            replyUsername = itemView.findViewById(R.id.replyUsername);
            replyTimestamp = itemView.findViewById(R.id.replyTimestamp);
        }
    }

    private String formatTimestamp(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time));
    }
}
