package fpis.reg.unit;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fpis.common.service.CommonGetInfoService;
import fpis.common.utils.FpisConstants;
import fpis.common.utils.Util;
import fpis.common.utils.Util_poi;
import fpis.common.vo.SessionVO;
import fpis.common.vo.usr.UsrInfoVO;
import fpis.online.stdinfo.car.service.FpisCarManageService;
import fpis.online.stdinfo.car.service.FpisCarManageVO;
import fpis.reg.RegVO;
import fpis.reg.mass.service.MassOrderService;
import twitter4j.internal.org.json.JSONArray;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.org.json.JSONObject;
/*
 * '실적 단위' 관련 컨트롤러
 * @auther : ysw
 * @history : 2020.08.18 생성
 * */
@SuppressWarnings({"rawtypes", "unchecked"})
@Controller
public class UnitController {
	// 로그 생성
	private static final Logger logger = Logger.getLogger(UnitController.class);

	@Value(value="#{fpis['FPIS.domain']}")
    private String program_domain;

	// 기본정보 얻어오기 공통 서비스
	@Resource(name = "CommonGetInfoService")
	private CommonGetInfoService commonGetInfoService;

	// 계약단위 공통 서비스
	@Resource(name = "UnitService")
	private UnitService unitService;

	@Resource(name = "FpisCarManageService")
	private FpisCarManageService CarManageService;

	// 대량실적신고 등록 서비스
	@Resource(name = "MassOrderService")
	private MassOrderService massOrderService;

	/* 2020. 08. 18 written by ysw 실적등록::연도선택 페이지 */
    @RequestMapping("/reg/unit/FpisOrderRegist_intro.do")
    public String fpisOrderRegister_intro(@ModelAttribute RegVO vo,
    		HttpServletRequest req, ModelMap model){

    	/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

	    SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
	    model.addAttribute("usr_mst_key",svo.getUsr_mst_key());

	    //2020.08.24 ysw 공통함수로 현재 사용자정보  [사업자 유형(1대사업자인지) 포함] 가져온다.
  		HashMap UsrInfo = commonGetInfoService.selectUsrInfoByUsrMstKey(svo.getUsr_mst_key());
  		model.addAttribute("UsrInfo",UsrInfo);

	    //2016. 07. 19 written by dyahn 리다이렉트 파리미터(실적없음 확인/해제 처리플래그)
	    if(vo.getResult() != null && !vo.getResult().equals("")) {
	    	model.addAttribute("RESULT", vo.getResult());
	    }

		//2016. 01. 22 written by dyahn 대행시 실적주체 업체명 가져오기
		model.addAttribute("PRICOMP_NAME", UsrInfo.get("NAME"));



	    Calendar c = Calendar.getInstance(); //객체 생성 및 현재 일시분초...셋팅
	    int currentYear = c.get(Calendar.YEAR);
	    int currentMonth = c.get(Calendar.MONTH)+1;

	    model.addAttribute("VO", vo);
	    model.addAttribute("currentYear", currentYear);
	    model.addAttribute("currentMonth", currentMonth);
	    model.addAttribute("rcode", req.getParameter("rcode"));
	    model.addAttribute("bcode", req.getParameter("bcode"));
	    return "/fpis/reg/unit/FpisOrderRegist_intro";
	}

    //여기
    /**
	 * 2018.03.12 pes 수정허가 유효성검사 및 지난연도 실적등록 불가능 추가
	 * 2020.08.19 ysw reg/unit에 맞게 수정처리함
	 **/
    @RequestMapping(value="/reg/unit/FpisOrderRegist_getCountQuarterRecord.do", method=RequestMethod.POST)
    public void FpisOrderRegist_getCountQuarterRecord(@ModelAttribute RegVO vo,
                                      HttpServletResponse res,
                                      HttpServletRequest req,
                                      Model model){

    	/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return;
		}

