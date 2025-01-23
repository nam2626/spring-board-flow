package com.kh.token;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.kh.dto.BoardMemberDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class JwtTokenProvider {
	//토근 유효시간 설정
	private final long expiredTime = 1000L * 60L * 60L * 1L; // 1시간의 유효시간 설정
	private SecretKey key = Jwts.SIG.HS256.key().build();
	// Key key = Keys.hmacShaKeyFor("256비트 이상인 문자열".getBytes(StandardCharsets.UTF_8));
	
	public String generateJwtToken(BoardMemberDTO member) {
		Date expire = new Date(Calendar.getInstance().getTimeInMillis() + expiredTime);
		
		return Jwts.builder().header().add(createHeader()).and()
				.setExpiration(expire).setClaims(createClaims(member))
				.subject(member.getId()).signWith(key).compact();
	}
	
	//토큰에 저장된 로그인 id값 꺼내서 반환
	public String getUserIDFromToken(String token) {
		return (String) getClaims(token).getSubject();
	}
	//토큰에 저장된 권한에 관련된 값 꺼내서 반환
	public String getRoleFromToken(String token) {
		return (String) getClaims(token).get("roles");
	}
	
	private Claims getClaims(String token) {
		return Jwts.parser().verifyWith(key).build().parseClaimsJws(token).getBody();
	}
	
	private Map<String, Object> createClaims(BoardMemberDTO member) {
		Map<String, Object> map = new HashMap<>();
		map.put("roles", member.getGrade() == 5 ? "Admin" : "User");
		return map;
	}
	private Map<String, Object> createHeader() {
		Map<String, Object> map = new HashMap<>();
		map.put("typ", "JWT");//토큰 종류
		map.put("alg", "HS256");//암호화에 사용할 알고리즘
		map.put("regDate", System.currentTimeMillis());//생성시간
		return map;
	}
}










