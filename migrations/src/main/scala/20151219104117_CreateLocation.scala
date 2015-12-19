package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151219104117_CreateLocation extends Migration {

  val table = "gps_location"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.decimal("latitude", Precision(9), Scale(5)) 
      t.decimal("longitude", Precision(9), Scale(5)) 
      t.bigint("route_id")
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }


  }

  def down() {
    //dropTable(table)
  }
}
