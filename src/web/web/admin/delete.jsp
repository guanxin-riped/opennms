<!--

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
// Modifications:
// 
// 2003 Oct 23: Show an alert message if no nodes or data items are selected.
// 2003 Feb 07: Fixed URLEncoder issues.
// 2002 Nov 26: Fixed breadcrumbs issue.
// 2002 Nov 12: Added the ability to delete data directories when deleting nodes.
// 2002 Sep 19: Added a "delete nodes" page.
// 
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
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

-->

<%@page language="java" contentType="text/html" session="true" import="java.io.File,java.util.*,org.opennms.web.element.NetworkElementFactory,org.opennms.web.admin.nodeManagement.*" %>

<%!
    int interfaceIndex;
    int serviceIndex;
%>

<%
    HttpSession userSession = request.getSession(false);
    List nodes = null;
    Integer lineItems= new Integer(0);
    
    interfaceIndex = 0;
    serviceIndex = 0;
    
    if (userSession != null)
    {
  	nodes = (List)userSession.getAttribute("listAll.delete.jsp");
        lineItems = (Integer)userSession.getAttribute("lineItems.delete.jsp");
    }
%>
<html>
<head>
  <title>Admin | OpenNMS Web Console</title>
  <base HREF="<%=org.opennms.web.Util.calculateUrlBase( request )%>" />
  <link rel="stylesheet" type="text/css" href="includes/styles.css" />
</head>

<script language="Javascript" type="text/javascript" >

  function applyChanges()
  {
        var hasCheckedItems = false;
        for (var i = 0; i < document.deleteAll.elements.length; i++)
        {
                if (document.deleteAll.elements[i].type == "checkbox")
                {
                        if (document.deleteAll.elements[i].checked)
                        {
                                hasCheckedItems = true;
                                break;
                        }
                }
        }
                
        if (hasCheckedItems)
        {
                if (confirm("Are you sure you want to proceed? This action will permanently delete the checked nodes and cannot be undone."))
                {
                        document.deleteAll.submit();
                }
        }
        else
        {
                alert("No nodes and data items are selected!");
        }
  }
  
  function cancel()
  {
      document.deleteAll.action="admin/index.jsp";
      document.deleteAll.submit();
  }
  
  function checkAll()
  {
      for (var c = 0; c < document.deleteAll.elements.length; c++)
      {  
          if (document.deleteAll.elements[c].type == "checkbox")
          {
              document.deleteAll.elements[c].checked = true;
          }
      }
  }
  
  function uncheckAll()
  {
      for (var c = 0; c < document.deleteAll.elements.length; c++)
      {  
          if (document.deleteAll.elements[c].type == "checkbox")
          {
              
              document.deleteAll.elements[c].checked = false;
          }
      }
  }
  
</script>

<body marginwidth="0" marginheight="0" LEFTMARGIN="0" RIGHTMARGIN="0" TOPMARGIN="0">

<% String breadcrumb1 = "<a href='admin/index.jsp'>Admin</a>"; %>
<% String breadcrumb2 = "Delete Nodes"; %>
<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Delete Nodes" />
  <jsp:param name="location" value="admin" />
  <jsp:param name="breadcrumb" value="<%=breadcrumb1%>" />
  <jsp:param name="breadcrumb" value="<%=breadcrumb2%>" />
</jsp:include>

<!-- Body -->
<br>

<FORM METHOD="POST" name="deleteAll" action="admin/deleteSelNodes">

<%
  int midNodeIndex = 1;
  
  if (nodes.size() > 1)
  {
    midNodeIndex = nodes.size()/2;
  }
%>

