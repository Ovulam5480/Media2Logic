package Media2Logic.LogicImage.ImageToLogic.ColorBlock;

import arc.func.Intc2;
import arc.math.Mathf;

public class ColorCircle extends ColorShape{
    public int x, y, radius;

    public ColorCircle(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
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

        code.append("draw poly ")
                .append(x).append(" ")
                .append(y).append(" ")
                .append(32).append(" ")
                .append(radius).append(" ")
                .append("0 0\n");
    }

    @Override
    public void each(Intc2 intc2) {
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                if(Mathf.dst(i, j) <= radius) intc2.get(x + i, y + j);
            }
        }
    }
}
