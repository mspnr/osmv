package com.applikationsprogramvara.osmviewer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Separates one- and two-fingers touch events.
 * Initialize listener and populate the MotionEvent from view onTouchEvent
 * Only finger events are accepted. Other tools, e.g. active stylus are ignored.
 * One finger event starts if:
 * 1. finger is moved to a certain DISTANCE_TOLERANCE_DP distance (default 4dp) or
 * 2. delay CONFIRM_DRAW_DELAY from MotionEvent.ACTION_DOWN has passed (200 ms)
 * Two fingers event start when the second finger is down. From that moment on till all fingers
 * are up there is no return to series of one finger events.
 */
public class TwoFingerDrag {

    private final static long CONFIRM_DRAW_DELAY_MS = 200;
    private final static float DISTANCE_TOLERANCE_DP = 4f;

    private static final int NOT_STARTED = 0;
    private static final int IN_PROGRESS = 1;
    private static final int FINISHED = 2;


//    private long timeDrawStarted;
    private int oneFingerOperationStatus;
    private MotionEvent oneFingerStartEvent;
    private final Handler handlerConfirmDrawByTime;
    private final Runnable confirmDrawByTime;
    private static float DISTANCE_TOLERANCE_PX;

    private boolean panStarted;

    private final Listener listener;

    public TwoFingerDrag(Context context, @NonNull Listener listener) {
        this.listener = listener;
        DISTANCE_TOLERANCE_PX = context.getResources().getDisplayMetrics().density * DISTANCE_TOLERANCE_DP;

        handlerConfirmDrawByTime = new Handler(Looper.getMainLooper());
        confirmDrawByTime = () -> {
            Log.d("MyApp3", "TwoFingerDrag   confirming draw by time");
            oneFingerOperationStatus = IN_PROGRESS;
            listener.onOneFinger(oneFingerStartEvent);
        };
    }

    boolean onTouchEvent(MotionEvent event) {
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            if (event.getPointerCount() == 1) {
//                Log.d("MyApp3", "TwoFingerDrag onTouchEvent one " + MotionEvent.actionToString(event.getAction()));
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
//                        timeDrawStarted = System.currentTimeMillis();
                        oneFingerOperationStatus = NOT_STARTED;
                        panStarted = false;
                        oneFingerStartEvent = MotionEvent.obtain(event);
                        handlerConfirmDrawByTime.postDelayed(confirmDrawByTime, CONFIRM_DRAW_DELAY_MS);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                        if (!panStarted) {
                            if (oneFingerOperationStatus == NOT_STARTED) {
                                double distance = Math.hypot(event.getX() - oneFingerStartEvent.getX(), event.getY() - oneFingerStartEvent.getY());
                                Log.d("MyApp3", "   distance " + distance + " vs " + DISTANCE_TOLERANCE_PX);
                                if (distance < DISTANCE_TOLERANCE_PX) {
                                    Log.d("MyApp3", "   cancelling draw by distance");
                                } else {
                                    Log.d("MyApp3", "   confirming draw by distance");
                                    handlerConfirmDrawByTime.removeCallbacks(confirmDrawByTime);
                                    oneFingerOperationStatus = IN_PROGRESS;
                                    listener.onOneFinger(oneFingerStartEvent);
                                    listener.onOneFinger(event);
                                }
                            } else {
                                if (event.getAction() == MotionEvent.ACTION_UP)
                                    oneFingerOperationStatus = FINISHED;
                                listener.onOneFinger(event);
                            }
                        }
                        return true;
                }
            } else if (event.getPointerCount() == 2) {
//                Log.d("MyApp3", "TwoFingerDrag onTouchEvent two " + MotionEvent.actionToString(event.getAction()));
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        handlerConfirmDrawByTime.removeCallbacks(confirmDrawByTime);
                        panStarted = true;
                        if (oneFingerOperationStatus == IN_PROGRESS) {
                            oneFingerOperationStatus = FINISHED;
                            listener.onOneFinger(null);
                        }
                        Log.d("MyApp3", "   start pan"); // + (System.currentTimeMillis() - timeDrawStarted));
                        listener.onTwoFingers(event);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_POINTER_UP:
                        listener.onTwoFingers(event);
                        return true;
                }
            } else {
                Log.d("MyApp3", "TwoFingerDrag onTouchEvent more " + event.getPointerCount() + " " + MotionEvent.actionToString(event.getAction()));
            }
        } else {
            Log.d("MyApp3", "TwoFingerDrag onTouchEvent not finger " + event.getToolType(0) + " " + event.getPointerCount() + " " + MotionEvent.actionToString(event.getAction()));
        }
        return false;
    }

    interface Listener {

        /**
         *
         * @param event can be null if the one finger event series is interrupted
         */
        void onOneFinger(@Nullable MotionEvent event);

        void onTwoFingers(MotionEvent event);

    }
}
