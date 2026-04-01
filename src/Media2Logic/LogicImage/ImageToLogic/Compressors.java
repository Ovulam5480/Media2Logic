package Media2Logic.LogicImage.ImageToLogic;

import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorBackgroundOnce;
import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorBlock;
import Media2Logic.LogicImage.ImageToLogic.ColorBlock.ColorShape;
import Media2Logic.LogicImage.ImageToLogic.Geometrize.*;
import Media2Logic.LogicImage.ImageToLogic.LowPoly.LowPoly;
import Media2Logic.LogicImage.LogicImageDialog;
import arc.func.*;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.*;
import mindustry.Vars;
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
    private static final Seq<ColorShape> sharps = new Seq<>();
    private static final Color tmp = new Color(), tmp2 = new Color();
    public static float tolerance;
    private static Pixmap pixmap = new Pixmap(0, 0);
    private static Pixmap flip = new Pixmap(0, 0);

    private static final Schematic tmpSche = new Schematic(new Seq<>(), null, 0, 0);
    
    private static final int veryBig = 99999;
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
    private static final String[] text = new String[]{
            "正在图像转为逻辑代码...", "正在编译逻辑代码...", "正在生成蓝图..."};
    
    public static void compressorWithProgress(LogicDisplay display, LogicBlock logic, int widthAmount, int heightAmount, 
                                              LogicImageDialog.Compressor compressor, String name, float wait, int logicGap,
                                              Cons2<String, Float> progressCallback, Boolp cancelled){
        int minSideAmount = Math.min(widthAmount, heightAmount);
        int maxSideAmount = Math.max(widthAmount, heightAmount);
        boolean isLandscape = widthAmount >= heightAmount;
        int displayShorter = minSideAmount * display.size;

        int logicSize = logic.size + logicGap;

        Pixmap[][] pixmapGrid = splitPixmap(pixmap, display.displaySize, widthAmount, heightAmount);
        GridMap<Seq<String>> codesGrid = new GridMap<>();

        float lastProgress = 0;
        for (int i = 0; i < widthAmount; i++) {
            for (int j = 0; j < heightAmount; j++) {
                if(cancelled != null && cancelled.get()){
                    return;
                }
                
                LogicImageDialog.compressPixmap(pixmapGrid[i][j], compressor);
                codesGrid.put(isLandscape ? i : heightAmount - 1 - j, isLandscape ? j : i, sharpsToCodes(wait));
                
                // 更新进度
                if(progressCallback != null){
                    float currentProgress = (float) (i * heightAmount + j + 1) / (widthAmount * heightAmount);

                    if(currentProgress - lastProgress > 0.01f) {
                        progressCallback.get(text[0], currentProgress);
                        lastProgress = currentProgress;
                    }
                }
            }
        }

        GridMap<DisplayConfig> codeGrid = new GridMap<>();

        int logicWidthAmount = Mathf.ceil((float) maxSideAmount * display.size / logicSize);
        int logicHeightAmount = logic.range > veryBig ? veryBig : Mathf.ceil(logic.range / 8 / logicSize - 1 / 2f);

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
                        if(cancelled != null && cancelled.get()){
                            return;
                        }

                        if (!validLink(logic, display, displayGridX, displayGridY, logicGridX, logicGridY, displayShorter, logicGap)) {
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

        lastProgress = 0;

        int[] maxDeep = {0, 0};
        boolean[] booleans = {true, false};
        GridMap<byte[]> grid = new GridMap<>();

        if (logic.range > veryBig) {
            for (boolean isPositive : booleans) {
                for (int x = 0; x < logicWidthAmount; x++) {
                    int nonNull = isPositive ? 0 : -1;
                    for (int j = 0; j < logicHeightAmount; j++) {
                        if(cancelled != null && cancelled.get()){
                            return;
                        }

                        DisplayConfig displayConfig = codeGrid.get(x, isPositive ? j : -(j + 1));
                        if (displayConfig != null) {
                            int y = isPositive ? nonNull++ : nonNull--;
                            byte[] config = logicConfig(logic, display,
                                    displayConfig.displayX, (minSideAmount - 1 - displayConfig.displayY), x, -(y + 1),
                                    displayShorter, displayConfig.code, logicGap);
                            grid.put(x, -(y + 1), config);
                        }

                        if (progressCallback != null) {
                            float currentProgress = ((float) (x + j * logicWidthAmount) / (logicWidthAmount * logicHeightAmount * 2)) + (isPositive ? 0 : 0.5f);
                            if (currentProgress - lastProgress > 0.01f) {
                                progressCallback.get(text[1], currentProgress);
                                lastProgress = currentProgress;
                            }
                        }
                    }
                    if (isPositive) {
                        maxDeep[1] = Math.max(maxDeep[1], nonNull);
                    } else {
                        maxDeep[0] = Math.max(maxDeep[0], -nonNull-1);
                    }
                }
            }
        }else {
            for (boolean b : booleans) {
                Seq<Integer> betterPos = new Seq<>();
                for (int j = 0; j < logicHeightAmount; j++) {
                    int y = b ? j : -(j + 1);

                    for (int i = 0; i < logicWidthAmount; i++) {
                        if(cancelled != null && cancelled.get()){
                            return;
                        }

                        int x = i % 2 == 0 ? (i / 2) : (logicWidthAmount - 1 - (i / 2));
                        if (codeGrid.containsKey(x, y)) {
                            int px = x;
                            int py = y;

                            DisplayConfig displayConfig = codeGrid.get(x, y);

                            if (!betterPos.isEmpty()) {
                                Integer pos2 = betterPos.find(pos -> {
                                    Point2 p1 = Point2.unpack(pos);

                                    return Math.abs(p1.y) < Math.abs(y) && validLink(logic, display,
                                            displayConfig.displayX, minSideAmount - 1 - displayConfig.displayY, p1.x, -p1.y - 1, displayShorter, logicGap);
                                });

                                if (pos2 != null) {
                                    Point2 p2 = Point2.unpack(pos2);
                                    px = p2.x;
                                    py = p2.y;

                                    betterPos.remove(pos2);
                                    betterPos.add(Point2.pack(x, y));
                                }
                            }

                            byte[] bytes = logicConfig(logic, display,
                                    displayConfig.displayX, minSideAmount - 1 - displayConfig.displayY, px, -py - 1,
                                    displayShorter, displayConfig.code, logicGap);

                            grid.put(px, -py - 1, bytes);
                        } else {
                            betterPos.add(Point2.pack(x, y));
                        }

                        if (progressCallback != null) {
                            float currentProgress = ((float) (i + j * logicWidthAmount) / (logicWidthAmount * logicHeightAmount * 2)) + (b ? 0 : 0.5f);
                            if (currentProgress - lastProgress > 0.01f) {
                                progressCallback.get(text[1], currentProgress);
                                lastProgress = currentProgress;
                            }
                        }
                    }
                }
                int deep = Point2.unpack(betterPos.first()).y;
                maxDeep[b ? 1 : 0] = deep >= 0 ? deep + 1 : -deep;
            }
        }

        if(progressCallback != null){
            progressCallback.get(text[2], 1f);
        }

        Seq<Schematic.Stile> stiles = new Seq<>();

        for (int i = 0; i < maxSideAmount; i++) {
            for (int j = 0; j < minSideAmount; j++) {
                Vec2 displayTile = getDisplayTile(display.size, i, j, new Vec2());
                int TileX = (int) displayTile.x;
                int TileY = (int) displayTile.y + maxDeep[0] * logicSize;

                stiles.add(new Schematic.Stile(display, TileX, TileY, null, (byte) 0));
            }
        }

        for (int i = -logicWidthAmount; i < logicWidthAmount; i++) {
            for (int j = -logicHeightAmount; j < logicHeightAmount; j++) {
                byte[] bytes = grid.get(i, j);
                if (bytes == null) continue;

                Vec2 logicTile = getLogicTile(logicSize, i, j, new Vec2(), displayShorter);
                int TileX = Mathf.floor(logicTile.x);
                int TileY = Mathf.floor(logicTile.y + maxDeep[0] * logicSize);

                stiles.add(new Schematic.Stile(logic, TileX, TileY, bytes, (byte) 0));
            }
        }

        StringMap stringMap = new StringMap();
        stringMap.put("name", name != null ? name : sdf.format(new Date()));

        tmpSche.tiles.clear();
        tmpSche.tiles.add(stiles);
        tmpSche.width = maxSideAmount * display.size;
        tmpSche.height = minSideAmount * display.size + (maxDeep[0] + maxDeep[1]) * logicSize;

        Schematic tmpSche2;

        if(!isLandscape)tmpSche2 = Schematics.rotate(tmpSche, 1);
        else tmpSche2 = tmpSche;

        align(tmpSche2);

        Vars.schematics.add(new Schematic(Seq.with(tmpSche2.tiles), stringMap, tmpSche2.width, tmpSche2.height));
    }

    public static byte[] logicConfig(LogicBlock logic, Block display,
                                     int displayGridX, int displayGridY, int logicGridX, int logicGridY,
                                     int displayShorter, String code, int logicGap) {
        Vec2 displayTile = getDisplayTile(display.size, displayGridX, displayGridY, new Vec2());
        Vec2 logicTile = getLogicTile(logic.size + logicGap, logicGridX, logicGridY, new Vec2(), displayShorter);

        Vec2 diff = new Vec2(displayTile).sub(logicTile);

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
                                    int displayShorter, int logicGap) {
        if(logic.range > veryBig)return true;
        Vec2 displayTile = getDisplayTile(display.size, displayGridX, displayGridY, new Vec2());
        Vec2 logicTile = getLogicTile(logic.size + logicGap, logicGridX, logicGridY, new Vec2(), displayShorter);

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

    public static Seq<String> sharpsToCodes(float wait) {
        StringBuilder code = new StringBuilder();
        Seq<String> codes = new Seq<>();
        boolean hasWait = wait > 0;
        int maxCount = hasWait ? 997 : 998;

        int drawCount = 0;
        int totalCount = 0;

        for (ColorShape sharp : sharps) {
            boolean isFill = totalCount >= maxCount;
            if (drawCount >= 250 || isFill) {

                code.append("drawflush display1\n");
                drawCount = 0;
                totalCount++;

                if (isFill) {
                    code.append("wait ").append(wait).append("\n");
                    codes.add(code.toString());
                    code.setLength(0);

                    totalCount = 0;
                }
            }

            sharp.toInstruction(code);

            drawCount += sharp.getLogicLines();
            totalCount += sharp.getLogicLines();
        }

        if (totalCount > 0) {
            code.append("drawflush display1\n");
            code.append("wait ").append(wait).append("\n");
            codes.add(code.toString());
        }

        return codes;
    }

    public static Pixmap decompressor(boolean blend) {
        Pixmap review = new Pixmap(pixmap.width, pixmap.height);
        for (ColorShape sharp : sharps) {
            sharp.decompressor(review, blend);
        }
        return flip(review);
    }

    public static void setPixmap(Pixmap pixmap) {
        sharps.clear();

        Compressors.pixmap = pixmap;
        Compressors.flip = flip(pixmap);
    }

    public static Pixmap flip(Pixmap source) {
        Pixmap flip = new Pixmap(source.width, source.height);
        source.each((x, y) -> flip.set(x, y, source.get(x, source.height - 1 - y)));
        return flip;
    }

    public static void bestHillClimb(){
        Model model = new Model(pixmap);

        sharps.add(new ColorBackgroundOnce(model.avgColor));

        for (ShapeResult result : model.step(new ImageRunnerOptions(), (int)(tolerance * 100))) {
            sharps.add(result.shape.toColorShape(result.color));
        }
    }

    public static void triSplitCompress(){
        LowPoly.generate(flip, sharps, tolerance);
    }

    public static void growCompress() {
        GridBits covered = new GridBits(pixmap.getWidth(), pixmap.getHeight());
        Color avgColor = new Color(); // 创建局部颜色对象
        Color tmpColor = new Color(); // 创建局部颜色对象

        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                if (covered.get(x, y)) continue;

                int width = 1, height = 1;
                setColor(x, y, tmpColor);
                avgColor.set(tmpColor);

                while (x + width < pixmap.getWidth() && !covered.get(x + width, y)) {
                    setColor(x + width, y, tmpColor);
                    if (tmpColor.diff(avgColor) > tolerance * 1.8f) break;
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
                        if (tmpColor.diff(avgColor) > tolerance * 1.8f) {
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

                ColorBlock block = new ColorBlock();
                block.set(avgColor, x, y, width, height);
                sharps.add(block);
            }
        }
    }

    public static void eerieCompress() {
        GridBits covered = new GridBits(pixmap.getWidth(), pixmap.getHeight());
        Color firstColor = new Color(); // 创建局部颜色对象
        Color currColor = new Color(); // 创建局部颜色对象

        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                if (covered.get(x, y)) continue;

                int width = 1;
                setColor(x, y, firstColor);

                while (x + width < pixmap.getWidth() && !covered.get(x + width, y)) {
                    setColor(x + width, y, currColor);
                    if (covered.get(x + width, y) || firstColor.diff(currColor) > tolerance * 1.8f) break;
                    width++;
                }

                int[] maxDepth = new int[width];
                int minDepth = pixmap.getHeight();

                for (int dx = 0; dx < width; dx++) {
                    int depth;
                    for (depth = 1; y + depth < minDepth; depth++){
                        if (firstColor.diff(setColor(x + dx, y + depth, currColor)) > tolerance * 1.8f) {
                            break;
                        }
                    }

                    minDepth = Math.min(minDepth, depth + y);
                    maxDepth[dx] = depth;
                }

                int height = 0;
                int maxArea = 0;
                for (int i = 0; i < maxDepth.length; i++) {
                    int area = maxDepth[i] * (i + 1);
                    if(area > maxArea){
                        height = i;
                        maxArea = area;
                    }
                }

                for (int dy = 0; dy < height; dy++) {
                    for (int dx = 0; dx < width; dx++) {
                        covered.set(x + dx, y + dy, true);
                    }
                }

                ColorBlock block = new ColorBlock();
                block.set(getAverageColor(x, y, width, height, currColor), x, y, width, height);
                sharps.add(block);
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

                loop : for (size = 0; size < Math.max(pixmap.width, pixmap.height); size++) {
                    for (int j = 0; j <= 2 * size; j++) {
                        int px, py;
                        if (j <= size) {
                            px = x + j;
                            py = y + size;
                        } else {
                            px = x + size;
                            py = y + 2 * size - j;
                        }

                        if (averageColor.diff(setColor(px, py, tmp)) > tolerance * 1.8f || covered.get(px, py)) {
                            break loop;
                        } else {
                            r += tmp.r;
                            g += tmp.g;
                            b += tmp.b;
                            area++;
                        }
                    }

                    averageColor.set(r / area, g / area, b / area, 1);
                }

                for (int dy = 0; dy < size; dy++) {
                    for (int dx = 0; dx < size; dx++) {
                        covered.set(x + dx, y + dy);
                    }
                }

                ColorBlock block = new ColorBlock();
                block.set(averageColor, x, y, size, size);
                sharps.add(block);
            }
        }
    }

    //四叉树递归分割
    public static void quadtreeCompressor() {
        quadtreeCompressor(0, 0, pixmap.width, pixmap.height);
    }

    private static void quadtreeCompressor(int x, int y, int w, int h) {
        Color tempColor = new Color(); // 创建局部颜色对象
        if (isUniformColor(x, y, w, h, tempColor)) {
            ColorBlock block = new ColorBlock();
            block.set(tempColor, x, y, w, h);
            sharps.add(block);
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
                        } else {
                            tile.set(x, y, Color.clear);
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
        Color tempColor = new Color(); // 创建局部颜色对象，避免线程冲突

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                setColor(i + x, j + y, tempColor);

                if (averageColor.diff(tempColor) > tolerance * 1.8f) {
                    return false;
                } else {
                    r += tempColor.r;
                    g += tempColor.g;
                    b += tempColor.b;

                    int area = (i + 1) * (j + 1);

                    averageColor.set(r / area, g / area, b / area, 1);
                }
            }
        }

        return true;
    }

    public static Color getAverageColor(int x, int y, int w, int h, Color averageColor){
        float r = 0, g = 0, b = 0;
        Color tempColor = new Color(); // 创建局部颜色对象，避免线程冲突

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                setColor(i + x, j + y, tempColor);

                r += tempColor.r;
                g += tempColor.g;
                b += tempColor.b;
            }
        }
        int area = (w + 1) * (h + 1);

        return averageColor.set(r / area, g / area, b / area, 1);
    }

    public static Color setColor(int x, int y, Color color) {
        return color.set(flip.get(x, y));
    }

    public static int rectSize() {
        return sharps.size;
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
