package io.zartra.bedwars.proxy.api;

import java.util.Objects;

/** Immutable backend capacity without player identities. */ public final class CapacitySnapshot{
private final int maximum;
private final int active;
private final int reserved;
private CapacitySnapshot(final int maximum,final int active,final int reserved){
if(maximum<1||active<0||reserved<0||active+reserved>maximum){
throw new IllegalArgumentException("invalid capacity");
}
this.maximum=maximum;
this.active=active;
this.reserved=reserved;
}

/** Creates capacity. */ public static CapacitySnapshot of(final int maximum,final int active,final int reserved){
return new CapacitySnapshot(maximum,active,reserved);
}
/** Returns maximum. */ public int maximum(){
return maximum;
}
/** Returns active count. */ public int active(){
return active;
}
/** Returns reserved count. */ public int reserved(){
return reserved;
}
/** Returns available slots. */ public int available(){
return maximum-active-reserved;
}
@Override public boolean equals(final Object other){
if(!(other instanceof CapacitySnapshot)){
return false;
}
CapacitySnapshot value=(CapacitySnapshot)other;
return maximum==value.maximum&&active==value.active&&reserved==value.reserved;
}
@Override public int hashCode(){
return Objects.hash(maximum,active,reserved);
}
}
