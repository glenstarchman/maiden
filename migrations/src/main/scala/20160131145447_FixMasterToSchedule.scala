package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160131145447_FixMasterToSchedule extends Migration {

  val table = "schedule"; //put your table name here

  def up() {
    removeColumn(table, "masterScheduleId")
    addColumn(table, "master_schedule_id", VarcharType, Limit(255))
    addIndex(table, Array("master_schedule_id"), Name("sched_master_index"))


  }

  def down() {
    //dropTable(table)
  }
}
