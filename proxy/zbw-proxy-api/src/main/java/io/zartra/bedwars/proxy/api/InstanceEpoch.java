package io.zartra.bedwars.proxy.api;

/** Positive monotonic backend boot epoch. */ public final class InstanceEpoch implements Comparable<InstanceEpoch>{
private final long value;
private InstanceEpoch(final long value){
if(value<1){
throw new IllegalArgumentException("epoch must be positive");
}
this.value=value;
}

/** Creates an epoch. */ public static InstanceEpoch of(final long value){
return new InstanceEpoch(value);
}
/** Returns the numeric epoch. */ public long value(){
return value;
}
/** Returns whether this epoch is newer. */ public boolean isNewerThan(final InstanceEpoch other){
return compareTo(other)>0;
}
/** Rejects a stale or repeated epoch. */ public void requireNewerThan(final InstanceEpoch other){
if(!isNewerThan(other)){
throw new IllegalArgumentException("stale backend epoch");
}
}
/** Requires an exact current epoch. */ public void requireCurrent(final InstanceEpoch current){
if(!equals(current)){
throw new IllegalArgumentException("stale backend epoch");
}
}
@Override public int compareTo(final InstanceEpoch other){
return Long.compare(value,other.value);
}
@Override public boolean equals(final Object other){
return other instanceof InstanceEpoch&&value==((InstanceEpoch)other).value;
}
@Override public int hashCode(){
return Long.hashCode(value);
}
@Override public String toString(){
return Long.toString(value);
}
}
