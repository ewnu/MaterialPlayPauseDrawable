/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialplaypausedrawable;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class VisibilityAggregatedImageView extends AppCompatImageView {

    public VisibilityAggregatedImageView(Context context) {
        super(context);
    }

    public VisibilityAggregatedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VisibilityAggregatedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            onVisibilityAggregated(visibility == VISIBLE);
        }
    }
}
