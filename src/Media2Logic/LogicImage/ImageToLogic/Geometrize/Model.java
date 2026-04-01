package Media2Logic.LogicImage.ImageToLogic.Geometrize;

import Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps.Circle;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps.Rectangle;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps.Shape;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps.Triangle;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.struct.Seq;

/**
 * Geometrize 模型 - 核心算法实现
 */
public class Model {
    private final Pixmap target;
    private final Pixmap current;
    private final Pixmap buffer;
    public final Color avgColor;
    private double lastScore;

    public Model(Pixmap target) {
        this.target = target;
        this.current = new Pixmap(target.width, target.height);
        this.buffer = new Pixmap(target.width, target.height);

        // 使用图像平均颜色作为初始背景
        avgColor = GeometrizeCore.getAverageColor(target);
        current.draw(createFilledPixmap(target.width, target.height, avgColor));

        this.lastScore = GeometrizeCore.differenceFull(target, current);
    }

    public Seq<ShapeResult> step(ImageRunnerOptions options, int amount) {
        int[] shapeTypes = options.shapeTypes;
        int alpha = options.alpha;
        int shapeCount = options.shapeCount;
        int maxShapeMutations = options.maxShapeMutations;

        Seq<ShapeResult> shapes = new Seq<>();

        for (int step = 0; step < options.maxSteps * amount; step++) {
            ShapeResult bestShape = null;
            double bestScore = Double.MAX_VALUE;

            // 随机形状生成阶段
            for (int i = 0; i < shapeCount; i++) {
                Shape shape = createRandomShape(shapeTypes, target.width, target.height);

                Seq<Scanline> lines = shape.rasterize();
                if (lines.isEmpty()) continue;

                Color color = GeometrizeCore.computeColor(target, current, lines, alpha);

                // 复制当前状态到 buffer
                buffer.draw(current);
                // 在 buffer 上绘制新形状
                GeometrizeCore.drawLines(buffer, color, lines);

                // 计算分数（增量更新）
                double score = GeometrizeCore.differencePartial(
                    target, current, buffer, lastScore, lines);

                if (i == 0 || score < bestScore) {
                    bestScore = score;
                    bestShape = new ShapeResult(score, color, shape);
                }
            }

            if (bestShape == null) break;

            // 变异优化阶段
            for (int i = 0; i < maxShapeMutations; i++) {
                Shape shape = bestShape.shape.cloneShape(target.width, target.height);
                if (i > 0) {
                    shape.mutate(target.width, target.height);
                }

                Seq<Scanline> lines = shape.rasterize();
                if (lines.isEmpty()) continue;

                Color color = GeometrizeCore.computeColor(target, current, lines, alpha);

                buffer.draw(current);
                GeometrizeCore.drawLines(buffer, color, lines);

                double score = GeometrizeCore.differencePartial(
                    target, current, buffer, lastScore, lines);

                if (score < bestScore) {
                    bestScore = score;
                    bestShape = new ShapeResult(score, color, 
                                               shape.cloneShape(target.width, target.height));
                }
            }

            // 应用最佳形状到当前图像
            Seq<Scanline> lines = bestShape.shape.rasterize();
            GeometrizeCore.drawLines(current, bestShape.color, lines);
            lastScore = bestScore;

            shapes.add(bestShape);
        }

        return shapes;
    }

    private Shape createRandomShape(int[] shapeTypes, int width, int height) {
        int type = shapeTypes[(int)(Math.random() * shapeTypes.length)];

        return switch (type) {
            case 1 -> new Triangle(width, height);
            case 2 -> new Circle(width, height);
            default -> new Rectangle(width, height);
        };
    }

    private Pixmap createFilledPixmap(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.set(x, y, color.rgba8888());
            }
        }
        return pixmap;
    }

    public Pixmap getCurrent() {
        return current;
    }

    public double getLastScore() {
        return lastScore;
    }
}
