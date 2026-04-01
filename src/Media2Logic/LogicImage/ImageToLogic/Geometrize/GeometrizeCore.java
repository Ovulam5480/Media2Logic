package Media2Logic.LogicImage.ImageToLogic.Geometrize;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;

public class GeometrizeCore {

    /**
     * 计算最佳颜色（考虑 alpha 混合）
     */
    public static Color computeColor(Pixmap target, Pixmap current, Seq<Scanline> lines, int alpha) {
        if (lines.isEmpty()) {
            return new Color(0, 0, 0, 0);
        }

        long totalRed = 0;
        long totalGreen = 0;
        long totalBlue = 0;
        long count = 0;

        // Alpha 预乘因子
        float aFactor = 257.0f * 255.0f / alpha;

        for (Scanline line : lines) {
            int y = line.y;
            for (int x = line.x1; x <= line.x2; x++) {
                if (y < 0 || y >= target.height || x < 0 || x >= target.width) continue;

                int targetColor = target.get(x, y);
                int currentColor = current.get(x, y);

                int tr = (targetColor >> 24) & 0xFF;
                int tg = (targetColor >> 16) & 0xFF;
                int tb = (targetColor >> 8) & 0xFF;

                int cr = (currentColor >> 24) & 0xFF;
                int cg = (currentColor >> 16) & 0xFF;
                int cb = (currentColor >> 8) & 0xFF;

                // 正确的 alpha 混合公式
                totalRed += (long)((tr - cr) * aFactor + cr * 257);
                totalGreen += (long)((tg - cg) * aFactor + cg * 257);
                totalBlue += (long)((tb - cb) * aFactor + cb * 257);
                count++;
            }
        }

        int r = (int)(totalRed / count) >> 8;
        int g = (int)(totalGreen / count) >> 8;
        int b = (int)(totalBlue / count) >> 8;

        r = Mathf.clamp(r, 0, 255);
        g = Mathf.clamp(g, 0, 255);
        b = Mathf.clamp(b, 0, 255);

        return new Color(r / 255.0f, g / 255.0f, b / 255.0f, alpha / 255.0f);
    }

    /**
     * 绘制扫描线到图像（使用 Alpha 混合）
     */
    public static void drawLines(Pixmap image, Color color, Seq<Scanline> lines) {
        // 创建临时 Pixmap 用于绘制半透明形状
        Pixmap temp = new Pixmap(image.width, image.height);
        
        // 在临时 Pixmap 上绘制填充形状（不透明）
        int opaqueColor = color.rgba8888();
        
        for (Scanline line : lines) {
            int y = line.y;
            for (int x = line.x1; x <= line.x2; x++) {
                temp.set(x, y, opaqueColor);
            }
        }

        image.draw(temp, 0, 0, true);
        
        temp.dispose();
    }

