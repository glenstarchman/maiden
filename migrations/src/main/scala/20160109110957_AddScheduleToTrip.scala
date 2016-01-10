package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160109110957_AddScheduleToTrip extends Migration {

  val table = "trip"; //put your table name here

  def up() {
    addColumn(table, "schedule_id", BigintType)
  }

  def down() {
    //dropTable(table)
  }
}
