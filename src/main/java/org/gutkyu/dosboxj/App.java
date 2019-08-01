package org.gutkyu.dosboxj;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.misc.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.util.*;

public final class App {
    public static void main(String[] args) {
        System.out.printf("App Start up!");
        GUIPlatform.setup();
        App app = new App();
        app.commandLineArgs = args;
        app.start();
        GUIPlatform.shutdown();
        System.exit(0);

    }

    private void launchEditor() {
        // std::string path,file;
        // Cross::CreatePlatformConfigDir(path);
        // Cross::GetPlatformConfigName(file);
        // path += file;
        // FILE* f = fopen(path.c_str(),"r");
        // if(!f && !control.PrintConfig(path.c_str())) {
        // printf("tried creating %s. but failed.\n",path.c_str());
        // exit(1);
        // }
        // if(f) fclose(f);
        /// * if(edit.empty()) {
        // printf("no editor specified.\n");
        // exit(1);
        // }*/
        // std::string edit;
        // while(control.cmdline.FindString("-editconf",edit,true)) //Loop until one
        // succeeds
        // execlp(edit.c_str(),edit.c_str(),path.c_str(),(char*) 0);
        // //if you get here the launching failed!
        // printf("can't find editor(s) specified at the command line.\n");
        // exit(1);
    }

    private void printConfiglocation() {
        // std::string path,file;
        // Cross::CreatePlatformConfigDir(path);
        // Cross::GetPlatformConfigName(file);
        // path += file;

        // FILE* f = fopen(path.c_str(),"r");
        // if(!f && !control.PrintConfig(path.c_str())) {
        // printf("tried creating %s. but failed",path.c_str());
        // exit(1);
        // }
        // if(f) fclose(f);
        // printf("%s\n",path.c_str());
        // exit(0);
    }

    private void eraseConfigFile() {
        // FILE* f = fopen("dosbox.conf","r");
        // if(f) {
        // fclose(f);
        // show_warning("Warning: dosbox.conf exists in current working directory.\nThis
        // will override the configuration file at runtime.\n");
        // }
        // std::string path,file;
        // Cross::GetPlatformConfigDir(path);
        // Cross::GetPlatformConfigName(file);
        // path += file;
        // f = fopen(path.c_str(),"r");
        // if(!f) exit(0);
        // fclose(f);
        // unlink(path.c_str());
        // exit(0);
    }

    private void eraseMapperFile() {
        // FILE* g = fopen("dosbox.conf","r");
        // if(g) {
        // fclose(g);
        // show_warning("Warning: dosbox.conf exists in current working
        // directory.\nKeymapping might not be properly reset.\n"
        // "Please reset configuration as well and delete the dosbox.conf.\n");
        // }

        // std::string path,file=MAPPERFILE;
        // Cross::GetPlatformConfigDir(path);
        // path += file;
        // FILE* f = fopen(path.c_str(),"r");
        // if(!f) exit(0);
        // fclose(f);
        // unlink(path.c_str());
        // exit(0);
    }

