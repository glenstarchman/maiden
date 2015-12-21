package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151220151624_AddExtraToStop extends Migration {

  val table = "stop"; //put your table name here

  def up() {
    addColumn(table, "active", BooleanType, Default("true"))
    addColumn(table, "marker_type", VarcharType)
    addColumn(table, "marker_color", VarcharType)
    addColumn(table, "show_marker", BooleanType, Default("true"))


  }

  def down() {
    //dropTable(table)
  }
}
