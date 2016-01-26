package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160126103332_AddCloseByToStop extends Migration {

  val table = "stop"; //put your table name here

  def up() {
    addColumn(table, "close_by", VarcharType, Limit(1024))
  }

  def down() {
    //dropTable(table)
  }
}
