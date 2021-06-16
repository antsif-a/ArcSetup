package arc.setup.desktop;

import arc.*;
import arc.backend.sdl.*;
import arc.backend.sdl.jni.*;
import arc.setup.*;
import arc.util.*;

public class DesktopLauncher {
	public static void main(String[] arg) {
		SdlConfig config = new SdlConfig();
		config.title = "Arc Project Setup";
		config.width = 800;
		config.height = 600;
		config.decorated = false;
		config.resizable = false;

		new SdlApplication(new ArcSetup(), config);
	}
}
