package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160106134523_AddSeatToTrip extends Migration {

  val table = "trip"; //put your table name here

  def up() {
    addColumn(table, "seats", IntegerType, Default(1))

  }

  def down() {
    //dropTable(table)
  }
}
