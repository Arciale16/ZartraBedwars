package io.zartra.bedwars.redis.api;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
/** Immutable reservation attempt result. */ public final class ReservationResult{
/** Outcome. */ public enum Status{
ACQUIRED,CONFLICT,EXPIRED,REJECTED}
private final ReservationId id;
private final Status status;
private final Instant expiry;
private final FencingToken token;
private ReservationResult(final ReservationId i,final Status s,final Instant e,final FencingToken t){
id=Objects.requireNonNull(i,"id");
status=Objects.requireNonNull(s,"status");
expiry=e;
token=t;
if ((s==Status.ACQUIRED)!=(e!=null&&t!=null)) {
throw new IllegalArgumentException("invalid reservation result");
}
}
/** Creates success. */ public static ReservationResult acquired(final ReservationId i,final Instant e,final FencingToken t){
return new ReservationResult(i,Status.ACQUIRED,e,t);
}
/** Creates failure. */ public static ReservationResult failed(final ReservationId i,final Status s){
if(s==Status.ACQUIRED) {
throw new IllegalArgumentException("use acquired");
}
return new ReservationResult(i,s,null,null);
}
/** Returns ID. */ public ReservationId id(){
return id;
}
/** Returns status. */ public Status status(){
return status;
}
/** Returns expiry. */ public Optional<Instant> expiresAt(){
return Optional.ofNullable(expiry);
}
/** Returns fencing token. */ public Optional<FencingToken> fencingToken(){
return Optional.ofNullable(token);
}
@Override public boolean equals(final Object o){
if(!(o instanceof ReservationResult)) {
return false;
}
ReservationResult x=(ReservationResult)o;
return id.equals(x.id)&&status==x.status&&Objects.equals(expiry,x.expiry)&&Objects.equals(token,x.token);
}
@Override public int hashCode(){
return Objects.hash(id,status,expiry,token);
}
}
