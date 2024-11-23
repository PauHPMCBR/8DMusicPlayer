package com.pmb.musicplayer.player;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.pmb.musicplayer.FileChooser;
import com.pmb.musicplayer.R;
import com.pmb.openal.OpenALManager;

import java.io.File;
import java.util.ArrayList;

public class PlayerFragment extends Fragment implements OpenALManager.OpenALCallback {
    View rootView;
    PlaybackManager playbackManager;
    FileChooser fileChooser;
    public static float MIN_RADIUS = 0; //in meters
    public static float MAX_RADIUS = 7; //in meters
    public static float MAX_ROTATION_SPEED = 5; //in radians/s, both ways
    public static float MAX_HEIGHT = 7; //in meters, both ways
    public static int PLAYBACK_SEEK_BAR_RESOLUTION = 1000;

    public static int[] CIRCLE_COLORS = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
    int colorCycle = 0;

    private SeekBar radiusSeekBar;
    private SeekBar rotationSpeedSeekBar;
    private SeekBar heightSeekBar;
    private TextView radiusValue;
    private TextView rotationSpeedValue;
    private TextView heightValue;

    private ImageButton automaticMovementButton;

    private CardView innerCardView;

    private File mLastFile;

    public void setupSeekBarListeners() {
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float radius = (progress * 0.01f) * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS;
                radiusValue.setText(String.format("%.1f m", radius));
                if (playbackManager.selectedAudioSource != null) playbackManager.selectedAudioSource.setRadius(radius);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        rotationSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float radPerSec = (progress * 0.01f - 0.5f) * MAX_ROTATION_SPEED * 2;
                rotationSpeedValue.setText(String.format("%.1f rad/s", radPerSec));
                if (playbackManager.selectedAudioSource != null) playbackManager.selectedAudioSource.setRotationSpeed(radPerSec);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float height = (progress * 0.01f - 0.5f) * MAX_HEIGHT * 2;
                heightValue.setText(String.format("%.1f m", height));
                if (playbackManager.selectedAudioSource != null) playbackManager.selectedAudioSource.setHeight(height);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        float radius = (radiusSeekBar.getProgress() * 0.01f) * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS;
        radiusValue.setText(String.format("%.1f m", radius));
        float radPerSec = (rotationSpeedSeekBar.getProgress() * 0.01f - 0.5f) * MAX_ROTATION_SPEED * 2;
        rotationSpeedValue.setText(String.format("%.1f rad/s", radPerSec));
        float height = (heightSeekBar.getProgress() * 0.01f - 0.5f) * MAX_HEIGHT * 2;
        heightValue.setText(String.format("%.1f m", height));
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_audio_player, container, false);
        if (savedInstanceState != null) {
            return rootView;
        }

        OpenALManager.setCallback(this);

        radiusSeekBar = rootView.findViewById(R.id.radiusSeekBar);
        rotationSpeedSeekBar = rootView.findViewById(R.id.rotationSpeedSeekBar);
        heightSeekBar = rootView.findViewById(R.id.heightSeekBar);

        radiusValue = rootView.findViewById(R.id.radiusValue);
        rotationSpeedValue = rootView.findViewById(R.id.rotationSpeedValue);
        heightValue = rootView.findViewById(R.id.heightValue);

        setupSeekBarListeners();

        innerCardView = rootView.findViewById(R.id.innerCardView);

        innerCardView.setOnTouchListener((v, event) -> {
            if (playbackManager.selectedAudioSource != null)
                playbackManager.selectedAudioSource.touchedCardAtPosition(event.getX(), event.getY());
            return true;
        });

        fileChooser = new FileChooser(rootView.getContext(), "Select audio file", mLastFile);

        Button addSourceButton = rootView.findViewById(R.id.addSourceButton);
        addSourceButton.setOnClickListener(v -> {
            ArrayList<String> filePaths = new ArrayList<>();
            for (AudioSource as : playbackManager.audioSources) filePaths.add(as.filePath);
            chooseFile(filePaths);
        });

        automaticMovementButton = rootView.findViewById(R.id.setAutomaticMovementButton);
        automaticMovementButton.setOnClickListener(v -> {
            if (playbackManager.selectedAudioSource != null) {
                playbackManager.selectedAudioSource.automaticallyRotate = true;
                float radius = (radiusSeekBar.getProgress() * 0.01f) * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS;
                playbackManager.selectedAudioSource.setRadius(radius);
            }
        });

        playbackManager = new PlaybackManager(this);
        playbackManager.stopAllSources();

        return rootView;
    }

    public void resetColorCycle() {
        colorCycle = 0;
    }

    public void chooseFile(ArrayList<String> unpickableFiles) {
        FileChooser.FileSelectionCallback callback = file -> {
            if (file != null) {
                mLastFile = file.getParentFile();
                if (!unpickableFiles.contains(file.getPath())) {
                    AudioSource audioSource = new AudioSource(getContext(), playbackManager, innerCardView, file.getPath(), CIRCLE_COLORS[colorCycle % CIRCLE_COLORS.length]);
                    ++colorCycle;
                    audioSource.setHeight((radiusSeekBar.getProgress() * 0.01f) * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS);
                    audioSource.setRotationSpeed((rotationSpeedSeekBar.getProgress() * 0.01f - 0.5f) * MAX_ROTATION_SPEED * 2);
                    audioSource.setHeight((heightSeekBar.getProgress() * 0.01f - 0.5f) * MAX_HEIGHT * 2);
                    audioSource.play();
                    playbackManager.addNewSource(audioSource);
                }
            }
        };
        fileChooser.show(callback);
    }

    @Override
    public void onSoundFinished(String filePath) {
        getActivity().runOnUiThread(() -> {
            playbackManager.stopSource(filePath);
        });
    }
}