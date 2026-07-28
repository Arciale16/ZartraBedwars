package io.zartra.bedwars.redis.api;
import org.junit.jupiter.api.Assertions;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class RedisContractsTest {
private RedisNamespace namespace(){
return RedisNamespace.of("install","prod","coordination",SchemaVersion.of(1,0));
}
@Test void keysAreIsolatedImmutableAndDeterministic(){
RedisKey key=RedisKey.of(namespace(),"lease","node-01");
Assertions.assertEquals("install:prod:coordination:v1.0:lease:node-01",key.qualified());
Assertions.assertEquals(key,RedisKey.of(namespace(),"lease","node-01"));
Assertions.assertThrows(IllegalArgumentException.class,()->RedisKey.of(namespace(),"lease","player@example.com"));
Assertions.assertThrows(IllegalArgumentException.class,()->RedisNamespace.of("Install","prod","x",SchemaVersion.of(1,0)));
}
@Test void versionsAndStreamsRejectInvalidOrdering(){
Assertions.assertThrows(IllegalArgumentException.class,()->SchemaVersion.of(0,0));
Assertions.assertTrue(SchemaVersion.of(2,0).compareTo(SchemaVersion.of(1,9))>0);
StreamCursor cursor=StreamCursor.after(RedisKey.of(namespace(),"stream","events"),StreamId.of(10,2));
Assertions.assertEquals(StreamId.of(11,0),cursor.advance(StreamId.of(11,0)).lastConsumed());
Assertions.assertThrows(IllegalArgumentException.class,()->cursor.advance(StreamId.of(10,2)));
Assertions.assertEquals(StreamId.of(10,2),StreamId.parse("10-2"));
Assertions.assertThrows(IllegalArgumentException.class,()->StreamId.parse("bad"));
}
@Test void fencingRejectsStaleEpochs(){
FencingToken current=FencingToken.of(4);
Assertions.assertTrue(FencingToken.of(5).isNewerThan(current));
Assertions.assertThrows(IllegalArgumentException.class,()->current.requireNewerThan(current));
LeaseState lease=LeaseState.of(LeaseId.random(),"node-01",current,Instant.EPOCH,Instant.EPOCH.plusSeconds(5),LeaseState.Status.ACTIVE);
Assertions.assertThrows(IllegalArgumentException.class,()->lease.requireSuccessor(FencingToken.of(3)));
lease.requireSuccessor(FencingToken.of(5));
}
@Test void reservationsAreBoundedAndConsistent(){
Instant now=Instant.ofEpochSecond(10);
ReservationId id=ReservationId.random();
ReservationRequest request=ReservationRequest.of(id,OperationId.random(),RedisKey.of(namespace(),"reservation","match-1"),"node-01",now,now.plusSeconds(5));
Assertions.assertEquals(id,request.id());
ReservationResult acquired=ReservationResult.acquired(id,now.plusSeconds(5),FencingToken.of(1));
Assertions.assertTrue(acquired.fencingToken().isPresent());
Assertions.assertFalse(ReservationResult.failed(id,ReservationResult.Status.CONFLICT).expiresAt().isPresent());
Assertions.assertThrows(IllegalArgumentException.class,()->ReservationRequest.of(id,OperationId.random(),request.resource(),"node",now,now));
Assertions.assertThrows(IllegalArgumentException.class,()->ReservationResult.failed(id,ReservationResult.Status.ACQUIRED));
}
@Test void invalidationsDeduplicateAndHealthDoesNotExposeSecrets(){
OperationId operation=OperationId.parse("00000000-0000-0000-0000-000000000001");
DeduplicationKey dedupe=DeduplicationKey.of(namespace(),operation);
Assertions.assertEquals(dedupe,DeduplicationKey.of(namespace(),operation));
CacheInvalidation event=CacheInvalidation.of(RedisKey.of(namespace(),"cache","rank-1"),InvalidationVersion.of(2),operation,Instant.EPOCH);
Assertions.assertTrue(event.version().isNewerThan(InvalidationVersion.of(1)));
RedisHealth health=RedisHealth.of(RedisAvailability.DEGRADED,DegradationMode.LOCAL_ONLY,"timeout",3,Instant.EPOCH);
Assertions.assertEquals("timeout",health.diagnosticCode());
Assertions.assertFalse(health.toString().contains("password"));
Assertions.assertThrows(IllegalArgumentException.class,()->RedisHealth.of(RedisAvailability.AVAILABLE,DegradationMode.READ_ONLY,"bad",0,Instant.EPOCH));
}
@Test void identityValuesHaveStableEquality(){
UUID value=UUID.randomUUID();
Assertions.assertEquals(OperationId.parse(value.toString()),OperationId.parse(value.toString()));
Assertions.assertEquals(ReservationId.parse(value.toString()),ReservationId.parse(value.toString()));
Assertions.assertEquals(LeaseId.parse(value.toString()),LeaseId.parse(value.toString()));
Assertions.assertNotEquals(OperationId.random(),OperationId.random());
}
}
