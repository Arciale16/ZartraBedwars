package io.zartra.bedwars.redis.api;
import java.util.Objects;
import java.util.UUID;
/** Immutable collision-resistant coordination identity. */
public final class LeaseId implements Comparable<LeaseId> {
private final UUID value;
private LeaseId(final UUID value){
this.value=Objects.requireNonNull(value,"value");
}
/** Creates a random identity. */ public static LeaseId random(){
return new LeaseId(UUID.randomUUID());
}
/** Parses canonical UUID text. */ public static LeaseId parse(final String value){
return new LeaseId(UUID.fromString(Objects.requireNonNull(value,"value")));
}
/** Returns UUID value. */ public UUID value(){
return value;
}
@Override public int compareTo(final LeaseId other){
return value.compareTo(other.value);
}
@Override public boolean equals(final Object other){
return other instanceof LeaseId&&value.equals(((LeaseId)other).value);
}
@Override public int hashCode(){
return value.hashCode();
}
@Override public String toString(){
return value.toString();
}
}
