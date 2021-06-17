package arc.setup;

import arc.struct.ObjectMap;
import arc.util.*;

public class Dependencies {
    //Versions
    static String roboVMVersion = "2.3.0";
    static String androidToolsVersion = "4.2.0";
    static String buildToolsVersion = "28.0.3";
    static String androidAPILevel = "28";

    //Project plugins
    static String androidPluginImport = "com.android.tools.build:gradle:" + androidToolsVersion;
    static String roboVMPluginImport = "com.mobidevelop.robovm:robovm-gradle-plugin:" + roboVMVersion;

    /**
     * This enum will hold all dependencies available for libgdx, allowing the setup to pick the ones needed by default,
     * and allow the option to choose extensions as the user wishes.
     * <p/>
     * These depedency strings can be later used in a simple gradle plugin to manipulate the users project either after/before
     * project generation
     */
    public enum ProjectDependency {
        arc(
            ProjectType.core, new String[]{"arc arc-core"},
            ProjectType.desktop, new String[]{"arc backends:backend-sdl", "arc natives:natives-desktop", "arc natives:natives-freetype-desktop"},
            ProjectType.android, new String[]{"arc backends:backend-android", "arc natives:natives-android", "arc natives:natives-freetype-android"},
            ProjectType.ios, new String[]{"arc backends:backend-robovm", "arc natives:natives-ios", "arc natives:natives-freetype-ios"}
        );

        public final ObjectMap<ProjectType, String[]> dependencies;

        ProjectDependency(Object... deps) {
            this.dependencies = ObjectMap.of(deps);
        }
    }

    public enum ProjectType {
        core(null, "java"),
        desktop(null, "java"),
        android(androidPluginImport, "com.android.application"),
        ios(roboVMPluginImport, "java", "robovm");

        public final String classpathPlugin;
        public final String[] plugins;

        ProjectType(String classpathPlugin, String... plugins) {
            this.classpathPlugin = classpathPlugin;
            this.plugins = plugins;
        }

        @Override
        public String toString() {
            return this == ios ? "IOS" : Strings.capitalize(name());
        }
    }
}
