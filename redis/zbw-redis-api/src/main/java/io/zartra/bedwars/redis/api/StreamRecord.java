package io.zartra.bedwars.redis.api;
import io.zartra.bedwars.storage.api.MessageEnvelope;
import java.util.Objects;
/** Ordered Redis record reusing the authoritative M04 message envelope. */ public final class StreamRecord{
private final StreamId id;
private final MessageEnvelope envelope;
private StreamRecord(final StreamId i,final MessageEnvelope e){
id=Objects.requireNonNull(i,"id");
envelope=Objects.requireNonNull(e,"envelope");
}
/** Creates a record. */ public static StreamRecord of(final StreamId i,final MessageEnvelope e){
return new StreamRecord(i,e);
}
/** Returns stream ID. */ public StreamId id(){
return id;
}
/** Returns defensive M04 envelope. */ public MessageEnvelope envelope(){
return envelope;
}
@Override public boolean equals(final Object o){
return o instanceof StreamRecord&&id.equals(((StreamRecord)o).id)&&envelope.equals(((StreamRecord)o).envelope);
}
@Override public int hashCode(){
return Objects.hash(id,envelope);
}
}
