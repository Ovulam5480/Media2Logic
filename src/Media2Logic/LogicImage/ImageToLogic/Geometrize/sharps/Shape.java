package Media2Logic.LogicImage.ImageToLogic.Geometrize.sharps;

import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorShape;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.Scanline;
import arc.graphics.Color;
import arc.struct.Seq;

/**
 * 形状接口
 */
public interface Shape {
    /**
     * 光栅化形状为扫描线
     */
    Seq<Scanline> rasterize();

    /**
     * 随机变异
     */
    void mutate(int xBound, int yBound);

    /**
     * 克隆形状
     */
    Shape cloneShape(int xBound, int yBound);

    /**
     * 获取类型
     */
    int getType();

    /**
     * 获取原始数据
     */
    int[] getRawShapeData();

    /**
     * 平移
     */
    void translate(int x, int y);

    /**
     * 转换为 ColorShape
     */
    ColorShape toColorShape(Color color);
}
