package io.zartra.bedwars.redis.api;
import java.time.Instant;
import java.util.Objects;
/** Bounded reservation request carrying only opaque actor identity. */ public final class ReservationRequest{
private final ReservationId id;
private final OperationId operation;
private final RedisKey resource;
private final String requester;
private final Instant requestedAt,expiresAt;
private ReservationRequest(final ReservationId i,final OperationId o,final RedisKey r,final String q,final Instant a,final Instant e){
id=Objects.requireNonNull(i,"id");
operation=Objects.requireNonNull(o,"operation");
resource=Objects.requireNonNull(r,"resource");
requester=RedisContractValidation.opaque(q,"requester");
requestedAt=Objects.requireNonNull(a,"requestedAt");
expiresAt=Objects.requireNonNull(e,"expiresAt");
if (!e.isAfter(a)) {
throw new IllegalArgumentException("expiry must follow request");
}
}
/** Creates a request. */ public static ReservationRequest of(final ReservationId i,final OperationId o,final RedisKey r,final String q,final Instant a,final Instant e){
return new ReservationRequest(i,o,r,q,a,e);
}
/** Returns ID. */ public ReservationId id(){
return id;
}
/** Returns operation. */ public OperationId operation(){
return operation;
}
/** Returns resource. */ public RedisKey resource(){
return resource;
}
/** Returns opaque requester. */ public String requester(){
return requester;
}
/** Returns request time. */ public Instant requestedAt(){
return requestedAt;
}
/** Returns expiry. */ public Instant expiresAt(){
return expiresAt;
}
@Override public boolean equals(final Object x){
if(!(x instanceof ReservationRequest)) {
return false;
}
ReservationRequest y=(ReservationRequest)x;
return id.equals(y.id)&&operation.equals(y.operation)&&resource.equals(y.resource)&&requester.equals(y.requester)&&requestedAt.equals(y.requestedAt)&&expiresAt.equals(y.expiresAt);
}
@Override public int hashCode(){
return Objects.hash(id,operation,resource,requester,requestedAt,expiresAt);
}
}
