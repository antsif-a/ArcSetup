package arc.setup;

import arc.*;
import arc.freetype.*;
import arc.freetype.FreetypeFontLoader.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.scene.Scene;
import arc.setup.Dependencies.ProjectDependency;
import arc.setup.Dependencies.ProjectType;
import arc.util.*;

public class UI implements ApplicationListener {
    String homeDir = Core.files.absolute(Core.files.getExternalStoragePath()).toString();
    Seq<String> templates = Seq.with(Core.files.internal("templates/list.txt").readString().replace("\n", "").split(","));
    ProjectBuilder builder;

    Dialog buildDialog;
    Label buildLabel;
    TextField packageField;
    TextField destField;

    public UI() {
        Styles.load();
        Core.scene = new Scene();
        Core.scene.registerStyles(Styles.class);
        Core.input.addProcessor(Core.scene);

        builder = new ProjectBuilder();
        builder.appName = "Project";
        builder.packageName = builder.appName.toLowerCase();
        builder.outputDir = homeDir + "/" + builder.appName;
        builder.template = templates.first();
        builder.modules = Seq.with(ProjectType.core, ProjectType.desktop);
        builder.dependencies = Seq.with(ProjectDependency.arc);
        builder.sdkLocation = OS.propNoNull("ANDROID_HOME");
        builder.print = this::log;
    }

