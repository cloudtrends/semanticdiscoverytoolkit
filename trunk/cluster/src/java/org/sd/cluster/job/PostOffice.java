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
package org.sd.cluster.job;


import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.Config;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Shutdownable;
import org.sd.util.EmailUtil;
import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.mail.MessagingException;

/**
 * Utility to collect and send e-mail messages.
 * <p>
 * @author Spence Koehler
 */
public class PostOffice implements Shutdownable {
  
  /**
   * The number of millis to wait to finish collecting a batch before sending.
   */
//todo: parameterize this.
  private static final long BATCH_DELAY = 10000;

  /**
   * The number of threads to use for simultaneous sending of emails.
   */
  private static final int NUM_EMAIL_THREADS = 5;

  private static final String SUBJECT_BASE = "ClusterBatch ";


  private ScheduledExecutorService scheduler;
  private Map<String, MessageBatch> id2batch;  //batchID to MessageBatch map
  private String batchSenderString;
  private boolean disabled;

  public PostOffice(Context context) {
    this.scheduler = Executors.newScheduledThreadPool(NUM_EMAIL_THREADS);
    this.id2batch = new HashMap<String, MessageBatch>();

    String senderString = "Cluster";

    // register with cluster context for shutdown
    if (context instanceof ClusterContext) {
      final ClusterContext clusterContext = (ClusterContext)context;
      clusterContext.getJobManager().registerShutdownable(this);

      final Config config = clusterContext.getConfig();
      senderString = config.getUser() + " " + senderString;
    }

    this.batchSenderString = senderString + " <no-reply@semanticdiscovery.com>";
    this.disabled = false;
  }

  public void submit(PostOfficeMessage message) {
    System.out.println(new Date() + ": NOTE : PostOffice received message! batchId=" +
                       message.getBatchID() + ", subject=" +
                       message.getSubject() + ", dest=" +
                       message.getDestEmailAddresses());

    if (message.getBatchID() == null) {
      // send message right away.
      sendMessage(message);
    }
    else {
      // schedule message to be sent after the batch delay
      addBatchMessage(message);
    }
  }

  /**
   * Shutdown this instance.
   */
  public void shutdown(boolean now) {
    if (scheduler != null) {
      synchronized (scheduler) {
        if (!scheduler.isShutdown()) {
          if (now) {
            scheduler.shutdownNow();
          }
          else {
            scheduler.shutdown();
          }
        }
      }
    }
  }

  protected final void sendMessage(PostOfficeMessage message) {
    System.out.println(new Date() + " : PostOffice (disabled=" + disabled +
                       ") sending message! subject=" + message.getSubject() +
                       ", dest=" + message.getDestEmailAddresses());

    if (!disabled) {
      try {
        EmailUtil.sendEmail(message.getSender(),
                            message.getDestEmailAddresses(),
                            message.getSubject(),
                            message.getMessage(),
                            message.isHtml());
      }
      catch (MessagingException e) {
        disable();
        System.err.println(new Date() + ": ERROR : PostOffice couldn't send message: " + message.getSubject());
        e.printStackTrace(System.err);
      }
    }
  }

  /**
   * Disable this post-office.
   */
  public final void disable() {
    this.disabled = true;
  }

  /**
   * Determine whether this post-office is disabled.
   */
  public final boolean isDisabled() {
    return disabled;
  }

  private final void addBatchMessage(PostOfficeMessage message) {
    final String batchID = message.getBatchID();

    System.out.println(new Date() + ": PostOffice addingBatchMessage batchID=" + batchID);

    synchronized (id2batch) {
      MessageBatch messageBatch = id2batch.get(batchID);
      if (messageBatch == null) {
        messageBatch = new MessageBatch(batchID, batchSenderString);
        id2batch.put(batchID, messageBatch);

        // schedule batch to be sent
        scheduler.schedule(new Mailer(this, messageBatch), BATCH_DELAY, TimeUnit.MILLISECONDS);

        System.out.println(new Date() + ": PostOffice scheduledBatch '" + batchID + "'");
      }
      messageBatch.addMessage(message);
    }
  }

  protected final void removeBatch(MessageBatch messageBatch) {
    synchronized (id2batch) {
      id2batch.remove(messageBatch.batchID);
    }
  }


  private static final class MessageBatch {
    final String batchID;
    final String batchSenderString;
    final List<PostOfficeMessage> messages;

    public MessageBatch(String batchID, String batchSenderString) {
      this.batchID = batchID;
      this.batchSenderString = batchSenderString;
      this.messages = new ArrayList<PostOfficeMessage>();
    }

    public void addMessage(PostOfficeMessage message) {
      this.messages.add(message);
    }

    /**
     * Create a digest message from this batch's messages.
     */
    public PostOfficeMessage createDigestMessage() {

      // create a set of recipients, turn into a string
      final String destEmailAddresses = collectRecipients();

      // create sender string
      final String sender = batchSenderString;

      // create subject string
      final String subject = SUBJECT_BASE + " (" + messages.size() + " messages)";

      // create message string
      final String message = createDigestMessageContent();

      return new PostOfficeMessage(null, destEmailAddresses,
                                   sender, subject, message,
                                   true/*html*/);
    }

    private final String collectRecipients() {
      final Set<String> result = new HashSet<String>();
      
      for (PostOfficeMessage message : messages) {
        final String[] recipients = message.getDestEmailAddresses().split("\\s*,\\s*");
        for (String recipient : recipients) {
          result.add(recipient);
        }
      }

      return StringUtil.concat(result, ",");
    }

    private final String createDigestMessageContent() {
      final StringBuilder result = new StringBuilder();

      //todo: group messages by subject and by sender

      result.append("<table border=\"1\">");
      result.append("<tr><td colspan=\"2\">BatchID=").append(batchID).append("</td></tr>");

      for (PostOfficeMessage message : messages) {
        result.
          append("<tr>").
//          append("<td>").append(message.getSender()).append("<\td>").
          append("<td>").append(message.getSubject()).append("<\td>").
          append("<td>").append(htmlifyMessage(message.getMessage())).append("<\td>").
          append("</tr>");
      }
      result.append("</table>");

      return result.toString();
    }

    private final String htmlifyMessage(String message) {
      message = message.replaceAll("\n", "\n<br>\n");
      message = message.replaceAll("\t", "&nbsp;&nbsp;");
      return message;
    }
  }

  private static final class Mailer implements Runnable {
    private PostOffice postOffice;
    private MessageBatch messageBatch;

    public Mailer(PostOffice postOffice, MessageBatch messageBatch) {
      this.postOffice = postOffice;
      this.messageBatch = messageBatch;
    }

    public void run() {
      // remove the message batch from the post office (thread safe)
      postOffice.removeBatch(messageBatch);

      // compile a digest message from the message batch
      final PostOfficeMessage digest = messageBatch.createDigestMessage();

      System.out.println(new Date() + ": PostOffice sendingBatch '" + messageBatch.batchID + "'");

      // send the digest message
      postOffice.sendMessage(digest);
    }
  }
}
