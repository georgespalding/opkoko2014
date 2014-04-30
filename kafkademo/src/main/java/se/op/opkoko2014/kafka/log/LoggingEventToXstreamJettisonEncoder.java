package se.op.opkoko2014.kafka.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import ch.qos.logback.classic.spi.ILoggingEvent;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 29/04/14
 * Time: 18:51
 * To change this template use File | Settings | File Templates.
 */
public class LoggingEventToXstreamJettisonEncoder implements Encoder<ILoggingEvent>{
    private final VerifiableProperties verifiableProperties;

    public LoggingEventToXstreamJettisonEncoder(VerifiableProperties verifiableProperties){
        this.verifiableProperties = verifiableProperties;
    }
    @Override
    public byte[] toBytes(ILoggingEvent iLoggingEvent) {
        XStream xstream = new XStream(new JettisonMappedXmlDriver());
        xstream.alias("ILoggingEvent", ILoggingEvent.class);

        try(ByteArrayOutputStream baos=new ByteArrayOutputStream()){
            xstream.toXML(iLoggingEvent,baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize iLoggingEvent:"+iLoggingEvent+" to string.", e);
        }
    }
}
