package Media2Logic.LogicImage;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.*;
import arc.util.Log;
import arc.util.Tmp;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.world.Block;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//todo 透明像素的处理
public class Compressors {
    private static final Seq<ColorBlock> rects = new Seq<>();
    private static final Color tmp = new Color();
    private static final Color tmp2 = new Color();
    public static float tolerance;
    private static Pixmap pixmap = new Pixmap(0, 0);
    private static Pixmap flip = new Pixmap(0, 0);
    
    private static final int veryBig = 99999;
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());

    public static void compressor(LogicDisplay display, LogicBlock logic, int widthAmount, int heightAmount, LogicImageDialog.Compressor compressor, String name) {
        int minSideAmount = Math.min(widthAmount, heightAmount);
        int maxSideAmount = Math.max(widthAmount, heightAmount);
        boolean isLandscape = widthAmount >= heightAmount;
        int displayShorter = minSideAmount * display.size;

        Pixmap[][] pixmapGrid = splitPixmap(pixmap, display.displaySize, widthAmount, heightAmount);
        GridMap<Seq<String>> codesGrid = new GridMap<>();

        for (int i = 0; i < widthAmount; i++) {
            for (int j = 0; j < heightAmount; j++) {
                LogicImageDialog.compressPixmap(pixmapGrid[i][j], compressor);
                codesGrid.put(isLandscape ? i : heightAmount - 1 - j, isLandscape ? j : i, rectToCodes());
            }
        }

        GridMap<DisplayConfig> codeGrid = new GridMap<>();

        int logicWidthAmount = Mathf.ceil((float) maxSideAmount * display.size / logic.size);
        int logicHeightAmount = logic.range > veryBig ? veryBig : Mathf.ceil(logic.range / 8 / logic.size - 1 / 2f);

        IntSeq lxseq = new IntSeq();
        for (int i = 0; i < logicWidthAmount; i++) {
            lxseq.add(i);
        }

        IntSeq dxseq = new IntSeq();
        for (int i = 0; i < maxSideAmount; i++) {
            dxseq.add(i);
        }

        for (int i = 0; i < minSideAmount; i++) {
            int midden = (minSideAmount - 1) / 2;
            boolean rightTop = i > midden;
            int displayGridY = rightTop ? midden - i + minSideAmount : i;

            for (int displayGridX : dxseq.toArray()) {
                Seq<String> codes = codesGrid.get(displayGridX, displayGridY);

                loop:
                for (int j = 0; j < logicHeightAmount; j++) {
                    int logicGridY = rightTop ? logicHeightAmount - 1 - j : j - logicHeightAmount;
                    for (int logicGridX : lxseq.toArray()) {
                        if (!validLink(logic, display, displayGridX, displayGridY, logicGridX, logicGridY, displayShorter)) {
                            continue;
                        }

                        if (codeGrid.get(logicGridX, logicGridY) == null) {
                            codeGrid.put(logicGridX, logicGridY, new DisplayConfig(displayGridX, displayGridY, codes.pop()));
                        }

                        if (codes.isEmpty()) {
                            break loop;
                        }
                    }
                    lxseq.reverse();
                }

                dxseq.reverse();

                if (!codes.isEmpty()) {
                    Vars.ui.showErrorMessage("无法放置足够的逻辑, 请适量增加容差");
                    return;
                }
            }
        }

        int[] maxDeep = {0, 0};
        boolean[] booleans = {true, false};

        GridMap<byte[]> configGrid = new GridMap<>();
        for (boolean isPositive : booleans) {
            for (int x = 0; x < logicWidthAmount; x++) {
                int nonNull = isPositive ? 0 : -1;
                for (int j = 0; j < logicHeightAmount; j++) {
                    DisplayConfig displayConfig = codeGrid.get(x, isPositive ? j : -(j + 1));
                    if (displayConfig != null) {
                        int y = isPositive ? nonNull++ : nonNull--;
                        byte[] config = logicConfig(logic, display,
                                displayConfig.displayX, (minSideAmount - 1 - displayConfig.displayY), x, -(y + 1),
                                displayShorter, displayConfig.code);
                        configGrid.put(x, -(y + 1), config);
                    }
                }
                if (isPositive) {
                    maxDeep[1] = Math.max(maxDeep[1], nonNull);
                } else {
                    maxDeep[0] = Math.max(maxDeep[0], -nonNull-1);
                }
            }
        }

        Seq<Schematic.Stile> stiles = new Seq<>();

        for (int i = 0; i < maxSideAmount; i++) {
            for (int j = 0; j < minSideAmount; j++) {
                Vec2 displayTile = getDisplayTile(display.size, i, j, Tmp.v1);
                int TileX = (int) displayTile.x;
                int TileY = (int) displayTile.y + maxDeep[0] * logic.size;

                stiles.add(new Schematic.Stile(display, TileX, TileY, null, (byte) 0));
            }
        }

        for (int i = -logicWidthAmount; i < logicWidthAmount; i++) {
            for (int j = -logicHeightAmount; j < logicHeightAmount; j++) {
                byte[] bytes = configGrid.get(i, j);
                if (bytes == null) continue;

                Vec2 logicTile = getLogicTile(logic.size, i, j, Tmp.v1, displayShorter);
                int TileX = Mathf.floor(logicTile.x);
                int TileY = Mathf.floor(logicTile.y + maxDeep[0] * logic.size);

                stiles.add(new Schematic.Stile(logic, TileX, TileY, bytes, (byte) 0));
            }
        }

        StringMap stringMap = new StringMap();
        stringMap.put("name", name != null ? name : sdf.format(new Date()));

        Schematic schematic = new Schematic(stiles,
                null,
                maxSideAmount * display.size,
                minSideAmount * display.size + (maxDeep[0] + maxDeep[1]) * logic.size);

        if(!isLandscape){
            schematic = Schematics.rotate(schematic, 1);
        }

        align(schematic);
        schematic.tags = stringMap;

        Vars.schematics.add(schematic);
    }

    public static byte[] logicConfig(LogicBlock logic, Block display,
                                     int displayGridX, int displayGridY, int logicGridX, int logicGridY,
                                     int displayShorter, String code) {
        Vec2 displayTile = getDisplayTile(display.size, displayGridX, displayGridY, Tmp.v1);
        Vec2 logicTile = getLogicTile(logic.size, logicGridX, logicGridY, Tmp.v2, displayShorter);

        Vec2 diff = Tmp.v3.set(displayTile).sub(logicTile);

        LogicBlock.LogicLink link = new LogicBlock.LogicLink((int) diff.x, (int) diff.y, "display1", true);
        return LogicBlock.compress(code, Seq.with(link));
    }

    public static void align(Schematic schematic){
        int[] deep = {veryBig,veryBig};
        Seq<Schematic.Stile> tiles = schematic.tiles;
        tiles.each(t -> {
            int radius = (t.block.size - 1) / 2;
            deep[0] = Math.min(deep[0], t.x - radius);
            deep[1] = Math.min(deep[1], t.y - radius);
        });

        tiles.each(t -> {
            t.x -= (short) deep[0];
            t.y -= (short) deep[1];
        });
    }

    public static boolean validLink(LogicBlock logic, Block display,
                                    int displayGridX, int displayGridY, int logicGridX, int logicGridY,
                                    int displayShorter) {
        if(logic.range > veryBig)return true;
        Vec2 displayTile = getDisplayTile(display.size, displayGridX, displayGridY, Tmp.v1);
        Vec2 logicTile = getLogicTile(logic.size, logicGridX, logicGridY, Tmp.v2, displayShorter);

        return displayTile.within(logicTile, logic.range / 8 + display.size / 2f);
    }

    public static Vec2 getDisplayTile(int size, int displayGridX, int displayGridY, Vec2 input) {
        int offset = (size + 1) % 2;
        return input.set(displayGridX + 0.5f, displayGridY + 0.5f).scl(size).sub(offset, offset);
    }

    public static Vec2 getLogicTile(int size, int logicGridX, int logicGridY, Vec2 input, int displayShorter) {
        int offset = (size + 1) % 2;
        input.set(logicGridX + 0.5f, logicGridY + 0.5f).scl(size).sub(offset, offset);
        if (logicGridY >= 0) {
            input.y += displayShorter;
        }
        return input;
    }

    public static Seq<String> rectToCodes() {
        StringBuilder code = new StringBuilder();
        Seq<String> codes = new Seq<>();

        int drawCount = 0;
        int totalCount = 0;

        for (ColorBlock colorBlock : rects) {
            if (drawCount >= 252 || totalCount >= 998) {

                code.append("drawflush display1\n");
                drawCount = 0;
                totalCount++;

                if (totalCount >= 998) {
                    codes.add(code.toString());
                    code.setLength(0);

                    totalCount = 0;
                }
            }

            code.append("draw color ")
                    .append(colorBlock.color.r * 255).append(" ")
                    .append(colorBlock.color.g * 255).append(" ")
                    .append(colorBlock.color.b * 255).append(" ")
                    .append("255 0 0\n");

            code.append("draw rect ")
                    .append(colorBlock.x).append(" ")
                    .append(colorBlock.y).append(" ")
                    .append(colorBlock.width).append(" ")
                    .append(colorBlock.height).append(" ")
                    .append("0 0\n");

            drawCount += 2;
            totalCount += 2;
        }

        if (totalCount > 0) {
            code.append("drawflush display1\n");
            codes.add(code.toString());
        }

        return codes;
    }

    public static void addStile(String code, Seq<Schematic.Stile> stiles, int x, int y) {
        stiles.add(new Schematic.Stile(
                Blocks.microProcessor,
                x, y,
                LogicBlock.compress(code, Seq.with(new LogicBlock.LogicLink(7 - x, -1 - y, "display1", true))),
                (byte) 0));
    }

    public static Pixmap decompressor() {
        Pixmap review = new Pixmap(pixmap.width, pixmap.height);
        for (ColorBlock rect : rects) {
            for (int i = 0; i < rect.width; i++) {
                for (int j = 0; j < rect.height; j++) {
                    review.set(rect.x + i, rect.y + j, rect.color);
                }
            }
        }
        return flip(review);
    }

    public static void setPixmap(Pixmap pixmap) {
        Pools.freeAll(rects);
        rects.clear();

        Compressors.pixmap = pixmap;
        Compressors.flip = flip(pixmap);
    }

    public static Pixmap flip(Pixmap source) {
        Pixmap flip = new Pixmap(source.width, source.height);
        source.each((x, y) -> flip.set(x, y, source.get(x, source.height - 1 - y)));
        return flip;
    }

    public static void growCompress() {
        GridBits covered = new GridBits(pixmap.getWidth(), pixmap.getHeight());
        Color avgColor = Tmp.c1;
        Color tmpColor = Tmp.c2;

        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                if (covered.get(x, y)) continue;

                int width = 1, height = 1;
                setColor(x, y, tmpColor);
                avgColor.set(tmpColor);

                while (x + width < pixmap.getWidth() && !covered.get(x + width, y)) {
                    setColor(x + width, y, tmpColor);
                    if (tmpColor.diff(avgColor) > tolerance) break;
                    width++;
                }

                while (y + height < pixmap.getHeight()) {
                    boolean rowUniform = true;
                    for (int dx = 0; dx < width; dx++) {
                        if (covered.get(x + dx, y + height)) {
                            rowUniform = false;
                            break;
                        }
                        setColor(x + dx, y + height, tmpColor);
                        if (tmpColor.diff(avgColor) > tolerance) {
                            rowUniform = false;
                            break;
                        }
                    }
                    if (!rowUniform) break;
                    height++;
                }

                for (int dy = 0; dy < height; dy++) {
                    for (int dx = 0; dx < width; dx++) {
                        covered.set(x + dx, y + dy, true);
                    }
                }

                ColorBlock block = obtain();
                block.set(avgColor, x, y, width, height);
                rects.add(block);
            }
        }
    }

    //最大矩形分割
    public static void maxRectCompress() {
        GridBits covered = new GridBits(pixmap.width, pixmap.height);

        for (int x = 0; x < pixmap.width; x++) {
            for (int y = 0; y < pixmap.height; y++) {
                if (covered.get(x, y)) continue;

                Color averageColor = setColor(x, y, tmp2);
                float r = averageColor.r, g = averageColor.g, b = averageColor.b;
                int size;
                int area = 1;

                for (size = 0; size < Math.max(pixmap.width, pixmap.height); size++) {
                    boolean shouldBreak = false;

                    for (int i = 0; i < size; i++) {
                        if (averageColor.diff(setColor(x + size, y + i, tmp)) > tolerance
                                || covered.get(x + size, y + i)) {
                            shouldBreak = true;
                            break;
                        } else {
                            r += tmp.r;
                            g += tmp.g;
                            b += tmp.b;
                            area++;
                        }
                        if (averageColor.diff(setColor(x + i, y + size, tmp)) > tolerance
                                || covered.get(x + i, y + size)) {
                            shouldBreak = true;
                            break;
                        } else {
                            r += tmp.r;
                            g += tmp.g;
                            b += tmp.b;
                            area++;
                        }
                    }
                    if (shouldBreak) break;

                    averageColor.set(r / area, g / area, b / area, 1);
                }

                for (int dy = 0; dy < size; dy++) {
                    for (int dx = 0; dx < size; dx++) {
                        covered.set(x + dx, y + dy);
                    }
                }

                ColorBlock block = obtain();
                block.set(averageColor, x, y, size, size);
                rects.add(block);
            }
        }
    }

    //四叉树分割
    public static void quadtreeCompressor() {
        quadtreeCompressor(0, 0, pixmap.width, pixmap.height);
    }

    private static void quadtreeCompressor(int x, int y, int w, int h) {
        if (isUniformColor(x, y, w, h, Tmp.c3)) {
            rects.add(obtain().set(Tmp.c3, x, y, w, h));
            return;
        }

        int halfW = w / 2, halfH = h / 2;

        if (halfW > 0 && halfH > 0) quadtreeCompressor(x, y, halfW, halfH);
        if (halfH > 0) quadtreeCompressor(x + halfW, y, w - halfW, halfH);
        if (halfW > 0) quadtreeCompressor(x, y + halfH, halfW, h - halfH);
        quadtreeCompressor(x + halfW, y + halfH, w - halfW, h - halfH);
    }

    public static Pixmap[][] splitPixmap(Pixmap pixmap, int displaySize, int widthAmount, int heightAmount) {
        Pixmap[][] result = new Pixmap[widthAmount][heightAmount];

        for (int gridX = 0; gridX < widthAmount; gridX++) {
            for (int gridY = 0; gridY < heightAmount; gridY++) {
                Pixmap tile = new Pixmap(displaySize, displaySize);

                int startX = gridX * displaySize;
                int startY = gridY * displaySize;

                for (int y = 0; y < displaySize; y++) {
                    for (int x = 0; x < displaySize; x++) {
                        int srcX = startX + x;
                        int srcY = startY + y;

                        if (srcX < pixmap.width && srcY < pixmap.height) {
                            tile.set(x, y, pixmap.get(srcX, srcY));
                        }
                        else {
                            Log.info("x " + srcX + " y " + srcY);
                            tile.set(x, y, Color.red);
                        }
                    }
                }

                result[gridX][gridY] = tile;
            }
        }

        return result;
    }

    //色块是否单一颜色
    public static boolean isUniformColor(int x, int y, int w, int h, Color averageColor) {
        setColor(x, y, averageColor);
        float r = 0, g = 0, b = 0;

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                setColor(i + x, j + y, tmp);

                if (averageColor.diff(tmp) > tolerance) {
                    return false;
                } else {
                    r += tmp.r;
                    g += tmp.g;
                    b += tmp.b;

                    int area = (i + 1) * (j + 1);

                    averageColor.set(r / area, g / area, b / area, 1);
                }
            }
        }

        return true;
    }

    public static Color setColor(int x, int y, Color color) {
        return color.set(flip.get(x, y));
    }

    public static ColorBlock obtain() {
        return Pools.obtain(ColorBlock.class, ColorBlock::new);
    }

    public static int rectSize() {
        return rects.size;
    }

    public static class DisplayConfig {
        int displayX, displayY;
        String code;

        public DisplayConfig(int displayX, int displayY, String code) {
            this.displayX = displayX;
            this.displayY = displayY;
            this.code = code;
        }
    }
}
