package io.zartra.bedwars.redis.api;
import java.util.Objects;
/** Namespaced operation identity for bounded duplicate suppression. */ public final class DeduplicationKey{
private final RedisNamespace namespace;
private final OperationId operation;
private DeduplicationKey(final RedisNamespace n,final OperationId o){
namespace=Objects.requireNonNull(n,"namespace");
operation=Objects.requireNonNull(o,"operation");
}
/** Creates a key. */ public static DeduplicationKey of(final RedisNamespace n,final OperationId o){
return new DeduplicationKey(n,o);
}
/** Returns namespace. */ public RedisNamespace namespace(){
return namespace;
}
/** Returns operation. */ public OperationId operation(){
return operation;
}
/** Returns Redis representation. */ public RedisKey asRedisKey(){
return RedisKey.of(namespace,"dedupe",operation.toString());
}
@Override public boolean equals(final Object o){
return o instanceof DeduplicationKey&&namespace.equals(((DeduplicationKey)o).namespace)&&operation.equals(((DeduplicationKey)o).operation);
}
@Override public int hashCode(){
return Objects.hash(namespace,operation);
}
@Override public String toString(){
return asRedisKey().toString();
}
}
