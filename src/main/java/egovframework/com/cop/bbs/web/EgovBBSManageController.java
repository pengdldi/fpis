package egovframework.com.cop.bbs.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springmodules.validation.commons.DefaultBeanValidator;

import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.service.EgovFileMngService;
import egovframework.com.cmm.service.EgovFileMngUtil;
import egovframework.com.cmm.service.FileVO;
import egovframework.com.cmm.util.EgovUserDetailsHelper;
import egovframework.com.cop.bbs.service.Board;
import egovframework.com.cop.bbs.service.BoardMaster;
import egovframework.com.cop.bbs.service.BoardMasterVO;
import egovframework.com.cop.bbs.service.BoardVO;
import egovframework.com.cop.bbs.service.EgovBBSAttributeManageService;
import egovframework.com.cop.bbs.service.EgovBBSCommentService;
import egovframework.com.cop.bbs.service.EgovBBSManageService;
import egovframework.com.cop.bbs.service.EgovBBSSatisfactionService;
import egovframework.com.cop.bbs.service.EgovBBSScrapService;
import egovframework.com.utl.sim.service.EgovFileScrty;
import egovframework.rte.fdl.property.EgovPropertyService;
import egovframework.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import fpis.common.service.FpisMenuVO;
import fpis.common.utils.FpisConstants;
import fpis.common.utils.FpisUtil;
import fpis.common.vo.SessionVO;

/**
 * 게시물 관리를 위한 컨트롤러 클래스
 * @author 공통서비스개발팀 이삼섭
 * @since 2009.06.01
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------       --------    ---------------------------
 *   2009.3.19  이삼섭          최초 생성
 *   2009.06.29    한성곤            2단계 기능 추가 (댓글관리, 만족도조사)
 *   2011.07.01 안민정             댓글, 스크랩, 만족도 조사 기능의 종속성 제거
 *   2011.8.26    정진오            IncludedInfo annotation 추가
 *   2011.09.07 서준식           유효 게시판 게시일 지나도 게시물이 조회되던 오류 수정
 *   2013.09.13  jhoh            첨부파일 삭제후 화면 표출 오류 수정
 * </pre>
 */
@Controller
public class EgovBBSManageController {

	@Resource(name = "EgovBBSManageService")
	private EgovBBSManageService bbsMngService;

	@Resource(name = "EgovBBSAttributeManageService")
	private EgovBBSAttributeManageService bbsAttrbService;

	@Resource(name = "EgovFileMngService")
	private EgovFileMngService fileMngService;

	@Resource(name = "EgovFileMngUtil")
	private EgovFileMngUtil fileUtil;

	@Resource(name = "propertiesService")
	protected EgovPropertyService propertyService;

	@Resource(name = "egovMessageSource")
	EgovMessageSource egovMessageSource;

	//---------------------------------
	// 2009.06.29 : 2단계 기능 추가
	// 2011.07.01 : 댓글, 스크랩, 만족도 조사 기능의 종속성 제거
	//---------------------------------
	@Autowired(required = false)
	private EgovBBSCommentService bbsCommentService;

	@Autowired(required = false)
	private EgovBBSSatisfactionService bbsSatisfactionService;

	@Autowired(required = false)
	private EgovBBSScrapService bbsScrapService;
	////-------------------------------

	@Autowired
	private DefaultBeanValidator beanValidator;

	//protected Logger log = Logger.getLogger(this.getClass());

	/**
	 * XSS 방지 처리.
	 *
	 * @param data
	 * @return
	 */
	protected String unscript(String data) {
		if (data == null || data.trim().equals("")) {
			return "";
		}

		String ret = data;

		ret = ret.replaceAll("<(S|s)(C|c)(R|r)(I|i)(P|p)(T|t)", "&lt;script");
		ret = ret.replaceAll("</(S|s)(C|c)(R|r)(I|i)(P|p)(T|t)", "&lt;/script");

		ret = ret.replaceAll("<(O|o)(B|b)(J|j)(E|e)(C|c)(T|t)", "&lt;object");
		ret = ret.replaceAll("</(O|o)(B|b)(J|j)(E|e)(C|c)(T|t)", "&lt;/object");

		ret = ret.replaceAll("<(A|a)(P|p)(P|p)(L|l)(E|e)(T|t)", "&lt;applet");
		ret = ret.replaceAll("</(A|a)(P|p)(P|p)(L|l)(E|e)(T|t)", "&lt;/applet");

		ret = ret.replaceAll("<(E|e)(M|m)(B|b)(E|e)(D|d)", "&lt;embed");
		ret = ret.replaceAll("</(E|e)(M|m)(B|b)(E|e)(D|d)", "&lt;embed");

		ret = ret.replaceAll("<(F|f)(O|o)(R|r)(M|m)", "&lt;form");
		ret = ret.replaceAll("</(F|f)(O|o)(R|r)(M|m)", "&lt;form");

		return ret;
	}

	/**
	 * 게시물에 대한 목록을 조회한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/cop/bbs/selectBoardList.do", method = RequestMethod.POST)
	public String selectBoardArticles(@ModelAttribute("searchVO") BoardVO boardVO, HttpServletRequest req, ModelMap model) throws Exception, NullPointerException {
		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();

		BoardMasterVO vo = new BoardMasterVO();
		HttpSession session = req.getSession();
		SessionVO SVO = (SessionVO) session.getAttribute("SessionVO");

		boardVO.setBbsIdList(bbsMngService.selectAllowedGnrBBSList());
		boolean flag = false;

		/* 웹취약점[불충분한인가] 수정. bbsid 변조 차단 - 2021.09.23 suhyun*/
		if (SVO != null) {
			if ("GNR".equals(SVO.getMber_cls()) && boardVO.getBbsId() != null) {
				for (int i = 0; i < boardVO.getBbsIdList().size(); i++) {
					if (boardVO.getBbsIdList().get(i).equals(boardVO.getBbsId())) {
						flag = true;
					}
				}
			} else if (!"GNR".equals(SVO.getMber_cls())) {
				flag = true;
			}
		} else {
			if (boardVO.getBbsId() != null) {
				for (int i = 0; i < boardVO.getBbsIdList().size(); i++) {
					if (boardVO.getBbsIdList().get(i).equals(boardVO.getBbsId())) {
						flag = true;
					}
				}
			}
		}

		/* 2022.08.23 jwchoi 웹취약점 조치 시스템 장애접수 페이지 접근차단 */
		if ("R5".equals(req.getParameter("rcode")) && "R5-01".equals(req.getParameter("bcode"))) {
			return "redirect:/userMain.do";
		}
		
		if (boardVO.getBbsId().isEmpty()) {
			flag = true;
		}

		if (!flag) {
			boardVO.setBbsId("BBSMSTR_000000000001");
		}

		if (boardVO.getBbsId() == null || boardVO.getBbsId() == "") {
			FpisMenuVO menuVO = new FpisMenuVO();
			menuVO.setRcode(req.getParameter("rcode"));
			menuVO.setBcode(req.getParameter("bcode"));

			boardVO.setBbsId(bbsMngService.selectBbsIdByRnBCode(menuVO));

			//내부망에서는 USB사용등이 불편하여 외부망에서 사용하기를 원한다는 공단 요청사항 때문에 메뉴에는 없고 bbsid로 따로
			if (menuVO.getRcode().equals("R15") && menuVO.getBcode().equals("R15-14") && boardVO.getBbsId() == null) {
				boardVO.setBbsId("BBSMSTR_000000000025");
			}
		}

		boardVO.setBbsId(boardVO.getBbsId().replaceAll("&apos;", ""));

		vo.setBbsId(boardVO.getBbsId());
		vo.setUniqId(user.getUniqId());

		BoardMasterVO master = bbsAttrbService.selectBBSMasterInf(vo);

		boardVO.setPageUnit(propertyService.getInt("pageUnit"));
		boardVO.setPageSize(propertyService.getInt("pageSize"));

		PaginationInfo paginationInfo = new PaginationInfo();

		paginationInfo.setCurrentPageNo(boardVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(boardVO.getPageUnit());
		paginationInfo.setPageSize(boardVO.getPageSize());

		boardVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		boardVO.setLastIndex(paginationInfo.getLastRecordIndex());
		boardVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		/*2020.10.20 국토부 홈페이지 취약점(크로스사이트스크립트) 조치*/
		String SearchWrd = boardVO.getSearchWrd();
		String SearchCnd = boardVO.getSearchCnd();

		boardVO.setSearchWrd(XSS_secure(SearchWrd));
		boardVO.setSearchCnd(XSS_secure(SearchCnd));

		Map<String, Object> map = bbsMngService.selectBoardArticles(boardVO, master.getBbsAttrbCode());//2011.09.07
		int totCnt = Integer.parseInt((String) map.get("resultCnt"));

		paginationInfo.setTotalRecordCount(totCnt);

		model.addAttribute("resultList", map.get("resultList"));
		model.addAttribute("resultCnt", map.get("resultCnt"));
		model.addAttribute("boardVO", boardVO);
		model.addAttribute("brdMstrVO", master);
		model.addAttribute("paginationInfo", paginationInfo);

		return "egovframework/com/cop/bbs/EgovNoticeList";
	}

