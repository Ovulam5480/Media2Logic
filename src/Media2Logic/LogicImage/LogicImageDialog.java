package Media2Logic.LogicImage;

import Media2Logic.LogicImage.ImageToLogic.Compressors;
import Media2Logic.LogicImage.ImageToLogic.Scaler;
import arc.Core;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Threads;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;
import mindustry.world.Block;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.Setting;
import mindustry.world.blocks.logic.TileableLogicDisplay;

import static arc.Core.settings;
import static mindustry.Vars.ui;

public class LogicImageDialog extends BaseDialog {
    Table reviewTable = new Table();
    SettingsTable settingsTable = new SettingsTable(){
        final Cell empty = new Cell<>();

        @Override
        public <T extends Element> Cell<T> add(T element) {
            if(element.getClass() == TextButton.class){
                return empty;
            }
            return super.add(element);
        }
    };

    LogicBlock logicBlock = (LogicBlock) Blocks.microProcessor;
    LogicDisplay logicDisplay = (LogicDisplay) Blocks.logicDisplay;
    int widthAmount = 1, heightAmount = 1;
    boolean minScale = false;
    boolean limit = false;
    boolean stopCompress = false;
    int logicGap = 0;
    
    float imageSize = 300f;

    Pixmap image = new Pixmap(180,180);
    Pixmap afterScaling = new Pixmap(0,0);
    Pixmap afterCompressor = new Pixmap(0,0);
    Pixmap allBlack = new Pixmap(1,1);
    Compressor compressor = Compressor.quadtree;
    ScalingAlgorithm algorithm = ScalingAlgorithm.nearest;
    String name = null;

    private volatile boolean previewCancelled = false;
    private Thread previewThread = null;
    private volatile boolean previewProcessing = false;
    private volatile long previewStartTime = 0;

    private Label statusLabel;
    private boolean updateListenerAdded = false;

