package org.gutkyu.dosboxj.hardware.sound;

import java.util.Arrays;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

import org.gutkyu.dosboxj.dos.software.Program;
import org.gutkyu.dosboxj.dos.software.Programs;
import org.gutkyu.dosboxj.hardware.Timer;
import org.gutkyu.dosboxj.misc.setup.Section;
import org.gutkyu.dosboxj.misc.setup.SectionProperty;
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
    public void makeVolume(String scan, float vol0, float vol1) {
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
            listMidi();
            return;
        }
        if (cmd.findString("MASTER", false)) {
            tempLine = cmd.returnedString;
            makeVolume(tempLine, mixer.mastervol[0], mixer.mastervol[1]);
        }
        MixerChannel chan = mixer.channels;
        while (chan != null) {
            if (cmd.findString(chan.name, false)) {
                tempLine = cmd.returnedString;
                makeVolume(tempLine, chan.volMain[0], chan.volMain[1]);
            }
            chan.UpdateVolume();
            chan = chan.next;
        }
        if (cmd.findExist("/NOSHOW"))
            return;
        chan = mixer.channels;
        writeOut("Channel  Main    Main(dB)\n");
        showVolume("MASTER", mixer.mastervol[0], mixer.mastervol[1]);
        for (chan = mixer.channels; chan != null; chan = chan.next)
            showVolume(chan.name, chan.volMain[0], chan.volMain[1]);
    }

    private void showVolume(String name, float vol0, float vol1) {
        writeOut("%-8s %3.0f:%-3.0f  %+3.2f:%-+3.2f \n", name, vol0 * 100, vol1 * 100,
                20 * Math.log(vol0) / Math.log(10.0f), 20 * Math.log(vol1) / Math.log(10.0f));
    }

    private void listMidi() {
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

        SectionProperty section = (SectionProperty) sec;
        /* Read out config section */
        MixerCore mixer = MixerCore.instance();
        IAudioSystem audioSys = JavaAudio.instance();

        mixer.freq = section.getInt("rate");
        mixer.nosound = section.getBool("nosound");
        mixer.blockSize = section.getInt("blocksize");

        /* Initialize the internal stuff */
        mixer.channels = null;
        mixer.pos = 0;
        mixer.done = 0;
        for (int[] el : mixer.work) {
            Arrays.fill(el, 0);
        }
        mixer.mastervol[0] = 1.0f;
        mixer.mastervol[1] = 1.0f;

        AudioSpecs actualSpecs = new AudioSpecs();

        mixer.tickRemain = 0;
        if (mixer.nosound) {
            Log.logMsg("MIXER:No Sound Mode Selected.");
            mixer.tickAdd = ((mixer.freq) << MixerCore.MIXER_SHIFT) / 1000;
            Timer.addTickHandler(mixer::mixNoSound);
        } else {
            try {
                audioSys.open(mixer::callback, actualSpecs);
                if ((mixer.freq != actualSpecs.frequency) || (mixer.blockSize != actualSpecs.sampleFrames))
                    Log.logMsg("MIXER:Got different values from SDL: freq %d, blocksize %d", actualSpecs.frequency,
                            actualSpecs.sampleFrames);
                mixer.freq = actualSpecs.frequency;
                mixer.blockSize = actualSpecs.sampleFrames;
                mixer.tickAdd = (mixer.freq << MixerCore.MIXER_SHIFT) / 1000;
                Timer.addTickHandler(mixer::mix);
                audioSys.start();
            } catch (Exception e) {
                mixer.nosound = true;
                Log.logMsg("MIXER:Can't open audio: %s , running in nosound mode.", e.getMessage());
                mixer.tickAdd = ((mixer.freq) << MixerCore.MIXER_SHIFT) / 1000;
                Timer.addTickHandler(mixer::mixNoSound);
            }
        }

        mixer.minNeeded = section.getInt("prebuffer");
        if (mixer.minNeeded > 100)
            mixer.minNeeded = 100;
        mixer.minNeeded = (mixer.freq * mixer.minNeeded) / 1000;
        mixer.maxNeeded = mixer.blockSize * 2 + 2 * mixer.minNeeded;
        mixer.needed = mixer.minNeeded + 1;
        Programs.makeFile("MIXER.COM", Mixer::makeProgram);
    }
}
