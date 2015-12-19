package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151219085731_CreateVehicle extends Migration {

  val table = "vehicle"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("driver_id", NotNull)
      t.integer("maximum_occupancy", NotNull)
      t.varchar("license", NotNull, Unique, Limit(12))
      t.varchar("color", NotNull, Limit(50))
      t.varchar("model", NotNull, Limit(128))
      t.boolean("active", NotNull, Default("false"))
      t.bigint("current_location") 
      t.varchar("thumbnail", NotNull, Limit(255))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("driver_id"), Name("vehicle_driver_index"))
    addIndex(table, Array("current_location"), Name("vehicle_location_index"))
  }

  def down() {
    dropTable(table)
  }
}
