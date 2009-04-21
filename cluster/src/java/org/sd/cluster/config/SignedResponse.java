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


import org.sd.cio.MessageHelper;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Response;
import org.sd.cluster.config.ClusterContext;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A response that identifies the server that is responding.
 * <p>
 * @author Spence Koehler
 */
public abstract class SignedResponse extends Response {

  private String user;
  private String machineName;
  private int jvmNum;
  private int serverPort;
  private String signature;

  private ClusterContext _clusterContext;

  protected SignedResponse() {
    this.user = null;
    this.machineName = null;
    this.jvmNum = -1;
    this.serverPort = -1;
    this.signature = null;
    this._clusterContext = null;
  }

  protected SignedResponse(Context context) {
    if (context != null) {
      this._clusterContext = (ClusterContext)context;
      final Config config = _clusterContext.getConfig();

      this.user = config.getUser();
      this.machineName = config.getMachineName();
      this.jvmNum = config.getJvmNum();
      this.serverPort = config.getServerPort();
      this.signature = config.getName();
    }
    else {
      this.signature = "<unsigned>";
    }
  }

  public String getUser() {
    return user;
  }

  public String getMachineName() {
    return machineName;
  }

  public int getJvmNum() {
    return jvmNum;
  }

  public String getNodeName() {
    return machineName + "-" + Integer.toString(jvmNum);
  }

  public int getServerPort() {
    return serverPort;
  }

  public String getSignature() {
    return signature;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, user);
    MessageHelper.writeString(dataOutput, machineName);
    dataOutput.writeInt(jvmNum);
    dataOutput.writeInt(serverPort);
    MessageHelper.writeString(dataOutput, signature);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.user = MessageHelper.readString(dataInput);
    this.machineName = MessageHelper.readString(dataInput);
    this.jvmNum = dataInput.readInt();
    this.serverPort = dataInput.readInt();
    this.signature = MessageHelper.readString(dataInput);
  }
  
  /**
   * Get the current cluster context (if active).
   */
  protected ClusterContext getClusterContext() {
    return _clusterContext;
  }
}
