package org.gutkyu.dosboxj.hardware.sound;

import java.util.Arrays;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.sampled.AudioSystem;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.dos.software.*;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.Log;

public class Mixer extends Program {
    final private MixerCore mixer = MixerCore.instance();

    private Mixer() {
        super();
    }

    public float returnedMakeVoluemVol0, returnedMakeVoluemVol1;

    // TODO: 검증 필요, strtod()함수의 java 구현을 진행하지 않고
    // "D ### : ###" or "D ###" or "###" format scan input을 전제로 구현했음
    // 만약, "D #### a"와 같이 숫자 앞뒤에 'D' 와 ':' 가 아닌 문자열이 붙으면 오류발생
    public void MakeVolume(String scan, float vol0, float vol1) {
        returnedMakeVoluemVol0 = vol0;
        returnedMakeVoluemVol1 = vol1;
        int w = 0;// u32
        int scanPos = 0;
        boolean db = Character.toUpperCase(scan.charAt(scanPos)) == 'D';
        if (db)
            scanPos++;
        String[] toks = scan.split(":");
        for (String tok : toks) {
            float val = 0.0f;
            try {
                val = (float) Double.parseDouble(tok);
            } catch (Exception e) {
                continue;
            }
            if (!db)
                val /= 100;
            else
                val = (float) Math.pow(10.0, val / 20.0);
            if (val < 0)
                val = 1.0f;
            if (w == 0) {
                returnedMakeVoluemVol0 = val;
            } else {
                returnedMakeVoluemVol1 = val;
            }
            w++;
        }

        if (w == 0)
            returnedMakeVoluemVol1 = returnedMakeVoluemVol0;
    }

    @Override
    public void run() {
        if (cmd.findExist("/LISTMIDI")) {
            ListMidi();
            return;
        }
        if (cmd.findString("MASTER", false)) {
            tempLine = cmd.returnedString;
            MakeVolume(tempLine, mixer.mastervol[0], mixer.mastervol[1]);
        }
        MixerChannel chan = mixer.channels;
        while (chan != null) {
            if (cmd.findString(chan.name, false)) {
                tempLine = cmd.returnedString;
                MakeVolume(tempLine, chan.volmain[0], chan.volmain[1]);
            }
            chan.UpdateVolume();
            chan = chan.next;
        }
        if (cmd.findExist("/NOSHOW"))
            return;
        chan = mixer.channels;
        writeOut("Channel  Main    Main(dB)\n");
        ShowVolume("MASTER", mixer.mastervol[0], mixer.mastervol[1]);
        for (chan = mixer.channels; chan != null; chan = chan.next)
            ShowVolume(chan.name, chan.volmain[0], chan.volmain[1]);
    }

    private void ShowVolume(String name, float vol0, float vol1) {
        writeOut("%-8s %3.0f:%-3.0f  %+3.2f:%-+3.2f \n", name, vol0 * 100, vol1 * 100,
                20 * Math.log(vol0) / Math.log(10.0f), 20 * Math.log(vol1) / Math.log(10.0f));
    }

    private void ListMidi() {
        int i = 0;
        for (MidiDevice.Info devInfo : MidiSystem.getMidiDeviceInfo()) {
            writeOut("%2d\t \"%s\"\n", i++, devInfo.getName());
        }
        return;
    }


    private static void stop(Section sec) {
    }

    private static Program makeProgram() {
        return new Mixer();
    }

    public static final void init(Section sec) {
        sec.addDestroyFunction(Mixer::stop);
    
        SectionProperty  section=(SectionProperty)sec;
        /* Read out config section */
    MixerCore mixer = MixerCore.instance();
IAudioSystem audioSys = JavaAudio.instance();

        mixer.freq=section.getInt("rate");
        mixer.nosound=section.getBool("nosound");
        mixer.blocksize=section.getInt("blocksize");
    
        /* Initialize the internal stuff */
        mixer.channels=null;
        mixer.pos=0;
        mixer.done=0;
        for (int[] el : mixer.work) {
            Arrays.fill(el, 0);
        }
        mixer.mastervol[0]=1.0f;
        mixer.mastervol[1]=1.0f;
    
        /* Start the Mixer using SDL Sound at 22 khz */
        SDL_AudioSpec spec;
        SDL_AudioSpec obtained;
    
        spec.freq=mixer.freq;
        spec.format=AUDIO_S16SYS;
        spec.channels=2;
        spec.callback=MIXER_CallBack;
        spec.samples=(Uint16)mixer.blocksize;
    
        mixer.tick_remain=0;
        if (mixer.nosound) {
            Log.logMsg("MIXER:No Sound Mode Selected.");
            mixer.tick_add=((mixer.freq) << MixerCore. MIXER_SHIFT)/1000;
            Timer.addTickHandler(mixer::mixNoSound);
        } else if (SDL_OpenAudio(&spec, &obtained) <0 ) {
            mixer.nosound = true;
            Log.logMsg("MIXER:Can't open audio: %s , running in nosound mode.",SDL_GetError());
            mixer.tick_add=((mixer.freq) << MixerCore.MIXER_SHIFT)/1000;
            Timer.addTickHandler(mixer::mixNoSound);
        } else {
            if((mixer.freq != obtained.freq) || (mixer.blocksize != obtained.samples))
                LOG_MSG("MIXER:Got different values from SDL: freq %d, blocksize %d",obtained.freq,obtained.samples);
            mixer.freq=obtained.freq;
            mixer.blocksize=obtained.samples;
            mixer.tick_add=(mixer.freq << MixerCore.MIXER_SHIFT)/1000;
            Timer.addTickHandler( mixer::mix);
            audioSys.pause(0);
        }
        mixer.min_needed=section->Get_int("prebuffer");
        if (mixer.min_needed>100) mixer.min_needed=100;
        mixer.min_needed=(mixer.freq*mixer.min_needed)/1000;
        mixer.max_needed=mixer.blocksize * 2 + 2*mixer.min_needed;
        mixer.needed=mixer.min_needed+1;
        Programs.makeFile("MIXER.COM",Mixer::makeProgram);
    }
}
