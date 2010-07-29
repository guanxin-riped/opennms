//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006-2009 The OpenNMS Group, Inc. All rights
// reserved.
// OpenNMS(R) is a derivative work, containing both original code, included
// code and modified
// code that was published under the GNU General Public License. Copyrights
// for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2009 Oct 01: Add ability to update database when an interface is deleted. - ayres@opennms.org
//
// Original code base Copyright (C) 1999-2001 Oculan Corp. All rights
// reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
// OpenNMS Licensing <license@opennms.org>
// http://www.opennms.org/
// http://www.opennms.com/
//
/*
 * Created on 8-lug-2005 To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package org.opennms.netmgt.linkd;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.opennms.core.utils.DBUtils;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.capsd.snmp.SnmpTableEntry;
import org.opennms.netmgt.linkd.snmp.CdpCacheTableEntry;
import org.opennms.netmgt.linkd.snmp.Dot1dBaseGroup;
import org.opennms.netmgt.linkd.snmp.Dot1dBasePortTableEntry;
import org.opennms.netmgt.linkd.snmp.Dot1dStpGroup;
import org.opennms.netmgt.linkd.snmp.Dot1dStpPortTableEntry;
import org.opennms.netmgt.linkd.snmp.Dot1dTpFdbTableEntry;
import org.opennms.netmgt.linkd.snmp.IpNetToMediaTableEntry;
import org.opennms.netmgt.linkd.snmp.IpRouteTableEntry;
import org.opennms.netmgt.linkd.snmp.QBridgeDot1dTpFdbTableEntry;
import org.opennms.netmgt.linkd.snmp.VlanCollectorEntry;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <P>
 * This class is used to store informations owned by SnmpCollection and
 * DiscoveryLink Classes in DB. When saving Snmp Collection it populate Bean
 * LinkableNode with information for DiscoveryLink. It performes data test for
 * DiscoveryLink. Also take correct action on DB tables in case node is deleted
 * service SNMP is discovered, service SNMP is Lost and Regained Also this class
 * holds
 * </P>
 *
 * @author antonio
 * @version $Id: $
 */
public class DbEventWriter implements QueryManager {

    JdbcTemplate jdbcTemplate;

    /**
     * Query to select info for specific node
     */
    private static final String SQL_SELECT_SNMP_NODE = "SELECT nodesysoid, ipaddr FROM node " + "LEFT JOIN ipinterface ON node.nodeid = ipinterface.nodeid "
            + "WHERE node.nodeid = ? AND nodetype = 'A' AND issnmpprimary = 'P'";

    private static final String SQL_SELECT_SNMP_IP_ADDR = "SELECT ipaddr FROM ipinterface WHERE nodeid = ? AND issnmpprimary = 'P'";

    private static final String SQL_GET_NODEID = "SELECT node.nodeid FROM node LEFT JOIN ipinterface ON node.nodeid = ipinterface.nodeid WHERE nodetype = 'A' AND ipaddr = ?";

    private static final String SQL_GET_NODEID__IFINDEX_MASK = "SELECT node.nodeid,snmpinterface.snmpifindex,snmpinterface.snmpipadentnetmask FROM node LEFT JOIN snmpinterface ON node.nodeid = snmpinterface.nodeid WHERE nodetype = 'A' AND ipaddr = ?";

    private static final String SQL_GET_NODEID_IFINDEX_IPINT = "SELECT node.nodeid,ipinterface.ifindex FROM node LEFT JOIN ipinterface ON node.nodeid = ipinterface.nodeid WHERE nodetype = 'A' AND ipaddr = ?";

    private static final String SQL_UPDATE_DATALINKINTERFACE = "UPDATE datalinkinterface set status = 'N'  WHERE lastpolltime < ? AND status = 'A'";

    private static final String SQL_UPDATE_ATINTERFACE = "UPDATE atinterface set status = 'N'  WHERE sourcenodeid = ? AND lastpolltime < ? AND status = 'A'";

    private static final String SQL_UPDATE_IPROUTEINTERFACE = "UPDATE iprouteinterface set status = 'N'  WHERE nodeid = ? AND lastpolltime < ? AND status = 'A'";

    private static final String SQL_UPDATE_STPNODE = "UPDATE stpnode set status = 'N'  WHERE nodeid = ? AND lastpolltime < ? AND status = 'A'";

    private static final String SQL_UPDATE_STPINTERFACE = "UPDATE stpinterface set status = 'N'  WHERE nodeid = ? AND lastpolltime < ? AND status = 'A'";

    private static final String SQL_UPDATE_VLAN = "UPDATE vlan set status = 'N'  WHERE nodeid =? AND lastpolltime < ? AND status = 'A'";

    private static final String SQL_UPDATE_ATINTERFACE_STATUS = "UPDATE atinterface set status = ?  WHERE sourcenodeid = ? OR nodeid = ?";

    private static final String SQL_UPDATE_IPROUTEINTERFACE_STATUS = "UPDATE iprouteinterface set status = ? WHERE nodeid = ? ";

    private static final String SQL_UPDATE_STPNODE_STATUS = "UPDATE stpnode set status = ?  WHERE nodeid = ? ";

    private static final String SQL_UPDATE_STPINTERFACE_STATUS = "UPDATE stpinterface set status = ? WHERE nodeid = ? ";

    private static final String SQL_UPDATE_VLAN_STATUS = "UPDATE vlan set status = ?  WHERE nodeid = ? ";

    private static final String SQL_UPDATE_DATALINKINTERFACE_STATUS = "UPDATE datalinkinterface set status = ? WHERE nodeid = ? OR nodeparentid = ? ";

    // private static final String SQL_GET_NODEID_IFINDEX =
    // "SELECT atinterface.nodeid, atinterface.ipaddr, snmpinterface.snmpifindex from atinterface left JOIN snmpinterface ON atinterface.nodeid = snmpinterface.nodeid AND atinterface.ipaddr = snmpinterface.ipaddr WHERE atphysaddr = ? AND status = 'A'";
    private static final String SQL_GET_NODEID_IFINDEX = "SELECT atinterface.nodeid, atinterface.ipaddr, ipinterface.ifindex from atinterface left JOIN ipinterface ON atinterface.nodeid = ipinterface.nodeid AND atinterface.ipaddr = ipinterface.ipaddr WHERE atphysaddr = ? AND atinterface.status <> 'D'";

    private static final String SQL_GET_SNMPIFTYPE = "SELECT snmpiftype FROM snmpinterface WHERE nodeid = ? AND snmpifindex = ?";

    private static final String SQL_GET_IFINDEX_SNMPINTERFACE_NAME = "SELECT snmpifindex FROM snmpinterface WHERE nodeid = ? AND (snmpifname = ? OR snmpifdescr = ?) ";

    private static final String SQL_GET_SNMPPHYSADDR_SNMPINTERFACE = "SELECT snmpphysaddr FROM snmpinterface WHERE nodeid = ? AND  snmpphysaddr <> ''";

    /**
     * query to select SNMP nodes
     */
    private static final String SQL_SELECT_SNMP_NODES = "SELECT node.nodeid, nodesysoid, ipaddr FROM node LEFT JOIN ipinterface ON node.nodeid = ipinterface.nodeid WHERE nodetype = 'A' AND issnmpprimary = 'P'";

    /**
     * update status to D on node marked as Deleted on table Nodes
     */
    private static final String SQL_UPDATE_VLAN_D = "UPDATE vlan set status = 'D' WHERE nodeid IN (SELECT nodeid from node WHERE nodetype = 'D' ) AND status <> 'D' ";

    private static final String SQL_UPDATE_ATINTERFACE_D = "UPDATE atinterface set status = 'D' WHERE nodeid IN (SELECT nodeid from node WHERE nodetype = 'D' ) AND status <> 'D' ";

    private static final String SQL_UPDATE_STPNODE_D = "UPDATE stpnode set status = 'D' WHERE nodeid IN (SELECT nodeid from node WHERE nodetype = 'D' ) AND status <> 'D'";

    private static final String SQL_UPDATE_STPINTERFACE_D = "UPDATE stpinterface set status = 'D' WHERE nodeid IN (SELECT nodeid from node WHERE nodetype = 'D' ) AND status <> 'D'";

