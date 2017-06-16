package com.mx.classes;

import java.io.IOException;
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

public class TrapListener implements CommandResponder {

    public synchronized void listen(TransportIpAddress address) throws IOException {
        AbstractTransportMapping transport;
        //starting services
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
        SecurityProtocols.getInstance().addDefaultProtocols();
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

        //Create Target
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));

        Snmp snmp = new Snmp(mtDispatcher, transport);
        snmp.addCommandResponder(this);

        transport.listen();
        // started listening traps

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
            List<VariableBinding> varBinds = (List<VariableBinding>) pdu.getVariableBindings();
            // varBinds contains SNMP trap message elements...
            // do whatever with varBinds
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
                    System.err.println("Error while sending response: " + ex.getMessage());
                    LogFactory.getLogger(SnmpRequest.class).error(ex);
                }
            }
        }
    }

    private void showVarBinds(List<VariableBinding> varBinds) {
        int i = 0;
        String oid;
        boolean isCritical = false;
        StringBuilder sb = new StringBuilder();
        for (VariableBinding varBind : varBinds) {
            if (i != 0) { // skip timeTicks
                oid = varBind.getVariable().toString();
                if (oid.startsWith("1.")) {
                    //if is critical oid{
                    isCritical = true;
                    //}                   
                }
                sb.append(varBind.toString());
                //print oid                
            }
            i++;
        }
        if (isCritical) {
            //JOptionPane.showMessageDialog(rootPane, sb.toString(), "Critical trap detected", JOptionPane.ERROR_MESSAGE);
            sendEmail("receiver_mail@gmail.com", sb.toString());
            //JOptionPane.showMessageDialog(rootPane, "Email sent to the service desk to its properly management", "Sent email", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void sendEmail(String receiver, String text) {
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

}
