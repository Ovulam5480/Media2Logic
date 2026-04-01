package Media2Logic.AtlasViewer;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Dialog;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.ui;

public class AtlasViewerDialog extends BaseDialog {
    Table view = new Table();
    Cons<String> stringCons;

    public AtlasViewerDialog() {
        super("图集查看器");
        build();

        addCloseButton();

        //Vars.ui.logic.shown(() -> Vars.ui.logic.buttons.button("图集查看器", this::show));

        hidden(() -> view.setPosition(0, 0));
    }

    public Dialog show(Cons<String> stringCons) {
        this.stringCons = stringCons;
        return super.show();
    }

    void build(){
        ScrollPane pane = new ScrollPane(view);
        pane.setOverscroll(false, false);
        pane.setScrollingDisabled(true, false);
        cont.add(pane);

        view.table(imageTable -> {
            Cell<Table> cell = imageTable.table();

            ObjectMap<String, TextureAtlas.AtlasRegion> regions = Core.atlas.getRegionMap();
            for (String name : regions.keys().toSeq().sort(String::compareTo)) {
                TextureRegion region = regions.get(name);

                if (cell.minWidth() + region.width > Core.graphics.getWidth() - 200) {
                    cell.row();
                    cell = imageTable.table();
                }

                ImageButton elem = cell.get().button(new TextureRegionDrawable(region), Styles.clearNonei, () -> {
                    if(stringCons != null){
                        stringCons.get(name);
                        stringCons = null;
                    }

                    hide();
                }).size(region.width, region.height).get();

                ui.addDescTooltip(elem, name);
            }
        }).margin(40);
    }
}
