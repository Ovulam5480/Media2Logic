package Media2Logic.LogicImage;

import arc.Core;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Nullable;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;
import mindustry.world.Block;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.Setting;

import static arc.Core.settings;
import static mindustry.Vars.ui;

public class LogicImageDialog extends BaseDialog {
    Table reviewTable = new Table();
    SettingsTable settingsTable = new SettingsTable();

    LogicBlock logicBlock = (LogicBlock) Blocks.microProcessor;
    LogicDisplay logicDisplay = (LogicDisplay) Blocks.logicDisplay;
    int widthAmount = 1, heightAmount = 1;
    boolean minScale = false;
    boolean showGrid = false;
    
    float imageSize = 300f;

    Pixmap image = new Pixmap(180,180);
    Pixmap afterScaling = new Pixmap(0,0);
    Pixmap afterCompressor = new Pixmap(0,0);
    Compressor compressor = Compressor.quadtree;
    ScalingAlgorithm algorithm = ScalingAlgorithm.nearest;
    String name = null;

    public LogicImageDialog() {
        super("逻辑图像");

        cont.add(reviewTable).row();

        ScrollPane pane = new ScrollPane(settingsTable);
        pane.setOverscroll(false, false);
        pane.setScrollingDisabled(true, false);
        cont.add(pane);

        Seq<Setting> list = settingsTable.getSettings();

        list.add(new NotSavedSetting("逻辑图像名称", "", () -> name,s -> name = s));

        list.add(new NotSavedCheckSetting("比例缩放", true, () -> minScale, b -> {
            minScale = b;

            scaling();
        }));

//        list.add(new NotSavedCheckSetting("显示网格", true, () -> showGrid, b -> {
//            showGrid = b;
//
//            scaling();
//        }));

        Seq<Selection<LogicBlock>> logicBlocks = new Seq<>();
        for (Block block : Vars.content.blocks().select(b -> b instanceof LogicBlock)) {
            logicBlocks.add(new Selection<>((LogicBlock) block, new TextureRegionDrawable(block.fullIcon), block.localizedName));
        }
        list.add(new SelectionSetting<>("逻辑处理器类型",
                logicBlocks,
                () -> logicBlock,
                lb -> logicBlock = lb
        ));

        Seq<Selection<LogicDisplay>> logicDisplays = new Seq<>();
        for (Block block : Vars.content.blocks().select(b -> b instanceof LogicDisplay)) {
            logicDisplays.add(new Selection<>((LogicDisplay) block, new TextureRegionDrawable(block.fullIcon), block.localizedName));
        }
        list.add(new SelectionSetting<>("逻辑画板类型",
                logicDisplays,
                () -> logicDisplay,
                lp -> {
                    logicDisplay = lp;
                    scaling();
                }
        ));

        list.add(new SelectionSetting<>("色块化算法",
                Seq.with(
                        new Selection<>(Compressor.quadtree, Icon.move, "四叉树分割, 该算法存在的意义是凑数"),
                        new Selection<>(Compressor.maxRect, Icon.distribution, "最大矩形分割"),
                        new Selection<>(Compressor.grow, Icon.logic, "贪婪生长")
                ),
                () -> compressor,
                c -> {
                    compressor = c;
                    scaling();
                }
        ));

        list.add(new DynamicLimitSliderSetting("色块容差", (int) Compressors.tolerance * 100,
                () -> 0,
                () -> 180,
                1,
                i -> {
                    Compressors.tolerance = i / 100f;
                    scaling();
                    return i + "%";
                }
        ));

        list.add(new SelectionSetting<>("缩放算法",
                Seq.with(
                        new Selection<>(ScalingAlgorithm.nearest, Icon.effect, "最近邻插值"),
                        new Selection<>(ScalingAlgorithm.bilinear, Icon.diagonal, "双线性插值")),
                () -> algorithm,
                c -> {
                    algorithm = c;
                    scaling();
                }
        ));

        Intp maxAmount = () -> Mathf.ceil(((int)logicBlock.range - logicBlock.size * 8 / 2f) / (logicDisplay.size * 8)) * 2;

        list.add(new DynamicLimitSliderSetting("逻辑画板宽数", 1,
                () -> 1,
                () -> heightAmount > maxAmount.get() ? Mathf.clamp(maxAmount.get(), 1, 32) : 32,
                1,
                i -> {
                    widthAmount = i;
                    scaling();
                    return "" + i;
        }));

        list.add(new DynamicLimitSliderSetting("逻辑画板高数", 1,
                () -> 1,
                () -> widthAmount > maxAmount.get() ? Mathf.clamp(maxAmount.get(), 1, 32) : 32,
                1,
                i -> {
                    heightAmount = i;
                    scaling();
                    return "" + i;
        }));

        BaseDialog schematics = Vars.ui.schematics;
        list.add(new ToggleCheckSetting("生成并保存", () -> {
            scaling();
            Compressors.setPixmap(afterScaling);
            Compressors.compressor(logicDisplay, logicBlock, widthAmount, heightAmount, compressor, name);
            schematics.hide();
            schematics.show();
        }));

        addCloseButton();
    }

