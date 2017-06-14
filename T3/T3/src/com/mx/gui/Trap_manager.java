/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mx.gui;

import com.mx.classes.SyslogMessagesHandler;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.net.udp.UDPNetSyslogServerConfig;
import org.productivity.java.syslog4j.util.SyslogUtility;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.StateReference;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TransportIpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.tools.console.SnmpRequest;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

/**
 *
 * @author root
 */
public class Trap_manager extends javax.swing.JFrame implements CommandResponder {

    List<VariableBinding> varBinds;
    String logPath = "log.txt";
    HashMap<String, String[]> mibMap = new HashMap<>();
    String[][] mibs = new String[][]{
        {"1.3.6.1.6.3.1.1.5.4", "linkUp", "0"},
        {"1.3.6.1.6.3.1.1.5.3", "linkDown", "1"},
        {"1.3.6.1.2.1.2.2.1.1.3", "ifIndex", "0"},
        {"1.3.6.1.2.1.2.2.1.2.3", "ifDescr", "0"},
        {"1.3.6.1.2.1.2.2.1.7.3", "ifAdminStatus", "0"},
        {"1.3.6.1.2.1.2.2.1.8.3", "ifOperStatus", "0"},};

    public Trap_manager() throws IOException {
        initComponents();
        setLocationRelativeTo(null);
        logTraps.setEditable(false);
        startDaemonTraps();
        startDaemonSyslog();
        loadMibs();
    }

    // <editor-fold defaultstate="collapsed" desc="SNMP session code"> 
    public synchronized void listen(TransportIpAddress address) throws IOException {
        AbstractTransportMapping transport;
        addLog("Starting services", logTraps);
        if (address instanceof TcpAddress) {
            transport = new DefaultTcpTransportMapping((TcpAddress) address);
        } else {
            transport = new DefaultUdpTransportMapping((UdpAddress) address);
        }

        ThreadPool threadPool = ThreadPool.create("DispatcherPool", 10);
        MessageDispatcher mtDispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());

        // add message processing models
        mtDispatcher.addMessageProcessingModel(new MPv1());
        mtDispatcher.addMessageProcessingModel(new MPv2c());

        // add all security protocols
        addLog("Adding security protocols", logTraps);
        SecurityProtocols.getInstance().addDefaultProtocols();
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

