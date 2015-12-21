package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151221091920_AddExtraToTrip extends Migration {

  val table = "trip"; //put your table name here

  def up() {
    addColumn(table, "eta", TimestampType)

  }

  def down() {
    //dropTable(table)
  }
}
