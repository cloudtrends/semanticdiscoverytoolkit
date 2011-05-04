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
package org.sd.cluster.config;


import org.sd.cluster.io.Response;
import org.sd.cluster.job.PostOfficeMessage;
import org.sd.cluster.util.LogManager;
import org.sd.cluster.util.LogManager.LogInfo;
import org.sd.cluster.util.LogManager.LogVisitor;
import org.sd.io.FileUtil;
import org.sd.util.EmailUtil;
import org.sd.util.StringUtil;

import javax.mail.MessagingException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A log visitor for cluster logs.
 * <p>
 * @author Spence Koehler
 */
public class ClusterLogVisitor implements LogVisitor {
  
  public static final String LOG_SETTINGS_FILENAME = "log-visitor.txt";
  private static final String EMAIL_FROM = "ClusterNotification <no-reply@semanticdiscovery.com>";


// This class's behavior is controlled by a resource file:
//
// cluster/conf/log-visitor.txt  (copied from core/resources/log/default-log-visitor.txt or overridden location on deploy)
//
// The file format for the log resource is:
//
//  @postOfficeNodeGroup|messageTimeout
//  nodeGroup | logType | notifyFlag | maxLines | notificationAddresses | regexKeys [| fromAddress]
//  regexKey:regex
//
// where,
//   blank lines and those starting with a # are ignored.
//
//  In the lines starting with '@',
//   postOfficeNodeGroup is the name of a node (nodeName-jvmNum) or group from which email messages can be sent.
//   messageTimeout is the number of millis to use for a timeout when sending a post office message.
//  In the '|'-delimitted lines,
//   nodeGroup is a comma-separated list consisting of "all" or nodeName-jvmNum or groupName tokens.
//   logType is "both", "err", or "out"
//   notifyFlag is "on" or "off"
//   maxLines is "0" or a number identifying at which point a log is no longer visited.
//   notificationAddresses is a comma-separated list of e-mail addresses to send notifications to.
//   regexKeys is a comma-separated list of regexKeys identifying applicable regex patterns.
//
//  In the ':'-delimitted lines,
//   regexKey is a case-sensitive string identifying the regex, cross-correlated with the regexKeys field
//   all data after the colon is taken to be a regex pattern to be compiled and applied to each log line.
//
// NOTE that non-empty fields from lines read later from the file supercede those read earlier.
//
// An example log resource file would be:
//
//  # send messages to the controller node(s) with a timeout of 10 seconds
//  @controller|10000
//
//  # turn on notifications for all error logs
//  all|err|on|0|cluster-notifications@semanticdiscovery.com|exception,error
//
//  # turn off notifications for all output logs
//  all|out|off|1000|cluster-notifications@semanticdiscovery.com|note
//
//  # alert with errors and warnings from controller nodes
//  controller|both|||exception,error,warning
//
//  # regex patterns for error log alerts
//  exception:^.*\bException\b.*$
//  error:^.*\bERROR\b.*$
//
//  # regex pattern for output log alerts (when turned "on")
//  note:^.*\bNOTE\b.*$
//
//  # regex pattern for detecting warnings
//  warning:^.*\bWARNING\b.*$
//

  private File logSettingsFile;
  private Config config;
  private ClusterDefinition clusterDef;
  private Console console;
  private long lastModified;

  private Settings outSettings;
  private Settings errSettings;
  private Map<String, Pattern> key2pattern;
  private String poGroup;  // post-office group (or node in form nodeName-jvmNum)
  private int poTimeout;   // timeout when sending message to poGroup

  public ClusterLogVisitor(Config config, ClusterDefinition clusterDef, Console console) {
    this.logSettingsFile = new File(ConfigUtil.getClusterConfDir() + LOG_SETTINGS_FILENAME);
    this.config = config;
    this.clusterDef = clusterDef;
    this.console = console;
    this.lastModified = 0L;

    this.outSettings = new Settings();
    this.errSettings = new Settings();
    this.key2pattern = new HashMap<String, Pattern>();
  }

  /**
   * Reset the visitor before visiting each log line.
     *
     * @return true to continue with visiting log lines; false to abort visit.
   */
  public boolean reset(LogInfo visitingLogInfo, int nextLineToVisit) {
    boolean result = false;

    if (logSettingsFile.exists()) {

      // check on freshness of resources, reload if changed
      if (logSettingsFile.lastModified() > lastModified) {
        loadSettings();
      }

      // get applicable settings
      final Settings settings = visitingLogInfo.isErrLog ? errSettings : outSettings;
      
      if (settings.canNotify(nextLineToVisit)) {
        result = true;

        settings.clearNotifications(visitingLogInfo.logFile.getName());
      }
    }

    return result;
  }

  public void disable() {
    this.errSettings.setNotifyFlag(false);
    this.outSettings.setNotifyFlag(false);
  }

