package Media2Logic.AtlasViewer;

import Media2Logic.Media2LogicMod;
import arc.Events;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.logic.LCanvas;
import mindustry.logic.LMarkerControl;
import mindustry.logic.LStatements;
import mindustry.ui.Styles;

import java.lang.reflect.Field;

public class StatementElemModifier {
    private int amount;

    @SuppressWarnings("unchecked")
    public StatementElemModifier(){
        Events.run(EventType.Trigger.update, () -> {
            if(!Vars.ui.logic.isShown())return;

            Seq<Element> elements;

            Class<?> clazz = LCanvas.DragLayout.class;
            try {
                Field field = clazz.getDeclaredField("seq");
                field.setAccessible(true);
                elements = (Seq<Element>) field.get(Vars.ui.logic.canvas.statements);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            if(elements.size != amount){
                amount = elements.size;
                modify(elements);
            }
        });
    }

    public void modify(Seq<Element> elements){
        elements.each(e -> {
            if(e instanceof LCanvas.StatementElem se
                    && se.st instanceof LStatements.SetMarkerStatement st
                    && st.type == LMarkerControl.texture){
                Table table = (Table) ((Table)se.getChildren().get(1)).getChildren().getFrac(1);
                if(table.getChildren().size <= 2) {
                    table.button(b -> {
                        b.image(Icon.pencilSmall);
                        b.clicked(() -> Media2LogicMod.atlasViewerDialog.show(s -> {
                            st.p2 = '"' + s + '"';
                            Vars.ui.logic.canvas.rebuild();
                        }));
                    }, Styles.logict, () -> {}).size(40f).padLeft(-11).color(table.color);
                }
            }
        });
    }
}
