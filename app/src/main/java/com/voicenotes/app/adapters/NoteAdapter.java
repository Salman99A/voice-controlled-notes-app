package com.voicenotes.app.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.voicenotes.app.R;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView Adapter for displaying notes and To-Do tasks with completion toggles and smooth animations.
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteLongClick(Note note);
        void onQuickTtsClick(Note note);
        void onTaskStatusChanged(Note note, boolean isCompleted);
    }

    private final List<Note> notes = new ArrayList<>();
    private final OnNoteClickListener listener;

    public NoteAdapter(OnNoteClickListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<Note> newNotes) {
        if (newNotes == null) return;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return notes.size();
            }

            @Override
            public int getNewListSize() {
                return newNotes.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return notes.get(oldItemPosition).getId() == newNotes.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Note o = notes.get(oldItemPosition);
                Note n = newNotes.get(newItemPosition);
                return Objects.equals(o.getTitle(), n.getTitle()) &&
                        Objects.equals(o.getContent(), n.getContent()) &&
                        Objects.equals(o.getCategory(), n.getCategory()) &&
                        Objects.equals(o.getNoteType(), n.getNoteType()) &&
                        Objects.equals(o.getTaskStatus(), n.getTaskStatus()) &&
                        o.getUpdatedAt() == n.getUpdatedAt() &&
                        o.isPinned() == n.isPinned() &&
                        o.isReminderEnabled() == n.isReminderEnabled() &&
                        o.getReminderTime() == n.getReminderTime();
            }
        });

        notes.clear();
        notes.addAll(newNotes);
        diffResult.dispatchUpdatesTo(this);
    }

    public Note getNoteAt(int position) {
        if (position >= 0 && position < notes.size()) {
            return notes.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, listener);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardNote;
        private final CheckBox cbTaskStatus;
        private final TextView tvNoteTitle;
        private final TextView tvNoteContent;
        private final TextView tvCategoryBadge;
        private final TextView tvModifiedDate;
        private final TextView tvReminderText;
        private final LinearLayout layoutReminderBadge;
        private final ImageView ivPinStatus;
        private final ImageView btnQuickTts;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNote = itemView.findViewById(R.id.cardNote);
            cbTaskStatus = itemView.findViewById(R.id.cbTaskStatus);
            tvNoteTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvNoteContent = itemView.findViewById(R.id.tvNoteContent);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvModifiedDate = itemView.findViewById(R.id.tvModifiedDate);
            tvReminderText = itemView.findViewById(R.id.tvReminderText);
            layoutReminderBadge = itemView.findViewById(R.id.layoutReminderBadge);
            ivPinStatus = itemView.findViewById(R.id.ivPinStatus);
            btnQuickTts = itemView.findViewById(R.id.btnQuickTts);
        }

        public void bind(Note note, OnNoteClickListener listener) {
            // To-Do Checkbox vs Regular Note
            if (note.isTodo()) {
                cbTaskStatus.setVisibility(View.VISIBLE);
                cbTaskStatus.setOnCheckedChangeListener(null);
                cbTaskStatus.setChecked(note.isCompleted());

                if (note.isCompleted()) {
                    tvNoteTitle.setPaintFlags(tvNoteTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    cardNote.setAlpha(0.65f);
                } else {
                    tvNoteTitle.setPaintFlags(tvNoteTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    cardNote.setAlpha(1.0f);
                }

                cbTaskStatus.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTaskStatusChanged(note, cbTaskStatus.isChecked());
                    }
                });
            } else {
                cbTaskStatus.setVisibility(View.GONE);
                tvNoteTitle.setPaintFlags(tvNoteTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                cardNote.setAlpha(1.0f);
            }

            // Title
            if (note.getTitle() != null && !note.getTitle().trim().isEmpty()) {
                tvNoteTitle.setText(note.getTitle());
                tvNoteTitle.setVisibility(View.VISIBLE);
            } else {
                tvNoteTitle.setText("Untitled");
            }

            // Content
            if (note.getContent() != null && !note.getContent().trim().isEmpty()) {
                tvNoteContent.setText(note.getContent());
                tvNoteContent.setVisibility(View.VISIBLE);
            } else {
                tvNoteContent.setVisibility(View.GONE);
            }

            // Pinned indicator
            ivPinStatus.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

            // Category & Type Badge
            String cat = (note.getCategory() != null && !note.getCategory().isEmpty()) ? note.getCategory() : "General";
            if (note.isTodo()) {
                if (note.isCompleted()) {
                    tvCategoryBadge.setText("☑ Completed");
                } else {
                    tvCategoryBadge.setText("☐ To-Do • " + cat);
                }
            } else {
                tvCategoryBadge.setText(cat);
            }

            // Modified / Completed timestamp
            if (note.isCompleted() && note.getCompletedAt() > 0) {
                tvModifiedDate.setText("Completed " + DateUtils.formatRelative(note.getCompletedAt()));
            } else {
                tvModifiedDate.setText(DateUtils.formatRelative(note.getUpdatedAt()));
            }

            // Reminder status
            if (note.isReminderEnabled() && note.getReminderTime() > 0 && !note.isCompleted()) {
                layoutReminderBadge.setVisibility(View.VISIBLE);
                String repeatSuffix = "";
                if (note.getReminderRepeatInterval() > 0) {
                    long mins = note.getReminderRepeatInterval() / 60000;
                    repeatSuffix = " (Every " + (mins > 0 ? mins : 10) + "m)";
                }
                tvReminderText.setText(DateUtils.formatTime(note.getReminderTime()) + repeatSuffix);
            } else {
                layoutReminderBadge.setVisibility(View.GONE);
            }

            // Click events
            cardNote.setOnClickListener(v -> {
                if (listener != null) listener.onNoteClick(note);
            });

            cardNote.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onNoteLongClick(note);
                    return true;
                }
                return false;
            });

            btnQuickTts.setOnClickListener(v -> {
                if (listener != null) listener.onQuickTtsClick(note);
            });
        }
    }
}
