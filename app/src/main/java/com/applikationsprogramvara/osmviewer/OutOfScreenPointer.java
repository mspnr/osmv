package com.applikationsprogramvara.osmviewer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class OutOfScreenPointer extends FrameLayout {
    private final int textViewDistanceID;
    private TextView textViewDistance;
    private OnClickListener clickToJump;
    private boolean featureOn;

    public OutOfScreenPointer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        View view = inflate(getContext(), R.layout.layout_out_of_screen_pointer, null);
        addView(view);

        TypedArray attribs = context.obtainStyledAttributes(attrs, R.styleable.OutOfScreenPointer);
        textViewDistanceID = attribs.getResourceId(R.styleable.OutOfScreenPointer_textViewDistance, -1);
        attribs.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        textViewDistance = getRootView().findViewById(textViewDistanceID);
        textViewDistance.setOnClickListener(this.clickToJump);
    }

    public void turnOnOff(boolean on) {
        this.featureOn = on;
    }

    public void setClickToJump(View.OnClickListener clickToJump) {
        this.setOnClickListener(clickToJump);
        this.clickToJump = clickToJump;
    }

    public void updatePosition(MapView map, MyLocationNewOverlay mLocationOverlay) {

        if (!featureOn || textViewDistance == null || mLocationOverlay == null) {
            hideView();
            return;
        }

        Location userLocation = mLocationOverlay.getLastFix();

        if (userLocation == null || !mLocationOverlay.isMyLocationEnabled()) {
            hideView();
            return;
        }

        // getting user position on the screen
        GeoPoint userGeoPoint = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
        Projection projection = map.getProjection();
        Point userScreenPosition = projection.toPixels(userGeoPoint, null);

        boolean userInsideScreen = (new RectF(0, 0, map.getWidth(), map.getHeight())).contains(userScreenPosition.x, userScreenPosition.y);

        if (userInsideScreen) {
            hideView();
            return;
        }

        double angle = Math.atan2(userScreenPosition.y - map.getHeight() / 2f, userScreenPosition.x - map.getWidth() / 2f);

        float speed = userLocation.getSpeed();
        findViewById(R.id.ivOosBubble).setRotation((float) Math.toDegrees(angle) + 90);
        findViewById(R.id.ivOosStill).setVisibility(speed == 0 ? VISIBLE : GONE);
        findViewById(R.id.ivOosMoving).setRotation(speed > 0 ? userLocation.getBearing() : 0);
        findViewById(R.id.ivOosMoving).setVisibility(speed == 0 ? GONE : VISIBLE);

        int halfSize = getContext().getResources().getDrawableForDensity(R.drawable.oos_bubble, (int) getResources().getDisplayMetrics().density).getIntrinsicWidth() / 2;

        PointF offsetIntersection = locationOutsideScreen(userScreenPosition, halfSize, map.getWidth(), map.getHeight());

        if (offsetIntersection == null) {
            // ? hide view
            moveView(this, 0, 0);
            moveView(textViewDistance, 0, 0);
            return;
        }

        moveView(this, (int) (offsetIntersection.x - halfSize), (int) (offsetIntersection.y - halfSize));
        setVisibility(View.VISIBLE);

        PointF screenEdgeIntersection = locationOutsideScreen(userScreenPosition, 0, map.getWidth(), map.getHeight());

        if (screenEdgeIntersection == null) {
            textViewDistance.setVisibility(View.GONE);
            return;
        }

        // displaying distance in text view
        Polyline line = new Polyline(map);
        line.addPoint(userGeoPoint);
        line.addPoint((GeoPoint) projection.fromPixels((int) screenEdgeIntersection.x, (int) screenEdgeIntersection.y));

        textViewDistance.setText(Utils.distanceToStr(line.getDistance(), getContext()));
        textViewDistance.setVisibility(View.VISIBLE);

        // rotate distance text for the top and bottom parts of the screen and keep straight for the middle
        if (-Math.PI * 3f / 4f < angle && angle < -Math.PI / 4f) {
            moveView(textViewDistance, (int) (offsetIntersection.x - textViewDistance.getWidth() / 2 - Math.cos(angle) * halfSize), (int) (offsetIntersection.y - textViewDistance.getHeight() / 2 - Math.sin(angle) * halfSize));
            textViewDistance.setRotation((float) Math.toDegrees(angle) + 90);
        } else if (Math.PI / 4f < angle && angle < Math.PI * 3f / 4f) {
            moveView(textViewDistance, (int) (offsetIntersection.x - textViewDistance.getWidth() / 2 - Math.cos(angle) * halfSize), (int) (offsetIntersection.y - textViewDistance.getHeight() / 2 - Math.sin(angle) * halfSize));
            textViewDistance.setRotation((float) Math.toDegrees(angle) + 90 + 180);
        } else {
            moveView(textViewDistance, (int) (offsetIntersection.x - Math.ceil(Math.cos(angle)) * textViewDistance.getWidth() - Math.round(Math.cos(angle)) *  .73 * halfSize), (int) (offsetIntersection.y - textViewDistance.getHeight() / 2));
            textViewDistance.setRotation(0);
        }

    }

    private void hideView() {
        if (textViewDistance != null)
            textViewDistance.setVisibility(View.GONE);
        setVisibility(View.GONE);
    }

    private static void moveView(View view, int x, int y) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
        lp.leftMargin = x;
        lp.topMargin = y;
        view.setLayoutParams(lp);
    }

    @Nullable
    private static PointF locationOutsideScreen(Point screenLocation, int screenEdgeOffset, int width, int height) {
        PointF leftTop = new PointF(screenEdgeOffset, screenEdgeOffset);
        PointF leftBottom = new PointF(screenEdgeOffset, height - screenEdgeOffset);
        PointF rightTop = new PointF(width - screenEdgeOffset, screenEdgeOffset);
        PointF rightBottom = new PointF(width - screenEdgeOffset, height - screenEdgeOffset);
        PointF center = new PointF(width / 2f, height / 2f);
        PointF location = new PointF(screenLocation.x, screenLocation.y);

        PointF intersecton;

        intersecton = lineSegmentsIntersect(leftTop, rightTop, center, location);
        if (intersecton != null) return intersecton;

        intersecton = lineSegmentsIntersect(rightTop, rightBottom, center, location);
        if (intersecton != null) return intersecton;

        intersecton = lineSegmentsIntersect(rightBottom, leftBottom, center, location);
        if (intersecton != null) return intersecton;

        intersecton = lineSegmentsIntersect(leftBottom, leftTop, center, location);
        return intersecton;
    }


    // Determines if the lines AB and CD intersect.
    private static PointF lineSegmentsIntersect(PointF A, PointF B, PointF C, PointF D) {
        PointF CmP = new PointF(C.x - A.x, C.y - A.y);
        PointF r = new PointF(B.x - A.x, B.y - A.y);
        PointF s = new PointF(D.x - C.x, D.y - C.y);

        float CmPxr = CmP.x * r.y - CmP.y * r.x;
        float CmPxs = CmP.x * s.y - CmP.y * s.x;
        float rxs = r.x * s.y - r.y * s.x;

        // Lines are collinear, and so intersect if they have any overlap
        if (CmPxr == 0f)
            return null;
//                    ((C.x - A.x < 0f) != (C.x - B.x < 0f))
//                    || ((C.y - A.y < 0f) != (C.y - B.y < 0f));

        // Lines are parallel
        if (rxs == 0f)
            return null;

        float rxsr = 1f / rxs;
        float t = CmPxs * rxsr;
        float u = CmPxr * rxsr;

        if (t >= 0f && t <= 1f && u >= 0f && u <= 1f)
            return new PointF(A.x + r.x * t, A.y + r.y * t);

        return null;
    }


}
