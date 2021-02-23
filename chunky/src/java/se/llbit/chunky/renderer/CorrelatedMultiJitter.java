package se.llbit.chunky.renderer;

import org.apache.commons.math3.util.FastMath;
import se.llbit.math.QuickMath;
import se.llbit.math.Ray;
import se.llbit.math.Vector2;

public class CorrelatedMultiJitter {
  private int x;
  private int y;
  private int width;
  private int height;
  private int seed;
  private int spp;
  private int maxSpp;

  public CorrelatedMultiJitter(int x, int y, int width, int height, int seed, int spp, int maxSpp) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.seed = seed;
    this.spp = spp;
    this.maxSpp = maxSpp;
  }

  public void set(int x, int y, int width, int height, int seed, int spp, int maxSpp) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.seed = seed;
    this.spp = spp;
    this.maxSpp = maxSpp;
  }

  public void setSpp(int spp) {
    this.spp = spp;
  }

  public void jitter2D(Vector2 position, int rayDepth) {
    jitter2D(position, x, y, width, height, seed, rayDepth, spp, maxSpp);
  }

  public static void jitter2D(Vector2 position, int x, int y, int width, int height, int seed, int rayDepth, int spp, int maxSpp) {
    Vector2 temp = new Vector2();
    randomVec2(temp, x, y, width, height, seed + (rayDepth+1)*maxSpp + spp);
    position.add(temp);
  }

  private static int permutate(int i, int l, int p) {
    int w = l - 1;
    w |= w >>> 1;
    w |= w >>> 2;
    w |= w >>> 4;
    w |= w >>> 8;
    w |= w >>> 16;

    do {
      i ^= p;
      i *= 0xe170893d;
      i ^= p >>> 16;
      i ^= (i & w) >>> 4;
      i ^= p >>> 8;
      i *= 0x0929eb3f;
      i ^= p >>> 23;
      i ^= (i & w) >>> 1;
      i *= 1 | p >>> 27;
      i *= 0x6935fa69;
      i ^= (i & w) >>> 11;
      i *= 0x74dcb303;
      i ^= (i & w) >>> 2;
      i *= 0x9e501cc3;
      i ^= (i & w) >>> 2;
      i *= 0xc860a3df;
      i &= w;
      i ^= i >>> 5;
    } while (Integer.compareUnsigned(i, l) >= 0);

    return Integer.remainderUnsigned((i + p), l);
  }

  private static float randfloat(int i, int p) {
    i ^= p;
    i ^= i >>> 17;
    i ^= i >>> 10;
    i *= 0xb36534e5;
    i ^= i >>> 12;
    i ^= i >>> 21;
    i *= 0x93fc4796;
    i ^= 0xdf6e307f;
    i ^= i >>> 17;
    i *= 1 | p >> 18;
    return FastMath.abs(i) * (1.0f/Integer.MAX_VALUE);
  }

  public static void randomVec2(Vector2 out, int x, int y, int width, int height, int seed) {
    int m = (int) FastMath.sqrt(width * height);
    int n = (width * height + m - 1) / m;

    int s = permutate(x + width*y, width*height, seed * 0x51633e2d);
    int sx = permutate(s%m, m, seed * 0x68bc21eb);
    int sy = permutate(s/m, n, seed * 0x02e5be93);
    float jx = randfloat(s, seed * 0x967a889b);
    float jy = randfloat(s, seed * 0x368cc8b7);
    out.set(
            QuickMath.clamp((sx + (sy + jx) / n) / m, 0, 1- Ray.EPSILON),
            QuickMath.clamp((s + jy) / (width*height), 0, 1-Ray.EPSILON)
    );
  }
}
