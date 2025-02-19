/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package com.maiden.migrations

import com.imageworks.migration._

class Migrate_20150805082429_AddAdminFlagToUser extends Migration {

  val table = "users"; //put your table name here

  def up() {
    addColumn(table, "admin", BooleanType, NotNull, Default("false"))

  }

  def down() {
    removeColumn(table, "admin")
  }
}
