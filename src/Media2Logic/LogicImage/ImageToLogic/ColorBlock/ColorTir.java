package Media2Logic.LogicImage.ImageToLogic.ColorBlock;

import arc.func.Intc2;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;

public class ColorTir extends ColorShape {
    public int x1, y1, x2, y2, x3, y3;

    public ColorTir(int x1, int y1, int x2, int y2, int x3, int y3) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
    }

    public ColorTir(Point2 p1, Point2 p2, Point2 p3){
        this(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
    }

    @Override
    public int getLogicLines() {
        return 2;
    }

    @Override
    public void toInstruction(StringBuilder code) {
        code.append("draw color ")
                .append(color.r * 255).append(" ")
                .append(color.g * 255).append(" ")
                .append(color.b * 255).append(" ")
                .append(color.a * 255).append(" ")
                .append("0 0\n");

        code.append("draw triangle ")
                .append(x1).append(" ")
                .append(y1).append(" ")
                .append(x2).append(" ")
                .append(y2).append(" ")
                .append(x3).append(" ")
                .append(y3).append("\n");
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
                if (Intersector.isInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
                    intc2.get(x, y);
                }
            }
        }
    }

    public Point2 one(){
        return new Point2(x1, y1);
    }

    public Point2 two(){
        return new Point2(x2, y2);
    }

    public Point2 three(){
        return new Point2(x3, y3);
    }
}