    private static final String SQL_UPDATE_IPROUTEINTERFACE_D = "UPDATE iprouteinterface set status = 'D' WHERE nodeid IN (SELECT nodeid from node WHERE nodetype = 'D' ) AND status <> 'D'";

    private static final String SQL_UPDATE_DATALINKINTERFACE_D = "UPDATE datalinkinterface set status = 'D' WHERE (nodeid IN (SELECT nodeid from node WHERE nodetype = 'D' ) OR nodeparentid IN (SELECT nodeid from node WHERE nodetype = 'D' )) AND status <> 'D'";

    /**
     * update table status for interfaces
     */
    private static final String SQL_UPDATE_ATINTERFACE_STATUS_INTFC = "UPDATE atinterface set status = ?  WHERE nodeid = ? AND ipaddr = ?";
    
    private static final String SQL_UPDATE_ATINTERFACE_STATUS_SRC_INTFC = "UPDATE atinterface set status = ?  WHERE sourcenodeid = ? AND ifindex = ?";

    private static final String SQL_UPDATE_STPINTERFACE_STATUS_INTFC = "UPDATE stpinterface set status = ? WHERE nodeid = ? AND ifindex = ?";

    private static final String SQL_UPDATE_IPROUTEINTERFACE_STATUS_INTFC = "UPDATE iprouteinterface set status = ? WHERE nodeid = ? AND routeifindex = ?";

    private static final String SQL_UPDATE_DATALINKINTERFACE_STATUS_INTFC = "UPDATE datalinkinterface set status = ? WHERE (nodeid = ? and ifindex = ?) OR (nodeparentid = ? AND parentifindex = ?)";

    /**
     * <p>Constructor for DbEventWriter.</p>
     */
    public DbEventWriter() {

    }

    private Connection getConnection() throws SQLException {
        return jdbcTemplate.getDataSource().getConnection();
    }

    /** {@inheritDoc} */
    public void storeDiscoveryLink(DiscoveryLink discovery) throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {
            Connection dbConn = getConnection();
            d.watch(dbConn);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            PreparedStatement stmt = null;
            ResultSet rs = null;
    
            NodeToNodeLink[] links = discovery.getLinks();
    
            LogUtils.debugf(this, "storelink: Storing %d NodeToNodeLink information into database", links.length);
            for (int i = 0; i < links.length; i++) {
                NodeToNodeLink lk = links[i];
                int nodeid = lk.getNodeId();
                int ifindex = lk.getIfindex();
                int nodeparentid = lk.getNodeparentid();
                int parentifindex = lk.getParentifindex();
    
                DbDataLinkInterfaceEntry dbentry = DbDataLinkInterfaceEntry.get(dbConn, nodeid, ifindex);
                if (dbentry == null) {
                    // Create a new entry
                    dbentry = DbDataLinkInterfaceEntry.create(nodeid, ifindex);
                }
                dbentry.updateNodeParentId(nodeparentid);
                dbentry.updateParentIfIndex(parentifindex);
                dbentry.updateStatus(DbDataLinkInterfaceEntry.STATUS_ACTIVE);
                dbentry.set_lastpolltime(now);
    
                dbentry.store(dbConn);
    
                // now parsing simmetrical and setting to D if necessary
    
                dbentry = DbDataLinkInterfaceEntry.get(dbConn, nodeparentid, parentifindex);
    
                if (dbentry != null) {
                    if (dbentry.get_nodeparentid() == nodeid && dbentry.get_parentifindex() == ifindex
                            && dbentry.get_status() != DbDataLinkInterfaceEntry.STATUS_DELETE) {
                        dbentry.updateStatus(DbDataLinkInterfaceEntry.STATUS_DELETE);
                        dbentry.store(dbConn);
                    }
                }
            }
    
            MacToNodeLink[] linkmacs = discovery.getMacLinks();
    
            LogUtils.debugf(this, "storelink: Storing " + linkmacs.length + " MacToNodeLink information into database");
            for (int i = 0; i < linkmacs.length; i++) {
    
                MacToNodeLink lkm = linkmacs[i];
                String macaddr = lkm.getMacAddress();
    
                LogUtils.debugf(this, "storelink: finding nodeid,ifindex on DB using mac address: " + macaddr);
    
                stmt = dbConn.prepareStatement(SQL_GET_NODEID_IFINDEX);
                d.watch(stmt);
    
                stmt.setString(1, macaddr);
    
                rs = stmt.executeQuery();
                d.watch(rs);
    
                LogUtils.debugf(this, "storelink: finding nodeid,ifindex on DB. Sql Statement " + SQL_GET_NODEID_IFINDEX + " with mac address " + macaddr);
    
                if (!rs.next()) {
                    LogUtils.debugf(this, "storelink: no nodeid found on DB for mac address " + macaddr + " on link. .... Skipping");
                    continue;
                }
    
                // extract the values.
                //
                int ndx = 1;
    
                int nodeid = rs.getInt(ndx++);
                if (rs.wasNull()) {
                    LogUtils.debugf(this, "storelink: no nodeid found on DB for mac address " + macaddr + " on link. .... Skipping");
                    continue;
                }
    
                String ipaddr = rs.getString(ndx++);
                if (rs.wasNull()) {
                    LogUtils.debugf(this, "storelink: no ipaddr found on DB for mac address " + macaddr + " on link. .... Skipping");
                    continue;
                }
    
                if (!Linkd.getInstance().isInterfaceInPackage(ipaddr, discovery.getPackageName())) {
                    LogUtils.debugf(this, "storelink: not in package ipaddr found: " + ipaddr + " on link. .... Skipping");
                    continue;
    
                }
                int ifindex = rs.getInt(ndx++);
                if (rs.wasNull()) {
                    LogUtils.debugf(this, "storelink: no ifindex found on DB for mac address " + macaddr + " on link.");
                    ifindex = -1;
                }
    
                int nodeparentid = lkm.getNodeparentid();
                int parentifindex = lkm.getParentifindex();
                DbDataLinkInterfaceEntry dbentry = DbDataLinkInterfaceEntry.get(dbConn, nodeid, ifindex);
                if (dbentry == null) {
                    // Create a new entry
                    dbentry = DbDataLinkInterfaceEntry.create(nodeid, ifindex);
                }
                dbentry.updateNodeParentId(nodeparentid);
                dbentry.updateParentIfIndex(parentifindex);
                dbentry.updateStatus(DbDataLinkInterfaceEntry.STATUS_ACTIVE);
                dbentry.set_lastpolltime(now);
    
                dbentry.store(dbConn);
    
            }
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_DATALINKINTERFACE);
            d.watch(stmt);
            stmt.setTimestamp(1, now);
    
