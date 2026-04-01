package Media2Logic.LogicImage.ImageToLogic.ColorBlock;

import arc.func.Intc2;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;

public class ColorPoint extends ColorShape {
    int x, y;

    public ColorPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public ColorPoint(Point2 p) {
        this.x = p.x;
        this.y = p.y;
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
                .append(3).append(" ")
                .append(3).append(" ")
                .append("0 0\n");
    }

    int radius = 0;
    Color color = Color.acid;
    @Override
    public void each(Intc2 intc2) {
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                if(Mathf.dst(i, j) <= radius) intc2.get(x + i, y + j);
            }
        }
    }
}
