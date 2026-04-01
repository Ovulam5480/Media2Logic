package Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps;

import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorCircle;
import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorShape;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.GeometrizeCore;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.Scanline;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.Seq;

import java.util.Random;

/**
 * 圆形形状实现
 */
public class Circle implements Shape {
    public int x, y, radius;
    private final int xBound, yBound;
    private static final Random random = new Random();

    public Circle(int xBound, int yBound) {
        this.xBound = xBound;
        this.yBound = yBound;
        
        this.x = random.nextInt(xBound);
        this.y = random.nextInt(yBound);
        this.radius = random.nextInt(32) + 1;
    }

    public Circle(int xBound, int yBound, int x, int y, int radius) {
        this.xBound = xBound;
        this.yBound = yBound;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override
    public Seq<Scanline> rasterize() {
        return GeometrizeCore.rasterizeCircle(x, y, radius, xBound, yBound);
    }

    @Override
    public void mutate(int xBound, int yBound) {
        if (Mathf.chance(0.5f)) {
            x = Mathf.clamp(x + Mathf.random(-16, 16), 0, xBound - 1);
            y = Mathf.clamp(y + Mathf.random(-16, 16), 0, yBound - 1);
        } else {
            radius = Mathf.clamp(radius + Mathf.random(-16, 16), 1, 
                                Math.min(xBound, yBound) - 1);
        }
    }

    @Override
    public Shape cloneShape(int xBound, int yBound) {
        return new Circle(xBound, yBound, x, y, radius);
    }

    @Override
    public int getType() {
        return 32; // CIRCLE
    }

    @Override
    public int[] getRawShapeData() {
        return new int[]{x, y, radius};
    }

    @Override
    public void translate(int x, int y) {
        this.x += x;
        this.y += y;
    }

    @Override
    public ColorShape toColorShape(Color color) {
        return new ColorCircle(x, yBound - 1 - y, radius).setColor(color);
    }
}
