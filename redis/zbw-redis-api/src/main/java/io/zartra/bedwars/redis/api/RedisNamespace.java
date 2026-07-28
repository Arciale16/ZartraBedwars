package io.zartra.bedwars.redis.api;
import java.util.Objects;
/** Installation, environment and domain isolated namespace. */ public final class RedisNamespace{
private final String installation,environment,name;
private final SchemaVersion schema;
private RedisNamespace(final String i,final String e,final String n,final SchemaVersion s){
installation=RedisContractValidation.token(i,"installation");
environment=RedisContractValidation.token(e,"environment");
name=RedisContractValidation.token(n,"namespace");
schema=Objects.requireNonNull(s,"schema");
}
/** Creates a namespace. */ public static RedisNamespace of(final String i,final String e,final String n,final SchemaVersion s){
return new RedisNamespace(i,e,n,s);
}
/** Returns installation. */ public String installation(){
return installation;
}
/** Returns environment. */ public String environment(){
return environment;
}
/** Returns domain name. */ public String name(){
return name;
}
/** Returns schema. */ public SchemaVersion schema(){
return schema;
}
/** Returns deterministic prefix. */ public String prefix(){
return installation+":"+environment+":"+name+":v"+schema;
}
@Override public boolean equals(final Object o){
if (!(o instanceof RedisNamespace)) {
return false;
}
RedisNamespace x=(RedisNamespace)o;
return installation.equals(x.installation)&&environment.equals(x.environment)&&name.equals(x.name)&&schema.equals(x.schema);
}
@Override public int hashCode(){
return Objects.hash(installation,environment,name,schema);
}
@Override public String toString(){
return prefix();
}
}
