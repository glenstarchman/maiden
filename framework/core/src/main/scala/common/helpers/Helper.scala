/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package com.maiden.framework.common.helpers


object Helper {
  def typeOf[T: Manifest](t: T): Manifest[T] = manifest[T]
}