    	PrintWriter out = null;
    	try {
	    	//2018.03.12 pes 수정허가 여부 판단 START
	    	String updPermission = "";
	    	String[] cur_date = Util.getDateFormat().split("-");
	    	if(Integer.parseInt(cur_date[1]) > 3 &&  Integer.parseInt(cur_date[1]) < 7 ){		//4월부터 6월에만 검증(나머지는 빈값)

	    		//2021.04.01 ysw 기존 vo를 변경해버리면 안되는걸 확인... tmp 만들어서 결과를 가져오도록 합니다.
	    		RegVO tmpRegVo = new RegVO();
	    		tmpRegVo.setUsr_mst_key(vo.getUsr_mst_key());
	    		tmpRegVo.setBase_year(Integer.toString((Integer.parseInt(cur_date[0])-1)) );
	    		//vo.setBase_year(Integer.toString((Integer.parseInt(cur_date[0])-1)) );
				updPermission = unitService.checkUpdPermission(tmpRegVo);
	    	}
	    	//END



	        vo.setFrom_date_unit(req.getParameter("from_date_unit"));
	        vo.setTo_date_unit(req.getParameter("to_date_unit"));
	        vo.setSearch_company_unit("Y");


	        int record_cnt = unitService.countOrderRegistUnitCase2(vo);


	        //2016. 07. 19 written by dyahn
	        //record_flagCnt > 0 : 실적없음확인 이력 존재
	        //record_flagCnt = 0 : 실적없음확인 이력 없음
	        int record_flagCnt = 0;

	        //여기
            record_flagCnt = unitService.countRegNoRecordFlag(vo);
            if(record_flagCnt > 0){
            	vo = unitService.selectQuarterRecordEmpty(vo);
            }

	        JSONObject json = new JSONObject();
	        json.put("record_cnt", record_cnt);
	        json.put("record_flagCnt", record_flagCnt);
	        json.put("record_flag", vo.getRecord_flag());
	        json.put("record_seq", vo.getRecord_seq());
	        json.put("reg_date", vo.getReg_date());
	        json.put("upd_date", vo.getUpd_date());
	        json.put("updPermission", updPermission);  // 2017_Y, 2017_N
			json.put("updPermissionYear", Integer.parseInt(cur_date[0])-1);

	        json.put("mm", Integer.parseInt(cur_date[1]));

	        res.setContentType("application/json");
	        res.setCharacterEncoding("UTF-8");
			out = res.getWriter();

	        out.write(json.toString());

    	}catch(NumberFormatException e) {
    		logger.error("[ERROR] - NumberFormatException : ", e);
		}catch(JSONException e) {
			logger.error("[ERROR] - JSONException : ", e);
		}catch(IOException e) {
			logger.error("[ERROR] - IOException : ", e);
		}finally{
			if(out != null) out.close();
		}
    }

    /*실적없음 등록*/
    @RequestMapping("/reg/unit/FpisOrderRegist_quarterRecordEmpty.do")
    public String FpisOrderRegist_recordEmpty(@ModelAttribute RegVO vo,
                                      HttpServletResponse res,
                                      HttpServletRequest req,
                                      Model model){
    	SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
	    int result = -1;

	    // xss 조치로인해 confirm이 x-confirm로 치환되는 현상으로 추가 - 2021.09.23 suhyun
	    if(vo.getRecord_flag().equals("empty_x-confirm")) {
	    	vo.setRecord_flag("empty_confirm");
	    }

	    try {
	    	if(vo.getRecord_flag().equals("empty_confirm")){
	    		int flagcnt = unitService.countRegNoRecordFlag(vo);

	    		if(flagcnt == 0 ){
	    			vo.setReg_user(svo.getUser_id());
	    			unitService.insertQuarterRecordEmpty(vo);
                }
                else if(flagcnt > 0){
                	vo.setUpd_user(svo.getUser_id());
                	unitService.updateQuarterRecordEmpty(vo);
                }
                result = 0;
	        } else if(vo.getRecord_flag().equals("empty_cancel")){
	        	vo.setUpd_user(svo.getUser_id());
	        	unitService.updateQuarterRecordEmpty(vo);
                result = 0;
	        }

		}catch (NoSuchMethodError e) {
	        logger.error("[ERROR] - NoSuchMethodError : ", e);
	    }catch (IllegalAccessError e) {
	        logger.error("[ERROR] - IllegalAccessError : ", e);
	    }
	    String chk_massPage = req.getParameter("chk_mass_page");
	    model.addAttribute("result", result);
	    model.addAttribute("rcode", req.getParameter("rcode"));
	    model.addAttribute("bcode", req.getParameter("bcode"));
	    if ("Y".equals(chk_massPage)) {
	    	return "redirect:/reg/unit/FpisOrderMassRegist_intro.do";
	    } else {
	    	return "redirect:/reg/unit/FpisOrderRegist_intro.do";	
	    }
    }

    /**
	 * 2018.12.13 pes 실적신고 제한
	 * 2020.08.21 ysw 실적신고 제한 - 정리
	 **/
    @RequestMapping(value="/reg/unit/FpisOrderRegist_getRegLimit.do", method=RequestMethod.POST)
    public void FpisOrderRegist_getRegLimit(@ModelAttribute RegVO vo, HttpServletResponse res, HttpServletRequest req, Model model) throws Exception, JSONException{


    	/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return;
		}

    	//사용자정보 얻어와서 넣어줍니다.
		HashMap UsrInfo = commonGetInfoService.selectUsrInfoByUsrMstKey(vo.getUsr_mst_key());
    	String comp_cls_detail = (String) UsrInfo.get("COMP_CLS_DETAIL");
    	String result = "";
    	String compCls01 = "";
    	String compCls02 = "";
    	String compCls03 = "";

        String[] strCCD = comp_cls_detail.split(",");

        for(int i=0; i<strCCD.length; i++){
                 if(strCCD[i].equals("01-01")){ compCls01 += "일반화물운송,"; }
            else if(strCCD[i].equals("01-02")){ compCls01 += "개별화물운송,"; }
            else if(strCCD[i].equals("01-03")){ compCls01 += "용달화물운송,"; }
            else if(strCCD[i].equals("01-04") && !compCls01.contains("택배,")){ compCls01 += "택배,"; }
            else if(strCCD[i].equals("02-01")){ compCls02 += "일반주선"; }
            else if(strCCD[i].equals("04-01")){ compCls03 += "가맹사업자"; }

        }

        JSONObject json = new JSONObject();
        JSONArray jarr = new JSONArray();
		result = unitService.selectRegDivision(vo);
		if(!("01").equals(result)){
			if("02-01".equals(comp_cls_detail) ||"02-01,02-02".equals(comp_cls_detail) || "02-01,04-01".equals(comp_cls_detail) ||"02-01,05".equals(comp_cls_detail)||"04-01".equals(comp_cls_detail)|| "02-01,04-01".equals(comp_cls_detail)){
	    		result = unitService.selectRegLimitSunJ(vo);
	    	}else if("01-02".equals(comp_cls_detail)){
	    		result = unitService.selectRegLimitSunGu(vo);
	    	}else if("05".equals(comp_cls_detail) || "06".equals(comp_cls_detail) || "07".equals(comp_cls_detail)){
	    		result = "99";
	    	}else{
	    		result = unitService.selectRegLimitCDUG(vo);
	    	}
		}else{
			//2019.01.23 pes 분산신고 대상자 정보 조회
			List<RegVO> control_list = unitService.selectRegControlByUsr(vo.getUsr_mst_key());
			for(int i = 0; i < control_list.size(); i++){
				JSONObject obj = new JSONObject();
				obj.put("from_ctrl_date", control_list.get(i).getFrom_ctrl_date());
				obj.put("to_ctrl_date", control_list.get(i).getTo_ctrl_date());
				obj.put("ctrl_day", control_list.get(i).getCtrl_day());
				obj.put("ctrl_time", control_list.get(i).getCtrl_time());
				jarr.put(obj);
			}
			json.put("list",jarr);
		}

        json.put("result", result);
        json.put("compCls01", compCls01);
        json.put("compCls02", compCls02);
        json.put("compCls03", compCls03);


        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out;
        out = res.getWriter();

        out.write(json.toString());
        out.close();
    }

    /* 2021.01.11 ysw 보안취약점 METHOD POST 만 가능하도록 수정 */
    @RequestMapping(value="/reg/unit/FpisOrderList_unit.do")
	public String FpisOrderList_unit(RegVO vo,HttpServletRequest req,ModelMap model){

    	/*2021.05.27 lsa 실적신고없음 안내 추가*/

    	//record_flagCnt > 0 : 실적없음확인 이력 존재
        //record_flagCnt = 0 : 실적없음확인 이력 없음
    	RegVO vo1 = new RegVO(); // 실적없음 확인 VO
    	RegVO validateVo = new RegVO(); //vo1을 위한 회원확인, 년도를 담는 VO
    	UsrInfoVO usrInfoVO = new UsrInfoVO();
    	if(usrInfoVO.getComp_cls() == "04" || usrInfoVO.getComp_cls() == "05" || usrInfoVO.getComp_cls() == "06" || usrInfoVO.getComp_cls() == "07"){
    		String pricomp_bsns_num_unit1 = validateVo.getPricomp_bsns_num_unit();
    		validateVo.setUsr_mst_key(pricomp_bsns_num_unit1);
    	} else {
    		String usr_mst_key1 = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
    		validateVo.setUsr_mst_key(usr_mst_key1);
    	}

    	validateVo.setRecord_year(req.getParameter("base_year"));

    	int record_flagCnt = unitService.countRegNoRecordFlag(validateVo);
        if(record_flagCnt > 0){
        	vo1 = unitService.selectQuarterRecordEmpty(validateVo);
        }
        model.addAttribute("vo1", vo1);
        /*2021.05.27 lsa 실적신고없음 안내 종료*/



    	/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}


    	/*2020.10.23 ysw ★★★★대행실적 등록후 이 메뉴로 오는 경우 넘어온 usr_mst_key를 대행 검색조건으로 해줘야한다. */
    	if("Y".equals(vo.getAgency_yn())) {
    		vo.setPricomp_bsns_num(vo.getUsr_mst_key());
    		vo.setSearch_company_unit("N");
    	}

    	SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
    	String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
		String BCODE = req.getParameter("bcode");
		String RCODE = req.getParameter("rcode");
    	vo.setUsr_mst_key(usr_mst_key);

    	//2020.08.24 ysw 공통함수로 현재 사용자정보[대행도포함임]  [사업자 유형(1대사업자인지) 포함] 가져온다.
		HashMap UsrInfo = commonGetInfoService.selectUsrInfoByUsrMstKey(usr_mst_key);
		UsrInfo.put("yyyy", vo.getSelectedQuarterValue());
		model.addAttribute("UsrInfo", UsrInfo);


    	/*2016.01.26. mwchoi 사용자 아이디 부여*/
		vo.setReg_user(svo.getUser_id());

		int strYear = 2017;
        int endYear = Calendar.getInstance().get(Calendar.YEAR);

        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

		//실적년도
		String base_year = req.getParameter("base_year");
		if(vo.getBase_year() == null || vo.getBase_year().equals("")){
			String[] cur_date = Util.getDateFormat().split("-");
			vo.setBase_year("P"+cur_date[0]);
		}else{
			if(vo.getBase_year().contains("P")){
				vo.setBase_year(base_year);
			}else{
				vo.setBase_year("P"+base_year);
			}
		}

		/* 기간검색 */
		String search_kind_unit = req.getParameter("search_kind_unit");
		if(vo.getSearch_kind_unit() == null || vo.getSearch_kind_unit().equals("")) {
			if(search_kind_unit != null && !search_kind_unit.equals("")){
				vo.setSearch_kind_unit(search_kind_unit);
			}else{
				vo.setSearch_kind_unit("1"); //기본값은 등록일자로 세팅
			}
		}

		String from_date_unit = req.getParameter("from_date_unit");
		String to_date_unit = req.getParameter("to_date_unit");
		if(vo.getFrom_date() == null || vo.getFrom_date().equals("")) {
			if(from_date_unit != null && !from_date_unit.equals("")){
				vo.setFrom_date_unit(from_date_unit);
			}
			else{
				vo.setFrom_date_unit(Util.getMonthFirstDay());
			}
		}

		if(vo.getTo_date() == null || vo.getTo_date().equals("")) {
			if(to_date_unit != null && !to_date_unit.equals("")){
				vo.setTo_date_unit(to_date_unit);
			}
			else{
				vo.setTo_date_unit(Util.getCurDate(0,0));
			}
		}

		//등록방식
		String pg_id_unit = req.getParameter("pg_id_unit");
		if(vo.getPg_id_unit() == null || vo.getPg_id_unit().equals("")){
			if(pg_id_unit != null && !pg_id_unit.equals("")){
				vo.setPg_id_unit(pg_id_unit);
			}
		}

		//등록여부
		String regist_code_unit = req.getParameter("regist_code_unit");
		model.addAttribute("regist_code_unit", regist_code_unit);

		//대행여부
		String agency_yn_unit = req.getParameter("agency_yn_unit");
		if(vo.getAgency_yn() == null || vo.getAgency_yn().equals("")){
			if(agency_yn_unit != null && !agency_yn_unit.equals("")){
				vo.setAgency_yn_unit(agency_yn_unit);
			}
		}

		//확정여부
		String confirm_yn_unit = req.getParameter("confirm_yn_unit");
		if(vo.getConfirm_yn() == null || vo.getConfirm_yn().equals("")){
			if(confirm_yn_unit != null && !confirm_yn_unit.equals("")){
				vo.setConfirm_yn_unit(confirm_yn_unit);
			}
		}

		//대행업체 개별 사업자 정보 조회
		String search_company_sh = req.getParameter("search_company_unit");
		if(vo.getSearch_company_unit() == null || vo.getSearch_company_unit().equals("")) {
			if(search_company_sh != null && !search_company_sh.equals("")){
				vo.setSearch_company_unit(search_company_sh);
			}
			else{
				vo.setSearch_company_unit("Y"); //기본값은 등록일자로 세팅
			}
		}

		/* 의뢰자사업자번호 */
		String comp_bsns_num_unit = req.getParameter("comp_bsns_num_unit");
		if(vo.getComp_bsns_num_unit() == null || vo.getComp_bsns_num_unit().equals("") || vo.getComp_bsns_num_unit().contains("-")) {
			if(comp_bsns_num_unit != null && !comp_bsns_num_unit.equals("")){
				vo.setComp_bsns_num_unit(comp_bsns_num_unit.replaceAll("-", ""));
			}
			else{
				vo.setComp_bsns_num_unit(""); //기본값은 없는것으로 세팅
			}
		}



		/* 대행_회원사 사업자번호 */
		String pricomp_bsns_num_unit = vo.getPricomp_bsns_num_unit();
		if(vo.getPricomp_bsns_num_unit() == null || vo.getPricomp_bsns_num_unit().equals("")) {
			if(pricomp_bsns_num_unit != null && !pricomp_bsns_num_unit.equals("")){
				vo.setPricomp_bsns_num_unit(pricomp_bsns_num_unit.replaceAll("-", ""));
			}
		}else if(vo.getPricomp_bsns_num_unit() != null && vo.getPricomp_bsns_num_unit() != ""){
			//2015. 11. 10 written by dyahn 대행시 실적주체 차량대수 가져오기
			//2016. 01. 22 written by dyahn 대행실적주체(개별회원사) 업체정보 가져오기추가
			if(pricomp_bsns_num_unit != null && !"".equals(pricomp_bsns_num_unit) || vo.getPricomp_bsns_num_unit().contains("-")){
				vo.setPricomp_bsns_num_unit(pricomp_bsns_num_unit.replaceAll("-", ""));
			}

			//2016. 06. 10 written by dyahn 대행실적주체(개별회원사) 업체정보 가져오기 개선
			RegVO pricomp_vo = new RegVO();
			pricomp_vo = unitService.selectPricompInfo(vo);

			// 미가입자 조건 추가 - 2021.09.27 suhyun
			if(pricomp_vo != null) {
				vo.setPricomp_name(pricomp_vo.getPricomp_name());
				vo.setPricomp_cond(pricomp_vo.getPricomp_cond());
			}else {
				vo.setPricomp_name("미가입자");
			}

			//개별회원사 차량등록대수 가져오기
			FpisCarManageVO shVO = new FpisCarManageVO();
			shVO.setUsr_mst_key(vo.getPricomp_bsns_num_unit());
			int totCnt = 0;
			try {
				totCnt = CarManageService.CarManageFirstChkCntForReg(shVO);
			}catch(SQLException e) {
				logger.error("[ERROR] - SQLException : ", e);
			}catch(Exception e) {
				logger.error("[ERROR] - Exception : ", e);
			}

			model.addAttribute("totCnt_pricomp", totCnt);    //대행실적주체 차량 수

		}

		// 2020.09.07 검색조건 추가:: 위탁사업자번호 or 배차차량번호
		String search_unit = req.getParameter("search_unit");
		if(vo.getSearch_unit() == null || vo.getSearch_unit().equals("")) {
			if(search_unit != null && !search_unit.equals("")){
				vo.setSearch_unit(search_unit);
			}
			else{
				vo.setSearch_unit("trust");	//기본값 : 위탁 사업자 등록번호
			}
		}



		String cur_page_unit = req.getParameter("cur_page_unit");
		if(vo.getCur_page()<= 0) {
			if(cur_page_unit != null && !cur_page_unit.equals("")){
				vo.setCur_page(Integer.parseInt(cur_page_unit));
			}
			else{
				vo.setCur_page(1);
			}
		}

		String rtn = req.getParameter("rtn");

		int tot = 0;
		List<RegVO> voList = new ArrayList<RegVO>();

		//searcg_kind_unit = 1 : 등록년월 검색
		//searcg_kind_unit = 2 : 계약년월 검색
		if(vo.getSearch_kind_unit().equals("1")){
			tot = unitService.selectFpisOrderCnt_Unit(vo);
			vo.setS_row(Util.getPagingStart(vo.getCur_page()));
			vo.setE_row(Util.getPagingEnd(vo.getCur_page()));
			vo.setTot_page(Util.calcurateTPage(tot));
			model.addAttribute("TOTCNT", tot);

			voList = unitService.selectFpisOrderList_Unit(vo);
			model.addAttribute("VO", vo);
			model.addAttribute("voList", voList);
		}else if(vo.getSearch_kind_unit().equals("2")){
			tot = unitService.selectFpisOrderCount_UnitCase2(vo);
			vo.setS_row(Util.getPagingStart(vo.getCur_page()));
			vo.setE_row(Util.getPagingEnd(vo.getCur_page()));
			vo.setTot_page(Util.calcurateTPage(tot));
			model.addAttribute("TOTCNT", tot);

			voList = unitService.selectFpisOrderList_UnitCase2(vo);
			model.addAttribute("VO", vo);
			model.addAttribute("voList", voList);
		}else {
			tot = 0;
			vo.setS_row(Util.getPagingStart(vo.getCur_page()));
			vo.setE_row(Util.getPagingEnd(vo.getCur_page()));
			vo.setTot_page(Util.calcurateTPage(tot));
			model.addAttribute("TOTCNT", tot);

			voList = null;
			model.addAttribute("VO", vo);
			model.addAttribute("voList", voList);
		}
		model.addAttribute("usr_mst_key", usr_mst_key);
		model.addAttribute("RTN", rtn);
		model.addAttribute("BCODE", BCODE);
		model.addAttribute("RCODE", RCODE);

		//2020.11.05 ysw 수정허가 확인
  		String updPermission = "";
  		String[] cur_date = Util.getDateFormat().split("-");
  		if(Integer.parseInt(cur_date[1]) > 3 &&  Integer.parseInt(cur_date[1]) < 7 ){
  		//2021.04.01 ysw 기존 vo를 변경해버리면 안되는걸 확인... tmp 만들어서 결과를 가져오도록 합니다.
  			RegVO tmpRegVo = new RegVO();
  			tmpRegVo.setUsr_mst_key(vo.getUsr_mst_key());
  			tmpRegVo.setBase_year(Integer.toString((Integer.parseInt(cur_date[0])-1)) );
  			//vo.setBase_year(Integer.toString((Integer.parseInt(cur_date[0])-1)) );
  			updPermission = unitService.checkUpdPermission(tmpRegVo);
  		}
  		model.addAttribute("updPermission", updPermission);  // 2017_Y, 2017_N
  		model.addAttribute("updPermissionYear", Integer.parseInt(cur_date[0])-1);
  		model.addAttribute("updPermissionMonth", Integer.parseInt(cur_date[1]));
  		if(voList != null && voList.size() > 0){
  			model.addAttribute("voListMinYear", voList.get(0).getMinYear());
  		}


		return "/fpis/reg/unit/FpisOrderList_unit";
    }

    /**
	 * 2020.09.20 ysw 회원가입 되어있는지, 대행자 회원관리에 있는지 확인.
	 **/
    @RequestMapping("/reg/unit/selectChkUsrAndAgencyByUsrMstKey.do")
    public void selectChkUsrAndAgencyByUsrMstKey(@ModelAttribute RegVO vo,
                                      HttpServletResponse res,
                                      HttpServletRequest req,
                                      Model model) throws Exception, JSONException, IOException{
    	SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
    	HashMap<String, Object> map = new HashMap<String, Object>();
    	map.put("agency_usr_mst_key", vo.getUsr_mst_key()); //대행 사업자번호
    	map.put("usr_mst_key",svo.getUsr_mst_key()); //자신의 사업자번호
    	map = commonGetInfoService.selectChkUsrAndAgencyByUsrMstKey(map);

    	PrintWriter out = null;

        JSONObject json = new JSONObject();
        json.put("usr_cnt", map.get("USR_CNT"));
        json.put("agency_cnt", map.get("AGENCY_CNT"));

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
		out = res.getWriter();

        out.write(json.toString());
    }

    /* 2020.09.28 ysw 등록단위 계약 삭제*/
	@RequestMapping(value="/reg/unit/FpisOrderList_renewal_delete.do", method=RequestMethod.POST)
	public String FpisOrderList_renewal_delete( RegVO vo, HttpServletRequest req, ModelMap model){

		SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);

		String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
		vo.setUsr_mst_key(usr_mst_key);

		String BCODE = req.getParameter("bcode");
		String RCODE = req.getParameter("rcode");

		model.addAttribute("bcode", BCODE);  //리다이렉트니까 소문자
		model.addAttribute("rcode", RCODE);    //리다이렉트니까 소문자

		//검색조건 유지합니다. redirect이기 때문에 model.addAttribute에 하나씩 넣어줘야함.
		model.addAttribute("cur_page_unit", vo.getCur_page_unit()); //페이지번호
		model.addAttribute("base_year",vo.getBase_year()); //실적년도
		model.addAttribute("search_kind_unit",vo.getSearch_kind_unit()); //기간검색 구분
		model.addAttribute("from_date_unit",vo.getFrom_date_unit()); //기간검색 from 년
		model.addAttribute("to_date_unit",vo.getTo_date_unit()); //기간검색 to 월
		model.addAttribute("pg_id_unit",vo.getPg_id_unit()); //등록방식 구분
		model.addAttribute("regist_code_unit",vo.getRegist_code_unit()); //등록여부
		model.addAttribute("agency_yn_unit",vo.getAgency_yn_unit()); //대행여부
		model.addAttribute("confirm_yn_unit",vo.getConfirm_yn_unit()); //확정여부
		model.addAttribute("comp_bsns_num_unit",vo.getComp_bsns_num_unit()); //의뢰자사업자번호
		model.addAttribute("search_unit",vo.getSearch_unit()); //세부정보 검색 구분
		model.addAttribute("search_unit_detail",vo.getSearch_unit_detail()); //세부정보 검색 값
		model.addAttribute("search_company_unit",vo.getSearch_company_unit()); //대행 검색분류
		model.addAttribute("pricomp_bsns_num_unit",vo.getPricomp_bsns_num_unit()); //회원사 사업자번호



		String checkedList = req.getParameter("checkedList");


		svo.getUsr_mst_key();
		int    delRtn   = -1;
		List<String> orderList  = new ArrayList<String>();
		List<String> uuSeqList  = new ArrayList<String>();

		if(checkedList != null && !checkedList.equals("")) {
			StringTokenizer token = new StringTokenizer(checkedList,",");
			String order_key ="";
			String [] itemArry  = null;

			while(token.hasMoreTokens()) {
				order_key = token.nextToken();

				if(order_key != null && !order_key.equals("") && order_key.length() > 1) {
					itemArry = order_key.split("\\|");
					orderList.add(order_key);
					uuSeqList.add(itemArry[1]);
				}
			}
			orderList.toArray(checkedList.split(","));

			//190627 오승민 확정 실적있는지 확인
			//200423 오승민 연계프로그램 등록 대기건 있는지 확인 추가(regist_code = 'DPR001')
			//			등록단위 -계약정보 확정컬럼 값이 다른경우 확인
			//			추정으로 연계프로그램에서 대기일떄 삭제 하여 계약정보는 삭제되었으나,
			//			연계프로그램 등록완료로 등록단위 flag 변경
			int confirm_cnt = unitService.OrderDelete_confirm_check(uuSeqList);

			if(confirm_cnt > 0){
				delRtn = 99;
			}else{
				if(orderList.size() > 0) {
					delRtn = unitService.OrderDelete_renewal_batch(orderList);
				}
			}
		}
		model.addAttribute("rtn", delRtn);
		String contextPath = req.getContextPath();
		model.addAttribute("contextPath",contextPath);

		return "redirect:/reg/unit/FpisOrderList_unit.do";
	}

	/* 2020.09.29 ysw 계약단위 확정처리 */
	@RequestMapping(value="/reg/unit/FpisOrderList_renewal_confirm.do", method=RequestMethod.POST)
	public String FpisOrderList_renewal_confirm(RegVO vo, HttpServletRequest req,ModelMap model){
	//public void FpisOrderList_renewal_confirm(RegVO vo, HttpServletRequest req,ModelMap model, HttpServletResponse res) throws ServletException, IOException{
		String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
		vo.setUsr_mst_key(usr_mst_key);

		//공통
		String BCODE = req.getParameter("bcode");
		String RCODE = req.getParameter("rcode");
		String pg_id = req.getParameter("pg_id");

		//공통 model
		model.addAttribute("bcode", BCODE);        //리다이렉트이니까 소문자
		model.addAttribute("rcode", RCODE);        //리다이렉트이니까 소문자
		model.addAttribute("pg_id", pg_id);        //리다이렉트이니까 소문자


		//검색조건 유지합니다. redirect이기 때문에 model.addAttribute에 하나씩 넣어줘야함.
		model.addAttribute("cur_page_unit", vo.getCur_page_unit()); //페이지번호
		model.addAttribute("base_year",vo.getBase_year()); //실적년도
		model.addAttribute("search_kind_unit",vo.getSearch_kind_unit()); //기간검색 구분
		model.addAttribute("from_date_unit",vo.getFrom_date_unit()); //기간검색 from 년
		model.addAttribute("to_date_unit",vo.getTo_date_unit()); //기간검색 to 월
		model.addAttribute("pg_id_unit",vo.getPg_id_unit()); //등록방식 구분
		model.addAttribute("regist_code_unit",vo.getRegist_code_unit()); //등록여부
		model.addAttribute("agency_yn_unit",vo.getAgency_yn_unit()); //대행여부
		model.addAttribute("confirm_yn_unit",vo.getConfirm_yn_unit()); //확정여부
		model.addAttribute("comp_bsns_num_unit",vo.getComp_bsns_num_unit()); //의뢰자사업자번호
		model.addAttribute("search_unit",vo.getSearch_unit()); //세부정보 검색 구분
		model.addAttribute("search_unit_detail",vo.getSearch_unit_detail()); //세부정보 검색 값
		model.addAttribute("search_company_unit",vo.getSearch_company_unit()); //대행 검색분류
		model.addAttribute("pricomp_bsns_num_unit",vo.getPricomp_bsns_num_unit()); //회원사 사업자번호


		String checkedList = req.getParameter("checkedList");
		int confirmRtn = -1;

		//대행 구분
		List<String> orderList  = new ArrayList<String>();

		if(checkedList != null && !checkedList.equals("")) {
			StringTokenizer token = new StringTokenizer(checkedList,",");
			String order_key ="";
			while(token.hasMoreTokens()) {
				order_key = token.nextToken();
				if(order_key != null && !order_key.equals("") && order_key.length() > 1) {
					order_key.split("\\|");
					orderList.add(order_key);
				}
			}

			orderList.toArray(checkedList.split(","));


			if(orderList.size() > 0) {
				//계약 확정
				confirmRtn = unitService.updateReg_contract_confirm_yn(orderList);
			}

		}

		model.addAttribute("rtn",confirmRtn);

		String contextPath = req.getContextPath();
		model.addAttribute("contextPath",contextPath);

		return "redirect:/reg/unit/FpisOrderList_unit.do";

		/*RequestDispatcher rd = req.getRequestDispatcher("/fpis/reg/unit/FpisOrderList_unit.do");
  		rd.forward(req, res);*/
	}

	/* 2020.10.06 ysw 미확정 처리*/
	@RequestMapping(value="/reg/unit/FpisOrderInvertConfirm.do", method=RequestMethod.POST)
	public String FpisOrderInvertConfirm(RegVO vo, HttpServletRequest req, ModelMap model){
		SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
    	String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
    	vo.setUsr_mst_key(usr_mst_key);
		vo.setReg_user(svo.getUser_id());

		String rcode = req.getParameter("rcode");
		String bcode = req.getParameter("bcode");
		String mode = req.getParameter("mode");
		String checkedList = "";
		String msg = "";

		//검색조건 유지
		model.addAttribute("VO", vo);

		//mode 에 따라 checkedList 생성을 다르게 처리
		if("A".equals(mode)){
			vo.setUsr_mst_key(usr_mst_key);
			List<RegVO> searchResultList = getSearchListAll(vo);
			for(int i = 0 ; i < searchResultList.size() ; i++) {
				if(i == 0){
					checkedList = searchResultList.get(i).getUnique_seq();
				}else{
					checkedList += ","+searchResultList.get(i).getUnique_seq();
				}
			}
		}else {
			checkedList = req.getParameter("checkedList");
		}


		//체크리스트 유효성 검사
		if(checkedList == null){
			model.addAttribute("MSG", "-88");
			return "redirect:/reg/unit/FpisOrderList_unit.do";
		}

		int updateCnt = 0;

		if("".equals(checkedList)){
			msg = "-888";
		}else{
			updateCnt = unitService.updateInvertConfirm(checkedList);
			msg = "SUC_unConfirm";
		}

		String contextPath = req.getContextPath();
		model.addAttribute("contextPath", contextPath);
		model.addAttribute("bcode", bcode);
		model.addAttribute("rcode", rcode);
		model.addAttribute("MSG" ,msg);
		model.addAttribute("updateCnt", updateCnt);

		//검색조건 유지합니다. redirect이기 때문에 model.addAttribute에 하나씩 넣어줘야함.
		model.addAttribute("cur_page_unit", vo.getCur_page_unit()); //페이지번호
		model.addAttribute("base_year",vo.getBase_year()); //실적년도
		model.addAttribute("search_kind_unit",vo.getSearch_kind_unit()); //기간검색 구분
		model.addAttribute("from_date_unit",vo.getFrom_date_unit()); //기간검색 from 년
		model.addAttribute("to_date_unit",vo.getTo_date_unit()); //기간검색 to 월
		model.addAttribute("pg_id_unit",vo.getPg_id_unit()); //등록방식 구분
		model.addAttribute("regist_code_unit",vo.getRegist_code_unit()); //등록여부
		model.addAttribute("agency_yn_unit",vo.getAgency_yn_unit()); //대행여부
		model.addAttribute("confirm_yn_unit",vo.getConfirm_yn_unit()); //확정여부
		model.addAttribute("comp_bsns_num_unit",vo.getComp_bsns_num_unit()); //의뢰자사업자번호
		model.addAttribute("search_unit",vo.getSearch_unit()); //세부정보 검색 구분
		model.addAttribute("search_unit_detail",vo.getSearch_unit_detail()); //세부정보 검색 값
		model.addAttribute("search_company_unit",vo.getSearch_company_unit()); //대행 검색분류
		model.addAttribute("pricomp_bsns_num_unit",vo.getPricomp_bsns_num_unit()); //회원사 사업자번호


		return "redirect:/reg/unit/FpisOrderList_unit.do";
	}

	/*2015. 06. 17 written by dyahn 택배 실적조회 개선안 입력단위 조회 */
	@RequestMapping("/reg/unit/FpisOrderList_tb_unit.do")
	public String FpisOrderList_tb_renewal(RegVO vo, HttpServletRequest req, ModelMap model){
        SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
        model.addAttribute("usr_mst_key", svo.getUsr_mst_key());
        String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
        String sh_confirm_yn = req.getParameter("sh_confirm_yn");
        String BCODE = req.getParameter("bcode");
        String RCODE = req.getParameter("rcode");

        String cur_page_sh = req.getParameter("cur_page");
        String from_date_unit = req.getParameter("from_date_unit");
        String to_date_unit = req.getParameter("to_date_unit");
        String confirm_yn_unit = req.getParameter("confirm_yn_unit");
        String cur_page_unit = req.getParameter("cur_page_unit");
        String rtn = req.getParameter("rtn");
        model.addAttribute("RTN", rtn);
        model.addAttribute("BCODE", BCODE);
        model.addAttribute("RCODE", RCODE);
        vo.setUsr_mst_key(usr_mst_key);

        /* 기간검색 */
		if(vo.getSearch_kind_unit() == null || vo.getSearch_kind_unit().equals("")) {
			vo.setSearch_kind_unit("1"); //기본값은 등록일자로 세팅
		}
		int strYear = 2017;
        int endYear = Calendar.getInstance().get(Calendar.YEAR);

        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);
		//현재년도..?
		if(vo.getBase_year() == null || vo.getBase_year().equals("")) {
			vo.setBase_year(Integer.toString(Util.getCurYear()));
		}

		if(vo.getFrom_date() == null || vo.getFrom_date().equals("")) {
			if(from_date_unit != null && !from_date_unit.equals("")){
				vo.setFrom_date_unit(from_date_unit);
			}
			else{
				vo.setFrom_date_unit(Util.getMonthFirstDay());
			}
		}

		if(vo.getTo_date() == null || vo.getTo_date().equals("")) {
			if(to_date_unit != null && !to_date_unit.equals("")){
				vo.setTo_date_unit(to_date_unit);
			}
			else{
				vo.setTo_date_unit(Util.getCurDate(0,0));
			}
		}

        if(confirm_yn_unit == null || confirm_yn_unit == ""){
            if(vo.getConfirm_yn() == null || vo.getConfirm_yn().equals("")){
                if(sh_confirm_yn != null && !sh_confirm_yn.equals("")){
                    vo.setConfirm_yn(sh_confirm_yn);
                }
            }
        }
        else{
        	vo.setConfirm_yn(confirm_yn_unit);
        }


        if(cur_page_unit == null || cur_page_unit == ""){
            if(vo.getCur_page()<= 0) {
                if(cur_page_sh != null && !cur_page_sh.equals("")){
                    vo.setCur_page(Integer.parseInt(cur_page_sh));
                }
                else{
                    vo.setCur_page(1);
                }
            }
        }
        else{
            vo.setCur_page(Integer.parseInt(cur_page_unit));
        }

        int tot = unitService.getTbOrderRenewalTotCount(vo);
        vo.setS_row(Util.getPagingStart(vo.getCur_page()));
        vo.setE_row(Util.getPagingEnd(vo.getCur_page()));
        vo.setTot_page(Util.calcurateTPage(tot));
        model.addAttribute("TOTCNT", tot);

        List<RegVO> voList = unitService.getTbOrderRenewalList(vo);

        model.addAttribute("voList", voList);
        model.addAttribute("TOTCNT", tot);
        model.addAttribute("VO",vo);

      //180312 smoh 수정허가 확인
  		String updPermission = "";
  		String[] cur_date = Util.getDateFormat().split("-");
  		if(Integer.parseInt(cur_date[1]) > 3 &&  Integer.parseInt(cur_date[1]) < 7 ){
  		//2021.04.01 ysw 기존 vo를 변경해버리면 안되는걸 확인... tmp 만들어서 결과를 가져오도록 합니다.
  			RegVO tmpRegVo = new RegVO();
  			tmpRegVo.setUsr_mst_key(vo.getUsr_mst_key());
  			tmpRegVo.setBase_year(Integer.toString((Integer.parseInt(cur_date[0])-1)) );
  			//vo.setBase_year(Integer.toString((Integer.parseInt(cur_date[0])-1)) );
  			updPermission = unitService.checkUpdPermission(tmpRegVo);
  		}
  		model.addAttribute("updPermission", updPermission);  // 2017_Y, 2017_N
  		model.addAttribute("updPermissionYear", Integer.parseInt(cur_date[0])-1);
  		model.addAttribute("updPermissionMonth", Integer.parseInt(cur_date[1]));
  		if(voList.size() > 0){
  			model.addAttribute("voListMinYear", voList.get(0).getMinYear());
  		}

  		return "/fpis/reg/unit/FpisOrderList_tb_unit";
	}

	/* 2020.10.06 ysw 택배 확정처리*/
	@RequestMapping(value="/reg/unit/FpisOrderConfirm_tb_renewal.do", method=RequestMethod.POST)
    public String FpisOrderConfirm_tb_renewal(RegVO vo, HttpServletRequest req, ModelMap model){
        String checkedList = req.getParameter("checkedList");;

        //공통
        String BCODE = req.getParameter("bcode");
        String RCODE = req.getParameter("rcode");
        String pg_id = req.getParameter("pg_id");

        model.addAttribute("bcode", BCODE);
        model.addAttribute("rcode", RCODE);
        model.addAttribute("pg_id", pg_id);

        //입력단위 검색조건 유지 파라미터 (리다이렉트라서 이렇게 하나하나씩 넣어줘야 다음 VO에서 받아짐)
        model.addAttribute("from_date_unit", vo.getFrom_date_unit());
        model.addAttribute("to_date_unit", vo.getTo_date_unit());
        model.addAttribute("confirm_yn_unit", vo.getConfirm_yn_unit());
        model.addAttribute("cur_page_unit", vo.getCur_page_unit());
        model.addAttribute("search_kind_unit", vo.getSearch_kind_unit());
        model.addAttribute("base_year", vo.getBase_year());
        model.addAttribute("search_unit", vo.getSearch_unit());
        model.addAttribute("search_unit_detail", vo.getSearch_unit_detail());

        List<String> orderList  = new ArrayList<String>();
        int rtn = -1;
        if(checkedList != null && !checkedList.equals("")) {
            StringTokenizer token = new StringTokenizer(checkedList,",");
            String order_key ="";
            while(token.hasMoreTokens()) {
                order_key = token.nextToken();
                if(order_key != null && !order_key.equals("") && order_key.length() > 1) {
                    order_key.split("\\|");
                    orderList.add(order_key);
                }
            }
            orderList.toArray(checkedList.split(","));

            if(orderList.size() > 0) {
                //계약 확정
                rtn = unitService.confirmContract_unit_tb(orderList);
                model.addAttribute("rtn", rtn);
            }
            else{
                model.addAttribute("rtn", rtn);
            }
        }
        else{
        	model.addAttribute("rtn", "rtn");
        }
        return "redirect:/reg/unit/FpisOrderList_tb_unit.do";
    }

	/* 2020.10.06 ysw 계약단위(택배) 미확정처리*/
	@RequestMapping(value="/reg/unit/FpisOrderInvertConfirmTB.do", method=RequestMethod.POST)
	public String FpisOrderInvertConfirmTB(RegVO vo,HttpServletRequest req, ModelMap model){
		SessionVO session = (SessionVO)req.getSession().getAttribute("SessionVO");
		vo.setUsr_mst_key(((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key());
		String rcode = req.getParameter("rcode");
		String bcode = req.getParameter("bcode");


		String checkedList_str = req.getParameter("checkedList");
		checkedList_str = XSS_secure(checkedList_str);
		String[] checkedList = checkedList_str.split(",");

		String mode = req.getParameter("mode");
		String rtn = "";

		//입력단위 검색조건 유지 파라미터
        int updateCnt = 0;
		model.addAttribute("SessionVO", session);
		model.addAttribute("bcode", bcode);
		model.addAttribute("rcode", rcode);
		model.addAttribute("MODE", mode);

		//입력단위 검색조건 유지 파라미터 (리다이렉트라서 이렇게 하나하나씩 넣어줘야 다음 VO에서 받아짐)
        model.addAttribute("from_date_unit", vo.getFrom_date_unit());
        model.addAttribute("to_date_unit", vo.getTo_date_unit());
        model.addAttribute("confirm_yn_unit", vo.getConfirm_yn_unit());
        model.addAttribute("cur_page_unit", vo.getCur_page_unit());
        model.addAttribute("search_kind_unit", vo.getSearch_kind_unit());
        model.addAttribute("base_year", vo.getBase_year());
        model.addAttribute("search_unit", vo.getSearch_unit());
        model.addAttribute("search_unit_detail", vo.getSearch_unit_detail());

		//mode 에 따라 checkedList 생성을 다르게 처리
		if("A".equals(mode)){
			// 전체 미확정인 경우 검색조건으로 미확정 처리
			vo.setFrom_date(vo.getFrom_date_unit());
			vo.setTo_date(vo.getTo_date_unit());
			updateCnt = unitService.updateInvertConfirmTBAll(vo);
		}else{
			//개별 미확정인경우 가져온 uu_seq로 미확정 처리

			//체크리스트 유효성 검사
			if(checkedList == null || checkedList.equals("")){
				model.addAttribute("rtn", "-88");
				return "redirect:/reg/unit/FpisOrderList_tb_unit.do";
			}

			updateCnt = unitService.updateInvertConfirmTB(checkedList);
			rtn = "SUC_unConfirm";
		}

		model.addAttribute("rtn" ,rtn);
		model.addAttribute("updateCnt", updateCnt);

		return "redirect:/reg/unit/FpisOrderList_tb_unit.do";
	}

	/* 택배실적 삭제 처리 */
	@RequestMapping(value="/reg/unit/FpisOrderDelete_tb_renewal.do", method=RequestMethod.POST)
    public String FpisOrderDelete_tb_renewal(RegVO vo, HttpServletRequest req, ModelMap model){
        String checkedList = req.getParameter("checkedList");


        //공통
        String BCODE = req.getParameter("bcode");
        String RCODE = req.getParameter("rcode");
        String pg_id = req.getParameter("pg_id");

        model.addAttribute("bcode", BCODE);
        model.addAttribute("rcode", RCODE);
        model.addAttribute("pg_id", pg_id);

        //입력단위 검색조건 유지 파라미터
        model.addAttribute("from_date_unit", vo.getFrom_date_unit());
        model.addAttribute("to_date_unit", vo.getTo_date_unit());
        model.addAttribute("confirm_yn_unit", vo.getConfirm_yn_unit());
        model.addAttribute("cur_page_unit", vo.getCur_page_unit());
        model.addAttribute("search_kind_unit", vo.getSearch_kind_unit());
        model.addAttribute("base_year", vo.getBase_year());
        model.addAttribute("search_unit", vo.getSearch_unit());
        model.addAttribute("search_unit_detail", vo.getSearch_unit_detail());

        List<String> orderList  = new ArrayList<String>();

        int rtn = -1;
        if(checkedList != null && !checkedList.equals("")) {
            StringTokenizer token = new StringTokenizer(checkedList,",");
            String order_key ="";
            while(token.hasMoreTokens()) {
                order_key = token.nextToken();
                if(order_key != null && !order_key.equals("") && order_key.length() > 1) {
                    order_key.split("\\|");
                    orderList.add(order_key);
                }
            }

            orderList.toArray(checkedList.split(","));

            if(orderList.size() > 0) {
                //계약 확정
                rtn = unitService.deleteContract_unit_tb(orderList);
                model.addAttribute("rtn", rtn);
            }
            else{
            	model.addAttribute("rtn", rtn);
            }
        }
        else{
        	model.addAttribute("rtn", rtn);
        }
        return "redirect:/reg/unit/FpisOrderList_tb_unit.do";
    }

	/* 2020.11.05 ysw 등록단위 실적조회 페이지 검색한 리스트 가져옵니다.*/
	public List<RegVO> getSearchListAll(RegVO vo){
		/*2020.10.23 ysw ★★★★대행실적 등록후 이 메뉴로 오는 경우 넘어온 usr_mst_key를 대행 검색조건으로 해줘야한다. */
    	if("Y".equals(vo.getAgency_yn())) {
    		vo.setPricomp_bsns_num(vo.getUsr_mst_key());
    		vo.setSearch_company_unit("N");
    	}

		//실적년도
		String base_year = vo.getBase_year();
		if(vo.getBase_year() == null || vo.getBase_year().equals("")){
			String[] cur_date = Util.getDateFormat().split("-");
			vo.setBase_year("P"+cur_date[0]);
		}else{
			if(vo.getBase_year().contains("P")){
				vo.setBase_year(base_year);
			}else{
				vo.setBase_year("P"+base_year);
			}
		}

		/* 의뢰자사업자번호 */
		if(vo.getComp_bsns_num_unit() == null || vo.getComp_bsns_num_unit().equals("") || vo.getComp_bsns_num_unit().contains("-")) {
			if(vo.getComp_bsns_num_unit() != null && !("").equals(vo.getComp_bsns_num_unit())){
				vo.setComp_bsns_num_unit(vo.getComp_bsns_num_unit().replaceAll("-", ""));
			}
			else{
				vo.setComp_bsns_num_unit(""); //기본값은 없는것으로 세팅
			}
		}

		/* 기간검색 */
		if(vo.getSearch_kind_unit() == null || vo.getSearch_kind_unit().equals("")) {
			vo.setSearch_kind_unit("1"); //기본값은 등록일자로 세팅
		}

		//대행업체 개별 사업자 정보 조회
		if(vo.getSearch_company_unit() == null || vo.getSearch_company_unit().equals("")) {
			vo.setSearch_company_unit("Y"); //기본값은 등록일자로 세팅
		}

		/* 대행_회원사 사업자번호 */
		String pricomp_bsns_num_unit = vo.getPricomp_bsns_num_unit();
		if(vo.getPricomp_bsns_num_unit() == null || vo.getPricomp_bsns_num_unit().equals("")) {
			if(pricomp_bsns_num_unit != null && !pricomp_bsns_num_unit.equals("")){
				vo.setPricomp_bsns_num_unit(pricomp_bsns_num_unit.replaceAll("-", ""));
			}
		}else if(vo.getPricomp_bsns_num_unit() != null && vo.getPricomp_bsns_num_unit() != ""){
			//2015. 11. 10 written by dyahn 대행시 실적주체 차량대수 가져오기
			//2016. 01. 22 written by dyahn 대행실적주체(개별회원사) 업체정보 가져오기추가
			if(pricomp_bsns_num_unit != null && !"".equals(pricomp_bsns_num_unit) || vo.getPricomp_bsns_num_unit().contains("-")){
				vo.setPricomp_bsns_num_unit(pricomp_bsns_num_unit.replaceAll("-", ""));
			}

			//2016. 06. 10 written by dyahn 대행실적주체(개별회원사) 업체정보 가져오기 개선
			RegVO pricomp_vo = new RegVO();
			pricomp_vo = unitService.selectPricompInfo(vo);
			vo.setPricomp_name(pricomp_vo.getPricomp_name());
			vo.setPricomp_cond(pricomp_vo.getPricomp_cond());
		}

		List<RegVO> voList = new ArrayList<RegVO>();

		//searcg_kind_unit = 1 : 등록년월 검색
		//searcg_kind_unit = 2 : 계약년월 검색
		if(vo.getSearch_kind_unit().equals("1")){
			vo.setS_row(0);
			vo.setE_row(999);
			voList = unitService.selectFpisOrderList_Unit(vo);
		}else if(vo.getSearch_kind_unit().equals("2")){
			vo.setS_row(0);
			vo.setE_row(999);
			voList = unitService.selectFpisOrderList_UnitCase2(vo);
		}

		return voList;
	}

	/*2020.11.05 ysw 등록단위 실적조회 검색목록 삭제*/
	@RequestMapping(value="/reg/unit/FpisOrderList_renewal_delete_all.do", method=RequestMethod.POST)
	public String FpisOrderList_renewal_delete_all(RegVO vo, HttpServletRequest req,ModelMap model){

		SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
    	String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
    	vo.setUsr_mst_key(usr_mst_key);
		vo.setReg_user(svo.getUser_id());

		List<RegVO> searchResultList = getSearchListAll(vo);

		boolean allDeleteResult = unitService.allDeleteRenewal(searchResultList);
		if(allDeleteResult){
			model.addAttribute("MSG", "9");
		}else{
			model.addAttribute("MSG", "-7");
		}





		//검색조건 유지합니다. redirect이기 때문에 model.addAttribute에 하나씩 넣어줘야함.
		model.addAttribute("cur_page_unit", 1); //페이지번호
		model.addAttribute("base_year",vo.getBase_year()); //실적년도
		model.addAttribute("search_kind_unit",vo.getSearch_kind_unit()); //기간검색 구분
		model.addAttribute("from_date_unit",vo.getFrom_date_unit()); //기간검색 from 년
		model.addAttribute("to_date_unit",vo.getTo_date_unit()); //기간검색 to 월
		model.addAttribute("pg_id_unit",vo.getPg_id_unit()); //등록방식 구분
		model.addAttribute("regist_code_unit",vo.getRegist_code_unit()); //등록여부
		model.addAttribute("agency_yn_unit",vo.getAgency_yn_unit()); //대행여부
		model.addAttribute("confirm_yn_unit",vo.getConfirm_yn_unit()); //확정여부
		model.addAttribute("comp_bsns_num_unit",vo.getComp_bsns_num_unit()); //의뢰자사업자번호
		model.addAttribute("search_unit",vo.getSearch_unit()); //세부정보 검색 구분
		model.addAttribute("search_unit_detail",vo.getSearch_unit_detail()); //세부정보 검색 값
		model.addAttribute("search_company_unit",vo.getSearch_company_unit()); //대행 검색분류
		model.addAttribute("pricomp_bsns_num_unit",vo.getPricomp_bsns_num_unit()); //회원사 사업자번호

		return "redirect:/reg/unit/FpisOrderList_unit.do";
	}

	/* 2020.11.05 pch 등록단위 실적조회 검색목록 엑셀출력 */
	@RequestMapping("/reg/unit/FpisOrderList_renewal_ExportExcel.do")
	public void FpisOrderListExportExcel_renewal(RegVO vo, HttpServletRequest req, ModelMap model, HttpServletResponse res) throws Exception, UnsupportedEncodingException{
		SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
    	String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
		vo.setUsr_mst_key(usr_mst_key);
		vo.setReg_user(svo.getUser_id());

		List<RegVO> searchResultList = getSearchListAll(vo);

		// 엑셀 작성 시작!!
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet worksheet = workbook.createSheet("WorkSheet");
		HSSFRow row = null;
		Cell cell1 = null;// 셀


		CellStyle cellStyle_td = workbook.createCellStyle(); // 스타일 생성 - 기본셀

		CellStyle cellStyle_header = workbook.createCellStyle(); // 스타일 생성 - 헤더
		cellStyle_header.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle_header.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//높이 가운데 정렬

		row = worksheet.createRow(0);
		Util_poi.setCell(cell1, row, 0, cellStyle_header, "등록");
		Util_poi.setCell(cell1, row, 3, cellStyle_header, "계약년월");
		Util_poi.setCell(cell1, row, 4, cellStyle_header, "계약");
		Util_poi.setCell(cell1, row, 5, cellStyle_header, "위탁");
		Util_poi.setCell(cell1, row, 6, cellStyle_header, "배차");
		Util_poi.setCell(cell1, row, 8, cellStyle_header, "등록여부");
		Util_poi.setCell(cell1, row, 9, cellStyle_header, "확정여부");
		Util_poi.setCell(cell1, row, 10, cellStyle_header, "수정기한");

		/* 셀병합 */
		worksheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
		worksheet.addMergedRegion(new CellRangeAddress(0, 1, 3, 3));
		worksheet.addMergedRegion(new CellRangeAddress(0, 0, 6, 7));
		worksheet.addMergedRegion(new CellRangeAddress(0, 1, 8, 8));
		worksheet.addMergedRegion(new CellRangeAddress(0, 1, 9, 9));
		worksheet.addMergedRegion(new CellRangeAddress(0, 1, 10, 10));

		row = worksheet.createRow(1);
		Util_poi.setCell(cell1, row, 0, cellStyle_header, "방식");
		Util_poi.setCell(cell1, row, 1, cellStyle_header, "등록일자");
		Util_poi.setCell(cell1, row, 2, cellStyle_header, "대행여부");
		Util_poi.setCell(cell1, row, 4, cellStyle_header, "금액합계(원)");
		Util_poi.setCell(cell1, row, 5, cellStyle_header, "금액합계(원)");
		Util_poi.setCell(cell1, row, 6, cellStyle_header, "배차횟수(회)");
		Util_poi.setCell(cell1, row, 7, cellStyle_header, "금액합계(원)");


		// 넓이 지정
		for(int i = 0 ; i < 9 ; i++) {
			worksheet.setColumnWidth(i, 3713);
		}

		// 내용작성
		for(int i = 0 ; i < searchResultList.size() ; i++) {
			row = worksheet.createRow(i + 2);
			if("web".equals(searchResultList.get(i).getPg_id())){
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "홈페이지");
			}else if("module".equals(searchResultList.get(i).getPg_id())){
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "연계모듈");
			}else if("sutak".equals(searchResultList.get(i).getPg_id())){
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "수탁");
			}else if("sutak_t".equals(searchResultList.get(i).getPg_id())){
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "수탁택배");
			}else if("mobile".equals(searchResultList.get(i).getPg_id())){
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "모바일");
			}else if("web_m".equals(searchResultList.get(i).getPg_id())){
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "대량등록");
			}else if("web_t".equals(searchResultList.get(i).getPg_id())){
				Util_poi.setCell(cell1, row, 0, cellStyle_td, "대량택배");
			}

			Util_poi.setCell(cell1, row, 1, cellStyle_td, searchResultList.get(i).getReg_date());
			if("Y".equals(searchResultList.get(i).getAgency_yn())) {
				Util_poi.setCell(cell1, row, 2, cellStyle_td, "대행");
			}else {
				Util_poi.setCell(cell1, row, 2, cellStyle_td, "미대행");
			}

			Util_poi.setCell(cell1, row, 3, cellStyle_td, searchResultList.get(i).getCont_from_range());
			Util_poi.setCell(cell1, row, 4, cellStyle_td, searchResultList.get(i).getContract_charge());
			Util_poi.setCell(cell1, row, 5, cellStyle_td, searchResultList.get(i).getTrust_charge());
			Util_poi.setCell(cell1, row, 6, cellStyle_td, searchResultList.get(i).getOperate_cnt());
			Util_poi.setCell(cell1, row, 7, cellStyle_td, searchResultList.get(i).getOperate_charge());

			if("DPR099".equals(searchResultList.get(i).getRegist_code())) {
				Util_poi.setCell(cell1, row, 8, cellStyle_td, "완료");
			}else if("DPR001".equals(searchResultList.get(i).getRegist_code())){
				Util_poi.setCell(cell1, row, 8, cellStyle_td, "대기");
			}

			if("Y".equals(searchResultList.get(i).getConfirm_yn())) {
				Util_poi.setCell(cell1, row, 9, cellStyle_td, "확정");
			}else if("N".equals(searchResultList.get(i).getConfirm_yn())){
				Util_poi.setCell(cell1, row, 9, cellStyle_td, "미확정");

			}else if("R".equals(searchResultList.get(i).getConfirm_yn())){
				Util_poi.setCell(cell1, row, 9, cellStyle_td, "수정요청");

			}else if("P".equals(searchResultList.get(i).getConfirm_yn())){
				Util_poi.setCell(cell1, row, 9, cellStyle_td, "미확정");

			}else if("C".equals(searchResultList.get(i).getConfirm_yn())){
				Util_poi.setCell(cell1, row, 9, cellStyle_td, "요청거절");

			}
			if(Integer.parseInt(searchResultList.get(i).getModify_limit()) > 0){
				Util_poi.setCell(cell1, row, 10, cellStyle_td, "D-"+searchResultList.get(i).getModify_limit());
			}else if(searchResultList.get(i).getModify_limit() == null){
				Util_poi.setCell(cell1, row, 10, cellStyle_td, "-");
			}else{
				Util_poi.setCell(cell1, row, 10, cellStyle_td, "마감");
			}

		}

		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int date = cal.get(Calendar.DATE);

		res.setContentType("ms-vnd/excel");
		String fileName = "등록단위실적_" + year + "년" + month + "월" + date + "일" + ".xls";
		fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
		res.setHeader("Content-Disposition", "ATTachment; Filename=" + fileName);

		/*2021.01.11 ysw 정보노출 보안처리*/
		res.setHeader("Cache-Control","no-store");
		res.setHeader("Pragma","no-cache");
		res.setDateHeader("Expires",0);
		if (req.getProtocol().equals("HTTP/1.1")){
			res.setHeader("Cache-Control", "no-cache");
		}
		workbook.write(res.getOutputStream());

	}
	
	/* 2022.08.03 jwchoi 대량실적등록::연도선택 페이지 */
    @RequestMapping("/reg/unit/FpisOrderMassRegist_intro.do")
    public String fpisOrderExcelRegist_intro(@ModelAttribute RegVO vo,
    		HttpServletRequest req, ModelMap model){

    	/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}
		
	    SessionVO svo = (SessionVO)req.getSession().getAttribute(FpisConstants.SESSION_KEY);
	    model.addAttribute("usr_mst_key",svo.getUsr_mst_key());
	    
	    String BCODE = "";
	    String chkCond = svo.getCond();
	    BCODE = massOrderService.getBCODE(chkCond);
	    
	    /*2022.11.09 jwchoi 웹연계 대량실적 업로드 upload_flag [확인]여부 */
	    int fileCnt = 0;
	    fileCnt = massOrderService.getUploadFileCnt(svo.getUsr_mst_key());
	    String go_page = "";
	    if (fileCnt > 0) {
	    	//model.addAttribute("fileCnt"); //회원사 사업자번호
	    	go_page = "redirect:/reg/mass/MassOrderUploadPage.do";
	    } else {
	    	go_page = "/fpis/reg/unit/FpisOrderMassRegist_intro";
	    }

	    //2020.08.24 ysw 공통함수로 현재 사용자정보  [사업자 유형(1대사업자인지) 포함] 가져온다.
  		HashMap UsrInfo = commonGetInfoService.selectUsrInfoByUsrMstKey(svo.getUsr_mst_key());
  		model.addAttribute("UsrInfo",UsrInfo);

	    //2016. 07. 19 written by dyahn 리다이렉트 파리미터(실적없음 확인/해제 처리플래그)
	    if(vo.getResult() != null && !vo.getResult().equals("")) {
	    	model.addAttribute("RESULT", vo.getResult());
	    }

		//2016. 01. 22 written by dyahn 대행시 실적주체 업체명 가져오기
		model.addAttribute("PRICOMP_NAME", UsrInfo.get("NAME"));



	    Calendar c = Calendar.getInstance(); //객체 생성 및 현재 일시분초...셋팅
	    int currentYear = c.get(Calendar.YEAR);
	    int currentMonth = c.get(Calendar.MONTH)+1;

	    model.addAttribute("VO", vo);
	    model.addAttribute("currentYear", currentYear);
	    model.addAttribute("currentMonth", currentMonth);
	    model.addAttribute("rcode", "R1");
	    model.addAttribute("bcode", BCODE);
	    //model.addAttribute("bcode", req.getParameter("bcode"));
	    return go_page;
	}

	//2020.05.28 pch : 보안취약점(크로스스크립트)
  	public String XSS_secure(String param){
  		if(param.indexOf("\"") != -1){ param= param.replace("\"", "&quot;"); }
        if(param.indexOf("\'") != -1){ param= param.replace("\'", "&apos;"); }
  		if(param.indexOf("<") != -1){ param= param.replace("<", "&lt;"); }
        if(param.indexOf(">") != -1){ param= param.replace(">", "&gt;"); }
        if(param.indexOf("&") != -1){ param= param.replace("&", "&amp;"); }
        if(param.indexOf(">") != -1){ param= param.replace("/", "&#x2F;"); }

  		return param;
  	}


}
