package io.zartra.bedwars.proxy.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque proxy reservation identity. */ public final class ProxyReservationId{
private final UUID value;
private ProxyReservationId(final UUID value){
this.value=Objects.requireNonNull(value,"value");
}
/** Creates a random identity. */ public static ProxyReservationId random(){
return new ProxyReservationId(UUID.randomUUID());
}
/** Parses an identity. */ public static ProxyReservationId parse(final String value){
return new ProxyReservationId(UUID.fromString(value));
}
/** Returns UUID value. */ public UUID value(){
return value;
}
@Override public boolean equals(final Object other){
return other instanceof ProxyReservationId&&value.equals(((ProxyReservationId)other).value);
}
@Override public int hashCode(){
return value.hashCode();
}
@Override public String toString(){
return value.toString();
}
}
