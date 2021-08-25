/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialplaypausedrawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public class MaterialPlayPauseDrawable extends BasePaintDrawable {
    private static final int[] playStart = new int[]{
            8, 5, 8, 12, 19, 12, 19, 12,
            8, 12, 8, 19, 19, 12, 19, 12
    };
    private static final int[] playEnd = new int[]{
            12, 5, 5, 16, 12, 16, 12, 5,
            12, 5, 12, 16, 19, 16, 12, 5
    };
    private static final int[] pauseStart = new int[]{
            6, 5, 6, 19, 10, 19, 10, 5,
            14, 5, 14, 19, 18, 19, 18, 5
    };
    private static final int[] pauseEnd = new int[]{
            5, 6, 5, 10, 19, 10, 19, 6,
            5, 14, 5, 18, 19, 18, 19, 14
    };

    public enum State {

        Play(playStart, playEnd),
        Pause(pauseStart, pauseEnd);

        private final int[] mStartPoints;
        private final int[] mEndPoints;

        State(int[] startPoints, int[] endPoints) {
            mStartPoints = startPoints;
            mEndPoints = endPoints;
        }

        public int[] getStartPoints() {
            return mStartPoints;
        }

        public int[] getEndPoints() {
            return mEndPoints;
        }
    }

    private static final int INTRINSIC_SIZE_DP = 24;
    private final int mIntrinsicSize;

    private static final FloatPropertyCompat<MaterialPlayPauseDrawable> FRACTION =
            new FloatPropertyCompat<MaterialPlayPauseDrawable>("fraction") {
                @Override
                public void setValue(MaterialPlayPauseDrawable object, float value) {
                    object.mFraction = value;
                }

                @Override
                public Float get(MaterialPlayPauseDrawable object) {
                    return object.mFraction;
                }
            };

    private State mPreviousState;
    private State mCurrentState = State.Play;
    private float mFraction = 1;
    private State mNextState;

    private Animator mAnimator;

    private Path mPath = new Path();
    private Matrix mMatrix = new Matrix();

    public MaterialPlayPauseDrawable(Context context) {
        mIntrinsicSize = ViewUtils.dpToPxSize(INTRINSIC_SIZE_DP, context);
        mAnimator = ObjectAnimator.ofFloat(this, FRACTION, 0, 1)
                .setDuration(ViewUtils.getShortAnimTime(context));
        mAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tryMoveToNextState();
            }
        });
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicSize;
    }

    public void setAnimationDuration(long duration) {
        mAnimator.setDuration(duration);
    }

    // The name getState() clashes with Drawable.getState().
    public State getPlayPauseState() {
        return mNextState != null ? mNextState : mCurrentState;
    }

    public void jumpToState(State state) {
        stop();
        mCurrentState = state;
        mFraction = 1;
        mNextState = null;
        invalidateSelf();
    }

    public void setState(State state) {
        if (mCurrentState == state) {
            mNextState = null;
            return;
        }
        if (!isVisible()) {
            jumpToState(state);
            return;
        }
        mNextState = state;
        tryMoveToNextState();
    }

    private void tryMoveToNextState() {
        if (mNextState == null || isRunning()) {
            return;
        }
        mPreviousState = mCurrentState;
        mCurrentState = mNextState;
        mFraction = 0;
        mNextState = null;
        start();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if ((isVisible() != visible) || restart) {
            stop();
            if (mNextState != null) {
                jumpToState(mNextState);
            }
            invalidateSelf();
        }
        return super.setVisible(visible, restart);
    }

    private void start() {
        if (mAnimator.isStarted()) {
            return;
        }
        mAnimator.start();
        invalidateSelf();
    }

    private void stop() {
        if (!mAnimator.isStarted()) {
            return;
        }
        mAnimator.end();
    }

    private boolean isRunning() {
        return mAnimator.isRunning();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (isRunning()) {
            invalidateSelf();
        }
    }

    @Override
    protected void onPreparePaint(Paint paint) {
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas, int width, int height, Paint paint) {
        mMatrix.setScale((float) width / 24, (float) height / 24);
        if (mFraction == 0) {
            drawState(canvas, paint, mPreviousState);
        } else if (mFraction == 1) {
            drawState(canvas, paint, mCurrentState);
        } else {
            drawBetweenStates(canvas, paint, mPreviousState, mCurrentState, mFraction);
        }
    }

    private void drawState(Canvas canvas, Paint paint, State state) {
        int[] points = state.getStartPoints();
        mPath.rewind();
        for (int i = 0, count = points.length, subCount = count / 2; i < count; i += 2) {
            float x = points[i];
            float y = points[i + 1];
            if (i % subCount == 0) {
                if (i > 0) {
                    mPath.close();
                }
                mPath.moveTo(x, y);
            } else {
                mPath.lineTo(x, y);
            }
        }
        mPath.close();
        mPath.transform(mMatrix);
        // Drawing the transformed path makes rendering much less blurry than using canvas transform
        // directly. See https://stackoverflow.com/a/16091390 .
        canvas.drawPath(mPath, paint);
    }

    private void drawBetweenStates(Canvas canvas, Paint paint, State fromState, State toState,
                                   float fraction) {
        mMatrix.preRotate(MathUtils.lerp(0, 90, fraction), 12, 12);
        int[] startPoints = fromState.getStartPoints();
        int[] endPoints = toState.getEndPoints();
        mPath.rewind();
        for (int i = 0, count = startPoints.length, subCount = count / 2; i < count; i += 2) {
            int startX = startPoints[i];
            int startY = startPoints[i + 1];
            int endX = endPoints[i];
            int endY = endPoints[i + 1];
            float x = MathUtils.lerp(startX, endX, fraction);
            float y = MathUtils.lerp(startY, endY, fraction);
            if (i % subCount == 0) {
                if (i > 0) {
                    mPath.close();
                }
                mPath.moveTo(x, y);
            } else {
                mPath.lineTo(x, y);
            }
        }
        mPath.close();
        mPath.transform(mMatrix);
        canvas.drawPath(mPath, paint);
    }
}
