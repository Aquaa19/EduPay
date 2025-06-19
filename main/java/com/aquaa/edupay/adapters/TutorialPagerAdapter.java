package com.aquaa.edupay.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.edupay.R;

import java.util.List;

public class TutorialPagerAdapter extends RecyclerView.Adapter<TutorialPagerAdapter.TutorialViewHolder> {

    private List<SlideData> slideDataList;

    public TutorialPagerAdapter(List<SlideData> slideDataList) {
        this.slideDataList = slideDataList;
    }

    @NonNull
    @Override
    public TutorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutorial_slide, parent, false);
        return new TutorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorialViewHolder holder, int position) {
        SlideData slide = slideDataList.get(position);
        holder.tvTitle.setText(slide.getTitle());
        holder.tvDescription.setText(slide.getDescription());
        if (slide.getImageResId() != 0) {
            holder.ivImage.setImageResource(slide.getImageResId());
        } else {
            holder.ivImage.setVisibility(View.GONE); // Hide if no image provided
        }
    }

    @Override
    public int getItemCount() {
        return slideDataList.size();
    }

    public static class TutorialViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvDescription;

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivSlideImage);
            tvTitle = itemView.findViewById(R.id.tvSlideTitle);
            tvDescription = itemView.findViewById(R.id.tvSlideDescription);
        }
    }

    // Helper class to hold data for each slide
    public static class SlideData {
        private String title;
        private String description;
        private int imageResId; // Resource ID for image

        public SlideData(String title, String description, int imageResId) {
            this.title = title;
            this.description = description;
            this.imageResId = imageResId;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public int getImageResId() {
            return imageResId;
        }
    }
}
