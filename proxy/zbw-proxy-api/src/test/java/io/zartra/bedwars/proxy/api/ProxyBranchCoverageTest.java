package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProxyBranchCoverageTest{
private static final Instant NOW=Instant.ofEpochSecond(20);
private BackendId backend(){
return BackendId.of("backend");
}
private InstanceEpoch epoch(){
return InstanceEpoch.of(1);
}
private BackendCapabilities caps(){
return BackendCapabilities.of(Collections.singleton("mode"));
}

@Test void validationBranchesRejectEveryMalformedBoundary(){
Assertions.assertThrows(IllegalArgumentException.class,()->BackendCapabilities.of(null));
Assertions.assertThrows(IllegalArgumentException.class,()->BackendCapabilities.of(Arrays.asList("ok","BAD")));
Assertions.assertThrows(IllegalArgumentException.class,()->CapacitySnapshot.of(2,0,-1));
Assertions.assertThrows(IllegalArgumentException.class,()->CapacitySnapshot.of(2,3,0));
Assertions.assertThrows(IllegalArgumentException.class,()->ProxyDiagnostic.of(DegradationState.NORMAL,"ok",0,-1,NOW));
Assertions.assertThrows(NullPointerException.class,()->RoutingRequest.of(UUID.randomUUID(),"subject","audience",null,NOW,NOW.plusSeconds(1)));
Assertions.assertThrows(IllegalArgumentException.class,()->RoutingRequest.of(UUID.randomUUID(),"subject","audience",Collections.singleton("BAD"),NOW,NOW.plusSeconds(1)));
Assertions.assertThrows(IllegalArgumentException.class,()->ReservationRequest.of(ProxyReservationId.random(),backend(),epoch(),"subject","audience",NOW,NOW));
Assertions.assertThrows(IllegalArgumentException.class,()->ReservationRequest.of(ProxyReservationId.random(),backend(),epoch(),"subject","audience",NOW,NOW.minusSeconds(1)));
Assertions.assertThrows(IllegalArgumentException.class,()->TransferToken.of(UUID.randomUUID(),ProxyReservationId.random(),backend(),epoch(),"audience",NOW,NOW));
Assertions.assertThrows(IllegalArgumentException.class,()->TransferToken.of(UUID.randomUUID(),ProxyReservationId.random(),backend(),epoch(),"audience",NOW,NOW.minusSeconds(1)));
}

@Test void heartbeatAndProtocolCoverTimeAndOrderingBranches(){
HealthSnapshot future=HealthSnapshot.of(HealthSnapshot.State.HEALTHY,"ok",NOW.plusSeconds(1));
Assertions.assertThrows(IllegalArgumentException.class,()->Heartbeat.of(backend(),epoch(),CapacitySnapshot.of(1,0,0),future,NOW,NOW.plusSeconds(2)));
ProtocolVersion one=ProtocolVersion.of(1,0);
Assertions.assertTrue(one.compareTo(ProtocolVersion.of(2,0))<0);
Assertions.assertTrue(ProtocolVersion.of(2,0).compareTo(one)>0);
Assertions.assertEquals(0,one.compareTo(ProtocolVersion.of(1,0)));
Assertions.assertThrows(IllegalArgumentException.class,()->ProtocolVersion.of(1,-1));
}

@Test void equalityContractsRejectOtherTypesAndDifferentValues(){
BackendId id=backend();
Assertions.assertFalse(id.equals("backend"));
Assertions.assertFalse(id.equals(BackendId.of("other")));
ProxyReservationId reservation=ProxyReservationId.random();
Assertions.assertFalse(reservation.equals("reservation"));
Assertions.assertFalse(reservation.equals(ProxyReservationId.random()));
BackendCapabilities capabilities=caps();
Assertions.assertFalse(capabilities.equals("mode"));
Assertions.assertFalse(capabilities.equals(BackendCapabilities.of(Collections.singleton("other"))));
CapacitySnapshot capacity=CapacitySnapshot.of(2,1,0);
Assertions.assertFalse(capacity.equals("capacity"));
Assertions.assertFalse(capacity.equals(CapacitySnapshot.of(2,0,0)));
HealthSnapshot health=HealthSnapshot.of(HealthSnapshot.State.HEALTHY,"ok",NOW);
Assertions.assertFalse(health.equals("health"));
Assertions.assertFalse(health.equals(HealthSnapshot.of(HealthSnapshot.State.UNHEALTHY,"bad",NOW)));
BackendRegistration registration=BackendRegistration.of(id,epoch(),capabilities,BackendStatus.ONLINE,NOW);
Assertions.assertFalse(registration.equals("registration"));
Assertions.assertFalse(registration.equals(BackendRegistration.of(BackendId.of("other"),epoch(),capabilities,BackendStatus.ONLINE,NOW)));
Heartbeat heartbeat=Heartbeat.of(id,epoch(),capacity,health,NOW,NOW.plusSeconds(1));
Assertions.assertFalse(heartbeat.equals("heartbeat"));
Assertions.assertFalse(heartbeat.equals(Heartbeat.of(id,epoch(),capacity,health,NOW,NOW.plusSeconds(2))));
}

@Test void resultsAndRequestsCoverFailureEqualityBranches(){
UUID requestId=UUID.randomUUID();
RoutingRequest request=RoutingRequest.of(requestId,"subject","audience",Collections.singleton("mode"),NOW,NOW.plusSeconds(1));
Assertions.assertFalse(request.equals("request"));
Assertions.assertFalse(request.equals(RoutingRequest.of(UUID.randomUUID(),"subject","audience",Collections.singleton("mode"),NOW,NOW.plusSeconds(1))));
RoutingResult failed=RoutingResult.failed(requestId,RoutingResult.Status.REJECTED,"rejected");
Assertions.assertFalse(failed.equals("result"));
Assertions.assertFalse(failed.equals(RoutingResult.failed(UUID.randomUUID(),RoutingResult.Status.REJECTED,"rejected")));
ProxyReservationId reservationId=ProxyReservationId.random();
ReservationResult reservation=ReservationResult.failed(reservationId,ReservationResult.Status.REJECTED);
Assertions.assertFalse(reservation.equals("result"));
Assertions.assertFalse(reservation.equals(ReservationResult.failed(ProxyReservationId.random(),ReservationResult.Status.REJECTED)));
TokenConsumptionResult token=TokenConsumptionResult.of(requestId,TokenConsumptionResult.Status.CONSUMED);
Assertions.assertTrue(token.consumed());
Assertions.assertFalse(token.equals("token"));
Assertions.assertFalse(token.equals(TokenConsumptionResult.of(UUID.randomUUID(),TokenConsumptionResult.Status.CONSUMED)));
}

@Test void transferEqualityAndAccessorsRemainDeterministic(){
UUID id=UUID.randomUUID();
ProxyReservationId reservation=ProxyReservationId.random();
TransferToken token=TransferToken.of(id,reservation,backend(),epoch(),"audience",NOW,NOW.plusSeconds(1));
Assertions.assertEquals(id,token.tokenId());
Assertions.assertEquals(reservation,token.reservationId());
Assertions.assertEquals(backend(),token.backendId());
Assertions.assertEquals(epoch(),token.epoch());
Assertions.assertEquals(NOW,token.issuedAt());
Assertions.assertEquals(NOW.plusSeconds(1),token.expiresAt());
Assertions.assertFalse(token.equals("token"));
Assertions.assertFalse(token.equals(TransferToken.of(UUID.randomUUID(),reservation,backend(),epoch(),"audience",NOW,NOW.plusSeconds(1))));
}

}
