package com.example.quanlychitieu.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class PieChartLineRenderer implements IMarker {

    private final PieChart chart;
    private final Paint mLinePaint;
    private final Path mPath;

    public PieChartLineRenderer(PieChart chart) {
        this.chart = chart;
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.STROKE);

        mLinePaint.setColor(Color.parseColor("#9E9E9E")); // Màu xám dẫn đường thanh lịch
        mLinePaint.setStrokeWidth(2.5f);

        mPath = new Path();
    }

    @Override public MPPointF getOffset() { return new MPPointF(0, 0); }
    @Override public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) { return new MPPointF(0, 0); }
    @Override public void refreshContent(Entry e, Highlight highlight) {}

    @Override
    public void draw(Canvas canvas, float posX, float posY) {
        if (chart.getData() == null || chart.getData().getDataSet() == null) return;

        float centerX = chart.getWidth() / 2f;
        float centerY = chart.getHeight() / 2f;

        float radius = chart.getRadius() * 0.9f;

        float[] drawAngles = chart.getDrawAngles();
        float[] absoluteAngles = chart.getAbsoluteAngles();
        float rotationAngle = chart.getRotationAngle();

        int entryCount = chart.getData().getDataSet().getEntryCount();

        for (int i = 0; i < entryCount; i++) {
            float currentAbsoluteAngle = (i == 0) ? 0f : absoluteAngles[i - 1];
            float sliceCenterAngle = rotationAngle + currentAbsoluteAngle + (drawAngles[i] / 2f);

            sliceCenterAngle = (sliceCenterAngle % 360 + 360) % 360;
            float angleRad = (float) Math.toRadians(sliceCenterAngle);

            float startX = centerX + (float) Math.cos(angleRad) * radius;
            float startY = centerY + (float) Math.sin(angleRad) * radius;

            float breakRadius = radius + 40f;
            float breakX = centerX + (float) Math.cos(angleRad) * breakRadius;
            float breakY = centerY + (float) Math.sin(angleRad) * breakRadius;

            float endX = (breakX > centerX) ? (chart.getWidth() - 15f) : 15f;
            float endY = breakY;

            mPath.reset();
            mPath.moveTo(startX, startY);
            mPath.lineTo(breakX, breakY);
            mPath.lineTo(endX, endY);

            canvas.drawPath(mPath, mLinePaint);
        }
    }
}