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
//
// Tab Size = 8
//
package org.opennms.protocols.snmp;


/**
 * Defines a SNMPv1 32-bit counter object. The object is a 
 * 32-bit unsigned value that is incremented periodically
 * by an agent normally. 
 *
 * The object inherients and uses most of the methods defined
 * by the SnmpUInt32 class. This class does not define any 
 * specific data, but is instead used to override the ASN.1 type
 * of the base class.
 *
 * @author  <a href="mailto:weave@oculan.com">Brian Weaver</a>
 */
public class SnmpCounter32
	extends SnmpUInt32
{
	/**
	 * required for version control of serialization.
	 *
	 */
	static final long serialVersionUID = -5722134291677293080L;

	/**
	 * Defines the ASN.1 type for this object.
	 *
	 */
	public static final byte ASNTYPE = SnmpSMI.SMI_COUNTER32;
	
	/**
	 * Constructs the default counter object.
	 * The initial value is defined
	 * by the super class default constructor
	 */
	public SnmpCounter32( )
	{
		super();
	}
	/**
	 * Constructs the object with the specified
	 * value.
	 *
	 * @param value	The default value for the object.
	 */
	public SnmpCounter32(long value)
	{
		super(value);
	}

	/**
	 * Constructs the object with the specified value.
	 *
	 * @param value	The default value for the object.
	 *
	 */
	public SnmpCounter32(Long value)
	{
		super(value);
	}

	/**
	 * Constructs a new object with the same value
	 * as the passed object.
	 *
	 * @param second	The object to recover values from.
	 *
	 */
	public SnmpCounter32(SnmpCounter32 second)
	{
		super(second);
	}

	/**
	 * Constructs a new object with the value
	 * constained in the SnmpUInt32 object.
	 *
	 * @param uint32 The SnmpUInt32 object to copy.
	 *
	 */
	public SnmpCounter32(SnmpUInt32 uint32)
	{
		super(uint32);
	}

	/**
	 * <p>Simple class constructor that is used to create an initialize
	 * the new instance with the unsigned value decoded from the 
	 * passed String argument. If the decoded argument is malformed,
	 * null, or evaluates to a negative value then an exception is 
	 * generated.</p>
	 *
	 * @param value	The string encoded value.
	 *
	 * @throws java.lang.NumberFormatException Thrown if the passed value is
	 *	malformed and cannot be parsed.
	 * @throws java.lang.IllegalArgumentException Throws if the passed value 
	 *	evaluates to a negative value.
	 * @throws java.lang.NullPointerException Throws if the passed value is
	 *	a null reference.
	 */
	public SnmpCounter32(String value)
	{
		super(value);
	}

	/**
	 * Returns the ASN.1 type specific to this object.
	 *
	 * @return The ASN.1 value for this object.
	 */
	public byte typeId()
	{
		return ASNTYPE;
	}

	/**
	 * Creates a new object that is a duplicate of the
	 * current object.
	 *
	 * @return The newly created duplicate object.
	 *
	 */
	public SnmpSyntax duplicate() 
	{
		return new SnmpCounter32(this);
	}

	/**
	 * Creates a new object that is a duplicate of the
	 * current object.
	 *
	 * @return The newly created duplicate object.
	 *
	 */
	public Object clone()
	{
		return new SnmpCounter32(this);
	}
}
