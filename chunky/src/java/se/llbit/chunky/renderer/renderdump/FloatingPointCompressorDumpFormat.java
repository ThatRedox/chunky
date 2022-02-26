/* Copyright (c) 2020-2021 Chunky contributors
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

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.math.Vector3;

import java.io.*;
import java.util.Iterator;
import java.util.function.IntConsumer;

/**
 * Implementation of the FPC algorithm
 * (compression algorithm for double precision floating pointer number)
 * http://cs.txstate.edu/~burtscher/papers/tr06.pdf
 */
public class FloatingPointCompressorDumpFormat extends LegacyAbstractDumpFormat {
  public static final FloatingPointCompressorDumpFormat INSTANCE = new FloatingPointCompressorDumpFormat();

  private FloatingPointCompressorDumpFormat() {}

  @Override
  public int getVersion() {
    return 1;
  }

  @Override
  public String getName() {
    return "Floating Point Compressor";
  }

  @Override
  public String getDescription() {
    return "Fast FPC compressed dump format.";
  }

  @Override
  public String getId() {
    return "FloatingPointCompressorDumpFormat";
  }

  @Override
  protected void readSamples(DataInputStream inputStream, Scene scene,
                             PixelConsumer consumer, IntConsumer pixelProgress)
      throws IOException {
    decompress(inputStream, scene.getSampleBuffer().length, consumer, pixelProgress);
  }

  @Override
  protected void writeSamples(DataOutputStream outputStream, Scene scene,
                              Iterable<Pixel> pixels, IntConsumer pixelProgress)
      throws IOException {
    Vector3 firstColor = new Vector3();
    Vector3 secondColor = new Vector3();
    Iterator<Pixel> iterator = pixels.iterator();

    int size = scene.width * scene.height - 1;

    EncoderDecoder rEncoder = new EncoderDecoder();
    EncoderDecoder gEncoder = new EncoderDecoder();
    EncoderDecoder bEncoder = new EncoderDecoder();

    for (int i = 0; i < size; i += 2) {
      iterator.next().getColor(firstColor);
      iterator.next().getColor(secondColor);

      rEncoder.encodePair(firstColor.x, secondColor.x, outputStream);
      gEncoder.encodePair(firstColor.y, secondColor.y, outputStream);
      bEncoder.encodePair(firstColor.z, secondColor.z, outputStream);

      pixelProgress.accept(i);
    }

    // Add the last one and a special terminator if there is an odd number
    if (iterator.hasNext()) {
      iterator.next().getColor(firstColor);

      rEncoder.encodeSingleWithOddTerminator(firstColor.x, outputStream);
      gEncoder.encodeSingleWithOddTerminator(firstColor.y, outputStream);
      bEncoder.encodeSingleWithOddTerminator(firstColor.z, outputStream);

      pixelProgress.accept(size);
    }
  }

  private void decompress(InputStream inputStream, int bufferLength,
                          PixelConsumer consumer, IntConsumer pixelProgress)
      throws IOException {
    assert bufferLength % 3 == 0;

    int pixels = bufferLength / 3;
    int size = pixels - 1;

    EncoderDecoder rDecoder = new EncoderDecoder();
    EncoderDecoder gDecoder = new EncoderDecoder();
    EncoderDecoder bDecoder = new EncoderDecoder();

    for (int i = 0; i < size; i += 2) {
      byte rGroupedHeader = (byte) inputStream.read();
      byte rFirstHeader = (byte) ((rGroupedHeader >>> 4) & 0x0F);
      byte rSecondHeader = (byte) (rGroupedHeader & 0x0F);
      double r1 = rDecoder.decodeSingle(rFirstHeader, inputStream);
      double r2 = rDecoder.decodeSingle(rSecondHeader, inputStream);

      byte gGroupedHeader = (byte) inputStream.read();
      byte gFirstHeader = (byte) ((gGroupedHeader >>> 4) & 0x0F);
      byte gSecondHeader = (byte) (gGroupedHeader & 0x0F);
      double g1 = gDecoder.decodeSingle(gFirstHeader, inputStream);
      double g2 = gDecoder.decodeSingle(gSecondHeader, inputStream);

      byte bGroupedHeader = (byte) inputStream.read();
      byte bFirstHeader = (byte) ((bGroupedHeader >>> 4) & 0x0F);
      byte bSecondHeader = (byte) (bGroupedHeader & 0x0F);
      double b1 = bDecoder.decodeSingle(bFirstHeader, inputStream);
      double b2 = bDecoder.decodeSingle(bSecondHeader, inputStream);

      consumer.consume(i, r1, g1, b1);
      consumer.consume(i+1, r2,  g2, b2);
      pixelProgress.accept(i);
    }

    // Add the last one and a special terminator if there is an odd number
    if (pixels % 2 == 1) {
      byte rGroupedHeader = (byte) inputStream.read();
      byte rFirstHeader = (byte) ((rGroupedHeader >>> 4) & 0x0F);
      byte rSecondHeader = (byte) (rGroupedHeader & 0x0F);
      double r = rDecoder.decodeSingle(rFirstHeader, inputStream);
      rDecoder.decodeSingle(rSecondHeader, inputStream); // discard

      byte gGroupedHeader = (byte) inputStream.read();
      byte gFirstHeader = (byte) ((gGroupedHeader >>> 4) & 0x0F);
      byte gSecondHeader = (byte) (gGroupedHeader & 0x0F);
      double g = gDecoder.decodeSingle(gFirstHeader, inputStream);
      gDecoder.decodeSingle(gSecondHeader, inputStream); // discard

      byte bGroupedHeader = (byte) inputStream.read();
      byte bFirstHeader = (byte) ((bGroupedHeader >>> 4) & 0x0F);
      byte bSecondHeader = (byte) (bGroupedHeader & 0x0F);
      double b = bDecoder.decodeSingle(bFirstHeader, inputStream);
      bDecoder.decodeSingle(bSecondHeader, inputStream); // discard

      consumer.consume(size, r, g, b);
      pixelProgress.accept(size);
    }
  }

