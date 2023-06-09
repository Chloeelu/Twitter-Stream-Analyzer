package phemeservice;

import io.github.redouane59.twitter.dto.tweet.TweetV2;
import org.junit.jupiter.api.Test;
import twitter.TwitterListener;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Task3A {

    @Test
    public void testSubscriptions() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        tl.addSubscription("SFU", "YES");
        assertFalse(tl.addSubscription("SFU", "YES"));
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertTrue(tweets.size() > 0);
        tl.addSubscription("SFU", "hi");
        assertFalse(tl.cancelSubscription("SFU", "no"));
        assertTrue(tl.cancelSubscription("SFU", "hi"));
        assertTrue(tl.cancelSubscription("SFU", "YES"));
        assertFalse(tl.cancelSubscription("UofT"));
        assertFalse(tl.cancelSubscription("UofT", "hi"));
        assertFalse(tl.addSubscription("UBC"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                ()->{tl.addSubscription("zzzzztztztcyguh oib");});
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                ()->{tl.addSubscription("zzzzztztztcyguh oib","yy");});
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                ()->{tl.cancelSubscription("zzzzztztztcyguh oib","yy");});
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
                ()->{tl.cancelSubscription("zzzzztztztcyguh oib");});
    }

    @Test
    public void testDoubleFetchRecentTweets() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertTrue(tweets.size() > 0);
        tweets = tl.getRecentTweets();
        assertEquals(0, tweets.size()); // second time around, in quick succession, no tweet
    }
}