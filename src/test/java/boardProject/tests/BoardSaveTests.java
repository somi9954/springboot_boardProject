package boardProject.tests;


import boardProject.commons.configs.ConfigSaveService;
import boardProject.controllers.admins.ConfigForm;
import boardProject.controllers.boards.BoardForm;
import boardProject.controllers.members.JoinForm;
import boardProject.entities.board.Board;
import boardProject.models.board.BoardDataSaveService;
import boardProject.models.board.BoardValidationException;
import boardProject.models.board.config.BoardConfigInfoService;
import boardProject.models.board.config.BoardConfigSaveService;
import boardProject.models.member.MemberSaveService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.profiles.active=test")
@DisplayName("게시글 등록, 수정 테스트")
@Transactional
public class BoardSaveTests {

    @Autowired
    private BoardDataSaveService saveService;

    @Autowired
    private BoardConfigSaveService configSaveService;

    @Autowired
    private BoardConfigInfoService configInfoService;

    @Autowired
    private MemberSaveService memberSaveService;

    private Board board;

    private JoinForm joinForm;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConfigSaveService siteconfigSaveService;

    @BeforeEach
    @Transactional
    void init() {
        // 사이트 설정 등록
        siteconfigSaveService.save("siteConfig", new ConfigForm());

        // 게시판 설정 추가
        boardProject.controllers.admins.BoardForm boardForm = new boardProject.controllers.admins.BoardForm();
        boardForm.setBId("freetalk");
        boardForm.setBName("자유게시판");
        configSaveService.save(boardForm);
        board = configInfoService.get(boardForm.getBId(), true);

        // 회원 가입 추가
        joinForm = JoinForm.builder()
                .userId("user01")
                .userPw("aA!123456")
                .userPwRe("aA!123456")
                .email("user01@test.org")
                .userNm("사용자01")
                .mobile("01000000000")
                .agrees(new boolean[]{true})
                .build();
        memberSaveService.save(joinForm);
    }

    private BoardForm getGuestBoardForm() {

        BoardForm boardForm = getCommonBoardForm();

        boardForm.setGuestPw("12345678");

        return boardForm;
    }

    private BoardForm getCommonBoardForm() {
        return BoardForm.builder()
                .bId(board.getBId())
                .gid(UUID.randomUUID().toString())
                .writer(joinForm.getUserNm())
                .subject("제목!")
                .content("내용!")
                .category(board.getCategories() == null ? null : board.getCategories()[0])
                .build();
    }

    @Test
    @DisplayName("게시글 등록(비회원) 성공시 예외 없음")
    @WithAnonymousUser
    void registerGuestSuccessTest() {
        assertDoesNotThrow(() -> saveService.save(getGuestBoardForm()));
    }

    @Test
    @DisplayName("게시글 등록(회원) 성공시 예외 없음")
    @WithMockUser(username = "user01", password = "_aA123456")
    @Disabled
    void registerMemberSuccessTest() {
        assertDoesNotThrow(() -> saveService.save(getCommonBoardForm()));
    }

    // 공통(회원, 비회원) 유효성 검사 체크
    private void commonRequiredFieldsTest() {
        assertAll(
                // bId - null
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setBId(null);
                    saveService.save(boardForm);
                }),
                // bId - 공백
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setBId("    ");
                    saveService.save(boardForm);
                }),
                // gid - null
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setGid(null);
                    saveService.save(boardForm);
                }),
                // gid - 공백
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setGid("    ");
                    saveService.save(boardForm);
                }),
                // writer - null
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setWriter(null);
                    saveService.save(boardForm);
                }),
                // writer - 공백
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setWriter("      ");
                    saveService.save(boardForm);
                }),
                // subject - null
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setSubject(null);
                    saveService.save(boardForm);
                }),
                // subject - 공백
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setSubject("       ");
                    saveService.save(boardForm);
                }),
                // content - null
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setContent(null);
                    saveService.save(boardForm);
                }),
                // content - 공백
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getCommonBoardForm();
                    boardForm.setContent("     ");
                    saveService.save(boardForm);
                })
        );
    }
    @Test
    @DisplayName("필수 항목 검증(비회원) - bId, gid, writer, subject, content, guestPw(자리수는 6자리 이상), BoardValidationException 발생")
    @WithAnonymousUser
    void requiredFieldsGuestTest() {
        commonRequiredFieldsTest();

        assertAll(
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getGuestBoardForm();
                    boardForm.setGuestPw(null);
                    saveService.save(boardForm);
                }),
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getGuestBoardForm();
                    boardForm.setGuestPw("   ");
                    saveService.save(boardForm);
                }),
                () -> assertThrows(BoardValidationException.class, () -> {
                    BoardForm boardForm = getGuestBoardForm();
                    boardForm.setGuestPw("1234");
                    saveService.save(boardForm);
                })
        );
    }

    @Test
    @DisplayName("필수 항목 검증(회원) - bId, gid, writer, subject, content, BoardValidationException 발생")
    @WithMockUser(username = "user01", password = "_aA123456")
    void requiredFieldsMemberTest() {
        commonRequiredFieldsTest();
    }


    @Test
    @DisplayName("게시판 설정 저장 테스트 - 유효성 검사")
    void boardConfigTest() throws Exception {
        String body = mockMvc.perform(
                        post("/admin/board/save")
                                .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString(Charset.forName("UTF-8"));

        assertTrue(body.contains("게시판 ID"));
        assertTrue(body.contains("게시판명"));
    }
}