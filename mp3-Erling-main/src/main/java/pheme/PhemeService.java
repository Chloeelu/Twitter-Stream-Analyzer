package pheme;

import io.github.redouane59.twitter.dto.tweet.TweetV2;
import timedelayqueue.BasicMessageType;
import timedelayqueue.MessageType;
import timedelayqueue.PubSubMessage;
import timedelayqueue.TimeDelayQueue;
import twitter.TwitterListener;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

public class PhemeService {

    public static final int DELAY = 1000; // 1 second or 1000 milliseconds
    private final File twitterCredentialsFile;
    private final Map<String, UUID> user_name_id = Collections.synchronizedMap(new LinkedHashMap<>());//<username, password>
    private final Map<String, String> user = Collections.synchronizedMap(new LinkedHashMap<>());//<username, password>
    private final Set<UUID> userid = Collections.synchronizedSet(new HashSet<>());
    private final List<ArrayList<String>> sub = Collections.synchronizedList(new ArrayList<>());
    private final TimeDelayQueue t = new TimeDelayQueue(DELAY);
    private final Map<UUID, ArrayList<UUID>> messages_list = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, TimeDelayQueue> user_map_tdq = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<UUID, TwitterListener> user_map_sub = Collections.synchronizedMap(new LinkedHashMap<>());
    // Rep invariant:
    //   DELAY=1000
    //   twitterCredentialsFile is a file of twitter Credentials
    // Safety from rep exposure:
    //   All fields are private, only DELAY is immutable.
    //Thread safety argument:
    //    -  all fields are final, so those variables are immutable
    //      and Thread-safe
    //    -  DELAY is thread-safe type
    //    -  all other fields  point to Thread-safe set, list, and map data types.


    public PhemeService(File twitterCredentialsFile) {
        this.twitterCredentialsFile = twitterCredentialsFile;
    }

    public void saveState(String configDirName) {

    }

    /**
     * add a user to PhemeService
     *
     * @param userID       the id of the user to be added
     * @param userName     a string of name of the user to be added
     * @param hashPassword a hashed version of the password
     * @return true if the user was successfully added and the user is valid,  and false otherwise
     */
    public boolean addUser(UUID userID, String userName, String hashPassword) {
        if (user.containsKey(userName)) {
            return false;
        } else {
            user.put(userName, hashPassword);
            user_name_id.put(userName, userID);
            user_map_tdq.put(userID, new TimeDelayQueue(DELAY));
            user_map_sub.put(userID, new TwitterListener(twitterCredentialsFile));
            userid.add(userID);
            messages_list.put(userID, new ArrayList<>());
            return true;
        }
    }

    /**
     * remove the given user
     *
     * @param userName     a string of the name of the user to be removed
     * @param hashPassword a hashed version of the password to be removed
     * @return true if the user was removed successfully and the user is valid,  and false otherwise
     */

    public boolean removeUser(String userName, String hashPassword) {
        if (user.containsKey(userName)) {
            if (user.get(userName).equals(hashPassword)) {
                user.remove(userName);
                UUID id = user_name_id.get(userName);
                userid.remove(id);
                user_map_tdq.remove(id);
                user_map_sub.remove(id);
                user_name_id.remove(userName);
                messages_list.remove(id);
                return true;
            }
        }
        return false;
    }

