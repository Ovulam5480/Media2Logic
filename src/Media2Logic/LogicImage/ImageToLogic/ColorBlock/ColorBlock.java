package Media2Logic.LogicImage.ImageToLogic.ColorBlock;

import arc.func.Intc2;
import arc.graphics.Color;
import arc.math.geom.QuadTree;
import arc.math.geom.Rect;

public class ColorBlock extends ColorShape implements QuadTree.QuadTreeObject {
    public int x, y;
    public int width, height;

    public ColorBlock() {
    }

    public ColorBlock(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public ColorBlock set(Color color, int x, int y, int w, int h){
        this.color.set(color);
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;

        return this;
    }

    public ColorBlock set(ColorBlock colorBlock){
        this.color.set(colorBlock.color);
        this.x = colorBlock.x;
        this.y = colorBlock.y;
        this.width = colorBlock.width;
        this.height = colorBlock.height;

        return this;
    }

    public void setRect(Rect rect){
        this.x = (int) rect.x;
        this.y = (int) rect.y;
        this.width = (int) rect.width;
        this.height = (int) rect.height;
    }

    public void setRect(int x, int y, int w, int h){
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    @Override
    public void hitbox(Rect out) {
        out.set(x, y, width, height);
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

        code.append("draw rect ")
                .append(x).append(" ")
                .append(y).append(" ")
                .append(width).append(" ")
                .append(height).append(" ")
                .append("0 0\n");
    }

    @Override
    public void each(Intc2 intc2) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                intc2.get(x + i, y + j);
            }
        }
    }
}