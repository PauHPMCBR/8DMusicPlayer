package com.pmb.musicplayer.player;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pmb.musicplayer.R;

public class SourceSelect {
    private final Button sourceSelectButton;
    PopupWindow popupWindow;
    final PlayerFragment pf;
    final PlaybackManager playbackManager;
    final RecyclerView recyclerView;

    public SourceSelect(PlayerFragment pf, PlaybackManager playbackManager) {
        this.pf = pf;
        this.playbackManager = playbackManager;

        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        circleDrawable.setColor(Color.GRAY);

        sourceSelectButton = pf.rootView.findViewById(R.id.sourceSelectButton);
        sourceSelectButton.setBackground(circleDrawable);

        View popupView = pf.getLayoutInflater().inflate(R.layout.popup_source_colors, null);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        recyclerView = popupView.findViewById(R.id.colorRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(pf.getActivity()));

        updatePopupWindow();

        sourceSelectButton.setOnClickListener(v -> popupWindow.showAsDropDown(sourceSelectButton));
    }

    public void updatePopupWindow() {
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.popup_item, parent, false);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                int positionCopy = position;
                final int color = playbackManager.audioSources.get(positionCopy).getInnerColor();
                holder.itemView.setBackgroundColor(color);
                holder.itemView.setOnClickListener(v -> {
                    changeSelectedAudioSource(positionCopy);
                    popupWindow.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return playbackManager.audioSources.size();
            }
        });
    }

    void changeSelectedAudioSource(int index) {
        if (playbackManager.selectedAudioSource != null) playbackManager.selectedAudioSource.isSelected = false;
        if (index == -1) {
            playbackManager.selectedAudioSource = null;
            sourceSelectButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        }
        else {
            playbackManager.audioSources.get(index).isSelected = true;
            playbackManager.selectedAudioSource = playbackManager.audioSources.get(index);
            sourceSelectButton.setBackgroundTintList(ColorStateList.valueOf(playbackManager.selectedAudioSource.getInnerColor()));
            playbackManager.changePlayResumeButton(playbackManager.selectedAudioSource.getPlayStatus() != AudioSource.PlayStatus.PLAYING);
        }
    }
}
