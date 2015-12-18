/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
 */

package com.maiden.common.helpers


object Helper {
  def typeOf[T: Manifest](t: T): Manifest[T] = manifest[T]
}
