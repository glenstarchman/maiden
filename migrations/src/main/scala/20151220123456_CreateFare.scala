package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151220123456_CreateFare extends Migration {

  val table = "fare"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.integer("fare_type", NotNull)
      t.decimal("amount", Precision(5), Scale(2))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }
  }

  def down() {
    //dropTable(table)
  }
}
