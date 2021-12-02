package com.airing.health;

import com.airing.NettySocketHolder;
import com.airing.Startup;
import io.netty.channel.Channel;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;

public class HealthCheckUtils {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckUtils.class);

    public static int getMinNodeId() {
        int min = Startup.nodeId;
        Map<String, Channel> map = NettySocketHolder.getMAP();
        if (!map.isEmpty()) {
            Set<String> nodeIds = map.keySet();
            for (String nodeId : nodeIds) {
                min = Math.min(Integer.parseInt(nodeId), min);
            }
        }
        return min;
    }

    public static String exec(String command) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            CommandLine commandline = CommandLine.parse(command);
            DefaultExecutor exec = new DefaultExecutor();
            exec.setExitValues(null);
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandline);
            String out = outputStream.toString("utf-8");
            String error = errorStream.toString("utf-8");
            String result = out + error;
            return result;
        } catch (Exception e) {
            log.error("exec error: {}", e.getMessage());
            return e.toString();
        }
    }

    public static void failover(int failedNodeId, String failedNodeHost, int newMainNodeId, String newMainNodeHost,
                                int oldPrimaryNodeId) {
        if (!DBHealthCheck.isPrimary(newMainNodeHost, 5432)) {
            synchronized (HealthCheckUtils.class) {
                if (!DBHealthCheck.isPrimary(newMainNodeHost, 5432)) {
                    String failover = "/root/WatchPig/bin/failover.sh %d %s %d %s %d";
                    String command = String.format(failover, failedNodeId, failedNodeHost, newMainNodeId,
                            newMainNodeHost,
                            oldPrimaryNodeId);
                    String ret = exec(command);
                    log.info(ret);
                }
            }
        }
    }

    public static void stop(String hostname) {
        String stop = "/root/WatchPig/bin/stop.sh %s";
        String command = String.format(stop, hostname);
        String ret = exec(command);
        log.info(ret);
    }
}
