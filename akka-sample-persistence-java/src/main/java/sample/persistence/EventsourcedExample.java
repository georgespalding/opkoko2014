package sample.persistence;

//#eventsourced-example

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import akka.actor.*;
import akka.japi.Procedure;
import akka.persistence.*;
import static java.util.Arrays.asList;

abstract class TransactionPart implements Serializable {
    public final String note;
    public final BigDecimal amount;

    public TransactionPart(BigDecimal amount, String note) {
        assert null!=amount :"Amount should not be null";
        assert amount.compareTo(BigDecimal.ZERO)>0 :"Amount should not be greater than zero.";
        assert null!=amount :"Note should not be null";
        this.amount = amount;
        this.note = note;
    }

    @Override
    public String toString() {
        return "TransactionPart{" +
                "note='" + note + '\'' +
                ", amount=" + amount +
                '}';
    }
}

final class Debit extends TransactionPart {
    Debit(BigDecimal amount, String note) {
        super(amount, note);
    }
}

final class Credit extends TransactionPart {
    Credit(BigDecimal amount, String note) {
        super(amount, note);
    }
}

final class Transaction{
    public final BigDecimal amount;
    public final long fromAccountId;
    public final long toAccountId;

    Transaction(BigDecimal amount, long fromAccountId, long toAccountId) {
        this.amount = amount;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
    }
}

// Bookeeping
class Record implements Serializable {
    private static final long serialVersionUID = 1L;
    public final BigDecimal amount;
    public final String note;
    public final Date received;

    Record(BigDecimal amount, String note) {
        this.amount = amount;
        this.note = note;
        this.received = new Date();
    }

    @Override
    public String toString() {
        return "Record{" +
                "amount=" + amount +
                ", note='" + note + '\'' +
                ", received=" + received +
                '}';
    }
}

class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    public final long accountId;
    public BigDecimal balance;
    public /*transient*/ LinkedList<Record> history;

    public Account(long accountId) {
        this(accountId, BigDecimal.ZERO, new LinkedList<Record>());
    }

    public Account(long accountId, BigDecimal balance, LinkedList<Record> history) {
        this.accountId = accountId;
        this.balance = balance;
        this.history = history;
    }

    public Account copy() {
        return new Account(accountId, balance,history);
    }

    public void update(Record record) {
        balance = balance.add(record.amount);
    }

    public int size() {
        return history.size();
    }

    @Override
    public String toString() {
        return "Account{" +
                "balance=" + balance +
                ", history=" + history +
                '}';
    }
}

class ExampleProcessor extends UntypedEventsourcedProcessor {
    private ConcurrentHashMap<Long,Account> state = new ConcurrentHashMap<Long, Account>();

    public int getNumEvents() {
        return state.size();
    }

    public void onReceiveRecover(Object msg) {
        System.out.println(msg);
        if (msg instanceof Record) {
            state.update((Record) msg);
        } else if (msg instanceof SnapshotOffer) {
            state = (Account)((SnapshotOffer)msg).snapshot();
        }
    }

    public void onReceiveCommand(Object msg) {
        System.out.println(msg);
        if (msg instanceof Cmd) {
            final String data = ((Cmd)msg).getData();
            final Evt evt1 = new Evt(data + "-" + getNumEvents());
            final Evt evt2 = new Evt(data + "-" + (getNumEvents() + 1));
            persist(asList(evt1, evt2), new Procedure<Evt>() {
                public void apply(Evt evt) throws Exception {
                    state.update(evt);
                    if (evt.equals(evt2)) {
                        getContext().system().eventStream().publish(evt);
                    }
                }
            });
        } else {
            if (msg.equals("snap")) {
                // IMPORTANT: create a copy of snapshot
                // because Account is mutable !!!
                saveSnapshot(state.copy());
            } else if (msg.equals("print")) {
                System.out.println(state);
            } else if (msg.equals("nuke")) {
                state = new Account();
            }
        }
    }
}
//#eventsourced-example

public class EventsourcedExample {
    public static void main(String... args) throws Exception {

        final ActorSystem system = ActorSystem.create("example");
        final ActorRef processor = system.actorOf(Props.create(ExampleProcessor.class), "processor-4-java");

        processor.tell("nuke", null);
        processor.tell("print", null);
        //
        processor.tell(new Cmd("foo"), null);
        //
        processor.tell(new Cmd("baz"), null);
        //
        processor.tell(new Cmd("bar"), null);
        //processor.tell("snap", null);
        //processor.tell(new TransactionPart("buzz"), null);
        processor.tell("print", null);

        Thread.sleep(1000);
        system.shutdown();
    }
}

class RandomNumberGenerator{
    private final SecureRandom sr;
    private final int seedIter;
    private final int width;
    private volatile int count=0;

    public RandomNumberGenerator(int width){
        assert width > 0 : "Width "+width+" must be greater than zero";
        sr = SecureRandom.getInstance("SHA1PRNG");
        seedIter = sr.nextInt(width/2+1)+7;
        this.width=width;
    }

    public int nextInt(){
        try{
            return sr.nextInt(width);
        }finally{
            if(++count>seedIter){
                sr.setSeed(sr.generateSeed(7));
                count=0;
            }
        }
    }
}
