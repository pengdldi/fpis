package fpis.stat.result.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.SynchronizedOutputStream;

import fpis.admin.accessLog.FpisAccessLogService;
import fpis.admin.accessLog.FpisAccessLogVO;
import fpis.admin.obeySystem.FpisAdminObeySystemService;
import fpis.admin.obeySystem.FpisAdminStatReCountVO;
import fpis.admin.obeySystem.FpisAdminStatTrans4VO;
import fpis.admin.obeySystem.FpisAdminStatTrans7VO;
import fpis.admin.obeySystem.FpisAdminStatTrans8VO;
import fpis.admin.obeySystem.FpisAdminSysCarMinDataVO;
import fpis.admin.obeySystem.trans.FpisAdminTransService;
import fpis.admin.obeySystem.trans.FpisMviewCarMinVO;
import fpis.admin.obeySystem.trans.FpisMviewContractVO;
import fpis.admin.obeySystem.trans.FpisMviewOperateTrustVO;
import fpis.admin.obeySystem.trans.FpisMviewStateVO;
import fpis.admin.stat.FpisAdminStatBase12VO;
import fpis.admin.stat.FpisAdminStatService;
import fpis.common.service.CommonService;
import fpis.common.utils.Util;
import fpis.common.utils.Util_poi;
import fpis.common.vo.SessionVO;
import fpis.stat.result.service.FpisStatCarMinDataVO;
import fpis.stat.result.service.FpisStatTrans1CarMinYearData;
import fpis.stat.result.service.FpisStateBase1VO;
import fpis.stat.result.service.FpisStateQueryService;
import twitter4j.internal.org.json.JSONArray;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.org.json.JSONObject;

@Controller
public class FpisStateQueryContrller {

	private static final Logger logger = Logger.getLogger(FpisStateQueryContrller.class);

	@Value(value = "#{fpis['FPIS.domain']}")
	private String program_domain;

	@Value(value = "#{globals['Globals.majarStatFilePath']}")
	private String majarStatFilePath;

	@Value(value = "#{globals['Globals.fileStorePath']}")
	private String fileStorePath;

	// Service Area
	@Resource(name = "FpisStateQueryService")
	private FpisStateQueryService QuerySvc;

	@Resource(name = "FpisAdminObeySystemService")
	private FpisAdminObeySystemService AdminObeySystemService;

	@Resource(name = "FpisAdminTransService")
	private FpisAdminTransService FpisSvc; // 2014.09.16 mgkim 지역 업체 검색 기능 추가

	@Resource(name = "FpisAdminStatService")
	private FpisAdminStatService adminStatSvc;

	//2020.11.10 ysw 사업자정보 이력을 위한 서비스
	@Resource(name = "FpisAccessLogService")
	private FpisAccessLogService accessLogService;

	@Resource(name = "CommonService")
	private CommonService commonService;

