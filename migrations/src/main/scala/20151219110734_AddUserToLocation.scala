package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151219110734_AddUserToLocation extends Migration {

  val table = "gps_location"; //put your table name here

  def up() {
    addColumn(table, "user_id", BigintType, NotNull)

  }

  def down() {
    //dropTable(table)
  }
}
