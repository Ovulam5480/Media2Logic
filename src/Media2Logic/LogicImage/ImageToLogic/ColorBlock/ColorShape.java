package Media2Logic.LogicImage.ImageToLogic.ColorBlock;

import arc.func.Intc2;
import arc.graphics.Color;
import arc.graphics.Pixmap;

public abstract class ColorShape {
    public Color color = new Color();

    public ColorShape setColor(Color color){
        this.color.set(color);
        return this;
    }
    public abstract int getLogicLines();

    public abstract void toInstruction(StringBuilder code);

    public abstract void each(Intc2 intc2);

    public void decompressor(Pixmap pixmap, boolean blend){
        if(blend){
            each((x, y) -> pixmap.set(x, y, Pixmap.blend(pixmap.get(x, y), color.rgba8888())));
        }else {
            each((x, y) -> pixmap.set(x, y, color));
        }
    }
}
