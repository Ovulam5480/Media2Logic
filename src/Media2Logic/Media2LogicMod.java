package Media2Logic;

import Media2Logic.LogicImage.LogicImageDialog;
import arc.util.Log;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;

public class Media2LogicMod extends Mod{
    public static LogicImageDialog logicImageDialog;

    @Override
    public void init() {
        logicImageDialog = new LogicImageDialog();
        Vars.ui.schematics.buttons.button("逻辑图像", Icon.image, () -> logicImageDialog.show());
    }
}
