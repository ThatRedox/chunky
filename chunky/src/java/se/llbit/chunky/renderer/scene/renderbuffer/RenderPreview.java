package se.llbit.chunky.renderer.scene.renderbuffer;

public interface RenderPreview {
    /**
     * Get the real width of the render preview.
     */
    int getWidth();

    /**
     * Get the real height of the render preview.
     */
    int getHeight();

    /**
     * Get the double array backing the render preview.
     */
    double[] getPreview();
}
