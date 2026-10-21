package com.example.workadmin.config;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.RequirePermission;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.service.AccessService;
import com.example.workadmin.service.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 校验请求头里的 token。
 * 未通过时返回 HTTP 200 + code 401，前端 axios 拦截器可直接拿到 msg。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "CURRENT_USER";

    private final TokenStore tokenStore;
    private final AccessService accessService;

    public AuthInterceptor(TokenStore tokenStore, AccessService accessService) {
        this.tokenStore = tokenStore;
        this.accessService = accessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("token");
        UserInfo userInfo = tokenStore.get(token);
        if (userInfo == null) {
            writeJson(response, 401, "未登录或登录已过期");
            return false;
        }
        // 远程 stu 账号只允许访问远程登录态和远程群聊；不能调用本地 H2 的用户/聊天接口。
        if ("REMOTE".equals(userInfo.getAuthSource())) {
            String path = request.getRequestURI();
            if (!path.startsWith("/api/remote-auth/") && !path.startsWith("/api/remote-chat/")) {
                writeJson(response, 403, "远程账号仅可访问远程群聊");
                return false;
            }
        }
        request.setAttribute(CURRENT_USER, userInfo);
        if (handler instanceof HandlerMethod method) {
            RequirePermission permission = method.getMethodAnnotation(RequirePermission.class);
            if (permission == null) {
                permission = method.getBeanType().getAnnotation(RequirePermission.class);
            }
            if (permission != null) {
                try {
                    accessService.require(userInfo.getUsername(), permission.value());
                } catch (BizException e) {
                    writeJson(response, e.getCode(), e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    private void writeJson(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
    }
}
