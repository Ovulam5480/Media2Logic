package Media2Logic.LogicImage.ImageToLogic.Geometrize;

/**
 * 扫描线 - 表示一行像素
 */
public class Scanline {
    public int y;
    public int x1;
    public int x2;

    public Scanline(int y, int x1, int x2) {
        this.y = y;
        this.x1 = x1;
        this.x2 = x2;
    }
}
