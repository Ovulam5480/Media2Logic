package Media2Logic.LogicImage.ImageToLogic.LowPoly;

import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorShape;
import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorTirEdge;
import Media2Logic.LogicImage.ImageToLogic.LowPoly.tinfour.common.SimpleTriangle;
import Media2Logic.LogicImage.ImageToLogic.LowPoly.tinfour.common.Vertex;
import Media2Logic.LogicImage.ImageToLogic.LowPoly.tinfour.standard.IncrementalTin;
import Media2Logic.LogicImage.ImageToLogic.LowPoly.tinfour.utils.HilbertSort;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nce3xin on 2017/10/23.
 */

public class LowPoly {
    public static void generate(Pixmap image, Seq<ColorShape> out, float tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();

        Seq<Point2> particles = new Seq<>();
        Seq<Point2> collectors = new Seq<>();

        Sobel.sobel(image, (magnitude, x, y) -> {
            if (magnitude > tolerance) {
                collectors.add(new Point2(x, y));
            }
        });

        int len = (int) (collectors.size * 0.5f);
        for (int i = 0; i < len; ++i) {
            if (collectors.isEmpty()) break;
            Point2 random = collectors.random();
            particles.add(random);
            collectors.remove(random);
        }

        int randParticles = (int)Mathf.sqrt(len);
        for (int i = 0; i < randParticles; i++) {
            particles.add(new Point2(Mathf.random(width), Mathf.random(height)));
        }

        int grid = Math.max(Math.min(width / 8, randParticles), 1);
        for (int i = 0; i <= grid; i++) {
            particles.add(new Point2(i * width / grid, height));
            particles.add(new Point2(i * width / grid, 0));
        }

        for (int i = 1; i < grid; i++) {
            particles.add(new Point2(width, i * height / grid));
            particles.add(new Point2(0, i * height / grid));
        }


        Seq<ColorTirEdge> tirs = triangulateWithIncrementalTin(particles, Math.sqrt(width * height / (double)particles.size));

        tirs.each(tir -> {
            float[] rgbarea = {0,0,0,0};
            Color tempColor = new Color(); // 创建局部颜色对象，避免线程冲突
            tir.each((x, y) -> {
                if(image.in(x, y)) {
                    int pixel = image.getRaw(x, y);
                    tempColor.rgba8888(pixel);
                    rgbarea[0] += tempColor.r;
                    rgbarea[1] += tempColor.g;
                    rgbarea[2] += tempColor.b;
                    rgbarea[3]++;
                }
            });

            tir.color.set(rgbarea [0] / rgbarea [3], rgbarea [1] / rgbarea [3], rgbarea [2] / rgbarea [3], 1);
            out.add(tir);
        });

        //particles.each(p -> out.add(new ColorPoint(p)));
    }

    private static Seq<ColorTirEdge> triangulateWithIncrementalTin(Seq<Point2> points, double estimatedSpacing) {
        Seq<ColorTirEdge> result = new Seq<>();

        List<Vertex> vertices = new ArrayList<>();
        for (Point2 p : points) {
            vertices.add(new Vertex(p.x, p.y, 0));
        }

        IncrementalTin tin = new IncrementalTin(estimatedSpacing);

        for (Vertex v : vertices) {
            tin.add(v);
        }

        HilbertSort hilbertSort = new HilbertSort();
        hilbertSort.sort(vertices);

        for (SimpleTriangle triangle : tin.triangles()) {
            Vertex v1 = triangle.getVertexA();
            Vertex v2 = triangle.getVertexB();
            Vertex v3 = triangle.getVertexC();

            if (v1 == null || v2 == null || v3 == null) {
                continue;
            }

            ColorTirEdge tir = new ColorTirEdge(
                    new Point2((int)v1.x, (int)v1.y),
                    new Point2((int)v2.x, (int)v2.y),
                    new Point2((int)v3.x, (int)v3.y)
            );

            result.add(tir);
        }

        return result;
    }
}
