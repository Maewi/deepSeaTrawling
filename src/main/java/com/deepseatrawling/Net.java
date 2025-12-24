package com.deepseatrawling;

public class Net {

    private final int netIndex;
    private NetDepth netDepth = NetDepth.RAISED;
    private final static int netSize = 125;

    public Net(int netIndex) {
        this.netIndex = netIndex;
    }

    public int getNetSize() {
        return netSize;
    }

    public void setNetDepth(int netDepth) {
        for (NetDepth newDepth : NetDepth.values())
        {
            if (newDepth.depth == netDepth) {
                this.netDepth = newDepth;
            }
        }
    }

    public NetDepth getNetDepth() {
        return netDepth;
    }
    public int getNetDepthAsInt() {
        return netDepth.depth;
    }

    public enum NetDepth {
        RAISED(0),
        SHALLOW(1),
        MEDIUM(2),
        DEEP(3);

        public static int asInt(NetDepth depth) {
            switch (depth) {
                case RAISED:
                    return 0;
                case SHALLOW:
                    return 1;
                case MEDIUM:
                    return 2;
                case DEEP:
                    return 3;
                default:
                    return -1;
            }
        }

        private final int depth;

        NetDepth(int depth) {
            this.depth = depth;
        }
    }
}
