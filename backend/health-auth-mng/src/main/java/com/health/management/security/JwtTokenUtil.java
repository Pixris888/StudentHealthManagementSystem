package com.health.management.security;

import com.health.management.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类：
 * - Access Token：短期，携带角色/权限，用于接口认证；
 * - Refresh Token：长期，只携带用户和会话信息，用于换取新的 Access Token。
 */
@Component
public class JwtTokenUtil {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Autowired
    private JwtProperties props;

    private SecretKey key;

    private SecretKey getKey() {
        if (key == null) {
            byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("jwt.secret 长度至少需要32字节");
            }
            key = Keys.hmacShaKeyFor(bytes);
        }
        return key;
    }

    public String createAccessToken(String username, Long userId, List<String> roles, List<Long> roleIds,
                                    List<String> perms, Integer tokenVersion, String sid, String jti) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setId(jti)
                .claim("uid", userId)
                .claim("roles", roles)
                .claim("rids", roleIds)
                .claim("perms", perms)
                .claim("ver", tokenVersion == null ? 0 : tokenVersion)
                .claim("type", TYPE_ACCESS)
                .claim("sid", sid)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + props.getAccessExpiration()))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String username, Long userId, Integer tokenVersion, String sid) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setId(sid)
                .claim("uid", userId)
                .claim("ver", tokenVersion == null ? 0 : tokenVersion)
                .claim("type", TYPE_REFRESH)
                .claim("sid", sid)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + props.getRefreshExpiration()))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build()
                .parseClaimsJws(token).getBody();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    public Long getUserId(Claims claims) {
        return toLong(claims.get("uid"));
    }

    public String getTokenType(Claims claims) {
        Object value = claims.get("type");
        return value == null ? null : value.toString();
    }

    public String getSid(Claims claims) {
        Object value = claims.get("sid");
        return value == null ? null : value.toString();
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public Date getExpiration(Claims claims) {
        return claims.getExpiration();
    }

    public List<String> getRoles(Claims claims) {
        return toStringList(claims.get("roles"));
    }

    public List<Long> getRoleIds(Claims claims) {
        List<Long> result = new ArrayList<>();
        Object value = claims.get("rids");
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                Long id = toLong(item);
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    public List<String> getPerms(Claims claims) {
        return toStringList(claims.get("perms"));
    }

    /**
     * @return token 中的版本号；旧 token 没有 ver 字段时返回 null
     */
    public Integer getTokenVersion(Claims claims) {
        Object value = claims.get("ver");
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> toStringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
