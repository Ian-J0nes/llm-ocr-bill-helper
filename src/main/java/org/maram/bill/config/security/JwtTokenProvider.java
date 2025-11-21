package org.maram.bill.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.maram.bill.config.properties.JwtProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT（JSON Web Token）工具类，负责生成、解析和验证令牌。
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private long jwtExpirationInMs;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey jwtSecretKey;
    private final MacAlgorithm signatureAlgorithm = Jwts.SIG.HS512;

    @PostConstruct
    public void init() {
        byte[] secretBytes = jwtProperties.getSecret().getBytes();
        this.jwtSecretKey = Keys.hmacShaKeyFor(secretBytes);
        this.jwtExpirationInMs = jwtProperties.getExpiration().getMs();
    }

    /**
     * 从JWT中提取单个声明。
     * @param token JWT
     * @param claimsResolver 一个函数，用于从Claims中提取所需的数据
     * @return 声明的值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从JWT中提取所有声明。
     * @param token JWT
     * @return Claims对象
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 根据UserDetails生成一个新的JWT。
     * openid将作为JWT的subject。
     * @param userDetails Spring Security的用户信息
     * @return 生成的JWT字符串
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // 你可以在这里添加额外的claims，例如角色
        // claims.put("roles", userDetails.getAuthorities().stream()...);
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject) // 使用 openid 作为 subject
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecretKey, signatureAlgorithm)
                .compact();
    }

    /**
     * 从JWT中获取openid（即subject）。
     * @param token JWT
     * @return openid
     */
    public String getOpenidFromJWT(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 验证令牌是否有效。
     * 检查令牌中的用户名是否与UserDetails中的用户名匹配，并且令牌没有过期。
     * @param token 要验证的JWT。
     * @param userDetails 从数据库加载的用户信息。
     * @return 如果令牌有效则返回 true，否则返回 false。
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String openid = getOpenidFromJWT(token);
        return (openid.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
