package com.applikationsprogramvara.osmviewer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class UserTouchSurface extends View {
    private TwoFingerDrag a;

    public UserTouchSurface(Context context) {
        super(context);
    }

    public UserTouchSurface(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public UserTouchSurface(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return a.onTouchEvent(event);
    }

    public void setCallback(TwoFingerDrag a) {
        this.a = a;
    }

}
