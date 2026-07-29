package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;

/** Sanitized diagnostic without endpoints, credentials or player data. */ public final class ProxyDiagnostic{
private final DegradationState state;
private final String code;
private final int registeredBackends;
private final int pendingOperations;
private final Instant observedAt;
private ProxyDiagnostic(final DegradationState state,final String code,final int backends,final int pending,final Instant observedAt){
this.state=Objects.requireNonNull(state,"state");
this.code=ProxyContractValidation.token(code,"diagnosticCode");
if(backends<0||pending<0){
throw new IllegalArgumentException("negative diagnostic count");
}
registeredBackends=backends;
pendingOperations=pending;
this.observedAt=Objects.requireNonNull(observedAt,"observedAt");
}
/** Creates a diagnostic. */ public static ProxyDiagnostic of(final DegradationState state,final String code,final int backends,final int pending,final Instant observedAt){
return new ProxyDiagnostic(state,code,backends,pending,observedAt);
}
/** Returns state. */ public DegradationState state(){
return state;
}
/** Returns code. */ public String code(){
return code;
}
/** Returns backend count. */ public int registeredBackends(){
return registeredBackends;
}
/** Returns pending count. */ public int pendingOperations(){
return pendingOperations;
}
/** Returns observation time. */ public Instant observedAt(){
return observedAt;
}
@Override public boolean equals(final Object other){
if(!(other instanceof ProxyDiagnostic)){
return false;
}
ProxyDiagnostic value=(ProxyDiagnostic)other;
return state==value.state&&code.equals(value.code)&&registeredBackends==value.registeredBackends&&pendingOperations==value.pendingOperations&&observedAt.equals(value.observedAt);
}
@Override public int hashCode(){
return Objects.hash(state,code,registeredBackends,pendingOperations,observedAt);
}
}
