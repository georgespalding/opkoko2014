import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ch.qos.logback.classic.LoggerContext;

/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 29/04/14
 * Time: 21:07
 * To change this template use File | Settings | File Templates.
 */
public class KafkaLogTest {
    static final org.slf4j.Logger slf4jLogger=LoggerFactory.getLogger(KafkaLogTest.class);

    private static final int NTHREDS = 10;

    public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {
        // OR -Djava.util.logging.config.file=log.properties
        //# register SLF4JBridgeHandler as handler for the j.u.l. root logger
        //#handlers = org.slf4j.bridge.SLF4JBridgeHandler

        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
        SLF4JBridgeHandler.install();

        final SecureRandom sr=SecureRandom.getInstance("SHA1PRNG");
        //static final Logger log = Logger.getLogger("test");

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
        List<Future<Long>> list = new LinkedList<Future<Long>>();
        for (int i = 0; i < 20000; i++) {
            final int ci=i;
            Callable worker = new Callable(){

                @Override
                public Double call() throws Exception {
                    final String userid = ""+sr.nextInt(17);
                    try{
                        MDC.getMDCAdapter().put("userid", userid);
                        Double d = sr.nextDouble();
                        slf4jLogger.info("Message-{0}: {1}",ci,d);
                        return d;
                    }finally{
                        MDC.getMDCAdapter().remove(userid);
                        slf4jLogger.debug("leaving");
                    }
                }
            };
            Future<Long> submit = executor.submit(worker);
            list.add(submit);
        }
        long sum = 0;
        System.out.println(list.size());
        // now retrieve the result
        for (Future<Long> future : list) {
            try {
                sum += future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }
}
