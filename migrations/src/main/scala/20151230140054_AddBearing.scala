package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151230140054_AddBearing extends Migration {

  val table = "gps_location"; //put your table name here

  def up() {
    addColumn(table, "heading", DecimalType, Precision(9), Scale(5));

  }

  def down() {
    //dropTable(table)
  }
}
