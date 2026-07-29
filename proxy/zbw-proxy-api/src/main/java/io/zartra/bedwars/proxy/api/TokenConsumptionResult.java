package io.zartra.bedwars.proxy.api;

import java.util.Objects;
import java.util.UUID;

/** Result of atomic transfer-token consumption. */ public final class TokenConsumptionResult{
/** Consumption outcome. */ public enum Status{
CONSUMED,DUPLICATE,EXPIRED,WRONG_AUDIENCE,STALE_EPOCH,INVALID}
private final UUID tokenId;
private final Status status;
private TokenConsumptionResult(final UUID id,final Status status){
tokenId=Objects.requireNonNull(id,"tokenId");
this.status=Objects.requireNonNull(status,"status");
}
/** Creates a result. */ public static TokenConsumptionResult of(final UUID id,final Status status){
return new TokenConsumptionResult(id,status);
}
/** Returns token ID. */ public UUID tokenId(){
return tokenId;
}
/** Returns status. */ public Status status(){
return status;
}
/** Tests committed consumption. */ public boolean consumed(){
return status==Status.CONSUMED;
}
@Override public boolean equals(final Object other){
if(!(other instanceof TokenConsumptionResult)){
return false;
}
TokenConsumptionResult value=(TokenConsumptionResult)other;
return tokenId.equals(value.tokenId)&&status==value.status;
}
@Override public int hashCode(){
return Objects.hash(tokenId,status);
}
}
