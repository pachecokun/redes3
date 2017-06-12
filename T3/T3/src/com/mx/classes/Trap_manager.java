/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mx.classes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JOptionPane;
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
        log.setEditable(false);
        startDaemon();
        loadMibs();
    }

    // <editor-fold defaultstate="collapsed" desc="SNMP session code"> 
    public synchronized void listen(TransportIpAddress address) throws IOException {
        addLog("Starting listening for traps");
        AbstractTransportMapping transport;
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
        addLog("Adding security protocols");
        SecurityProtocols.getInstance().addDefaultProtocols();
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

        //Create Target
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));

        Snmp snmp = new Snmp(mtDispatcher, transport);
        snmp.addCommandResponder(this);

        transport.listen();
        addLog("Started listening traps");

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
                    addLog(ex.toString());
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
    private void startDaemon() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listen(new UdpAddress("0.0.0.0/162"));
                } catch (IOException e) {
                    addLog(e.toString());
                }
            }
        });
        t.start();
    }

    private void addLog(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(log.getText());
        if (!sb.toString().equals("")) {
            sb.append("\n");
        }
        sb.append(s);
        log.setText(sb.toString());
    }

    private boolean sendEmail(String receiver, String text) {
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

            return true;
        } catch (Exception ex) {
            return false;
        }

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
                    } else {
                        addLog("------------Trap detected------------------------");
                        addLog(mibMap.get(oid)[0] + "\nDetails:");
                    }
                } else {
                    if (isCritical) {
                        sb.append("\t").append(mibMap.get(varBind.getOid().toDottedString())[0]).append("=").append(varBind.toValueString()).append("\n");

                    } else {
                        addLog("\t" + mibMap.get(varBind.getOid().toDottedString())[0] + "=" + varBind.toValueString());
                    }
                }
            }
            i++;
        }
        if (isCritical) {
            JOptionPane.showMessageDialog(rootPane, sb.toString(), "Critical trap detected", JOptionPane.ERROR_MESSAGE);
            if (sendEmail("c42trujillo@yahoo.com.mx", sb.toString())) {
                JOptionPane.showMessageDialog(rootPane, "Email sent to the service desk to its properly management", "Sent email", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(rootPane, "Email sent to the service desk", "Error", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    private boolean isCritical(String oid) {
        return mibMap.get(oid)[1].equals("1");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        log = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setViewportView(log);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(15, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                .addGap(23, 23, 23))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Trap_manager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Trap_manager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Trap_manager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Trap_manager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Trap_manager().setVisible(true);
                } catch (IOException e) {
                    System.out.println(e.toString());
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JEditorPane log;
    // End of variables declaration//GEN-END:variables
}
