package Media2Logic.LogicImage;

import arc.graphics.Pixmap;

public class Scaler {
    //贴图拉伸

    //最近邻插值算法
    public static void nearestNeighbor(Pixmap src, Pixmap dst) {
        final float scaleX = (float) src.getWidth() / dst.getWidth();
        final float scaleY = (float) src.getHeight() / dst.getHeight();

        for (int y = 0; y < dst.getHeight(); y++) {
            int srcY = (int) (y * scaleY);
            for (int x = 0; x < dst.getWidth(); x++) {
                int srcX = (int) (x * scaleX);
                dst.setRaw(x, y, src.getRaw(srcX, srcY));
            }
        }
    }

    //双线性插值算法
    public static void bilinearInterpolation(Pixmap src, Pixmap dst) {
        final float scaleX = (float) (src.getWidth() - 1) / dst.getWidth();
        final float scaleY = (float) (src.getHeight() - 1) / dst.getHeight();

        for (int y = 0; y < dst.getHeight(); y++) {
            float srcY = y * scaleY;
            int y1 = (int) srcY;
            int y2 = Math.min(y1 + 1, src.getHeight() - 1);
            float yFrac = srcY - y1;

            for (int x = 0; x < dst.getWidth(); x++) {
                float srcX = x * scaleX;
                int x1 = (int) srcX;
                int x2 = Math.min(x1 + 1, src.getWidth() - 1);
                float xFrac = srcX - x1;

                int c00 = src.getRaw(x1, y1);
                int c10 = src.getRaw(x2, y1);
                int c01 = src.getRaw(x1, y2);
                int c11 = src.getRaw(x2, y2);

                int color = blendColors(
                        blendColors(c00, c10, xFrac),
                        blendColors(c01, c11, xFrac),
                        yFrac
                );

                dst.setRaw(x, y, color);
            }
        }
    }

    //混合颜色
    public static int blendColors(int c1, int c2, float ratio) {
        if (ratio <= 0) return c1;
        if (ratio >= 1) return c2;

        float invRatio = 1 - ratio;
        int r = (int)(((c1 >>> 24) & 0xFF) * invRatio + ((c2 >>> 24) & 0xFF) * ratio);
        int g = (int)(((c1 >>> 16) & 0xFF) * invRatio + ((c2 >>> 16) & 0xFF) * ratio);
        int b = (int)(((c1 >>> 8) & 0xFF) * invRatio + ((c2 >>> 8) & 0xFF) * ratio);
        int a = (int)((c1 & 0xFF) * invRatio + (c2 & 0xFF) * ratio);

        return (r << 24) | (g << 16) | (b << 8) | a;
    }
}
