package project.jaehyeok.chatchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// 프로필 이미지의 테두리를 둥글게 표현하기 위한 커스텀 이미지뷰
public class RoundProfileImageView extends androidx.appcompat.widget.AppCompatImageView {
    // 값이 높을수록 둥글어진다
    public static float radius = 100.0f;

    public RoundProfileImageView(@NonNull Context context) {
        super(context);
    }

    public RoundProfileImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundProfileImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setRadius (Float radius) {
        this.radius = radius;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path clipPath = new Path();
        RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);

        super.onDraw(canvas);
    }
}