    public void addGUIConfig() throws WrongType {
        // Section_prop sdl_sec=dosbox.control.AddSection_prop("sdl",GUI_StartUp);
        SectionProperty guiSec = DOSBox.Control.addSectionProp("sdl", GUIPlatform::startupGUI);
        guiSec.addInitFunction(GUIPlatform.mapper::startup);
        PropertyBool pBool;
        PropertyString pString;
        PropertyInt pInt;
        PropertyMultival pMulti;

        pBool = guiSec.addBool("fullscreen", Property.Changeable.Always, false);
        pBool.setHelp("Start dosbox directly in fullscreen. (Press ALT-Enter to go back)");

        pBool = guiSec.addBool("fulldouble", Property.Changeable.Always, false);
        pBool.setHelp(
                "Use double buffering in fullscreen. It can reduce screen flickering, but it can also result in a slow DOSBox.");

        pString = guiSec.addString("fullresolution", Property.Changeable.Always, "original");
        pString.setHelp(
                "What resolution to use for fullscreen: original or fixed size (e.g. 1024x768).\n"
                        + "  Using your monitor's native resolution with aspect=true might give the best results.\n"
                        + "  If you end up with small window on a large screen, try an output different from surface.");

        pString = guiSec.addString("windowresolution", Property.Changeable.Always, "original");
        pString.setHelp(
                "Scale the window to this size IF the output device supports hardware scaling.\n"
                        + "  (output=surface does not!)");

        String[] outputs = {"surface", "overlay"};
        pString = guiSec.addString("output", Property.Changeable.Always, "surface");
        pString.setHelp("What video system to use for output.");
        pString.setValues(outputs);

        pBool = guiSec.addBool("autolock", Property.Changeable.Always, true);
        pBool.setHelp(
                "Mouse will automatically lock, if you click on the screen. (Press CTRL-F10 to unlock)");

        pInt = guiSec.addInt("sensitivity", Property.Changeable.Always, 100);
        pInt.setMinMax(new Value(1), new Value(1000));
        pInt.setHelp("Mouse sensitivity.");

        pBool = guiSec.addBool("waitonerror", Property.Changeable.Always, true);
        pBool.setHelp("Wait before closing the console if dosbox has an error.");

        pMulti = guiSec.addMulti("priority", Property.Changeable.Always, ',');
        pMulti.setValue("higher,normal");
        pMulti.setHelp(
                "Priority levels for dosbox. Second entry behind the comma is for when dosbox is not focused/minimized.\n"
                        + "  pause is only valid for the second entry.");

        String[] actt = {"lowest", "lower", "normal", "higher", "highest", "pause"};
        pString = pMulti.getSection().addString("active", Property.Changeable.Always, "higher");
        pString.setValues(actt);

        String[] inactt = {"lowest", "lower", "normal", "higher", "highest", "pause"};
        pString = pMulti.getSection().addString("inactive", Property.Changeable.Always, "normal");
        pString.setValues(inactt);

        pString = guiSec.addPath("mapperfile", Property.Changeable.Always, Mapper.MAPPERFILE);
        pString.setHelp(
                "File used to load/save the key/event mappings from. Resetmapper only works with the defaul value.");

        pBool = guiSec.addBool("usescancodes", Property.Changeable.Always, true);
        pBool.setHelp("Avoid usage of symkeys, might not work on all operating systems.");
    }

    public String[] commandLineArgs = null;

