package Media2Logic;

import Media2Logic.LogicImage.Compressors;
import Media2Logic.LogicImage.LogicImageDialog;
import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.mod.*;
import mindustry.world.blocks.logic.LogicBlock;

public class Media2LogicMod extends Mod{
    public static LogicImageDialog logicImageDialog;


    public Media2LogicMod(){
    }

    @Override
    public void init() {
        logicImageDialog = new LogicImageDialog();
        Vars.ui.schematics.buttons.button("逻辑图像", Icon.image, () -> logicImageDialog.show());
    }
}
