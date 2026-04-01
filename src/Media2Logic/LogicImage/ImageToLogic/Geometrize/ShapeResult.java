package Media2Logic.LogicImage.ImageToLogic.Geometrize;

import Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps.Shape;
import arc.graphics.Color;

/**
 * 形状结果 - 包含添加的形状的信息
 */
public class ShapeResult {
    public final double score;
    public final Color color;
    public final Shape shape;

    public ShapeResult(double score, Color color, Shape shape) {
        this.score = score;
        this.color = color;
        this.shape = shape;
    }
}