  /**
   * Visit the log line.
   *
   * @return true to continue visiting logLines; false to stop visiting.
   */
  public boolean visit(LogInfo visitingLogInfo, String logLine, int lineNum) {
    boolean result = false;

    // get applicable settings
    final Settings settings = visitingLogInfo.isErrLog ? errSettings : outSettings;

    if (settings.canNotify(lineNum)) {  // if notify is enabled
      for (String patternKey : settings.getPatternKeys()) {
        final Pattern pattern = key2pattern.get(patternKey);
        if (pattern != null) {
          final Matcher m = pattern.matcher(logLine);
          if (m.matches()) {
            settings.addNotification(patternKey, logLine);
          }
        }
      }

      result = true;
    }

    return result;
  }

  /**
   * Flush the visitor after visiting all log lines.
   *
   * @return true if an actionable event occurred while visiting.
   */
  public boolean flush(LogInfo visitingLogInfo) {
    final Settings settings = visitingLogInfo.isErrLog ? errSettings : outSettings;
    return settings.hasNotifications();
  }

  public void sendReport() {
    final Set<String> emailRecipients = new HashSet<String>();
    final StringBuilder message = new StringBuilder();

    // NOTE: we'll just send everybody the full message

    if (outSettings.hasNotifications()) {
      for (String notifyAddress : outSettings.getNotifyAddresses()) {
        emailRecipients.add(notifyAddress);
      }
      outSettings.addNotificationMessage(message);
    }

    if (errSettings.hasNotifications()) {
      for (String notifyAddress : errSettings.getNotifyAddresses()) {
        emailRecipients.add(notifyAddress);
      }
      errSettings.addNotificationMessage(message);
    }

    if (emailRecipients.size() > 0 && message.length() > 0) {
      final String subject = config.getUser() + "@" + config.getNodeName();
      final String theMessage = message.toString();
      final String emailRecipientString = StringUtil.concat(emailRecipients, ",");

      sendEmailMessage(EMAIL_FROM, emailRecipientString, subject, theMessage, false);
    }
  }

  private final void sendEmailMessage(String from, String emailRecipientString, String subject, String theMessage, boolean isHtml) {
    if (console == null) {
      // if there is no console, then try to send the message directly from this node.
      // note that if the machine is not properly set up with a tunnel through the host "mailserver", this will fail.
      try {
        EmailUtil.sendEmail(from, emailRecipientString, subject, theMessage, isHtml);
      }
      catch (MessagingException e) {
        disable();
        System.err.println(new Date() + ": ERROR : ClusterLogVisitor (non-console) couldn't send message: " + subject);
        e.printStackTrace();
      }
    }
    else {
      // email through a central aggregator
      final PostOfficeMessage poMessage = new PostOfficeMessage("ClusterLogs", emailRecipientString, from, subject, theMessage, isHtml);

      try {
        final Response[] responses = console.sendMessageToNodes(poMessage, poGroup, poTimeout, false);
        if (responses != null && responses.length > 0 && responses[0] instanceof BooleanResponse) {
          final BooleanResponse booleanResponse = (BooleanResponse)(responses[0]);
          if (!booleanResponse.getValue()) {
            // need to disable sending email messages.
            disable();

            System.err.println(new Date() + ": ClusterLogVisitor couldn't get response from PostOffice (" +
                               poGroup + " timeOut=" + poTimeout + ")! Disabling (until settings are reloaded)!");
          }
        }
      }
      catch (ClusterException e) {
        disable();
        System.err.println(new Date() + ": ERROR : ClusterLogVisitor (w/console) couldn't send message:\n" +
                           theMessage);
        e.printStackTrace();
      }
    }
  }


