package com.airing.utils.http;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpRequestUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestUtils.class);

    private static final HttpClient HTTP_CLIENT = new HttpClient();
    private static final String DEFAULT_CHARSET = "UTF-8";

    public static String get(String url, Map<String, String> headerParams, int requestTimeout, int socketTimeout) {
        CloseableHttpResponse response = null;
        try {
            HttpGet httpRequest = new HttpGet(url);
            CloseableHttpClient client = HTTP_CLIENT.getHttpClient(requestTimeout, socketTimeout);
            if (headerParams != null && headerParams.size() > 0) {
                for (Map.Entry<String, String> key : headerParams.entrySet()) {
                    String headerKey = key.getKey();
                    httpRequest.setHeader(headerKey, headerParams.get(headerKey));
                }
            }
            response = client.execute(httpRequest);
            int stateCode = response.getStatusLine().getStatusCode();
            byte[] resultByte = EntityUtils.toByteArray(response.getEntity());
            String resultStr = new String(resultByte, DEFAULT_CHARSET);
            log.debug("url:{}, stateCode:{}, response:{}", url, stateCode, resultStr);
            return resultStr;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException();
        } finally {
            if (response != null) {
                try {
                    response.getEntity().getContent().close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static String get(String url) {
        return get(url, null, 5000, 5000);
    }

    public static String get(String url, int requestTimeout, int socketTimeout) {
        return get(url, null, requestTimeout, socketTimeout);
    }

    public static String post(String url, String jsonParams, int requestTimeout, int socketTimeout) {
        CloseableHttpResponse response = null;
        try {
            HttpPost httpRequest = new HttpPost(url);
            CloseableHttpClient client = HTTP_CLIENT.getHttpClient(10000, 10000);
            // 设置请求头参数
            httpRequest.addHeader("Content-type", "application/json; charset=utf-8");
            httpRequest.setHeader("Accept", "application/json");
            if (jsonParams != null) {
                httpRequest.setEntity(new StringEntity(jsonParams, Charset.forName(DEFAULT_CHARSET)));
            }
            response = client.execute(httpRequest);
            int stateCode = response.getStatusLine().getStatusCode();
            byte[] resultByte = EntityUtils.toByteArray(response.getEntity());
            String resultStr = new String(resultByte, DEFAULT_CHARSET);
            log.debug("url:{}, jsonParams:{}, stateCode:{}, response:{}", url, jsonParams, stateCode, resultStr);
            return resultStr;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        } finally {
            if (response != null) {
                try {
                    response.getEntity().getContent().close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static String post(String url, String jsonParams) {
        return post(url, jsonParams, 5000, 5000);
    }

    public static String postForm(String url, Map<String, String> headerParams, Map<String, String> params) {
        CloseableHttpResponse response = null;
        try {
            HttpPost httpRequest = new HttpPost(url);
            CloseableHttpClient client = HTTP_CLIENT.getHttpClient(10000, 10000);
            if (headerParams != null && headerParams.size() > 0) {
                for (Map.Entry<String, String> key : headerParams.entrySet()) {
                    String headerKey = key.getKey();
                    httpRequest.setHeader(headerKey, headerParams.get(headerKey));
                }
            }
            List<BasicNameValuePair> pairList = new ArrayList<>();
            Set<Map.Entry<String, String>> paramsSet = params.entrySet();
            for (Map.Entry<String, String> param : paramsSet) {
                pairList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }
            httpRequest.setEntity(new UrlEncodedFormEntity(pairList, DEFAULT_CHARSET));
            response = client.execute(httpRequest);
            int stateCode = response.getStatusLine().getStatusCode();
            byte[] resultByte = EntityUtils.toByteArray(response.getEntity());
            String resultStr = new String(resultByte, DEFAULT_CHARSET);
            log.debug("url:{}, headerParams:{}, params:{}, stateCode:{}, response:{}", url, headerParams, params,
                    stateCode,
                    resultStr);
            return resultStr;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        } finally {
            if (response != null) {
                try {
                    response.getEntity().getContent().close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static Map<String, String> toParamMap(Object bean) throws IllegalAccessException {
        Map<String, String> ret = new HashMap<>();
        Class clazz = bean.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            JSONField jsonField = field.getDeclaredAnnotation(JSONField.class);
            String key = null;
            if (jsonField != null) {
                key = jsonField.name();
            }
            if (StringUtils.isEmpty(key)) {
                key = field.getName();
            }
            ret.put(key, String.valueOf(field.get(bean)));
        }
        return ret;
    }

}
