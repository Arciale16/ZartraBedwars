package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Explicit reservation outcome. */ public final class ReservationResult{
/** Reservation outcome. */ public enum Status{
RESERVED,STALE_EPOCH,CONFLICT,EXPIRED,REJECTED}
private final ProxyReservationId id;
private final Status status;
private final BackendId backendId;
private final InstanceEpoch epoch;
private final Instant expiresAt;
private ReservationResult(final ProxyReservationId id,final Status status,final BackendId backend,final InstanceEpoch epoch,final Instant expiresAt){
this.id=Objects.requireNonNull(id,"id");
this.status=Objects.requireNonNull(status,"status");
if((status==Status.RESERVED)!=(backend!=null&&epoch!=null&&expiresAt!=null)){
throw new IllegalArgumentException("reserved result requires backend, epoch and expiry");
}
backendId=backend;
this.epoch=epoch;
this.expiresAt=expiresAt;
}
/** Creates success. */ public static ReservationResult reserved(final ProxyReservationId id,final BackendId backend,final InstanceEpoch epoch,final Instant expiresAt){
return new ReservationResult(id,Status.RESERVED,backend,epoch,expiresAt);
}
/** Creates failure. */ public static ReservationResult failed(final ProxyReservationId id,final Status status){
if(status==Status.RESERVED){
throw new IllegalArgumentException("use reserved factory");
}
return new ReservationResult(id,status,null,null,null);
}
/** Returns ID. */ public ProxyReservationId id(){
return id;
}
/** Returns status. */ public Status status(){
return status;
}
/** Returns backend. */ public Optional<BackendId> backendId(){
return Optional.ofNullable(backendId);
}
/** Returns epoch. */ public Optional<InstanceEpoch> epoch(){
return Optional.ofNullable(epoch);
}
/** Returns expiry. */ public Optional<Instant> expiresAt(){
return Optional.ofNullable(expiresAt);
}
@Override public boolean equals(final Object other){
if(!(other instanceof ReservationResult)){
return false;
}
ReservationResult value=(ReservationResult)other;
return id.equals(value.id)&&status==value.status&&Objects.equals(backendId,value.backendId)&&Objects.equals(epoch,value.epoch)&&Objects.equals(expiresAt,value.expiresAt);
}
@Override public int hashCode(){
return Objects.hash(id,status,backendId,epoch,expiresAt);
}
}
