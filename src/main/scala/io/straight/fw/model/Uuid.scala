/*
 * Copyright (C) 2013 soqqo ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.straight.fw.model

/**
 * Refer to http://rbtech.blogspot.co.uk/2013/10/embedding-primary-key-in-uuid-guid-for.html
 *
 * This Uuid Class internally stores the 3 values we care about
 *
 * Two others are fixed  (version and variant)
 *
 * {{{
 * A UUID is comprised of two 64bit longs.
 * [       lower    ] [    upper      ]
 * 00000000-0000-6000-a000-000000000000
 *                    -----------------
 *                    60 bits Random bits (time or other)
 *                    -
 *                    x == a, b, 8 or 9 - we will use 'a'
 *               ---
 *               Group ID - of 12 bits value is (0 to 4095)
 *              -
 *              6 == always for (Version 6 -- because we can (we are not really UUID's)
 * -------- ----
 * Primary key ID of 48 bits  (max 281,474,976,710,656)
 * }}}
 * Essentially, the ID and the Group ID of the object
 * will be found in the lower 64 bits, separated in the UUID by the '6' (the version).
 * The other upper 64 bits will be random, excepting the IETF marker.
 */
case class Uuid(id: Long, groupId: Int, uniqId: Long) {
  // we blatantly ignore the groupId in a max check
  def max(other: Uuid) = this.id > other.id
  /*
   * See the companion object for what this all means
   */
  val uuid = new java.util.UUID(groupId |
    (Uuid.UUID_VERSION << Uuid.UUID_VERSION_OFFSET) |
    (id << Uuid.ID_OFFSET), (Uuid.UUID_VARIANT << Uuid.UUID_VARIANT_OFFSET) |
    (uniqId >>> 4)).toString

  /**
   * Just the id and the groupId
   * eg. 12-66df, 2a-6afa
   * 6df is the groupId, afa is the groupId
   */
  val shortId:String = {
    val ShortIdReg = """[0\-]*([\da-fA-F\-]+-[\da-fA-F]{4})-[\da-fA-F]{4}-[\da-fA-F]{12}""".r
    val ShortIdReg(shortId) = uuid
    shortId
  }

  override def toString = uuid

}

object Uuid {


  val UUID_VERSION = 6L
  val UUID_VARIANT = 10L   // a as the variant

  val ID_OFFSET = 16
  val UUID_VERSION_OFFSET = 12
  val UUID_VARIANT_OFFSET = 60

  /**
   * Create a partial Uuid .. the last part
   * ::
   * :: The beginning is always 0
   */
  def createPartialUuidString(id: Long, groupId: Int): String = new java.util.UUID(groupId | (UUID_VERSION << UUID_VERSION_OFFSET) | (id << ID_OFFSET), 0L).toString().substring(0, 18)

  /**
   * A String name version
   */
  def createPartialUuidString(id: Long, groupName: String): String = createPartialUuidString(id, groupId(groupName))

  def createPartialUuidString(id: Long, klass: Class[_]): String = createPartialUuidString(id, groupIdForClass(klass))

  def groupIdForClass(klass: Class[_]):Int = groupId(klass.getCanonicalName)

  /**
   * Create a Uuid object from the standard UUID String
   * @param uuid
   * @return
   */
  def apply(uuid:String):Uuid = {

    val uuidMatchString = """([\da-fA-F]{8})-([\da-fA-F]{4})-""" +  // two parts of the ID
      UUID_VERSION.toHexString               +  // '6'
      """([\da-fA-F]{3})-"""                  +  // groupId
      UUID_VARIANT.toHexString               +  // 'a'
      """([\da-fA-F]{3})-([\da-fA-F]{12})"""

    val uuidRegex = uuidMatchString.r
    val uuidRegex(id0,id1,groupId,uniq0,uniq1) = uuid
    val tId = java.lang.Long.parseLong(id0+id1,16)
    val tUniqId = java.lang.Long.parseLong(uniq0+uniq1,16)
    val tGroupId = Integer.parseInt(groupId)
    return new Uuid(tId,tGroupId,tUniqId)
  }

  def fromString(s:String) = apply(s)

  /**
   * Determine a Group Id for a Class
   */
  def groupId(groupName : String) = crc12(groupName)

  private def crc12(toHash: String) = {

    /**
     * ************************************************************************
     *  Using direct calculation
     * ************************************************************************
     */

    var crc:Int = 0xFFF; // initial contents of LFBSR
    var poly: Int = 0xF01; // reverse polynomial
    var bytes = toHash.getBytes()

    for (b: Byte <- bytes) {
      var temp = (crc ^ b) & 0xff;

      // read 8 bits one at a time
      for (i <- 0 to 7) {
        if ((temp & 1) == 1) temp = (temp >>> 1) ^ poly;
        else temp = (temp >>> 1);
      }
      crc = (crc >>> 8) ^ temp;
    }

    // flip bits
    crc = crc ^ 0xfff;

    crc;

  }

}
