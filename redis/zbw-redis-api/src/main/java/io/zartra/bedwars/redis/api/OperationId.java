package io.zartra.bedwars.redis.api;
import java.util.Objects;
import java.util.UUID;
/** Immutable collision-resistant coordination identity. */
public final class OperationId implements Comparable<OperationId> {
private final UUID value;
private OperationId(final UUID value){
this.value=Objects.requireNonNull(value,"value");
}
/** Creates a random identity. */ public static OperationId random(){
return new OperationId(UUID.randomUUID());
}
/** Parses canonical UUID text. */ public static OperationId parse(final String value){
return new OperationId(UUID.fromString(Objects.requireNonNull(value,"value")));
}
/** Returns UUID value. */ public UUID value(){
return value;
}
@Override public int compareTo(final OperationId other){
return value.compareTo(other.value);
}
@Override public boolean equals(final Object other){
return other instanceof OperationId&&value.equals(((OperationId)other).value);
}
@Override public int hashCode(){
return value.hashCode();
}
@Override public String toString(){
return value.toString();
}
}