	/**
	 * 게시물에 대한 상세 정보를 조회한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 * 2014.03.17 mgkim 상세보기시 관리자인경우 작성자 ID 표출기능 추가
	 * 2014.03.18 mgkim Q&A 상세보기 질문/답변 같이 보이게 기능 추가
	 * 2021.01.11 ysw 보안취약점 METHOD POST 만 가능하도록 수정
	 */
	@RequestMapping(value = "/cop/bbs/selectBoardArticle.do", method = RequestMethod.POST)
	public String selectBoardArticle(@ModelAttribute("searchVO") BoardVO boardVO,
			HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		SessionVO SVO = (SessionVO) req.getSession().getAttribute("SessionVO");

		//boardVO.setSearchWrd(FpisUtil.AntiCrossScripting_dec(boardVO.getSearchWrd()));    // XSS 방지
		//2020.11.20 웹취약점 조치
		boardVO.setSearchWrd(XSS_secure(boardVO.getSearchWrd()));
		boardVO.setSearchCnd(XSS_secure(boardVO.getSearchCnd()));

		//     조회수 증가 여부 지정
		boardVO.setPlusCount(true);

		//---------------------------------
		// 2009.06.29 : 2단계 기능 추가
		//---------------------------------
		if (!boardVO.getSubPageIndex().equals("")) {
			boardVO.setPlusCount(false);
		}
		////-------------------------------
		boardVO.setLastUpdusrId(user.getUniqId());

		BoardVO vo = bbsMngService.selectBoardArticle(boardVO);

		boardVO.setNttCn(XSS_secure(boardVO.getNttCn()));

		model.addAttribute("result", vo);
		
		/* 2022.08.23 jwchoi 웹취약점 조치 게시물 만료날짜 확인*/
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		
		String tmpDate = vo.getNtceEndde().replaceAll(" ", "");
		
		int curDate = Integer.parseInt(formatter.format(date));
		int endDate = Integer.parseInt(tmpDate);

		if (curDate - 1 >= endDate && "GNR".equals(SVO.getMber_cls())) {
			return "redirect:/userMain.do";
		}

		if (SVO != null) {
			if (!vo.getParnts().equals("0") && (boardVO.getBbsId().equals("BBSMSTR_000000000022") || boardVO.getBbsId().equals("BBSMSTR_000000000023")) && (SVO.getMber_cls().equals("ADM") || SVO.getMber_cls().equals("SYS") || SVO.getMber_cls().equals("SUP"))) {
				boardVO.setParnts(vo.getParnts());
				BoardVO voParnts = bbsMngService.selectBoardParnts(boardVO);
				model.addAttribute("voParnts", voParnts);
			}
		}

		if (bbsSatisfactionService != null) {
			if (bbsSatisfactionService.canUseSatisfaction(boardVO.getBbsId())) {
				model.addAttribute("useSatisfaction", "true");
			}
		}
		if (bbsScrapService != null) {
			if (bbsScrapService.canUseScrap()) {
				model.addAttribute("useScrap", "true");
			}
		}

		////--------------------------
		model.addAttribute("SessionVO", SVO);

		model.addAttribute("boardVO", boardVO); // 2014.04.02 mgkim 글보기 이후 검색파라메터 유지 오류 수정

		return "egovframework/com/cop/bbs/EgovNoticeInqire";
	}

	/**
	 * 게시물에 대한 답변 등록을 위한 등록페이지로 이동한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 * 2014.03.18 mgkim 답글 작성시 원본글 보기 기능 추가
	 */
	@RequestMapping("/cop/bbs/addReplyBoardArticle.do")
	public String addReplyBoardArticle(HttpServletRequest req, @ModelAttribute("searchVO") BoardVO boardVO, ModelMap model) throws Exception, NullPointerException {
		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");
		BoardMasterVO master = new BoardMasterVO();
		BoardMasterVO vo = new BoardMasterVO();
		vo.setBbsId(boardVO.getBbsId());
		vo.setUniqId(user.getUniqId());

		master = bbsAttrbService.selectBBSMasterInf(vo);
		boardVO.setBbsId(boardVO.getBbsId().replaceAll("&apos;", ""));

		/* qout로 잘못 저장된 데이터 보정 - 2021.11.09 suhyun */
		boardVO.setNttCn(boardVO.getNttCn().replaceAll("qout;", "quot;"));

		model.addAttribute("bdMstr", master);

		/* 2014.03.18 mgkim 답글 작성시 원본글 보기 기능 추가 시작 */
		boardVO.setLastUpdusrId(user.getUniqId());
		
		/* 2022.08.23 jwchoi 웹취약점 조치 일반사용자는 답글작성 안됨 */
		if ("GNR".equals(svo.getMber_cls()))
		{
			return "redirect:/userMain.do";
		} 
		
		boardVO = bbsMngService.selectBoardArticle(boardVO);
		/* 2014.03.18 mgkim 답글 작성시 원본글 보기 기능 추가 끝 */
		model.addAttribute("result", boardVO);

		//----------------------------
		// 기본 BBS template 지정
		//----------------------------
		if (master.getTmplatCours() == null || master.getTmplatCours().equals("")) {
			master.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
		}
		model.addAttribute("brdMstrVO", master);
		return "egovframework/com/cop/bbs/EgovNoticeReply";
	}

	/**
	 * 게시물 등록을 위한 등록페이지로 이동한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/addBoardArticle.do")
	public String addBoardArticle(@ModelAttribute("searchVO") BoardVO boardVO, ModelMap model, HttpServletRequest req) throws Exception, NullPointerException {
		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

		//2019.06.19 보안취약점 조치(불충분한 인증 및 인가)
		HttpSession session = req.getSession();
		SessionVO svo = (SessionVO) session.getAttribute("SessionVO");

		BoardMasterVO bdMstr = new BoardMasterVO();
		boardVO.setBbsId(boardVO.getBbsId().replaceAll("&apos;", ""));
		if (isAuthenticated) {

			BoardMasterVO vo = new BoardMasterVO();
			vo.setBbsId(boardVO.getBbsId());
			vo.setUniqId(user.getUniqId());

			bdMstr = bbsAttrbService.selectBBSMasterInf(vo);
			model.addAttribute("bdMstr", bdMstr);
		}

		//----------------------------
		// 기본 BBS template 지정
		//----------------------------

		if (bdMstr.getTmplatCours() == null || bdMstr.getTmplatCours().equals("")) {
			bdMstr.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
		}

		model.addAttribute("brdMstrVO", bdMstr);
		////-----------------------------

		if (!svo.getMber_cls().equals("SYS") &&
				(bdMstr.getBbsId().equals("BBSMSTR_000000000001") ||
						bdMstr.getBbsId().equals("BBSMSTR_000000000002") ||
						bdMstr.getBbsId().equals("BBSMSTR_000000000003"))) {
			model.addAttribute("brdMstrVO", bdMstr);
			////-----------------------------
			model.addAttribute("MSG", "noAuth");
			return "forward:/cop/bbs/selectBoardList.do";

		} else {
			return "egovframework/com/cop/bbs/EgovNoticeRegist";
		}
	}

	/**
	 * 게시물을 등록한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/insertBoardArticle.do")
	public String insertBoardArticle(final MultipartHttpServletRequest multiRequest,
			@ModelAttribute("searchVO") BoardVO boardVO,
			@ModelAttribute("bdMstr") BoardMaster bdMstr,
			@ModelAttribute("board") Board board,
			BindingResult bindingResult,
			SessionStatus status,
			HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		HttpSession session = req.getSession();

		/*2014.10.30 양상완 팝업 추가*/
		board.setPopYn(req.getParameter("pop_yn"));

		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		SessionVO SVO = (SessionVO) session.getAttribute("SessionVO");
		beanValidator.validate(board, bindingResult);

