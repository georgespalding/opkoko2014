package sample.persistence;

//#eventsourced-example

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import akka.actor.*;
import akka.japi.Procedure;
import akka.persistence.*;
import static java.util.Arrays.asList;

abstract class Transfer {
    public final BigDecimal amount;
    public final long toAccountId;

    Transfer(BigDecimal amount, long toAccountId) {
        assert null!=amount :"Amount should not be null";
        assert amount.compareTo(BigDecimal.ZERO)>0 :"Amount should not be greater than zero.";
        this.amount = amount;
        this.toAccountId = toAccountId;
    }
}

final class AccountTransfer extends Transfer{
    public final long fromAccountId;

    AccountTransfer(BigDecimal amount, long fromAccountId, long toAccountId) {
        super(amount, toAccountId);
        this.fromAccountId = fromAccountId;
    }
}

abstract class CashTransfer extends Transfer{
    CashTransfer(BigDecimal amount, long toAccountId) {
        super(amount, toAccountId);
    }
}

final class CashWithdrawal extends CashTransfer {
    CashWithdrawal(BigDecimal amount, long toAccountId) {
        super(amount, toAccountId);
    }
}

final class CashDeposit extends CashTransfer {
    CashDeposit(BigDecimal amount, long accountId) {
        super(amount, accountId);
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
        if (msg instanceof Transfer) {
            state.update((Transfer) msg);
        } else if (msg instanceof SnapshotOffer) {
            Map<Long,Account> backup = (HashMap<Long,Account>)((SnapshotOffer)msg).snapshot();
            state = new ConcurrentHashMap<Long, Account>(backup);
        } else {
            System.err.println("onReceiveRecover unknown type! Type:"+msg.getClass());
        }
    }

    public void onReceiveCommand(Object msg) {
        System.out.println(msg);
        if (msg instanceof Transfer) {
            // Translate Transfer to Records
            final List<Record> events=new LinkedList<>();

            if(msg instanceof AccountTransfer){
                final AccountTransfer accTran = (Transfer)msg;
                events.add(new Record(
                        accTran.amount.negate(),
                        accTran.fromAccountId,
                        "Transfer to "+tran.toAccountId));
                events.add(new Record(
                        accTran.amount,
                        accTran.toAccountId,
                        "Transfer from "+tran.fromAccountId));
            } else if(msg instanceof CashTransfer){
                boolean isDeposit = msg instanceof CashDeposit;
                CashTransfer cashTran=(CashTransfer)msg;
                events.add(new Record(isDeposit
                        ?cashTran.amount
                        :cashTran.amount.negate(),
                        cashTran.toAccountId,"Cash "+(isDeposit?"deposit":"withdrawal")+" to "+cashTran.toAccountId));
            }

            // Persist the Records
            persist(evants, new Procedure<Record>() {
                public void apply(Record record) throws Exception {
                    state.update(record);
                    if (evt.equals(events.last())) {
                        getContext().system().eventStream().publish(evt);
                    }
                }
            });
        } else if (msg.equals("snap")) {
            // IMPORTANT: create a copy of snapshot
            // because the Map is mutable !!!
            // FIXME break account into its own Persistent actor, onew per account
            HashMap<Long, Account> copy = new HashMap();
            for(Map.Entry<Long, Account> each:state.entrySet()){
                // Copy Mutable Account, but Long which is immutable
                copy.put(each.getKey(),each.getValue().copy());
            }
            saveSnapshot(copy);
        } else if (msg.equals("print")) {
            System.out.println(state);
        } else if (msg.equals("nuke")) {
            state = new ConcurrentHashMap<Long, Account>();
        } else {
            System.err.println("What is ");
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
        //Sätt in lite pengar
        processor.tell(new CashDeposit(BigDecimal.TEN,4711L), null);
        processor.tell(new CashDeposit(BigDecimal.ONE,1337L), null);
        // Gör en överföring
        processor.tell(new AccountTransfer(BigDecimal.ONE,4711L,1337L), null);
        processor.tell("snap", null);
        processor.tell("print", null);

        Thread.sleep(1000);
        system.shutdown();
    }
}

// Not used right now
class RandomNumberGenerator{
    private final SecureRandom sr;
    private final int seedIter;
    private final int width;
    private volatile int count=0;

    public RandomNumberGenerator(int width) throws NoSuchAlgorithmException {
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