	/* 대메뉴 최초접근시 너어어무 느려서 수정 - 2021.11.08 suhyun */
	@RequestMapping(value = "/stat/FpisStatBase1_page.do", method = RequestMethod.POST)
	public String FpisStatBase1_page(HttpServletRequest req,	ModelMap model) {
		/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String rCode = req.getParameter("rcode");
		String refer_domain = req.getHeader("referer");
		if (!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}
		int strYear = 2012;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);

		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);
		model.addAttribute("rcode", rCode);
		return "/fpis/stat/base/FpisStatBase1";
	}

	/* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 */

	/*
	 * 2013.09.30 mgkim 통계관리 - 신고내역(기초통계) [신규]
	 * 2014.10.07 mgkim 실시간 집계로 통계시간이 너무 느림 관리자 페이지의 MVIEW로 변경처리 sqlmap만 변경됨.
	 * 2015.01.28 mgkim 사업단회의 결과 실적신고 선택항목 제거에 따라 운송품목별 통계 기능 제거
	 * 2021.01.11 ysw 보안취약점 METHOD POST 만 가능하도록 수정
	 */
	@RequestMapping(value = "/stat/FpisStatBase1.do", method = RequestMethod.POST)
	public String FpisStatBase1(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {

		String rCode = req.getParameter("rcode");
		/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if (!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		String search_year = req.getParameter("search_year");
		String search_month = req.getParameter("search_month");

		//        String search_type    = req.getParameter("search_type"); // 2015.01.28 mgkim 운송품목별 통계 기능 제거
		// String search_type = "type01"; // not used 제거 - 2021.11.03 suhyun

		int strYear = 2012;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);

		// 2015.01.28 mgkim 운송품목별 통계 기능 제거
		//        if(search_type == null || search_type.equals("")){         // 2013.12.30 mgkim 초기값 상태 세팅 요청 - 이부장
		//            search_type = "type01";
		//            search_year = ""+endYear;
		//            search_month = ""+(Calendar.getInstance().get(Calendar.MONTH)+1);
		//        }
		if (search_year == null || search_year.equals("")) { // 2013.12.30 mgkim 초기값 상태 세팅 요청 - 이부장
			search_year = "" + (Calendar.getInstance().get(Calendar.YEAR));
		}
		/*if(search_month != null && search_month.length() < 2){
		    search_month = "0"+search_month;
		}*/
		String search_yyyy_mm = search_year;//+"-"+search_month;

		List<FpisStateBase1VO> voList = null;
		//        if(search_type != null && !search_type.equals("")){         // 검색전 초기상태 // 2015.01.28 mgkim 운송품목별 통계 기능 제거
		FpisStateBase1VO search_params = new FpisStateBase1VO();
		search_params.setUsr_mst_key(svo.getUsr_mst_key());
		search_params.setSearch_yyyy_mm(search_yyyy_mm);
		//            if(search_type.equals("type01")){                        // 실적신고  // 2015.01.28 mgkim 운송품목별 통계 기능 제거
		voList = QuerySvc.FpisStatBase1_type01(search_params);
		//            }else if(search_type.equals("type02")){                  // 화물품목별 // 2015.01.28 mgkim 운송품목별 통계 기능 제거
		//                voList = QuerySvc.FpisStatBase1_type02(search_params);
		//            }else if(search_type.equals("type03")){                  // 화물형태별
		//                voList = QuerySvc.FpisStatBase1_type03(search_params);
		//            } // 2015.01.28 mgkim 운송품목별 통계 기능 제거
		//        }

		/*실적 그래프 표현 데이터*/
		JSONArray mainArr = new JSONArray();
		FpisStateBase1VO gFpisStateBase1VO = null;

		JSONObject contObj = null;
		for (int i = 0; i < voList.size(); i++) {
			gFpisStateBase1VO = voList.get(i);
			contObj = new JSONObject();
			contObj.put("유형", "cont");
			contObj.put("ym", gFpisStateBase1VO.getSearch_yyyy_mm());
			contObj.put("통계월", gFpisStateBase1VO.getSearch_month() + "월");
			contObj.put("횟수", 0);
			contObj.put("charge", gFpisStateBase1VO.getCont_charge());
			mainArr.put(contObj);
		}

		JSONObject operObj = null;
		for (int i = 0; i < voList.size(); i++) {
			gFpisStateBase1VO = voList.get(i);
			operObj = new JSONObject();
			operObj.put("유형", "oper");
			operObj.put("ym", gFpisStateBase1VO.getSearch_yyyy_mm());
			operObj.put("통계월", gFpisStateBase1VO.getSearch_month() + "월");
			operObj.put("횟수", gFpisStateBase1VO.getOperate_cnt());
			operObj.put("charge", gFpisStateBase1VO.getOperate_charge());
			mainArr.put(operObj);
		}

		JSONObject trustObj = null;
		for (int i = 0; i < voList.size(); i++) {
			gFpisStateBase1VO = voList.get(i);
			trustObj = new JSONObject();
			trustObj.put("유형", "trust");
			trustObj.put("ym", gFpisStateBase1VO.getSearch_yyyy_mm());
			trustObj.put("통계월", gFpisStateBase1VO.getSearch_month() + "월");
			trustObj.put("횟수", 0);
			trustObj.put("charge", gFpisStateBase1VO.getTrust_charge());
			mainArr.put(trustObj);
		}


		model.addAttribute("gdata", mainArr.toString());

		/*위탁실적 관계도 표현*/
		/*FpisBase1_MapVo fpisBase1_MapVo = new FpisBase1_MapVo();
		Calendar c = Calendar.getInstance();
		String rl_year = String.valueOf(c.get(Calendar.YEAR));
		String rl_month = String.valueOf(c.get(Calendar.MONTH)+1);
		String e_day = String.valueOf(c.getActualMaximum(Calendar.DATE));

		fpisBase1_MapVo.setSearch_start(rl_year+"-"+"01"+"-"+"01");
		fpisBase1_MapVo.setSearch_end(rl_year+"-"+rl_month+"-"+e_day);
		fpisBase1_MapVo.setUsr_mst_key(svo.getUsr_mst_key());

		List<FpisBase1_MapVo> rlList = QuerySvc.selectFpisBase1_Map(fpisBase1_MapVo);
		JSONObject main = new JSONObject();
		JSONArray mainchildren = new JSONArray();

		JSONObject operjson = new JSONObject();
		operjson.put("name", "배차");
		JSONObject trustjson = new JSONObject();
		trustjson.put("name", "위탁");
		JSONArray operchildren = new JSONArray();
		JSONArray trustchildren = new JSONArray();
		FpisBase1_MapVo f = null;

		boolean isOperExist = false;
		boolean isTrustExist = false;

		Map<String, List<String>> operMap = new HashMap<String, List<String>>();
		Map<String, List<String>> trustMap = new HashMap<String, List<String>>();
		for(int i = 0 ; i < rlList.size(); i++) {
			f = rlList.get(i);

			if(f.getType().equals("1")){
				if(operMap.containsKey(f.getClient())){
					List<String> lll = operMap.get(f.getClient());
					lll.add(f.getTarget());
					operMap.put(f.getClient(), lll);
				}else{
					List<String> lll = new ArrayList<String>();
					lll.add(f.getTarget());
					operMap.put(f.getClient(), lll);
				}
				isOperExist = true;
			}else{
				//trustchildren.put(o_json);
				if(trustMap.containsKey(f.getClient())){
					List<String> lll = trustMap.get(f.getClient());
					lll.add(f.getTarget());
					trustMap.put(f.getClient(), lll);
				}else{
					List<String> lll = new ArrayList<String>();
					lll.add(f.getTarget());
					trustMap.put(f.getClient(), lll);
				}
				isTrustExist = true;
			}
		}

		if(isOperExist) {
			Set<String> operKeys = operMap.keySet();
			Iterator<String> operIter = operKeys.iterator();
			while(operIter.hasNext()) {
				String key = operIter.next();
				List<String> lll = operMap.get(key);
				JSONObject operClient = new JSONObject();
				operClient.put("name", key);
				JSONArray arr = new JSONArray();
				int operLimit = 0;
				for(int i = 0 ; i < lll.size(); i++) {
					JSONObject ccc = new JSONObject();
					if(operLimit == 30){
						ccc.put("name", "그 외 "+(lll.size()-operLimit)+"건");
						ccc.put("size", 1);
						arr.put(ccc);
						break;
					}
					ccc.put("name", lll.get(i));
					ccc.put("size", 1);
					arr.put(ccc);
					operLimit++;
				}
				operClient.put("children", arr);
				operchildren.put(operClient);
			}
			operjson.put("children", operchildren);
		}else{
			operjson.put("size", "1");
		}
		if(isTrustExist) {
			Set<String> trustKeys = trustMap.keySet();
			Iterator<String> trustIter = trustKeys.iterator();
			while(trustIter.hasNext()) {
				String key = trustIter.next();
				List<String> lll = trustMap.get(key);
				JSONObject trustClient = new JSONObject();
				trustClient.put("name", key);
				JSONArray arr = new JSONArray();
				int trustLimit = 0;
				for(int i = 0 ; i < lll.size(); i++) {
					JSONObject ccc = new JSONObject();
					if(trustLimit == 30){
						ccc.put("name", "그 외 "+(lll.size()-trustLimit)+"건");
						ccc.put("size", 1);
						arr.put(ccc);
						break;
					}
					ccc.put("name", lll.get(i));
					ccc.put("size", 1);
					arr.put(ccc);
				}
				trustClient.put("children", arr);
				trustchildren.put(trustClient);
			}
			trustjson.put("children", trustchildren);
		}else{
			trustjson.put("size", "1");
		}

		mainchildren.put(operjson);
		mainchildren.put(trustjson);

		main.put("name", svo.getUser_id());
		main.put("children", mainchildren);

		model.addAttribute("rljson", main.toString());*/

		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);
		List<String> monthList = new ArrayList<String>();
		monthList.add("01");
		monthList.add("02");
		monthList.add("03");
		monthList.add("04");
		monthList.add("05");
		monthList.add("06");
		monthList.add("07");
		monthList.add("08");
		monthList.add("09");
		monthList.add("10");
		monthList.add("11");
		monthList.add("12");
		model.addAttribute("monthList", monthList);
		model.addAttribute("search_year", search_year);
		model.addAttribute("search_month", search_month);
		//        model.addAttribute("search_type", search_type); // 2015.01.28 mgkim 운송품목별 통계 기능 제거

		model.addAttribute("voList", voList);
		model.addAttribute("rcode", rCode);

		return "/fpis/stat/base/FpisStatBase1";
	}

	/*
	 * 2013.10.28 mgkim 통계관리 - 운송의무제 [신규] 기본 UI 구현
	 * 2014.01.22 mgkim 운송의무제 년도 검색 항목 추가
	 * 2014.09.19 mgkim 최소운송기준제 전용메뉴로 분리
	 */
	@RequestMapping("/stat/FpisStatTrans1.do")
	public String FpisStatTrans1(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		//SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");
		String search_year = req.getParameter("search_year");

		/* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 시작*/
		FpisAdminSysCarMinDataVO sysCarMinDataVO = null;
		List<FpisAdminSysCarMinDataVO> sysCarMinDataRateList = AdminObeySystemService.selectSysCarMinDataRateList(sysCarMinDataVO);
		model.addAttribute("sysCarMinDataRateList", sysCarMinDataRateList);
		/* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 끝 */

		// 2014.01.22 mgkim 년도 데이터 추가
		int strYear = 2013;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);

		model.addAttribute("search_year", search_year);

		return "/fpis/stat/trans/FpisStatTrans1";
	}

	/*
	 * 2014.09.19 mgkim 통계관리 - 직접운송의무제 전용메뉴로 분리
	 */
	@RequestMapping("/stat/FpisStatTrans2.do")
	public String FpisStatTrans2(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		//SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");
		String search_year = req.getParameter("search_year");

		// 2014.01.22 mgkim 년도 데이터 추가
		int strYear = 2013;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);

		model.addAttribute("search_year", search_year);

		return "/fpis/stat/trans/FpisStatTrans2";
	}

	/*
	 * 2014.03.25 mgkim 최소운송 매출액 보기 DB로 기능변경
	 */
	@RequestMapping("/stat/FpisPopupStatCarMinDataViewDB.do")
	public String FpisPopupStatCarMinDataViewDB(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		//SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");
		String search_year = req.getParameter("search_year");
		List<FpisStatCarMinDataVO> carMinDataList = QuerySvc.selectSysCarMinData(search_year);

		model.addAttribute("search_year", search_year);
		model.addAttribute("carMinDataList", carMinDataList);

		return "/fpis/stat/trans/FpisPopupStatCarMinDataView";
	}

	/*
	 * 2014.03.20 mgkim 사용자P - 통계정보 - 운송의무제
	 * 최소운송기준제 데이터 분석 ajax 모듈
	 * 2014.09.19 mgkim 관리자P - 제도준수 - 최소운송기준제 기능 추가
	 * 2015.07.16 mgkim 최소운송기준 온라인마트 기능개발(2015년 데이터만 조회가능)
	 */
	@RequestMapping("/stat/FpisStatTrans1_CarMinYearData.do")
	public void FpisStatTrans1_CarMinYearData(@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			@RequestParam(value = "car_min_data_year", required = false) String car_min_data_year,
			HttpServletResponse res,
			HttpServletRequest req,
			Model model) throws Exception, NullPointerException {
		FpisStatTrans1CarMinYearData shVO = new FpisStatTrans1CarMinYearData();
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		shVO.setSearch_year(search_year);
		shVO.setCar_min_data_year(car_min_data_year);
		if (svo.getMber_cls().equals("GNR")) { // 신고자 본인이 조회하는경우
			shVO.setUsr_mst_key(svo.getUsr_mst_key());
		} else { // 2014.09.19 mgkim
			String s_usr_mst_key = usr_mst_key.replaceAll("-", "");
			shVO.setUsr_mst_key(s_usr_mst_key);
		}

		List<FpisStatTrans1CarMinYearData> carMinYearDataList = QuerySvc.selectStatTrans1CarMinYearData(shVO);
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(carMinYearDataList.toString());
		out.close();
	}

	/*
	 * 2015.07.17 mgkim 2015년 온라인마트 통계가공 완료상태 체크
	 */
	@RequestMapping("/stat/FpisMviewState.do")
	public void FpisMviewState(@RequestParam(value = "search_year", required = false) String search_year,
			HttpServletResponse res,
			HttpServletRequest req,
			Model model) throws Exception, NullPointerException {
		FpisMviewStateVO shVO = new FpisMviewStateVO();
		shVO.setData_year(search_year);
		FpisMviewStateVO directYearData = QuerySvc.selectFpisMviewState(shVO);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(directYearData.toString());
		out.close();
	}

	/*
	 * 2015.07.27 mgkim 최소운송기준제 위수탁(지입) 차량대수, 해당차량 기준금액 합산 가져오기
	 */
	@RequestMapping("/stat/selectOnlineMartCarMinBaseValue.do")
	public void selectOnlineMartCarMinBaseValue(@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			HttpServletResponse res,
			HttpServletRequest req,
			Model model) throws Exception, NullPointerException {
		FpisMviewCarMinVO shVO = new FpisMviewCarMinVO();
		shVO.setData_year(search_year);
		String s_usr_mst_key = usr_mst_key.replaceAll("-", "");
		shVO.setUsr_mst_key(s_usr_mst_key);
		FpisMviewCarMinVO reg_contract = QuerySvc.selectOnlineMartCarMinBaseValue(shVO);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(reg_contract.toString());
		out.close();
	}

	/*
	 * 2015.07.20 mgkim 온라인마트 계약정보 가져오기 ajax 모듈
	 *
	 * 계약(일반운송) 1단계 금액, 계약(일반운송) 2단계 금액, 계약(택배) 금액
	 *
	 * 2015.07.27 mgkim 최소운송기준제 계약금액가져오기 공통사용
	 * 2015.07.27 mgkim 직접운송의무제 계약금액가져오기 공통사용
	 */
	@RequestMapping("/stat/selectOnlineMartRegContract.do")
	public void selectOnlineMartRegContract(@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			@RequestParam(value = "gubun", required = false) String gubun,
			HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, NullPointerException {
		FpisMviewContractVO shVO = new FpisMviewContractVO();
		shVO.setData_year(search_year);
		String s_usr_mst_key = usr_mst_key.replaceAll("-", "");
		shVO.setUsr_mst_key(s_usr_mst_key);
		shVO.setGubun(gubun);
		FpisMviewContractVO reg_contract = QuerySvc.selectOnlineMartRegContract(shVO);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(reg_contract.toString());
		out.close();
	}

	/*
	 * 2015.08.05 mgkim 온라인마트 배차정보 가져오기 ajax 모듈
	 *
	 * 배차(일반운송) 1단계 금액, 배차(일반운송) 2단계 금액, 배차(택배) 금액
	 * 배차(택배) 금액은 사용자 최종화면에서는 배차(일반운송) 1단계 금액과 합산하여 정보를 제공한다.
	 */
	@RequestMapping("/stat/selectOnlineMartRegOperateTrust.do")
	public void selectOnlineMartRegOperateTrust(@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			@RequestParam(value = "bungi", required = false) String bungi,
			HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, NullPointerException {
		FpisMviewOperateTrustVO shVO = new FpisMviewOperateTrustVO();
		shVO.setData_year(search_year);
		String s_usr_mst_key = usr_mst_key.replaceAll("-", "");
		shVO.setUsr_mst_key(s_usr_mst_key);
		shVO.setBungi(bungi);
		FpisMviewOperateTrustVO reg_contract = QuerySvc.selectOnlineMartRegOperateTrust(shVO);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(reg_contract.toString());
		out.close();
	}

	/*
	 * 2015.08.17 mgkim 오프라인마트 계약정보 가져오기 ajax 모듈
	 *
	 * 계약(일반운송) 1단계 금액, 계약(일반운송) 2단계 금액, 계약(택배) 금액
	 *
	 * 2015.08.17 mgkim 직접운송의무제 계약금액가져오기 공통사용
	 */
	@RequestMapping("/stat/selectMartRegContract.do")
	public void selectMartRegContract(@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			@RequestParam(value = "gubun", required = false) String gubun,
			HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, NullPointerException {
		FpisMviewContractVO shVO = new FpisMviewContractVO();
		shVO.setData_year(search_year);
		String s_usr_mst_key = usr_mst_key.replaceAll("-", "");
		shVO.setUsr_mst_key(s_usr_mst_key);
		shVO.setGubun(gubun);
		FpisMviewContractVO reg_contract = QuerySvc.selectMartRegContract(shVO);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(reg_contract.toString());
		out.close();
	}

	/*
	 * 2015.08.17 mgkim 오프라인마트 배차정보 가져오기 ajax 모듈
	 *
	 * 온라인마트 조회 기능과 같은구조로 함
	 *
	 * 배차(일반운송) 1단계 금액, 배차(일반운송) 2단계 금액, 배차(택배) 금액
	 * 배차(택배) 금액은 사용자 최종화면에서는 배차(일반운송) 1단계 금액과 합산하여 정보를 제공한다.
	 */
	@RequestMapping("/stat/selectMartRegOperateTrust.do")
	public void selectMartRegOperateTrust(@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, NullPointerException {
		FpisMviewOperateTrustVO shVO = new FpisMviewOperateTrustVO();
		shVO.setData_year(search_year);
		String s_usr_mst_key = usr_mst_key.replaceAll("-", "");
		shVO.setUsr_mst_key(s_usr_mst_key);

		FpisMviewOperateTrustVO reg_contract = QuerySvc.selectMartRegOperateTrust(shVO);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(reg_contract.toString());
		out.close();
	}

	/*
	 * 오프라인마트 최소운송 기준금액 가져오기
	 */
	@RequestMapping("/stat/selectMartCarMinBaseValue.do")
	public void selectMartCarMinBaseValue(@RequestParam(value = "search_year", required = false) String search_year,
			@RequestParam(value = "usr_mst_key", required = false) String usr_mst_key,
			@RequestParam(value = "gubun", required = false) String gubun,
			HttpServletResponse res,
			HttpServletRequest req,
			Model model) throws Exception, NullPointerException {
		FpisMviewCarMinVO shVO = new FpisMviewCarMinVO();
		shVO.setData_year(search_year);
		String s_usr_mst_key = usr_mst_key.replaceAll("-", "");
		shVO.setUsr_mst_key(s_usr_mst_key);
		FpisMviewCarMinVO reg_contract = QuerySvc.selectMartCarMinBaseValue(shVO);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		;
		PrintWriter out = res.getWriter();
		out.write(reg_contract.toString());
		out.close();
	}

	/* 2020.11.09 pch 최소운송기준금액 산정 */
	@RequestMapping("/stat/FpisStatMinStandardCharge.do")
	public String FpisStatMinStandardCharge(HttpServletRequest req, ModelMap model, @ModelAttribute FpisAdminStatReCountVO vo) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		int strYear = 2018;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);
		model.addAttribute("comp_cls_detail", svo.getComp_cls_detail());
		double sum = 0;

		vo.setUsr_mst_key(svo.getUsr_mst_key().replace("-", ""));

		if (AdminObeySystemService.selectSysCarMinDataForSearchYear(vo.getSearch_year()) == 0) {
			if (vo.getSearch_year() != null) {
				model.addAttribute("msg", "no_minData");
			}
		} else {
			int except_cnt = 0;
			vo.setSearch_cars_cls("02");

			List<FpisAdminStatReCountVO> carList = AdminObeySystemService.selectMinStandardCarCharge(vo);
			
			int year = Integer.parseInt(vo.getSearch_year());			
			int days = 365; //평년시 365일
			if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
				days = 366;	//윤년시 366일			
			}
			

			for (int i = 0; i < carList.size(); i++) {
				if (carList.get(i).getCar_min_charge() != null && sum + Double.parseDouble(carList.get(i).getCar_min_charge()) >= 0) {
										
					
					double car_year_days_per = Double.parseDouble(carList.get(i).getCar_year_days())/days;    // 일할 계산 값					
					double cydp = Math.floor(car_year_days_per*1000)/1000.0;           //소수 셋째자리까지 표시(넷째 자리 이후 버림)
					double car_min_charge = Double.parseDouble(carList.get(i).getCar_min_charge());
					String cmcp = String.format("%.0f",car_min_charge*cydp);   // 최소운송기준 금액 일할 계산
															
					carList.get(i).setCar_min_charge(cmcp);					
					
					sum += Double.parseDouble(carList.get(i).getCar_min_charge());
				}
				//제외차량cnt
				if ("05".equals(carList.get(i).getCars_kind()) || "11".equals(carList.get(i).getCars_kind()) || "13".equals(carList.get(i).getCars_kind())
						|| "14".equals(carList.get(i).getCars_kind()) || "15".equals(carList.get(i).getCars_kind()) || "16".equals(carList.get(i).getCars_kind())
						|| "17".equals(carList.get(i).getCars_kind()) || "18".equals(carList.get(i).getCars_kind()) || "19".equals(carList.get(i).getCars_kind())) {
					except_cnt++;
				}
			}

			model.addAttribute("list", carList);
			model.addAttribute("except_cnt", except_cnt);
			model.addAttribute("sum_charge", sum);
		}
		/*연간시장평균매출액 고시정보 조회*/
		FpisAdminSysCarMinDataVO sysCarMinDataVO = null;
		List<FpisAdminSysCarMinDataVO> sysCarMinDataRateList = AdminObeySystemService.selectSysCarMinDataRateList(sysCarMinDataVO);
		model.addAttribute("sysCarMinDataRateList", sysCarMinDataRateList);

		model.addAttribute("VO", vo);

		return "/fpis/stat/trans/FpisStatMinStandardCharge";
	}

	/* 2020.11.09 pch 최소운송기준금액 산정 엑셀다운로드 */
	@RequestMapping("/stat/FpisStatMinStandardChargeExportExcel.do")
	public void FpisStatMinStandardChargeExportExcel(HttpServletRequest req, HttpServletResponse res, ModelMap model, FpisAdminStatReCountVO vo) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		double sum = 0;
		vo.setUsr_mst_key(svo.getUsr_mst_key().replace("-", ""));

		String comp_cls_detail_nm = svo.getComp_cls_detail().replaceAll("01-01", "일반운송").replaceAll("01-02", "개별운송").replaceAll("01-03", "용달").replaceAll("01-04", "택배").replaceAll("02-01", "주선").replaceAll("02-02", "국제물류").replaceAll("04-01", "가맹").replaceAll("05", "인증망").replaceAll("06", "연합회/협회").replaceAll("07", "운영기관");

		int except_cnt = 0;
		vo.setSearch_cars_cls("02");

		List<FpisAdminStatReCountVO> carList = AdminObeySystemService.selectMinStandardCarCharge(vo);
		
		int year2 = Integer.parseInt(vo.getSearch_year());			
		int days = 365; //평년시 365일
		if ((year2 % 4 == 0 && year2 % 100 != 0) || year2 % 400 == 0) {
			days = 366;	//윤년시 366일			
		}

		for (int i = 0; i < carList.size(); i++) {
			if (carList.get(i).getCar_min_charge() != null && sum + Double.parseDouble(carList.get(i).getCar_min_charge()) >= 0) {
				
				double car_year_days_per = Double.parseDouble(carList.get(i).getCar_year_days())/days;    // 일할 계산 값					
				double cydp = Math.floor(car_year_days_per*1000)/1000.0;           //소수 셋째자리까지 표시(넷째 자리 이후 버림)
				double car_min_charge = Double.parseDouble(carList.get(i).getCar_min_charge());
				String cmcp = String.format("%.0f",car_min_charge*cydp);   // 최소운송기준 금액 일할 계산
														
				carList.get(i).setCar_min_charge(cmcp);					
				
				sum += Double.parseDouble(carList.get(i).getCar_min_charge());
			}
			//제외차량cnt
			if ("05".equals(carList.get(i).getCars_kind()) || "11".equals(carList.get(i).getCars_kind()) || "13".equals(carList.get(i).getCars_kind())
					|| "14".equals(carList.get(i).getCars_kind()) || "15".equals(carList.get(i).getCars_kind()) || "16".equals(carList.get(i).getCars_kind())
					|| "17".equals(carList.get(i).getCars_kind()) || "18".equals(carList.get(i).getCars_kind()) || "19".equals(carList.get(i).getCars_kind())) {
				except_cnt++;
			}
		}

		// 엑셀 작성 시작!!
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet worksheet = workbook.createSheet("WorkSheet");
		HSSFRow row = null;
		Cell cell1 = null;

		CellStyle cellStyle_top_header = workbook.createCellStyle(); // 탑헤더 스타일 생성
		cellStyle_top_header.setAlignment(CellStyle.ALIGN_CENTER); // 가운데정렬
		cellStyle_top_header.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//높이 가운데 정렬
		Font font_top_header = workbook.createFont(); //폰트생성
		font_top_header.setBoldweight(Font.BOLDWEIGHT_BOLD); //bold 지정
		font_top_header.setFontHeight((short) 320); //글자크기지정
		cellStyle_top_header.setFont(font_top_header); //셀스타일 적용

		CellStyle cellStyle_header = workbook.createCellStyle(); // 스타일 생성 - 헤더
		cellStyle_header.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle_header.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//높이 가운데 정렬
		Font table_header = workbook.createFont(); //폰트생성
		table_header.setBoldweight(Font.BOLDWEIGHT_BOLD); //bold지정
		cellStyle_header.setFont(table_header); //셀스타일 적용
		cellStyle_header.setBorderTop(CellStyle.BORDER_THIN); //border지정(top)
		cellStyle_header.setBorderBottom(CellStyle.BORDER_THIN);//border지정(bottom)
		cellStyle_header.setBorderLeft(CellStyle.BORDER_THIN); //border지정(left)
		cellStyle_header.setBorderRight(CellStyle.BORDER_THIN); //border지정(right)

		CellStyle cellStyle_header_left = workbook.createCellStyle(); // 스타일 생성 - 헤더
		cellStyle_header_left.setAlignment(CellStyle.ALIGN_LEFT); // left정렬
		cellStyle_header_left.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//높이 가운데 정렬
		cellStyle_header_left.setFont(table_header); //셀스타일 적용

		CellStyle cellStyle_td = workbook.createCellStyle(); // 일반데이터 스타일 생성
		cellStyle_td.setAlignment(CellStyle.ALIGN_CENTER); // 가운데정렬
		cellStyle_td.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//높이 가운데 정렬
		cellStyle_td.setBorderTop(CellStyle.BORDER_THIN); //border지정(top)
		cellStyle_td.setBorderBottom(CellStyle.BORDER_THIN);//border지정(bottom)
		cellStyle_td.setBorderLeft(CellStyle.BORDER_THIN); //border지정(left)
		cellStyle_td.setBorderRight(CellStyle.BORDER_THIN); //border지정(right)

		CellStyle cellStyle_td_font_red = workbook.createCellStyle(); // 일반데이터스타일 + 글자색 red
		cellStyle_td_font_red.setAlignment(CellStyle.ALIGN_CENTER);
		Font font_red = workbook.createFont(); //폰트생성
		font_red.setColor(IndexedColors.RED.getIndex()); //색상설정(red)
		cellStyle_td_font_red.setFont(font_red); //셀스타일에 적용
		cellStyle_td_font_red.setBorderTop(CellStyle.BORDER_THIN); //border지정(top)
		cellStyle_td_font_red.setBorderBottom(CellStyle.BORDER_THIN);//border지정(bottom)
		cellStyle_td_font_red.setBorderLeft(CellStyle.BORDER_THIN); //border지정(left)
		cellStyle_td_font_red.setBorderRight(CellStyle.BORDER_THIN); //border지정(right)

		row = worksheet.createRow(0);
		Util_poi.setCell(cell1, row, 0, cellStyle_top_header, "* 최소운송기준금액 산정 결과");
		/* 셀병합 */
		worksheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 6));

		row = worksheet.createRow(2);
		Util_poi.setCell(cell1, row, 0, cellStyle_header_left, "* 기본정보");
		/* 셀병합 */
		worksheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 6));

		row = worksheet.createRow(3);
		Util_poi.setCell(cell1, row, 0, cellStyle_header, "분석연도");
		Util_poi.setCell(cell1, row, 1, cellStyle_header, "");
		Util_poi.setCell(cell1, row, 2, cellStyle_td, vo.getSearch_year() + "년");
		Util_poi.setCell(cell1, row, 3, cellStyle_header, "사업자유형");
		Util_poi.setCell(cell1, row, 4, cellStyle_header, "");
		Util_poi.setCell(cell1, row, 5, cellStyle_td, comp_cls_detail_nm);
		Util_poi.setCell(cell1, row, 6, cellStyle_header, "");

		/* 셀병합 */
		worksheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 1));
		worksheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 4));
		worksheet.addMergedRegion(new CellRangeAddress(3, 3, 5, 6));

		row = worksheet.createRow(4);
		Util_poi.setCell(cell1, row, 0, cellStyle_header, "위수탁(지입) 차량대수");
		Util_poi.setCell(cell1, row, 1, cellStyle_header, "");
		Util_poi.setCell(cell1, row, 2, cellStyle_td, carList.size() + "대");
		Util_poi.setCell(cell1, row, 3, cellStyle_header, "제외차량 대수");
		Util_poi.setCell(cell1, row, 4, cellStyle_header, "");
		Util_poi.setCell(cell1, row, 5, cellStyle_td, except_cnt + "대");
		Util_poi.setCell(cell1, row, 6, cellStyle_header, "");
		/* 셀병합 */
		worksheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 1));
		worksheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 4));
		worksheet.addMergedRegion(new CellRangeAddress(4, 4, 5, 6));

		row = worksheet.createRow(5);
		worksheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 6));

		/* 최소운송기준금액 결과 헤더 */
		row = worksheet.createRow(6);
		Util_poi.setCell(cell1, row, 0, cellStyle_header_left, "* 최소운송기준금액 : " + String.format("%.0f",sum));

		row = worksheet.createRow(7);
		Util_poi.setCell(cell1, row, 0, cellStyle_header, "순번");
		Util_poi.setCell(cell1, row, 1, cellStyle_header, "차량번호");
		Util_poi.setCell(cell1, row, 2, cellStyle_header, "차량종류");
		Util_poi.setCell(cell1, row, 3, cellStyle_header, "크기(톤)");
		Util_poi.setCell(cell1, row, 4, cellStyle_header, "차량등록기간");
		Util_poi.setCell(cell1, row, 5, cellStyle_header, "등록일수");
		Util_poi.setCell(cell1, row, 6, cellStyle_header, "기준금액");

		/* 넓이지정 */
		worksheet.setColumnWidth(0, 12 * 256);
		worksheet.setColumnWidth(1, 24 * 256);
		worksheet.setColumnWidth(2, 30 * 256);
		worksheet.setColumnWidth(3, 12 * 256);
		worksheet.setColumnWidth(4, 30 * 256);
		worksheet.setColumnWidth(5, 12 * 256);
		worksheet.setColumnWidth(6, 20 * 256);

		/* 최소운송기준금액 결과 헤더 */
		for (int i = 0; i < carList.size(); i++) {
			if ("05".equals(carList.get(i).getCars_kind()) || "11".equals(carList.get(i).getCars_kind()) || "13".equals(carList.get(i).getCars_kind())
					|| "14".equals(carList.get(i).getCars_kind()) || "15".equals(carList.get(i).getCars_kind()) || "16".equals(carList.get(i).getCars_kind())
					|| "17".equals(carList.get(i).getCars_kind()) || "18".equals(carList.get(i).getCars_kind()) || "19".equals(carList.get(i).getCars_kind())) {
				row = worksheet.createRow(i + 8);
				Util_poi.setCell(cell1, row, 0, cellStyle_td_font_red, Integer.toString(i + 1));
				Util_poi.setCell(cell1, row, 1, cellStyle_td_font_red, carList.get(i).getCars_reg_num());
				Util_poi.setCell(cell1, row, 2, cellStyle_td_font_red, carList.get(i).getCars_kind_nm());
				Util_poi.setCell(cell1, row, 3, cellStyle_td_font_red, carList.get(i).getCars_size());
				Util_poi.setCell(cell1, row, 4, cellStyle_td_font_red, carList.get(i).getS_date().substring(0, 10) + "~" + carList.get(i).getE_date().substring(0, 10));
				Util_poi.setCell(cell1, row, 5, cellStyle_td_font_red, carList.get(i).getCar_year_days());
				Util_poi.setCell(cell1, row, 6, cellStyle_td_font_red, carList.get(i).getCar_min_charge());
			} else {
				row = worksheet.createRow(i + 8);
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "" + (i + 1) + "");
				Util_poi.setCell(cell1, row, 1, cellStyle_td, carList.get(i).getCars_reg_num());
				Util_poi.setCell(cell1, row, 2, cellStyle_td, carList.get(i).getCars_kind_nm());
				Util_poi.setCell(cell1, row, 3, cellStyle_td, carList.get(i).getCars_size());
				Util_poi.setCell(cell1, row, 4, cellStyle_td, carList.get(i).getS_date().substring(0, 10) + "~" + carList.get(i).getE_date().substring(0, 10));
				Util_poi.setCell(cell1, row, 5, cellStyle_td, carList.get(i).getCar_year_days());
				Util_poi.setCell(cell1, row, 6, cellStyle_td, carList.get(i).getCar_min_charge());
			}
		}

		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int date = cal.get(Calendar.DATE);

		res.setContentType("ms-vnd/excel");
		String fileName = "최소운송기준금액_산정_결과_" + year + "년" + month + "월" + date + "일" + ".xls";
		fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
		res.setHeader("Content-Disposition", "ATTachment; Filename=" + fileName);
		workbook.write(res.getOutputStream());
	}

	/*
	 * 2020.09.21 pch 제도준수 일괄조회_2019년 이후 제도준수결과
	 */
	@RequestMapping("/stat/FpisStatTransResult.do")
	public String FpisStatTransResult(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		String rCode = req.getParameter("rcode");

		/* 2021.09.23 jwchoi strYear=2019 > 2020 으로 수정*/
		/* 2021.09.28 jwchoi strYear=2020 > 2021 로 수정 1안 테스트반영으로 */
		String search_year = req.getParameter("search_year");
		int strYear = 2021;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
		/* 2021.09.23 jwchoi : search_year = "2020"주석처리 , 윗줄 주석해체 */
		if (search_year == null || search_year.equals("")) {
			search_year = "" + (Calendar.getInstance().get(Calendar.YEAR));
			//search_year = "2019";
		}

		FpisAdminStatTrans7VO search_params = new FpisAdminStatTrans7VO();
		search_params.setUsr_mst_key(svo.getUsr_mst_key());
		search_params.setBase_year(search_year);
		search_params.setSearch_comp_bsns_num(svo.getUsr_mst_key());
		search_params.setSearch_year(search_year);
		search_params.setQuarter("60");
		search_params.setDisposition_type("OMISSION");

		if ("Y".equals(req.getParameter("loading_chk"))) {
			FpisAdminStatTrans7VO disposition = FpisSvc.selectStatTransUsrBase_omi(search_params); //디테일 정보 가져오기
			if (disposition == null) {
				disposition = FpisSvc.selectStatTransUsrBase_dir(search_params); //디테일 정보 가져오기
			}
			FpisAdminStatTrans7VO statTrans = AdminObeySystemService.selectAllStatTrans(search_params);

			model.addAttribute("userInfo", disposition);
			model.addAttribute("statTrans", statTrans);
		}

		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);
		model.addAttribute("search_year", search_year);
		model.addAttribute("rcode", rCode);

		return "/fpis/stat/trans/FpisStatTransResult";
	}
	
	/*
	 * 2023.09.25 chbaek 3월 신고마감 기준 허위의심 포함, 수탁누락 포함
	 */
	@RequestMapping("/stat/FpisStatTransResult_New.do")
	public String FpisStatTransResult_New(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		String rCode = req.getParameter("rcode");

		/* 2021.09.23 jwchoi strYear=2019 > 2020 으로 수정*/
		/* 2021.09.28 jwchoi strYear=2020 > 2021 로 수정 1안 테스트반영으로 */
		String search_year = req.getParameter("search_year");
		int strYear = 2023;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
		strYear = 2023; endYear = 2023; //20231020 chbaek 현재 2022년 신고마감 데이터만 들어가있음 2024년에 3월 신고마감 넣을 때 이줄 지워야됨.
		/* 2021.09.23 jwchoi : search_year = "2020"주석처리 , 윗줄 주석해체 */
		if (search_year == null || search_year.equals("")) {
			search_year = "" + (Calendar.getInstance().get(Calendar.YEAR));
			//search_year = "2019";
		}

		FpisAdminStatTrans7VO search_params = new FpisAdminStatTrans7VO();
		search_params.setUsr_mst_key(svo.getUsr_mst_key());
		search_params.setBase_year(search_year);
		search_params.setSearch_comp_bsns_num(svo.getUsr_mst_key());
		search_params.setSearch_year(search_year);
		search_params.setQuarter("30");
		search_params.setDisposition_type("OMISSION");

		if ("Y".equals(req.getParameter("loading_chk"))) {
			FpisAdminStatTrans7VO disposition = FpisSvc.selectStatTransUsrBase_omi_30(search_params); //디테일 정보 가져오기
			if (disposition == null) {
				disposition = FpisSvc.selectStatTransUsrBase_dir(search_params); //디테일 정보 가져오기
			}
			FpisAdminStatTrans7VO statTrans = AdminObeySystemService.selectAllStatTrans_30(search_params);

			model.addAttribute("userInfo", disposition);
			model.addAttribute("statTrans", statTrans);
		}

		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);
		model.addAttribute("search_year", search_year);
		model.addAttribute("rcode", rCode);

		return "/fpis/stat/trans/FpisStatTransResult_New";
	}
	
	
	

	/*
	 * 2019.10.01 pch 제도준수 일괄조회[신규]
	 * 2020.09.21 pch 2019년 실적신고제 검증강화로 메뉴분리
	 * 2021.01.11 ysw 보안취약점 METHOD POST 만 가능하도록 수정
	 */
	@RequestMapping(value = "/stat/FpisStatTransResult_OLD.do", method = RequestMethod.POST)
	public String FpisStatTransResult_OLD(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {

		/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if (!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		String search_year = req.getParameter("search_year");
		//int endYear = Calendar.getInstance().get(Calendar.YEAR);
		//int strYear = 2017;
		//int endYear = 2020;
		int strYear = FpisSvc.selectstrYear();
		int endYear = FpisSvc.selectendYear();
		String searchYear = FpisSvc.selectsearchYear();

		if (search_year == null || search_year.equals("")) {
			//search_year = ""+(Calendar.getInstance().get(Calendar.YEAR));
			//search_year = "2020";
			search_year = searchYear;
			
		}

		FpisAdminStatTrans7VO search_params = new FpisAdminStatTrans7VO();
		search_params.setUsr_mst_key(svo.getUsr_mst_key());
		search_params.setBase_year(search_year);
		search_params.setSearch_comp_bsns_num(svo.getUsr_mst_key());
		search_params.setSearch_year(search_year);
		search_params.setQuarter("60");
		search_params.setDisposition_type("OMISSION");

		if ("Y".equals(req.getParameter("loading_chk"))) {
			FpisAdminStatTrans7VO disposition = FpisSvc.selectStatTransUsrBase_omi(search_params); //디테일 정보 가져오기
			if (disposition == null) {
				disposition = FpisSvc.selectStatTransUsrBase_dir(search_params); //디테일 정보 가져오기
			}
			FpisAdminStatTrans7VO statTrans = AdminObeySystemService.selectAllStatTrans(search_params);

			model.addAttribute("userInfo", disposition);
			model.addAttribute("statTrans", statTrans);
		}

		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);
		model.addAttribute("search_year", search_year);
		model.addAttribute("rcode", req.getParameter("rcode"));
		model.addAttribute("bcode", req.getParameter("bcode"));

		return "/fpis/stat/trans/FpisStatTransResult_OLD";
	}

	/*
	 * 2019.10.01 pch 제도준수_실적신고의무제 상세조회[신규]
	 */

	@RequestMapping("/stat/FpisStatTransDetail_omission.do")
	public ModelAndView FpisStatTransDetail_omission(HttpServletRequest req) throws Exception, NullPointerException {

		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		FpisAdminStatTrans7VO search_params = new FpisAdminStatTrans7VO();
		String masked_info_status = req.getParameter("masked_info_status");
		search_params.setMasked_addr(masked_info_status); //2021.11.03 jwchoi 정보노출
		search_params.setMasked_tel(masked_info_status); //2022.11.01 jwchoi 김선희 팀장님 요청. 수탁자 유선 전화번호 추가.
		search_params.setUsr_mst_key(req.getParameter("usr_mst_key"));
		search_params.setBase_year(req.getParameter("base_year"));
		search_params.setSearch_comp_bsns_num(req.getParameter("usr_mst_key"));
		search_params.setSearch_year(req.getParameter("base_year"));
		search_params.setQuarter("60");
		search_params.setDisposition_type("OMISSION");

		List<FpisAdminStatTrans7VO> noPerformList = FpisSvc.selectNoPerformList(search_params); //2018.08.28 PES 미이행율 추가
		List<FpisAdminStatTrans7VO> omissionSutakList = new ArrayList<FpisAdminStatTrans7VO>();

		ModelAndView mav = new ModelAndView();
		/* 2021.09.28 jwchoi 2020>2021 으로 수정, 테스트서버 반영 때문   */
		/* 2022.10.18 jwchoi 허위의심 반영 취소되어 주석처리   */
//		if (Integer.parseInt(search_params.getBase_year()) >= 2021) {
//			FpisAdminStatTrans7VO fallacyResult = FpisSvc.selectFallacyResult(search_params);
//			List<FpisAdminStatTrans7VO> omissionDivisionList_WE = FpisSvc.selectOmissionDetailList_WE(search_params);
//			List<FpisAdminStatTrans7VO> omissionDivisionList_SU = FpisSvc.selectOmissionDetailList_SU(search_params);
//			List<FpisAdminStatTrans7VO> fallacyDivisionList_WE = FpisSvc.selectFallacyDetailList_WE(search_params);
//			List<FpisAdminStatTrans7VO> fallacyDivisionList_SU = FpisSvc.selectFallacyDetailList_SU(search_params);
//
//			mav.addObject("fallacyResult", fallacyResult);
//			mav.addObject("omissionDivisionList_WE", omissionDivisionList_WE);
//			mav.addObject("omissionDivisionList_SU", omissionDivisionList_SU);
//			mav.addObject("fallacyDivisionList_WE", fallacyDivisionList_WE);
//			mav.addObject("fallacyDivisionList_SU", fallacyDivisionList_SU);
//		} 
//		else {
			omissionSutakList = FpisSvc.selectOmissionSutakList(search_params);

			//정보노출이 off일시
			/*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
			if ("Y".equals(masked_info_status) && omissionSutakList.size() > 0) {
				List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
				/*이력 삽입*/
				for (int i = 0; i < omissionSutakList.size(); i++) {
					FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
					accessLogVO.setMasked_addr(masked_info_status); //2021.11.08 jwchoi 정보노출
					accessLogVO.setMasked_tel(masked_info_status); //2022.11.01 jwchoi 김선희 팀장님 요청. 수탁자 유선 전화번호 추가.
					accessLogVO.setRcode(req.getParameter("rcode"));
					accessLogVO.setBcode(req.getParameter("bcode"));
					System.out.println("=================="+omissionSutakList.get(i).getUsr_mst_key());
					accessLogVO.setComp_mst_key(omissionSutakList.get(i).getUsr_mst_key().replaceAll("-", ""));
					accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
					accessLogVO.setJob_cls("DE"); //상세정보보기
					accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
					accessLogVOList.add(accessLogVO);
				}
				accessLogService.insertAccessLogByList(accessLogVOList);
			}

			mav.addObject("omissionSutakList", omissionSutakList);
//		}

		mav.addObject("noPerformList", noPerformList);
		mav.setViewName("jsonView");

		return mav;
	}
	
	
	@RequestMapping("/stat/FpisStatTransDetail_omission_New.do")
	public ModelAndView FpisStatTransDetail_omission_New(HttpServletRequest req) throws Exception, NullPointerException {
		
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		FpisAdminStatTrans7VO search_params = new FpisAdminStatTrans7VO();
		String masked_info_status = req.getParameter("masked_info_status");
		search_params.setMasked_addr(masked_info_status); //2021.11.03 jwchoi 정보노출
		search_params.setMasked_tel(masked_info_status); //2022.11.01 jwchoi 김선희 팀장님 요청. 수탁자 유선 전화번호 추가.
		search_params.setUsr_mst_key(req.getParameter("usr_mst_key"));
		search_params.setBase_year(req.getParameter("base_year"));
		search_params.setSearch_comp_bsns_num(req.getParameter("usr_mst_key"));
		search_params.setSearch_year(req.getParameter("base_year"));
		search_params.setQuarter("30");
		search_params.setDisposition_type("OMISSION");

		List<FpisAdminStatTrans7VO> noPerformList = FpisSvc.selectNoPerformList(search_params); //2018.08.28 PES 미이행율 추가
		List<FpisAdminStatTrans7VO> omissionSutakList = new ArrayList<FpisAdminStatTrans7VO>();		

		ModelAndView mav = new ModelAndView();
		/* 2021.09.28 jwchoi 2020>2021 으로 수정, 테스트서버 반영 때문   */
		/* 2022.10.18 jwchoi 허위의심 반영 취소되어 주석처리   */
		if (Integer.parseInt(search_params.getBase_year()) >= 2021) {			
			FpisAdminStatTrans7VO fallacyResult = FpisSvc.selectFallacyResult(search_params);
			List<FpisAdminStatTrans7VO> omissionDivisionList_WE = FpisSvc.selectOmissionDetailList_WE_30(search_params);
			List<FpisAdminStatTrans7VO> omissionDivisionList_SU = FpisSvc.selectOmissionDetailList_SU_30(search_params);
			List<FpisAdminStatTrans7VO> fallacyDivisionList_WE = FpisSvc.selectFallacyDetailList_WE_30(search_params);
			List<FpisAdminStatTrans7VO> fallacyDivisionList_SU = FpisSvc.selectFallacyDetailList_SU_30(search_params);

			mav.addObject("fallacyResult", fallacyResult);
			mav.addObject("omissionDivisionList_WE", omissionDivisionList_WE);
			mav.addObject("omissionDivisionList_SU", omissionDivisionList_SU);
			mav.addObject("fallacyDivisionList_WE", fallacyDivisionList_WE);
			mav.addObject("fallacyDivisionList_SU", fallacyDivisionList_SU);
		} 
		else {
			omissionSutakList = FpisSvc.selectOmissionSutakList(search_params);

			//정보노출이 off일시
			/*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
			if ("Y".equals(masked_info_status) && omissionSutakList.size() > 0) {
				List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
				/*이력 삽입*/
				for (int i = 0; i < omissionSutakList.size(); i++) {
					FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
					accessLogVO.setMasked_addr(masked_info_status); //2021.11.08 jwchoi 정보노출
					accessLogVO.setMasked_tel(masked_info_status); //2022.11.01 jwchoi 김선희 팀장님 요청. 수탁자 유선 전화번호 추가.
					accessLogVO.setRcode(req.getParameter("rcode"));
					accessLogVO.setBcode(req.getParameter("bcode"));
					System.out.println("=================="+omissionSutakList.get(i).getUsr_mst_key());
					accessLogVO.setComp_mst_key(omissionSutakList.get(i).getUsr_mst_key().replaceAll("-", ""));
					accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
					accessLogVO.setJob_cls("DE"); //상세정보보기
					accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
					accessLogVOList.add(accessLogVO);
				}
				accessLogService.insertAccessLogByList(accessLogVOList);
			}

			mav.addObject("omissionSutakList", omissionSutakList);
		}

		mav.addObject("noPerformList", noPerformList);
		mav.setViewName("jsonView");

		return mav;
	}

	/*
	 * 2019.10.01 pch 제도준수_직접운송의무제 상세조회[신규]
	 */
	@RequestMapping("/stat/FpisStatTransDetail_direct.do")
	public ModelAndView FpisStatTransDetail_direct(HttpServletRequest req,
			FpisAdminStatTrans4VO shVO) throws Exception, NullPointerException {

		FpisAdminStatTrans7VO search_params = new FpisAdminStatTrans7VO();
		search_params.setSearch_comp_bsns_num(req.getParameter("usr_mst_key"));
		search_params.setSearch_year(req.getParameter("base_year"));
		search_params.setS_row(0);
		search_params.setE_row(21);

		List<FpisAdminStatTrans4VO> compList = FpisSvc.selectUsrInfoMartDirectList_renewal(search_params);

		ModelAndView mav = new ModelAndView();

		mav.addObject("compList", compList);
		mav.addObject("VO", shVO);
		mav.setViewName("jsonView");

		return mav;
	}

	/*
	 * 2019.10.01 pch 제도준수_최소운송기준제 상세조회[신규]
	 */
	@RequestMapping("/stat/FpisStatTransDetail_min.do")
	public ModelAndView FpisStatTransDetail_minimum(HttpServletRequest req,
			ModelMap model) throws Exception, NullPointerException {
		//SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

		FpisAdminStatTrans7VO search_params = new FpisAdminStatTrans7VO();
		search_params.setSearch_comp_bsns_num(req.getParameter("usr_mst_key"));
		search_params.setSearch_year(req.getParameter("base_year"));
		search_params.setS_row(0);
		search_params.setE_row(21);
		List<FpisAdminStatTrans4VO> compList = FpisSvc.selectUsrInfoMartCarminList_renewal(search_params);
		search_params.setUsr_mst_key(req.getParameter("usr_mst_key"));

		ModelAndView mav = new ModelAndView();

		mav.addObject("compList", compList);
		mav.setViewName("jsonView");

		return mav;
	}

	@RequestMapping("/stat/FpisStatTransDetail_excel_detail_omiAndFal.do")
	public void FpisStatTransDetail_excel_detail_omiAndFal(HttpServletRequest req, HttpServletResponse res, Model model, FpisAdminStatTrans7VO shVO) throws Exception, NullPointerException {

		shVO.setBase_year(shVO.getSearch_year());
		shVO.setQuarter("30"); //20231016 chbaek 기존에 60으로 있던것 30으로 바꿈, 신고마감 기준 1안 출력
		shVO.setMasked_addr(req.getParameter("masked_info_status"));
		shVO.setMasked_tel(req.getParameter("masked_info_status"));

		List<FpisAdminStatTrans7VO> omissionList = FpisSvc.selectOmiList_excel(shVO);
		List<FpisAdminStatTrans7VO> fallacyList = FpisSvc.selectFalList_excel(shVO);

		String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
		if (org_comp_bsns_num != null) {
			shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", "")); // 2013.10.18 사업자번호 검색 "-" 기호 제거
		}
		shVO.setUsr_mst_key(org_comp_bsns_num);

		FileInputStream inputStream = null;
		FileOutputStream out = null;
		PrintWriter pout = null;

		//1.템플릿 파일 복사
		String makingDtm = Util.getDateFormat2();
		String excelFileName = makingDtm + "_" + shVO.getUsr_mst_key() + "_" + shVO.getSearch_year() + ".xlsx";
		//String file_path = EgovProperties.getProperty("Globals.majarStatFilePath") + File.separator;
		majarStatFilePath += File.separator;
		String excelFileSize = "";

		excelFileSize = "_2019_omifal";

		moveToUploadDirectory(excelFileName, excelFileSize);

		//2.복사한 파일 메모리 로드
		String excelFile = majarStatFilePath + excelFileName;
		try {
			inputStream = new FileInputStream(new File(excelFile));

			XSSFWorkbook workbook = new XSSFWorkbook(inputStream); //20231016 chbaek HSSF -> XSSF

			/* 스타일 작업 */
			CellStyle cellStyle = workbook.createCellStyle(); // 스타일 생성 - 일반셀
			cellStyle.setAlignment(CellStyle.ALIGN_LEFT); //스타일 - 왼쪽정렬
			cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
			cellStyle.setBorderTop(CellStyle.BORDER_THIN);
			cellStyle.setBorderRight(CellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
			cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
			cellStyle.setWrapText(true);		

			CellStyle cellStyle_center = workbook.createCellStyle(); // 스타일 생성 - 일반셀			
			cellStyle_center.setAlignment(CellStyle.ALIGN_CENTER); //스타일 - 가운데정렬
			cellStyle_center.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
			cellStyle_center.setBorderTop(CellStyle.BORDER_THIN);
			cellStyle_center.setBorderRight(CellStyle.BORDER_THIN);
			cellStyle_center.setBorderLeft(CellStyle.BORDER_THIN);
			cellStyle_center.setBorderBottom(CellStyle.BORDER_THIN);
			cellStyle_center.setWrapText(true);

			CellStyle cellStyle_right = workbook.createCellStyle(); // 스타일 생성 - 일반셀
			cellStyle_right.setAlignment(CellStyle.ALIGN_RIGHT); //스타일 - 오른쪽정렬
			cellStyle_right.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
			cellStyle_right.setBorderTop(CellStyle.BORDER_THIN);
			cellStyle_right.setBorderRight(CellStyle.BORDER_THIN);
			cellStyle_right.setBorderLeft(CellStyle.BORDER_THIN);
			cellStyle_right.setBorderBottom(CellStyle.BORDER_THIN);
			cellStyle_right.setWrapText(true);

			CellStyle cellStyle_number = workbook.createCellStyle(); // 스타일 생성 - 숫자
			DataFormat format = workbook.createDataFormat();
			cellStyle_number.setAlignment(CellStyle.ALIGN_RIGHT); //스타일 - 가운데정렬
			cellStyle_number.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
			cellStyle_number.setDataFormat(format.getFormat("_-* #,##0_-;-* #,##0_-;_-* \"-\"_-;_-@_-"));
			cellStyle_number.setBorderTop(CellStyle.BORDER_THIN);
			cellStyle_number.setBorderRight(CellStyle.BORDER_THIN);
			cellStyle_number.setBorderLeft(CellStyle.BORDER_THIN);
			cellStyle_number.setBorderBottom(CellStyle.BORDER_THIN);
			/* 스타일 작업 끝  */

			//3.데이터 채우기
			//누락의심 시트 채우기
			Sheet omi_Sheet = workbook.getSheetAt(1); //누락의심 시트 가져오기			
			//System.out.println("ddddd omissionList.get(0).getNo_perform() = "+ omissionList.get(0).getNo_perform());			
			if (omissionList == null || omissionList.size() == 0 || omissionList.get(0).getNo_perform() == null) {
				omi_Sheet.createRow(7).createCell(0).setCellValue("누락의심내역이 없습니다.");
			} else {
				int row_num = 7;
				for (int i = 0; i < omissionList.size(); i++) {
					Row row = null;

					if (omi_Sheet.getRow(row_num) == null) {
						row = omi_Sheet.createRow(row_num);
					} else {
						row = omi_Sheet.getRow(row_num);
					}

					/* 데이터 입력 */
					Util_poi.setCell(null, row, 0, cellStyle_center, omissionList.get(i).getSigungu_nm()); //시군구
					Util_poi.setCell(null, row, 1, cellStyle, omissionList.get(i).getComp_nm()); //업체명
					Util_poi.setCell(null, row, 2, cellStyle, omissionList.get(i).getAddr1());//주소
					Util_poi.setCell(null, row, 3, cellStyle_center, omissionList.get(i).getUsr_mst_key()); //사업자번호
					Util_poi.setCell(null, row, 4, cellStyle_center, omissionList.get(i).getComp_corp_num()); //법인번호
					Util_poi.setCell(null, row, 5, cellStyle, omissionList.get(i).getComp_cls()); //업태
					Util_poi.setNumberCell3(null, row, 6, cellStyle_number, omissionList.get(i).getCar_cnt()); //보유차량수
					Util_poi.setNumberCell3(null, row, 7, cellStyle_number, omissionList.get(i).getOk_charge()); //계약금액
					Util_poi.setCell(null, row, 8, cellStyle_right, omissionList.get(i).getNo_perform()); //미이행률
					Util_poi.setCell(null, row, 9, cellStyle_center, omissionList.get(i).getIs_reg()); //신고여부
					if ("WE".equals(omissionList.get(i).getDivision())) { //누락의심_종류
						Util_poi.setCell(null, row, 10, cellStyle_center, "위탁");
					} else if ("SU".equals(omissionList.get(i).getDivision())) {
						Util_poi.setCell(null, row, 10, cellStyle_center, "수탁");
					}
					Util_poi.setCell(null, row, 11, cellStyle_center, omissionList.get(i).getIs_target_reg()); //누락의심_신고대상자신고여부
					Util_poi.setNumberCell3(null, row, 12, cellStyle_number, omissionList.get(i).getTarget_charge()); //누락의심_신고대상자신고금액
					Util_poi.setCell(null, row, 13, cellStyle_right, omissionList.get(i).getReg_nurak_rate()); //누락의심_누락신고비율
					Util_poi.setCell(null, row, 14, cellStyle, omissionList.get(i).getContractor_nm()); //계약자상호
					Util_poi.setCell(null, row, 15, cellStyle, omissionList.get(i).getContractor_addr()); //계약자 주소
					Util_poi.setCell(null, row, 16, cellStyle_center, omissionList.get(i).getContractor_tel()); //계약자 연락처
					Util_poi.setCell(null, row, 17, cellStyle_center, omissionList.get(i).getContractor_num()); //계약자 사업자번호
					Util_poi.setCell(null, row, 18, cellStyle, omissionList.get(i).getContractor_cls()); //계약자 업태
					Util_poi.setNumberCell3(null, row, 19, cellStyle_number, omissionList.get(i).getContractor_cars_cnt()); //계약자보유차량수
					Util_poi.setNumberCell3(null, row, 20, cellStyle_number, omissionList.get(i).getContractor_sum_charge()); //계약금액합계
					Util_poi.setNumberCell3(null, row, 21, cellStyle_number, omissionList.get(i).getContractor_charge1()); //계약금액1월
					Util_poi.setNumberCell3(null, row, 22, cellStyle_number, omissionList.get(i).getContractor_charge2()); //계약금액2월
					Util_poi.setNumberCell3(null, row, 23, cellStyle_number, omissionList.get(i).getContractor_charge3()); //계약금액3월
					Util_poi.setNumberCell3(null, row, 24, cellStyle_number, omissionList.get(i).getContractor_charge4()); //계약금액4월
					Util_poi.setNumberCell3(null, row, 25, cellStyle_number, omissionList.get(i).getContractor_charge5()); //계약금액5월
					Util_poi.setNumberCell3(null, row, 26, cellStyle_number, omissionList.get(i).getContractor_charge6()); //계약금액6월
					Util_poi.setNumberCell3(null, row, 27, cellStyle_number, omissionList.get(i).getContractor_charge7()); //계약금액7월
					Util_poi.setNumberCell3(null, row, 28, cellStyle_number, omissionList.get(i).getContractor_charge8()); //계약금액8월
					Util_poi.setNumberCell3(null, row, 29, cellStyle_number, omissionList.get(i).getContractor_charge9()); //계약금액9월
					Util_poi.setNumberCell3(null, row, 30, cellStyle_number, omissionList.get(i).getContractor_charge1()); //계약금액10월
					Util_poi.setNumberCell3(null, row, 31, cellStyle_number, omissionList.get(i).getContractor_charge11()); //계약금액11월
					Util_poi.setNumberCell3(null, row, 32, cellStyle_number, omissionList.get(i).getContractor_charge12()); //계약금액12월
					row_num++;

					row.setHeight((short) 400);
				}
			}

			//허위의심 시트 채우기
			Sheet fal_Sheet = workbook.getSheetAt(2); //허위의심 시트 가져오기
			if (fallacyList == null || fallacyList.size() == 0) {
				fal_Sheet.createRow(7).createCell(0).setCellValue("허위의심내역이 없습니다.");
			} else {
				int row_num = 7;
				for (int i = 0; i < fallacyList.size(); i++) {
					Row row = null;
					if (fal_Sheet.getRow(row_num) == null) {
						row = fal_Sheet.createRow(row_num);
					} else {
						row = fal_Sheet.getRow(row_num);
					}

					/* 데이터 입력 */
					Util_poi.setCell(null, row, 0, cellStyle_center, fallacyList.get(i).getSigungu_nm()); //시군구
					Util_poi.setCell(null, row, 1, cellStyle, fallacyList.get(i).getComp_nm()); //업체명
					Util_poi.setCell(null, row, 2, cellStyle, fallacyList.get(i).getAddr1()); //주소
					Util_poi.setCell(null, row, 3, cellStyle_center, fallacyList.get(i).getUsr_mst_key()); //사업자번호
					Util_poi.setCell(null, row, 4, cellStyle_center, fallacyList.get(i).getComp_corp_num()); //법인번호
					Util_poi.setCell(null, row, 5, cellStyle, fallacyList.get(i).getComp_cls()); //업태
					Util_poi.setNumberCell3(null, row, 6, cellStyle_number, fallacyList.get(i).getCar_cnt()); //보유차량수
					Util_poi.setNumberCell3(null, row, 7, cellStyle_number, fallacyList.get(i).getOk_charge()); //계약금액
					Util_poi.setCell(null, row, 8, cellStyle_center, fallacyList.get(i).getIs_reg()); //신고여부
					if ("WE".equals(fallacyList.get(i).getDivision())) { //허위의심_종류
						Util_poi.setCell(null, row, 9, cellStyle_center, "위탁");
					} else if ("SU".equals(fallacyList.get(i).getDivision())) {
						Util_poi.setCell(null, row, 9, cellStyle_center, "수탁");
					}
					Util_poi.setNumberCell3(null, row, 10, cellStyle_number, fallacyList.get(i).getTarget_charge()); //허위의심_신고대상자신고금액
					Util_poi.setCell(null, row, 11, cellStyle_right, fallacyList.get(i).getReg_untruth_rate()); //허위의심_허위신고비율
					Util_poi.setCell(null, row, 12, cellStyle, fallacyList.get(i).getContractor_nm()); //계약자상호
					Util_poi.setCell(null, row, 13, cellStyle, fallacyList.get(i).getContractor_addr()); //계약자 주소
					Util_poi.setCell(null, row, 14, cellStyle_center, fallacyList.get(i).getContractor_tel()); //계약자 연락처
					Util_poi.setCell(null, row, 15, cellStyle_center, fallacyList.get(i).getContractor_num()); //계약자 사업자번호
					Util_poi.setCell(null, row, 16, cellStyle, fallacyList.get(i).getContractor_cls()); //계약자 업태
					Util_poi.setNumberCell3(null, row, 17, cellStyle_number, fallacyList.get(i).getContractor_cars_cnt()); //계약자보유차량수
					Util_poi.setCell(null, row, 18, cellStyle_center, fallacyList.get(i).getIs_target_reg()); //계약자 신고여부
					Util_poi.setNumberCell3(null, row, 19, cellStyle_number, fallacyList.get(i).getContractor_sum_charge()); //계약금액합계
					Util_poi.setNumberCell3(null, row, 20, cellStyle_number, fallacyList.get(i).getContractor_charge1()); //계약금액1월
					Util_poi.setNumberCell3(null, row, 21, cellStyle_number, fallacyList.get(i).getContractor_charge2()); //계약금액2월
					Util_poi.setNumberCell3(null, row, 22, cellStyle_number, fallacyList.get(i).getContractor_charge3()); //계약금액3월
					Util_poi.setNumberCell3(null, row, 23, cellStyle_number, fallacyList.get(i).getContractor_charge4()); //계약금액4월
					Util_poi.setNumberCell3(null, row, 24, cellStyle_number, fallacyList.get(i).getContractor_charge5()); //계약금액5월
					Util_poi.setNumberCell3(null, row, 25, cellStyle_number, fallacyList.get(i).getContractor_charge6()); //계약금액6월
					Util_poi.setNumberCell3(null, row, 26, cellStyle_number, fallacyList.get(i).getContractor_charge7()); //계약금액7월
					Util_poi.setNumberCell3(null, row, 27, cellStyle_number, fallacyList.get(i).getContractor_charge8()); //계약금액8월
					Util_poi.setNumberCell3(null, row, 28, cellStyle_number, fallacyList.get(i).getContractor_charge9()); //계약금액9월
					Util_poi.setNumberCell3(null, row, 29, cellStyle_number, fallacyList.get(i).getContractor_charge10()); //계약금액10월
					Util_poi.setNumberCell3(null, row, 30, cellStyle_number, fallacyList.get(i).getContractor_charge11()); //계약금액11월
					Util_poi.setNumberCell3(null, row, 31, cellStyle_number, fallacyList.get(i).getContractor_charge12()); //계약금액12월
					row_num++;
				}
			}

			out = new FileOutputStream(new File(excelFile));
			workbook.write(out);

			JSONObject json = new JSONObject();

			json.put("fileCls", "99");
			json.put("file_path", majarStatFilePath);
			json.put("file_name", excelFileName);
			json.put("fileName", excelFileName);

			pout = res.getWriter();

			pout.write(json.toString());

		} catch (FileNotFoundException e) {
			logger.error("[ERROR] - FileNotFoundException : ", e);
		} catch (IOException e) {
			logger.error("[ERROR] - IOException : ", e);
		} catch (OutOfMemoryError e) {
			logger.error("[ERROR] - OutOfMemoryError : ", e);
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			if (pout != null)
				pout.close();
		}

	}

	@RequestMapping("/stat/FpisStatTransDetail_excel_detail_omi.do")
	public void FpisStatTransDetail_excel_detail_omi(HttpServletRequest req, HttpServletResponse res, Model model, FpisAdminStatTrans7VO shVO) throws Exception, NullPointerException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");
		List<FpisAdminStatTrans7VO> omissionList = null;
		List<FpisAdminStatTrans7VO> omissionTotList = null;
		List<FpisAdminStatBase12VO> voList = null;
		List<FpisAdminStatBase12VO> voOmissionList = null;
		String sTableName = "";
		FpisAdminStatBase12VO VO = new FpisAdminStatBase12VO();

		String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
		if (org_comp_bsns_num != null) {
			shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", "")); // 2013.10.18 사업자번호 검색 "-" 기호 제거
		}
		shVO.setUsr_mst_key(org_comp_bsns_num);

		//180209 오승민 연 1회신고로 변경 후 2017년 이후 연도검색 시 분기 조건 추가
		//191028 박창희 실적신고의무제 수정마감기준 조회.(60)
		shVO.setSearch_bungi("60");
		//2021.11.08 jwchoi 정보노출 onoff
		shVO.setMasked_addr(req.getParameter("masked_info_status"));
		shVO.setMasked_tel(req.getParameter("masked_info_status"));

		VO.setSearch_year(shVO.getSearch_year());
		VO.setSearch_bungi(shVO.getSearch_bungi());
		VO.setSearch_type("R");
		VO.setSearch_sort1("OMISSION");
		VO.setSearch_sort2("ASC");

		//신고자조회
		sTableName = adminStatSvc.getSearchTableName(VO);
		VO.setSearch_table_name(sTableName);

		//누락의심 조회
		omissionList = FpisSvc.selectOmissionList_excel_detail(shVO);

		//정보노출이 off일시
		/*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
		String masked_info_status = req.getParameter("masked_info_status");
		if ("Y".equals(masked_info_status)) {
			List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
			/*이력 삽입*/
			for (int i = 0; i < omissionList.size(); i++) {
				FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
				accessLogVO.setRcode(req.getParameter("rcode"));
				accessLogVO.setBcode(req.getParameter("bcode"));
				accessLogVO.setJob_memo(req.getParameter("job_memo"));
				accessLogVO.setComp_mst_key(omissionList.get(i).getUsr_mst_key().replaceAll("-", ""));
				accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
				accessLogVO.setJob_cls("EX"); //엑셀다운로드
				accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
				accessLogVOList.add(accessLogVO);
			}
			/*이력 삽입_수탁자*/
			for (int i = 0; i < omissionList.size(); i++) {
				if (omissionList.get(i).getSutak_num() != null) {
					FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
					accessLogVO.setRcode(req.getParameter("rcode"));
					accessLogVO.setBcode(req.getParameter("bcode"));
					accessLogVO.setComp_mst_key(omissionList.get(i).getSutak_num().replaceAll("-", ""));
					accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
					accessLogVO.setJob_cls("EX");
					accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
					accessLogVOList.add(accessLogVO);
				}
			}
			accessLogService.insertAccessLogByList(accessLogVOList);
		}

		shVO.setSearch_comp_bsns_num(org_comp_bsns_num); // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

		parseExcel_FpisAdminStatTrans9(req, res, shVO, omissionTotList, voList, omissionList, voOmissionList);
	}

	public void parseExcel_FpisAdminStatTrans9(HttpServletRequest req, HttpServletResponse res,
			FpisAdminStatTrans7VO VO,
			List<FpisAdminStatTrans7VO> resultList_sheet2, // 시군구 총괄
			List<FpisAdminStatBase12VO> resultList_sheet3, // 신고현황
			List<FpisAdminStatTrans7VO> resultList_sheet4, // 미신고 의심내역
			List<FpisAdminStatBase12VO> resultList_sheet5 // 신고금액 검증
	) {

		String masked_info_status = req.getParameter("masked_info_status");
		SessionVO SessionVO = (SessionVO) req.getSession().getAttribute("SessionVO");
		//String file_path = EgovProperties.getProperty("globals.fileStorePath");
		File folder = new File(fileStorePath);//지정된 경로에 폴더를 만든다.
		folder.setReadable(true);
		folder.setWritable(true);
		if (!folder.exists()) {
			folder.mkdirs();//폴더가 존재 한다면 무시한다.
		}
		/* Create a Workbook and Worksheet */
		XSSFWorkbook workbook = new XSSFWorkbook();

		/* =======================================================================  공통 작업 시작 */
		String search_year = VO.getSearch_year();
		String search_bungi = VO.getSearch_bungi();
		String month1 = "1월";
		String month2 = "2월";
		String month3 = "3월";
		if ("1".equals(search_bungi)) {
			month1 = "1월";
			month2 = "2월";
			month3 = "3월";
		} else if ("2".equals(search_bungi)) {
			month1 = "4월";
			month2 = "5월";
			month3 = "6월";
		} else if ("3".equals(search_bungi)) {
			month1 = "7월";
			month2 = "8월";
			month3 = "9월";
		} else if ("4".equals(search_bungi)) {
			month1 = "10월";
			month2 = "11월";
			month3 = "12월";
		}

		/* 스타일 작업 */
		// 시트별 제목 스타일
		CellStyle cellStyle_title = workbook.createCellStyle(); //스타일 생성 - 헤더1
		cellStyle_title.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		Font font = workbook.createFont();
		font.setFontName("맑은 고딕"); // 폰트 이름
		font.setFontHeightInPoints((short) 18); // 폰트 크기
		//font.setColor(IndexedColors.RED.getIndex());    // 폰트 컬러
		//font.setStrikeout(true);                        // 글자 가운데 라인
		//font.setItalic(true);                            // 이탤릭체
		//font.setUnderline(Font.U_SINGLE);                // 밑줄
		//font.setBoldweight((short)8); //굵기?
		font.setBoldweight(Font.BOLDWEIGHT_BOLD); //글씨 bold
		cellStyle_title.setFont(font);

		// 시트별 노말 스타일
		CellStyle cellStyle_normal = workbook.createCellStyle(); //스타일 생성 - 헤더1
		cellStyle_normal.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		Font font2 = workbook.createFont();
		font2.setFontName("맑은 고딕"); // 폰트 이름
		cellStyle_normal.setFont(font2);

		// 표 셀 스타일 연녹색
		CellStyle cellStyle_td1 = workbook.createCellStyle(); //스타일 생성 - 헤더1
		cellStyle_td1.setAlignment(CellStyle.ALIGN_CENTER); //스타일 - 가운데정렬
		cellStyle_td1.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_td1.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_td1.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_td1.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_td1.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_td1.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle_td1.setWrapText(true);
		XSSFColor color1 = new XSSFColor(new java.awt.Color(215, 228, 188)); // 2017.09.28 mgkim RGB적용
		((XSSFCellStyle) cellStyle_td1).setFillForegroundColor(color1);

		// 표 셀 스타일 연블루
		CellStyle cellStyle_td2 = workbook.createCellStyle(); //스타일 생성 - 헤더1
		cellStyle_td2.setAlignment(CellStyle.ALIGN_CENTER); //스타일 - 가운데정렬
		cellStyle_td2.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_td2.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_td2.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_td2.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_td2.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_td2.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle_td2.setWrapText(true);
		XSSFColor color2 = new XSSFColor(new java.awt.Color(217, 229, 255)); // 2017.09.28 mgkim RGB적용
		((XSSFCellStyle) cellStyle_td2).setFillForegroundColor(color2);

		// 표 셀 스타일 연분홍
		CellStyle cellStyle_td3 = workbook.createCellStyle(); //스타일 생성 - 헤더1
		cellStyle_td3.setAlignment(CellStyle.ALIGN_CENTER); //스타일 - 가운데정렬
		cellStyle_td3.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_td3.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_td3.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_td3.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_td3.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_td3.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle_td3.setWrapText(true);
		XSSFColor color3 = new XSSFColor(new java.awt.Color(250, 224, 212)); // 2017.09.28 mgkim RGB적용
		((XSSFCellStyle) cellStyle_td3).setFillForegroundColor(color3);

		// 표 셀 스타일 연보라
		CellStyle cellStyle_td4 = workbook.createCellStyle(); //스타일 생성 - 헤더1
		cellStyle_td4.setAlignment(CellStyle.ALIGN_CENTER); //스타일 - 가운데정렬
		cellStyle_td4.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_td4.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_td4.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_td4.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_td4.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_td4.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle_td4.setWrapText(true);
		XSSFColor color4 = new XSSFColor(new java.awt.Color(173, 161, 247)); // 2018.09.14 pes RGB적용
		((XSSFCellStyle) cellStyle_td4).setFillForegroundColor(color4);

		CellStyle cellStyle = workbook.createCellStyle(); // 스타일 생성 - 일반셀
		cellStyle.setAlignment(CellStyle.ALIGN_LEFT); //스타일 - 가운데정렬
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setWrapText(true);

		CellStyle cellStyle_center = workbook.createCellStyle(); // 스타일 생성 - 일반셀
		cellStyle_center.setAlignment(CellStyle.ALIGN_CENTER); //스타일 - 가운데정렬
		cellStyle_center.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_center.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_center.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_center.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_center.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_center.setWrapText(true);

		CellStyle cellStyle_right = workbook.createCellStyle(); // 스타일 생성 - 일반셀
		cellStyle_right.setAlignment(CellStyle.ALIGN_RIGHT); //스타일 - 가운데정렬
		cellStyle_right.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_right.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_right.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_right.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_right.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_right.setWrapText(true);

		CellStyle cellStyle_number = workbook.createCellStyle(); // 스타일 생성 - 숫자
		XSSFDataFormat format = workbook.createDataFormat();
		cellStyle_number.setAlignment(CellStyle.ALIGN_RIGHT); //스타일 - 가운데정렬
		cellStyle_number.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_number.setDataFormat(format.getFormat("_-* #,##0_-;-* #,##0_-;_-* \"-\"_-;_-@_-"));
		cellStyle_number.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_number.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_number.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_number.setBorderBottom(CellStyle.BORDER_THIN);

		Font font_b = workbook.createFont();
		font_b.setBoldweight(Font.BOLDWEIGHT_BOLD); //글씨 bold

		CellStyle cellStyle_center_b = workbook.createCellStyle(); // 스타일 생성 - 일반셀
		cellStyle_center_b.setAlignment(CellStyle.ALIGN_CENTER); //스타일 - 가운데정렬
		cellStyle_center_b.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_center_b.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_center_b.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_center_b.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_center_b.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_center_b.setWrapText(true);
		cellStyle_center_b.setFont(font_b);

		CellStyle cellStyle_right_b = workbook.createCellStyle(); // 스타일 생성 - 일반셀
		cellStyle_right_b.setAlignment(CellStyle.ALIGN_RIGHT); //스타일 - 가운데정렬
		cellStyle_right_b.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
		cellStyle_right_b.setDataFormat(format.getFormat("_-* #,##0_-;-* #,##0_-;_-* \"-\"_-;_-@_-"));
		cellStyle_right_b.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle_right_b.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle_right_b.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle_right_b.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle_right_b.setWrapText(true);
		cellStyle_right_b.setFont(font_b);

		/* =======================================================================  공통 작업 끝 */

		/* =======================================================================  1번째 시트 시작 */
		XSSFSheet worksheet1 = workbook.createSheet("자료추출방법");

		Row row1 = null; //로우
		Cell cell1 = null;// 셀

		row1 = worksheet1.createRow(0); //첫 줄 생성
		Util_poi.setCell(cell1, row1, 0, cellStyle_title, "< 자료 추출 방법 >");

		row1 = worksheet1.createRow(1);
		row1 = worksheet1.createRow(2);
		row1 = worksheet1.createRow(3);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "1. 신고대상 : FPIS 등록된 운수사업자 기준, 실적신고 제외대상(1대사업자, 겸업하지않은 국제주선사업자, 주선-이사)");
		row1 = worksheet1.createRow(4);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "   * 지자체에서 제출한 자료로 운수사업자 DB 구축 곤란");
		row1 = worksheet1.createRow(5);
		row1 = worksheet1.createRow(6);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "2. 신고여부 : FPIS에 실적신고가 1건 이상 있는 경우 신고자로, 1건도 없는 경우 미신고자로 분류");
		row1 = worksheet1.createRow(7);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "    * 신고내용 중 계약금액과 함께, 배차금액 또는 재위탁금액이 반드시 포함되어 있어야만 신고건으로 인정(계약금액+배차금액, 계약금액+배차금액+재위탁금액, 계약금액+재위탁금액)");
		row1 = worksheet1.createRow(8);
		row1 = worksheet1.createRow(9);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "3. 신고자 중 누락 의심자 : 위탁자 A가 수탁자 가, 나, 다, 라 등과 운송거래 실적이 있는데 가,나와의 운송실적만 신고하고, 다,라와의 운송실적은 미신고한 경우");
		row1 = worksheet1.createRow(10);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "    * 위탁자 A : 신고대상자(1대, 순수국제주선, 주선-이사 제외)");
		row1 = worksheet1.createRow(11);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "    * 수탁자 가, 나, 다, 라 등 : FPIS에 실적신고한 모든 사업자(1대, 순수국제주선 등 신고제외대상도 포함)의 실적신고 건");
		row1 = worksheet1.createRow(12);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 3-1. 위탁자 A가 (겸업하지 않는 주선사업자) 또는 (1대+주선사업자) 일 경우 : 수탁자 가나다라 중 1대사업자 실적신고 제외");
		row1 = worksheet1.createRow(13);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 3-2. 위탁자 A가 (운송사업자) 또는 (2대이상+주선사업자) 일 경우 : 수탁자 가나다라 중 동일항만 및 이사화물 운송실적 제외");
		row1 = worksheet1.createRow(14);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 3-3. 위탁자 A가 가맹사업자 일 경우 : 수탁자 가나다라 중 동일항만 및 이사화물 운송실적 제외");
		row1 = worksheet1.createRow(15);
		row1 = worksheet1.createRow(16);
		Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "4. 미신고자 중 운송실적 있는 의심자 : 위탁자 A가 수탁자 가, 나, 다, 라 등과 운송거래 실적이 있는데 실적신고를 1건도 하지 않은 사업자");
		row1 = worksheet1.createRow(17);
		row1 = worksheet1.createRow(18);
		if (Integer.parseInt(search_year) < 2017) {
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "5. 파일링");
			row1 = worksheet1.createRow(19);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-1. 시군구별 자료는 17개 시도 파일로 구성");
			row1 = worksheet1.createRow(20);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-2. 시도별 파일안에 시군구별 시트로 신고현황 및 미신고의심세부내역을 각각의 시트로 작성");
		} else if (Integer.parseInt(search_year) == 2017 && Integer.parseInt(search_bungi) == 30) {
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "5. 신고금액 검증결과");
			row1 = worksheet1.createRow(19);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-1. 연간 등록한 모든 실적의 합으로 계산(택배실적포함)");
			row1 = worksheet1.createRow(20);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-2. [계약금액] 대비 [위탁금액+배차금액]이 50% 이상, 이하인 사업자 추출");
			row1 = worksheet1.createRow(21);
			row1 = worksheet1.createRow(22);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "6. 파일링");
			row1 = worksheet1.createRow(23);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 6-1. 시군구별 자료는 17개 시도 파일로 구성");
			row1 = worksheet1.createRow(24);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 6-2. 시도별 파일안에 시군구별 시트로 신고현황 및 미신고의심세부내역을 각각의 시트로 작성");
		} else {
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "5. 실적의무 미이행률");
			row1 = worksheet1.createRow(19);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "   * 실적의무 미이행률 = 누락의심 계약금액 / (계약금액 + 누락의심 계약금액)");
			row1 = worksheet1.createRow(20);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-1. 계약금액 기준 : 실적신고 의무대상이 아닌 신고제외차량의 배차실적을 비율만큼 제외, 직영&항만내이송&이사화물 제외.");
			row1 = worksheet1.createRow(21);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-2. 누락의심 계약금액 기준 : 항만내이송, 이사화물 제외.");
			row1 = worksheet1.createRow(22);
			Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "    * 순수주선의 계약금액이 0원일 때, 위탁금액을 기준으로 산정");
		}
		/* =======================================================================  1번째 시트 끝 */

		/* =======================================================================  2번째 시트 시작 */
		XSSFSheet worksheet4 = workbook.createSheet("미신고의심내역");

		Row row4 = null; //로우
		Cell cell4 = null;// 셀

		worksheet4.setColumnWidth(0, (short) 3600);
		worksheet4.setColumnWidth(1, (short) 9000);
		worksheet4.setColumnWidth(2, (short) 18000);
		worksheet4.setColumnWidth(3, (short) 3400);
		worksheet4.setColumnWidth(4, (short) 3800);
		worksheet4.setColumnWidth(5, (short) 5000);
		worksheet4.setColumnWidth(6, (short) 3000);

		if (Integer.valueOf(search_year) < 2017) {
			worksheet4.setColumnWidth(7, (short) 2600);
			worksheet4.setColumnWidth(8, (short) 9000);
			worksheet4.setColumnWidth(9, (short) 18000);
			worksheet4.setColumnWidth(10, (short) 8000);
			worksheet4.setColumnWidth(11, (short) 8000);
			worksheet4.setColumnWidth(12, (short) 8000);
			worksheet4.setColumnWidth(13, (short) 8000);
			worksheet4.setColumnWidth(14, (short) 6000);

			worksheet4.setColumnWidth(15, (short) 6000);
			worksheet4.setColumnWidth(16, (short) 6000);
			worksheet4.setColumnWidth(17, (short) 6000);
			worksheet4.setColumnWidth(18, (short) 16000);
			worksheet4.setColumnWidth(19, (short) 16000);
			worksheet4.setColumnWidth(20, (short) 16000);
		} else if (Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30) {
			worksheet4.setColumnWidth(7, (short) 2600);
			worksheet4.setColumnWidth(8, (short) 9000);
			worksheet4.setColumnWidth(9, (short) 18000);
			worksheet4.setColumnWidth(10, (short) 8000);
			worksheet4.setColumnWidth(11, (short) 8000);
			worksheet4.setColumnWidth(12, (short) 8000);
			worksheet4.setColumnWidth(13, (short) 6000);

			worksheet4.setColumnWidth(14, (short) 6000);
			worksheet4.setColumnWidth(15, (short) 6000);
			worksheet4.setColumnWidth(16, (short) 6000);
			worksheet4.setColumnWidth(17, (short) 6000);
			worksheet4.setColumnWidth(18, (short) 6000);
			worksheet4.setColumnWidth(19, (short) 6000);
			worksheet4.setColumnWidth(20, (short) 6000);
			worksheet4.setColumnWidth(21, (short) 6000);
			worksheet4.setColumnWidth(22, (short) 6000);
			worksheet4.setColumnWidth(23, (short) 6000);
			worksheet4.setColumnWidth(24, (short) 6000);
			worksheet4.setColumnWidth(25, (short) 6000);
			//20221205 chbaek 추가
			worksheet4.setColumnWidth(26, (short) 6000);
		} else {
			worksheet4.setColumnWidth(7, (short) 8000);
			worksheet4.setColumnWidth(8, (short) 3000);
			worksheet4.setColumnWidth(9, (short) 2600);
			worksheet4.setColumnWidth(10, (short) 9000);
			worksheet4.setColumnWidth(11, (short) 18000);
			worksheet4.setColumnWidth(12, (short) 8000);
			worksheet4.setColumnWidth(13, (short) 8000);
			worksheet4.setColumnWidth(14, (short) 8000);
			worksheet4.setColumnWidth(15, (short) 6000);

			worksheet4.setColumnWidth(16, (short) 6000);
			worksheet4.setColumnWidth(17, (short) 6000);
			worksheet4.setColumnWidth(18, (short) 6000);
			worksheet4.setColumnWidth(19, (short) 6000);
			worksheet4.setColumnWidth(20, (short) 6000);
			worksheet4.setColumnWidth(21, (short) 6000);
			worksheet4.setColumnWidth(22, (short) 6000);
			worksheet4.setColumnWidth(23, (short) 6000);
			worksheet4.setColumnWidth(24, (short) 6000);
			worksheet4.setColumnWidth(25, (short) 6000);
			worksheet4.setColumnWidth(26, (short) 6000);
			worksheet4.setColumnWidth(27, (short) 6000);
			worksheet4.setColumnWidth(28, (short) 6000);
			//20221205 chbaek 계약년도 2개 제외
			//worksheet4.setColumnWidth(29, (short) 6000);			
			//worksheet4.setColumnWidth(30, (short) 6000);
		}

		//헤더작업
		row4 = worksheet4.createRow(0); //첫 줄 생성
		Util_poi.setCell(cell4, row4, 0, cellStyle_title, "< FPIS 등록자 기준 미신고 의심 세부내역 >");

		row4 = worksheet4.createRow(1);
		row4 = worksheet4.createRow(2);
		Util_poi.setCell(cell4, row4, 0, cellStyle_normal, "* 1개의 신고대상자에게 의심내역이 여러건 있을 경우 신고대상자 셀병합 하여 작성");
		if ((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 60) || (Integer.valueOf(search_year) > 2017))
			Util_poi.setCell(cell4, row4, 4, cellStyle_normal, "   * 실적의무 미이행률 = 누락의심 계약금액 / (계약금액 + 누락의심 계약금액)");
		row4 = worksheet4.createRow(3);
		Util_poi.setCell(cell4, row4, 0, cellStyle_normal, "* 신고자(O) 중 의심내역이 있는 경우는 '누락의심자', 미신고자(X) 중 의심내역 있는 경우는 '미신고 중 실적있는 의심자'");
		if ((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 60) || (Integer.valueOf(search_year) > 2017))
			Util_poi.setCell(cell4, row4, 4, cellStyle_normal, "   * 계약금액 기준 : 실적신고 의무대상이 아닌 신고제외차량의 배차실적을 비율만큼 제외, 직영&항만내이송&이사화물 제외.");
		row4 = worksheet4.createRow(4);
		Util_poi.setCell(cell4, row4, 0, cellStyle_normal, "* 수탁금액은 : 계약금액이 있을 경우 계약금액 기재, 없을경우 배차금액+위탁금액 기재");
		if ((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 60) || (Integer.valueOf(search_year) > 2017))
			Util_poi.setCell(cell4, row4, 4, cellStyle_normal, "   * 누락의심 계약금액 기준 : 항만내이송, 이사화물 제외.");

		row4 = worksheet4.createRow(5);
		Util_poi.setCell(cell4, row4, 0, cellStyle_td1, "신고대상자");
		Util_poi.setCell(cell4, row4, 1, cellStyle_td1, "");
		Util_poi.setCell(cell4, row4, 2, cellStyle_td1, "");
		Util_poi.setCell(cell4, row4, 3, cellStyle_td1, "");
		Util_poi.setCell(cell4, row4, 4, cellStyle_td1, "");
		Util_poi.setCell(cell4, row4, 5, cellStyle_td1, "");
		Util_poi.setCell(cell4, row4, 6, cellStyle_td1, "");
		if ((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30) || (Integer.valueOf(search_year) < 2017)) {
			Util_poi.setCell(cell4, row4, 7, cellStyle_td2, "신고여부\n(O/X)");
			Util_poi.setCell(cell4, row4, 8, cellStyle_td3, "미신고 의심내역");
			Util_poi.setCell(cell4, row4, 9, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 10, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 11, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 12, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 13, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 14, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 15, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 16, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 17, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 18, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 19, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 20, cellStyle_td1, "");
		} else {
			Util_poi.setCell(cell4, row4, 7, cellStyle_td4, "계약금액");
			Util_poi.setCell(cell4, row4, 8, cellStyle_td4, "누락의심\n미이행률");
			Util_poi.setCell(cell4, row4, 9, cellStyle_td2, "신고여부\n(O/X)");
			Util_poi.setCell(cell4, row4, 10, cellStyle_td3, "미신고 의심내역");
			Util_poi.setCell(cell4, row4, 11, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 12, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 13, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 14, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 15, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 16, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 17, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 18, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 19, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 20, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 21, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 22, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 23, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 24, cellStyle_td1, "");
			//20221205 chbaek 여기서부터 추가 계약년도 2개 제외
			Util_poi.setCell(cell4, row4, 25, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 26, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 27, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 28, cellStyle_td1, "");
			//Util_poi.setCell(cell4, row4, 29, cellStyle_td1, "");
			//Util_poi.setCell(cell4, row4, 30, cellStyle_td1, "");
		}

		if (Integer.valueOf(search_year) > 2016) {
			for (int i = 21; i < 26; i++) {
				Util_poi.setCell(cell4, row4, i, cellStyle_td1, "");
			}
		}

		row4 = worksheet4.createRow(6); //첫 줄 생성
		Util_poi.setCell(cell4, row4, 0, cellStyle_td1, "시군구");
		Util_poi.setCell(cell4, row4, 1, cellStyle_td1, "사업자명");
		Util_poi.setCell(cell4, row4, 2, cellStyle_td1, "주소");
		Util_poi.setCell(cell4, row4, 3, cellStyle_td1, "사업자번호\n(세금관련)");
		Util_poi.setCell(cell4, row4, 4, cellStyle_td1, "법인번호");
		Util_poi.setCell(cell4, row4, 5, cellStyle_td1, "업태");
		Util_poi.setCell(cell4, row4, 6, cellStyle_td1, "보유차량수");
		Util_poi.setCell(cell4, row4, 7, cellStyle_td1, "");
		//2018.02.26 pes 수탁금액, 배차정보 12월까지 출력
		//2018.04.26 osm 수탁자 연락처, 배차정보 삭제

		if (Integer.valueOf(search_year) < 2017) {
			Util_poi.setCell(cell4, row4, 8, cellStyle_td3, "수탁자 상호");
			Util_poi.setCell(cell4, row4, 9, cellStyle_td3, "수탁자 주소");
			Util_poi.setCell(cell4, row4, 10, cellStyle_td3, "수탁자 사업자번호");
			Util_poi.setCell(cell4, row4, 11, cellStyle_td3, "수탁자 연락처");
			Util_poi.setCell(cell4, row4, 12, cellStyle_td3, "수탁자 업태");
			Util_poi.setCell(cell4, row4, 13, cellStyle_td3, "수탁자 보유차량수");
			Util_poi.setCell(cell4, row4, 14, cellStyle_td3, "수탁금액 합계");

			Util_poi.setCell(cell4, row4, 15, cellStyle_td3, "수탁금액 " + month1);
			Util_poi.setCell(cell4, row4, 16, cellStyle_td3, "수탁금액 " + month2);
			Util_poi.setCell(cell4, row4, 17, cellStyle_td3, "수탁금액 " + month3);
			Util_poi.setCell(cell4, row4, 18, cellStyle_td3, "배차정보 " + month1);
			Util_poi.setCell(cell4, row4, 19, cellStyle_td3, "배차정보 " + month2);
			Util_poi.setCell(cell4, row4, 20, cellStyle_td3, "배차정보 " + month3);
		} else if (Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30) {
			Util_poi.setCell(cell4, row4, 8, cellStyle_td3, "수탁자 상호");
			Util_poi.setCell(cell4, row4, 9, cellStyle_td3, "수탁자 주소");
			Util_poi.setCell(cell4, row4, 10, cellStyle_td3, "수탁자 사업자번호");
			Util_poi.setCell(cell4, row4, 11, cellStyle_td3, "수탁자 업태");
			Util_poi.setCell(cell4, row4, 12, cellStyle_td3, "수탁자 보유차량수");
			Util_poi.setCell(cell4, row4, 13, cellStyle_td3, "수탁금액 합계");

			int j = 14;
			for (int i = 1; i < 13; i++) {
				Util_poi.setCell(cell4, row4, j, cellStyle_td3, "수탁금액 " + i + "월");
				j++;
			}

		} else {
			Util_poi.setCell(cell4, row4, 8, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 9, cellStyle_td1, "");
			Util_poi.setCell(cell4, row4, 10, cellStyle_td3, "수탁자 상호");
			Util_poi.setCell(cell4, row4, 11, cellStyle_td3, "수탁자 주소");
			Util_poi.setCell(cell4, row4, 12, cellStyle_td3, "수탁자 연락처");
			Util_poi.setCell(cell4, row4, 13, cellStyle_td3, "수탁자 사업자번호");
			Util_poi.setCell(cell4, row4, 14, cellStyle_td3, "수탁자 업태");
			Util_poi.setCell(cell4, row4, 15, cellStyle_td3, "수탁자 보유차량수");
			Util_poi.setCell(cell4, row4, 16, cellStyle_td3, "수탁금액 합계");
			//20221205 chbaek 계약년도 2개 제외
			//Util_poi.setCell(cell4, row4, 17, cellStyle_td3, "계약년도 " + (Integer.valueOf(search_year) - 2));
			//Util_poi.setCell(cell4, row4, 18, cellStyle_td3, "계약년도 " + (Integer.valueOf(search_year) - 1));

			int j = 17; //20221205 chbaek 18 -> 19(tel추가) -> 17(계약년도 2개 제외)
			for (int i = 1; i < 13; i++) {
				String month = "";
				if (i < 10)
					month = "0" + i;
				else
					month = String.valueOf(i);
				Util_poi.setCell(cell4, row4, j, cellStyle_td3, "계약년월 " + (Integer.valueOf(search_year)) + month);
				j++;
			}
		} //2018.09.14 pes 계약금액, 미이행률, 계약년도 추가

		worksheet4.addMergedRegion(new CellRangeAddress(5, 5, 0, 6)); //셀병합 -  행 : 0   을   1까지    열 : 0   을  0까지
		worksheet4.addMergedRegion(new CellRangeAddress(5, 6, 7, 7)); //셀병합
		if (Integer.valueOf(search_year) < 2017)
			worksheet4.addMergedRegion(new CellRangeAddress(5, 5, 8, 20)); //셀병합
		else if (Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30)
			worksheet4.addMergedRegion(new CellRangeAddress(5, 5, 8, 25)); //셀병합
		else {
			worksheet4.addMergedRegion(new CellRangeAddress(5, 6, 8, 8)); //계약금액
			worksheet4.addMergedRegion(new CellRangeAddress(5, 6, 9, 9)); //미이행률
			worksheet4.addMergedRegion(new CellRangeAddress(5, 5, 10, 28)); // 계약년도 20221205 chbaek 29 -> 30 -> 28(계약년도 2개 제외)로 변경
		} //2018.09.14 pes 계약금액, 미이행률, 계약년도 추가

		int index_sigungu = 1; // 시군구명 셀 병합용
		int index_comp_data = 1; // 업체정보 셀 병합용

		for (int i = 0; i < resultList_sheet4.size(); i++) {
			row4 = worksheet4.createRow(i + 7);

			Util_poi.setCell(cell4, row4, 0, cellStyle, resultList_sheet4.get(i).getSigungu_nm());
			Util_poi.setCell(cell4, row4, 1, cellStyle, resultList_sheet4.get(i).getComp_nm());

			/* if ("Y".equals(masked_info_status)) {
				Util_poi.setCell(cell4, row4, 2, cellStyle, resultList_sheet4.get(i).getAddr1());
			} else {
				Util_poi.setCell(cell4, row4, 2, cellStyle, resultList_sheet4.get(i).getMasked_addr1());
			} */

			/* 2021.11.08 jwchoi */
			Util_poi.setCell(cell4, row4, 2, cellStyle, resultList_sheet4.get(i).getAddr1());

			Util_poi.setCell(cell4, row4, 3, cellStyle, resultList_sheet4.get(i).getUsr_mst_key());
			Util_poi.setCell(cell4, row4, 4, cellStyle, resultList_sheet4.get(i).getComp_corp_num());
			if (Integer.valueOf(search_year) < 2017) {
				Util_poi.setCell(cell4, row4, 5, cellStyle, resultList_sheet4.get(i).getComp_cls());
			} else {
				Util_poi.setCell(cell4, row4, 5, cellStyle, resultList_sheet4.get(i).getComp_cls_detail());
			}

			Util_poi.setNumberCell3(cell4, row4, 6, cellStyle_number, resultList_sheet4.get(i).getCar_cnt());
			if (Integer.valueOf(search_year) < 2017) {
				Util_poi.setCell(cell4, row4, 7, cellStyle_center, resultList_sheet4.get(i).getIs_reg());

				Util_poi.setCell(cell4, row4, 8, cellStyle, resultList_sheet4.get(i).getSutak_nm());

				/* if ("Y".equals(masked_info_status)) {
					Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getSutak_addr1());
				} else {
					Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getMasked_sutak_addr1());
				} */

				/* 2021.11.08 jwchoi */
				Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getSutak_addr1());

				Util_poi.setCell(cell4, row4, 10, cellStyle, resultList_sheet4.get(i).getSutak_num());

				if ("Y".equals(masked_info_status)) {
					Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getTel());
				} else {
					Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getMasked_tel());
				}

				Util_poi.setCell(cell4, row4, 12, cellStyle, resultList_sheet4.get(i).getSutak_cls());
				Util_poi.setNumberCell3(cell4, row4, 13, cellStyle_number, resultList_sheet4.get(i).getSutak_cars_cnt());
				Util_poi.setNumberCell3(cell4, row4, 14, cellStyle_number, resultList_sheet4.get(i).getSutak_sum_charge());

				Util_poi.setNumberCell3(cell4, row4, 15, cellStyle_number, resultList_sheet4.get(i).getSutak_charge1());
				Util_poi.setNumberCell3(cell4, row4, 16, cellStyle_number, resultList_sheet4.get(i).getSutak_charge2());
				Util_poi.setNumberCell3(cell4, row4, 17, cellStyle_number, resultList_sheet4.get(i).getSutak_charge3());
				Util_poi.setCell(cell4, row4, 18, cellStyle, resultList_sheet4.get(i).getSutak_car1());
				Util_poi.setCell(cell4, row4, 19, cellStyle, resultList_sheet4.get(i).getSutak_car2());
				Util_poi.setCell(cell4, row4, 20, cellStyle, resultList_sheet4.get(i).getSutak_car3());
			} else if (Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30) {

				Util_poi.setCell(cell4, row4, 7, cellStyle_center, resultList_sheet4.get(i).getIs_reg());

				Util_poi.setCell(cell4, row4, 8, cellStyle, resultList_sheet4.get(i).getSutak_nm());

				/* 2020.11.11 ysw 정보노출에 따른 마스킹 처리*/
				/* if ("Y".equals(masked_info_status)) {
					Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getSutak_addr1());
				} else {
					Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getMasked_sutak_addr1());
				} */

				/* 2021.11.08 jwchoi */
				Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getSutak_addr1());
				/* 2022.11.01 jwchoi */
				Util_poi.setCell(cell4, row4, 10, cellStyle, resultList_sheet4.get(i).getTel());
				Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getSutak_num());
				Util_poi.setCell(cell4, row4, 12, cellStyle, resultList_sheet4.get(i).getComp_cls_detail_nm());
				Util_poi.setNumberCell3(cell4, row4, 13, cellStyle_number, resultList_sheet4.get(i).getSutak_cars_cnt());
				Util_poi.setNumberCell3(cell4, row4, 14, cellStyle_number, resultList_sheet4.get(i).getSutak_sum_charge());

				Util_poi.setNumberCell3(cell4, row4, 15, cellStyle_number, resultList_sheet4.get(i).getSutak_charge1());
				Util_poi.setNumberCell3(cell4, row4, 16, cellStyle_number, resultList_sheet4.get(i).getSutak_charge2());
				Util_poi.setNumberCell3(cell4, row4, 17, cellStyle_number, resultList_sheet4.get(i).getSutak_charge3());
				Util_poi.setNumberCell3(cell4, row4, 18, cellStyle_number, resultList_sheet4.get(i).getSutak_charge4());
				Util_poi.setNumberCell3(cell4, row4, 19, cellStyle_number, resultList_sheet4.get(i).getSutak_charge5());
				Util_poi.setNumberCell3(cell4, row4, 20, cellStyle_number, resultList_sheet4.get(i).getSutak_charge6());
				Util_poi.setNumberCell3(cell4, row4, 21, cellStyle_number, resultList_sheet4.get(i).getSutak_charge7());
				Util_poi.setNumberCell3(cell4, row4, 22, cellStyle_number, resultList_sheet4.get(i).getSutak_charge8());
				Util_poi.setNumberCell3(cell4, row4, 23, cellStyle_number, resultList_sheet4.get(i).getSutak_charge9());
				Util_poi.setNumberCell3(cell4, row4, 24, cellStyle_number, resultList_sheet4.get(i).getSutak_charge10());
				Util_poi.setNumberCell3(cell4, row4, 25, cellStyle_number, resultList_sheet4.get(i).getSutak_charge11());
				Util_poi.setNumberCell3(cell4, row4, 26, cellStyle_number, resultList_sheet4.get(i).getSutak_charge12());
			} else {
				Util_poi.setNumberCell3(cell4, row4, 7, cellStyle_number, resultList_sheet4.get(i).getOk_charge());
				Util_poi.setCell(cell4, row4, 8, cellStyle_number, resultList_sheet4.get(i).getNo_perform());
				Util_poi.setCell(cell4, row4, 9, cellStyle_center, resultList_sheet4.get(i).getIs_reg());

				Util_poi.setCell(cell4, row4, 10, cellStyle, resultList_sheet4.get(i).getSutak_nm());

				/* 2020.11.11 ysw 정보노출에 따른 마스킹 처리*/
				/* if ("Y".equals(masked_info_status)) {
					Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getSutak_addr1());
				} else {
					Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getMasked_sutak_addr1());
				} */

				/* 2021.11.08 jwchoi */
				Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getSutak_addr1());
				/* 2022.11.01 jwchoi */
				Util_poi.setCell(cell4, row4, 12, cellStyle, resultList_sheet4.get(i).getTel());
				Util_poi.setCell(cell4, row4, 13, cellStyle, resultList_sheet4.get(i).getSutak_num());
				Util_poi.setCell(cell4, row4, 14, cellStyle, resultList_sheet4.get(i).getComp_cls_detail_nm());
				Util_poi.setNumberCell3(cell4, row4, 15, cellStyle_number, resultList_sheet4.get(i).getSutak_cars_cnt());
				Util_poi.setNumberCell3(cell4, row4, 16, cellStyle_number, resultList_sheet4.get(i).getSutak_sum_charge());
				
				//20221205 chbaek 계약년도 -2, 계약년도 -1 제외
				//Util_poi.setNumberCell3(cell4, row4, 17, cellStyle_number, resultList_sheet4.get(i).getSutak_charge0_1());
				//Util_poi.setNumberCell3(cell4, row4, 18, cellStyle_number, resultList_sheet4.get(i).getSutak_charge0_2());
				Util_poi.setNumberCell3(cell4, row4, 17, cellStyle_number, resultList_sheet4.get(i).getSutak_charge1());
				Util_poi.setNumberCell3(cell4, row4, 18, cellStyle_number, resultList_sheet4.get(i).getSutak_charge2());
				Util_poi.setNumberCell3(cell4, row4, 19, cellStyle_number, resultList_sheet4.get(i).getSutak_charge3());
				Util_poi.setNumberCell3(cell4, row4, 20, cellStyle_number, resultList_sheet4.get(i).getSutak_charge4());
				Util_poi.setNumberCell3(cell4, row4, 21, cellStyle_number, resultList_sheet4.get(i).getSutak_charge5());
				Util_poi.setNumberCell3(cell4, row4, 22, cellStyle_number, resultList_sheet4.get(i).getSutak_charge6());
				Util_poi.setNumberCell3(cell4, row4, 23, cellStyle_number, resultList_sheet4.get(i).getSutak_charge7());
				Util_poi.setNumberCell3(cell4, row4, 24, cellStyle_number, resultList_sheet4.get(i).getSutak_charge8());
				Util_poi.setNumberCell3(cell4, row4, 25, cellStyle_number, resultList_sheet4.get(i).getSutak_charge9());
				Util_poi.setNumberCell3(cell4, row4, 26, cellStyle_number, resultList_sheet4.get(i).getSutak_charge10());
				Util_poi.setNumberCell3(cell4, row4, 27, cellStyle_number, resultList_sheet4.get(i).getSutak_charge11());
				Util_poi.setNumberCell3(cell4, row4, 28, cellStyle_number, resultList_sheet4.get(i).getSutak_charge12());
			}

			/* ============== 시군구명, 업체정보 셀병합작업 시작 */
			if (index_sigungu == Integer.parseInt(resultList_sheet4.get(i).getSigungu_group())) {
				if (index_sigungu != 1) { // 셀병합작업
					int index_base_data = (i + 7 - Integer.parseInt(resultList_sheet4.get(i).getSigungu_group())) + 1;// 병합대상 기준 데이터 위치
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 0, 0)); // 시군구명 셀병합
				}
				index_sigungu = 1;
			} else {
				index_sigungu++;
			}
			if (index_comp_data == Integer.parseInt(resultList_sheet4.get(i).getComp_group())) {
				if (index_comp_data != 1) { // 셀병합작업
					int index_base_data = (i + 7 - Integer.parseInt(resultList_sheet4.get(i).getComp_group())) + 1;// 병합대상 기준 데이터 위치
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 1, 1)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 2, 2)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 3, 3)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 4, 4)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 5, 5)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 6, 6)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 7, 7)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 8, 8)); // 업체정보 셀병합
					worksheet4.addMergedRegion(new CellRangeAddress(index_base_data, (i + 7), 9, 9)); // 업체정보 셀병합
				}
				index_comp_data = 1;
			} else {
				index_comp_data++;
			}
			/* ============== 시군구명, 업체정보 셀병합작업 끝 */

		}

		/* =======================================================================  4번째 시트 끝 */

		String file_name = Util.getDateFormat3() + "_" + SessionVO.getUser_id() + ".xlsx"; //임시저장할 파일 이름
		FileOutputStream output = null;
		PrintWriter out = null;
		try {
			output = new FileOutputStream(fileStorePath + file_name);
			workbook.write(output);//파일쓰기 끝.
			String fileName = "";
			//2018.02.22 pes 17년 이후 연도별조회
			if (Integer.parseInt(search_year) < 2017) {
				fileName = search_year + "_" + search_bungi + "_BungiSingoResult" + "_" + Util.getDateFormat3() + ".xlsx";//다운로드할 파일 이름
			} else {
				fileName = search_year + "_YearSingoResult" + "_" + Util.getDateFormat3() + ".xlsx";//다운로드할 파일 이름
			}
			out = res.getWriter();
			JSONObject result = new JSONObject();
			result.put("file_path", fileStorePath);
			result.put("file_name", file_name);
			result.put("fileName", fileName);
			out.write(result.toString());
			//Util_file.fileDownloadAndDelete(req, res, file_path, file_name, fileName);//파일다운로드 후 임시저장파일 삭제
		} catch (FileNotFoundException e) {
			logger.error("[ERROR] - FileNotFoundException : ", e);
		} catch (IOException e) {
			logger.error("[ERROR] - IOException : ", e);
		} catch (JSONException e) {
			logger.error("[ERROR] - JSONException : ", e);
		} finally {
			try {
				if (output != null)
					output.close();
				if (out != null)
					out.close();
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			}
		}
	}

	@RequestMapping("/stat/FpisStatTransDetail_excel_detail_dir.do")
	public void FpisStatTransDetail_excel_detail_dir(FpisAdminStatTrans4VO shVO,
			HttpServletRequest req,
			HttpServletResponse response) throws Exception, NullPointerException {
		int search_year = Integer.parseInt(req.getParameter("search_year"));

		if (search_year >= 2017) {
			//17년도 양식 start============================================================================================================================

			// 신고자정보 불러오기 -> 새양식의 실적신고여부 확인하기위해서
			FpisAdminStatBase12VO shVO12 = new FpisAdminStatBase12VO();
			shVO12.setSearch_year(shVO.getSearch_year());
			shVO12.setSearch_bungi("60");
			shVO12.setSearch_type("R");
			String sTableName = adminStatSvc.getSearchTableName(shVO12);
			shVO.setSearch_table_name(sTableName);

			//sheet별 리스트들 가져오는 곳
			FpisAdminStatTrans4VO summaryVO = FpisSvc.selectMinDirSummary_2017(shVO); // 총괄
			//List<FpisAdminStatTrans8VO> minList =FpisSvc.selectMinDetailList(shVO); //최소운송 차량 상세정보
			FpisAdminStatTrans4VO dirSummaryVO = FpisSvc.selectDirSummary(shVO); //직접운송 금액 상세 총괄
			List<FpisAdminStatTrans8VO> dirList = FpisSvc.selectDirDetailList(shVO); //직접운송1단계
			List<FpisAdminStatTrans8VO> dirTbList = FpisSvc.selectDirTbDetailList(shVO); //직접운송2단계
			List<FpisAdminStatTrans4VO> dirVioList = FpisSvc.selectDirVioList(shVO); //직접운송 위탁금지위반 상세정보

			//int otherCompCarCnt = summaryVO.getMin_other_comp_car_cnt();
			FileInputStream inputStream = null;
			FileOutputStream out = null;
			PrintWriter pout = null;

			//1.템플릿 파일 복사 17년도 파일로
			String makingDtm = Util.getDateFormat2();
			String excelFileName = makingDtm + "_" + shVO.getUsr_mst_key() + "_" + shVO.getSearch_year() + ".xlsx";

			String excelFileSize = "";

			excelFileSize = "_2017_dir";

			moveToUploadDirectory(excelFileName, excelFileSize);

			//2.복사한 파일 메모리 로드
			//String excelFile = EgovProperties.getProperty("Globals.majarStatFilePath") + File.separator + excelFileName;
			String excelFile = majarStatFilePath + File.separator + excelFileName;
			try {
				inputStream = new FileInputStream(new File(excelFile));
				
				//2022.01.24 jwchoi HSSFWorkbook > XSSFWorkbook
				XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

				CellStyle wrapCellStyle = workbook.createCellStyle();
				wrapCellStyle.setWrapText(true);

				CellStyle cellformat_solid = workbook.createCellStyle();
				cellformat_solid.setBorderTop(CellStyle.BORDER_THIN);
				cellformat_solid.setBorderRight(CellStyle.BORDER_THIN);
				cellformat_solid.setBorderLeft(CellStyle.BORDER_THIN);
				cellformat_solid.setBorderBottom(CellStyle.BORDER_THIN);
				cellformat_solid.setAlignment(CellStyle.ALIGN_CENTER);
				cellformat_solid.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

				//3.데이터 채우기

				//3-1. 총괄--------------------------------------------------------------------------------------------------
				//기본정보---------------
				Sheet firstSheet = workbook.getSheetAt(0); //첫번째 시트 가져오기

				firstSheet.getRow(3).getCell(0).setCellValue(Util.splitUsrMstKey(summaryVO.getUsr_mst_key())); //사업자번호
				firstSheet.getRow(3).getCell(1).setCellValue(summaryVO.getComp_nm()); //업체명
				firstSheet.getRow(3).getCell(2).setCellValue(summaryVO.getCeo()); //대표자
				firstSheet.getRow(3).getCell(3).setCellValue(summaryVO.getComp_cls_detail()); //업종 및 업태
				firstSheet.getRow(3).getCell(4).setCellValue(summaryVO.getBase_year() + "년"); //분석 기준연도
				firstSheet.getRow(3).getCell(5).setCellValue("수정마감기한 기준 신고데이터"); //분석 기준시점
				firstSheet.getRow(3).getCell(6).setCellValue("basic".equals(summaryVO.getIs_reg()) ? "실적신고" : "no_record".equals(summaryVO.getIs_reg()) ? "실적없음 신고" : "신고된 실적 없음"); //실적신고 여부

				//직접운송 의무제----------
				firstSheet.getRow(3).getCell(7).setCellValue(summaryVO.getStep_1_result()); //1단계 준수여부
				firstSheet.getRow(3).getCell(8).setCellValue(Util.Comma_won(summaryVO.getStep_1_cont())); //1단계 계약금액
				firstSheet.getRow(3).getCell(9).setCellValue(Util.Comma_won(summaryVO.getStep_1_valid())); //1단계 인정금액
				firstSheet.getRow(3).getCell(10).setCellValue(Util.Comma_won(summaryVO.getStep_1_unvalid())); //1단계 미인정금액
				firstSheet.getRow(3).getCell(11).setCellValue(summaryVO.getStep_2_result()); //2단계 이상 준수여부
				firstSheet.getRow(3).getCell(12).setCellValue(Util.Comma_won(summaryVO.getStep_2_cont())); //2단계 이상 계약금액
				firstSheet.getRow(3).getCell(13).setCellValue(Util.Comma_won(summaryVO.getStep_2_valid())); //2단계 이상 인정금액
				firstSheet.getRow(3).getCell(14).setCellValue(Util.Comma_won(summaryVO.getStep_2_unvalid())); //2단계 이상 미인정금액
				firstSheet.getRow(3).getCell(15).setCellValue(summaryVO.getDir_result()); //직접운송 비율 위반여부
				firstSheet.getRow(3).getCell(16).setCellValue(summaryVO.getDir_not_percent()); //직접운송의무 비율 미이행율
				firstSheet.getRow(3).getCell(17).setCellValue(summaryVO.getTrust_violation()); //위탁금지 위반여부
				firstSheet.getRow(3).getCell(18).setCellValue(summaryVO.getTrust_violation_cnt()); //위탁금지 위반 건수

				//3-2. 직접운송 금액 상세 총괄 시트--------------------------------------------------------------------------------------------------
				Sheet Sheet_2 = workbook.getSheetAt(1); // 직접운송 위탁금지위반 상세정보 시트 가져오기

				//직접운송 금액 상세 총괄 쿼리 List 아마 size 1이겠지

				// 데이터 존재
				//row 객체 가져오기
				Sheet_2.getRow(4).getCell(0).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_01_valid())); //1단계인정금액 - 직영차량 배차실적
				Sheet_2.getRow(4).getCell(1).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_02_valid())); //1단계인정금액 - 위수탁 차량 배차실적
				Sheet_2.getRow(4).getCell(2).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_03_valid())); //1단계인정금액 - 장기용차 배차실적
				Sheet_2.getRow(4).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_trust_mang_valid())); //1단계인정금액 - 위탁 실적
				Sheet_2.getRow(4).getCell(4).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_valid())); //1단계 인정금액 - 합계
				Sheet_2.getRow(4).getCell(5).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_03_unvalid())); //1단계 미인정금액 - 장기용차 배차실적
				Sheet_2.getRow(4).getCell(6).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_out_unvalid())); //1단계 미인정금액 - 장기용차 기한외 실적
				Sheet_2.getRow(4).getCell(7).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_04_unvalid())); //1단계 미인정금액 - 단기용차 배차실적
				Sheet_2.getRow(4).getCell(8).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_not_unvalid())); //1단계 미인정금액 - FPIS상 미등록차량 배차실적
				Sheet_2.getRow(4).getCell(9).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_trust_mang_unvalid())); //1단계 미인정금액 - 화물정보망 미이용 위탁실적
				Sheet_2.getRow(4).getCell(10).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_unvalid())); //1단계 미인정금액 - 합계
				Sheet_2.getRow(4).getCell(11).setCellValue(dirSummaryVO.getStep_1_result()); // 1단계 준수여부
				Sheet_2.getRow(4).getCell(12).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_cont())); //1단계 계약금액
				Sheet_2.getRow(4).getCell(13).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_01_valid())); //2단계 이상 인정금액 - 직영차량 배차실적
				Sheet_2.getRow(4).getCell(14).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_02_valid())); //2단계 이상 인정금액 - 위수탁 차량 배차실적
				Sheet_2.getRow(4).getCell(15).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_03_valid())); //2단계 이상 인정금액 - 장기용차 배차실적
				Sheet_2.getRow(4).getCell(16).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_trust_mang_valid())); //2단계 이상 인정금액 - 위탁 실적
				Sheet_2.getRow(4).getCell(17).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_valid())); //2단계 이상 인정금액 - 합계
				Sheet_2.getRow(4).getCell(18).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_03_unvalid())); //2단계 이상 미인정금액 - 장기용차 배차실적
				Sheet_2.getRow(4).getCell(19).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_out_unvalid())); //2단계 이상 미인정금액 - 장기용차 기한외 실적
				Sheet_2.getRow(4).getCell(20).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_04_unvalid())); //2단계 이상 미인정금액 - 단기용차 배차실적
				Sheet_2.getRow(4).getCell(21).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_not_unvalid())); //2단계 이상 미인정금액 - FPIS상 미등록차량 배차실적
				Sheet_2.getRow(4).getCell(22).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_trust_mang_unvalid())); //2단계 이상 미인정금액 - 화물정보망 미이용 위탁실적
				Sheet_2.getRow(4).getCell(23).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_unvalid())); //2단계 이상 미인정금액 - 합계
				Sheet_2.getRow(4).getCell(24).setCellValue(dirSummaryVO.getStep_2_result()); //2단계 이상 준수여부
				Sheet_2.getRow(4).getCell(25).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_cont())); //2단계 이상 계약금액
				Sheet_2.getRow(4).getCell(26).setCellValue(dirSummaryVO.getDir_result()); // 직접운송 비율 위반여부
				Sheet_2.getRow(4).getCell(27).setCellValue(dirSummaryVO.getDir_not_percent() + "%"); // 직접운송 비율 미이행율
				//3-4. 직접 상세내역--------------------------------------------------------------------------------------------------
				Sheet thirdSheet = workbook.getSheetAt(2);
				int thirdstartrow = 1;

				Sheet fourthSheet = workbook.getSheetAt(3);
				int fourthstartrow = 1;

				if ((dirList == null || dirList.size() == 0)) {
					if (dirTbList == null || dirTbList.size() == 0) {
						thirdSheet.createRow(thirdstartrow).createCell(0).setCellValue("1단계 정보가 없습니다.");
					}
					fourthSheet.createRow(fourthstartrow).createCell(0).setCellValue("2단계 정보가 없습니다.");
				} else {

					for (int i = 0; i < dirList.size(); i++) {

						if ("STEP1".equals(dirList.get(i).getReg_dir_step())) {// 1단계 ------------------

							if ("OPER".equals(dirList.get(i).getReg_gubun())) { // 1단계 배차

								if ("CAR_02_D".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_01".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_02".equals(dirList.get(i).getCar_type_final())) {
								} else {
									Row row = null;
									if (thirdSheet.getRow(thirdstartrow) == null) {
										row = thirdSheet.createRow(thirdstartrow);
									} else {
										row = thirdSheet.getRow(thirdstartrow);
									}

									row.createCell(0).setCellValue("배차 실적");
									row.createCell(1).setCellValue(dirList.get(i).getYyyymm());

									if ("CAR_01_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("인정 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if ("CAR_01_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("직영");
											row.createCell(5).setCellValue("-");
										} else if ("CAR_02_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("지입");
											row.createCell(5).setCellValue("-");
										} else if ("CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("장기용차");
											row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt() + " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
										}
									} else if ("CAR_03_N".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("장기용차 기준회수 미달");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt() + " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
									} else if ("CAR_04_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_04_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_03".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("등록기간 미달(단기용차)");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("단기용차");
										row.createCell(5).setCellValue("-");
									} else if ("CAR_99_N".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("미등록차량 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("확인불가");										
										row.createCell(5).setCellValue("-");
									} else if ("CAR_03_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_05".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_06".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_99".equals(dirList.get(i).getCar_type_final())) { //20231012 CAR_03_N_05,06,99 chbaek 미등록차량 실적 - 장기용차에서 장기용차 기한외 실적 - 장기용차로 이동
										row.createCell(2).setCellValue("장기용차 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue("-");
									} else if ("CAR_01_N".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_N".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("차량등록 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("확인불가");
										row.createCell(5).setCellValue("-");
									}
									row.createCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));

									row.getCell(0).setCellStyle(cellformat_solid);
									row.getCell(1).setCellStyle(cellformat_solid);
									row.getCell(2).setCellStyle(cellformat_solid);
									row.getCell(3).setCellStyle(cellformat_solid);
									row.getCell(4).setCellStyle(cellformat_solid);
									row.getCell(5).setCellStyle(cellformat_solid);
									row.getCell(6).setCellStyle(cellformat_solid);

									row.setHeight((short) 650);
									thirdstartrow++;
								}

							} else if ("TRUST".equals(dirList.get(i).getReg_gubun())) { // 1단계 위탁
								Row row = null;
								if (thirdSheet.getRow(thirdstartrow) == null) {
									row = thirdSheet.createRow(thirdstartrow);
								} else {
									row = thirdSheet.getRow(thirdstartrow);
								}
								row.createCell(0).setCellValue("위탁 실적");
								row.createCell(1).setCellValue(dirList.get(i).getYyyymm());
								if ("RT_STEP1_MANG_N".equals(dirList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("인증망을 이용하지 않은 실적");
								} else if ("RT_TB_ONE_N".equals(dirList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("인증망을 이용하지 않은 실적(택배)");
								} else {
									row.createCell(2).setCellValue("인정 실적");
								}
								row.createCell(3).setCellValue(Util.splitUsrMstKey(dirList.get(i).getCars_reg_num()));
								row.createCell(4).setCellValue("-");
								row.createCell(5).setCellValue("-");
								row.createCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));

								row.getCell(0).setCellStyle(cellformat_solid);
								row.getCell(1).setCellStyle(cellformat_solid);
								row.getCell(2).setCellStyle(cellformat_solid);
								row.getCell(3).setCellStyle(cellformat_solid);
								row.getCell(4).setCellStyle(cellformat_solid);
								row.getCell(5).setCellStyle(cellformat_solid);
								row.getCell(6).setCellStyle(cellformat_solid);

								row.setHeight((short) 650);
								thirdstartrow++;

							} //위탁배차 구분 끝

						} else if ("STEP2".equals(dirList.get(i).getReg_dir_step())) { // 2단계 --------------------------------------------------
							if ("OPER".equals(dirList.get(i).getReg_gubun())) { // 2단계 배차
								if ("CAR_02_D".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_01".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_02".equals(dirList.get(i).getCar_type_final())) {
									//								if(fourthstartrow > 2 && fourthSheet.getRow(fourthstartrow-1).createCell(0).getStringCellValue().equals("")){
									//									fourthstartrow--;
									//								}
								} else {
									Row row = null;
									if (fourthSheet.getRow(fourthstartrow) == null) {
										row = fourthSheet.createRow(fourthstartrow);
									} else {
										row = fourthSheet.getRow(fourthstartrow);
									}
									row.createCell(0).setCellValue("배차 실적");
									row.createCell(1).setCellValue(dirList.get(i).getYyyymm());

									if ("CAR_01_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("인정 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if ("CAR_01_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("직영");
											row.createCell(5).setCellValue("-");
										} else if ("CAR_02_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("지입");
											row.createCell(5).setCellValue("-");
										} else if ("CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("장기용차");
											row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt() + " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
										}
									} else if ("CAR_03_N".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("장기용차 기준회수 미달");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt() + " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
									} else if ("CAR_04_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_04_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_03".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("등록기간 미달(단기용차)");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("단기용차");
										row.createCell(5).setCellValue("-");
									} else if ("CAR_99_N".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("미등록차량 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());										
										row.createCell(4).setCellValue("확인불가");										
										row.createCell(5).setCellValue("-");
									} else if ("CAR_03_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_05".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_06".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_99".equals(dirList.get(i).getCar_type_final())) { //20231012 CAR_03_N_05,06,99 chbaek 미등록차량 실적 - 장기용차에서 장기용차 기한외 실적 - 장기용차로 이동
										row.createCell(2).setCellValue("장기용차 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue("-");
									} else if ("CAR_01_N".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_N".equals(dirList.get(i).getCar_type_final())) {
										row.createCell(2).setCellValue("차량등록 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("확인불가");
										row.createCell(5).setCellValue("-");
									}
									row.createCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));

									row.getCell(0).setCellStyle(cellformat_solid);
									row.getCell(1).setCellStyle(cellformat_solid);
									row.getCell(2).setCellStyle(cellformat_solid);
									row.getCell(3).setCellStyle(cellformat_solid);
									row.getCell(4).setCellStyle(cellformat_solid);
									row.getCell(5).setCellStyle(cellformat_solid);
									row.getCell(6).setCellStyle(cellformat_solid);

									row.setHeight((short) 650);
									fourthstartrow++;
								}
							} else if ("TRUST".equals(dirList.get(i).getReg_gubun())) { // 2단계 위탁

								Row row = null;
								if (fourthSheet.getRow(fourthstartrow) == null) {
									row = fourthSheet.createRow(fourthstartrow);
								} else {
									row = fourthSheet.getRow(fourthstartrow);
								}

								row.createCell(0).setCellValue("위탁 실적");
								row.createCell(1).setCellValue(dirList.get(i).getYyyymm());
								if ("RT_STEP2_MANG_N".equals(dirList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("인증망을 이용하지 않은 실적");
								} else if ("RT_STEP1_MANG_N_OUT".equals(dirList.get(i).getCar_type_final()) || "RT_STEP2_MANG_N_OUT".equals(dirList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("인증망 사업기간 외 이용 실적");
								} else {
									row.createCell(2).setCellValue("인정 실적");
								}
								row.createCell(3).setCellValue(Util.splitUsrMstKey(dirList.get(i).getCars_reg_num()));
								row.createCell(4).setCellValue("-");
								row.createCell(5).setCellValue("-");
								row.createCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));

								row.getCell(0).setCellStyle(cellformat_solid);
								row.getCell(1).setCellStyle(cellformat_solid);
								row.getCell(2).setCellStyle(cellformat_solid);
								row.getCell(3).setCellStyle(cellformat_solid);
								row.getCell(4).setCellStyle(cellformat_solid);
								row.getCell(5).setCellStyle(cellformat_solid);
								row.getCell(6).setCellStyle(cellformat_solid);

								row.setHeight((short) 650);

								fourthstartrow++;
							} //위탁배차 구분 끝

						} //단계 구분 끝

					} // 일반실적 반복문 끝

					for (int i = 0; i < dirTbList.size(); i++) { //택배 실적~~~
						//2022.01.24 jwchoi if문 주석처리 
						/*						if (thirdstartrow > 655330) {
													
													break;
												}*/
						;

						if ("OPER".equals(dirTbList.get(i).getReg_gubun())) { // 1단계 배차
							if ("CAR_02_D".equals(dirTbList.get(i).getCar_type_final())
									|| "CAR_03_D_01".equals(dirTbList.get(i).getCar_type_final())
									|| "CAR_03_D_02".equals(dirTbList.get(i).getCar_type_final())) {
								//							if(thirdstartrow > 2  && thirdSheet.getRow(thirdstartrow-1).createCell(0).getStringCellValue().equals("")){
								//								thirdstartrow--;
								//							}
							} else {
								Row row = null;
								if (thirdSheet.getRow(thirdstartrow) == null) {
									row = thirdSheet.createRow(thirdstartrow);
								} else {
									row = thirdSheet.getRow(thirdstartrow);
								}

								row.createCell(0).setCellValue("배차 실적");
								row.createCell(1).setCellValue(dirTbList.get(i).getYyyymm());

								if ("CAR_01_Y".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_02_Y".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_Y".equals(dirTbList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("인정 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									if ("CAR_01_Y".equals(dirTbList.get(i).getCar_type_final())) {
										row.createCell(4).setCellValue("직영");
										row.createCell(5).setCellValue("-");
									} else if ("CAR_02_Y".equals(dirTbList.get(i).getCar_type_final())) {
										row.createCell(4).setCellValue("지입");
										row.createCell(5).setCellValue("-");
									} else if ("CAR_03_Y".equals(dirTbList.get(i).getCar_type_final())) {
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue(dirTbList.get(i).getOrder_year_cnt() + " / " + dirTbList.get(i).getCar03_cut_oper_year_cnt());
									}
								} else if ("CAR_03_N".equals(dirTbList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("장기용차 기준회수 미달");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("장기용차");
									row.createCell(5).setCellValue(dirTbList.get(i).getOrder_year_cnt() + " / " + dirTbList.get(i).getCar03_cut_oper_year_cnt());
								} else if ("CAR_04_N_01".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_04_N_02".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_03".equals(dirTbList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("등록기간 미달(단기용차)");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("단기용차");
									row.createCell(5).setCellValue("-");
								} else if ("CAR_99_N".equals(dirTbList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("미등록차량 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("확인불가");									
									row.createCell(5).setCellValue("-");
								} else if ("CAR_03_N_01".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_02".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_05".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_06".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_99".equals(dirTbList.get(i).getCar_type_final())) { //20231012 CAR_03_N_05,06,99 chbaek 미등록차량 실적 - 장기용차에서 장기용차 기한외 실적 - 장기용차로 이동
									row.createCell(2).setCellValue("장기용차 기한외 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("장기용차");
									row.createCell(5).setCellValue("-");
								} else if ("CAR_01_N".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_02_N".equals(dirTbList.get(i).getCar_type_final())) {
									row.createCell(2).setCellValue("차량등록 기한외 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("확인불가");
									row.createCell(5).setCellValue("-");
								}
								row.createCell(6).setCellValue(Util.Comma_won(String.valueOf(dirTbList.get(i).getCharge_sum())));

								row.getCell(0).setCellStyle(cellformat_solid);
								row.getCell(1).setCellStyle(cellformat_solid);
								row.getCell(2).setCellStyle(cellformat_solid);
								row.getCell(3).setCellStyle(cellformat_solid);
								row.getCell(4).setCellStyle(cellformat_solid);
								row.getCell(5).setCellStyle(cellformat_solid);
								row.getCell(6).setCellStyle(cellformat_solid);

								row.setHeight((short) 650);

								thirdstartrow++;
							}
						} else if ("TRUST".equals(dirTbList.get(i).getReg_gubun())) { // 1단계 위탁
							Row row = null;
							if (thirdSheet.getRow(thirdstartrow) == null) {
								row = thirdSheet.createRow(thirdstartrow);
							} else {
								row = thirdSheet.getRow(thirdstartrow);
							}

							row.createCell(0).setCellValue("위탁 실적");
							row.createCell(1).setCellValue(dirTbList.get(i).getYyyymm());
							if ("RT_STEP1_MANG_N".equals(dirTbList.get(i).getCar_type_final())) {
								row.createCell(2).setCellValue("인증망을 이용하지 않은 실적");
							} else if ("RT_STEP1_MANG_N_OUT".equals(dirTbList.get(i).getCar_type_final()) || "RT_STEP2_MANG_N_OUT".equals(dirTbList.get(i).getCar_type_final())) {
								row.createCell(2).setCellValue("인증망 사업기간 외 이용 실적");
							} else if ("RT_TB_ONE_N".equals(dirTbList.get(i).getCar_type_final())) {
								row.createCell(2).setCellValue("인증망을 이용하지 않은 실적(택배)");
							} else {
								row.createCell(2).setCellValue("인정 실적");
							}
							row.createCell(3).setCellValue(Util.splitUsrMstKey(dirTbList.get(i).getCars_reg_num()));
							row.createCell(4).setCellValue("-");
							row.createCell(5).setCellValue("-");
							row.createCell(6).setCellValue(Util.Comma_won(String.valueOf(dirTbList.get(i).getCharge_sum())));

							row.getCell(0).setCellStyle(cellformat_solid);
							row.getCell(1).setCellStyle(cellformat_solid);
							row.getCell(2).setCellStyle(cellformat_solid);
							row.getCell(3).setCellStyle(cellformat_solid);
							row.getCell(4).setCellStyle(cellformat_solid);
							row.getCell(5).setCellStyle(cellformat_solid);
							row.getCell(6).setCellStyle(cellformat_solid);

							row.setHeight((short) 650);

							thirdstartrow++;
						} //위탁배차 구분 끝

					} //택배 반복문 끝~~~

					if (thirdstartrow == 1) {
						Row row = null;
						if (thirdSheet.getRow(thirdstartrow) == null) {
							row = thirdSheet.createRow(thirdstartrow);
						} else {
							row = thirdSheet.getRow(thirdstartrow);
						}
						row.createCell(0).setCellValue("1단계 정보가 없습니다.");

						row.getCell(0).setCellStyle(cellformat_solid);

						row.setHeight((short) 650);
					}

					if (fourthstartrow == 1) {
						Row row = null;
						if (fourthSheet.getRow(fourthstartrow) == null) {
							row = fourthSheet.createRow(fourthstartrow);
						} else {
							row = fourthSheet.getRow(fourthstartrow);
						}
						row.createCell(0).setCellValue("2단계 정보가 없습니다.");

						row.getCell(0).setCellStyle(cellformat_solid);

						row.setHeight((short) 650);

					}

				} //직접 상세내역 끝ㅋ

				//3-5. 직접운송 위탁금지위반 상세정보 시트--------------------------------------------------------------------------------------------------
				Sheet Sheet_5 = workbook.getSheetAt(4); // 직접운송 위탁금지위반 상세정보 시트 가져오기

				int sh5_startrow = 3; //시작 row 셋팅
				int index_uu_seq = 1; // 등록단위 셀 병합용
				int index_reg_id = 1; // 계약단위 셀 병합용
				//minList => 직접운송 위탁금지위반 상세정보 쿼리 List로 대체

				if (dirVioList == null || dirVioList.size() == 0) { // DB에 데이터가 없을 때
					Row row = null;
					if (Sheet_5.getRow(sh5_startrow) == null) {
						row = Sheet_5.createRow(sh5_startrow);
					} else {
						row = Sheet_5.getRow(sh5_startrow);
					}
					// 셀병합으로 수정. -2021.12.18 suhyun
					row.createCell(0).setCellValue("직접운송 위탁금지위반 실적이 없습니다.");
					Sheet_5.addMergedRegion(new CellRangeAddress(3, 3, 0, 17));

					row.getCell(0).setCellStyle(cellformat_solid);
					row.getCell(3).setCellStyle(cellformat_solid);
					row.getCell(4).setCellStyle(cellformat_solid);
					row.getCell(5).setCellStyle(cellformat_solid);
					row.getCell(6).setCellStyle(cellformat_solid);
					row.getCell(7).setCellStyle(cellformat_solid);
					row.getCell(8).setCellStyle(cellformat_solid);
					row.getCell(9).setCellStyle(cellformat_solid);
					row.getCell(10).setCellStyle(cellformat_solid);
					row.getCell(11).setCellStyle(cellformat_solid);
					row.getCell(12).setCellStyle(cellformat_solid);
					row.getCell(13).setCellStyle(cellformat_solid);
					row.getCell(14).setCellStyle(cellformat_solid);
					row.getCell(15).setCellStyle(cellformat_solid);
					row.getCell(16).setCellStyle(cellformat_solid);
					row.getCell(17).setCellStyle(cellformat_solid);

					//Row 높이 고정
					row.setHeight((short) 450);
				} else {
					// 데이터 존재
					for (int i = 0; i < dirVioList.size(); i++) {

						//row객체 가져오기
						Row row = null;
						if (Sheet_5.getRow(sh5_startrow) == null) {
							row = Sheet_5.createRow(sh5_startrow);
						} else {
							row = Sheet_5.getRow(sh5_startrow);
						}

						/*
						 * 셀값에 조건 적용 예시
						if("RT_STEP1_MANG_N".equals(dirTbList.get(i).getCar_type_final())){
							row.createCell(2).setCellValue("");
						}else{
							row.createCell(2).setCellValue("");
						}
						*/

						row.createCell(0).setCellValue((dirVioList.get(i).getPg_id().equals("web")) ? "웹" : "연계"); //방식
						row.createCell(1).setCellValue(dirVioList.get(i).getUnit_reg_date()); //등록일
						row.createCell(2).setCellValue(dirVioList.get(i).getAgency_yn().equals("N") ? "미대행" : "대행"); //대행여부
						row.createCell(3).setCellValue(dirVioList.get(i).getUnit_cont_from()); //계약년월
						row.createCell(4).setCellValue(Util.Comma_won(String.valueOf(dirVioList.get(i).getUnit_contract_charge()))); //계약금액
						row.createCell(5).setCellValue(Util.Comma_won(String.valueOf(dirVioList.get(i).getUnit_trust_charge()))); //위탁금액
						row.createCell(6).setCellValue(dirVioList.get(i).getUnit_operate_cnt()); //배차횟수
						row.createCell(7).setCellValue(Util.Comma_won(String.valueOf(dirVioList.get(i).getUnit_operate_charge()))); //배차금액
						row.createCell(8).setCellValue(Util.splitUsrMstKey(dirVioList.get(i).getClient_comp_bsns_num())); //계약의뢰자 사업자번호
						row.createCell(9).setCellValue(dirVioList.get(i).getC_cont_from()); //계약년월
						row.createCell(10).setCellValue(Util.Comma_won(String.valueOf(dirVioList.get(i).getC_charge()))); //계약금액
						row.createCell(11).setCellValue(Util.splitUsrMstKey(dirVioList.get(i).getTrust_comp_bsns_num())); //위탁 위반 사업자번호
						row.createCell(12).setCellValue(Util.Comma_won(String.valueOf(dirVioList.get(i).getT_charge()))); //위탁 위반 금액

						row.createCell(13).setCellValue("");

						row.createCell(14).setCellValue(dirVioList.get(i).getSido_nm_2016()); //시도
						row.createCell(15).setCellValue(dirVioList.get(i).getSigungu_nm_2016()); //시군구
						row.createCell(16).setCellValue(dirVioList.get(i).getComp_cls()); //업체명
						row.createCell(17).setCellValue(dirVioList.get(i).getName()); //업체명

						//Cell Style 적용 - 테두리, 가로/세로 가운데정렬
						row.getCell(0).setCellStyle(cellformat_solid);
						row.getCell(1).setCellStyle(cellformat_solid);
						row.getCell(2).setCellStyle(cellformat_solid);
						row.getCell(3).setCellStyle(cellformat_solid);
						row.getCell(4).setCellStyle(cellformat_solid);
						row.getCell(5).setCellStyle(cellformat_solid);
						row.getCell(6).setCellStyle(cellformat_solid);
						row.getCell(7).setCellStyle(cellformat_solid);
						row.getCell(8).setCellStyle(cellformat_solid);
						row.getCell(9).setCellStyle(cellformat_solid);
						row.getCell(10).setCellStyle(cellformat_solid);
						row.getCell(11).setCellStyle(cellformat_solid);
						row.getCell(12).setCellStyle(cellformat_solid);
						row.getCell(13).setCellStyle(cellformat_solid);
						row.getCell(14).setCellStyle(cellformat_solid);
						row.getCell(15).setCellStyle(cellformat_solid);
						row.getCell(16).setCellStyle(cellformat_solid);
						row.getCell(17).setCellStyle(cellformat_solid);

						//Row 높이 고정
						row.setHeight((short) 450);

						sh5_startrow++;

						if (index_uu_seq == dirVioList.get(i).getUu_seq_cnt()) {
							if (index_uu_seq != 1) { // 셀병합작업
								int index_base_data = (i + 4 - dirVioList.get(i).getUu_seq_cnt()) + 1;// 병합대상 기준 데이터 위치
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 0, 0)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 1, 1)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 2, 2)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 3, 3)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 4, 4)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 5, 5)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 6, 6)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 7, 7)); // 업체정보 셀병합
							}
							index_uu_seq = 1;
						} else {
							index_uu_seq++;
						}
						if (index_reg_id == dirVioList.get(i).getReg_id_cnt()) {
							if (index_reg_id != 1) { // 셀병합작업
								int index_base_data = (i + 4 - dirVioList.get(i).getReg_id_cnt()) + 1;// 병합대상 기준 데이터 위치
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 8, 8)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 9, 9)); // 업체정보 셀병합
								Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data, (i + 4), 10, 10)); // 업체정보 셀병합
							}
							index_reg_id = 1;
						} else {
							index_reg_id++;
						}
					}
				}

				out = new FileOutputStream(new File(excelFile));
				workbook.write(out);

				JSONObject json = new JSONObject();

				json.put("fileCls", "99");
				json.put("fileName", excelFileName);

				pout = response.getWriter();

				pout.write(json.toString());

			} catch (FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} catch (OutOfMemoryError e) {
				logger.error("[ERROR] - OutOfMemoryError : ", e);
			} finally {
				if (inputStream != null)
					try {
						inputStream.close();
					} catch (IOException e) {
						logger.error("[ERROR] - IOException : ", e);
					}
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						logger.error("[ERROR] - IOException : ", e);
					}
				if (pout != null)
					pout.close();
			}
		}
	}

	@RequestMapping("/stat/FpisStatTransDetail_excel_detail_min.do")
	public void FpisStatTransDetail_excel_detail_min(FpisAdminStatTrans4VO shVO,
			HttpServletRequest req,
			HttpServletResponse response) throws Exception, NullPointerException {
		int search_year = Integer.parseInt(req.getParameter("search_year"));

		if (search_year >= 2017) {
			//17년도 양식 start============================================================================================================================

			// 신고자정보 불러오기 -> 새양식의 실적신고여부 확인하기위해서
			FpisAdminStatBase12VO shVO12 = new FpisAdminStatBase12VO();
			shVO12.setSearch_year(shVO.getSearch_year());
			shVO12.setSearch_bungi("60");
			shVO12.setSearch_type("R");
			String sTableName = adminStatSvc.getSearchTableName(shVO12);
			shVO.setSearch_table_name(sTableName);

			//sheet별 리스트들 가져오는 곳
			FpisAdminStatTrans4VO summaryVO = FpisSvc.selectMinDirSummary_2017(shVO); // 총괄
			List<FpisAdminStatTrans8VO> minList = FpisSvc.selectMinDetailList(shVO); //최소운송 차량 상세정보
			/*	        FpisAdminStatTrans4VO dirSummaryVO = FpisSvc.selectDirSummary(shVO); //직접운송 금액 상세 총괄
						List<FpisAdminStatTrans8VO> dirList = FpisSvc.selectDirDetailList(shVO); //직접운송1단계
						List<FpisAdminStatTrans8VO> dirTbList = FpisSvc.selectDirTbDetailList(shVO); //직접운송2단계
						List<FpisAdminStatTrans4VO> dirVioList = FpisSvc.selectDirVioList(shVO); //직접운송 위탁금지위반 상세정보
			*/
			int otherCompCarCnt = summaryVO.getMin_other_comp_car_cnt();
			PrintWriter pout = null;
			FileOutputStream out = null;
			FileInputStream inputStream = null;

			//1.템플릿 파일 복사 17년도 파일로
			String makingDtm = Util.getDateFormat2();
			String excelFileName = makingDtm + "_" + shVO.getUsr_mst_key() + "_" + shVO.getSearch_year() + ".xlsx";

			String excelFileSize = "";

			excelFileSize = "_2017_min";

			moveToUploadDirectory(excelFileName, excelFileSize);

			//2.복사한 파일 메모리 로드
			//String excelFile = EgovProperties.getProperty("Globals.majarStatFilePath") + File.separator + excelFileName;
			String excelFile = majarStatFilePath + File.separator + excelFileName;
			try {
				inputStream = new FileInputStream(new File(excelFile));
				//2022.10.26 jwchoi HSSFWorkbook > XSSFWorkbook
				Workbook workbook = new XSSFWorkbook(inputStream);

				CellStyle wrapCellStyle = workbook.createCellStyle();
				wrapCellStyle.setWrapText(true);

				CellStyle cellformat_solid = workbook.createCellStyle();
				cellformat_solid.setBorderTop(CellStyle.BORDER_THIN);
				cellformat_solid.setBorderRight(CellStyle.BORDER_THIN);
				cellformat_solid.setBorderLeft(CellStyle.BORDER_THIN);
				cellformat_solid.setBorderBottom(CellStyle.BORDER_THIN);
				cellformat_solid.setAlignment(CellStyle.ALIGN_CENTER);
				cellformat_solid.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

				//3.데이터 채우기

				//3-1. 총괄--------------------------------------------------------------------------------------------------
				Sheet firstSheet = workbook.getSheetAt(0); //첫번째 시트 가져오기

				//기본정보---------------
				firstSheet.getRow(2).getCell(0).setCellValue(Util.splitUsrMstKey(summaryVO.getUsr_mst_key())); //사업자번호
				firstSheet.getRow(2).getCell(1).setCellValue(summaryVO.getComp_nm()); //업체명
				firstSheet.getRow(2).getCell(2).setCellValue(summaryVO.getCeo()); //대표자
				firstSheet.getRow(2).getCell(3).setCellValue(summaryVO.getComp_cls_detail()); //업종 및 업태
				firstSheet.getRow(2).getCell(4).setCellValue(summaryVO.getBase_year() + "년"); //분석 기준연도
				firstSheet.getRow(2).getCell(5).setCellValue("수정마감기한 기준 신고데이터"); //분석 기준시점
				firstSheet.getRow(2).getCell(6).setCellValue("basic".equals(summaryVO.getIs_reg()) ? "실적신고" : "no_record".equals(summaryVO.getIs_reg()) ? "실적없음 신고" : "신고된 실적 없음"); //실적신고 여부

				//최소운송기준제-----------
				firstSheet.getRow(2).getCell(7).setCellValue(summaryVO.getMin_result()); //최소운송기준 위반여부
				firstSheet.getRow(2).getCell(8).setCellValue(summaryVO.getMin_not_percent()); //미이행률
				firstSheet.getRow(2).getCell(9).setCellValue(Util.Comma_won(summaryVO.getMin_charge())); //최소운송기준 준수 필요금액
				firstSheet.getRow(2).getCell(10).setCellValue(Util.Comma_won(summaryVO.getMin_cont_charge())); //실적금액(계약금액)

				//3-2. 최소운송 상세내역--------------------------------------------------------------------------------------------------
				Sheet secondSheet = workbook.getSheetAt(1); // 두번째 시트 가져오기

				int startrow = 1; //시작 row 셋팅
				if (minList == null || minList.size() == 0) { // 시트에 데이터가 없을 때
					secondSheet.createRow(startrow).createCell(0).setCellValue("자사 지입차량이 없습니다.");
				} else {
					// 데이터 존재
					for (int i = 0; i < minList.size(); i++) {
						Row row = null;
						if (secondSheet.getRow(startrow) == null) {
							row = secondSheet.createRow(startrow);
						} else {
							row = secondSheet.getRow(startrow);
						}

						row.setHeightInPoints((2 * secondSheet.getDefaultRowHeightInPoints()));

						int iPlusOne = (i == minList.size() - 1) ? i : i + 1;
						int iMinusOne = (i == 0) ? 0 : i - 1;

						if ("my".equals(minList.get(i).getCompany_kind())) {
							if (minList.get(i).getCars_reg_num().equals(minList.get(iMinusOne).getCars_reg_num())
									&& minList.get(i).getCars_kind().equals(minList.get(iMinusOne).getCars_kind())
									&& minList.get(i).getCars_size().equals(minList.get(iMinusOne).getCars_size())
									&& i != 0) {
								startrow--;

								if (secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								} else {
									row = secondSheet.getRow(startrow);
								}

								row.createCell(5).setCellValue(row.getCell(5).getStringCellValue() + "\n" + minList.get(i).getS_date() + "~" + minList.get(i).getE_date());
								row.createCell(5).setCellStyle(cellformat_solid);
							} else {
								row.createCell(0).setCellValue("자사 지입 차량");
								row.createCell(1).setCellValue(minList.get(i).getCars_reg_num());
								row.createCell(2).setCellValue(Util.splitUsrMstKey(minList.get(i).getUsr_mst_key()));
								row.createCell(3).setCellValue(convertCarsKind(minList.get(i).getCars_kind()) + "(" + minList.get(i).getCars_size() + ")");
								row.createCell(4).setCellValue(minList.get(i).getCar_year_days() + "일");
								row.createCell(5).setCellValue(minList.get(i).getS_date() + "~" + minList.get(i).getE_date());
								row.createCell(6).setCellValue(Util.Comma_won(minList.get(i).getCar_min_value_days_20per()) + "원(" + minList.get(i).getComp_all_144() + ")");
								row.createCell(7).setCellValue(("Y".equals(minList.get(i).getCarmin_flag())) ? "제외(" + minList.get(i).getOrder_cnt_car03_total() + ")" : "미제외(" + minList.get(i).getOrder_cnt_car03_total() + ")");

								row.getCell(0).setCellStyle(cellformat_solid);
								row.getCell(1).setCellStyle(cellformat_solid);
								row.getCell(2).setCellStyle(cellformat_solid);
								row.getCell(3).setCellStyle(cellformat_solid);
								row.getCell(4).setCellStyle(cellformat_solid);
								row.getCell(5).setCellStyle(cellformat_solid);
								row.getCell(6).setCellStyle(cellformat_solid);
								row.getCell(7).setCellStyle(cellformat_solid);
							}

							if ((!minList.get(iPlusOne).getCars_reg_num().equals(minList.get(i).getCars_reg_num()) && otherCompCarCnt != 0) || i == iPlusOne) {
								startrow++;
								if (secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								} else {
									row = secondSheet.getRow(startrow);
								}
								row.createCell(0).setCellValue("타사에서 장기용차로 이용되지 않았습니다.");

								row.createCell(2).setCellValue("");
								row.createCell(3).setCellValue("");
								row.createCell(4).setCellValue("");
								row.createCell(5).setCellValue("");
								row.createCell(6).setCellValue("");
								row.createCell(7).setCellValue("");

								row.getCell(0).setCellStyle(cellformat_solid);

								row.getCell(2).setCellStyle(cellformat_solid);
								row.getCell(3).setCellStyle(cellformat_solid);
								row.getCell(4).setCellStyle(cellformat_solid);
								row.getCell(5).setCellStyle(cellformat_solid);
								row.getCell(6).setCellStyle(cellformat_solid);
								row.getCell(7).setCellStyle(cellformat_solid);

								row.setHeight((short) 500);

								startrow++;
								if (secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								} else {
									row = secondSheet.getRow(startrow);
								}
								row.createCell(0).setCellValue("");
								row.createCell(1).setCellValue("");
								row.createCell(2).setCellValue("");
								row.createCell(3).setCellValue("");
								row.createCell(4).setCellValue("");
								row.createCell(5).setCellValue("");
								row.createCell(6).setCellValue("");
								row.createCell(7).setCellValue("");

								row.getCell(0).setCellStyle(cellformat_solid);
								row.getCell(1).setCellStyle(cellformat_solid);
								row.getCell(2).setCellStyle(cellformat_solid);
								row.getCell(3).setCellStyle(cellformat_solid);
								row.getCell(4).setCellStyle(cellformat_solid);
								row.getCell(5).setCellStyle(cellformat_solid);
								row.getCell(6).setCellStyle(cellformat_solid);
								row.getCell(7).setCellStyle(cellformat_solid);

								row.setHeight((short) 50);
							}
						} else {
							row.createCell(0).setCellValue("타사 장기용차 차량");
							row.createCell(1).setCellValue(minList.get(i).getCars_reg_num());
							row.createCell(2).setCellValue(Util.splitUsrMstKey(minList.get(i).getUsr_mst_key()));
							row.createCell(3).setCellValue("-");
							row.createCell(4).setCellValue("-");
							row.createCell(5).setCellValue("-");
							row.createCell(6).setCellValue(minList.get(i).getComp_one_cnt() + "(" + minList.get(i).getComp_one_96() + ")");
							row.createCell(7).setCellValue("");

							row.getCell(0).setCellStyle(cellformat_solid);
							row.getCell(1).setCellStyle(cellformat_solid);
							row.getCell(2).setCellStyle(cellformat_solid);
							row.getCell(3).setCellStyle(cellformat_solid);
							row.getCell(4).setCellStyle(cellformat_solid);
							row.getCell(5).setCellStyle(cellformat_solid);
							row.getCell(6).setCellStyle(cellformat_solid);
							row.getCell(7).setCellStyle(cellformat_solid);

							if (("my".equals(minList.get(iPlusOne).getCompany_kind()) && otherCompCarCnt != 0) || i == iPlusOne) {
								startrow++;
								if (secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								} else {
									row = secondSheet.getRow(startrow);
								}
								row.createCell(0).setCellValue("");
								row.createCell(1).setCellValue("");
								row.createCell(2).setCellValue("");
								row.createCell(3).setCellValue("");
								row.createCell(4).setCellValue("");
								row.createCell(5).setCellValue("");
								row.createCell(6).setCellValue("");
								row.setHeight((short) 50);
							}
						}
						startrow++;

					}

					if (otherCompCarCnt == 0) {
						Row row = null;
						if (secondSheet.getRow(startrow) == null) {
							row = secondSheet.createRow(startrow);
						} else {
							row = secondSheet.getRow(startrow);
						}
						row.createCell(0).setCellValue("타사에서 장기용차로 이용된 차량이 1대도 없습니다.");
						startrow++;
					}

				}

				secondSheet.autoSizeColumn((short) 2);

				out = new FileOutputStream(new File(excelFile));
				workbook.write(out);

				JSONObject json = new JSONObject();
				json.put("fileCls", "99");
				json.put("fileName", excelFileName);

				pout = response.getWriter();

				pout.write(json.toString());

			} catch (FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				if (inputStream != null)
					try {
						inputStream.close();
					} catch (IOException e) {
						logger.error("[ERROR] - IOException : ", e);
					}
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						logger.error("[ERROR] - IOException : ", e);
					}
				if (pout != null)
					pout.close();
			}

		}
	}

	private synchronized void moveToUploadDirectory(String excelFileName, String excelFileSize) {
		// package안에 들어간 템플릿 삭제 및 경로 수정, Globals.properties, majarStatFilePath - 2021.12.06 suhyun
		// 2022.01.24 jwchoi, XSSFWork 사용을 위해 xls > xlsx
		String fileName = "majar_stat_live_template"+excelFileSize+".xlsx";
		//majarStatFilePath += File.separator;
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel fcin = null;
		FileChannel fcout = null;

		File fileInSwap = new File(majarStatFilePath, fileName);

		try {
			fis = new FileInputStream(fileInSwap);
			fos = new FileOutputStream(majarStatFilePath + excelFileName);

			fcin = fis.getChannel();
			fcout = fos.getChannel();

			long size = fcin.size();
			fcin.transferTo(0, size, fcout);

		} catch (FileNotFoundException e) {
			logger.error("[ERROR] - FileNotFoundException : ", e);
		} catch (IOException e) {
			logger.error("[ERROR] - IOException : ", e);
		} finally {
			if (fcout != null)
				try {
					fcout.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			if (fcin != null)
				try {
					fcin.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			//if(fileInSwap.exists()) fileInSwap.delete();
		}
	}

	private String convertCarsKind(String carskind) {
		if ("01".equals(carskind)) {
			return "일반형";
		} else if ("02".equals(carskind)) {
			return "덤프형";
		} else if ("03".equals(carskind)) {
			return "밴형";
		} else if ("04".equals(carskind)) {
			return "(특수용도형) 청소차";
		} else if ("05".equals(carskind)) {
			return "(특수용도형) 살수차";
		} else if ("06".equals(carskind)) {
			return "(특수용도형) 냉장,냉동차";
		} else if ("07".equals(carskind)) {
			return "(특수용도형) 곡물,사료운반";
		} else if ("08".equals(carskind)) {
			return "(특수용도형) 유조차";
		} else if ("09".equals(carskind)) {
			return "(특수용도형) 탱크로리";
		} else if ("10".equals(carskind)) {
			return "(특수용도형) 기타 - 그 외";
		} else if ("11".equals(carskind)) {
			return "(특수자동차) 구난형";
		} else if ("12".equals(carskind)) {
			return "(특수자동차) 견인형";
		} else if ("13".equals(carskind)) {
			return "(특수자동차) 특수작업형";
		} else if ("14".equals(carskind)) {
			return "(특수용도형) 노면청소자";
		} else if ("15".equals(carskind)) {
			return "(특수용도형) 소방차";
		} else if ("16".equals(carskind)) {
			return "(특수용도형) 피견인차";
		} else if ("17".equals(carskind)) {
			return "(특수용도형) 기타 - 사다리";
		} else if ("18".equals(carskind)) {
			return "(특수용도형) 가타 - 크레인";
		} else if ("19".equals(carskind)) {
			return "(특수용도형) 기타 - 고소작업대";
		} else
			return "확인불가";
	}
}
