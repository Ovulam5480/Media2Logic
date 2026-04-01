package Media2Logic.LogicImage.ImageToLogic.ColorBlock;

import arc.func.Intc2;
import arc.graphics.Color;

public class ColorBackgroundOnce extends ColorShape{
    public ColorBackgroundOnce(Color color) {
        setColor(color);
    }

    @Override
    public int getLogicLines() {
        return 4;
    }

    @Override
    public void toInstruction(StringBuilder code) {
        code.append("jump 4 equal clear 1\n");
        code.append("draw color ")
                .append(color.r * 255).append(" ")
                .append(color.g * 255).append(" ")
                .append(color.b * 255).append(" ")
                .append(color.a * 255).append(" ")
                .append("0 0\n");

        code.append("draw rect ")
                .append(0).append(" ")
                .append(0).append(" ")
                .append(8192).append(" ")
                .append(8192).append(" ")
                .append("0 0\n");
        code.append("set clear 1\n");
    }

    @Override
    public void each(Intc2 intc2) {}
}
