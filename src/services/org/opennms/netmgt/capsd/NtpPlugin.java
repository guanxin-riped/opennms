//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.                                                            
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact: 
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.blast.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.capsd;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.utils.ParameterMap;
import org.opennms.protocols.ntp.NtpMessage;

/**
 * This plugin is used to check a host for NTP (Network Time
 * Protocol) support. This is done by contacting the specified host on
 * UDP port 123 and making a tie request.  If a valid response is 
 * returned then the server is considered an NTP server.
 *
 * @author <A HREF="mailto:mhuot@mhuot.net">mhuot</A>
 * @author <A HREF="mailto:sowmya@opennms.org">Sowmya</A>
 * @author <a href="mailto:weave@oculan.com">Weave</a>
 * @author <A HREF="http://www.opennms.org/">OpenNMS</A>
 *
 */
public final class NtpPlugin
	extends AbstractPlugin 
{
	/**
	 * </P>The protocol name that is tested by this plugin.</P>
	 */
	private final static String	PROTOCOL_NAME = "NTP";

	/**
	 * </P>The default port on which the host is checked 
	 * to see if it supports NTP.</P>
	 */
	private final static int 	DEFAULT_PORT 	= 123;

	/**
	 * Default number of retries for DNS requests
	 */
	private final static int	DEFAULT_RETRY	= 3;
	
	/**
	 * Default timeout (in milliseconds) for DNS requests.
	 */
	private final static int	DEFAULT_TIMEOUT	= 3000; // in milliseconds

	/**
	 *
	 * @param nserver	The address for the NTP server test.
	 * @param port		The port to test for NTP
	 * @param timeout	Timeout in milliseconds
	 *
	 * @return True if server, false if not.
	 */
	private boolean isServer(InetAddress nserver, int port, int retries, int timeout)
	{
		boolean isAServer = false;
		Category log = ThreadCategory.getInstance(getClass());
		
		// Allocate a communication socket
		//
		DatagramSocket socket = null;
		try 
		{
			// Allocate a socket
			//
			socket = new DatagramSocket();
			socket.setSoTimeout(timeout);

			// Allocate a receive buffer
			//
			byte[] data = new byte[512];

			for(int count = 0; count < retries && !isAServer; count++)
			{
				try 
				{
					// Construct a new DNS Address Request
					//
					NtpMessage request = new NtpMessage();

					// build the datagram packet used to request the address.
					//
					byte[] buf = new NtpMessage().toByteArray();
					DatagramPacket outpkt = new DatagramPacket(buf, buf.length, nserver, port);

					// send the output packet
					//
					socket.send(outpkt);

					// receive a resposne
					//
					DatagramPacket inpkt = new DatagramPacket(data, data.length);
					socket.receive(inpkt);
					if(inpkt.getAddress().equals(nserver))
					{
//						try
//						{
							NtpMessage msg = new NtpMessage(inpkt.getData());
							isAServer = true;
//						}
//						catch(IOException ex)
//						{
//							log.debug("Failed to match response to request, an IOException occured", ex);
//						}
					}
				} 
				catch (InterruptedIOException ex)
				{
					// discard this exception, do next loop
					//
				}
			} 
		} 
		catch (IOException ex) 
		{
			log.warn("isServer: An I/O exception during NTP resolution test.", ex);
		}
		finally
		{
			if(socket != null)
				socket.close();
		}

		return isAServer;
	}

	/**
	 * Returns the name of the protocol that this plugin
	 * checks on the target system for support.
	 *
	 * @return The protocol name for this plugin.
	 */
	public String getProtocolName()
	{
		return PROTOCOL_NAME;
	}

	/**
	 * Returns true if the protocol defined by this
	 * plugin is supported. If the protocol is not 
	 * supported then a false value is returned to the 
	 * caller.
	 *
	 * @param address	The address to check for support.
	 *
	 * @return True if the protocol is supported by the address.
	 */
	public boolean isProtocolSupported(InetAddress address)
	{
		return isServer(address, DEFAULT_PORT, DEFAULT_RETRY, DEFAULT_TIMEOUT);
	}

	/**
	 * <p>Returns true if the protocol defined by this plugin is supported.
	 * If the protocol is not supported then a false value is returned to the 
	 * caller. The qualifier map passed to the method is used by the plugin
	 * to return additional information by key-name. These key-value pairs
	 * can be added to service events if needed.</p>
	 *
	 * <p>In addition, the input qualifiers map also provides information 
	 * about how the plugin should contact the remote server. The plugin
	 * may check the qualifier map for specific elements and then adjust
	 * its behavior as necessary</p>
	 *
	 * @param address	The address to check for support.
	 * @param qualifiers	The map where qualification are set
	 *			by the plugin.
	 *
	 * @return True if the protocol is supported by the address.
	 */
	public boolean isProtocolSupported(InetAddress address, Map qualifiers)
	{
		int port    = DEFAULT_PORT;
		int timeout = DEFAULT_TIMEOUT;
		int retries = DEFAULT_RETRY;
		if(qualifiers != null)
		{
			port    = ParameterMap.getKeyedInteger(qualifiers, "port", DEFAULT_PORT);
			timeout = ParameterMap.getKeyedInteger(qualifiers, "timeout", DEFAULT_TIMEOUT);
			retries = ParameterMap.getKeyedInteger(qualifiers, "retries", DEFAULT_RETRY);
		} 
	
		boolean result = isServer(address, port, retries, timeout);
		if(result && qualifiers != null && !qualifiers.containsKey("port"))
			qualifiers.put("port", new Integer(port));

		return result;
	}
}

