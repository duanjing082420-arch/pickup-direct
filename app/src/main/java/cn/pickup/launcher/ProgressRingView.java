package cn.pickup.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * 汇总卡里的进度环：外圈进度弧 + 中间「已取/总数」。
 * 分母 = 列表里的取件码总数（已取 + 未取）。
 */
final class ProgressRingView extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private int taken;
    private int total;
    private float fraction;
    private ValueAnimator animator;

    ProgressRingView(Context context, int trackColor, int progressColor, int textColor, int labelColor) {
        super(context);
        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(5f * density);
        trackPaint.setColor(trackColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(5f * density);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(progressColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(13f * scaledDensity);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setColor(textColor);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(8f * scaledDensity);
        labelPaint.setColor(labelColor);
    }

    /** taken/total 变化时从当前比例平滑过渡到新比例。 */
    void setProgress(int takenCount, int totalCount, boolean animate) {
        taken = takenCount;
        total = Math.max(totalCount, 0);
        float target = total == 0 ? 0f : taken / (float) total;
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (!animate) {
            fraction = target;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(fraction, target);
        animator.setDuration(360);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            fraction = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float halfStroke = trackPaint.getStrokeWidth() / 2f + 1f;
        arcBounds.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);
        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint);
        if (fraction > 0f) {
            canvas.drawArc(arcBounds, -90f, 360f * fraction, false, progressPaint);
        }

        String number = taken + "/" + total;
        float numberY = h / 2f - labelPaint.getTextSize() * 0.4f;
        canvas.drawText(number, w / 2f, numberY, textPaint);
        canvas.drawText("已取", w / 2f, h / 2f + labelPaint.getTextSize() * 1.6f, labelPaint);
    }
}
