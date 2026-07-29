package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProxyEqualityBranchTest{
private static final Instant NOW=Instant.ofEpochSecond(50);
private static final BackendId BACKEND=BackendId.of("backend");
private static final InstanceEpoch EPOCH=InstanceEpoch.of(1);
private static final ProxyReservationId ID=ProxyReservationId.parse("00000000-0000-0000-0000-000000000001");

@Test void reservationEqualityChecksEveryField(){
ReservationRequest value=request(ID,BACKEND,EPOCH,"subject","audience",NOW,NOW.plusSeconds(5));
Assertions.assertFalse(value.equals(request(ProxyReservationId.random(),BACKEND,EPOCH,"subject","audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(request(ID,BackendId.of("other"),EPOCH,"subject","audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(request(ID,BACKEND,InstanceEpoch.of(2),"subject","audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(request(ID,BACKEND,EPOCH,"other","audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(request(ID,BACKEND,EPOCH,"subject","other",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(request(ID,BACKEND,EPOCH,"subject","audience",NOW.plusSeconds(1),NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(request(ID,BACKEND,EPOCH,"subject","audience",NOW,NOW.plusSeconds(6))));
Assertions.assertEquals(value,request(ID,BACKEND,EPOCH,"subject","audience",NOW,NOW.plusSeconds(5)));
}
private ReservationRequest request(final ProxyReservationId id,final BackendId backend,final InstanceEpoch epoch,final String subject,final String audience,final Instant issued,final Instant expires){
return ReservationRequest.of(id,backend,epoch,subject,audience,issued,expires);
}

@Test void transferEqualityChecksEveryField(){
UUID tokenId=UUID.fromString("00000000-0000-0000-0000-000000000002");
TransferToken value=token(tokenId,ID,BACKEND,EPOCH,"audience",NOW,NOW.plusSeconds(5));
Assertions.assertFalse(value.equals(token(UUID.randomUUID(),ID,BACKEND,EPOCH,"audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(token(tokenId,ProxyReservationId.random(),BACKEND,EPOCH,"audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(token(tokenId,ID,BackendId.of("other"),EPOCH,"audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(token(tokenId,ID,BACKEND,InstanceEpoch.of(2),"audience",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(token(tokenId,ID,BACKEND,EPOCH,"other",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(token(tokenId,ID,BACKEND,EPOCH,"audience",NOW.plusSeconds(1),NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(token(tokenId,ID,BACKEND,EPOCH,"audience",NOW,NOW.plusSeconds(6))));
Assertions.assertEquals(value,token(tokenId,ID,BACKEND,EPOCH,"audience",NOW,NOW.plusSeconds(5)));
}
private TransferToken token(final UUID tokenId,final ProxyReservationId id,final BackendId backend,final InstanceEpoch epoch,final String audience,final Instant issued,final Instant expires){
return TransferToken.of(tokenId,id,backend,epoch,audience,issued,expires);
}

@Test void routingRequestEqualityChecksOrderedFields(){
UUID id=UUID.fromString("00000000-0000-0000-0000-000000000003");
RoutingRequest value=route(id,"subject","audience","mode",NOW,NOW.plusSeconds(5));
Assertions.assertFalse(value.equals(route(id,"other","audience","mode",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(route(id,"subject","other","mode",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(route(id,"subject","audience","other",NOW,NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(route(id,"subject","audience","mode",NOW.plusSeconds(1),NOW.plusSeconds(5))));
Assertions.assertFalse(value.equals(route(id,"subject","audience","mode",NOW,NOW.plusSeconds(6))));
}
private RoutingRequest route(final UUID id,final String subject,final String audience,final String capability,final Instant issued,final Instant deadline){
return RoutingRequest.of(id,subject,audience,Collections.singleton(capability),issued,deadline);
}

}
