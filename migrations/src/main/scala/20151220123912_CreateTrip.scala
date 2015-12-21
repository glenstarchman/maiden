package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151220123912_CreateTrip extends Migration {

  val table = "trip"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("user_id", NotNull)
      t.bigint("driver_id")
      t.bigint("vehicle_id")
      t.bigint("route_id")
      t.bigint("fare_id", NotNull)
      t.integer("reservation_type", NotNull)
      t.integer("ride_state", NotNull)
      t.integer("payment_state", NotNull)
      t.integer("discount_type", NotNull)
      t.boolean("is_transfer", NotNull, Default("false"))
      t.bigint("pickup_stop", NotNull)
      t.bigint("dropoff_stop", NotNull)
      t.timestamp("reservation_time", NotNull, Default("NOW()"))
      t.timestamp("pickup_time")
      t.timestamp("dropoff_time")
      t.timestamp("cancellation_time")
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("user_id"), Name("trip_user_index"))
    addIndex(table, Array("driver_id"), Name("trip_driver_index"))
    addIndex(table, Array("route_id"), Name("trip_route_index"))
    addIndex(table, Array("reservation_time"), Name("trip_reservation_time_index"))


  }

  def down() {
    //dropTable(table)
  }
}
