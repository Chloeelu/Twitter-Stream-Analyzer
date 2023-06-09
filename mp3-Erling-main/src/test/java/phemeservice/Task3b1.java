package phemeservice;

import org.junit.jupiter.api.*;
import pheme.PhemeService;
import security.BlowfishCipher;
import timedelayqueue.PubSubMessage;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Task3b1 {

    private static PhemeService srv;
    private static String userName1;
    private static UUID userID1;
    private static String userName2;
    private static UUID userID2;
    private static String hashPwd1;
    private static String hashPwd2;
    private static String userName3;
    private static UUID userID3;
    private static String hashPwd3;
    private static String userName4;
    private static String hashPwd4;
    private static UUID userID4;
    private static String userName5;
    private static String hashPwd5;
    private static UUID userID5;

    private static PubSubMessage msg1;
    private static PubSubMessage msg2;

    @BeforeAll
    public static void setup() {
        srv = new PhemeService(new File("secret/credentials.json"));

        userName1 = "Test User 1";
        userID1 = UUID.randomUUID();
        hashPwd1 = BlowfishCipher.hashPassword("Test Password 1", BlowfishCipher.gensalt(12));

        userName2 = "Test User 2";
        userID2 = UUID.randomUUID();
        hashPwd2 = BlowfishCipher.hashPassword("Test Password 2", BlowfishCipher.gensalt(12));

        userName3 = "Test User 3";
        userID3 = UUID.randomUUID();
        hashPwd3 = BlowfishCipher.hashPassword("Test Password 3", BlowfishCipher.gensalt(12));

        userName4 = "Test User 4";
        userID4 = UUID.randomUUID();
        hashPwd4 = BlowfishCipher.hashPassword("Test Password 4", BlowfishCipher.gensalt(12));

        userName5 = "Test User 5";
        userID5 = UUID.randomUUID();
        hashPwd5 = BlowfishCipher.hashPassword("Test Password 5", BlowfishCipher.gensalt(12));
    }

    @Test
    @Order(1)
    public void testAddUser() {
        assertTrue(srv.addUser(userID1, userName1, hashPwd1));
    }

    @Test
    @Order(2)
    public void testAddDuplicateUser() {
        String userName = "Test User 1";
        String hashPwd = BlowfishCipher.hashPassword("Test Password 1", BlowfishCipher.gensalt(12));
        UUID userID = UUID.randomUUID();

        assertFalse(srv.addUser(userID, userName, hashPwd));
    }

    @Test
    @Order(3)
    public void testAddSecondUser() {
        assertTrue(srv.addUser(userID2, userName2, hashPwd2));
    }

    @Test
    @Order(4)
    public void testRemoveUser() {
        assertTrue(srv.removeUser(userName2, hashPwd2));
        assertFalse(srv.removeUser(userName2, hashPwd2));
        assertFalse(srv.removeUser(userName2, "hashPwd2"));
        assertTrue(srv.addUser(userID2, userName2, hashPwd2));
    }
    @Test
    @Order(4)
    public void testSendMsg1() {
        msg1 = new PubSubMessage(
                userID1,
                userID2,
                "Test Msg"
        );
        srv.sendMessage(userName1, hashPwd1, msg1);
        assertFalse(srv.sendMessage("A","b",msg1));
        assertFalse(srv.sendMessage(userName1,"b",msg1));
        assertFalse(srv.isDelivered(msg1.getId(),userID2));
        assertEquals(PubSubMessage.NO_MSG, srv.getNext(userName2, hashPwd2));
    }
    @Test
    @Order(5)
    public void testReceiveMsg1() {
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ie) {
            fail();
        }
        assertEquals(msg1, srv.getNext(userName2, hashPwd2));
        assertFalse(srv.sendMessage("userName1", hashPwd1, msg1));
    }

    @Test
    @Order(6)
    public void testMsgDelivered() {
        assertTrue(srv.isDelivered(msg1.getId(), userID2));
        IllegalArgumentException e2=assertThrows(
                IllegalArgumentException.class, () ->{
                    srv.isDelivered(msg1.getId(),userID3);
                }
        );
    }


    @Test
    @Order(7)
    public void testAddSubscription1() {
        assertTrue(srv.addSubscription(userName1, hashPwd1, "UBC"));
    }

    @Test
    @Order(7)
    public void testAddSubscription2() {
        assertFalse(srv.addSubscription("userName1", hashPwd1, "UBC"));
    }

    @Test
    @Order(7)
    public void testAddSubscription3() {
        assertFalse(srv.addSubscription(userName1, "hashPwd1", "UBC"));
    }

    @Test
    @Order(8)
    public void testAddSubscription4() {
        assertFalse(srv.addSubscription(userName1, "hashPwd2", "UBC"));
    }
    @Test
    @Order(9)
    public void testAddSubscription5() {
        assertFalse(srv.addSubscription("userName2", "hashPwd1", "UBC"));
    }

    @Test
    @Order(10)
    public void testCancelSubscription1() {
        assertTrue(srv.cancelSubscription(userName1, hashPwd1, "UBC"));
        assertFalse(srv.cancelSubscription(userName1, hashPwd1, "UBC"));
        assertFalse(srv.cancelSubscription("userName1", "h","UBC"));
        assertFalse(srv.cancelSubscription(userName1, "h","UBC"));
        assertFalse(srv.cancelSubscription("userName1", hashPwd1,"UBC"));
        assertFalse(srv.cancelSubscription(userName1, hashPwd1, "SFU"));
        assertTrue(srv.addSubscription(userName1,hashPwd1,"UBC"));
    }

    @Test
    @Order(11)
    public void testAddSubscription6() {
        assertTrue(srv.addSubscription(userName1,hashPwd1,"UBC", "a"));
        assertTrue(srv.addSubscription(userName1,hashPwd1,"Canada"));
        srv.addSubscription(userName1,hashPwd1,"UBC", "b");
        srv.addSubscription(userName2,hashPwd2,"SFU");
        srv.addSubscription(userName2,hashPwd2,"SFU","a");
        srv.addSubscription(userName2,hashPwd2,"SFU", "b");
        assertFalse(srv.addSubscription(userName2,hashPwd1,"SFU"));
        assertFalse(srv.addSubscription(userName2,hashPwd2,"SFU"));
        assertFalse(srv.addSubscription(userName2,hashPwd2,"SFU","a"));
        assertFalse(srv.addSubscription(userName2,"hashPwd2", "SFU", "a"));
        assertFalse(srv.addSubscription("a","hashPwd2", "SFU", "a"));

    }
    @Test
    @Order(12)
    public void testCancelSubscription6() {
       assertTrue(srv.cancelSubscription(userName2,hashPwd2,"SFU","b"));
       assertFalse(srv.cancelSubscription(userName2,hashPwd2,"SFU","b"));
       assertFalse(srv.cancelSubscription("a",hashPwd2,"SFU","a"));
       assertFalse(srv.cancelSubscription(userName2,"a","UBC","a"));
    }

    @Test
    @Order(13)
    public void testIsUserTrue() {
        assertTrue(srv.isUser(userName2));
        assertFalse(srv.isUser(""));
    }

    @Test
    @Order(14)
    public void testIsUserFalse() {
        assertFalse(srv.isUser("testuser1"));
    }

    @Test
    @Order(17)
    public void testReceiveMsg2() {
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ie) {
            fail();
        }
        IllegalArgumentException e1=assertThrows(
                IllegalArgumentException.class, () ->{
                    srv.getNext("UserName2", hashPwd2);
                }
        );
        IllegalArgumentException e2=assertThrows(
                IllegalArgumentException.class, () ->{
                    srv.getNext(userName2, "hashPwd2");
                }
        );
    }
    @Test
    @Order(18)
    public void testAllRecent() {

        IllegalArgumentException e1=assertThrows(
                IllegalArgumentException.class, () ->{
                    srv.getAllRecent("UserName2", hashPwd2);
                }
        );
        IllegalArgumentException e2=assertThrows(
                IllegalArgumentException.class, () ->{
                    srv.getAllRecent(userName2, "hashPwd2");
                }
        );
    }
    @Test
    @Order(19)
    public void equal() {
        assertTrue(!msg1.equals("PubSubMessage.NO_MSG"));
    }

}