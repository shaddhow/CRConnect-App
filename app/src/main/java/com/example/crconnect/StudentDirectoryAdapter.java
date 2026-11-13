package com.example.crconnect;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentDirectoryAdapter extends RecyclerView.Adapter<StudentDirectoryAdapter.StudentViewHolder> {

    public interface OnStudentClickListener {
        void onStudentClick(StudentItem student);
    }

    public interface OnStudentMoreClickListener {
        void onStudentMoreClick(StudentItem student, View anchorView);
    }

    private final Context context;
    private final List<StudentItem> originalList = new ArrayList<>();
    private final List<StudentItem> filteredList = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentFilterMode = "ALL"; // "ALL", "VOTED", "PENDING"
    private OnStudentClickListener listener;
    private OnStudentMoreClickListener moreListener;

    public StudentDirectoryAdapter(Context context) {
        this.context = context;
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }

    public void setOnStudentMoreClickListener(OnStudentMoreClickListener moreListener) {
        this.moreListener = moreListener;
    }

    public void setStudentList(List<StudentItem> list) {
        originalList.clear();
        if (list != null) {
            originalList.addAll(list);
        }
        applyFilters();
    }

    public void filter(String query) {
        this.currentSearchQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        applyFilters();
    }

    public void setFilterMode(String mode) {
        this.currentFilterMode = mode != null ? mode : "ALL";
        applyFilters();
    }

    public int getVotedCount() {
        int count = 0;
        for (StudentItem item : originalList) {
            if (item.isHasVoted()) count++;
        }
        return count;
    }

    public int getPendingCount() {
        int count = 0;
        for (StudentItem item : originalList) {
            if (!item.isHasVoted()) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return originalList.size();
    }

    public void removeStudent(StudentItem student) {
        if (student == null) return;
        String targetId = student.getId();
        for (int i = 0; i < originalList.size(); i++) {
            if (originalList.get(i).getId().equalsIgnoreCase(targetId)) {
                originalList.remove(i);
                break;
            }
        }
        int removedIndex = -1;
        for (int i = 0; i < filteredList.size(); i++) {
            if (filteredList.get(i).getId().equalsIgnoreCase(targetId)) {
                removedIndex = i;
                break;
            }
        }
        if (removedIndex != -1) {
            filteredList.remove(removedIndex);
            notifyItemRemoved(removedIndex);
        } else {
            notifyDataSetChanged();
        }
    }

    public void updateStudent(StudentItem student) {
        if (student == null) return;
        String targetId = student.getId();
        for (int i = 0; i < originalList.size(); i++) {
            if (originalList.get(i).getId().equalsIgnoreCase(targetId)) {
                originalList.set(i, student);
                break;
            }
        }
        applyFilters();
    }

    private void applyFilters() {
        filteredList.clear();
        for (StudentItem item : originalList) {
            boolean matchesCategory = true;
            if ("VOTED".equalsIgnoreCase(currentFilterMode)) {
                matchesCategory = item.isHasVoted();
            } else if ("PENDING".equalsIgnoreCase(currentFilterMode)) {
                matchesCategory = !item.isHasVoted();
            }

            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String name = item.getName().toLowerCase(Locale.getDefault());
                String id = item.getId().toLowerCase(Locale.getDefault());
                String email = item.getEmail().toLowerCase(Locale.getDefault());
                matchesSearch = name.contains(currentSearchQuery) || id.contains(currentSearchQuery) || email.contains(currentSearchQuery);
            }

            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_directory, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentItem student = filteredList.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStudentName;
        private final TextView tvStudentId;
        private final TextView tvStudentSubline;
        private final TextView tvStudentInitials;
        private final ImageView imgStudentAvatar;
        private final TextView tvBadgeCr;
        private final TextView tvVoteStatusBadge;
        private final ImageView btnStudentMore;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvStudentSubline = itemView.findViewById(R.id.tvStudentSubline);
            tvStudentInitials = itemView.findViewById(R.id.tvStudentInitials);
            imgStudentAvatar = itemView.findViewById(R.id.imgStudentAvatar);
            tvBadgeCr = itemView.findViewById(R.id.tvBadgeCr);
            tvVoteStatusBadge = itemView.findViewById(R.id.tvVoteStatusBadge);
            btnStudentMore = itemView.findViewById(R.id.btnStudentMore);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStudentClick(filteredList.get(pos));
                }
            });

            if (btnStudentMore != null) {
                btnStudentMore.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        StudentItem item = filteredList.get(pos);
                        if (moreListener != null) {
                            moreListener.onStudentMoreClick(item, v);
                        } else if (listener != null) {
                            listener.onStudentClick(item);
                        }
                    }
                });
            }
        }

        public void bind(StudentItem item) {
            tvStudentName.setText(item.getName());
            tvStudentId.setText("ID: " + item.getId());
            tvStudentSubline.setText("Intake " + item.getIntake() + " · Sec " + item.getSection());

            // CR Badge
            tvBadgeCr.setVisibility(item.isCr() ? View.VISIBLE : View.GONE);

            // Avatar Handling
            String photo = item.getProfileImageUrl();
            if (photo != null && !photo.trim().isEmpty() && (photo.startsWith("http") || photo.startsWith("content:"))) {
                tvStudentInitials.setVisibility(View.GONE);
                imgStudentAvatar.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(photo)
                        .circleCrop()
                        .placeholder(R.drawable.bg_icon_square_blue)
                        .into(imgStudentAvatar);
            } else {
                imgStudentAvatar.setVisibility(View.GONE);
                tvStudentInitials.setVisibility(View.VISIBLE);
                tvStudentInitials.setText(item.getInitials());
            }

            // Vote Status Pill
            boolean isDark = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(dpToPx(999));

            if (item.isHasVoted()) {
                tvVoteStatusBadge.setText("✓ Voted");
                int bgCol = isDark ? Color.parseColor("#0D3B24") : Color.parseColor("#DCFCE7");
                int strokeCol = isDark ? Color.parseColor("#15803D") : Color.parseColor("#BBF7D0");
                int textCol = isDark ? Color.parseColor("#4ADE80") : Color.parseColor("#166534");
                badgeBg.setColor(bgCol);
                badgeBg.setStroke(dpToPx(1), strokeCol);
                tvVoteStatusBadge.setTextColor(textCol);
            } else {
                tvVoteStatusBadge.setText("Pending");
                int bgCol = isDark ? Color.parseColor("#1E293B") : Color.parseColor("#F1F5F9");
                int strokeCol = isDark ? Color.parseColor("#334155") : Color.parseColor("#E2E8F0");
                int textCol = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
                badgeBg.setColor(bgCol);
                badgeBg.setStroke(dpToPx(1), strokeCol);
                tvVoteStatusBadge.setTextColor(textCol);
            }
            tvVoteStatusBadge.setBackground(badgeBg);
        }

        private int dpToPx(int dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
    }
}
