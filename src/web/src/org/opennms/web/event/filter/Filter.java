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

package org.opennms.web.event.filter;


/** Convenience class to determine what sort of events to include in a query. */
public interface Filter 
{
    
    /** 
     * Returns an expresions for a SQL where clause.
     * Remember to include a trailing space, but no leading AND or OR. 
     */
    public String getSql();
    
    
    /**
     * Returns a terse string (including a "=") that describes this filter
     * in such a way to easily be included in an HTTP GET parameter.
     *
     * <p>Some examples: 
     *   <ul>
     *     <li>"node=1"</li>
     *     <li>"interface=192.168.0.1"</li>
     *     <li>"severity=3"</li>     
     *     <li>"nodenamelike=opennms"</li>     
     *   </ul>
     * </p>
     */
    public String getDescription();
    

    /** 
     * Returns a terse but human-readable string describing this filter in
     * such a way to easily be included in a search results list.
     *
     * <p>Some examples (corresponding to the examples in 
     * {@link #getDescription getDescription}): 
     *   <ul>
     *     <li>"node=nodelabel_of_node_1"</li>
     *     <li>"interface=192.168.0.1"</li>
     *     <li>"severity=Normal"</li>     
     *     <li>"node name containing \"opennms\""</li>     
     *   </ul>
     * </p>     
     */     
    public String getTextDescription();
}




