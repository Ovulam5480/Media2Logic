package Media2Logic.LogicImage.ImageToLogic.ColorBlock;

import arc.func.Intc2;
import arc.math.geom.Point2;

public class ColorTirEdge extends ColorTir {
    public ColorTirEdge(int x1, int y1, int x2, int y2, int x3, int y3) {
        super(x1, y1, x2, y2, x3, y3);
    }

    public ColorTirEdge(Point2 p1, Point2 p2, Point2 p3){
        super(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
    }

    @Override
    public void each(Intc2 intc2) {
        intc2.get(x1, y1);
        intc2.get(x2, y2);
        intc2.get(x3, y3);

        int minx = Math.min(x1, Math.min(x2, x3));
        int miny = Math.min(y1, Math.min(y2, y3));
        int maxx = Math.max(x1, Math.max(x2, x3));
        int maxy = Math.max(y1, Math.max(y2, y3));

        for (int x = minx; x <= maxx; x++) {
            for (int y = miny; y <= maxy; y++) {
                if (isInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
                    intc2.get(x, y);
                }
            }
        }
    }

    public static boolean isInTriangle(float px, float py, float ax, float ay, float bx, float by, float cx, float cy){
        float px1 = px - ax;
        float py1 = py - ay;
        boolean side12 = (bx - ax) * py1 - (by - ay) * px1 >= 0;//这里和下面改为了>=而非原来的>
        if((cx - ax) * py1 - (cy - ay) * px1 > 0 == side12) return false;
        return (cx - bx) * (py - by) - (cy - by) * (px - bx) >= 0 == side12;
    }
}
