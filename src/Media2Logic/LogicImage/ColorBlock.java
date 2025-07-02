package Media2Logic.LogicImage;

import arc.graphics.Color;
import arc.math.geom.QuadTree;
import arc.math.geom.Rect;

public class ColorBlock implements QuadTree.QuadTreeObject {
    public Color color = new Color();
    public int x, y;
    public int width, height;

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
}