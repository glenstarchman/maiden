package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151221110056_AddGeomToTrip extends Migration {

  val table = "trip"; //put your table name here

  def up() {
    val sql = "alter table trip add column geom geometry"
    execute(sql)
    val index = "create index trip_geom_index on trip using gist(geom)"
    execute(index)
  }

  def down() {
    //dropTable(table)
  }
}
