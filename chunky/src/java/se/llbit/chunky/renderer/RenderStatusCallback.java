package se.llbit.chunky.renderer;

public interface RenderStatusCallback {
    /**
     * Give a render status update.
     *
     * @param complete  Render units complete.
     * @param total     Total render units.
     * @return True if the render should terminate.
     */
    boolean update(long complete, long total);
}
