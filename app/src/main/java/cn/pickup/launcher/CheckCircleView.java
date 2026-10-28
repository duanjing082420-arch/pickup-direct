package cn.pickup.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * 待办清单风格的大号勾选圈：空心圆 + 一笔画对勾。
 * 点一下画对勾（对应原型里的 stroke-dashoffset 动画），再点由外层负责恢复。
 */
final class CheckCircleView extends View {
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path checkPath = new Path();
    private final Path drawPath = new Path();
    private final PathMeasure measure = new PathMeasure();

    private int idleColor;
    private int activeColor;
    private boolean checked;
    private float checkProgress;
    private ValueAnimator animator;

    CheckCircleView(Context context, int idleColor, int activeColor) {
        super(context);
        this.idleColor = idleColor;
        this.activeColor = activeColor;
        float density = context.getResources().getDisplayMetrics().density;
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2.5f * density);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(3.5f * density);
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
        // 对勾锚点按 46dp 视口规划，onDraw 里按实际尺寸等比缩放
        checkPath.moveTo(14, 23.5f);
        checkPath.lineTo(20.5f, 30);
        checkPath.lineTo(32, 17);
        measure.setPath(checkPath, false);
        setClickable(true);
        setFocusable(true);
    }

    boolean isCheckedState() {
        return checked;
    }

    /** checked=true 时带一笔画动画；false 直接复位到空心圈。 */
    void setCheckedState(boolean checked, boolean animate) {
        this.checked = checked;
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (!animate || !checked) {
            checkProgress = checked ? 1f : 0f;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(420);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            checkProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - circlePaint.getStrokeWidth();
        circlePaint.setColor(checked ? activeColor : idleColor);
        canvas.drawCircle(cx, cy, radius, circlePaint);

        if (checkProgress > 0f) {
            canvas.save();
            canvas.scale(w / 46f, h / 46f, cx, cy);
            checkPaint.setColor(activeColor);
            drawPath.reset();
            measure.getSegment(0f, measure.getLength() * checkProgress, drawPath, true);
            canvas.drawPath(drawPath, checkPaint);
            canvas.restore();
        }
    }
}
