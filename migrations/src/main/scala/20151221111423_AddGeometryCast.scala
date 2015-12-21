package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151221111423_AddGeometryCast extends Migration {

  val table = ""; //put your table name here

  def up() {
    execute("CREATE CAST (varchar AS geometry) WITH INOUT AS IMPLICIT;")

  }

  def down() {
    //dropTable(table)
  }
}
