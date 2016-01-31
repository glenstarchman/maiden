package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160131093014_UpdateScheduleTime extends Migration {

  val table = "schedule"; //put your table name here

  def up() {
    alterColumn(table, "stop_time", VarcharType, Limit(12))
  }

  def down() {
    //dropTable(table)
  }
}
