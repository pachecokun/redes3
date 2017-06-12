package com.mx.classes;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionlessEventHandlerIF;

public class SyslogMessagesHandler implements SyslogServerSessionlessEventHandlerIF {

    private JEditorPane log;
    private JRootPane root;
    private LocalDate date = LocalDate.now();
    private String logPath;

    public SyslogMessagesHandler(JEditorPane log, JRootPane root, String logPath) {
        this.log = log;
        this.root = root;
        this.logPath = logPath;
    }

    private void addLog(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(log.getText());
        if (!sb.toString().equals("")) {
            sb.append("\n");
        }
        sb.append(s);
        log.setText(sb.toString());
        log.setCaretPosition(log.getDocument().getLength());
    }

    @Override
    public void event(SyslogServerIF syslogServer, SocketAddress socketAddress, SyslogServerEventIF event) {
        String syslogMessage = event.getMessage();

        addLog("[" + date.toString() + "] " + syslogMessage);
        try {
            File f = new File(logPath);
            if (!f.exists()) {
                f.createNewFile();
            }
            Files.write(f.toPath(), ("[" + date.toString() + "] " + syslogMessage + "\n").getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            addLog(ioe.toString());
        }
        if (syslogMessage.contains("failure") || syslogMessage.contains("error")) {
            JOptionPane.showMessageDialog(root, "Failure syslog:\n" + syslogMessage);
        }
    }

    @Override
    public void exception(SyslogServerIF syslogServer, SocketAddress socketAddress, Exception exception) {
    }

    @Override
    public void initialize(SyslogServerIF syslogServer) {
    }

    @Override
    public void destroy(SyslogServerIF syslogServer) {
    }

}
