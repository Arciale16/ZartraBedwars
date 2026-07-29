package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProxyCoverageTest{
@Test void valueObjectsExposeDeterministicEquality(){
Instant now=Instant.EPOCH;
BackendCapabilities caps=BackendCapabilities.of(Arrays.asList("a","b"));
BackendRegistration registration=BackendRegistration.of(BackendId.of("node"),InstanceEpoch.of(1),caps,BackendStatus.OFFLINE,now);
Assertions.assertEquals(registration,BackendRegistration.of(registration.backendId(),registration.epoch(),caps,BackendStatus.OFFLINE,now));
Assertions.assertFalse(registration.acceptsRouting());
Assertions.assertEquals(registration.hashCode(),registration.hashCode());
CapacitySnapshot capacity=CapacitySnapshot.of(1,0,0);
Assertions.assertEquals(capacity,CapacitySnapshot.of(1,0,0));
HealthSnapshot health=HealthSnapshot.of(HealthSnapshot.State.DEGRADED,"slow",now);
Assertions.assertEquals(health,HealthSnapshot.of(HealthSnapshot.State.DEGRADED,"slow",now));
ProxyDiagnostic diagnostic=ProxyDiagnostic.of(DegradationState.LOCAL_ONLY,"partition",1,0,now);
Assertions.assertEquals(diagnostic,ProxyDiagnostic.of(DegradationState.LOCAL_ONLY,"partition",1,0,now));
Assertions.assertEquals(1,diagnostic.registeredBackends());
Assertions.assertEquals(0,diagnostic.pendingOperations());
Assertions.assertEquals(now,diagnostic.observedAt());
}

@Test void invalidInputsFailClosed(){
Instant now=Instant.EPOCH;
Assertions.assertThrows(IllegalArgumentException.class,()->CapacitySnapshot.of(0,0,0));
Assertions.assertThrows(IllegalArgumentException.class,()->CapacitySnapshot.of(1,-1,0));
Assertions.assertThrows(IllegalArgumentException.class,()->RoutingRequest.of(UUID.randomUUID(),"subject","audience",Collections.<String>emptyList(),now,now));
Assertions.assertThrows(NullPointerException.class,()->BackendCapabilities.of(Collections.singleton("a")).supportsAll(null));
Assertions.assertThrows(IllegalArgumentException.class,()->ProxyReservationId.parse("bad"));
Assertions.assertThrows(IllegalArgumentException.class,()->ProxyDiagnostic.of(DegradationState.NORMAL,"ok",-1,0,now));
}

@Test void resultAccessorsAndEnumsRemainStable(){
UUID request=UUID.randomUUID();
RoutingResult failed=RoutingResult.failed(request,RoutingResult.Status.UNAVAILABLE,"offline");
Assertions.assertEquals(request,failed.requestId());
Assertions.assertFalse(failed.epoch().isPresent());
Assertions.assertEquals("offline",failed.code());
ProxyReservationId id=ProxyReservationId.random();
ReservationResult expired=ReservationResult.failed(id,ReservationResult.Status.EXPIRED);
Assertions.assertEquals(id,expired.id());
Assertions.assertEquals(ReservationResult.Status.EXPIRED,expired.status());
Assertions.assertFalse(expired.epoch().isPresent());
TokenConsumptionResult invalid=TokenConsumptionResult.of(request,TokenConsumptionResult.Status.INVALID);
Assertions.assertFalse(invalid.consumed());
Assertions.assertEquals(request,invalid.tokenId());
Assertions.assertArrayEquals(new BackendStatus[]{
BackendStatus.ONLINE,BackendStatus.DRAINING,BackendStatus.UNHEALTHY,BackendStatus.OFFLINE}
,BackendStatus.values());
}

}
