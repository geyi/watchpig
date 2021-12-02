package com.airing.utils;

import com.airing.entity.AppInfo;
import com.airing.entity.DBInfo;
import com.airing.entity.NodeInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesUtils {

    private static volatile PropertiesUtils instance;

    public static List<String> groupList = new ArrayList<>();

    public static String hostname;
    public static int port;

    public static List<NodeInfo> anotherNodeList = new ArrayList<>();
    public static List<AppInfo> appList = new ArrayList<>();
    public static List<DBInfo> dbList = new ArrayList<>();

    private PropertiesUtils() {}

    public static PropertiesUtils getInstance() {
        if (instance == null) {
            synchronized (PropertiesUtils.class) {
                if (instance == null) {
                    instance = new PropertiesUtils();
                }
            }
        }
        return instance;
    }

    public void load(int nodeId) throws IOException {
        Properties properties = new Properties();
        InputStream is = PropertiesUtils.class.getClassLoader().getResourceAsStream("config.properties");
        properties.load(is);

        String groups = properties.getProperty("groups");
        for (String group : StringUtils.split(groups, ',')) {
            groupList.add(group);
        }
        hostname = properties.getProperty("hostname" + nodeId);
        port = Integer.parseInt(properties.getProperty("port" + nodeId));

        String nidStr;
        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith("hostname")
                    && !(nidStr = propertyName.substring(8)).equals(String.valueOf(nodeId))) {
                String hostname = properties.getProperty(propertyName);
                String port = properties.getProperty("port" + nidStr);
                NodeInfo nodeInfo = new NodeInfo(Integer.valueOf(nidStr), hostname, Integer.valueOf(port));
                anotherNodeList.add(nodeInfo);
            } else if (propertyName.startsWith("app_hostname")) {
                int num = Integer.parseInt(propertyName.substring(12));
                AppInfo appInfo = new AppInfo();
                appInfo.setHostname(properties.getProperty(propertyName));
                appInfo.setPort(Integer.valueOf(properties.getProperty("app_port" + num)));
                appInfo.setHealthApi(properties.getProperty("app_health_api" + num));
                appInfo.setGroup(properties.getProperty("app_group" + num));
                appList.add(appInfo);
            } else if (propertyName.startsWith("db_hostname")) {
                int num = Integer.parseInt(propertyName.substring(11));
                DBInfo dbInfo = new DBInfo();
                dbInfo.setHostname(properties.getProperty(propertyName));
                dbInfo.setPort(Integer.valueOf(properties.getProperty("db_port" + num)));
                dbInfo.setUsername(properties.getProperty("db_username" + num));
                dbInfo.setPassword(properties.getProperty("db_password" + num));
                dbInfo.setGroup(properties.getProperty("db_group" + num));
                dbList.add(dbInfo);
            }
        }
    }
}
