package Media2Logic.LogicImage.ImageToLogic.LowPoly;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.GridMap;

/**
 * Created by nce3xin on 2017/10/23.
 */
public class Sobel {
    private static final int[][] kernelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    private static final int[][] kernelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
    private static final float sqrt2 = Mathf.sqrt2;
    private static final GridMap<Float> grayValues = new GridMap<>();
    private static final Point2[] d8c = {
            new Point2(1, 0),
            new Point2(1, 1),
            new Point2(0, 1),
            new Point2(-1, 1),
            new Point2(-1, 0),
            new Point2(-1, -1),
            new Point2(0, -1),
            new Point2(1, -1),
            new Point2(0, 0)
    };

    public static void sobel(Pixmap image, SobelCallback callback) {
        grayValues.clear();

        int w = image.getWidth();
        int h = image.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                float pixelX = 0;
                float pixelY = 0;

                for (Point2 point2 : d8c) {
                    int xx = x + point2.x;
                    int yy = y + point2.y;

                    if(!grayValues.containsKey(xx, yy)){
                        grayValues.put(xx, yy, getGrayValue(image, Mathf.clamp(xx, 0, image.width - 1), Mathf.clamp(yy, 0, image.height - 1)));
                    }

                    float gray = grayValues.get(xx, yy);

                    pixelX += kernelX[point2.x + 1][point2.y + 1] * gray;
                    pixelY += kernelY[point2.x + 1][point2.y + 1] * gray;
                }

                Vec2 tempVec = new Vec2(pixelX, pixelY);
                float magnitude = tempVec.scl(sqrt2 / 4).len();
                callback.call(magnitude, x, y);
            }
        }
    }

    private static float getGrayValue(Pixmap image, int x, int y) {
        Color color = new Color();
        color.rgba8888(image.get(x, y));
        return (color.r + color.g + color.b) / 3;
    }

    public interface SobelCallback {
        void call(float magnitude, int x, int y);
    }
}
