package io.zartra.bedwars.proxy.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable transfer-token claim containing no bearer secret. */ public final class TransferToken{
private final UUID tokenId;
private final ProxyReservationId reservationId;
private final BackendId backendId;
private final InstanceEpoch epoch;
private final String audience;
private final Instant issuedAt;
private final Instant expiresAt;
private TransferToken(final UUID tokenId,final ProxyReservationId reservationId,final BackendId backend,final InstanceEpoch epoch,final String audience,final Instant issuedAt,final Instant expiresAt){
this.tokenId=Objects.requireNonNull(tokenId,"tokenId");
this.reservationId=Objects.requireNonNull(reservationId,"reservationId");
backendId=Objects.requireNonNull(backend,"backendId");
this.epoch=Objects.requireNonNull(epoch,"epoch");
this.audience=ProxyContractValidation.token(audience,"audience");
this.issuedAt=Objects.requireNonNull(issuedAt,"issuedAt");
this.expiresAt=Objects.requireNonNull(expiresAt,"expiresAt");
Duration lifetime=Duration.between(issuedAt,expiresAt);
if(lifetime.isNegative()||lifetime.isZero()||lifetime.compareTo(ReservationRequest.MAX_LIFETIME)>0){
throw new IllegalArgumentException("transfer token lifetime must be within 15 seconds");
}
}

/** Creates token claims. */ public static TransferToken of(final UUID id,final ProxyReservationId reservation,final BackendId backend,final InstanceEpoch epoch,final String audience,final Instant issuedAt,final Instant expiresAt){
return new TransferToken(id,reservation,backend,epoch,audience,issuedAt,expiresAt);
}
/** Returns token ID. */ public UUID tokenId(){
return tokenId;
}
/** Returns reservation ID. */ public ProxyReservationId reservationId(){
return reservationId;
}
/** Returns backend ID. */ public BackendId backendId(){
return backendId;
}
/** Returns epoch. */ public InstanceEpoch epoch(){
return epoch;
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
/** Tests expiry. */ public boolean isExpiredAt(final Instant now){
return !Objects.requireNonNull(now,"now").isBefore(expiresAt);
}
/** Tests exact audience. */ public boolean hasAudience(final String expected){
return audience.equals(ProxyContractValidation.token(expected,"audience"));
}
/** Evaluates consumption;
 atomic single-use storage remains an adapter responsibility. */ public TokenConsumptionResult evaluateConsumption(final String expected,final InstanceEpoch current,final Instant now,final boolean consumed){
if(consumed){
return TokenConsumptionResult.of(tokenId,TokenConsumptionResult.Status.DUPLICATE);
}
if(isExpiredAt(now)){
return TokenConsumptionResult.of(tokenId,TokenConsumptionResult.Status.EXPIRED);
}
if(!hasAudience(expected)){
return TokenConsumptionResult.of(tokenId,TokenConsumptionResult.Status.WRONG_AUDIENCE);
}
if(!epoch.equals(current)){
return TokenConsumptionResult.of(tokenId,TokenConsumptionResult.Status.STALE_EPOCH);
}
return TokenConsumptionResult.of(tokenId,TokenConsumptionResult.Status.CONSUMED);
}
@Override public boolean equals(final Object other){
if(!(other instanceof TransferToken)){
return false;
}
TransferToken value=(TransferToken)other;
return tokenId.equals(value.tokenId)&&reservationId.equals(value.reservationId)&&backendId.equals(value.backendId)&&epoch.equals(value.epoch)&&audience.equals(value.audience)&&issuedAt.equals(value.issuedAt)&&expiresAt.equals(value.expiresAt);
}
@Override public int hashCode(){
return Objects.hash(tokenId,reservationId,backendId,epoch,audience,issuedAt,expiresAt);
}
}
