package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/** Immutable privacy-safe routing request. */ public final class RoutingRequest{
private final UUID requestId;
private final String subjectReference;
private final String audience;
private final SortedSet<String> requiredCapabilities;
private final Instant requestedAt;
private final Instant deadline;
private RoutingRequest(final UUID id,final String subject,final String audience,final Collection<String> required,final Instant requestedAt,final Instant deadline){
requestId=Objects.requireNonNull(id,"requestId");
subjectReference=ProxyContractValidation.token(subject,"subjectReference");
this.audience=ProxyContractValidation.token(audience,"audience");
if(required==null){
throw new NullPointerException("requiredCapabilities");
}
TreeSet<String> copy=new TreeSet<String>();
for(String capability:required){
copy.add(ProxyContractValidation.token(capability,"capability"));
}
requiredCapabilities=Collections.unmodifiableSortedSet(copy);
this.requestedAt=Objects.requireNonNull(requestedAt,"requestedAt");
this.deadline=Objects.requireNonNull(deadline,"deadline");
if(!deadline.isAfter(requestedAt)){
throw new IllegalArgumentException("deadline must follow request");
}
}

/** Creates a request. */ public static RoutingRequest of(final UUID id,final String subject,final String audience,final Collection<String> required,final Instant requestedAt,final Instant deadline){
return new RoutingRequest(id,subject,audience,required,requestedAt,deadline);
}
/** Returns request ID. */ public UUID requestId(){
return requestId;
}
/** Returns opaque subject. */ public String subjectReference(){
return subjectReference;
}
/** Returns audience. */ public String audience(){
return audience;
}
/** Returns required capabilities. */ public SortedSet<String> requiredCapabilities(){
return requiredCapabilities;
}
/** Returns request time. */ public Instant requestedAt(){
return requestedAt;
}
/** Returns deadline. */ public Instant deadline(){
return deadline;
}
/** Tests backend eligibility. */ public boolean accepts(final BackendRegistration backend){
return backend.acceptsRouting()&&backend.capabilities().supportsAll(requiredCapabilities);
}
@Override public boolean equals(final Object other){
if(!(other instanceof RoutingRequest)){
return false;
}
RoutingRequest value=(RoutingRequest)other;
return requestId.equals(value.requestId)&&subjectReference.equals(value.subjectReference)&&audience.equals(value.audience)&&requiredCapabilities.equals(value.requiredCapabilities)&&requestedAt.equals(value.requestedAt)&&deadline.equals(value.deadline);
}
@Override public int hashCode(){
return Objects.hash(requestId,subjectReference,audience,requiredCapabilities,requestedAt,deadline);
}
}
