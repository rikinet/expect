/*
 * Copyright (c) 2017 Riki Network Systems Inc.
 * All rights reserved.
 */

package jp.co.rikinet.expect;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ExpectTest {

    private TelnetClient client;

    private Expect talker;

    @Before
    public void setupConnection() {
        client = new TelnetClient();
        EchoOptionHandler echoOption = new EchoOptionHandler(false, true, false, true);
        SuppressGAOptionHandler gaOption = new SuppressGAOptionHandler(true, true, true, true);
        try {
            client.addOptionHandler(echoOption);
            client.addOptionHandler(gaOption);
            client.connect("10.0.6.7", 23);
        } catch (InvalidTelnetOptionException | IOException e) {
            e.printStackTrace();
            fail();
        }
        talker = new Expect(client.getInputStream(), client.getOutputStream());
        talker.setCharset(Charset.forName("Shift_JIS"));
    }

    @After
    public void closeConnection() {
        if (client == null)
            return;
        try {
//            client.getOutputStream().close();
//            client.getInputStream().close();
            client.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        client = null;
    }

    @Test
    public void testExpect() {
        String str;
        long start = System.currentTimeMillis();
        str = talker.expect("login: ", 5000L);
        System.out.println(System.currentTimeMillis() - start);
        talker.sendLine("manager\r");
        str = talker.expect("Password: ", 1000L);
        System.out.println(System.currentTimeMillis() - start);
        talker.sendLine("friend\r");
        str = talker.expect("Manager > ", 1000L);
        System.out.println(System.currentTimeMillis() - start);
        talker.sendLine("help\r");
        str = talker.expect("Manager > ", 1000L);
        System.out.println(System.currentTimeMillis() - start);
        assertThat(str, is(containsString("GS908M")));
    }
}
