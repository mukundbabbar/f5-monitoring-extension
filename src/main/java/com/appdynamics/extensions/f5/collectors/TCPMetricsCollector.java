package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.TCP;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.SystemStatisticsBindingStub;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class TCPMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(TCPMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> metricExcludes;

    public TCPMetricsCollector(Interfaces iControlInterfaces,
                               F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.metricExcludes = metricsFilter.getTcpMetricExcludes();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.System__Statistics__get_tcp_statistics.ashx
     *
     */
    public Void call() {
        LOGGER.info("TCP metrics collector started...");

        try {
            SystemStatisticsBindingStub systemStats = iControlInterfaces.getSystemStatistics();

            if (systemStats != null) {
                String tcpMetricPrefix = getTCPMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (CommonStatistic stat : systemStats.get_tcp_statistics().getStatistics()) {
                    if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                        String metricName = String.format("%s%s", tcpMetricPrefix, stat.getType().getValue());
                        BigInteger value = convertValue(stat.getValue());
                        printCollectiveObservedCurrent(metricName, value);
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching tcp statistics", e);
        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching tcp statistics", e);
        }

        return null;
    }

    private String getTCPMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                "Network", METRIC_PATH_SEPARATOR, TCP, METRIC_PATH_SEPARATOR);
    }
}
