package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;

/** Immutable backend registration snapshot. */ public final class BackendRegistration{
private final BackendId backendId;
private final InstanceEpoch epoch;
private final BackendCapabilities capabilities;
private final BackendStatus status;
private final Instant registeredAt;
private BackendRegistration(final BackendId id,final InstanceEpoch epoch,final BackendCapabilities capabilities,final BackendStatus status,final Instant registeredAt){
backendId=Objects.requireNonNull(id,"backendId");
this.epoch=Objects.requireNonNull(epoch,"epoch");
this.capabilities=Objects.requireNonNull(capabilities,"capabilities");
this.status=Objects.requireNonNull(status,"status");
this.registeredAt=Objects.requireNonNull(registeredAt,"registeredAt");
}

/** Creates a registration. */ public static BackendRegistration of(final BackendId id,final InstanceEpoch epoch,final BackendCapabilities capabilities,final BackendStatus status,final Instant registeredAt){
return new BackendRegistration(id,epoch,capabilities,status,registeredAt);
}
/** Returns backend ID. */ public BackendId backendId(){
return backendId;
}
/** Returns epoch. */ public InstanceEpoch epoch(){
return epoch;
}
/** Returns capabilities. */ public BackendCapabilities capabilities(){
return capabilities;
}
/** Returns status. */ public BackendStatus status(){
return status;
}
/** Returns registration time. */ public Instant registeredAt(){
return registeredAt;
}
/** Tests whether new routing is accepted. */ public boolean acceptsRouting(){
return status==BackendStatus.ONLINE;
}
@Override public boolean equals(final Object other){
if(!(other instanceof BackendRegistration)){
return false;
}
BackendRegistration value=(BackendRegistration)other;
return backendId.equals(value.backendId)&&epoch.equals(value.epoch)&&capabilities.equals(value.capabilities)&&status==value.status&&registeredAt.equals(value.registeredAt);
}
@Override public int hashCode(){
return Objects.hash(backendId,epoch,capabilities,status,registeredAt);
}
}
