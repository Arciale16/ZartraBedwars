package io.zartra.bedwars.redis.api;
import java.time.Instant;
import java.util.Objects;
/** Immutable cache invalidation reference without cached content. */ public final class CacheInvalidation{
private final RedisKey key;
private final InvalidationVersion version;
private final OperationId operation;
private final Instant occurredAt;
private CacheInvalidation(final RedisKey k,final InvalidationVersion v,final OperationId o,final Instant at){
key=Objects.requireNonNull(k,"key");
version=Objects.requireNonNull(v,"version");
operation=Objects.requireNonNull(o,"operation");
occurredAt=Objects.requireNonNull(at,"occurredAt");
}
/** Creates an invalidation. */ public static CacheInvalidation of(final RedisKey k,final InvalidationVersion v,final OperationId o,final Instant at){
return new CacheInvalidation(k,v,o,at);
}
/** Returns key. */ public RedisKey key(){
return key;
}
/** Returns version. */ public InvalidationVersion version(){
return version;
}
/** Returns operation. */ public OperationId operation(){
return operation;
}
/** Returns occurrence time. */ public Instant occurredAt(){
return occurredAt;
}
@Override public boolean equals(final Object o){
if (!(o instanceof CacheInvalidation)) {
return false;
}
CacheInvalidation x=(CacheInvalidation)o;
return key.equals(x.key)&&version.equals(x.version)&&operation.equals(x.operation)&&occurredAt.equals(x.occurredAt);
}
@Override public int hashCode(){
return Objects.hash(key,version,operation,occurredAt);
}
}
