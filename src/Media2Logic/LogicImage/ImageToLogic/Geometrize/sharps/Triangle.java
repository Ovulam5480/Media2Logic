package Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps;

import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorShape;
import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorTir;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.GeometrizeCore;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.Scanline;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.Seq;

import java.util.Random;

/**
 * 三角形形状实现
 */
public class Triangle implements Shape {
    public int x1, y1, x2, y2, x3, y3;
    private final int xBound, yBound;
    private static final Random random = new Random();

    public Triangle(int xBound, int yBound) {
        this.xBound = xBound;
        this.yBound = yBound;
        
        this.x1 = random.nextInt(xBound);
        this.y1 = random.nextInt(yBound);
        this.x2 = random.nextInt(xBound);
        this.y2 = random.nextInt(yBound);
        this.x3 = random.nextInt(xBound);
        this.y3 = random.nextInt(yBound);
    }

    public Triangle(int xBound, int yBound, int x1, int y1, int x2, int y2, 
                   int x3, int y3) {
        this.xBound = xBound;
        this.yBound = yBound;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
    }

    @Override
    public Seq<Scanline> rasterize() {
        return GeometrizeCore.rasterizeTriangle(
            x1, y1, x2, y2, x3, y3, xBound, yBound);
    }

    @Override
    public void mutate(int xBound, int yBound) {
        int r = Mathf.random(3);
        if (r == 0) {
            x1 = Mathf.clamp(x1 + Mathf.random(-16, 16), 0, xBound - 1);
            y1 = Mathf.clamp(y1 + Mathf.random(-16, 16), 0, yBound - 1);
        } else if (r == 1) {
            x2 = Mathf.clamp(x2 + Mathf.random(-16, 16), 0, xBound - 1);
            y2 = Mathf.clamp(y2 + Mathf.random(-16, 16), 0, yBound - 1);
        } else {
            x3 = Mathf.clamp(x3 + Mathf.random(-16, 16), 0, xBound - 1);
            y3 = Mathf.clamp(y3 + Mathf.random(-16, 16), 0, yBound - 1);
        }
    }

    @Override
    public Shape cloneShape(int xBound, int yBound) {
        return new Triangle(xBound, yBound, x1, y1, x2, y2, x3, y3);
    }

    @Override
    public int getType() {
        return 4; // TRIANGLE
    }

    @Override
    public int[] getRawShapeData() {
        return new int[]{x1, y1, x2, y2, x3, y3};
    }

    @Override
    public void translate(int x, int y) {
        x1 += x; x2 += x; x3 += x;
        y1 += y; y2 += y; y3 += y;
    }

    @Override
    public ColorShape toColorShape(Color color) {
        return new ColorTir(x1, yBound - 1 - y1, x2, yBound - 1 - y2, x3, yBound - 1 - y3).setColor(color);
    }
}
