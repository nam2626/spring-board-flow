package com.kh.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.dto.BoardCommentDTO;
import com.kh.dto.BoardDTO;
import com.kh.dto.BoardFileDTO;
import com.kh.dto.BoardMemberDTO;
import com.kh.service.BoardService;
import com.kh.token.JwtTokenProvider;
import com.kh.vo.PaggingVO;

import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BoardController {

	private BoardService boardService;
	private JwtTokenProvider tokenProvider;

	public BoardController(BoardService boardService, JwtTokenProvider tokenProvider) {
		this.boardService = boardService;
		this.tokenProvider = tokenProvider;
	}

	@GetMapping("/board/list")
	public Map<String, Object> index(@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "30") int pageContentEa) {
		System.out.println(pageNo);
		// 전체 게시글 개수 조회
		int count = boardService.selectBoardTotalCount();
		// 페이지 번호를 보내서 해당 페이지 게시글 목록만 조회
		List<BoardDTO> list = boardService.getBoardList(pageNo, pageContentEa);
		// PaggingVO 페이징 정보 생성
		PaggingVO pagging = new PaggingVO(count, pageNo, pageContentEa);
		// 데이터 추가
		Map<String, Object> map = new HashMap<>();
		map.put("boardList", list);
		map.put("pagging", pagging);

		return map;
	}

	@GetMapping("/board/{bno}")
	public Map<String, Object> boardDetail(@PathVariable int bno) {
		Map<String, Object> map = new HashMap<>();
		BoardDTO board = boardService.selectBoard(bno);
		List<BoardCommentDTO> commentList = boardService.getCommentList(bno, 1);
		List<BoardFileDTO> fileList = boardService.getBoardFileList(bno);

		map.put("board", board);
		map.put("commentList", commentList);
		map.put("fileList", fileList);

		return map;
	}

