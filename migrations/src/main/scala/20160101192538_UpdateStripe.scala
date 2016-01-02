package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20160101192538_UpdateStripe extends Migration {

  val table = "stripe"; //put your table name here

  def up() {
    addColumn(table, "description", VarcharType, Limit(128))
    addColumn(table, "last4", VarcharType, Limit(4))
    addColumn(table, "brand", VarcharType, Limit(100))

    addIndex(table, Array("user_id"), Name("stripe_user_index"))


  }

  def down() {
    //dropTable(table)
  }
}
