package com.kh.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import com.kh.dto.BoardMemberDTO;
import com.kh.service.BoardMemberService;
import com.kh.token.JwtTokenProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MemberController {
	private final BoardMemberService memberService;
	private final JwtTokenProvider tokenProvider;
	
	public MemberController(BoardMemberService memberService, JwtTokenProvider tokenProvider) {
		this.memberService = memberService;
		this.tokenProvider = tokenProvider;
	}
	
	@PostMapping("/member/login")
	public Map<String, Object> login(@RequestBody Map<String, String> map) {
		Map<String, Object> result = new HashMap<>(); 
		
		String id = map.get("id");
		String passwd = map.get("passwd");
		
		BoardMemberDTO member = memberService.login(id, passwd);
		boolean flag = false;
		
		if(member != null) {
			String token = tokenProvider.generateJwtToken(member);
			flag = true;
			result.put("token", token);
		}
		result.put("flag", flag);
		
		return result;
	}
	
	
}










