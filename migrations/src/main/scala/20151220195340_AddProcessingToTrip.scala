package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151220195340_AddProcessingToTrip extends Migration {

  val table = "trip"; //put your table name here

  def up() {
    addColumn(table, "is_processing", BooleanType, Default("false"))

  }

  def down() {
    //dropTable(table)
  }
}
