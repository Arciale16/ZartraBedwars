package io.zartra.bedwars.redis.api;
import java.time.Instant;
import java.util.Objects;
/** Sanitized health snapshot without endpoints, credentials or payloads. */ public final class RedisHealth{
private final RedisAvailability availability;
private final DegradationMode mode;
private final String code;
private final int pending;
private final Instant observedAt;
private RedisHealth(final RedisAvailability a,final DegradationMode m,final String c,final int p,final Instant o){
availability=Objects.requireNonNull(a,"availability");
mode=Objects.requireNonNull(m,"mode");
code=RedisContractValidation.token(c,"diagnosticCode");
if (p<0) {
throw new IllegalArgumentException("negative pending count");
}
pending=p;
observedAt=Objects.requireNonNull(o,"observedAt");
if((a==RedisAvailability.AVAILABLE)!=(m==DegradationMode.NORMAL)) {
throw new IllegalArgumentException("inconsistent health mode");
}
}
/** Creates snapshot. */ public static RedisHealth of(final RedisAvailability a,final DegradationMode m,final String c,final int p,final Instant o){
return new RedisHealth(a,m,c,p,o);
}
/** Returns availability. */ public RedisAvailability availability(){
return availability;
}
/** Returns mode. */ public DegradationMode mode(){
return mode;
}
/** Returns sanitized code. */ public String diagnosticCode(){
return code;
}
/** Returns pending count. */ public int pendingOperations(){
return pending;
}
/** Returns observation time. */ public Instant observedAt(){
return observedAt;
}
@Override public boolean equals(final Object x){
if(!(x instanceof RedisHealth)) {
return false;
}
RedisHealth y=(RedisHealth)x;
return availability==y.availability&&mode==y.mode&&code.equals(y.code)&&pending==y.pending&&observedAt.equals(y.observedAt);
}
@Override public int hashCode(){
return Objects.hash(availability,mode,code,pending,observedAt);
}
}
