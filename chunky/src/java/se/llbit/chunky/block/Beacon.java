package se.llbit.chunky.block;

import se.llbit.chunky.entity.BeaconBeam;
import se.llbit.chunky.entity.Entity;
import se.llbit.chunky.model.BeaconModel;
import se.llbit.chunky.model.BlockModel;
import se.llbit.chunky.resources.Texture;
import se.llbit.math.Vector3;
import se.llbit.nbt.CompoundTag;

public class Beacon extends AbstractModelBlock {

  public Beacon() {
    super("beacon", Texture.beacon);
    localIntersect = true;
    solid = false;
    this.model = new BeaconModel();
  }

  @Override
  public boolean isBlockWithEntity() {
    return true;
  }

  @Override
  public boolean isBlockEntity() {
    return true;
  }

  @Override
  public Entity toBlockEntity(Vector3 position, CompoundTag entityTag) {
    if (entityTag.get("Levels").intValue(0) > 0) {
      return new BeaconBeam(position);
    }
    return null;
  }
}
