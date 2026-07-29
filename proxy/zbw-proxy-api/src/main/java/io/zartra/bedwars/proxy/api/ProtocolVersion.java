package io.zartra.bedwars.proxy.api;

import java.util.Objects;

/** Semantic proxy protocol version. */ public final class ProtocolVersion implements Comparable<ProtocolVersion>{
private final int major;
private final int minor;
private ProtocolVersion(final int major,final int minor){
if(major<1||minor<0){
throw new IllegalArgumentException("invalid protocol version");
}
this.major=major;
this.minor=minor;
}
/** Creates a version. */ public static ProtocolVersion of(final int major,final int minor){
return new ProtocolVersion(major,minor);
}
/** Returns major. */ public int major(){
return major;
}
/** Returns minor. */ public int minor(){
return minor;
}
/** Tests rolling compatibility. */ public boolean compatibleWith(final ProtocolVersion other){
return major==Objects.requireNonNull(other,"other").major;
}
@Override public int compareTo(final ProtocolVersion other){
int order=Integer.compare(major,other.major);
return order!=0?order:Integer.compare(minor,other.minor);
}
@Override public boolean equals(final Object other){
return other instanceof ProtocolVersion&&major==((ProtocolVersion)other).major&&minor==((ProtocolVersion)other).minor;
}
@Override public int hashCode(){
return Objects.hash(major,minor);
}
@Override public String toString(){
return major+"."+minor;
}
}
