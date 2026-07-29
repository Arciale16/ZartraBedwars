package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;

/** Sanitized backend health snapshot. */ public final class HealthSnapshot{
/** Health classification. */ public enum State{
HEALTHY,DEGRADED,UNHEALTHY}
private final State state;
private final String code;
private final Instant observedAt;
private HealthSnapshot(final State state,final String code,final Instant observedAt){
this.state=Objects.requireNonNull(state,"state");
this.code=ProxyContractValidation.token(code,"healthCode");
this.observedAt=Objects.requireNonNull(observedAt,"observedAt");
}

/** Creates a snapshot. */ public static HealthSnapshot of(final State state,final String code,final Instant observedAt){
return new HealthSnapshot(state,code,observedAt);
}
/** Returns state. */ public State state(){
return state;
}
/** Returns sanitized code. */ public String code(){
return code;
}
/** Returns observation time. */ public Instant observedAt(){
return observedAt;
}
@Override public boolean equals(final Object other){
if(!(other instanceof HealthSnapshot)){
return false;
}
HealthSnapshot value=(HealthSnapshot)other;
return state==value.state&&code.equals(value.code)&&observedAt.equals(value.observedAt);
}
@Override public int hashCode(){
return Objects.hash(state,code,observedAt);
}
}
