package com.orgista.openpdf;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public final class LoadingSkeletonAnimator {
    private LoadingSkeletonAnimator() {
    }

    public static ObjectAnimator create(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.ALPHA, 0.55f, 1f);
        animator.setDuration(820L);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }
}