    /**
     * cancel subscription with given Twitter username
     *
     * @param userName        a string of the name of the user
     * @param hashPassword    a hashed version of the password of the user
     * @param twitterUserName name of the Twitter user to be canceled
     * @return true if the subscription has been successfully canceled and the user is valid,  and false otherwise
     */

    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName) {
        // check if the user is existed
        if (user.containsKey(userName) && user.get(userName).equals(hashPassword)) {
            UUID id = user_name_id.get(userName);
            boolean cancelsub = user_map_sub.get(id).cancelSubscription(twitterUserName);
            if (cancelsub) {
                sub.removeIf(x -> x.get(0).equals(userName) && x.get(1).equals(twitterUserName));
            }
            return cancelsub;
        }
        return false;
    }

    /**
     * cancel subscription with given Twitter username
     *
     * @param userName        a string of the name of the user
     * @param hashPassword    a hashed version of the password of the user
     * @param twitterUserName name of the Twitter user to be canceled
     * @param pattern         the pattern string made by Twitter User
     * @return true if the subscription has been successfully canceled and the user is valid,  and false otherwise
     */

    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName,
                                      String pattern) {
        if (user.containsKey(userName) && user.get(userName).equals(hashPassword)) {
            UUID id = user_name_id.get(userName);
            boolean cancelsub = user_map_sub.get(id).cancelSubscription(twitterUserName, pattern);
            if (cancelsub) {
                sub.removeIf(x -> x.size() > 2 && x.get(0).equals(userName) && x.get(1).equals(twitterUserName) && x.get(2).equals(pattern));
            }
            return cancelsub;
        }
        return false;
    }

    /**
     * add subscription to a specific Twitter user
     *
     * @param userName        a string of the user's name
     * @param hashPassword    a hashed version of the password of the user
     * @param twitterUserName the Twitter user to be subscribed
     * @return true if the subscriptions was added successfully and the user is valid,  and false otherwise
     */

    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName) {
        UUID id = user_name_id.get(userName);
        // check if the user is existed
        if (user.containsKey(userName) && user.get(userName).equals(hashPassword)) {
            for (ArrayList<String> a : sub) {
                //check duplicate
                if (a.size() == 2 && a.get(0).equals(userName) && a.get(1).equals(twitterUserName)) {
                    return false;
                }
            }
            boolean addsub = user_map_sub.get(id).addSubscription(twitterUserName);
            if (addsub) {
                ArrayList<String> new_sub = new ArrayList<>(2);
                new_sub.add(userName);
                new_sub.add(twitterUserName);
                sub.add(new_sub);
            }
            return addsub;
        }
        return false;
    }

    /**
     * add subscription to a specific Twitter user and specific tweets
     *
     * @param userName        a string of the user's name
     * @param hashPassword    a hashed version of the password of the user
     * @param twitterUserName the Twitter user to be subscribed
     * @param pattern         the pattern of specific tweets to be subscribed
     * @return true if the subscriptions was added successfully and the user is valid, and false otherwise
     */

    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName,
                                   String pattern) {
        // check if the user is existed
        if (user.containsKey(userName) && user.get(userName).equals(hashPassword)) {
            for (ArrayList<String> a : sub) {
                //check duplicate
                if (a.size() == 3 && a.get(0).equals(userName) && a.get(1).equals(twitterUserName) && a.get(2).equals(pattern)) {
                    return false;
                }
            }
            UUID id = user_name_id.get(userName);
            boolean addsub = user_map_sub.get(id).addSubscription(twitterUserName, pattern);
            if (addsub) {
                ArrayList<String> new_sub = new ArrayList<>();
                new_sub.add(userName);
                new_sub.add(twitterUserName);
                new_sub.add(pattern);
                sub.add(new_sub);
            }
            return addsub;
        }
        return false;
    }


    /**
     * send a message to all valid users in the list of recipients
     *
     * @param userName     the sender's username is not null
     * @param hashPassword a hashed version of the password of the user
     * @param msg          the message to be sent
     * @return true if the message was sent successfully and  all recipients are valid,
     * false otherwise
     */
    public boolean sendMessage(String userName,
                               String hashPassword,
                               PubSubMessage msg) {
        //check if the user is valid
        if (user.containsKey(userName) && user.get(userName).equals(hashPassword)) {
            UUID userID = user_name_id.get(userName);
            //check if the message valid
            if (msg.getSender().equals(userID)) {
                List<UUID> receivers = new ArrayList<>(msg.getReceiver());
                if (userid.containsAll(receivers)) {
                    for (UUID r : receivers) {
                        user_map_tdq.get(r).add(msg);
                    }
                    return t.add(msg);
                }

            }
        }

        return false;
    }


    /**
     * check if a message is delivered given a msgid
     *
     * @param msgID    the id of the message is not null
     * @param userList the receiver list
     * @return boolean list of all
     */
    public List<Boolean> isDelivered(UUID msgID, List<UUID> userList) {
        Boolean[] d = new Boolean[userList.size()];
        Arrays.fill(d, Boolean.FALSE);
        for (int i = 0; i < userList.size(); i++) {
            d[i] = isDelivered(msgID, userList.get(i));
        }
        return Arrays.asList(d);
    }


    /**
     * check if a message is delivered to a receiver given a msgid
     *
     * @param msgID the id of the message is not null
     * @param user  the receiver
     * @return true if the message was delivered to the specific receiver and false otherwise
     * throw IllegalArgumentException if the user is not valid
     */
    public boolean isDelivered(UUID msgID, UUID user) {
        if (userid.contains(user)) {
            return messages_list.get(user).contains(msgID);
        } else {
            throw new IllegalArgumentException();
        }
    }


    /**
     * check if the user is valid
     *
     * @param userName the username is not null
     * @return true if the user is valid and false otherwise
     */
    public boolean isUser(String userName) {
        return user.containsKey(userName);
    }


    /**
     * get the next message or tweet from tdq
     *
     * @param userName     the name of user who received messages to tweets
     * @param hashPassword a hashed version of the password of the user
     * @return next massage or tweet in tbq
     * throw IllegalArgumentException if the user is not valid
     */
    public PubSubMessage getNext(String userName, String hashPassword) {
        UUID id = user_name_id.get(userName);
        if (!user.containsKey(userName) || !user.get(userName).equals(hashPassword)) {
            throw new IllegalArgumentException();
        } else {
            List<TweetV2.TweetData> tweets = user_map_sub.get(id).getRecentTweets();
            for (TweetV2.TweetData tt : tweets) {
                UUID msg_id = new UUID(Long.parseLong(tt.getId()), Long.parseLong(tt.getId()));
                UUID sender = new UUID(Long.parseLong(tt.getAuthorId()), Long.parseLong(tt.getAuthorId()));
                UUID receiver = UUID.randomUUID();
                Timestamp timestamp = Timestamp.valueOf(tt.getCreatedAt());
                String content = tt.getText();
                MessageType type = BasicMessageType.TWEET;
                PubSubMessage tm = new PubSubMessage(msg_id, timestamp, sender, receiver, content, type);
                user_map_tdq.get(id).add(tm);
            }
            t.getNext();
            PubSubMessage next = user_map_tdq.get(id).getNext();
            if (!next.equals(PubSubMessage.NO_MSG)) {
                messages_list.get(id).add(next.getId());
            }
            return next;
        }
    }


    /**
     * get all recent message and tweets in tdq
     *
     * @param userName     a string name of the specific user
     * @param hashPassword a hashed version of the password of the user
     * @return a list of all message and tweets in tdq
     * throw IllegalArgumentException if the user is invalid
     */
    public List<PubSubMessage> getAllRecent(String userName, String hashPassword) {
        UUID id = user_name_id.get(userName);
        List<PubSubMessage> recent = new ArrayList<>();
        if (!user.containsKey(userName) || !user.get(userName).equals(hashPassword)) {
            throw new IllegalArgumentException();
        } else {
            PubSubMessage m = getNext(userName, hashPassword);
            if (!m.equals(PubSubMessage.NO_MSG)) {
                recent.add(m);
                messages_list.get(id).add(m.getId());
                long c = user_map_tdq.get(user_name_id.get(userName)).getSize();
                for (int i = 1; i <= c; i++) {
                    PubSubMessage mm = user_map_tdq.get(user_name_id.get(userName)).getNext();
                    recent.add(mm);
                    messages_list.get(id).add(m.getId());
                }
            }
            return recent;

        }
    }

}
