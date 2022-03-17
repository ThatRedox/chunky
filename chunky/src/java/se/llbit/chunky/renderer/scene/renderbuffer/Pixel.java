package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.math.Vector3;

public class Pixel {
    public final Vector3 color;
    public final int samples;
    public final int bufferX;
    public final int bufferY;

    public Pixel(Vector3 color, int samples, int bufferX, int bufferY) {
        this.color = color;
        this.samples = samples;
        this.bufferX = bufferX;
        this.bufferY = bufferY;
    }

    public static Pixel fromTile(RenderTile tile, int bufferX, int bufferY) {
        Vector3 color = new Vector3();
        int samples = tile.getColor(bufferX, bufferY, color);
        return new Pixel(color, samples, bufferX, bufferY);
    }
}
