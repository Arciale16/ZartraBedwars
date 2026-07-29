package io.zartra.bedwars.proxy.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Bounded proxy reservation request tied to a backend epoch. */ public final class ReservationRequest{
/** Maximum transfer reservation lifetime. */ public static final Duration MAX_LIFETIME=Duration.ofSeconds(15);
private final ProxyReservationId id;
private final BackendId backendId;
private final InstanceEpoch epoch;
private final String subjectReference;
private final String audience;
private final Instant issuedAt;
private final Instant expiresAt;
private ReservationRequest(final ProxyReservationId id,final BackendId backend,final InstanceEpoch epoch,final String subject,final String audience,final Instant issuedAt,final Instant expiresAt){
this.id=Objects.requireNonNull(id,"id");
backendId=Objects.requireNonNull(backend,"backendId");
this.epoch=Objects.requireNonNull(epoch,"epoch");
subjectReference=ProxyContractValidation.token(subject,"subjectReference");
this.audience=ProxyContractValidation.token(audience,"audience");
this.issuedAt=Objects.requireNonNull(issuedAt,"issuedAt");
this.expiresAt=Objects.requireNonNull(expiresAt,"expiresAt");
Duration lifetime=Duration.between(issuedAt,expiresAt);
if(lifetime.isNegative()||lifetime.isZero()||lifetime.compareTo(MAX_LIFETIME)>0){
throw new IllegalArgumentException("reservation lifetime must be within 15 seconds");
}
}

/** Creates a request. */ public static ReservationRequest of(final ProxyReservationId id,final BackendId backend,final InstanceEpoch epoch,final String subject,final String audience,final Instant issuedAt,final Instant expiresAt){
return new ReservationRequest(id,backend,epoch,subject,audience,issuedAt,expiresAt);
}
/** Returns ID. */ public ProxyReservationId id(){
return id;
}
/** Returns backend. */ public BackendId backendId(){
return backendId;
}
/** Returns epoch. */ public InstanceEpoch epoch(){
return epoch;
}
/** Returns opaque subject. */ public String subjectReference(){
return subjectReference;
}
/** Returns audience. */ public String audience(){
return audience;
}
/** Returns issue time. */ public Instant issuedAt(){
return issuedAt;
}
/** Returns expiry. */ public Instant expiresAt(){
return expiresAt;
}
/** Rejects stale epoch. */ public void requireCurrentEpoch(final InstanceEpoch current){
epoch.requireCurrent(current);
}
@Override public boolean equals(final Object other){
if(!(other instanceof ReservationRequest)){
return false;
}
ReservationRequest value=(ReservationRequest)other;
return id.equals(value.id)&&backendId.equals(value.backendId)&&epoch.equals(value.epoch)&&subjectReference.equals(value.subjectReference)&&audience.equals(value.audience)&&issuedAt.equals(value.issuedAt)&&expiresAt.equals(value.expiresAt);
}
@Override public int hashCode(){
return Objects.hash(id,backendId,epoch,subjectReference,audience,issuedAt,expiresAt);
}
}
