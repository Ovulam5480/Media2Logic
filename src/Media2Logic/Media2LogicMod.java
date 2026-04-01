package Media2Logic;

import Media2Logic.AtlasViewer.AtlasViewerDialog;
import Media2Logic.AtlasViewer.StatementElemModifier;
import Media2Logic.LogicImage.LogicImageDialog;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;

public class Media2LogicMod extends Mod{
    public static LogicImageDialog logicImageDialog;
    public static AtlasViewerDialog atlasViewerDialog;

    @Override
    public void init() {
        logicImageDialog = new LogicImageDialog();
        Vars.ui.schematics.buttons.button("逻辑图像", Icon.image, () -> logicImageDialog.show());

        atlasViewerDialog = new AtlasViewerDialog();

        new StatementElemModifier();
    }
}
