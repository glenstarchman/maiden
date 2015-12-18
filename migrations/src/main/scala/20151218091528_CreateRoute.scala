package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20151218091528_CreateRoute extends Migration {

  val table = "route"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.varchar("name", NotNull, Limit(255))
      t.varchar("description", NotNull, Limit(10245))
      t.varchar("detail", NotNull, Limit(10245))
      t.varchar("hour_start", NotNull, Limit(5))
      t.varchar("hour_end", NotNull, Limit(5))
      t.int("day_start", NotNull, Default("4"))
      t.int("day_end", NotNull, Default("1"))
      t.varchar("thumbnail", NotNull, Limit(10245))
      t.boolean("active", NotNull, Default("false"))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }
  }

  def down() {
    dropTable(table)
  }
}
