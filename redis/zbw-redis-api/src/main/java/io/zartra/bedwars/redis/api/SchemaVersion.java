package io.zartra.bedwars.redis.api;
import java.util.Objects;
/** Immutable coordination schema version (ZBW-DEPLOY-006/008). */ public final class SchemaVersion implements Comparable<SchemaVersion>{
private final int major,minor;
private SchemaVersion(final int major,final int minor){
if (major<1||minor<0) {
throw new IllegalArgumentException("invalid schema version");
}
this.major=major;
this.minor=minor;
}
/** Creates a validated version. */ public static SchemaVersion of(final int major,final int minor){
return new SchemaVersion(major,minor);
}
/** Returns major. */ public int major(){
return major;
}
/** Returns minor. */ public int minor(){
return minor;
}
@Override public int compareTo(final SchemaVersion o){
int c=Integer.compare(major,o.major);
return c==0?Integer.compare(minor,o.minor):c;
}
@Override public boolean equals(final Object o){
return o instanceof SchemaVersion&&major==((SchemaVersion)o).major&&minor==((SchemaVersion)o).minor;
}
@Override public int hashCode(){
return Objects.hash(major,minor);
}
@Override public String toString(){
return major+"."+minor;
}
}
