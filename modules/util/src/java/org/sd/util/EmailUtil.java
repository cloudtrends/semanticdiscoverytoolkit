/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Util class for sending automatic emails.
 * 
 * @author Dave Barney, Spence Koehler
 */
public class EmailUtil {

  public static final String MAIL_SERVER_ENV_KEY = "SMTP";
  public static final String DEFAULT_MAIL_SERVER = "mailserver";


  /** Command-line usage */
  public static final String USAGE = "\nUSAGE:\n\tjava " + EmailUtil.class.getName() + " <mail server> <port> <from address> <to address> <subject> <message>\n";
  
  private static String mailserver = System.getenv(MAIL_SERVER_ENV_KEY);
  static {
    if (mailserver == null) mailserver = DEFAULT_MAIL_SERVER;
    System.out.println(new Date() + ": EmailUtil is using '" + mailserver +
                       "' as the mail server. Override with '" + MAIL_SERVER_ENV_KEY +
                       "' environment variable.");
  }

  /**
   * Send an email through mailserver with the following attributes.
   * 
   * @param host     Email host
   * @param from     From line in email
   * @param to       To line in email
   * @param cc       CC line in email (can be null)
   * @param bcc      BCC line in email (can be null)
   * @param subject  Subject of email
   * @param message  body of email
   * @param html     flag as to whether message is in HTML format or raw text
   * 
   * @throws MessagingException 
   */
  public static void sendEmail(String host, String port, String from, String to, String cc, String bcc, String subject, String message, boolean html) throws MessagingException {
    //Set the host smtp address
    Properties props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);

    // create some properties and get the default Session
    Session session = Session.getDefaultInstance(props, null);
//    session.setDebug(true);

    // create a message
    Message msg = new MimeMessage(session);

    // set the from and to address
    InternetAddress addressFrom = new InternetAddress(from);
    msg.setFrom(addressFrom);

    final String[] toPieces = to.split("\\s*,\\s*");
    for (String toPiece : toPieces) {
      InternetAddress addressTo = new InternetAddress(toPiece);
      msg.addRecipient(Message.RecipientType.TO, addressTo);
    }

    if (cc != null) {
      final String[] ccPieces = cc.split("\\s*,\\s*");
      for (String ccPiece : ccPieces) {
        InternetAddress addressCc = new InternetAddress(ccPiece);
        msg.addRecipient(Message.RecipientType.CC, addressCc);
      }
    }

    if (bcc != null) {
      final String[] bccPieces = bcc.split("\\s*,\\s*");
      for (String bccPiece : bccPieces) {
        InternetAddress addressBcc = new InternetAddress(bccPiece);
        msg.addRecipient(Message.RecipientType.BCC, addressBcc);
      }
    }
    
    // Setting the Subject and Content Type
    msg.setSubject(subject);
    if (html) msg.setContent(message, "text/html");
    else msg.setContent(message, "text/plain");
    
    // Send message
    Transport.send(msg);
  }
    
  /**
   * Send an email through mailserver with the following attributes.
   * This uses environment "SMTP" value or hosts "mailserver" as the
   * default host and "25" as the default port.
   * 
   * @param from     From line in email
   * @param to       To line in email
   * @param subject  Subject of email
   * @param message  body of email
   * 
   * @throws MessagingException 
   */
  public static void sendEmail(String from, String to, String subject, String message, boolean html) throws MessagingException {
    sendEmail(mailserver, "25", from, to, null, null, subject, message, html);
  }
  
  public static void main(String[] args) throws MessagingException {
    if (args.length != 6) {
      System.err.println(USAGE);
      return;
    }
    
    sendEmail(args[0], args[1], args[2], args[3], null, null, args[4], args[5], false);
  }
}
