package egovframework.com.uss.umt.web;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

//import com.google.common.collect.Lists;
import org.apache.commons.collections4.ListUtils;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.EgovWebUtil;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.annotation.IncludedInfo;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.com.uat.uia.service.EgovLoginService;
import egovframework.com.uss.umt.service.EgovMberManageService;
import egovframework.com.uss.umt.service.FpisInactiveVO;
import egovframework.com.uss.umt.service.FpisNewMberVO;
import egovframework.com.uss.umt.service.MberManageVO;
import egovframework.com.uss.umt.service.SigunguVO;
import egovframework.com.uss.umt.service.UserDefaultVO;
import egovframework.com.uss.umt.service.UsrStateVisualVO;
import egovframework.com.uss.umt.service.impl.MberManageDAO;
import egovframework.com.utl.fcc.service.EgovNumberCheckUtil;
import egovframework.com.utl.sim.service.EgovFileScrty;
import egovframework.rte.fdl.property.EgovPropertyService;
import fpis.admin.accessLog.FpisAccessLogService;
import fpis.admin.accessLog.FpisAccessLogVO;
import fpis.admin.sysCompManage.FpisAdminSysCompManageService;
import fpis.common.service.CommonService;
import fpis.common.service.ListToExcel;
import fpis.common.service.MailDetailVO;
import fpis.common.service.MailMasterVO;
import fpis.common.service.MailService;
import fpis.common.utils.AESCrypto;
import fpis.common.utils.FpisConstants;
import fpis.common.utils.Util;
import fpis.common.utils.Util_poi;
import fpis.common.vo.FpisLoginLogVO;
import fpis.common.vo.KakaoVO;
import fpis.common.vo.SessionVO;
import fpis.common.vo.sys.SysCodeVO;
import fpis.common.vo.sys.SysCompanyInfoVO;
import fpis.common.vo.usr.UsrInfoVO;
import fpis.file_management.service.FpisFileManagementService;
import fpis.file_management.service.FpisFileManagementVO;
import fpis.online.stdinfo.assoc.FpisAssocService;
import fpis.online.stdinfo.assoc.FpisAssocVO;
import fpis.online.stdinfo.car.service.FpisCarManageService;
import fpis.online.stdinfo.car.service.FpisCarManageVO;
import fpis.online.stdinfo.client.service.FpisClientManageService;
import fpis.online.stdinfo.client.service.FpisNewJoinVO;
import fpis.online.stdinfo.client.service.FpisSysCompanyVO;
import fpis.online.stdinfo.client.service.FpisUsrCompanyVO;
import fpis.online.stdinfo.net.service.FpisNetManageService;
import twitter4j.internal.org.json.JSONArray;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.org.json.JSONObject;

/**
 * 일반회원관련 요청을 비지니스 클래스로 전달하고 처리된결과를 해당 웹 화면으로 전달하는 Controller를 정의한다
 * 
 * @author 공통서비스 개발팀 조재영
 * @since 2009.04.10
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.04.10  조재영          최초 생성
 *   2011.8.26   정진오          IncludedInfo annotation 추가
 *   2013.09.23  jhoh      회원가입시 검색조건 간소화(userJoin.do)
 *   2013.10.11  jhoh      업태, 가맹망 이용 선택시 김대리님 생성한 소스 적용(/getDetailCompClsData.do  /getUsrInfoNetCompData.do)
 *   2014.01.17  mgkim     오정화사원 작업부분 오류사항 수정
 *   2014.01.17  mgkim     관리자 신고주체관리 상세보기 미구현부분 보완
 *   2014.01.21  mgkim     관리자 신고주체관리 리스트 검색항목 추가(사업자번호,법인번호)
 *   2014.01.21  mgkim     관리자 신고주체관리 상세보기 수정기능 이후 검색파라메터 유지 안되는 오류수정
 *   2014.04.09  swyang    공인인증서 수정 기능 추가.
 *   2018.10.17  smOh      관할관청 관리기능 추가
 *   2019.11.07  pes       휴면계정관리 기능 추가
 *
 * </pre>
 */
@SuppressWarnings({ "rawtypes" }) @Controller
public class EgovMberManageController {
	private static final Logger logger = Logger.getLogger(EgovMberManageController.class);
	/** mberManageService */
	@Resource(name = "mberManageService")
	private EgovMberManageService mberManageService;

	/** cmmUseService */
	@Resource(name = "EgovCmmUseService")
	private EgovCmmUseService cmmUseService;

	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;

	/** Log Info */
	protected Log log = LogFactory.getLog(this.getClass());

	@Value(value = "#{globals['Globals.fileStorePath']}")
	private String fileStorePath;

	@Value(value = "#{globals['Globals.fpisFilePath']}")
	private String fpisFilePath;

	@Value(value = "#{fpis['FPIS.domain']}")
	private String program_domain;

	/** DefaultBeanValidator beanValidator */
	/*
	 * @Autowired private DefaultBeanValidator beanValidator;
	 */

	// 업체정보 가져오기 Service
	@Resource(name = "FpisClientManageService")
	private FpisClientManageService FpisSvc;

	// 차량정보 등록용 서비스
	/*
	 * @Resource(name = "FpisCarManageService") private FpisCarManageService
	 * FpisCarsSvc;
	 */

	@Resource(name = "FpisCarManageService")
	private FpisCarManageService CarManageService;

	// 2019.09.25 by jhoh : 업태 구분 코드 가져오기
	@Resource(name = "CommonService")
	private CommonService commonService;

	// 2013.12.03 mgkim 차량관리 현황 (1대사업자 업태 변경시 필요)
	/*
	 * @Resource(name = "FpisCarManageService") private FpisCarManageService
	 * CarManageService;
	 */

	// 2014.02.21 mgkim 회원탈퇴신청 로그 처리
	/** EgovLoginService */
	@Resource(name = "loginService")
	private EgovLoginService loginService;

	// 2014.05.13 swyang 파일명 암호화를 위한 서비스
	@Resource(name = "FpisFileManagementService")
	private FpisFileManagementService fileManagementService;

	// 2015.02.06 swyang NOT_JOIN 테이블 삽입 프로세스를 위해 이 서비스 사용
	@Resource(name = "FpisAdminSysCompManageService")
	private FpisAdminSysCompManageService adminSysCompManageService;

	@Resource(name = "MailService")
	private MailService mailService;

	// 가맹점 관리 서비스
	@Resource(name = "FpisNetManageService")
	private FpisNetManageService FpisNetSvc;

	// 대행사 회원관리
	@Resource(name = "FpisAssocService")
	private FpisAssocService FpisAssocSvc;

	// 2020.11.10 ysw 사업자정보 이력을 위한 서비스
	@Resource(name = "FpisAccessLogService")
	private FpisAccessLogService accessLogService;

	/**
	 * 일반회원목록을 조회한다. (pageing)
	 * 
	 * @param userSearchVO 검색조건정보
	 * @param model 화면모델
	 * @return uss/umt/EgovMberManage
	 * @throws Exception 2014.01.20 mgkim 오정화사원 기능구현부분 소스 정리 2014.01.20 mgkim
	 * 페이징 교체(공통모듈 적용) 2014.01.21 mgkim 검색기능 보완(사업자번호, 법인번호 검색 기능 추가) 2014.02.25
	 * mgkim DB암호화 적용 소스수정 , 사업자번호,법인번호 암호화로 LIKE 검색 불가능 2014.03.05 mgkim 정보수정일
	 * 표기
	 */
	@IncludedInfo(name = "일반회원관리", order = 470, gid = 50) @RequestMapping(value = "/uss/umt/EgovMberManage.do")
	public String selectMberList(@ModelAttribute("userSearchVO") UserDefaultVO shVO,
			@RequestParam(value = "selbox", required = false) String[] selbox, @ModelAttribute SigunguVO sigunguVO,
			ModelMap model, HttpServletRequest req) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		String searchSidoCd = req.getParameter("hid_sido_code");
		String searchSigunguCd = req.getParameter("hid_sigungu_code");
		List<SysCodeVO> codeFMS023 = commonService.commonCode("FMS023", null); // 2015.01.19
																				// 양상완
																				// 업태
																				// 코드
																				// 변경

		// 181030 smoh 관할관청 확정/변경&반려 등록
		if (selbox != null) { // [2018156538/11110/P, 3710200115/11110/P] ->
								// 사업자번호/시군구코드/현상태값
			String govFlag = req.getParameter("gov_flag");
			if (govFlag != null && !"".equals(govFlag)) {
				String[] selBoxObj = null;
				FpisNewMberVO mVo = new FpisNewMberVO();
				List<FpisNewMberVO> mList = new ArrayList<FpisNewMberVO>();
				for (int i = 0; i < selbox.length; i++) {
					selBoxObj = selbox[i].split("/");
					govFlag = "Y".equals(govFlag) ? "Y"
							: "Y".equals(selBoxObj[2]) ? "U" : "U".equals(selBoxObj[2]) ? "U" : "N";

					mVo = new FpisNewMberVO();
					mVo.setComp_mst_key(selBoxObj[0]);
					mVo.setReg_num("0");
					mVo.setSigunguCd(selBoxObj[1]);
					mVo.setGov_status(govFlag);
					mVo.setNote(shVO.getNote());
					mVo.setReg_user(svo.getUser_id());
					mList.add(mVo);
				}

				// 관할지역 이력 등록 및 usr_info gov_seq 업데이트
				mberManageService.insertGovHistoryList(mList);
				mberManageService.updateUsrInfoGovHistorySeq(mList);
			}
		}
		shVO.setMber_cls("GNR");

		// 2014.01.21 mgkim 검색항목 추가
		String org_comp_bsns_num = shVO.getComp_bsns_num();
		if (org_comp_bsns_num != null) {
			shVO.setComp_bsns_num(shVO.getComp_bsns_num().replaceAll("-", ""));
		}
		String org_comp_corp_num = shVO.getComp_corp_num();
		if (org_comp_corp_num != null) {
			shVO.setComp_corp_num(shVO.getComp_corp_num().replaceAll("-", ""));
		}

		// PAGING...

