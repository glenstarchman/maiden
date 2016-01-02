package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160101081259_CreateStripe extends Migration {

  val table = "stripe"; //put your table name here

  def up() {
    createTable(table) { t => 
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("user_id", NotNull)
      t.varchar("stripe_customer")
      t.boolean("is_default", NotNull, Default("true"))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }
  }

  def down() {
    dropTable(table)
  }
}
