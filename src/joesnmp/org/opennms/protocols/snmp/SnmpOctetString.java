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

package org.opennms.protocols.snmp;

import java.io.Serializable;

import org.opennms.protocols.snmp.asn1.AsnDecodingException;
import org.opennms.protocols.snmp.asn1.AsnEncoder;
import org.opennms.protocols.snmp.asn1.AsnEncodingException;

/**
 * Implements the ASN1.UNIVERSAL Octet String datatype. The
 * string is a sequence of 8-bit octet data. The format of
 * the 8-bit characters are defined by the application.
 *
 * @version	1.1.1.1
 * @author	<a href="mailto:weave@oculan.com>Brian Weaver</a>
 */
public class SnmpOctetString extends Object
	implements SnmpSyntax, Cloneable, Serializable
{
	/**
	 * Required to allow evolution of serialization format.
	 */
	static final long serialVersionUID =  1848780739976444105L; // generated by serialver tool

	/**
	 * The actual octet string data (UTF-8)
	 *
	 */
	private byte[]	m_data;

	/**
	 * The ASN.1 value for the OCTET STRING type.
	 *
	 */
	public static final byte ASNTYPE = SnmpSMI.SMI_STRING;

	/**
	 * This can be used by a derived class to <em>force</em>
	 * the data contained by the octet string. The data is
	 * not duplicated, only the reference to the array
	 * is stored. No validation of data is performed at all.
	 *
	 * @param data	The new data buffer.
	 */
	protected void assumeString(byte[] data)
	{
		m_data = data;
	}

	/**
	 * The default class constructor. Constructs an Octet
	 * String with a length of zero and no data.
	 *
	 */
	public SnmpOctetString( )
	{
		m_data = null;
	}

	/**
	 * Constructs an octet string with the 
	 * inital value equal to data. The data
	 * is actually copied so changes to the
	 * data reference do not affect the Octet
	 * string object.
	 *
	 * @param data	The data to be copied to self
	 *
	 */
	public SnmpOctetString(byte[] data)
	{
		this();
		if(data != null)
		{
			m_data = new byte[data.length];
			System.arraycopy(data,
				         0,
				         m_data,
			                 0,
			                 data.length);
		}
	}
	
	/**
	 * Class copy constructor. Constructs and octet
	 * string object that is a duplicate of the 
	 * object second.
	 *
	 * @param second The object to copy into self
	 *
	 */
	public SnmpOctetString(SnmpOctetString second)
	{
		this(second.m_data);
	}

	/**
	 * Returns a reference to the internal object string.
	 * Changes to this byte array WILL affect the octet
	 * string object. These changes should not be made 
	 * lightly. 
	 *
	 * @return A reference to the internal byte array.
	 */
	public byte[] getString( )
	{
		return m_data;
	}

	/**
	 * Sets the internal string array so that it is
	 * identical to the passed array. The array is 
	 * actually copied so that changes to data after
	 * the construction of the object are not reflected
	 * in the SnmpOctetString Object.
	 *
	 * @param data	The new octet string data.
	 *
	 */
	public void setString(byte[] data)
	{
		m_data = null;
		if(data != null)
		{
			m_data = new byte[data.length];
			System.arraycopy(data, 
				         0,
				         m_data, 
				         0,
				         data.length);
		}
	}

	/**
	 * Sets the internal octet string equal to the
	 * converted stirng via the method getBytes().
	 * This may cause some data corruption since the
	 * conversion is platform specific.
	 *
	 * @param data	The new octet string data.
	 *
	 * @see java.lang.String#getBytes()
	 */
	public void setString(String data)
	{
		m_data = null;
		if(data != null)
		{
			m_data = data.getBytes();
		}
	}

	/**
	 * Returns the internal length of the octet string.
	 * This method is favored over recovereing the length
	 * from the internal array. The method compensates for
	 * a null set of data and returns zero if the internal
	 * array is null.
	 *
	 * @return The length of the octet string.
	 *
	 */
	public int getLength()
	{
		int len = 0;
		if(m_data != null)
			len = m_data.length;
		return len;
	}

	/**
	 * Returns the ASN.1 type identifier for the Octet String.
	 *
	 * @return The ASN.1 identifier.
	 *
	 */
	public byte typeId()
	{
		return ASNTYPE;
	}

	/**
	 * Encodes the ASN.1 octet string using the passed encoder and stores
	 * the results in the passed buffer. An exception is thrown if an 
	 * error occurs with the encoding of the information. 
	 *
	 * @param buf		The buffer to write the encoded information.
	 * @param offset	The offset to start writing information
	 * @param encoder	The encoder object.
	 *
	 * @return The offset of the byte immediantly after the last encoded byte.
	 *
	 * @exception AsnEncodingException Thrown if the encoder finds an error in the
	 *	buffer.
	 */
	public int encodeASN(byte[]	buf,
			     int	offset,
			     AsnEncoder encoder) throws AsnEncodingException
	{
		if(m_data == null)
			throw new AsnEncodingException("No data in octet string");

		return encoder.buildString(buf, offset, typeId(), m_data);
	}

	/**
	 * Decodes the ASN.1 octet string from the passed buffer. If an error
	 * occurs during the decoding sequence then an AsnDecodingException is
	 * thrown by the method. The value is decoded using the AsnEncoder
	 * passed to the object.
	 *
	 * @param buf		The encode buffer 
	 * @param offset	The offset byte to begin decoding
	 * @param encoder	The decoder object.
	 *
	 * @return The index of the byte immediantly after the last decoded
	 *	byte of information.
	 *
	 * @exception AsnDecodingException Thrown by the encoder if an error
	 *	occurs trying to decode the data buffer.
	 */
	public int decodeASN(byte[]	buf,
			     int	offset,
			     AsnEncoder encoder) throws AsnDecodingException
	{
		Object[] rVals = encoder.parseString(buf, offset);

		if(((Byte)rVals[1]).byteValue() != typeId())
			throw new AsnDecodingException("Invalid ASN.1 type");

		m_data = (byte[])rVals[2];

		return ((Integer)rVals[0]).intValue();
	}

	/**
	 * Creates a duplicate copy of the object and
	 * returns it to the caller.
	 *
	 * @return A newly constructed copy of self
	 *
	 */
	public SnmpSyntax duplicate() 
	{
		return new SnmpOctetString(this);
	}

	/**
	 * Creates a duplicate copy of the object and
	 * returns it to the caller.
	 *
	 * @return A newly constructed copy of self
	 *
	 */
	public Object clone()
	{
		return new SnmpOctetString(this);
	}

	/** 
	 * Returns a string representation of the object. If
	 * the object contains non-printable characters then
	 * the contents are printed in hexidecimal.
	 *
	 */
	public String toString()
	{
		//
		// check for non-printable characters. If they
		// exist then print the string out as hexidecimal
		//
		boolean asHex = false;
		for(int i = 0; i < m_data.length; i++)
		{
			byte b = m_data[i];
			if((b < 32 && b != 10 && b != 13) ||  b == 127)
			{
				asHex = true;
				break;
			}
		}
		
		String rs = null;
		if(asHex)
		{
			//
			// format the string for hex
			//
			StringBuffer b = new StringBuffer();
			//b.append("SNMP Octet String [length = " + m_data.length + ", fmt = HEX] = [");
			for(int i = 0; i < m_data.length; ++i)
			{
				int x = (int)m_data[i] & 0xff;
				if(x < 16)
					b.append('0');
				b.append(Integer.toString(x,16).toUpperCase());
				
				if(i < m_data.length-1)
					b.append(' ');
			}
			//b.append(']');
			rs = b.toString();
		}
		else
		{
			//
			// raw output
			//
			//rs = "SNMP Octet String [length = " + m_data.length + ", fmt = RAW] = [" + new String(m_data) + "]";
			rs = new String(m_data);
		}
			
		
		return rs;
	}
}
