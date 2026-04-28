package com.solutions.alphil.zambiajobalerts.ui.aigenerate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.solutions.alphil.zambiajobalerts.R;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocument;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GeneratedDocumentAdapter extends RecyclerView.Adapter<GeneratedDocumentAdapter.DocumentViewHolder> {

    public interface OnDocumentActionListener {
        void onOpen(GeneratedDocument document);
        void onDelete(GeneratedDocument document);
    }

    private List<GeneratedDocument> items = new ArrayList<>();
    private final OnDocumentActionListener listener;

    public GeneratedDocumentAdapter(OnDocumentActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generated_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        GeneratedDocument item = items.get(position);
        holder.tvTitle.setText(item.getFileName());
        holder.tvType.setText(item.getDisplayLabel());
        holder.tvSource.setText(item.getDisplaySource());
        holder.tvDate.setText(DateFormat.getDateTimeInstance().format(new Date(item.getCreatedAt())));

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpen(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<GeneratedDocument> newItems) {
        items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvType;
        TextView tvSource;
        TextView tvDate;
        Button btnOpen;
        Button btnDelete;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFileName);
            tvType = itemView.findViewById(R.id.tvDocumentType);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnOpen = itemView.findViewById(R.id.btnOpen);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