    void scaling(){
        afterScaling.dispose();
        afterCompressor.dispose();

        int width, height;
        int displaySize = logicDisplay.displaySize;

        if(minScale){
            float scl = Math.min((float) displaySize * widthAmount / image.width, (float)displaySize * heightAmount / image.height);
            width = (int)(image.width * scl);
            height = (int)(image.height * scl);
        }else {
            width = displaySize * widthAmount;
            height = displaySize * heightAmount;
        }

        afterScaling = new Pixmap(width, height);

        if(algorithm == ScalingAlgorithm.nearest) Scaler.nearestNeighbor(image, afterScaling);
        else if(algorithm == ScalingAlgorithm.bilinear) Scaler.bilinearInterpolation(image, afterScaling);

        compressPixmap(afterScaling, compressor);

        afterCompressor = Compressors.decompressor();

//        if(showGrid) {
//            gridImage(afterScaling);
//            //gridImage(afterCompressor);
//        }

        refreshReviewImage();
    }

    public static void compressPixmap(Pixmap pixmap, Compressor compressor){
        Compressors.setPixmap(pixmap);

        if(compressor == Compressor.quadtree) Compressors.quadtreeCompressor();
        else if(compressor == Compressor.maxRect) Compressors.maxRectCompress();
        else if(compressor == Compressor.grow) Compressors.growCompress();
    }

    public void gridImage(Pixmap pixmap){
        for (int i = 0; i <= widthAmount; i++){
            for (int j = 0; j < height; j++){
                for (int k = -1; k <= 0; k++){
                    int x = i * logicDisplay.displaySize + k;
                    if (x >= width || x < 0)continue;
                    pixmap.set(x, j, Tmp.c1.set(afterScaling.get(x, j)).hue(180).value(1).saturation(1));
                }
            }
        }

        for (int i = 0; i <= heightAmount; i++){
            for (int j = 0; j < width; j++){
                for (int k = -1; k <= 0; k++){
                    int y = i * logicDisplay.displaySize + k;
                    if (y >= height || y < 0)continue;
                    pixmap.set(j, y, Tmp.c1.set(afterScaling.get(j, y)).hue(180).value(1).saturation(1));
                }
            }
        }
    }

    void refreshReviewImage(){
        reviewTable.clear();

        int before = afterScaling.width * afterScaling.height;
        int after = Compressors.rectSize();

        if(Vars.mobile){
            Table imageTable = new Table();
            ScrollPane pane = new ScrollPane(imageTable);
            pane.setOverscroll(false, false);
            pane.setScrollingDisabled(true, false);
            reviewTable.add(pane);

            addImage(image, imageTable);
            imageTable.row();

            addImage(afterScaling, imageTable);
            imageTable.add(before + "");
            imageTable.row();

            addImage(afterCompressor, imageTable);
            imageTable.add(after + "(" + String.format("%.2f", after * 100f / before) + "%)");
        }else {
            addImage(image, reviewTable);
            addImage(afterScaling, reviewTable);
            addImage(afterCompressor, reviewTable);
        }

        reviewTable.row();
        reviewTable.button("选择图片", () -> Vars.platform.showFileChooser(true, "png", f -> {
            image.dispose();
            try {
                image = PixmapIO.readPNG(f);
            } catch (Exception e) {
                ui.showErrorMessage("无法读取文件");
            }
            scaling();
        }));

        if(!Vars.mobile) {
            reviewTable.add(before + "");
            reviewTable.add(after + "(" + String.format("%.2f", after * 100f / before) + "%)");
        }
    }

    void addImage(Pixmap pixmap, Table table){
        TextureRegion region = new TextureRegion(new Texture(pixmap));
        float scale = imageSize / Math.max(region.width, region.height);
        float w = region.width * scale;
        float h = region.height * scale;
        Element element = table.image(region).size(w, h).pad(15f).get();
        ui.addDescTooltip(element, pixmap.width + "×" + pixmap.height);
    }