		if (bindingResult.hasErrors()) {

			BoardMasterVO master = new BoardMasterVO();
			BoardMasterVO vo = new BoardMasterVO();

			vo.setBbsId(boardVO.getBbsId());
			vo.setUniqId(user.getUniqId());

			master = bbsAttrbService.selectBBSMasterInf(vo);
			model.addAttribute("bdMstr", master);

			//----------------------------
			// 기본 BBS template 지정
			//----------------------------
			if (master.getTmplatCours() == null || master.getTmplatCours().equals("")) {
				master.setTmplatCours("css/egovframework/com/cop/tpl/egovBaseTemplate.css");
			}

			model.addAttribute("brdMstrVO", master);
			////-----------------------------
			model.addAttribute("MSG", "fail");

			return "redirect:/cop/bbs/selectBoardList.do";
		}

		if (isAuthenticated) {
			List<FileVO> result = null;
			String atchFileId = "";

			final Map<String, MultipartFile> files = multiRequest.getFileMap();
			Set<String> keyset = files.keySet();
			Iterator<String> iter = keyset.iterator();
			while (iter.hasNext()) {
				if (!FpisUtil.isValidFileExt(files.get(iter.next()).getOriginalFilename())) {
					model.addAttribute("MSG", "notPermit");
					return "forward:/cop/bbs/selectBoardList.do";
				}
			}

			if (!board.getCaptcha().equals(session.getAttribute(session.getId() + "captcha"))) {
				model.addAttribute("MSG", "notEqualCaptcha");
				return "forward:/cop/bbs/selectBoardList.do";
			}

			session.setAttribute(session.getId() + "captcha", "c_expired"); //1회성으로 사용하고 폐기

			if (!files.isEmpty()) {
				result = fileUtil.parseFileInf(files, "BBS_", 0, "", "");
				atchFileId = fileMngService.insertFileInfs(result);
			}

			board.setAtchFileId(atchFileId);
			board.setFrstRegisterId(user.getUniqId());
			board.setBbsId(board.getBbsId());

			//board.setNtcrNm("");    // dummy 오류 수정 (익명이 아닌 경우 validator 처리를 위해 dummy로 지정됨)
			board.setPassword(""); // dummy 오류 수정 (익명이 아닌 경우 validator 처리를 위해 dummy로 지정됨)

			if (SVO != null) {
				/* 웹취약점[불충분한인가] 수정. bbsid 변조 차단 - 2021.09.23 suhyun*/
				if ("GNR".equals(SVO.getMber_cls()) && !"BBSMSTR_000000000022".equals(board.getBbsId())) {
					model.addAttribute("MSG", "noAuth");
					return "forward:/cop/bbs/selectBoardList.do";
				}

				board.setNtcrId(SVO.getUser_id());
				//board.setNtcrNm(SVO.getName());
				if (SVO.getMber_cls().equals("ADM") || SVO.getMber_cls().equals("SYS") || SVO.getMber_cls().equals("SUP")) {
					board.setNtcrNm(SVO.getUser_name());
				} else if (SVO.getMber_cls().equals("GNR")) {
					board.setNtcrNm(SVO.getName());
				} else {
					board.setNtcrNm("미가입 사용자");
				}
				board.setFrstRegisterId(SVO.getUniqid()); // 2013.11.01 by jhoh : 사용자 UniqId

			} else {
				board.setNtcrId(user.getId()); //게시물 통계 집계를 위해 등록자 ID 저장
				board.setNtcrNm("미가입 사용자");
				//board.setNtcrNm(user.getName()); //게시물 통계 집계를 위해 등록자 Name 저장
				/* null만 오는곳임.. 주석 -2021.11.05 suhyun */
				/*if(SVO.getMber_cls().equals("ADM") || SVO.getMber_cls().equals("SYS") || SVO.getMber_cls().equals("SUP")) {
				    board.setNtcrNm(SVO.getUser_name());
				}else if(SVO.getMber_cls().equals("GNR")){
				    board.setNtcrNm(SVO.getName());
				}else{
				    board.setNtcrNm("미가입 사용자");
				}*/
			}
			//2020.05.28 pch XSS방지
			board.setNttCn(XSS_secure(board.getNttCn()));
			board.setNttSj(XSS_secure(board.getNttSj()));

			board.setNttCn(XSS_secure_Dec(board.getNttCn())); // XSS 방지
			board.setNttSj(XSS_secure_Dec(board.getNttSj())); // XSS 방지

			bbsMngService.insertBoardArticle(board);
		}

