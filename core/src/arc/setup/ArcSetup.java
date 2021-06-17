package arc.setup;

import arc.*;
import arc.assets.*;
import arc.graphics.g2d.*;

public class ArcSetup extends ApplicationCore {
    @Override
    public void setup() {
        Core.assets = new AssetManager();
        Core.batch = new SpriteBatch();
        Core.atlas = new TextureAtlas("ui/ui.atlas");

        add(new UI());
    }
}
