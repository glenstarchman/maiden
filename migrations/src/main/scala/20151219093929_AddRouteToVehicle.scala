package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151219093929_AddRouteToVehicle extends Migration {

  val table = "vehicle"; //put your table name here

  def up() {
    addColumn(table, "route_id", BigintType)

    addIndex(table, Array("route_id"), Name("vehicle_route_index"))

  }

  def down() {
    //dropTable(table)
  }
}
