package fpis.reg.mass.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.poi.util.SystemOutLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import egovframework.com.utl.fcc.service.EgovDateUtil;
import fpis.common.service.CommonGetInfoService;
import fpis.common.service.CommonService;

import fpis.common.vo.SessionVO;
import fpis.common.vo.sys.SysCodeVO;
import fpis.reg.RegVO;
import fpis.common.vo.mod.ErrorVO;
import fpis.reg.oper.OperService;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.org.json.JSONObject;
import fpis.reg.mass.res.RESOURCE_VAR;
import fpis.reg.mass.service.DateUtil;
import fpis.reg.mass.service.ExcelExportManager;
import fpis.reg.mass.service.ExtractObjectForSmalling;
import fpis.reg.mass.service.ExtractObjectForSpd;
import fpis.reg.mass.service.FpisFileMaker;
import fpis.reg.mass.service.MassOrderExtractService;
import fpis.reg.mass.service.MassOrderService;
import fpis.reg.mass.service.Utils;
import fpis.reg.mass.service.FpisResult;
import fpis.reg.mass.service.FpisResultCommon;
import fpis.reg.mass.set.ImportDataSet;
import fpis.reg.mass.set.ImportStatus;
import fpis.reg.mass.set.WorkFileType;
import fpis.reg.mass.service.MassOrderVO;

/**
 * @class_desc 관리자 공통 클래스 fpis.reg.mass.web MassController.java
 *
 * @DATE 2022. 08. 03.
 * @AUTHOR GnT 최정원
 * @HISTORY DATE 			AUTHOR 		NOTE 
 * 			-------------	-------- 	-------------------- 
 * 			2022. 08. 03.	최정원 		최초생성
 */

@SuppressWarnings({"unchecked", "rawtypes"})
@Controller
public class MassOrderController {
	// 로그 생성
	private static final Logger logger = Logger.getLogger(MassOrderController.class);

	// 사용자 정보 서비스
	@Resource(name = "CommonGetInfoService")
	private CommonGetInfoService commonGetInfoService;

	// 공통 서비스
	@Resource(name = "CommonService")
	private CommonService commonService;

	@Resource(name = "OperService")
	private OperService operService;

	// 대량실적신고 등록 서비스
	@Resource(name = "MassOrderService")
	private MassOrderService massOrderService;

	// 대량실적신고 업로드파일(.fpis) 변환 서비스
	@Resource(name = "MassOrderExtractService")
	private MassOrderExtractService massExtractService;
	
	// 대량실적신고 업로드파일(.fpis) 변환 
	//@Resource(name = "FpisFileMaker")
	//private FpisFileMaker fpisFileMaker;
	
	//대량실적신고 병합파일 경로
	@Value(value="#{globals['Globals.orderMergeFilePath']}")
	private String mergeFilePath;
	
	//대량실적신고 에러파일 경로
	@Value(value="#{globals['Globals.orderErrorFilePath']}")
	private String errorFilePath;
	
	//대량실적신고 .fpis 변환파일 경로
	@Value(value="#{globals['Globals.orderExportFilePath']}")
	private String exportFilePath;
	
	private FpisResult fr = null;
	private List<FpisResultCommon> fpisResultCommonList = null;

	
	@Autowired
	MappingJackson2JsonView jsonView;

	/**
	 * @method_desc 대량실적등록 검증 화면 함수
	 * @returns String
	 *
	 * @HISTORY DATE AUTHOR NOTE ------------- --------- ------------------------ 2022. 08. 03. 최정원
	 *          최초생성
	 *
	 */
	@RequestMapping(value = "/reg/mass/FpisOrderVerify.do", method = RequestMethod.POST)
	public String fpisOrderVerify(HttpServletRequest req, ModelMap model, RegVO vo)
			throws Exception, NullPointerException {
		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String tbYN = session.getComp_cls_detail_tb_yn();
		String chkCond = session.getCond(); // 계정 업태 가져오기
		String usrCond = "";
		String BCODE = "";
		
		BCODE = massOrderService.getBCODE(chkCond);
		
		MassOrderVO massorderVO = new MassOrderVO();
		massorderVO.setCond(chkCond);
		
		/* 계정 업태 확인 */
		if ("04".equals(chkCond) || "05".equals(chkCond) || "06".equals(chkCond)
				|| "07".equals(chkCond)) {
			usrCond = "D";
		} else if ("02".equals(chkCond)) {
			usrCond = "J";
		} else if ("01".equals(chkCond) || "03".equals(chkCond)) {
			usrCond = "U";
		}
		
		if ("Y".equals(tbYN)) {
			usrCond = "T";
		}
		
		model.addAttribute("usrCond", usrCond);
		model.addAttribute("tbYN", tbYN);

		// 2020.08.24 ysw 공통함수로 현재 사용자정보[대행도포함임] [사업자 유형(1대사업자인지) 포함] 가져온다.
		HashMap UsrInfo = commonGetInfoService.selectUsrInfoByUsrMstKey(vo.getUsr_mst_key());
		model.addAttribute("UsrInfo", UsrInfo);
		UsrInfo.put("yyyy", vo.getSelectedQuarterValue());
		UsrInfo.put("usr_mst_key", UsrInfo.get("USR_MST_KEY"));
		UsrInfo.put("FIND_USR_ONE", commonGetInfoService.selectUsrOneByUsrMstKey(UsrInfo));

		// 타운송수단정보
		List<SysCodeVO> tType = commonService.commonCode("FMS020", null);
		// 이사화물/동일항만내 이송
		List<SysCodeVO> dType = commonService.commonCode("FMS029", null);

		// 타운송수단(02.13)
		model.addAttribute("TTYPE", tType);
		// 운행구분(02.13)
		model.addAttribute("DTYPE", dType);
		model.addAttribute("USR_MST_KEY", vo.getUsr_mst_key());
		model.addAttribute("UsrInfo", UsrInfo);

		String search_date = EgovDateUtil.getToday();
		RegVO shVO = new RegVO();


		// 실적주체와의 비교는 세션의 usr_mst_key와 넘어온 usr_mst_key
		shVO.setSearch_date(search_date);
		shVO.setUsr_mst_key(vo.getUsr_mst_key());
		int totCnt = operService.CarManageFirstChkCntForReg(shVO);
		model.addAttribute("totCnt", totCnt);

		if (session != null) {
			List<SysCodeVO> codeFMS012 = null; // 망 사업자인지 체크하기 위한
			codeFMS012 = commonService.commonCode("FMS012", null);
			String netUsrYN = "N";
			for (int j = 0; j < codeFMS012.size(); j++) {
				if (codeFMS012.get(j).getCode().equals(session.getComp_cls_detail())) { // 망종류
																						// 목록에서
																						// 망사업자
																						// 체크
					netUsrYN = "Y";
					model.addAttribute("MANG_REGISTER", "Y");
				}
			}
			if (netUsrYN == "N") { // 망사업자가 아닌경우
				model.addAttribute("MANG_REGISTER", "N");
			}
		}
		
		String contextPath = req.getContextPath();
    	model.addAttribute("contextPath",contextPath);
		// 검색조건을 담습니다.
		model.addAttribute("VO", vo);
		model.addAttribute("rcode", req.getParameter("rcode"));
		model.addAttribute("bcode", req.getParameter("bcode"));
		return "/fpis/reg/mass/FpisOrderVerify";
	}
	
