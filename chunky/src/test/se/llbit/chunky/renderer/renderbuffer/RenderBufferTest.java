package se.llbit.chunky.renderer.renderbuffer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import se.llbit.chunky.renderer.scene.renderbuffer.DoubleArrayRenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.math.Vector3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

@RunWith(Parameterized.class)
public class RenderBufferTest {
    public RenderBuffer.Factory factory;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { (RenderBuffer.Factory) DoubleArrayRenderBuffer::new }
        });
    }

    public RenderBufferTest(RenderBuffer.Factory factory) {
        this.factory = factory;
    }

    @Test
    public void testWriteReadback() throws Exception {
        Random rand = new Random(0);
        RenderBuffer buffer = factory.create(512, 512);
        // Whole buffer in 1 tile
        RenderTile tile = buffer.getTile(0, 0, 512, 512).get();
        // Write random data
        for (int x = 0; x < tile.getTileWidth(); x++) {
            for (int y = 0; y < tile.getTileHeight(); y++) {
                tile.setPixel(tile.getBufferX(x), tile.getBufferY(y), rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextInt());
            }
        }

        tile.commit();

        // Read back in tiles
        rand = new Random(0);
        Vector3 color = new Vector3();
        for (int i = 0; i < 512; i += 128) {
            tile = buffer.getTile(i, 0, 128, 512).get();
            for (int x = 0; x < tile.getTileWidth(); x++) {
                for (int y = 0; y < tile.getTileHeight(); y++) {
                    int spp = tile.getColor(tile.getBufferX(x), tile.getBufferY(y), color);
                    Assert.assertEquals(rand.nextDouble(), color.x, 0);
                    Assert.assertEquals(rand.nextDouble(), color.y, 0);
                    Assert.assertEquals(rand.nextDouble(), color.z, 0);
                    Assert.assertEquals(rand.nextInt(), spp);
                }
            }
        }
    }

    @Test
    public void testGetters() throws Exception {
        RenderBuffer buffer = factory.create(128, 256);
        RenderTile tile = buffer.getTile(32, 64, 4, 8).get();

        Assert.assertEquals(4, tile.getTileWidth());
        Assert.assertEquals(8, tile.getTileHeight());
        Assert.assertEquals(32, tile.getBufferX(0));
        Assert.assertEquals(64, tile.getBufferY(0));
        Assert.assertEquals(34, tile.getBufferX(2));
        Assert.assertEquals(65, tile.getBufferY(1));
        Assert.assertEquals(128, tile.getBufferWidth());
        Assert.assertEquals(256, tile.getBufferHeight());
    }
}
