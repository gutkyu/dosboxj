package org.gutkyu.dosboxj.hardware.sound;

import org.gutkyu.dosboxj.misc.Support;

class MixerObject {
    private boolean installed = false;
    private String m_name = "";

    public MixerChannel install(MixerHandler handler, int freq, String name) {
        if (!installed) {
            if (name.length() > 31)
                Support.exceptionExit("Too long mixer channel name");
            m_name = name;
            installed = true;
            return MixerCore.instance().addChannel(handler, freq, name);
        } else {
            Support.exceptionExit("All ready added mixer channel.");
            return null;
        }
    }

    public void dispose() {
        if (!installed)
            return;
        MixerCore.instance().delChannel(MixerCore.instance().findChannel(m_name));
    }
}
