package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Activity_Splashscreen extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2800L;

    private View     ivLogo;
    private View     tvAppName;
    private View     tvTagline;
    private View     splashProgress;
    private FirebaseAuth mAuth;

    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        playEntranceAnimations();
        scheduleNavigation();
    }

    // -------------------------------------------------------------------------
    private void bindViews() {
        ivLogo        = findViewById(R.id.ivLogo);
        tvAppName     = findViewById(R.id.tvAppName);
        tvTagline     = findViewById(R.id.tvTagline);
        splashProgress = findViewById(R.id.splashProgress);

        // Start all invisible — animations will reveal them
        ivLogo.setVisibility(View.INVISIBLE);
        tvAppName.setVisibility(View.INVISIBLE);
        tvTagline.setVisibility(View.INVISIBLE);
        splashProgress.setVisibility(View.INVISIBLE);
    }

    // -------------------------------------------------------------------------
    // ANIMATIONS
    // -------------------------------------------------------------------------

    private void playEntranceAnimations() {
        // Logo: scale-up + fade-in
        scaleIn(ivLogo, 0);

        // App name: fade-in-up after logo
        fadeInUp(tvAppName, 350);

        // Tagline: fade-in-up after name
        fadeInUp(tvTagline, 520);

        // Progress bar: fade in last
        fadeIn(splashProgress, 750);
    }

    /** Scale from 0.6 → 1.0 with fade */
    private void scaleIn(View view, long delayMs) {
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new DecelerateInterpolator(2f));
        set.setStartOffset(delayMs);
        set.setFillAfter(true);

        ScaleAnimation scale = new ScaleAnimation(
                0.6f, 1f, 0.6f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(600);

        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(600);

        set.addAnimation(scale);
        set.addAnimation(alpha);

        set.setAnimationListener(makeVisibleOn(view));
        view.startAnimation(set);
    }

    /** Slide up 20% + fade in */
    private void fadeInUp(View view, long delayMs) {
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new DecelerateInterpolator(1.8f));
        set.setStartOffset(delayMs);
        set.setFillAfter(true);

        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(450);

        TranslateAnimation translate = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0.2f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f);
        translate.setDuration(450);

        set.addAnimation(alpha);
        set.addAnimation(translate);

        set.setAnimationListener(makeVisibleOn(view));
        view.startAnimation(set);
    }

    /** Simple fade in only */
    private void fadeIn(View view, long delayMs) {
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(400);
        alpha.setStartOffset(delayMs);
        alpha.setFillAfter(true);
        alpha.setAnimationListener(makeVisibleOn(view));
        view.startAnimation(alpha);
    }

    /** Returns a listener that makes view VISIBLE when animation starts */
    private Animation.AnimationListener makeVisibleOn(View view) {
        return new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {
                view.setVisibility(View.VISIBLE);
            }
            @Override public void onAnimationEnd(Animation a)    {}
            @Override public void onAnimationRepeat(Animation a) {}
        };
    }

    // -------------------------------------------------------------------------
    // NAVIGATION — check Firebase auth, route accordingly
    // -------------------------------------------------------------------------
    private void scheduleNavigation() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            Intent intent;
            if (currentUser != null) {
                // Already logged in → go to role selection
                // (role stored in SharedPreferences or re-ask)
                intent = new Intent(this, Activity_Roleselection.class);
            } else {
                // Not logged in → role selection (will go login from there)
                intent = new Intent(this, Activity_Roleselection.class);
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in,
                    android.R.anim.fade_out);
            finish();

        }, SPLASH_DURATION_MS);
    }

    // -------------------------------------------------------------------------
    // Prevent back press on splash
    // -------------------------------------------------------------------------
    @Override
    public void onBackPressed() {
        // do nothing — user cannot go back from splash
    }
}