        //Create Target
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));

        Snmp snmp = new Snmp(mtDispatcher, transport);
        snmp.addCommandResponder(this);

        transport.listen();
        addLog("Started listening traps", logTraps);

        try {
            this.wait();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void processPdu(CommandResponderEvent cmdRespEvent) {
        PDU pdu = cmdRespEvent.getPDU();
        if (pdu != null) {
            varBinds = (List<VariableBinding>) pdu.getVariableBindings();
            showVarBinds(varBinds);
            int pduType = pdu.getType();
            if ((pduType != PDU.TRAP) && (pduType != PDU.V1TRAP) && (pduType != PDU.REPORT)
                    && (pduType != PDU.RESPONSE)) {
                pdu.setErrorIndex(0);
                pdu.setErrorStatus(0);
                pdu.setType(PDU.RESPONSE);
                StatusInformation statusInformation = new StatusInformation();
                StateReference ref = cmdRespEvent.getStateReference();
                try {
                    cmdRespEvent.getMessageDispatcher().returnResponsePdu(cmdRespEvent.getMessageProcessingModel(),
                            cmdRespEvent.getSecurityModel(), cmdRespEvent.getSecurityName(), cmdRespEvent.getSecurityLevel(),
                            pdu, cmdRespEvent.getMaxSizeResponsePDU(), ref, statusInformation);
                } catch (MessageException ex) {
                    addLog(ex.toString(), logTraps);
                    System.err.println("Error while sending response: " + ex.getMessage());
                    LogFactory.getLogger(SnmpRequest.class).error(ex);
                }
            }
        }
    }

    private void loadMibs() {
        for (String[] s : mibs) {
            mibMap.put(s[0], new String[]{s[1], s[2]});
        }
    }
    // </editor-fold>

    private void startDaemonSyslog() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SyslogServerConfigIF config = new UDPNetSyslogServerConfig("0.0.0.0", 514);
                config.setUseStructuredData(true);
                config.addEventHandler(new SyslogMessagesHandler(syslog, rootPane, logPath));
                SyslogServerIF syslogServer = SyslogServer.createThreadedInstance("notebook", config);
                //config.addEventHandler(new SyslogMessagesHandler());
                syslog.setText("");
                addLog("Syslog started", syslog);
                while (true) {
                    SyslogUtility.sleep(1000l);
                }
            }
        });
        t.start();
    }

    private void startDaemonTraps() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listen(new UdpAddress("0.0.0.0/162"));
                } catch (IOException e) {
                    addLog(e.toString(), logTraps);
                }
            }
        });
        t.start();
    }

    private void addLog(String s, JEditorPane log) {
        StringBuilder sb = new StringBuilder();
        sb.append(log.getText());
        if (!sb.toString().equals("")) {
            sb.append("\n");
        }
        sb.append(s);
        log.setText(sb.toString());
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void sendEmail(String receiver, String text) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String username = "service.desk.failures@gmail.com";
                    final String password = "servicedeskfailures";

                    Properties props = new Properties();
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.host", "smtp.gmail.com");
                    props.put("mail.smtp.port", "587");

                    Session session = Session.getInstance(props,
                            new javax.mail.Authenticator() {
                        @Override
                        protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                            return new javax.mail.PasswordAuthentication(username, password);
                        }
                    });

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(username));
                    message.setRecipients(Message.RecipientType.TO,
                            InternetAddress.parse(receiver));
                    message.setSubject("Critical trap receiver");
                    message.setText(text);

                    javax.mail.Transport.send(message);
                } catch (Exception ex) {
                    System.out.println(ex.toString());
                }
            }
        });
        t.start();
    }

    private void showVarBinds(List<VariableBinding> varBinds) {
        int i = 0;
        String oid;
        boolean isCritical = false;
        StringBuilder sb = new StringBuilder();
        for (VariableBinding varBind : varBinds) {
            if (i != 0) {
                oid = varBind.getVariable().toString();
                if (oid.startsWith("1.")) {
                    if (isCritical(oid)) {
                        isCritical = true;
                        sb.append(mibMap.get(oid)[0]).append("\nDetails:\n");
                    }
                    addLog("------------Trap detected------------------------", logTraps);
                    addLog(mibMap.get(oid)[0] + "\nDetails:", logTraps);
                } else {
                    if (isCritical) {
                        sb.append("\t").append(mibMap.get(varBind.getOid().toDottedString())[0]).append("=").append(varBind.toValueString()).append("\n");

                    }
                    addLog("\t" + mibMap.get(varBind.getOid().toDottedString())[0] + "=" + varBind.toValueString(), logTraps);
                }
            }
            i++;
        }
        if (isCritical) {
            JOptionPane.showMessageDialog(rootPane, sb.toString(), "Critical trap detected", JOptionPane.ERROR_MESSAGE);
            sendEmail("c42trujillo@yahoo.com.mx", sb.toString());
            JOptionPane.showMessageDialog(rootPane, "Email sent to the service desk to its properly management", "Sent email", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean isCritical(String oid) {
        return mibMap.get(oid)[1].equals("1");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        logTraps = new javax.swing.JEditorPane();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        syslog = new javax.swing.JEditorPane();
        jLabel2 = new javax.swing.JLabel();
        btnOpenFile = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        logTraps.setBackground(new java.awt.Color(0, 0, 0));
        logTraps.setForeground(new java.awt.Color(0, 255, 102));
        jScrollPane1.setViewportView(logTraps);

        jLabel1.setText("Consola de logs:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 639, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Monitoreo de traps", jPanel1);

        syslog.setEditable(false);
        syslog.setBackground(new java.awt.Color(0, 0, 0));
        syslog.setForeground(new java.awt.Color(51, 255, 0));
        jScrollPane2.setViewportView(syslog);

        jLabel2.setText("Consola de syslog:");

        btnOpenFile.setText("Abrir archivo log");
        btnOpenFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane2)
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 406, Short.MAX_VALUE)
                        .addComponent(btnOpenFile)
                        .addGap(80, 80, 80))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addComponent(btnOpenFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Monitoreo de syslogs", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 786, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOpenFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenFileActionPerformed
        try {
            File f = new File(logPath);
            Desktop.getDesktop().open(f);
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(rootPane, "Error opening file:\n" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnOpenFileActionPerformed

    public static void main(String[] args) throws IOException {
        new Trap_manager().setVisible(true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnOpenFile;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JEditorPane logTraps;
    private javax.swing.JEditorPane syslog;
    // End of variables declaration//GEN-END:variables
}
