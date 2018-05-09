package org.opennms.smoketest;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opennms.test.system.api.NewTestEnvironment.ContainerAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.dao.hibernate.AlarmDaoHibernate;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpTrapBuilder;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.smoketest.utils.DaoUtils;
import org.opennms.smoketest.utils.HibernateDaoFactory;
import org.opennms.test.system.api.TestEnvironment;
import org.opennms.test.system.api.TestEnvironmentBuilder;

public class SnmpV3IT {

    private static final Logger LOG = LoggerFactory.getLogger(SnmpV3IT.class);

    private static TestEnvironment m_testEnvironment;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @ClassRule
    public static final TestEnvironment getTestEnvironment() {

        if (!OpenNMSSeleniumTestCase.isDockerEnabled()) {
            return new NullTestEnvironment();
        }

        try {
            final TestEnvironmentBuilder builder = TestEnvironment.builder().opennms();
            builder.withOpenNMSEnvironment().addFile(SnmpV3IT.class.getResource("/trapd-configuration.xml"),
                    "etc/trapd-configuration.xml");
            OpenNMSSeleniumTestCase.configureTestEnvironment(builder);
            m_testEnvironment = builder.build();
            return m_testEnvironment;
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }

    }

    @Before
    public void checkForDocker() {
        Assume.assumeTrue(OpenNMSSeleniumTestCase.isDockerEnabled());
    }

    @Test
    public void testSnmpV3Traps() throws Exception {
        Date startOfTest = new Date();
        final InetSocketAddress opennmsSnmp = m_testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 162);
        InetSocketAddress pgsql = m_testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 5432);
        HibernateDaoFactory daoFactory = new HibernateDaoFactory(pgsql);
        AlarmDao alarmDao = daoFactory.getDao(AlarmDaoHibernate.class);

        Criteria criteria = new CriteriaBuilder(OnmsAlarm.class)
                .eq("uei", "uei.opennms.org/generic/traps/EnterpriseDefault").ge("lastEventTime", startOfTest)
                .toCriteria();

        executor.scheduleWithFixedDelay(() -> {
            try {
                sendV3Trap(opennmsSnmp);
            } catch (Exception e) {
                LOG.error("exception while sending traps");
            }
        }, 0, 5, TimeUnit.SECONDS);
        // Check if there is atleast one alarm
        await().atMost(30, SECONDS).pollInterval(10, SECONDS).pollDelay(10, SECONDS)
                .until(DaoUtils.countMatchingCallable(alarmDao, criteria), greaterThanOrEqualTo(1));
        // Check if multiple traps are getting received not just the first one
        await().atMost(30, SECONDS).pollInterval(10, SECONDS).pollDelay(10, SECONDS)
                .until(DaoUtils.findMatchingCallable(alarmDao, new CriteriaBuilder(OnmsAlarm.class)
                        .eq("uei", "uei.opennms.org/generic/traps/EnterpriseDefault").ge("counter", 5).toCriteria()),
                        notNullValue());
        executor.shutdown();
    }

    private void sendV3Trap(InetSocketAddress opennmsSnmp) throws Exception {

        SnmpTrapBuilder pdu = SnmpUtils.getV3TrapBuilder();
        pdu.addVarBind(SnmpObjId.get(".1.3.6.1.2.1.1.3.0"), SnmpUtils.getValueFactory().getTimeTicks(0));
        pdu.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.1.0"),
                SnmpUtils.getValueFactory().getObjectId(SnmpObjId.get(".1.3.6.1.6.3.1.1.5.4.0")));
        pdu.send(InetAddressUtils.str(opennmsSnmp.getAddress()), opennmsSnmp.getPort(), "traptest");
        LOG.info("V3 trap sent successfully");

    }

}