            int i = stmt.executeUpdate();
            LogUtils.debugf(this, "storelink: datalinkinterface - updated to NOT ACTIVE status " + i + " rows ");
        } finally {
            d.cleanUp();
        }
    }

    /** {@inheritDoc} */
    public LinkableNode storeSnmpCollection(LinkableNode node, SnmpCollection snmpcoll) throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {
            Connection dbConn = getConnection();
            d.watch(dbConn);
            Timestamp now = new Timestamp(System.currentTimeMillis());
    
            if (snmpcoll.hasIpNetToMediaTable()) {
                Iterator<IpNetToMediaTableEntry> ite1 = snmpcoll.getIpNetToMediaTable().getEntries().iterator();
                LogUtils.debugf(this, "store: saving IpNetToMediaTable to atinterface table in DB");
                // the AtInterfaces used by LinkableNode where to save info
                java.util.List<AtInterface> atInterfaces = new java.util.ArrayList<AtInterface>();
                while (ite1.hasNext()) {
    
                    IpNetToMediaTableEntry ent = ite1.next();
    
                    int ifindex = ent.getIpNetToMediaIfIndex();
    
                    if (ifindex < 0) {
                        LogUtils.warnf(this, "store: invalid ifindex " + ifindex);
                        continue;
                    }
    
                    InetAddress ipaddress = ent.getIpNetToMediaNetAddress();
    
                    if (ipaddress == null || ipaddress.isLoopbackAddress() || ipaddress.getHostAddress().equals("0.0.0.0")) {
                        LogUtils.warnf(this, "store: ipNetToMedia invalid ip " + ipaddress.getHostAddress());
                        continue;
                    }
    
                    String physAddr = ent.getIpNetToMediaPhysAddress();
    
                    if (physAddr == null || physAddr.equals("000000000000") || physAddr.equalsIgnoreCase("ffffffffffff")) {
                        LogUtils.warnf(this, "store: ipNetToMedia invalid mac address " + physAddr + " for ip " + ipaddress.getHostAddress());
                        continue;
                    }
                    
                    LogUtils.debugf(this, "store: trying save ipNetToMedia info: ipaddr " + ipaddress.getHostName() + " mac address " + physAddr + " ifindex " + ifindex);
    
                    // get an At interface but without setting mac address
                    AtInterface at = getNodeidIfindexFromIp(dbConn, ipaddress);
                    if (at == null) {
                        LogUtils.warnf(this, "getNodeidIfindexFromIp: no nodeid found for ipaddress " + ipaddress + ".");
                        sendNewSuspectEvent(ipaddress, snmpcoll.getTarget(), snmpcoll.getPackageName());
                        continue;
                    }
                    // set the mac address
                    at.setMacAddress(physAddr);
                    // add At Inteface to list of valid interfaces
                    atInterfaces.add(at);
    
                    // Save in DB
                    DbAtInterfaceEntry atInterfaceEntry = DbAtInterfaceEntry.get(dbConn, at.getNodeId(), ipaddress.getHostAddress());
    
                    if (atInterfaceEntry == null) {
                        atInterfaceEntry = DbAtInterfaceEntry.create(at.getNodeId(), ipaddress.getHostAddress());
                    }
    
                    // update object
                    atInterfaceEntry.updateAtPhysAddr(physAddr);
                    atInterfaceEntry.updateSourceNodeId(node.getNodeId());
                    atInterfaceEntry.updateIfIndex(ifindex);
                    atInterfaceEntry.updateStatus(DbAtInterfaceEntry.STATUS_ACTIVE);
                    atInterfaceEntry.set_lastpolltime(now);
    
                    // store object in database
                    atInterfaceEntry.store(dbConn);
                }
                // set AtInterfaces in LinkableNode
                node.setAtInterfaces(atInterfaces);
            }
    
            if (snmpcoll.hasCdpCacheTable()) {
                LogUtils.debugf(this, "store: saving CdpCacheTable into SnmpLinkableNode");
                java.util.List<CdpInterface> cdpInterfaces = new java.util.ArrayList<CdpInterface>();
                Iterator<CdpCacheTableEntry> ite2 = snmpcoll.getCdpCacheTable().getEntries().iterator();
                while (ite2.hasNext()) {
                    CdpCacheTableEntry cdpEntry = ite2.next();
                    int cdpAddrType = cdpEntry.getCdpCacheAddressType();
    
                    if (cdpAddrType != CDP_ADDRESS_TYPE_IP_ADDRESS) {
                        LogUtils.warnf(this, "cdp Address Type not valid " + cdpAddrType);
                        continue;
                    }
    
                    InetAddress cdpTargetIpAddr = cdpEntry.getCdpCacheAddress();
    
                    if (cdpTargetIpAddr == null || cdpTargetIpAddr.isLoopbackAddress() || cdpTargetIpAddr.getHostAddress().equals("0.0.0.0")) {
                        LogUtils.warnf(this, "cdp Ip Address is not valid " + cdpTargetIpAddr);
                        continue;
                    }
    
                    LogUtils.debugf(this, "cdp ip address found " + cdpTargetIpAddr.getHostAddress());
    
                    int cdpIfIndex = cdpEntry.getCdpCacheIfIndex();
    
                    if (cdpIfIndex < 0) {
                        LogUtils.warnf(this, "cdpIfIndex not valid " + cdpIfIndex);
                        continue;
                    }
    
                    LogUtils.debugf(this, "cdp ifindex found " + cdpIfIndex);
    
                    String cdpTargetDevicePort = cdpEntry.getCdpCacheDevicePort();
    
                    if (cdpTargetDevicePort == null) {
                        LogUtils.warnf(this, "cdpTargetDevicePort null. Skipping. ");
                        continue;
                    }
    
                    LogUtils.debugf(this, "cdp Target device port name found " + cdpTargetDevicePort);
    
                    int targetCdpNodeId = getNodeidFromIp(dbConn, cdpTargetIpAddr);
    
                    if (targetCdpNodeId == -1) {
                        LogUtils.warnf(this, "No nodeid found: cdp interface not added to Linkable Snmp Node. Skipping");
                        sendNewSuspectEvent(cdpTargetIpAddr, snmpcoll.getTarget(), snmpcoll.getPackageName());
                        continue;
                    }
    
                    int cdpTargetIfindex = getIfIndexByName(dbConn, targetCdpNodeId, cdpTargetDevicePort);
    
                    if (cdpTargetIfindex == -1) {
                        LogUtils.warnf(this, "No valid if target index found: cdp interface not added to Linkable Snmp Node. Skipping");
                        continue;
                    }
    
                    CdpInterface cdpIface = new CdpInterface(cdpIfIndex);
                    cdpIface.setCdpTargetNodeId(targetCdpNodeId);
                    cdpIface.setCdpTargetIpAddr(cdpTargetIpAddr);
                    cdpIface.setCdpTargetIfIndex(cdpTargetIfindex);
    
                    LogUtils.debugf(this, "Adding cdp interface to Linkable Snmp Node." + cdpIface.toString());
    
                    cdpInterfaces.add(cdpIface);
                }
                node.setCdpInterfaces(cdpInterfaces);
            }
    
            if (snmpcoll.hasRouteTable()) {
                java.util.List<RouterInterface> routeInterfaces = new java.util.ArrayList<RouterInterface>();
    
                Iterator<IpRouteTableEntry> ite3 = snmpcoll.getIpRouteTable().getEntries().iterator();
                LogUtils.debugf(this, "store: saving ipRouteTable to iprouteinterface table in DB");
                while (ite3.hasNext()) {
                    IpRouteTableEntry ent = ite3.next();
    
                    int ifindex = ent.getIpRouteIfIndex();
    
                    if (ifindex < 0) {
                        LogUtils.warnf(this, "store: Not valid ifindex" + ifindex + " Skipping...");
                        continue;
                    }
    
                    InetAddress nexthop = ent.getIpRouteNextHop();
    
                    if (nexthop == null) {
                        LogUtils.warnf(this, "storeSnmpCollection: next hop null found skipping.");
                        continue;
                    }
    
                    InetAddress routedest = ent.getIpRouteDest();
                    if (routedest == null) {
                        LogUtils.warnf(this, "storeSnmpCollection: route dest null found skipping.");
                        continue;
                    }
                    InetAddress routemask = ent.getIpRouteMask();
    
                    if (routemask == null) {
                        LogUtils.warnf(this, "storeSnmpCollection: route dest null found skipping.");
                        continue;
                    }
    
                    LogUtils.debugf(this, "storeSnmpCollection: parsing routedest/routemask/nexthop: " + routedest + "/" + routemask + "/" + nexthop + " ifindex "
                                    + (ifindex < 1 ? "less than 1" : ifindex));
    
                    int routemetric1 = ent.getIpRouteMetric1();
    
                    /**
                     * FIXME: send routedest 0.0.0.0 to discoverylink remeber that
                     * now nexthop 0.0.0.0 is not parsed, anyway we should analize
                     * this case in link discovery so here is the place where you
                     * can have this info saved for now is discarded. See
                     * DiscoveryLink for more details......
                     */
    
                    // the routerinterface constructor set nodeid, ifindex,
                    // netmask
                    // for nexthop address
                    // try to find on snmpinterface table
                    RouterInterface routeIface = getNodeidMaskFromIp(dbConn, nexthop);
    
                    // if target node is not snmp node always try to find info
                    // on ipinterface table
                    if (routeIface == null) {
                        routeIface = getNodeFromIp(dbConn, nexthop);
                    }
    
                    if (routeIface == null) {
                        LogUtils.warnf(this, "store: No nodeid found for next hop ip" + nexthop + " Skipping ip route interface add to Linkable Snmp Node");
                        // try to find it in ipinterface
                        sendNewSuspectEvent(nexthop, snmpcoll.getTarget(), snmpcoll.getPackageName());
                    } else {
                        int snmpiftype = -2;
    
                        if (ifindex > 0) snmpiftype = getSnmpIfType(dbConn, node.getNodeId(), ifindex);
    
                        if (snmpiftype == -1) {
                            LogUtils.warnf(this, "store: interface has wrong or null snmpiftype " + snmpiftype + " . Skipping saving to discoverylink. ");
                        } else if (nexthop.isLoopbackAddress()) {
                            LogUtils.infof(this, "storeSnmpCollection: next hop loopbackaddress found. Skipping saving 	to discoverylink.");
                        } else if (nexthop.getHostAddress().equals("0.0.0.0")) {
                            LogUtils.infof(this, "storeSnmpCollection: next hop broadcast address found. Skipping saving to discoverylink.");
                        } else if (nexthop.isMulticastAddress()) {
                            LogUtils.infof(this, "storeSnmpCollection: next hop multicast address found. Skipping saving to discoverylink.");
                        } else if (routemetric1 < 0) {
                            LogUtils.infof(this, "storeSnmpCollection: route metric is invalid. Skipping saving to discoverylink.");
                        } else {
                            LogUtils.debugf(this, "store: interface has snmpiftype " + snmpiftype + " . Adding to DiscoverLink ");
    
                            routeIface.setRouteDest(routedest);
                            routeIface.setRoutemask(routemask);
                            routeIface.setSnmpiftype(snmpiftype);
                            routeIface.setIfindex(ifindex);
                            routeIface.setMetric(routemetric1);
                            routeIface.setNextHop(nexthop);
                            routeInterfaces.add(routeIface);
    
                        }
                    }
    
                    int routemetric2 = ent.getIpRouteMetric2();
                    int routemetric3 = ent.getIpRouteMetric3();
                    int routemetric4 = ent.getIpRouteMetric4();
                    int routemetric5 = ent.getIpRouteMetric5();
                    int routetype = ent.getIpRouteType();
                    int routeproto = ent.getIpRouteProto();
    
                    // always save info to DB
                    if (snmpcoll.getSaveIpRouteTable()) {
                        DbIpRouteInterfaceEntry iprouteInterfaceEntry = DbIpRouteInterfaceEntry.get(dbConn, node.getNodeId(), routedest.getHostAddress());
                        if (iprouteInterfaceEntry == null) {
                            // Create a new entry
                            iprouteInterfaceEntry = DbIpRouteInterfaceEntry.create(node.getNodeId(), routedest.getHostAddress());
                        }
                        // update object
                        iprouteInterfaceEntry.updateRouteMask(routemask.getHostAddress());
                        iprouteInterfaceEntry.updateRouteNextHop(nexthop.getHostAddress());
                        iprouteInterfaceEntry.updateIfIndex(ifindex);
    
                        // okay to autobox these since were checking for null
                        iprouteInterfaceEntry.updateRouteMetric1(routemetric1);
                        iprouteInterfaceEntry.updateRouteMetric2(routemetric2);
                        iprouteInterfaceEntry.updateRouteMetric3(routemetric3);
                        iprouteInterfaceEntry.updateRouteMetric4(routemetric4);
                        iprouteInterfaceEntry.updateRouteMetric5(routemetric5);
                        iprouteInterfaceEntry.updateRouteType(routetype);
                        iprouteInterfaceEntry.updateRouteProto(routeproto);
                        iprouteInterfaceEntry.updateStatus(DbAtInterfaceEntry.STATUS_ACTIVE);
                        iprouteInterfaceEntry.set_lastpolltime(now);
    
                        // store object in database
                        iprouteInterfaceEntry.store(dbConn);
                    }
                }
                node.setRouteInterfaces(routeInterfaces);
            }
    
            LogUtils.debugf(this, "store: saving VlanTable in DB");
    
            if (snmpcoll.hasVlanTable()) {
    
                List<Vlan> vlans = new ArrayList<Vlan>();
                Iterator<SnmpTableEntry> ite3 = snmpcoll.getVlanTable().getEntries().iterator();
                LogUtils.debugf(this, "store: saving Snmp Vlan Table to vlan table in DB");
                while (ite3.hasNext()) {
                    SnmpTableEntry ent = ite3.next();
    
                    Integer vlanindex = ent.getInt32(VlanCollectorEntry.VLAN_INDEX);
    
                    if (vlanindex == null || vlanindex < 0) {
                        LogUtils.warnf(this, "store: Not valid vlan ifindex" + vlanindex + " Skipping...");
                        continue;
                    }
    
                    String vlanName = ent.getDisplayString(VlanCollectorEntry.VLAN_NAME);
                    if (vlanName == null) {
                        LogUtils.warnf(this, "store: Null vlan name. forcing to default...");
                        vlanName = "default-" + vlanindex;
                    }
    
                    Integer vlantype = ent.getInt32(VlanCollectorEntry.VLAN_TYPE);
                    Integer vlanstatus = ent.getInt32(VlanCollectorEntry.VLAN_STATUS);
    
                    // always save info to DB
                    DbVlanEntry vlanEntry = DbVlanEntry.get(dbConn, node.getNodeId(), vlanindex);
                    if (vlanEntry == null) {
                        // Create a new entry
                        vlanEntry = DbVlanEntry.create(node.getNodeId(), vlanindex);
                    }
    
                    vlanEntry.updateVlanName(vlanName);
                    // okay to autobox these since were checking for null
                    if (vlantype != null) {
                        vlanEntry.updateVlanType(vlantype);
                    } else {
                        vlantype = -1;
                    }
                    if (vlanstatus != null) {
                        vlanEntry.updateVlanStatus(vlanstatus);
                    } else {
                        vlanstatus = -1;
                    }
                    vlanEntry.updateStatus(DbAtInterfaceEntry.STATUS_ACTIVE);
                    vlanEntry.set_lastpolltime(now);
    
                    LogUtils.debugf(this, "vlanEntry = %s", vlanEntry);

                    // store object in database
                    vlanEntry.store(dbConn);
                    Vlan vlan = new Vlan(vlanindex, vlanName, vlanstatus, vlantype);
    
                    vlans.add(vlan);
                }
                node.setVlans(vlans);
            }
    
            LogUtils.debugf(this, "store: saving SnmpVlanCollection's in DB");
    
            Iterator<Entry<Vlan, SnmpVlanCollection>> ite4 = snmpcoll.getSnmpVlanCollections().entrySet().iterator();
    
            SnmpVlanCollection snmpVlanColl = null;
            Vlan vlan = null;
            while (ite4.hasNext()) {
    
                Entry<Vlan, SnmpVlanCollection> entry = ite4.next();
    
                vlan = entry.getKey();
    
                int vlanid = vlan.getVlanIndex();
                String vlanname = vlan.getVlanName();
                String vlanindex = Integer.toString(vlanid);
                LogUtils.debugf(this, "store: parsing VLAN " + vlanindex + " VLAN_NAME " + vlanname);
    
                snmpVlanColl = entry.getValue();
    
                if (snmpVlanColl.hasDot1dBase()) {
                    LogUtils.debugf(this, "store: saving Dot1dBaseGroup in stpnode table");
    
                    Dot1dBaseGroup dod1db = snmpVlanColl.getDot1dBase();
    
                    DbStpNodeEntry dbStpNodeEntry = null;
    
                    String baseBridgeAddress = dod1db.getBridgeAddress();
                    if (baseBridgeAddress == null || baseBridgeAddress == "000000000000") {
                        LogUtils.warnf(this, "store: invalid base bridge address " + baseBridgeAddress);
                    } else {
                        node.addBridgeIdentifier(baseBridgeAddress, vlanindex);
                        int basenumports = dod1db.getNumberOfPorts();
    
                        int bridgetype = dod1db.getBridgeType();
    
                        if (snmpcoll.getSaveStpNodeTable()) {
                            dbStpNodeEntry = DbStpNodeEntry.get(dbConn, node.getNodeId(), vlanid);
                            if (dbStpNodeEntry == null) {
                                // Create a new entry
                                dbStpNodeEntry = DbStpNodeEntry.create(node.getNodeId(), vlanid);
                            }
                            // update object
    
                            dbStpNodeEntry.updateBaseBridgeAddress(baseBridgeAddress);
                            dbStpNodeEntry.updateBaseNumPorts(basenumports);
                            dbStpNodeEntry.updateBaseType(bridgetype);
                            dbStpNodeEntry.updateBaseVlanName(vlanname);
                        }
                        if (snmpVlanColl.hasDot1dStp()) {
                            LogUtils.debugf(this, "store: adding Dot1dStpGroup in stpnode table");
    
                            Dot1dStpGroup dod1stp = snmpVlanColl.getDot1dStp();
                            int protospec = dod1stp.getStpProtocolSpecification();
                            int stppriority = dod1stp.getStpPriority();
                            int stprootcost = dod1stp.getStpRootCost();
                            int stprootport = dod1stp.getStpRootPort();
                            String stpDesignatedRoot = dod1stp.getStpDesignatedRoot();
    
                            if (stpDesignatedRoot == null || stpDesignatedRoot == "0000000000000000") {
                                LogUtils.debugf(this, "store: Dot1dStpGroup found stpDesignatedRoot " + stpDesignatedRoot + " not adding to Linkable node");
                                stpDesignatedRoot = "0000000000000000";
                            } else {
                                node.setVlanStpRoot(vlanindex, stpDesignatedRoot);
                            }
    
                            if (snmpcoll.getSaveStpNodeTable()) {
                                dbStpNodeEntry.updateStpProtocolSpecification(protospec);
                                dbStpNodeEntry.updateStpPriority(stppriority);
                                dbStpNodeEntry.updateStpDesignatedRoot(stpDesignatedRoot);
                                dbStpNodeEntry.updateStpRootCost(stprootcost);
                                dbStpNodeEntry.updateStpRootPort(stprootport);
                            }
                        }
                        // store object in database
                        if (snmpcoll.getSaveStpNodeTable()) {
                            dbStpNodeEntry.updateStatus(DbStpNodeEntry.STATUS_ACTIVE);
                            dbStpNodeEntry.set_lastpolltime(now);
                            dbStpNodeEntry.store(dbConn);
                        }
    
                        if (snmpVlanColl.hasDot1dBasePortTable()) {
                            Iterator<Dot1dBasePortTableEntry> sub_ite = snmpVlanColl.getDot1dBasePortTable().getEntries().iterator();
                            LogUtils.debugf(this, "store: saving Dot1dBasePortTable in stpinterface table");
                            while (sub_ite.hasNext()) {
                                Dot1dBasePortTableEntry dot1dbaseptentry = sub_ite.next();
    
                                int baseport = dot1dbaseptentry.getBaseBridgePort();
                                int ifindex = dot1dbaseptentry.getBaseBridgePortIfindex();
    
                                if (baseport == -1 || ifindex == -1) {
                                    LogUtils.warnf(this, "store: Dot1dBasePortTable invalid baseport or ifindex " + baseport + " / " + ifindex);
                                    continue;
                                }
    
                                node.setIfIndexBridgePort(ifindex, baseport);
    
                                if (snmpcoll.getSaveStpInterfaceTable()) {
    
                                    DbStpInterfaceEntry dbStpIntEntry = DbStpInterfaceEntry.get(dbConn, node.getNodeId(), baseport, vlanid);
                                    if (dbStpIntEntry == null) {
                                        // Create a new entry
                                        dbStpIntEntry = DbStpInterfaceEntry.create(node.getNodeId(), baseport, vlanid);
                                    }
    
                                    dbStpIntEntry.updateIfIndex(ifindex);
                                    dbStpIntEntry.updateStatus(DbStpNodeEntry.STATUS_ACTIVE);
                                    dbStpIntEntry.set_lastpolltime(now);
                                    dbStpIntEntry.store(dbConn);
                                }
                            }
                        }
    
                        if (snmpVlanColl.hasDot1dStpPortTable()) {
                            LogUtils.debugf(this, "store: adding Dot1dStpPortTable in stpinterface table");
                            Iterator<Dot1dStpPortTableEntry> sub_ite = snmpVlanColl.getDot1dStpPortTable().getEntries().iterator();
                            while (sub_ite.hasNext()) {
                                Dot1dStpPortTableEntry dot1dstpptentry = sub_ite.next();
    
                                DbStpInterfaceEntry dbStpIntEntry = null;
    
                                int stpport = dot1dstpptentry.getDot1dStpPort();
    
                                if (stpport == -1) {
                                    LogUtils.warnf(this, "store: Dot1dStpPortTable found invalid stp port. Skipping");
                                    continue;
                                }
    
                                if (snmpcoll.getSaveStpInterfaceTable()) {
    
                                    dbStpIntEntry = DbStpInterfaceEntry.get(dbConn, node.getNodeId(), stpport, vlanid);
                                    if (dbStpIntEntry == null) {
                                        // Cannot create the object becouse must
                                        // exists
                                        // the dot1dbase
                                        // object!!!!!
                                        LogUtils.warnf(this, "store: StpInterface not found in database when storing STP info" + " for bridge node with nodeid " + node.getNodeId()
                                                    + " bridgeport number " + stpport + " and vlan index " + vlanindex + " skipping.");
                                    }
                                }
    
                                String stpPortDesignatedBridge = dot1dstpptentry.getDot1dStpPortDesignatedBridge();
                                String stpPortDesignatedPort = dot1dstpptentry.getDot1dStpPortDesignatedPort();
    
                                if (stpPortDesignatedBridge == null || stpPortDesignatedBridge.equals("0000000000000000")) {
                                    LogUtils.warnf(this, "store: " + stpPortDesignatedBridge + " designated bridge is invalid not adding to discoveryLink");
                                    stpPortDesignatedBridge = "0000000000000000";
                                } else if (stpPortDesignatedPort == null || stpPortDesignatedPort.equals("0000")) {
                                    LogUtils.warnf(this, "store: " + stpPortDesignatedPort + " designated port is invalid not adding to discoveryLink");
                                    stpPortDesignatedPort = "0000";
                                } else {
                                    BridgeStpInterface stpIface = new BridgeStpInterface(stpport, vlanindex);
                                    stpIface.setStpPortDesignatedBridge(stpPortDesignatedBridge);
                                    stpIface.setStpPortDesignatedPort(stpPortDesignatedPort);
                                    node.addStpInterface(stpIface);
                                }
    
                                if (snmpcoll.getSaveStpInterfaceTable()) {
                                    dbStpIntEntry.updateStpPortState(dot1dstpptentry.getDot1dStpPortState());
                                    dbStpIntEntry.updateStpPortPathCost(dot1dstpptentry.getDot1dStpPortPathCost());
                                    dbStpIntEntry.updateStpportDesignatedBridge(stpPortDesignatedBridge);
                                    dbStpIntEntry.updateStpportDesignatedRoot(dot1dstpptentry.getDot1dStpPortDesignatedRoot());
                                    dbStpIntEntry.updateStpPortDesignatedCost(dot1dstpptentry.getDot1dStpPortDesignatedCost());
                                    dbStpIntEntry.updateStpportDesignatedPort(stpPortDesignatedPort);
                                    dbStpIntEntry.updateStatus(DbStpNodeEntry.STATUS_ACTIVE);
                                    dbStpIntEntry.set_lastpolltime(now);
    
                                    dbStpIntEntry.store(dbConn);
    
                                }
                            }
                        }
    
                        if (snmpVlanColl.hasDot1dTpFdbTable()) {
                            LogUtils.debugf(this, "store: parsing Dot1dTpFdbTable");
    
                            Iterator<Dot1dTpFdbTableEntry> subite = snmpVlanColl.getDot1dFdbTable().getEntries().iterator();
                            while (subite.hasNext()) {
                                Dot1dTpFdbTableEntry dot1dfdbentry = subite.next();
                                String curMacAddress = dot1dfdbentry.getDot1dTpFdbAddress();
    
                                if (curMacAddress == null || curMacAddress.equals("000000000000")) {
                                    LogUtils.warnf(this, "store: Dot1dTpFdbTable invalid macaddress " + curMacAddress + " Skipping.");
                                    continue;
                                }
    
                                LogUtils.debugf(this, "store: Dot1dTpFdbTable found macaddress " + curMacAddress);
    
                                int fdbport = dot1dfdbentry.getDot1dTpFdbPort();
    
                                if (fdbport == 0 || fdbport == -1) {
                                    LogUtils.debugf(this, "store: Dot1dTpFdbTable mac learned on invalid port " + fdbport + " . Skipping");
                                    continue;
                                }
    
                                LogUtils.debugf(this, "store: Dot1dTpFdbTable mac address found " + " on bridge port " + fdbport);
    
                                int curfdbstatus = dot1dfdbentry.getDot1dTpFdbStatus();
    
                                if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_LEARNED) {
                                    node.addMacAddress(fdbport, curMacAddress, vlanindex);
                                    LogUtils.debugf(this, "store: Dot1dTpFdbTable found learned status" + " on bridge port ");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_SELF) {
                                    node.addBridgeIdentifier(curMacAddress);
                                    LogUtils.debugf(this, "store: Dot1dTpFdbTable mac is bridge identifier");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_INVALID) {
                                    LogUtils.debugf(this, "store: Dot1dTpFdbTable found INVALID status. Skipping");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_MGMT) {
                                    LogUtils.debugf(this, "store: Dot1dTpFdbTable found MGMT status. Skipping");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_OTHER) {
                                    LogUtils.debugf(this, "store: Dot1dTpFdbTable found OTHER status. Skipping");
                                } else if (curfdbstatus == -1) {
                                    LogUtils.warnf(this, "store: Dot1dTpFdbTable null status found. Skipping");
                                }
                            }
                        }
    
                        if (snmpVlanColl.hasQBridgeDot1dTpFdbTable()) {
                            LogUtils.debugf(this, "store: parsing QBridgeDot1dTpFdbTable");
    
                            Iterator<QBridgeDot1dTpFdbTableEntry> subite = snmpVlanColl.getQBridgeDot1dFdbTable().getEntries().iterator();
                            while (subite.hasNext()) {
                                QBridgeDot1dTpFdbTableEntry dot1dfdbentry = subite.next();
    
                                String curMacAddress = dot1dfdbentry.getQBridgeDot1dTpFdbAddress();
    
                                if (curMacAddress == null || curMacAddress.equals("000000000000")) {
                                    LogUtils.warnf(this, "store: QBridgeDot1dTpFdbTable invalid macaddress " + curMacAddress + " Skipping.");
                                    continue;
                                }
    
                                LogUtils.debugf(this, "store: Dot1dTpFdbTable found macaddress " + curMacAddress);
    
                                int fdbport = dot1dfdbentry.getQBridgeDot1dTpFdbPort();
    
                                if (fdbport == 0 || fdbport == -1) {
                                    LogUtils.debugf(this, "store: QBridgeDot1dTpFdbTable mac learned on invalid port " + fdbport + " . Skipping");
                                    continue;
                                }
    
                                LogUtils.debugf(this, "store: QBridgeDot1dTpFdbTable mac address found " + " on bridge port " + fdbport);
    
                                int curfdbstatus = dot1dfdbentry.getQBridgeDot1dTpFdbStatus();
    
                                if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_LEARNED) {
                                    node.addMacAddress(fdbport, curMacAddress, vlanindex);
                                    LogUtils.debugf(this, "store: QBridgeDot1dTpFdbTable found learned status" + " on bridge port ");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_SELF) {
                                    node.addBridgeIdentifier(curMacAddress);
                                    LogUtils.debugf(this, "store: QBridgeDot1dTpFdbTable mac is bridge identifier");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_INVALID) {
                                    LogUtils.debugf(this, "store: QBridgeDot1dTpFdbTable found INVALID status. Skipping");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_MGMT) {
                                    LogUtils.debugf(this, "store: QBridgeDot1dTpFdbTable found MGMT status. Skipping");
                                } else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_OTHER) {
                                    LogUtils.debugf(this, "store: QBridgeDot1dTpFdbTable found OTHER status. Skipping");
                                } else if (curfdbstatus == -1) {
                                    LogUtils.warnf(this, "store: QBridgeDot1dTpFdbTable null status found. Skipping");
                                }
                            }
                        }
    
                        // now adding bridge identifier mac addresses of switch
                        // from
                        // snmpinterface
                        PreparedStatement stmt = null;
                        stmt = dbConn.prepareStatement(SQL_GET_SNMPPHYSADDR_SNMPINTERFACE);
                        d.watch(stmt);
                        stmt.setInt(1, node.getNodeId());
    
                        ResultSet rs = stmt.executeQuery();
                        d.watch(rs);
    
                        while (rs.next()) {
                            String macaddr = rs.getString("snmpphysaddr");
                            if (macaddr == null) continue;
                            node.addBridgeIdentifier(macaddr);
                            LogUtils.debugf(this, "setBridgeIdentifierFromSnmpInterface: found bridge identifier " + macaddr + " from snmpinterface db table");
                        }
    
                    }
                }
            }
            update(dbConn, now, node.getNodeId());
    
            return node;
        } finally {
            d.cleanUp();
        }

    }

    private void update(Connection dbConn, Timestamp now, int nodeid) throws SQLException {

        final DBUtils d = new DBUtils(getClass());

        try {
            PreparedStatement stmt = null;
    
            int i = 0;
            stmt = dbConn.prepareStatement(SQL_UPDATE_ATINTERFACE);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            stmt.setTimestamp(2, now);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "store: SQL statement " + SQL_UPDATE_ATINTERFACE + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_VLAN);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            stmt.setTimestamp(2, now);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "store: SQL statement " + SQL_UPDATE_VLAN + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_IPROUTEINTERFACE);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            stmt.setTimestamp(2, now);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "store: SQL statement " + SQL_UPDATE_IPROUTEINTERFACE + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_STPNODE);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            stmt.setTimestamp(2, now);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "store: SQL statement " + SQL_UPDATE_STPNODE + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_STPINTERFACE);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            stmt.setTimestamp(2, now);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "store: SQL statement " + SQL_UPDATE_STPINTERFACE + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
        } finally {
            d.cleanUp();
        }
    }

    /** {@inheritDoc} */
    public void update(int nodeid, char status) throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {
            Connection dbConn = getConnection();
            d.watch(dbConn);
            PreparedStatement stmt = null;
    
            int i = 0;
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_VLAN_STATUS);
            d.watch(stmt);
            stmt.setString(1, new String(new char[] { status }));
            stmt.setInt(2, nodeid);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "update: SQL statement " + SQL_UPDATE_VLAN_STATUS + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_ATINTERFACE_STATUS);
            d.watch(stmt);
            stmt.setString(1, new String(new char[] { status }));
            stmt.setInt(2, nodeid);
            stmt.setInt(3, nodeid);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "update: SQL statement " + SQL_UPDATE_ATINTERFACE_STATUS + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_IPROUTEINTERFACE_STATUS);
            d.watch(stmt);
            stmt.setString(1, new String(new char[] { status }));
            stmt.setInt(2, nodeid);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "update: SQL statement " + SQL_UPDATE_IPROUTEINTERFACE_STATUS + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_STPNODE_STATUS);
            d.watch(stmt);
            stmt.setString(1, new String(new char[] { status }));
            stmt.setInt(2, nodeid);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "update: SQL statement " + SQL_UPDATE_STPNODE_STATUS + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_STPINTERFACE_STATUS);
            d.watch(stmt);
            stmt.setString(1, new String(new char[] { status }));
            stmt.setInt(2, nodeid);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "update: SQL statement " + SQL_UPDATE_STPINTERFACE_STATUS + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
    
            stmt = dbConn.prepareStatement(SQL_UPDATE_DATALINKINTERFACE_STATUS);
            d.watch(stmt);
            stmt.setString(1, new String(new char[] { status }));
            stmt.setInt(2, nodeid);
            stmt.setInt(3, nodeid);
    
            i = stmt.executeUpdate();
            LogUtils.debugf(this, "update: SQL statement " + SQL_UPDATE_DATALINKINTERFACE_STATUS + ". " + i + " rows UPDATED for nodeid=" + nodeid + ".");
        } finally {
            d.cleanUp();
        }

    }

    private int getNodeidFromIp(Connection dbConn, InetAddress ipaddr) throws SQLException {

        if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return -1;

        int nodeid = -1;

        final DBUtils d = new DBUtils(getClass());
        try {
            PreparedStatement stmt = null;
            stmt = dbConn.prepareStatement(SQL_GET_NODEID);
            d.watch(stmt);
            stmt.setString(1, ipaddr.getHostAddress());
    
            LogUtils.debugf(this, "getNodeidFromIp: executing query " + SQL_GET_NODEID + " with ip address=" + ipaddr.getHostAddress());
    
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            if (!rs.next()) {
                LogUtils.debugf(this, "getNodeidFromIp: no entries found in ipinterface");
                return -1;
            }
            // extract the values.
            //
            int ndx = 1;
    
            // get the node id
            //
            nodeid = rs.getInt(ndx++);
            if (rs.wasNull()) nodeid = -1;
    
            LogUtils.debugf(this, "getNodeidFromIp: found nodeid " + nodeid);
        } finally {
            d.cleanUp();
        }

        return nodeid;

    }

    private RouterInterface getNodeidMaskFromIp(Connection dbConn, InetAddress ipaddr) throws SQLException {
        if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return null;

        int nodeid = -1;
        int ifindex = -1;
        String netmask = null;

        PreparedStatement stmt = null;

        final DBUtils d = new DBUtils(getClass());
        try {
            stmt = dbConn.prepareStatement(SQL_GET_NODEID__IFINDEX_MASK);
            d.watch(stmt);
            stmt.setString(1, ipaddr.getHostAddress());
    
            LogUtils.debugf(this, "getNodeidMaskFromIp: executing query " + SQL_GET_NODEID__IFINDEX_MASK + " with ip address=" + ipaddr.getHostAddress());
    
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            if (!rs.next()) {
                LogUtils.debugf(this, "getNodeidMaskFromIp: no entries found in snmpinterface");
                return null;
            }
            // extract the values.
            //
            // get the node id
            //
            nodeid = rs.getInt("nodeid");
            if (rs.wasNull()) {
                LogUtils.debugf(this, "getNodeidMaskFromIp: no nodeid found");
                return null;
            }
    
            ifindex = rs.getInt("snmpifindex");
            if (rs.wasNull()) {
                LogUtils.debugf(this, "getNodeidMaskFromIp: no snmsnmpifindex found");
                ifindex = -1;
            }
    
            netmask = rs.getString("snmpipadentnetmask");
            if (rs.wasNull()) {
                LogUtils.debugf(this, "getNodeidMaskFromIp: no snmpipadentnetmask found");
                netmask = "255.255.255.255";
            }
        } finally {
            d.cleanUp();
        }

        RouterInterface ri = new RouterInterface(nodeid, ifindex, netmask);
        return ri;

    }

    private RouterInterface getNodeFromIp(Connection dbConn, InetAddress ipaddr) throws SQLException {
        if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return null;

        int nodeid = -1;
        int ifindex = -1;

        PreparedStatement stmt = null;

        final DBUtils d = new DBUtils(getClass());
        try {
            stmt = dbConn.prepareStatement(SQL_GET_NODEID);
            d.watch(stmt);
            stmt.setString(1, ipaddr.getHostAddress());
    
            LogUtils.debugf(this, "getNodeFromIp: executing query " + SQL_GET_NODEID + " with ip address=" + ipaddr.getHostAddress());
    
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            if (!rs.next()) {
                LogUtils.debugf(this, "getNodeFromIp: no entries found in snmpinterface");
                return null;
            }
            // extract the values.
            //
            // get the node id
            //
            nodeid = rs.getInt("nodeid");
            if (rs.wasNull()) {
                LogUtils.debugf(this, "getNodeFromIp: no nodeid found");
                return null;
            }
        } finally {
            d.cleanUp();
        }

        RouterInterface ri = new RouterInterface(nodeid, ifindex);
        return ri;

    }

    private AtInterface getNodeidIfindexFromIp(Connection dbConn, InetAddress ipaddr) throws SQLException {

        if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return null;

        int atnodeid = -1;
        int atifindex = -1;
        AtInterface ati = null;

        final DBUtils d = new DBUtils(getClass());
        try {
            PreparedStatement stmt = dbConn.prepareStatement(SQL_GET_NODEID_IFINDEX_IPINT);
            d.watch(stmt);
    
            stmt.setString(1, ipaddr.getHostAddress());
    
            LogUtils.debugf(this, "getNodeidIfindexFromIp: executing SQL Statement " + SQL_GET_NODEID_IFINDEX_IPINT + " with ip address=" + ipaddr.getHostAddress());
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            if (!rs.next()) {
                return null;
            }
    
            atnodeid = rs.getInt("nodeid");
            if (rs.wasNull()) { return null; }
            // save info for DiscoveryLink
            ati = new AtInterface(atnodeid, ipaddr.getHostAddress());
    
            // get ifindex if exists
            atifindex = rs.getInt("ifindex");
            if (rs.wasNull()) {
                LogUtils.infof(this, "getNodeidIfindexFromIp: nodeid " + atnodeid + " no ifindex (-1) found for ipaddress " + ipaddr + ".");
            } else {
                LogUtils.infof(this, "getNodeidIfindexFromIp: nodeid " + atnodeid + " ifindex " + atifindex + " found for ipaddress " + ipaddr + ".");
                ati.setIfindex(atifindex);
            }
        } finally {
            d.cleanUp();
        }

        return ati;

    }

    private int getSnmpIfType(Connection dbConn, int nodeid, int ifindex) throws SQLException {

        int snmpiftype = -1;
        PreparedStatement stmt = null;

        final DBUtils d = new DBUtils(getClass());
        try {
            stmt = dbConn.prepareStatement(SQL_GET_SNMPIFTYPE);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            stmt.setInt(2, ifindex);
    
            LogUtils.debugf(this, "getSnmpIfType: executing query " + SQL_GET_SNMPIFTYPE + " with nodeid=" + nodeid + " and ifindex=" + ifindex);
    
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            if (!rs.next()) {
                LogUtils.debugf(this, "getSnmpIfType: no entries found in snmpinterface");
                return -1;
            }
    
            // extract the values.
            //
            int ndx = 1;
    
            // get the node id
            //
            snmpiftype = rs.getInt(ndx++);
            if (rs.wasNull()) snmpiftype = -1;
    
            LogUtils.debugf(this, "getSnmpIfType: found in snmpinterface snmpiftype=" + snmpiftype);
    
            return snmpiftype;
        } finally {
            d.cleanUp();
        }

    }

    private int getIfIndexByName(Connection dbConn, int nodeid, String ifName) throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {
            PreparedStatement stmt = null;
            stmt = dbConn.prepareStatement(SQL_GET_IFINDEX_SNMPINTERFACE_NAME);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            stmt.setString(2, ifName);
            stmt.setString(3, ifName);
    
            LogUtils.debugf(this, "getIfIndexByName: executing query" + SQL_GET_IFINDEX_SNMPINTERFACE_NAME + "nodeid =" + nodeid + "and ifName=" + ifName);
    
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            if (!rs.next()) {
                LogUtils.debugf(this, "getIfIndexByName: no entries found in snmpinterface");
                return -1;
            }
    
            // extract the values.
            //
            int ndx = 1;
    
            if (rs.wasNull()) {
    
                LogUtils.debugf(this, "getIfIndexByName: no entries found in snmpinterface");
                return -1;
    
            }
    
            int ifindex = rs.getInt(ndx++);
    
            LogUtils.debugf(this, "getIfIndexByName: found ifindex=" + ifindex);
    
            return ifindex;
        } finally {
            d.cleanUp();
        }
    }

    private void sendNewSuspectEvent(InetAddress ipaddress, InetAddress ipowner, String name) {
        Linkd.getInstance().sendNewSuspectEvent(ipaddress.getHostAddress(), ipowner.getHostAddress(), name);
    }

    /** {@inheritDoc} */
    public LinkableNode getSnmpNode(int nodeid) throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {

            Connection dbConn = getConnection();
            d.watch(dbConn);
            LinkableNode node = null;
    
            PreparedStatement stmt = dbConn.prepareStatement(SQL_SELECT_SNMP_NODE);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            LogUtils.debugf(this, "getSnmpCollection: execute '" + SQL_SELECT_SNMP_NODE + "' with nodeid =" + nodeid);
    
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            while (rs.next()) {
                String sysoid = rs.getString("nodesysoid");
                if (sysoid == null) sysoid = "-1";
                String ipaddr = rs.getString("ipaddr");
                LogUtils.debugf(this, "getSnmpCollection: found nodeid " + nodeid + " ipaddr " + ipaddr + " sysoid " + sysoid);
    
                node = new LinkableNode(nodeid, ipaddr, sysoid);
            }
    
            return node;
        } finally {
            d.cleanUp();
        }

    }

    /**
     * <p>getSnmpNodeList</p>
     *
     * @return a {@link java.util.List} object.
     * @throws java.sql.SQLException if any.
     */
    public List<LinkableNode> getSnmpNodeList() throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {

            Connection dbConn = getConnection();
            d.watch(dbConn);
    
            List<LinkableNode> linknodes = new ArrayList<LinkableNode>();
            PreparedStatement ps = dbConn.prepareStatement(SQL_SELECT_SNMP_NODES);
            d.watch(ps);
    
            ResultSet rs = ps.executeQuery();
            d.watch(rs);
            LogUtils.debugf(this, "getNodesInfo: execute query: \" " + SQL_SELECT_SNMP_NODES + "\"");
    
            while (rs.next()) {
                int nodeid = rs.getInt("nodeid");
                String ipaddr = rs.getString("ipaddr");
                String sysoid = rs.getString("nodesysoid");
                if (sysoid == null) sysoid = "-1";
                LogUtils.debugf(this, "getNodesInfo: found node element: nodeid " + nodeid + " ipaddr " + ipaddr + " sysoid " + sysoid);
    
                LinkableNode node = new LinkableNode(nodeid, ipaddr, sysoid);
                linknodes.add(node);
    
            }
    
            LogUtils.debugf(this, "getNodesInfo: found " + linknodes.size() + " snmp primary ip nodes");
    
            return linknodes;
        } finally {
            d.cleanUp();
        }
    }

    /**
     * <p>updateDeletedNodes</p>
     *
     * @throws java.sql.SQLException if any.
     */
    public void updateDeletedNodes() throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {

            Connection dbConn = getConnection();
            d.watch(dbConn);
    
            // update atinterface
            int i = 0;
            PreparedStatement ps = dbConn.prepareStatement(SQL_UPDATE_ATINTERFACE_D);
            d.watch(ps);
            i = ps.executeUpdate();
            LogUtils.infof(this, "updateDeletedNodes: execute '" + SQL_UPDATE_ATINTERFACE_D + "' updated rows: " + i);
    
            // update vlan
            ps = dbConn.prepareStatement(SQL_UPDATE_VLAN_D);
            d.watch(ps);
            i = ps.executeUpdate();
            LogUtils.infof(this, "updateDeletedNodes: execute '" + SQL_UPDATE_VLAN_D + "' updated rows: " + i);
    
            // update stpnode
            ps = dbConn.prepareStatement(SQL_UPDATE_STPNODE_D);
            d.watch(ps);
            i = ps.executeUpdate();
            LogUtils.infof(this, "updateDeletedNodes: execute '" + SQL_UPDATE_STPNODE_D + "' updated rows: " + i);
    
            // update stpinterface
            ps = dbConn.prepareStatement(SQL_UPDATE_STPINTERFACE_D);
            d.watch(ps);
            i = ps.executeUpdate();
            LogUtils.infof(this, "updateDeletedNodes: execute '" + SQL_UPDATE_STPINTERFACE_D + "' updated rows: " + i);
    
            // update iprouteinterface
            ps = dbConn.prepareStatement(SQL_UPDATE_IPROUTEINTERFACE_D);
            d.watch(ps);
            i = ps.executeUpdate();
            LogUtils.infof(this, "updateDeletedNodes: execute '" + SQL_UPDATE_IPROUTEINTERFACE_D + "'updated rows: " + i);
    
            // update datalinkinterface
            ps = dbConn.prepareStatement(SQL_UPDATE_DATALINKINTERFACE_D);
            d.watch(ps);
            i = ps.executeUpdate();
            LogUtils.infof(this, "updateDeletedNodes: execute '" + SQL_UPDATE_DATALINKINTERFACE_D + "' updated rows: " + i);
        } finally {
            d.cleanUp();
        }

    }
    
    /** {@inheritDoc} */
    public void updateForInterface(int nodeId, String ipAddr, int ifIndex, char status) throws SQLException {
        final DBUtils d = new DBUtils(getClass());
        try {
            Connection dbConn = getConnection();
            d.watch(dbConn);
            PreparedStatement ps = null;
            int i=0;
            if(!EventUtils.isNonIpInterface(ipAddr)) {  
                // update atinterface
                ps = dbConn.prepareStatement(SQL_UPDATE_ATINTERFACE_STATUS_INTFC);
                d.watch(ps);
                ps.setString(1, new String(new char[] { status }));
                ps.setInt(2, nodeId);
                ps.setString(3, ipAddr);
                i = ps.executeUpdate();
                LogUtils.infof(this, "updateForInterface: atinterface: node = " + nodeId
                               + ", IP Address = " + ipAddr + ", status = " + status + ": updated rows = " + i);
            }
            if(ifIndex > -1) {
                 // update atinterface
                ps = dbConn.prepareStatement(SQL_UPDATE_ATINTERFACE_STATUS_SRC_INTFC);
                d.watch(ps);
                ps.setString(1, new String(new char[] { status }));
                ps.setInt(2, nodeId);
                ps.setInt(3, ifIndex);
                i = ps.executeUpdate();
                LogUtils.infof(this, "updateForInterface: atinterface: source node = " + nodeId
                               + ", ifIndex = " + ifIndex + ", status = " + status + ": updated rows = " + i);
                // update stpinterface
                ps = dbConn.prepareStatement(SQL_UPDATE_STPINTERFACE_STATUS_INTFC);
                d.watch(ps);
                ps.setString(1, new String(new char[] { status }));
                ps.setInt(2, nodeId);
                ps.setInt(3, ifIndex);
                i = ps.executeUpdate();
                LogUtils.infof(this, "updateForInterface: stpinterface: node = " + nodeId
                               + ", ifIndex = " + ifIndex  + ", status = " + status + ": updated rows = " + i);
    
                // update iprouteinterface
                ps = dbConn.prepareStatement(SQL_UPDATE_IPROUTEINTERFACE_STATUS_INTFC);
                d.watch(ps);
                ps.setString(1, new String(new char[] { status }));
                ps.setInt(2, nodeId);
                ps.setInt(3, ifIndex);
                i = ps.executeUpdate();
                LogUtils.infof(this, "updateForInterface: iprouteinterface: node = " + nodeId
                               + ", rpouteIfIndex = " + ifIndex  + ", status = " + status + ": updated rows = " + i);
    
                // update datalinkinterface
                ps = dbConn.prepareStatement(SQL_UPDATE_DATALINKINTERFACE_STATUS_INTFC);
                d.watch(ps);
                ps.setString(1, new String(new char[] { status }));
                ps.setInt(2, nodeId);
                ps.setInt(3, ifIndex);
                ps.setInt(4, nodeId);
                ps.setInt(5, ifIndex);
                i = ps.executeUpdate();
                LogUtils.infof(this, "updateForInterface: datalinkinterface: node = " + nodeId
                               + ", ifIndex = " + ifIndex  + ", status = " + status + ": updated rows = " + i);
            }
            
        } finally {
            d.cleanUp();
        }
    }
    
    /** {@inheritDoc} */
    public String getSnmpPrimaryIp(int nodeid) throws SQLException {

        final DBUtils d = new DBUtils(getClass());
        try {

            Connection dbConn = getConnection();
            d.watch(dbConn);

            /**
             * Query to select info for specific node
             */
    
            String ipaddr = null;
            PreparedStatement stmt = dbConn.prepareStatement(SQL_SELECT_SNMP_IP_ADDR);
            d.watch(stmt);
            stmt.setInt(1, nodeid);
            LogUtils.debugf(this, "getSnmpPrimaryIp: SQL statement = " + stmt.toString());
    
            ResultSet rs = stmt.executeQuery();
            d.watch(rs);
    
            while (rs.next()) {
                ipaddr = rs.getString("ipaddr");
                if (ipaddr == null) return null;
                LogUtils.debugf(this, "getSnmpPrimaryIp: found node element: nodeid " + nodeid + " ipaddr " + ipaddr);
    
            }
            return ipaddr;
        } finally {
            d.cleanUp();
        }

    }

    /** {@inheritDoc} */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

}
