package com.kh.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.kh.dto.BoardMemberDTO;

@Mapper
public interface BoardMemberMapper {
	//Mapper에서 id 속성 값과 추상메서드 명으로 실행할 SQL문 찾아서 실행
	List<BoardMemberDTO> selectAllMember();
	int insertMember(BoardMemberDTO dto);
	List<BoardMemberDTO> selectNameMember(String name);
	int deleteMember(String id);
	int selectIdMember(String id);
	BoardMemberDTO findMemberByIdAndPassword(Map<String, String> map);
  int updateMember(Map<String, String> map);
  int updateColumnMember(Map<String, String> map);

}