    /**
     * 计算完整差异（归一化）
     */
    public static double differenceFull(Pixmap first, Pixmap second) {
        int width = first.width;
        int height = first.height;
        long total = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int f = first.get(x, y);
                int s = second.get(x, y);

                int fr = (f >>> 24) & 0xFF;
                int fg = (f >>> 16) & 0xFF;
                int fb = (f >>> 8) & 0xFF;
                int fa = f & 0xFF;

                int sr = (s >>> 24) & 0xFF;
                int sg = (s >>> 16) & 0xFF;
                int sb = (s >>> 8) & 0xFF;
                int sa = s & 0xFF;

                int dr = fr - sr;
                int dg = fg - sg;
                int db = fb - sb;
                int da = fa - sa;

                total += dr * dr + dg * dg + db * db + da * da;
            }
        }

        return Math.sqrt(total / (double)(width * height * 4)) / 255.0;
    }

    /**
     * 计算部分差异（增量更新）
     */
    public static double differencePartial(Pixmap target, Pixmap before, Pixmap after, 
                                          double lastScore, Seq<Scanline> lines) {
        long totalPixels = (long)target.width * target.height;
        // 从上次总分开始
        long total = (long)((lastScore * 255.0) * (lastScore * 255.0) * totalPixels * 4);

        for (Scanline line : lines) {
            int y = line.y;
            for (int x = line.x1; x <= line.x2; x++) {
                if (y < 0 || y >= target.height || x < 0 || x >= target.width) continue;

                int t = target.get(x, y);
                int b = before.get(x, y);
                int a = after.get(x, y);

                int tr = (t >>> 24) & 0xFF;
                int tg = (t >>> 16) & 0xFF;
                int tb = (t >>> 8) & 0xFF;
                int ta = t & 0xFF;

                int br = (b >>> 24) & 0xFF;
                int bg = (b >>> 16) & 0xFF;
                int bb = (b >>> 8) & 0xFF;
                int ba = b & 0xFF;

                int ar = (a >>> 24) & 0xFF;
                int ag = (a >>> 16) & 0xFF;
                int ab = (a >>> 8) & 0xFF;
                int aa = a & 0xFF;

                // 减去之前的误差
                int dtbr = tr - br;
                int dtbg = tg - bg;
                int dtbb = tb - bb;
                int dtba = ta - ba;

                // 加上新的误差
                int dtar = tr - ar;
                int dtag = tg - ag;
                int dtab = tb - ab;
                int dtaa = ta - aa;

                total -= (dtbr * dtbr + dtbg * dtbg + dtbb * dtbb + dtba * dtba);
                total += (dtar * dtar + dtag * dtag + dtab * dtab + dtaa * dtaa);
            }
        }

        return Math.sqrt(total / (double)(totalPixels * 4)) / 255.0;
    }

    /**
     * 获取图像平均颜色
     */
    public static Color getAverageColor(Pixmap pixmap) {
        float totalR = 0, totalG = 0, totalB = 0;
        int count = 0;
        Color tempColor = new Color(); // 创建局部颜色对象，避免线程冲突

        for (int y = 0; y < pixmap.height; y++) {
            for (int x = 0; x < pixmap.width; x++) {
                tempColor.rgba8888(pixmap.get(x, y));
                totalR += tempColor.r;
                totalG += tempColor.g;
                totalB += tempColor.b;
                count++;
            }
        }

        if (count == 0) return Color.white;

        return new Color(totalR / count , totalG / count, totalB / count, 1.0f);
    }

    /**
     * 矩形光栅化
     */
    public static Seq<Scanline> rasterizeRectangle(int x1, int y1, int x2, int y2, 
                                                   int xBound, int yBound) {
        Seq<Scanline> lines = new Seq<>();
        
        int yMin = Math.max(0, Math.min(y1, y2));
        int yMax = Math.min(yBound - 1, Math.max(y1, y2));
        int xMin = Math.max(0, Math.min(x1, x2));
        int xMax = Math.min(xBound - 1, Math.max(x1, x2));

        if (yMin == yMax) {
            lines.add(new Scanline(yMin, xMin, xMax));
        } else {
            for (int y = yMin; y <= yMax; y++) {
                lines.add(new Scanline(y, xMin, xMax));
            }
        }

        return lines;
    }

    /**
     * 圆形光栅化
     */
    public static Seq<Scanline> rasterizeCircle(int cx, int cy, int radius, 
                                                int xBound, int yBound) {
        Seq<Scanline> lines = new Seq<>();

        for (int dy = -radius; dy <= radius; dy++) {
            int y = cy + dy;
            if (y < 0 || y >= yBound) continue;

            int dx = (int)Math.sqrt(radius * radius - dy * dy);
            int xMin = Math.max(0, cx - dx);
            int xMax = Math.min(xBound - 1, cx + dx);

            if (xMin <= xMax) {
                lines.add(new Scanline(y, xMin, xMax));
            }
        }

        return lines;
    }

    /**
     * 三角形光栅化（扫描线算法）
     */
    public static Seq<Scanline> rasterizeTriangle(int x1, int y1, int x2, int y2, 
                                                  int x3, int y3, int xBound, int yBound) {
        Seq<Point2> points = new Seq<>();
        points.add(new Point2(x1, y1));
        points.add(new Point2(x2, y2));
        points.add(new Point2(x3, y3));
        return scanlinesForPolygon(points, xBound, yBound);
    }

    /**
     * 多边形扫描线转换
     */
    private static Seq<Scanline> scanlinesForPolygon(Seq<Point2> points, 
                                                    int xBound, int yBound) {
        Seq<Scanline> lines = new Seq<>();

        if (points.size < 3) return lines;

        // 计算 Y 范围
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Point2 p : points) {
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        minY = Math.max(0, minY);
        maxY = Math.min(yBound - 1, maxY);

        // 对每条扫描线计算交点
        for (int y = minY; y <= maxY; y++) {
            Seq<Integer> intersections = new Seq<>();

            for (int i = 0; i < points.size; i++) {
                Point2 p1 = points.get(i);
                Point2 p2 = points.get((i + 1) % points.size);

                if ((p1.y <= y && p2.y > y) || (p2.y <= y && p1.y > y)) {
                    double x = p1.x + (double)(y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y);
                    intersections.add((int)Math.round(x));
                }
            }

            intersections.sort(Integer::compare);

            // 成对连接
            for (int i = 0; i < intersections.size - 1; i += 2) {
                int x1 = Math.max(0, intersections.get(i));
                int x2 = Math.min(xBound - 1, intersections.get(i + 1));
                if (x1 <= x2) {
                    lines.add(new Scanline(y, x1, x2));
                }
            }
        }

        return lines;
    }
}
