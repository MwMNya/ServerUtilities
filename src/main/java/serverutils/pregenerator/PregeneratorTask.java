package serverutils.pregenerator;

import serverutils.lib.math.TeleporterDimPos;

public class PregeneratorTask {

    public final int dimension;

    private final int centerX;
    private final int centerZ;
    private final int radius;

    private int x;
    private int z;

    private final TeleporterDimPos teleporterDimPos;

    public PregeneratorTask(int dimension, int centerX, int centerZ, int radius, TeleporterDimPos teleporterDimPos) {

        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;

        this.x = -radius;
        this.z = -radius;

        this.teleporterDimPos = teleporterDimPos;
    }

    public int nextX() {
        return centerX + x;
    }

    public int nextZ() {
        return centerZ + z;
    }

    public void advance() {

        z++;

        if (z > radius) {
            z = -radius;
            x++;
        }

    }

    public TeleporterDimPos getPos() {
        return teleporterDimPos;
    }

    public boolean finished() {
        return x > radius;
    }

}
