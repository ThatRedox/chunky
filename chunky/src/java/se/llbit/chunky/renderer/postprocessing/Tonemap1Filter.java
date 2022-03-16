package se.llbit.chunky.renderer.postprocessing;

import se.llbit.math.QuickMath;

/**
 * Implementation of the tone mapping operator from Jim Hejl and Richard Burgess-Dawson
 * @link http://filmicworlds.com/blog/filmic-tonemapping-operators/
 */
public class Tonemap1Filter extends IntensityScalingPostProcessingFilter {
  @Override
  public double process(double intensity) {
    intensity = QuickMath.max(0, intensity - 0.004);
    intensity = (intensity * (6.2 * intensity + .5)) / (intensity * (6.2 * intensity + 1.7) + 0.06);
    return intensity;
  }

  @Override
  public String getName() {
    return "Tonemap operator 1";
  }

  @Override
  public String getDescription() {
    return "Tonemap operator 1";
  }

  @Override
  public String getId() {
    return "TONEMAP1";
  }
}
