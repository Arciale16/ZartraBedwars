package io.zartra.bedwars.redis.api;
/** Positive epoch that rejects stale distributed writers. */ public final class FencingToken implements Comparable<FencingToken>{
private final long value;
private FencingToken(final long v){
if (v<1) {
throw new IllegalArgumentException("token must be positive");
}
value=v;
}
/** Creates a token. */ public static FencingToken of(final long v){
return new FencingToken(v);
}
/** Returns epoch. */ public long value(){
return value;
}
/** Tests strict order. */ public boolean isNewerThan(final FencingToken o){
return compareTo(o)>0;
}
/** Rejects stale/repeated epochs. */ public void requireNewerThan(final FencingToken o){
if(!isNewerThan(o)) {
throw new IllegalArgumentException("stale fencing token");
}
}
@Override public int compareTo(final FencingToken o){
return Long.compare(value,o.value);
}
@Override public boolean equals(final Object o){
return o instanceof FencingToken&&value==((FencingToken)o).value;
}
@Override public int hashCode(){
return Long.hashCode(value);
}
@Override public String toString(){
return Long.toString(value);
}
}
