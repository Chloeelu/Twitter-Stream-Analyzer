package phemeservice;

import org.junit.jupiter.api.*;
import pheme.PhemeService;
import security.BlowfishCipher;
import timedelayqueue.PubSubMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Task3b2 {
    private static PhemeService srv;
    private static String userName3;
    private static UUID userID3;
    private static String hashPwd3;
    private static String userName4;
    private static String hashPwd4;
    private static UUID userID4;
    private static String userName5;
    private static String hashPwd5;
    private static UUID userID5;

    //private static PubSubMessage msg1;
    private static PubSubMessage msg2;
    private static UUID ID;

    @BeforeAll
    public static void setup() {
        srv = new PhemeService(new File("secret/credentials.json"));

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
    public void adduser1() {
        srv.addUser(userID3, userName3, hashPwd3);

    }
    @Test
    @Order(2)
    public void adduser2() {
        srv.addUser(userID4, userName4, hashPwd4);
    }
    @Test
    @Order(3)
    public void adduser() {
        srv.addUser(userID5,userName5,hashPwd5);
    }


    @Test
    @Order(4)
    public void testSendMessage1(){
        List<UUID> re=new ArrayList<>();
        re.add(userID4);
        re.add((userID5));
        msg2 = new PubSubMessage(
                userID3,
                re,
                "Test Msg2"
        );
        ID=msg2.getId();
        assertTrue(srv.sendMessage(userName3,hashPwd3,msg2));
        assertEquals(PubSubMessage.NO_MSG, srv.getNext(userName4,hashPwd4));
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
        assertEquals(msg2, srv.getNext(userName4, hashPwd4));
        List<UUID> re=new ArrayList<>();
        re.add(userID4);
        re.add((userID5));

        List<Boolean> b=new ArrayList<>();
        b.add(true);
        b.add(false);
        assertEquals(b, srv.isDelivered(msg2.getId(),re));
    }

    @Test
    @Order(6)
    public void testReceiveMsg2() {
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ie) {
            fail();
        }
        assertEquals(msg2, srv.getNext(userName5, hashPwd5));
        List<UUID> re=new ArrayList<>();
        re.add(userID4);
        re.add((userID5));

        List<Boolean> b=new ArrayList<>();
        b.add(true);
        b.add(true);
        assertEquals(b, srv.isDelivered(msg2.getId(),re));
    }

}
