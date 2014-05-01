package sample.persistence;

import java.math.BigDecimal;

import akka.actor.*;
import akka.japi.Procedure;
import akka.persistence.*;
/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 01/05/14
 * Time: 17:10
 * To change this template use File | Settings | File Templates.
 */
//#eventsourced-example2
public class EventsourcedEx2 {
    public static void main(String... args) throws Exception {
        System.err.println("EXAMPLE 2");
        final ActorSystem system = ActorSystem.create("example");
        final ActorRef processor = system.actorOf(Props.create(AccountProcessor.class), "processor-4-java");

        processor.tell("print", null);
        //Sätt in lite pengar
        //processor.tell(BigDecimal.TEN.negate(), null);
        processor.tell("print", null);

        processor.tell(BigDecimal.ONE.negate(), null);
        processor.tell("print", null);

        Thread.sleep(1000);
        system.shutdown();
    }
}
