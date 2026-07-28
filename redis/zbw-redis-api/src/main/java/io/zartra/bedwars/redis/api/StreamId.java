package io.zartra.bedwars.redis.api;
import java.util.Objects;
/** Deterministically ordered stream identity. */ public final class StreamId implements Comparable<StreamId>{
private final long millis,sequence;
private StreamId(final long m,final long s){
if (m<0||s<0) {
throw new IllegalArgumentException("negative stream component");
}
millis=m;
sequence=s;
}
/** Creates identity. */ public static StreamId of(final long m,final long s){
return new StreamId(m,s);
}
/** Parses millis-sequence. */ public static StreamId parse(final String value){
String[] p=Objects.requireNonNull(value,"value").split("-",-1);
if(p.length!=2) {
throw new IllegalArgumentException("invalid stream id");
}
try{
return of(Long.parseLong(p[0]),Long.parseLong(p[1]));
}
catch(NumberFormatException e){
throw new IllegalArgumentException("invalid stream id",e);
}
}
/** Returns milliseconds. */ public long epochMillis(){
return millis;
}
/** Returns sequence. */ public long sequence(){
return sequence;
}
@Override public int compareTo(final StreamId o){
int c=Long.compare(millis,o.millis);
return c==0?Long.compare(sequence,o.sequence):c;
}
@Override public boolean equals(final Object o){
return o instanceof StreamId&&millis==((StreamId)o).millis&&sequence==((StreamId)o).sequence;
}
@Override public int hashCode(){
return Objects.hash(millis,sequence);
}
@Override public String toString(){
return millis+"-"+sequence;
}
}
