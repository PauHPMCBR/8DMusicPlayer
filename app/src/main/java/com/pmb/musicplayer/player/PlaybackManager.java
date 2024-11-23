package com.pmb.musicplayer.player;

import static com.pmb.musicplayer.player.PlayerFragment.PLAYBACK_SEEK_BAR_RESOLUTION;

import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pmb.musicplayer.R;
import com.pmb.openal.OpenALManager;

import java.util.ArrayList;
import java.util.Objects;

public class PlaybackManager {
    private final PlayerFragment pf;
    private final SourceSelect sourceSelect;
    private final TextView playbackValue;
    private final ImageButton pauseResumeAllButton;
    private final ImageButton stopAllButton;
    private final ImageButton pauseResumeButton;
    private final ImageButton stopButton;
    private final SeekBar playbackSeekBar;

    final ArrayList<AudioSource> audioSources = new ArrayList<>();
    AudioSource selectedAudioSource;

    public static String secondsToPrettyStr(int seconds) {
        String secStr = "" + seconds%60;
        if (secStr.length() == 1) secStr = "0" + secStr;
        return seconds/60 + ":" + secStr;
    }

    public PlaybackManager(PlayerFragment pf) {
        this.pf = pf;
        sourceSelect = new SourceSelect(pf, this);

        pauseResumeButton = pf.rootView.findViewById(R.id.pauseResumeButton);
        pauseResumeButton.setOnClickListener(v -> {
            if (selectedAudioSource != null) {
                if (selectedAudioSource.getPlayStatus() == AudioSource.PlayStatus.PLAYING)
                    selectedAudioSource.pause();
                else selectedAudioSource.resume();
            }
        });

        stopButton = pf.rootView.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> {
            if (selectedAudioSource != null) stopSource(selectedAudioSource.filePath);
        });

        playbackValue = pf.rootView.findViewById(R.id.playbackValue);
        playbackValue.setText("0:00 / 0:00");

        playbackSeekBar = pf.rootView.findViewById(R.id.playbackSeekBar);
        playbackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && selectedAudioSource != null) {
                    selectedAudioSource.changePlaybackTo(progress / 1000f);
                    playbackValue.setText(secondsToPrettyStr((int) selectedAudioSource.currentPlaybackPosition) + " / " + secondsToPrettyStr((int) selectedAudioSource.audioDuration));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        pauseResumeAllButton = pf.rootView.findViewById(R.id.pauseResumeAllButton);
        pauseResumeAllButton.setOnClickListener(v -> {
            if (selectedAudioSource == null || selectedAudioSource.getPlayStatus() == AudioSource.PlayStatus.PLAYING) {
                pauseAllSources();
            }
            else {
                resumeAllSources();
            }
        });

        stopAllButton = pf.rootView.findViewById(R.id.stopAllButton);
        stopAllButton.setOnClickListener(v -> {
            stopAllSources();
            pf.resetColorCycle();
        });
    }

    public void changePlayResumeButton(boolean setPlay) {
        pf.requireActivity().runOnUiThread(() -> {
            if (setPlay) { //called when the selected audio source is paused, for example
                pauseResumeButton.setImageResource(R.drawable.ic_play);
                boolean onePlaying = false;
                for (AudioSource as : audioSources)
                    if (as.getPlayStatus() == AudioSource.PlayStatus.PLAYING) {
                        onePlaying = true;
                        break;
                    }
                if (!onePlaying)
                    pauseResumeAllButton.setImageResource(R.drawable.ic_play);
            } else { //called when the selected audio source is resumed, for example
                pauseResumeButton.setImageResource(R.drawable.ic_pause);
                pauseResumeAllButton.setImageResource(R.drawable.ic_pause);
            }
        });
    }

    public void updateSeekBar(float currentPlaybackPosition, float audioDuration) {
        pf.requireActivity().runOnUiThread(() -> {
            playbackSeekBar.setProgress((int) (PLAYBACK_SEEK_BAR_RESOLUTION * currentPlaybackPosition / audioDuration));
            playbackValue.setText(secondsToPrettyStr((int) currentPlaybackPosition) + " / " + secondsToPrettyStr((int) audioDuration));
        });
    }

    public void addNewSource(AudioSource audioSource) {
        audioSources.add(audioSource);
        sourceSelect.changeSelectedAudioSource(audioSources.size() - 1);
        if (audioSources.size() == 1) {
            pauseResumeAllButton.setImageResource(R.drawable.ic_pause);
        }
    }

    public void pauseAllSources() {
        for (AudioSource audioSource : audioSources) {
            audioSource.pause();
        }
        pauseResumeAllButton.setImageResource(R.drawable.ic_play);
        changePlayResumeButton(true);
    }

    public void resumeAllSources() {
        for (AudioSource audioSource : audioSources) {
            audioSource.resume();
        }
        pauseResumeAllButton.setImageResource(R.drawable.ic_pause);
        changePlayResumeButton(false);
    }

    public void stopAllSources() {
        OpenALManager.stopMusic();
        for (AudioSource audioSource : audioSources) {
            audioSource.stop();
        }
        audioSources.clear();

        playbackValue.setText("0:00 / 0:00");
        playbackSeekBar.setProgress(0);
        sourceSelect.changeSelectedAudioSource(-1);
        pauseResumeAllButton.setImageResource(R.drawable.ic_play);
        changePlayResumeButton(true);
    }

    public void stopSource(String filePath) {
        pf.requireActivity().runOnUiThread(() -> {
            for (AudioSource source : audioSources) {
                if (source.filePath.equals(filePath)) {
                    source.stop();
                    audioSources.remove(source);
                    break;
                }
            }
            if (audioSources.isEmpty()) {
                stopAllSources();
                return;
            }

            sourceSelect.changeSelectedAudioSource(0);
            sourceSelect.updatePopupWindow();
        });
    }
}
