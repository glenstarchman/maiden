package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151218031940_CreateStop extends Migration {

  val table = "stop"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("route_id", NotNull)
      t.integer("stop_order", NotNull)
      t.varchar("name", Limit(100))
      t.varchar("address", NotNull, Limit(255))
      t.varchar("description", Limit(1024))
      t.varchar("details", Limit(1024))
      t.varchar("thumbnail", Limit(1024))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("route_id"), Name("stop_route_id_index"))


  }

  def down() {
    dropTable(table)
  }
}
