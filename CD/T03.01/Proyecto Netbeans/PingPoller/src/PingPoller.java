import com.sun.mail.smtp.SMTPTransport;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class PingPoller extends TimerTask {
    //NOMBRE DEL ARCHIVO CON LAS IPS DEL PING POLLER
    private static final String INPUT_FILE = "ip_addr.txt";
    //CAMBIAR TIEMPO DE INTERRUPCIÓN ENTRE EJECUCIONES
    private static final long HOURS = 0, MINUTES = 1, SECONDS = 0;
    private static final long TIME_INTERVAL = ((HOURS * 60 + MINUTES) * 60 + SECONDS) * 1000; //NO TOCAR
    //CUENTA DE CORREO (DE PREFERENCIA GMAIL) QUE SERÁ EMISOR
    private static final String MAIL_SENDR = "example@gmail.com", SENDR_PASSWORD = "123456",
    //DESTINOS DEL CORREO
            MAIL_RECV[] = {"destino@correo.com"};
    String addr[];
    boolean important[];

    public PingPoller() {
        File archivo = null;
        FileReader fr = null;
        BufferedReader br = null;

        try {
            archivo = new File(INPUT_FILE);
            fr = new FileReader(archivo);
            br = new BufferedReader(fr);
            ArrayList<String> list = new ArrayList<>();

            String ln;
            while ((ln = br.readLine()) != null) {
                list.add(ln);
            }
            
            addr = new String[list.size()];
            important = new boolean[list.size()];
            
            for(int i=0; i<list.size(); i++){
                String[] read = list.get(i).split(" ");
                addr[i] = read[0];
                important[i] = read[1].equalsIgnoreCase("true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fr) {
                    fr.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
    
    public boolean sendMail(String[] addr_down){
        String msg_cont = "Mensaje de alerta. No se pudieron alcanzar las siguientes direcciones de red:\n";
        msg_cont += new Date() + "\n";
        for(int i=0; i<addr_down.length; i++){
            msg_cont+= "- "+addr_down[i]+"\n";
        }
        try {
            Properties props = System.getProperties();
            
            props.put("mail.smtps.host","smtp.gmail.com");
            props.put("mail.smtps.auth","true");
            
            Session session = Session.getInstance(props, null);
            Message msg = new MimeMessage(session);
            
            msg.setFrom(new InternetAddress(MAIL_SENDR));
            
            InternetAddress[] addressTo = new InternetAddress[MAIL_RECV.length];
            for (int i = 0; i < MAIL_RECV.length; i++) {
                addressTo[i] = new InternetAddress(MAIL_RECV[i]);
            }
            msg.setRecipients(Message.RecipientType.TO, addressTo);
            
            msg.setSubject("Alerta Ping Poller: Direcciones importantes inalcanzables");
            msg.setText(msg_cont);
            msg.setSentDate(new Date());
            SMTPTransport t =
                    (SMTPTransport)session.getTransport("smtps");
            t.connect("smtp.gmail.com", MAIL_SENDR, SENDR_PASSWORD);
            t.sendMessage(msg, msg.getAllRecipients());
            t.close();
            return true;
        } catch (MessagingException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void run() {
        InetAddress ping;
        String ip;
        ArrayList<String> down = new ArrayList<>();
        
        for (int i = 0; i < addr.length; i++) {
            ip = addr[i];
            try {
                long start = 0, finish = 0, time;
                ping = InetAddress.getByName(ip);
                start = new GregorianCalendar().getTimeInMillis();
                if (ping.isReachable(5000)) {
                    finish = new GregorianCalendar().getTimeInMillis();
                    time = finish - start;
                    System.out.println(ip + " - reponse time: " + time + " ms");
                } else {
                    System.out.println(ip + " - not reachable");
                    if(important[i])
                        down.add(ip);
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
        
        if(down.size() > 0)
            if(sendMail(down.toArray(new String[0])))
                System.out.println("Mensaje enviado");
    }

    public static void main(String[] args) {
        PingPoller poller = new PingPoller();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(poller, 0, TIME_INTERVAL);
    }
}
