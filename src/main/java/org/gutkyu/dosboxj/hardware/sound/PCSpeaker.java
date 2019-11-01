package org.gutkyu.dosboxj.hardware.sound;

import org.gutkyu.dosboxj.hardware.Timer;
import org.gutkyu.dosboxj.misc.setup.ModuleBase;
import org.gutkyu.dosboxj.misc.setup.Section;
import org.gutkyu.dosboxj.misc.setup.SectionProperty;

public final class PCSpeaker extends ModuleBase {
    private MixerObject MixerChan;
    private PCSpeakerCore pcSpk = PCSpeakerCore.instance();

    public PCSpeaker(Section configuration) {
        super(configuration);
        pcSpk.chan = null;
        SectionProperty section = (SectionProperty) configuration;
        if (!section.getBool("pcspeaker"))
            return;
        pcSpk.mode = PCSpeakerCore.SPKR_MODES.SPKR_OFF;
        pcSpk.lastTicks = 0;
        pcSpk.lastIndex = 0;
        pcSpk.rate = section.getInt("pcrate");
        pcSpk.pitMax = (1000.0f / Timer.PIT_TICK_RATE) * 65535;
        pcSpk.pitHalf = pcSpk.pitMax / 2;
        pcSpk.pitNewMax = pcSpk.pitMax;
        pcSpk.pitNewHalf = pcSpk.pitHalf;
        pcSpk.pitIndex = 0;
        pcSpk.minTr = (Timer.PIT_TICK_RATE + pcSpk.rate / 2 - 1) / (pcSpk.rate / 2);
        pcSpk.used = 0;
        /* Register the sound channel */
        MixerChan = new MixerObject();
        pcSpk.chan = MixerChan.install(pcSpk::callback, pcSpk.rate, "SPKR");
    }

    private static PCSpeaker obj;

    public static void shutdown(Section sec) {
        obj = null;
    }

    public static void init(Section sec) {
        obj = new PCSpeaker(sec);
        sec.addDestroyFunction(PCSpeaker::shutdown, true);
    }
}
