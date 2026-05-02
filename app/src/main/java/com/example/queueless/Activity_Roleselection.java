package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

import androidx.appcompat.app.AppCompatActivity;

public class Activity_Roleselection extends AppCompatActivity {

    private View cardUser;
    private View cardAdmin;
    private View appLogo;
    private View tvTitle;
    private View tvSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roleselection);

        bindViews();
        applyEntranceAnimations();
        setupClickListeners();
    }

    // -------------------------------------------------------------------------
    private void bindViews() {
        appLogo    = findViewById(R.id.appLogo);
        tvTitle    = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        cardUser   = findViewById(R.id.cardUser);
        cardAdmin  = findViewById(R.id.cardAdmin);
    }

    // -------------------------------------------------------------------------
    private void setupClickListeners() {
        cardUser.setOnClickListener(v -> {
            animatePress(cardUser, () -> navigateTo("user"));
        });

        cardAdmin.setOnClickListener(v -> {
            animatePress(cardAdmin, () -> navigateTo("admin"));
        });
    }

    // -------------------------------------------------------------------------
    private void navigateTo(String role) {
        Intent intent = new Intent(this, Activity_Login.class);
        intent.putExtra("role", role);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // -------------------------------------------------------------------------
    // ANIMATIONS
    // -------------------------------------------------------------------------

    /** Staggered fade-in-up for each element on screen enter */
    private void applyEntranceAnimations() {
        fadeInUp(appLogo,    0);
        fadeInUp(tvTitle,    120);
        fadeInUp(tvSubtitle, 220);
        fadeInUp(cardUser,   340);
        fadeInUp(cardAdmin,  460);
    }

    /** Single view: fade in + slide up from 40dp below */
    private void fadeInUp(View view, long delayMs) {
        view.setVisibility(View.INVISIBLE);

        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new DecelerateInterpolator(1.8f));
        set.setStartOffset(delayMs);
        set.setFillAfter(true);

        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(400);

        TranslateAnimation translate = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0.18f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f);
        translate.setDuration(400);

        set.addAnimation(alpha);
        set.addAnimation(translate);

        set.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {
                view.setVisibility(View.VISIBLE);
            }
            @Override public void onAnimationEnd(Animation a)   {}
            @Override public void onAnimationRepeat(Animation a){}
        });

        view.startAnimation(set);
    }

    /** Quick scale-down then scale-up on press, then run callback */
    private void animatePress(View view, Runnable onEnd) {
        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(80)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80)
                                .withEndAction(onEnd)
                                .start())
                .start();
    }
}