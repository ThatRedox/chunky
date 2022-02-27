package se.llbit.chunky.renderer.renderdump;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.math.Vector3;
import se.llbit.util.IsolatedOutputStream;

import java.io.*;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.zip.*;

public class LegacyStreamDumpFormat extends AbstractLegacyDumpFormat {
    public static final LegacyStreamDumpFormat UNCOMPRESSED = new LegacyStreamDumpFormat(2,
        is -> is, os -> os,
        "Legacy Uncompressed Dump", "Legacy uncompressed dump format.", "LegacyUncompressedDumpFormat");
    public static final LegacyStreamDumpFormat HUFFMAN = new LegacyStreamDumpFormat(3,
        InflaterInputStream::new, os -> {
        Deflater deflater = new Deflater(Deflater.HUFFMAN_ONLY);
        return new DeflaterOutputStream(os, deflater);
    }, "Legacy Huffman Compressed Dump", "Legacy huffman compressed dump format.", "LegacyHuffmanDumpFormat");
    public static final LegacyStreamDumpFormat GZIP = new LegacyStreamDumpFormat(4,
        is -> {
            try {
                return new GZIPInputStream(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, os -> {
            try {
                return new GZIPOutputStream(os);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        },
        "Legacy GZIP Compressed Dump", "Legacy GZIP dump format.", "LegacyGzipDumpFormat");

    protected final int version;
    protected final String name;
    protected final String description;
    protected final String id;

    protected final Function<InputStream, InputStream> inputStreamSupplier;
    protected final Function<OutputStream, OutputStream> outputStreamSupplier;

    public LegacyStreamDumpFormat(int version,
                                  Function<InputStream, InputStream> inputStreamSupplier,
                                  Function<OutputStream, OutputStream> outputStreamSupplier,
                                  String name, String description, String id) {
        this.version = version;
        this.name = name;
        this.description = description;
        this.id = id;

        this.inputStreamSupplier = inputStreamSupplier;
        this.outputStreamSupplier = outputStreamSupplier;
    }

    @Override
    protected void readSamples(DataInputStream inputStream, Scene scene, PixelConsumer consumer, IntConsumer pixelProgress) throws IOException {
        DataInputStream in = new DataInputStream(inputStreamSupplier.apply(inputStream));
        int numPixels = scene.width * scene.height;
        for (int pixel = 0; pixel < numPixels; pixel++) {
            double r = in.readDouble();
            double g = in.readDouble();
            double b = in.readDouble();
            consumer.consume(pixel, r, g, b);
            pixelProgress.accept(pixel);
        }
    }

    @Override
    protected void writeSamples(DataOutputStream outputStream, Scene scene, Iterable<Pixel> pixels, IntConsumer pixelProgress) throws IOException {
        Vector3 color = new Vector3();
        int progress = 0;
        try (DataOutputStream out = new DataOutputStream(outputStreamSupplier.apply(new IsolatedOutputStream(outputStream)))) {
            for (Pixel pixel : pixels) {
                pixel.getColor(color);
                out.writeDouble(color.x);
                out.writeDouble(color.y);
                out.writeDouble(color.z);

                pixelProgress.accept(progress++);
            }
        }
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getId() {
        return id;
    }
}