  private static class EncoderDecoder {
    private static final int TABLE_SIZE = 1 << 10; // Must be a power of 2

    private final long[] fcm = new long[TABLE_SIZE];
    private final long[] dfcm = new long[TABLE_SIZE];
    private int fcm_hash = 0;
    private int dfcm_hash = 0;
    private long current = 0;
    private long previous = 0;
    private final byte[] smallBuffer = new byte[17]; // In the worst case, a double pair takes 17 bytes
    private int bytesWritten;

    private long predictFcm() {
      long prediction = fcm[fcm_hash];
      fcm[fcm_hash] = current;
      fcm_hash = (int) (((fcm_hash << 6) ^ (current >>> 48)) & (TABLE_SIZE - 1));
      return prediction;
    }

    private long predictDfcm() {
      long prediction = dfcm[dfcm_hash] + previous;
      dfcm[dfcm_hash] = current - previous;
      dfcm_hash = (int) (((dfcm_hash << 2) ^ ((current - previous) >>> 40)) & (TABLE_SIZE - 1));
      previous = current;
      return prediction;
    }

    private static int zeroBytes(long value) {
      return Long.numberOfLeadingZeros(value) / 8;
    }

    private byte encodeSingle(double d) {
      long bits = Double.doubleToRawLongBits(d);

      long xoredValue = predictFcm() ^ bits;
      long dfcmXoredValue = predictDfcm() ^ bits;
      current = bits;

      int zeroBytesCount = zeroBytes(xoredValue);
      int dfcmZeroBytesCount = zeroBytes(dfcmXoredValue);

      byte choiceBit = 0;

      if (dfcmZeroBytesCount > zeroBytesCount) {
        // Choose the prediction with the most leading zero bytes
        xoredValue = dfcmXoredValue;
        zeroBytesCount = dfcmZeroBytesCount;
        choiceBit = 1;
      }

      // We use 3 bits to represent numbers between 0 and 8 inclusive (ie 9 values)
      // One of them cannot be represented, and is excluded
      // According to the paper, it is best to exclude 4
      if (zeroBytesCount == 4)
        zeroBytesCount = 3;

      // Encode the number of 0 bytes to be between 0 and 7
      byte encodedZeroBytes = (byte) zeroBytesCount;
      if (encodedZeroBytes > 4)
        --encodedZeroBytes;

      byte header = (byte) ((encodedZeroBytes << 1) | choiceBit);

      for (int byteNo = 7 - zeroBytesCount; byteNo >= 0; --byteNo) {
        smallBuffer[bytesWritten] = (byte) (xoredValue >>> (byteNo * 8));
        ++bytesWritten;
      }

      return header;
    }

    public void encodePair(double first, double second, OutputStream output) throws IOException {
      bytesWritten = 1;
      byte headerFirst = encodeSingle(first);
      byte headerSecond = encodeSingle(second);
      byte header = (byte) ((headerFirst << 4) | headerSecond);
      smallBuffer[0] = header;

      output.write(smallBuffer, 0, bytesWritten);
    }

    public void encodeSingleWithOddTerminator(double val, OutputStream output) throws IOException {
      bytesWritten = 1;
      byte headerFirst = encodeSingle(val);
      byte headerSecond = 0b1100; // Pretend we have 7 zero bytes but the 8th byte is 0 as well
      smallBuffer[bytesWritten] = 0;
      ++bytesWritten;
      byte header = (byte) ((headerFirst << 4) | headerSecond);
      smallBuffer[0] = header;

      output.write(smallBuffer, 0, bytesWritten);
    }

    public double decodeSingle(byte header, InputStream input) throws IOException {
      long prediction = predictFcm();
      long dfcmPrediction = predictDfcm();

      byte choiceBit = (byte) (header & 1);
      byte encodedZeroBytes = (byte) ((header >>> 1) & 0x07);

      if (choiceBit == 1)
        prediction = dfcmPrediction;

      int zeroBytes = encodedZeroBytes;
      if (zeroBytes > 3)
        ++zeroBytes;

      long difference = 0;
      int byteToRead = 8 - zeroBytes;
      for (int i = 0; i < byteToRead; ++i) {
        difference = (difference << 8) | input.read();
      }

      long bits = prediction ^ difference;
      current = bits;

      return Double.longBitsToDouble(bits);
    }
  }
}
