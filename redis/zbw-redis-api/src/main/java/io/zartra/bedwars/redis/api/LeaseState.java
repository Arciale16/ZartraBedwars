package io.zartra.bedwars.redis.api;
import java.time.Instant;
import java.util.Objects;
/** Immutable fenced lease snapshot with no durable authority. */ public final class LeaseState{
/** Lifecycle. */ public enum Status{
ACTIVE,RELEASED,EXPIRED}
private final LeaseId id;
private final String holder;
private final FencingToken token;
private final Instant acquiredAt,expiresAt;
private final Status status;
private LeaseState(final LeaseId i,final String h,final FencingToken t,final Instant a,final Instant e,final Status s){
id=Objects.requireNonNull(i,"id");
holder=RedisContractValidation.opaque(h,"holder");
token=Objects.requireNonNull(t,"token");
acquiredAt=Objects.requireNonNull(a,"acquiredAt");
expiresAt=Objects.requireNonNull(e,"expiresAt");
status=Objects.requireNonNull(s,"status");
if (!e.isAfter(a)) {
throw new IllegalArgumentException("expiry must follow acquisition");
}
}
/** Creates snapshot. */ public static LeaseState of(final LeaseId i,final String h,final FencingToken t,final Instant a,final Instant e,final Status s){
return new LeaseState(i,h,t,a,e,s);
}
/** Returns ID. */ public LeaseId id(){
return id;
}
/** Returns opaque holder. */ public String holder(){
return holder;
}
/** Returns fencing token. */ public FencingToken token(){
return token;
}
/** Returns acquisition. */ public Instant acquiredAt(){
return acquiredAt;
}
/** Returns expiry. */ public Instant expiresAt(){
return expiresAt;
}
/** Returns status. */ public Status status(){
return status;
}
/** Rejects a stale successor. */ public void requireSuccessor(final FencingToken candidate){
candidate.requireNewerThan(token);
}
@Override public boolean equals(final Object o){
if(!(o instanceof LeaseState)) {
return false;
}
LeaseState x=(LeaseState)o;
return id.equals(x.id)&&holder.equals(x.holder)&&token.equals(x.token)&&acquiredAt.equals(x.acquiredAt)&&expiresAt.equals(x.expiresAt)&&status==x.status;
}
@Override public int hashCode(){
return Objects.hash(id,holder,token,acquiredAt,expiresAt,status);
}
}
