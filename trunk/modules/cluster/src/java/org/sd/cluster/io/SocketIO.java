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
package org.sd.cluster.io;


import java.net.Socket;
import java.net.SocketException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.io.IOException;

/**
 * Encapsulation for socket i/o.
 * <p>
 * @author Spence Koehler
 */
class SocketIO {

  private Socket socket;
  private DataOutputStream dataOutput;
  private DataInputStream dataInput;
  
  SocketIO(Socket socket) {
    this.socket = socket;
    this.dataOutput = null;
    this.dataInput = null;
  }

  /**
   * Close the IO streams, not the socket.
   */
  public void close() throws IOException {
    if (!socket.isOutputShutdown()) socket.shutdownOutput();
    if (!socket.isInputShutdown()) {
      try {
        socket.shutdownInput();
      }
      catch (SocketException e) {
        // ignore -- socket is already disconnected at the other end.
      }
    }

    if (dataOutput != null) dataOutput.close();
    if (dataInput != null) dataInput.close();

    dataOutput = null;
    dataInput = null;
  }

  public DataOutputStream getDataOutput() throws IOException {
    if (socket.isClosed()) {
      if (dataOutput != null) {
        // need to close the dataOutput
        dataOutput.close();
      }
      this.dataOutput = null;
    }
    else {
      if (dataOutput == null && socket != null && socket.isConnected() && !socket.isOutputShutdown()) {
        dataOutput = new DataOutputStream(socket.getOutputStream());
      }
    }

    return dataOutput;
  }

  public DataInputStream getDataInput() throws IOException {
    if (socket.isClosed()) {
      if (dataInput != null) {
        // need to close the dataInput
        dataInput.close();
      }
      this.dataInput = null;
    }
    else {
      if (dataInput == null && socket != null && socket.isConnected() && !socket.isInputShutdown()) {
        dataInput = new DataInputStream(socket.getInputStream());
      }
    }

    return dataInput;
  }

}
