package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151230143205_AddBearingToStop extends Migration {

  val table = "stop"; //put your table name here

  def up() {
    addColumn(table, "bearing", DecimalType, Precision(9), Scale(5));
    addColumn(table, "bearing_name", VarcharType, Limit(5))
  }

  def down() {
    //dropTable(table)
  }
}
