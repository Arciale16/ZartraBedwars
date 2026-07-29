package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProxyContractsTest{
private static final Instant NOW=Instant.ofEpochSecond(100);
private BackendCapabilities capabilities(){
return BackendCapabilities.of(Arrays.asList("mode.bedwars","region.eu"));
}
private BackendRegistration backend(final String id,final long epoch,final BackendStatus status){
return BackendRegistration.of(BackendId.of(id),InstanceEpoch.of(epoch),capabilities(),status,NOW);
}

@Test void identitiesCapabilitiesAndSnapshotsAreImmutable(){
BackendId id=BackendId.of("backend-01");
Assertions.assertEquals(id,BackendId.of("backend-01"));
Assertions.assertEquals(0,id.compareTo(BackendId.of("backend-01")));
Assertions.assertEquals("backend-01",id.value());
Assertions.assertEquals("backend-01",id.toString());
List<String> source=new ArrayList<String>(Arrays.asList("region.eu","mode.bedwars"));
BackendCapabilities value=BackendCapabilities.of(source);
source.clear();
Assertions.assertEquals(Arrays.asList("mode.bedwars","region.eu"),new ArrayList<String>(value.values()));
Assertions.assertTrue(value.supports("region.eu"));
Assertions.assertTrue(value.supportsAll(Collections.singleton("mode.bedwars")));
Assertions.assertFalse(value.supportsAll(Collections.singleton("mode.skywars")));
Assertions.assertThrows(UnsupportedOperationException.class,()->value.values().add("other"));
Assertions.assertThrows(IllegalArgumentException.class,()->BackendId.of("Player@Email"));
Assertions.assertThrows(IllegalArgumentException.class,()->BackendCapabilities.of(Collections.<String>emptyList()));
}

@Test void epochsRejectStaleBackends(){
InstanceEpoch first=InstanceEpoch.of(4);
InstanceEpoch next=InstanceEpoch.of(5);
Assertions.assertTrue(next.isNewerThan(first));
next.requireNewerThan(first);
first.requireCurrent(InstanceEpoch.of(4));
Assertions.assertThrows(IllegalArgumentException.class,()->InstanceEpoch.of(0));
Assertions.assertThrows(IllegalArgumentException.class,()->first.requireNewerThan(next));
Assertions.assertThrows(IllegalArgumentException.class,()->first.requireCurrent(next));
Assertions.assertEquals("4",first.toString());
}

@Test void heartbeatAndCapacityAreBounded(){
CapacitySnapshot capacity=CapacitySnapshot.of(20,10,3);
HealthSnapshot health=HealthSnapshot.of(HealthSnapshot.State.HEALTHY,"ok",NOW);
Heartbeat heartbeat=Heartbeat.of(BackendId.of("backend-01"),InstanceEpoch.of(1),capacity,health,NOW,NOW.plusSeconds(5));
Assertions.assertEquals(7,capacity.available());
Assertions.assertFalse(heartbeat.isExpiredAt(NOW.plusSeconds(4)));
Assertions.assertTrue(heartbeat.isExpiredAt(NOW.plusSeconds(5)));
Assertions.assertEquals(heartbeat,Heartbeat.of(heartbeat.backendId(),heartbeat.epoch(),capacity,health,NOW,NOW.plusSeconds(5)));
Assertions.assertThrows(IllegalArgumentException.class,()->CapacitySnapshot.of(5,4,2));
Assertions.assertThrows(IllegalArgumentException.class,()->Heartbeat.of(heartbeat.backendId(),heartbeat.epoch(),capacity,health,NOW,NOW));
}

@Test void routingIsDeterministicAndDrainingIsExcluded(){
RoutingRequest request=RoutingRequest.of(UUID.fromString("00000000-0000-0000-0000-000000000001"),"subject-01","proxy-main",Collections.singleton("mode.bedwars"),NOW,NOW.plusSeconds(5));
List<BackendRegistration> values=Arrays.asList(backend("backend-02",1,BackendStatus.ONLINE),backend("backend-01",1,BackendStatus.ONLINE),backend("backend-00",1,BackendStatus.DRAINING));
DestinationSelector selector=(registrations,input)->registrations.stream().filter(input::accepts).sorted(Comparator.comparing(BackendRegistration::backendId)).findFirst();
Optional<BackendRegistration> selected=selector.select(values,request);
Assertions.assertEquals(BackendId.of("backend-01"),selected.get().backendId());
Assertions.assertFalse(request.accepts(backend("backend-00",1,BackendStatus.DRAINING)));
RoutingResult routed=RoutingResult.routed(request.requestId(),selected.get().backendId(),selected.get().epoch());
Assertions.assertEquals(RoutingResult.Status.ROUTED,routed.status());
Assertions.assertTrue(routed.backendId().isPresent());
Assertions.assertEquals(RoutingResult.failed(request.requestId(),RoutingResult.Status.NO_CAPACITY,"no-capacity"),RoutingResult.failed(request.requestId(),RoutingResult.Status.NO_CAPACITY,"no-capacity"));
Assertions.assertThrows(IllegalArgumentException.class,()->RoutingResult.failed(request.requestId(),RoutingResult.Status.ROUTED,"bad"));
}

@Test void reservationsAreBoundedAndEpochChecked(){
ProxyReservationId id=ProxyReservationId.parse("00000000-0000-0000-0000-000000000010");
ReservationRequest request=ReservationRequest.of(id,BackendId.of("backend-01"),InstanceEpoch.of(3),"subject-01","proxy-main",NOW,NOW.plusSeconds(15));
request.requireCurrentEpoch(InstanceEpoch.of(3));
Assertions.assertThrows(IllegalArgumentException.class,()->request.requireCurrentEpoch(InstanceEpoch.of(4)));
Assertions.assertThrows(IllegalArgumentException.class,()->ReservationRequest.of(id,request.backendId(),request.epoch(),request.subjectReference(),request.audience(),NOW,NOW.plusSeconds(16)));
ReservationResult result=ReservationResult.reserved(id,request.backendId(),request.epoch(),request.expiresAt());
Assertions.assertTrue(result.expiresAt().isPresent());
Assertions.assertFalse(ReservationResult.failed(id,ReservationResult.Status.CONFLICT).backendId().isPresent());
Assertions.assertThrows(IllegalArgumentException.class,()->ReservationResult.failed(id,ReservationResult.Status.RESERVED));
}

@Test void tokensExpireRejectAudienceAndPreventDuplicateOutcome(){
UUID tokenId=UUID.fromString("00000000-0000-0000-0000-000000000020");
TransferToken token=TransferToken.of(tokenId,ProxyReservationId.random(),BackendId.of("backend-01"),InstanceEpoch.of(7),"backend-01",NOW,NOW.plusSeconds(15));
Assertions.assertTrue(token.hasAudience("backend-01"));
Assertions.assertEquals(TokenConsumptionResult.Status.CONSUMED,token.evaluateConsumption("backend-01",InstanceEpoch.of(7),NOW,false).status());
Assertions.assertEquals(TokenConsumptionResult.Status.DUPLICATE,token.evaluateConsumption("backend-01",InstanceEpoch.of(7),NOW,true).status());
Assertions.assertEquals(TokenConsumptionResult.Status.EXPIRED,token.evaluateConsumption("backend-01",InstanceEpoch.of(7),NOW.plusSeconds(15),false).status());
Assertions.assertEquals(TokenConsumptionResult.Status.WRONG_AUDIENCE,token.evaluateConsumption("backend-02",InstanceEpoch.of(7),NOW,false).status());
Assertions.assertEquals(TokenConsumptionResult.Status.STALE_EPOCH,token.evaluateConsumption("backend-01",InstanceEpoch.of(8),NOW,false).status());
Assertions.assertThrows(IllegalArgumentException.class,()->TransferToken.of(tokenId,token.reservationId(),token.backendId(),token.epoch(),token.audience(),NOW,NOW.plusSeconds(16)));
}

@Test void diagnosticsAndProtocolArePrivacySafe(){
ProtocolVersion version=ProtocolVersion.of(1,2);
Assertions.assertTrue(version.compatibleWith(ProtocolVersion.of(1,9)));
Assertions.assertFalse(version.compatibleWith(ProtocolVersion.of(2,0)));
Assertions.assertTrue(version.compareTo(ProtocolVersion.of(1,1))>0);
Assertions.assertEquals("1.2",version.toString());
ProxyDiagnostic diagnostic=ProxyDiagnostic.of(DegradationState.RESERVATIONS_PAUSED,"redis-unavailable",2,3,NOW);
Assertions.assertEquals("redis-unavailable",diagnostic.code());
Assertions.assertFalse(diagnostic.toString().contains("password"));
Assertions.assertThrows(IllegalArgumentException.class,()->ProxyDiagnostic.of(DegradationState.NORMAL,"secret=value",0,0,NOW));
Assertions.assertThrows(IllegalArgumentException.class,()->ProtocolVersion.of(0,0));
}

}
