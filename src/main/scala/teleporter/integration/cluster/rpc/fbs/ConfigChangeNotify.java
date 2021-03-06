// automatically generated by the FlatBuffers compiler, do not modify

package teleporter.integration.cluster.rpc.fbs;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class ConfigChangeNotify extends Table {
  public static ConfigChangeNotify getRootAsConfigChangeNotify(ByteBuffer _bb) { return getRootAsConfigChangeNotify(_bb, new ConfigChangeNotify()); }
  public static ConfigChangeNotify getRootAsConfigChangeNotify(ByteBuffer _bb, ConfigChangeNotify obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public ConfigChangeNotify __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public String key() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer keyAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public byte action() { int o = __offset(6); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public long timestamp() { int o = __offset(8); return o != 0 ? bb.getLong(o + bb_pos) : 0; }

  public static int createConfigChangeNotify(FlatBufferBuilder builder,
      int keyOffset,
      byte action,
      long timestamp) {
    builder.startObject(3);
    ConfigChangeNotify.addTimestamp(builder, timestamp);
    ConfigChangeNotify.addKey(builder, keyOffset);
    ConfigChangeNotify.addAction(builder, action);
    return ConfigChangeNotify.endConfigChangeNotify(builder);
  }

  public static void startConfigChangeNotify(FlatBufferBuilder builder) { builder.startObject(3); }
  public static void addKey(FlatBufferBuilder builder, int keyOffset) { builder.addOffset(0, keyOffset, 0); }
  public static void addAction(FlatBufferBuilder builder, byte action) { builder.addByte(1, action, 0); }
  public static void addTimestamp(FlatBufferBuilder builder, long timestamp) { builder.addLong(2, timestamp, 0); }
  public static int endConfigChangeNotify(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

