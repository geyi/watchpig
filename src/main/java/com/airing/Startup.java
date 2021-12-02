package com.airing;

import com.airing.entity.NodeInfo;
import com.airing.health.AppHealthCheck;
import com.airing.health.DBHealthCheck;
import com.airing.utils.PropertiesUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Startup {

    private static final Logger log = LoggerFactory.getLogger(Startup.class);

    // watchpig pid file
    private static final String PID = "watchpig.pid";
    // watchpig node id file
    private static final String WP_NODE_ID = "watchpig_node_id";

    public static int nodeId;

    public static void main(String[] args) throws Exception {
        generatePid();

        InputStream is = Startup.class.getClassLoader().getResourceAsStream(WP_NODE_ID);
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        char[] buffer = new char[1];
        inputStreamReader.read(buffer);
        nodeId = Integer.parseInt(new String(buffer));
        log.info("watchpig_node_id: {}", nodeId);

        PropertiesUtils.getInstance().load(nodeId);
        log.info("current service hostname: {}, port: {}", PropertiesUtils.hostname, PropertiesUtils.port);
        log.info("another node list: {}", JSONObject.toJSONString(PropertiesUtils.anotherNodeList));

        Server watchPig = new Server(2, 2);
        Channel channel = watchPig.startServer(PropertiesUtils.port);

        for (NodeInfo nodeInfo : PropertiesUtils.anotherNodeList) {
            Client client = new Client();
            client.startClient(nodeInfo);
        }

        log.info("app list: {}", JSONObject.toJSONString(PropertiesUtils.appList));
        ScheduledExecutorService appMonitorScheduled = Executors.newSingleThreadScheduledExecutor();
        appMonitorScheduled.scheduleWithFixedDelay(new AppHealthCheck(PropertiesUtils.appList), 10000,
                5000, TimeUnit.MILLISECONDS);

        log.info("db list: {}", JSONObject.toJSONString(PropertiesUtils.dbList));
        ScheduledExecutorService dbMonitorScheduled = Executors.newSingleThreadScheduledExecutor();
        dbMonitorScheduled.scheduleWithFixedDelay(new DBHealthCheck(PropertiesUtils.dbList), 10000,
                5000, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> watchPig.destroy()));
        channel.closeFuture().syncUninterruptibly();
    }

    private static void generatePid() throws IOException {
        File file = new File(PID);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(getPid().getBytes());
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
}