//	@PostMapping("/board/write")
//	public Map<String, Object> boardWrite(
//			@RequestHeader("Authorization") String token,
//			@RequestPart("title") String title,
//			@RequestPart("content") String content){
//		Map<String, Object> map = new HashMap<>();
//		token = token != null ? token.replace("Bearer ", "") : null;
//		System.out.println(token);
//		System.out.println(tokenProvider.getUserIDFromToken(token));
//		System.out.println(title);
//		System.out.println(content);
//		
//		map.put("msg", "테스트 메세지");
//		return map;		
//	}
	@PostMapping("/board/write")
	public Map<String, Object> boardWrite(@RequestHeader("Authorization") String token,
			@RequestPart("params") String params, @RequestPart(value = "file", required = false) MultipartFile[] files)
			throws IllegalStateException, IOException {
		Map<String, Object> map = new HashMap<>();
		token = token != null ? token.replace("Bearer ", "") : null;
//		System.out.println(token);
//		System.out.println(tokenProvider.getUserIDFromToken(token));
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> paramsMap = objectMapper.readValue(params, new TypeReference<Map<String, String>>() {
		});
		System.out.println(paramsMap.get("title"));
		System.out.println(paramsMap.get("content"));

		BoardDTO board = new BoardDTO();
		try {
			board.setId(tokenProvider.getUserIDFromToken(token));
		} catch (Exception e) {
			map.put("msg", "로그인 하셔야 이용하실수 있습니다.");
			map.put("code", 2);
			return map;
		}
		board.setTitle(paramsMap.get("title"));
		board.setContent(paramsMap.get("content"));

		int bno = boardService.selectBoardNo();
		board.setBno(bno);

		// 4. 파일 업로드
		List<BoardFileDTO> fileList = new ArrayList<>();
		File root = new File("C:\\fileupload");
		// 해당 경로가 있는지 체크, 없으면 해당 경로를 생성
		if (!root.exists()) {
			root.mkdirs();
		}
		if (files != null) {
			for (MultipartFile file : files) {
				// 파일 업로드 전에 검사
				if (file.isEmpty()) {
					continue;
				}
				// 업로드한 파일명
				String fileName = file.getOriginalFilename();
				// 파일 저장할 경로 완성
				String filePath = root + File.separator + fileName;
				// 실제 파일 저장 부분
				file.transferTo(new File(filePath));
				BoardFileDTO fileDTO = new BoardFileDTO();
				fileDTO.setBno(bno);
				fileDTO.setFpath(filePath);
				fileList.add(fileDTO);
			}
		}

		// 5. 게시글 데이터베이스에 추가
		int count = boardService.insertBoard(board, fileList);
		if (count != 0) {
			map.put("bno", bno);
			map.put("code", 1);
			map.put("msg", "게시글 쓰기 성공");
		} else {
			map.put("code", 2);
			map.put("msg", "게시글 쓰기 실패");
		}
		return map;
	}

	@GetMapping("/board/download/{fno}")
	public ResponseEntity<Resource> fileDownload(@PathVariable int fno, HttpServletResponse response) throws IOException {
		// 1. 파일 정보 DB로부터 읽어옴
		String filePath = boardService.selectFilePath(fno);
		// 2. 스트림으로 파일 연결해서, 클라이언트에게 전송
		File file = new File(filePath);
		String encodingFileName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);
		Resource resource = new FileSystemResource(file);
		String contentDisposition 
				= "attachement;filename=" + encodingFileName;
		
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
				.body(resource);
		
		/*
		//초기 파일 다운로드
		response.setHeader("Content-Disposition", 
				"attachement;filename=" + encodingFileName);
		response.setHeader("Content-Transfer-Encoding", "binary");
		response.setContentLength((int) file.length());

		try (FileInputStream fis = new FileInputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())) {

			byte[] buffer = new byte[1024 * 1024];

			while (true) {
				int count = fis.read(buffer);
				if (count == -1) {
					break;
				}
				bos.write(buffer, 0, count);
				bos.flush();
			}

		}
		*/
	}

	@GetMapping("/board/comment/{bno}")
	public List<BoardCommentDTO> getMethodName(@PathVariable int bno, @RequestParam int start) {
		List<BoardCommentDTO> commentList = boardService.getCommentList(bno, start);
		return commentList;
	}

	@GetMapping("/board/like/{bno}")
	public Map<String, Object> boardLike(@PathVariable int bno, @RequestHeader("Authorization") String token) {
		Map<String, Object> map = new HashMap<String, Object>();
		token = token != null ? token.replace("Bearer ", "") : null;
		if (token == null) {
			map.put("code", 2);
			map.put("msg", "로그인 하셔야 이용하실수 있습니다.");
		} else {
			String id = tokenProvider.getUserIDFromToken(token);
			try {
				boardService.insertBoardLike(bno, id);
				map.put("code", 1);
				map.put("msg", "해당 게시글에 좋아요 하셨습니다.");
			} catch (Exception e) {
				boardService.deleteBoardLike(bno, id);
				map.put("code", 1);
				map.put("msg", "해당 게시글에 좋아요를 취소 하셨습니다.");
			}
			map.put("count", boardService.getBoardLike(bno));
		}
		return map;
	}

	@GetMapping("/board/hate/{bno}")
	public Map<String, Object> boardHate(@PathVariable int bno, @RequestHeader("Authorization") String token) {
		Map<String, Object> map = new HashMap<String, Object>();
		token = token != null ? token.replace("Bearer ", "") : null;
		if (token == null) {
			map.put("code", 2);
			map.put("msg", "로그인 하셔야 이용하실수 있습니다.");
		} else {
			String id = tokenProvider.getUserIDFromToken(token);
			try {
				boardService.insertBoardHate(bno, id);
				map.put("code", 1);
				map.put("msg", "해당 게시글에 싫어요 하셨습니다.");
			} catch (Exception e) {
				boardService.deleteBoardHate(bno, id);
				map.put("code", 1);
				map.put("msg", "해당 게시글에 싫어요를 취소 하셨습니다.");
			}
			map.put("count", boardService.getBoardHate(bno));
		}
		return map;
	}

	@GetMapping("/board/comment/like/{cno}")
	public Map<String, Object> boardCommentLike(@PathVariable int cno, @RequestHeader("Authorization") String token) {
		Map<String, Object> map = new HashMap<String, Object>();

		token = token != null ? token.replace("Bearer ", "") : null;
		if (token == null) {
			map.put("code", 2);
			map.put("msg", "로그인 하셔야 이용하실수 있습니다.");
		} else {
			String id = tokenProvider.getUserIDFromToken(token);
			try {
				boardService.insertBoardCommentLike(cno, id);
				map.put("code", 1);
				map.put("msg", "해당 댓글에 좋아요 하셨습니다.");
			} catch (Exception e) {
				boardService.deleteBoardCommentLike(cno, id);
				map.put("code", 1);
				map.put("msg", "해당 댓글에 좋아요를 취소 하셨습니다.");
			}
			map.put("count", boardService.selectCommentLikeCount(cno));
		}
		return map;
	}

	@GetMapping("/board/comment/hate/{cno}")
	public Map<String, Object> boardCommentHate(@PathVariable int cno, @RequestHeader("Authorization") String token) {
		Map<String, Object> map = new HashMap<String, Object>();

		token = token != null ? token.replace("Bearer ", "") : null;
		if (token == null) {
			map.put("code", 2);
			map.put("msg", "로그인 하셔야 이용하실수 있습니다.");
		} else {
			String id = tokenProvider.getUserIDFromToken(token);
			try {
				boardService.insertBoardCommentHate(cno, id);
				map.put("code", 1);
				map.put("msg", "해당 댓글에 싫어요 하셨습니다.");
			} catch (Exception e) {
				boardService.deleteBoardCommentHate(cno, id);
				map.put("code", 1);
				map.put("msg", "해당 댓글에 싫어요를 취소 하셨습니다.");
			}
			map.put("count", boardService.selectCommentHateCount(cno));
		}
		return map;
	}

	@PostMapping("/board/comment")
	public Map<String, Object> boardCommentWrite(@RequestBody Map<String, Object> map,
			@RequestHeader("Authorization") String token) {
		Map<String, Object> result = new HashMap<String, Object>();

		token = token != null ? token.replace("Bearer ", "") : null;
		if (token == null) {
			result.put("code", 2);
			result.put("msg", "로그인 하셔야 이용하실수 있습니다.");
		} else {
			String id = tokenProvider.getUserIDFromToken(token);
			BoardCommentDTO comment = new BoardCommentDTO(Integer.parseInt(map.get("bno").toString()), id,
					map.get("content").toString());
			boardService.insertBoardComment(comment);
			result.put("code", 1);
			result.put("msg", "댓글 추가 완료");
			result.put("commentList", boardService.getCommentList(comment.getBno(), 1));
		}
		return result;
	}

	@DeleteMapping("/board/{bno}")
	public Map<String, Object> boardDelete(@PathVariable int bno, @RequestHeader("Authorization") String token) {
		Map<String, Object> map = new HashMap<>();
		token = token != null ? token.replace("Bearer ", "") : null;
		if (token != null && tokenProvider.getUserIDFromToken(token).equals(boardService.selectBoard(bno).getId())) {
			// 첨부파일 삭제
			// 1. 파일 목록 받아옴
			List<BoardFileDTO> fileList = boardService.getBoardFileList(bno);
			// 2. 파일 삭제
			fileList.forEach(file -> {
				File f = new File(file.getFpath());
				f.delete();
			});
			// 만약 board, board_file 테이블이 외래키로 cascade 제약조건이 설정되어있지 않다면, 직접 board_file 테이블의
			// 데이터를 삭제해야함.
			boardService.deleteBoard(bno);
			map.put("code", 1);
			map.put("msg", "해당 게시글 삭제를 완료하였습니다.");
		} else {
			map.put("code", 2);
			map.put("msg", "게시글 삭제를 실패하였습니다.");
		}
		return map;
	}

	@PutMapping("/board/comment")
	public Map<String, Object> boardCommentUpdate(@RequestBody Map<String, String> body,
			@RequestHeader("Authorization") String token) {
		Map<String, Object> map = new HashMap<String, Object>();
		BoardCommentDTO comment = boardService.selectComment(Integer.parseInt(body.get("cno")));
		token = token != null ? token.replace("Bearer ", "") : null;

		if (token != null && tokenProvider.getUserIDFromToken(token).equals(comment.getId())) {
			comment.setContent(body.get("content"));
			boardService.updateBoardComment(comment);
			map.put("code", 1);
			map.put("msg", "해당 댓글 수정 완료");
			map.put("commentList", boardService.getCommentList(comment.getBno(), 1));
		} else {
			map.put("code", 2);
			map.put("msg", "해당 댓글 작성자만 수정이 가능합니다.");
		}
		return map;
	}

	@DeleteMapping("/board/comment/{cno}")
	public Map<String, Object> boardCommentDelete(@PathVariable int cno, @RequestHeader("Authorization") String token) {
		Map<String, Object> map = new HashMap<String, Object>();
		BoardCommentDTO comment = boardService.selectComment(cno);
		token = token != null ? token.replace("Bearer ", "") : null;

		if (token != null && tokenProvider.getUserIDFromToken(token).equals(comment.getId())) {
			boardService.deleteBoardComment(cno);
			map.put("code", 1);
			map.put("msg", "해당 댓글 삭제 완료");
			map.put("commentList", boardService.getCommentList(comment.getBno(), 1));
		} else {
			map.put("code", 2);
			map.put("msg", "해당 댓글 작성자만 삭제가 가능합니다.");
		}
		return map;
	}
}