  private final void loadSettings() {
    System.out.println(new Date() + ": NOTE : ClusterLogVisitor Loading logSettingsFile '" +
                       logSettingsFile + "'");

    try {
      final BufferedReader reader = FileUtil.getReader(logSettingsFile);
      String line = null;
      while ((line = reader.readLine()) != null) {
        if ("".equals(line)) continue;

        final char firstChar = line.charAt(0);
        if (firstChar == '#') continue;

        if (firstChar == '@') {
          // parse out post office node (or group) name and timeout of form:
          // @group|timeOut
          loadPostOfficeId(line.substring(1));
        }
        else {
          final int cPos = line.indexOf(':');
          final int vbPos = line.indexOf('|');

          if (cPos >= 0 && (vbPos < 0 || cPos < vbPos)) {
            // load regex pattern
            loadRegexPattern(line, cPos);
          }
          else if (vbPos >= 0) {
            // load settings line
            loadSettingsLine(line);
          }
          else {
            System.err.println(new Date() + ": WARNING : ClusterLogVisitor encountered unrecognized logLine=" + line +
                               " file=" + logSettingsFile);
          }
        }
      }
      reader.close();
      this.lastModified = logSettingsFile.lastModified();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private final void loadPostOfficeId(String line) {
    final String[] pieces = StringUtil.splitFields(line, 2);
    this.poGroup = pieces[0];
    this.poTimeout = "".equals(pieces[1]) ? 10000 : Integer.parseInt(pieces[1]);
  }

  private final void loadRegexPattern(String line, int cPos) {
    final String key = line.substring(0, cPos);
    final Pattern pattern = Pattern.compile(line.substring(cPos + 1));
    key2pattern.put(key, pattern);
  }

  private final void loadSettingsLine(String line) {
    final String[] pieces = StringUtil.splitFields(line, 6);

    // first make sure this setting line applies to this node
    final String nodeGroup = pieces[0];
    boolean applies = ("".equals(nodeGroup) ||   // count <empty> as applicable
                       "all".equals(nodeGroup.toLowerCase()) ||
                       config.getNodeName().equals(nodeGroup) ||
                       config.getMachineName().equals(nodeGroup));
    if (!applies) {
      final List<String> groupNodes = clusterDef.getGroupNodeNames(nodeGroup, true);
      if (groupNodes != null) {
        applies = groupNodes.contains(config.getNodeName());
      }
    }

    if (!applies) return;  // this line doesn't apply here.

    // next determine which type(s) of logs we're dealing with
    final String logType = pieces[1];
    Settings[] settings =
      ("".equals(logType) || "both".equals(logType)) ?
      new Settings[]{outSettings, errSettings} :
    ("out".equals(logType) ? new Settings[]{outSettings} :
     "err".equals(logType) ? new Settings[]{errSettings} : null);

    if (settings == null) {
      System.err.println(new Date() + ": WARNING : ClusterLogVisitor encountered unexpected logType=" + logType + "! ignoring.");
      return;  // this line doesn't apply here.
    }

    // adjust the indicated fields for the selected settings
    for (Settings setting : settings) {
      setting.setNotifyFlag(pieces[2]);
      setting.setMaxLines(pieces[3]);
      setting.setNotifyAddresses(pieces[4]);
      setting.setPatternKeys(pieces[5]);
      setting.setEmailFrom((pieces.length > 6) ? pieces[6] : EMAIL_FROM);
    }
  }


  private static final class Settings {
    private boolean notifyFlag;
    private int maxLines;
    private String[] notifyAddresses;
    private String[] patternKeys;
    private String emailFrom;

    private Map<String, List<String>> notifications;
    private String logName;

    public Settings() {
      this.notifyFlag = false;
      this.maxLines = 0;
      this.notifyAddresses = null;
      this.patternKeys = null;
      this.emailFrom = null;
      this.notifications = null;
      this.logName = "<unknown>";
    }

    public void setNotifyFlag(String notifyFlagString) {
      if (!"".equals(notifyFlagString)) {
        notifyFlagString = notifyFlagString.toLowerCase();
        this.notifyFlag =
          "on".equals(notifyFlagString) ||
          "true".equals(notifyFlagString) ||
          "yes".equals(notifyFlagString);
      }
    }

    public void setNotifyFlag(boolean notifyFlag) {
      this.notifyFlag = notifyFlag;
    }

    public boolean canNotify(int lineNum) {
      boolean result = false;

      if (notifyFlag && notifyAddresses != null && patternKeys != null) {
        result = (maxLines <= 0 || lineNum < maxLines);
      }

      return result;
    }


    public void clearNotifications(String logName) {
      if (notifications != null) notifications.clear();
      this.logName = logName;
    }

    public void addNotification(String patternKey, String logLine) {
      if (notifications == null) {
        notifications = new HashMap<String, List<String>>();
      }

      List<String> logLines = notifications.get(patternKey);
      if (logLines == null) {
        logLines = new ArrayList<String>();
        notifications.put(patternKey, logLines);
      }
      logLines.add(logLine);
    }

    public boolean hasNotifications() {
      return notifications != null && notifications.size() > 0;
    }

    public void addNotificationMessage(StringBuilder message) {
      if (hasNotifications()) {
        if (message.length() > 0) message.append("\n\n");

        message.
          append("Found ").append(notifications.size()).append(" patterns in ").
          append(logName).append("\n\t").append(notifications.keySet()).append("\n");

        for (Map.Entry<String, List<String>> entry : notifications.entrySet()) {
          final String key = entry.getKey();
          final List<String> values = entry.getValue();

          message.append("\n").append(key).append(" : (").append(values.size()).append(")\n");
          for (String string : values) {
            message.append("\t").append(string).append("\n");
          }
        }
      }
    }


    public boolean getNotifyFlag() {
      return notifyFlag;
    }

    public void setMaxLines(String maxLinesString) {
      if (!"".equals(maxLinesString)) {
        this.maxLines = Integer.parseInt(maxLinesString);
      }
    }

    public int getMaxLines() {
      return maxLines;
    }

    public void setNotifyAddresses(String notifyAddressesString) {
      if (!"".equals(notifyAddressesString)) {
        this.notifyAddresses = notifyAddressesString.split("\\s*,\\s*");
      }
    }

    public String[] getNotifyAddresses() {
      return notifyAddresses;
    }

    public void setPatternKeys(String patternsString) {
      if (!"".equals(patternsString)) {
        this.patternKeys = patternsString.split("\\s*,\\s*");
      }
    }

    public String[] getPatternKeys() {
      return patternKeys;
    }

    public void setEmailFrom(String emailFrom) {
      if (!"".equals(emailFrom)) {
        this.emailFrom = emailFrom;
      }
    }

    public String getEmailFrom() {
      return emailFrom;
    }
  }
}
