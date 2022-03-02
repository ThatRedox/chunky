/* Copyright (c) 2021 Chunky contributors
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.llbit.chunky.renderer.renderdump;

import org.junit.Before;
import org.junit.Test;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.math.Vector3;
import se.llbit.util.ProgressListener;
import se.llbit.util.TaskTracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RenderDumpTest {
  protected static final int testWidth = Scene.MIN_CANVAS_WIDTH;
  protected static final int testHeight = Scene.MIN_CANVAS_HEIGHT;
  protected static final int testSPP = 100;
  protected static final long testRenderTime = 654321L;

  protected static final double[][][] humanReadable1QuarterRGBSamples = {
      {{1, 1, 1}, {0, 0, 0}, {1, 1, 1}, {1, 0, 0}, {0, 1, 0}},
      {{0, 0, 0}, {1, 1, 1}, {0, 0, 0}, {0, 1, 0}, {0, 0, 1}},
      {{1, 1, 1}, {0, 0, 0}, {1, 1, 1}, {0, 0, 1}, {1, 0, 0}},
      {{1, 1, 0}, {1, 0, 1}, {0, 1, 1}, {0, 0.25, 0.5}, {0.125, 0.1, 0.5}},
      {{2, 0.01, 0}, {2, 0, 0.01}, {0, 0.01, 2}, {0.5, 0.1, 0.25}, {0.5, 0.25, 0}}
  };
  protected static final double[] testSampleBuffer;

  static {
    int rgbSamplesHeight = humanReadable1QuarterRGBSamples.length;
    int rgbSamplesWidth = humanReadable1QuarterRGBSamples[0].length;
    testSampleBuffer = new double[testWidth * testHeight * 3];
    int index;
    for (int y = 0; y < testWidth; y++) {
      for (int x = 0; x < testHeight; x++) {
        double[] rgb = humanReadable1QuarterRGBSamples[y % rgbSamplesHeight][x % rgbSamplesWidth];
        index = (y * testWidth + x) * 3;
        System.arraycopy(rgb, 0, testSampleBuffer, index, 3);
      }
    }
  }

  protected TaskTracker taskTracker;

  @Before
  public void init() {
    taskTracker = new TaskTracker(new ProgressListener() {
      final Map<String, Integer> previousProgress = new HashMap<>();

      @Override
      public void setProgress(String task, int done, int start, int target) {
        int previous = previousProgress.getOrDefault(task, Integer.MIN_VALUE);
        // check that progress is monotonically increasing
        assertTrue("progress (" + done + ") should be greater or equal to previous progress (" + previous + ")", done >= previous);
        previousProgress.put(task, done);
      }
    });
  }

  protected Scene createTestScene(long renderTime) {
    Scene scene = new Scene();
    scene.setCanvasSize(testWidth, testWidth);
    scene.renderTime = renderTime;
    return scene;
  }

  private static byte[] getTestDump(String dumpName) {
    return Base64.getDecoder().decode(
      testDumps.get(dumpName)
    );
  }

  @Test
  public void testLoadClassicFormatDump() throws Exception {
    testLoadDump("classicFormatDump");
  }

  @Test
  public void testLoadCompressedFloatFormatDump() throws Exception {
    testLoadDump("compressedFloatFormatDump");
  }

  private void testLoadDump(String dumpName) throws Exception {
    Scene scene = createTestScene(0);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(getTestDump(dumpName));
    RenderDump.load(inputStream, scene, taskTracker);
    assertArrayEquals(testSampleBuffer, scene.getSampleBuffer(), 0.0);
    assertEquals(testRenderTime, scene.renderTime);

    RenderTile tile = scene.getRenderBuffer().getTile(0, 0, testWidth, testHeight).get();
    for (int x = 0; x < testWidth; x++) {
      for (int y = 0; y < testHeight; y++) {
        assertEquals(testSPP, tile.getColor(x, y, null));
      }
    }
  }

  @Test
  public void testMergeClassicFormatDump() throws Exception {
    testMergeDump("classicFormatDump");
  }

  @Test
  public void testMergeCompressedFloatFormatDump() throws Exception {
    testMergeDump("compressedFloatFormatDump");
  }

  public void testMergeDump(String dumpName) throws Exception {
    long renderTime = 123456L;
    double[][] preMergeSamples = {
        {0.5, 1.0, 2.0},
        {0.5, 1.0, 2.0},
        {2.0, 1.5, 2.5}
    };
    int[] preMergeSpp = {100, 100, 100};
    double[][] postMergeSamples = {
        {0.75, 1.0, 1.5},
        {0.25, 0.5, 1.0},
        {1.5, 1.25, 1.75}
    };

    Vector3 color = new Vector3();
    Scene scene = createTestScene(renderTime);
    RenderBuffer buffer = scene.getRenderBuffer();
    RenderTile tile = buffer.getTile(0, 0, testWidth, testHeight).get();
    for (int i = 0; i < preMergeSamples.length; i++) {
      tile.setPixel(i, 0, preMergeSamples[i][0], preMergeSamples[i][1], preMergeSamples[i][2], preMergeSpp[i]);
    }
    tile.commit();

    ByteArrayInputStream inputStream = new ByteArrayInputStream(getTestDump(dumpName));
    RenderDump.merge(inputStream, scene, taskTracker);

    tile = buffer.getTile(0, 0, testWidth, testHeight).get();
    for (int i = 0; i < preMergeSamples.length; i++) {
      assertEquals(preMergeSpp[i] + testSPP, tile.getColor(i, 0, color));
      assertEquals(color.x, postMergeSamples[i][0], 0);
      assertEquals(color.y, postMergeSamples[i][1], 0);
      assertEquals(color.z, postMergeSamples[i][2], 0);
    }
    for (int x = 0; x < testWidth; x++) {
      for (int y = 0; y < testHeight; y++) {
        if (y != 0 || x >= preMergeSamples.length) {
          assertEquals(testSPP, tile.getColor(x, y, null));
        }
      }
    }

    assertEquals(renderTime + testRenderTime, scene.renderTime);
  }

  /**
   * it is currently not expected to write the old format (but it would be possible)
   */
  @Test
  public void testSaveCompressedFloatFormatDump() throws Exception {
    testSaveDump("compressedFloatFormatDump", 1);
  }

  public void testSaveDump(String dumpName, int version) throws Exception {
    Scene scene = createTestScene(testRenderTime);
    scene.getRenderBuffer().getTile(0, 0, 1, 1).get().setPixel(0, 0, 0, 0, 0, 100);

    System.arraycopy(testSampleBuffer, 0, scene.getSampleBuffer(), 0, testSampleBuffer.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    RenderDump.save(outputStream, scene, taskTracker, version);
    assertArrayEquals(getTestDump(dumpName), outputStream.toByteArray());
  }

  // This is just at the bottom because the strings are soooo lllooooonnnnngggggg
  private static final Map<String, String> testDumps = new HashMap<String, String>() {{

    final String CLASSIC_TEST_DUMP_STRING = "H4sIAAAAAAAAAO3Wuw0CMRAEUKfk14+d0QhVQEoTUAQ5GSVcCUcHEJOg4xeMsNYr+fzRrCXks3a9L0HyOOcG9/5t3LxWj7u/vb4c7rEV64/dD997++11fRp20hxz87ra+VI/ngP2Cb65ed1c/6tYHb0Q6TN3WVdyUut+/OwT7OfDvI6/urlFXO38WJ+/wHx0xz+XzF3cZcsbbK52vtSP54B9pDmnllv73Te3jCs5qfXW8wabq53fe95gc9nyBpurnS/14zlgH2nOqeXWfvfNLeNKTmq99bzB5mrn95432Fy2vMHmaudL/XgO2Eeac2q5td99c8u4kpNabz1vsLna+b3nDTb3Ce81b9eUJQAA";
    final String TEST_DUMP_STRING_COMPRESSED = "RFVNUAAAAAEAAAAUAAAAFAAAAGQAAAAAAAn78Q4/8AAAAAAAAA4/8AAAAAAAAA4/8AAAAAAAAA4/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAADuDj/wAAAAAAAA7gA/8AAAAAAAAD/wAAAAAAAA7uA/8AAAAAAAAO4AP/AAAAAAAAA/8AAAAAAAAA4/8AAAAAAAAOA/8AAAAAAAAO7uDj/wAAAAAAAA4D/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAADuDj/wAAAAAAAA7gA/8AAAAAAAAD/wAAAAAAAA7uA/8AAAAAAAAO4AP/AAAAAAAAA/8AAAAAAAAA4/8AAAAAAAAA4/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAADuAD/wAAAAAAAAP/AAAAAAAADuDj/wAAAAAAAADj/wAAAAAAAA7g4/8AAAAAAAAO4AP/AAAAAAAAA/8AAAAAAAAOA/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAA7uA/8AAAAAAAAO7gP/AAAAAAAADu4D/wAAAAAAAADj/wAAAAAAAADj/wAAAAAAAADj/wAAAAAAAA7g4/8AAAAAAAAO4AP/AAAAAAAAA/8AAAAAAAAOA/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAA7g4/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAADj/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAO4OP/AAAAAAAADu7u7gP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAADj/wAAAAAAAA7u7u4D/wAAAAAAAA4D/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAAAOP/AAAAAAAAAOP/AAAAAAAADu7u7gP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAADj/wAAAAAAAA7uA/8AAAAAAAAO4OP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAAI/8AAAAAAAACAAAAAAAAAAP/AAAAAAAAA/4AAAAAAAACAwAAAAAAAAP/AAAAAAAAADP7mZmZmZmZpAAAAAAAAALhAAAAAAAAAOP/AAAAAAAADvLhAAAAAAAAAOP/AAAAAA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IAAAAAAAAD/wAAAAAAAAIBAAAAAAAAA/8AAAAAAAAO4OP/AAAAAAAADuAD/wAAAAAAAAP/AAAAAAAADu4D/wAAAAAAAA7gA/8AAAAAAAAD/wAAAAAAAADj/wAAAAAAAA4D/wAAAAAAAA7u4OP/AAAAAAAADgP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAO4OP/AAAAAAAADuAD/wAAAAAAAAP/AAAAAAAADu4D/wAAAAAAAA7gA/8AAAAAAAAD/wAAAAAAAADj/wAAAAAAAADj/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAO4AP/AAAAAAAAA/8AAAAAAAAO4OP/AAAAAAAAAOP/AAAAAAAADuDj/wAAAAAAAA7gA/8AAAAAAAAD/wAAAAAAAA4D/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAADu4D/wAAAAAAAA7uA/8AAAAAAAAO7gP/AAAAAAAAAOP/AAAAAAAAAOP/AAAAAAAAAOP/AAAAAAAADuDj/wAAAAAAAA7gA/8AAAAAAAAD/wAAAAAAAA4D/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAADuDj/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAAAOP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAA7g4/8AAAAAAAAO7u7uA/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAAAOP/AAAAAAAADu7u7gP/AAAAAAAADgP/AAAAAAAAAAP/AAAAAAAAA/8AAAAAAAAA4/8AAAAAAAAA4/8AAAAAAAAO7u7uA/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAAAD/wAAAAAAAAP/AAAAAAAAAOP/AAAAAAAADu4D/wAAAAAAAA7g4/8AAAAAAAAAA/8AAAAAAAAD/wAAAAAAAAAj/wAAAAAAAAIAAAAAAAAAA/8AAAAAAAAD/gAAAAAAAAIDAAAAAAAAA/8AAAAAAAAAI/uZmZmZmZmkmZmZmZmZouEAAAAAAAAA5/8AAAAAAAAAA/hHrhR64Uez/wAAAAAAAALhAAAAAAAAAOP/AAAAAAAAAiIAAAAAAAAGmZmZmZmZriEAAAAAAAAA4/8AAAAAAAACBJmZmZmZmaP/AAAAAAAADiEAAAAAAAAOA/8AAAAAAAAAI/8AAAAAAAACAAAAAAAADu4D/wAAAAAAAAImmZmZmZmZpJmZmZmZmaLhAAAAAAAADuAD/wAAAAAAAAP/AAAAAAAAAuEAAAAAAAAA4/8AAAAAAAACIgAAAAAAAAaZmZmZmZmuIQAAAAAAAAAEAAAAAAAAAAf/AAAAAAAAAgPeN43jeN4T/wAAAAAAAA4mR64UeuFHsAP8AAAAAAAAB/4AAAAAAAAAI/hHrhR64Uez3jeN43jeEAf/AAAAAAAAB/0AAAAAAAAAA/4AAAAAAAAH/wAAAAAAAAImmZmZmZmZpUeuFHrhR7Dj/QAAAAAAAAAH/gAAAAAAAAP+AAAAAAAAACP/AAAAAAAAA943jeN43h4EAAAAAAAAAAAH/gAAAAAAAAP+AAAAAAAAAjPeN43jeN4WmZmZmZmZoAf9AAAAAAAAA/0AAAAAAAAOB/4AAAAAAAACBUeuFHrhR7P4R64UeuFHvuAD/gAAAAAAAAf+AAAAAAAAAiPeN43jeN4T3jeN43jeEAQAAAAAAAAAB/0AAAAAAAAA4/4AAAAAAAADJpmZmZmZmaVHrhR64Uew4/0AAAAAAAAAB/4AAAAAAAAD/gAAAAAAAAAj+EeuFHrhR7PeN43jeN4eBAAAAAAAAAAAB/4AAAAAAAAD/gAAAAAAAAIz3jeN43jeFpmZmZmZmaAH/QAAAAAAAAP9AAAAAAAAA=";

    put("classicFormatDump", CLASSIC_TEST_DUMP_STRING);
    put("compressedFloatFormatDump", TEST_DUMP_STRING_COMPRESSED);
  }};
}
