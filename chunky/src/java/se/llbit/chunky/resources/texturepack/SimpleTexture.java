/* Copyright (c) 2013-2015 Jesper Öqvist <jesper@llbit.se>
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
package se.llbit.chunky.resources.texturepack;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import se.llbit.chunky.resources.BitmapImage;
import se.llbit.chunky.resources.Texture;
import se.llbit.chunky.resources.pbr.LabPbrSpecularMap;
import se.llbit.chunky.resources.pbr.MetalnessMap;
import se.llbit.chunky.resources.pbr.NormalMap;
import se.llbit.chunky.resources.pbr.OldPbrSpecularMap;
import se.llbit.chunky.resources.pbr.EmissionMap;
import se.llbit.chunky.resources.pbr.ReflectanceMap;
import se.llbit.chunky.resources.pbr.RoughnessMap;
import se.llbit.resources.ImageLoader;

/**
 * Non-animated texture loader.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class SimpleTexture extends TextureLoader {

  public final String file;
  protected Texture texture;

  public SimpleTexture(String file, Texture texture) {
    this.file = file;
    this.texture = texture;
  }

  @Override
  protected boolean load(InputStream imageStream) throws IOException {
    texture.setTexture(getTextureOrFirstFrame(imageStream));
    return true;
  }

  private static BitmapImage getTextureOrFirstFrame(InputStream imageStream) throws IOException {
    BitmapImage image = ImageLoader.read(imageStream);

    if (image.height > image.width) {
      // Assuming this is an animated texture.
      // Just grab the first frame.
      int frameW = image.width;

      BitmapImage frame0 = new BitmapImage(frameW, frameW);
      for (int y = 0; y < frameW; ++y) {
        for (int x = 0; x < frameW; ++x) {
          frame0.setPixel(x, y, image.getPixel(x, y));
        }
      }
      return frame0;
    } else {
      return image;
    }
  }

  @Override
  public boolean load(ZipFile texturePack, String topLevelDir) {
    boolean loaded = load(topLevelDir + file, texturePack);

    String specularFormat = System.getProperty("chunky.pbr.specular", "labpbr");
    if (specularFormat.equals("oldpbr") || specularFormat.equals("labpbr")) {
      try (InputStream in = texturePack.getInputStream(new ZipEntry(file + "_s.png"))) {
        if (in != null) {
          if (specularFormat.equals("oldpbr")) {
            OldPbrSpecularMap specular = new OldPbrSpecularMap(getTextureOrFirstFrame(in));
            texture.setEmissionMap(specular.hasEmission() ? specular : EmissionMap.EMPTY);
            texture.setReflectanceMap(specular.hasReflectance() ? specular : ReflectanceMap.EMPTY);
            texture.setRoughnessMap(specular.hasRoughness() ? specular : RoughnessMap.EMPTY);
            texture.setMetalnessMap(MetalnessMap.EMPTY);
          } else if (specularFormat.equals("labpbr")) {
            LabPbrSpecularMap specular = new LabPbrSpecularMap(getTextureOrFirstFrame(in));
            texture.setEmissionMap(specular.hasEmission() ? specular : EmissionMap.EMPTY);
            texture.setReflectanceMap(specular.hasReflectance() ? specular : ReflectanceMap.EMPTY);
            texture.setRoughnessMap(specular.hasRoughness() ? specular : RoughnessMap.EMPTY);
            texture.setMetalnessMap(specular.hasMetalness() ? specular : MetalnessMap.EMPTY);
          }
        } else {
          texture.setEmissionMap(EmissionMap.EMPTY);
          texture.setReflectanceMap(ReflectanceMap.EMPTY);
          texture.setRoughnessMap(RoughnessMap.EMPTY);
          texture.setMetalnessMap(MetalnessMap.EMPTY);
        }
      } catch (IOException e) {
        // Safe to ignore
      }
    }
    if (System.getProperty("chunky.pbr.normal", "false").equals("true")) {
      try (InputStream in = texturePack.getInputStream(new ZipEntry(file + "_n.png"))) {
        if (in != null) {
          texture.setNormalMap(new NormalMap(getTextureOrFirstFrame(in)));
        } else {
          texture.setNormalMap(null);
        }
      } catch (IOException e) {
        // Safe to ignore
      }
    }

    return loaded;
  }

  @Override
  public String toString() {
    return "texture:" + file;
  }

  @Override
  public void reset() {
    texture.reset();
  }
}