    @Override
    public void init() {
        Core.graphics.setContinuousRendering(false);

        Core.scene.table(t -> {
            t.defaults().pad(10f);

            t.table(prefs -> {
                float fw = 400;

                prefs.defaults().padTop(8);

                prefs.add("Name: ").left();
                prefs.field(builder.appName, name -> {
                    if (builder.packageName.equals(builder.appName.toLowerCase())) {
                        builder.packageName = name.toLowerCase();
                        packageField.setText(builder.packageName);
                    }

                    if (builder.outputDir.equals(homeDir + "/" + builder.appName)) {
                        builder.outputDir = homeDir + "/" + name;
                        destField.setText(builder.outputDir);
                    }

                    builder.appName = name;
                }).width(fw);

                prefs.row();

                prefs.add("Package:").left();
                packageField = prefs.field(builder.packageName, name -> builder.packageName = name).width(fw).get();

                prefs.row();

                prefs.add("Destination:");
                destField = prefs.field(builder.outputDir, name -> builder.outputDir = name).width(fw).get();
            });

            t.row();

            t.table(temp -> {
                temp.marginTop(12);

                temp.add("Template:").padBottom(6).left();
                temp.row();

                temp.table(c -> {
                    ButtonGroup<CheckBox> group = new ButtonGroup<>();

                    templates.each(type -> {
                        c.check(Strings.capitalize(type), type.equals(builder.template),
                                b -> builder.template = type).group(group).pad(4).padRight(8).padLeft(0).fill();
                    });
                });
            }).left();

            t.row();

            t.table(proj -> {
                proj.marginTop(12);

                proj.add("Sub-projects:").padBottom(6).left();
                proj.row();

                proj.table(c -> {
                    for (ProjectType type : ProjectType.values()) {
                        c.check(type.toString(),
                        builder.modules.contains(type), b -> {
                            if (b) {
                                builder.modules.add(type);
                            } else {
                                builder.modules.remove(type);
                            }
                        }).pad(4).padRight(8).padLeft(0);
                    }
                });

            }).left();

            t.row();

            t.button("Generate", this::generate).padTop(10).fill().height(60);
        });

        Core.scene.table(t -> t.top().table(h -> h.add("Arc Project Setup").color(Color.coral).get().setFontScale(1f)).growX().height(50f));

        Core.scene.table(t -> {
            float sz = 40;

            t.top().right();
            t.marginTop(0).marginRight(0);

            t.button("-", () -> {
                try {
                    Class.forName("arc.backend.sdl.jni.SDL").getDeclaredMethod("SDL_MinimizeWindow", long.class)
                            .invoke(null, Reflect.<Long>get(Class.forName("arc.backend.sdl.SdlApplication"), Core.app, "window"));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }).size(sz).padRight(5);
            t.button("X", Core.app::exit).size(sz);
        });
    }

    void generate() {
        callSetup();
    }

    void callSetup() {
        buildDialog = new Dialog("Project Log");
        buildDialog.setFillParent(true);
        buildLabel = new Label("");

        Table inner = new Table().margin(20);
        inner.add(buildLabel).grow().top().left().wrap().get().setAlignment(Align.topLeft, Align.topLeft);

        ScrollPane pane = new ScrollPane(inner);
        pane.setFadeScrollBars(false);

        buildDialog.cont.add(pane).grow().padTop(8);

        new Thread(() -> {
            log("Generating app in " + builder.outputDir + "...\n");

            try{
                builder.build();
            }catch(Exception e){
                e.printStackTrace();
                Core.app.post(() -> {
                    buildDialog.hide();
                    Dialog d = new Dialog("Error generating project!");
                    d.setFillParent(true);
                    d.cont.pane(p -> p.add(Strings.neatError(e, true)).grow());
                    d.buttons.button("oh god", d::hide).size(100f, 50f);
                    d.show();
                });
                return;
            }

            log("Done!\n");

            Core.app.post(() -> {
                buildLabel.invalidateHierarchy();
                buildDialog.invalidateHierarchy();
                buildDialog.cont.invalidateHierarchy();
                buildDialog.pack();

                buildDialog.buttons.button("OK", buildDialog::hide).width(100f);
                buildDialog.buttons.button("Exit", Core.app::exit).width(100f);
            });
        }).start();

        buildDialog.show();
    }

    void log(String str) {
        System.out.println(str);
        Core.app.post(() -> {
            buildLabel.getText().append(str).append("\n");
            buildLabel.invalidateHierarchy();
            buildLabel.pack();
        });
    }

    @Override
    public void update() {
        Core.graphics.clear(Color.black);
        Core.scene.act();
        Core.scene.draw();
        Time.update();
    }

    @Override
    public void resize(int width, int height) {
        Core.scene.resize(width, height);
    }

    public static class Styles {
        public static Drawable black;
        public static Font font;
        public static Label.LabelStyle defaultLabel;
        public static TextField.TextFieldStyle defaultField;
        public static CheckBox.CheckBoxStyle defaultCheck;
        public static Button.ButtonStyle defaultButton;
        public static Dialog.DialogStyle defaultDialog;
        public static ScrollPane.ScrollPaneStyle defaultScrollPane;

        public static TextButton.TextButtonStyle defaultTextButton;

        public static void load() {
            loadFonts();

            black = ((TextureRegionDrawable)drawable("whiteui")).tint(0f, 0f, 0f, 1f);

            defaultLabel = new Label.LabelStyle(font, Color.white);
            defaultField = new TextField.TextFieldStyle(){{
                font = Styles.font;
                fontColor = Color.white;
                disabledFontColor = Color.gray;
                disabledBackground = drawable("underline-disabled");
                selection = drawable("selection");
                background = drawable("underline");
                invalidBackground = drawable("underline-red");
                cursor = drawable("cursor");
                messageFont = Styles.font;
                messageFontColor = Color.gray;
            }};
            defaultCheck = new CheckBox.CheckBoxStyle(){{
                checkboxOn = drawable("check-on");
                checkboxOff = drawable("check-off");
                checkboxOnOver = drawable("check-on-over");
                checkboxOver = drawable("check-over");
                checkboxOnDisabled = drawable("check-on-disabled");
                checkboxOffDisabled = drawable("check-disabled");
                font = Styles.font;
                fontColor = Color.white;
                disabledFontColor = Color.gray;
            }};
            defaultButton = new Button.ButtonStyle(){{
                down = drawable("button-down");
                up = drawable("button");
                over = drawable("button-over");
                disabled = drawable("button-disabled");
            }};
            defaultTextButton = new TextButton.TextButtonStyle(){{
                over = drawable("button-over");
                disabled = drawable("button-disabled");
                font = Styles.font;
                fontColor = Color.white;
                disabledFontColor = Color.gray;
                down = drawable("button-down");
                up = drawable("button");
            }};
            defaultDialog = new Dialog.DialogStyle(){{
                background = drawable("window-empty");
                titleFont = Styles.font;
                stageBackground = black;
                titleFontColor = Color.valueOf("ffd37f");
            }};
            defaultScrollPane = new ScrollPane.ScrollPaneStyle(){{
                vScroll = drawable("scroll");
                vScrollKnob = drawable("scroll-knob-vertical");
            }};
        }

        public static Drawable drawable(String name) {
            return Core.atlas.drawable(name);
        }

        public static void loadFonts() {
            Core.assets.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(Core.files::internal));
            Core.assets.setLoader(Font.class, new FreetypeFontLoader(Core.files::internal));

            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter(){{
                incremental = true;
            }};

            Core.assets.load("font", Font.class, new FreeTypeFontLoaderParameter("fonts/audiowide-regular.ttf", p));
            Core.assets.finishLoadingAsset("font");
            font = Core.assets.get("font");
            font.setUseIntegerPositions(true);
        }
    }
}
