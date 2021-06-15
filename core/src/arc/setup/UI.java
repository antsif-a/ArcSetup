package arc.setup;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.assets.loaders.resolvers.*;
import arc.files.*;
import arc.freetype.*;
import arc.freetype.FreetypeFontLoader.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.scene.Scene;
import arc.setup.DependencyBank.ProjectDependency;
import arc.setup.DependencyBank.ProjectType;
import arc.util.*;

public class UI implements ApplicationListener{
    String homeDir = Core.files.absolute(Core.files.getExternalStoragePath()).toString();

    String[] templates = {"default", "gamejam", "simple"};

    Graphics graphics = Core.graphics;
    ArcSetup setup = new ArcSetup();

    Dialog buildDialog;
    Label buildLabel;
    TextField packageField;
    TextField destField;

    public UI(){
        Core.assets = new AssetManager();
        Core.batch = new SpriteBatch();
        Core.atlas = new TextureAtlas(new Fi("ui/ui.atlas"));

        Styles.load();
        Core.scene = new Scene();
        Core.scene.registerStyles(Styles.class);
        Core.input.addProcessor(Core.scene);

        setup.appName = "Project";
        setup.packageName = setup.appName.toLowerCase();
        setup.outputDir = homeDir + "/" + setup.appName;
        setup.template = templates[0];
        setup.modules = Seq.with(ProjectType.core, ProjectType.desktop);
        setup.dependencies = Seq.with(ProjectDependency.arc);
        setup.sdkLocation = "/home/anuke/Android/Sdk"; // todo custom location
        setup.callback = this::printlog;
    }

    @Override
    public void init(){
        Core.graphics.setContinuousRendering(false);

        Core.scene.table(t -> {
            t.defaults().pad(10f);

            t.row();

            t.table(prefs -> {
                float fw = 400;

                prefs.defaults().padTop(8);

                prefs.add("Name: ").left();
                prefs.field(setup.appName, name -> {
                    if (setup.packageName.equals(setup.appName.toLowerCase())) {
                        setup.packageName = name.toLowerCase();
                        packageField.setText(setup.packageName);
                    }

                    if (setup.outputDir.equals(homeDir + "/" + setup.appName)) {
                        setup.outputDir = homeDir + "/" + name;
                        destField.setText(setup.outputDir);
                    }

                    setup.appName = name;
                }).width(fw);
                prefs.row();

                prefs.add("Package:").left();
                packageField = prefs.field(setup.packageName, name -> setup.packageName = name).width(fw).get();
                prefs.row();

                prefs.add("Destination:");
                destField = prefs.field(setup.outputDir, name -> setup.outputDir = name).width(fw).get();
            });

            t.row();

            t.table(temp -> {
                temp.marginTop(12).margin(14f).left();
                temp.add("Template:").left().padBottom(6).left();

                temp.row();
                temp.table(groups -> {
                    ButtonGroup<CheckBox> group = new ButtonGroup<>();

                    for(String type : templates){
                        groups.check(Strings.capitalize(type), type.equals(setup.template), b -> setup.template = type)
                        .group(group).pad(4).left().padRight(8).padLeft(0).fill();
                    }
                });
            });

            t.row();

            t.table(proj -> {
                proj.marginTop(12).margin(14f).left();

                proj.add("Sub-projects:").left().padBottom(6).left();
                proj.row();

                proj.table(c -> {
                    for(ProjectType type : ProjectType.values()){
                        c.check(Strings.capitalize(type.name()),
                        setup.modules.contains(type), b -> {
                            if(b){
                                setup.modules.add(type);
                            }else{
                                setup.modules.remove(type);
                            }
                        }).pad(4).left().padRight(8).padLeft(0);
                    }
                });

            }).fillX();

            //refer to initial commit to get the extensions menu out

            t.row();

            t.button("Generate", this::generate).padTop(10).fill().height(60);
        });

        Core.scene.table(t -> t.top().table(h -> h.add("Arc Project Setup").color(Color.coral).get().setFontScale(1f)).growX().height(50f));

        Core.scene.table(t -> {
            float sz = 50;

            t.top().right();
            t.marginTop(0).marginRight(0);

            // todo: t.button("-", graphics.getWindow()::iconifyWindow).size(sz);
            t.button("X", Core.app::exit).size(sz);
        });
    }

    void generate(){
        callSetup();
    }

    void callSetup(){
        buildDialog = new Dialog("Project Log");
        buildDialog.setFillParent(true);
        buildLabel = new Label("");

        Table inner = new Table().margin(20);
        inner.add(buildLabel).grow().top().left().wrap().get().setAlignment(Align.topLeft, Align.topLeft);

        ScrollPane pane = new ScrollPane(inner);
        pane.setFadeScrollBars(false);

        buildDialog.cont.add(pane).grow().padTop(8);

        new Thread(() -> {
            printlog("Generating app in " + setup.outputDir + "...\n");

            try{
                setup.build();
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

            printlog("Done!\n");

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

    void printlog(String str){
        System.out.println(str);
        Core.app.post(() -> {
            buildLabel.getText().append(str).append("\n");
            buildLabel.invalidateHierarchy();
            buildLabel.pack();
        });
    }

    @Override
    public void update(){
        Core.graphics.clear(Color.black);
        Core.scene.act();
        Core.scene.draw();
        Time.update();
    }

    @Override
    public void resize(int width, int height){
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
                disabled = drawable("button-gray");
            }};
            defaultTextButton = new TextButton.TextButtonStyle(){{
                over = drawable("button-over");
                disabled = drawable("button-gray");
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
            FileHandleResolver resolver = new InternalFileHandleResolver();
            Core.assets.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
            Core.assets.setLoader(Font.class, null, new FreetypeFontLoader(resolver){
                ObjectSet<FreeTypeFontGenerator.FreeTypeFontParameter> scaled = new ObjectSet<>();

                @Override
                public Font loadSync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
                    if(fileName.equals("outline")){
                        parameter.fontParameters.borderWidth = Scl.scl(2f);
                        parameter.fontParameters.spaceX -= parameter.fontParameters.borderWidth;
                    }

                    if(!scaled.contains(parameter.fontParameters) && !ObjectSet.with("iconLarge").contains(fileName)){
                        parameter.fontParameters.size = (int)(Scl.scl(parameter.fontParameters.size));
                        scaled.add(parameter.fontParameters);
                    }

                    parameter.fontParameters.magFilter = Texture.TextureFilter.linear;
                    parameter.fontParameters.minFilter = Texture.TextureFilter.linear;
                    return super.loadSync(manager, fileName, file, parameter);
                }
            });

            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter(){{
                size = 16;
                incremental = true;
            }};

            Core.assets.load("font", Font.class, new FreeTypeFontLoaderParameter("fonts/audiowide-regular.ttf", p)).loaded = f -> font = (Font)f;
            Core.assets.finishLoadingAsset("font");

            font.setUseIntegerPositions(true);
        }
    }
}
