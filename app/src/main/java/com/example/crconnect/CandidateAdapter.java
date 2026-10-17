package com.example.crconnect;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.CandidateViewHolder> {

    public interface OnCandidateClickListener {
        void onVoteClick(Candidate candidate);
        void onDeleteClick(Candidate candidate);
        void onItemClick(Candidate candidate);
        void onTestVoteClick(Candidate candidate);
    }

    private final Context context;
    private final List<Candidate> candidates = new ArrayList<>();
    private boolean isAdminView = false;
    private int totalVotes = 0;
    private OnCandidateClickListener listener;

    private static final String[] PALETTE_HEX = {
        "#6C63FF", // Purple/Indigo
        "#0066FF", // Blue
        "#00C9A7", // Teal/Green
        "#FF6B6B", // Red/Coral
        "#F59E0B", // Amber
        "#8B5CF6"  // Violet
    };

    public CandidateAdapter(Context context, boolean isAdminView) {
        this.context = context;
        this.isAdminView = isAdminView;
    }

    public void setIsAdminView(boolean isAdminView) {
        this.isAdminView = isAdminView;
        notifyDataSetChanged();
    }

    public boolean isAdminView() {
        return isAdminView;
    }

    public void setOnCandidateClickListener(OnCandidateClickListener listener) {
        this.listener = listener;
    }

    public void updateCandidates(List<Candidate> newCandidates, int totalVotes) {
        this.candidates.clear();
        if (newCandidates != null) {
            this.candidates.addAll(newCandidates);
        }
        this.totalVotes = Math.max(0, totalVotes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CandidateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_candidate, parent, false);
        return new CandidateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CandidateViewHolder holder, int position) {
        Candidate candidate = candidates.get(position);
        int rankIndex = position + 1;
        int votes = Math.max(0, candidate.getVotes());
        int percentage = totalVotes > 0 ? (int) Math.round((double) votes * 100.0 / totalVotes) : 0;
        int color = Color.parseColor(PALETTE_HEX[position % PALETTE_HEX.length]);

        holder.tvRank.setText(String.format(Locale.US, "%02d", rankIndex));

        String cName = candidate.getName() != null ? candidate.getName() : "Candidate";
        holder.tvName.setText(cName);

        String dept = candidate.getDept();
        if (dept == null || dept.trim().isEmpty()) {
            dept = "Dept. of CSE";
        }

        if (isAdminView) {
            // ADMIN MODE LAYOUT
            int p20 = dpToPx(20);
            if (holder.cardContentLayout != null) {
                holder.cardContentLayout.setPadding(p20, p20, p20, p20);
            }

            // Hide Vote button, department chip, and "N Votes" label
            if (holder.btnVote != null) holder.btnVote.setVisibility(View.GONE);
            if (holder.tvDept != null) holder.tvDept.setVisibility(View.GONE);
            if (holder.tvVotes != null) holder.tvVotes.setVisibility(View.GONE);

            // Number badge: 44dp, 12dp radius, #E6F0FF background, #0066FF number w800
            ViewGroup.LayoutParams rankLp = holder.tvRank.getLayoutParams();
            int sz44 = dpToPx(44);
            rankLp.width = sz44;
            rankLp.height = sz44;
            holder.tvRank.setLayoutParams(rankLp);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dpToPx(12));
            badgeBg.setColor(Color.parseColor("#E6F0FF"));
            holder.tvRank.setBackground(badgeBg);
            holder.tvRank.setTextColor(Color.parseColor("#0066FF"));
            holder.tvRank.setTypeface(null, Typeface.BOLD);
            holder.tvRank.setTextSize(15f);

            // Sub-line "N votes · Dept. of CSE"
            if (holder.tvSubline != null) {
                holder.tvSubline.setVisibility(View.VISIBLE);
                holder.tvSubline.setText(votes + " votes · " + dept);
            }

            // Percentage "P%"
            if (holder.tvPct != null) {
                holder.tvPct.setVisibility(View.VISIBLE);
                holder.tvPct.setText(percentage + "%");
            }

            // Progress bar: 6dp height, track #E2E8F0, fill = candidate's palette color, animate width 400ms
            if (holder.pbVotes != null) {
                holder.pbVotes.setVisibility(View.VISIBLE);
                holder.pbVotes.setMax(100);

                holder.pbVotes.setProgressTintList(ColorStateList.valueOf(color));
                holder.pbVotes.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));

                int currentProg = holder.pbVotes.getProgress();
                ObjectAnimator anim = ObjectAnimator.ofInt(holder.pbVotes, "progress", currentProg, percentage);
                anim.setDuration(400);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.start();
            }

            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.GONE);
            }

            if (holder.btnTestVote != null) {
                holder.btnTestVote.setVisibility(View.VISIBLE);
                holder.btnTestVote.setOnClickListener(v -> {
                    if (listener != null) listener.onTestVoteClick(candidate);
                });
            }
        } else {
            // STUDENT MODE LAYOUT
            int p16 = dpToPx(16);
            if (holder.cardContentLayout != null) {
                holder.cardContentLayout.setPadding(p16, p16, p16, p16);
            }

            if (holder.tvSubline != null) holder.tvSubline.setVisibility(View.GONE);
            if (holder.tvPct != null) holder.tvPct.setVisibility(View.GONE);
            if (holder.btnTestVote != null) holder.btnTestVote.setVisibility(View.GONE);

            if (holder.btnVote != null) holder.btnVote.setVisibility(View.VISIBLE);
            if (holder.tvDept != null) {
                holder.tvDept.setVisibility(View.VISIBLE);
                holder.tvDept.setText(dept);
            }
            if (holder.tvVotes != null) {
                holder.tvVotes.setVisibility(View.VISIBLE);
                holder.tvVotes.setText(votes + " Votes");
            }

            ViewGroup.LayoutParams rankLp = holder.tvRank.getLayoutParams();
            int sz40 = dpToPx(40);
            rankLp.width = sz40;
            rankLp.height = sz40;
            holder.tvRank.setLayoutParams(rankLp);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dpToPx(10));
            badgeBg.setColor(Color.parseColor("#F1F5F9"));
            holder.tvRank.setBackground(badgeBg);
            holder.tvRank.setTextColor(Color.parseColor("#0066FF"));

            if (holder.pbVotes != null) {
                holder.pbVotes.setMax(Math.max(1, totalVotes));
                holder.pbVotes.setProgressTintList(ColorStateList.valueOf(color));
                ObjectAnimator anim = ObjectAnimator.ofInt(holder.pbVotes, "progress", holder.pbVotes.getProgress(), votes);
                anim.setDuration(400);
                anim.start();
            }

            if (holder.btnVote != null) {
                holder.btnVote.setOnClickListener(v -> {
                    if (listener != null) listener.onVoteClick(candidate);
                });
            }
        }

        if (holder.cardItem != null) {
            holder.cardItem.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(candidate);
            });
        }
    }

    @Override
    public int getItemCount() {
        return candidates.size();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static class CandidateViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardItem;
        ConstraintLayout cardContentLayout;
        TextView tvRank, tvName, tvVotes, tvDept, tvSubline, tvPct;
        MaterialButton btnVote, btnDelete, btnTestVote;
        ProgressBar pbVotes;

        public CandidateViewHolder(@NonNull View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.cardCandidateItem);
            cardContentLayout = itemView.findViewById(R.id.layoutCandidateCardContent);
            tvRank = itemView.findViewById(R.id.tvCandidateRank);
            tvName = itemView.findViewById(R.id.tvCandidateName);
            tvVotes = itemView.findViewById(R.id.tvCandidateVotes);
            tvDept = itemView.findViewById(R.id.tvCandidateDept);
            tvSubline = itemView.findViewById(R.id.tvCandidateSubline);
            tvPct = itemView.findViewById(R.id.tvCandidatePct);
            btnVote = itemView.findViewById(R.id.btnVoteCandidate);
            btnDelete = itemView.findViewById(R.id.btnDeleteCandidate);
            btnTestVote = itemView.findViewById(R.id.btnTestVoteCandidate);
            pbVotes = itemView.findViewById(R.id.pbCandidateVotes);
        }
    }
}
