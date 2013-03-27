import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static void main(String argv[]) throws Exception {
        final Properties props = new Properties();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream("properties.txt"));
        try {
            props.load(bis);
        } finally {
            bis.close();
        }

        String emailBody = readFileAsString(props.getProperty("EMAIL_BODY_FILE"));

        String xSmptHeader = readFileAsString(props.getProperty("X_SMTPAPI_FILE"));
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String username = props.getProperty("SMTP_AUTH_USER");
                String password = props.getProperty("SMTP_AUTH_PWD");
                return new PasswordAuthentication(username, password);
            }
        };
        Session mailSession = Session.getDefaultInstance(props, auth);
        mailSession.setDebug(true);
        Transport transport = mailSession.getTransport();

        transport.connect();
        for (int i = 0; i < 5; ++i) {
            MimeMessage message = createMessage(props, mailSession, emailBody, xSmptHeader);
            transport.sendMessage(message,
                    message.getRecipients(Message.RecipientType.TO));
        }
        transport.close();
    }

    private static MimeMessage createMessage(Properties props, Session mailSession,
                                             String body, String xSmptHeader) throws Exception {
        MimeMessage message = new MimeMessage(mailSession);
        Multipart multipart = new MimeMultipart("alternative");

        BodyPart part1 = new MimeBodyPart();
        part1.setText("This is part 1 (text) of a multipart");

        BodyPart part2 = new MimeBodyPart();
        part2.setContent(body, "text/html");

        multipart.addBodyPart(part1);
        multipart.addBodyPart(part2);

        message.setContent(multipart);
        message.setFrom(new InternetAddress(props.getProperty("FROM")));
        message.setSubject(props.getProperty("SUBJECT"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(props.getProperty("TO")));
        message.addHeader("X-SMTPAPI", xSmptHeader);
        return message;
    }

    private static String readFileAsString(String filepath) throws IOException {
        File file = new File(filepath);
        long length = file.length();
        byte[] oneChunk = new byte[(int) length];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        try {
            dis.readFully(oneChunk);
        } finally {
            dis.close();
        }
        return new String(oneChunk, "UTF-8");
    }
}
