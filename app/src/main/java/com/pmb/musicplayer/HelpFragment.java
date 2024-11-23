package com.pmb.musicplayer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.noties.markwon.Markwon;
import io.noties.markwon.image.DefaultMediaDecoder;
import io.noties.markwon.image.ImageItem;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.image.SchemeHandler;
import io.noties.markwon.image.file.FileSchemeHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;

public class HelpFragment extends Fragment {
    private static final String TAG = "HelpFragment";
    private static final String HELP_FILE = "help.md";

    private TextView markdownView;
    private Markwon markwon;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Markwon
        markwon = Markwon.builder(requireContext())
                .usePlugin(ImagesPlugin.create(plugin -> {
                    plugin.addSchemeHandler(FileSchemeHandler.createWithAssets(requireContext()));
                }))
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        markdownView = view.findViewById(R.id.markdown_view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            String markdownContent = readFromAssets(HELP_FILE);
            markwon.setMarkdown(markdownView, preprocessMarkdown(markdownContent));
        } catch (IOException e) {
            Log.e(TAG, "Error loading help content", e);
            Toast.makeText(requireContext(), "Error loading help content", Toast.LENGTH_LONG).show();
        }
    }

    private String readFromAssets(String fileName) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (InputStream inputStream = requireContext().getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }
        }
        return buffer.toString();
    }

    private String preprocessMarkdown(String markdown) {
        // Replace all image paths with the correct asset prefix
        return markdown.replaceAll("\\!\\[(.*?)\\]\\((.*?)\\)", "![$1](file:///android_asset/$2)");
    }
}