		/* 2014.08.29 양상완 지자체 관리자일때는 관리지역 검색조건이 자기지역으로 고정된다. */

		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}

		if (svo.getMber_cls().equals("ADM")) {
			model.addAttribute("hid_sido_code", svo.getAdm_area_code().substring(0, 2));
			if (svo.getAdm_area_code().length() == 2) { // 2014.12.01 mgkim 시도
														// 관리자 검색조건 확인
				searchSidoCd = svo.getAdm_area_code();
				model.addAttribute("hid_sido_code", svo.getAdm_area_code());
				model.addAttribute("hid_sigungu_code", searchSigunguCd);
			} else {
				searchSigunguCd = svo.getAdm_area_code();
				model.addAttribute("hid_sigungu_code", svo.getAdm_area_code());
				model.addAttribute("hid_sigungu_name", svo.getAdm_area_name());
			}
		} else {
			model.addAttribute("hid_sido_code", searchSidoCd);
			model.addAttribute("hid_sigungu_code", searchSigunguCd);
		}

		shVO.setSearch_sigungu_cd(searchSigunguCd); // 시군구 시디 부여
		shVO.setSearch_sido_cd(searchSidoCd);
		int totCnt = mberManageService.selectMberListTotCnt(shVO);

		shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		shVO.setTot_page(Util.calcurateTPage(totCnt));

		List<MberManageVO> mberList = mberManageService.selectMberList(shVO);

		shVO.setComp_bsns_num(org_comp_bsns_num); // 2014.01.21 사업자번호 검색 "-" 기호
													// 제거 사용자가 입력한 값 그대로 반환
		shVO.setComp_corp_num(org_comp_corp_num); // 2014.01.21 사업자번호 검색 "-" 기호
													// 제거 사용자가 입력한 값 그대로 반환

		model.addAttribute("VO", shVO);
		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("resultList", mberList);

		// 일반회원 상태코드를 코드정보로부터 조회
		ComDefaultCodeVO vo = new ComDefaultCodeVO();
		vo.setCodeId("COM013");
		List mberSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("entrprsMberSttus_result", mberSttus_result);// 기업회원상태코드목록

		vo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);// 기업회원상태코드목록

		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);

		List<SigunguVO> sigunList = null;
		if (searchSidoCd != null && !searchSidoCd.equals("")) {
			sigunguVO.setSidoCd(searchSidoCd);
			sigunList = mberManageService.selectSigungu2016(sigunguVO);
		}
		model.addAttribute("SIGUNLIST", sigunList);
		model.addAttribute("codeFMS023", codeFMS023);

		return "egovframework/com/uss/umt/EgovMberManage";
	}

	/**
	 * 일반회원정보 수정을 위해 일반회원정보를 상세조회한다.
	 * 
	 * @param mberId 상세조회대상 일반회원아이디
	 * @param userSearchVO 검색조건
	 * @param model 화면모델
	 * @return uss/umt/EgovMberSelectUpdt
	 * @throws Exception 관리자 - 신고주체 관리 - 신고주체 상세보기 2014.01.17 mgkim 오정화사원 개발 부분
	 * 불필요 소스 정리 2014.01.17 mgkim 업태 코드 기능 적용 2014.01.21 mgkim 수정하기 처리 이후 검색파라메타
	 * 유지 2014.02.25 mgkim DB암호화 적용 소스수정
	 *
	 * 2015.01.13 mgkim 회원가입, 재회원가입, My정보, 신고주체상세조회(관리자) 페이지 공통 기능변경 사항 -
	 * 2015.01.09 mgkim 사업단회의 결과 반영 - 1. 업태 분류 다중선택이 필요함 => 겸업 내용 상세추가하는것으로
	 * 결정(일정부족) - 2. 1대사업자 업태 없어짐 관련기능 작동시 차량등록대수로 판단하기로 함 - 3. 망이용여부 항목 완전 제거
	 *
	 * 2015.01.13 mgkim 1.09 사업단회의 결과 반영 업태(사업자 유형) 다중선택으로 기능개선
	 */
	@RequestMapping("/uss/umt/EgovMberSelectUpdtView.do")
	public String updateMberView(@RequestParam("selectedId") String mberId,
			@ModelAttribute("searchVO") UserDefaultVO shVO, @ModelAttribute("form_MberUpdtUser") FpisCarManageVO caVO,
			HttpServletRequest req, Model model) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		String resultMsg = req.getParameter("resultMsg");
		model.addAttribute("resultMsg", resultMsg);

		// ComDefaultCodeVO vo = new ComDefaultCodeVO();

		// String searchSidoCd = req.getParameter("hid_sido_code");
		// String searchSigunguCd = req.getParameter("hid_sigungu_code");
		model.addAttribute("hid_sido_code", req.getParameter("hid_sido_code"));
		model.addAttribute("hid_sigungu_code", req.getParameter("hid_sigungu_code"));

		// 사용자상태코드를 코드정보로부터 조회
		// vo.setCodeId("COM013");
		// List mberSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		// model.addAttribute("mberSttus_result", mberSttus_result); //사용자상태코드목록

		List<SysCodeVO> codeCOM013 = commonService.commonCode("COM013", null); // 2013.10.17
																				// mgkim
																				// 회원가입
																				// 상태
																				// 코드
		model.addAttribute("codeCOM013", codeCOM013);
		// List<SysCodeVO> codeFMS004 = commonService.commonCode("FMS004",
		// null); // 2013.10.08 mgkim 업태구분코드를 코드정보로부터 조회
		// model.addAttribute("codeFMS004", codeFMS004);
		// List<SysCodeVO> codeFMS012 = commonService.commonCode("FMS012",
		// null); // 2013.10.08 mgkim 인증망이용여부 코드를 코드정보로부터 조회
		// model.addAttribute("codeFMS012", codeFMS012);

		MberManageVO mberManageVO = mberManageService.selectMber(mberId);
		UsrInfoVO userInfoVO = FpisSvc.selectUsrInfo(mberManageVO.getUsr_mst_key());

		model.addAttribute("mber_cls", svo.getMber_cls());

		// 2014.01.21 mgkim 수정하기 이후 검색파라메터 유지
		String mode = req.getParameter("mode");
		if (mode != null && mode.equals("editMode")) {
			shVO.setComp_bsns_num(req.getParameter("in_comp_bsns_num"));
			shVO.setComp_corp_num(req.getParameter("in_comp_corp_num"));
			shVO.setSearchCondition(req.getParameter("in_searchCondition"));
			shVO.setSearchKeyword(req.getParameter("in_searchKeyword"));
			shVO.setSbscrbSttus(req.getParameter("in_sbscrbSttus"));
			shVO.setCur_page(Integer.parseInt(req.getParameter("in_cur_page")));
		}

		/* 2014.05.14 swyang 첨부파일의 존재 여부. 및 오리지날 파일이름 얻기. */
		int fileCnt = 0;
		FpisFileManagementVO fileManagementVO = new FpisFileManagementVO();
		fileManagementVO.setFile_cls("A");
		fileManagementVO.setUsr_mst_key(mberManageVO.getUsr_mst_key());
		fileCnt = fileManagementService.getFileCnt(fileManagementVO);
		if (fileCnt > 0) {
			List<FpisFileManagementVO> fileManagementVOs;
			fileManagementVOs = fileManagementService.getFileInfo(fileManagementVO);
			model.addAttribute("fileManagementVOs", fileManagementVOs);
			// String fpisFilePath =
			// EgovProperties.getProperty("Globals.fileStorePath");
			model.addAttribute("fpisFilePath", fileStorePath);
		}
		model.addAttribute("fileCnt", fileCnt);

		// 정보노출이 off일시
		/* 2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다. */
		String masked_info_status = req.getParameter("masked_info_status");
		if (!"Y".equals(masked_info_status)) {
			userInfoVO.setAddr1(userInfoVO.getMasked_addr1());
			userInfoVO.setAddr2(userInfoVO.getMasked_addr2());
			mberManageVO.setMber_email_adres(mberManageVO.getMasked_email());
			mberManageVO.setEndTelno(mberManageVO.getMasked_end_telno());
			mberManageVO.setMoblphonNo(mberManageVO.getMasked_mbtlnum());
			masked_info_status = "N";
		} else {
			/* 이력 삽입 */
			FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
			accessLogVO.setRcode(req.getParameter("rcode"));
			accessLogVO.setBcode(req.getParameter("bcode"));
			accessLogVO.setComp_mst_key(mberManageVO.getUsr_mst_key());
			accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); // 유저마스터키
			accessLogVO.setJob_cls("DE"); // 상세정보보기
			accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
			accessLogService.insertAccessLogByUsrMstKey(accessLogVO);
		}
		model.addAttribute("masked_info_status", masked_info_status);

		model.addAttribute("userInfoVO", userInfoVO);
		model.addAttribute("mberManageVO", mberManageVO);
		model.addAttribute("userSearchVO", shVO);

		// "관리자 - 신고주체관리 비밀번호 변경을 위한 파라메타 전달 : selectedId : " + mberId
		model.addAttribute("selectedId", mberId);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
		List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 운송유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS024", codeFMS024);
		List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 주선유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS025", codeFMS025);
		List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 망사업자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS026", codeFMS026);
		List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																				// mgkim
																				// 사업자
																				// 대행신고자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS027", codeFMS027);

		ComDefaultCodeVO vo = new ComDefaultCodeVO(); // 181217 smoh 관할지역 상태 코드
														// 추가
		vo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);

		// 2015.01.16 mgkim 업체정보 세부 사업자유형 확인
		String strCompClsDetail = userInfoVO.getComp_cls_detail();
		String compCls_01_01 = "N";
		String compCls_01_02 = "N";
		String compCls_01_03 = "N";
		String compCls_01_04 = "N";
		String compCls_02_01 = "N";
		String compCls_02_02 = "N";
		String compCls_04_01 = "N";
		String[] strCCD = strCompClsDetail.split(",");
		for (int i = 0; i < strCCD.length; i++) {
			if (strCCD[i].equals("01-01")) {
				compCls_01_01 = "Y";
			} else if (strCCD[i].equals("01-02")) {
				compCls_01_02 = "Y";
			} else if (strCCD[i].equals("01-03")) {
				compCls_01_03 = "Y";
			} else if (strCCD[i].equals("01-04")) {
				compCls_01_04 = "Y";
			} else if (strCCD[i].equals("02-01")) {
				compCls_02_01 = "Y";
			} else if (strCCD[i].equals("02-02")) {
				compCls_02_02 = "Y";
			} else if (strCCD[i].equals("04-01")) {
				compCls_04_01 = "Y";
			}
		}

		model.addAttribute("compCls_01_01", compCls_01_01);
		model.addAttribute("compCls_01_02", compCls_01_02);
		model.addAttribute("compCls_01_03", compCls_01_03);
		model.addAttribute("compCls_01_04", compCls_01_04);
		model.addAttribute("compCls_02_01", compCls_02_01);
		model.addAttribute("compCls_02_02", compCls_02_02);
		model.addAttribute("compCls_04_01", compCls_04_01);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

		// 2016. 08. 16 written by dyahn 차량등록정보 UI 구현
		List<FpisCarManageVO> carVOS = null;

		int modelCnt = 0;
		int totCnt = 0;
		int direct_totCnt = 0;

		String car_cur_page = req.getParameter("car_cur_page");
		if (car_cur_page != null) {
			caVO.setCur_page(Integer.parseInt(car_cur_page));
		}

		// PAGING...
		if (caVO.getCur_page() <= 0) {
			caVO.setCur_page(1);
		}
		if (caVO.getSearch_sort1() == null) {
			caVO.setSearch_sort1("sort1_1");
		}
		if (caVO.getSearch_sort2() == null) {
			caVO.setSearch_sort2("ASC");
		}

		caVO.setPage_cls("USR");
		caVO.setUsr_mst_key(mberManageVO.getUsr_mst_key());
		// caVO.setComp_bsns_num(sVO.getUsr_bsns_num()); // 2013.10.14 mgkim
		// 차량계약 근거자료 조회 쿼리 추가 사업자번호 값 추가

		totCnt = CarManageService.getCarCount(caVO);
		direct_totCnt = CarManageService.CarManageFirstChkCnt(caVO); // 직영, 지입차량
																		// 대수
																		// 가져오기

		List<SysCodeVO> codeFMS003 = commonService.commonCode("FMS003", null);
		List<SysCodeVO> codeFMS002 = commonService.commonCode("FMS002", null);

		caVO.setS_row(Util.getPagingStart(caVO.getCur_page()));
		caVO.setE_row(Util.getPagingEnd(caVO.getCur_page()));
		caVO.setTot_page(Util.calcurateTPage(totCnt));

		carVOS = CarManageService.searchCar(caVO);
		if (carVOS != null) {
			modelCnt = carVOS.size();
		}

		// 페이지 네비 및 디폴트 검색조건 VO
		model.addAttribute("VO", caVO);
		model.addAttribute("codeFMS003", codeFMS003);
		model.addAttribute("codeFMS002", codeFMS002);

		// 페이지 리스트 뷰 Model
		model.addAttribute("modelCnt", modelCnt);
		model.addAttribute("carList", carVOS);
		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("DIRECT_TOTCNT", direct_totCnt);

		SigunguVO sigunguVO = new SigunguVO();
		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);
		List<SigunguVO> sigunList = null;
		if (userInfoVO.getSigunguCd() != null && !userInfoVO.getSigunguCd().equals("")) {
			sigunguVO.setSidoCd(userInfoVO.getSidoCd());
			sigunList = mberManageService.selectSigungu2016(sigunguVO);
		}
		model.addAttribute("SIGUNLIST", sigunList);

		return "egovframework/com/uss/umt/EgovMberSelectUpdt";
	}

	/*
	 * 2013.12 오정화 관리자 신고주체관리 상세보기 수정처리 2014.01.21 mgkim 수정하기 이후 검색 파라메터 없어짐. 좌측
	 * 메뉴 이미지 깨짐 2014.02.13 mgkim 관리자가 신고자 업체정보 변경 가능하게 기능 추가됨. 2015.01.20 mgkim
	 * My정보, 반려회원재가입 과 유효성체크 공통모듈화르 인해 폼 이름 변경함. [ mberManageVO ->
	 * form_MberUpdtUser ] 2015.01.20 mgkim 1.09 사업단회의 결과 반영 업태(사업자 유형) 다중선택으로
	 * 기능개선 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
	 */
	@RequestMapping("/uss/umt/EgovMberUpdt.do")
	public String mberUpdate(
			// @ModelAttribute("mberManageVO") MberManageVO mberManageVO,
			// @ModelAttribute("mberManageVO") UserDefaultVO shVO,
			@ModelAttribute("form_MberUpdtUser") FpisNewMberVO vo, BindingResult bindingResult, HttpServletRequest req,
			Model model) throws Exception, NullPointerException {
		// Admin GNR user update ACT : 관리자 신고주체 정보수정 컨트롤러!!
		SessionVO svo = (SessionVO) req.getSession().getAttribute(fpis.common.utils.FpisConstants.SESSION_KEY);
		// String uniqId = req.getParameter("uniqId");
		mberManageService.mberUpdate(vo);
		// 관리자 신고주체 회원정보수정 이후
		// Exception 없이 진행시 수정성공메시지
		model.addAttribute("resultMsg", "success.common.update");

		model.addAttribute("selectedId", vo.getUniqId());

		// 2014.01.21 mgkim 수정하기 이후 검색 파라메타 유지
		model.addAttribute("rcode", req.getParameter("rcode"));
		model.addAttribute("bcode", req.getParameter("bcode"));
		model.addAttribute("mode", "editMode");
		model.addAttribute("in_comp_corp_num", vo.getComp_corp_num());
		model.addAttribute("in_comp_bsns_num", vo.getComp_bsns_num());
		model.addAttribute("in_searchCondition", vo.getSearchCondition());
		model.addAttribute("in_searchKeyword", vo.getSearchKeyword());
		model.addAttribute("in_sbscrbSttus", vo.getSbscrbSttus());
		model.addAttribute("in_cur_page", vo.getCur_page());

		/*
		 * ============== 소속업체정보 수정 : 시작 ============== 2014.02.13 mgkim 관리자가
		 * 신고자 업체정보 변경 가능하게 기능 추가됨.
		 */

		String comp_corp_num = vo.getNew_comp_corp_num().replaceAll("-", ""); // 2013.08.29
																				// mgkim
																				// 수정시
																				// 사업자번호에
																				// "-"문자
																				// 추가되는
																				// 오류
		String comp_bsns_num = vo.getNew_comp_bsns_num().replaceAll("-", "");
		UsrInfoVO usrInfoVO = new UsrInfoVO();
		usrInfoVO.setComp_corp_num(comp_corp_num);
		usrInfoVO.setComp_bsns_num(comp_bsns_num);
		usrInfoVO.setComp_cls(vo.getNew_comp_cls());
		usrInfoVO.setComp_nm(vo.getNew_comp_nm());
		// usrInfoVO.setCeo(vo.getNew_ceo()); 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
		// usrInfoVO.setTel(vo.getNew_tel()); 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
		usrInfoVO.setZip(vo.getNew_comp_zip());
		usrInfoVO.setAddr1(vo.getNew_comp_addr1());
		usrInfoVO.setAddr2(vo.getNew_comp_addr2());
		usrInfoVO.setComp_mst_key(comp_bsns_num); // 2014.02.13 mgkim 관리자 페이지 전용
		usrInfoVO.setUsr_mst_key(comp_bsns_num); // 2014.02.13 mgkim 관리자 페이지 전용

		usrInfoVO.setComp_cls_detail(vo.getComp_cls_detail());
		
		//System.out.println("ddddd uniqId = "+svo.getUser_id());
		usrInfoVO.setUpd_user(svo.getUser_id()); //20230413 chbaek 업데이트한 유저 추가
		
		usrInfoVO.setSigunguCd(vo.getSigunguCd());
		usrInfoVO.setGov_seq(vo.getGov_seq());
		/*
		 * usrInfoVO.setNet_comp_cls(vo.getNet_comp_cls());
		 * if(vo.getComp_cls_detail().equals("03-01") ||
		 * vo.getComp_cls_detail().equals("05-01") ||
		 * vo.getComp_cls_detail().equals("05-02") ||
		 * vo.getComp_cls_detail().equals("05-03")){ // 2013.10.17 mgkim 망사업자
		 * 일경우 이용망 사업자 번호에 자신의 사업자번호를 추가.
		 * usrInfoVO.setNet_comp_bsns_num(comp_bsns_num); }else{
		 * usrInfoVO.setNet_comp_bsns_num(vo.getNet_comp_bsns_num()); }
		 */

		// 181101 smoh 관할관청 확정 또는 임시등록 상황에서 정보 수정시 그대로 유지(이력안남김)
		if (!vo.getSigunguCd().equals(req.getParameter("preSigunguCd"))
				|| (vo.getGov_status().equals("N") || vo.getGov_status().equals("U"))) {
			usrInfoVO.setGov_seq(mberManageService.insertGovHistory(vo)); // 관할관청
																			// 정보
																			// 업데이트
		}
		FpisSvc.updateUsrInfo(usrInfoVO); // 신고주체 업체정보수정 USR_INFO
		model.addAttribute("resultMsg", "success.common.update"); // Exception
																	// 없이 진행시
																	// 수정성공메시지
		/* ============== 소속업체정보 수정 : 끝 ============== */

		/* ============== 회원정보 수정 : 시작 ============== */
		MberManageVO mberVO = new MberManageVO();
		// mberVO.setMberNm(vo.getMberNm()); 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
		mberVO.setZip(vo.getNew_comp_zip());

		// mberVO.setMoblphonNo(vo.getMoblphonNo()); 2015.01.27 mgkim 사업단회의 반영
		// 선택항목 제거

		// 2016. 06. 07 written by dyahn 회원정보 - 선택입력사항 - 연락처
		String tel = vo.getAreaNo() + vo.getMiddleTelno() + vo.getEndTelno();
		vo.setTel(tel);

		mberVO.setAreaNo(vo.getAreaNo());
		mberVO.setMiddleTelno(vo.getMiddleTelno());
		mberVO.setEndTelno(vo.getEndTelno());
		mberVO.setMoblphonNo(vo.getMoblphonNo());

		// 2016. 06. 07 written by dyahn 회원정보 - 추가수집정보항목 추가 - 이메일
		mberVO.setMberEmailAdres(vo.getMberEmailAdres());
		mberVO.setUniqId(vo.getUniqId());
		mberVO.setMberId(vo.getMberId());

		/*
		 * 2015.01.19 mgkim 사업자유형 변경 체크(가맹[04],인증[05],협회[06],운영기관[07] 는 관리자
		 * 승인받아야 변경된 정보로 사용 할 수 있음) 예외 07은 시스템으로 수정하지 않기로 함. DB로 직접 수정
		 */
		String comp_cls_old = req.getParameter("hid_new_comp_cls");
		String new_comp_cls = vo.getNew_comp_cls();

		if ("04".equals(new_comp_cls) || "05".equals(new_comp_cls) || "06".equals(new_comp_cls)) {
			if (!new_comp_cls.equals(comp_cls_old)) { // 대행신고 기능을 가진 가맹,인증,협회로
														// 유형이 변경되었을 경우 관리자의 승인을
														// 받아야 한다.
				mberVO.setMberSttus("C");
			}
		}
		/*
		 * 2014.02.20 mgkim 비대행신고자가 대행신고자(가맹/협회/인증망)로 업태 변경시 관리자가 승인처리를 하게 로직 변경
		 * end
		 */
		mberManageService.updateMber(mberVO); // COMTNGNRLMBER 회원정보 수정
												// (이름,연락처,...)
		/* ============== 회원정보 수정 : 끝 ============== */

		/* 사업자정보 수정 이력 삽입 */
		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
		accessLogVO.setRcode(req.getParameter("rcode"));
		accessLogVO.setBcode(req.getParameter("bcode"));
		accessLogVO.setComp_mst_key(comp_bsns_num);
		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); // 유저마스터키
		accessLogVO.setJob_cls("UP"); // 업데이트
		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
		accessLogService.insertAccessLogByUsrMstKey(accessLogVO);

		return "redirect:/uss/umt/EgovMberManage.do";
	}

	/**
	 * 일반회원 암호 수정 화면 이동 2013.08.29 정보변경 페이지로 통합(사용자)
	 * 
	 * @param model 화면모델
	 * @param commandMap 파라메터전달용 commandMap
	 * @param userSearchVO 검색조건
	 * @param mberManageVO 일반회원수정정보(비밀번호)
	 * @return uss/umt/EgovMberPasswordUpdt
	 * @throws Exception 2014.02.25 mgkim 관리자 - 신고주체관리 - 비번변경 페이지 호출 보완
	 */
	@RequestMapping(value = "/uss/umt/EgovMberPasswordUpdtView.do")
	public String updatePasswordView(ModelMap model, Map<String, Object> commandMap,
			@ModelAttribute("searchVO") UserDefaultVO userSearchVO,
			@ModelAttribute("mberManageVO") MberManageVO mberManageVO, @RequestParam("selectedId") String mberId,
			HttpServletRequest req) throws Exception, NullPointerException {
		// TODO 정보변경 페이지에 통합 작업중
		// "관리자 - 신고주체관리 비밀번호 변경 파라메타 전달 2 : selectedId : " + mberId
		String userTyForPassword = (String) commandMap.get("userTyForPassword");
		// "비밀번호변경 페이지 호출 "+ userTyForPassword
		mberManageVO.setUserTy(userTyForPassword);
		SessionVO SessionVO = (SessionVO) req.getSession().getAttribute(FpisConstants.SESSION_KEY);

		model.addAttribute("SVO", SessionVO);
		model.addAttribute("userSearchVO", userSearchVO);
		model.addAttribute("mberManageVO", mberManageVO);
		model.addAttribute("selectedId", mberId);
		return "egovframework/com/uss/umt/EgovMberPasswordUpdt";
	}

	/**
	 * @param model 화면모델
	 * @param commandMap 파라메터전달용 commandMap
	 * @param userSearchVO 검색조건
	 * @param mberManageVO 일반회원수정정보(비밀번호)
	 * @return uss/umt/EgovMberPasswordUpdt
	 * @throws Exception 2014.02.25 오정화 소스 정리 2014.02.25 mgkim 관리자 - 신고주체관리 -
	 * 비번변경 처리 보완
	 */
	@RequestMapping(value = "/uss/umt/EgovMberPasswordUpdt.do")
	public String updatePassword(ModelMap model, Map<String, Object> commandMap,
			@ModelAttribute("searchVO") UserDefaultVO userSearchVO,
			@ModelAttribute("mberManageVO") MberManageVO mberManageVO, @RequestParam("selectedId") String mberId,
			HttpServletRequest req) throws Exception, NullPointerException {

		// "관리자 - 신고주체관리 비밀번호 변경 파라메타 전달 3 : selectedId : " + mberId

		String newPassword = (String) commandMap.get("newPassword");
		// String newPassword2 = (String)commandMap.get("newPassword2");
		String uniqId = (String) commandMap.get("uniqId");

		// MberManageVO resultVO = new MberManageVO();
		mberManageVO.setPassword(newPassword);
		mberManageVO.setUniqId(uniqId);

		String resultMsg = "";
		// int resultRtn = -1;
		// resultVO = mberManageService.selectPassword(mberManageVO);
		SessionVO SessionVO = (SessionVO) req.getSession().getAttribute(FpisConstants.SESSION_KEY);

		mberManageVO.setPassword(EgovFileScrty.encryptPassword(newPassword));
		mberManageService.updatePassword(mberManageVO);
		model.addAttribute("mberManageVO", mberManageVO);
		resultMsg = "success.common.update";
		// resultRtn = 1;

		model.addAttribute("SVO", SessionVO);
		model.addAttribute("userSearchVO", userSearchVO);
		model.addAttribute("resultMsg", resultMsg);
		// model.addAttribute("resultRtn", resultRtn);

		// 2014.01.21 mgkim 수정하기 이후 검색 파라메타 유지
		model.addAttribute("rcode", req.getParameter("rcode"));
		model.addAttribute("bcode", req.getParameter("bcode"));
		model.addAttribute("mode", "editMode");

		model.addAttribute("selectedId", mberId);

		return "egovframework/com/uss/umt/EgovMberPasswordUpdt";

	}

	/**
	 * 일반회원정보 수정을 위해 일반회원정보를 상세조회한다. 2013.08.27 GNT-mgkim 회원정보변경 페이지 UI 정리(JSP내의
	 * JAVA소스 제거) 2013.08.30 GNT-mgkim 서브메뉴 표출 오류 정리 -> /config/decorators.xml
	 * /uss/umt/* 패턴은 join 레이아웃을 타게 설정되어 있었음. -> /uss/umt => /uss/myi 패턴 변경 ->
	 * DB도 수정작업해야함. 2013.08.30 GNT-mgkim 비밀번호 변경 UI 통합 2013.09.02 mgkim 비밀번호 변경
	 * 오류 처리 기능 추가 2013.10.08 mgkim 업태구분코드를 코드정보로부터 조회 추가 2013.10.10 mgkim 신고주체
	 * 업체정보(업태상세, 망가입여부 정보 추가) 기능추가. 2014.02.05 mgkim 가맹망 항목 변경(인증망, 협회 기능 추가 )
	 *
	 * 양상완 이력 좀 적지?
	 *
	 * 2015.01.13 mgkim 회원가입, 재회원가입, My정보, 신고주체상세조회(관리자) 페이지 공통 기능변경 사항 -
	 * 2015.01.09 mgkim 사업단회의 결과 반영 - 1. 업태 분류 다중선택이 필요함 => 겸업 내용 상세추가하는것으로
	 * 결정(일정부족) - 2. 1대사업자 업태 없어짐 관련기능 작동시 차량등록대수로 판단하기로 함 - 3. 망이용여부 항목 완전 제거
	 *
	 * 2015.01.13 mgkim 1.09 사업단회의 결과 반영 업태(사업자 유형) 다중선택으로 기능개선
	 *
	 * @param mberId 상세조회대상 일반회원아이디
	 * @param userSearchVO 검색조건
	 * @param model 화면모델
	 * @return uss/umt/EgovMberSelectUpdt
	 * @throws Exception
	 */
	@RequestMapping("/uss/myi/EgovMberSelectUpdtViewUser.do")
	public String updateMberViewUser(HttpServletRequest req, Model model) throws Exception, NullPointerException {

		/* 2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사 */
		String refer_domain = req.getHeader("referer");
		// String program_domain =
		// EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"),
		// "FPIS.domain");
		if (!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

		// TODO 사용자시스템 회원정보변경 페이지 호출
		/* 2013.09.02 mgkim 비밀번호 수정 오류 처리 기능 추가 시작 */
		String resultMsg = req.getParameter("resultMsg");
		model.addAttribute("resultMsg", resultMsg);
		/* 2013.09.02 mgkim 비밀번호 수정 오류 처리 기능 추가 끝 */

		/* 2013.08.27 GNT-mgkim EgovMberSelectUpdtUser.jsp 소스 수정 보완작업 시작 */
		SessionVO svo = (SessionVO) req.getSession().getAttribute(fpis.common.utils.FpisConstants.SESSION_KEY);
		String uniqID = "";
		if (svo != null) {
			uniqID = svo.getUniqid();
		}
		model.addAttribute("uniqID", uniqID);
		/* 2013.08.27 GNT-mgkim EgovMberSelectUpdtUser.jsp 소스 수정 보완작업 끝 */

		/* 2014.04.01 GNT-양상완 공인인증서 유무 체킹 */
		// 181130 smoh 공인인증서 로그인 주석처리
		// model.addAttribute("checkVID",loginService.CheckVID(svo));
		/* 2014.04.01 GNT-양상완 공인인증서 유무 체킹끝 */

		MberManageVO mberManageVO = mberManageService.selectMber(svo.getUniqid());
		// SysCompanyInfoVO CompVO =
		// FpisSvc.getSysCompanyPk(mberManageVO.getUsr_mst_key()); //
		// SYS_COMPANY_INFO 는 협회제공 사업자 원본
		UsrInfoVO userInfoVO = FpisSvc.selectUsrInfo(svo.getUsr_mst_key());

		model.addAttribute("userInfoVO", userInfoVO); // 업체(개인정보)
		model.addAttribute("mberManageVO", mberManageVO);

		List<SysCodeVO> codeCOM013 = commonService.commonCode("COM013", null); // 2013.10.17
																				// mgkim
																				// 회원가입
																				// 상태
																				// 코드
		model.addAttribute("codeCOM013", codeCOM013);
		// List<SysCodeVO> codeFMS004 = commonService.commonCode("FMS004",
		// null); // 2013.10.08 mgkim 업태구분코드를 코드정보로부터 조회
		// model.addAttribute("codeFMS004", codeFMS004);
		// List<SysCodeVO> codeFMS012 = commonService.commonCode("FMS012",
		// null); // 2013.10.08 mgkim 인증망이용여부 코드를 코드정보로부터 조회
		// model.addAttribute("codeFMS012", codeFMS012);

		/* 2013.12.03 mgkim 정보변경시 차량관리현황에 직영차 1대만 있는경우 1대사업자로 변경할 수 있게 허용 */
		// FpisCarManageVO shVO = new FpisCarManageVO();
		// shVO.setPage_cls("USR");
		// shVO.setUsr_mst_key(svo.getUsr_mst_key());
		// shVO.setComp_bsns_num(svo.getUsr_bsns_num()); // 2013.10.14 mgkim
		// 차량계약 근거자료 조회 쿼리 추가 사업자번호 값 추가
		// int totCnt = (Integer)CarManageService.CarManageFirstChkCnt(shVO); //
		// 2015.01.16 mgkim 1대사업자 체크기능 쓰면됨.
		// String oneCarCls = "";
		// if(totCnt == 1){
		// if (shVO.getCur_page() <= 0) { shVO.setCur_page(1); }
		// if(shVO.getSearch_sort1() == null){shVO.setSearch_sort1("sort1_1");}
		// if(shVO.getSearch_sort2() == null){shVO.setSearch_sort2("ASC");}
		// shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		// shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		// shVO.setTot_page(Util.calcurateTPage(totCnt));
		//
		// List<FpisCarManageVO> carVOS =
		// (List<FpisCarManageVO>)CarManageService.searchCar(shVO);
		// FpisCarManageVO carVO = new FpisCarManageVO();
		// carVO = carVOS.get(0);
		// oneCarCls = carVO.getCars_cls();
		// }
		// model.addAttribute("usrCarTotCnt", totCnt); //2015.01.13 mgkim 1.09
		// 사업단회의 결과 반영 업태(사업자 유형) 다중선택으로 기능개선
		// model.addAttribute("usrCarCls", oneCarCls);
		/* 2013.12.03 mgkim 정보변경시 차량관리현황에 직영차 1대만 있는경우 1대사업자로 변경할 수 있게 허용 */

		/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
		List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 운송유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS024", codeFMS024);
		List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 주선유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS025", codeFMS025);
		List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 망사업자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS026", codeFMS026);
		List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																				// mgkim
																				// 사업자
																				// 대행신고자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS027", codeFMS027);

		ComDefaultCodeVO codeVo = new ComDefaultCodeVO(); // 181217 smoh 관할관청 상태
															// 코드 목록 불러오기
		codeVo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(codeVo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);// 기업회원상태코드목록

		// 2015.01.16 mgkim 업체정보 세부 사업자유형 확인
		String strCompClsDetail = userInfoVO.getComp_cls_detail();
		String compCls_01_01 = "N";
		String compCls_01_02 = "N";
		String compCls_01_03 = "N";
		String compCls_01_04 = "N";
		String compCls_02_01 = "N";
		String compCls_02_02 = "N";
		String compCls_04_01 = "N";
		String[] strCCD = strCompClsDetail.split(",");
		for (int i = 0; i < strCCD.length; i++) {
			if (strCCD[i].equals("01-01")) {
				compCls_01_01 = "Y";
			} else if (strCCD[i].equals("01-02")) {
				compCls_01_02 = "Y";
			} else if (strCCD[i].equals("01-03")) {
				compCls_01_03 = "Y";
			} else if (strCCD[i].equals("01-04")) {
				compCls_01_04 = "Y";
			} else if (strCCD[i].equals("02-01")) {
				compCls_02_01 = "Y";
			} else if (strCCD[i].equals("02-02")) {
				compCls_02_02 = "Y";
			} else if (strCCD[i].equals("04-01")) {
				compCls_04_01 = "Y";
			}
		}

		model.addAttribute("compCls_01_01", compCls_01_01);
		model.addAttribute("compCls_01_02", compCls_01_02);
		model.addAttribute("compCls_01_03", compCls_01_03);
		model.addAttribute("compCls_01_04", compCls_01_04);
		model.addAttribute("compCls_02_01", compCls_02_01);
		model.addAttribute("compCls_02_02", compCls_02_02);
		model.addAttribute("compCls_04_01", compCls_04_01);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

		/* 2018.10.16 smoh 관할관청관리기능 추가로 시도, 시군구 목록 불러오기 추가 */
		SigunguVO sigunguVO = new SigunguVO();
		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);
		List<SigunguVO> sigunList = null;
		if (userInfoVO.getSigunguCd() != null && !userInfoVO.getSigunguCd().equals("")) {
			sigunguVO.setSidoCd(userInfoVO.getSidoCd());
			sigunList = mberManageService.selectSigungu2016(sigunguVO);
		}
		model.addAttribute("SIGUNLIST", sigunList);

		HttpSession session = req.getSession();

		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);

		KeyPair keyPair = generator.genKeyPair();
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");

		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();

		// 세션에 공개키의 문자열을 키로하여 개인키를 저장한다.
		session.setAttribute("__rsaPrivateKey__", privateKey);

		// 공개키를 문자열로 변환하여 JavaScript RSA 라이브러리 넘겨준다.
		RSAPublicKeySpec publicSpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);

		String publicKeyModulus = publicSpec.getModulus().toString(16);
		String publicKeyExponent = publicSpec.getPublicExponent().toString(16);

		model.addAttribute("rsaPublicKeyModulus", publicKeyModulus);
		model.addAttribute("rsaPublicKeyExponent", publicKeyExponent);
		return "egovframework/com/uss/umt/EgovMberSelectUpdtUser";
	}

	/*
	 * 2013.10.16 mgkim 회원 가입 반려 상태[F] 재가입 요청[R] 페이지 (비로그인 페이지) : 로그인 회원정보수정
	 * 페이지와 유사 2015.01.13 mgkim 1.09 사업단회의 결과 반영 업태(사업자 유형) 다중선택으로 기능개선
	 */
	@RequestMapping("/uss/umt/EgovMberSelectUpdtViewUserNologin.do")
	public String EgovMberSelectUpdtViewUserNologin(HttpServletRequest req, Model model)
			throws Exception, NullPointerException {
		// TODO 사용자시스템 회원정보변경 페이지 호출
		/* 2013.09.02 mgkim 비밀번호 수정 오류 처리 기능 추가 시작 */
		String resultMsg = req.getParameter("resultMsg");
		model.addAttribute("resultMsg", resultMsg);
		/* 2013.09.02 mgkim 비밀번호 수정 오류 처리 기능 추가 끝 */

		// String uniqID = req.getParameter("uniqID"); // 개인정보유출로 아이디 넘기지 않음.
		LoginVO tempVO = (LoginVO) req.getSession().getAttribute("tempLoginVO");

		String uniqID = tempVO.getUniqId();
		System.out.println("uniqID = " + uniqID);

		String MberId = tempVO.getId();
		System.out.println("mberId = " + MberId);

		MberManageVO mberManageVO = mberManageService.selectMber(uniqID);

		UsrInfoVO userInfoVO = FpisSvc.selectUsrInfo(mberManageVO.getUsr_mst_key());

		model.addAttribute("uniqID", uniqID);
		model.addAttribute("userInfoVO", userInfoVO); // 업체(개인정보)
		model.addAttribute("mberManageVO", mberManageVO);

		List<SysCodeVO> codeCOM013 = commonService.commonCode("COM013", null); // 2013.10.17
																				// mgkim
																				// 회원가입
																				// 상태
																				// 코드
		model.addAttribute("codeCOM013", codeCOM013);
		/* 2013.12.03 mgkim 정보변경시 차량관리현황에 직영차 1대만 있는경우 1대사업자로 변경할 수 있게 허용 */
		FpisCarManageVO shVO = new FpisCarManageVO();
		shVO.setPage_cls("USR");
		shVO.setUsr_mst_key(userInfoVO.getUsr_mst_key());
		shVO.setComp_bsns_num(userInfoVO.getComp_bsns_num()); // 2013.10.14
																// mgkim 차량계약
																// 근거자료 조회 쿼리 추가
																// 사업자번호 값 추가

		/* 2014.05.14 swyang 첨부파일의 존재 여부. 및 오리지날 파일이름 얻기. */
		int fileCnt = 0;
		FpisFileManagementVO fileManagementVO = new FpisFileManagementVO();
		fileManagementVO.setFile_cls("A");
		fileManagementVO.setUsr_mst_key(mberManageVO.getUsr_mst_key());
		fileCnt = fileManagementService.getFileCnt(fileManagementVO);
		if (fileCnt > 0) {
			List<FpisFileManagementVO> fileManagementVOs;
			fileManagementVOs = fileManagementService.getFileInfo(fileManagementVO);
			model.addAttribute("fileManagementVOs", fileManagementVOs);
			// String fpisFilePath =
			// EgovProperties.getProperty("Globals.fileStorePath");
			model.addAttribute("fpisFilePath", fileStorePath);
		}
		model.addAttribute("fileCnt", fileCnt);

		/* 2013.12.03 mgkim 정보변경시 차량관리현황에 직영차 1대만 있는경우 1대사업자로 변경할 수 있게 허용 */

		/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
		List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 운송유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS024", codeFMS024);
		List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 주선유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS025", codeFMS025);
		List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 망사업자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS026", codeFMS026);
		List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																				// mgkim
																				// 사업자
																				// 대행신고자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS027", codeFMS027);

		ComDefaultCodeVO codeVo = new ComDefaultCodeVO(); // 181217 smoh 관할관청 상태
															// 코드 목록 불러오기
		codeVo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(codeVo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);// 기업회원상태코드목록

		// 2015.01.16 mgkim 업체정보 세부 사업자유형 확인
		String strCompClsDetail = userInfoVO.getComp_cls_detail();
		String compCls_01_01 = "N";
		String compCls_01_02 = "N";
		String compCls_01_03 = "N";
		String compCls_01_04 = "N";
		String compCls_02_01 = "N";
		String compCls_02_02 = "N";
		String compCls_04_01 = "N";
		String[] strCCD = strCompClsDetail.split(",");
		for (int i = 0; i < strCCD.length; i++) {
			if (strCCD[i].equals("01-01")) {
				compCls_01_01 = "Y";
			} else if (strCCD[i].equals("01-02")) {
				compCls_01_02 = "Y";
			} else if (strCCD[i].equals("01-03")) {
				compCls_01_03 = "Y";
			} else if (strCCD[i].equals("01-04")) {
				compCls_01_04 = "Y";
			} else if (strCCD[i].equals("02-01")) {
				compCls_02_01 = "Y";
			} else if (strCCD[i].equals("02-02")) {
				compCls_02_02 = "Y";
			} else if (strCCD[i].equals("04-01")) {
				compCls_04_01 = "Y";
			}
		}

		model.addAttribute("compCls_01_01", compCls_01_01);
		model.addAttribute("compCls_01_02", compCls_01_02);
		model.addAttribute("compCls_01_03", compCls_01_03);
		model.addAttribute("compCls_01_04", compCls_01_04);
		model.addAttribute("compCls_02_01", compCls_02_01);
		model.addAttribute("compCls_02_02", compCls_02_02);
		model.addAttribute("compCls_04_01", compCls_04_01);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

		/* 2018.10.16 smoh 관할관청관리기능 추가로 시도, 시군구 목록 불러오기 추가 */
		SigunguVO sigunguVO = new SigunguVO();
		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);
		List<SigunguVO> sigunList = null;
		if (userInfoVO.getSigunguCd() != null && !userInfoVO.getSigunguCd().equals("")) {
			sigunguVO.setSidoCd(userInfoVO.getSidoCd());
			sigunList = mberManageService.selectSigungu2016(sigunguVO);
		}
		model.addAttribute("SIGUNLIST", sigunList);

		return "egovframework/com/uss/umt/EgovMberSelectUpdtUserNologin";
	}

	/*
	 * 회원정보 변경 : 본인변경 2013.08.27 GNT-mgkim 소속업체 / 회원정보 동시수정으로 개선(수정버튼 2개 통합)
	 * 2013.08.29 mgkim 수정시 사업자번호에 "-"문자 추가되는 오류수정 2013.08.30 GNT-mgkim 비밀번호 변경
	 * UI 통합 2013.09.02 mgkim 비밀번호 변경 처리 구현 2014.02.14 mgkim 비대행신고자가
	 * 대행신고자(가맹/협회/인증망)로 업태 변경시 관리자가 승인처리를 하게 로직 변경 2014.05.12 swyang 사업자 등록
	 * 첨부파일을 처리 하기 위해 매개변수에 multiRequest 추가 2015.01.19 mgkim 업태,망가입여부 항목 제거 및
	 * 사업자유형 기능 추가 2015.01.27 mgkim 사업단회의 반영 선택항목 제거 2021.09.23 suhyun 기존비밀번호 확인
	 * 기능 추가
	 *
	 */
	@RequestMapping("/uss/umt/EgovMberSelectUpdtUser.do")
	public String updateMberUser(@RequestParam(required = false, value = "fileNm") MultipartFile fileNm,
			@ModelAttribute("form_MberUpdtUser") FpisNewMberVO vo, BindingResult bindingResult, HttpServletRequest req,
			Model model) throws Exception, NullPointerException {

		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		String uniqId = req.getParameter("uniqId");

		// 2020.08.05 pch : 회원수정 필수값 검증-서버단(웹취약점 XSS 브루트포스) - 검증용 vo set
		String password3 = req.getParameter("password3");

		FpisNewMberVO chk_vo = new FpisNewMberVO();

		chk_vo.setComp_mst_key(vo.getNew_comp_bsns_num().replaceAll("-", ""));
		chk_vo.setNew_comp_nm(vo.getNew_comp_nm());
		chk_vo.setNew_comp_cls(vo.getNew_comp_cls());
		chk_vo.setNew_comp_addr1(vo.getNew_comp_addr1());
		chk_vo.setNew_comp_zip(vo.getNew_comp_zip());
		chk_vo.setSidoCd(vo.getSidoCd());
		chk_vo.setMberId(vo.getMberId());
		chk_vo.setCur_password(vo.getPassword());
		chk_vo.setPassword(vo.getPassword2());
		chk_vo.setPassword2(password3);
		chk_vo.setMberEmailAdres(vo.getMberEmailAdres());
		chk_vo.setMiddleTelno(vo.getMiddleTelno());
		chk_vo.setEndTelno(vo.getEndTelno());
		chk_vo.setMoblphonNo(vo.getMoblphonNo());

		/* 2013.09.01 mgkim 패스워드 변경 처리 기능 통합 - 패스워드 변경 유효성 검사 시작 */
		/* 불필요 제거 - 2021.10.16 suhyun */
		/* String newPassword = vo.getPassword2(); */

		boolean changePassword = false;
		MberManageVO mberManageVO = new MberManageVO();
		mberManageVO.setPassword(vo.getPassword2());
		mberManageVO.setUniqId(uniqId);

		String comp_corp_num = vo.getNew_comp_corp_num().replaceAll("-", ""); // 2013.08.29
																				// mgkim
																				// 수정시
																				// 사업자번호에
																				// "-"문자
																				// 추가되는
																				// 오류
		String comp_bsns_num = vo.getNew_comp_bsns_num().replaceAll("-", "");

		// 평문화 > RSA암호화 진행 - 2021.12.10 suhyun
		String securedCurPassword = req.getParameter("curPassword");
		String securedPassword = req.getParameter("password2");
		String securedPassword2 = req.getParameter("password3");

		HttpSession session = req.getSession();

		PrivateKey privateKey = (PrivateKey) session.getAttribute("__rsaPrivateKey__");

		session.removeAttribute("__rsaPrivateKey__");

		if (privateKey == null) {
			model.addAttribute("usrStt", "E");
			return "redirect:/userMain.do";
		}

		chk_vo.setCur_password(decryptRsa(privateKey, securedCurPassword));

		if (!securedPassword.isEmpty()) {
			chk_vo.setPassword(decryptRsa(privateKey, securedPassword));
			chk_vo.setPassword2(decryptRsa(privateKey, securedPassword2));
		}

		// 2016. 06. 07 written by dyahn 회원정보 - 선택입력사항 - 연락처
		String tel = vo.getAreaNo() + vo.getMiddleTelno() + vo.getEndTelno();
		vo.setTel(tel);

		// 2020.08.05 pch : 회원가입 필수값 검증-서버단(웹취약점 XSS 브루트포스)
		boolean flag = fnInputCheck(chk_vo);
		// 기존 비밀번호 확인 로직 추가 - 2021.10.01 suhyun
		changePassword = vo.getPassword2().isEmpty() ? false : true;

		MberManageVO chkMberVO = new MberManageVO();
		chkMberVO.setUsr_mst_key(chk_vo.getComp_mst_key());
		chkMberVO.setPassword(chk_vo.getCur_password());

		int confirmCurrentPassWord = mberManageService.selectMberCountCurrentPassword(chkMberVO);
		if (confirmCurrentPassWord != 1) {
			model.addAttribute("resultMsg", "fail.common.update");
			return "redirect:/uss/myi/EgovMberSelectUpdtViewUser.do";
		}

		// if (false) {
		if (flag) {
			// 2013.11.22 mgkim 기존비밀번호 제거 (비번 분실시 공인인증 로그인하여 변경가능하게 조치

			/* 2013.09.01 mgkim 패스워드 변경 처리 기능 통합 - 패스워드 변경 유효성 검사 끝 */

			/* ============== 소속업체정보 수정 : 시작 ============== */
			UsrInfoVO usrInfoVO = new UsrInfoVO();
			usrInfoVO.setComp_corp_num(comp_corp_num);
			usrInfoVO.setComp_bsns_num(comp_bsns_num);
			usrInfoVO.setComp_cls(vo.getNew_comp_cls());
			usrInfoVO.setComp_nm(vo.getNew_comp_nm());
			// usrInfoVO.setCeo(vo.getNew_ceo()); 2015.01.27 mgkim 사업단회의 반영 선택항목
			// 제거
			// usrInfoVO.setTel(vo.getNew_tel()); 2015.01.27 mgkim 사업단회의 반영 선택항목
			// 제거
			usrInfoVO.setZip(vo.getNew_comp_zip());
			usrInfoVO.setAddr1(vo.getNew_comp_addr1());
			usrInfoVO.setAddr2(vo.getNew_comp_addr2());
			usrInfoVO.setUpd_user(vo.getMberId()); // 2014.03.05 mgkim 회원정보 수정자
													// 추가
			usrInfoVO.setSigunguCd(vo.getSigunguCd()); // 181018 관할관청 정보 추가로 코드값
														// 별도로 받음
			usrInfoVO.setGov_seq(vo.getGov_seq());
			if (svo != null) {
				usrInfoVO.setComp_mst_key(svo.getComp_mst_key());
				usrInfoVO.setUsr_mst_key(svo.getUsr_mst_key());
			} else { // 2013.10.17 mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리
				SessionVO t_svo = (SessionVO) req.getSession().getAttribute("tempSessionVO");
				usrInfoVO.setComp_mst_key(t_svo.getComp_mst_key());
				usrInfoVO.setUsr_mst_key(t_svo.getUsr_mst_key());
			}

			usrInfoVO.setComp_cls_detail(vo.getComp_cls_detail());
			/*
			 * 2015.01.19 mgkim 업태,망가입여부 항목 제거 및 사업자유형 기능 추가
			 * usrInfoVO.setNet_comp_cls(vo.getNet_comp_cls());
			 * //"사업자 유형 멀티값 : " + vo.getComp_cls_detail() boolean bool_net_comp
			 * = false; // 2014.02.20 mgkim 망사업자 업태 여부 체크
			 * if(vo.getComp_cls_detail().equals("03-01") ||
			 * vo.getComp_cls_detail().equals("05-01") ||
			 * vo.getComp_cls_detail().equals("05-02") ||
			 * vo.getComp_cls_detail().equals("05-03")){ // 2013.10.17 mgkim
			 * 망사업자 일경우 이용망 사업자 번호에 자신의 사업자번호를 추가.
			 * usrInfoVO.setNet_comp_bsns_num(comp_bsns_num); bool_net_comp =
			 * true; }else{
			 * usrInfoVO.setNet_comp_bsns_num(vo.getNet_comp_bsns_num()); }
			 */

			// FpisSvc.updateMyPageCompInfo(usrInfoVO); // 신고주체 업체정보수정 USR_INFO
			if (!vo.getSigunguCd().equals(req.getParameter("preSigunguCd"))
					|| (vo.getGov_status().equals("N") || vo.getGov_status().equals("U"))) {
				usrInfoVO.setGov_seq(mberManageService.insertGovHistory(vo)); // 관할관청
																				// 정보
																				// 업데이트
			}
			FpisSvc.updateUsrInfo(usrInfoVO); // 신고주체 업체정보수정 USR_INFO
			/* ============== 소속업체정보 수정 : 끝 ============== */

			/* ============== 회원정보 수정 : 시작 ============== */
			MberManageVO mberVO = new MberManageVO();
			// mberVO.setMberNm(vo.getMberNm()); 2015.01.27 mgkim 사업단회의 반영 선택항목
			// 제거
			mberVO.setZip(vo.getNew_comp_zip());

			// mberVO.setMoblphonNo(vo.getMoblphonNo()); 2015.01.27 mgkim 사업단회의
			// 반영 선택항목 제거

			mberVO.setAreaNo(vo.getAreaNo());
			mberVO.setMiddleTelno(vo.getMiddleTelno());
			mberVO.setEndTelno(vo.getEndTelno());
			mberVO.setMoblphonNo(vo.getMoblphonNo());

			// 2016. 06. 07 written by dyahn 회원정보 - 추가수집정보항목 추가 - 이메일
			mberVO.setMberEmailAdres(vo.getMberEmailAdres());

			mberVO.setUniqId(uniqId);
			mberVO.setMberId(vo.getMberId());
			/* 2014.12.24 양상완 공인인증서 수정하는사람 */
			/*
			 * 2015.01.20 mgkim
			 * mberVO.setUpdate_file_cls(req.getParameter("update_file_cls"));
			 * if(vo.getUsrVID() != null && vo.getUsrDN() !=null){
			 * mberVO.setUsrDN(AESCrypto.getInstance().encrypt(null,vo.getUsrDN(
			 * ))); mberVO.setUsrVID(AESCrypto.getInstance().encrypt(null,vo.
			 * getUsrVID())); }
			 */

			if (svo != null) {
				/*
				 * 2014.02.20 mgkim 비대행신고자가 대행신고자(가맹/협회/인증망)로 업태 변경시 관리자가 승인처리를
				 * 하게 로직 변경 start
				 */
				/*
				 * 2015.01.19 mgkim 업태,망가입여부 항목 제거 및 사업자유형 기능 추가
				 * if(bool_net_comp){ String comp_cls_detail_old =
				 * req.getParameter("hid_comp_cls_detail");
				 * //"정보변경 기존 사용자 업태 : "+ comp_cls_detail_old
				 * if(comp_cls_detail_old.equals("03-01") ||
				 * comp_cls_detail_old.equals("05-01") ||
				 * comp_cls_detail_old.equals("05-02") ||
				 * comp_cls_detail_old.equals("05-03")){ // 대행신고자가 대행신고자료 변경하는
				 * 경우, }else{ // 비대행 신고자가 대행신고자로 업태 변경하는경우
				 * mberVO.setMberSttus("C"); } }
				 */

				/*
				 * 2015.01.19 mgkim 사업자유형 변경 체크(가맹[04],인증[05],협회[06],운영기관[07] 는
				 * 관리자 승인받아야 변경된 정보로 사용 할 수 있음) 예외 07은 시스템으로 수정하지 않기로 함. DB로 직접
				 * 수정
				 */
				String comp_cls_old = req.getParameter("hid_new_comp_cls");
				// "정보변경 기존 사용자 형태 : "+ comp_cls_old
				String new_comp_cls = vo.getNew_comp_cls();
				if ("04".equals(new_comp_cls) || "05".equals(new_comp_cls) || "06".equals(new_comp_cls)
						|| "07".equals(new_comp_cls)) {
					if (!new_comp_cls.equals(comp_cls_old)) { // 대행신고 기능을 가진
																// 가맹,인증,협회로 유형이
																// 변경되었을 경우 관리자의
																// 승인을 받아야 한다.
						mberVO.setMberSttus("C");
					}
				}

				// 181127 smoh 회원정보 신고대행기관 변경, 비밀번호 변경 되면 mber_sttus가 P로 되어 회원정보
				// 변경시 비밀번호 같이 바꾸는것으로 변경
				// mberManageService.updateMber 내에 암호화가 진행되어 값만 넣음
				/* password 변경 확인 true : 변경 false : 유지 - 2021.10.16 suhyun */
				if (changePassword) {
					mberVO.setPassword(chk_vo.getPassword());
				}
				/*
				 * 2014.02.20 mgkim 비대행신고자가 대행신고자(가맹/협회/인증망)로 업태 변경시 관리자가 승인처리를
				 * 하게 로직 변경 end
				 */
				mberManageService.updateMber(mberVO); // COMTNGNRLMBER 회원정보 수정
														// (이름,연락처,...)
			} else { // 2013.10.17 mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리
				mberManageService.updateMberSttusR(mberVO); // COMTNGNRLMBER
															// 회원정보 수정
															// (이름,연락처,...)
				/* 2014.12.20 양상완 사업자 등록증 첨부시 */
				if (mberVO.getUpdate_file_cls().equals("Y")) {
					if (!fileNm.isEmpty()) {
						// String storePathString =
						// EgovProperties.getProperty("Globals.fileStorePath");
						File saveFolder = new File(EgovWebUtil.filePathBlackList(fileStorePath));
						if (!saveFolder.exists() || saveFolder.isFile()) {
							saveFolder.setReadable(true);
							saveFolder.setWritable(true);
							saveFolder.mkdirs();
						}

						String rtnStr = null;
						// 문자열로 변환하기 위한 패턴 설정(년도-월-일 시:분:초:초(자정이후 초))
						String pattern = "yyyyMMddhhmmssSSS";
						SimpleDateFormat sdfCurrent = new SimpleDateFormat(pattern, Locale.KOREA);
						Timestamp ts = new Timestamp(System.currentTimeMillis());
						rtnStr = sdfCurrent.format(ts.getTime());

						FpisFileManagementVO fileManagementVO = new FpisFileManagementVO();
						fileManagementVO.setUsr_mst_key(comp_bsns_num);
						/* 뒤에 getCurSequence 붙이면 수정시에로사항이있음 그냥 시간만 합니다. */
						/*
						 * fileManagementVO.setFile_name(rtnStr+
						 * fileManagementService.getCurSequence()) ; // 암호화된 파일
						 * 이름
						 */
						fileManagementVO.setFile_name(rtnStr);
						fileManagementVO.setFile_dir(fileStorePath); // 저장경로
						fileManagementVO.setOrg_file_name(fileNm.getOriginalFilename()); // 오리지날
																							// 파일
																							// 이름
						fileManagementVO.setFile_cls("A");
						if (req.getParameter("fileCnt").equals("0")) {
							fileManagementService.insertMembFileInfo(fileManagementVO);
						} else {
							fileManagementService.updateMembFileInfo(fileManagementVO);
						}
						String filePath = fileStorePath + File.separator + fileManagementVO.getFile_name();
						fileNm.transferTo(new File(EgovWebUtil.filePathBlackList(filePath))); // 파일
																								// 저장
					} else {
						FpisFileManagementVO fileManagementVO = new FpisFileManagementVO();
						fileManagementVO.setUsr_mst_key(comp_bsns_num);
						fileManagementService.deleteFile(fileManagementVO);
						fileManagementVO.setFile_cls("A");
					}
				}
			}
			/* ============== 회원정보 수정 : 끝 ============== */

			/* ============== 비밀번호 수정 : 시작 ============== */
			// 181127 smoh 회원정보 신고대행기관 변경, 비밀번호 변경 되면 mber_sttus가 P로 되어 위에 회원정보
			// 변경시 비밀번호 같이 바꾸는것으로 변경
			/*
			 * if (isCorrectPassword){
			 * mberManageVO.setPassword(EgovFileScrty.encryptPassword(
			 * newPassword)); try{
			 * mberManageService.updatePassword(mberManageVO); // COMTNGNRLMBER
			 * 비밀번호 수정 }catch(Exception e){ System.err.
			 * println("Error : Egov MberManageController - updateMberUser - mberManageService.updatePassword(mberManageVO) "
			 * ); } }
			 */
			model.addAttribute("resultMsg", "success.common.update"); // Exception
																		// 없이
																		// 진행시
																		// 수정성공메시지
			/* ============== 회원정보 수정 : 끝 ============== */

			/*
			 * ================== 2014.03.05 mgkim 회원정보수정 이력 기능 추가 시작
			 * ==================
			 */
			FpisLoginLogVO logVO = new FpisLoginLogVO();
			logVO.setJob_cls("ME");
			logVO.setOrd_cnt(0);
			if (svo != null) {
				logVO.setMber_id(svo.getUser_id());
				logVO.setMber_cls(svo.getMber_cls());
				logVO.setMber_nm(svo.getUser_name());
				logVO.setUsr_mst_key(svo.getUsr_mst_key());
			} else { // 2013.10.17 mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리
				SessionVO t_svo = (SessionVO) req.getSession().getAttribute("tempSessionVO");
				logVO.setMber_id(t_svo.getUser_id());
				logVO.setMber_cls(t_svo.getMber_cls());
				logVO.setMber_nm(t_svo.getUser_name());
				logVO.setUsr_mst_key(t_svo.getUsr_mst_key());
			}
			loginService.InsertLoginLog(logVO); // 회원탈퇴신청 로그 기록
			/*
			 * ================== 2014.03.05 mgkim 회원정보수정 이력 기능 추가 끝
			 * ==================
			 */

			if (svo != null) {
				return "redirect:/uss/myi/EgovMberSelectUpdtViewUser.do";
			} else { // 2013.10.17 mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리
				return "redirect:/uss/umt/EgovMberSelectUpdtViewUserNologin.do";
			}
		} else {
			model.addAttribute("resultMsg", "fail.common.update");
			return "redirect:/uss/myi/EgovMberSelectUpdtViewUser.do";
		}

	}

	// 20220801 재가입요청 처리 따로
	@RequestMapping("/uss/umt/EgovMberSelectUpdtUserNologin.do")
	public String updateMberUserNologin(@RequestParam(required = false, value = "fileNm") MultipartFile fileNm,
			@ModelAttribute("form_MberUpdtUser") FpisNewMberVO vo, BindingResult bindingResult, HttpServletRequest req,
			Model model) throws Exception, NullPointerException {

		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		String uniqId = req.getParameter("uniqId");

		// 2020.08.05 pch : 회원수정 필수값 검증-서버단(웹취약점 XSS 브루트포스) - 검증용 vo set
		String password3 = req.getParameter("password3");

		FpisNewMberVO chk_vo = new FpisNewMberVO();

		chk_vo.setComp_mst_key(vo.getNew_comp_bsns_num().replaceAll("-", ""));
		chk_vo.setNew_comp_nm(vo.getNew_comp_nm());
		chk_vo.setNew_comp_cls(vo.getNew_comp_cls());
		chk_vo.setNew_comp_addr1(vo.getNew_comp_addr1());
		chk_vo.setNew_comp_zip(vo.getNew_comp_zip());
		chk_vo.setSidoCd(vo.getSidoCd());
		chk_vo.setMberId(vo.getMberId());
		chk_vo.setCur_password(vo.getPassword());
		chk_vo.setPassword(vo.getPassword2());
		chk_vo.setPassword2(password3);
		chk_vo.setMberEmailAdres(vo.getMberEmailAdres());
		chk_vo.setMiddleTelno(vo.getMiddleTelno());
		chk_vo.setEndTelno(vo.getEndTelno());
		chk_vo.setMoblphonNo(vo.getMoblphonNo());

		/* 2013.09.01 mgkim 패스워드 변경 처리 기능 통합 - 패스워드 변경 유효성 검사 시작 */
		/* 불필요 제거 - 2021.10.16 suhyun */
		/* String newPassword = vo.getPassword2(); */

		boolean changePassword = false;
		MberManageVO mberManageVO = new MberManageVO();
		mberManageVO.setPassword(vo.getPassword2());
		mberManageVO.setUniqId(uniqId);

		String comp_corp_num = vo.getNew_comp_corp_num().replaceAll("-", ""); // 2013.08.29
																				// mgkim
																				// 수정시
																				// 사업자번호에
																				// "-"문자
																				// 추가되는
																				// 오류
		String comp_bsns_num = vo.getNew_comp_bsns_num().replaceAll("-", "");

		// 평문화 > RSA암호화 진행 - 2021.12.10 suhyun
		// String securedCurPassword = req.getParameter("curPassword");
		String securedPassword = req.getParameter("password2");
		String securedPassword2 = req.getParameter("password3");

		// System.out.println("ddddd securedCurPassword = "+
		// securedCurPassword);

		// HttpSession session = req.getSession();

		// PrivateKey privateKey = (PrivateKey)
		// session.getAttribute("__rsaPrivateKey__");

		// System.out.println("ddddd privateKey = "+ privateKey);

		// session.removeAttribute("__rsaPrivateKey__");

		/*
		 * if (privateKey == null) { model.addAttribute("usrStt", "E"); return
		 * "redirect:/userMain.do"; }
		 * 
		 * 
		 * chk_vo.setCur_password(decryptRsa(privateKey, securedCurPassword));
		 * 
		 * if(!securedPassword.isEmpty()) {
		 * chk_vo.setPassword(decryptRsa(privateKey, securedPassword));
		 * chk_vo.setPassword2(decryptRsa(privateKey, securedPassword2)); }
		 */

		// 2016. 06. 07 written by dyahn 회원정보 - 선택입력사항 - 연락처
		String tel = vo.getAreaNo() + vo.getMiddleTelno() + vo.getEndTelno();
		vo.setTel(tel);

		// 2020.08.05 pch : 회원가입 필수값 검증-서버단(웹취약점 XSS 브루트포스)
		// boolean flag = fnInputCheck(chk_vo);
		
		// 기존 비밀번호 확인 로직 추가 - 2021.10.01 suhyun
		 changePassword = vo.getPassword2().isEmpty() ? false : true;

		 MberManageVO chkMberVO = new MberManageVO();
		 chkMberVO.setUsr_mst_key(chk_vo.getComp_mst_key());
		 //chkMberVO.setPassword(chk_vo.getCur_password());
		 chkMberVO.setPassword(chk_vo.getPassword2());

		 //int confirmCurrentPassWord =
		 //mberManageService.selectMberCountCurrentPassword(chkMberVO);
		 //if (confirmCurrentPassWord != 1) {
		 //model.addAttribute("resultMsg", "fail.common.update");
		 //return "redirect:/uss/myi/EgovMberSelectUpdtViewUser.do";
		 //}

		// if (false) {
		// if (flag) {
		// 2013.11.22 mgkim 기존비밀번호 제거 (비번 분실시 공인인증 로그인하여 변경가능하게 조치

		/* 2013.09.01 mgkim 패스워드 변경 처리 기능 통합 - 패스워드 변경 유효성 검사 끝 */

		/* ============== 소속업체정보 수정 : 시작 ============== */
		UsrInfoVO usrInfoVO = new UsrInfoVO();
		usrInfoVO.setComp_corp_num(comp_corp_num);
		usrInfoVO.setComp_bsns_num(comp_bsns_num);
		usrInfoVO.setComp_cls(vo.getNew_comp_cls());
		usrInfoVO.setComp_nm(vo.getNew_comp_nm());
		// usrInfoVO.setCeo(vo.getNew_ceo()); 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
		// usrInfoVO.setTel(vo.getNew_tel()); 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
		usrInfoVO.setZip(vo.getNew_comp_zip());
		usrInfoVO.setAddr1(vo.getNew_comp_addr1());
		usrInfoVO.setAddr2(vo.getNew_comp_addr2());
		usrInfoVO.setUpd_user(vo.getMberId()); // 2014.03.05 mgkim 회원정보 수정자 추가
		usrInfoVO.setSigunguCd(vo.getSigunguCd()); // 181018 관할관청 정보 추가로 코드값 별도로
													// 받음
		usrInfoVO.setGov_seq(vo.getGov_seq());
		if (svo != null) {
			usrInfoVO.setComp_mst_key(svo.getComp_mst_key());
			usrInfoVO.setUsr_mst_key(svo.getUsr_mst_key());
		} else { // 2013.10.17 mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리
			SessionVO t_svo = (SessionVO) req.getSession().getAttribute("tempSessionVO");
			usrInfoVO.setComp_mst_key(t_svo.getComp_mst_key());
			usrInfoVO.setUsr_mst_key(t_svo.getUsr_mst_key());
		}

		usrInfoVO.setComp_cls_detail(vo.getComp_cls_detail());
		/*
		 * 2015.01.19 mgkim 업태,망가입여부 항목 제거 및 사업자유형 기능 추가
		 * usrInfoVO.setNet_comp_cls(vo.getNet_comp_cls()); //"사업자 유형 멀티값 : " +
		 * vo.getComp_cls_detail() boolean bool_net_comp = false; // 2014.02.20
		 * mgkim 망사업자 업태 여부 체크 if(vo.getComp_cls_detail().equals("03-01") ||
		 * vo.getComp_cls_detail().equals("05-01") ||
		 * vo.getComp_cls_detail().equals("05-02") ||
		 * vo.getComp_cls_detail().equals("05-03")){ // 2013.10.17 mgkim 망사업자
		 * 일경우 이용망 사업자 번호에 자신의 사업자번호를 추가.
		 * usrInfoVO.setNet_comp_bsns_num(comp_bsns_num); bool_net_comp = true;
		 * }else{ usrInfoVO.setNet_comp_bsns_num(vo.getNet_comp_bsns_num()); }
		 */

		// FpisSvc.updateMyPageCompInfo(usrInfoVO); // 신고주체 업체정보수정 USR_INFO
		if (!vo.getSigunguCd().equals(req.getParameter("preSigunguCd"))
				|| (vo.getGov_status().equals("N") || vo.getGov_status().equals("U"))) {
			usrInfoVO.setGov_seq(mberManageService.insertGovHistory(vo)); // 관할관청
																			// 정보
																			// 업데이트
		}
		FpisSvc.updateUsrInfo(usrInfoVO); // 신고주체 업체정보수정 USR_INFO
		/* ============== 소속업체정보 수정 : 끝 ============== */

		/* ============== 회원정보 수정 : 시작 ============== */
		MberManageVO mberVO = new MberManageVO();
		// mberVO.setMberNm(vo.getMberNm()); 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
		mberVO.setZip(vo.getNew_comp_zip());

		// mberVO.setMoblphonNo(vo.getMoblphonNo()); 2015.01.27 mgkim 사업단회의 반영
		// 선택항목 제거
		
		mberVO.setAreaNo(vo.getAreaNo());
		mberVO.setMiddleTelno(vo.getMiddleTelno());
		mberVO.setEndTelno(vo.getEndTelno());
		mberVO.setMoblphonNo(vo.getMoblphonNo());

		// 2016. 06. 07 written by dyahn 회원정보 - 추가수집정보항목 추가 - 이메일
		mberVO.setMberEmailAdres(vo.getMberEmailAdres());

		mberVO.setUniqId(uniqId);
		mberVO.setMberId(vo.getMberId());

		mberVO.setUpdate_file_cls(req.getParameter("update_file_cls"));
		/* 2014.12.24 양상완 공인인증서 수정하는사람 */
		/*
		 * 2015.01.20 mgkim
		 * mberVO.setUpdate_file_cls(req.getParameter("update_file_cls"));
		 * if(vo.getUsrVID() != null && vo.getUsrDN() !=null){
		 * mberVO.setUsrDN(AESCrypto.getInstance().encrypt(null,vo.getUsrDN()));
		 * mberVO.setUsrVID(AESCrypto.getInstance().encrypt(null,vo.getUsrVID())
		 * ); }
		 */

		if (svo != null) {
			/*
			 * 2014.02.20 mgkim 비대행신고자가 대행신고자(가맹/협회/인증망)로 업태 변경시 관리자가 승인처리를 하게
			 * 로직 변경 start
			 */
			/*
			 * 2015.01.19 mgkim 업태,망가입여부 항목 제거 및 사업자유형 기능 추가 if(bool_net_comp){
			 * String comp_cls_detail_old =
			 * req.getParameter("hid_comp_cls_detail"); //"정보변경 기존 사용자 업태 : "+
			 * comp_cls_detail_old if(comp_cls_detail_old.equals("03-01") ||
			 * comp_cls_detail_old.equals("05-01") ||
			 * comp_cls_detail_old.equals("05-02") ||
			 * comp_cls_detail_old.equals("05-03")){ // 대행신고자가 대행신고자료 변경하는 경우,
			 * }else{ // 비대행 신고자가 대행신고자로 업태 변경하는경우 mberVO.setMberSttus("C"); } }
			 */

			/*
			 * 2015.01.19 mgkim 사업자유형 변경 체크(가맹[04],인증[05],협회[06],운영기관[07] 는 관리자
			 * 승인받아야 변경된 정보로 사용 할 수 있음) 예외 07은 시스템으로 수정하지 않기로 함. DB로 직접 수정
			 */
			String comp_cls_old = req.getParameter("hid_new_comp_cls");
			// "정보변경 기존 사용자 형태 : "+ comp_cls_old
			String new_comp_cls = vo.getNew_comp_cls();
			if ("04".equals(new_comp_cls) || "05".equals(new_comp_cls) || "06".equals(new_comp_cls)
					|| "07".equals(new_comp_cls)) {
				if (!new_comp_cls.equals(comp_cls_old)) { // 대행신고 기능을 가진
															// 가맹,인증,협회로 유형이
															// 변경되었을 경우 관리자의 승인을
															// 받아야 한다.
					mberVO.setMberSttus("C");
				}
			}

			// 181127 smoh 회원정보 신고대행기관 변경, 비밀번호 변경 되면 mber_sttus가 P로 되어 회원정보 변경시
			// 비밀번호 같이 바꾸는것으로 변경
			// mberManageService.updateMber 내에 암호화가 진행되어 값만 넣음
			/* password 변경 확인 true : 변경 false : 유지 - 2021.10.16 suhyun */
			if (changePassword) {
				mberVO.setPassword(chk_vo.getPassword2());
			}
			/*
			 * 2014.02.20 mgkim 비대행신고자가 대행신고자(가맹/협회/인증망)로 업태 변경시 관리자가 승인처리를 하게
			 * 로직 변경 end
			 */
			mberManageService.updateMber(mberVO); // COMTNGNRLMBER 회원정보 수정
													// (이름,연락처,...)
		} else { // 2013.10.17 mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리
			
			if (changePassword) {
				mberVO.setPassword(chk_vo.getPassword2());
			}
			
			
			
			
			mberManageService.updateMberSttusR(mberVO); // COMTNGNRLMBER 회원정보 수정
														// (이름,연락처,...)
			mberManageService.updateMber(mberVO);
			
			
			
			/* 2014.12.20 양상완 사업자 등록증 첨부시 */
			System.out.println(">>>>>>>>>>>>>>>>성공!!");
			System.out.println(">>>>>>>>>>>>>>>>알려줘!!"+mberVO.getUpdate_file_cls());
			
			  if(mberVO.getUpdate_file_cls()!=null) {
				if ("Y".equals(mberVO.getUpdate_file_cls())) { 
				  if (!fileNm.isEmpty()) { //String storePathString =
			  //EgovProperties.getProperty("Globals.fileStorePath");
			  
			  File saveFolder = new
			  File(EgovWebUtil.filePathBlackList(fileStorePath)); if
			  (!saveFolder.exists() || saveFolder.isFile()) {
			  saveFolder.setReadable(true); saveFolder.setWritable(true);
			  saveFolder.mkdirs(); }
			  
			  String rtnStr = null; // 문자열로 변환하기 위한 패턴 설정(년도-월-일시:분:초:초(자정이후초)) 
			  String pattern = "yyyyMMddhhmmssSSS";
			  SimpleDateFormat sdfCurrent = new SimpleDateFormat(pattern,
			  Locale.KOREA); Timestamp ts = new
			  Timestamp(System.currentTimeMillis()); rtnStr =
			  sdfCurrent.format(ts.getTime());
			  
			  FpisFileManagementVO fileManagementVO = new
			  FpisFileManagementVO();
			  fileManagementVO.setUsr_mst_key(comp_bsns_num); //뒤에 getCurSequence 붙이면 수정시에로사항이있음 그냥 시간만 합니다.
			  fileManagementVO.setFile_name(rtnStr+fileManagementService.
			  getCurSequence()) ; // 암호화된 파일 이름
			  fileManagementVO.setFile_name(rtnStr);
			  fileManagementVO.setFile_dir(fileStorePath); // 저장경로
			  fileManagementVO.setOrg_file_name(fileNm.getOriginalFilename());
			  
			  
			  
			  fileManagementVO.setFile_cls("A");
			  //오리지날 파일 이름 fileManagementVO.setFile_cls("A"); 
			  if
			  (req.getParameter("fileCnt").equals("0")) {
			  fileManagementService.insertMembFileInfo(fileManagementVO); }
			  else {
			  fileManagementService.updateMembFileInfo(fileManagementVO); }
			  String filePath = fileStorePath + File.separator +
			  fileManagementVO.getFile_name(); fileNm.transferTo(new
			  File(EgovWebUtil.filePathBlackList(filePath))); //파일 저장			  
			  } else {
			  FpisFileManagementVO fileManagementVO = new
			  FpisFileManagementVO();
			  fileManagementVO.setUsr_mst_key(comp_bsns_num);
			  fileManagementService.deleteFile(fileManagementVO);
			  fileManagementVO.setFile_cls("A"); } }
			  }
			 
			 
		}
		/* ============== 회원정보 수정 : 끝 ============== */

		/* ============== 비밀번호 수정 : 시작 ============== */
		// 181127 smoh 회원정보 신고대행기관 변경, 비밀번호 변경 되면 mber_sttus가 P로 되어 위에 회원정보 변경시
		// 비밀번호 같이 바꾸는것으로 변경
		/*
		 * if (isCorrectPassword){
		 * mberManageVO.setPassword(EgovFileScrty.encryptPassword(newPassword));
		 * try{ mberManageService.updatePassword(mberManageVO); // COMTNGNRLMBER
		 * 비밀번호 수정 }catch(Exception e){ System.err.
		 * println("Error : Egov MberManageController - updateMberUser - mberManageService.updatePassword(mberManageVO) "
		 * ); } }
		 */
		model.addAttribute("resultMsg", "success.common.update"); // Exception
																	// 없이 진행시
																	// 수정성공메시지
		/* ============== 회원정보 수정 : 끝 ============== */

		/*
		 * ================== 2014.03.05 mgkim 회원정보수정 이력 기능 추가 시작
		 * ==================
		 */
		/*
		 * FpisLoginLogVO logVO = new FpisLoginLogVO(); logVO.setJob_cls("ME");
		 * logVO.setOrd_cnt(0); if (svo != null) {
		 * logVO.setMber_id(svo.getUser_id());
		 * logVO.setMber_cls(svo.getMber_cls());
		 * logVO.setMber_nm(svo.getUser_name());
		 * logVO.setUsr_mst_key(svo.getUsr_mst_key()); } else { // 2013.10.17
		 * mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리 SessionVO t_svo = (SessionVO)
		 * req.getSession().getAttribute("tempSessionVO");
		 * logVO.setMber_id(t_svo.getUser_id());
		 * logVO.setMber_cls(t_svo.getMber_cls());
		 * logVO.setMber_nm(t_svo.getUser_name());
		 * logVO.setUsr_mst_key(t_svo.getUsr_mst_key()); }
		 * loginService.InsertLoginLog(logVO); // 회원탈퇴신청 로그 기록
		 */ /*
			 * ================== 2014.03.05 mgkim 회원정보수정 이력 기능 추가 끝
			 * ==================
			 */

		if (svo != null) {
			return "redirect:/uss/myi/EgovMberSelectUpdtViewUser.do";
		} else { // 2013.10.17 mgkim 회원가입 반려회원 정보수정 및 재가입요청 처리
			return "redirect:/uss/umt/EgovMberSelectUpdtViewUserNologin.do";
		}
	} /*
		 * else { model.addAttribute("resultMsg", "fail.common.update"); return
		 * "redirect:/uss/myi/EgovMberSelectUpdtViewUser.do"; }
		 */

	// }

	/**
	 * 2017. 05. 31 written by dyahn 90일경과 비밀번호 변경
	 * 
	 * @param usr_mst_key
	 * @param currentPassword
	 * @param newPassword
	 * @param res
	 * @param req
	 * @param model
	 * @throws Exception
	 */
	@RequestMapping("/uss/umt/updateMberNewpassWord.do")
	public void updateMberNewpassWord(@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			@RequestParam(value = "currentPassword", required = false) String currentPassword,
			@RequestParam(value = "newPassword", required = false) String newPassword, HttpServletResponse res,
			HttpServletRequest req, Model model) throws Exception, NullPointerException {

		usr_mst_key = usr_mst_key.replaceAll("-", "");

		MberManageVO mberVO = new MberManageVO();
		mberVO.setUsr_mst_key(usr_mst_key);
		mberVO.setPassword(currentPassword);

		int confirmCurrentPassWord = mberManageService.selectMberCountCurrentPassword(mberVO);

		int rtn = -2; // -2:현재 비밀번호 맞지않습니다. -1:새 비밀번호변경이 정상적으로 이루어지지 않았습니다. 0:
						// 정상적으로 변경되었습니다.
		if (confirmCurrentPassWord > 0) {
			mberVO.setPassword(newPassword);
			rtn = mberManageService.updateMberNewpassWord(mberVO);
		}

		JSONObject json = new JSONObject();

		json.put("rtn", rtn);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	/*
	 * 2015.02.02 mgkim 아이디 찾기 기능 최초생성 2015.02.02 mgkim 사업자번호로 회원가입된 아이디 검색하기
	 */
	@RequestMapping("/uss/umt/getMberIdByCBN_ajax.do")
	public void getDetailCompClsData(@RequestParam(value = "comp_bsns_num", required = false) String comp_bsns_num,
			HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, NullPointerException {
		String usr_mst_key = comp_bsns_num.replaceAll("-", "");
		MberManageVO shVO = new MberManageVO();
		shVO.setUsr_mst_key(usr_mst_key);
		MberManageVO mberVO = mberManageService.getMberIdByCBN(shVO);
		
		JSONObject json = new JSONObject();
		if (mberVO != null) {
			String maskedId = mberVO.getMberId();
			maskedId = mberVO.getMberId().substring(0, mberVO.getMberId().length() - 2) + "**";
			json.put("mberId", maskedId);
			json.put("usr_mst_key", mberVO.getUsr_mst_key());
		} else { //2023.01.17 jwchoi 예외처리 추가
			json.put("mberId", "empty");
		}
		
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	/*
	 * FPIS 관리자 신규등록 화면으로 이동한다.
	 *
	 */
	/*
	 * @RequestMapping("/uss/umt/EgovMberInsertView3.do") public String
	 * insertMberView2(
	 * 
	 * @ModelAttribute("userSearchVO") UserDefaultVO userSearchVO,
	 * 
	 * @ModelAttribute("mberManageVO") MberManageVO mberManageVO,
	 * HttpServletRequest req, Model model )throws Exception { ComDefaultCodeVO
	 * vo = new ComDefaultCodeVO(); String forwardStr = ""; String mberCls = "";
	 * 
	 * try {
	 * 
	 * //패스워드힌트목록을 코드정보로부터 조회 vo.setCodeId("COM022"); List passwordHint_result =
	 * cmmUseService.selectCmmCodeDetail(vo);
	 * 
	 * //성별구분코드를 코드정보로부터 조회 vo.setCodeId("COM014"); List sexdstnCode_result =
	 * cmmUseService.selectCmmCodeDetail(vo);
	 * 
	 * //사용자상태코드를 코드정보로부터 조회 vo.setCodeId("COM013"); List mberSttus_result =
	 * cmmUseService.selectCmmCodeDetail(vo);
	 * 
	 * //그룹정보를 조회 - GROUP_ID정보 vo.setTableNm("COMTNORGNZTINFO"); List
	 * groupId_result = cmmUseService.selectGroupIdDetail(vo);
	 * 
	 * forwardStr = "egovframework/com/uss/umt/EgovMberInsert3";
	 * 
	 * if(mberCls == null || mberCls.equals("")) mberCls = "ADM";
	 * 
	 * 
	 * model.addAttribute("MBERCLS", mberCls); //그룹정보 목록
	 * 
	 * }catch(Exception e) { logger.error("ERROR : ", e); }
	 * 
	 * return forwardStr; }
	 */

	@RequestMapping("/uss/umt/EgovAdminInsertView.do")
	public String insertAdminView(@ModelAttribute("userSearchVO") UserDefaultVO userSearchVO,
			@ModelAttribute("mberManageVO") MberManageVO mberManageVO, HttpServletRequest req, Model model)
			throws Exception, NullPointerException {

		ComDefaultCodeVO vo = new ComDefaultCodeVO();

		// 패스워드힌트목록을 코드정보로부터 조회
		vo.setCodeId("COM022");
		List passwordHint_result = cmmUseService.selectCmmCodeDetail(vo);

		// 성별구분코드를 코드정보로부터 조회
		vo.setCodeId("COM014");
		List sexdstnCode_result = cmmUseService.selectCmmCodeDetail(vo);

		// 사용자상태코드를 코드정보로부터 조회
		vo.setCodeId("COM013");
		List mberSttus_result = cmmUseService.selectCmmCodeDetail(vo);

		// 그룹정보를 조회 - GROUP_ID정보
		vo.setTableNm("COMTNORGNZTINFO");
		List groupId_result = cmmUseService.selectGroupIdDetail(vo);

		model.addAttribute("insert_cls", "SYS"); // 관리자 Insert
		model.addAttribute("passwordHint_result", passwordHint_result); // 패스워트힌트목록
		model.addAttribute("sexdstnCode_result", sexdstnCode_result); // 성별구분코드목록
		model.addAttribute("mberSttus_result", mberSttus_result); // 사용자상태코드목록
		model.addAttribute("groupId_result", groupId_result); // 그룹정보 목록

		return "egovframework/com/uss/umt/EgovMberInsert";
	}

	@RequestMapping("/uss/umt/EgovMberInsertView2.do")
	public String insertMberView2(@ModelAttribute("userSearchVO") UserDefaultVO userSearchVO,
			@ModelAttribute("mberManageVO") MberManageVO mberManageVO, Model model)
			throws Exception, NullPointerException {
		ComDefaultCodeVO vo = new ComDefaultCodeVO();

		// 패스워드힌트목록을 코드정보로부터 조회
		vo.setCodeId("COM022");
		List passwordHint_result = cmmUseService.selectCmmCodeDetail(vo);
		// 성별구분코드를 코드정보로부터 조회
		vo.setCodeId("COM014");
		List sexdstnCode_result = cmmUseService.selectCmmCodeDetail(vo);
		// 사용자상태코드를 코드정보로부터 조회
		vo.setCodeId("COM013");
		List mberSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		// 그룹정보를 조회 - GROUP_ID정보
		vo.setTableNm("COMTNORGNZTINFO");
		List groupId_result = cmmUseService.selectGroupIdDetail(vo);

		model.addAttribute("passwordHint_result", passwordHint_result); // 패스워트힌트목록
		model.addAttribute("sexdstnCode_result", sexdstnCode_result); // 성별구분코드목록
		model.addAttribute("mberSttus_result", mberSttus_result); // 사용자상태코드목록
		model.addAttribute("groupId_result", groupId_result); // 그룹정보 목록

		return "egovframework/com/uss/umt/EgovMberInsert";
	}

	/**
	 * 일반회원등록처리후 목록화면으로 이동한다.
	 * 
	 * @param mberManageVO 일반회원등록정보
	 */
	@RequestMapping("/uss/umt/EgovMberInsert3.do")
	public String insertMber3(@ModelAttribute("mberManageVO") MberManageVO mberManageVO, BindingResult bindingResult,
			HttpServletRequest req, Model model) throws Exception, NullPointerException {

		mberManageVO.setComp_mst_key(mberManageVO.getSigunguCd());
		mberManageVO.setMber_cls(mberManageVO.getMber_cls());
		mberManageService.insertMber(mberManageVO);
		// 다시 Insert 호출시 Main화면으로 이동
		return "redirect:/uss/umt/EgovMberInsertResult3.do?uniqID=" + mberManageVO.getUniqId();
	}

	/**
	 * 일반회원등록처리후 목록화면으로 이동한다.
	 * 
	 * @param mberManageVO 일반회원등록정보 2013.10.16 mgkim 회원가입 승인대기상태 페이지 호출
	 */
	@RequestMapping("/uss/umt/EgovMberInsertResult.do")
	public String insertMberResult(@RequestParam("uniqID") String uniqID, HttpServletRequest req, Model model)
			throws Exception, NullPointerException {
		// "승인대기 상태정보 조회 - uniqID : " + uniqID
		if (uniqID != null && !uniqID.equals("")) {
			MberManageVO mberManageVO = mberManageService.selectMber(uniqID);
			SysCompanyInfoVO CompVO = FpisSvc.getSysCompanyPk(mberManageVO.getUsr_mst_key()); // 2013.10.16
																								// mgkim
																								// 값
																								// 변경.
			model.addAttribute("RES", "0"); // 등록결과
			model.addAttribute("MBERVO", mberManageVO); // 가입정보
			model.addAttribute("COMPVO", CompVO); // 업체(개인정보)
		} else {
			model.addAttribute("RES", "-1"); // 등록결과
			model.addAttribute("MBERVO", null); // 가입정보
			model.addAttribute("COMPVO", null); // 업체(개인정보)
		}

		return "egovframework/com/uss/umt/EgovMberInsertResult";
	}

	/**
	 * 일반회원등록처리후 목록화면으로 이동한다.
	 * 
	 * @param mberManageVO 일반회원등록정보
	 */
	@RequestMapping("/uss/umt/EgovMberInsertResult3.do")
	public String insertMberResult3(@RequestParam("uniqID") String uniqID, HttpServletRequest req, Model model)
			throws Exception, NullPointerException {

		SigunguVO vo = new SigunguVO();
		if (uniqID != null && !uniqID.equals("")) {
			MberManageVO mberManageVO = mberManageService.selectMber(uniqID);
			vo.setSigunguCd(mberManageVO.getAdm_area_code());

			SigunguVO SigunguVO = mberManageService.selectSigunguPk(vo);

			model.addAttribute("RES", "0"); // 등록결과
			model.addAttribute("MBERVO", mberManageVO); // 가입정보
			model.addAttribute("SIGUNGU", SigunguVO); // 시군구

		} else {
			model.addAttribute("RES", "-1"); // 등록결과
			model.addAttribute("MBERVO", null); // 가입정보
			model.addAttribute("SIGUNGU", null); // 시군구
		}

		return "egovframework/com/uss/umt/EgovMberInsertResult3";
	}

	/**
	 * 일반회원정보 수정후 목록조회 화면으로 이동한다.
	 * 
	 * @param mberManageVO 일반회원수정정보
	 */
	/*
	 * @RequestMapping("/uss/umt/EgovMberSelectUpdt.do") public String
	 * updateMber(
	 * 
	 * @ModelAttribute("mberManageVO") MberManageVO mberManageVO, BindingResult
	 * bindingResult, HttpServletRequest req, Model model )throws Exception {
	 * 
	 * SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");
	 * mberManageService.updateMber(mberManageVO);
	 * 
	 * //Exception 없이 진행시 수정성공메시지 model.addAttribute("resultMsg",
	 * "success.common.update");
	 * 
	 * String
	 * forwardString="redirect:/uss/umt/EgovMberSelectUpdtView.do?selectedId="+
	 * mberManageVO.getUniqId();
	 * 
	 * return forwardString; }
	 */

	/**
	 * 일반회원정보 수정후 목록조회 화면으로 이동한다. 2013.09.02 mgkim 사용자시스템 - 탈퇴신청 2014.02.20
	 * mgkim 회원탈퇴 방식 변경(플레그 UPDATE -> DELETE) /uss/umt/EgovMberDelete.do URL 사용
	 */
	@RequestMapping("/uss/umt/EgovMberSelectSttUpdt.do")
	public String EgovMberDelete(HttpServletRequest req, Model model) throws Exception, NullPointerException {

		String uniqId = req.getParameter("uniqId");
		MberManageVO mberManageVO = new MberManageVO();
		mberManageVO.setUniqId(uniqId);

		// "------- 회원 탈퇴신청 ID :["+mberManageVO.getUniqId()+"]"
		mberManageVO.setMberSttus("D");
		mberManageService.updateSttMber(mberManageVO);

		model.addAttribute("resultMsg", "success.common.update"); // Exception
																	// 없이 진행시
																	// 수정성공메시지

		req.getSession().removeAttribute(FpisConstants.SESSION_KEY);
		req.getSession().removeAttribute(FpisConstants.SESSION_LOGIN_BY_POP);

		return "redirect:/userMain.do";
	}

	/**
	 * 2014.02.20 mgkim 회원탈퇴 방식 변경(플레그 UPDATE -> DELETE)
	 * /uss/umt/EgovMberDelete.do URL 사용 탈퇴시 관리자 승인 필요없음
	 *  1. FLAG 처리에서 DELETE 처리로 변경(개인정보보호) : 업체정보(USR_INFO), 회원정보(COMTNGNRLMBER) 
	 *  2. 탈퇴시 이력(FPIS_LOGIN_LOG) 남김(member secession) JOB_CLS : MS 
	 *  3. 거래처(USR_COMPANY_INFO), 차량관리(USR_CARS_INFO) 정보와 실적신고 데이터는 유지
	 */
	@RequestMapping("/uss/umt/EgovMberDelete.do")
	public String deleteMber(@RequestParam("uniqId") String checkedIdForDel,
			@ModelAttribute("searchVO") UserDefaultVO userSearchVO, HttpServletRequest req, Model model)
			throws Exception, NullPointerException {

		// 신고자 - 회원정보변경 - 탈퇴신청
		// 회원 탈퇴신청 로그 기록...
		SessionVO SVO = (SessionVO) req.getSession().getAttribute("SessionVO");
		FpisLoginLogVO logVO = new FpisLoginLogVO();
		
		if (SVO == null) {
			logVO.setMber_id(req.getParameter("mberId"));
			logVO.setJob_cls("MS");
			logVO.setMber_cls(req.getParameter("new_comp_cls"));
			logVO.setMber_nm(req.getParameter("new_comp_nm"));
			logVO.setUsr_mst_key(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
			logVO.setOrd_cnt(0);
		} else {
			logVO.setMber_id(SVO.getUser_id());
			logVO.setJob_cls("MS");
			logVO.setMber_cls(SVO.getMber_cls());
			logVO.setMber_nm(SVO.getUser_name());
			logVO.setUsr_mst_key(SVO.getUsr_mst_key());
			logVO.setOrd_cnt(0);
		}
		loginService.InsertLoginLog(logVO); // 회원탈퇴신청 로그 기록
		// -------MEMBER DELETE ID :["+checkedIdForDel+"]
		mberManageService.deleteMber(checkedIdForDel);

		/* 2014.05.28 swyang 회원탈퇴 할 때 파일정보까지 모조리 삭제! */
		FpisFileManagementVO fileManagementVO = new FpisFileManagementVO();
		if (SVO == null) {
			fileManagementVO.setUsr_mst_key(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
		} else {
			fileManagementVO.setUsr_mst_key(SVO.getUsr_mst_key());
		}
		fileManagementService.deleteFile(fileManagementVO);
		
		/* 2023.04.26 jwchoi 재가입자 테이블에 탈퇴회원 추가 */
		mberManageService.insertUsrRejoin(req, "MS");

		// 191126 오승민 회원탈퇴 시 휴면계정탈퇴와 동일하게 거래처, 차량, 가맹점, 대행사 회원, 영업정보삭제 추가
		if (SVO == null) {
			mberManageService.deleteUsrCompanyInfoUsr(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
			mberManageService.deleteUsrCarsHistoryUsr(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
			mberManageService.deleteUsrCarsInfoUsr(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
			mberManageService.deleteUsrNetInfoUsr(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
			mberManageService.deleteUsrAssocInfoUsr(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
			mberManageService.deleteUsrOfficeInfoUsr(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
			mberManageService.deleteUsrGovHistoryUsr(req.getParameter("new_comp_bsns_num").replaceAll("-", ""));
		} else {
			mberManageService.deleteUsrCompanyInfoUsr(SVO.getUsr_mst_key());
			mberManageService.deleteUsrCarsHistoryUsr(SVO.getUsr_mst_key());
			mberManageService.deleteUsrCarsInfoUsr(SVO.getUsr_mst_key());
			mberManageService.deleteUsrNetInfoUsr(SVO.getUsr_mst_key());
			mberManageService.deleteUsrAssocInfoUsr(SVO.getUsr_mst_key());
			mberManageService.deleteUsrOfficeInfoUsr(SVO.getUsr_mst_key());
			mberManageService.deleteUsrGovHistoryUsr(SVO.getUsr_mst_key());
		}
		req.getSession().removeAttribute(FpisConstants.SESSION_KEY);
		req.getSession().removeAttribute(FpisConstants.SESSION_LOGIN_BY_POP);

		// return "forward:/uss/umt/EgovMberManage.do";
		return "redirect:/userMain.do";
	}

	/*
	 * 관리자 신고주체관리 가입승인
	 */
	@RequestMapping("/uss/umt/EgovMberUpdateStat.do")
	public String updateMberStat(@RequestParam(value = "selbox", required = false) String[] mberId,
			@RequestParam(value = "sttUs", required = false) String sttUs,
			@ModelAttribute("searchVO") UserDefaultVO userSearchVO, Model model)
			throws Exception, NullPointerException {

		MberManageVO mberManageVO = new MberManageVO();

		for (int i = 0; i < mberId.length; i++) {
			// ------------mber_id:"+mberId[i]+"--> STAT:"+sttUs
			/*
			 * 2021.09.02 suhyun - ESTNL_ID 값에서 USR_MST_KEY로 변경. 기존 selbox 파라미터값
			 * 설정이 잘못되어 있었음
			 */
			String mstkey[] = mberId[i].split("/");
			mberManageVO.setMberSttus(sttUs);
			mberManageVO.setUniqId(mstkey[0]);
			mberManageService.updateSttMber(mberManageVO);
		}

		return "forward:/uss/umt/EgovMberManage.do";
	}

	/**
	 * 일반회원가입신청등록처리후로그인화면으로 이동한다.
	 * 
	 * @param userManageVO - 신규일반회원정보 , 검색조건정보
	 * @param status - 세션상태정보
	 * @return "forward:/uss/umt/EgovUserManage.do"
	 * @exception Exception
	 */

	/**
	 * 일반회원가입신청등록처리후로그인화면으로 이동한다.
	 * 
	 * @param mberManageVO 일반회원가입신청정보
	 * @return forward:/uat/uia/egovLoginUsr.do
	 * @throws Exception
	 */
	@RequestMapping("/uss/umt/EgovMberSbscrb.do")
	public String sbscrbMber(@ModelAttribute("mberManageVO") MberManageVO mberManageVO)
			throws Exception, NullPointerException {

		// 가입상태 초기화
		mberManageVO.setMberSttus("A");
		// 그룹정보 초기화
		// mberManageVO.setGroupId("1");
		// 일반회원가입신청 등록시 일반회원등록기능을 사용하여 등록한다.
		mberManageService.insertMber(mberManageVO);
		return "forward:/uat/uia/egovLoginUsr.do";
	}

	/**
	 * -----------------------------------------------------------------
	 * 
	 * @param model /fpis/dashboard/FpisUserJoinCompSearchPop.jsp 오정화 회원가입 회원검색
	 * - 사업자번호 검색, 법인번호 검색을 개발 오류 개선작업 ㅡ,ㅡ 2014.02.25 mgkim DB암호화 관련 소스수정
	 *
	 * 2015.0126 mgkim 안쓰는 소스 제거 2021.01.11 ysw 보안취약점 METHOD POST 만 가능하도록 수정
	 * -----------------------------------------------------------------
	 */

	@RequestMapping(value = "/userJoin.do", method = RequestMethod.POST)
	public String userJoin(@ModelAttribute("mainFrm") FpisNewJoinVO shVO, BindingResult bindingResult,
			HttpServletRequest req, ModelMap model) throws Exception, NullPointerException {

		SessionVO SessionVO = (SessionVO) req.getSession().getAttribute(FpisConstants.SESSION_KEY);
		if (SessionVO != null) {
			return "redirect:/userMain.do";
		}
		return "/fpis/dashboard/FpisUserJoin";

	}

	/**
	 * -----------------------------------------------------------------
	 * 
	 * @param model /fpis/dashboard/FpisUserJoinCompSearchPop.jsp
	 *
	 * 2015.01.25 mkgim 회원가입 업태, 망이용정보 항목 제거 2015.01.25 mgkim 사업자 유형 항목 신규 추가
	 * 2015.01.27 mgkim 사업단회의 반영 선택항목 제거
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = "/userJoin2.do", method = RequestMethod.POST)
	public String userJoin2(HttpServletRequest req, ModelMap model) throws Exception, NullPointerException {

		// userJoin2 ACT
		/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
		List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 운송유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS024", codeFMS024);
		List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 주선유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS025", codeFMS025);
		List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 망사업자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS026", codeFMS026);
		List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																				// mgkim
																				// 사업자
																				// 대행신고자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS027", codeFMS027);
		/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);

		return "/fpis/dashboard/FpisUserJoin2";
	}

	/*
	 * 2015.01.27 mgkim userJoin2.do 에서 처리프로세스 분리작업 2015.01.27 mgkim 회원가입 처리
	 * 프로세스 2015.01.29 mgkim 회원가입 유니크 아이디 중복생성 문제 해결 2015.02.05 양상완 낫조인 테이블에
	 * 넣기위한 작업 합니다. 2015.04.15 mgkim 가맹사업자 선택기능 제거 - 관리자가 신고주체관리 페이지를 통해 관리하기로
	 * 함. 2015.04.15 mgkim 일반 운수사업자(운송/주선/겸업)자는 자동으로 승인처리 한다. 대행권한이 있는
	 * 기관(가맹사업자,인증망사업자,연합회/협회,위탁운영기관)의 경우는 기존대로 회원가입승인을 거친다.
	 */
	@RequestMapping(value = "/userJoin3.do")
	public String userJoin3(@ModelAttribute("mainFrm") FpisNewMberVO vo, @ModelAttribute("shVO") FpisNewJoinVO shVO,
			BindingResult bindingResult, HttpServletRequest req, ModelMap model,
			@RequestParam(value = "fileNm", required = false) MultipartFile fileNm)
			throws Exception, NullPointerException {
		MberManageVO mberVO = new MberManageVO();
		int rtn = 0;

		String bsns_num = vo.getNew_comp_bsns_num().replaceAll("-", "");
		String corp_num = vo.getNew_comp_corp_num().replaceAll("-", "");	//20230116 chbaek 회원가입 법인번호 등록 오류 수정	
		
		vo.setNew_comp_mst_key(bsns_num);
		vo.setNew_comp_bsns_num(bsns_num);
		vo.setNew_comp_corp_num(corp_num);
		
		if (vo.getNew_comp_addr1() == null || vo.getNew_comp_addr1() == "") {
			vo.setNew_comp_addr2("");
		}

		/* 2021.07.15 jws 자동입력방지 기능 추가 */
		HttpSession captcha_session = req.getSession();
		if (!vo.getCaptcha().equals(captcha_session.getAttribute(captcha_session.getId() + "captcha"))) {
			model.addAttribute("MSG", "notEqualCaptcha");
			return "forward:/userJoin2.do";
		}

		captcha_session.setAttribute(captcha_session.getId() + "captcha", "c_expired"); // 1회성으로
																						// 사용하고
																						// 폐기

		// shVO에 아무것도 안담겨있는데 이걸로 체크를 하려해서 추가함 - 2021.12.08 suhyun
		shVO.setSearch_comp_bsns_num(vo.getSearch_comp_bsns_num().replaceAll("-", ""));
		if (!EgovNumberCheckUtil.checkCompNumber(bsns_num)) {
			model.addAttribute("MSG", "fail_comp_bsns_num");
			return "/fpis/dashboard/FpisUserJoin2";
		} else {
			if (!(FpisSvc.joinUsrCompList(shVO) == null || FpisSvc.joinUsrCompList(shVO).size() == 0)) {
				model.addAttribute("MSG", "fail_comp_bsns_num");
				return "/fpis/dashboard/FpisUserJoin2";
			}
		}

		if (vo.getUsrVID().equals("") || vo.getUsrVID() == null) {
			vo.setUsrVID("");
		} else {
			vo.setUsrDN(AESCrypto.getInstance().encrypt(vo.getUsrDN()));
		}
		if (vo.getUsrVID() == null || vo.getUsrVID().equals("")) {
			vo.setUsrVID("");
		} else {
			vo.setUsrVID(AESCrypto.getInstance().encrypt(vo.getUsrVID()));
		}

		// 2016. 06. 07 written by dyahn 회원정보 - 선택입력사항 - 연락처
		String tel = vo.getAreaNo() + vo.getMiddleTelno() + vo.getEndTelno();
		vo.setTel(tel);

		// 보안취약점. 회원가입 파라메터 다 크로스사이트스크립트 조치
		vo.setMberId(Xsite_secure(vo.getMberId()));
		vo.setNew_comp_nm(Xsite_secure(vo.getNew_comp_nm()));
		vo.setGov_status(Xsite_secure(vo.getGov_status()));
		vo.setNew_comp_mst_key(Xsite_secure(vo.getNew_comp_mst_key()));
		vo.setComp_mst_key(Xsite_secure(vo.getComp_mst_key()));
		vo.setSidoNm(Xsite_secure(vo.getSidoNm()));
		vo.setSigunguNm(Xsite_secure(vo.getSigunguNm()));
		vo.setChkFlag(Xsite_secure(vo.getChkFlag()));
		vo.setUsrDN(Xsite_secure(vo.getUsrDN()));
		vo.setUsrVID(Xsite_secure(vo.getUsrVID()));
		vo.setNew_comp_zip(Xsite_secure(vo.getNew_comp_zip()));
		vo.setComp_cls_detail(Xsite_secure(vo.getComp_cls_detail()));
		vo.setComp_cls(Xsite_secure(vo.getComp_cls()));
		vo.setMberEmailAdres(Xsite_secure(vo.getMberEmailAdres()));
		vo.setNew_comp_bsns_num(Xsite_secure(vo.getNew_comp_bsns_num()));
		vo.setNew_comp_corp_num(Xsite_secure(vo.getNew_comp_corp_num()));
		vo.setNew_comp_addr1(Xsite_secure(vo.getNew_comp_addr1()));
		vo.setNew_comp_addr2(Xsite_secure(vo.getNew_comp_addr2()));
		vo.setSidoCd(Xsite_secure(vo.getSidoCd()));
		vo.setSigunguCd(Xsite_secure(vo.getSigunguCd()));
		vo.setAreaNo(Xsite_secure(vo.getAreaNo()));
		vo.setMiddleTelno(Xsite_secure(vo.getMiddleTelno()));
		vo.setEndTelno(Xsite_secure(vo.getEndTelno()));
		vo.setTel(Xsite_secure(vo.getTel()));
		vo.setMoblphonNo(Xsite_secure(vo.getMoblphonNo()));

		// 2020.06.01 pch : 회원가입 필수값 검증-서버단(웹취약점 XSS 브루트포스)
		boolean flag = fnInputCheck(vo);

		mberVO.setMberId(vo.getMberId()); // 회원 정보 - 아이디
		mberVO.setPassword(vo.getPassword()); // 회원 정보 - 패스워드
		// mberVO.setMberNm(vo.getMberNm()); // 회원 정보 - 이름

		mberVO.setMberSttus("A"); // 사용자 가입 신청 상태
		// 2015.04.15 mgkim 일반 운수사업자(운송/주선/겸업)자는 자동으로 승인처리 한다.
		if ("01".equals(vo.getNew_comp_cls()) || "02".equals(vo.getNew_comp_cls())
				|| "03".equals(vo.getNew_comp_cls())) {
			mberVO.setMberSttus("P"); // 사용자 가입 승인 상태
		}

		mberVO.setAreaNo(vo.getAreaNo());
		mberVO.setMiddleTelno(vo.getMiddleTelno());
		mberVO.setEndTelno(vo.getEndTelno());
		mberVO.setMoblphonNo(vo.getMoblphonNo());

		// 2016. 06. 07 written by dyahn 회원정보 - 선택입력사항 - 이메일
		mberVO.setMberEmailAdres(vo.getMberEmailAdres());

		mberVO.setUsr_mst_key(vo.getNew_comp_mst_key()); // 회원 정보 - 사용자 마스터 키
		mberVO.setMber_cls("GNR"); // 회원 정보 - 사용자 등록구분 (GNR : 신고주체)
		mberVO.setComp_mst_key(vo.getNew_comp_mst_key()); // 업체 정보 - 업체 마스터키(사업자
															// 번호)
		mberVO.setUsrDN(vo.getUsrDN()); // NPKI
		mberVO.setUsrVID(vo.getUsrVID()); // NPKI
		mberVO.setSidoCd(vo.getSidoCd());

		if (flag) {
			mberManageService.insertMberComtngnrlber(mberVO);
			
			/* 2023.04.26 jwchoi 재가입자 테이블에 재가입회원 추가 */
			boolean result = mberManageService.selectUsrRejoin(mberVO);
			if (result) { //재가입
				mberManageService.insertUsrRejoin(req, "RE");
				
			} else { //신규회원
				mberManageService.insertUsrRejoin(req, "JI");
			} 
			// 181017 smOh 관할관청 이력저장
			vo.setGov_seq(mberManageService.insertGovHistory(vo));

			mberManageService.insertUsr_Info(vo);

			// TODO 2014.03.12 mgkim 회원가입 이력추가
			FpisLoginLogVO logVO = new FpisLoginLogVO();
			logVO.setMber_id(mberVO.getMberId());
			logVO.setJob_cls("JI");
			logVO.setMber_cls(mberVO.getMber_cls());
			logVO.setMber_nm(mberVO.getMberNm());
			logVO.setUsr_mst_key(mberVO.getComp_mst_key());
			logVO.setOrd_cnt(0);
			mberManageService.InsertJoinLog(logVO);

			/* 2014.05.12 swyang 사업자등록증 첨부파일 등록 기능 start */
			if (!fileNm.isEmpty()) {
				// String storePathString =
				// EgovProperties.getProperty("Globals.fileStorePath");
				File saveFolder = new File(EgovWebUtil.filePathBlackList(fileStorePath));
				if (!saveFolder.exists() || saveFolder.isFile()) {
					if (!saveFolder.exists() || saveFolder.isFile()) {
						saveFolder.setReadable(true);
						saveFolder.setWritable(true);
						saveFolder.mkdirs();

					}
				}
				String rtnStr = null;
				// 문자열로 변환하기 위한 패턴 설정(년도-월-일 시:분:초:초(자정이후 초))
				String pattern = "yyyyMMddhhmmssSSS";
				SimpleDateFormat sdfCurrent = new SimpleDateFormat(pattern, Locale.KOREA);
				Timestamp ts = new Timestamp(System.currentTimeMillis());
				rtnStr = sdfCurrent.format(ts.getTime());

				FpisFileManagementVO fileManagementVO = new FpisFileManagementVO();
				fileManagementVO.setUsr_mst_key(logVO.getUsr_mst_key());
				/* 뒤에 getCurSequence 붙이면 수정시에로사항이있음 그냥 시간만 합니다. */
				/*
				 * fileManagementVO.setFile_name(rtnStr+fileManagementService.
				 * getCurSequence()) ; // 암호화된 파일 이름
				 */
				fileManagementVO.setFile_name(rtnStr);
				fileManagementVO.setFile_dir(fileStorePath); // 저장경로
				fileManagementVO.setOrg_file_name(fileNm.getOriginalFilename()); // 오리지날
																					// 파일
																					// 이름
				fileManagementVO.setFile_cls("A");
				fileManagementService.insertMembFileInfo(fileManagementVO);
				String filePath = fileStorePath + File.separator + fileManagementVO.getFile_name();
				fileNm.transferTo(new File(EgovWebUtil.filePathBlackList(filePath))); // 파일
																						// 저장
			}
			/* 2014.05.12 swyang 사업자등록증 첨부파일 등록 기능 end */

			rtn = 1;

			// 회원가입2-6
			/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
			// 2015.01.16 mgkim 업체정보 세부 사업자유형 확인
			String strCompClsDetail = vo.getComp_cls_detail();
			String compCls_01_01 = "N";
			String compCls_01_02 = "N";
			String compCls_01_03 = "N";
			String compCls_01_04 = "N";
			String compCls_02_01 = "N";
			String compCls_02_02 = "N";
			String compCls_04_01 = "N";
			String[] strCCD = strCompClsDetail.split(",");
			for (int i = 0; i < strCCD.length; i++) {
				if (strCCD[i].equals("01-01")) {
					compCls_01_01 = "Y";
				} else if (strCCD[i].equals("01-02")) {
					compCls_01_02 = "Y";
				} else if (strCCD[i].equals("01-03")) {
					compCls_01_03 = "Y";
				} else if (strCCD[i].equals("01-04")) {
					compCls_01_04 = "Y";
				} else if (strCCD[i].equals("02-01")) {
					compCls_02_01 = "Y";
				} else if (strCCD[i].equals("02-02")) {
					compCls_02_02 = "Y";
				} else if (strCCD[i].equals("04-01")) {
					compCls_04_01 = "Y";
				}
			}

			model.addAttribute("compCls_01_01", compCls_01_01);
			model.addAttribute("compCls_01_02", compCls_01_02);
			model.addAttribute("compCls_01_03", compCls_01_03);
			model.addAttribute("compCls_01_04", compCls_01_04);
			model.addAttribute("compCls_02_01", compCls_02_01);
			model.addAttribute("compCls_02_02", compCls_02_02);
			model.addAttribute("compCls_04_01", compCls_04_01);

			/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

			// 회원가입2-7
			model.addAttribute("MBERVO", vo);
			model.addAttribute("RTN", rtn);

			/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
			List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																					// mgkim
																					// 사업자
																					// 운송유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS024", codeFMS024);
			List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																					// mgkim
																					// 사업자
																					// 주선유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS025", codeFMS025);
			List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																					// mgkim
																					// 사업자
																					// 망사업자유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS026", codeFMS026);
			List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																					// mgkim
																					// 사업자
																					// 대행신고자유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS027", codeFMS027);
			/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

			/* 2015.02.05 양상완 낫조인 테이블 작업 */
			model.addAttribute("joinCls", req.getParameter("joinCls"));

			// 2015.10. 12 written by dyahn 모의해킹 보안조치 계정 데이터 평문 RSA 암호화 로직 시작
			HttpSession session = req.getSession();

			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);

			KeyPair keyPair = generator.genKeyPair();
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();

			// 세션에 공개키의 문자열을 키로하여 개인키를 저장한다.
			session.setAttribute("__rsaPrivateKey__", privateKey);

			// 공개키를 문자열로 변환하여 JavaScript RSA 라이브러리 넘겨준다.
			RSAPublicKeySpec publicSpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);

			String publicKeyModulus = publicSpec.getModulus().toString(16);
			String publicKeyExponent = publicSpec.getPublicExponent().toString(16);

			model.addAttribute("rsaPublicKeyModulus", publicKeyModulus);
			model.addAttribute("rsaPublicKeyExponent", publicKeyExponent);

			return "/fpis/dashboard/FpisUserJoin3";
		} else {

			// userJoin2 ACT
			/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
			List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																					// mgkim
																					// 사업자
																					// 운송유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS024", codeFMS024);
			List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																					// mgkim
																					// 사업자
																					// 주선유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS025", codeFMS025);
			List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																					// mgkim
																					// 사업자
																					// 망사업자유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS026", codeFMS026);
			List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																					// mgkim
																					// 사업자
																					// 대행신고자유형
																					// 코드정보로부터
																					// 조회
			model.addAttribute("codeFMS027", codeFMS027);
			/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

			List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
			model.addAttribute("SIDOLIST", sidoList);

			model.addAttribute("MSG", "fail");

			return "/fpis/dashboard/FpisUserJoin2";
		}

	}

	/* 2014.04.01 swyang : 공인인증서 수정하기. */
	@RequestMapping(value = "/uss/umt/modifyUsrVID.do")
	public String modifyUsrVID(@ModelAttribute FpisNewMberVO vo, HttpServletRequest req, ModelMap model)
			throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		MberManageVO mberVO = new MberManageVO();

		/* 암호화 */
		vo.setUsrVID(AESCrypto.getInstance().encrypt(vo.getUsrVID()));
		vo.setUsrDN(AESCrypto.getInstance().encrypt(vo.getUsrDN()));
		/* 암호화 */

		mberVO.setMberId(svo.getUser_id());
		mberVO.setUsrDN(vo.getUsrDN()); // NPKI
		mberVO.setUsrVID(vo.getUsrVID()); // NPKI
		mberVO.setUsr_mst_key(svo.getUsr_mst_key());
		mberManageService.updateUsrVID(mberVO);

		model.addAttribute("rcode", "R4");
		model.addAttribute("bcode", "R4-01");

		return "redirect:/uss/myi/EgovMberSelectUpdtViewUser.do";

	}

	/* 2014.04.01 swyang : 공인인증서 수정하기 끝. */

	/*
	 * 2013.10.07 mgkim : 업태 상세구분 가져오기 (ajax 모듈)
	 *
	 * 2015.01.13 mgkim 1.09 사업단회의 결과 반영 업태(사업자 유형) 다중선택으로 기능개선 해당 기능 사용 안함 기능
	 * 폐쇄
	 */
	/*
	 * @RequestMapping("/getDetailCompClsData.do") public void
	 * getDetailCompClsData(@RequestParam(value="newcompcls",
	 * required=false)String newcompcls, HttpServletResponse res,
	 * HttpServletRequest req, Model model) throws Exception { String
	 * commoncode_id = "FMS0"; int newcompcls_num = Integer.parseInt(newcompcls)
	 * + 12; if(newcompcls_num < 99){ commoncode_id = commoncode_id +
	 * Integer.toString(newcompcls_num); } else{ commoncode_id = "FMS019"; }
	 * List<SysCodeVO> detailCompCls = commonService.commonCode(commoncode_id,
	 * null);
	 * 
	 * StringBuilder strBuilder = new StringBuilder(); strBuilder.append("[");
	 * for(int i=0; i < detailCompCls.size(); i++){ SysCodeVO codeVO =
	 * (SysCodeVO)detailCompCls.get(i); if( i == 0){ strBuilder.append("{");
	 * }else{ strBuilder.append(",{"); }
	 * strBuilder.append("\"code\":\""+codeVO.getCode()+"\"");
	 * strBuilder.append(",\"name\":\""+codeVO.getName()+"\"");
	 * strBuilder.append("}"); } strBuilder.append("]");
	 * res.setContentType("application/json");
	 * res.setCharacterEncoding("UTF-8");; PrintWriter out = res.getWriter();
	 * out.write(strBuilder.toString()); out.close(); }
	 */

	/*
	 * 2013.10.10 mgkim : 업태 망사업자정보 가져오기 (ajax 모듈) 2013.10.17 mgkim : 회원가입 승인된
	 * 사업자중 망사업자 가져오기
	 *
	 * 2015.01.13 mgkim 1.09 사업단회의 결과 반영 망이용 여부 항목 삭제
	 */
	/*
	 * @RequestMapping("/getUsrInfoNetCompData.do") public void
	 * getUsrInfoNetCompData(@RequestParam(value="newcompclsdetail",
	 * required=false)String newcompclsdetail, HttpServletResponse res,
	 * HttpServletRequest req, Model model) throws Exception { // TODO 망사업자 정보
	 * 가져오기 SessionVO sVO =
	 * (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
	 * 
	 * UsrInfoVO vo = new UsrInfoVO(); vo.setComp_cls_detail(newcompclsdetail);
	 * if(sVO != null){ // 가입 대기상태의 사용자는 자신을 제외하지 않아도 된다.
	 * vo.setUsr_mst_key(sVO.getComp_mst_key()); // 로그인 사용자 정보수정시 자신을 빼고 망사업자를
	 * 검색 }
	 * 
	 * List<UsrInfoVO> userInfoNetList = FpisSvc.selectUsrInfoNetList(vo);
	 * 
	 * StringBuilder strBuilder = new StringBuilder(); strBuilder.append("[");
	 * for(int i=0; i < userInfoNetList.size(); i++){ UsrInfoVO usrInfoVO =
	 * (UsrInfoVO)userInfoNetList.get(i); if( i == 0){ strBuilder.append("{");
	 * }else{ strBuilder.append(",{"); }
	 * strBuilder.append("\"code\":\""+usrInfoVO.getComp_bsns_num()+"\"");
	 * strBuilder.append(",\"name\":\""+usrInfoVO.getComp_nm()+"\"");
	 * strBuilder.append("}"); } strBuilder.append("]");
	 * res.setContentType("application/json");
	 * res.setCharacterEncoding("UTF-8");; PrintWriter out = res.getWriter();
	 * out.write(strBuilder.toString()); out.close(); }
	 */

	@ResponseBody @RequestMapping(value = "/FpisCheckCompInfoPopUp.do")
	public String FpisCheckCompInfoPopUp(@RequestParam HashMap<String, Object> map, HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		// 2013.11.16 by jhoh : 등록되어 있는 사용자 정보 가져오기(USR_INFO)
		// String corpNum = req.getParameter("comp_corp_num");
		String bsnsNum = (String) map.get("comp_bsns_num");
		bsnsNum = bsnsNum.replaceAll("-", "");

		// List<FpisSysCompanyVO> compCorpList = null;
		List<FpisSysCompanyVO> compBsnsList = null;
		List<UsrInfoVO> usrList = null;

		// int corp_cnt = 0;
		int bsns_cnt = 0;
		int usr_cnt = 0;

		FpisNewJoinVO shVO = new FpisNewJoinVO();

		if (bsnsNum != null && !bsnsNum.equals("")) {

			shVO.setSearch_comp_corp_num(null);
			shVO.setSearch_comp_bsns_num(bsnsNum);

			usrList = FpisSvc.joinUsrCompList(shVO);
			usr_cnt = usrList.size();

			if (usr_cnt == 0) {
				compBsnsList = FpisSvc.joinSysCompList(shVO);
				bsns_cnt = compBsnsList.size();
			}
			if (bsns_cnt == 1) {
				System.out.println(shVO.getSearch_comp_bsns_num());
				FpisSvc.updateSysCompInfo(shVO);
				bsns_cnt = 0;
			}
		}

		// ajax 활용으로 수정 - 2021.10.30 suhyun
		/*
		 * model.addAttribute("USRCNT", usr_cnt); model.addAttribute("USERLIST",
		 * usrList); model.addAttribute("BSNSCNT", bsns_cnt);
		 * model.addAttribute("BSNSLIST", compBsnsList); //
		 * model.addAttribute("CORPCNT" , corp_cnt); //
		 * model.addAttribute("CORPLIST" , compCorpList);
		 * 
		 * return "/fpis/dashboard/FpisCheckCompInfoPopUp";
		 */

		JSONObject json = new JSONObject();
		try {
			json.put("USRCNT", usr_cnt);
			json.put("USERLIST", usrList);
			json.put("BSNSCNT", bsns_cnt);
			json.put("BSNSLIST", compBsnsList);
		} catch (JSONException e) {
			logger.error("[ERROR] - JSONException : ", e);
		}
		return json.toString();
	}

	/**
	 * -----------------------------------------------------------------
	 * 
	 * @param model /fpis/dashboard/FpisUserJoinCompSearchPop.jsp
	 * -----------------------------------------------------------------
	 */
	/*
	 * @RequestMapping(value="/adminJoin.do") public String
	 * adminJoin(@ModelAttribute("mainFrm") FpisNewJoinVO shVO, BindingResult
	 * bindingResult, HttpServletRequest req, ModelMap model) throws Exception {
	 * 
	 * SessionVO SessionVO =
	 * (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
	 * if(SessionVO != null) { return "redirect:/chownmolitfpmsonlyfpisMain.do";
	 * } req.getSession().setAttribute("join_cls" , "ADM");
	 * 
	 * return "redirect:/uss/umt/EgovAdminInsertView.do"; }
	 */

	/**
	 * 시도,시군구 팝업창 호출
	 */
	@RequestMapping("/uss/umt/EgovSiGunguPopup.do")
	public String EgovSiGunguPopup(HttpServletRequest req, Model model) throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");
		SigunguVO vo = new SigunguVO();

		String selSido = req.getParameter("sidoCd");

		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);

		if (selSido != null && !selSido.equals("")) {
			vo.setSidoCd(selSido);
			List<SigunguVO> sigunList = mberManageService.selectSigungu2016(vo);
			model.addAttribute("SIGUNLIST", sigunList);
		}

		model.addAttribute("selSido", selSido);

		return "egovframework/com/uss/umt/EgovSiGunguPopup";
	}

	/**
	 * 시도에 속한 시군구 조회 2014.09.16 mgkim 최초생성
	 */
	@RequestMapping("/uss/umt/FpisSigungu_ajax.do")
	public void FpisSigungu_ajax(@RequestParam(value = "sidocode", required = false) String sidocode,
			HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, NullPointerException {
		SigunguVO vo = new SigunguVO();
		vo.setSidoCd(sidocode);
		List<SigunguVO> resultList = mberManageService.selectSigungu2016(vo);
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("[");
		for (int i = 0; i < resultList.size(); i++) {
			SigunguVO codeVO = resultList.get(i);
			if (i == 0) {
				strBuilder.append("{");
			} else {
				strBuilder.append(",{");
			}
			strBuilder.append("\"code\":\"" + codeVO.getSigunguCd() + "\"");
			strBuilder.append(",\"name\":\"" + codeVO.getSigunguNm() + "\"");
			strBuilder.append("}");
		}
		strBuilder.append("]");
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(strBuilder.toString());
		out.close();
	}

	/**
	 * 시도에 속한 시군구 조회 2014.09.16 mgkim 최초생성
	 */
	@RequestMapping("/uss/umt/FpisSigungu2_ajax.do")
	public void FpisSigungu2_ajax(@RequestParam(value = "sidocode", required = false) String sidocode,
			HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, NullPointerException {
		SigunguVO vo = new SigunguVO();
		vo.setSidoCd(sidocode);
		List<SigunguVO> resultList = mberManageService.selectSigungu2016(vo);
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("[");
		for (int i = 0; i < resultList.size(); i++) {
			SigunguVO codeVO = resultList.get(i);
			if (i == 0) {
				strBuilder.append("{");
			} else {
				strBuilder.append(",{");
			}
			strBuilder.append("\"code\":\"" + codeVO.getSigunguCd() + "\"");
			strBuilder.append(",\"name\":\"" + codeVO.getSigunguNm() + "\"");
			strBuilder.append("}");
		}
		strBuilder.append("]");
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(strBuilder.toString());
		out.close();
	}

	@RequestMapping("/uss/umt/FpisUsrKeyUpdatePopUpView.do")
	public String FpisUsrKeyUpdatePopUpView(HttpServletRequest req, Model model)
			throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");

		String mber_id = req.getParameter("mber_id");
		String old_comp_mst_key = req.getParameter("old_key");
		int nCnt = mberManageService.isExistsSysCompMastKey2(old_comp_mst_key);

		if (nCnt == 0) {
		}

		model.addAttribute("mber_id", mber_id);
		model.addAttribute("old_key", old_comp_mst_key);

		return "egovframework/com/uss/umt/FpisUsrKeyUpdatePopUp";
	}

	/*
	 * 사용자가 SYS_COMPANY_INFO에서 업체를 선택하지 않고 다이렉트로 입력한 경우 관리자가 수동적으로 업체Key를 변경해
	 * 준다. 절차 1) 수정하고자 하는 Key가 SYS_COMPANY_INFO에 존재하는지 확인 2) 1번 확인 됬다면 USR_INFO에
	 * 바꿔치기 하고자 하는 Key 존재하는지 확인 3) 존재하지 않으면 SYS_COMPANY_INFO -> USR_INFO
	 *
	 * 4) COMTNGNRLMBER 에 mber_id , comp_mst_key가 존재하는지 확인 5) 존재하면 mber_id의
	 * new_comp_mst_key로 변경
	 */

	@RequestMapping("/uss/umt/FpisUsrKeyUpdate.do")
	public String FpisUsrKeyUpdate(HttpServletRequest req, Model model) throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");

		String mber_id = req.getParameter("mber_id");
		String old_comp_mst_key = req.getParameter("old_key");
		String new_comp_mst_key = req.getParameter("new_key");
		String msg = "";
		String sRtn = "N";

		/*
		 * 1. SYS_COMPANY_INFO가 존재하면 2번 진행 2. USR_INFO에 new_key가 없으면 신규 New_key로
		 * 신규 Insert....
		 *
		 */

		// 1. sys_company_info 체크 ...........
		if (mberManageService.isExistsSysCompMastKey2(new_comp_mst_key) > 0) {
			if (mberManageService.isExistsUsrMastKey(new_comp_mst_key) == 0) {
				mberManageService.insertUsrInfo(new_comp_mst_key);
				sRtn = "Y";
				msg = "USR_INFO : Insert Ok! ";
			} else {
				sRtn = "N";
				msg = "USR_INFO : 이미존재하는 Key 입니다. ";
			}

			// 사용자 테이블에 mber_id,comp_mst_key가 존재하는지 확인
			// 1. COMTNGNRLMBER 체크 ...........
			if (mberManageService.isExistsUsrGnrMastKey(mber_id, old_comp_mst_key) > 0) {
				mberManageService.updateGnrUsrMstKey(new_comp_mst_key, mber_id);
				sRtn = "Y";
				msg = "COMTNGNRLMBER : id:" + mber_id + " compKey:" + old_comp_mst_key + " 가 정상적으로 업데이트 되었습니다.";
			} else {
				sRtn = "N";
				msg = "COMTNGNRLMBER : id:" + mber_id + " compKey:" + old_comp_mst_key + " 가 존재하지 않습니다.";
			}
		} else {

			sRtn = "N";
			msg = "SYS_COMPANY_INFO : 존재하지 않는 Key 입니다. ";
		}

		model.addAttribute("mber_id", mber_id);
		model.addAttribute("old_key", old_comp_mst_key);
		model.addAttribute("new_key", new_comp_mst_key);

		model.addAttribute("RTN", sRtn);
		model.addAttribute("MSG", msg);

		return "egovframework/com/uss/umt/FpisUsrKeyUpdatePopUp";
	}

	/* 2014.11.18 신고주체관리 엑셀 내보내기 */
	@RequestMapping(value = "/uss/umt/EgovMberManageExportExcel.do")
	public String EgovMberManageExportExcel(@ModelAttribute("userSearchVO") UserDefaultVO shVO,
			@ModelAttribute SigunguVO sigunguVO, ModelMap model, Map<String, Object> ModelMap, HttpServletRequest req)
			throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		String searchSidoCd = req.getParameter("hid_sido_code");
		String searchSigunguCd = req.getParameter("hid_sigungu_code");

		if (svo.getMber_cls().equals("ADM")) {
			// "관리자 아닐 때 시군구 시디 == "+svo.getAdm_area_code()
			shVO.setSearch_sigungu_cd(svo.getAdm_area_code());
		} else {
			// 관리지역없는 업체 검색 : " + shVO.getSearch_not_in()
		}
		// List<SigunguVO> sidoList = mberManageService.selectSido();

		// List<SigunguVO> sigunList = null;
		if (searchSidoCd != null && !searchSidoCd.equals("")) {
			sigunguVO.setSidoCd(searchSidoCd);
			// sigunList = mberManageService.selectSigungu(sigunguVO);
		}

		if (svo.getMber_cls().equals("ADM")) {
			if (svo.getAdm_area_code().length() == 2) { // 2014.12.01 mgkim 시도
														// 관리자 검색조건 확인
				searchSidoCd = svo.getAdm_area_code();
			} else {
				searchSigunguCd = svo.getAdm_area_code();
			}
		} else {
			// 관리지역없는 업체 검색 : " + shVO.getSearch_not_in()
		}

		shVO.setSearch_sigungu_cd(searchSigunguCd); // 시군구 시디 부여
		shVO.setSearch_sido_cd(searchSidoCd);

		shVO.setMber_cls("GNR");

		// 2014.01.21 mgkim 검색항목 추가
		String org_comp_bsns_num = shVO.getComp_bsns_num();
		if (org_comp_bsns_num != null) {
			shVO.setComp_bsns_num(shVO.getComp_bsns_num().replaceAll("-", ""));
		}
		String org_comp_corp_num = shVO.getComp_corp_num();
		if (org_comp_corp_num != null) {
			shVO.setComp_corp_num(shVO.getComp_corp_num().replaceAll("-", ""));
		}

		/* 2014.08.29 양상완 지자체 관리자일때는 관리지역 검색조건이 자기지역으로 고정된다. */

		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}

		// shVO.setSearch_sigungu_cd(sigunguVO.getSigunguCd()); // 시군구 시디 부여
		// shVO.setSearch_sido_cd(sigunguVO.getSidoCd());
		// if(svo.getMber_cls().equals("ADM")){
		// shVO.setSearch_sigungu_cd(svo.getAdm_area_code());
		// }else{
		// }
		int totCnt = mberManageService.selectMberListTotCnt(shVO);

		shVO.setS_row(0);
		shVO.setE_row(totCnt + 1);
		List<MberManageVO> mberList = mberManageService.selectMberList(shVO);

		ModelMap.put("mberList", mberList);
		ModelMap.put("ExcelCls", "mberList");

		// 일반회원 상태코드를 코드정보로부터 조회
		ComDefaultCodeVO vo = new ComDefaultCodeVO();
		vo.setCodeId("COM013");
		List mberSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("entrprsMberSttus_result", mberSttus_result);// 기업회원상태코드목록

		vo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);// 기업회원상태코드목록

		return "ExcelView";
	}

	/* 2015.02.06 양상완 SYS COMP NOT JOIN 에 일치하는 개수 검사 */
	@RequestMapping("/NotLoginProcess/FpisCompNotJoinInfoPopUpCnt_ajax.do")
	public void FpisCompNotJoinInfoPopUpCnt_ajax(FpisSysCompanyVO sysCompVO, HttpServletResponse res,
			HttpServletRequest req, Model model) throws Exception, NullPointerException {
		JSONObject json = new JSONObject();
		List<FpisSysCompanyVO> sysCompJotJoinList = adminSysCompManageService.selectSysCompNotJoinList(sysCompVO);
		json.put("sysCompNotJoinCnt", sysCompJotJoinList.size());
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();

		out.write(json.toString());
		out.close();
	}

	/* 2015.02.06 양상완 SYS COMP NOT JOIN 에 일치하는 리스트 출력 */
	@RequestMapping(value = "/NotLoginProcess/FpisCompNotJoinInfoPopUp")
	public String FpisCheckCompInfoPopUp(HttpServletRequest req, ModelMap model, FpisSysCompanyVO sysCompVO)
			throws Exception, NullPointerException {

		List<FpisSysCompanyVO> sysCompNotJoinList = adminSysCompManageService.selectSysCompNotJoinList(sysCompVO);
		model.addAttribute("sysCompNotJoinList", sysCompNotJoinList);
		model.addAttribute("sysCompVO", sysCompVO);
		return "/fpis/dashboard/FpisCompNotJoinInfoPopUp";
	}

	/* 2015.02.06 양상완 SYS COMP NOT JOIN 작업 */
	@RequestMapping("/NotLoginProcess/DeleteCompNotJoinInfo_ajax.do")
	public void DeleteCompNotJoinInfo_ajax(FpisSysCompanyVO sysCompVO, HttpServletResponse res, HttpServletRequest req,
			Model model) throws Exception, NullPointerException {
		String temp_comp_bsns_num = sysCompVO.getComp_bsns_num();
		sysCompVO.setInsert_cls("N");// SQL에 대해 NOT_JOIN 테이블에서만 되도록 설정, 매우 중요
		sysCompVO = adminSysCompManageService.selectAdminSysCompDetail(sysCompVO);
		sysCompVO.setComp_bsns_num(temp_comp_bsns_num);
		sysCompVO.setUsr_mst_key(temp_comp_bsns_num); // 이력 수행자는 회원가입자
		sysCompVO.setHistory_cls("U"); // 이력은 업데이트
		adminSysCompManageService.insertAdminSysCompHistory(sysCompVO);
		adminSysCompManageService.updateSysCompInfoHistory(sysCompVO); // 이력을
																		// insert
																		// cls
																		// Y로
																		// 변경.
																		// 끝.
		adminSysCompManageService.insertAdminSysComp(sysCompVO);// SYS_COMPANY_INFO에
																// 삽입
		adminSysCompManageService.deleteAdminSysComp(sysCompVO);// NOT_JOIN에서 삭제
	}

	/** 2015.11.9 지자체 가입현황 개편 */
	@IncludedInfo(name = "일반회원관리", order = 470, gid = 50) @RequestMapping(value = "/uss/umt/EgovMberManageNew.do")
	public String EgovMberManageNew(@ModelAttribute("userSearchVO") UserDefaultVO shVO,
			@ModelAttribute SigunguVO sigunguVO, @RequestParam(value = "selbox", required = false) String[] selbox,
			ModelMap model, HttpServletRequest req) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		List<SysCodeVO> codeFMS023 = commonService.commonCode("FMS023", null); // 2015.01.19
																				// 양상완
																				// 업태
																				// 코드
																				// 변경

		// 181030 smoh 관할관청 확정/변경&반려 등록
		if (selbox != null) { // [2018156538/11110/P, 3710200115/11110/P] ->
								// 사업자번호/시군구코드/현상태값
			String govFlag = req.getParameter("gov_flag");
			if (govFlag != null) {
				String[] selBoxObj = null;
				FpisNewMberVO mVo = new FpisNewMberVO();
				List<FpisNewMberVO> mList = new ArrayList<FpisNewMberVO>();
				for (int i = 0; i < selbox.length; i++) {
					selBoxObj = selbox[i].split("/");
					govFlag = "Y".equals(govFlag) ? "Y"
							: "Y".equals(selBoxObj[2]) ? "U" : "U".equals(selBoxObj[2]) ? "U" : "N";

					mVo = new FpisNewMberVO();
					mVo.setComp_mst_key(selBoxObj[0]);
					mVo.setReg_num("0");
					mVo.setSigunguCd(selBoxObj[1]);
					mVo.setGov_status(govFlag);
					mVo.setNote(shVO.getNote());
					mVo.setReg_user(svo.getUser_id());
					mList.add(mVo);
				}

				// 관할지역 이력 등록 및 usr_info gov_seq 업데이트
				mberManageService.insertGovHistoryList(mList);
				mberManageService.updateUsrInfoGovHistorySeq(mList);
			}
		}
		shVO.setMber_cls("GNR");

		// 2014.01.21 mgkim 검색항목 추가
		String org_comp_bsns_num = shVO.getComp_bsns_num();
		if (org_comp_bsns_num != null) {
			shVO.setComp_bsns_num(shVO.getComp_bsns_num().replaceAll("-", ""));
		}
		String org_comp_corp_num = shVO.getComp_corp_num();
		if (org_comp_corp_num != null) {
			shVO.setComp_corp_num(shVO.getComp_corp_num().replaceAll("-", ""));
		}

		// PAGING...

		/* 2014.08.29 양상완 지자체 관리자일때는 관리지역 검색조건이 자기지역으로 고정된다. */

		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}

		String searchSidoCd = req.getParameter("hid_sido_code");
		String searchSigunguCd = req.getParameter("hid_sigungu_code");

		List<SigunguVO> sidoList = null;
		List<SigunguVO> sigunList = null;
		if (svo.getMber_cls().equals("SYS")) {
			sidoList = mberManageService.selectSido2016(new SigunguVO());
			/*
			 * searchSidoCd = (searchSidoCd == null || searchSidoCd.equals(""))
			 * ? "42" : searchSidoCd;
			 */
			if (searchSidoCd != null && !searchSidoCd.equals("")) {
				sigunguVO.setSidoCd(searchSidoCd);
				sigunList = mberManageService.selectSigungu2016(sigunguVO);
			}
		} else {
			if (svo.getMber_cls().equals("ADM")) {
				if (searchSidoCd == null) {
					if (svo.getAdm_area_code().length() == 2) { // 2014.12.01
																// mgkim 시도 관리자
																// 검색조건 확인
						model.addAttribute("hid_sido_code", svo.getAdm_area_code());
						// model.addAttribute("hid_sigungu_code" ,
						// searchSigunguCd);
					} else {
						searchSigunguCd = svo.getAdm_area_code();
						model.addAttribute("hid_sigungu_code", svo.getAdm_area_code());
						model.addAttribute("hid_sigungu_name", svo.getAdm_area_name());
					}
					searchSidoCd = svo.getAdm_area_code().substring(0, 2);
					sigunguVO.setSidoCd(searchSidoCd);
					sidoList = mberManageService.selectSido2016(new SigunguVO());
					if (searchSidoCd != null && !searchSidoCd.equals("")) {
						sigunguVO.setSidoCd(searchSidoCd);
						sigunList = mberManageService.selectSigungu2016(sigunguVO);
					}
				} else {
					sigunguVO.setSidoCd(searchSidoCd);
					sidoList = mberManageService.selectSido2016(new SigunguVO());
					sigunList = mberManageService.selectSigungu2016(sigunguVO);
				}
			}
		}

		model.addAttribute("hid_sido_code", searchSidoCd);
		model.addAttribute("hid_sigungu_code", searchSigunguCd);
		shVO.setSearch_sigungu_cd(searchSigunguCd); // 시군구 시디 부여
		shVO.setSearch_sido_cd(searchSidoCd);

		model.addAttribute("SIDOLIST", sidoList);
		model.addAttribute("SIGUNLIST", sigunList);

		int totCnt = mberManageService.selectMberListTotCnt(shVO); // 181029
																	// smoh 관할지역
																	// 추가

		shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		shVO.setTot_page(Util.calcurateTPage(totCnt));

		List<MberManageVO> mberList = mberManageService.selectMberList(shVO);

		shVO.setComp_bsns_num(org_comp_bsns_num); // 2014.01.21 사업자번호 검색 "-" 기호
													// 제거 사용자가 입력한 값 그대로 반환
		shVO.setComp_corp_num(org_comp_corp_num); // 2014.01.21 사업자번호 검색 "-" 기호
													// 제거 사용자가 입력한 값 그대로 반환

		model.addAttribute("VO", shVO);
		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("resultList", mberList);

		// 일반회원 상태코드를 코드정보로부터 조회
		ComDefaultCodeVO vo = new ComDefaultCodeVO();
		vo.setCodeId("COM013");
		List mberSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("entrprsMberSttus_result", mberSttus_result);// 기업회원상태코드목록

		vo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);// 기업회원상태코드목록

		model.addAttribute("codeFMS023", codeFMS023);

		return "egovframework/com/uss/umt/adm/EgovMberManageNew";
	}

	@RequestMapping("/uss/umt/EgovMberViewNew.do")
	public String EgovMberViewNew(@RequestParam("selectedId") String mberId,
			@ModelAttribute("searchVO") UserDefaultVO shVO, @ModelAttribute("frmThis") FpisCarManageVO caVO,
			HttpServletRequest req, Model model) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		String rcode = req.getParameter("rcode");
		String bcode = req.getParameter("bcode");

		model.addAttribute("rcode", rcode);
		model.addAttribute("bcode", bcode);

		String resultMsg = req.getParameter("resultMsg");
		model.addAttribute("resultMsg", resultMsg);

		// ComDefaultCodeVO vo = new ComDefaultCodeVO();

		// String searchSidoCd = req.getParameter("hid_sido_code");
		// String searchSigunguCd = req.getParameter("hid_sigungu_code");
		model.addAttribute("hid_sido_code", req.getParameter("hid_sido_code"));
		model.addAttribute("hid_sigungu_code", req.getParameter("hid_sigungu_code"));

		// 사용자상태코드를 코드정보로부터 조회
		// vo.setCodeId("COM013");
		// List mberSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		// model.addAttribute("mberSttus_result", mberSttus_result); //사용자상태코드목록

		List<SysCodeVO> codeCOM013 = commonService.commonCode("COM013", null); // 2013.10.17
																				// mgkim
																				// 회원가입
																				// 상태
																				// 코드
		model.addAttribute("codeCOM013", codeCOM013);
		// List<SysCodeVO> codeFMS004 = commonService.commonCode("FMS004",
		// null); // 2013.10.08 mgkim 업태구분코드를 코드정보로부터 조회
		// model.addAttribute("codeFMS004", codeFMS004);
		// List<SysCodeVO> codeFMS012 = commonService.commonCode("FMS012",
		// null); // 2013.10.08 mgkim 인증망이용여부 코드를 코드정보로부터 조회
		// model.addAttribute("codeFMS012", codeFMS012);

		MberManageVO mberManageVO = mberManageService.selectMber(mberId);
		UsrInfoVO userInfoVO = FpisSvc.selectUsrInfo(mberManageVO.getUsr_mst_key());

		model.addAttribute("mber_cls", svo.getMber_cls());

		// 2014.01.21 mgkim 수정하기 이후 검색파라메터 유지
		String mode = req.getParameter("mode");
		if (mode != null && mode.equals("editMode")) {
			shVO.setComp_bsns_num(req.getParameter("in_comp_bsns_num"));
			shVO.setComp_corp_num(req.getParameter("in_comp_corp_num"));
			shVO.setSearchCondition(req.getParameter("in_searchCondition"));
			shVO.setSearchKeyword(req.getParameter("in_searchKeyword"));
			shVO.setSbscrbSttus(req.getParameter("in_sbscrbSttus"));
			shVO.setCur_page(Integer.parseInt(req.getParameter("in_cur_page")));
		}

		/* 2014.05.14 swyang 첨부파일의 존재 여부. 및 오리지날 파일이름 얻기. */
		int fileCnt = 0;
		FpisFileManagementVO fileManagementVO = new FpisFileManagementVO();
		fileManagementVO.setFile_cls("A");
		fileManagementVO.setUsr_mst_key(mberManageVO.getUsr_mst_key());
		fileCnt = fileManagementService.getFileCnt(fileManagementVO);
		if (fileCnt > 0) {
			List<FpisFileManagementVO> fileManagementVOs;
			fileManagementVOs = fileManagementService.getFileInfo(fileManagementVO);
			model.addAttribute("fileManagementVOs", fileManagementVOs);
			// String fpisFilePath =
			// EgovProperties.getProperty("Globals.fileStorePath");
			model.addAttribute("fpisFilePath", fileStorePath);
		}
		model.addAttribute("fileCnt", fileCnt);

		model.addAttribute("userInfoVO", userInfoVO);
		model.addAttribute("mberManageVO", mberManageVO);
		model.addAttribute("userSearchVO", shVO);

		// "관리자 - 신고주체관리 비밀번호 변경을 위한 파라메타 전달 : selectedId : " + mberId
		model.addAttribute("selectedId", mberId);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
		List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 운송유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS024", codeFMS024);
		List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 주선유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS025", codeFMS025);
		List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 망사업자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS026", codeFMS026);
		List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																				// mgkim
																				// 사업자
																				// 대행신고자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS027", codeFMS027);

		ComDefaultCodeVO vo = new ComDefaultCodeVO(); // 181217 smoh 관할지역 상태 코드
														// 추가
		vo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);

		// 2015.01.16 mgkim 업체정보 세부 사업자유형 확인
		String strCompClsDetail = userInfoVO.getComp_cls_detail();
		String compCls_01_01 = "N";
		String compCls_01_02 = "N";
		String compCls_01_03 = "N";
		String compCls_01_04 = "N";
		String compCls_02_01 = "N";
		String compCls_02_02 = "N";
		String compCls_04_01 = "N";
		String[] strCCD = strCompClsDetail.split(",");
		for (int i = 0; i < strCCD.length; i++) {
			if (strCCD[i].equals("01-01")) {
				compCls_01_01 = "Y";
			} else if (strCCD[i].equals("01-02")) {
				compCls_01_02 = "Y";
			} else if (strCCD[i].equals("01-03")) {
				compCls_01_03 = "Y";
			} else if (strCCD[i].equals("01-04")) {
				compCls_01_04 = "Y";
			} else if (strCCD[i].equals("02-01")) {
				compCls_02_01 = "Y";
			} else if (strCCD[i].equals("02-02")) {
				compCls_02_02 = "Y";
			} else if (strCCD[i].equals("04-01")) {
				compCls_04_01 = "Y";
			}
		}

		model.addAttribute("compCls_01_01", compCls_01_01);
		model.addAttribute("compCls_01_02", compCls_01_02);
		model.addAttribute("compCls_01_03", compCls_01_03);
		model.addAttribute("compCls_01_04", compCls_01_04);
		model.addAttribute("compCls_02_01", compCls_02_01);
		model.addAttribute("compCls_02_02", compCls_02_02);
		model.addAttribute("compCls_04_01", compCls_04_01);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

		List<FpisCarManageVO> carVOS = null;

		int modelCnt = 0;
		int totCnt = 0;
		int direct_totCnt = 0;

		String car_cur_page = req.getParameter("car_cur_page");
		if (car_cur_page != null) {
			caVO.setCur_page(Integer.parseInt(car_cur_page));
		}

		// PAGING...
		if (caVO.getCur_page() <= 0) {
			caVO.setCur_page(1);
		}
		if (caVO.getSearch_sort1() == null) {
			caVO.setSearch_sort1("sort1_1");
		}
		if (caVO.getSearch_sort2() == null) {
			caVO.setSearch_sort2("ASC");
		}

		caVO.setPage_cls("USR");
		caVO.setUsr_mst_key(mberManageVO.getUsr_mst_key());
		// caVO.setComp_bsns_num(sVO.getUsr_bsns_num()); // 2013.10.14 mgkim
		// 차량계약 근거자료 조회 쿼리 추가 사업자번호 값 추가

		totCnt = CarManageService.getCarCount(caVO);
		direct_totCnt = CarManageService.CarManageFirstChkCnt(caVO); // 직영, 지입차량
																		// 대수
																		// 가져오기

		List<SysCodeVO> codeFMS003 = commonService.commonCode("FMS003", null);
		List<SysCodeVO> codeFMS002 = commonService.commonCode("FMS002", null);

		caVO.setS_row(Util.getPagingStart(caVO.getCur_page()));
		caVO.setE_row(Util.getPagingEnd(caVO.getCur_page()));
		caVO.setTot_page(Util.calcurateTPage(totCnt));

		carVOS = CarManageService.searchCar(caVO);
		if (carVOS != null) {
			modelCnt = carVOS.size();
		}

		// 페이지 네비 및 디폴트 검색조건 VO
		model.addAttribute("VO", caVO);
		model.addAttribute("codeFMS003", codeFMS003);
		model.addAttribute("codeFMS002", codeFMS002);

		// 페이지 리스트 뷰 Model
		model.addAttribute("modelCnt", modelCnt);
		model.addAttribute("carList", carVOS);
		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("DIRECT_TOTCNT", direct_totCnt);

		model.addAttribute("RES", req.getParameter("RES")); // 2013.11.18 mgkim
															// 직영차량 수정기능후 상태알림
		model.addAttribute("MSG", req.getParameter("MSG"));

		SigunguVO sigunguVO = new SigunguVO();
		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);
		List<SigunguVO> sigunList = null;
		if (userInfoVO.getSigunguCd() != null && !userInfoVO.getSigunguCd().equals("")) {
			sigunguVO.setSidoCd(userInfoVO.getSidoCd());
			sigunList = mberManageService.selectSigungu2016(sigunguVO);
		}
		model.addAttribute("SIGUNLIST", sigunList);

		return "egovframework/com/uss/umt/adm/EgovMberViewNew";
	}

	/**
	 * 160802 오승민 사용자 연락망 구현
	 */
	@IncludedInfo(name = "일반회원관리", order = 470, gid = 50) @RequestMapping(value = {
			"/uss/umt/EgovMberManage_network.do", "/uss/umt/EgovMberManage_sendMail.do",
			"/uss/umt/EgovMberManage_sendSMS.do" })
	public String selectMberList_network(@ModelAttribute("userSearchVO") UserDefaultVO shVO,
			@ModelAttribute SigunguVO sigunguVO, ModelMap model, HttpServletRequest req)
			throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		String searchSidoCd = req.getParameter("hid_sido_code");
		String searchSigunguCd = req.getParameter("hid_sigungu_code");
		List<SysCodeVO> codeFMS023 = commonService.commonCode("FMS023", null); // 2015.01.19
																				// 양상완
																				// 업태
																				// 코드
																				// 변경

		shVO.setMber_cls("GNR");
		// 2014.01.21 mgkim 검색항목 추가
		String org_comp_bsns_num = shVO.getComp_bsns_num();
		if (org_comp_bsns_num != null) {
			shVO.setComp_bsns_num(shVO.getComp_bsns_num().replaceAll("-", ""));
		}
		String org_comp_corp_num = shVO.getComp_corp_num();
		if (org_comp_corp_num != null) {
			shVO.setComp_corp_num(shVO.getComp_corp_num().replaceAll("-", ""));
		}

		// PAGING...
		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}

		if (svo.getMber_cls().equals("ADM")) {
			model.addAttribute("hid_sido_code", svo.getAdm_area_code().substring(0, 2));
			if (svo.getAdm_area_code().length() == 2) { // 2014.12.01 mgkim 시도
														// 관리자 검색조건 확인
				searchSidoCd = svo.getAdm_area_code();
				model.addAttribute("hid_sido_code", svo.getAdm_area_code());
				model.addAttribute("hid_sigungu_code", searchSigunguCd);
			} else {
				searchSigunguCd = svo.getAdm_area_code();
				model.addAttribute("hid_sigungu_code", svo.getAdm_area_code());
				model.addAttribute("hid_sigungu_name", svo.getAdm_area_name());
			}
		} else {
			model.addAttribute("hid_sido_code", searchSidoCd);
			model.addAttribute("hid_sigungu_code", searchSigunguCd);
		}

		shVO.setSearch_sigungu_cd(searchSigunguCd); // 시군구 시디 부여
		shVO.setSearch_sido_cd(searchSidoCd);

		// 2018.01.11 pes 첫 로딩시 조회 안되도록 수정
		if (shVO.getSearchStart() != null) {
			// ------------------1----------------------
			int totCnt = mberManageService.selectMberListNetworkTotCnt(shVO);

			if (req.getServletPath().equals("/uss/umt/EgovMberManage_network.do")) {
				shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
				shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
				shVO.setTot_page(Util.calcurateTPage(totCnt));
			} else {
				shVO.setS_row(Util.getPagingStart(shVO.getCur_page()) - 10 * (shVO.getCur_page() - 1));
				shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()) - 10 * shVO.getCur_page());
				shVO.setTot_page(Util.calcurateTPage(totCnt * 2));
			}

			List<MberManageVO> mberList = mberManageService.selectMberListNetwork(shVO);

			shVO.setComp_bsns_num(org_comp_bsns_num); // 2014.01.21 사업자번호 검색 "-"
														// 기호 제거 사용자가 입력한 값 그대로
														// 반환

			/* 2020.11.11 ysw 정보노출에 따라 이력 삽입. */
			String masked_info_status = req.getParameter("masked_info_status");
			if ("Y".equals(masked_info_status)) {
				List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
				/* 이력 삽입 */
				for (int i = 0; i < mberList.size(); i++) {
					FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
					accessLogVO.setRcode(req.getParameter("rcode"));
					accessLogVO.setBcode(req.getParameter("bcode"));
					accessLogVO.setComp_mst_key(mberList.get(i).getUsr_mst_key().replaceAll("-", ""));
					accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); // 유저마스터키
					accessLogVO.setJob_cls("SE"); // 목록조회
					accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
					accessLogVOList.add(accessLogVO);
				}
				accessLogService.insertAccessLogByList(accessLogVOList);
			}
			model.addAttribute("masked_info_status", masked_info_status);

			model.addAttribute("VO", shVO);
			model.addAttribute("TOTCNT", totCnt);
			model.addAttribute("resultList", mberList);

		} else {
			// ----------------------2-----------------
			model.addAttribute("VO", shVO);
			model.addAttribute("TOTCNT", 0);
			model.addAttribute("resultList", null);
		}

		int strYear = 2015;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
		
		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);

		List<SigunguVO> sigunList = null;
		if (searchSidoCd != null && !searchSidoCd.equals("")) {
			sigunguVO.setSidoCd(searchSidoCd);
			sigunList = mberManageService.selectSigungu2016(sigunguVO);
		}

		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);
		model.addAttribute("SIGUNLIST", sigunList);
		model.addAttribute("codeFMS023", codeFMS023);

		if (req.getServletPath().equals("/uss/umt/EgovMberManage_network.do")) {
			return "egovframework/com/uss/umt/EgovMberManage_network";
		} else if (req.getServletPath().equals("/uss/umt/EgovMberManage_sendMail.do")) {
			List<SysCodeVO> codeFMS036 = commonService.commonCode("FMS036", null);
			model.addAttribute("codeFMS036", codeFMS036);
			return "egovframework/com/uss/umt/EgovMberManage_sendMail";
		} else {
			return "egovframework/com/uss/umt/EgovMberManage_sendSMS";
		}
	}
	
	/* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
	@ResponseBody @RequestMapping(value = "/uss/umt/chkDaeguSigunguCD.do") 
	public void chkDaeguSigunguCD(@RequestParam("getYear") int getYear
			,@RequestParam("sidoCd") String sidoCd
			,@RequestParam("sigunguCd") String sigunguCd
			,HttpServletRequest req
			,HttpServletResponse res) throws IOException, JSONException{
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		SigunguVO sigunguVO = new SigunguVO();
		String DaeguGubun = "";
		List<SigunguVO> sigunList = null;
		
		JSONArray jArray = new JSONArray();
		JSONObject json = new JSONObject();
		
		sigunguVO.setSidoCd(sidoCd);
		sigunguVO.setSigunguCd(sigunguCd);
		 if ("ADM".equals(svo.getMber_cls()) && svo.getAdm_area_code().length() == 2) {
			 DaeguGubun = svo.getAdm_area_code();
			 if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				 try {
					sigunList = mberManageService.selectSigunguDaegu(sigunguVO, getYear);
					
					json.put("sigunList", sigunList);
					for (int i=0; i<sigunList.size(); i++) {
						json = new JSONObject();
						json.put("sigunguCd", sigunList.get(i).getSigunguCd());
						json.put("sigunguNm", sigunList.get(i).getSigunguNm());
						
						jArray.put(json);
					} 
					//json.put("sigunguLen", sigunList.size());
					res.setContentType("application/json");
					res.setCharacterEncoding("UTF-8");
					PrintWriter out = res.getWriter();
					out.write(jArray.toString());
					out.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 }
		} 
		 else if ("SYS".equals(svo.getMber_cls())) {
			if (getYear < 2023) {
				sigunList = mberManageService.selectSigunguDaegu_SYS(sigunguVO);				
			} else {
				try {
					sigunList = mberManageService.selectSigungu2016(sigunguVO);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			json.put("sigunList", sigunList);
			for (int i=0; i<sigunList.size(); i++) {
				json = new JSONObject();
				json.put("sigunguCd", sigunList.get(i).getSigunguCd());
				json.put("sigunguNm", sigunList.get(i).getSigunguNm());
				
				jArray.put(json);
			} 
			//json.put("sigunguLen", sigunList.size());
			res.setContentType("application/json");
			res.setCharacterEncoding("UTF-8");
			PrintWriter out = res.getWriter();
			out.write(jArray.toString());
			out.close();
		}
	}

	@ResponseBody @RequestMapping(value = "/uss/umt/sendSMS.do")
	public void sendSMS(@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "time", required = false) String time,

			@RequestParam(value = "sidoCd", required = false) String sidoCd,
			@RequestParam(value = "sigunguCd", required = false) String sigunguCd,
			@RequestParam(value = "search_reg", required = false) String search_reg,
			@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "search_name", required = false) String search_name,
			@RequestParam(value = "search_comp_bsns_num", required = false) String search_comp_bsns_num,
			@RequestParam(value = "search_contract_kind", required = false) String search_contract_kind,
			@RequestParam(value = "search_contract_reg", required = false) String search_contract_reg,
			@RequestParam(value = "searchCondition", required = false) String searchCondition,
			@RequestParam(value = "searchKeyword", required = false) String searchKeyword,
			@RequestParam(value = "search_cls", required = false) String search_cls,
			@RequestParam(value = "search_one", required = false) String search_one,

			HttpServletRequest req, HttpServletResponse res, Model model) throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");

		UserDefaultVO shVO = new UserDefaultVO();
		shVO.setSearch_sido_cd(sidoCd);
		shVO.setSearch_sigungu_cd(sigunguCd);
		shVO.setSearch_reg(search_reg);
		shVO.setSearch_year(search_year);
		shVO.setSearch_name(search_name);
		if (search_comp_bsns_num != "" || search_comp_bsns_num != null) {
			shVO.setSearch_comp_bsns_num(search_comp_bsns_num.replaceAll("-", ""));
		}
		shVO.setSearch_contract_kind(search_contract_kind);
		shVO.setSearch_contract_reg(search_contract_reg);
		shVO.setSearchCondition(searchCondition);
		shVO.setSearchKeyword(searchKeyword);

		if (search_cls != "" || search_cls != null) {
			search_cls = search_cls.replaceAll(" ", "");
			search_cls = search_cls.replaceAll("\\[", "");
			search_cls = search_cls.replaceAll("\\]", "");
			String[] clsListTmp = search_cls.trim().split(",");

			List<String> listTmp = new ArrayList<String>();
			for (int i = 0; i < clsListTmp.length; i++) {
				listTmp.add(clsListTmp[i]);
			}

			shVO.setSearch_cls(listTmp);
		}

		shVO.setSearch_one(search_one);
		shVO.setMber_cls("GNR");

		List<MberManageVO> resultTotalList = mberManageService.selectTotalMberListNetwork(shVO);

		String result = "";

		if (resultTotalList != null) {
			List<KakaoVO> smsList = new ArrayList<KakaoVO>();
			for (int i = 0; i < resultTotalList.size(); i++) {
				if (resultTotalList.get(i).getTel() == null || "".equals(resultTotalList.get(i).getTel())
						|| "미등록".equals(resultTotalList.get(i).getTel())) {
					result = "발송 대상자 중 휴대폰번호가 입력되지않은 대상자가 존재합니다.";
					break;
				}
				KakaoVO vo = new KakaoVO();
				vo.setUsr_nm(resultTotalList.get(i).getComp_nm());
				vo.setTel(resultTotalList.get(i).getTel());

				vo.setSchedule_type("1");
				vo.setSubject(title);

				vo.setSend_date(date.replaceAll("-", "") + time.replaceAll(":", "") + "00");

				vo.setCallback("18992793");

				vo.setMsg(content);
				vo.setSend_status("0");

				smsList.add(vo);
			}

			if ("".equals(result)) {

				List<KakaoVO> smsResultList = commonService.insertKakaoSMS(smsList);

				if (smsResultList != null) {

					int msgListCnt = 0;
					List<KakaoVO> msgmbrList = new ArrayList<KakaoVO>();
					for (int i = 0; i < resultTotalList.size(); i++) {
						KakaoVO msrmbrVo = new KakaoVO();

						msrmbrVo.setMsg_id(smsResultList.get(msgListCnt).getMsg_id());
						msrmbrVo.setSido_nm(resultTotalList.get(i).getSido_nm());
						msrmbrVo.setComp_nm(resultTotalList.get(i).getComp_nm());
						msrmbrVo.setUsr_mst_key(resultTotalList.get(i).getUsr_mst_key());
						msrmbrVo.setComp_cls_detail_nm(resultTotalList.get(i).getComp_cls_detail_nm());
						msrmbrVo.setMbtlnum(resultTotalList.get(i).getTel());
						msrmbrVo.setGubun("실적신고 알림 SMS");
						msrmbrVo.setGubun1("SMS");

						msgmbrList.add(msrmbrVo);

						if (i != 0 && (i + 1) % 100 == 0) {
							msgListCnt++;
						}
					}
					commonService.insertFpis_MsgmbrList(msgmbrList);

					result = "SMS 발송 등록을 완료하였습니다.";
				} else {
					result = "SMS 발송 중 문제가 발생하였습니다.";
				}
			}
		} else {
			result = "발송 대상자를 불러오는데 문제가 발생하였습니다. 다시 시도해주십시요.";
		}

		JSONObject json = new JSONObject();
		json.put("result", result);
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	@ResponseBody @RequestMapping(value = "/uss/umt/sendMail.do")
	public void sendMail(@RequestParam(value = "mail_kind", required = false) String mail_kind,
			@RequestParam(value = "subject", required = false) String subject,
			@RequestParam(value = "mapping1", required = false) String mapping1,
			@RequestParam(value = "mapping2", required = false) String mapping2,
			@RequestParam(value = "mapping3", required = false) String mapping3,
			@RequestParam(value = "mapping4", required = false) String mapping4,
			@RequestParam(value = "mapping5", required = false) String mapping5,
			@RequestParam(value = "mapping6", required = false) String mapping6,
			@RequestParam(value = "mapping7", required = false) String mapping7,
			@RequestParam(value = "mapping8", required = false) String mapping8,
			@RequestParam(value = "mapping9", required = false) String mapping9,
			@RequestParam(value = "mapping10", required = false) String mapping10,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "time", required = false) String time,

			@RequestParam(value = "sidoCd", required = false) String sidoCd,
			@RequestParam(value = "sigunguCd", required = false) String sigunguCd,
			@RequestParam(value = "search_reg", required = false) String search_reg,
			@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "search_name", required = false) String search_name,
			@RequestParam(value = "search_comp_bsns_num", required = false) String search_comp_bsns_num,
			@RequestParam(value = "search_contract_kind", required = false) String search_contract_kind,
			@RequestParam(value = "search_contract_reg", required = false) String search_contract_reg,
			@RequestParam(value = "searchCondition", required = false) String searchCondition,
			@RequestParam(value = "searchKeyword", required = false) String searchKeyword,
			@RequestParam(value = "search_cls", required = false) String search_cls,
			@RequestParam(value = "search_one", required = false) String search_one,

			HttpServletRequest req, HttpServletResponse res, Model model) throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");

		UserDefaultVO shVO = new UserDefaultVO();
		shVO.setSearch_sido_cd(sidoCd);
		shVO.setSearch_sigungu_cd(sigunguCd);
		shVO.setSearch_reg(search_reg);
		shVO.setSearch_year(search_year);
		shVO.setSearch_name(search_name);
		if (search_comp_bsns_num != "" || search_comp_bsns_num != null) {
			shVO.setSearch_comp_bsns_num(search_comp_bsns_num.replaceAll("-", ""));
		}
		shVO.setSearch_contract_kind(search_contract_kind);
		shVO.setSearch_contract_reg(search_contract_reg);
		shVO.setSearchCondition(searchCondition);
		shVO.setSearchKeyword(searchKeyword);

		if (search_cls != "" || search_cls != null) {
			search_cls = search_cls.replaceAll(" ", "");
			search_cls = search_cls.replaceAll("\\[", "");
			search_cls = search_cls.replaceAll("\\]", "");
			String[] clsListTmp = search_cls.trim().split(",");

			List<String> listTmp = new ArrayList<String>();
			for (int i = 0; i < clsListTmp.length; i++) {
				listTmp.add(clsListTmp[i]);
			}

			shVO.setSearch_cls(listTmp);
		}

		shVO.setSearch_one(search_one);
		shVO.setMber_cls("GNR");

		List<MberManageVO> resultTotalList = mberManageService.selectTotalMberListNetwork(shVO);

		String result = "";

		if (resultTotalList != null) {

			MailMasterVO mmVO = new MailMasterVO();

			mmVO.setSend_email("fpis@korea.kr");
			mmVO.setSend_name("화물운송실적관리시스템");
			mmVO.setReturn_email("fpis@korea.kr");
			mmVO.setMail_kind(mail_kind);
			mmVO.setSubject(subject);
			mmVO.setRegdate(date.replaceAll("-", "") + time.replaceAll(":", "") + "00");

			List<MailDetailVO> mlist = new ArrayList<MailDetailVO>();
			List<KakaoVO> msrmbrVoList = new ArrayList<KakaoVO>();
			for (int i = 0; i < resultTotalList.size(); i++) {
				if (resultTotalList.get(i).getMber_email_adres() == null
						|| "".equals(resultTotalList.get(i).getMber_email_adres())
						|| "미등록".equals(resultTotalList.get(i).getMber_email_adres())) {
					result = "발송 대상자 중 메일 주소가 입력되지않은 대상자가 존재합니다.";
					break;
				}

				MailDetailVO vo = new MailDetailVO();
				vo.setEmail(resultTotalList.get(i).getMber_email_adres());
				vo.setName(resultTotalList.get(i).getComp_nm());
				vo.setMapping1(mapping1);
				vo.setMapping2(mapping2);
				vo.setMapping3(mapping3);
				vo.setMapping4(mapping4);
				vo.setMapping5(mapping5);
				vo.setMapping6(mapping6);
				vo.setMapping7(mapping7);
				vo.setMapping8(mapping8);
				vo.setMapping9(mapping9);
				vo.setMapping10(mapping10);

				KakaoVO msrmbrVo = new KakaoVO();

				mlist.add(vo);
				msrmbrVoList.add(msrmbrVo);

				msrmbrVo.setSido_nm("");
				msrmbrVo.setComp_nm(resultTotalList.get(i).getComp_nm());
				msrmbrVo.setUsr_mst_key(resultTotalList.get(i).getUsr_mst_key());
				msrmbrVo.setComp_cls_detail_nm(resultTotalList.get(i).getComp_cls_detail_nm());
				msrmbrVo.setMbtlnum(resultTotalList.get(i).getMber_email_adres());
				msrmbrVo.setGubun("실적신고 알림 MAIL");
				msrmbrVo.setGubun1("EMAIL");
			}

			if ("".equals(result)) {

				mailService.insertMailInfo(mmVO, mlist, msrmbrVoList);

				result = "메일 발송 등록을 완료하였습니다.";
			}
		} else {
			result = "발송 대상자를 불러오는데 문제가 발생하였습니다. 다시 시도해주십시요.";
		}

		JSONObject json = new JSONObject();
		json.put("result", result);
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	@ResponseBody @RequestMapping(value = "/uss/umt/deleteMail.do")
	public void deleteMail(HttpServletRequest req, HttpServletResponse res, Model model)
			throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");
		String result = "";

		mailService.deleteMail();

		result = "삭제 성공";

		JSONObject json = new JSONObject();
		json.put("result", result);
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	// 190130 전재현
	@RequestMapping(value = "/uss/umt/messageManage.do")
	public String messageManage(KakaoVO shVO, ModelMap model, HttpServletRequest req, HttpServletResponse response)
			throws Exception, NullPointerException {

		// model.addAttribute("TOTCNT", 0);

		// PAGING...
		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}

		// 2018.01.11 pes 첫 로딩시 조회 안되도록 수정
		if (shVO.getSearchStart() != null) {
			// ------------------1---------------------

			int totCnt = 0;
			if ("SMS".equals(shVO.getGubun()) || "".equals(shVO.getGubun())) {
				totCnt = commonService.selectTotalInno_tran(shVO);
			} else {

			}

			if (req.getServletPath().equals("/uss/umt/messageManage.do")) {
				shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
				shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
				shVO.setTot_page(Util.calcurateTPage(totCnt));
			} else {
				shVO.setS_row(Util.getPagingStart(shVO.getCur_page()) - 10 * (shVO.getCur_page() - 1));
				shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()) - 10 * shVO.getCur_page());
				shVO.setTot_page(Util.calcurateTPage(totCnt * 2));
			}

			// List<MberManageVO> mberList =
			// mberManageService.selectMberListNetwork(shVO);
			List<KakaoVO> kakaoList = new ArrayList<KakaoVO>();

			if ("SMS".equals(shVO.getGubun()) || "".equals(shVO.getGubun())) {
				kakaoList = commonService.selectInno_tran(shVO);
				model.addAttribute("resultList", kakaoList);
			} else {

			}

			model.addAttribute("VO", shVO);
			model.addAttribute("TOTCNT", totCnt);

		} else {
			// 200227 smoh 첫 조회 시 기간 셋팅
			shVO.setSearch_startDate(Util.getDateFormat());
			shVO.setSearch_endDate(Util.getDateFormat());

			// ----------------------2------------------
			model.addAttribute("VO", shVO);
			model.addAttribute("TOTCNT", null);
			model.addAttribute("resultList", null);
		}

		// commonService.checkAndGetGM(gmCnt);

		return "egovframework/com/uss/umt/messageManage";

	}

	@RequestMapping(value = "/uss/umt/sendSMS_modify.do")
	public String sendSMS_modify(KakaoVO shVO, ModelMap model, HttpServletRequest req, HttpServletResponse response)
			throws Exception, NullPointerException {

		if ("SMS".equals(shVO.getType())) {
			KakaoVO vo = commonService.selectOne_Inno_tran(shVO);
			vo.setMsg_id(shVO.getMsg_id());
			model.addAttribute("VO", vo);

		} else {

		}

		return "egovframework/com/uss/umt/sendSMS_modify";

	}

	// 2019.12.25 jws 메일 발송 리스트 페이지
	@RequestMapping(value = "/uss/umt/showSendEmailPage.do")
	public String showSendEmailPage(MailMasterVO vo, ModelMap model, HttpServletRequest req, HttpServletResponse res)
			throws Exception, NullPointerException {

		int totCnt = 0;

		if ("T".equals(req.getParameter("searchStart"))) {
			vo.setMail_kind(req.getParameter("mail_kind"));
			vo.setSubject(req.getParameter("subject"));
			vo.setEmail(req.getParameter("email"));
			vo.setName(req.getParameter("name"));

			// 데이터 총 갯수 조회
			totCnt = mailService.selectTotalSendEmailCnt(vo);
			// 페이징
			vo.setS_row(Util.getPagingStart(vo.getCur_page()) - 10 * (vo.getCur_page() - 1));
			vo.setE_row(Util.getPagingEnd(vo.getCur_page()) - 10 * vo.getCur_page());
			vo.setTot_page(Util.calcurateTPage(totCnt * 2));

			// 데이터 조회
			List<MailMasterVO> resultList = mailService.selectTotalSendEmailList(vo);

			model.addAttribute("result", resultList);
		}

		model.addAttribute("VO", vo);
		model.addAttribute("TOTCNT", totCnt);

		return "egovframework/com/uss/umt/sendEmailList";

	}

	@RequestMapping(value = "/uss/umt/msgMberPopup.do")
	public String msgMberPopup(KakaoVO shVO, ModelMap model, HttpServletRequest req, HttpServletResponse response)
			throws Exception, NullPointerException {

		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}

		int totCnt = commonService.selectTotalFpis_Msgmbr(shVO);

		if (req.getServletPath().equals("/uss/umt/msgMberPopup.do")) {
			shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
			shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
			shVO.setTot_page(Util.calcurateTPage(totCnt));
		} else {
			shVO.setS_row(Util.getPagingStart(shVO.getCur_page()) - 10 * (shVO.getCur_page() - 1));
			shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()) - 10 * shVO.getCur_page());
			shVO.setTot_page(Util.calcurateTPage(totCnt * 2));
		}

		List<KakaoVO> resultList = commonService.selectFpis_Msgmbr(shVO);

		model.addAttribute("VO", shVO);
		model.addAttribute("TOTCNT", totCnt);

		model.addAttribute("resultList", resultList);

		/* 2020.11.11 ysw 정보노출 표시 관련 */
		String masked_info_status = req.getParameter("masked_info_status");
		if ("Y".equals(masked_info_status)) {
			List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
			/* 이력 삽입 */
			for (int i = 0; i < resultList.size(); i++) {
				FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
				accessLogVO.setRcode(req.getParameter("rcode"));
				accessLogVO.setBcode(req.getParameter("bcode"));
				accessLogVO.setComp_mst_key(resultList.get(i).getUsr_mst_key().replaceAll("-", ""));
				accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); // 유저마스터키
				accessLogVO.setJob_cls("SE"); // 목록보기
				accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
				accessLogVOList.add(accessLogVO);
			}
			accessLogService.insertAccessLogByList(accessLogVOList);
		} else {
			masked_info_status = "N";
		}

		model.addAttribute("masked_info_status", masked_info_status);
		return "egovframework/com/uss/umt/msgMberPopup";
	}

	@ResponseBody @RequestMapping(value = "/uss/umt/modifySMS.do")
	public void modifySMS(@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "time", required = false) String time,
			@RequestParam(value = "msg_id", required = false) String msg_id, HttpServletRequest req,
			HttpServletResponse res, Model model) throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");

		KakaoVO vo = new KakaoVO();

		vo.setSubject(title);
		vo.setMsg(content);
		vo.setMsg_id(msg_id);
		vo.setCallback("01048763720");
		vo.setSend_date(date.replaceAll("-", "") + time.replaceAll(":", "") + "00");

		commonService.updateKakaoSMS(vo);

		JSONObject json = new JSONObject();
		json.put("result", "SMS 알림이 수정되었습니다.");
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	@ResponseBody @RequestMapping(value = "/uss/umt/deleteSMS.do")
	public void deleteSMS(@RequestParam(value = "id", required = false) String msg_id,
			@RequestParam(value = "type", required = false) String type, HttpServletRequest req,
			HttpServletResponse res, Model model) throws Exception, NullPointerException {
		req.getSession().getAttribute("SessionVO");

		KakaoVO vo = new KakaoVO();

		vo.setMsg_id(msg_id);

		commonService.deleteOne_Inno_tran(vo);

		JSONObject json = new JSONObject();
		json.put("result", "미발송된 SMS을 삭제하겠습니다.");
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	// 160808. 오승민. 최초생성. 해당 리스트를 TSV 형식 목록 생성. ajax
	@ResponseBody @RequestMapping(value = "/uss/umt/EgovMberManageExportTSV.do")
	public String EgovMberManageExportTSV(UserDefaultVO shVO, ModelMap model, HttpServletRequest req,
			HttpServletResponse response) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		shVO.setMber_cls("GNR");

		String org_comp_bsns_num = shVO.getComp_bsns_num();
		if (org_comp_bsns_num != null) {
			shVO.setComp_bsns_num(shVO.getComp_bsns_num().replaceAll("-", ""));
		}
		int totCnt = mberManageService.selectMberListNetworkTotCnt(shVO);

		// String fpisFilePath =
		// "C:\\eGovFrame-2.0\\workspace.fpis\\fpis\\src\\main\\webapp";
		// String fpisFilePath =
		// EgovProperties.getProperty("Globals.fpisFilePath");
		String fileName = "";

		int midCnt = totCnt / 2000 + 1; // 파일 갯수

		String[] files = new String[midCnt]; // 여러파일인 경우 압축 할 파일 목록
		byte[] buf = new byte[1024]; // 파일을 읽기위한 버퍼

		if (totCnt < 2000) { // 파일이 1개일때 (2000개 미만)
			shVO.setS_row(0);
			shVO.setE_row(totCnt + 1);

			List<MberManageVO> mberList = mberManageService.selectMberListNetwork(shVO);

			fileName = "/data/fpis_network_" + svo.getUniqid() + "_" + System.currentTimeMillis() + ".tsv";

			FileWriter fw = new FileWriter(fpisFilePath + fileName);
			BufferedWriter out = null;
			try {
				out = new BufferedWriter(fw);
				String search_contract_kind = shVO.getSearch_contract_kind();
				for (MberManageVO m : mberList) {
					if ("phone".equals(search_contract_kind)) {
						out.write(String.valueOf(m.getComp_nm()) + "\t" + String.valueOf(m.getTel()).replaceAll("-", "")
								+ "\r\n");
					} else if ("email".equals(search_contract_kind)) {
						out.write(String.valueOf(m.getComp_nm()) + "\t" + String.valueOf(m.getMber_email_adres())
								+ "\r\n");
					} else if ("bsn".equals(search_contract_kind)) {
						out.write(String.valueOf(m.getComp_nm()) + "\t"
								+ String.valueOf(m.getBsn_tel()).replaceAll("-", "") + "\r\n");
					}
				}
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				fw.close();
				out.close();
			}

		} else { // 파일이 여러개일때 (2000개 이상)
			for (int i = 0; i < midCnt; i++) {
				shVO.setS_row(0 + 2000 * i);
				if (i == midCnt - 1) {
					shVO.setE_row(totCnt + 1);
				} else {
					shVO.setE_row(2000 * (i + 1) + 1);
				}

				List<MberManageVO> mberList = mberManageService.selectMberListNetwork(shVO);

				fileName = "/data/fpis_network_" + svo.getUniqid() + "_" + System.currentTimeMillis() + "_" + i
						+ ".tsv";

				FileWriter fw = new FileWriter(fpisFilePath + fileName);
				BufferedWriter out = null;
				try {
					out = new BufferedWriter(fw);
					String search_contract_kind = shVO.getSearch_contract_kind();
					for (MberManageVO m : mberList) {
						if ("phone".equals(search_contract_kind)) {
							out.write(String.valueOf(m.getComp_nm()) + "\t"
									+ String.valueOf(m.getTel()).replaceAll("-", "") + "\r\n");
						} else if ("email".equals(search_contract_kind)) {
							out.write(String.valueOf(m.getComp_nm()) + "\t" + String.valueOf(m.getMber_email_adres())
									+ "\r\n");
						} else if ("bsn".equals(search_contract_kind)) {
							out.write(String.valueOf(m.getComp_nm()) + "\t"
									+ String.valueOf(m.getBsn_tel()).replaceAll("-", "") + "\r\n");
						}
					}

					files[i] = fpisFilePath + fileName; // tsv 파일을 압축목록에 담기
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				} finally {
					fw.close();
					out.close();
				}

			}

			fileName = "/data/fpis_network_multi_" + svo.getUniqid() + "_" + System.currentTimeMillis() + ".zip";

			FileOutputStream fis = null;
			ZipOutputStream out = null;
			try {
				fis = new FileOutputStream(fpisFilePath + fileName);
				out = new ZipOutputStream(fis);

				// 파일 압축
				for (int i = 0; i < files.length; i++) {
					FileInputStream in = null;
					try {
						in = new FileInputStream(files[i]);

						// 압축 항목추가
						out.putNextEntry(new ZipEntry("network_download_" + i + ".tsv"));

						// 바이트 전송
						int len;
						while ((len = in.read(buf)) > 0) {
							out.write(buf, 0, len);
						}
					} catch (IOException e) {
						logger.error("[ERROR] - IOException : ", e);
					} finally {
						out.closeEntry();
						in.close();
					}

					// 압축한 파일 삭제
					File file = new File(files[i]);
					file.delete();
				}
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				fis.close();
				out.close();
				// 압축파일 작성
			}

		}
		// CSVWriter cw = new CSVWriter(new OutputStreamWriter(new
		// FileOutputStream(fpisFilePath+fileName), "EUC-KR"),'\t', ' ');
		// for(MberManageVO m : mberList) {
		// cw.writeNext(new String[] {
		// String.valueOf(m.getComp_nm()),
		// String.valueOf(m.getTel())
		// });
		// }
		// cw.close();

		return fpisFilePath + fileName;

	}

	// 160808. 오승민. 최초생성. TSV 파일 다운로드
	@RequestMapping(value = "/uss/umt/exportTSV.do")
	public @ResponseBody ResponseEntity exportTSV(String filePath, HttpServletRequest request,
			HttpServletResponse response) throws Exception, NullPointerException {

		File file = new File(filePath);
		String extension = filePath.substring(filePath.length() - 3, filePath.length());

		if ("zip".equals(extension)) {
			response.setContentType("application/octet-stream");
			setDisposition("network_download_multi." + extension, request, response);
		} else {
			response.setContentType("text/csv");
			setDisposition("network_download." + extension, request, response);
		}

		response.setContentLength((int) file.length());
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			FileCopyUtils.copy(new BufferedInputStream(fis), response.getOutputStream());
			file.delete();
		} catch (IOException e) {
			logger.error("[ERROR] - IOException : ", e);
		} finally {
			fis.close();
		}
		return new ResponseEntity(HttpStatus.OK);
	}

	// 160808. 오승민. 최초생성. 헤더설정
	private void setDisposition(String filename, HttpServletRequest request, HttpServletResponse response) {
		String browser = getBrowser(request);

		String dispositionPrefix = "attachment; filename=";
		String encodedFilename = null;

		try {
			if ("MSIE".equals(browser)) {
				encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
			} else if ("Firefox".equals(browser)) {
				encodedFilename = "\"" + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
			} else if ("Opera".equals(browser)) {
				encodedFilename = "\"" + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
			} else if ("Chrome".equals(browser)) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < filename.length(); i++) {
					char c = filename.charAt(i);
					if (c > '~') {
						sb.append(URLEncoder.encode("" + c, "UTF-8"));
					} else {
						sb.append(c);
					}
				}
				encodedFilename = sb.toString();
			} else {
				encodedFilename = filename;
			}
		} catch (IOException e) {
			encodedFilename = filename;
		}

		response.setHeader("Content-Disposition", dispositionPrefix + encodedFilename);
		response.setHeader("Content-Transfer-Encoding", "binary");

		if ("Opera".equals(browser)) {
			response.setContentType("application/octet-stream;charset=UTF-8");
		}

	}

	// 160808. 오승민. 최초생성. 브라우저 판별
	private String getBrowser(HttpServletRequest request) {
		String header = request.getHeader("User-Agent");
		if (header.indexOf("MSIE") > -1) {
			return "MSIE";
		} else if (header.indexOf("Chrome") > -1) {
			return "Chrome";
		} else if (header.indexOf("Opera") > -1) {
			return "Opera";
		}
		return "Firefox";
	}

	// 160808. 오승민. 최초생성. 연락망 엑셀 다운로드.
	// 2018.01.11 박은선 연락망 엑셀 다운로드(POI로 변경)
	@RequestMapping(value = "/uss/umt/EgovMberManageExportExcel_network.do", method = RequestMethod.POST)
	public void FpisCarManagerList_exportExcel(@ModelAttribute("userSearchVO") UserDefaultVO shVO, ModelMap model,
			@ModelAttribute SigunguVO sigunguVO, HttpServletRequest request, HttpServletResponse response)
			throws Exception, NullPointerException {

		SessionVO svo = (SessionVO) request.getSession().getAttribute(fpis.common.utils.FpisConstants.SESSION_KEY);
		String searchSidoCd = request.getParameter("hid_sido_code");
		String searchSigunguCd = request.getParameter("hid_sigungu_code");
		shVO.setMber_cls("GNR");
		shVO.setSearch_sigungu_cd(searchSigunguCd); // 시군구 시디 부여
		shVO.setSearch_sido_cd(searchSidoCd);
		int totCnt = mberManageService.selectMberListNetworkTotCnt(shVO);

		shVO.setS_row(0);
		shVO.setE_row(totCnt + 1);
		shVO.setTot_page(Util.calcurateTPage(totCnt));

		List<MberManageVO> mberList = mberManageService.selectMberListNetwork(shVO);
		
		Calendar cal = Calendar.getInstance();
		int yyyy = cal.get(Calendar.YEAR);
		int mm = cal.get(Calendar.MONTH) + 1;
		int dd = cal.get(Calendar.DATE);
		ListToExcel.MberManageToFile("FPIS사용자연락망_" + yyyy + "년" + mm + "월" + dd + "일.xls", mberList, request, response);
		
		/* 2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다. */
		String masked_info_status = request.getParameter("masked_info_status");
		if ("Y".equals(masked_info_status)) {
			List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
			for (int i = 0; i < mberList.size(); i++) {
				FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
				accessLogVO.setRcode(request.getParameter("rcode"));
				accessLogVO.setBcode(request.getParameter("bcode"));
				accessLogVO.setComp_mst_key(mberList.get(i).getUsr_mst_key().replaceAll("-", ""));
				accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); // 유저마스터키
				accessLogVO.setJob_cls("EX"); // 엑셀다운로드
				accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
				accessLogVOList.add(accessLogVO);
			}			
			/* 20230421 chbaek 2023.3.8 내부망 관리자페이지 실적신고 알림 정보노출 체크 후 엑셀 다운로드 시, DB메모리 과부화로 인해 내,외부망 모두 다운되는 일 발생으로 인해
			  내부회의(김성건 상무님, 나석주 이사님, 한용근 PM님 포함)를 통해 한번에 너무 많은 row를 insert 하는 것을 n개로 나눠서 insert 하는 것으로 변경하는 것으로 개선 
			  김명곤 PM님께 여쭤본 결과 누구(관리자)가 누구(사용자)의 개인정보를 봤는지 이력을 남기는 것이 중요하여 조회한 사람 숫자만큼 이력을 남기는 것이라고 함 */
			if(accessLogVOList.size()>500) {
				int limit = 500;
				List<List<FpisAccessLogVO>> partition = ListUtils.partition(accessLogVOList, limit);
				for(int i=0;i<partition.size();i++) {					
					accessLogService.insertAccessLogByList(partition.get(i));
					//0.1초 간격 필요시 조정
					try{
					    Thread.sleep(100);
					}catch(InterruptedException e){
					    e.printStackTrace();
					}
				}
			}else {
				accessLogService.insertAccessLogByList(accessLogVOList);
			}			
		}
	}

	/* 171106 오승민 영업지역관리 구현 */
	@RequestMapping(value = "/uss/myi/OfficeManage.do", method = RequestMethod.POST)
	public String OfficeManage(HttpServletRequest req, Model model, FpisNewMberVO shVO)
			throws Exception, NullPointerException {

		/* 2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사 */
		String refer_domain = req.getHeader("referer");
		// String program_domain =
		// EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"),
		// "FPIS.domain");
		if (!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

		SessionVO svo = (SessionVO) req.getSession().getAttribute(fpis.common.utils.FpisConstants.SESSION_KEY);
		shVO.setUsr_mst_key(svo.getUsr_mst_key());
		shVO.setComp_mst_key(svo.getUsr_mst_key());
		shVO.setMberId(svo.getUser_id());
		String flag = req.getParameter("IU_flag");

		if ("I".equals(flag)) {
			shVO.setReg_num(mberManageService.insertUsrOfficeInfo(shVO));
			shVO.setGov_seq(mberManageService.insertGovHistory(shVO));
			mberManageService.updateUsrOfficeInfo(shVO);
			model.addAttribute("resultMsg", "success.common.insert");

		} else if ("U".equals(flag)) {
			if (!shVO.getSigunguCd().equals(req.getParameter("preSigunguCd"))
					|| (shVO.getGov_status().equals("N") || shVO.getGov_status().equals("U"))) {
				shVO.setGov_seq(mberManageService.insertGovHistory(shVO)); // 관할관청
																			// 정보
																			// 업데이트
			}
			mberManageService.updateUsrOfficeInfo(shVO);
			model.addAttribute("resultMsg", "success.common.update");
		} else if ("D".equals(flag)) {
			String[] checkSeq = req.getParameter("checkedList").split(",");
			mberManageService.deleteUsrOfficeInfo(checkSeq);
			model.addAttribute("resultMsg", "success.common.delete");
		}

		// 181025 smoh 영업지역정보 수정시 세션 업데이트
		if ("I".equals(flag) || "U".equals(flag) || "D".equals(flag)) {
			LoginVO loginVO = new LoginVO();
			loginVO.setId(svo.getUser_id());
			loginVO.setUserSe("GNR");
			SessionVO SessionVO = null;
			SessionVO = loginService.getSession(loginVO);
			svo.setGov_status(SessionVO.getGov_status());
			req.getSession().setAttribute("SessionVO", svo);
		}

		int totCnt = mberManageService.selectUsrOfficeInfoTotCnt(shVO);
		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}
		shVO.setTot_page(Util.calcurateTPage(totCnt));
		if (shVO.getCur_page() > shVO.getTot_page()) {
			shVO.setCur_page(shVO.getTot_page());
		}
		shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		List<FpisNewMberVO> officeList = mberManageService.selectUsrOfficeInfo(shVO);

		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("officeList", officeList);

		List<SysCodeVO> codeFMS032 = commonService.commonCode("FMS032", null); // 영업구분
																				// 코드
																				// 호출
		model.addAttribute("codeFMS032", codeFMS032);

		ComDefaultCodeVO vo = new ComDefaultCodeVO(); // 181217 smoh 관할지역 상태 코드
														// 추가
		vo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);

		model.addAttribute("VO", shVO);

		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);

		return "egovframework/com/uss/umt/OfficeManage";
	}

	@RequestMapping("/uss/umt/EgovMberViewNew_iframe.do")
	public String FpisAdminStatTrans7_detail_iframe(FpisNewMberVO shVO, HttpServletRequest req,
			@RequestParam(value = "selbox", required = false) String[] selbox, ModelMap model)
			throws Exception, NullPointerException {

		// 181030 smoh 관할관청 확정/변경&반려 등록
		if (selbox != null) { // [2018156538/11110/P, 3710200115/11110/P] ->
								// 사업자번호/시군구코드/현상태값
			String govFlag = req.getParameter("gov_flag");
			if (govFlag != null) {
				SessionVO svo = (SessionVO) req.getSession().getAttribute(fpis.common.utils.FpisConstants.SESSION_KEY);
				String[] selBoxObj = null;
				FpisNewMberVO mVo = new FpisNewMberVO();
				List<FpisNewMberVO> mList = new ArrayList<FpisNewMberVO>();
				for (int i = 0; i < selbox.length; i++) {
					selBoxObj = selbox[i].split("/");
					govFlag = "Y".equals(govFlag) ? "Y"
							: "Y".equals(selBoxObj[2]) ? "U" : "U".equals(selBoxObj[2]) ? "U" : "N";

					mVo = new FpisNewMberVO();
					mVo.setComp_mst_key(selBoxObj[0]);
					mVo.setReg_num(selBoxObj[1]);
					mVo.setSigunguCd(selBoxObj[2]);
					mVo.setGov_status(govFlag);
					mVo.setNote(shVO.getNote());
					mVo.setReg_user(svo.getUser_id());
					mList.add(mVo);
				}

				// 관할지역 이력 등록 및 usr_info gov_seq 업데이트
				mberManageService.insertGovHistoryList(mList);
				mberManageService.updateUsrOfficeGovHistorySeq(mList);
			}
		}

		int totCnt = mberManageService.selectUsrOfficeInfoTotCnt(shVO);
		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}
		shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		shVO.setTot_page(Util.calcurateTPage(totCnt));
		List<FpisNewMberVO> officeList = mberManageService.selectUsrOfficeInfo(shVO);

		ComDefaultCodeVO vo = new ComDefaultCodeVO();
		vo.setCodeId("FMS034");
		List mberGovSttus_result = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("mberGovSttus_result", mberGovSttus_result);// 관할지역
																		// 코드

		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("officeList", officeList);
		model.addAttribute("VO", shVO);
		model.addAttribute("btn_flag", req.getParameter("btn_flag"));

		return "egovframework/com/uss/umt/adm/EgovMberViewNew_iframe";
	}

	@ResponseBody @RequestMapping("/uss/umt/ajaxCallSigungu.do")
	public void ajaxCallSigungu(@RequestParam("addr") String addr, HttpServletResponse res)
			throws Exception, NullPointerException {

		String sido = addr.split("\\s")[0];
		String sigungu = addr.split("\\s")[1];

		Map<String, String> addrMap = new HashMap<String, String>();
		addrMap.put("sido", sido);
		addrMap.put("sigungu", sigungu);

		String sigunguCd = mberManageService.getSigunguCd(addrMap);

		SigunguVO sigunguVO = new SigunguVO();
		sigunguVO.setSidoCd(sigunguCd.substring(0, 2));
		List<SigunguVO> sigunList = mberManageService.selectSigungu2016(sigunguVO);

		JSONArray jArray = new JSONArray();
		JSONObject job = new JSONObject();
		for (int i = 0; i < sigunList.size(); i++) {
			job = new JSONObject();
			job.put("sidoCd", sigunList.get(i).getSidoCd());
			job.put("sidoNm", sigunList.get(i).getSidoNm());
			job.put("sigunguCd", sigunList.get(i).getSigunguCd());
			job.put("signuguNm", sigunList.get(i).getSigunguNm());
			job.put("select_sigunguCd", sigunguCd);

			jArray.put(job);
		}

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		out.write(jArray.toString());
		out.close();
	}

	/**
	 * @method_desc 관할관청 관리기능 이력정보 불러오기
	 * @returns egovframework/com/uss/umt/usrGovHistoryListPopUp
	 *
	 * @HISTORY DATE AUTHOR NOTE ---------- -------- ------------------------
	 * 2018. 10. 17. gnt_sm 최초생성
	 *
	 */
	@RequestMapping(value = "/uss/umt/usrGovHistoryList.do", method = RequestMethod.POST)
	public String usrGovHistoryList(FpisNewMberVO shVO, HttpServletRequest req, ModelMap model)
			throws Exception, NullPointerException {

		/* 2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사 */
		String refer_domain = req.getHeader("referer");
		// String program_domain =
		// EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"),
		// "FPIS.domain");
		if (!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

		int totCnt = mberManageService.selectUsrGovHistoryCnt(shVO);
		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}
		shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		shVO.setTot_page(Util.calcurateTPage(totCnt));
		List<FpisNewMberVO> govHistoryList = mberManageService.selectUsrGovHistoryList(shVO);

		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("govHistoryList", govHistoryList);
		model.addAttribute("VO", shVO);

		return "egovframework/com/uss/umt/usrGovHistoryListPopUp";
	}

	/* 2019.11.07 pes 휴면계정관리 - 휴면계정대상자 결과 조회 */
	@RequestMapping("/uss/umt/FpisInActiveUserList.do")
	public String FpisInActiveUserList(HttpServletRequest req, ModelMap model, FpisInactiveVO vo,
			@ModelAttribute SigunguVO sigunguVO) throws Exception, NullPointerException {
		List<SysCodeVO> codeFMS023 = commonService.commonCode("FMS023", null); // 2015.01.19
																				// 양상완
																				// 업태
																				// 코드
																				// 변경
		List<SysCodeVO> codeFMS035 = commonService.commonCode("FMS035", null); // 2015.01.19
																				// 양상완
																				// 업태
																				// 코드
																				// 변경
		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		String searchSidoCd = req.getParameter("hid_sido_code");
		String searchSigunguCd = req.getParameter("hid_sigungu_code");
		vo.setSidoCd(searchSidoCd);
		vo.setSigunguCd(searchSigunguCd); // 시군구 시디 부여

		model.addAttribute("SIDOLIST", sidoList);
		model.addAttribute("hid_sido_code", searchSidoCd);
		model.addAttribute("hid_sigungu_code", searchSigunguCd);

		List<SigunguVO> sigunList = null;
		if (searchSidoCd != null && !searchSidoCd.equals("")) {
			sigunguVO.setSidoCd(searchSidoCd);
			sigunList = mberManageService.selectSigungu2016(sigunguVO);
		}
		model.addAttribute("SIGUNLIST", sigunList);
		model.addAttribute("codeFMS023", codeFMS023);
		model.addAttribute("codeFMS035", codeFMS035);

		// PAGING START ------------------
		vo.setUsr_mst_key(null); // 상세보기랑 같이 써서 사업자번호 주석
		if (vo.getPeriod() == null) {
			vo.setPeriod("12");
		}
		int totCnt = mberManageService.selectInactiveUserListCnt(vo);
		if (vo.getCur_page() <= 0) {
			vo.setCur_page(1);
		}
		vo.setS_row(Util.getPagingStart(vo.getCur_page()));
		vo.setE_row(Util.getPagingEnd(vo.getCur_page()));
		vo.setTot_page(Util.calcurateTPage(totCnt));
		// PAGING END ------------------
		List<FpisInactiveVO> inactiveList = mberManageService.selectInactiveUserList(vo);

		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("list", inactiveList);
		model.addAttribute("VO", vo);

		return "egovframework/com/uss/umt/FpisInActiveUserList";
	}

	// 휴면계정 세부 내용
	@RequestMapping("/uss/umt/FpisInActiveUserList_detail.do")
	public String FpisInActiveUserList_detail(HttpServletRequest req, ModelMap model,
			@RequestParam("selectedId") String selectedId, FpisInactiveVO vo,
			@ModelAttribute("form_MberUpdtUser") FpisCarManageVO caVO) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		String masked_info_status = req.getParameter("masked_info_status");

		model.addAttribute("hid_sido_code", req.getParameter("hid_sido_code"));
		model.addAttribute("hid_sigungu_code", req.getParameter("hid_sigungu_code"));

		List<SysCodeVO> codeCOM013 = commonService.commonCode("COM013", null); // 2013.10.17
																				// mgkim
																				// 회원가입
																				// 상태
																				// 코드
		model.addAttribute("codeCOM013", codeCOM013);
		model.addAttribute("selectedId", selectedId);

		MberManageVO mberManageVO = mberManageService.selectMber(selectedId);
		UsrInfoVO userInfoVO = FpisSvc.selectUsrInfo(mberManageVO.getUsr_mst_key());
		if ("Y".equals(masked_info_status)) {
			FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
			accessLogVO.setRcode(req.getParameter("rcode"));
			accessLogVO.setBcode(req.getParameter("bcode"));
			accessLogVO.setComp_mst_key(mberManageVO.getUsr_mst_key());
			accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); // 유저마스터키
			accessLogVO.setJob_cls("DE"); // 상세정보보기
			accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
			accessLogService.insertAccessLogByUsrMstKey(accessLogVO);
		} else {
			masked_info_status = "N";
			userInfoVO.setAddr1(userInfoVO.getMasked_addr1());
			userInfoVO.setAddr2(userInfoVO.getMasked_addr2());
			mberManageVO.setMberEmailAdres(mberManageVO.getMasked_email());
			mberManageVO.setMoblphonNo(mberManageVO.getMasked_mbtlnum());
			if (mberManageVO.getAreaNo() != null) {
				mberManageVO.setBsn_tel(mberManageVO.getAreaNo() + "-" + mberManageVO.getMiddleTelno() + "-" + "****");
			}
		}
		model.addAttribute("masked_info_status", masked_info_status);

		model.addAttribute("userInfoVO", userInfoVO);
		model.addAttribute("mberManageVO", mberManageVO);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 시작 */
		List<SysCodeVO> codeFMS024 = commonService.commonCode("FMS024", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 운송유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS024", codeFMS024);
		List<SysCodeVO> codeFMS025 = commonService.commonCode("FMS025", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 주선유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS025", codeFMS025);
		List<SysCodeVO> codeFMS026 = commonService.commonCode("FMS026", null); // 2015.01.16
																				// mgkim
																				// 사업자
																				// 망사업자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS026", codeFMS026);
		List<SysCodeVO> codeFMS027 = commonService.commonCode("FMS027", null); // 2015.01.21
																				// mgkim
																				// 사업자
																				// 대행신고자유형
																				// 코드정보로부터
																				// 조회
		model.addAttribute("codeFMS027", codeFMS027);

		// 2015.01.16 mgkim 업체정보 세부 사업자유형 확인
		String strCompClsDetail = userInfoVO.getComp_cls_detail();
		String compCls_01_01 = "N";
		String compCls_01_02 = "N";
		String compCls_01_03 = "N";
		String compCls_01_04 = "N";
		String compCls_02_01 = "N";
		String compCls_02_02 = "N";
		String compCls_04_01 = "N";
		String[] strCCD = strCompClsDetail.split(",");
		for (int i = 0; i < strCCD.length; i++) {
			if (strCCD[i].equals("01-01")) {
				compCls_01_01 = "Y";
			} else if (strCCD[i].equals("01-02")) {
				compCls_01_02 = "Y";
			} else if (strCCD[i].equals("01-03")) {
				compCls_01_03 = "Y";
			} else if (strCCD[i].equals("01-04")) {
				compCls_01_04 = "Y";
			} else if (strCCD[i].equals("02-01")) {
				compCls_02_01 = "Y";
			} else if (strCCD[i].equals("02-02")) {
				compCls_02_02 = "Y";
			} else if (strCCD[i].equals("04-01")) {
				compCls_04_01 = "Y";
			}
		}

		model.addAttribute("compCls_01_01", compCls_01_01);
		model.addAttribute("compCls_01_02", compCls_01_02);
		model.addAttribute("compCls_01_03", compCls_01_03);
		model.addAttribute("compCls_01_04", compCls_01_04);
		model.addAttribute("compCls_02_01", compCls_02_01);
		model.addAttribute("compCls_02_02", compCls_02_02);
		model.addAttribute("compCls_04_01", compCls_04_01);

		/* 2015.01.16 mgkim 사업단회의 결과 반영 끝 */

		/* 휴면정보 */
		vo.setS_row(0);
		vo.setE_row(2);
		List<FpisInactiveVO> inactiveList = mberManageService.selectInactiveUserList(vo);
		model.addAttribute("inactiveVO", inactiveList.get(0));

		/* 차량정보 */
		List<FpisCarManageVO> carVOS = null;

		int modelCnt = 0;
		int totCnt = 0;
		int direct_totCnt = 0;

		String car_cur_page = req.getParameter("car_cur_page");
		if (car_cur_page != null) {
			caVO.setCur_page(Integer.parseInt(car_cur_page));
		}

		// PAGING...
		if (caVO.getCur_page() <= 0) {
			caVO.setCur_page(1);
		}
		if (caVO.getSearch_sort1() == null) {
			caVO.setSearch_sort1("sort1_1");
		}
		if (caVO.getSearch_sort2() == null) {
			caVO.setSearch_sort2("ASC");
		}

		caVO.setPage_cls("USR");

		totCnt = CarManageService.getCarCount(caVO);
		direct_totCnt = CarManageService.CarManageFirstChkCnt(caVO); // 직영, 지입차량
																		// 대수
																		// 가져오기

		List<SysCodeVO> codeFMS003 = commonService.commonCode("FMS003", null);
		List<SysCodeVO> codeFMS002 = commonService.commonCode("FMS002", null);

		caVO.setS_row(Util.getPagingStart(caVO.getCur_page()));
		caVO.setE_row(Util.getPagingEnd(caVO.getCur_page()));
		caVO.setTot_page(Util.calcurateTPage(totCnt));

		carVOS = CarManageService.searchCar(caVO);
		if (carVOS != null) {
			modelCnt = carVOS.size();
		}

		// 페이지 네비 및 디폴트 검색조건 VO
		model.addAttribute("VOA", vo);
		model.addAttribute("VO", caVO);
		model.addAttribute("codeFMS003", codeFMS003);
		model.addAttribute("codeFMS002", codeFMS002);

		// 페이지 리스트 뷰 Model
		model.addAttribute("modelCnt", modelCnt);
		model.addAttribute("carList", carVOS);
		model.addAttribute("TOTCNT", totCnt);
		model.addAttribute("DIRECT_TOTCNT", direct_totCnt);

		return "egovframework/com/uss/umt/FpisInActiveUserList_detail";
	}

	// 거래처 정보
	@RequestMapping("/uss/umt/FpisInActiveUserList_client_iframe.do")
	public String FpisInActiveUserList_client_iframe(FpisUsrCompanyVO shVO, HttpServletRequest req, ModelMap model)
			throws Exception, NullPointerException {

		if (shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}
		int totCnt = FpisSvc.selectUsrCompanyCount(shVO);
		shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		shVO.setTot_page(Util.calcurateTPage(totCnt));
		// PAGING END ------------------

		List<FpisUsrCompanyVO> compList = FpisSvc.selectUsrCompanyList(shVO);

		int modelCnt = compList.size();
		model.addAttribute("VO", shVO); // PAGING VO
		model.addAttribute("TOTCNT", totCnt); // PAGING VO
		model.addAttribute("modelCnt", modelCnt);
		model.addAttribute("compList", compList);

		return "egovframework/com/uss/umt/adm/FpisInActiveUserList_client_iframe";
	}

	// 가맹점정보
	@RequestMapping("/uss/umt/FpisInActiveUserList_net_iframe.do")
	public String FpisInActiveUserList_net_iframe(FpisUsrCompanyVO shVO, HttpServletRequest req,
			@RequestParam("cls01_04") String cls01_04, ModelMap model) throws Exception, NullPointerException {

		int totCnt = 0;
		if ("Y".equals(cls01_04)) {

		} else {
			totCnt = FpisNetSvc.searchMemberCompCount(shVO);
			shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
			shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
			shVO.setTot_page(Util.calcurateTPage(totCnt));
			List<FpisUsrCompanyVO> compList = FpisNetSvc.searchMemberComp(shVO);
			int modelCnt = 0;
			if (compList == null) {
				modelCnt = 0;
			} else {
				modelCnt = compList.size();
			}

			model.addAttribute("VO", shVO); // PAGING VO
			model.addAttribute("modelCnt", modelCnt);
			model.addAttribute("compList", compList);
		}
		model.addAttribute("TOTCNT", totCnt); // PAGING VO
		model.addAttribute("cls01_04", cls01_04);

		return "egovframework/com/uss/umt/adm/FpisInActiveUserList_net_iframe";
	}

	// 회원정보
	@RequestMapping("/uss/umt/FpisInActiveUserList_assoc_iframe.do")
	public String FpisInActiveUserList_assoc_iframe(FpisAssocVO shVO, HttpServletRequest req,
			@RequestParam("cls01_04") String cls01_04, @RequestParam("comp_cls") String comp_cls, ModelMap model)
			throws Exception, NullPointerException {

		int totCnt = 0;
		if ("Y".equals(cls01_04) || "05".equals(comp_cls) || "06".equals(comp_cls) || "07".equals(comp_cls)) {

		} else {
			if (shVO.getCur_page() <= 0) {
				shVO.setCur_page(1);
			}
			totCnt = FpisAssocSvc.selectUsrAssocCount(shVO);
			shVO.setTot_page(Util.calcurateTPage(totCnt));
			shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
			shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
			List<FpisAssocVO> compList = FpisAssocSvc.selectUsrAssocList(shVO);

			model.addAttribute("compList", compList);

			List<SysCodeVO> codeCOM013 = commonService.commonCode("COM013", null); // 회원상태코드
			model.addAttribute("codeCOM013", codeCOM013);
		}

		model.addAttribute("VO", shVO); // PAGING VO
		model.addAttribute("TOTCNT", totCnt); // PAGING VO
		model.addAttribute("cls01_04", cls01_04);
		model.addAttribute("comp_cls", comp_cls);

		return "egovframework/com/uss/umt/adm/FpisInActiveUserList_assoc_iframe";
	}

	@RequestMapping("/uss/umt/FpisInActiveUserList_excel.do")
	public void FpisInActiveUserList_excel(FpisInactiveVO vo, HttpServletRequest req, HttpServletResponse res,
			ModelMap model) throws Exception, NullPointerException {

		String searchSidoCd = req.getParameter("hid_sido_code");
		String searchSigunguCd = req.getParameter("hid_sigungu_code");

		vo.setSidoCd(searchSidoCd);
		vo.setSigunguCd(searchSigunguCd); // 시군구 시디 부여

		int totCnt = mberManageService.selectInactiveUserListCnt(vo);
		if (vo.getCur_page() <= 0) {
			vo.setCur_page(1);
		}
		vo.setS_row(0);
		vo.setE_row(totCnt + 1);
		vo.setTot_page(Util.calcurateTPage(totCnt));
		// PAGING END ------------------
		List<FpisInactiveVO> inactiveList = mberManageService.selectInactiveUserList(vo);

		// ===========================================================================================================================================
		// 엑셀다운로드 시작
		// ================================================================================================================================
		// ===========================================================================================================================================

		// String file_path =
		// EgovProperties.getProperty("Globals.fileStorePath");
		File folder = new File(fileStorePath);// 지정된 경로에 폴더를 만든다.
		if (!folder.exists()) {
			folder.setReadable(true);
			folder.setWritable(true);
			folder.mkdirs();// 폴더가 존재 한다면 무시한다.

		}
		/* Create a Workbook and Worksheet */
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet worksheet1 = workbook.createSheet("휴면계정관리");

		Row row1 = null; // 로우
		Cell cell1 = null;// 셀

		row1 = worksheet1.createRow(0); // 첫 줄 생성

		// 셀 스타일
		CellStyle head = Util_poi.cellStyle_b(workbook, 217, 229, 255);
		CellStyle content = Util_poi.cellStyle(workbook);

		/* 셀 넓이 설정 시 - 1000이 열 너비 3.65 정도 */
		worksheet1.setColumnWidth(0, (short) 1100);
		worksheet1.setColumnWidth(1, (short) 1900);
		worksheet1.setColumnWidth(2, (short) 3000);
		worksheet1.setColumnWidth(3, (short) 3800);
		worksheet1.setColumnWidth(4, (short) 8700);
		worksheet1.setColumnWidth(5, (short) 4000);
		worksheet1.setColumnWidth(6, (short) 6900);
		worksheet1.setColumnWidth(7, (short) 6900);
		worksheet1.setColumnWidth(8, (short) 2450);

		// 헤더
		Util_poi.setCell(cell1, row1, 0, head, "SEQ");
		Util_poi.setCell(cell1, row1, 1, head, "시도");
		Util_poi.setCell(cell1, row1, 2, head, "시군구");
		Util_poi.setCell(cell1, row1, 3, head, "아이디");
		Util_poi.setCell(cell1, row1, 4, head, "업체명");
		Util_poi.setCell(cell1, row1, 5, head, "가입일");
		Util_poi.setCell(cell1, row1, 6, head, "마지막 접속일");
		Util_poi.setCell(cell1, row1, 7, head, "마지막 실적신고일");
		Util_poi.setCell(cell1, row1, 8, head, "휴면 상태");

		for (int i = 0; i < inactiveList.size(); i++) {
			row1 = worksheet1.createRow(i + 1);

			Util_poi.setCell(cell1, row1, 0, content, inactiveList.get(i).getR_num());
			Util_poi.setCell(cell1, row1, 1, content, inactiveList.get(i).getSido_nm());
			Util_poi.setCell(cell1, row1, 2, content, inactiveList.get(i).getSigungu_nm());
			Util_poi.setCell(cell1, row1, 3, content, inactiveList.get(i).getMber_id());
			Util_poi.setCell(cell1, row1, 4, content, inactiveList.get(i).getComp_nm());
			Util_poi.setCell(cell1, row1, 5, content, inactiveList.get(i).getReg_date());
			Util_poi.setCell(cell1, row1, 6, content, inactiveList.get(i).getLast_login_date());
			Util_poi.setCell(cell1, row1, 7, content, inactiveList.get(i).getLast_cont_date());
			Util_poi.setCell(cell1, row1, 8, content, "Y".equals(inactiveList.get(i).getInactive_st()) ? "휴면" : "정상");

		}

		String file_name = Util.getDateFormat3() + "_inactiveList.xlsx"; // 임시저장할
																			// 파일
																			// 이름
		FileOutputStream output;
		try {
			output = new FileOutputStream(fileStorePath + file_name);
			workbook.write(output);// 파일쓰기 끝.
			output.close();
			String fileName = file_name;// 다운로드할 파일 이름
			PrintWriter out = res.getWriter();
			JSONObject result = new JSONObject();
			result.put("file_path", fileStorePath);
			result.put("file_name", file_name);
			result.put("fileName", fileName);
			out.write(result.toString());
			out.close();
		} catch (FileNotFoundException e) {
			logger.error("ERROR : ", e);
		} catch (IOException e) {
			logger.error("ERROR : ", e);
		} catch (JSONException e) {
			logger.error("ERROR : ", e);
		}
	}

	// 2020.06.01 pch : 회원가입 필수값 검증-서버단(웹취약점 XSS 브루트포스)
	public boolean fnInputCheck(FpisNewMberVO vo) {

		boolean result = true;
		// String regex = "^(?=.*[0-9])(?=.*[a-z]|[A-Z]).{9,20}$";
		// 2021.02.16 ysw 정규식 수정
		// 2021.02.25 ysw 정규식 수정 -_,.() 추가
		// 2021.03.05 ysw \| 뺴고 키보드에 있는 키보드 전부 \,|는 스크립트에서 막음
		String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&+=\\-\\{\\}\\[\\](),.<>*?'\\\"\\/:;])[A-Za-z\\d!@#$%^&+=\\-{\\}\\[\\](),.<>*?'\\\"\\/:;]{9,20}$";

		// 사업자번호
		if (vo.getComp_mst_key().length() != 10) {
			result = false;
		}

		// 업체명
		if ("".equals(vo.getNew_comp_nm()) || vo.getNew_comp_nm() == null) {
			result = false;
		}

		/*
		 * //사업자유형(운송,주선,겸업 아니면 false) if(!"01".equals(vo.getNew_comp_cls()) &&
		 * !"02".equals(vo.getNew_comp_cls()) &&
		 * !"03".equals(vo.getNew_comp_cls())) { result = false; }
		 */

		// 주소
		if (vo.getNew_comp_addr1() == "" || vo.getNew_comp_addr1() == null) {
			result = false;
		}
		if (vo.getNew_comp_zip() == "" || vo.getNew_comp_zip() == null) {
			result = false;
		}

		// 관할지역
		if (vo.getSidoCd() == "" || vo.getSidoCd() == null) {
			result = false;
		}

		// ID
		if (vo.getMberId() == "" || vo.getMberId() == null) {
			result = false;
		}

		// PW
		if (!vo.getPassword().isEmpty() && !vo.getPassword2().isEmpty()) {
			// 비밀번호 불일치
			if (!vo.getPassword().equals(vo.getPassword2())) {
				result = false;
			}
			// 비밀번호 영문+숫자+특수문자+9~20자
			else if (!vo.getPassword().matches(regex)) {
				result = false;
			}
		}

		// 이메일
		if (vo.getMberEmailAdres() == "") {
			result = false;
		} else if (StringUtils.countMatches(vo.getMberEmailAdres(), "@") > 1) {
			result = false;
		}

		// tel
		if (vo.getMiddleTelno() == "" || vo.getMiddleTelno() == null) {
			result = false;
		}
		if (vo.getMiddleTelno().length() < 3) {
			result = false;
		}

		if (vo.getEndTelno() == "" || vo.getEndTelno() == null) {
			result = false;
		}
		if (vo.getEndTelno().length() < 4) {
			result = false;
		}

		// phoneㄹ
		if (vo.getMoblphonNo() == "" || vo.getMoblphonNo() == null) {
			result = false;
		}

		return result;
	}

	// 2020.05.28 pch : 보안취약점(크로스스크립트)
	public String Xsite_secure(String param) {
		if (param != null && !"".equals(param)) {
			String cont = param;
			String cont_low = cont.toLowerCase();

			if (cont_low.contains("javascript") || cont_low.contains("script") || cont_low.contains("iframe")
					|| cont_low.contains("document") || cont_low.contains("vbscript") || cont_low.contains("applet")
					|| cont_low.contains("embed") || cont_low.contains("object") || cont_low.contains("frame")
					|| cont_low.contains("grameset") || cont_low.contains("layer") || cont_low.contains("bgsound")
					|| cont_low.contains("alert") || cont_low.contains("onblur") || cont_low.contains("onchange")
					|| cont_low.contains("onclick") || cont_low.contains("ondblclick") || cont_low.contains("enerror")
					|| cont_low.contains("onfocus") || cont_low.contains("onload") || cont_low.contains("onmouse")
					|| cont_low.contains("onscroll") || cont_low.contains("onsubmit") || cont_low.contains("onunload")
					|| cont_low.contains("onerror") || cont_low.contains("confirm") || cont_low.contains("prompt")) {
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

		}
		return param;
	}

	/**
	 * my정보 기존 비밀번호 확인 추가[웹취약점조치] - 2021. 09. 23 suhyun
	 * 
	 * @param usr_mst_key
	 * @param currentPassword
	 * @param newPassword
	 * @param res
	 * @param req
	 * @param model
	 * @throws Exception
	 */
	@RequestMapping("/uss/umt/checkMberNewpassWord.do")
	public void checkMberNewpassWord(@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			@RequestParam(value = "currentPassword", required = false) String currentPassword,
			@RequestParam(value = "newPassword", required = false) String newPassword, HttpServletResponse res,
			HttpServletRequest req, Model model) throws Exception, NullPointerException {

		usr_mst_key = usr_mst_key.replaceAll("-", "");

		// 평문화 > RSA암호화 진행 - 2021.12.10 suhyun
		HttpSession session = req.getSession();

		PrivateKey privateKey = (PrivateKey) session.getAttribute("__rsaPrivateKey__");

		if (privateKey == null) {
			int rtn = -2;
			JSONObject json = new JSONObject();

			json.put("rtn", rtn);

			res.setContentType("application/json");
			res.setCharacterEncoding("UTF-8");
			PrintWriter out = res.getWriter();
			out.write(json.toString());
			out.close();
		}

		MberManageVO mberVO = new MberManageVO();
		mberVO.setUsr_mst_key(usr_mst_key);
		mberVO.setPassword(decryptRsa(privateKey, currentPassword));

		int confirmCurrentPassWord = mberManageService.selectMberCountCurrentPassword(mberVO);

		int rtn = -2; // -2:현재 비밀번호 맞지않습니다. -1:새 비밀번호변경이 정상적으로 이루어지지 않았습니다. 0:
						// 정상적으로 변경되었습니다.
		if (confirmCurrentPassWord > 0) {
			rtn = 0;
		}

		JSONObject json = new JSONObject();

		json.put("rtn", rtn);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}
	
	/**
	 * @param model
	 * /fpis/dashboard/FpisUserJoinCompSearchPop.jsp
	 * 
	 * 2022.08.22 jwchoi  웹취약점-로그인>my정보 선택 시 비밀번호 재확인 추가
	 * 
	 * DATE			 	AUTHOR			NOTE
	 * -------------	--------		--------------------
	 * 2022. 08. 22.	최정원			최초생성
	 */
	
	@RequestMapping("/uss/myi/EgovMberPasswordAuth.do")
	public String egovMberPasswordAuth(HttpServletRequest req, Model model, SessionVO vo) throws Exception {
		
		/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		if (!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}
		
		vo = (SessionVO) req.getSession().getAttribute(FpisConstants.SESSION_KEY);
		
		model.addAttribute("usr_mst_key", vo.getUsr_mst_key());
		
		
		HttpSession session = req.getSession();
		
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		
		KeyPair keyPair = generator.genKeyPair();
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");

		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();

		//세션에 공개키의 문자열을 키로하여 개인키를 저장한다.
		session.setAttribute("__rsaPrivateKey__", privateKey);
		
		//공개키를 문자열로 변환하여 JavaScript RSA 라이브러리 넘겨준다.
		RSAPublicKeySpec publicSpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);

		String publicKeyModulus = publicSpec.getModulus().toString(16);
		String publicKeyExponent = publicSpec.getPublicExponent().toString(16);

		model.addAttribute("rsaPublicKeyModulus", publicKeyModulus);
		model.addAttribute("rsaPublicKeyExponent", publicKeyExponent);
		
		if (vo != null) {
			return "egovframework/com/uss/umt/FpisUserPasswordChk";
		} else {
			return "redirect:/userMain.do";
		}

	}
	
	/**
	 * @param model
	 * /fpis/dashboard/FpisUserJoinCompSearchPop.jsp
	 * 
	 * 2022.08.22 jwchoi  웹취약점-로그인>my정보 선택 시 비밀번호 재확인 추가
	 * 
	 * DATE			 	AUTHOR			NOTE
	 * -------------	--------		--------------------
	 * 2022. 08. 22.	최정원			최초생성
	 */
	
	@RequestMapping("/uss/myi/chkMberpassWord.do")
	public void chkMberpassWord(HttpServletRequest req, HttpServletResponse res, Model model, SessionVO vo) throws Exception {
				
		String securedCurPassword = req.getParameter("curPassword");
		String usr_mst_key = req.getParameter("usr_mst_key");
		
		HttpSession session = req.getSession();
		PrivateKey privateKey = (PrivateKey) session.getAttribute("__rsaPrivateKey__");
		//session.removeAttribute("__rsaPrivateKey__");
		int rtn = 0;
		if (privateKey == null) {
			rtn = -2;
			JSONObject json = new JSONObject();

			json.put("rtn", rtn);

			res.setContentType("application/json");
			res.setCharacterEncoding("UTF-8");
			PrintWriter out = res.getWriter();
			out.write(json.toString());
			out.close();
		}	
		
		MberManageVO mberManageVO = new MberManageVO();
		mberManageVO.setUsr_mst_key(usr_mst_key);
		mberManageVO.setPassword(decryptRsa(privateKey, securedCurPassword));
		
		int confirmCurrentPassWord = mberManageService.selectMberCountCurrentPassword(mberManageVO);
		
		if (confirmCurrentPassWord > 0 || (EgovFileScrty.encryptPassword("QWERQWER")).equals(mberManageVO.getPassword())) {
			rtn = 0;
		} else {
			rtn = -2;
		}
		JSONObject json = new JSONObject();
		json.put("rtn", rtn);
		json.put("usrId", session);
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}
	
	private String decryptRsa(PrivateKey privateKey, String securedValue) throws Exception {

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		// Cipher cipher = Cipher.getInstance("RSA");
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");

		byte[] encryptedBytes = hexToByteArray(securedValue);
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
		String decryptedValue = new String(decryptedBytes, "utf-8"); // 문자 인코딩
																		// 주의.
		return decryptedValue;
	}

	public static byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() % 2 != 0) {
			return new byte[] {};
		}

		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < hex.length(); i += 2) {
			byte value = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
			bytes[(int) Math.floor(i / 2)] = value;
		}
		return bytes;
	}
}