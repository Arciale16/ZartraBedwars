package io.zartra.bedwars.redis.api;
import java.util.Objects;
/** Cursor for the last fully consumed stream record. */ public final class StreamCursor{
private final RedisKey stream;
private final StreamId last;
private StreamCursor(final RedisKey s,final StreamId l){
stream=Objects.requireNonNull(s,"stream");
last=Objects.requireNonNull(l,"last");
}
/** Creates a cursor. */ public static StreamCursor after(final RedisKey s,final StreamId l){
return new StreamCursor(s,l);
}
/** Returns stream. */ public RedisKey stream(){
return stream;
}
/** Returns last ID. */ public StreamId lastConsumed(){
return last;
}
/** Advances monotonically. */ public StreamCursor advance(final StreamId next){
if (next.compareTo(last)<=0) {
throw new IllegalArgumentException("cursor must advance");
}
return new StreamCursor(stream,next);
}
@Override public boolean equals(final Object o){
return o instanceof StreamCursor&&stream.equals(((StreamCursor)o).stream)&&last.equals(((StreamCursor)o).last);
}
@Override public int hashCode(){
return Objects.hash(stream,last);
}
}