    public static class NotSavedSetting extends Setting{
        String def;
        Prov<String> holder;
        Cons<String> changed;

        public NotSavedSetting(String name, String def, Prov<String> holder, Cons<String> changed){
            super(name);
            this.def = def;
            this.holder = holder;
            this.changed = changed;
        }

        @Override
        public void add(SettingsTable table){
            TextField field = new TextField();

            field.update(() -> field.setText(holder.get()));

            field.changed(() -> {
                changed.get(field.getText());
            });

            Table prefTable = table.table().left().padTop(3f).get();
            prefTable.add(field);
            prefTable.label(() -> title);
            addDesc(prefTable);
            table.row();
        }
    }

    public static class NotSavedCheckSetting extends Setting{
        boolean def;
        Boolc changed;
        Boolp holder;

        public NotSavedCheckSetting(String name, boolean def, Boolp holder, Boolc changed){
            super(name);
            this.def = def;
            this.changed = changed;
            this.holder = holder;
        }

        @Override
        public void add(SettingsTable table){
            CheckBox box = new CheckBox(title);

            box.update(() -> box.setChecked(holder.get()));

            box.changed(() -> {
                settings.put(name, box.isChecked());
                if(changed != null){
                    changed.get(box.isChecked());
                }
            });

            box.left();
            addDesc(table.add(box).left().padTop(3f).get());
            table.row();
        }
    }

    static class ToggleCheckSetting extends Setting{
        Runnable toggle;

        public ToggleCheckSetting(String name, Runnable toggle) {
            super(name);
            this.toggle = toggle;
        }

        @Override
        public void add(SettingsTable table) {
            CheckBox box = new CheckBox(title);

            box.setChecked(true);
            box.changed(() -> {
                box.setChecked(true);
                toggle.run();
            });

            box.left();
            addDesc(table.add(box).left().padTop(3f).get());
            table.row();
        }
    }

    static class DynamicLimitSliderSetting extends Setting{
        int def, step;
        Intp min, max;
        SettingsMenuDialog.StringProcessor sp;

        public DynamicLimitSliderSetting(String name, int def, Intp min, Intp max, int step, SettingsMenuDialog.StringProcessor s) {
            super(name);
            this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;
            this.sp = s;
        }

        @Override
        public void add(SettingsTable table) {
            Slider slider = new Slider(min.get(), max.get(), step, false);

            slider.setValue(def);

            Label value = new Label("", Styles.outlineLabel);
            Table content = new Table();
            content.add(title, Styles.outlineLabel).left().growX().wrap();
            content.add(value).padLeft(10f).right();
            content.margin(3f, 33f, 3f, 33f);
            content.touchable = Touchable.disabled;

            slider.changed(() -> {
                settings.put(name, (int)slider.getValue());
                value.setText(sp.get((int)slider.getValue()));
            });

            slider.change();
            slider.update(() -> slider.setRange(min.get(), max.get()));

            addDesc(table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f, 460f)).left().padTop(4f).get());
            table.row();
        }
    }

    static class SelectionSetting<T> extends Setting{
        Seq<Selection<T>> items;
        Prov<T> holder;
        Cons<T> consumer;

        public SelectionSetting(String name, Seq<Selection<T>> items, Prov<T> holder, Cons<T> consumer) {
            super(name);

            this.items = items;
            this.holder = holder;
            this.consumer = consumer;
        }

        @Override
        public void add(SettingsTable table) {
            Stack child = new Stack();
            CheckBox box = new CheckBox(title);

            Table is = new Table(b -> items.each(t -> {
                Element elem = b.button(t.icon, () -> {
                    consumer.get(t.item);
                    box.setChecked(false);
                }).size(48).checked(ib -> holder.get() == t.item).get();
                ui.addDescTooltip(elem, t.description);
            }));

            is.visible(box::isChecked);

            is.setSize(30);
            is.setPosition(0, -box.getHeight());

            child.add(box);
            child.add(is);

            box.left();
            addDesc(table.add(child).left().padTop(3f).get());
            table.row();
        }
    }

    static class Selection<T>{
        T item;
        Drawable icon;
        String description;

        public Selection(T item, Drawable icon, String description) {
            this.item = item;
            this.icon = icon;
            this.description = description;
        }
    }

    //压缩
    public enum Compressor{
        quadtree, maxRect, grow
    }

    //缩放
    enum ScalingAlgorithm{
        nearest, bilinear
    }
}
