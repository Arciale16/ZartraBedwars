package io.zartra.bedwars.proxy.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Explicit routing outcome without platform objects. */ public final class RoutingResult{
/** Routing outcome. */ public enum Status{
ROUTED,NO_CAPACITY,UNAVAILABLE,REJECTED}
private final UUID requestId;
private final Status status;
private final BackendId backendId;
private final InstanceEpoch epoch;
private final String code;
private RoutingResult(final UUID id,final Status status,final BackendId backend,final InstanceEpoch epoch,final String code){
requestId=Objects.requireNonNull(id,"requestId");
this.status=Objects.requireNonNull(status,"status");
this.code=ProxyContractValidation.token(code,"resultCode");
if((status==Status.ROUTED)!=(backend!=null&&epoch!=null)){
throw new IllegalArgumentException("routed result requires backend and epoch");
}
backendId=backend;
this.epoch=epoch;
}

/** Creates success. */ public static RoutingResult routed(final UUID id,final BackendId backend,final InstanceEpoch epoch){
return new RoutingResult(id,Status.ROUTED,backend,epoch,"routed");
}
/** Creates failure. */ public static RoutingResult failed(final UUID id,final Status status,final String code){
if(status==Status.ROUTED){
throw new IllegalArgumentException("use routed factory");
}
return new RoutingResult(id,status,null,null,code);
}
/** Returns request ID. */ public UUID requestId(){
return requestId;
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
/** Returns code. */ public String code(){
return code;
}
@Override public boolean equals(final Object other){
if(!(other instanceof RoutingResult)){
return false;
}
RoutingResult value=(RoutingResult)other;
return requestId.equals(value.requestId)&&status==value.status&&Objects.equals(backendId,value.backendId)&&Objects.equals(epoch,value.epoch)&&code.equals(value.code);
}
@Override public int hashCode(){
return Objects.hash(requestId,status,backendId,epoch,code);
}
}
