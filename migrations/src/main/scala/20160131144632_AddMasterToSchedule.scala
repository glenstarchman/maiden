package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160131144632_AddMasterToSchedule extends Migration {

  val table = "schedule"; //put your table name here

  def up() {
    addColumn(table, "masterScheduleId", VarcharType, Limit(255))

    addIndex(table, Array("masterScheduleId"), Name("sched_master_index"))
  }

  def down() {
    //dropTable(table)
  }
}