    private void setup() throws WrongType {
        String[] argv = commandLineArgs;
        int argc = argv.length;

        CommandLine cmdLine = new CommandLine(argc, argv);
        Config conf = new Config(cmdLine);
        DOSBox.Control = conf;
        /* Init the configuration system and add default values */
        addGUIConfig();
        DOSBox.init();

        String editor = "";
        if (DOSBox.Control.CmdLine.findString("-editconf", false)) {
            editor = DOSBox.Control.CmdLine.returnedString;
            launchEditor();
        }
        // if(dosbox.control.cmdline.FindString("-opencaptures",ref editor,true))
        // launchcaptures(editor);
        if (DOSBox.Control.CmdLine.findExist("-eraseconf"))
            eraseConfigFile();
        if (DOSBox.Control.CmdLine.findExist("-resetconf"))
            eraseConfigFile();
        if (DOSBox.Control.CmdLine.findExist("-erasemapper"))
            eraseMapperFile();
        if (DOSBox.Control.CmdLine.findExist("-resetmapper"))
            eraseMapperFile();

        /* Can't disable the console with debugger enabled */
        if (DOSBox.Control.CmdLine.findExist("-version")
                || DOSBox.Control.CmdLine.findExist("--version")) {
            System.out.printf("\nDOSBox.j version %s, copyright 2019 Sangkyu Jung.\n",
                    DOSBox.VERSION);
            System.out.printf("\nDOSBox version %s, copyright 2002-2010 DOSBox Team.\n\n",
                    DOSBox.BASE_VERSION);
            System.out.printf("DOSBox is written by the DOSBox Team (See AUTHORS file))\n");
            System.out
                    .printf("DOSBox comes with ABSOLUTELY NO WARRANTY.  This is free software,\n");
            System.out.printf("and you are welcome to redistribute it under certain conditions;\n");
            System.out.printf("please read the COPYING file thoroughly before doing so.\n\n");
            throw new DOSException();
        }
        if (DOSBox.Control.CmdLine.findExist("-printconf"))
            printConfiglocation();

        /* Display Welcometext in the console */
        Log.logMsg("DOSBox.j version %s", DOSBox.VERSION);
        Log.logMsg("DOSBox version %s", DOSBox.BASE_VERSION);
        // LOG_MSG("Copyright 2002-2010 DOSBox Team, published under GNU GPL.");
        // LOG_MSG("---");

        /* Init SDL */

        // 그래픽라이브러리 초기화?, 각종 장치 초기화를 여기에서 진행하는듯, 나중에 구현
        // if ( SDL_Init( SDL_INIT_AUDIO|SDL_INIT_VIDEO|SDL_INIT_TIMER|SDL_INIT_CDROM
        // |SDL_INIT_NOPARACHUTE
        // ) < 0 ) E_Exit("Can't init SDL %s",SDL_GetError());
        // sdl.inited = true;

        // #ifndef DISABLE_JOYSTICK
        // //Initialise Joystick seperately. This way we can warn when it fails instead
        // //of exiting the application
        // if( SDL_InitSubSystem(SDL_INIT_JOYSTICK) < 0 ) LOG_MSG("Failed to init joystick
        // support");
        // #endif

        // sdl.laltstate = SDL_KEYUP;
        // sdl.raltstate = SDL_KEYUP;

        // #if defined (WIN32)
        // #if SDL_VERSION_ATLEAST(1, 2, 10)
        // sdl.using_windib=true;
        // #else
        // sdl.using_windib=false;
        // #endif
        // char sdl_drv_name[128];
        // if (getenv("SDL_VIDEODRIVER")==NULL) {
        // if (SDL_VideoDriverName(sdl_drv_name,128)!=NULL) {
        // sdl.using_windib=false;
        // if (strcmp(sdl_drv_name,"directx")!=0) {
        // SDL_QuitSubSystem(SDL_INIT_VIDEO);
        // putenv("SDL_VIDEODRIVER=directx");
        // if (SDL_InitSubSystem(SDL_INIT_VIDEO)<0) {
        // putenv("SDL_VIDEODRIVER=windib");
        // if (SDL_InitSubSystem(SDL_INIT_VIDEO)<0) E_Exit("Can't init SDL Video
        // %s",SDL_GetError());
        // sdl.using_windib=true;
        // }
        // }
        // }
        // } else {
        // char* sdl_videodrv = getenv("SDL_VIDEODRIVER");
        // if (strcmp(sdl_videodrv,"directx")==0) sdl.using_windib = false;
        // else if (strcmp(sdl_videodrv,"windib")==0) sdl.using_windib = true;
        // }
        // if (SDL_VideoDriverName(sdl_drv_name,128)!=NULL) {
        // if (strcmp(sdl_drv_name,"windib")==0) LOG_MSG("SDL_Init: Starting up with SDL windib
        // video driver.\n Try to update your video card and directx drivers!");
        // }
        // #endif
        // sdl.num_joysticks=SDL_NumJoysticks();

        /* Parse configuration files */
        String configFile = "", configPath = "";
        boolean parsedAnyConfigFile = false;
        // First Parse -userconf
        if (DOSBox.Control.CmdLine.findExist("-userconf", true)) {
            configFile = "";
            configPath = Cross.getPlatformConfigDir();
            configFile = Cross.getPlatformConfigName();
            String confFullPath = Paths.get(configPath, configFile).toString();
            if (DOSBox.Control.parseConfigFile(confFullPath))
                parsedAnyConfigFile = true;
            if (!parsedAnyConfigFile) {
                // Try to create the userlevel configfile.
                configFile = "";
                Cross.createPlatformConfigDir(configPath);
                configFile = Cross.getPlatformConfigName();
                confFullPath = Paths.get(configPath, configFile).toString();
                if (DOSBox.Control.printConfig(confFullPath)) {
                    Log.logMsg("CONFIG: Generating default configuration.\nWriting it to %s",
                            configPath);
                    // Load them as well. Makes relative paths much easier
                    if (DOSBox.Control.parseConfigFile(confFullPath))
                        parsedAnyConfigFile = true;
                }
            }
        }

        // Second parse -conf entries
        while (DOSBox.Control.CmdLine.findString("-conf", true)) {
            configFile = DOSBox.Control.CmdLine.returnedString;
            if (DOSBox.Control.parseConfigFile(configFile))
                parsedAnyConfigFile = true;
        }
        // if none found => parse localdir conf
        configFile = "dosbox.conf";
        if (!parsedAnyConfigFile && DOSBox.Control.parseConfigFile(configFile))
            parsedAnyConfigFile = true;

        // if none found => parse userlevel conf
        if (!parsedAnyConfigFile) {
            configFile = "";
            configPath = Cross.getPlatformConfigDir();
            configFile = Cross.getPlatformConfigName();
            String confFullPath = Paths.get(configPath, configFile).toString();
            if (DOSBox.Control.parseConfigFile(confFullPath))
                parsedAnyConfigFile = true;
        }

        if (!parsedAnyConfigFile) {
            // Try to create the userlevel configfile.
            configFile = "";
            Cross.createPlatformConfigDir(configPath);
            configFile = Cross.getPlatformConfigName();
            String confFullPath = Paths.get(configPath, configFile).toString();
            if (DOSBox.Control.printConfig(confFullPath)) {
                Log.logMsg("CONFIG: Generating default configuration.\nWriting it to %s",
                        confFullPath);
                // Load them as well. Makes relative paths much easier
                DOSBox.Control.parseConfigFile(confFullPath);
            } else {
                Log.logMsg("CONFIG: Using default settings. Create a configfile to change them");
            }
        }

        // UI_Init();
        // if (control.cmdline.FindExist("-startui")) UI_Run(false);
        /* Init all the sections */
        DOSBox.Control.init();
        /* Some extra SDL Functions */
        SectionProperty guiSec = (SectionProperty) DOSBox.Control.getSection("sdl");

        if (DOSBox.Control.CmdLine.findExist("-fullscreen") || guiSec.getBool("fullscreen")) {
            // if(!sdl.desktop.fullscreen) { //only switch if not allready in fullscreen
            // GUIFlatform.Video.GFX_SwitchFullScreen();
            // }
        }

        /* Init the keyMapper */
        GUIPlatform.mapper.init();
        if (DOSBox.Control.CmdLine.findExist("-startmapper"))
            GUIPlatform.mapper.runInternal();


    }

    public void start() {
        try {
            setup();
            /* Start up main machine */
            DOSBox.Control.startup();
            /* Shutdown everything */
        } catch (Exception ex) {
            // System.Windows.MessageBox.Show(ex.Message);
            StringWriter exWrt = new StringWriter();
            ex.printStackTrace(new PrintWriter(exWrt));
            System.out.println(exWrt.toString());
        }
        // catch (char * error) {
        // GFX_ShowMsg("Exit to error: %s",error);
        // fflush(NULL);
        // if(sdl.wait_on_error) {
        // //TODO Maybe look for some way to show message in linux?

        // }

        // }
        // catch (int){
        // ;//nothing pressed killswitch
        // }
        // catch(...){
        // //Force visible mouse to end user. Somehow this sometimes doesn't happen
        // SDL_WM_GrabInput(SDL_GRAB_OFF);
        // SDL_ShowCursor(SDL_ENABLE);
        // throw;//dunno what happened. rethrow for sdl to catch
        // }
        // GUIFlatform.Shutdown();
    }
}
