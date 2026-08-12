package serverutils.events.chunks;

import serverutils.events.ServerUtilitiesEvent;

public class PreGenPositionPolledEvent extends ServerUtilitiesEvent {

    private final int dimension;

    public PreGenPositionPolledEvent(int dimension) {
        this.dimension = dimension;
    }

    public int getDimension() {
        return dimension;
    }
}
