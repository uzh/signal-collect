package com.signalcollect.factory.storage

import com.signalcollect.interfaces._
import com.signalcollect.storage.TroveStorageBackend
import com.signalcollect.storage.DefaultStorage


class Trove  extends StorageFactory {
  def createInstance: Storage = new DefaultStorage with TroveStorageBackend
}