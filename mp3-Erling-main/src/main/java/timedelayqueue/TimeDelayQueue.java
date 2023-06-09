package timedelayqueue;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeDelayQueue {
    // Rep invariants:
    // timeDelayQueue != null
    // delay >= 0
    // count >= 0
    // operations != null
    // ids != null
    //Abstraction function:
    //represents a queue data structure that returns objects in an order
    //that is determined by their individual timestamps and a delay parameter

    private final PriorityQueue<PubSubMessage> timeDelayQueue;
    private final int delay;
    private AtomicInteger count;
    private int countt = 0;
    private final List<Timestamp> operations;
    private final Set<UUID> ids;

    /**
     * Create a new TimeDelayQueue
     *
     * @param delay the delay, in milliseconds, that the queue can tolerate, >= 0
     */
    public TimeDelayQueue(int delay) {
        timeDelayQueue = new PriorityQueue<>(new PubSubMessageComparator());
        this.delay = delay;
        ids = new HashSet<>();
        count=new AtomicInteger(0);
        operations = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * add a message to the TimeDelayQueue
     * if a message with the same id exists then
     * return false
     *
     * @return if add successfully
     */
    public boolean add(PubSubMessage msg) {
        if(ids.contains(msg.getId())){
            return false;
        }
        operations.add(msg.getTimestamp());
        timeDelayQueue.add(msg);
        countt=count.incrementAndGet();
        ids.add(msg.getId());
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * Get the count of the total number of messages processed
     * by this TimeDelayQueue
     *
     * @return count
     */
    public long getTotalMsgCount() {
        return countt;
    }

    /**
     * Get the next message
     *
     * @return the next message and PubSubMessage.NO_MSG
     * if there is ni suitable message
     */
    public PubSubMessage getNext() {
        if (!timeDelayQueue.isEmpty()) {
            Timestamp nowTime = new Timestamp(System.currentTimeMillis());
            while (timeDelayQueue.peek().isTransient() &&
                    nowTime.getTime()-timeDelayQueue.peek().getTimestamp().getTime()
                            >((TransientPubSubMessage) timeDelayQueue.peek()).getLifetime()) {
                timeDelayQueue.remove(timeDelayQueue.peek());
            }
            assert timeDelayQueue.peek() != null;
            if (nowTime.getTime()-timeDelayQueue.peek().getTimestamp().getTime()>delay) {
                operations.add(nowTime);
                return timeDelayQueue.poll();
            }
        }
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return PubSubMessage.NO_MSG;
    }

    /**
     * Get the maximum number of operations
     * performed on this TimeDelayQueue over
     * any window of length timeWindow
     * the operations of interest are add and getNext
     *
     * @return the number of operations
     */
    public int getPeakLoad(int timeWindow) {
        List<Integer> numbers = new ArrayList<>();
        long start = operations.get(0).getTime();
        long end = operations.get(operations.size()-1).getTime();
        if((start+timeWindow) >= end){
            return operations.size();
        }
        for (long i = start; i <= end-timeWindow; i++) {
            int number = 0;
            for (Timestamp t : operations) {
                if(t.getTime()>=i && t.getTime()<=i+timeWindow){
                    number++;
                }
            }
            numbers.add(number);
        }
        return Collections.max(numbers);
    }

    /** a comparator to sort messages*/
    private class PubSubMessageComparator implements Comparator<PubSubMessage> {
        public int compare(PubSubMessage msg1, PubSubMessage msg2) {
            return msg1.getTimestamp().compareTo(msg2.getTimestamp());
        }
    }

    /**
     * Get the count of the total number of messages in TimeDelayQueue
     *
     * @return size
     */
    public int getSize(){
        return timeDelayQueue.size();
    }

}