<table width="100%" cellspacing="0" cellpadding="0" border="0">
  
  <tr>
    <td> &nbsp; </td>  
    
    <td>
    <h3>Delete Nodes</h3>

    <table width="100%" cellspacing="0" cellpadding="0" border="0">
      <tr>
        <td colspan="3"> 
	<P>The nodes present in the system are listed below. To permanently delete a node (and all associated
	   interfaces, services, outages, events and notifications), check the "Delete?" box beside the node's ID and
           select "Delete Nodes". You may check more than one.
        </P>
	<P>Checking the "Data?" box will delete the SNMP performance and response time directories from the system as well.
	   Note that it is possible for the directory to be deleted <i>before</i> the fact that the node has been removed has
           fully propagated through the system. Thus the system may recreate the directory for a single update after
           this action. In that case, the directory will need to be removed manually.
	</P>
        <P><b>Note:</b> If the IP address of any of the node's interfaces is still configured for discovery,
	   the node will be discovered again. To prevent this, either remove the IP address from the
	   discovery range or unmanage the device instead of deleting it.
        </P>
        </td>
      </tr>
	
      <TR>
      <td>&nbsp;</td>
      </tr>

   </tr> 
      
   <tr>
        <td align="left" valign="center">
          <input type="button" value="Delete Nodes" onClick="applyChanges()">
          <input type="button" value="Cancel" onClick="cancel()">
          <input type="button" value="Select All" onClick="checkAll()">
          <input type="button" value="Unselect All" onClick="uncheckAll()">
          <input type="reset"><br>&nbsp;
        </td>
	<td>&nbsp;</td>
	<td>&nbsp;</td>
   </tr>
      
   <% if (nodes.size() > 0) { %>
   <tr>
        <td width="49%" align="left" valign="top">
          <table border="1" cellspacing="0" cellpadding="2" bordercolor="black">
            <tr bgcolor="#999999">
              <td width="5%" align="center"><b>Delete?</b></td>
              <td width="5%" align="center"><b>Data?</b></td>
              <td width="5%" align="center"><b>Node ID</b></td>
              <td width="10%" align="center"><b>Node Label</b></td>
            </tr>
            
            <%=buildTableRows(nodes, 0, midNodeIndex)%>
            
          </table>
          <% } /*end if*/ %>
        </td>
        
        <td>
          &nbsp;&nbsp;
        </td>
        
      <!--see if there is a second column to draw-->
      <% if (midNodeIndex < nodes.size()) { %>
        <td width="49%" align="left" valign="top">
          <table border="1" cellspacing="0" cellpadding="2" bordercolor="black">
            <tr bgcolor="#999999">
              <td width="5%" align="center"><b>Delete?</b></td>
              <td width="5%" align="center"><b>Data?</b></td>
              <td width="5%" align="center"><b>Node ID</b></td>
              <td width="10%" align="center"><b>Node Label</b></td>
            </tr>
            
            <%=buildTableRows(nodes, midNodeIndex, nodes.size())%>
               
          </table>
        </td>
        <% } /*end if */ %>
   </tr>
      
   <tr>
        <td align="left" valign="center" colspan="5">
          &nbsp;<br>
          <input type="button" value="Delete Nodes" onClick="applyChanges()">
          <input type="button" value="Cancel" onClick="cancel()"> 
          <input type="button" value="Select All" onClick="checkAll()">
          <input type="button" value="Unselect All" onClick="uncheckAll()">
          <input type="reset">
        </td>
	<td>&nbsp;</td>
	<td>&nbsp;</td>
    </tr>
  
</table>
</FORM>

<br>

<jsp:include page="/includes/footer.jsp" flush="true" >
  <jsp:param name="location" value="admin" />
</jsp:include>
</body>
</html>

<%!
      public String buildTableRows(List nodes, int start, int stop)
      	throws java.sql.SQLException
      {
          StringBuffer row = new StringBuffer();
          
          for (int i = start; i < stop; i++)
          {
                
                ManagedNode curNode = (ManagedNode)nodes.get(i);
                String nodelabel = NetworkElementFactory.getNodeLabel(curNode.getNodeID());
		int nodeid = curNode.getNodeID();
                 
          row.append("<tr>\n");
          row.append("<td width=\"5%\" align=\"center\">");
          row.append("<input type=\"checkbox\" name=\"nodeCheck\" value=\""+ nodeid +"\" >");
          row.append("</td>\n");
          row.append("<td width=\"5%\" align=\"center\">");
          row.append("<input type=\"checkbox\" name=\"nodeData\" value=\""+ nodeid +"\" >");
          row.append("</td>\n");
          row.append("<td width=\"5%\" align=\"center\">");
	  row.append(nodeid);
          row.append("</td>\n");
          row.append("<td width=\"10%\" align=\"left\">");
	  row.append(nodelabel);
          row.append("</td>\n");
          row.append("</tr>\n");
          } /* end i for */
          
          return row.toString();
      }
      
%>
