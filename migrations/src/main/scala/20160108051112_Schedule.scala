package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160108051112_Schedule extends Migration {

  val table = "schedule"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("route_id", NotNull)
      t.bigint("stop_id", NotNull)
      t.integer("day_of_week", NotNull)
      t.varchar("stop_time", NotNull, Limit(5))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("stop_id"), Name("schedule_stop_index"))
    addIndex(table, Array("route_id"), Name("schedule_route_index"))
    addIndex(table, Array("day_of_week"), Name("schedule_day_index"))
  }

  def down() {
    dropTable(table)
  }
}
