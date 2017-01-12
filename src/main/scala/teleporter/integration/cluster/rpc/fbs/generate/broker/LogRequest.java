// automatically generated by the FlatBuffers compiler, do not modify

package teleporter.integration.cluster.rpc.fbs.generate.broker;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class LogRequest extends Table {
  public static LogRequest getRootAsLogRequest(ByteBuffer _bb) { return getRootAsLogRequest(_bb, new LogRequest()); }
  public static LogRequest getRootAsLogRequest(ByteBuffer _bb, LogRequest obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public LogRequest __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public int request() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public String cmd() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer cmdAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }

  public static int createLogRequest(FlatBufferBuilder builder,
      int request,
      int cmdOffset) {
    builder.startObject(2);
    LogRequest.addCmd(builder, cmdOffset);
    LogRequest.addRequest(builder, request);
    return LogRequest.endLogRequest(builder);
  }

  public static void startLogRequest(FlatBufferBuilder builder) { builder.startObject(2); }
  public static void addRequest(FlatBufferBuilder builder, int request) { builder.addInt(0, request, 0); }
  public static void addCmd(FlatBufferBuilder builder, int cmdOffset) { builder.addOffset(1, cmdOffset, 0); }
  public static int endLogRequest(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

