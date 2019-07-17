package org.gutkyu.dosboxj.gui;

public final class GFXFlag {
        public static int CAN8 = 0x0001;
        public static int CAN15 = 0x0002;
        public static int CAN16 = 0x0004;
        public static int CAN32 = 0x0008;

        public static int LOVE8 = 0x0010;
        public static int LOVE15 = 0x0020;
        public static int LOVE16 = 0x0040;
        public static int LOVE32 = 0x0080;

        public static int RGBONLY = 0x0100;

        public static int SCALING = 0x1000;
        public static int HARDWARE = 0x2000;

        public static int CAN_RANDOM = 0x4000; // If the interface can also do random access surface

        public static String toString(int flag) {
                switch (flag) {
                        case 0x0001:
                                return "CAN8";
                        case 0x0002:
                                return "CAN15";
                        case 0x0004:
                                return "CAN16";
                        case 0x0008:
                                return "CAN32";
                        case 0x0010:
                                return "LOVE8";
                        case 0x0020:
                                return "LOVE15";
                        case 0x0040:
                                return "LOVE16";
                        case 0x0080:
                                return "LOVE32";
                        case 0x0100:
                                return "RGBONLY";
                        case 0x1000:
                                return "SCALING";
                        case 0x2000:
                                return "HARDWARE";
                        default:
                                return "UNKNOWN";
                }
        }

}
