package com.mx.classes;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import javax.swing.JOptionPane;
import org.joda.time.LocalDate;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionlessEventHandlerIF;
import org.productivity.java.syslog4j.server.impl.net.udp.UDPNetSyslogServerConfig;
import org.productivity.java.syslog4j.util.SyslogUtility;

public class SyslogListener implements SyslogServerSessionlessEventHandlerIF {

    public static void main(String[] args) {
        SyslogServerConfigIF config = new UDPNetSyslogServerConfig("0.0.0.0", 514);
        config.setUseStructuredData(true);
        config.addEventHandler(new SyslogListener());
        SyslogServerIF syslogServer = SyslogServer.createThreadedInstance("mySyslogServer", config);
        // syslog server started!
        while (true) {
            SyslogUtility.sleep(1000l);
        }
    }

    @Override
    public void event(SyslogServerIF syslogServer, SocketAddress socketAddress, SyslogServerEventIF event) {
        LocalDate date = LocalDate.now();
        String syslogMessage = event.getMessage();
        try {
            File f = new File("log.txt");
            if (!f.exists()) {
                f.createNewFile();
            }
            Files.write(f.toPath(), ("[" + date.toString() + "] " + syslogMessage + "\n").getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ioe) {
        }
        if (syslogMessage.contains("failure") || syslogMessage.contains("error")) {
            System.err.println("[" + date.toString() + "] " + syslogMessage);
        }
    }

    @Override
    public void exception(SyslogServerIF syslogServer, SocketAddress socketAddress, Exception exception) {
    }

    @Override
    public void destroy(SyslogServerIF syslogServer) {
    }

    @Override
    public void initialize(SyslogServerIF syslogServer) {
    }
}
