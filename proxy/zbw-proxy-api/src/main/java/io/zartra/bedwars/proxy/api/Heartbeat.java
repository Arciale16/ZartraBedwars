package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;

/** Expiring backend heartbeat tied to an instance epoch. */ public final class Heartbeat{
private final BackendId backendId;
private final InstanceEpoch epoch;
private final CapacitySnapshot capacity;
private final HealthSnapshot health;
private final Instant issuedAt;
private final Instant expiresAt;
private Heartbeat(final BackendId id,final InstanceEpoch epoch,final CapacitySnapshot capacity,final HealthSnapshot health,final Instant issuedAt,final Instant expiresAt){
backendId=Objects.requireNonNull(id,"backendId");
this.epoch=Objects.requireNonNull(epoch,"epoch");
this.capacity=Objects.requireNonNull(capacity,"capacity");
this.health=Objects.requireNonNull(health,"health");
this.issuedAt=Objects.requireNonNull(issuedAt,"issuedAt");
this.expiresAt=Objects.requireNonNull(expiresAt,"expiresAt");
if(!expiresAt.isAfter(issuedAt)||health.observedAt().isAfter(issuedAt)){
throw new IllegalArgumentException("invalid heartbeat time");
}
}

/** Creates a heartbeat. */ public static Heartbeat of(final BackendId id,final InstanceEpoch epoch,final CapacitySnapshot capacity,final HealthSnapshot health,final Instant issuedAt,final Instant expiresAt){
return new Heartbeat(id,epoch,capacity,health,issuedAt,expiresAt);
}
/** Returns backend ID. */ public BackendId backendId(){
return backendId;
}
/** Returns epoch. */ public InstanceEpoch epoch(){
return epoch;
}
/** Returns capacity. */ public CapacitySnapshot capacity(){
return capacity;
}
/** Returns health. */ public HealthSnapshot health(){
return health;
}
/** Returns issued time. */ public Instant issuedAt(){
return issuedAt;
}
/** Returns expiry. */ public Instant expiresAt(){
return expiresAt;
}
/** Tests expiry. */ public boolean isExpiredAt(final Instant now){
return !Objects.requireNonNull(now,"now").isBefore(expiresAt);
}
@Override public boolean equals(final Object other){
if(!(other instanceof Heartbeat)){
return false;
}
Heartbeat value=(Heartbeat)other;
return backendId.equals(value.backendId)&&epoch.equals(value.epoch)&&capacity.equals(value.capacity)&&health.equals(value.health)&&issuedAt.equals(value.issuedAt)&&expiresAt.equals(value.expiresAt);
}
@Override public int hashCode(){
return Objects.hash(backendId,epoch,capacity,health,issuedAt,expiresAt);
}
}
