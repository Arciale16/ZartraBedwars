package io.zartra.bedwars.redis.api;
import java.util.Objects;
import java.util.UUID;
/** Immutable collision-resistant coordination identity. */
public final class ReservationId implements Comparable<ReservationId> {
private final UUID value;
private ReservationId(final UUID value){
this.value=Objects.requireNonNull(value,"value");
}
/** Creates a random identity. */ public static ReservationId random(){
return new ReservationId(UUID.randomUUID());
}
/** Parses canonical UUID text. */ public static ReservationId parse(final String value){
return new ReservationId(UUID.fromString(Objects.requireNonNull(value,"value")));
}
/** Returns UUID value. */ public UUID value(){
return value;
}
@Override public int compareTo(final ReservationId other){
return value.compareTo(other.value);
}
@Override public boolean equals(final Object other){
return other instanceof ReservationId&&value.equals(((ReservationId)other).value);
}
@Override public int hashCode(){
return value.hashCode();
}
@Override public String toString(){
return value.toString();
}
}