	/**
	 * @throws JSONException 
	 * @method_desc 대량실적등록 파일 검증 처리
	 * @returns String
	 *
	 * @HISTORY 
	 * DATE 		 AUTHOR		NOTE 
	 * ------------- --------- ------------------------ 
	 * 2022. 08. 03. 최정원		최초생성
	 *
	 */

	@RequestMapping(value = "/reg/mass/FpisOrderFileList.do", method = RequestMethod.POST, produces = "application/json; charset=utf8")
	public void multiFileValidation(MultipartHttpServletRequest req, HttpServletResponse res,
			RegVO vo, ModelMap model) throws JSONException {
		//System.out.println("################# 검증 시작 ##################");
		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String usrCond = session.getCond(); // 계정 업태 가져오기
		String usrCondDetail = session.getComp_cls_detail();
		String chkOp = req.getParameter("chkOp");
		
		/* 검증 실패 데이터 관련 변수 */
		List<Map<String,Object>> errCodeList = new ArrayList<Map<String,Object>>(); //검증실패 중 데이터 오류가 아닌 것
		List<Map<String,Object>> errDataList = new ArrayList<Map<String,Object>>(); //검증실패 중 데이터 오류
		boolean yangFlag = true; //양식검증 에러 flag
		boolean dataFlag = true; //데이터검증 에러 flag
	
		MassOrderVO massorderVO = new MassOrderVO();
		massorderVO.setUserIDAtSystem(session.getUser_id());
		massorderVO.setUsrMstKeyAtSystem(session.getUsr_mst_key());
		massorderVO.setCond(usrCond);
		massorderVO.setCondDetail(usrCondDetail);
		
		Utils.setUserIDAtSystem(session.getUser_id());
		
		String usr_mst_key = MassOrderVO.getUsrMstKeyAtSystem();
		
		boolean isDeahangCheck = false; // 대행 체크
		boolean isOnly_0201 = false; // 주선
		boolean checkRowLimit = true;
		boolean checkRegistUsr = true;
		boolean chkAgencyCnt = true; // 데이터 미입력체크
		boolean chkEmpty = true; // 데이터 미입력체크
		boolean checkBasicFormData = true; // 데이터 정합성체크
		boolean checkIdentifyData = true;
		boolean checkContfromRangeData = true;
		boolean checkNoRecordInfo = true;
		boolean checkNoRecordInfoDae = true;
		boolean checkUsrGov = true;
		boolean checkUsrGovDae = true;
		boolean checkPreReg = true;
		boolean checkRegModifyAllow = true;
		boolean checkRegModifyAllowDae = true;
		boolean checkRegLimit = true;
		boolean checkRegLimitDae = true;

		
		/* 대행계정 여부 확인 */
		if ("04".equals(usrCond) || "05".equals(usrCond) || "06".equals(usrCond)
				|| "07".equals(usrCond)) {
			isDeahangCheck = true;
		} else if ("02".equals(usrCond)) {
			isOnly_0201 = true;
		}

		massOrderService.saveUsrInfo(usr_mst_key);
		massOrderService.saveUsrCond(usrCond, isDeahangCheck, isOnly_0201);

		List<MultipartFile> fileList = req.getFiles("regFile");
		
		int COL_CNT = 0;
		
		fpisResultCommonList = new ArrayList<FpisResultCommon>();
		
		int f = 0;
		while (f < fileList.size())  {
			ImportDataSet importDataSet = new ImportDataSet();
			MultipartFile mf = fileList.get(f);
			String[] extTemp = mf.getOriginalFilename().split("[.]");
			String ext = extTemp[extTemp.length - 1];
			String saveFileName = extTemp[0];
			String oriFileName = mf.getOriginalFilename();
			ExcelExportManager.setFlag_yang(true);
			massorderVO.setSaveFileName(saveFileName);

			/*************************************************************************************
			 * 데이터 파일 로딩 시작
			 *************************************************************************************/
			
			try {
				massOrderService.reSetErrCodeList();
				massOrderService.reSetErrDataList();
				//확장자 이상 검증 
				if (ext == null) {
					massOrderService.importCode(importDataSet, oriFileName, "UPL001");
					yangFlag = false;
				} else {
					//시트 수 0 혹은 2개 이상 확인 
					importDataSet = massOrderService.chkSheetCnt(mf);
				}
				
				try {
					if(ImportStatus.IS_NOT_OLD.equals(importDataSet.getImportStatus()) ) {
						//System.out.println("!!!!!!chkPreData");
						yangFlag = true;
						// 시트 수 정상 이전양식 vs 현재 간소화양식 검증
						importDataSet = massOrderService.chkPreData(mf);
								
						if (importDataSet.isSuccess()) {
							// 현재 간소화 양식 데이터 가져오기 , 연계프로그램 함수명 : getXlsDataForSmalling
							importDataSet = massOrderService.getDataForSmallingBefore(mf, importDataSet, importDataSet.getImportData());	
						} else {
							yangFlag = false;
						}
					} else {
						//시트 수 초과 에러메세지로 넘기기
						yangFlag = false;
					}
				} catch (NullPointerException e) {
					yangFlag = false;
					massOrderService.importCode(importDataSet, oriFileName, "UPL014");
				}
				
				if (importDataSet.isSuccess() && importDataSet.getImportData().size() != 0) {
					yangFlag = true;
					//계정, 양식일치 검증
					importDataSet = massOrderService.getDataForSmallingAfter(oriFileName, importDataSet, importDataSet.getImportData());
					
					//최신양식이 아닌 경우 병합무시 옵션 사용X 
					if ("no_merge".equals(chkOp) && (importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_D || importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_N)) { 
						massOrderService.chkNomerge(oriFileName, importDataSet, importDataSet.getImportData());
						yangFlag = false;
//						System.out.println("최신양식이 아님 : " + err.getCode());
					}
				} else {
					yangFlag = false;
				}
					  
			    //공백데이터 제거 
				try {
					dataEmptyArrayRemove(oriFileName, importDataSet, importDataSet.getImportData());
				} catch (Exception e) {
					yangFlag = false;
					massOrderService.importCode(importDataSet, oriFileName, "COR001");
//					System.out.println("공백데이터 제거 : " + err.getCode());
				}

				
				if (importDataSet.getImportData().size() != 0) {
					COL_CNT = importDataSet.getImportData().get(0).length;
				}
					
				if (importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_D) {
					importDataSet = massOrderService.makeOrderCnt(importDataSet, RESOURCE_VAR.TYPE_SMALLING_CONVERT_DAEHANG); 
					COL_CNT = RESOURCE_VAR.TYPE_SMALLING_DAEHANG;
				}
				if (importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_N) {
					importDataSet = massOrderService.makeOrderCnt(importDataSet, RESOURCE_VAR.TYPE_SMALLING_CONVERT_DAEHANG); 
					COL_CNT = RESOURCE_VAR.TYPE_SMALLING;
				}
				
			 /*************************************************************************************
				 * 데이터 파일 로딩 완료
				 *************************************************************************************/		
				/*************************************************************************************
				 * 데이터 유효성 검증 시작
				*************************************************************************************/
				if (importDataSet.isSuccess()) {
					
					//10000건 제한 검증
					importDataSet = massOrderService.chkRowLimit(oriFileName, importDataSet, importDataSet.getImportData());
					
					if (importDataSet.getImportData().size() > 10000) {
						checkRowLimit = false;
					} else {
						checkRowLimit = true;
					}
					
					//코드정보의 일관성유지를 위한 데이터 보정작업
					if (isDeahangCheck) {// 대행계정
						massOrderService.dataSupplement(oriFileName, importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_DAEHANG_DATA_SUPPLEMENT); 

					} else {// 운송,주선계정 
						if (isOnly_0201) { // 주선계정
							massOrderService.dataSupplement(oriFileName, importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_DATA_FORWARDONLY_SUPPLEMENT); 
						} else { // 운송계정
							massOrderService.dataSupplement(oriFileName, importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_DATA_SUPPLEMENT); 
						}
					}
					
					//계약고유번호 부여 
					if ("cont_group".equals(chkOp)) {
						if (isDeahangCheck) {
							importDataSet.setImportData(massOrderService.transDateString(oriFileName, importDataSet,importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_DATECHECK_VERIFYBITSTRING)); 
						} else { 
							if (isOnly_0201) { 
								importDataSet.setImportData(massOrderService.transDateString(oriFileName, importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_DATECHECK_VERIFYBITSTRING)); 
							} else {
								importDataSet.setImportData(massOrderService.transDateString(oriFileName, importDataSet,importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DATECHECK_VERIFYBITSTRING)); 
							} 
						}
					}
					
					//대행사업자-실적주체자 FPIS 미등록사업자 확인
					if (importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_D || importDataSet.getWorkFileType() == WorkFileType.G2_TYPE_D) { 
						// 미등록사업자 체크 
						Set<String> notRegistUsr = massOrderService.checkAgencyUsrMstKey(oriFileName, importDataSet, importDataSet.getImportData(), COL_CNT);
						
						if (notRegistUsr.size() > 0) {
								checkRegistUsr = false;
							}
					}
					
					//대행사업자-실적주체업체 1000개 초과
					if (importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_D || importDataSet.getWorkFileType() == WorkFileType.G2_TYPE_D) {
						chkAgencyCnt = massOrderService.checkAgencyCnt(oriFileName, importDataSet, importDataSet.getImportData(), COL_CNT);
					}
					
				    //데이터 미입력항목 검증 
					if (isDeahangCheck) {
						chkEmpty = massOrderService.checkEmptyData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_EMPTY_VERIFYBITSTRING, COL_CNT); 
					} else {
						if (isOnly_0201) {
							chkEmpty = massOrderService.checkEmptyData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_EMPTY_VERIFYBITSTRING, COL_CNT); 
						} else {
							chkEmpty = massOrderService.checkEmptyData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_EMPTY_VERIFYBITSTRING, COL_CNT); 
						}
					}
					
					//데이터 정합성(숫자입력,차량번호 규칙 등) 검증
					if (isDeahangCheck) {
						checkBasicFormData = massOrderService.checkBasicFormData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_FORM_VERIFYBITSTRING, COL_CNT, isOnly_0201);
					} else {
						if (isOnly_0201) { 
							checkBasicFormData = massOrderService.checkBasicFormData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_FORM_VERIFYBITSTRING, COL_CNT, isOnly_0201); 
						} else { 
							checkBasicFormData = massOrderService.checkBasicFormData(oriFileName, importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_FORM_VERIFYBITSTRING, COL_CNT, isOnly_0201); 
						} 
					}
					
					//데이터 중복계약(계약고유번호) 검증 
					if (isDeahangCheck) { 
						checkIdentifyData = massOrderService.checkIdentifyData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_FORM_VERIFYBITSTRING, COL_CNT); 
					} else {
						if (isOnly_0201) { 
							checkIdentifyData = massOrderService.checkIdentifyData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_FORM_VERIFYBITSTRING, COL_CNT); 
						} else {							
							checkIdentifyData = massOrderService.checkIdentifyData(oriFileName, importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_FORM_VERIFYBITSTRING,COL_CNT); 							
						} 
					}
					
					//데이터 동일년도 검증 
					if (isDeahangCheck) { 
						checkContfromRangeData = massOrderService.checkContfromRangeData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT); 
					} else { 
						if (isOnly_0201) { 
							checkContfromRangeData = massOrderService.checkContfromRangeData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
						} else { 
							checkContfromRangeData = massOrderService.checkContfromRangeData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT); 
						} 
					}
					
					//[실적없음] 신고 검증 
					if (isDeahangCheck) { 
						checkNoRecordInfo = massOrderService.checkNoRecordInfo(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
						checkNoRecordInfoDae = massOrderService.checkNoRecordInfoDae();
					} else { 
						if (isOnly_0201) { 
							checkNoRecordInfo = massOrderService.checkNoRecordInfo(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
						} else { 
							checkNoRecordInfo = massOrderService.checkNoRecordInfo(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT); 
						} 
					} 
					
					//관할관청 등록 여부 검증 
					if (isDeahangCheck) { 
						checkUsrGov = massOrderService.checkUsrGov(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
						checkUsrGovDae = massOrderService.checkUsrGovDae();
					} else { 
						if (isOnly_0201) { 
							checkUsrGov = massOrderService.checkUsrGov(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
						} else { 
							checkUsrGov = massOrderService.checkUsrGov(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT); 
						} 
					}
					
					//실적신고 가능년도 여부 확인 1. 실적등록 데이터 중 min날짜			  
					String ImportDate = ""; 
					if (isDeahangCheck) { 
						ImportDate = massOrderService.getImportDate(importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT); 
					} else { 
						if (isOnly_0201) { 
							ImportDate = massOrderService.getImportDate(importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
					} else { 
						ImportDate = massOrderService.getImportDate(importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT); 
						} 
					} 

					// 서버시간 가져오기 
					Calendar cal = Calendar.getInstance(); 
					String format = "yyyy/MM";
					SimpleDateFormat sdf = new SimpleDateFormat(format); 
					String serverDate = sdf.format(cal.getTime()); 
					String serverYear = serverDate.split("/")[0]; 
					String serverMonth = serverDate.split("/")[1]; 
					String importYear = ""; 
					String importMonth = "";
					
//					//실적신고 가능년도 여부 확인 1. 현재년도 -2보다 입력년도가 작거나 같으면 || 현재년도 -1이 입력년도와 같은데 현재 월이 7월 ~ 12월이면 2.
//				    //현재 월이 4~6월 && 입력한 데이터에 년도가 현재년도 -1 일때
//					
					if (!"".equals(ImportDate)) {
						//System.out.println("=========================== ImportDate : " + ImportDate);
						importYear = ImportDate.substring(0, 4);
						importMonth = ImportDate.substring(4, 6);
				  
						if ((Integer.parseInt(serverYear) - 2) >= Integer.parseInt(importYear) || ((Integer.parseInt(serverYear) - 1) == Integer.parseInt(importYear) && (Integer.parseInt(serverMonth) >= 7))) {
							if (isDeahangCheck) {
								checkPreReg = massOrderService.checkPreReg(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear); 
							} else {
								if (isOnly_0201) { 
									checkPreReg = massOrderService.checkPreReg(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear);
								} else { 
									checkPreReg = massOrderService.checkPreReg(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear); 
								} 
							}
						} else { // 현재 월이 4~6월이면서 입력한 데이터에 년도가 현재년도 -1 일때
							ExcelExportManager.setFlag_preReg(true);
							//등록허가 대상 여부 검증 
							if ((Integer.parseInt(serverMonth) >= 4 && Integer.parseInt(serverMonth) <= 6) && (Integer.parseInt(serverYear) - 1) == Integer.parseInt(importYear)) {
								if (isDeahangCheck) {
									checkRegModifyAllow = massOrderService.checkRegModifyAllow(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
									checkRegModifyAllowDae = massOrderService.checkRegModifyAllowDae();
								} else { 
									if (isOnly_0201) { 
										checkRegModifyAllow = massOrderService.checkRegModifyAllow(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
									} else { 
										checkRegModifyAllow = massOrderService.checkRegModifyAllow(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT); 
									} 
								}
							} else {
								ExcelExportManager.setFlag_regAllowDae(true);
							}
						} 
					}
		
					//실적신고 제한 여부 검증 관할관청 등록 여부 검증 
					if (isDeahangCheck) {
						checkRegLimit = massOrderService.checkRegLimit(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear);
						checkRegLimitDae = massOrderService.checkRegLimitDae();
					} else {
						if (isOnly_0201) {
							checkRegLimit = massOrderService.checkRegLimit(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear); 
						} else {
							checkRegLimit = massOrderService.checkRegLimit(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear);
						}
					}
									
					//에러 파일 생성
					if(!checkRowLimit || !chkAgencyCnt || !checkRegistUsr || !chkEmpty || !checkBasicFormData || !checkIdentifyData || !checkContfromRangeData || !checkUsrGovDae || !checkPreReg || !checkRegLimitDae || !checkRegModifyAllowDae || !checkNoRecordInfoDae) {
						dataFlag = false;
					} else if (!checkRegModifyAllow || !checkNoRecordInfo || !checkUsrGov || !checkRegLimit) {
						yangFlag = false;
					}

				} else {
					//양식검증 오류 엑셀 및 메세지 넘기기
					yangFlag = false;
				}
	
				if (!yangFlag) {
					String _fname = mf.getOriginalFilename();
					String _errorFilePath = massOrderService.makeErrorDirectory(_fname, errorFilePath);
					String _dateStr = DateUtil.getToDayTimeStrForErrorFileMake();
					ExcelExportManager.errFilename = _errorFilePath+_fname+"_E_"+_dateStr;
					ExcelExportManager.eFlag = true;
					ExcelExportManager.makeFinalHeaderErrorFile(importDataSet.getImportData(), COL_CNT);
					String excel_name = _fname+"_E_"+_dateStr;
					
					List<Map<String,Object>> finalCodeList = new ArrayList<Map<String,Object>>();
					errCodeList = massOrderService.getErrCodeList();
					finalCodeList = massOrderService.makeFinalList(oriFileName, errCodeList, COL_CNT);
					
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					
					if (errCodeList.size() > 0) {
						result.put("yangError", "Y");
						result.put("errCodeList", errCodeList);
						result.put("errCnt", errCodeList.size());
						result.put("codeList", finalCodeList);
						result.put("codeCnt", finalCodeList.size());
						result.put("res", "ERR");
						result.put("fname", oriFileName);
						result.put("excel_name", excel_name);
						
					}
					
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();

					
				} else if (!dataFlag) {
					/* 지우지 말것 */
					String _fname = mf.getOriginalFilename();
					String _errorFilePath = massOrderService.makeErrorDirectory(_fname, errorFilePath);
					String _dateStr = DateUtil.getToDayTimeStrForErrorFileMake();
					ExcelExportManager.errFilename = _errorFilePath+_fname+"_E_"+_dateStr;
					ExcelExportManager.eFlag = true;
					ExcelExportManager.makeFinalDataErrorFile(importDataSet.getImportData(), COL_CNT);
					String excel_name = _fname+"_E_"+_dateStr;
					
					List<Map<String,Object>> finalCodeList = new ArrayList<Map<String,Object>>();
					
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					
					errDataList = massOrderService.removeNull(COL_CNT);
					errDataList = massOrderService.removeDupliDataList(oriFileName);
					finalCodeList = massOrderService.makeFinalList(oriFileName, errDataList, COL_CNT);
					
					result.put("dataError", "Y");
					result.put("errDataList", errDataList);
					result.put("errCnt", errDataList.size());
					result.put("codeList", finalCodeList);
					result.put("codeCnt", finalCodeList.size());
					result.put("res", "ERR");
					result.put("fname", oriFileName);
					result.put("excel_name", excel_name);

					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();
//							
				} else if (yangFlag && dataFlag) {
					/*************************************************************************************
					 * 데이터 병합(계약고유번호 부여) 시작
					 *************************************************************************************/	
					if (!"no_merge".equals(chkOp)) {
						int beforeCount = importDataSet.getImportData().size(); 
						if (isDeahangCheck) { 
							importDataSet = massOrderService.transport( importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SMALLING_DAEHANG); 
						} else { 
							if (isOnly_0201) { 
								importDataSet = massOrderService.transport( importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY); 
							} else { 
								importDataSet = massOrderService.transport( importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING); 
							} 
						}
						int afterCount = importDataSet.getImportData().size();
						//System.out.println("병합전 : "+beforeCount+" // 병합후 : "+afterCount);
						if ("cont_group".equals(chkOp)) {
							if (isDeahangCheck) { 
								importDataSet = massOrderService.contGroupGenerate( importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_DAEHANG); 
							} else { 
								if (isOnly_0201) { 
									importDataSet = massOrderService.contGroupGenerate( importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY); 
								} else { 
									importDataSet = massOrderService.contGroupGenerate( importDataSet, importDataSet.getImportData(), RESOURCE_VAR.TYPE_SMALLING); 
								} 
							} 
						}
						
						String dateStr = DateUtil.getToDayTimeStrForErrorFileMake();

						if (isDeahangCheck) {
							ExcelExportManager.exportExcelForConvert(RESOURCE_VAR.TYPE_SMALLING_DAEHANG, importDataSet.getImportData(), mergeFilePath, "[병합완료]"+dateStr+"_"+saveFileName,false); 
						} else { 
							if (isOnly_0201) { 
								ExcelExportManager.exportExcelForConvert(RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY, importDataSet.getImportData(), mergeFilePath, "[병합완료]"+dateStr+"_"+saveFileName,false); 
							} else {
								ExcelExportManager.exportExcelForConvert(RESOURCE_VAR.TYPE_SMALLING, importDataSet.getImportData(), mergeFilePath, "[병합완료]"+dateStr+"_"+saveFileName,false); 
							} 
						} 	
					} 
				//System.out.println("=============데이터 병합 (계약고유번호 부여) 종료=========");
				 /*************************************************************************************
				 * 데이터 병합 (계약고유번호 부여) 종료
				 *************************************************************************************/
				/*************************************************************************************
				* .fpis 파일변환 시작
				************************************************************************************/
					ExtractObjectForSmalling eo = new ExtractObjectForSmalling();
					eo = (ExtractObjectForSmalling)massExtractService.extractData(importDataSet.getImportData(),COL_CNT,oriFileName);									
					eo.printContract();
					eo.printTrust();
					eo.printOperator();
					eo.printContractCharge();
					eo.printTrustCharge();

					//대행사업자인 경우 대행사업주체에 따라 파일을 나눠서 생성시킨다(입력단위설정과관련)
					if(isDeahangCheck){
						int dCnt = massExtractService.getDeahangBsnsNumCount(eo.getContractData());
						if(dCnt>1000){
//							JSONObject result = new JSONObject();
//							res.setCharacterEncoding("UTF-8");
//							result.put("fname", oriFileName);
//							result.put("res", "dCnt1000"); //대행-실적주체업체가 1000개 초과
//							PrintWriter out = res.getWriter();
//							out.write(result.toString());
//							out.close();

						} else {
							System.out.println("");
							FpisFileMaker fpisFileMaker = new FpisFileMaker(eo,saveFileName,exportFilePath);
							fr = fpisFileMaker.makeFPIS();
							fpisResultCommonList.addAll(fr.getFpisResultCommonList());
							JSONObject result = new JSONObject();
							res.setCharacterEncoding("UTF-8");
							result.put("fname", oriFileName);
							result.put("res", "SUC");
							PrintWriter out = res.getWriter();
							out.write(result.toString());
							out.close();
						}
					} else {
						FpisFileMaker fpisFileMaker = new FpisFileMaker(eo,saveFileName,exportFilePath);
						fr = fpisFileMaker.makeFPIS();
						fpisResultCommonList.addAll(fr.getFpisResultCommonList());
						JSONObject result = new JSONObject();
						res.setCharacterEncoding("UTF-8");
						result.put("fname", oriFileName);
						result.put("res", "SUC");
						PrintWriter out = res.getWriter();
						out.write(result.toString());
						out.close();
					}
						
				}
				f++;
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("ERROR : ", e);
				
				JSONObject result = new JSONObject();
				res.setCharacterEncoding("UTF-8");
				result.put("fname", oriFileName);
				result.put("res", "WARN");
				
				PrintWriter out;
				try {
					out = res.getWriter();
					out.write(result.toString());
					out.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	
	@RequestMapping(value = "/reg/mass/FpisOrderTBFileList.do", method = RequestMethod.POST, produces = "application/json; charset=utf8")
	public void multiTBFileValidation(MultipartHttpServletRequest req, HttpServletResponse res,
			RegVO vo, ModelMap model) throws JSONException {
		//System.out.println("################# (택배) 검증 시작 ##################");
		
		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String usrCond = session.getCond(); // 계정 업태 가져오기
		String usrCondDetail = session.getComp_cls_detail();
		String chkOp = req.getParameter("chkOp");
		ErrorVO err = null;
		
		/* 검증 실패 데이터 관련 변수 */
		List<Map<String,Object>> errCodeList = new ArrayList<Map<String,Object>>(); //검증실패 중 데이터 오류가 아닌 것
		List<Map<String,Object>> errDataList = new ArrayList<Map<String,Object>>(); //검증실패 중 데이터 오류
		boolean yangFlag = true; //양식검증 에러 flag
		boolean dataFlag = true; //데이터검증 에러 flag

		MassOrderVO massorderVO = new MassOrderVO();
		massorderVO.setUserIDAtSystem(session.getUser_id());
		massorderVO.setUsrMstKeyAtSystem(session.getUsr_mst_key());
		massorderVO.setCond(usrCond);
		massorderVO.setCondDetail(usrCondDetail);
		
		Utils.setUserIDAtSystem(session.getUser_id());
		String usr_mst_key = MassOrderVO.getUsrMstKeyAtSystem();
		//FpisResult fr = new FpisResult();
		
		boolean isDeahangCheck = false; // 대행 체크
		boolean isOnly_0201 = false;
		boolean checkRowLimit = true;
		boolean chkEmpty = true; // 데이터 미입력체크
		boolean checkBasicFormData = true; // 데이터 정합성체크
		boolean checkContfromRangeData = true;
		boolean checkUsrGov = true;
		boolean checkPreReg = true;
		boolean checkRegModifyAllow = true;
		boolean checkRegLimit = true;
		
		massOrderService.saveUsrInfo(usr_mst_key);
		massOrderService.saveUsrCond(usrCond, isDeahangCheck, isOnly_0201);
		
		List<MultipartFile> fileList = req.getFiles("regFile");
		
		int COL_CNT = 0;
		
		fpisResultCommonList = new ArrayList<FpisResultCommon>();
		
		for (int f = 0; f < fileList.size(); f++) {
			ImportDataSet importDataSet = new ImportDataSet();
			MultipartFile mf = fileList.get(f);
			String[] extTemp = mf.getOriginalFilename().split("[.]");
			String ext = extTemp[extTemp.length - 1];
			String saveFileName = extTemp[0];
			String oriFileName = mf.getOriginalFilename();
			//System.out.println("oriFileName : "+oriFileName);
			massorderVO.setSaveFileName(saveFileName);
			
			/*************************************************************************************
			 * 데이터 파일 로딩 시작
			 *************************************************************************************/
			
			try {
				massOrderService.reSetErrCodeList();
				massOrderService.reSetErrDataList();
				//확장자 이상 검증
				if (ext == null) {
					massOrderService.importCode(importDataSet, oriFileName, "UPL001");
					//System.out.println("(택배)UPL001 : " + err.getCode());
					yangFlag = false;
				} else { //시트 수 0 혹은 2개 이상 확인
					importDataSet = massOrderService.chkSheetCnt(mf);
					
				}
				
				try {
					if(ImportStatus.IS_NOT_OLD.equals(importDataSet.getImportStatus()) ) {
						yangFlag = true;
						importDataSet = massOrderService.getDataForSpd(mf);
						
					} else {
						yangFlag = false;
					//	System.out.println("!!!!!!!(택배)시트 수 초과");
					}
				} catch (NullPointerException e) {
					yangFlag = false;
					//System.out.println("&&&&&&&택배 UPL014, UPLCO1");
					massOrderService.importCode(importDataSet, oriFileName, "UPL014");
				}

			//공백데이터 제거 
				try { 
					dataEmptyArrayRemove(oriFileName, importDataSet,importDataSet.getImportData()); 
				} catch (Exception e) { 
					massOrderService.importCode(importDataSet, oriFileName, "COR001"); 
				}
				
				if (importDataSet.getImportData().size() != 0) {
					COL_CNT = importDataSet.getImportData().get(0).length;
				}
				
			/*************************************************************************************
			 * 데이터 파일 로딩 완료
			 *************************************************************************************/

			/*************************************************************************************
			 * 데이터 유효성 검증 시작
			 *************************************************************************************/
				if (importDataSet.isSuccess()) {
					
					//10000건 제한 검증
					importDataSet = massOrderService.chkRowLimit(oriFileName, importDataSet, importDataSet.getImportData());
					if (importDataSet.getImportData().size() > 10000) {
						checkRowLimit = false;
					} else {
						checkRowLimit = true;
					}
					
					//코드정보의 일관성유지를 위한 데이터 보정작업 
					massOrderService.dataSupplement(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_DATA_SUPPLEMENT);
					
					//데이터 미입력항목 검증 
					chkEmpty = massOrderService.checkEmptyData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING, COL_CNT);
					
					//데이터 정합성(숫자입력,차량번호 규칙 등) 검증 
					checkBasicFormData = massOrderService.checkBasicFormData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_FORM_VERIFYBITSTRING, RESOURCE_VAR.TYPE_SPD, isOnly_0201);			
					
					//데이터 동일년도 검증 
					checkContfromRangeData = massOrderService.checkContfromRangeData(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
					
					//관할관청 등록 여부 검증 	
					checkUsrGov = massOrderService.checkUsrGov(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
					String ImportDate = ""; 
					
					ImportDate = massOrderService.getImportDate(importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
					
					// 서버시간 가져오기 
					Calendar cal = Calendar.getInstance(); 
					String format = "yyyy/MM";
					SimpleDateFormat sdf = new SimpleDateFormat(format); 
					String serverDate = sdf.format(cal.getTime()); 
					String serverYear = serverDate.split("/")[0]; 
					String serverMonth = serverDate.split("/")[1]; 
					String importYear = ""; 
					String importMonth = "";	  
					
					//실적신고 가능년도 여부 확인 1. 현재년도 -2보다 입력년도가 작거나 같으면 || 현재년도 -1이 입력년도와 같은데 현재 월이 7월 ~ 12월이면 2.
					//현재 월이 4~6월 && 입력한 데이터에 년도가 현재년도 -1 일때
					
					if (!"".equals(ImportDate)) {
						importYear = ImportDate.substring(0, 4); 
						importMonth = ImportDate.substring(4, 6);
						
						if ((Integer.parseInt(serverYear) - 2) >= Integer.parseInt(importYear) || ((Integer.parseInt(serverYear) - 1) == Integer.parseInt(importYear) && (Integer.parseInt(serverMonth) >= 7))) { 
							
							checkPreReg = massOrderService.checkPreReg(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear);
						} else { // 현재 월이 4~6월이면서 입력한 데이터에 년도가 현재년도 -1 일때
							ExcelExportManager.setFlag_preReg(true);
							//등록허가 대상 여부 검증 
							if ((Integer.parseInt(serverMonth) >= 4 && Integer.parseInt(serverMonth) <= 6) && (Integer.parseInt(serverYear) - 1) == Integer.parseInt(importYear)) {
								
								checkRegModifyAllow = massOrderService.checkRegModifyAllow(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT);
							} else {
								ExcelExportManager.setFlag_regAllowDae(true);
							}
						}
					}
					
					//실적신고 제한 여부 검증 관할관청 등록 여부 검증 
					checkRegLimit = massOrderService.checkRegLimit(oriFileName, importDataSet, importDataSet.getImportData(),RESOURCE_VAR.TYPE_SPD_CONTFROM_RANGE_VERIFYBITSTRING, COL_CNT, importYear);
					/*************************************************************************************
					 * (택배)데이터 유효성 검증 종료
					 *************************************************************************************/
					//에러 파일 생성
					if(!checkRowLimit || !chkEmpty || !checkBasicFormData || !checkContfromRangeData || !checkPreReg) {
						dataFlag = false;
					} else if (!checkRegModifyAllow || !checkUsrGov || !checkRegLimit) {
						yangFlag = false;
					}
				} else {
					//양식검증 오류 엑셀 및 메세지 넘기기
					yangFlag = false;
				}
				
				if (!yangFlag) {
					
					String _fname = mf.getOriginalFilename();
					String _errorFilePath = massOrderService.makeErrorDirectory(_fname, errorFilePath);
					String _dateStr = DateUtil.getToDayTimeStrForErrorFileMake();
					ExcelExportManager.errFilename = _errorFilePath+_fname+"_E_"+_dateStr;
					ExcelExportManager.eFlag = true;
					ExcelExportManager.makeFinalHeaderErrorFile(importDataSet.getImportData(), COL_CNT);
					String excel_name = _fname+"_E_"+_dateStr;
					
					List<Map<String,Object>> finalCodeList = new ArrayList<Map<String,Object>>();
					errCodeList = massOrderService.getErrCodeList();
					finalCodeList = massOrderService.makeFinalList(oriFileName, errCodeList, COL_CNT);
					
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					
					if (errCodeList.size() > 0) {
						result.put("yangError", "Y");
						result.put("errCodeList", errCodeList);
						result.put("errCnt", errCodeList.size());
						result.put("codeList", finalCodeList);
						result.put("codeCnt", finalCodeList.size());
						result.put("res", "ERR");
						result.put("fname", oriFileName);
						result.put("excel_name", excel_name);
						
					}
					
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();

				} else if (!dataFlag) {
					String _fname = mf.getOriginalFilename();
					String _errorFilePath = massOrderService.makeErrorDirectory(_fname, errorFilePath);
					String _dateStr = DateUtil.getToDayTimeStrForErrorFileMake();
					ExcelExportManager.errFilename = _errorFilePath+_fname+"_E_"+_dateStr;
					ExcelExportManager.eFlag = true;
					ExcelExportManager.makeFinalDataErrorFile(importDataSet.getImportData(), COL_CNT);
					String excel_name = _fname+"_E_"+_dateStr;
					
					List<Map<String,Object>> finalCodeList = new ArrayList<Map<String,Object>>();
					
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					
					errDataList = massOrderService.removeNull(COL_CNT);
					errDataList = massOrderService.removeDupliDataList(oriFileName);
					finalCodeList = massOrderService.makeFinalList(oriFileName, errDataList, COL_CNT);
					//finalCodeList = massOrderService.makeErrorInfo(finalCodeList, COL_CNT);

					result.put("dataError", "Y");
					result.put("errDataList", errDataList);
					result.put("errCnt", errDataList.size());
					result.put("codeList", finalCodeList);
					result.put("codeCnt", finalCodeList.size());
					result.put("res", "ERR");
					result.put("fname", oriFileName);
					result.put("excel_name", excel_name);
					
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();
				} else if (yangFlag && dataFlag) {

					
				/*************************************************************************************
				 * (택배).fpis 파일변환 시작
				 *************************************************************************************/
					ExtractObjectForSpd eo = new ExtractObjectForSpd();
					eo = (ExtractObjectForSpd)massExtractService.extractData(importDataSet.getImportData(),COL_CNT, oriFileName);
					FpisFileMaker fpisFileMaker = new FpisFileMaker(eo, saveFileName, exportFilePath);			
					fr = fpisFileMaker.makeFPIS();
					fpisResultCommonList.addAll(fr.getFpisResultCommonList());
					
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					result.put("fname", oriFileName);
					result.put("res", "SUC");
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();
				}
			
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("ERROR : ", e);
				JSONObject result = new JSONObject();
				res.setCharacterEncoding("UTF-8");
				result.put("fname", oriFileName);
				result.put("res", "WARN");
				PrintWriter out;
				try {
					out = res.getWriter();
					out.write(result.toString());
					out.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

	public String rowLimit(ImportDataSet importDataSet) {
		int rowCnt = importDataSet.getImportData().size();
		if (rowCnt > 10000) {
			return "error";
		}
		return "pass";
	}

	private void dataEmptyArrayRemove(String fname, ImportDataSet importDataSet, List<String[]> listDataParam) {
		List<String[]> listData =
				massOrderService.dataEmptyArrayRemove(fname, importDataSet, listDataParam);
		setListData(importDataSet, listData);
	}

	public boolean setListData(ImportDataSet importDataSet,List<String[]> listData) {
		importDataSet.setImportData(listData);
		return true;
	}
	
	/**
	 * @method_desc 대량실적등록 등록 화면 
	 * @returns String
	 *
	 * @HISTORY DATE 		  AUTHOR 	NOTE
	 *  		------------- --------- ------------------------ 
	 *  		2022. 10. 06. 최정원		최초생성
	 *          c
	 *
	 */
	@RequestMapping("/reg/mass/MassOrderUploadPage.do")
	public String massOrderUploadPage(HttpServletRequest req, ModelMap model, RegVO vo)
			throws Exception, NullPointerException {
		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String usr_mst_key = session.getUsr_mst_key();
		String tbYN = session.getComp_cls_detail_tb_yn();
		String chkCond = session.getCond(); // 계정 업태 가져오기
//		String usrCond = "";
		String BCODE = "";
		String stt = "";
		int uploadCnt = 0;
		
		MassOrderVO massorderVO = new MassOrderVO();
		massorderVO.setCond(chkCond);
		
		BCODE = massOrderService.getBCODE(chkCond);
		uploadCnt = massOrderService.getUploadFileCnt(usr_mst_key);
		
		if (uploadCnt < 1) {
			stt = "N";
		} else {
			stt = "Y";
		}
		int fileCnt = 0;
		if (req.getParameter("fileCnt") != null) {
			fileCnt = Integer.parseInt(req.getParameter("fileCnt"));
		} else {
			fileCnt = massOrderService.getUploadFileCnt(usr_mst_key);
		}
		
		model.addAttribute("fileCnt", fileCnt);
		
		// 검색조건을 담습니다.
		model.addAttribute("tbYN",tbYN);
		model.addAttribute("rcode", "R1");
		model.addAttribute("bcode", BCODE);
		model.addAttribute("stt", stt);
		model.addAttribute("chkUploadPage", "Y");
		
		return "/fpis/reg/mass/FpisOrderUploadPage";
	}
	
	@RequestMapping(value = "/reg/mass/UploadFileList.do", method = RequestMethod.POST)
	public void uploadFileList(HttpServletRequest req, HttpServletResponse res, ModelMap model, @RequestParam(value="fileCnt") Integer fileCnt)
			throws Exception, NullPointerException {

		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String usr_mst_key = session.getUsr_mst_key();
		
		JSONObject result = new JSONObject();
		res.setCharacterEncoding("UTF-8");
		
		int uploadCnt = 0;
		uploadCnt = massOrderService.getUploadFileCnt(usr_mst_key);
		
		List<Map<String,Object>> uploadFileList = new ArrayList<Map<String,Object>>();
		
		uploadFileList = massOrderService.fpisFileUploadList(fileCnt, uploadCnt, fpisResultCommonList, usr_mst_key);
		if (uploadCnt < 1) {
			result.put("upload_YN", "X");
			result.put("fileList", uploadFileList);
		} else {
			result.put("upload_YN", "N");
			result.put("fileList", uploadFileList);
		}
		
		PrintWriter out = res.getWriter();
		out.write(result.toString());
		out.close();
		
	}
	
	@RequestMapping(value = "/reg/mass/MassSmallingUpload.do", method = RequestMethod.POST)
	public void massSmallingUpload(HttpServletRequest req, HttpServletResponse res, ModelMap model, RegVO vo){
		
		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String usr_mst_key = session.getUsr_mst_key();
		
		massOrderService.insertRegUploadResult(fpisResultCommonList,usr_mst_key);
		try {

			File [] fpisFiles = new File[fpisResultCommonList.size()];
			
			for (int i = 0; i < fpisResultCommonList.size(); i++) {
				fpisFiles[i] = new File(exportFilePath+fpisResultCommonList.get(i).getFpisFileName());
			}
			
			Map<String, Object> resultMap = massOrderService.fpisFileUploadForSmalling(fpisFiles,"1");
			//ManageList ml = new ManageList();
			if(resultMap.get("UPLOAD_ENABLE") == null){
				if(resultMap.get(RESOURCE_VAR.JSON_SUCCESS_CODE).equals("TRUE")){
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					switch(Integer.parseInt(String.valueOf(resultMap.get("RESULT")))) {
						case 1 :
							result.put("res","1");
							break;
						case -1 :
							result.put("res","-1");
							break;
						case -4 :
							result.put("res","-4");
							break;
						case -9 :
							result.put("res","-9");
							break;
						default :
							result.put("res","0");
							break;
					}
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();

				} else {
					//System.out.println(">>>>>>Warining");
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					result.put("res","-2");
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();
				}
				
			} else {
				//System.out.println(">>>>>>서버전송 제한");
				JSONObject result = new JSONObject();
				res.setCharacterEncoding("UTF-8");
				result.put("res","-3");
				PrintWriter out = res.getWriter();
				out.write(result.toString());
				out.close();
			}
		} catch(Exception e) {
			JSONObject result = new JSONObject();
			res.setCharacterEncoding("UTF-8");
			try {
				result.put("res","-5");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			PrintWriter out;
			try {
				out = res.getWriter();
				out.write(result.toString());
				out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/reg/mass/MassSmallingUpload_TB.do", method = RequestMethod.POST)
	public void massSmallingUpload_tb(HttpServletRequest req, HttpServletResponse res, ModelMap model, RegVO vo){
		
		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String usr_mst_key = session.getUsr_mst_key();
		
		massOrderService.insertRegUploadResult(fpisResultCommonList,usr_mst_key);
		try {
			File [] fpisFiles = new File[fpisResultCommonList.size()];
			
			for (int i = 0; i < fpisResultCommonList.size(); i++) {
				fpisFiles[i] = new File(exportFilePath+fpisResultCommonList.get(i).getFpisFileName());
			}
			
			
			Map<String, Object> resultMap = massOrderService.fpisFileUploadForSmallingTB(fpisFiles,"1");
			//ManageList ml = new ManageList();
			if(resultMap.get("UPLOAD_ENABLE") == null){
				if(resultMap.get(RESOURCE_VAR.JSON_SUCCESS_CODE).equals("TRUE")){
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					switch(Integer.parseInt(String.valueOf(resultMap.get("RESULT")))) {
						case 1 :
							result.put("res","1");
							break;
						case -1 :
							result.put("res","-1");
							break;
						case -4 :
							result.put("res","-4");
							break;
						case -9 :
							result.put("res","-9");
							break;
						default :
							result.put("res","0");
							break;
					}
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();

				} else {
					JSONObject result = new JSONObject();
					res.setCharacterEncoding("UTF-8");
					result.put("res","-2");
					PrintWriter out = res.getWriter();
					out.write(result.toString());
					out.close();
				}
				
			} else {
				//System.out.println(">>>>>>서버전송 제한");
				JSONObject result = new JSONObject();
				res.setCharacterEncoding("UTF-8");
				result.put("res","-3");
				PrintWriter out = res.getWriter();
				out.write(result.toString());
				out.close();
			}
		} catch(Exception e) {
			JSONObject result = new JSONObject();
			res.setCharacterEncoding("UTF-8");
			try {
				result.put("res","-5");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			PrintWriter out;
			try {
				out = res.getWriter();
				out.write(result.toString());
				out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	@RequestMapping("/reg/mass/UploadConfirm.do")
	public String uploadConfirm(HttpServletRequest req, HttpServletResponse res, ModelMap model){
		SessionVO session = (SessionVO) req.getSession().getAttribute("SessionVO");
		String usr_mst_key = session.getUsr_mst_key();
		massOrderService.updateUploadResultY(usr_mst_key);
		model.addAttribute("rcode",req.getParameter("rcode"));
		return "redirect:/reg/unit/FpisOrderMassRegist_intro.do";
	}
	
	/* 2023.04.12 jwchoi 대량실적 등록 :: 계약검증 내 상세코드 참고 팝업 */
	
	@RequestMapping(value = "/reg/mass/ErrCodePopup.do", produces = "application/text; charset=utf-8", method = { RequestMethod.POST, RequestMethod.GET })
	public String errCodePopup(HttpServletRequest req, HttpServletResponse res, ModelMap model){
		
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy");
		Date now = new Date();
		String nowYear = sdf1.format(now);
		model.addAttribute("nowYear", nowYear);
		return "/fpis/reg/mass/FpisErrCodePopup";
	}

}