    public LogicImageDialog() {
        super("逻辑图像");

        allBlack.set(0, 0, Color.black);
        cont.add(reviewTable).row();

        ScrollPane pane = new ScrollPane(settingsTable, Styles.noBarPane);
        pane.setOverscroll(false, false);
        pane.setScrollingDisabled(true, false);
        cont.add(pane);

        Seq<Setting> list = settingsTable.getSettings();

        list.add(new NotSavedSetting("逻辑图像名称", "", () -> name,s -> name = s));

        list.add(new NotSavedCheckSetting("禁用色块预览图更新", false, () -> stopCompress, b -> {
            stopCompress = b;
            if(!b)scaling();
        }));

        list.add(new NotSavedCheckSetting("比例缩放", true, () -> minScale, b -> {
            minScale = b;

            scaling();
        }));

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
        for (Block block : Vars.content.blocks().select(b -> b instanceof LogicDisplay && !(b instanceof TileableLogicDisplay))) {
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
                        new Selection<>(Compressor.quadtree, Icon.move, "四叉树递归"),
                        new Selection<>(Compressor.maxRect, Icon.distribution, "最大矩形分割"),
                        new Selection<>(Compressor.grow, Icon.logic, "贪婪生长"),
                        new Selection<>(Compressor.delaunay, Icon.play, "三角剖分, 该算法会关闭预览图更新"),
                        new Selection<>(Compressor.bestHillClimb, Icon.terrain, "爬山迭代拟合, 该算法会关闭预览图更新")

                ),
                () -> compressor,
                c -> {
                    compressor = c;

                    if(compressor == Compressor.delaunay || compressor == Compressor.bestHillClimb){
                        stopCompress = true;
                        return;
                    }

                    scaling();
                }
        ));

        list.add(new DynamicLimitSliderSetting("色块容差/逻辑数量", (int) Compressors.tolerance * 100,
                () -> 0,
                () -> 100,
                1,
                i -> {
                    Compressors.tolerance = i / 100f;
                    scaling();
                    return i + (compressor == Compressor.bestHillClimb ? "个" : "%");
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

        list.add(new NotSavedCheckSetting("允许某边过长", false, () -> limit, b -> {
            limit = b;
        }));

        Intp maxAmount = () -> Mathf.clamp(Mathf.ceil(((int)logicBlock.range - logicBlock.size * 8 / 2f) / (logicDisplay.size * 8)) * 2, 1, 32);

        list.add(new DynamicLimitSliderSetting("逻辑画板宽数", 1,
                () -> 1,
                () -> heightAmount > maxAmount.get() ? maxAmount.get() : limit ? 32 : maxAmount.get(),
                1,
                i -> {
                    widthAmount = i;
                    scaling();
                    return "" + i;
        }));

        list.add(new DynamicLimitSliderSetting("逻辑画板高数", 1,
                () -> 1,
                () -> widthAmount > maxAmount.get() ? maxAmount.get() : limit ? 32 : maxAmount.get(),
                1,
                i -> {
                    heightAmount = i;
                    scaling();
                    return "" + i;
        }));

        //todo 允许自定义X,Y轴的边长补偿, 让我偷个懒(?
        list.add(new DynamicLimitSliderSetting("逻辑处理器边长补偿(存在部分问题,不想修)", 0,
                () -> 0,
                () -> logicBlock.size,
                1,
                i -> {
                    logicGap = i;
                    return "" + i;
                }));

        list.add(new ToggleCheckSetting("生成并保存蓝图", () -> {
            generateBlueprintAsync(Vars.ui.schematics);
        }));

        settingsTable.rebuild();
        addCloseButton();
        scaling();
    }

    void scaling(){
        if(previewThread != null && previewThread.isAlive()){
            previewCancelled = true;
            try {
                previewThread.join(5000);
            } catch(InterruptedException ignored){
            }
        }
        previewCancelled = false;
        
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

        if(!stopCompress) {
            // 异步执行压缩
            previewProcessing = true;
            previewStartTime = System.currentTimeMillis();
            previewThread = Threads.daemon("PreviewGenerator", () -> {
                try {
                    compressPixmap(afterScaling, compressor);
                    if(!previewCancelled){
                        afterCompressor = Compressors.decompressor(compressor == Compressor.bestHillClimb);
                        Core.app.post(this::refreshReviewImage);
                    }
                }finally {
                    previewProcessing = false;
                }
            });
        }else{
            refreshReviewImage();
        }
    }

    void generateBlueprintAsync(BaseDialog schematics){
        scaling();
        Compressors.setPixmap(afterScaling);

        ui.loadfrag.setProgress(0f);
        ui.loadfrag.show("正在生成蓝图...");
        
        final boolean[] cancelled = {false};

        ui.loadfrag.setButton(() -> {
            cancelled[0] = true;
            ui.loadfrag.hide();
        });
        
        Threads.thread("BlueprintGenerator", () -> {
            try {
                Compressors.compressorWithProgress(
                    logicDisplay, 
                    logicBlock, 
                    widthAmount, 
                    heightAmount, 
                    compressor, 
                    name,
                    compressor == Compressor.bestHillClimb ? 60 : -1,
                    logicGap,
                        (show, progress) -> {
                        if(!cancelled[0]){
                            Core.app.post(() -> {
                                ui.loadfrag.setProgress(progress);
                                ui.loadfrag.setText(show);
                            });
                        }
                    },
                    () -> cancelled[0]
                );
                
                if(!cancelled[0]){
                    Core.app.post(() -> {
                        ui.loadfrag.hide();
                        schematics.hide();
                        schematics.show();
                    });
                }
            }catch(Exception e){
                if(!cancelled[0]){
                    Core.app.post(() -> {
                        ui.loadfrag.hide();
                        ui.showErrorMessage("蓝图生成失败：" + e.getMessage());
                        Log.err(e);
                    });
                }
            }
        });
    }

    public static void compressPixmap(Pixmap pixmap, Compressor compressor){
        Compressors.setPixmap(pixmap);

        if(compressor == Compressor.quadtree) Compressors.quadtreeCompressor();
        else if(compressor == Compressor.maxRect) Compressors.maxRectCompress();
        else if(compressor == Compressor.grow) Compressors.growCompress();
        else if(compressor == Compressor.delaunay) Compressors.triSplitCompress();
        else if(compressor == Compressor.bestHillClimb) Compressors.bestHillClimb();
    }

    void refreshReviewImage(){
        reviewTable.clear();
        updateListenerAdded = false; // 重置标志

        int before = afterScaling.width * afterScaling.height;

        if(Vars.mobile){
            Table imageTable = new Table();
            ScrollPane pane = new ScrollPane(imageTable);
            pane.setOverscroll(false, false);
            pane.setScrollingDisabled(true, false);
            reviewTable.add(pane);

            addImage(image, imageTable);
            imageTable.row();

            addImage(stopCompress ? allBlack : afterScaling, imageTable);
            imageTable.add(before + "");
            imageTable.row();

            addImage(afterCompressor, imageTable);
            statusLabel = imageTable.add("").get();
        }else {
            addImage(image, reviewTable);
            addImage(afterScaling, reviewTable);
            addImage(stopCompress ? allBlack : afterCompressor, reviewTable);
        }

        reviewTable.row();
        reviewTable.button("选择图片(png格式)", () -> Vars.platform.showFileChooser(true, "png", f -> {
            image.dispose();
            try {
                image = PixmapIO.readPNG(f);
            } catch (Exception e) {
                ui.showErrorMessage("无法读取文件");
            }
            scaling();
        })).width(100f);

        if(!Vars.mobile) {
            reviewTable.add(before + "");
            statusLabel = reviewTable.add("").get();
        }
        
        setupStatusLabelUpdate();
    }
    
    void setupStatusLabelUpdate(){
        if(statusLabel == null || updateListenerAdded) return;
        
        updateListenerAdded = true;
        statusLabel.update(() -> {
            if(stopCompress){
                statusLabel.setText("已禁用更新");
            }else if(previewProcessing){
                long elapsed = System.currentTimeMillis() - previewStartTime;
                statusLabel.setText("处理中... (" + (elapsed / 100)/10f + "s)");
            }else if(updateListenerAdded){
                // 编译完成，更新最终结果
                int finalAfter = Compressors.rectSize();
                int finalBefore = afterScaling.width * afterScaling.height;
                statusLabel.setText(finalAfter + "(" + String.format("%.2f", finalAfter * 100f / finalBefore) + "%)");
                // 移除更新监听器（通过设置标志）
                updateListenerAdded = false;
            }
        });
    }

    void addImage(Pixmap pixmap, Table table){
        float scale = imageSize / Math.max(pixmap.width, pixmap.height);
        float w = pixmap.width * scale;
        float h = pixmap.height * scale;

        TextureRegion region = new TextureRegion(new Texture(pixmap));
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

            // 滑动过程中只更新数值显示，不触发 sp.get()
            slider.changed(() -> {
                value.setText(String.valueOf((int)slider.getValue()));
            });

            slider.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    return true; // 先返回 true 以接收后续的 touchUp 事件
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                    // 只在松开时调用 sp.get() 并保存设置
                    settings.put(name, (int)slider.getValue());
                    value.setText(sp.get((int)slider.getValue()));
                }
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
        quadtree, maxRect, grow, delaunay, bestHillClimb
    }

    //缩放
    enum ScalingAlgorithm{
        nearest, bilinear
    }
}
