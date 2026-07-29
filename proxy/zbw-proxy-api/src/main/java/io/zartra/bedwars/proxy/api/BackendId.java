package io.zartra.bedwars.proxy.api;

/** Stable opaque backend identity. */ public final class BackendId implements Comparable<BackendId>{
private final String value;
private BackendId(final String value){
this.value=ProxyContractValidation.token(value,"backendId");
}

/** Creates an identity. */ public static BackendId of(final String value){
return new BackendId(value);
}
/** Returns the opaque value. */ public String value(){
return value;
}
@Override public int compareTo(final BackendId other){
return value.compareTo(other.value);
}
@Override public boolean equals(final Object other){
return other instanceof BackendId&&value.equals(((BackendId)other).value);
}
@Override public int hashCode(){
return value.hashCode();
}
@Override public String toString(){
return value;
}
}
