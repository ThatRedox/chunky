package se.llbit.chunky.block;

import se.llbit.chunky.entity.Book;
import se.llbit.chunky.entity.Entity;
import se.llbit.chunky.model.BlockModel;
import se.llbit.chunky.model.EnchantmentTableModel;
import se.llbit.chunky.resources.Texture;
import se.llbit.math.Vector3;

public class EnchantingTable extends AbstractModelBlock {

  public EnchantingTable() {
    super("enchanting_table", Texture.enchantmentTableSide);
    solid = false;
    localIntersect = true;
    this.model = new EnchantmentTableModel();
  }

  @Override
  public boolean isEntity() {
    return true;
  }

  @Override
  public boolean isBlockWithEntity() {
    return true;
  }

  @Override
  public Entity toEntity(Vector3 position) {
    Vector3 newPosition = new Vector3(position);
    newPosition.add(0, 0.35, 0);
    Book book = new Book(
        newPosition,
        Math.PI - Math.PI / 16,
        Math.toRadians(30),
        Math.toRadians(180 - 30));
    book.setPitch(Math.toRadians(80));
    book.setYaw(Math.toRadians(45));
    return book;
  }
}
