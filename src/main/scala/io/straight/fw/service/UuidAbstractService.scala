package io.straight.fw.service

import io.straight.fw.model._

/**
 * @author rbuckland
 */

trait UuidAbstractService[D <: DomainType[Uuid]] extends AbstractService[D,Uuid]{
  override def repository: UuidRepository[D]

  def findByIdAndGroup(id: Long, groupId: Int) = repository.findByIdAndGroup(id,groupId)
  def findByOnlyId(id: Long) = repository.findByOnlyId(id)

}

