package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151218033304_AddGeomToStop extends Migration {

  val table = "stop"; //put your table name here

  def up() {
    val sql = "alter table stop add column geom geometry not null"
    execute(sql)
    val index = "create index stop_geom_index on stop using gist(geom)"
    execute(index)
  }

  def down() {
    //dropTable(table)
  }
}
