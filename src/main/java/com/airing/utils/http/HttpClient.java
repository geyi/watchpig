package com.airing.utils.http;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;

import java.util.concurrent.TimeUnit;

public class HttpClient {
	private static final PoolingHttpClientConnectionManager CONN_MRG = new PoolingHttpClientConnectionManager();
	private static final ConnectionMonitorThread CONNECTION_MONITOR_THREAD = new ConnectionMonitorThread(CONN_MRG);
	private static final ConnectionKeepAliveStrategy KEEP_ALIVE_STRATEGY = (response, context) -> {
		HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
		while (it.hasNext()) {
			HeaderElement he = it.nextElement();
			String param = he.getName();
			String value = he.getValue();
			if (value != null && "timeout".equalsIgnoreCase(param)) {
				try {
					return Long.parseLong(value) * 1000;
				} catch (NumberFormatException ignore) {
					throw ignore;
				}
			}
		}

		return 30000;
	};
	private static final HttpClientBuilder builder = HttpClients.custom();

	private int maxThreadSize;
	private int maxRouteSize;
	private int defaultRequestTimeout;
	private int defaultSocketTimeout ;
	private HttpResponseInterceptor responseInterceptor;
	private HttpRequestInterceptor requestInterceptor;

	public HttpClient() {
		this(Integer.getInteger("httpclient.max.thread.size", 8),
				Integer.getInteger("httpclient.max.route.size", 8));
	}

	public HttpClient(int maxThreadSize, int maxRouteSize) {
		this.maxRouteSize = maxRouteSize;
		this.maxThreadSize = maxThreadSize;
		this.defaultRequestTimeout = -1;
		this.defaultSocketTimeout = -1;
		this.init();
	}

	private void init() {
		CONN_MRG.setMaxTotal(maxThreadSize);
		CONN_MRG.setDefaultMaxPerRoute(maxRouteSize);

		if (!CONNECTION_MONITOR_THREAD.isAlive()) {
			CONNECTION_MONITOR_THREAD.start();
		}

		builder.setConnectionManager(CONN_MRG);
		builder.setKeepAliveStrategy(KEEP_ALIVE_STRATEGY);
		if (requestInterceptor != null) {
			builder.addInterceptorFirst(requestInterceptor);
		}
		if (responseInterceptor != null) {
			builder.addInterceptorLast(responseInterceptor);
		}
	}

	public CloseableHttpClient getHttpClient() {
		return getHttpClient(defaultRequestTimeout, defaultSocketTimeout);
	}
	
	public CloseableHttpClient getHttpClient(int requestTimeout, int socketTimeout) {
		return getHttpClient(requestTimeout, socketTimeout, 5000);
	}
	
	public CloseableHttpClient getHttpClient(int requestTimeout, int socketTimeout, int connectTimeout) {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(requestTimeout)
				.setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout)
				.build();
		builder.setDefaultRequestConfig(requestConfig);
		return builder.build();
	}

	static class ConnectionMonitorThread extends Thread {
		private final HttpClientConnectionManager connMgr;
		private volatile boolean shutdown;

		public ConnectionMonitorThread(HttpClientConnectionManager connMgr) {
			super();
			this.connMgr = connMgr;
		}

		@Override
		public void run() {
			try {
				while (!shutdown) {
					synchronized (this) {
						wait(5000);
						// ????????????????????????
						connMgr.closeExpiredConnections();
						// ????????????????????????30?????????
						connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
					}
				}
			} catch (InterruptedException ex) {
				// terminate
			}
		}

		public void shutdown() {
			shutdown = true;
			synchronized (this) {
				notifyAll();
			}
		}

	}

	public void setResponseInterceptor(
			HttpResponseInterceptor responseInterceptor) {
		this.responseInterceptor = responseInterceptor;
	}

	public void setRequestInterceptor(HttpRequestInterceptor requestInterceptor) {
		this.requestInterceptor = requestInterceptor;
	}
}
