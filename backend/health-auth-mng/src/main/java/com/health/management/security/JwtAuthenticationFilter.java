package com.health.management.security;

import com.health.management.entity.SysUser;
import com.health.management.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * JWT 认证过滤器：
 * 1. 只接受 type=access 的 Access Token；
 * 2. 检查 Redis 黑名单；
 * 3. 每请求校验用户状态和 token_version，禁用/改密/改角色后旧 token 立即失效。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenUtil.parse(token);
                // Refresh Token 不能用于访问业务接口
                if (JwtTokenUtil.TYPE_ACCESS.equals(jwtTokenUtil.getTokenType(claims))) {
                    String jti = jwtTokenUtil.getJti(claims);
                    if (!tokenSessionService.isAccessTokenBlacklisted(jti)) {
                        Long userId = jwtTokenUtil.getUserId(claims);
                        Integer tokenVersion = jwtTokenUtil.getTokenVersion(claims);
                        if (userId != null && tokenVersion != null) {
                            SysUser state = userMapper.selectAuthStateById(userId);
                            if (state != null
                                    && Integer.valueOf(1).equals(state.getStatus())
                                    && state.getTokenVersion() != null
                                    && tokenVersion.equals(state.getTokenVersion())) {
                                LoginUser loginUser = new LoginUser();
                                loginUser.setUserId(userId);
                                loginUser.setUsername(jwtTokenUtil.getUsername(claims));
                                loginUser.setRoles(jwtTokenUtil.getRoles(claims));
                                loginUser.setRoleIds(jwtTokenUtil.getRoleIds(claims));
                                loginUser.setPermissions(jwtTokenUtil.getPerms(claims));
                                loginUser.setSid(jwtTokenUtil.getSid(claims));
                                loginUser.setJti(jti);
                                Date expiration = jwtTokenUtil.getExpiration(claims);
                                loginUser.setTokenExpireAt(expiration == null ? null : expiration.getTime());
                                loginUser.buildAuthorities();

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // token 非法/过期/Redis 不可用/版本不匹配：保持匿名，由 AuthenticationEntryPoint 返回 401
            }
        }
        chain.doFilter(request, response);
    }
}
