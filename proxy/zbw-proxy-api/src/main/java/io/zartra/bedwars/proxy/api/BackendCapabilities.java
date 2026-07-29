package io.zartra.bedwars.proxy.api;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/** Immutable declared backend capability set. */ public final class BackendCapabilities{
private final SortedSet<String> values;
private BackendCapabilities(final Collection<String> values){
if(values==null||values.isEmpty()){
throw new IllegalArgumentException("capabilities must not be empty");
}
TreeSet<String> copy=new TreeSet<String>();
for(String value:values){
copy.add(ProxyContractValidation.token(value,"capability"));
}
this.values=Collections.unmodifiableSortedSet(copy);
}

/** Creates capabilities. */ public static BackendCapabilities of(final Collection<String> values){
return new BackendCapabilities(values);
}
/** Returns a defensive immutable view. */ public SortedSet<String> values(){
return values;
}
/** Tests a declared capability. */ public boolean supports(final String capability){
return values.contains(ProxyContractValidation.token(capability,"capability"));
}
/** Tests every required capability. */ public boolean supportsAll(final Collection<String> required){
if(required==null){
throw new NullPointerException("required");
}
for(String capability:required){
if(!supports(capability)){
return false;
}
}
return true;
}
@Override public boolean equals(final Object other){
return other instanceof BackendCapabilities&&values.equals(((BackendCapabilities)other).values);
}
@Override public int hashCode(){
return values.hashCode();
}
}
