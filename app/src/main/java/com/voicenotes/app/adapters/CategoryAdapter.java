package com.voicenotes.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.voicenotes.app.R;

import java.util.List;

/**
 * Adapter for horizontal Category filter chips.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }

    private final List<String> categories;
    private final OnCategorySelectedListener listener;
    private int selectedPosition = 0;

    public CategoryAdapter(List<String> categories, OnCategorySelectedListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void setSelectedCategory(String category) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).equalsIgnoreCase(category)) {
                int oldPos = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        boolean isSelected = (position == selectedPosition);
        holder.bind(category, isSelected, v -> {
            int prevPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prevPos);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCategorySelected(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }

        public void bind(String category, boolean isSelected, View.OnClickListener clickListener) {
            tvCategoryName.setText(category);
            if (isSelected) {
                tvCategoryName.setBackgroundResource(R.drawable.bg_chip_selected);
                tvCategoryName.setTextColor(Color.WHITE);
            } else {
                tvCategoryName.setBackgroundResource(R.drawable.bg_chip_unselected);
                tvCategoryName.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary_dark));
            }
            itemView.setOnClickListener(clickListener);
        }
    }
}
