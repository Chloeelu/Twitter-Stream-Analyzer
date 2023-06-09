package twitter;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.user.User;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**TwitterListener is a service that uses the public API for Twitter to interact with Twitter
 * to obtain posts made by a specific user, which may also have to match a given pattern.*/
public class TwitterListener {
    //Rep invariants:
    //twitter != null
    //subscribedAll != null
    //each value of subscribedPattern is not empty
    //lastFetch != null
    //Abstraction function:
    //represents a service that interacts with Twitter
    //Thread safety argument:
    //    This class is Thread-safe because it is immutable:
    //    - twitter, subscribedAll, subscribedPattern, OCT_1_2022 are final
    //    - subscribedAll and subscribedPattern point to mutable set/map, but they are
    //      not shared with any other object or exposed to a client. Also, they are
    //      wrapped with Collections.synchronizedSet() and Collections.synchronizedMap()
    //    - lastFetch is not shared with any other object or exposed to a client

    private final TwitterClient twitter;
    private final Set<User> subscribedAll;
    private final Map<User, Set<String>> subscribedPattern;
    private LocalDateTime lastFetch;
    private static final LocalDateTime OCT_1_2022 = LocalDateTime.parse("2022-10-01T00:00:00");


    /**
     * create a new instance of TwitterListener
     * @param credentialsFile a JSON file that contains the API access keys
     * throws IllegalArgumentException if credentialsFile is null
     */
    public TwitterListener(File credentialsFile) {
        if(credentialsFile==null){
            throw new IllegalArgumentException();
        }
        twitter = new TwitterClient(TwitterClient.getAuthentication(credentialsFile));
        subscribedAll = Collections.synchronizedSet(new HashSet<>());
        subscribedPattern = Collections.synchronizedMap(new HashMap<>());
        lastFetch = OCT_1_2022;
    }

    /**
     * add a subscription for all tweets made by a user
     * @param twitterUserName the username of the targeted user
     *
     * @return true if the subscription was successfully added, otherwise false
     * throws IllegalArgumentException if the username does not exist
     */
    public boolean addSubscription(String twitterUserName) {
        if(isValidUser(twitterUserName)){
            User user = twitter.getUserFromUserName(twitterUserName);
            if(subscribedAll.contains(user)){
                return false;
            }
            subscribedAll.add(user);
            return true;
        }
        throw new IllegalArgumentException();
    }

    /**
     * test if a user exists
     * @param twitterUserName the username of the targeted user
     *
     * @return true if the user exists, otherwise false
     */
    private boolean isValidUser(String twitterUserName) {
        try{
            twitter.getUserFromUserName(twitterUserName);
            return true;
        }catch (NoSuchElementException e){
            return false;
        }
    }

    /**
     * add a subscription for all tweets made by a user that also
     * match a given pattern
     * @param twitterUserName the username of the targeted user
     * @param pattern the matching pattern, case-insensitive
     *
     * @return true if the subscription was successfully added, otherwise false
     * throws IllegalArgumentException if the username does not exist
     */
    public boolean addSubscription(String twitterUserName, String pattern) {
        if(isValidUser(twitterUserName)){
            User user = twitter.getUserFromUserName(twitterUserName);
            subscribedAll.remove(user);
            pattern = pattern.toLowerCase();
            if(subscribedPattern.containsKey(user)){
                if(subscribedPattern.get(user).contains(pattern)){
                    return false;
                }
                else{
                    subscribedPattern.get(user).add(pattern);
                    return true;
                }
            }
            else{
                Set<String> patterns = new HashSet<>();
                patterns.add(pattern);
                subscribedPattern.put(user, patterns);
                return true;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * cancel a subscription for all tweets made by a user
     * @param twitterUserName the username of the targeted user
     *
     * @return true if the subscription was successfully cancelled, otherwise false
     * throws IllegalArgumentException if the username does not exist
     */
    public boolean cancelSubscription(String twitterUserName) {
        if(isValidUser(twitterUserName)){
            User user = twitter.getUserFromUserName(twitterUserName);
            return subscribedAll.remove(user) || subscribedPattern.remove(user, subscribedPattern.get(user));
        }
        throw new IllegalArgumentException();
    }

    /**
     * cancel a subscription for all tweets made by a user that also
     * match a given pattern
     * @param twitterUserName the username of the targeted user
     * @param pattern the matching pattern, case-insensitive
     *
     * @return true if the subscription was successfully cancelled, otherwise false
     * throws IllegalArgumentException if the username does not exist
     */
    public boolean cancelSubscription(String twitterUserName, String pattern) {
        if(isValidUser(twitterUserName)){
            User user = twitter.getUserFromUserName(twitterUserName);
            pattern = pattern.toLowerCase();
            if(subscribedPattern.containsKey(user)){
                if(!subscribedPattern.get(user).contains(pattern)){
                    return false;
                }
                else if(subscribedPattern.get(user).contains(pattern) && subscribedPattern.get(user).size()==1){
                    subscribedPattern.remove(user);
                    return true;
                }
                else{
                    subscribedPattern.get(user).remove(pattern);
                    return true;
                }

            }
            else{
                return false;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * get all the subscribed tweets since the last fetch
     *
     * @return List<TweetV2.TweetData> the list of tweets since the last fetch
     */
    public List<TweetV2.TweetData> getRecentTweets() {
        List<TweetV2.TweetData> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for(User u: subscribedAll){
            result.addAll(getTweetsByUser(u.getName(), lastFetch, now));
        }
        for(User u: subscribedPattern.keySet()){
            List<TweetV2.TweetData> lst = getTweetsByUser(u.getName(), lastFetch, now);
            for(TweetV2.TweetData t: lst){
                for(String s: subscribedPattern.get(u)){
                    if(t.getText().toLowerCase().contains(s)){
                        result.add(t);
                    }
                }
            }
        }
        if(subscribedAll.size()!=0||subscribedPattern.size()!=0) {
            lastFetch = now;
        }
        return result;
    }

    /**
     * get all the tweets made by a user within a time range.
     * @param twitterUserName the username of the targeted user
     * @param startTime start time of the time range
     * @param endTime end time of the time range
     *
     * @return List<TweetV2.TweetData> the list of tweets made by a user within a time range
     * throws IllegalArgumentException if the username does not exist
     */
    public List<TweetV2.TweetData> getTweetsByUser(String twitterUserName,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        User twUser = twitter.getUserFromUserName(twitterUserName);
        if(isValidUser(twUser.getName())) {
            TweetList twList = twitter.getUserTimeline(twUser.getId(), AdditionalParameters.builder().startTime(startTime).endTime(endTime).build());
            return twList.getData();
        }
        else{
            throw new IllegalArgumentException();
        }
    }
}