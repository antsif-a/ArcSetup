package arc.setup.desktop;

import arc.ApplicationCore;
import arc.backend.sdl.*;
import arc.setup.UI;

public class DesktopLauncher {
	public static void main(String[] arg) {
		SdlConfig config = new SdlConfig();
		config.title = "Arc Project Setup";
		config.width = 800;
		config.height = 600;
		config.decorated = false;
		config.resizable = false;

		new SdlApplication(new ApplicationCore(){
			@Override
			public void setup(){
				add(new UI());
			}
		}, config);
	}
}
