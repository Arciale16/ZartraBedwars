package io.zartra.bedwars.redis.api;
/** Positive monotonic invalidation version. */ public final class InvalidationVersion implements Comparable<InvalidationVersion>{
private final long value;
private InvalidationVersion(final long v){
if (v<1) {
throw new IllegalArgumentException("version must be positive");
}
value=v;
}
/** Creates a version. */ public static InvalidationVersion of(final long v){
return new InvalidationVersion(v);
}
/** Returns value. */ public long value(){
return value;
}
/** Tests monotonic order. */ public boolean isNewerThan(final InvalidationVersion o){
return compareTo(o)>0;
}
@Override public int compareTo(final InvalidationVersion o){
return Long.compare(value,o.value);
}
@Override public boolean equals(final Object o){
return o instanceof InvalidationVersion&&value==((InvalidationVersion)o).value;
}
@Override public int hashCode(){
return Long.hashCode(value);
}
@Override public String toString(){
return Long.toString(value);
}
}
