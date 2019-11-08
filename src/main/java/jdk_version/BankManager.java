package jdk_version;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BankManager implements Runnable {

    private final int MAX_PEOPLE = 500;

    private ConcurrentHashMap<Integer, AtomicInteger> bankDatabase = new ConcurrentHashMap<>();
    private Person[] people = new Person[MAX_PEOPLE];
    int[] runningTasks = new int[MAX_PEOPLE];//list of user id's that are running at any given time

    private BlockingQueue<Person> finishedTasks = new ArrayBlockingQueue<>(MAX_PEOPLE);

    /**
     * initialize
     * create tasks and threads to run said tasks
     * tasks will simulate stripped-down bank system + users
     */
    public void run() {
        initializeUsers();
        initializeDatabase();
        execute();
    }

    /**
     * adds deposit amount to current stored money
     *
     * @param id     - user id
     * @param amount - amount to deposit
     * @return - not really needed
     */
    public void deposit(int id, int amount) {//read/write
        bankDatabase.put(id, new AtomicInteger(bankDatabase.get(id).get() + amount));
    }

    /**
     * withdraws amount from bank
     * if amount is not enough returns false without withdrawing
     *
     * @param id     - user id
     * @param amount - amount to withdraw
     * @return - true if withdrawn successfully, false otherwise
     */
    public boolean withdraw(int id, int amount) {//read only or read/write
        AtomicInteger currentAmount = bankDatabase.get(id);
        if (currentAmount.get() - amount > 0) {
            //enough money to withdraw, so attempt withdrawal
            bankDatabase.put(id, new AtomicInteger(currentAmount.get() - amount));
            return true;
        }
        return false;
    }


    /**
     * initialize n users into people array
     */
    private void initializeUsers() {
        for (int i = 0; i < MAX_PEOPLE; i++) {
            Person temp = new Person(i);
            people[i] = temp;
        }

        //set runningtasks to default of -1
        for(int i=0;i<runningTasks.length;i++)
        {
            runningTasks[i]= -1;
        }
    }

    /**
     * initialize database with account numbers and initial money
     */
    private void initializeDatabase() {
        for (Person p: people) {
            //make database accounts for each person, initialize with random money from $1.00-$19,999.99
            bankDatabase.put(p.getAccountNumber(), new AtomicInteger(ThreadLocalRandom.current().nextInt(1,2000000)));
        }
    }

    private void execute() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicBoolean doRun = new AtomicBoolean(true);

        Runnable taskCreator = () -> {//creates and runs tasks
            ExecutorService taskExecutor = Executors.newCachedThreadPool();

            for (int i = 0; i < 1000; i++) {//try 1000 tasks, max 500 concurrently running
                int user = getOpenUser();
                Client temp = new Client(this, people[user], decideTask(), finishedTasks);
                for (int j = 0; j < runningTasks.length; j++) {
                    if(runningTasks[j] == -1){
                        runningTasks[j] = people[user].getAccountNumber();
                    }
                }
                taskExecutor.execute(temp);
            }
            doRun.set(false);
            taskExecutor.shutdown();
        };
        Runnable taskListener = () -> {//listens for task completions and removes them from list of running tasks, as well as updates people list with new info
            while (doRun.get()) {
                try {
                    Person done = finishedTasks.poll(1, TimeUnit.SECONDS);

                    if(done != null) {
                        //replace old memory of person
                        for (int i = 0; i < people.length; i++) {
                            if (done.getAccountNumber() == people[i].getAccountNumber()) {
                                people[i] = done;
                            }
                        }
                        //remove person from list of running tasks, so new ones can be made of them
                        for (int i = 0; i < runningTasks.length; i++) {
                            if (done.getAccountNumber() == runningTasks[i]) {
                                runningTasks[i] = -1;
                            }
                        }
                    } else {
                        doRun.set(false);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        executor.execute(taskCreator);
        executor.execute(taskListener);
        while(doRun.get()){
            //
        }
        executor.shutdown();
    }


    /**
     * keeps iterating until it randomly finds a user that is not currently being used in a task
     * @return - int for location of open user inside person array
     */
    private int getOpenUser(){
        for(;;) {
            boolean use = true;
            int user = ThreadLocalRandom.current().nextInt(0, MAX_PEOPLE);
            for (int i = 0; i < runningTasks.length; i++) {
                if(people[user].getAccountNumber() == runningTasks[i]){
                    use = false;
                    break;
                    //if already running dont use
                }
            }
            if(use){
                return user;
            }
        }
    }

    /**
     * decide a task for a thread to simulate
     * 1: withdraw for purchase(read+write)
     * 2: deposit some amount of money to be decided in thread
     * 3: receive pay(no 'database' access, pay is in local user not bank)
     *
     * @return a number from 1-3 of which task to do, that will be given to a thread
     */
    private int decideTask() {
        return ThreadLocalRandom.current().nextInt(1, 4);
    }

}


//simulates a client connnection
class Client extends Thread {

    private final BankManager bm;
    private final Person p;
    private final int task;
    BlockingQueue<Person> doneListener;

    public Client(BankManager bm, Person p, int task, BlockingQueue<Person> b) {
        this.bm = bm;
        this.p = p;
        this.task = task;
        doneListener = b;
    }

    @Override
    public void run() {
        //do stuff
        if(task == 1){
            //make random withdrawal $1.00-$9,999.99
            int withdrawal = ThreadLocalRandom.current().nextInt(1, 1000000);
            boolean success = bm.withdraw(p.getAccountNumber(), withdrawal);
            if(success){
                //if successful withdrawal add to current money
                p.setMoney(p.getMoney().get()+withdrawal);
            }

        } else if(task == 2){
            //make random deposit of $1 to current money amount-1
            if(p.getMoney().get() > 1) {//more than $0.01 to deposit
                int deposit = ThreadLocalRandom.current().nextInt(1, p.getMoney().get());
                bm.deposit(p.getAccountNumber(), deposit);
                //modify on-person money
                p.setMoney(p.getMoney().get() - deposit);
            }
        } else if(task == 3){//increase money by an amount
            p.setMoney(p.getMoney().get()+ThreadLocalRandom.current().nextInt(50000, 2000000));
        }
        try {
            doneListener.put(p);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Person getPerson(){
        return p;
    }

}

class Person {
    private int accountNumber;
    private AtomicInteger money;

    public Person(int id) {
        this.accountNumber = id;

        //initial money in USD, last two digits are change
        this.money = new AtomicInteger(ThreadLocalRandom.current().nextInt(0, 100000));
    }

    public void printMoney() {
        System.out.println("User #" + accountNumber + " has $"+ (double)money.get() / 100);
    }

    public void setMoney(int newMoney) {
        money.set(newMoney);
    }

    public AtomicInteger getMoney() {
        return money;
    }

    public int getAccountNumber() {
        return accountNumber;
    }
}