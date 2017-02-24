// automatically generated by the FlatBuffers compiler, do not modify

package teleporter.integration.cluster.rpc.fbs;

public final class EventType {
  private EventType() { }
  public static final byte None = -1;
  public static final byte KVGet = 0;
  public static final byte RangeRegexKV = 1;
  public static final byte KVSave = 2;
  public static final byte AtomicSaveKV = 3;
  public static final byte KVRemove = 4;
  public static final byte LogTail = 10;
  public static final byte LinkInstance = 20;
  public static final byte LinkAddress = 21;
  public static final byte LinkVariable = 22;
  public static final byte TaskState = 31;
  public static final byte BrokerState = 32;
  public static final byte InstanceState = 33;
  public static final byte ConfigChangeNotify = 40;

  public static final String[] names = { "None", "KVGet", "RangeRegexKV", "KVSave", "AtomicSaveKV", "KVRemove", "", "", "", "", "", "LogTail", "", "", "", "", "", "", "", "", "", "LinkInstance", "LinkAddress", "LinkVariable", "", "", "", "", "", "", "", "", "TaskState", "BrokerState", "InstanceState", "", "", "", "", "", "", "ConfigChangeNotify", };

  public static String name(int e) { return names[e - None]; }
}

