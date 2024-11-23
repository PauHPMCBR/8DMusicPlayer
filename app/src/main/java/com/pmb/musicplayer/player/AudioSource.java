package com.pmb.musicplayer.player;

import static com.pmb.musicplayer.player.PlayerFragment.MAX_RADIUS;
import static com.pmb.musicplayer.player.PlayerFragment.PLAYBACK_SEEK_BAR_RESOLUTION;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.pmb.openal.OpenALManager;

public class AudioSource {
    public enum PlayStatus {
        PLAYING,
        PAUSED,
        STOPPED
    }
    private final PlaybackManager playbackManager;
    String filePath;
    private final CardView innerCardView;
    public final View circleView;
    private int innerColor;

    private Thread updateThread;
    private PlayStatus playStatus = PlayStatus.STOPPED;
    private float radius = 3.0f;
    private float angle = 0f;
    private float height = 0.0f;
    private float rotationSpeed = 1f; //in rad/s
    public boolean isSelected = false;
    public boolean automaticallyRotate = true;

    float audioDuration = -1; //in seconds
    float currentPlaybackPosition; //in seconds

    public AudioSource(Context context, PlaybackManager playbackManager, CardView innerCardView, String filePath, int innerColor) {
        this.playbackManager = playbackManager;
        this.innerCardView = innerCardView;
        this.filePath = filePath;
        this.innerColor = innerColor;
        circleView = new View(context);
        circleView.setBackgroundColor(Color.TRANSPARENT); // Set the background to transparent

        int circleRadius = 45;
        int circleBorder = 18;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(circleRadius*2, circleRadius*2);
        params.gravity = Gravity.CENTER;
        circleView.setLayoutParams(params);
        if (circleView.getParent() != null) {
            ((ViewGroup) circleView.getParent()).removeView(circleView);
        }
        innerCardView.addView(circleView);

        Bitmap bitmap = Bitmap.createBitmap(circleRadius*2, circleRadius*2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(circleBorder);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(circleRadius, circleRadius, (float) circleRadius - circleBorder, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(innerColor);
        canvas.drawCircle(circleRadius, circleRadius, circleRadius - circleBorder, paint);
        circleView.setBackground(new BitmapDrawable(context.getResources(), bitmap));
    }

    public int getInnerColor() {
        return innerColor;
    }
    public PlayStatus getPlayStatus() {
        return playStatus;
    }
    public void setRadius(float r) {
        radius = r;
    }
    public void setRotationSpeed(float rps) {
        rotationSpeed = rps;
    }
    public void setHeight(float h) {
        height = h;
        OpenALManager.updateSourcePosition(filePath, angle, radius, height);
    }
    public void changePlaybackTo(float relativePlayed) { //relativePlayed from 0 to 1
        if (audioDuration == -1) return; //not loaded yet?
        OpenALManager.setPlaybackPosition(filePath, audioDuration * relativePlayed);
    }
    public void updateCirclePosition(float angle, float radius) {
        float relX = (float) (radius/(2*MAX_RADIUS) * Math.cos(angle));
        float relY = (float) (radius/(2*MAX_RADIUS) * Math.sin(angle));
        circleView.setTranslationX(relX * innerCardView.getWidth());
        circleView.setTranslationY(relY * innerCardView.getHeight());
    }

    public void touchedCardAtPosition(float x, float y) {
        float relX = x / innerCardView.getWidth() - 0.5f; //from -0.5 to 0.5
        float relY = y / innerCardView.getHeight() - 0.5f;

        automaticallyRotate = false;
        angle = (float) Math.atan2(relY, relX);
        radius = Math.min((float) (MAX_RADIUS * Math.sqrt(relX*relX + relY*relY)*2), MAX_RADIUS);

        updateCirclePosition(angle, radius);
        OpenALManager.updateSourcePosition(filePath, angle, radius, height);
        //Log.d("AudioSource", relX + " " + relY + " -> radius: " + radius + "  angle: " + (int)(angle*360/2/Math.PI) + "º" );
    }

    public void play() {
        if (playStatus == PlayStatus.PAUSED) {
            resume();
        } else {
            audioDuration = OpenALManager.playMusic(filePath);
            currentPlaybackPosition = 0;
            playStatus = PlayStatus.PLAYING;
            startUpdateThread();
            if (isSelected) playbackManager.changePlayResumeButton(false);
        }
    }

    public void pause() {
        if (playStatus == PlayStatus.PAUSED) return;
        OpenALManager.pauseMusic(filePath);
        playStatus = PlayStatus.PAUSED;
        if (isSelected) playbackManager.changePlayResumeButton(true);
    }

    public void resume() {
        if (playStatus == PlayStatus.PLAYING) return;
        OpenALManager.resumeMusic(filePath);
        playStatus = PlayStatus.PLAYING;
        startUpdateThread();
        if (isSelected) playbackManager.changePlayResumeButton(false);
    }

    public void stop() {
        OpenALManager.stopMusic(filePath);
        innerCardView.removeView(circleView);
        stopUpdateThread();
        playStatus = PlayStatus.STOPPED;
        currentPlaybackPosition = 0;
        if (isSelected) playbackManager.changePlayResumeButton(true);
    }

    static final int UPDATE_INTERVAL_MILIS = 50;
    public void startUpdateThread() {
        updateThread = new Thread(() -> {
            while (playStatus == PlayStatus.PLAYING) {
                currentPlaybackPosition = OpenALManager.getPlaybackPosition(filePath);
                if (currentPlaybackPosition < 0 || currentPlaybackPosition >= audioDuration) {
                    //HERE
                    playbackManager.stopSource(filePath);
                    break;
                }

                if (isSelected) {
                    playbackManager.updateSeekBar(currentPlaybackPosition, audioDuration);
                }

                if (automaticallyRotate) {
                    updateCirclePosition(angle, radius);
                    OpenALManager.updateSourcePosition(filePath, angle, radius, height);

                    angle += rotationSpeed * UPDATE_INTERVAL_MILIS/1000;
                    if (angle >= 2 * Math.PI) {
                        angle -= (float) (2 * Math.PI);
                    } else if (angle < 0) {
                        angle += (float) (2 * Math.PI);
                    }
                }

                try {
                    Thread.sleep(UPDATE_INTERVAL_MILIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        updateThread.start();
    }

    public void stopUpdateThread() {
        playStatus = PlayStatus.STOPPED;
        if (updateThread != null) {
            updateThread.interrupt();
            try {
                updateThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            updateThread = null;
        }
    }
}
