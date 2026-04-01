package Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps;

import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorBlock;
import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorShape;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.GeometrizeCore;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.Scanline;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.Seq;

import java.util.Random;

/**
 * 矩形形状实现
 */
public class Rectangle implements Shape {
    public int x1, y1, x2, y2;
    private final int xBound, yBound;
    private static final Random random = new Random();

    public Rectangle(int xBound, int yBound) {
        this.xBound = xBound;
        this.yBound = yBound;
        
        this.x1 = random.nextInt(xBound);
        this.y1 = random.nextInt(yBound);
        int value = this.x1 + random.nextInt(32) + 1;
        int max = xBound - 1;
        this.x2 = Math.max(0, Math.min(value, max));
        value = this.y1 + random.nextInt(32) + 1;
        max = yBound - 1;
        this.y2 = Math.max(0, Math.min(value, max));
    }

    public Rectangle(int xBound, int yBound, int x1, int y1, int x2, int y2) {
        this.xBound = xBound;
        this.yBound = yBound;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public Seq<Scanline> rasterize() {
        return GeometrizeCore.rasterizeRectangle(x1, y1, x2, y2, xBound, yBound);
    }

    @Override
    public void mutate(int xBound, int yBound) {
        if (Mathf.chance(0.5f)) {
            x1 = Mathf.clamp(x1 + Mathf.random(-16, 16), 0, xBound - 1);
            y1 = Mathf.clamp(y1 + Mathf.random(-16, 16), 0, yBound - 1);
        } else {
            x2 = Mathf.clamp(x2 + Mathf.random(-16, 16), 0, xBound - 1);
            y2 = Mathf.clamp(y2 + Mathf.random(-16, 16), 0, yBound - 1);
        }
    }

    @Override
    public Shape cloneShape(int xBound, int yBound) {
        return new Rectangle(xBound, yBound, x1, y1, x2, y2);
    }

    @Override
    public int getType() {
        return 1; // RECTANGLE
    }

    @Override
    public int[] getRawShapeData() {
        return new int[]{
            Math.min(x1, x2), Math.min(y1, y2),
            Math.max(x1, x2), Math.max(y1, y2)
        };
    }

    @Override
    public void translate(int x, int y) {
        x1 += x;
        x2 += x;
        y1 += y;
        y2 += y;
    }

    @Override
    public ColorShape toColorShape(Color color) {
        ColorBlock block = new ColorBlock();
        block.set(color,
            Math.min(x1, x2), Math.min(yBound - 1 - y1, yBound - 1 - y2),
            Math.abs(x2 - x1) + 1, Math.abs(y2 - y1) + 1);
        return block;
    }
}
