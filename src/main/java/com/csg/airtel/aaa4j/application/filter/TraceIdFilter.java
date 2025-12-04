package com.csg.airtel.aaa4j.application.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class TraceIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final String TRACE_ID_HEADER = "Trace-Id";
	private static final String TRACE_ID_MDC_KEY = "traceId";
	private static final String USER_NAME_HEADER = "User-Name";
	private static final String USER_NAME_MDC_KEY = "userName";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String traceId = requestContext.getHeaderString(TRACE_ID_HEADER);
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString();
		}
		MDC.put(TRACE_ID_MDC_KEY, traceId);

		String userName = requestContext.getHeaderString(USER_NAME_HEADER);
		if (userName != null && !userName.isBlank()) {
			MDC.put(USER_NAME_MDC_KEY, userName);
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		Object traceIdObj = MDC.get(TRACE_ID_MDC_KEY);
		if (traceIdObj != null) {
			String traceId = traceIdObj.toString();
			responseContext.getHeaders().putSingle(TRACE_ID_HEADER, traceId);
		}

		Object userNameObj = MDC.get(USER_NAME_MDC_KEY);
		if (userNameObj != null) {
			responseContext.getHeaders().putSingle(USER_NAME_HEADER, userNameObj.toString());
		}
		MDC.remove(TRACE_ID_MDC_KEY);
		MDC.remove(USER_NAME_MDC_KEY);
	}
}


