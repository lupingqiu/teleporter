package teleporter.integration.utils

import com.google.common.base.Charsets
import org.apache.commons.io.FileUtils

import scala.util.matching.Regex

/**
  * Author: kui.dai
  * Date: 2016/4/8.
  */
trait Bytes {
  implicit def toBytes(value: Boolean): Array[Byte] = Array[Byte](if (value) 1 else 0)

  implicit def toBytes(value: Char): Array[Byte] = Array[Byte]((value >> 8).toByte, value.toByte)

  implicit def toBytes(value: Short): Array[Byte] = Array[Byte]((value >> 8).toByte, value.toByte)

  implicit def toBytes(value: Int): Array[Byte] = Array[Byte]((value >> 24).toByte, (value >> 16).toByte, (value >> 8).toByte, value.toByte)

  implicit def toBytes(value: Long): Array[Byte] = Array[Byte]((value >> 56).toByte, (value >> 48).toByte, (value >> 40).toByte, (value >> 32).toByte, (value >> 24).toByte, (value >> 16).toByte, (value >> 8).toByte, value.toByte)

  implicit def toBytes(value: Float): Array[Byte] = toBytes(java.lang.Float.floatToIntBits(value))

  implicit def toBytes(value: Double): Array[Byte] = toBytes(java.lang.Double.doubleToLongBits(value))

  implicit def toBytes(value: BigDecimal): Array[Byte] = java.lang.Double.doubleToLongBits(value.toDouble)

  implicit def toBytes(value: String): Array[Byte] = value.getBytes(Charsets.UTF_8)

  implicit def to(value: Any): Array[Byte] =
    value match {
      case value: Boolean ⇒ Bytes.toBytes(value)
      case value: Char ⇒ Bytes.toBytes(value)
      case value: Short ⇒ Bytes.toBytes(value)
      case value: Int ⇒ Bytes.toBytes(value)
      case value: Long ⇒ Bytes.toBytes(value)
      case value: Float ⇒ Bytes.toBytes(value)
      case value: Double ⇒ Bytes.toBytes(value)
      case value: BigDecimal ⇒ Bytes.toBytes(value)
      case value: String ⇒ Bytes.toBytes(value)
      case x ⇒ Bytes.toBytes(String.valueOf(x))
    }

  implicit def toBoolean(b: Array[Byte]): Boolean = b(0) != 0

  implicit def toChar(b: Array[Byte]): Char = ((b(0) << 8) | (b(1) & 0xFF)).toChar

  implicit def toShort(b: Array[Byte]): Short = ((b(0) << 8) | (b(1) & 0xFF)).toShort

  implicit def toInt(b: Array[Byte]): Int = b(0) << 24 | (b(1) & 0xFF) << 16 | (b(2) & 0xFF) << 8 | (b(3) & 0xFF)

  implicit def toLong(b: Array[Byte]): Long = (b(0) & 0xFFL) << 56 | (b(1) & 0xFFL) << 48 | (b(2) & 0xFFL) << 40 | (b(3) & 0xFFL) << 32 | (b(4) & 0xFFL) << 24 | (b(5) & 0xFFL) << 16 | (b(6) & 0xFFL) << 8 | (b(7) & 0xFFL)

  implicit def toFloat(b: Array[Byte]): Float = java.lang.Float.intBitsToFloat(toInt(b))

  implicit def toDouble(b: Array[Byte]): Double = java.lang.Double.longBitsToDouble(toLong(b))

  implicit def toStr(b: Array[Byte]): String = new String(b, Charsets.UTF_8)
}

object Bytes extends Bytes {
  val BYTE_COUNT_FORMATTER: Regex = "([\\d.]+)\\s*(\\w+)?".r

  def displaySizeToByteCount(displaySize: String): Long = {
    displaySize match {
      case BYTE_COUNT_FORMATTER(scale, unit) ⇒
        if (unit == null) scale.toLong
        else displaySizeToByteCount(scale.toDouble, unit.toUpperCase)
    }
  }

  def displaySizeToByteCount(scale: Double, unit: String): Long = {
    unit match {
      case "KB" ⇒ (scale * FileUtils.ONE_KB).toLong
      case "MB" ⇒ (scale * FileUtils.ONE_MB).toLong
      case "GB" ⇒ (scale * FileUtils.ONE_GB).toLong
      case "TB" ⇒ (scale * FileUtils.ONE_TB).toLong
      case "PB" ⇒ (scale * FileUtils.ONE_PB).toLong
      case "EB" ⇒ (scale * FileUtils.ONE_EB).toLong
      case _ ⇒ throw new IllegalArgumentException(s"UnSupport unit $unit");
    }
  }
}