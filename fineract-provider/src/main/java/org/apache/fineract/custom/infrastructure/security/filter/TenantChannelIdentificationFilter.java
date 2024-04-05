package org.apache.fineract.custom.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Slf4j
@Service(value = "tenantChannelIdentificationFilter")
public class TenantChannelIdentificationFilter extends GenericFilterBean {

    private final String tenantRequestChannel = "Fineract-Request-Channel";

    private static final String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR" };

    @Override
    public void doFilter(ServletRequest req, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("ignore to allow 'preflight' requests from AJAX applications in different origin (domain name)");
        } else {

            final String requestChannel = request.getHeader(tenantRequestChannel);
            if (StringUtils.isEmpty(requestChannel)) {
                ThreadLocalContextUtil.clearRequestChannelContext();
            } else {
                ThreadLocalContextUtil.setRequestChannelContext(requestChannel);
            }


            final String clientIP = getClientIpAddress(request);
            if (StringUtils.isEmpty(clientIP)) {
                ThreadLocalContextUtil.clearRequestIPContext();
            } else {
                ThreadLocalContextUtil.setRequestIPContext(clientIP);
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        for (String header : HEADERS_TO_TRY) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

}

