package io.zartra.bedwars.redis.api;
import java.util.Objects;
/** Immutable privacy-safe namespaced Redis key. */ public final class RedisKey implements Comparable<RedisKey>{
private final RedisNamespace namespace;
private final String category,identity;
private RedisKey(final RedisNamespace n,final String c,final String i){
namespace=Objects.requireNonNull(n,"namespace");
category=RedisContractValidation.token(c,"category");
identity=RedisContractValidation.opaque(i,"identity");
}
/** Creates a key. */ public static RedisKey of(final RedisNamespace n,final String c,final String i){
return new RedisKey(n,c,i);
}
/** Returns namespace. */ public RedisNamespace namespace(){
return namespace;
}
/** Returns category. */ public String category(){
return category;
}
/** Returns opaque identity. */ public String identity(){
return identity;
}
/** Returns qualified key. */ public String qualified(){
return namespace.prefix()+":"+category+":"+identity;
}
@Override public int compareTo(final RedisKey o){
return qualified().compareTo(o.qualified());
}
@Override public boolean equals(final Object o){
return o instanceof RedisKey&&qualified().equals(((RedisKey)o).qualified());
}
@Override public int hashCode(){
return qualified().hashCode();
}
@Override public String toString(){
return qualified();
}
}