		//status.setComplete();
		return "forward:/cop/bbs/selectBoardList.do";
	}

	/**
	 * 게시물에 대한 답변을 등록한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/replyBoardArticle.do")
	public String replyBoardArticle(final MultipartHttpServletRequest multiRequest,
			@ModelAttribute("searchVO") BoardVO boardVO,
			@ModelAttribute("bdMstr") BoardMaster bdMstr,
			@ModelAttribute("board") Board board,
			BindingResult bindingResult,
			HttpServletRequest req,
			ModelMap model,
			SessionStatus status) throws Exception, NullPointerException {
		HttpSession session = req.getSession();

		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		SessionVO SVO = (SessionVO) req.getSession().getAttribute("SessionVO");

		beanValidator.validate(board, bindingResult);
		if (bindingResult.hasErrors()) {
			BoardMasterVO master = new BoardMasterVO();
			BoardMasterVO vo = new BoardMasterVO();

			vo.setBbsId(boardVO.getBbsId());
			vo.setUniqId(user.getUniqId());

			master = bbsAttrbService.selectBBSMasterInf(vo);

			model.addAttribute("bdMstr", master);
			model.addAttribute("result", boardVO);

			//----------------------------
			// 기본 BBS template 지정
			//----------------------------
			if (master.getTmplatCours() == null || master.getTmplatCours().equals("")) {
				master.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
			}

			model.addAttribute("brdMstrVO", master);
			////-----------------------------

			return "egovframework/com/cop/bbs/EgovNoticeReply";
		}

		if (isAuthenticated) {
			final Map<String, MultipartFile> files = multiRequest.getFileMap();

			Set<String> keyset = files.keySet();
			Iterator<String> iter = keyset.iterator();
			while (iter.hasNext()) {
				if (!FpisUtil.isValidFileExt(files.get(iter.next()).getOriginalFilename())) {
					model.addAttribute("MSG", "notPermit");
					return "forward:/cop/bbs/selectBoardList.do";
				}
			}

			if (!board.getCaptcha().equals(session.getAttribute(session.getId() + "captcha"))) {
				model.addAttribute("MSG", "notEqualCaptcha");
				return "forward:/cop/bbs/selectBoardList.do";
			}

			session.setAttribute(session.getId() + "captcha", "c_expired"); //1회성으로 사용하고 폐기

			String atchFileId = "";

			if (!files.isEmpty()) {
				List<FileVO> result = fileUtil.parseFileInf(files, "BBS_", 0, "", "");
				atchFileId = fileMngService.insertFileInfs(result);
			}

			board.setAtchFileId(atchFileId);
			board.setReplyAt("Y");
			board.setFrstRegisterId(user.getUniqId());
			board.setBbsId(board.getBbsId());
			board.setParnts(Long.toString(boardVO.getNttId()));
			board.setSortOrdr(boardVO.getSortOrdr());
			board.setReplyLc(Integer.toString(Integer.parseInt(boardVO.getReplyLc()) + 1));

			board.setNtcrNm(""); // dummy 오류 수정 (익명이 아닌 경우 validator 처리를 위해 dummy로 지정됨)
			board.setPassword(""); // dummy 오류 수정 (익명이 아닌 경우 validator 처리를 위해 dummy로 지정됨)

			if (SVO != null) {
				board.setNtcrId(SVO.getUser_id());
				//board.setNtcrNm(SVO.getName());
				if (SVO.getMber_cls().equals("ADM") || SVO.getMber_cls().equals("SYS") || SVO.getMber_cls().equals("SUP")) {
					board.setNtcrNm(SVO.getUser_name());
				} else if (SVO.getMber_cls().equals("GNR")) {
					board.setNtcrNm(SVO.getName());
				} else {
					board.setNtcrNm("미가입 사용자");
				}
			} else {
				board.setNtcrId(user.getId()); //게시물 통계 집계를 위해 등록자 ID 저장
				//board.setNtcrNm(user.getName()); //게시물 통계 집계를 위해 등록자 Name 저장
				board.setNtcrNm("미가입 사용자");
				/* null만 오는곳임.. 주석 -2021.11.05 suhyun */
				/*if(SVO.getMber_cls().equals("ADM") || SVO.getMber_cls().equals("SYS") || SVO.getMber_cls().equals("SUP")) {
				    board.setNtcrNm(SVO.getUser_name());
				}else if(SVO.getMber_cls().equals("GNR")){
				    board.setNtcrNm(SVO.getName());
				}else{
				    board.setNtcrNm("미가입 사용자");
				}*/
			}

			//board.setNttCn(FpisUtil.AntiCrossScripting_dec(board.getNttCn()));    // XSS 방지
			//board.setNttSj(FpisUtil.AntiCrossScripting_dec(board.getNttSj()));    // XSS 방지
			//board.setNttCn(unscript(board.getNttCn()));    // XSS 방지

			bbsMngService.insertBoardArticle(board);
		}

		return "forward:/cop/bbs/selectBoardList.do";
	}

	/**
	 * 게시물 수정을 위한 수정페이지로 이동한다.
	 *
	 * @param boardVO
	 * @param vo
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/forUpdateBoardArticle.do")
	public String selectBoardArticleForUpdt(@ModelAttribute("searchVO") BoardVO boardVO,
			@ModelAttribute("board") BoardVO vo,
			HttpServletRequest req, // 2013.09.12 by jhoh : 첨부파일 삭제 후 표출오류 수정
			ModelMap model)
			throws Exception, NullPointerException {
		//log.debug(this.getClass().getName()+"selectBoardArticleForUpdt getNttId "+boardVO.getNttId());
		//log.debug(this.getClass().getName()+"selectBoardArticleForUpdt getBbsId "+boardVO.getBbsId());
		// 첨부파일 삭제 후 redirect로 넘겨받으면서 필요한 변수-----------
		String BBSID = req.getParameter("BBSID");
		if (BBSID != null && !BBSID.equals("")) {
			boardVO.setBbsId(BBSID);
		}
		String NTTID = req.getParameter("NTTID");
		if (NTTID != null && !NTTID.equals("")) {
			//boardVO.setBbsId(BBSID);
			boardVO.setNttId(Integer.parseInt(NTTID));
		}
		// 2013.09.12 by jhoh : 첨부파일 삭제 후 표출오류 수정

		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

		//2019.06.19 보안취약점 조치(불충분한 인증 및 인가)
		HttpSession session = req.getSession();
		SessionVO svo = (SessionVO) session.getAttribute("SessionVO");

		boardVO.setFrstRegisterId(user.getUniqId());

		BoardMaster master = new BoardMaster();
		BoardMasterVO bmvo = new BoardMasterVO();
		BoardVO bdvo = new BoardVO();

		vo.setBbsId(boardVO.getBbsId());

		master.setBbsId(boardVO.getBbsId());
		master.setUniqId(user.getUniqId());

		if (isAuthenticated) {
			bmvo = bbsAttrbService.selectBBSMasterInf(master);
			bdvo = bbsMngService.selectBoardArticle(boardVO);
		}
		bdvo.setNttCn(bdvo.getNttCn().replaceAll("qout;", "quot;"));
		model.addAttribute("result", bdvo);
		model.addAttribute("bdMstr", bmvo);

		model.addAttribute("brdMstrVO", bmvo);

		//2020.06.04 보안취약점 조치(불충분한 인증 및 인가) - nttid변경으로 다른사용자의 게시물 수정할수있음
		if (!svo.getUser_id().equals(bdvo.getNtcrId())) {
			model.addAttribute("MSG", "noMatchUser");
			return "forward:/cop/bbs/selectBoardList.do";
		}
		////-----------------------------
		if (!svo.getMber_cls().equals("SYS") &&
				(bmvo.getBbsId().equals("BBSMSTR_000000000001") ||
						bmvo.getBbsId().equals("BBSMSTR_000000000002") ||
						bmvo.getBbsId().equals("BBSMSTR_000000000003"))) {
			model.addAttribute("MSG", "noAuth");
			return "forward:/cop/bbs/selectBoardList.do";
		} else {
			return "egovframework/com/cop/bbs/EgovNoticeUpdt";
		}
	}

	/**
	 * 게시물에 대한 내용을 수정한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/updateBoardArticle.do")
	public String updateBoardArticle(final MultipartHttpServletRequest multiRequest, @ModelAttribute("searchVO") BoardVO boardVO,
			@ModelAttribute("bdMstr") BoardMaster bdMstr, @ModelAttribute("board") Board board, BindingResult bindingResult, ModelMap model, HttpServletRequest req,
			SessionStatus status) throws Exception, NullPointerException {
		HttpSession session = req.getSession();

		board.setPopYn(req.getParameter("pop_yn"));

		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

		SessionVO SVO = (SessionVO) req.getSession().getAttribute("SessionVO");
		String atchFileId = boardVO.getAtchFileId();

		/* 웹취약점[불충분한인가] 수정. bbsid 변조 차단 - 2021.09.23 suhyun*/
		if ("GNR".equals(SVO.getMber_cls()) && !"BBSMSTR_000000000022".equals(board.getBbsId())) {
			model.addAttribute("MSG", "noAuth");
			return "forward:/cop/bbs/selectBoardList.do";
		}

		beanValidator.validate(board, bindingResult);
		if (bindingResult.hasErrors()) {

			boardVO.setFrstRegisterId(user.getUniqId());

			BoardMaster master = new BoardMaster();
			BoardMasterVO bmvo = new BoardMasterVO();
			BoardVO bdvo = new BoardVO();

			master.setBbsId(boardVO.getBbsId());
			master.setUniqId(user.getUniqId());

			bmvo = bbsAttrbService.selectBBSMasterInf(master);
			bdvo = bbsMngService.selectBoardArticle(boardVO);

			model.addAttribute("result", bdvo);
			model.addAttribute("bdMstr", bmvo);

			model.addAttribute("MSG", "fail");

			return "egovframework/com/cop/bbs/EgovNoticeUpdt";
		}

		/*
		boardVO.setFrstRegisterId(user.getUniqId());
		BoardMaster _bdMstr = new BoardMaster();
		BoardMasterVO bmvo = new BoardMasterVO();
		BoardVO bdvo = new BoardVO();
		vo.setBbsId(boardVO.getBbsId());
		_bdMstr.setBbsId(boardVO.getBbsId());
		_bdMstr.setUniqId(user.getUniqId());

		if (isAuthenticated) {
		    bmvo = bbsAttrbService.selectBBSMasterInf(_bdMstr);
		    bdvo = bbsMngService.selectBoardArticle(boardVO);
		}
		//*/
		//2020.06.04 보안취약점 조치(불충분한 인증 및 인가) - nttid변경으로 다른사용자의 게시물 수정할수있음
		BoardVO bdvo = new BoardVO();
		bdvo = bbsMngService.selectBoardArticle(boardVO);
		if (!SVO.getUser_id().equals(bdvo.getNtcrId())) {
			model.addAttribute("MSG", "noMatchUser");
			return "forward:/cop/bbs/selectBoardList.do";
		}

		if (isAuthenticated) {
			final Map<String, MultipartFile> files = multiRequest.getFileMap();
			Set<String> keyset = files.keySet();
			Iterator<String> iter = keyset.iterator();
			while (iter.hasNext()) {
				if (!FpisUtil.isValidFileExt(files.get(iter.next()).getOriginalFilename())) {
					model.addAttribute("MSG", "notPermit");
					return "forward:/cop/bbs/selectBoardList.do";
				}
			}

			if (!board.getCaptcha().equals(session.getAttribute(session.getId() + "captcha"))) {
				model.addAttribute("MSG", "notEqualCaptcha");
				return "forward:/cop/bbs/selectBoardList.do";
			}

			session.setAttribute(session.getId() + "captcha", "c_expired"); //1회성으로 사용하고 폐기

			if (!files.isEmpty()) {
				if ("".equals(atchFileId)) {
					List<FileVO> result = fileUtil.parseFileInf(files, "BBS_", 0, atchFileId, "");
					atchFileId = fileMngService.insertFileInfs(result);
					board.setAtchFileId(atchFileId);
				} else {
					FileVO fvo = new FileVO();
					fvo.setAtchFileId(atchFileId);
					int cnt = fileMngService.getMaxFileSN(fvo);
					List<FileVO> _result = fileUtil.parseFileInf(files, "BBS_", cnt, atchFileId, "");
					fileMngService.updateFileInfs(_result);
				}
			}

			board.setLastUpdusrId(user.getUniqId());
			board.setNtcrNm(""); // dummy 오류 수정 (익명이 아닌 경우 validator 처리를 위해 dummy로 지정됨)
			board.setPassword(""); // dummy 오류 수정 (익명이 아닌 경우 validator 처리를 위해 dummy로 지정됨)
			board.setNtcrId(SVO.getUser_id());

			if (SVO.getMber_cls().equals("ADM") || SVO.getMber_cls().equals("SYS") || SVO.getMber_cls().equals("SUP")) {
				board.setNtcrNm(SVO.getUser_name());
			} else if (SVO.getMber_cls().equals("GNR")) {
				board.setNtcrNm(SVO.getName());
			} else {
				board.setNtcrNm("미가입 사용자");
			}

			//2020.05.28 pch XSS방지
			board.setNttCn(XSS_secure(board.getNttCn()));
			board.setNttSj(XSS_secure(board.getNttSj()));

			board.setNttCn(XSS_secure_Dec(board.getNttCn()));
			board.setNttSj(XSS_secure_Dec(board.getNttSj()));

			bbsMngService.updateBoardArticle(board);
		}

		return "forward:/cop/bbs/selectBoardList.do";
	}

	/**
	 * 게시물에 대한 내용을 삭제한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/deleteBoardArticle.do")
	public String deleteBoardArticle(@ModelAttribute("searchVO") BoardVO boardVO, @ModelAttribute("board") Board board,
			@ModelAttribute("bdMstr") BoardMaster bdMstr, ModelMap model, HttpServletRequest req) throws Exception, NullPointerException {

		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		BoardVO vo = bbsMngService.selectBoardArticle(boardVO);
		if (isAuthenticated) {
			board.setLastUpdusrId(user.getUniqId());

			SessionVO svo = (SessionVO) req.getSession().getAttribute(FpisConstants.SESSION_KEY);
			//접속자 아이디와 글쓴이 아이디가 다르고,  관리자가 아니라면
			if (!svo.getUser_id().equals(vo.getNtcrId()) && !"SYS".equals(svo.getMem_cls())) {
				return "forward:/cop/bbs/selectBoardList.do";
			}
			bbsMngService.deleteBoardArticle(board);
		}

		return "forward:/cop/bbs/selectBoardList.do";
	}

	/**
	 * 방명록에 대한 목록을 조회한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/selectGuestList.do")
	public String selectGuestList(@ModelAttribute("searchVO") BoardVO boardVO, ModelMap model) throws Exception, NullPointerException {

		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		EgovUserDetailsHelper.isAuthenticated();

		// 수정 및 삭제 기능 제어를 위한 처리
		model.addAttribute("sessionUniqId", user.getUniqId());

		BoardVO vo = new BoardVO();

		vo.setBbsId(boardVO.getBbsId());
		vo.setBbsNm(boardVO.getBbsNm());
		vo.setNtcrNm(user.getName());
		vo.setNtcrId(user.getUniqId());

		BoardMasterVO masterVo = new BoardMasterVO();

		masterVo.setBbsId(vo.getBbsId());
		masterVo.setUniqId(user.getUniqId());

		BoardMasterVO mstrVO = bbsAttrbService.selectBBSMasterInf(masterVo);

		vo.setPageUnit(propertyService.getInt("pageUnit"));
		vo.setPageSize(propertyService.getInt("pageSize"));

		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(vo.getPageIndex());
		paginationInfo.setRecordCountPerPage(vo.getPageUnit());
		paginationInfo.setPageSize(vo.getPageSize());

		vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
		vo.setLastIndex(paginationInfo.getLastRecordIndex());
		vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> map = bbsMngService.selectGuestList(vo);
		int totCnt = Integer.parseInt((String) map.get("resultCnt"));

		paginationInfo.setTotalRecordCount(totCnt);

		model.addAttribute("resultList", map.get("resultList"));
		model.addAttribute("resultCnt", map.get("resultCnt"));
		model.addAttribute("brdMstrVO", mstrVO);
		model.addAttribute("boardVO", vo);
		model.addAttribute("paginationInfo", paginationInfo);

		return "egovframework/com/cop/bbs/EgovGuestList";
	}

	/**
	 * 방명록에 대한 내용을 삭제한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/deleteGuestList.do")
	public String deleteGuestList(@ModelAttribute("searchVO") BoardVO boardVO, @ModelAttribute("board") Board board, ModelMap model) throws Exception, NullPointerException {
		EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

		if (isAuthenticated) {
			bbsMngService.deleteGuestList(boardVO);
		}

		return "forward:/cop/bbs/selectGuestList.do";
	}

	/**
	 * 방명록 수정의 위한 목록을 조회한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/updateGuestList.do")
	public String updateGuestList(@ModelAttribute("searchVO") BoardVO boardVO, @ModelAttribute("board") Board board, BindingResult bindingResult,
			ModelMap model) throws Exception, NullPointerException {

		//BBST02, BBST04
		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

		beanValidator.validate(board, bindingResult);
		if (bindingResult.hasErrors()) {

			BoardVO vo = new BoardVO();

			vo.setBbsId(boardVO.getBbsId());
			vo.setBbsNm(boardVO.getBbsNm());
			vo.setNtcrNm(user.getName());
			vo.setNtcrId(user.getUniqId());

			BoardMasterVO masterVo = new BoardMasterVO();

			masterVo.setBbsId(vo.getBbsId());
			masterVo.setUniqId(user.getUniqId());

			BoardMasterVO mstrVO = bbsAttrbService.selectBBSMasterInf(masterVo);

			vo.setPageUnit(propertyService.getInt("pageUnit"));
			vo.setPageSize(propertyService.getInt("pageSize"));

			PaginationInfo paginationInfo = new PaginationInfo();
			paginationInfo.setCurrentPageNo(vo.getPageIndex());
			paginationInfo.setRecordCountPerPage(vo.getPageUnit());
			paginationInfo.setPageSize(vo.getPageSize());

			vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
			vo.setLastIndex(paginationInfo.getLastRecordIndex());
			vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

			Map<String, Object> map = bbsMngService.selectGuestList(vo);
			int totCnt = Integer.parseInt((String) map.get("resultCnt"));

			paginationInfo.setTotalRecordCount(totCnt);

			model.addAttribute("resultList", map.get("resultList"));
			model.addAttribute("resultCnt", map.get("resultCnt"));
			model.addAttribute("brdMstrVO", mstrVO);
			model.addAttribute("boardVO", vo);
			model.addAttribute("paginationInfo", paginationInfo);

			return "egovframework/com/cop/bbs/EgovGuestList";
		}

		if (isAuthenticated) {
			bbsMngService.updateBoardArticle(board);
			boardVO.setNttCn("");
			boardVO.setPassword("");
			boardVO.setNtcrId("");
			boardVO.setNttId(0);
		}

		return "forward:/cop/bbs/selectGuestList.do";
	}

	/**
	 * 방명록에 대한 내용을 등록한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/insertGuestList.do")
	public String insertGuestList(@ModelAttribute("searchVO") BoardVO boardVO, @ModelAttribute("board") Board board, BindingResult bindingResult,
			ModelMap model) throws Exception, NullPointerException {

		//그러니까 무인증은 아니고  - _- 익명으로 등록이 가능한 부분임
		// 무인증이 되려면 별도의 컨트롤러를 하나 더 등록해야함

		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

		beanValidator.validate(board, bindingResult);
		if (bindingResult.hasErrors()) {

			BoardVO vo = new BoardVO();

			vo.setBbsId(boardVO.getBbsId());
			vo.setBbsNm(boardVO.getBbsNm());
			vo.setNtcrNm(user.getName());
			vo.setNtcrId(user.getUniqId());

			BoardMasterVO masterVo = new BoardMasterVO();

			masterVo.setBbsId(vo.getBbsId());
			masterVo.setUniqId(user.getUniqId());

			BoardMasterVO mstrVO = bbsAttrbService.selectBBSMasterInf(masterVo);

			vo.setPageUnit(propertyService.getInt("pageUnit"));
			vo.setPageSize(propertyService.getInt("pageSize"));

			PaginationInfo paginationInfo = new PaginationInfo();
			paginationInfo.setCurrentPageNo(vo.getPageIndex());
			paginationInfo.setRecordCountPerPage(vo.getPageUnit());
			paginationInfo.setPageSize(vo.getPageSize());

			vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
			vo.setLastIndex(paginationInfo.getLastRecordIndex());
			vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

			Map<String, Object> map = bbsMngService.selectGuestList(vo);
			int totCnt = Integer.parseInt((String) map.get("resultCnt"));

			paginationInfo.setTotalRecordCount(totCnt);

			model.addAttribute("resultList", map.get("resultList"));
			model.addAttribute("resultCnt", map.get("resultCnt"));
			model.addAttribute("brdMstrVO", mstrVO);
			model.addAttribute("boardVO", vo);
			model.addAttribute("paginationInfo", paginationInfo);

			return "egovframework/com/cop/bbs/EgovGuestList";

		}

		if (isAuthenticated) {
			board.setFrstRegisterId(user.getUniqId());

			bbsMngService.insertBoardArticle(board);

			boardVO.setNttCn("");
			boardVO.setPassword("");
			boardVO.setNtcrId("");
			boardVO.setNttId(0);
		}

		return "forward:/cop/bbs/selectGuestList.do";
	}

	/**
	 * 익명게시물에 대한 목록을 조회한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/selectBoardList.do")
	public String selectAnonymousBoardArticles(@ModelAttribute("searchVO") BoardVO boardVO, HttpServletRequest req, ModelMap model) throws Exception, NullPointerException {
		SessionVO SVO = (SessionVO) req.getSession().getAttribute("SessionVO");

		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
		//log.debug(this.getClass().getName() + " user.getId() "+ user.getId());
		//log.debug(this.getClass().getName() + " user.getName() "+ user.getName());
		//log.debug(this.getClass().getName() + " user.getUniqId() "+ user.getUniqId());
		//log.debug(this.getClass().getName() + " user.getOrgnztId() "+ user.getOrgnztId());
		//log.debug(this.getClass().getName() + " user.getUserSe() "+ user.getUserSe());
		//log.debug(this.getClass().getName() + " user.getEmail() "+ user.getEmail());

		//String attrbFlag = "";
		boardVO.setBbsId(boardVO.getBbsId());
		boardVO.setBbsNm(boardVO.getBbsNm());

		BoardMasterVO vo = new BoardMasterVO();

		vo.setBbsId(boardVO.getBbsId());
		vo.setUniqId("ANONYMOUS"); // 익명

		BoardMasterVO master = bbsAttrbService.selectBBSMasterInf(vo);

		//-------------------------------
		// 익명게시판이 아니면.. 원래 게시판 URL로 forward
		//-------------------------------
		if (!master.getBbsTyCode().equals("BBST02")) {
			return "forward:/cop/bbs/selectBoardList.do";
		}
		////-----------------------------

		boardVO.setPageUnit(propertyService.getInt("pageUnit"));
		boardVO.setPageSize(propertyService.getInt("pageSize"));

		PaginationInfo paginationInfo = new PaginationInfo();

		paginationInfo.setCurrentPageNo(boardVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(boardVO.getPageUnit());
		paginationInfo.setPageSize(boardVO.getPageSize());

		boardVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		boardVO.setLastIndex(paginationInfo.getLastRecordIndex());
		boardVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> map = bbsMngService.selectBoardArticles(boardVO, vo.getBbsAttrbCode());
		int totCnt = Integer.parseInt((String) map.get("resultCnt"));

		paginationInfo.setTotalRecordCount(totCnt);

		//-------------------------------
		// 기본 BBS template 지정
		//-------------------------------
		if (master.getTmplatCours() == null || master.getTmplatCours().equals("")) {
			master.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
		}
		////-----------------------------
		/*
		 *  BBS ID
		 */

		model.addAttribute("resultList", map.get("resultList"));
		model.addAttribute("resultCnt", map.get("resultCnt"));
		model.addAttribute("boardVO", boardVO);
		model.addAttribute("brdMstrVO", master);
		model.addAttribute("paginationInfo", paginationInfo);
		model.addAttribute("SessionVO", SVO);
		model.addAttribute("anonymous", "true");

		model.addAttribute("MSG", req.getParameter("MSG"));
		return "egovframework/com/cop/bbs/EgovNoticeList";
	}

	/**
	 * 익명게시물 등록을 위한 등록페이지로 이동한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/addBoardArticle.do")
	public String addAnonymousBoardArticle(@ModelAttribute("searchVO") BoardVO boardVO, HttpServletRequest req, ModelMap model) throws Exception, NullPointerException {
		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
		//Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		Boolean isAuthenticated = true;
		SessionVO SVO = (SessionVO) req.getSession().getAttribute("SessionVO");

		BoardMasterVO bdMstr = new BoardMasterVO();

		if (isAuthenticated) {
			BoardMasterVO vo = new BoardMasterVO();
			vo.setBbsId(boardVO.getBbsId());
			vo.setUniqId("ANONYMOUS");

			bdMstr = bbsAttrbService.selectBBSMasterInf(vo);
			model.addAttribute("bdMstr", bdMstr);
		}

		//-------------------------------
		// 익명게시판이 아니면.. 원래 게시판 URL로 forward
		//-------------------------------
		if (!bdMstr.getBbsTyCode().equals("BBST02")) {
			return "forward:/cop/bbs/addBoardArticle.do";
		}
		////-----------------------------

		//----------------------------
		// 기본 BBS template 지정
		//----------------------------
		if (bdMstr.getTmplatCours() == null || bdMstr.getTmplatCours().equals("")) {
			bdMstr.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
		}

		model.addAttribute("brdMstrVO", bdMstr);

		////-----------------------------

		model.addAttribute("anonymous", "true");
		model.addAttribute("SessionVO", SVO);

		return "egovframework/com/cop/bbs/EgovNoticeRegist";
	}

	/**
	 * 익명게시물을 등록한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/insertBoardArticle.do")
	public String insertAnonymousBoardArticle(final MultipartHttpServletRequest multiRequest,
			@ModelAttribute("searchVO") BoardVO boardVO,
			@ModelAttribute("bdMstr") BoardMaster bdMstr,
			@ModelAttribute("board") Board board,
			BindingResult bindingResult, SessionStatus status,
			HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {

		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
		//Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

		SessionVO SVO = (SessionVO) req.getSession().getAttribute("SessionVO");
		Boolean isAuthenticated = true;

		beanValidator.validate(board, bindingResult);
		if (bindingResult.hasErrors()) {

			BoardMasterVO master = new BoardMasterVO();
			BoardMasterVO vo = new BoardMasterVO();

			vo.setBbsId(boardVO.getBbsId());
			vo.setUniqId("ANONYMOUS");

			master = bbsAttrbService.selectBBSMasterInf(vo);

			model.addAttribute("bdMstr", master);

			//-------------------------------
			// 익명게시판이 아니면.. 원래 게시판 URL로 forward
			//-------------------------------
			if (!bdMstr.getBbsTyCode().equals("BBST02")) {
				return "forward:/cop/bbs/insertBoardArticle.do";
			}
			////-----------------------------

			//----------------------------
			// 기본 BBS template 지정
			//----------------------------
			if (master.getTmplatCours() == null || master.getTmplatCours().equals("")) {
				master.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
			}

			model.addAttribute("brdMstrVO", master);
			////-----------------------------

			model.addAttribute("anonymous", "true");
			model.addAttribute("SessionVO", SVO);

			return "egovframework/com/cop/bbs/EgovNoticeRegist";
		}

		if (isAuthenticated) {
			List<FileVO> result = null;
			String atchFileId = "";

			final Map<String, MultipartFile> files = multiRequest.getFileMap();
			if (!files.isEmpty()) {
				result = fileUtil.parseFileInf(files, "BBS_", 0, "", "");
				atchFileId = fileMngService.insertFileInfs(result);
			}
			board.setAtchFileId(atchFileId);
			board.setFrstRegisterId("ANONYMOUS");
			board.setBbsId(board.getBbsId());

			// 익명게시판 관련
			board.setNtcrNm(board.getNtcrNm());
			board.setPassword(EgovFileScrty.encryptPassword(board.getPassword()));

			board.setNttCn(unscript(board.getNttCn())); // XSS 방지

			bbsMngService.insertBoardArticle(board);
		}

		//status.setComplete();
		return "forward:/cop/bbs/anonymous/selectBoardList.do";
	}

	/**
	 * 익명게시물에 대한 상세 정보를 조회한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/selectBoardArticle.do")
	public String selectAnonymousBoardArticle(@ModelAttribute("searchVO") BoardVO boardVO, ModelMap model) throws Exception, NullPointerException {
		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();

		// 조회수 증가 여부 지정
		boardVO.setPlusCount(true);

		//---------------------------------
		// 2009.06.29 : 2단계 기능 추가
		//---------------------------------
		if (!boardVO.getSubPageIndex().equals("")) {
			boardVO.setPlusCount(false);
		}
		////-------------------------------

		boardVO.setLastUpdusrId("ANONYMOUS");
		BoardVO vo = bbsMngService.selectBoardArticle(boardVO);

		/* qout로 잘못 저장된 데이터 보정 - 2021.11.09 suhyun */
		vo.setNttCn(vo.getNttCn().replaceAll("qout;", "quot;"));
		model.addAttribute("result", vo);
		//CommandMap의 형태로 개선????

		model.addAttribute("sessionUniqId", "ANONYMOUS");

		//----------------------------
		// template 처리 (기본 BBS template 지정  포함)
		//----------------------------
		BoardMasterVO master = new BoardMasterVO();

		master.setBbsId(boardVO.getBbsId());
		master.setUniqId("ANONYMOUS");

		BoardMasterVO masterVo = bbsAttrbService.selectBBSMasterInf(master);

		//-------------------------------
		// 익명게시판이 아니면.. 원래 게시판 URL로 forward
		//-------------------------------
		if (!masterVo.getBbsTyCode().equals("BBST02")) {
			return "forward:/cop/bbs/selectBoardArticle.do";
		}
		////-----------------------------

		if (masterVo.getTmplatCours() == null || masterVo.getTmplatCours().equals("")) {
			masterVo.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
		}

		model.addAttribute("brdMstrVO", masterVo);
		////-----------------------------

		model.addAttribute("anonymous", "true");

		//----------------------------
		// 2009.06.29 : 2단계 기능 추가
		// 2011.07.01 : 댓글, 스크랩, 만족도 조사 기능의 종속성 제거
		//----------------------------
		if (bbsCommentService != null) {
			if (bbsCommentService.canUseComment(boardVO.getBbsId())) {
				model.addAttribute("useComment", "true");
			}
		}
		if (bbsSatisfactionService != null) {
			if (bbsSatisfactionService.canUseSatisfaction(boardVO.getBbsId())) {
				model.addAttribute("useSatisfaction", "true");
			}
		}
		if (bbsScrapService != null) {
			if (bbsScrapService.canUseScrap()) {
				model.addAttribute("useScrap", "true");
			}
		}
		////--------------------------

		return "egovframework/com/cop/bbs/EgovNoticeInqire";
	}

	/**
	 * 익명게시물에 대한 내용을 삭제한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/deleteBoardArticle.do")
	public String deleteAnonymousBoardArticle(@ModelAttribute("searchVO") BoardVO boardVO, @ModelAttribute("board") Board board,
			@ModelAttribute("bdMstr") BoardMaster bdMstr, ModelMap model) throws Exception, NullPointerException {

		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
		//Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		Boolean isAuthenticated = true;

		//--------------------------------------------------
		// 마스터 정보 얻기
		//--------------------------------------------------
		BoardMasterVO master = new BoardMasterVO();

		master.setBbsId(boardVO.getBbsId());
		master.setUniqId("ANONYMOUS");

		BoardMasterVO masterVo = bbsAttrbService.selectBBSMasterInf(master);

		//-------------------------------
		// 익명게시판이 아니면.. 원래 게시판 URL로 forward
		//-------------------------------
		if (!masterVo.getBbsTyCode().equals("BBST02")) {
			return "forward:/cop/bbs/deleteBoardArticle.do";
		}
		////-----------------------------

		//-------------------------------
		// 패스워드 비교
		//-------------------------------
		String dbpassword = bbsMngService.getPasswordInf(board);
		String enpassword = EgovFileScrty.encryptPassword(board.getPassword());

		if (!dbpassword.equals(enpassword)) {

			model.addAttribute("msg", egovMessageSource.getMessage("cop.password.not.same.msg"));

			return "forward:/cop/bbs/anonymous/selectBoardArticle.do";
		}
		////-----------------------------

		if (isAuthenticated) {
			board.setLastUpdusrId("ANONYMOUS");

			bbsMngService.deleteBoardArticle(board);
		}

		return "forward:/cop/bbs/anonymous/selectBoardList.do";
	}

	/**
	 * 익명게시물 수정을 위한 수정페이지로 이동한다.
	 *
	 * @param boardVO
	 * @param vo
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/forUpdateBoardArticle.do")
	public String selectAnonymousBoardArticleForUpdt(@ModelAttribute("searchVO") BoardVO boardVO, @ModelAttribute("board") BoardVO vo, ModelMap model)
			throws Exception, NullPointerException {

		//log.debug(this.getClass().getName()+"selectBoardArticleForUpdt getNttId "+boardVO.getNttId());
		//log.debug(this.getClass().getName()+"selectBoardArticleForUpdt getBbsId "+boardVO.getBbsId());

		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
		//Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		Boolean isAuthenticated = true;

		boardVO.setFrstRegisterId("ANONYMOUS");

		BoardMaster master = new BoardMaster();
		BoardMasterVO bmvo = new BoardMasterVO();
		BoardVO bdvo = new BoardVO();

		vo.setBbsId(boardVO.getBbsId());

		master.setBbsId(boardVO.getBbsId());
		master.setUniqId("ANONYMOUS");

		if (isAuthenticated) {
			bmvo = bbsAttrbService.selectBBSMasterInf(master);

			//-------------------------------
			// 익명게시판이 아니면.. 원래 게시판 URL로 forward
			//-------------------------------
			if (!bmvo.getBbsTyCode().equals("BBST02")) {
				return "forward:/cop/bbs/forUpdateBoardArticle.do";
			}
			////-----------------------------

			//-------------------------------
			// 패스워드 비교
			//-------------------------------
			String dbpassword = bbsMngService.getPasswordInf(boardVO);
			String enpassword = EgovFileScrty.encryptPassword(boardVO.getPassword());

			if (!dbpassword.equals(enpassword)) {

				model.addAttribute("msg", egovMessageSource.getMessage("cop.password.not.same.msg"));

				return "forward:/cop/bbs/anonymous/selectBoardArticle.do";
			}
			////-----------------------------

			bdvo = bbsMngService.selectBoardArticle(boardVO);
		}

		model.addAttribute("result", bdvo);
		model.addAttribute("bdMstr", bmvo);

		//----------------------------
		// 기본 BBS template 지정
		//----------------------------
		if (bmvo.getTmplatCours() == null || bmvo.getTmplatCours().equals("")) {
			bmvo.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
		}

		model.addAttribute("brdMstrVO", bmvo);
		////-----------------------------

		model.addAttribute("anonymous", "true");

		return "egovframework/com/cop/bbs/EgovNoticeUpdt";
	}

	/**
	 * 익명게시물에 대한 내용을 수정한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/updateBoardArticle.do")
	public String updateAnonymousBoardArticle(final MultipartHttpServletRequest multiRequest, @ModelAttribute("searchVO") BoardVO boardVO,
			@ModelAttribute("bdMstr") BoardMaster bdMstr, @ModelAttribute("board") Board board, BindingResult bindingResult, ModelMap model,
			SessionStatus status) throws Exception, NullPointerException {

		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
		//Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		Boolean isAuthenticated = true;

		String atchFileId = boardVO.getAtchFileId();

		beanValidator.validate(board, bindingResult);
		if (bindingResult.hasErrors()) {

			boardVO.setFrstRegisterId("ANONYMOUS");

			BoardMaster master = new BoardMaster();
			BoardMasterVO bmvo = new BoardMasterVO();
			BoardVO bdvo = new BoardVO();

			master.setBbsId(boardVO.getBbsId());
			master.setUniqId("ANONYMOUS");

			bmvo = bbsAttrbService.selectBBSMasterInf(master);

			//-------------------------------
			// 익명게시판이 아니면.. 원래 게시판 URL로 forward
			//-------------------------------
			if (!bdMstr.getBbsTyCode().equals("BBST02")) {
				return "forward:/cop/bbs/updateBoardArticle.do";
			}
			////-----------------------------

			bdvo = bbsMngService.selectBoardArticle(boardVO);

			model.addAttribute("result", bdvo);
			model.addAttribute("bdMstr", bmvo);

			model.addAttribute("anonymous", "true");

			return "egovframework/com/cop/bbs/EgovNoticeUpdt";
		}

		if (isAuthenticated) {
			final Map<String, MultipartFile> files = multiRequest.getFileMap();
			if (!files.isEmpty()) {
				if ("".equals(atchFileId)) {
					List<FileVO> result = fileUtil.parseFileInf(files, "BBS_", 0, atchFileId, "");
					atchFileId = fileMngService.insertFileInfs(result);
					board.setAtchFileId(atchFileId);
				} else {
					FileVO fvo = new FileVO();
					fvo.setAtchFileId(atchFileId);
					int cnt = fileMngService.getMaxFileSN(fvo);
					List<FileVO> _result = fileUtil.parseFileInf(files, "BBS_", cnt, atchFileId, "");
					fileMngService.updateFileInfs(_result);
				}
			}

			board.setLastUpdusrId("ANONYMOUS");

			// 익명게시판 관련
			board.setNtcrNm(board.getNtcrNm());
			board.setPassword(EgovFileScrty.encryptPassword(board.getPassword()));

			board.setNttCn(unscript(board.getNttCn())); // XSS 방지

			bbsMngService.updateBoardArticle(board);
		}

		return "forward:/cop/bbs/anonymous/selectBoardList.do";
	}

	/**
	 * 익명게시물에 대한 답변 등록을 위한 등록페이지로 이동한다.
	 *
	 * @param boardVO
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/addReplyBoardArticle.do")
	public String addAnonymousReplyBoardArticle(@ModelAttribute("searchVO") BoardVO boardVO, ModelMap model) throws Exception, NullPointerException {
		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();

		BoardMasterVO master = new BoardMasterVO();
		BoardMasterVO vo = new BoardMasterVO();

		vo.setBbsId(boardVO.getBbsId());
		vo.setUniqId("ANONYMOUS");

		master = bbsAttrbService.selectBBSMasterInf(vo);

		//-------------------------------
		// 익명게시판이 아니면.. 원래 게시판 URL로 forward
		//-------------------------------
		if (!master.getBbsTyCode().equals("BBST02")) {
			return "forward:/cop/bbs/addReplyBoardArticle.do";
		}
		////-----------------------------

		model.addAttribute("bdMstr", master);
		model.addAttribute("result", boardVO);

		//----------------------------
		// 기본 BBS template 지정
		//----------------------------
		if (master.getTmplatCours() == null || master.getTmplatCours().equals("")) {
			master.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
		}

		model.addAttribute("brdMstrVO", master);
		////-----------------------------

		model.addAttribute("anonymous", "true");

		return "egovframework/com/cop/bbs/EgovNoticeReply";
	}

	/**
	 * 익명게시물에 대한 답변을 등록한다.
	 *
	 * @param boardVO
	 * @param board
	 * @param sessionVO
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/cop/bbs/anonymous/replyBoardArticle.do")
	public String replyAnonymousBoardArticle(final MultipartHttpServletRequest multiRequest, @ModelAttribute("searchVO") BoardVO boardVO,
			@ModelAttribute("bdMstr") BoardMaster bdMstr, @ModelAttribute("board") Board board, BindingResult bindingResult, ModelMap model,
			SessionStatus status) throws Exception, NullPointerException {

		//LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
		//Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		Boolean isAuthenticated = true;

		beanValidator.validate(board, bindingResult);
		if (bindingResult.hasErrors()) {
			BoardMasterVO master = new BoardMasterVO();
			BoardMasterVO vo = new BoardMasterVO();

			vo.setBbsId(boardVO.getBbsId());
			vo.setUniqId("ANONYMOUS");

			master = bbsAttrbService.selectBBSMasterInf(vo);

			//-------------------------------
			// 익명게시판이 아니면.. 원래 게시판 URL로 forward
			//-------------------------------
			if (!master.getBbsTyCode().equals("BBST02")) {
				return "forward:/cop/bbs/replyBoardArticle.do";
			}
			////-----------------------------

			model.addAttribute("bdMstr", master);
			model.addAttribute("result", boardVO);

			//----------------------------
			// 기본 BBS template 지정
			//----------------------------
			if (master.getTmplatCours() == null || master.getTmplatCours().equals("")) {
				master.setTmplatCours("/css/egovframework/com/cop/tpl/egovBaseTemplate.css");
			}

			model.addAttribute("brdMstrVO", master);
			////-----------------------------

			model.addAttribute("anonymous", "true");

			return "egovframework/com/cop/bbs/EgovNoticeReply";
		}

		if (isAuthenticated) {
			final Map<String, MultipartFile> files = multiRequest.getFileMap();
			String atchFileId = "";

			if (!files.isEmpty()) {
				List<FileVO> result = fileUtil.parseFileInf(files, "BBS_", 0, "", "");
				atchFileId = fileMngService.insertFileInfs(result);
			}

			board.setAtchFileId(atchFileId);
			board.setReplyAt("Y");
			board.setFrstRegisterId("ANONYMOUS");
			board.setBbsId(board.getBbsId());
			board.setParnts(Long.toString(boardVO.getNttId()));
			board.setSortOrdr(boardVO.getSortOrdr());
			board.setReplyLc(Integer.toString(Integer.parseInt(boardVO.getReplyLc()) + 1));

			// 익명게시판 관련
			board.setNtcrNm(board.getNtcrNm());
			board.setPassword(EgovFileScrty.encryptPassword(board.getPassword()));

			board.setNttCn(unscript(board.getNttCn())); // XSS 방지

			bbsMngService.insertBoardArticle(board);
		}

		return "forward:/cop/bbs/anonymous/selectBoardList.do";
	}

	/*2014.10.31 양상완 공지사항 팝업 추가*/
	@RequestMapping("dashboard/FpisNoticePopup.do")
	public String popUpNotice(@ModelAttribute("searchVO") BoardVO boardVO, ModelMap model) throws Exception, NullPointerException {
		boardVO.setBbsId("BBSMSTR_000000000001");
		boardVO = bbsMngService.selectBoardArticle(boardVO);

		/* qout로 잘못 저장된 데이터 보정 - 2021.11.09 suhyun */
		boardVO.setNttCn(boardVO.getNttCn().replaceAll("qout;", "quot;"));

		model.addAttribute("boardVO", boardVO);
		return "fpis/dashboard/FpisNoticePopup";
	}

	//2020.05.28 pch : 보안취약점(크로스스크립트)
	public String XSS_secure(String param) {
		String cont = param;
		String cont_low = cont.toLowerCase();

		if (cont_low.contains("javascript") || cont_low.contains("script") || cont_low.contains("iframe") || cont_low.contains("document") ||
				cont_low.contains("vbscript") || cont_low.contains("applet") || cont_low.contains("embed") || cont_low.contains("object") ||
				cont_low.contains("frame") || cont_low.contains("grameset") || cont_low.contains("layer") || cont_low.contains("bgsound") ||
				cont_low.contains("alert") || cont_low.contains("onblur") || cont_low.contains("onchange") || cont_low.contains("onclick") ||
				cont_low.contains("ondblclick") || cont_low.contains("enerror") || cont_low.contains("onfocus") || cont_low.contains("onload") ||
				cont_low.contains("onmouse") || cont_low.contains("onscroll") || cont_low.contains("onsubmit") || cont_low.contains("onunload") ||
				cont_low.contains("onerror") || cont_low.contains("confirm") || cont_low.contains("prompt")) {
			cont = cont_low;
			cont = cont.replaceAll("javascript", "x-javascript");
			cont = cont.replaceAll("script", "x-script");
			cont = cont.replaceAll("iframe", "x-iframe");
			cont = cont.replaceAll("document", "x-document");
			cont = cont.replaceAll("vbscript", "x-vbscript");
			cont = cont.replaceAll("applet", "x-applet");
			cont = cont.replaceAll("embed", "x-embed");
			cont = cont.replaceAll("object", "x-object");
			cont = cont.replaceAll("frame", "x-frame");
			cont = cont.replaceAll("grameset", "x-grameset");
			cont = cont.replaceAll("layer", "x-layer");
			cont = cont.replaceAll("bgsound", "x-bgsound");
			cont = cont.replaceAll("alert", "x-alert");
			cont = cont.replaceAll("onblur", "x-onblur");
			cont = cont.replaceAll("onchange", "x-onchange");
			cont = cont.replaceAll("onclick", "x-onclick");
			cont = cont.replaceAll("ondblclick", "x-ondblclick");
			cont = cont.replaceAll("enerror", "x-enerror");
			cont = cont.replaceAll("onfocus", "x-onfocus");
			cont = cont.replaceAll("onload", "x-onload");
			cont = cont.replaceAll("onmouse", "x-onmouse");
			cont = cont.replaceAll("onscroll", "x-onscroll");
			cont = cont.replaceAll("onsubmit", "x-onsubmit");
			cont = cont.replaceAll("onunload", "x-onunload");
			cont = cont.replaceAll("onerror", "x-onerror");
			cont = cont.replaceAll("confirm", "x-confirm");
			cont = cont.replaceAll("prompt", "x-prompt");

			param = cont;
		}
		if (param.indexOf("\"") != -1) {
			param = param.replaceAll("\"", "&quot;");
		}
		if (param.indexOf("\'") != -1) {
			param = param.replaceAll("\'", "&apos;");
		}
		if (param.indexOf("<") != -1) {
			param = param.replaceAll("<", "&lt;");
		}
		if (param.indexOf(">") != -1) {
			param = param.replaceAll(">", "&gt;");
		}
		if (param.indexOf("&") != -1) {
			param = param.replaceAll("&", "&amp;");
		}
		if (param.indexOf(">") != -1) {
			param = param.replaceAll("/", "&#x2F;");
		}

		return param;
	}

	public static String XSS_secure_Dec(String param) {
		String statement = param;

		statement = statement.replaceAll("&amp;", "&");
		statement = statement.replaceAll("&quot;", "\"");
		statement = statement.replaceAll("&apos;", "\'");
		statement = statement.replaceAll("&lt;", "<");
		statement = statement.replaceAll("&gt;", ">");
		statement = statement.replaceAll("&#x2F;", "/");
		return statement;
	}

}
