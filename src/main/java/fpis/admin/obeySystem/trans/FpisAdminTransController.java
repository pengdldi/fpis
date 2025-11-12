package fpis.admin.obeySystem.trans;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import egovframework.com.uss.umt.service.EgovMberManageService;
import egovframework.com.uss.umt.service.SigunguVO;
import fpis.admin.accessLog.FpisAccessLogService;
import fpis.admin.accessLog.FpisAccessLogVO;
import fpis.admin.obeySystem.FpisAdminObeySystemService;
import fpis.admin.obeySystem.FpisAdminStatTrans10VO;
import fpis.admin.obeySystem.FpisAdminStatTrans4VO;
import fpis.admin.obeySystem.FpisAdminStatTrans7VO;
import fpis.admin.obeySystem.FpisAdminStatTrans8VO;
import fpis.admin.obeySystem.FpisAdminSysCarMinDataVO;
import fpis.admin.obeySystem.FpisProgressStatusVO;
import fpis.admin.stat.FpisAdminStatBase12VO;
import fpis.admin.stat.FpisAdminStatService;
import fpis.common.service.CommonService;
import fpis.common.utils.FpisUtil;
import fpis.common.utils.Util;
import fpis.common.utils.Util_poi;
import fpis.common.vo.SessionVO;
import fpis.common.vo.usr.UsrInfoVO;
import fpis.online.order.service.FpisOrderContractVO;
import fpis.online.stdinfo.car.service.FpisCarManageService;
import fpis.stat.result.service.FpisStateQueryService;
import twitter4j.internal.org.json.JSONArray;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.org.json.JSONObject;



/**
 *
 * 관리자 - 제도준수-의무제 통계분석  메뉴를 관리하는 컨트롤러
 * @author 김명곤
 *
 *
 *
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2014.09.05 mgkim          관리자 페이지 최소운송기준제 메뉴 추가
 *   2014.09.16 mgkim        지자체 관리자 관리지역 검색 ajax 모듈 구현  @RequestMapping("/uss/umt/FpisSigungu_ajax.do"
 *   2014.09.17 mgkim        지역 업체 검색 기능 구현
 */

@Controller
public class FpisAdminTransController {

	private static final Logger logger = Logger.getLogger(FpisAdminTransController.class);

    //@Resource(name = "FpisStateQueryService")
    //private FpisStateQueryService QuerySvc;        // 2014.09.05 mgkim 최소운송기준 매출표 유무 조회

    @Resource(name = "mberManageService")
    private EgovMberManageService mberManageService;   // 2014.09.05 mgkim 지역 시군구정보 조회

    @Resource(name = "FpisAdminTransService")
    private FpisAdminTransService FpisSvc;           // 2014.09.16 mgkim 지역 업체 검색 기능 추가

    @Resource(name = "FpisAdminObeySystemService")
    private FpisAdminObeySystemService AdminObeySystemService;   /* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 */

    @Resource(name = "FpisStateQueryService")
    private FpisStateQueryService QuerySvc;

	@Resource(name = "FpisCarManageService")
	private FpisCarManageService CarManageService;

	@Resource(name = "CommonService")
	private CommonService commonService;

	@Resource(name = "FpisAdminStatService")
	private FpisAdminStatService adminStatSvc;

	//2020.11.10 ysw 사업자정보 이력을 위한 서비스
	@Resource(name = "FpisAccessLogService")
	private FpisAccessLogService accessLogService;

	@Value(value="#{globals['Globals.fileStorePath']}")
    private String fileStorePath;

	@Value(value="#{globals['Globals.majarStatFilePath']}")
    private String majarStatFilePath;

    /*
     * 2014.09.05 mgkim 관리자 - 제도준수 - 최소운송기준제 [신규] 기본 UI 구현
     * 2014.09.16 mgkim 지역업체 검색 기능 추가
     * 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 끝
     * 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans1.do")
    public String FpisAdminStatTrans1(UsrInfoVO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<UsrInfoVO> compList = null;
        int totCnt   = 0;

        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        /* 지역 업체검색 */
        if (searchSidoCd != null) {

            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sigungu_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectUsrInfoCount_adm(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ------------------

            compList = FpisSvc.selectUsrInfoList_adm(shVO);
            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }

        // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        List<SigunguVO> sidoList = mberManageService.selectSido();
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            sigunList = mberManageService.selectSigungu(vo);
        }
        model.addAttribute("SIGUNLIST", sigunList);


        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("compList" , compList);



        /* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 시작 */
        FpisAdminSysCarMinDataVO sysCarMinDataVO = null;
        List<FpisAdminSysCarMinDataVO> sysCarMinDataRateList = AdminObeySystemService.selectSysCarMinDataRateList(sysCarMinDataVO);
        model.addAttribute("sysCarMinDataRateList", sysCarMinDataRateList);
        /* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 끝 */


        // 2014.01.22 mgkim 년도 데이터 추가
        int strYear = 2014;
        //int endYear = Calendar.getInstance().get(Calendar.YEAR);
        int endYear = 2016; // 2015.07.16 mgkim 데이터마트가 가공되어야만 서비스가 가능 수동으로 온라인마트구축후 변수 설정필요.
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("rcode", "R12");
        model.addAttribute("bcode", "R12-07");

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans1";
    }



    /*
     * 2015.07.16 mgkim 관리자 - 제도준수 - 최소운송기준제 _ 과거년도 데이터마트 조회
     *
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans1_mart.do")
    public String FpisAdminStatTrans1_mart(UsrInfoVO shVO, HttpServletRequest req, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();
        
        Calendar c = Calendar.getInstance();
        // 2014.01.22 mgkim 년도 데이터 추가
        int strYear = 2014;
        int endYear = c.get(Calendar.YEAR)-1; // 2015.07.16 mgkim 데이터마트가 가공되어야만 서비스가 가능 수동으로 마트구축후 변수 설정필요.
        
        // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
     	String DaeguGubun = "";
     	int getYear = 0;
     	if (shVO.getSearch_year() == null) {
     		getYear = endYear;
     	} else {
     		getYear = Integer.parseInt(shVO.getSearch_year());
     	}

        List<UsrInfoVO> compList = null;
        int totCnt   = 0;

        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sigungu_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectUsrInfoMartCarminCount_adm(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ------------------

            compList = FpisSvc.selectUsrInfoMartCarminList_adm(shVO);

            /*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
            String masked_info_status = req.getParameter("masked_info_status");
            if("Y".equals(masked_info_status)) {
            	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
            	/*이력 삽입*/
            	for(int i = 0; i < compList.size(); i++) {
            		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
            		accessLogVO.setRcode(req.getParameter("rcode"));
            		accessLogVO.setBcode(req.getParameter("bcode"));
            		accessLogVO.setComp_mst_key(compList.get(i).getComp_mst_key().replaceAll("-", ""));
            		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO.setJob_cls("SE"); //상세정보보기
            		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            		accessLogVOList.add(accessLogVO);
            	}
            	accessLogService.insertAccessLogByList(accessLogVOList);
            }else {
            	masked_info_status = "N";
            }
            model.addAttribute("masked_info_status", masked_info_status);

            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
            shVO.setSearch_comp_corp_num(org_comp_corp_num);    // 2015.08.07 법인번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }

        // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                DaeguGubun = svo.getAdm_area_code(); // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
                searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            /* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
			if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
				//sigunList = mberManageService.selectSigungu2016(sigunguVO);
			} else {
				sigunList = mberManageService.selectSigungu2016(vo);
			}
        }



        FpisAdminSysCarMinDataVO sysCarMinDataVO = null;
        List<FpisAdminSysCarMinDataVO> sysCarMinDataRateList = AdminObeySystemService.selectSysCarMinDataRateList(sysCarMinDataVO);
        model.addAttribute("sysCarMinDataRateList", sysCarMinDataRateList);
        /* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 끝 */

		if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
		}
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("RCODE", req.getParameter("rcode"));
        model.addAttribute("BCODE", req.getParameter("bcode"));


        model.addAttribute("SIGUNLIST", sigunList);

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("compList" , compList);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans1_mart";
    }


    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans1_mart_excel.do")
    public String FpisAdminStatTrans1_mart_excel(UsrInfoVO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<UsrInfoVO> compList = null;

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sigungu_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }

            compList = FpisSvc.selectUsrInfoMartCarminList_adm_excel(shVO);
        }

        model.addAttribute("compList" , compList);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans1_mart_excel";
    }

    /*
     * 2016.12.22 오승민 관리자 - 제도준수 - 최소운송기준제 _재구현
     *
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans1_renewal.do")
    public String FpisAdminStatTrans1_renewal(FpisAdminStatTrans4VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<FpisAdminStatTrans4VO> compList = null;
        int totCnt   = 0;

        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
            	shVO.setSearch_sido_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectUsrInfoMartCarminCount_renewal(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ------------------

            compList = FpisSvc.selectUsrInfoMartCarminList_renewal(shVO);
            /*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
            String masked_info_status = req.getParameter("masked_info_status");
            if("Y".equals(masked_info_status)) {
            	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
            	/*이력 삽입*/
            	for(int i = 0; i < compList.size(); i++) {
            		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
            		accessLogVO.setRcode(req.getParameter("rcode"));
            		accessLogVO.setBcode(req.getParameter("bcode"));
            		accessLogVO.setComp_mst_key(compList.get(i).getComp_mst_key().replaceAll("-", ""));
            		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO.setJob_cls("SE"); //상세정보보기
            		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            		accessLogVOList.add(accessLogVO);
            	}
            	accessLogService.insertAccessLogByList(accessLogVOList);
            }else {
            	masked_info_status = "N";
            }
            model.addAttribute("masked_info_status", masked_info_status);


            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
            shVO.setSearch_comp_corp_num(org_comp_corp_num);    // 2015.08.07 법인번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }

        // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
                searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            sigunList = mberManageService.selectSigungu2016(vo);
        }

        FpisAdminSysCarMinDataVO sysCarMinDataVO = null;
        List<FpisAdminSysCarMinDataVO> sysCarMinDataRateList = AdminObeySystemService.selectSysCarMinDataRateList(sysCarMinDataVO);
        model.addAttribute("sysCarMinDataRateList", sysCarMinDataRateList);
        /* 2014.10.07 mgkim 양상완 시장평균매출액 구현 기능 반영 끝 */




        // 2014.01.22 mgkim 년도 데이터 추가
        Calendar c = Calendar.getInstance();
        int strYear = 2014;
        int endYear = c.get(Calendar.YEAR)-1; // 2015.07.16 mgkim 데이터마트가 가공되어야만 서비스가 가능 수동으로 마트구축후 변수 설정필요.
        if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
		}
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        /*model.addAttribute("RCODE" , "R13");
        model.addAttribute("BCODE" , "R13-02");*/
        model.addAttribute("RCODE", req.getParameter("rcode"));
        model.addAttribute("BCODE", req.getParameter("bcode"));

        model.addAttribute("SIGUNLIST", sigunList);


        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("compList" , compList);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans1_renewal";
    }


    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans1_renewal_excel.do")
    public String FpisAdminStatTrans1_renewal_excel(FpisAdminStatTrans4VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<FpisAdminStatTrans4VO> compList = null;
        int totCnt = 0;
        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
            	shVO.setSearch_sido_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }

            totCnt = FpisSvc.selectUsrInfoMartCarminCount_renewal(shVO);
            if(totCnt >= 0 && totCnt < 2147483647){
	            shVO.setS_row(0);
	            shVO.setE_row(totCnt+1);
	            shVO.setTot_page(Util.calcurateTPage(totCnt));
	            // PAGING END ------------------
            }
            compList = FpisSvc.selectUsrInfoMartCarminList_renewal(shVO);
        }

        model.addAttribute("compList" , compList);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans1_renewal_excel";
    }



    /*
     * 관리자 - 제도준수 - 시장평균매출액 배차실적 통계분석
     * 2014.10.07 mgkim 최초생성 - 기본 UI 구현
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans3.do")
    public String FpisAdminStatTrans3(UsrInfoVO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        req.getSession().getAttribute("SessionVO");
        // 2014.01.22 mgkim 년도 데이터 추가
        int strYear = 2013;
        int endYear = Calendar.getInstance().get(Calendar.YEAR);
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans3";
    }

    /*
     * 2014.09.05 mgkim 관리자 - 제도준수 - 직접운송의무제 [신규] 기본 UI 구현
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans4.do")
    public String FpisAdminStatTrans4(UsrInfoVO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<UsrInfoVO> compList = null;
        int totCnt   = 0;

        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sigungu_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectUsrInfoCount_adm(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ------------------

            compList = FpisSvc.selectUsrInfoList_adm(shVO);
            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }


        // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }


        List<SigunguVO> sidoList = mberManageService.selectSido();
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            sigunList = mberManageService.selectSigungu(vo);
        }
        model.addAttribute("SIGUNLIST", sigunList);

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("compList" , compList);


        // 2014.01.22 mgkim 년도 데이터 추가
        int strYear = 2015;
        //int endYear = Calendar.getInstance().get(Calendar.YEAR);
        int endYear = 2015; // 2015.07.16 mgkim 데이터마트가 가공되어야만 서비스가 가능 수동으로 온라인마트구축후 변수 설정필요.
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans4";
    }




    /*
     * 2015.07.16 mgkim 관리자 - 제도준수 - 직접운송 현황조회(2014년 실적) 기능구현
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans4_mart.do")
    public String FpisAdminStatTrans4_mart(UsrInfoVO shVO, HttpServletRequest req, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<UsrInfoVO> compList = null;
        int totCnt   = 0;
        
        // 2014.01.22 mgkim 년도 데이터 추가
        Calendar c = Calendar.getInstance();
        int strYear = 2014;
        int endYear = c.get(Calendar.YEAR)-1;; // 2015.07.16 mgkim 데이터마트가 가공되어야만 서비스가 가능 수동으로 마트구축후 변수 설정필요.
        
     // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
     	String DaeguGubun = "";
     	int getYear = 0;
     	if (shVO.getSearch_year() == null) {
     		getYear = endYear;
     	} else {
     		getYear = Integer.parseInt(shVO.getSearch_year());
     	}
        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sigungu_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectUsrInfoMartDirectCount_adm(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ------------------

            compList = FpisSvc.selectUsrInfoMartDirectList_adm(shVO);

            /*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
            String masked_info_status = req.getParameter("masked_info_status");
            if("Y".equals(masked_info_status)) {
            	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
            	/*이력 삽입*/
            	for(int i = 0; i < compList.size(); i++) {
            		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
            		accessLogVO.setRcode(req.getParameter("rcode"));
            		accessLogVO.setBcode(req.getParameter("bcode"));
            		accessLogVO.setComp_mst_key(compList.get(i).getComp_mst_key().replaceAll("-", ""));
            		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO.setJob_cls("SE"); //목록조회
            		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            		accessLogVOList.add(accessLogVO);
            	}
            	accessLogService.insertAccessLogByList(accessLogVOList);
            }else {
            	masked_info_status = "N";
            }
            model.addAttribute("masked_info_status", masked_info_status);

            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
            shVO.setSearch_comp_corp_num(org_comp_corp_num);    // 2015.08.07 법인번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }


        // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                DaeguGubun = svo.getAdm_area_code(); // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
                searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }


        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            /* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
			if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
				//sigunList = mberManageService.selectSigungu2016(sigunguVO);
			} else {
				sigunList = mberManageService.selectSigungu2016(vo);
			}
        }

        if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
		}
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("RCODE", req.getParameter("rcode"));
        model.addAttribute("BCODE", req.getParameter("bcode"));

        model.addAttribute("SIGUNLIST", sigunList);

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("compList" , compList);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans4_mart";
    }


    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans4_mart_excel.do")
    public void FpisAdminStatTrans4_mart_excel(UsrInfoVO shVO, HttpServletRequest req, HttpServletResponse res, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<UsrInfoVO> compList = null;
        String masked_info_status = req.getParameter("masked_info_status");

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sigungu_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }

            compList = FpisSvc.selectUsrInfoMartDirectList_adm_excel(shVO);

            /*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
            if("Y".equals(masked_info_status)) {
            	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
            	/*이력 삽입*/
            	for(int i = 0; i < compList.size(); i++) {
            		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
            		accessLogVO.setRcode(req.getParameter("rcode"));
            		accessLogVO.setBcode(req.getParameter("bcode"));
            		accessLogVO.setComp_mst_key(compList.get(i).getComp_mst_key().replaceAll("-", ""));
            		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO.setJob_cls("EX"); //엑셀다운로드
            		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            		accessLogVOList.add(accessLogVO);
            	}
            	accessLogService.insertAccessLogByList(accessLogVOList);
            }else {
            	masked_info_status = "N";
            }
        }

 // ===========================================================================================================================================
// 엑셀다운로드 시작 ================================================================================================================================
// ===========================================================================================================================================
        //String file_path=EgovProperties.getProperty("Globals.fileStorePath");
        File folder = new File(fileStorePath);//지정된 경로에 폴더를 만든다.
        folder.setExecutable(false);
        folder.setReadable(true);
        folder.setWritable(true);
        if(!folder.exists()){
        	folder.mkdirs();//폴더가 존재 한다면 무시한다.
        }
        /* Create a Workbook and Worksheet */
        XSSFWorkbook workbook = new XSSFWorkbook();


        /* =======================================================================  공통 작업 시작 */
        /* 스타일 작업 */

        XSSFDataFormat format = workbook.createDataFormat();

        // 표 셀 스타일 연블루
        CellStyle cellStyle_td2 = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_td2.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_td2.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_td2.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_td2.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle_td2.setWrapText(true);
        XSSFColor color2 = new XSSFColor(new java.awt.Color(217,229,255)); // 2017.09.28 mgkim RGB적용
        ((XSSFCellStyle) cellStyle_td2).setFillForegroundColor(color2);


        CellStyle cellStyle_center = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_center.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_center.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_center.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_center.setWrapText(true);

        CellStyle cellStyle_left = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_left.setAlignment(CellStyle.ALIGN_LEFT);  //스타일 - 가운데정렬
        cellStyle_left.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_left.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_left.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_left.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_left.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_left.setWrapText(true);

        CellStyle cellStyle_right = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_right.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
        cellStyle_right.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_right.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_right.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_right.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_right.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_right.setWrapText(true);

        CellStyle cellStyle_number = workbook.createCellStyle(); // 스타일 생성 - 숫자
        format = workbook.createDataFormat();
        cellStyle_number.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
        cellStyle_number.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_number.setDataFormat(format.getFormat("_-* #,##0_-;-* #,##0_-;_-* \"-\"_-;_-@_-"));
        cellStyle_number.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_number.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_number.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_number.setBorderBottom(CellStyle.BORDER_THIN);
        /* =======================================================================  공통 작업 끝 */


        XSSFSheet worksheet1 = workbook.createSheet("행정처분결과조회");

        Row row1 = null; //로우
        Cell cell1 = null;// 셀

        row1 = worksheet1.createRow(0); //첫 줄 생성

        worksheet1.setColumnWidth(0, (short)1600);
        worksheet1.setColumnWidth(1, (short)6800);
        worksheet1.setColumnWidth(2, (short)3200);
        worksheet1.setColumnWidth(3, (short)3800);
        worksheet1.setColumnWidth(4, (short)4200);
        worksheet1.setColumnWidth(5, (short)9800);
        worksheet1.setColumnWidth(6, (short)4000);
        worksheet1.setColumnWidth(7, (short)5000);
        worksheet1.setColumnWidth(8, (short)4000);
        worksheet1.setColumnWidth(9, (short)4000);
        worksheet1.setColumnWidth(10, (short)5000);
        worksheet1.setColumnWidth(11, (short)4000);


        Util_poi.setCell(cell1, row1, 0, cellStyle_td2, "순번");
        Util_poi.setCell(cell1, row1, 1, cellStyle_td2, "업체명");
        Util_poi.setCell(cell1, row1, 2, cellStyle_td2, "업태");
        Util_poi.setCell(cell1, row1, 3, cellStyle_td2, "사업자번호");
        Util_poi.setCell(cell1, row1, 4, cellStyle_td2, "법인번호");
        Util_poi.setCell(cell1, row1, 5, cellStyle_td2, "주소");
        Util_poi.setCell(cell1, row1, 6, cellStyle_td2, "화주의뢰실적\n계약금액(원)");
        Util_poi.setCell(cell1, row1, 7, cellStyle_td2, "화주의뢰실적\n미인정금액(원)");
        Util_poi.setCell(cell1, row1, 8, cellStyle_td2, "화주의뢰실적\n직접운송비율(%)");
        Util_poi.setCell(cell1, row1, 9, cellStyle_td2, "운송사의뢰실적\n계약금액(원");
        Util_poi.setCell(cell1, row1, 10, cellStyle_td2, "운송사의뢰실적\n미인정금액(원)");
        Util_poi.setCell(cell1, row1, 11, cellStyle_td2, "운송사의뢰실적\n직접운송비율(%)");
        if(compList != null){
	        for(int i=0 ; i < compList.size() ; i++){
	        	row1 = worksheet1.createRow(i+1);

	        	Util_poi.setCell(cell1, row1, 0, cellStyle_center, String.valueOf(i+1));
	        	Util_poi.setCell(cell1, row1, 1, cellStyle_left, compList.get(i).getComp_nm());
	        	Util_poi.setCell(cell1, row1, 2, cellStyle_center, compList.get(i).getComp_cls_nm());
	        	Util_poi.setCell(cell1, row1, 3, cellStyle_center, compList.get(i).getComp_bsns_num());

	        	if("Y".equals(masked_info_status)) {
	        		Util_poi.setCell(cell1, row1, 5, cellStyle_left, compList.get(i).getAddr1());
	        	}else {
	        		Util_poi.setCell(cell1, row1, 5, cellStyle_left, compList.get(i).getMasked_addr1());
	        	}

	    		Util_poi.setCell(cell1, row1, 4, cellStyle_center,compList.get(i).getComp_corp_num());
	        	Util_poi.setNumberCell4(cell1, row1, 6, cellStyle_number, Long.parseLong(compList.get(i).getSum_rc_step1_tot_charge()));
	        	Util_poi.setNumberCell4(cell1, row1, 7, cellStyle_number, Long.parseLong(compList.get(i).getSum_step1_nopermit_tot_charge()));
	        	Util_poi.setNumberCell2(cell1, row1, 8, cellStyle_number, compList.get(i).getTot_step1_direct_percent());
	        	Util_poi.setNumberCell4(cell1, row1, 9, cellStyle_number, Long.parseLong(compList.get(i).getRc_step2_tot_charge()));
	        	Util_poi.setNumberCell4(cell1, row1, 10, cellStyle_number, Long.parseLong(compList.get(i).getSum_step2_nopermit_tot_charge() ));
	        	Util_poi.setNumberCell2(cell1, row1, 11, cellStyle_number, compList.get(i).getTot_step2_direct_percent());
	        }
        }

        String file_name = Util.getDateFormat3()+"_directList_"+svo.getUser_id()+".xlsx"; //임시저장할 파일 이름
        FileOutputStream output = null;
        PrintWriter out = null;
        try {
            output = new FileOutputStream(fileStorePath+file_name);
            workbook.write(output);//파일쓰기 끝.
            String fileName = shVO.getSearch_year()+"_directList_"+Util.getDateFormat3()+".xlsx";//다운로드할 파일 이름
            out = res.getWriter();
            JSONObject result = new JSONObject();
            result.put("file_path", fileStorePath);
            result.put("file_name", file_name);
            result.put("fileName", fileName);
            //2021.11.03 ysw EgovWebUtil.clearXSSMinimum때문에 json 리턴 에러가 납니다. 보안때문인거같은데 적절하게 고치는게 좋을듯합니다.
            //out.write(EgovWebUtil.clearXSSMinimum(result.toString()));
            out.write(result.toString());
        } catch (FileNotFoundException e) {
        	logger.error("[ERROR] - FileNotFoundException : ", e);
        } catch (IOException e) {
        	logger.error("[ERROR] - IOException : ", e);
        }catch(JSONException e) {
        	logger.error("[ERROR] - JSONException : ", e);
        }finally{
			try {
				if(output != null) output.close();
				if(out != null) out.close();
			}catch(IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			}
		}
    }




    /*
     * 2016.12.22 오승민 관리자 - 제도준수 - 직접운송 현황조회(2015년 실적) 기능구현
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans4_renewal.do")
    public String FpisAdminStatTrans4_renewal(FpisAdminStatTrans4VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        /*
         * 161019 오승민 재생성.
         */
        List<FpisAdminStatTrans4VO> compList = null;
        int totCnt   = 0;

        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
	        if(searchSigunguCd != null && !searchSigunguCd.equals("")){
	            shVO.setSearch_sigungu_cd(searchSigunguCd);
	        }else{
	        	shVO.setSearch_sido_cd(searchSidoCd);
	        }

	        String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
	        if(org_comp_bsns_num != null){
	            shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
	        }
	        String org_comp_corp_num = shVO.getSearch_comp_corp_num();
	        if(org_comp_corp_num != null){
	            shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
	        }

	        totCnt = FpisSvc.selectUsrInfoMartDirectCount_renewal(shVO);
	        shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
	        shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
	        shVO.setTot_page(Util.calcurateTPage(totCnt));
	        // PAGING END ------------------

	        compList = FpisSvc.selectUsrInfoMartDirectList_renewal(shVO);

	        /*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
            String masked_info_status = req.getParameter("masked_info_status");
            if("Y".equals(masked_info_status)) {
            	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
            	/*이력 삽입*/
            	for(int i = 0; i < compList.size(); i++) {
            		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
            		accessLogVO.setRcode(req.getParameter("rcode"));
            		accessLogVO.setBcode(req.getParameter("bcode"));
            		accessLogVO.setComp_mst_key(compList.get(i).getComp_mst_key().replaceAll("-", ""));
            		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO.setJob_cls("SE"); //목록조회
            		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            		accessLogVOList.add(accessLogVO);
            	}
            	accessLogService.insertAccessLogByList(accessLogVOList);
            }else {
            	masked_info_status = "N";
            }
            model.addAttribute("masked_info_status", masked_info_status);

	        shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
	        shVO.setSearch_comp_corp_num(org_comp_corp_num);    // 2015.08.07 법인번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }

        // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
	        model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
	        if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
	        searchSidoCd = svo.getAdm_area_code();
	        model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
	        model.addAttribute("hid_sigungu_code" , searchSigunguCd);
	        }else{
	            model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
	            model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
	            searchSidoCd = svo.getAdm_area_code().substring(0, 2);
	        }
        }else{
	        model.addAttribute("hid_sido_code" , searchSidoCd);
	        model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }
        List<SigunguVO> sidoList = mberManageService.selectSido2016(new  SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
        	vo.setSidoCd(searchSidoCd);
        	sigunList = mberManageService.selectSigungu2016(vo);
        }

        Calendar c = Calendar.getInstance();

        // 2014.01.22 mgkim 년도 데이터 추가
        int strYear = 2014;
        int endYear = c.get(Calendar.YEAR)-1; // 2015.07.16 mgkim 데이터마트가 가공되어야만 서비스가 가능 수동으로 마트구축후 변수 설정필요.

        if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
		}

        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("SIGUNLIST", sigunList);

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("compList" , compList);
        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans4_renewal";
    }

    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans4_renewal_excel.do")
    public void FpisAdminStatTrans4_mart_excel(FpisAdminStatTrans4VO shVO, HttpServletRequest req, HttpServletResponse res, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<FpisAdminStatTrans4VO> compList = null;
        int totCnt = 0;
        String masked_info_status = req.getParameter("masked_info_status");

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
            	shVO.setSearch_sido_cd(searchSidoCd);
            	shVO.setSearch_sigungu_cd(searchSigunguCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectUsrInfoMartDirectCount_renewal(shVO);
            if(totCnt >= 0 && totCnt < 2147483647){
		        shVO.setS_row(0);
		        shVO.setE_row(totCnt+1);
		        shVO.setTot_page(Util.calcurateTPage(totCnt));
            }
            compList = FpisSvc.selectUsrInfoMartDirectList_renewal(shVO);

            /*2020.11.10 ysw 정보노출에 따라 변수 변경해줍니다.*/
            if("Y".equals(masked_info_status)) {
            	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
            	/*이력 삽입*/
            	for(int i = 0; i < compList.size(); i++) {
            		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
            		accessLogVO.setRcode(req.getParameter("rcode"));
            		accessLogVO.setBcode(req.getParameter("bcode"));
            		accessLogVO.setComp_mst_key(compList.get(i).getComp_mst_key().replaceAll("-", ""));
            		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO.setJob_cls("EX"); //엑셀다운로드
            		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            		accessLogVOList.add(accessLogVO);
            	}
            	accessLogService.insertAccessLogByList(accessLogVOList);
            }else {
            	masked_info_status = "N";
            }
        }

 // ===========================================================================================================================================
 // 엑셀다운로드 시작 ================================================================================================================================
 // ===========================================================================================================================================
         //String file_path=EgovProperties.getProperty("Globals.fileStorePath");
         File folder = new File(fileStorePath);//지정된 경로에 폴더를 만든다.
         folder.setExecutable(false);
         folder.setReadable(true);
         folder.setWritable(true);
         if(!folder.exists()){
             folder.mkdirs();//폴더가 존재 한다면 무시한다.
         }
         /* Create a Workbook and Worksheet */
         XSSFWorkbook workbook = new XSSFWorkbook();


         /* =======================================================================  공통 작업 시작 */
         /* 스타일 작업 */
         // 표 셀 스타일 연블루
         CellStyle cellStyle_td2 = workbook.createCellStyle(); //스타일 생성 - 헤더1
         cellStyle_td2.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
         cellStyle_td2.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_td2.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_td2.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_td2.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_td2.setBorderBottom(CellStyle.BORDER_THIN);
         cellStyle_td2.setFillPattern(CellStyle.SOLID_FOREGROUND);
         cellStyle_td2.setWrapText(true);
         XSSFColor color2 = new XSSFColor(new java.awt.Color(217,229,255)); // 2017.09.28 mgkim RGB적용
         ((XSSFCellStyle) cellStyle_td2).setFillForegroundColor(color2);


         CellStyle cellStyle_center = workbook.createCellStyle(); // 스타일 생성 - 일반셀
         cellStyle_center.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
         cellStyle_center.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_center.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_center.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_center.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_center.setBorderBottom(CellStyle.BORDER_THIN);
         cellStyle_center.setWrapText(true);

         CellStyle cellStyle_left = workbook.createCellStyle(); // 스타일 생성 - 일반셀
         cellStyle_left.setAlignment(CellStyle.ALIGN_LEFT);  //스타일 - 가운데정렬
         cellStyle_left.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_left.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_left.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_left.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_left.setBorderBottom(CellStyle.BORDER_THIN);
         cellStyle_left.setWrapText(true);


         CellStyle cellStyle_usrMstKey = workbook.createCellStyle(); // 스타일 생성 - 사업자번호
         XSSFDataFormat format = workbook.createDataFormat();
         cellStyle_usrMstKey.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
         cellStyle_usrMstKey.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_usrMstKey.setDataFormat(format.getFormat("000-00-00000"));
         cellStyle_usrMstKey.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_usrMstKey.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_usrMstKey.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_usrMstKey.setBorderBottom(CellStyle.BORDER_THIN);


         CellStyle cellStyle_compCorpNum = workbook.createCellStyle(); // 스타일 생성 - 법인번호
         cellStyle_compCorpNum.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
         cellStyle_compCorpNum.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_compCorpNum.setDataFormat(format.getFormat("000000-0000000"));
         cellStyle_compCorpNum.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_compCorpNum.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_compCorpNum.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_compCorpNum.setBorderBottom(CellStyle.BORDER_THIN);

         CellStyle cellStyle_right = workbook.createCellStyle(); // 스타일 생성 - 일반셀
         cellStyle_right.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
         cellStyle_right.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_right.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_right.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_right.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_right.setBorderBottom(CellStyle.BORDER_THIN);
         cellStyle_right.setWrapText(true);

         CellStyle cellStyle_number = workbook.createCellStyle(); // 스타일 생성 - 숫자
         format = workbook.createDataFormat();
         cellStyle_number.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
         cellStyle_number.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_number.setDataFormat(format.getFormat("_-* #,##0_-;-* #,##0_-;_-* \"-\"_-;_-@_-"));
         cellStyle_number.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_number.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_number.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_number.setBorderBottom(CellStyle.BORDER_THIN);

         CellStyle cellStyle_float = workbook.createCellStyle(); // 스타일 생성 - 숫자
         format = workbook.createDataFormat();
         cellStyle_float.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
         cellStyle_float.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
         cellStyle_float.setDataFormat(format.getFormat("_-* #,##0.0#_-;-* #,##0.0#_-;_-* \"-\"_-;_-@_-"));
         cellStyle_float.setBorderTop(CellStyle.BORDER_THIN);
         cellStyle_float.setBorderRight(CellStyle.BORDER_THIN);
         cellStyle_float.setBorderLeft(CellStyle.BORDER_THIN);
         cellStyle_float.setBorderBottom(CellStyle.BORDER_THIN);
         /* =======================================================================  공통 작업 끝 */


         XSSFSheet worksheet1 = workbook.createSheet("행정처분결과조회");

         Row row1 = null; //로우
         Cell cell1 = null;// 셀

         row1 = worksheet1.createRow(0); //첫 줄 생성

         worksheet1.setColumnWidth(0, (short)1600);
         worksheet1.setColumnWidth(1, (short)6800);
         worksheet1.setColumnWidth(2, (short)3200);
         worksheet1.setColumnWidth(3, (short)3800);
         worksheet1.setColumnWidth(4, (short)4200);
         worksheet1.setColumnWidth(5, (short)9800);
         worksheet1.setColumnWidth(6, (short)4000);
         worksheet1.setColumnWidth(7, (short)5000);
         worksheet1.setColumnWidth(8, (short)4000);
         worksheet1.setColumnWidth(9, (short)4000);
         worksheet1.setColumnWidth(10, (short)5000);
         worksheet1.setColumnWidth(11, (short)4000);
         if(Integer.parseInt(shVO.getSearch_year()) >= 2017){
             worksheet1.setColumnWidth(12, (short)4000);
             worksheet1.setColumnWidth(13, (short)4000);
         }


         Util_poi.setCell(cell1, row1, 0, cellStyle_td2, "순번");
         Util_poi.setCell(cell1, row1, 1, cellStyle_td2, "업체명");
         Util_poi.setCell(cell1, row1, 2, cellStyle_td2, "업태");
         Util_poi.setCell(cell1, row1, 3, cellStyle_td2, "사업자번호");
         Util_poi.setCell(cell1, row1, 4, cellStyle_td2, "법인번호");
         Util_poi.setCell(cell1, row1, 5, cellStyle_td2, "주소");
         Util_poi.setCell(cell1, row1, 6, cellStyle_td2, "화주의뢰실적\n계약금액(원)");
         Util_poi.setCell(cell1, row1, 7, cellStyle_td2, "화주의뢰실적\n미인정금액(원)");
         Util_poi.setCell(cell1, row1, 8, cellStyle_td2, Integer.parseInt(shVO.getSearch_year()) >= 2017 ? "화주의뢰실적\n직접운송\n미이행률(%)" : "화주의뢰실적\n직접운송비율(%)");
         Util_poi.setCell(cell1, row1, 9, cellStyle_td2, "운송사의뢰실적\n계약금액(원");
         Util_poi.setCell(cell1, row1, 10, cellStyle_td2, "운송사의뢰실적\n미인정금액(원)");
         Util_poi.setCell(cell1, row1, 11, cellStyle_td2, Integer.parseInt(shVO.getSearch_year()) >= 2017 ? "운송사의뢰실적\n직접운송\n미이행률(%)" : "운송사의뢰실적\n직접운송비율(%)");
         if(Integer.parseInt(shVO.getSearch_year()) >= 2017){
             Util_poi.setCell(cell1, row1, 12, cellStyle_td2, "직접운송의무\n미이행률\n(%)");
             Util_poi.setCell(cell1, row1, 13, cellStyle_td2, "직접운송의무\n위탁금지위반");
         }

         if(compList != null){
	         for(int i=0 ; i < compList.size() ; i++){
	         	row1 = worksheet1.createRow(i+1);

	         	Util_poi.setCell(cell1, row1, 0, cellStyle_center, String.valueOf(i+1));
	         	Util_poi.setCell(cell1, row1, 1, cellStyle_left, compList.get(i).getComp_nm());
	         	Util_poi.setCell(cell1, row1, 2, cellStyle_center, FpisUtil.convertCompClsDetail(compList.get(i).getComp_cls_detail()));
	         	Util_poi.setNumberCell4(cell1, row1, 3, cellStyle_usrMstKey, Long.parseLong(compList.get(i).getComp_mst_key()));
	         	if(compList.get(i).getComp_corp_num() == null) {
	         		Util_poi.setCell(cell1, row1, 4, cellStyle_compCorpNum,compList.get(i).getComp_corp_num());
				}else{
					Util_poi.setNumberCell4(cell1, row1, 4, cellStyle_compCorpNum,Long.parseLong(compList.get(i).getComp_corp_num().replace("-", "")));
				}

	         	/*마스킹 처리*/
	         	if("Y".equals(masked_info_status)) {
	         		Util_poi.setCell(cell1, row1, 5, cellStyle_left, compList.get(i).getAddr1());
	         	}else {
	         		Util_poi.setCell(cell1, row1, 5, cellStyle_left, compList.get(i).getMasked_addr1());
	         	}

	         	Util_poi.setNumberCell4(cell1, row1, 6, cellStyle_number, Long.parseLong(compList.get(i).getStep_1_cont())+Long.parseLong(compList.get(i).getStep_t_cont()));
	         	Util_poi.setNumberCell4(cell1, row1, 7, cellStyle_number,
	         			Long.parseLong(compList.get(i).getStep_1_oper_car_03_unvalid())
	 					+ Long.parseLong(compList.get(i).getStep_1_oper_car_04_unvalid())
	 					+ Long.parseLong(compList.get(i).getStep_1_oper_car_etc_unvalid())
	 					+ Long.parseLong(compList.get(i).getStep_1_trust_mang_unvalid() )
	 					+ Long.parseLong(compList.get(i).getStep_1_trust_etc_unvalid() )
	 					+ Long.parseLong(compList.get(i).getStep_t_oper_car_03_unvalid())
	 					+ Long.parseLong(compList.get(i).getStep_t_oper_car_04_unvalid() )
	 					+ Long.parseLong(compList.get(i).getStep_t_oper_car_etc_unvalid() )
	 					+ Long.parseLong(compList.get(i).getStep_t_trust_unvalid())
	     			);
	         	Util_poi.setNumberCell2(cell1, row1, 8, cellStyle_float, Float.parseFloat((Integer.parseInt(shVO.getSearch_year()) >= 2017 ? compList.get(i).getStep_1_not_percent() : compList.get(i).getStep_1_direct_percent())));
	         	Util_poi.setNumberCell4(cell1, row1, 9, cellStyle_number, Long.parseLong(compList.get(i).getStep_2_cont()));
	         	Util_poi.setNumberCell4(cell1, row1, 10, cellStyle_number,
	         			Long.parseLong(compList.get(i).getStep_2_oper_car_03_unvalid() )
	         			+ Long.parseLong(compList.get(i).getStep_2_oper_car_04_unvalid() )
	         			+ Long.parseLong(compList.get(i).getStep_2_oper_car_etc_unvalid() )
	         			+ Long.parseLong(compList.get(i).getStep_2_trust_mang_unvalid() )
	         			+ Long.parseLong(compList.get(i).getStep_2_trust_etc_unvalid()         			)
	     			);
	         	Util_poi.setNumberCell2(cell1, row1, 11, cellStyle_float, Float.parseFloat((Integer.parseInt(shVO.getSearch_year()) >= 2017 ? compList.get(i).getStep_2_not_percent() : compList.get(i).getStep_2_direct_percent())));
	         	if(Integer.parseInt(shVO.getSearch_year()) >= 2017){
	         		Util_poi.setNumberCell2(cell1, row1, 12, cellStyle_float, Float.parseFloat(compList.get(i).getDir_not_percent()));
	         		Util_poi.setCell(cell1, row1, 13, cellStyle_center, compList.get(i).getDir_trust_violation());
	         	}
	         }
         }

         String file_name = Util.getDateFormat3()+"_directList_"+svo.getUser_id()+".xlsx"; //임시저장할 파일 이름
         FileOutputStream output = null;
         PrintWriter out = null;
         try {
             output = new FileOutputStream(fileStorePath+file_name);
             workbook.write(output);//파일쓰기 끝.
             String fileName = shVO.getSearch_year()+"_directList_"+Util.getDateFormat3()+".xlsx";//다운로드할 파일 이름
             out = res.getWriter();
             JSONObject result = new JSONObject();
             result.put("file_path", fileStorePath);
             result.put("file_name", file_name);
             result.put("fileName", fileName);
             out.write(result.toString());	//200409 오승민 json 오류로 xss필터 지움
         } catch (FileNotFoundException e) {
        	 logger.error("[ERROR] - FileNotFoundException : ", e);
         } catch (IOException e) {
        	 logger.error("[ERROR] - IOException : ", e);
         }catch(JSONException e) {
        	 logger.error("[ERROR] - JSONException : ", e);
         }finally{
			try {
				if(output != null) output.close();
 				if(out != null) out.close();
			}catch(IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			}
		}

    }




    /*
     * 관리자 - 제도준수 - 신고/미신고
     * 2014.10.07 mgkim 최초생성 - 기본 UI 구현
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans5.do")
    public String FpisAdminStatTrans5(UsrInfoVO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        String year = req.getParameter("year");
        String month = req.getParameter("month");

        model.addAttribute("year" , (year == null) ? "2015" : year);
        model.addAttribute("month" , (month == null) ? "1" : month);

        List<UsrInfoVO> compList = null;
        int totCnt   = 0;

        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        /* 지역 업체검색 */
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sigungu_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectUsrInfoCount_adm(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ------------------

            compList = FpisSvc.selectUsrInfoList_adm(shVO);
            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }
        // 2014.01.22 mgkim 년도 데이터 추가
        int strYear = 2013;
        int endYear = Calendar.getInstance().get(Calendar.YEAR);
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);
     // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
     	String DaeguGubun = "";
     	int getYear = 0;
     	if (year == null) {
     		getYear = endYear;
     	} else {
     		getYear = Integer.parseInt(year);
     	}

        // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                DaeguGubun = svo.getAdm_area_code(); // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }


        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            /* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
			if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
				//sigunList = mberManageService.selectSigungu2016(sigunguVO);
			} else {
				sigunList = mberManageService.selectSigungu2016(vo);
			}
        }
        model.addAttribute("SIGUNLIST", sigunList);

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("compList" , compList);


        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans5";
    }


    /*2014.10.13 양상완 선택업체의 실적신고 통계분석 ajax*/
	@RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans5_showTrust.do")
	public ModelAndView FpisAdminStatTrans5_showTrust( UsrInfoVO VO
													 ,String work_cls
			                                         ,String year
			                                         ,String month
			                                         ,String usr_mst_key
			                                         ,HttpServletRequest req) throws SQLException,Exception{
		ModelAndView mav = new ModelAndView();
		//if(month.length()=='1') month = '0'+month;
		if(month != null){
			month = (month.length() == 1) ? "0"+month : month;
		}else{
			month = "01";
		}

		VO.setFrom_date(year+"/"+month);
		List<UsrInfoVO> trustList = null;
		if(VO.getWork_cls().equals("01")){
			trustList = FpisSvc.selectTrusrList(VO);
		}else{
			trustList = FpisSvc.selectTrusrList2(VO);
		}

		mav.addObject("from_date",VO.getFrom_date());
		mav.addObject("trustList", trustList);
		mav.addObject("trust_cnt",trustList.size());
		mav.setViewName("jsonView"); // JsonView 형태로 이름을 지정해준다.
		return mav;
	}


	/*2014.10.13 양상완 선택업체의 실적신고 통계분석 ajax*/
	@RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans5_showTrust2.do")
	public ModelAndView FpisAdminStatTrans5_showTrust2( UsrInfoVO VO ,String usr_mst_key ,HttpServletRequest req) throws SQLException,Exception{
		ModelAndView mav = new ModelAndView();
		UsrInfoVO trustVO = FpisSvc.selectTrustVO(VO); // 선택업체간 실적신고 통계비교 위탁업체
		UsrInfoVO trustVO2 = FpisSvc.selectTrustVO2(VO); // 선택업체간 실적신고 통계비교 수탁업체

		String null_chk1 = "0";
		String null_chk2 = "0";
		if(trustVO != null){
			null_chk1 = "1";
		}
		if(trustVO2 != null){
			null_chk2 = "1";
		}
		/*계약 타입 1 = SUP 상위업체*/
		/*계약 타입 2 = SUB 하위업체*/
		mav.addObject("trustVO", trustVO);
		mav.addObject("trustVO2", trustVO2);
		mav.addObject("null_chk1", null_chk1);
		mav.addObject("null_chk2", null_chk2);

		mav.setViewName("jsonView"); // JsonView 형태로 이름을 지정해준다.
		return mav;
	}




//	엑셀파일 변환 및 다운로드
	@RequestMapping("/admin/obeySystem/trans/contract_excel.do")
	public String excelTransform(Map<String,Object> ModelMap ,UsrInfoVO VO) throws SQLException,Exception{
		List<FpisOrderContractVO> contractList = null;
		if(VO.getReg_type().equals("2")){
			contractList=FpisSvc.selectTrustList_sw(VO);
		}else if(VO.getReg_type().equals("1")){
			contractList=FpisSvc.selectContractList_sw(VO);
		}
		ModelMap.put("contractList",contractList);
		ModelMap.put("reg_type",VO.getReg_type());
	      return "TransExcelView"; // excelView 형태로 이름을 지정해준다.
	}


	/*
     * 2015.08.04 mgkim 관리자 - 온라인마트 통계가공 이력정보 보기
     */
    @RequestMapping("/admin/obeySystem/trans/FpisOnlineMartLog.do")
    public String FpisOnlineMartLog(FpisMviewStateVO shVO, HttpServletRequest req, ModelMap model) throws Exception,SQLException {
        FpisMviewStateVO onlineMartState = null;
        List<FpisAdminOnlineMartLogVO> logList01 = null;
        List<FpisAdminOnlineMartLogVO> logList02 = null;
        List<FpisAdminOnlineMartLogVO> logList03 = null;
        List<FpisAdminOnlineMartLogVO> logList04 = null;
        if(shVO.getData_year() != null){
        	onlineMartState = QuerySvc.selectFpisMviewState(shVO);

        	FpisAdminOnlineMartLogVO logVO = new FpisAdminOnlineMartLogVO();
        	logVO.setData_year(shVO.getData_year());
        	logVO.setBungi("1");
        	logList01 = QuerySvc.selectOnlineMartLogList(logVO);
        	logVO.setBungi("2");
        	logList02 = QuerySvc.selectOnlineMartLogList(logVO);
        	logVO.setBungi("3");
        	logList03 = QuerySvc.selectOnlineMartLogList(logVO);
        	logVO.setBungi("4");
        	logList04 = QuerySvc.selectOnlineMartLogList(logVO);
        }

        //FpisAdminOnlineMartLogVO ex = new FpisAdminOnlineMartLogVO();

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("onlineMartState" , onlineMartState);  // 온라인마트 전체 가공상태
        model.addAttribute("logList01"       , logList01);  // 1분기 상세이력
        model.addAttribute("logList02"       , logList02);  // 2분기 상세이력
        model.addAttribute("logList03"       , logList03);  // 3분기 상세이력
        model.addAttribute("logList04"       , logList04);  // 4분기 상세이력

        // 2014.01.22 mgkim 년도 데이터 추가
        int strYear = 2015;
        int endYear = 2015; // 2015.07.16 mgkim 데이터마트가 가공되어야만 서비스가 가능 수동으로 마트구축후 변수 설정필요.
        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        return "/fpis/admin/obeySystem/trans/FpisOnlineMartLog";
    }



    /**
     * 161114 오승민 행정처분 결과조회 구현
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans7.do")
    public String FpisAdminStatTrans7(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        int totCnt   = 0;

        List<FpisAdminStatTrans7VO> dispositionList = null;

        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sido_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectDispositionCount(shVO);
	        shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
	        shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
	        shVO.setTot_page(Util.calcurateTPage(totCnt));
	        // PAGING END ------------------

	        dispositionList = FpisSvc.selectDispositionList(shVO);
            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
            shVO.setSearch_comp_corp_num(org_comp_corp_num);    // 2015.08.07 법인번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }
		int strYear = 2016;
		int endYear = Calendar.getInstance().get(Calendar.YEAR)-1;
        
     // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
     	String DaeguGubun = "";
     	int getYear = 0;
     	if (shVO.getSearch_year() == null) {
     		getYear = endYear;
     	} else {
     		getYear = Integer.parseInt(shVO.getSearch_year());
     	}

     // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                DaeguGubun = svo.getAdm_area_code(); // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
                searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            /* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
			if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
				//sigunList = mberManageService.selectSigungu2016(sigunguVO);
			} else {
				sigunList = mberManageService.selectSigungu2016(vo);
			}
        }
        model.addAttribute("SIGUNLIST", sigunList);

		if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
			shVO.setSearch_minimum_status("A");
			shVO.setSearch_direct_status("A");
		}

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("dispositionList" , dispositionList);

        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("rcode", "R13");
        model.addAttribute("bcode", "R13-07");

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans7";
    }



    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans7_detail.do")
    public String FpisAdminStatTrans7_detail(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        FpisAdminStatTrans7VO vo = new FpisAdminStatTrans7VO();
        vo.setSearch_comp_bsns_num(shVO.getUsr_mst_key());
        vo.setUsr_mst_key(shVO.getUsr_mst_key());
        vo.setSearch_year(shVO.getSearch_year());
        vo.setBase_year(shVO.getSearch_year());
        vo.setIs_reg("Y");	//미인정 결과만 불러오기
        vo.setE_row(2);

        List<FpisAdminStatTrans4VO> minimumList = FpisSvc.selectUsrInfoMartCarminList_renewal(vo);  //최소 결과분석
        List<FpisAdminStatTrans4VO> directList = FpisSvc.selectUsrInfoMartDirectList_renewal(vo);   //직접 결과분석
        List<FpisAdminStatTrans4VO> trustList = FpisSvc.selectUsrInfoMartDirectList_Na_renewal(vo);   //직접(나항) 결과분석

        vo.setDisposition_type("DIRECT");
        int dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0) FpisSvc.insertDisposition(vo);
        vo.setDisposition_type("MINIMUM");
        dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0) FpisSvc.insertDisposition(vo);
        vo.setDisposition_type("TRUST");
        dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0) FpisSvc.insertDisposition(vo);

        FpisAdminStatTrans7VO disposition =  FpisSvc.selectDispositionDetail(vo); //디테일 정보 가져오기
        String masked_info_status = req.getParameter("masked_info_status");
        if("Y".equals(masked_info_status)){
        	/*사업자정보 이력 관리*/
        	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
        	/*이력 삽입*/
    		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
    		accessLogVO.setRcode(req.getParameter("rcode"));
    		accessLogVO.setBcode(req.getParameter("bcode"));
    		accessLogVO.setComp_mst_key(disposition.getUsr_mst_key().replaceAll("-", ""));
    		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
    		accessLogVO.setJob_cls("DE"); //엑셀다운로드
    		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
    		accessLogVOList.add(accessLogVO);
        	accessLogService.insertAccessLogByList(accessLogVOList);
        }else{
        	masked_info_status = "N";
        }

        model.addAttribute("masked_info_status", masked_info_status);

        //최소 결과등록 정보 가져오기
        if("D".equals(disposition.getMinimum_result()) || disposition.getMinimum_result() == null){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("MINIMUM");
        	vo.setDis_seq(disposition.getMinimum_seq());

        	if(disposition.getMinimum_result() != null){
        		FpisAdminStatTrans7VO select_minimum_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
	        	String minimum_cancel_type = select_minimum_cancel.getCancel_type();
	        	String minimum_cancel_period = select_minimum_cancel.getCancel_period();
	        	String minimum_start_date = select_minimum_cancel.getStart_date();
	        	model.addAttribute("minimum_cancel_type"  , minimum_cancel_type);
	        	model.addAttribute("minimum_from_date"  , minimum_start_date);
	        	model.addAttribute("minimum_cancel_period"  , minimum_cancel_period);
        	}

        }else if("P".equals(disposition.getMinimum_result())){  // 과징금 시
        	vo.setDisposition_type("MINIMUM");
        	vo.setDis_seq(disposition.getMinimum_seq());

        	FpisAdminStatTrans7VO select_minimum_fee = FpisSvc.selectDispositionFee(vo);
        	String minimum_fee = select_minimum_fee.getFee();
        	model.addAttribute("minimum_fee"  , minimum_fee);
        }
        //2018.08.29 PES 차량추출기간추가
    	shVO.setS_date(shVO.getSearch_year()+"0101");
    	int e_date = Integer.parseInt(shVO.getSearch_year());
    	shVO.setE_date((e_date+1)+"0101");

        int minimum_totCnt = FpisSvc.selectCancelCarCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(minimum_totCnt));
    	shVO.setDis_seq(disposition.getMinimum_seq());
    	shVO.setDisposition_type("MINIMUM");
    	shVO.setBase_year(shVO.getSearch_year());
    	List<FpisAdminStatTrans7VO> minimum_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	model.addAttribute("MINIMUM_TOTCNT", minimum_totCnt);
    	model.addAttribute("MinimumCancelCar", minimum_cancelCar);




        //직접 결과등록 정보 가져오기
        if("D".equals(disposition.getDirect_result()) || disposition.getDirect_result() == null){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("DIRECT");
        	vo.setDis_seq(disposition.getDirect_seq());

        	if(disposition.getDirect_result() != null){
        		FpisAdminStatTrans7VO select_direct_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
        		String direct_cancel_type = select_direct_cancel.getCancel_type();
        		String direct_cancel_period = select_direct_cancel.getCancel_period();
        		String direct_start_date = select_direct_cancel.getStart_date();
        		model.addAttribute("direct_cancel_type"  , direct_cancel_type);
        		model.addAttribute("direct_from_date"  , direct_start_date);
        		model.addAttribute("direct_cancel_period"  , direct_cancel_period);
        	}

        }else if("P".equals(disposition.getDirect_result())){  // 과징금 시
        	vo.setDisposition_type("DIRECT");
        	vo.setDis_seq(disposition.getDirect_seq());

        	FpisAdminStatTrans7VO select_direct_fee = FpisSvc.selectDispositionFee(vo);
        	String direct_fee = select_direct_fee.getFee();
        	model.addAttribute("direct_fee"  , direct_fee);
        }
        int direct_totCnt = FpisSvc.selectCancelCarCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(direct_totCnt));
    	shVO.setDis_seq(disposition.getDirect_seq());
    	shVO.setDisposition_type("DIRECT");
    	shVO.setBase_year(shVO.getSearch_year());
    	List<FpisAdminStatTrans7VO> direct_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	model.addAttribute("DIRECT_TOTCNT", direct_totCnt);
    	model.addAttribute("DirectCancelCar", direct_cancelCar);



    	 //직접(나항) 결과등록 정보 가져오기
        if("D".equals(disposition.getDirect_trust_result()) || disposition.getDirect_trust_result() == null){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("TRUST");
        	vo.setDis_seq(disposition.getDirect_trust_seq());

        	if(disposition.getDirect_trust_result() != null){
        		FpisAdminStatTrans7VO select_direct_trust_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
        		String direct_trust_cancel_type = select_direct_trust_cancel.getCancel_type();
        		String direct_trust_cancel_period = select_direct_trust_cancel.getCancel_period();
        		String direct_trust_start_date = select_direct_trust_cancel.getStart_date();
        		model.addAttribute("direct_trust_cancel_type"  , direct_trust_cancel_type);
        		model.addAttribute("direct_trust_from_date"  , direct_trust_start_date);
        		model.addAttribute("direct_trust_cancel_period"  , direct_trust_cancel_period);
        	}

        }else if("P".equals(disposition.getDirect_trust_result())){  // 과징금 시
        	vo.setDisposition_type("TRUST");
        	vo.setDis_seq(disposition.getDirect_trust_seq());

        	FpisAdminStatTrans7VO select_direct_trust_fee = FpisSvc.selectDispositionFee(vo);
        	String direct_trust_fee = select_direct_trust_fee.getFee();
        	model.addAttribute("direct_trust_fee"  , direct_trust_fee);
        }
        int direct_trust_totCnt = FpisSvc.selectCancelCarCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(direct_trust_totCnt));
    	shVO.setDis_seq(disposition.getDirect_trust_seq());
    	shVO.setDisposition_type("TRUST");
    	shVO.setBase_year(shVO.getSearch_year());
    	List<FpisAdminStatTrans7VO> direct_trust_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	model.addAttribute("DIRECT_TRUST_TOTCNT", direct_trust_totCnt);
    	model.addAttribute("DirectTrustCancelCar", direct_trust_cancelCar);


        //2018.08.29 PES 행정처분 최대일 계산(최소)
        if(minimumList.size() != 0){
        	if(!minimumList.get(0).getM_no_perform().equals("-")){
        		double m_no_perform = Double.parseDouble(minimumList.get(0).getM_no_perform()) * 0.01;
	            double m_p1 = m_no_perform * 30; //1차 행정처분 최대일
	            double m_p2 = m_no_perform * 60; //2차 행정처분 최대일
	            double m_p3 = m_no_perform * minimum_totCnt; //3차 행정처분 최대대수

	            if(m_p1 <= 0 || (0 < m_p1 && m_p1 <= 1)) m_p1 = 1;
	            //else if(0 < m_p1 && m_p1 <= 1) m_p1 = 1;
	            else m_p1 = Math.floor(m_p1);

	            if(m_p2 <= 0 || (0 < m_p2 && m_p2 <= 1)) m_p2 = 1;
	            //else if(0 < m_p2 && m_p2 <= 1) m_p2 = 1;
	            else m_p2 = Math.floor(m_p2);

	            if(m_p3 <= 0 || (0 < m_p3 && m_p3 <= 1)) m_p3 = 1;
	            //else if(0 < m_p3 && m_p3 <= 1) m_p3 = 1;
	            else m_p3 = Math.floor(m_p3);

	            model.addAttribute("m_p1", (int)m_p1);
	            model.addAttribute("m_p2", (int)m_p2);
	            model.addAttribute("m_p3", (int)m_p3);
	            model.addAttribute("m_no_perform_val", m_no_perform);
        	}
        }

      //2018.08.29 PES 행정처분 최대일 계산(직접)
        if(directList.size() != 0){
        	if(!directList.get(0).getD_no_perform().equals("-")){
        		double d_no_perform = Double.parseDouble(directList.get(0).getD_no_perform()) * 0.01;
	            double d_p1 = d_no_perform * 30; //1차 행정처분 최대일
	            double d_p2 = d_no_perform * 60; //2차 행정처분 최대일
	            double d_p3 = d_no_perform * direct_totCnt; //3차 행정처분 최대대수

	            if(d_p1 <= 0 || (0 < d_p1 && d_p1 <= 1)) d_p1 = 1;
	            //else if(0 < d_p1 && d_p1 <= 1) d_p1 = 1;
	            else d_p1 = Math.floor(d_p1);

	            if(d_p2 <= 0 || (0 < d_p2 && d_p2 <= 1)) d_p2 = 1;
	            //else if(0 < d_p2 && d_p2 <= 1) d_p2 = 1;
	            else d_p2 = Math.floor(d_p2);

	            if(d_p3 <= 0 || (0 < d_p3 && d_p3 <= 1)) d_p3 = 1;
	            //else if(0 < d_p3 && d_p3 <= 1) d_p3 = 1;
	            else d_p3 = Math.floor(d_p3);

	            model.addAttribute("d_p1", (int)d_p1);
	            model.addAttribute("d_p2", (int)d_p2);
	            model.addAttribute("d_p3", (int)d_p3);
	            model.addAttribute("d_no_perform_val", d_no_perform);
        	}
        }

        model.addAttribute("base_year",shVO.getBase_year());

        model.addAttribute("minimumList", minimumList);
        model.addAttribute("directList", directList);
        model.addAttribute("trustList", trustList);
        model.addAttribute("disposition", disposition);
        model.addAttribute("list_cur_page", req.getParameter("list_cur_page"));
        model.addAttribute("VO", shVO);

        model.addAttribute("rcode", "R13");
        model.addAttribute("bcode", "R13-07");

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans7_detail";
    }


    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans7_detail_save.do")
    public void FpisAdminStatTrans7_detail_save(FpisAdminStatTrans7VO vo, HttpServletResponse res, HttpServletRequest req,
    		@RequestParam(value="minimum_totChk[]") List<String> minimum_totChk, @RequestParam(value="direct_totChk[]") List<String> direct_totChk
    		, @RequestParam(value="direct_trust_totChk[]") List<String> direct_trust_totChk
    		) throws SQLException,ParseException{
    	try {
    		DateFormat dateFormat = null;
			Date date = null;
			Calendar cal = null;
			FpisAdminStatTrans7VO addVO = null;

    		/**
    		 * 최소 결과등록
    		 */
			if(!"0".equals(vo.getMinimum_seq())){
	    		vo.setDis_seq(vo.getMinimum_seq());
	    		vo.setContent(vo.getMinimum_content());
	    		vo.setDisposition_type("MINIMUM");
	    		vo.setDisposition_result(vo.getMinimum_result());
	    		vo.setContent_yn(vo.getMinimum_content_yn());
	    		vo.setStatus(vo.getMinimum_status());
	    		vo.setStep(vo.getM_step());

	    		FpisSvc.updateDisposition(vo);    //최소 행정처분 결과 기본데이터 등록

	    		if(vo.getMinimum_rst().equals("D")){ //허가취소 등 행정처분시         ---start_date // end_date
	    			if(Integer.parseInt(vo.getBase_year()) < 2017) vo.setCancel_type(vo.getMinimum_cancel_type());
	    			else vo.setCancel_type(vo.getM_step());

	    			vo.setCancel_period(vo.getMinimum_cancel_period());

	    			int cancelCNT = FpisSvc.selectDispositionCnacelYN(vo);
	    			if(cancelCNT == 0){
	    				FpisSvc.insertDispositionCancel(vo);
	    			}else{
	    				FpisSvc.updateDispositionCancel(vo);
	    			}


	    			if(minimum_totChk != null){
	    				vo.setStart_date(vo.getMinimum_from_date());

	        			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	        			date = dateFormat.parse(req.getParameter("minimum_from_date"));
	        			cal = Calendar.getInstance();
	        			cal.setTime(date);
	        			cal.add(Calendar.DATE, Integer.parseInt(vo.getMinimum_cancel_period()));
	        			vo.setEnd_date(dateFormat.format(cal.getTime()));

	    				List<FpisAdminStatTrans7VO> minimum_totChk_list = new ArrayList<FpisAdminStatTrans7VO>();
	    				String[] aa = minimum_totChk.get(0).split(",");

						for(int i = 0 ; i < aa.length  ; i++) {
							addVO = new FpisAdminStatTrans7VO();
							addVO.setCar_reg_seq(aa[i]);
							addVO.setDis_seq(vo.getDis_seq());
							addVO.setUsr_mst_key(vo.getUsr_mst_key());
							addVO.setBase_year(vo.getBase_year());
							addVO.setDisposition_type(vo.getDisposition_type());
							addVO.setStart_date(vo.getStart_date());
							addVO.setEnd_date(vo.getEnd_date());
							addVO.setStep(vo.getM_step());

							minimum_totChk_list.add(i, addVO);
						}

	    				FpisSvc.deleteDispositionCancelCar(vo);
	    				FpisSvc.insertDispositionCancelCar(minimum_totChk_list);
	    			}
	    			FpisSvc.deleteDispositionFee(vo);

	    		}else if(vo.getMinimum_rst().equals("P")){
	    			vo.setFee(req.getParameter("minimum_fee"));
	    			int feeCNT = FpisSvc.selectDispositionFeeYN(vo);
	    			if(feeCNT == 0){
	    				FpisSvc.insertDispositionFee(vo);
	    			}else{
	    				FpisSvc.updateDispositionFee(vo);
	    			}
	    			FpisSvc.deleteDispositionCancelCar(vo);
	    			FpisSvc.deleteDispositionCancel(vo);
	    		}else{
	    			FpisSvc.deleteDispositionCancelCar(vo);
	    			FpisSvc.deleteDispositionCancel(vo);
	    			FpisSvc.deleteDispositionFee(vo);
	    		}
	    	}




    		/**
    		* 직접 결과등록
    		*/
	    	if(!"0".equals(vo.getDirect_seq() != "0")){
	    		vo.setDis_seq(vo.getDirect_seq());
	    		vo.setContent(vo.getDirect_content());
	    		vo.setDisposition_type("DIRECT");
	    		vo.setDisposition_result(vo.getDirect_result());
	    		vo.setContent_yn(vo.getDirect_content_yn());
	    		vo.setStatus(vo.getDirect_status());
	    		vo.setStep(vo.getD_step());

	    		FpisSvc.updateDisposition(vo);    //최소 행정처분 결과 기본데이터 등록

	    		if(vo.getDirect_rst().equals("D")){ //허가취소 등 행정처분시         ---start_date // end_date
	    			if(Integer.parseInt(vo.getBase_year()) < 2017) vo.setCancel_type(vo.getDirect_cancel_type());
	    			else vo.setCancel_type(vo.getD_step());

	    			vo.setCancel_period(vo.getDirect_cancel_period());

	    			int cancelCNT = FpisSvc.selectDispositionCnacelYN(vo);
	    			if(cancelCNT == 0){
	    				FpisSvc.insertDispositionCancel(vo);
	    			}else{
	    				FpisSvc.updateDispositionCancel(vo);
	    			}


	    			if(direct_totChk != null){
	    				vo.setStart_date(vo.getDirect_from_date());

	        			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	        			date = dateFormat.parse(req.getParameter("direct_from_date"));
	        			cal = Calendar.getInstance();
	        			cal.setTime(date);

	        			cal.add(Calendar.DATE, Integer.parseInt(vo.getDirect_cancel_period()));
	        			vo.setEnd_date(dateFormat.format(cal.getTime()));

	    				List<FpisAdminStatTrans7VO> direct_totChk_list = new ArrayList<FpisAdminStatTrans7VO>();
	    				String[] bb = direct_totChk.get(0).split(",");

						for(int i = 0 ; i < bb.length  ; i++) {
							addVO = new FpisAdminStatTrans7VO();
							addVO.setCar_reg_seq(bb[i]);
							addVO.setDis_seq(vo.getDis_seq());
							addVO.setUsr_mst_key(vo.getUsr_mst_key());
							addVO.setBase_year(vo.getBase_year());
							addVO.setDisposition_type(vo.getDisposition_type());
							addVO.setStart_date(vo.getStart_date());
							addVO.setEnd_date(vo.getEnd_date());
							addVO.setStep(vo.getD_step());

							direct_totChk_list.add(i, addVO);
						}

	    				FpisSvc.deleteDispositionCancelCar(vo);
	    				FpisSvc.insertDispositionCancelCar(direct_totChk_list);
	    			}
	    			FpisSvc.deleteDispositionFee(vo);
	    		}else if(vo.getDirect_rst().equals("P")){
	    			vo.setFee(req.getParameter("direct_fee"));
	    			int feeCNT = FpisSvc.selectDispositionFeeYN(vo);
	    			if(feeCNT == 0){
	    				FpisSvc.insertDispositionFee(vo);
	    			}else{
	    				FpisSvc.updateDispositionFee(vo);
	    			}
	    			FpisSvc.deleteDispositionCancelCar(vo);
	    			FpisSvc.deleteDispositionCancel(vo);
	    		}else{
	    			FpisSvc.deleteDispositionCancelCar(vo);
	    			FpisSvc.deleteDispositionCancel(vo);
	    			FpisSvc.deleteDispositionFee(vo);
	    		}
			}
	    	/**
    		* 직접(나항) 결과등록
    		*/
	    	if(!"0".equals(vo.getDirect_trust_seq() != "0")){
	    		vo.setDis_seq(vo.getDirect_trust_seq());
	    		vo.setContent(vo.getDirect_trust_content());
	    		vo.setDisposition_type("TRUST");
	    		vo.setDisposition_result(vo.getDirect_trust_result());
	    		vo.setContent_yn(vo.getDirect_trust_content_yn());
	    		vo.setStatus(vo.getDirect_trust_status());
	    		vo.setStep(vo.getT_step());

	    		FpisSvc.updateDisposition(vo);    //최소 행정처분 결과 기본데이터 등록

	    		if(vo.getDirect_trust_rst().equals("D")){ //허가취소 등 행정처분시         ---start_date // end_date
	    			if(Integer.parseInt(vo.getBase_year()) < 2017) vo.setCancel_type(vo.getDirect_trust_cancel_type());
	    			else vo.setCancel_type(vo.getT_step());

	    			vo.setCancel_period(vo.getDirect_trust_cancel_period());

	    			int cancelCNT = FpisSvc.selectDispositionCnacelYN(vo);
	    			if(cancelCNT == 0){
	    				FpisSvc.insertDispositionCancel(vo);
	    			}else{
	    				FpisSvc.updateDispositionCancel(vo);
	    			}


	    			if(direct_trust_totChk != null){
	    				vo.setStart_date(vo.getDirect_trust_from_date());

	        			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	        			date = dateFormat.parse(req.getParameter("direct_trust_from_date"));
	        			cal = Calendar.getInstance();
	        			cal.setTime(date);

	        			cal.add(Calendar.DATE, Integer.parseInt(vo.getDirect_trust_cancel_period()));
	        			vo.setEnd_date(dateFormat.format(cal.getTime()));

	    				List<FpisAdminStatTrans7VO> direct_trust_totChk_list = new ArrayList<FpisAdminStatTrans7VO>();
	    				String[] bb = direct_trust_totChk.get(0).split(",");

						for(int i = 0 ; i < bb.length  ; i++) {
							addVO = new FpisAdminStatTrans7VO();
							addVO.setCar_reg_seq(bb[i]);
							addVO.setDis_seq(vo.getDis_seq());
							addVO.setUsr_mst_key(vo.getUsr_mst_key());
							addVO.setBase_year(vo.getBase_year());
							addVO.setDisposition_type(vo.getDisposition_type());
							addVO.setStart_date(vo.getStart_date());
							addVO.setEnd_date(vo.getEnd_date());
							addVO.setStep(vo.getD_step());

							direct_trust_totChk_list.add(i, addVO);
						}

	    				FpisSvc.deleteDispositionCancelCar(vo);
	    				FpisSvc.insertDispositionCancelCar(direct_trust_totChk_list);
	    			}
	    			FpisSvc.deleteDispositionFee(vo);
	    		}else if(vo.getDirect_trust_rst().equals("P")){
	    			vo.setFee(req.getParameter("direct_trust_fee"));
	    			int feeCNT = FpisSvc.selectDispositionFeeYN(vo);
	    			if(feeCNT == 0){
	    				FpisSvc.insertDispositionFee(vo);
	    			}else{
	    				FpisSvc.updateDispositionFee(vo);
	    			}
	    			FpisSvc.deleteDispositionCancelCar(vo);
	    			FpisSvc.deleteDispositionCancel(vo);
	    		}else{
	    			FpisSvc.deleteDispositionCancelCar(vo);
	    			FpisSvc.deleteDispositionCancel(vo);
	    			FpisSvc.deleteDispositionFee(vo);
	    		}
			}
    	}catch(ParseException e) {
    		logger.error("[ERROR] : ", e);
    	}
    }

    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans7_excel.do")
    public void FpisAdminStatTrans7_excel(FpisAdminStatTrans7VO shVO, HttpServletRequest req, HttpServletResponse res, ModelMap model) throws SQLException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        if(svo.getMber_cls().equals("ADM")){
        	searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            searchSigunguCd = svo.getAdm_area_code();
            shVO.setSearch_sido_cd(svo.getAdm_area_code().substring(0, 2));
            shVO.setSearch_sigungu_cd(svo.getAdm_area_code());
            shVO.setSigungu_nm(svo.getAdm_area_name());
        }else{
        	shVO.setSearch_sido_cd(searchSidoCd);
            shVO.setSearch_sigungu_cd(searchSigunguCd);
        }

        List<FpisAdminStatTrans7VO> dispositionList = null;
        int totCnt   = 0;
        int endYear = Calendar.getInstance().get(Calendar.YEAR)-1;
        if (shVO.getSearch_year() == null) {  shVO.setSearch_year(String.valueOf(endYear)); }


        String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
        if(org_comp_bsns_num != null){
            shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
        }
        totCnt = FpisSvc.selectDispositionCount(shVO);
        if(totCnt >= 0 && totCnt < 2147483647){
	        shVO.setS_row(0);
	        shVO.setE_row(totCnt+1);
	        // PAGING END ------------------
        }
        dispositionList = FpisSvc.selectDispositionList(shVO);
        shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환


// ===========================================================================================================================================
// 엑셀다운로드 시작 ================================================================================================================================
// ===========================================================================================================================================
        //String file_path=EgovProperties.getProperty("Globals.fileStorePath");
        File folder = new File(fileStorePath);//지정된 경로에 폴더를 만든다.
        folder.setExecutable(false);
        folder.setReadable(true);
        folder.setWritable(true);
        if(!folder.exists()){
            folder.mkdirs();//폴더가 존재 한다면 무시한다.
        }
        /* Create a Workbook and Worksheet */
        XSSFWorkbook workbook = new XSSFWorkbook();


        /* =======================================================================  공통 작업 시작 */
        /* 스타일 작업 */
        // 표 셀 스타일 연블루
        CellStyle cellStyle_td2 = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_td2.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_td2.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_td2.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_td2.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle_td2.setWrapText(true);
        XSSFColor color2 = new XSSFColor(new java.awt.Color(217,229,255)); // 2017.09.28 mgkim RGB적용
        ((XSSFCellStyle) cellStyle_td2).setFillForegroundColor(color2);


        CellStyle cellStyle_center = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_center.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_center.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_center.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_center.setWrapText(true);


        CellStyle cellStyle_usrMstKey = workbook.createCellStyle(); // 스타일 생성 - 사업자번호
        XSSFDataFormat format = workbook.createDataFormat();
        cellStyle_usrMstKey.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
        cellStyle_usrMstKey.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_usrMstKey.setDataFormat(format.getFormat("000-00-00000"));
        cellStyle_usrMstKey.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_usrMstKey.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_usrMstKey.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_usrMstKey.setBorderBottom(CellStyle.BORDER_THIN);
        /* =======================================================================  공통 작업 끝 */


        XSSFSheet worksheet1 = workbook.createSheet("행정처분결과조회");

        Row row1 = null; //로우
        Cell cell1 = null;// 셀

        row1 = worksheet1.createRow(0); //첫 줄 생성

        worksheet1.setColumnWidth(0, (short)1600);
        worksheet1.setColumnWidth(1, (short)3000);
        worksheet1.setColumnWidth(2, (short)3200);
        worksheet1.setColumnWidth(3, (short)6800);
        worksheet1.setColumnWidth(4, (short)3000);
        worksheet1.setColumnWidth(5, (short)6800);
        worksheet1.setColumnWidth(6, (short)2500);
        worksheet1.setColumnWidth(7, (short)3400);
        worksheet1.setColumnWidth(8, (short)4600);

        Util_poi.setCell(cell1, row1, 0, cellStyle_td2, "순번");
        Util_poi.setCell(cell1, row1, 1, cellStyle_td2, "시도");
        Util_poi.setCell(cell1, row1, 2, cellStyle_td2, "시군구");
        Util_poi.setCell(cell1, row1, 3, cellStyle_td2, "업체명");
        Util_poi.setCell(cell1, row1, 4, cellStyle_td2, "사업자번호");
        Util_poi.setCell(cell1, row1, 5, cellStyle_td2, "행정처분 유형");
        Util_poi.setCell(cell1, row1, 6, cellStyle_td2, "기간");
        Util_poi.setCell(cell1, row1, 7, cellStyle_td2, "최소운송기준\n처리구분");
        Util_poi.setCell(cell1, row1, 8, cellStyle_td2, "직접운송의무\n(비율위반)\n처리구분");
        if(Integer.parseInt(shVO.getSearch_year()) >= 2017){
        	worksheet1.setColumnWidth(9, (short)4600);
        	Util_poi.setCell(cell1, row1, 9, cellStyle_td2, "직접운송의무\n(위탁금지위반)\n처리구분");
        }

        for(int i=0 ; i < dispositionList.size() ; i++){
        	row1 = worksheet1.createRow(i+1);

        	Util_poi.setCell(cell1, row1, 0, cellStyle_center, String.valueOf(i+1));
        	Util_poi.setCell(cell1, row1, 1, cellStyle_center, dispositionList.get(i).getSido_nm());
        	Util_poi.setCell(cell1, row1, 2, cellStyle_center, dispositionList.get(i).getSigungu_nm());
        	Util_poi.setCell(cell1, row1, 3, cellStyle_center, "Y".equals(dispositionList.get(i).getContent_yn()) ? dispositionList.get(i).getComp_nm() + "<작성>" : dispositionList.get(i).getComp_nm());
        	Util_poi.setCell(cell1, row1, 4, cellStyle_usrMstKey, dispositionList.get(i).getUsr_mst_key());
			if("DTM".equals(dispositionList.get(i).getSearch_reg())){
				Util_poi.setCell(cell1, row1, 5, cellStyle_center, "최소,직접(비율),직접(위탁)");
			}else if("DTX".equals(dispositionList.get(i).getSearch_reg())){
				Util_poi.setCell(cell1, row1, 5, cellStyle_center, "직접(비율),직접(위탁)");
			}else if("DXM".equals(dispositionList.get(i).getSearch_reg())){
				Util_poi.setCell(cell1, row1, 5, cellStyle_center, "최소,직접(비율)");
			}else if("DXX".equals(dispositionList.get(i).getSearch_reg())){
				Util_poi.setCell(cell1, row1, 5, cellStyle_center, "직접(비율)");
			}else if("XTX".equals(dispositionList.get(i).getSearch_reg())){
				Util_poi.setCell(cell1, row1, 5, cellStyle_center, "직접(위탁)");
			}else if("XTM".equals(dispositionList.get(i).getSearch_reg())){
				Util_poi.setCell(cell1, row1, 5, cellStyle_center, "최소,직접(위탁)");
			}else if("XXM".equals(dispositionList.get(i).getSearch_reg())){
				Util_poi.setCell(cell1, row1, 5, cellStyle_center, "최소");
			}
        	Util_poi.setCell(cell1, row1, 6, cellStyle_center, dispositionList.get(i).getBase_year());
        	Util_poi.setCell(cell1, row1, 7, cellStyle_center, "X".equals(dispositionList.get(i).getSearch_reg().substring(2,3)) ? "-" :"Y".equals(dispositionList.get(i).getMinimum_status()) ? "입력완료" : "P".equals(dispositionList.get(i).getMinimum_status()) ? "입력중" : "미입력" );
        	Util_poi.setCell(cell1, row1, 8, cellStyle_center, "X".equals(dispositionList.get(i).getSearch_reg().substring(0,1)) ? "-" :"Y".equals(dispositionList.get(i).getDirect_status()) ? "입력완료" : "P".equals(dispositionList.get(i).getDirect_status()) ? "입력중" : "미입력" );
        	if(Integer.parseInt(shVO.getSearch_year()) >= 2017){
        		Util_poi.setCell(cell1, row1, 9, cellStyle_center, "X".equals(dispositionList.get(i).getSearch_reg().substring(1,2)) ? "-" :"Y".equals(dispositionList.get(i).getDirect_trust_status()) ? "입력완료" : "P".equals(dispositionList.get(i).getDirect_trust_status()) ? "입력중" : "미입력" );
        	}//2018.09.17 pes "-" 추가
        }

        String file_name = Util.getDateFormat3()+"_dispositionList_"+svo.getUser_id()+".xlsx"; //임시저장할 파일 이름
        FileOutputStream output = null;
        PrintWriter out = null;
        try {
            output = new FileOutputStream(fileStorePath+file_name);
            workbook.write(output);//파일쓰기 끝.
            String fileName = shVO.getSearch_year()+"_dispositionList"+"_"+Util.getDateFormat3()+".xlsx";//다운로드할 파일 이름
            out = res.getWriter();
            JSONObject result = new JSONObject();
            result.put("file_path", fileStorePath);
            result.put("file_name", file_name);
            result.put("fileName", fileName);
            out.write(result.toString());
        } catch (FileNotFoundException e) {
        	logger.error("[ERROR] - FileNotFoundException : ", e);
        } catch (IOException e) {
        	logger.error("[ERROR] - IOException : ", e);
        }catch(JSONException e) {
        	logger.error("[ERROR] - JSONException : ", e);
        }finally{
			try {
				if(output != null) output.close();
				if(out != null) out.close();
			}catch(IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			}
		}
    }


    /**
     * 170622 오승민 직접/최소 통계 엑셀세부내역 다운로드~
     */
	@RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans_renewal_excel_detail.do")
    public void FpisAdminStatTrans_renewal_excel_detail(FpisAdminStatTrans4VO shVO,
                                      HttpServletRequest req,
                                      HttpServletResponse response) throws SQLException,JSONException,FileNotFoundException,IOException {
        int search_year = Integer.parseInt(req.getParameter("search_year"));

        /*2021.11.01 ysw 사용할 파일패스 문자열 초기화 */
        String createFilePath = null;
        if(search_year == 2016) {
	        FpisAdminStatTrans4VO summaryVO = FpisSvc.selectMinDirSummary(shVO);
	        List<FpisAdminStatTrans8VO> minList =FpisSvc.selectMinDetailList(shVO);
			List<FpisAdminStatTrans8VO> dirList = FpisSvc.selectDirDetailList(shVO);
			List<FpisAdminStatTrans8VO> dirTbList = FpisSvc.selectDirTbDetailList(shVO);

			int otherCompCarCnt = summaryVO.getMin_other_comp_car_cnt();


			//1.템플릿 파일 복사
			String makingDtm = Util.getDateFormat2();
			String excelFileName = makingDtm + "_" + shVO.getUsr_mst_key()+".xls";

			String excelFileSize = "";

			int minCnt = minList.size();
			int dir1Cnt = Integer.parseInt(summaryVO.getSearch_one());
			int dir2Cnt = dirList.size() - dir1Cnt;
			dir1Cnt += dirTbList.size();

			if(minCnt < 65 && dir1Cnt < 100 && dir2Cnt < 100){
				excelFileSize = "_s";
			}else if(minCnt < 330 && dir1Cnt < 500 && dir2Cnt < 500){
				excelFileSize = "_m";
			}else if(minCnt < 650 && dir1Cnt < 1000 && dir2Cnt < 1000){
				excelFileSize = "_l";
			}else if(minCnt < 6500 && dir1Cnt < 10000 && dir2Cnt < 10000){
				excelFileSize = "_xl";
			}else{
				excelFileSize = "_xxl";
			}

			moveToUploadDirectory(excelFileName, excelFileSize);
			PrintWriter pout = null;
			FileOutputStream out = null;
			FileInputStream inputStream = null;

			//2.복사한 파일 메모리 로드
			//String excelFile = EgovProperties.getProperty("Globals.majarStatFilePath") + File.separator + excelFileName;



			createFilePath = majarStatFilePath + File.separator + excelFileName;
			try {
				inputStream = new FileInputStream(new File(createFilePath));

				Workbook workbook = new HSSFWorkbook(inputStream);

				CellStyle wrapCellStyle = workbook.createCellStyle();
				wrapCellStyle.setWrapText(true);

				//3.데이터 채우기

				//3-1. 총괄--------------------------------------------------------------------------------------------------
				Sheet firstSheet = workbook.getSheetAt(0);

				firstSheet.getRow(1).getCell(2).setCellValue(Util.splitUsrMstKey(summaryVO.getUsr_mst_key())); //시트1 - 사업자번호
				firstSheet.getRow(2).getCell(2).setCellValue("2017-06-16"); //시트1 - 작성일시
				firstSheet.getRow(3).getCell(2).setCellValue(summaryVO.getBase_year()+"년"); //시트1 - 기준년도
				firstSheet.getRow(4).getCell(2).setCellValue("신고마감기한 기준 신고데이터"); //시트1 - 기준시점

				firstSheet.getRow(7).getCell(2).setCellValue(Util.Comma_won(summaryVO.getMin_charge())); //시트1 - 최소운송기준금액
				firstSheet.getRow(8).getCell(2).setCellValue(Util.Comma_won(summaryVO.getMin_cont_charge())); //시트1 - 최소운송준수금액
				if(summaryVO.getMinimum_percent() == "0.00"){
					firstSheet.getRow(9).getCell(2).setCellValue("-"); //시트1 - 최소운송준수율
				}else{
					firstSheet.getRow(9).getCell(2).setCellValue(summaryVO.getMinimum_percent()+"%"); //시트1 - 최소운송준수율
				}
				firstSheet.getRow(10).getCell(2).setCellValue(summaryVO.getMin_result()); //시트1 - 최소운송준수여뷰

				firstSheet.getRow(13).getCell(2).setCellValue(summaryVO.getStep_1_result()); //시트1 - 1단계

				firstSheet.getRow(14).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_cont())); //시트1 - 1단계
				firstSheet.getRow(15).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_valid())); //시트1 - 1단계
				firstSheet.getRow(16).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_oper_car_01_valid())); //시트1 - 1단계
				firstSheet.getRow(17).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_oper_car_02_valid())); //시트1 - 1단계
				firstSheet.getRow(18).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_oper_car_03_valid())); //시트1 - 1단계
				firstSheet.getRow(19).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_trust_mang_valid())); //시트1 - 1단계

				firstSheet.getRow(20).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_unvalid())); //시트1 - 1단계
				firstSheet.getRow(21).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_oper_car_03_unvalid())); //시트1 - 1단계
				firstSheet.getRow(22).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_oper_car_out_unvalid())); //시트1 - 1단계
				firstSheet.getRow(23).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_oper_car_04_unvalid())); //시트1 - 1단계
				firstSheet.getRow(24).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_oper_car_not_unvalid())); //시트1 - 1단계
				firstSheet.getRow(25).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_1_trust_mang_unvalid())); //시트1 - 1단계
				//firstSheet.getRow(25).getCell(2).setCellValue(Util.Comma_won(compAllStatBean.getSumFsTrustNotRegistCompUnValidCharge())); //시트1 - 1단계



				firstSheet.getRow(27).getCell(2).setCellValue(summaryVO.getStep_2_result()); //시트1 - 2단계

				firstSheet.getRow(28).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_cont())); //시트1 - 2단계
				firstSheet.getRow(29).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_valid())); //시트1 - 2단계
				firstSheet.getRow(30).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_oper_car_01_valid())); //시트1 - 2단계
				firstSheet.getRow(31).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_oper_car_02_valid())); //시트1 - 2단계
				firstSheet.getRow(32).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_oper_car_03_valid())); //시트1 - 2단계
				firstSheet.getRow(33).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_trust_mang_valid())); //시트1 - 2단계

				firstSheet.getRow(34).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_unvalid())); //시트1 - 2단계
				firstSheet.getRow(35).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_oper_car_03_unvalid())); //시트1 - 2단계
				firstSheet.getRow(36).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_oper_car_out_unvalid())); //시트1 - 2단계
				firstSheet.getRow(37).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_oper_car_04_unvalid())); //시트1 - 2단계
				firstSheet.getRow(38).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_oper_car_not_unvalid())); //시트1 - 2단계
				firstSheet.getRow(39).getCell(2).setCellValue(Util.Comma_won(summaryVO.getStep_2_trust_mang_unvalid())); //시트1 - 2단계
				//firstSheet.getRow(39).getCell(2).setCellValue(Util.Comma_won(compAllStatBean.getSumSsTrustNotRegistCompUnValidCharge())); //시트1 - 2단계


				//3-2. 최소운송 상세내역--------------------------------------------------------------------------------------------------
				Sheet secondSheet = workbook.getSheetAt(1);
				int startrow = 1;

				if(minList == null || minList.size() == 0){
					secondSheet.getRow(startrow).getCell(0).setCellValue("자사 지입차량이 없습니다.");
				}else{
					for(int i = 0 ; i < minList.size(); i++) {
						secondSheet.getRow(startrow).setHeightInPoints((2*secondSheet.getDefaultRowHeightInPoints()));

						int iPlusOne = (i == minList.size()-1) ? i : i+1;
						int iMinusOne = (i == 0) ? 0 : i-1;

	//					if(i == 0 || (minList.get(iPlusOne).getCars_reg_num().equals(minList.get(i).getCars_reg_num()) && iPlusOne != i)){
	//					}else{
	//
	//					}

						if("my".equals(minList.get(i).getCompany_kind())){
							if(minList.get(i).getCars_reg_num().equals(minList.get(iMinusOne).getCars_reg_num())
								&& minList.get(i).getCars_kind().equals(minList.get(iMinusOne).getCars_kind())
								&& minList.get(i).getCars_size().equals(minList.get(iMinusOne).getCars_size())
								&& i != 0
							){
								startrow--;
								secondSheet.getRow(startrow).getCell(5).setCellValue(secondSheet.getRow(startrow).getCell(5).getStringCellValue() + "\n" + minList.get(i).getS_date() + "~" + minList.get(i).getE_date());
							}else{
								secondSheet.getRow(startrow).getCell(0).setCellValue("자사 지입 차량");
								secondSheet.getRow(startrow).getCell(1).setCellValue(minList.get(i).getCars_reg_num());
								secondSheet.getRow(startrow).getCell(2).setCellValue(Util.splitUsrMstKey(minList.get(i).getUsr_mst_key()));
								secondSheet.getRow(startrow).getCell(3).setCellValue(convertCarsKind(minList.get(i).getCars_kind())+"("+minList.get(i).getCars_size()+")");
								secondSheet.getRow(startrow).getCell(4).setCellValue(minList.get(i).getCar_year_days()+"일");
								secondSheet.getRow(startrow).getCell(5).setCellValue(minList.get(i).getS_date() + "~" + minList.get(i).getE_date());
								secondSheet.getRow(startrow).getCell(6).setCellValue(Util.Comma_won(minList.get(i).getCar_min_value_days_20per())+"원("+minList.get(i).getComp_all_144()+")");
								secondSheet.getRow(startrow).getCell(7).setCellValue(("Y".equals(minList.get(i).getCarmin_flag()))? "제외("+minList.get(i).getOrder_cnt_car03_total()+")":"미제외("+minList.get(i).getOrder_cnt_car03_total()+")");
							}
							if((!minList.get(iPlusOne).getCars_reg_num().equals(minList.get(i).getCars_reg_num()) && otherCompCarCnt != 0) || i == iPlusOne){
								startrow++;
								secondSheet.getRow(startrow).getCell(0).setCellValue("타사에서 장기용차로 이용되지 않았습니다.");
								startrow++;
								secondSheet.getRow(startrow).getCell(0).setCellValue("");
								secondSheet.getRow(startrow).getCell(1).setCellValue("");
								secondSheet.getRow(startrow).getCell(2).setCellValue("");
								secondSheet.getRow(startrow).getCell(3).setCellValue("");
								secondSheet.getRow(startrow).getCell(4).setCellValue("");
								secondSheet.getRow(startrow).getCell(5).setCellValue("");
								secondSheet.getRow(startrow).getCell(6).setCellValue("");
								secondSheet.getRow(startrow).setHeight((short)50);
							}
						}else{
							secondSheet.getRow(startrow).getCell(0).setCellValue("타사 장기용차 차량");
							secondSheet.getRow(startrow).getCell(1).setCellValue(minList.get(i).getCars_reg_num());
							secondSheet.getRow(startrow).getCell(2).setCellValue(Util.splitUsrMstKey(minList.get(i).getUsr_mst_key()));
							secondSheet.getRow(startrow).getCell(3).setCellValue("-");
							secondSheet.getRow(startrow).getCell(4).setCellValue("-");
							secondSheet.getRow(startrow).getCell(5).setCellValue("-");
							secondSheet.getRow(startrow).getCell(6).setCellValue(minList.get(i).getComp_one_cnt()+"("+minList.get(i).getComp_one_96()+")");
							secondSheet.getRow(startrow).getCell(7).setCellValue("");
							if(("my".equals(minList.get(iPlusOne).getCompany_kind()) && otherCompCarCnt != 0) || i == iPlusOne){
								startrow++;
								secondSheet.getRow(startrow).getCell(0).setCellValue("");
								secondSheet.getRow(startrow).getCell(1).setCellValue("");
								secondSheet.getRow(startrow).getCell(2).setCellValue("");
								secondSheet.getRow(startrow).getCell(3).setCellValue("");
								secondSheet.getRow(startrow).getCell(4).setCellValue("");
								secondSheet.getRow(startrow).getCell(5).setCellValue("");
								secondSheet.getRow(startrow).getCell(6).setCellValue("");
								secondSheet.getRow(startrow).setHeight((short)50);
							}
						}
						startrow++;


					}
					if(otherCompCarCnt == 0) {
						secondSheet.getRow(startrow).getCell(0).setCellValue("타사에서 장기용차로 이용된 차량이 1대도 없습니다.");
						startrow++;
					}

				}

				secondSheet.autoSizeColumn((short)2);


				//3-3. 직접 상세내역--------------------------------------------------------------------------------------------------
				Sheet thirdSheet = workbook.getSheetAt(2);
				int thirdstartrow = 1;

				Sheet fourthSheet = workbook.getSheetAt(3);
				int fourthstartrow = 1;

				if((dirList == null || dirList.size() == 0)){
					if(dirTbList == null || dirTbList.size() == 0){
						thirdSheet.getRow(thirdstartrow).getCell(0).setCellValue("1단계 정보가 없습니다.");
					}
					fourthSheet.getRow(fourthstartrow).getCell(0).setCellValue("2단계 정보가 없습니다.");
				}else{
					for (int i = 0; i < dirList.size(); i++) {

						if("STEP1".equals(dirList.get(i).getReg_dir_step())){ // 1단계 --------------------------------------------------
							if("OPER".equals(dirList.get(i).getReg_gubun())){ // 1단계 배차
								if("CAR_02_D".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_01".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_02".equals(dirList.get(i).getCar_type_final())
								){
	//								if(thirdstartrow > 2  && thirdSheet.getRow(thirdstartrow-1).getCell(0).getStringCellValue().equals("")){
	//									thirdstartrow--;
	//								}
								}else{
									thirdSheet.getRow(thirdstartrow).getCell(0).setCellValue("배차 실적");
									thirdSheet.getRow(thirdstartrow).getCell(1).setCellValue(dirList.get(i).getYyyymm());

									if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_Y".equals(dirList.get(i).getCar_type_final())
									){
										thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("인정 실적");
										thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())){
											thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("직영");
											thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
										}else if("CAR_02_Y".equals(dirList.get(i).getCar_type_final())){
											thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("지입");
											thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
										}else if("CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
											thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
											thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
										}
									}else if("CAR_03_N".equals(dirList.get(i).getCar_type_final())){
										thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("장기용차 기준회수 미달");
										thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
									}else if("CAR_04_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_04_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_03".equals(dirList.get(i).getCar_type_final())
									){
										thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("등록기간 미달(단기용차)");
										thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("단기용차");
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
									}else if("CAR_03_N_05".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_06".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_99".equals(dirList.get(i).getCar_type_final())
											|| "CAR_99_N".equals(dirList.get(i).getCar_type_final())
									){
										thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("미등록차량 실적");
										thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if("CAR_99_N".equals(dirList.get(i).getCar_type_final())){
											thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("확인불가");
										}else{
											thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
										}
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
									}else if("CAR_03_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_02".equals(dirList.get(i).getCar_type_final())
									){
										thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("장기용차 기한외 실적");
										thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
									}else if("CAR_01_N".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_N".equals(dirList.get(i).getCar_type_final())
									){
										thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("차량등록 기한외 실적");
										thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
	//									thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("CAR_01_N".equals(dirList.get(i).getCar_type_final()) ? "직영" : "지입");
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("확인불가");
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
									}
									thirdSheet.getRow(thirdstartrow).getCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));
									thirdstartrow++;
								}
							}else if("TRUST".equals(dirList.get(i).getReg_gubun())){ // 1단계 위탁
								thirdSheet.getRow(thirdstartrow).getCell(0).setCellValue("위탁 실적");
								thirdSheet.getRow(thirdstartrow).getCell(1).setCellValue(dirList.get(i).getYyyymm());
								if("RT_STEP1_MANG_N".equals(dirList.get(i).getCar_type_final())){
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("인증망을 이용하지 않은 실적");
								}else{
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("인정 실적");
								}
								thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
								thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("-");
								thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
								thirdSheet.getRow(thirdstartrow).getCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));
								thirdstartrow++;
							} //위탁배차 구분 끝

	//						thirdstartrow++;







						}else if("STEP2".equals(dirList.get(i).getReg_dir_step())){  // 2단계 --------------------------------------------------
							if("OPER".equals(dirList.get(i).getReg_gubun())){ // 2단계 배차
								if("CAR_02_D".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_01".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_02".equals(dirList.get(i).getCar_type_final())
								){
	//								if(fourthstartrow > 2 && fourthSheet.getRow(fourthstartrow-1).getCell(0).getStringCellValue().equals("")){
	//									fourthstartrow--;
	//								}
								}else{
									fourthSheet.getRow(fourthstartrow).getCell(0).setCellValue("배차 실적");
									fourthSheet.getRow(fourthstartrow).getCell(1).setCellValue(dirList.get(i).getYyyymm());

									if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_Y".equals(dirList.get(i).getCar_type_final())
									){
										fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("인정 실적");
										fourthSheet.getRow(fourthstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())){
											fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("직영");
											fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue("-");
										}else if("CAR_02_Y".equals(dirList.get(i).getCar_type_final())){
											fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("지입");
											fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue("-");
										}else if("CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
											fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("장기용차");
											fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
										}
									}else if("CAR_03_N".equals(dirList.get(i).getCar_type_final())){
										fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("장기용차 기준회수 미달");
										fourthSheet.getRow(fourthstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("장기용차");
										fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
									}else if("CAR_04_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_04_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_03".equals(dirList.get(i).getCar_type_final())
									){
										fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("등록기간 미달(단기용차)");
										fourthSheet.getRow(fourthstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("단기용차");
										fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue("-");
									}else if("CAR_03_N_05".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_06".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_99".equals(dirList.get(i).getCar_type_final())
											|| "CAR_99_N".equals(dirList.get(i).getCar_type_final())
									){
										fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("미등록차량 실적");
										fourthSheet.getRow(fourthstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if("CAR_99_N".equals(dirList.get(i).getCar_type_final())){
											fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("확인불가");
										}else{
											fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("장기용차");
										}
										fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue("-");
									}else if("CAR_03_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_02".equals(dirList.get(i).getCar_type_final())
									){
										fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("장기용차 기한외 실적");
										fourthSheet.getRow(fourthstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("장기용차");
										fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue("-");
									}else if("CAR_01_N".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_N".equals(dirList.get(i).getCar_type_final())
									){
										fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("차량등록 기한외 실적");
										fourthSheet.getRow(fourthstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
	//									fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("CAR_01_N".equals(dirList.get(i).getCar_type_final()) ? "직영" : "지입");
										fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("확인불가");
										fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue("-");
									}
									fourthSheet.getRow(fourthstartrow).getCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));
									fourthstartrow++;
								}
							}else if("TRUST".equals(dirList.get(i).getReg_gubun())){ // 2단계 위탁
								fourthSheet.getRow(fourthstartrow).getCell(0).setCellValue("위탁 실적");
								fourthSheet.getRow(fourthstartrow).getCell(1).setCellValue(dirList.get(i).getYyyymm());
								if("RT_STEP2_MANG_N".equals(dirList.get(i).getCar_type_final())){
									fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("인증망을 이용하지 않은 실적");
								}else{
									fourthSheet.getRow(fourthstartrow).getCell(2).setCellValue("인정 실적");
								}
								fourthSheet.getRow(fourthstartrow).getCell(3).setCellValue(dirList.get(i).getCars_reg_num());
								fourthSheet.getRow(fourthstartrow).getCell(4).setCellValue("-");
								fourthSheet.getRow(fourthstartrow).getCell(5).setCellValue("-");
								fourthSheet.getRow(fourthstartrow).getCell(6).setCellValue(Util.Comma_won(String.valueOf(dirList.get(i).getCharge_sum())));
								fourthstartrow++;
							} //위탁배차 구분 끝

	//						fourthstartrow++;

						} //단계 구분 끝



					}// 일반실적 반복문 끝

					for (int i = 0; i < dirTbList.size(); i++) {  //택배 실적~~~

						if(thirdstartrow > 65533) { break; };

						if("OPER".equals(dirTbList.get(i).getReg_gubun())){ // 1단계 배차
							if("CAR_02_D".equals(dirTbList.get(i).getCar_type_final())
									|| "CAR_03_D_01".equals(dirTbList.get(i).getCar_type_final())
									|| "CAR_03_D_02".equals(dirTbList.get(i).getCar_type_final())
							){
	//							if(thirdstartrow > 2  && thirdSheet.getRow(thirdstartrow-1).getCell(0).getStringCellValue().equals("")){
	//								thirdstartrow--;
	//							}
							}else{
								thirdSheet.getRow(thirdstartrow).getCell(0).setCellValue("배차 실적");
								thirdSheet.getRow(thirdstartrow).getCell(1).setCellValue(dirTbList.get(i).getYyyymm());

								if("CAR_01_Y".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_02_Y".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_Y".equals(dirTbList.get(i).getCar_type_final())
								){
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("인정 실적");
									thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									if("CAR_01_Y".equals(dirTbList.get(i).getCar_type_final())){
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("직영");
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
									}else if("CAR_02_Y".equals(dirTbList.get(i).getCar_type_final())){
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("지입");
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
									}else if("CAR_03_Y".equals(dirTbList.get(i).getCar_type_final())) {
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
										thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue(dirTbList.get(i).getOrder_year_cnt()+ " / " + dirTbList.get(i).getCar03_cut_oper_year_cnt());
									}
								}else if("CAR_03_N".equals(dirTbList.get(i).getCar_type_final())){
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("장기용차 기준회수 미달");
									thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
									thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue(dirTbList.get(i).getOrder_year_cnt()+ " / " + dirTbList.get(i).getCar03_cut_oper_year_cnt());
								}else if("CAR_04_N_01".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_04_N_02".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_03".equals(dirTbList.get(i).getCar_type_final())
								){
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("등록기간 미달(단기용차)");
									thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("단기용차");
									thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
								}else if("CAR_03_N_05".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_06".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_99".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_99_N".equals(dirTbList.get(i).getCar_type_final())
								){
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("미등록차량 실적");
									thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									if("CAR_99_N".equals(dirTbList.get(i).getCar_type_final())){
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("확인불가");
									}else{
										thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
									}
									thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
								}else if("CAR_03_N_01".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_02".equals(dirTbList.get(i).getCar_type_final())
								){
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("장기용차 기한외 실적");
									thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("장기용차");
									thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
								}else if("CAR_01_N".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_02_N".equals(dirTbList.get(i).getCar_type_final())
								){
									thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("차량등록 기한외 실적");
									thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
	//								thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("CAR_01_N".equals(dirTbList.get(i).getCar_type_final()) ? "직영" : "지입");
									thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("확인불가");
									thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
								}
								thirdSheet.getRow(thirdstartrow).getCell(6).setCellValue(Util.Comma_won(String.valueOf(dirTbList.get(i).getCharge_sum())));
								thirdstartrow++;
							}
						}else if("TRUST".equals(dirTbList.get(i).getReg_gubun())){ // 1단계 위탁
							thirdSheet.getRow(thirdstartrow).getCell(0).setCellValue("위탁 실적");
							thirdSheet.getRow(thirdstartrow).getCell(1).setCellValue(dirTbList.get(i).getYyyymm());
							if("RT_STEP1_MANG_N".equals(dirTbList.get(i).getCar_type_final())){
								thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("인증망을 이용하지 않은 실적");
							}else{
								thirdSheet.getRow(thirdstartrow).getCell(2).setCellValue("인정 실적");
							}
							thirdSheet.getRow(thirdstartrow).getCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
							thirdSheet.getRow(thirdstartrow).getCell(4).setCellValue("-");
							thirdSheet.getRow(thirdstartrow).getCell(5).setCellValue("-");
							thirdSheet.getRow(thirdstartrow).getCell(6).setCellValue(Util.Comma_won(String.valueOf(dirTbList.get(i).getCharge_sum())));
							thirdstartrow++;
						} //위탁배차 구분 끝

	//					thirdstartrow++;
					} //택배 반복문 끝~~~


					if(thirdstartrow == 1){ thirdSheet.getRow(thirdstartrow).getCell(0).setCellValue("1단계 정보가 없습니다."); }
					if(fourthstartrow == 1){ fourthSheet.getRow(fourthstartrow).getCell(0).setCellValue("2단계 정보가 없습니다."); }


				}//직접 상세내역 끝ㅋ


				out = new FileOutputStream(new File(createFilePath));
				workbook.write(out);


				JSONObject json = new JSONObject();

				json.put("fileCls", "99");
				json.put("fileName", excelFileName);

				pout = response.getWriter();

		        pout.write(json.toString());

			}catch(FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			}catch(IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			}finally{
				try {
					if(out != null) out.close();
					if(pout != null) pout.close();
					if(inputStream != null) inputStream.close();
				}catch(IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			}

	        out.close();

	//		model.addAttribute("fileCls", "99");
	//    	model.addAttribute("fileName", excelFileName);
			//엑셀파일 다운로드

	//		return "redirect:/cmm/fms/FpisFileDown_sw.do";



        }else if(search_year >= 2017) {
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
	        List<FpisAdminStatTrans8VO> minList =FpisSvc.selectMinDetailList(shVO); //최소운송 차량 상세정보
	        FpisAdminStatTrans4VO dirSummaryVO = FpisSvc.selectDirSummary(shVO); //직접운송 금액 상세 총괄
			List<FpisAdminStatTrans8VO> dirList = FpisSvc.selectDirDetailList(shVO); //직접운송1단계
			List<FpisAdminStatTrans8VO> dirTbList = FpisSvc.selectDirTbDetailList(shVO); //직접운송2단계
			List<FpisAdminStatTrans4VO> dirVioList = FpisSvc.selectDirVioList(shVO); //직접운송 위탁금지위반 상세정보

			int otherCompCarCnt = summaryVO.getMin_other_comp_car_cnt();


			//1.템플릿 파일 복사 17년도 파일로
			String makingDtm = Util.getDateFormat2();
			String excelFileName = makingDtm + "_" + shVO.getUsr_mst_key()+ "_" + shVO.getSearch_year()+".xls";

			String excelFileSize = "";

			excelFileSize = "_2017";

			moveToUploadDirectory(excelFileName, excelFileSize);

			PrintWriter pout = null;
			FileOutputStream out = null;
			FileInputStream inputStream = null;

			//2.복사한 파일 메모리 로드
			//String excelFile = EgovProperties.getProperty("Globals.majarStatFilePath") + File.separator + excelFileName;
			createFilePath = majarStatFilePath + File.separator + excelFileName;
			try {
				inputStream = new FileInputStream(new File(createFilePath));

				Workbook workbook = new HSSFWorkbook(inputStream);

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
				firstSheet.getRow(1).getCell(3).setCellValue(Util.splitUsrMstKey(summaryVO.getUsr_mst_key())); //사업자번호
				//신규
				firstSheet.getRow(2).getCell(3).setCellValue(summaryVO.getComp_nm());	//업체명
				firstSheet.getRow(3).getCell(3).setCellValue(summaryVO.getCeo());	//대표자
				firstSheet.getRow(4).getCell(3).setCellValue(summaryVO.getComp_cls_detail());	//업종 및 업태

				if(search_year == 2017) {
					firstSheet.getRow(5).getCell(3).setCellValue("2018-08-31"); 	//분석 일시
				}
				firstSheet.getRow(6).getCell(3).setCellValue(summaryVO.getBase_year()+"년"); 	//분석 기준연도
				firstSheet.getRow(7).getCell(3).setCellValue("신고마감기한 기준 신고데이터"); 	//분석 기준시점
				//신규
				firstSheet.getRow(8).getCell(3).setCellValue("basic".equals(summaryVO.getIs_reg()) ? "실적신고" : "no_record".equals(summaryVO.getIs_reg()) ? "실적없음 신고" : "신고된 실적 없음"); 	//실적신고 여부


				//최소운송기준제-----------
				//신규
				firstSheet.getRow(11).getCell(3).setCellValue(summaryVO.getMin_result()); 	//최소운송기준 위반여부
				firstSheet.getRow(12).getCell(3).setCellValue(summaryVO.getMin_not_percent()); 	//미이행률

				firstSheet.getRow(13).getCell(3).setCellValue(Util.Comma_won(summaryVO.getMin_charge())); 	//최소운송기준 준수 필요금액
				firstSheet.getRow(14).getCell(3).setCellValue(Util.Comma_won(summaryVO.getMin_cont_charge())); //실적금액(계약금액)


				//직접운송 의무제----------
				//신규
				firstSheet.getRow(17).getCell(3).setCellValue(summaryVO.getDir_result()); 	//직접운송 비율 위반여부
				firstSheet.getRow(18).getCell(3).setCellValue(summaryVO.getDir_not_percent()); 	//직접운송의무 비율 미이행율

				firstSheet.getRow(20).getCell(3).setCellValue(summaryVO.getStep_1_result()); 	//1단계 준수여부
				firstSheet.getRow(21).getCell(3).setCellValue(Util.Comma_won(summaryVO.getStep_1_cont())); //1단계 계약금액
				firstSheet.getRow(22).getCell(3).setCellValue(Util.Comma_won(summaryVO.getStep_1_valid())); //1단계 인정금액
				firstSheet.getRow(23).getCell(3).setCellValue(Util.Comma_won(summaryVO.getStep_1_unvalid())); //1단계 미인정금액

				firstSheet.getRow(25).getCell(3).setCellValue(summaryVO.getStep_2_result()); //2단계 이상 준수여부
				firstSheet.getRow(26).getCell(3).setCellValue(Util.Comma_won(summaryVO.getStep_2_cont())); //2단계 이상 계약금액
				firstSheet.getRow(27).getCell(3).setCellValue(Util.Comma_won(summaryVO.getStep_2_valid())); //2단계 이상 인정금액
				firstSheet.getRow(28).getCell(3).setCellValue(Util.Comma_won(summaryVO.getStep_2_unvalid())); //2단계 이상 미인정금액

				//신규
				firstSheet.getRow(30).getCell(3).setCellValue(summaryVO.getTrust_violation()); 	//위탁금지 위반여부
				firstSheet.getRow(31).getCell(3).setCellValue(summaryVO.getTrust_violation_cnt()); 	//위탁금지 위반 건수




				//3-2. 최소운송 상세내역--------------------------------------------------------------------------------------------------
				Sheet secondSheet = workbook.getSheetAt(1);  // 두번째 시트 가져오기
				int startrow = 1; //시작 row 셋팅
				if(minList == null || minList.size() == 0){ // 시트에 데이터가 없을 때
					secondSheet.createRow(startrow).createCell(0).setCellValue("자사 지입차량이 없습니다.");
				}else{
					// 데이터 존재
					for(int i = 0 ; i < minList.size(); i++) {

						Row row = null;
						if(secondSheet.getRow(startrow) == null) {
							row = secondSheet.createRow(startrow);
						}else {
							row = secondSheet.getRow(startrow);
						}

						row.setHeightInPoints((2*secondSheet.getDefaultRowHeightInPoints()));

						int iPlusOne = (i == minList.size()-1) ? i : i+1;
						int iMinusOne = (i == 0) ? 0 : i-1;

						if("my".equals(minList.get(i).getCompany_kind())){
							if(minList.get(i).getCars_reg_num().equals(minList.get(iMinusOne).getCars_reg_num())
								&& minList.get(i).getCars_kind().equals(minList.get(iMinusOne).getCars_kind())
								&& minList.get(i).getCars_size().equals(minList.get(iMinusOne).getCars_size())
								&& i != 0
							){
								startrow--;

								if(secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								}else {
									row = secondSheet.getRow(startrow);
								}

								row.createCell(5).setCellValue(row.getCell(5).getStringCellValue() + "\n" + minList.get(i).getS_date() + "~" + minList.get(i).getE_date());
								row.createCell(5).setCellStyle(cellformat_solid);
							}else{
								row.createCell(0).setCellValue("자사 지입 차량");
								row.createCell(1).setCellValue(minList.get(i).getCars_reg_num());
								row.createCell(2).setCellValue(Util.splitUsrMstKey(minList.get(i).getUsr_mst_key()));
								row.createCell(3).setCellValue(convertCarsKind(minList.get(i).getCars_kind())+"("+minList.get(i).getCars_size()+")");
								row.createCell(4).setCellValue(minList.get(i).getCar_year_days()+"일");
								row.createCell(5).setCellValue(minList.get(i).getS_date() + "~" + minList.get(i).getE_date());
								row.createCell(6).setCellValue(Util.Comma_won(minList.get(i).getCar_min_value_days_20per())+"원("+minList.get(i).getComp_all_144()+")");
								row.createCell(7).setCellValue(("Y".equals(minList.get(i).getCarmin_flag()))? "제외("+minList.get(i).getOrder_cnt_car03_total()+")":"미제외("+minList.get(i).getOrder_cnt_car03_total()+")");

								row.getCell(0).setCellStyle(cellformat_solid);
								row.getCell(1).setCellStyle(cellformat_solid);
								row.getCell(2).setCellStyle(cellformat_solid);
								row.getCell(3).setCellStyle(cellformat_solid);
								row.getCell(4).setCellStyle(cellformat_solid);
								row.getCell(5).setCellStyle(cellformat_solid);
								row.getCell(6).setCellStyle(cellformat_solid);
								row.getCell(7).setCellStyle(cellformat_solid);
							}

							if((!minList.get(iPlusOne).getCars_reg_num().equals(minList.get(i).getCars_reg_num()) && otherCompCarCnt != 0) || i == iPlusOne){
								startrow++;
								if(secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								}else {
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


								row.setHeight((short)500);

								startrow++;
								if(secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								}else {
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

								row.setHeight((short)50);
							}
						}else{
							row.createCell(0).setCellValue("타사 장기용차 차량");
							row.createCell(1).setCellValue(minList.get(i).getCars_reg_num());
							row.createCell(2).setCellValue(Util.splitUsrMstKey(minList.get(i).getUsr_mst_key()));
							row.createCell(3).setCellValue("-");
							row.createCell(4).setCellValue("-");
							row.createCell(5).setCellValue("-");
							row.createCell(6).setCellValue(minList.get(i).getComp_one_cnt()+"("+minList.get(i).getComp_one_96()+")");
							row.createCell(7).setCellValue("");

							row.getCell(0).setCellStyle(cellformat_solid);
							row.getCell(1).setCellStyle(cellformat_solid);
							row.getCell(2).setCellStyle(cellformat_solid);
							row.getCell(3).setCellStyle(cellformat_solid);
							row.getCell(4).setCellStyle(cellformat_solid);
							row.getCell(5).setCellStyle(cellformat_solid);
							row.getCell(6).setCellStyle(cellformat_solid);
							row.getCell(7).setCellStyle(cellformat_solid);

							if(("my".equals(minList.get(iPlusOne).getCompany_kind()) && otherCompCarCnt != 0) || i == iPlusOne){
								startrow++;
								if(secondSheet.getRow(startrow) == null) {
									row = secondSheet.createRow(startrow);
								}else {
									row = secondSheet.getRow(startrow);
								}
								row.createCell(0).setCellValue("");
								row.createCell(1).setCellValue("");
								row.createCell(2).setCellValue("");
								row.createCell(3).setCellValue("");
								row.createCell(4).setCellValue("");
								row.createCell(5).setCellValue("");
								row.createCell(6).setCellValue("");
								row.setHeight((short)50);
							}
						}
						startrow++;


					}

					if(otherCompCarCnt == 0) {
						Row row = null;
						if(secondSheet.getRow(startrow) == null) {
							row = secondSheet.createRow(startrow);
						}else {
							row = secondSheet.getRow(startrow);
						}
						row.createCell(0).setCellValue("타사에서 장기용차로 이용된 차량이 1대도 없습니다.");
						startrow++;
					}

				}
				secondSheet.autoSizeColumn((short)2);

				//3-3. 직접운송 금액 상세 총괄 시트--------------------------------------------------------------------------------------------------
				Sheet Sheet_3 = workbook.getSheetAt(2);  // 직접운송 위탁금지위반 상세정보 시트 가져오기

				//직접운송 금액 상세 총괄 쿼리 List 아마 size 1이겠지

				// 데이터 존재
				//row 객체 가져오기
				Sheet_3.getRow(1).getCell(3).setCellValue(dirSummaryVO.getDir_result()); // 직접운송 비율 위반여부
				Sheet_3.getRow(2).getCell(3).setCellValue(dirSummaryVO.getDir_not_percent() + "%"); // 직접운송 비율 미이행율

				Sheet_3.getRow(4).getCell(3).setCellValue(dirSummaryVO.getStep_1_result()); // 1단계 준수여부
				Sheet_3.getRow(5).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_cont()));	//1단계 계약금액


				Sheet_3.getRow(6).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_valid())); //1단계 인정금액 - 합계
				Sheet_3.getRow(7).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_01_valid())); //1단계인정금액 - 직영차량 배차실적
				Sheet_3.getRow(8).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_02_valid())); //1단계인정금액 - 위수탁 차량 배차실적
				Sheet_3.getRow(9).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_03_valid())); //1단계인정금액 - 장기용차 배차실적
				Sheet_3.getRow(10).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_trust_mang_valid())); //1단계인정금액 - 위탁 실적

				Sheet_3.getRow(11).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_unvalid()));	//1단계 미인정금액 - 합계
				Sheet_3.getRow(12).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_03_unvalid()));	//1단계 미인정금액 - 장기용차 배차실적
				Sheet_3.getRow(13).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_out_unvalid()));	//1단계 미인정금액 - 장기용차 기한외 실적
				Sheet_3.getRow(14).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_04_unvalid()));	//1단계 미인정금액 - 단기용차 배차실적
				Sheet_3.getRow(15).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_oper_car_not_unvalid()));	//1단계 미인정금액 - FPIS상 미등록차량 배차실적
				Sheet_3.getRow(16).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_1_trust_mang_unvalid()));	//1단계 미인정금액 - 화물정보망 미이용 위탁실적

				Sheet_3.getRow(18).getCell(3).setCellValue(dirSummaryVO.getStep_2_result());	//2단계 이상 준수여부
				Sheet_3.getRow(19).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_cont()));	//2단계 이상 계약금액

				Sheet_3.getRow(20).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_valid()));	//2단계 이상 인정금액 - 합계
				Sheet_3.getRow(21).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_01_valid()));	//2단계 이상 인정금액 - 직영차량 배차실적
				Sheet_3.getRow(22).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_02_valid()));	//2단계 이상 인정금액 - 위수탁 차량 배차실적
				Sheet_3.getRow(23).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_03_valid()));	//2단계 이상 인정금액 - 장기용차 배차실적
				Sheet_3.getRow(24).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_trust_mang_valid()));	//2단계 이상 인정금액 - 위탁 실적

				Sheet_3.getRow(25).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_unvalid()));	//2단계 이상 미인정금액 - 합계
				Sheet_3.getRow(26).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_03_unvalid()));	//2단계 이상 미인정금액 - 장기용차 배차실적
				Sheet_3.getRow(27).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_out_unvalid()));	//2단계 이상 미인정금액 - 장기용차 기한외 실적
				Sheet_3.getRow(28).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_04_unvalid()));	//2단계 이상 미인정금액 - 단기용차 배차실적
				Sheet_3.getRow(29).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_oper_car_not_unvalid()));	//2단계 이상 미인정금액 - FPIS상 미등록차량 배차실적
				Sheet_3.getRow(30).getCell(3).setCellValue(Util.Comma_won(dirSummaryVO.getStep_2_trust_mang_unvalid()));	//2단계 이상 미인정금액 - 화물정보망 미이용 위탁실적





				//3-4. 직접 상세내역--------------------------------------------------------------------------------------------------
				Sheet thirdSheet = workbook.getSheetAt(3);
				int thirdstartrow = 1;

				Sheet fourthSheet = workbook.getSheetAt(4);
				int fourthstartrow = 1;

				if((dirList == null || dirList.size() == 0)){
					if(dirTbList == null || dirTbList.size() == 0){
						thirdSheet.createRow(thirdstartrow).createCell(0).setCellValue("1단계 정보가 없습니다.");
					}
					fourthSheet.createRow(fourthstartrow).createCell(0).setCellValue("2단계 정보가 없습니다.");
				}else{

					for (int i = 0; i < dirList.size(); i++) {


						if("STEP1".equals(dirList.get(i).getReg_dir_step())){// 1단계 ------------------


							if("OPER".equals(dirList.get(i).getReg_gubun())){ // 1단계 배차


								if("CAR_02_D".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_01".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_02".equals(dirList.get(i).getCar_type_final())
								){
								}else{
									Row row = null;
									if(thirdSheet.getRow(thirdstartrow) == null) {
										row = thirdSheet.createRow(thirdstartrow);
									}else {
										row = thirdSheet.getRow(thirdstartrow);
									}

									row.createCell(0).setCellValue("배차 실적");
									row.createCell(1).setCellValue(dirList.get(i).getYyyymm());

									if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_Y".equals(dirList.get(i).getCar_type_final())
									){
										row.createCell(2).setCellValue("인정 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())){
											row.createCell(4).setCellValue("직영");
											row.createCell(5).setCellValue("-");
										}else if("CAR_02_Y".equals(dirList.get(i).getCar_type_final())){
											row.createCell(4).setCellValue("지입");
											row.createCell(5).setCellValue("-");
										}else if("CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("장기용차");
											row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
										}
									}else if("CAR_03_N".equals(dirList.get(i).getCar_type_final())){
										row.createCell(2).setCellValue("장기용차 기준회수 미달");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
									}else if("CAR_04_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_04_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_03".equals(dirList.get(i).getCar_type_final())
									){
										row.createCell(2).setCellValue("등록기간 미달(단기용차)");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("단기용차");
										row.createCell(5).setCellValue("-");
									}else if( "CAR_99_N".equals(dirList.get(i).getCar_type_final())
									){
										row.createCell(2).setCellValue("미등록차량 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("확인불가");										
										row.createCell(5).setCellValue("-");
									}else if("CAR_03_N_01".equals(dirList.get(i).getCar_type_final())											
											|| "CAR_03_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_05".equals(dirList.get(i).getCar_type_final())	
											|| "CAR_03_N_06".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_99".equals(dirList.get(i).getCar_type_final()) //20230922 CAR_03_N_05,06,99 chbaek 미등록차량 실적 - 장기용차에서 장기용차 기한외 실적 - 장기용차로 이동
									){
										row.createCell(2).setCellValue("장기용차 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue("-");
									}else if("CAR_01_N".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_N".equals(dirList.get(i).getCar_type_final())
									){
										row.createCell(2).setCellValue("차량등록 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
	//									row.createCell(4).setCellValue("CAR_01_N".equals(dirList.get(i).getCar_type_final()) ? "직영" : "지입");
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


									row.setHeight((short)650);
									thirdstartrow++;
								}

							}else if("TRUST".equals(dirList.get(i).getReg_gubun())){ // 1단계 위탁
								Row row = null;
								if(thirdSheet.getRow(thirdstartrow) == null) {
									row = thirdSheet.createRow(thirdstartrow);
								}else {
									row = thirdSheet.getRow(thirdstartrow);
								}
								row.createCell(0).setCellValue("위탁 실적");
								row.createCell(1).setCellValue(dirList.get(i).getYyyymm());
								if("RT_STEP1_MANG_N".equals(dirList.get(i).getCar_type_final())){
									row.createCell(2).setCellValue("인증망을 이용하지 않은 실적");
								}else if("RT_TB_ONE_N".equals(dirList.get(i).getCar_type_final())){
									row.createCell(2).setCellValue("인증망을 이용하지 않은 실적(택배)");
								}else{
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


								row.setHeight((short)650);
								thirdstartrow++;

							} //위탁배차 구분 끝

	//						thirdstartrow++;




						}else if("STEP2".equals(dirList.get(i).getReg_dir_step())){  // 2단계 --------------------------------------------------
							if("OPER".equals(dirList.get(i).getReg_gubun())){ // 2단계 배차
								if("CAR_02_D".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_01".equals(dirList.get(i).getCar_type_final())
										|| "CAR_03_D_02".equals(dirList.get(i).getCar_type_final())
								){
	//								if(fourthstartrow > 2 && fourthSheet.getRow(fourthstartrow-1).createCell(0).getStringCellValue().equals("")){
	//									fourthstartrow--;
	//								}
								}else{
									Row row = null;
									if(fourthSheet.getRow(fourthstartrow) == null) {
										row = fourthSheet.createRow(fourthstartrow);
									}else {
										row = fourthSheet.getRow(fourthstartrow);
									}
									row.createCell(0).setCellValue("배차 실적");
									row.createCell(1).setCellValue(dirList.get(i).getYyyymm());

									if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_Y".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_Y".equals(dirList.get(i).getCar_type_final())
									){
										row.createCell(2).setCellValue("인정 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										if("CAR_01_Y".equals(dirList.get(i).getCar_type_final())){
											row.createCell(4).setCellValue("직영");
											row.createCell(5).setCellValue("-");
										}else if("CAR_02_Y".equals(dirList.get(i).getCar_type_final())){
											row.createCell(4).setCellValue("지입");
											row.createCell(5).setCellValue("-");
										}else if("CAR_03_Y".equals(dirList.get(i).getCar_type_final())) {
											row.createCell(4).setCellValue("장기용차");
											row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
										}
									}else if("CAR_03_N".equals(dirList.get(i).getCar_type_final())){
										row.createCell(2).setCellValue("장기용차 기준회수 미달");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue(dirList.get(i).getOrder_year_cnt()+ " / " + dirList.get(i).getCar03_cut_oper_year_cnt());
									}else if("CAR_04_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_04_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_03".equals(dirList.get(i).getCar_type_final())
									){
										row.createCell(2).setCellValue("등록기간 미달(단기용차)");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("단기용차");
										row.createCell(5).setCellValue("-");
									}else if("CAR_99_N".equals(dirList.get(i).getCar_type_final())){
										row.createCell(2).setCellValue("미등록차량 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());										
										row.createCell(4).setCellValue("확인불가");										
										row.createCell(5).setCellValue("-");
									}else if("CAR_03_N_01".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_02".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_05".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_06".equals(dirList.get(i).getCar_type_final())
											|| "CAR_03_N_99".equals(dirList.get(i).getCar_type_final()) //20230922 CAR_03_N_05,06,99 chbaek 미등록차량 실적 - 장기용차에서 장기용차 기한외 실적 - 장기용차로 이동
									){
										row.createCell(2).setCellValue("장기용차 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue("-");
									}else if("CAR_01_N".equals(dirList.get(i).getCar_type_final())
											|| "CAR_02_N".equals(dirList.get(i).getCar_type_final())
									){
										row.createCell(2).setCellValue("차량등록 기한외 실적");
										row.createCell(3).setCellValue(dirList.get(i).getCars_reg_num());
	//									row.createCell(4).setCellValue("CAR_01_N".equals(dirList.get(i).getCar_type_final()) ? "직영" : "지입");
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

									row.setHeight((short)650);
									fourthstartrow++;
								}
							}else if("TRUST".equals(dirList.get(i).getReg_gubun())){ // 2단계 위탁

								Row row = null;
								if(fourthSheet.getRow(fourthstartrow) == null) {
									row = fourthSheet.createRow(fourthstartrow);
								}else {
									row = fourthSheet.getRow(fourthstartrow);
								}

								row.createCell(0).setCellValue("위탁 실적");
								row.createCell(1).setCellValue(dirList.get(i).getYyyymm());
								if("RT_STEP2_MANG_N".equals(dirList.get(i).getCar_type_final())){
									row.createCell(2).setCellValue("인증망을 이용하지 않은 실적");
								}else if("RT_STEP1_MANG_N_OUT".equals(dirList.get(i).getCar_type_final())||"RT_STEP2_MANG_N_OUT".equals(dirList.get(i).getCar_type_final())){
									row.createCell(2).setCellValue("인증망 사업기간 외 이용 실적");
								}else{
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

								row.setHeight((short)650);

								fourthstartrow++;
							} //위탁배차 구분 끝

	//						fourthstartrow++;

						} //단계 구분 끝



					}// 일반실적 반복문 끝

					for (int i = 0; i < dirTbList.size(); i++) {  //택배 실적~~~

						if(thirdstartrow > 65533) { break; };

						if("OPER".equals(dirTbList.get(i).getReg_gubun())){ // 1단계 배차
							if("CAR_02_D".equals(dirTbList.get(i).getCar_type_final())
									|| "CAR_03_D_01".equals(dirTbList.get(i).getCar_type_final())
									|| "CAR_03_D_02".equals(dirTbList.get(i).getCar_type_final())
							){
	//							if(thirdstartrow > 2  && thirdSheet.getRow(thirdstartrow-1).createCell(0).getStringCellValue().equals("")){
	//								thirdstartrow--;
	//							}
							}else{
								Row row = null;
								if(thirdSheet.getRow(thirdstartrow) == null) {
									row = thirdSheet.createRow(thirdstartrow);
								}else {
									row = thirdSheet.getRow(thirdstartrow);
								}

								row.createCell(0).setCellValue("배차 실적");
								row.createCell(1).setCellValue(dirTbList.get(i).getYyyymm());

								if("CAR_01_Y".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_02_Y".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_Y".equals(dirTbList.get(i).getCar_type_final())
								){
									row.createCell(2).setCellValue("인정 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									if("CAR_01_Y".equals(dirTbList.get(i).getCar_type_final())){
										row.createCell(4).setCellValue("직영");
										row.createCell(5).setCellValue("-");
									}else if("CAR_02_Y".equals(dirTbList.get(i).getCar_type_final())){
										row.createCell(4).setCellValue("지입");
										row.createCell(5).setCellValue("-");
									}else if("CAR_03_Y".equals(dirTbList.get(i).getCar_type_final())) {
										row.createCell(4).setCellValue("장기용차");
										row.createCell(5).setCellValue(dirTbList.get(i).getOrder_year_cnt()+ " / " + dirTbList.get(i).getCar03_cut_oper_year_cnt());
									}
								}else if("CAR_03_N".equals(dirTbList.get(i).getCar_type_final())){
									row.createCell(2).setCellValue("장기용차 기준회수 미달");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("장기용차");
									row.createCell(5).setCellValue(dirTbList.get(i).getOrder_year_cnt()+ " / " + dirTbList.get(i).getCar03_cut_oper_year_cnt());
								}else if("CAR_04_N_01".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_04_N_02".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_03".equals(dirTbList.get(i).getCar_type_final())
								){
									row.createCell(2).setCellValue("등록기간 미달(단기용차)");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("단기용차");
									row.createCell(5).setCellValue("-");
								}else if( "CAR_99_N".equals(dirTbList.get(i).getCar_type_final())){
									row.createCell(2).setCellValue("미등록차량 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("확인불가");									
									row.createCell(5).setCellValue("-");
								}else if("CAR_03_N_01".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_02".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_05".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_06".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_03_N_99".equals(dirTbList.get(i).getCar_type_final())  //20230922 CAR_03_N_05,06,99 chbaek 미등록차량 실적 - 장기용차에서 장기용차 기한외 실적 - 장기용차로 이동
								){
									row.createCell(2).setCellValue("장기용차 기한외 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
									row.createCell(4).setCellValue("장기용차");
									row.createCell(5).setCellValue("-");
								}else if("CAR_01_N".equals(dirTbList.get(i).getCar_type_final())
										|| "CAR_02_N".equals(dirTbList.get(i).getCar_type_final())
								){
									row.createCell(2).setCellValue("차량등록 기한외 실적");
									row.createCell(3).setCellValue(dirTbList.get(i).getCars_reg_num());
	//								row.createCell(4).setCellValue("CAR_01_N".equals(dirTbList.get(i).getCar_type_final()) ? "직영" : "지입");
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

								row.setHeight((short)650);

								thirdstartrow++;
							}
						}else if("TRUST".equals(dirTbList.get(i).getReg_gubun())){ // 1단계 위탁
							Row row = null;
							if(thirdSheet.getRow(thirdstartrow) == null) {
								row = thirdSheet.createRow(thirdstartrow);
							}else {
								row = thirdSheet.getRow(thirdstartrow);
							}

							row.createCell(0).setCellValue("위탁 실적");
							row.createCell(1).setCellValue(dirTbList.get(i).getYyyymm());
							if("RT_STEP1_MANG_N".equals(dirTbList.get(i).getCar_type_final())){
								row.createCell(2).setCellValue("인증망을 이용하지 않은 실적");
							}else if("RT_STEP1_MANG_N_OUT".equals(dirTbList.get(i).getCar_type_final())||"RT_STEP2_MANG_N_OUT".equals(dirTbList.get(i).getCar_type_final())){
								row.createCell(2).setCellValue("인증망 사업기간 외 이용 실적");
							}else if("RT_TB_ONE_N".equals(dirTbList.get(i).getCar_type_final())){
								row.createCell(2).setCellValue("인증망을 이용하지 않은 실적(택배)");
							}else{
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

							row.setHeight((short)650);

							thirdstartrow++;
						} //위탁배차 구분 끝

	//					thirdstartrow++;
					} //택배 반복문 끝~~~


					if(thirdstartrow == 1){
						Row row = null;
						if(thirdSheet.getRow(thirdstartrow) == null) {
							row = thirdSheet.createRow(thirdstartrow);
						}else {
							row = thirdSheet.getRow(thirdstartrow);
						}
						row.createCell(0).setCellValue("1단계 정보가 없습니다.");

						row.getCell(0).setCellStyle(cellformat_solid);

						row.setHeight((short)650);
					}

					if(fourthstartrow == 1){
						Row row = null;
						if(fourthSheet.getRow(fourthstartrow) == null) {
							row = fourthSheet.createRow(fourthstartrow);
						}else {
							row = fourthSheet.getRow(fourthstartrow);
						}
						row.createCell(0).setCellValue("2단계 정보가 없습니다.");

						row.getCell(0).setCellStyle(cellformat_solid);

						row.setHeight((short)650);

					}


				}//직접 상세내역 끝ㅋ




				//3-5. 직접운송 위탁금지위반 상세정보 시트--------------------------------------------------------------------------------------------------
				Sheet Sheet_5 = workbook.getSheetAt(5);  // 직접운송 위탁금지위반 상세정보 시트 가져오기

				int sh5_startrow = 3; //시작 row 셋팅
				int index_uu_seq = 1;	// 등록단위 셀 병합용
				int index_reg_id = 1;	// 계약단위 셀 병합용
				//minList => 직접운송 위탁금지위반 상세정보 쿼리 List로 대체

				if(dirVioList == null || dirVioList.size() == 0){ // DB에 데이터가 없을 때
					Row row = null;
					if(Sheet_5.getRow(sh5_startrow) == null) {
						row = Sheet_5.createRow(sh5_startrow);
					}else {
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
					row.setHeight((short)450);
				}else{
					// 데이터 존재
					for(int i = 0 ; i < dirVioList.size(); i++) {

						//row객체 가져오기
						Row row = null;
						if(Sheet_5.getRow(sh5_startrow) == null) {
							row = Sheet_5.createRow(sh5_startrow);
						}else {
							row = Sheet_5.getRow(sh5_startrow);
						}

						row.createCell(0).setCellValue((dirVioList.get(i).getPg_id().equals("web"))?"웹":"연계" ); //방식
						row.createCell(1).setCellValue(dirVioList.get(i).getUnit_reg_date()); //등록일
						row.createCell(2).setCellValue(dirVioList.get(i).getAgency_yn().equals("N")?"미대행":"대행" ); //대행여부
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
						row.setHeight((short)450);

						sh5_startrow++;


						if(index_uu_seq == dirVioList.get(i).getUu_seq_cnt()) {
						    if(index_uu_seq != 1){ // 셀병합작업
						        int index_base_data = (i+4-dirVioList.get(i).getUu_seq_cnt())+1;// 병합대상 기준 데이터 위치
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),0,0)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),1,1)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),2,2)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),3,3)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),4,4)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),5,5)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),6,6)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),7,7)); // 업체정보 셀병합
						    }
						    index_uu_seq = 1;
						}else{
							index_uu_seq++;
						}
						if(index_reg_id == dirVioList.get(i).getReg_id_cnt()) {
						    if(index_reg_id != 1){ // 셀병합작업
						        int index_base_data = (i+4-dirVioList.get(i).getReg_id_cnt())+1;// 병합대상 기준 데이터 위치
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),8,8)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),9,9)); // 업체정보 셀병합
						        Sheet_5.addMergedRegion(new CellRangeAddress(index_base_data,(i+4),10,10)); // 업체정보 셀병합
						    }
						    index_reg_id = 1;
						}else{
							index_reg_id++;
						}
					}
				}




				out = new FileOutputStream(new File(createFilePath));
				workbook.write(out);
				JSONObject json = new JSONObject();

				json.put("fileCls", "99");
				json.put("fileName", excelFileName);

				pout = response.getWriter();

		        pout.write(json.toString());


			}catch(FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			}catch(IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			}finally{
				try {
					if(inputStream != null) inputStream.close();
					if(out != null) out.close();
					if(pout != null) pout.close();
				}catch(IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			}

        }
    }

	private synchronized void moveToUploadDirectory(String excelFileName, String excelFileSize){
		// package안에 들어간 템플릿 삭제 및 경로 수정, Globals.properties, majarStatFilePath - 2021.12.06 suhyun
		String fileName = "majar_stat_live_template"+excelFileSize+".xls";
		majarStatFilePath += File.separator;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel fcin = null;
		FileChannel fcout = null;

		/*2021.11.01 ysw majarStatFilePath를 그대로 쓰면 안됨!!! 계속 업데이트가 되어버림!!! majarStatFilePath*/

		File fileInSwap = new File(majarStatFilePath, fileName);

		try {
			fis = new FileInputStream(fileInSwap);
			fos = new FileOutputStream(majarStatFilePath+excelFileName);

			fcin = fis.getChannel();
			fcout = fos.getChannel();

			long size = fcin.size();
			fcin.transferTo(0, size, fcout);

			fcout.close();
			fcin.close();
		} catch (FileNotFoundException e) {
			logger.error("[ERROR] - FileNotFoundException : ", e);
		} catch (IOException e) {
			logger.error("[ERROR] - IOException : ", e);
		} finally {
			if(fis != null) try { fis.close(); } catch(IOException e) {logger.error("[ERROR] - IOException : ", e);}
			if(fos != null) try { fos.close(); } catch(IOException e) {logger.error("[ERROR] - IOException : ", e);}
			//if(fileInSwap.exists()) fileInSwap.delete();
		}
	}

	private String convertCarsKind(String carskind){
		if("01".equals(carskind)){ return "일반형"; }
        else if("02".equals(carskind)){ return "덤프형"; }
        else if("03".equals(carskind)){ return "밴형"; }
        else if("04".equals(carskind)){ return "(특수용도형) 청소차"; }
        else if("05".equals(carskind)){ return "(특수용도형) 살수차"; }
        else if("06".equals(carskind)){ return "(특수용도형) 냉장,냉동차"; }
        else if("07".equals(carskind)){ return "(특수용도형) 곡물,사료운반"; }
        else if("08".equals(carskind)){ return "(특수용도형) 유조차"; }
        else if("09".equals(carskind)){ return "(특수용도형) 탱크로리"; }
        else if("10".equals(carskind)){ return "(특수용도형) 기타 - 그 외"; }
        else if("11".equals(carskind)){ return "(특수자동차) 구난형"; }
        else if("12".equals(carskind)){ return "(특수자동차) 견인형"; }
        else if("13".equals(carskind)){ return "(특수자동차) 특수작업형"; }
        else if("14".equals(carskind)){ return "(특수용도형) 노면청소자"; }
        else if("15".equals(carskind)){ return "(특수용도형) 소방차"; }
        else if("16".equals(carskind)){ return "(특수용도형) 피견인차"; }
        else if("17".equals(carskind)){ return "(특수용도형) 기타 - 사다리"; }
        else if("18".equals(carskind)){ return "(특수용도형) 가타 - 크레인"; }
        else if("19".equals(carskind)){ return "(특수용도형) 기타 - 고소작업대"; }
        else return "확인불가";
	}


	/**
     * 170922 실적 누락의심 결과조회 및 행정처분 결과등록
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans9.do")
    public String FpisAdminStatTrans8(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        int totCnt   = 0;

        List<FpisAdminStatTrans7VO> omissionList = null;

        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sido_cd(searchSidoCd);
            }
            //180209 오승민 연 1회신고로 변경 후 2017년 이후 연도검색 시 분기 조건 추가
            if("".equals(shVO.getSearch_bungi())){
            	shVO.setSearch_bungi("0");
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }
            totCnt = FpisSvc.selectOmissionCount(shVO);
            if(totCnt >= 0){
		        shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		        shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		        shVO.setTot_page(Util.calcurateTPage(totCnt));
		        // PAGING END ------------------
            }
	        omissionList = FpisSvc.selectOmissionList(shVO);
            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
            shVO.setSearch_comp_corp_num(org_comp_corp_num);    // 2015.08.07 법인번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }
		int strYear = 2016;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
     // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
     	String DaeguGubun = "";
     	int getYear = 0;
     	if (shVO.getSearch_year() == null) {
     		getYear = endYear;
     	} else {
     		getYear = Integer.parseInt(shVO.getSearch_year());
     	}

     // 2014.12.01 mgkim 시도 관리자 기능추가로 변경작업
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
            	DaeguGubun = svo.getAdm_area_code(); // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
            	searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
                searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            /* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
			if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
				//sigunList = mberManageService.selectSigungu2016(sigunguVO);
			} else {
				sigunList = mberManageService.selectSigungu2016(vo);
			}
        }
        model.addAttribute("SIGUNLIST", sigunList);

		if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
			shVO.setSearch_status("A");
		}

		//2020.11.16 pch 사업자정보 관리이력
		String masked_info_status = req.getParameter("masked_info_status");
		if(masked_info_status == null) masked_info_status = "N";

		model.addAttribute("masked_info_status" , masked_info_status);

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("omissionList" , omissionList);

        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("rcode", "R13");
        model.addAttribute("bcode", "R13-07");

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans9";
    }


    /**
     * 2017.09.27 mgkim 누락의심 결과 엑셀다운로드 초기작업
     *
     * */
    @RequestMapping(value="/admin/obeySystem/trans/FpisAdminStatTrans9_excel.do")
    public void FpisAdminStatTrans9_excel(HttpServletRequest req, HttpServletResponse res, Model model, FpisAdminStatTrans7VO shVO) throws SQLException,UnknownHostException{
    	SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

    	String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<FpisAdminStatTrans7VO> omissionList = null;
        List<FpisAdminStatTrans7VO> omissionTotList = null;
        List<FpisAdminStatBase12VO> voList = null;
        List<FpisAdminStatBase12VO> voOmissionList = null;
        String sTableName = "";
        FpisAdminStatBase12VO VO = new FpisAdminStatBase12VO();
        int totCnt = 0;
        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
                VO.setSearch_sigungu_cd(searchSigunguCd);
                VO.setSearch_sido_cd(searchSidoCd);
            }else{
                shVO.setSearch_sido_cd(searchSidoCd);
                VO.setSearch_sido_cd(searchSidoCd);
            }
            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }

          //180209 오승민 연 1회신고로 변경 후 2017년 이후 연도검색 시 분기 조건 추가
            if("".equals(shVO.getSearch_bungi())){
            	shVO.setSearch_bungi("0");
            }
        	VO.setSearch_year(shVO.getSearch_year());
			VO.setSearch_bungi(shVO.getSearch_bungi());
			VO.setSearch_type("R");
			VO.setSearch_sort1("OMISSION");
			VO.setSearch_sort2("ASC");

            //신고자조회
            sTableName = adminStatSvc.getSearchTableName(VO);
            VO.setSearch_table_name(sTableName);
			totCnt = adminStatSvc.selectRegUsrDayTotal(VO);
			VO.setS_row(0);
			if(totCnt >= 0 && totCnt < 2147483647){
				VO.setE_row(Util.getPagingEnd(totCnt+1));
			}
			voList = adminStatSvc.selectRegUsrDay(VO);

            //총괄 조회
            shVO.setSearch_table_name(sTableName);
            omissionTotList = FpisSvc.selectOmissionTotList(shVO);
			//누락의심 조회
            omissionList = FpisSvc.selectOmissionList_excel(shVO);


            //180430 smoh 신고금액검증 추가
            if(Integer.valueOf(shVO.getSearch_year()) == 2017 && Integer.valueOf(shVO.getSearch_bungi()) == 30){
            	voOmissionList = adminStatSvc.selectRegUsrDay_excel_omission(VO);
            }

            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
        }

        String masked_info_status = req.getParameter("masked_info_status");
        if("Y".equals(masked_info_status)){
        	/*사업자정보 이력 관리*/
        	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
        	/*이력 삽입_신고사업자*/
        	for(int i=0;i<voList.size();i++){
        		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
        		accessLogVO.setRcode(req.getParameter("rcode"));
            	accessLogVO.setBcode(req.getParameter("bcode"));
            	accessLogVO.setComp_mst_key(voList.get(i).getUsr_mst_key().replaceAll("-", ""));
            	accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            	accessLogVO.setJob_cls("EX"); //엑셀다운로드
            	accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            	accessLogVOList.add(accessLogVO);
        	}
        	/*이력 삽입_누락의심자*/
        	for(int i=0;i<omissionList.size();i++){
        		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
        		accessLogVO.setRcode(req.getParameter("rcode"));
            	accessLogVO.setBcode(req.getParameter("bcode"));
            	accessLogVO.setComp_mst_key(omissionList.get(i).getUsr_mst_key().replaceAll("-", ""));
            	accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            	accessLogVO.setJob_cls("EX"); //엑셀다운로드
            	accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            	accessLogVOList.add(accessLogVO);
        	}
        	/*이력 삽입_수탁자*/
        	for(int i=0;i<omissionList.size();i++){
        		if(omissionList.get(i).getSutak_num() != null){
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
        }else{
        	masked_info_status = "N";
        }

        parseExcel_FpisAdminStatTrans9(req,res,shVO,omissionTotList,voList,omissionList, voOmissionList, masked_info_status);
    }

    /**
     * *****************************************************************************
     * 작  성  일   : 2017. 09. 27.
     * 작  성  자   : mgkim
     * 메  소  드   : parseExcel_FpisAdminStatTrans9
     * 파 라 메 터:
     * 리         턴 : void (엑셀 파일 다운)
     * 설         명 : 누락의심 결과 엑셀다운로드  처리 로직
     * 변경  이력 : 2017. 09. 27. - 최초 생성
     *
     * 2017.09.27 mgkim 미신고 의심내역 시트생성 작업
     * 2017.09.28 mgkim 미신고 의심내역 셀 병합작업 및 시트 스타일 작업
     * 2017.10.11 mgkim 시군구 총괄 시트생성 작업
     *
     * 2018.04.26 smoh	2017년 이후 양식 변경으로 인한 수정 및 신고금액검증 시트 추가
     *
     ******************************************************************************
     */
    public void parseExcel_FpisAdminStatTrans9(HttpServletRequest req, HttpServletResponse res,
                                               FpisAdminStatTrans7VO VO,
                                               List<FpisAdminStatTrans7VO> resultList_sheet2, // 시군구 총괄
                                               List<FpisAdminStatBase12VO> resultList_sheet3, // 신고현황
                                               List<FpisAdminStatTrans7VO> resultList_sheet4,  // 미신고 의심내역
                                               List<FpisAdminStatBase12VO> resultList_sheet5,  // 신고금액 검증
                                               String masked_info_status						//정보노출여부
                                               ) {
        SessionVO SessionVO = (SessionVO)req.getSession().getAttribute("SessionVO");
        //String file_path=EgovProperties.getProperty("globals.fileStorePath");
        File folder = new File(fileStorePath);//지정된 경로에 폴더를 만든다.
        folder.setExecutable(false);
        folder.setReadable(true);
        folder.setWritable(true);
        if(!folder.exists()){
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
        if("1".equals(search_bungi)){
            month1 = "1월"; month2 = "2월"; month3 = "3월";
        }else if("2".equals(search_bungi)){
            month1 = "4월"; month2 = "5월"; month3 = "6월";
        }else if("3".equals(search_bungi)){
            month1 = "7월"; month2 = "8월"; month3 = "9월";
        }else if("4".equals(search_bungi)){
            month1 = "10월"; month2 = "11월"; month3 = "12월";
        }

        /* 스타일 작업 */
        // 시트별 제목 스타일
        CellStyle cellStyle_title = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_title.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");                  // 폰트 이름
        font.setFontHeightInPoints((short)18);          // 폰트 크기
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);  //글씨 bold
        cellStyle_title.setFont(font);


        // 시트별 노말 스타일
        CellStyle cellStyle_normal = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_normal.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        Font font2 = workbook.createFont();
        font2.setFontName("맑은 고딕");                  // 폰트 이름
        cellStyle_normal.setFont(font2);

        // 표 셀 스타일 연녹색
        CellStyle cellStyle_td1 = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_td1.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_td1.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_td1.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_td1.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_td1.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_td1.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_td1.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle_td1.setWrapText(true);
        XSSFColor color1 = new XSSFColor(new java.awt.Color(215,228,188)); // 2017.09.28 mgkim RGB적용
        ((XSSFCellStyle) cellStyle_td1).setFillForegroundColor(color1);

        // 표 셀 스타일 연블루
        CellStyle cellStyle_td2 = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_td2.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_td2.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_td2.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_td2.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_td2.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle_td2.setWrapText(true);
        XSSFColor color2 = new XSSFColor(new java.awt.Color(217,229,255)); // 2017.09.28 mgkim RGB적용
        ((XSSFCellStyle) cellStyle_td2).setFillForegroundColor(color2);

        // 표 셀 스타일 연분홍
        CellStyle cellStyle_td3 = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_td3.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_td3.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_td3.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_td3.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_td3.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_td3.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_td3.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle_td3.setWrapText(true);
        XSSFColor color3 = new XSSFColor(new java.awt.Color(250,224,212)); // 2017.09.28 mgkim RGB적용
        ((XSSFCellStyle) cellStyle_td3).setFillForegroundColor(color3);

        // 표 셀 스타일 연보라
        CellStyle cellStyle_td4 = workbook.createCellStyle(); //스타일 생성 - 헤더1
        cellStyle_td4.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_td4.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_td4.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_td4.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_td4.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_td4.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_td4.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle_td4.setWrapText(true);
        XSSFColor color4 = new XSSFColor(new java.awt.Color(173,161,247)); // 2018.09.14 pes RGB적용
        ((XSSFCellStyle) cellStyle_td4).setFillForegroundColor(color4);


        CellStyle cellStyle = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle.setAlignment(CellStyle.ALIGN_LEFT);  //스타일 - 가운데정렬
        cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle.setWrapText(true);

        CellStyle cellStyle_center = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_center.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_center.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_center.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_center.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_center.setWrapText(true);

        CellStyle cellStyle_right = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_right.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
        cellStyle_right.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_right.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_right.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_right.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_right.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_right.setWrapText(true);

        CellStyle cellStyle_number = workbook.createCellStyle(); // 스타일 생성 - 숫자
        XSSFDataFormat format = workbook.createDataFormat();
        cellStyle_number.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
        cellStyle_number.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_number.setDataFormat(format.getFormat("_-* #,##0_-;-* #,##0_-;_-* \"-\"_-;_-@_-"));
        cellStyle_number.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_number.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_number.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_number.setBorderBottom(CellStyle.BORDER_THIN);

        Font font_b = workbook.createFont();
        font_b.setBoldweight(Font.BOLDWEIGHT_BOLD);  //글씨 bold

        CellStyle cellStyle_center_b = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_center_b.setAlignment(CellStyle.ALIGN_CENTER);  //스타일 - 가운데정렬
        cellStyle_center_b.setVerticalAlignment(CellStyle.VERTICAL_CENTER);//세로 가운데 정렬
        cellStyle_center_b.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle_center_b.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle_center_b.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle_center_b.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle_center_b.setWrapText(true);
        cellStyle_center_b.setFont(font_b);

        CellStyle cellStyle_right_b = workbook.createCellStyle(); // 스타일 생성 - 일반셀
        cellStyle_right_b.setAlignment(CellStyle.ALIGN_RIGHT);  //스타일 - 가운데정렬
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
        if(Integer.parseInt(search_year) < 2017){
        	Util_poi.setCell(cell1, row1, 0, cellStyle_normal, "5. 파일링");
        	row1 = worksheet1.createRow(19);
        	Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-1. 시군구별 자료는 17개 시도 파일로 구성");
        	row1 = worksheet1.createRow(20);
        	Util_poi.setCell(cell1, row1, 0, cellStyle_normal, " 5-2. 시도별 파일안에 시군구별 시트로 신고현황 및 미신고의심세부내역을 각각의 시트로 작성");
        }else if(Integer.parseInt(search_year) == 2017 && Integer.parseInt(search_bungi) == 30){
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
        }else{
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
        XSSFSheet worksheet2 = workbook.createSheet("시군구 총괄");

        Row row2 = null; //로우
        Cell cell2 = null;// 셀


        worksheet2.setColumnWidth(0, (short)3200);
        worksheet2.setColumnWidth(1, (short)3000);
        worksheet2.setColumnWidth(2, (short)3600);
        worksheet2.setColumnWidth(3, (short)3400);
        worksheet2.setColumnWidth(4, (short)3400);
        worksheet2.setColumnWidth(5, (short)5000);
        worksheet2.setColumnWidth(6, (short)5000);
        worksheet2.setColumnWidth(7, (short)3400);
        worksheet2.setColumnWidth(8, (short)4600);
        worksheet2.setColumnWidth(9, (short)4600);


        //헤더작업
        row2 = worksheet2.createRow(0); //첫 줄 생성

        if(Integer.valueOf(search_year) < 2017){
        	Util_poi.setCell(cell2, row2, 0, cellStyle_title, "< "+search_year+"년 "+search_bungi+"분기 화물운송실적 신고현황 시군구 총괄표 >");
        }else{
        	if("30".equals(search_bungi)) Util_poi.setCell(cell2, row2, 0, cellStyle_title, "< "+search_year+"년 화물운송실적 신고현황 시군구 총괄표 >");
        	else if("60".equals(search_bungi)) Util_poi.setCell(cell2, row2, 0, cellStyle_title, "< "+search_year+"년 화물운송실적 신고현황 시군구 총괄표 >");
        }
        row2 = worksheet2.createRow(1);
        row2 = worksheet2.createRow(2);
        Util_poi.setCell(cell2, row2, 0, cellStyle_normal, "주1) 신고자 중 누락의심자 : 운송실적 중 일부만 실적신고한 경우(예시: A사업자가 10개의 각기 다른 사업자와 운송거래 실적이 있는데 이중 일부 사업자의 운송실적만 등록한 경우)");
        row2 = worksheet2.createRow(3);
        Util_poi.setCell(cell2, row2, 0, cellStyle_normal, "주2) 미신고자 중 실적있는 의심자 : 해당 운수사업자로부터 운송계약을 맺은 수탁자가 실적신고 한 경우(예시: B가 A로 부터 의뢰받았다고 실적신고하였으나 A는 신고하지 않음)");
        row2 = worksheet2.createRow(4);

        row2 = worksheet2.createRow(5);
        Util_poi.setCell(cell2, row2, 0, cellStyle_center, "시도");
        Util_poi.setCell(cell2, row2, 1, cellStyle_center, "시군구");
        Util_poi.setCell(cell2, row2, 2, cellStyle_center, "신고자 합계\n(A+B+C+D+E)");
        Util_poi.setCell(cell2, row2, 3, cellStyle_td1, "운송(A)");
        Util_poi.setCell(cell2, row2, 4, cellStyle_td1, "주선(B)");
        Util_poi.setCell(cell2, row2, 5, cellStyle_td1, "겸업");
        Util_poi.setCell(cell2, row2, 6, cellStyle_td1, "");
        Util_poi.setCell(cell2, row2, 7, cellStyle_td1, "가맹\n(E)");
        Util_poi.setCell(cell2, row2, 8, cellStyle_td3, "신고자 중\n누락의심자");
        Util_poi.setCell(cell2, row2, 9, cellStyle_td2, "미신고자 중\n실적있는 의심자");
        row2 = worksheet2.createRow(6);
        Util_poi.setCell(cell2, row2, 0, cellStyle, "");
        Util_poi.setCell(cell2, row2, 1, cellStyle, "");
        Util_poi.setCell(cell2, row2, 2, cellStyle, "");
        Util_poi.setCell(cell2, row2, 3, cellStyle_td1, "");
        Util_poi.setCell(cell2, row2, 4, cellStyle_td1, "");
        Util_poi.setCell(cell2, row2, 5, cellStyle_td1, "1대운송+주선(C)");
        Util_poi.setCell(cell2, row2, 6, cellStyle_td1, "2대이상운송+주선(D)");
        Util_poi.setCell(cell2, row2, 7, cellStyle_td1, "");
        Util_poi.setCell(cell2, row2, 8, cellStyle_td3, "");
        Util_poi.setCell(cell2, row2, 9, cellStyle_td2, "");

        worksheet2.addMergedRegion(new CellRangeAddress(5,6,0,0)); //셀병합 -  행 : 0   을   1까지    열 : 0   을  0까지
        worksheet2.addMergedRegion(new CellRangeAddress(5,6,1,1)); //셀병합
        worksheet2.addMergedRegion(new CellRangeAddress(5,6,2,2)); //셀병합
        worksheet2.addMergedRegion(new CellRangeAddress(5,6,3,3)); //셀병합
        worksheet2.addMergedRegion(new CellRangeAddress(5,6,4,4)); //셀병합
        worksheet2.addMergedRegion(new CellRangeAddress(5,5,5,6)); //셀병합
        worksheet2.addMergedRegion(new CellRangeAddress(5,6,7,7)); //셀병합
        worksheet2.addMergedRegion(new CellRangeAddress(5,6,8,8)); //셀병합
        worksheet2.addMergedRegion(new CellRangeAddress(5,6,9,9)); //셀병합


        int index_sigungu_s2 = 1;   // 시도명 셀 병합용
        for(int i=0; i<resultList_sheet2.size(); i++){
            row2 = worksheet2.createRow(i+7);

            if(VO.getSigunguCd().equals("") || (!VO.getSigunguCd().equals("") && !resultList_sheet2.get(i).getSigungu_nm().substring(0, 1).equals("훅"))){
	            Util_poi.setCell(cell2, row2, 0, cellStyle_center, resultList_sheet2.get(i).getSido_nm());
	            Util_poi.setCell(cell2, row2, 1, cellStyle_center, resultList_sheet2.get(i).getSigungu_nm());
//	            Util_poi.setCell(cell2, row2, 2, cellStyle_right, Util.Comma_wonf(resultList_sheet2.get(i).getTotal()));
	            Util_poi.setNumberCell3(cell2, row2, 2, cellStyle_number, resultList_sheet2.get(i).getTotal());
	            Util_poi.setNumberCell3(cell2, row2, 3, cellStyle_number, resultList_sheet2.get(i).getU_cls());
	            Util_poi.setNumberCell3(cell2, row2, 4, cellStyle_number, resultList_sheet2.get(i).getJ_cls());
	            Util_poi.setNumberCell3(cell2, row2, 5, cellStyle_number, resultList_sheet2.get(i).getC_cls());
	            Util_poi.setNumberCell3(cell2, row2, 6, cellStyle_number, resultList_sheet2.get(i).getD_cls());
	            Util_poi.setNumberCell3(cell2, row2, 7, cellStyle_number, resultList_sheet2.get(i).getG_cls());
	            Util_poi.setNumberCell3(cell2, row2, 8, cellStyle_number, resultList_sheet2.get(i).getIs_reg_o());
	            Util_poi.setNumberCell3(cell2, row2, 9, cellStyle_number, resultList_sheet2.get(i).getIs_reg_x());
            }
            /* ============== 시도명 셀병합작업 시작 */
            if(VO.getSigunguCd().equals("") && index_sigungu_s2 == Integer.parseInt(resultList_sheet2.get(i).getSigungu_group())) {
                if(index_sigungu_s2 != 1){ // 셀병합작업
                    int index_base_data = (i+7-Integer.parseInt(resultList_sheet2.get(i).getSigungu_group()))+1;// 병합대상 기준 데이터 위치
                    worksheet2.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),0,0)); // 시도 셀병합
                }
                index_sigungu_s2 = 1;
                // 합계 처리
                Util_poi.setCell(cell2, row2, 1, cellStyle_center_b, "합계");
                Util_poi.setNumberCell3(cell2, row2, 2, cellStyle_right_b, resultList_sheet2.get(i).getTotal());
                Util_poi.setNumberCell3(cell2, row2, 3, cellStyle_right_b, resultList_sheet2.get(i).getU_cls());
                Util_poi.setNumberCell3(cell2, row2, 4, cellStyle_right_b, resultList_sheet2.get(i).getJ_cls());
                Util_poi.setNumberCell3(cell2, row2, 5, cellStyle_right_b, resultList_sheet2.get(i).getC_cls());
                Util_poi.setNumberCell3(cell2, row2, 6, cellStyle_right_b, resultList_sheet2.get(i).getD_cls());
                Util_poi.setNumberCell3(cell2, row2, 7, cellStyle_right_b, resultList_sheet2.get(i).getG_cls());
                Util_poi.setNumberCell3(cell2, row2, 8, cellStyle_right_b, resultList_sheet2.get(i).getIs_reg_o());
                Util_poi.setNumberCell3(cell2, row2, 9, cellStyle_right_b, resultList_sheet2.get(i).getIs_reg_x());

            }else{
                index_sigungu_s2++;
            }
            /* ============== 시도명 셀병합작업 끝 */
        }


        /* =======================================================================  2번째 시트 끝 */


        /* =======================================================================  3번째 시트 시작 */
        XSSFSheet worksheet3 = workbook.createSheet("신고 사업자 내역");//2018.02.22 pes 신고현황 --> 신고 사업자 내역으로 수정

        Row row3 = null; //로우
        Cell cell3 = null;// 셀

        worksheet3.setColumnWidth(0, (short)3400);
        worksheet3.setColumnWidth(1, (short)4600);
        worksheet3.setColumnWidth(2, (short)6000);
        worksheet3.setColumnWidth(3, (short)3600);
        worksheet3.setColumnWidth(4, (short)4000);
        worksheet3.setColumnWidth(5, (short)5000);
        worksheet3.setColumnWidth(6, (short)5000);

        //헤더작업
        row3 = worksheet3.createRow(0); //첫 줄 생성
        if(Integer.valueOf(search_year) < 2017){
        	Util_poi.setCell(cell3, row3, 0, cellStyle_title, "< "+search_year+"년 "+search_bungi+"분기 화물운송실적 사업자별 신고현황 >");
        }else{
        	if("30".equals(search_bungi)) Util_poi.setCell(cell3, row3, 0, cellStyle_title, "< "+search_year+"년 화물운송실적 사업자별 신고현황 >");
        	else if("60".equals(search_bungi)) Util_poi.setCell(cell3, row3, 0, cellStyle_title, "< "+search_year+"년 화물운송실적 사업자별 신고현황 >");
        }
        row3 = worksheet3.createRow(1);
        row3 = worksheet3.createRow(2);
        row3 = worksheet3.createRow(3);

        Util_poi.setCell(cell3, row3, 0, cellStyle_td1, "신고자");
        Util_poi.setCell(cell3, row3, 1, cellStyle_td1, "");
        Util_poi.setCell(cell3, row3, 2, cellStyle_td1, "");
        Util_poi.setCell(cell3, row3, 3, cellStyle_td1, "");
        Util_poi.setCell(cell3, row3, 4, cellStyle_td1, "");
        Util_poi.setCell(cell3, row3, 5, cellStyle_td1, "");
        Util_poi.setCell(cell3, row3, 6, cellStyle_td1, "");

        row3 = worksheet3.createRow(4);
        Util_poi.setCell(cell3, row3, 0, cellStyle_td1, "시군구");
        Util_poi.setCell(cell3, row3, 1, cellStyle_td1, "사업자명");
        Util_poi.setCell(cell3, row3, 2, cellStyle_td1, "주소");
        Util_poi.setCell(cell3, row3, 3, cellStyle_td1, "사업자번호\n(세금관련)");
        Util_poi.setCell(cell3, row3, 4, cellStyle_td1, "법인번호");
        Util_poi.setCell(cell3, row3, 5, cellStyle_td1, "업태");
        Util_poi.setCell(cell3, row3, 6, cellStyle_td1, "보유차량수");

        worksheet3.addMergedRegion(new CellRangeAddress(3,3,0,6)); //셀병합 -  행 : 0   을   1까지    열 : 0   을  0까지


        int index_sigungu_s3 = 1;   // 시도명 셀 병합용
        for(int i=0; i<resultList_sheet3.size(); i++){
            row3 = worksheet3.createRow(i+5);
            Util_poi.setCell(cell3, row3, 0, cellStyle, "["+resultList_sheet3.get(i).getSido_nm()+"] "+resultList_sheet3.get(i).getSigungu_nm());
            Util_poi.setCell(cell3, row3, 1, cellStyle, resultList_sheet3.get(i).getComp_nm());
            if("Y".equals(masked_info_status)){ Util_poi.setCell(cell3, row3, 2, cellStyle, resultList_sheet3.get(i).getAddr1());}
            else{ Util_poi.setCell(cell3, row3, 2, cellStyle, resultList_sheet3.get(i).getMasked_addr1());}
            Util_poi.setCell(cell3, row3, 3, cellStyle, resultList_sheet3.get(i).getComp_bsns_num());
            Util_poi.setCell(cell3, row3, 4, cellStyle, resultList_sheet3.get(i).getComp_corp_num());
            if(Integer.valueOf(search_year) < 2017){
            	Util_poi.setCell(cell3, row3, 5, cellStyle, resultList_sheet3.get(i).getComp_cls_detail_nm());
            }else{
            	Util_poi.setCell(cell3, row3, 5, cellStyle, resultList_sheet3.get(i).getComp_cls_detail());
            }
            Util_poi.setNumberCell3(cell3, row3, 6, cellStyle_number, resultList_sheet3.get(i).getCar_cls_tot());


            /* ============== 시도명 셀병합작업 시작 */
            if(index_sigungu_s3 == Integer.parseInt(resultList_sheet3.get(i).getSigungu_group())) {
                if(index_sigungu_s3 != 1){ // 셀병합작업
                    int index_base_data = (i+5-Integer.parseInt(resultList_sheet3.get(i).getSigungu_group()))+1;// 병합대상 기준 데이터 위치
                    worksheet3.addMergedRegion(new CellRangeAddress(index_base_data,(i+5),0,0)); // 시도 셀병합
                }
                index_sigungu_s3 = 1;
            }else{
                index_sigungu_s3++;
            }
            /* ============== 시도명 셀병합작업 끝 */
        }


        /* =======================================================================  3번째 시트 끝 */


        /* =======================================================================  4번째 시트 시작 */
        XSSFSheet worksheet4 = workbook.createSheet("미신고의심내역");

        Row row4 = null; //로우
        Cell cell4 = null;// 셀

        worksheet4.setColumnWidth(0, (short)3600);
        worksheet4.setColumnWidth(1, (short)9000);
        worksheet4.setColumnWidth(2, (short)18000);
        worksheet4.setColumnWidth(3, (short)3400);
        worksheet4.setColumnWidth(4, (short)3800);
        worksheet4.setColumnWidth(5, (short)5000);
        worksheet4.setColumnWidth(6, (short)3000);


        if(Integer.valueOf(search_year) < 2017){
        	worksheet4.setColumnWidth(7, (short)2600);
            worksheet4.setColumnWidth(8, (short)9000);
            worksheet4.setColumnWidth(9, (short)18000);
        	worksheet4.setColumnWidth(10, (short)8000);
            worksheet4.setColumnWidth(11, (short)8000);
            worksheet4.setColumnWidth(12, (short)8000);
            worksheet4.setColumnWidth(13, (short)8000);
            worksheet4.setColumnWidth(14, (short)6000);

            worksheet4.setColumnWidth(15, (short)6000);
	        worksheet4.setColumnWidth(16, (short)6000);
	        worksheet4.setColumnWidth(17, (short)6000);
	        worksheet4.setColumnWidth(18, (short)16000);
	        worksheet4.setColumnWidth(19, (short)16000);
	        worksheet4.setColumnWidth(20, (short)16000);
        }else if(Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30){
        	worksheet4.setColumnWidth(7, (short)2600);
            worksheet4.setColumnWidth(8, (short)9000);
            worksheet4.setColumnWidth(9, (short)18000);
        	worksheet4.setColumnWidth(10, (short)8000);
            worksheet4.setColumnWidth(11, (short)8000);
            worksheet4.setColumnWidth(12, (short)8000);
            worksheet4.setColumnWidth(13, (short)6000);

            worksheet4.setColumnWidth(14, (short)6000);
            worksheet4.setColumnWidth(15, (short)6000);
	        worksheet4.setColumnWidth(16, (short)6000);
	        worksheet4.setColumnWidth(17, (short)6000);
	        worksheet4.setColumnWidth(18, (short)6000);
	        worksheet4.setColumnWidth(19, (short)6000);
	        worksheet4.setColumnWidth(20, (short)6000);
	        worksheet4.setColumnWidth(21, (short)6000);
	        worksheet4.setColumnWidth(22, (short)6000);
	        worksheet4.setColumnWidth(23, (short)6000);
	        worksheet4.setColumnWidth(24, (short)6000);
	        worksheet4.setColumnWidth(25, (short)6000);
        }else{
        	worksheet4.setColumnWidth(7, (short)8000);
            worksheet4.setColumnWidth(8, (short)3000);
            worksheet4.setColumnWidth(9, (short)2600);
            worksheet4.setColumnWidth(10, (short)9000);
            worksheet4.setColumnWidth(11, (short)18000);
        	worksheet4.setColumnWidth(12, (short)8000);
            worksheet4.setColumnWidth(13, (short)8000);
            worksheet4.setColumnWidth(14, (short)8000);
            worksheet4.setColumnWidth(15, (short)6000);

	        worksheet4.setColumnWidth(16, (short)6000);
	        worksheet4.setColumnWidth(17, (short)6000);
	        worksheet4.setColumnWidth(18, (short)6000);
	        worksheet4.setColumnWidth(19, (short)6000);
	        worksheet4.setColumnWidth(20, (short)6000);
	        worksheet4.setColumnWidth(21, (short)6000);
	        worksheet4.setColumnWidth(22, (short)6000);
	        worksheet4.setColumnWidth(23, (short)6000);
	        worksheet4.setColumnWidth(24, (short)6000);
	        worksheet4.setColumnWidth(25, (short)6000);
	        worksheet4.setColumnWidth(26, (short)6000);
	        worksheet4.setColumnWidth(27, (short)6000);
	        worksheet4.setColumnWidth(28, (short)6000);
	        worksheet4.setColumnWidth(29, (short)6000);
        }


        //헤더작업
        row4 = worksheet4.createRow(0); //첫 줄 생성
        Util_poi.setCell(cell4, row4, 0, cellStyle_title, "< FPIS 등록자 기준 미신고 의심 세부내역 >");

        row4 = worksheet4.createRow(1);
        row4 = worksheet4.createRow(2);
        Util_poi.setCell(cell4, row4, 0, cellStyle_normal, "* 1개의 신고대상자에게 의심내역이 여러건 있을 경우 신고대상자 셀병합 하여 작성");
        if((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 60) || (Integer.valueOf(search_year) > 2017)) Util_poi.setCell(cell4, row4, 4, cellStyle_normal, "   * 실적의무 미이행률 = 누락의심 계약금액 / (계약금액 + 누락의심 계약금액)");
        row4 = worksheet4.createRow(3);
        Util_poi.setCell(cell4, row4, 0, cellStyle_normal, "* 신고자(O) 중 의심내역이 있는 경우는 '누락의심자', 미신고자(X) 중 의심내역 있는 경우는 '미신고 중 실적있는 의심자'");
        if((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 60) || (Integer.valueOf(search_year) > 2017)) Util_poi.setCell(cell4, row4, 4, cellStyle_normal, "   * 계약금액 기준 : 실적신고 의무대상이 아닌 신고제외차량의 배차실적을 비율만큼 제외, 직영&항만내이송&이사화물 제외.");
        row4 = worksheet4.createRow(4);
        Util_poi.setCell(cell4, row4, 0, cellStyle_normal, "* 수탁금액은 : 계약금액이 있을 경우 계약금액 기재, 없을경우 배차금액+위탁금액 기재");
        if((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 60) || (Integer.valueOf(search_year) > 2017)) Util_poi.setCell(cell4, row4, 4, cellStyle_normal, "   * 누락의심 계약금액 기준 : 항만내이송, 이사화물 제외.");


        row4 = worksheet4.createRow(5);
        Util_poi.setCell(cell4, row4, 0, cellStyle_td1, "신고대상자");
        Util_poi.setCell(cell4, row4, 1, cellStyle_td1, "");
        Util_poi.setCell(cell4, row4, 2, cellStyle_td1, "");
        Util_poi.setCell(cell4, row4, 3, cellStyle_td1, "");
        Util_poi.setCell(cell4, row4, 4, cellStyle_td1, "");
        Util_poi.setCell(cell4, row4, 5, cellStyle_td1, "");
        Util_poi.setCell(cell4, row4, 6, cellStyle_td1, "");
        if((Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30) || (Integer.valueOf(search_year) < 2017)){
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
        }else{
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
        }

        if(Integer.valueOf(search_year) > 2016){
        	for(int i = 21; i < 26; i++){
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

        if(Integer.valueOf(search_year) < 2017){
            Util_poi.setCell(cell4, row4, 8, cellStyle_td3, "수탁자 상호");
            Util_poi.setCell(cell4, row4, 9, cellStyle_td3, "수탁자 주소");
            Util_poi.setCell(cell4, row4, 10, cellStyle_td3, "수탁자 사업자번호");
	        Util_poi.setCell(cell4, row4, 11, cellStyle_td3, "수탁자 연락처");
	        Util_poi.setCell(cell4, row4, 12, cellStyle_td3, "수탁자 업태");
	        Util_poi.setCell(cell4, row4, 13, cellStyle_td3, "수탁자 보유차량수");
	        Util_poi.setCell(cell4, row4, 14, cellStyle_td3, "수탁금액 합계");

	        Util_poi.setCell(cell4, row4, 15, cellStyle_td3, "수탁금액 "+month1);
	        Util_poi.setCell(cell4, row4, 16, cellStyle_td3, "수탁금액 "+month2);
	        Util_poi.setCell(cell4, row4, 17, cellStyle_td3, "수탁금액 "+month3);
	        Util_poi.setCell(cell4, row4, 18, cellStyle_td3, "배차정보 "+month1);
	        Util_poi.setCell(cell4, row4, 19, cellStyle_td3, "배차정보 "+month2);
	        Util_poi.setCell(cell4, row4, 20, cellStyle_td3, "배차정보 "+month3);
        }else if(Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30){
            Util_poi.setCell(cell4, row4, 8, cellStyle_td3, "수탁자 상호");
            Util_poi.setCell(cell4, row4, 9, cellStyle_td3, "수탁자 주소");
            Util_poi.setCell(cell4, row4, 10, cellStyle_td3, "수탁자 사업자번호");
            Util_poi.setCell(cell4, row4, 11, cellStyle_td3, "수탁자 업태");
            Util_poi.setCell(cell4, row4, 12, cellStyle_td3, "수탁자 보유차량수");
            Util_poi.setCell(cell4, row4, 13, cellStyle_td3, "수탁금액 합계");

        	int j = 14;
        	for(int i = 1; i < 13; i++){
        		Util_poi.setCell(cell4, row4, j, cellStyle_td3, "수탁금액 "+i+"월");
        		j++;
        	}

        }else{
        	Util_poi.setCell(cell4, row4, 8, cellStyle_td1, "");
            Util_poi.setCell(cell4, row4, 9, cellStyle_td1, "");
            Util_poi.setCell(cell4, row4, 10, cellStyle_td3, "수탁자 상호");
            Util_poi.setCell(cell4, row4, 11, cellStyle_td3, "수탁자 주소");
            Util_poi.setCell(cell4, row4, 12, cellStyle_td3, "수탁자 사업자번호");
            Util_poi.setCell(cell4, row4, 13, cellStyle_td3, "수탁자 업태");
            Util_poi.setCell(cell4, row4, 14, cellStyle_td3, "수탁자 보유차량수");
            Util_poi.setCell(cell4, row4, 15, cellStyle_td3, "수탁금액 합계");
            Util_poi.setCell(cell4, row4, 16, cellStyle_td3, "계약년도 "+(Integer.valueOf(search_year)-2));
            Util_poi.setCell(cell4, row4, 17, cellStyle_td3, "계약년도 "+(Integer.valueOf(search_year)-1));

        	int j = 18;
        	for(int i = 1; i < 13; i++){
        		String month = "";
        		if(i < 10) month = "0"+i;
        		else month = String.valueOf(i);
        		Util_poi.setCell(cell4, row4, j, cellStyle_td3, "계약년월 "+(Integer.valueOf(search_year))+month);
        		j++;
        	}
        }//2018.09.14 pes 계약금액, 미이행률, 계약년도 추가

        worksheet4.addMergedRegion(new CellRangeAddress(5,5,0,6)); //셀병합 -  행 : 0   을   1까지    열 : 0   을  0까지
        worksheet4.addMergedRegion(new CellRangeAddress(5,6,7,7)); //셀병합
        if(Integer.valueOf(search_year) < 2017) worksheet4.addMergedRegion(new CellRangeAddress(5,5,8,20)); //셀병합
        else if(Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30) worksheet4.addMergedRegion(new CellRangeAddress(5,5,8,25)); //셀병합
        else{
        	worksheet4.addMergedRegion(new CellRangeAddress(5,6,8,8)); //계약금액
        	worksheet4.addMergedRegion(new CellRangeAddress(5,6,9,9)); //미이행률
        	worksheet4.addMergedRegion(new CellRangeAddress(5,5,10,29)); // 계약년도
        }//2018.09.14 pes 계약금액, 미이행률, 계약년도 추가

        int index_sigungu = 1;   // 시군구명 셀 병합용
        int index_comp_data = 1; // 업체정보 셀 병합용

        for(int i=0; i<resultList_sheet4.size(); i++){
        	row4 = worksheet4.createRow(i+7);

            Util_poi.setCell(cell4, row4, 0, cellStyle, resultList_sheet4.get(i).getSigungu_nm());
            Util_poi.setCell(cell4, row4, 1, cellStyle, resultList_sheet4.get(i).getComp_nm());
            if("Y".equals(masked_info_status)){ Util_poi.setCell(cell4, row4, 2, cellStyle, resultList_sheet4.get(i).getAddr1());}
            else{ Util_poi.setCell(cell4, row4, 2, cellStyle, resultList_sheet4.get(i).getMasked_addr1());}
            Util_poi.setCell(cell4, row4, 3, cellStyle, resultList_sheet4.get(i).getUsr_mst_key());
            Util_poi.setCell(cell4, row4, 4, cellStyle, resultList_sheet4.get(i).getComp_corp_num());
            if(Integer.valueOf(search_year) < 2017){
            	Util_poi.setCell(cell4, row4, 5, cellStyle, resultList_sheet4.get(i).getComp_cls());
            }else{
            	Util_poi.setCell(cell4, row4, 5, cellStyle, resultList_sheet4.get(i).getComp_cls_detail());
            }

            Util_poi.setNumberCell3(cell4, row4, 6, cellStyle_number, resultList_sheet4.get(i).getCar_cnt());
            if(Integer.valueOf(search_year) < 2017){
            	Util_poi.setCell(cell4, row4, 7, cellStyle_center, resultList_sheet4.get(i).getIs_reg());

                Util_poi.setCell(cell4, row4, 8, cellStyle, resultList_sheet4.get(i).getSutak_nm());
                if("Y".equals(masked_info_status)){ Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getSutak_addr1());}
                else{ Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getMasked_sutak_addr1());}
                Util_poi.setCell(cell4, row4, 10, cellStyle, resultList_sheet4.get(i).getSutak_num());
                if("Y".equals(masked_info_status)){ Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getTel());}
                else{ Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getMasked_tel());}
            	Util_poi.setCell(cell4, row4, 12, cellStyle, resultList_sheet4.get(i).getSutak_cls());
	            Util_poi.setNumberCell3(cell4, row4, 13, cellStyle_number, resultList_sheet4.get(i).getSutak_cars_cnt());
	            Util_poi.setNumberCell3(cell4, row4, 14, cellStyle_number, resultList_sheet4.get(i).getSutak_sum_charge());

	        	Util_poi.setNumberCell3(cell4, row4, 15, cellStyle_number, resultList_sheet4.get(i).getSutak_charge1());
		        Util_poi.setNumberCell3(cell4, row4, 16, cellStyle_number, resultList_sheet4.get(i).getSutak_charge2());
		        Util_poi.setNumberCell3(cell4, row4, 17, cellStyle_number, resultList_sheet4.get(i).getSutak_charge3());
	        	Util_poi.setCell(cell4, row4, 18, cellStyle, resultList_sheet4.get(i).getSutak_car1());
	            Util_poi.setCell(cell4, row4, 19, cellStyle, resultList_sheet4.get(i).getSutak_car2());
	            Util_poi.setCell(cell4, row4, 20, cellStyle, resultList_sheet4.get(i).getSutak_car3());
            }else if(Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30){
//            	Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getTel());
            	Util_poi.setCell(cell4, row4, 7, cellStyle_center, resultList_sheet4.get(i).getIs_reg());

                Util_poi.setCell(cell4, row4, 8, cellStyle, resultList_sheet4.get(i).getSutak_nm());

                if("Y".equals(masked_info_status)){ Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getSutak_addr1());}
                else{ Util_poi.setCell(cell4, row4, 9, cellStyle, resultList_sheet4.get(i).getMasked_sutak_addr1());}

                Util_poi.setCell(cell4, row4, 10, cellStyle, resultList_sheet4.get(i).getSutak_num());
                Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getComp_cls_detail_nm());
                Util_poi.setNumberCell3(cell4, row4, 12, cellStyle_number, resultList_sheet4.get(i).getSutak_cars_cnt());
                Util_poi.setNumberCell3(cell4, row4, 13, cellStyle_number, resultList_sheet4.get(i).getSutak_sum_charge());


            	Util_poi.setNumberCell3(cell4, row4, 14, cellStyle_number, resultList_sheet4.get(i).getSutak_charge1());
    	        Util_poi.setNumberCell3(cell4, row4, 15, cellStyle_number, resultList_sheet4.get(i).getSutak_charge2());
    	        Util_poi.setNumberCell3(cell4, row4, 16, cellStyle_number, resultList_sheet4.get(i).getSutak_charge3());
            	Util_poi.setNumberCell3(cell4, row4, 17, cellStyle_number, resultList_sheet4.get(i).getSutak_charge4());
    	        Util_poi.setNumberCell3(cell4, row4, 18, cellStyle_number, resultList_sheet4.get(i).getSutak_charge5());
    	        Util_poi.setNumberCell3(cell4, row4, 19, cellStyle_number, resultList_sheet4.get(i).getSutak_charge6());
    	        Util_poi.setNumberCell3(cell4, row4, 20, cellStyle_number, resultList_sheet4.get(i).getSutak_charge7());
    	        Util_poi.setNumberCell3(cell4, row4, 21, cellStyle_number, resultList_sheet4.get(i).getSutak_charge8());
    	        Util_poi.setNumberCell3(cell4, row4, 22, cellStyle_number, resultList_sheet4.get(i).getSutak_charge9());
    	        Util_poi.setNumberCell3(cell4, row4, 23, cellStyle_number, resultList_sheet4.get(i).getSutak_charge10());
    	        Util_poi.setNumberCell3(cell4, row4, 24, cellStyle_number, resultList_sheet4.get(i).getSutak_charge11());
    	        Util_poi.setNumberCell3(cell4, row4, 25, cellStyle_number, resultList_sheet4.get(i).getSutak_charge12());
    	        /*
    	        Util_poi.setCell(cell4, row4, 27, cellStyle, resultList_sheet4.get(i).getSutak_car1());
	            Util_poi.setCell(cell4, row4, 28, cellStyle, resultList_sheet4.get(i).getSutak_car2());
	            Util_poi.setCell(cell4, row4, 29, cellStyle, resultList_sheet4.get(i).getSutak_car3());
	            Util_poi.setCell(cell4, row4, 30, cellStyle, resultList_sheet4.get(i).getSutak_car4());
	            Util_poi.setCell(cell4, row4, 31, cellStyle, resultList_sheet4.get(i).getSutak_car5());
	            Util_poi.setCell(cell4, row4, 32, cellStyle, resultList_sheet4.get(i).getSutak_car6());
	            Util_poi.setCell(cell4, row4, 33, cellStyle, resultList_sheet4.get(i).getSutak_car7());
	            Util_poi.setCell(cell4, row4, 34, cellStyle, resultList_sheet4.get(i).getSutak_car8());
	            Util_poi.setCell(cell4, row4, 35, cellStyle, resultList_sheet4.get(i).getSutak_car9());
	            Util_poi.setCell(cell4, row4, 36, cellStyle, resultList_sheet4.get(i).getSutak_car10());
	            Util_poi.setCell(cell4, row4, 37, cellStyle, resultList_sheet4.get(i).getSutak_car11());
	            Util_poi.setCell(cell4, row4, 38, cellStyle, resultList_sheet4.get(i).getSutak_car12());
    	         */
            }else{
            	Util_poi.setNumberCell3(cell4, row4, 7, cellStyle_number, resultList_sheet4.get(i).getOk_charge());
            	Util_poi.setCell(cell4, row4, 8, cellStyle_number, resultList_sheet4.get(i).getNo_perform());
            	Util_poi.setCell(cell4, row4, 9, cellStyle_center, resultList_sheet4.get(i).getIs_reg());

                Util_poi.setCell(cell4, row4, 10, cellStyle, resultList_sheet4.get(i).getSutak_nm());

                if("Y".equals(masked_info_status)){ Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getSutak_addr1());}
                else{ Util_poi.setCell(cell4, row4, 11, cellStyle, resultList_sheet4.get(i).getMasked_sutak_addr1());}

                Util_poi.setCell(cell4, row4, 12, cellStyle, resultList_sheet4.get(i).getSutak_num());
                Util_poi.setCell(cell4, row4, 13, cellStyle, resultList_sheet4.get(i).getComp_cls_detail_nm());
                Util_poi.setNumberCell3(cell4, row4, 14, cellStyle_number, resultList_sheet4.get(i).getSutak_cars_cnt());
                Util_poi.setNumberCell3(cell4, row4, 15, cellStyle_number, resultList_sheet4.get(i).getSutak_sum_charge());

                Util_poi.setNumberCell3(cell4, row4, 16, cellStyle_number, resultList_sheet4.get(i).getSutak_charge0_1());
                Util_poi.setNumberCell3(cell4, row4, 17, cellStyle_number, resultList_sheet4.get(i).getSutak_charge0_2());
            	Util_poi.setNumberCell3(cell4, row4, 18, cellStyle_number, resultList_sheet4.get(i).getSutak_charge1());
    	        Util_poi.setNumberCell3(cell4, row4, 19, cellStyle_number, resultList_sheet4.get(i).getSutak_charge2());
    	        Util_poi.setNumberCell3(cell4, row4, 20, cellStyle_number, resultList_sheet4.get(i).getSutak_charge3());
            	Util_poi.setNumberCell3(cell4, row4, 21, cellStyle_number, resultList_sheet4.get(i).getSutak_charge4());
    	        Util_poi.setNumberCell3(cell4, row4, 22, cellStyle_number, resultList_sheet4.get(i).getSutak_charge5());
    	        Util_poi.setNumberCell3(cell4, row4, 23, cellStyle_number, resultList_sheet4.get(i).getSutak_charge6());
    	        Util_poi.setNumberCell3(cell4, row4, 24, cellStyle_number, resultList_sheet4.get(i).getSutak_charge7());
    	        Util_poi.setNumberCell3(cell4, row4, 25, cellStyle_number, resultList_sheet4.get(i).getSutak_charge8());
    	        Util_poi.setNumberCell3(cell4, row4, 26, cellStyle_number, resultList_sheet4.get(i).getSutak_charge9());
    	        Util_poi.setNumberCell3(cell4, row4, 27, cellStyle_number, resultList_sheet4.get(i).getSutak_charge10());
    	        Util_poi.setNumberCell3(cell4, row4, 28, cellStyle_number, resultList_sheet4.get(i).getSutak_charge11());
    	        Util_poi.setNumberCell3(cell4, row4, 29, cellStyle_number, resultList_sheet4.get(i).getSutak_charge12());
            }

            /* ============== 시군구명, 업체정보 셀병합작업 시작 */
            if(index_sigungu == Integer.parseInt(resultList_sheet4.get(i).getSigungu_group())) {
                if(index_sigungu != 1){ // 셀병합작업
                    int index_base_data = (i+7-Integer.parseInt(resultList_sheet4.get(i).getSigungu_group()))+1;// 병합대상 기준 데이터 위치
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),0,0)); // 시군구명 셀병합
                }
                index_sigungu = 1;
            }else{
                index_sigungu++;
            }
            if(index_comp_data == Integer.parseInt(resultList_sheet4.get(i).getComp_group())) {
                if(index_comp_data != 1){ // 셀병합작업
                    int index_base_data = (i+7-Integer.parseInt(resultList_sheet4.get(i).getComp_group()))+1;// 병합대상 기준 데이터 위치
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),1,1)); // 업체정보 셀병합
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),2,2)); // 업체정보 셀병합
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),3,3)); // 업체정보 셀병합
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),4,4)); // 업체정보 셀병합
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),5,5)); // 업체정보 셀병합
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),6,6)); // 업체정보 셀병합
                    worksheet4.addMergedRegion(new CellRangeAddress(index_base_data,(i+7),7,7)); // 업체정보 셀병합
                }
                index_comp_data = 1;
            }else{
                index_comp_data++;
            }
            /* ============== 시군구명, 업체정보 셀병합작업 끝 */


        }

        /* =======================================================================  4번째 시트 끝 */

        /* =======================================================================  5번째 시트 시작 */
        if(Integer.valueOf(search_year) == 2017 && Integer.valueOf(search_bungi) == 30){
        	 XSSFSheet worksheet5 = workbook.createSheet("신고금액 검증결과");

             Row row5 = null; //로우
             Cell cell5 = null;// 셀

             worksheet5.setColumnWidth(0, (short)3400);
             worksheet5.setColumnWidth(1, (short)4600);
             worksheet5.setColumnWidth(2, (short)6000);
             worksheet5.setColumnWidth(3, (short)3600);
             worksheet5.setColumnWidth(4, (short)4000);
             worksheet5.setColumnWidth(5, (short)5000);
             worksheet5.setColumnWidth(6, (short)5000);
             worksheet5.setColumnWidth(7, (short)6000);
             worksheet5.setColumnWidth(8, (short)6000);
             worksheet5.setColumnWidth(9, (short)6000);
             worksheet5.setColumnWidth(10, (short)6000);
             worksheet5.setColumnWidth(11, (short)3200);
             worksheet5.setColumnWidth(12, (short)3600);


             //헤더작업
             row5 = worksheet5.createRow(0); //첫 줄 생성
         	if("30".equals(search_bungi)) Util_poi.setCell(cell5, row5, 0, cellStyle_title, "< "+search_year+"년 신고금액 검증결과 >");
         	else if("60".equals(search_bungi)) Util_poi.setCell(cell5, row5, 0, cellStyle_title, "< "+search_year+"년 신고금액 검증결과 >");
             row5 = worksheet5.createRow(1);
             row5 = worksheet5.createRow(2);
             Util_poi.setCell(cell5, row5, 0, cellStyle_normal, "* 비교율(%) : (연간위탁금액+연간배차금액)/연간계약금액 *100");
             Util_poi.setCell(cell5, row5, 7, cellStyle_normal, "**[계약금액]에 비해 [배차+위탁]금액의 차이가 큰 경우 추출(50% 이하 혹은 150%이상). 신고된 금액에 대한 확인을 요함.");

             row5 = worksheet5.createRow(3);
             Util_poi.setCell(cell5, row5, 0, cellStyle_td1, "신고자");
             Util_poi.setCell(cell5, row5, 1, cellStyle_td1, "");
             Util_poi.setCell(cell5, row5, 2, cellStyle_td1, "");
             Util_poi.setCell(cell5, row5, 3, cellStyle_td1, "");
             Util_poi.setCell(cell5, row5, 4, cellStyle_td1, "");
             Util_poi.setCell(cell5, row5, 5, cellStyle_td1, "");
             Util_poi.setCell(cell5, row5, 6, cellStyle_td1, "");
             Util_poi.setCell(cell5, row5, 7, cellStyle_td3, "신고금액 검증결과");
             Util_poi.setCell(cell5, row5, 8, cellStyle_td3, "");
             Util_poi.setCell(cell5, row5, 9, cellStyle_td3, "");
             Util_poi.setCell(cell5, row5, 10, cellStyle_td3, "");
             Util_poi.setCell(cell5, row5, 11, cellStyle_td3, "");
             Util_poi.setCell(cell5, row5, 12, cellStyle_td3, "");

             row5 = worksheet5.createRow(4);
             Util_poi.setCell(cell5, row5, 0, cellStyle_td1, "시군구");
             Util_poi.setCell(cell5, row5, 1, cellStyle_td1, "사업자명");
             Util_poi.setCell(cell5, row5, 2, cellStyle_td1, "주소");
             Util_poi.setCell(cell5, row5, 3, cellStyle_td1, "사업자번호\n(세금관련)");
             Util_poi.setCell(cell5, row5, 4, cellStyle_td1, "법인번호");
             Util_poi.setCell(cell5, row5, 5, cellStyle_td1, "업태");
             Util_poi.setCell(cell5, row5, 6, cellStyle_td1, "보유차량수");
             Util_poi.setCell(cell5, row5, 7, cellStyle_td3, "전체계약금액");
             Util_poi.setCell(cell5, row5, 8, cellStyle_td3, "전체위탁금액");
             Util_poi.setCell(cell5, row5, 9, cellStyle_td3, "전체배차금액");
             Util_poi.setCell(cell5, row5, 10, cellStyle_td3, "위탁+배차금액");
             Util_poi.setCell(cell5, row5, 11, cellStyle_td3, "비교율(%)*");
             Util_poi.setCell(cell5, row5, 12, cellStyle_td3, "검증결과**");

             worksheet5.addMergedRegion(new CellRangeAddress(3,3,0,6)); //셀병합 -  행 : 0   을   1까지    열 : 0   을  0까지
             worksheet5.addMergedRegion(new CellRangeAddress(3,3,7,12)); //셀병합 -  행 : 0   을   1까지    열 : 0   을  0까지


             int index_sigungu_s5 = 1;   // 시도명 셀 병합용
             for(int i=0; i<resultList_sheet5.size(); i++){
                 row5 = worksheet5.createRow(i+5);
                 Util_poi.setCell(cell5, row5, 0, cellStyle, "["+resultList_sheet5.get(i).getSido_nm()+"] "+resultList_sheet5.get(i).getSigungu_nm());
                 Util_poi.setCell(cell5, row5, 1, cellStyle, resultList_sheet5.get(i).getComp_nm());

                 if("Y".equals(masked_info_status)){ Util_poi.setCell(cell5, row5, 2, cellStyle, resultList_sheet5.get(i).getAddr1());}
                 else{ Util_poi.setCell(cell5, row5, 2, cellStyle, resultList_sheet5.get(i).getMasked_addr1());}

                 Util_poi.setCell(cell5, row5, 3, cellStyle, resultList_sheet5.get(i).getComp_bsns_num());
                 Util_poi.setCell(cell5, row5, 4, cellStyle, resultList_sheet5.get(i).getComp_corp_num());
                 Util_poi.setCell(cell5, row5, 5, cellStyle, resultList_sheet5.get(i).getComp_cls_detail());
                 Util_poi.setNumberCell3(cell5, row5, 6, cellStyle_number, resultList_sheet5.get(i).getCar_cls_tot());
                 Util_poi.setNumberCell3(cell5, row5, 7, cellStyle_number, resultList_sheet5.get(i).getContract_charge());
                 Util_poi.setNumberCell3(cell5, row5, 8, cellStyle_number, resultList_sheet5.get(i).getTrust_charge());
                 Util_poi.setNumberCell3(cell5, row5, 9, cellStyle_number, resultList_sheet5.get(i).getOper_charge());
                 Util_poi.setNumberCell3(cell5, row5, 10, cellStyle_number,resultList_sheet5.get(i).getTo_charge());
                 Util_poi.setCell(cell5, row5, 11, cellStyle_right, resultList_sheet5.get(i).getPer_());
                 Util_poi.setCell(cell5, row5, 12, cellStyle, resultList_sheet5.get(i).getStatus());


                 /* ============== 시도명 셀병합작업 시작 */
                 if(index_sigungu_s5 == Integer.parseInt(resultList_sheet5.get(i).getSigungu_group())) {
                     if(index_sigungu_s5 != 1){ // 셀병합작업
                         int index_base_data = (i+5-Integer.parseInt(resultList_sheet5.get(i).getSigungu_group()))+1;// 병합대상 기준 데이터 위치
                         worksheet5.addMergedRegion(new CellRangeAddress(index_base_data,(i+5),0,0)); // 시도 셀병합
                     }
                     index_sigungu_s5 = 1;
                 }else{
                	 index_sigungu_s5++;
                 }
                 /* ============== 시도명 셀병합작업 끝 */
             }


        }
        /* =======================================================================  5번째 시트 끝 */

        String file_name = Util.getDateFormat3()+"_"+SessionVO.getUser_id()+".xlsx"; //임시저장할 파일 이름
        FileOutputStream output = null;
        PrintWriter out = null;
        try {
            output = new FileOutputStream(fileStorePath+file_name);
            workbook.write(output);//파일쓰기 끝.
            output.close();
            String fileName = "";
            //2018.02.22 pes 17년 이후 연도별조회
            if(Integer.parseInt(search_year) < 2017){
            	fileName=search_year+"_"+search_bungi+"_BungiSingoResult"+"_"+Util.getDateFormat3()+".xlsx";//다운로드할 파일 이름
            }else{
            	fileName=search_year+"_YearSingoResult"+"_"+Util.getDateFormat3()+".xlsx";//다운로드할 파일 이름
            }
            out = res.getWriter();
            JSONObject result = new JSONObject();
            result.put("file_path", fileStorePath);
            result.put("file_name", file_name);
            result.put("fileName", fileName);
            out.write(result.toString());	//200413 오승민 엑셀 다운로드 시 json이 xss필터 걸려 에러나서 주석
//            Util_file.fileDownloadAndDelete(req, res, file_path, file_name, fileName);//파일다운로드 후 임시저장파일 삭제
        } catch (FileNotFoundException e) {
        	logger.error("[ERROR] - FileNotFoundException : ", e);
        } catch (IOException e) {
        	logger.error("[ERROR] - IOException : ", e);
        }catch(JSONException e) {
        	logger.error("[ERROR] - JSONException : ", e);
		}finally{
			try {
				if(output != null) output.close();
				if(out != null) out.close();
			}catch(IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			}
		}

    }



    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans9_detail.do")
    public String FpisAdminStatTrans9_detail(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,UnknownHostException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        FpisAdminStatTrans7VO vo = new FpisAdminStatTrans7VO();
        vo.setSearch_comp_bsns_num(shVO.getUsr_mst_key());
        vo.setUsr_mst_key(shVO.getUsr_mst_key());
        vo.setSearch_year(shVO.getSearch_year());
        vo.setBase_year(shVO.getBase_year());
        vo.setQuarter(shVO.getQuarter());
        vo.setDisposition_type("OMISSION");

        List<FpisAdminStatTrans7VO> omissionSutakList = FpisSvc.selectOmissionSutakList(vo);
        List<FpisAdminStatTrans7VO> noPerformList = FpisSvc.selectNoPerformList(vo); //2018.08.28 PES 미이행율 추가

        int dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0){
        	vo.setDisposition_type("OMISSION");
        	FpisSvc.insertDisposition(vo);
        }

        FpisAdminStatTrans7VO disposition =  FpisSvc.selectDispositionDetail_omission(vo); //디테일 정보 가져오기

        String masked_info_status = req.getParameter("masked_info_status");
        if("Y".equals(masked_info_status)){
        	/*사업자정보 이력 관리*/
        	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
        	/*이력 삽입_누락의심자*/
        	FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
        	accessLogVO.setRcode(req.getParameter("rcode"));
        	accessLogVO.setBcode(req.getParameter("bcode"));
        	accessLogVO.setComp_mst_key(disposition.getUsr_mst_key().replaceAll("-", ""));
        	accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
        	accessLogVO.setJob_cls("DE"); //엑셀다운로드
        	accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
        	accessLogVOList.add(accessLogVO);

        	/*이력 삽입_수탁자*/
        	for(int i=0;i<omissionSutakList.size();i++){
        		if(omissionSutakList.get(i).getUsr_mst_key() != null){
        			FpisAccessLogVO accessLogVO_sutak = new FpisAccessLogVO();
            		accessLogVO_sutak.setRcode(req.getParameter("rcode"));
            		accessLogVO_sutak.setBcode(req.getParameter("bcode"));
            		accessLogVO_sutak.setComp_mst_key(omissionSutakList.get(i).getUsr_mst_key().replaceAll("-", ""));
            		accessLogVO_sutak.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO_sutak.setJob_cls("DE");
            		accessLogVO_sutak.setMber_ip(InetAddress.getLocalHost().getHostAddress());
                	accessLogVOList.add(accessLogVO_sutak);
        		}
        	}
        	accessLogService.insertAccessLogByList(accessLogVOList);
        }else{
        	masked_info_status = "N";
        }

        model.addAttribute("masked_info_status", masked_info_status);

        //누락의심 결과등록 정보 가져오기
        if("D".equals(disposition.getDisposition_result()) || disposition.getDisposition_result() == null){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("OMISSION");
        	vo.setDis_seq(disposition.getDis_seq());

        	if(disposition.getDisposition_result() != null){
        		FpisAdminStatTrans7VO select_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
	        	String cancel_type = select_cancel.getCancel_type();
	        	String cancel_period = select_cancel.getCancel_period();
	        	String start_date = select_cancel.getStart_date();
	        	model.addAttribute("cancel_type"  , cancel_type);
	        	model.addAttribute("cancel_period"  , cancel_period);
	        	model.addAttribute("from_date"  , start_date);
        	}

        }else if("P".equals(disposition.getDisposition_result())){  // 과징금 시
        	vo.setDisposition_type("OMISSION");
        	vo.setDis_seq(disposition.getDis_seq());

        	FpisAdminStatTrans7VO select_fee = FpisSvc.selectDispositionFee(vo);
        	String omission_fee = select_fee.getFee();
        	model.addAttribute("omission_fee"  , omission_fee);
        }
        //2018.08.29 PES 차량추출기간추가
    	shVO.setS_date(shVO.getBase_year()+"0101");
    	int e_date = Integer.parseInt(shVO.getBase_year());
    	shVO.setE_date((e_date+1)+"0101");

        int cancel_totCnt = FpisSvc.selectCancelCarCount(shVO);
        int noperform_totCnt = FpisSvc.selectNoPerformCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(cancel_totCnt));
    	shVO.setDis_seq(disposition.getMinimum_seq());
    	shVO.setDisposition_type("OMISSION");
    	shVO.setDis_seq(disposition.getDis_seq());

    	List<FpisAdminStatTrans7VO> omission_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	//2018.08.29 PES 행정처분 최대일 계산
    	if(noPerformList.size() != 0){
    		double no_perform = Double.parseDouble(noPerformList.get(0).getNo_perform()) * 0.01;
	    	double p1 = no_perform * 10; //1차 행정처분 최대일
	    	double p2 = no_perform * 20; //2차 행정처분 최대일
	    	double p3 = no_perform * 30; //3차 행정처분 최대일

	    	if(p1 <= 0 || (0 < p1 && p1 <= 1)) p1 = 1;
	    	else p1 = Math.floor(p1);

	    	if(p2 <= 0 || (0 < p2 && p2 <= 1)) p2 = 1;
	    	//else if(0 < p2 && p2 <= 1) p2 = 1;
	    	else p2 = Math.floor(p2);

	    	if(p3 <= 0 || (0 < p3 && p3 <= 1)) p3 = 1;
	    	//else if(0 < p3 && p3 <= 1) p3 = 1;
	    	else p3 = Math.floor(p3);

	    	model.addAttribute("p1", (int)p1);
	    	model.addAttribute("p2", (int)p2);
	    	model.addAttribute("p3", (int)p3);
	    	model.addAttribute("no_perform", no_perform);
    	}
    	model.addAttribute("base_year",shVO.getBase_year());
    	model.addAttribute("OMISSION_TOTCNT", cancel_totCnt);
    	model.addAttribute("OmissionCancelCar", omission_cancelCar);
    	model.addAttribute("noperform_totCnt",noperform_totCnt);
    	model.addAttribute("noPerformList", noPerformList);
    	model.addAttribute("omissionSutakList", omissionSutakList);
        model.addAttribute("disposition", disposition);
        model.addAttribute("list_cur_page", req.getParameter("list_cur_page"));
        model.addAttribute("VO", shVO);

        model.addAttribute("rcode", "R13");
        model.addAttribute("bcode", "R13-09");

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans9_detail";
    }



    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans9_detail_save.do")
    public void FpisAdminStatTrans9_detail_save(FpisAdminStatTrans7VO vo, HttpServletResponse res, HttpServletRequest req,
    		@RequestParam(value="omission_totChk[]") List<String> omission_totChk) throws IOException,SQLException{
    	try {
    		DateFormat dateFormat = null;
    		Date date = null;
    		Calendar cal = null;
    		FpisAdminStatTrans7VO addVO = null;

    		FpisSvc.updateDisposition(vo);    // 결과 기본데이터 등록 disposition_base

    		if(vo.getDisposition_result().equals("D")){ //허가취소 등 행정처분시         ---start_date // end_date

    			int cancelCNT = FpisSvc.selectDispositionCnacelYN(vo);
    			if(cancelCNT == 0){
    				FpisSvc.insertDispositionCancel(vo);
    			}else{
    				FpisSvc.updateDispositionCancel(vo);
    			}


    			if(omission_totChk != null){
    				vo.setStart_date(req.getParameter("omission_from_date"));

    				dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					date = dateFormat.parse(vo.getStart_date());
    				cal = Calendar.getInstance();
    				cal.setTime(date);
    				cal.add(Calendar.DATE, Integer.parseInt(vo.getCancel_period()));
    				vo.setEnd_date(dateFormat.format(cal.getTime()));

    				List<FpisAdminStatTrans7VO> omission_totChk_list = new ArrayList<FpisAdminStatTrans7VO>();
    				String[] aa = omission_totChk.get(0).split(",");

    				for(int i = 0 ; i < aa.length  ; i++) {
    					addVO = new FpisAdminStatTrans7VO();
    					addVO.setCar_reg_seq(aa[i]);
    					addVO.setDis_seq(vo.getDis_seq());
    					addVO.setUsr_mst_key(vo.getUsr_mst_key());
    					addVO.setBase_year(vo.getBase_year());
    					addVO.setDisposition_type(vo.getDisposition_type());
    					addVO.setStart_date(vo.getStart_date());
    					addVO.setEnd_date(vo.getEnd_date());
    					addVO.setQuarter(vo.getQuarter());
    					addVO.setStep(vo.getStep());

    					omission_totChk_list.add(i, addVO);
    				}

    				FpisSvc.deleteDispositionCancelCar(vo);
    				FpisSvc.insertDispositionCancelCar(omission_totChk_list);
    			}
    			FpisSvc.deleteDispositionFee(vo);

    		}else if(vo.getDisposition_result().equals("P")){  //과징금
    			int feeCNT = FpisSvc.selectDispositionFeeYN(vo);
    			if(feeCNT == 0){
    				FpisSvc.insertDispositionFee(vo);
    			}else{
    				FpisSvc.updateDispositionFee(vo);
    			}
    			FpisSvc.deleteDispositionCancelCar(vo);
    			FpisSvc.deleteDispositionCancel(vo);
    		}else{
    			FpisSvc.deleteDispositionCancelCar(vo);
    			FpisSvc.deleteDispositionCancel(vo);
    			FpisSvc.deleteDispositionFee(vo);
    		}

    	}catch(ParseException e) {
    		logger.error("[ERROR] - ParseException : ", e);
    	}
    }


    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTrans10.do")
    public String FpisAdminStatTrans10(FpisAdminStatTrans10VO shVO, HttpServletRequest req, ModelMap model) throws SQLException {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        List<FpisAdminStatTrans10VO> dispositionRatio = null;
        List<FpisAdminStatTrans10VO> dispositionList = null;

        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sido_cd(searchSidoCd);
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }
            String org_comp_corp_num = shVO.getSearch_comp_corp_num();
            if(org_comp_corp_num != null){
                shVO.setSearch_comp_corp_num(shVO.getSearch_comp_corp_num().replaceAll("-", ""));    // 2015.08.07 법인번호 검색 "-" 기호 제거
            }

            //180209 오승민 연 1회신고로 변경 후 2017년 이후 연도검색 시 분기 조건 추가
            if("".equals(shVO.getSearch_bungi()) && "OMISSION".equals(shVO.getSearch_reg())){
            	shVO.setSearch_bungi("0");
            }

            dispositionRatio=FpisSvc.selectDispositionRatio(shVO);
    		dispositionList=FpisSvc.selectDispositionStatsList(shVO);

            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
            shVO.setSearch_comp_corp_num(org_comp_corp_num);    // 2015.08.07 법인번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
        }
		int strYear = 2016;
		int endYear = Calendar.getInstance().get(Calendar.YEAR)-1;
     // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
     	String DaeguGubun = "";
     	int getYear = 0;
     	if (shVO.getSearch_year() == null) {
     		getYear = endYear;
     	} else {
     		getYear = Integer.parseInt(shVO.getSearch_year());
     	}
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                DaeguGubun = svo.getAdm_area_code(); // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
                searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            /* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
			if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
				//sigunList = mberManageService.selectSigungu2016(sigunguVO);
			} else {
				sigunList = mberManageService.selectSigungu2016(vo);
			}
        }
        model.addAttribute("SIGUNLIST", sigunList);

		if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
		}

        model.addAttribute("VO"       , shVO);
        model.addAttribute("dispositionRatio"   , dispositionRatio);
        model.addAttribute("dispositionList" , dispositionList);

        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("rcode", "R13");
        model.addAttribute("bcode", "R13-10");

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTrans10";
    }


    /**
     * 2019.12.02 jws 제도통계 결과 통합 조회
     */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTransUnit.do")
    public String FpisAdminStatTransUnit(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();
        String searchName = req.getParameter("search_name");
        String searchCompBsnsNum = req.getParameter("search_comp_bsns_num");

        int totCnt   = 0;

        List<FpisAdminStatTrans7VO> resultList = null;

        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        if (searchSidoCd != null) {
            if(searchSigunguCd != null && !searchSigunguCd.equals("")){
            	shVO.setSearch_sido_cd(searchSidoCd);
                shVO.setSearch_sigungu_cd(searchSigunguCd);
            }else{
                shVO.setSearch_sido_cd(searchSidoCd);
            }
            //180209 오승민 연 1회신고로 변경 후 2017년 이후 연도검색 시 분기 조건 추가
            if("".equals(shVO.getSearch_bungi())){
            	shVO.setSearch_bungi("0");
            }

            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 사업자번호 검색 "-" 기호 제거
            }

            if(searchName != null){
                shVO.setSearch_name(searchName);
            }
            if(searchCompBsnsNum != null){
            	shVO.setSearch_comp_bsns_num(searchCompBsnsNum.replaceAll("-", ""));
            }


            totCnt = FpisSvc.selectOmissionCount(shVO);
            totCnt = FpisSvc.selectStatTransUnitCnt(shVO);
	        shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
	        shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
	        shVO.setTot_page(Util.calcurateTPage(totCnt));
	        // PAGING END ------------------

	        resultList = FpisSvc.selectStatTransUnitList(shVO);
            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        }

        // 시도 관리자 기능 추가
        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
                searchSidoCd = svo.getAdm_area_code().substring(0, 2);
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
        model.addAttribute("SIDOLIST", sidoList);

        SigunguVO  vo  = new SigunguVO();
        List<SigunguVO> sigunList = null;
        if(searchSidoCd != null  && !searchSidoCd.equals("")) {
            vo.setSidoCd(searchSidoCd);
            sigunList = mberManageService.selectSigungu2016(vo);
        }
        model.addAttribute("SIGUNLIST", sigunList);

		/* 2021.09.23 jwchoi : strYear = 2016 > 2021, 1안 테스트반영으로 년도변경 */
		int strYear = 2021;
		int endYear = Calendar.getInstance().get(Calendar.YEAR); // 현재 년도

		// 현재 년도가 마감 됬는지 안됬는지 확인 후 마감 안됬으면 endYear를 작년으로 변경
		/* 2021.09.28 jwchoi 1안 테스트반영때문에 주석처리, 나중에 주석풀것 */
		//if(!Util.modifyEndDecide(Integer.toString(endYear)))
			//endYear = endYear - 1;

		if (shVO.getSearch_year() == null){
			shVO.setSearch_year(String.valueOf(endYear));
			shVO.setSearch_status("A");
		}
        shVO.setBase_year(req.getParameter("search_year"));

        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("resultList" , resultList);

        model.addAttribute("strYear", strYear);
        model.addAttribute("endYear", endYear);

        model.addAttribute("rcode", "R13");
        model.addAttribute("bcode", "R13-11");

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTransUnit";
    }

    /* 2021. 09. 14 jwchoi : 1안 테스트반영, FpisAdminStatTransUnit_OLD추가, 2017~2020메뉴 */
	@RequestMapping("/admin/obeySystem/trans/FpisAdminStatTransUnit_OLD.do")
	public String FpisAdminStatTransUnit_OLD(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException {
		SessionVO svo = (SessionVO) req.getSession().getAttribute("SessionVO");

		String searchSidoCd = shVO.getHid_sido_code();
		String searchSigunguCd = shVO.getHid_sigungu_code();
		String searchName = req.getParameter("search_name");
		String searchCompBsnsNum = req.getParameter("search_comp_bsns_num");

		int totCnt = 0;

		List<FpisAdminStatTrans7VO> resultList = null;
		
		//int strYear = 2017;
		/* 2021.09.23 jwchoi : endYear = 2019 추가 - 과거메뉴라서 년도고정 */
		/* 2021.09.28 jwchoi : endYear 2019 > 2020 테스트반영때메 */
		//int endYear = 2020;
		//int endYear = Calendar.getInstance().get(Calendar.YEAR); // 현재 년도
		int strYear = FpisSvc.selectstrYear();
		int endYear = FpisSvc.selectendYear();
		
		// 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
     	String DaeguGubun = "";
     	int getYear = 0;
     	if (req.getParameter("search_year") == null) {
     		getYear = endYear;
     	} else {
     		getYear = Integer.parseInt(req.getParameter("search_year"));
     	}

		if(shVO.getCur_page() <= 0) {
			shVO.setCur_page(1);
		}
		
		if(shVO.getSearch_year() == null) {
			shVO.setSearch_year(String.valueOf(endYear));
			shVO.setSearch_status("A");
		}
		shVO.setBase_year(req.getParameter("search_year"));

		if(searchSidoCd != null) {
			if(searchSigunguCd != null && !searchSigunguCd.equals("")) {
				shVO.setSearch_sido_cd(searchSidoCd);
				shVO.setSearch_sigungu_cd(searchSigunguCd);
			}else {
				shVO.setSearch_sido_cd(searchSidoCd);
			}
			// 180209 오승민 연 1회신고로 변경 후 2017년 이후 연도검색 시 분기 조건 추가
			if("".equals(shVO.getSearch_bungi())) {
				shVO.setSearch_bungi("0");
			}

			String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
			if(org_comp_bsns_num != null) {
				shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", "")); // 사업자번호
																									// 검색
																									// "-"
																									// 기호
																									// 제거
			}

			if(searchName != null) {
				shVO.setSearch_name(searchName);
			}
			if(searchCompBsnsNum != null) {
				shVO.setSearch_comp_bsns_num(searchCompBsnsNum.replaceAll("-", ""));
			}
			
	        /* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
	        if(svo.getMber_cls().equals("ADM")){
	        	if ("27".equals(DaeguGubun) && getYear < 2023 && "27720".equals(searchSigunguCd)) {
	        		shVO.setSearch_sigungu_cd("27720");
	        	} else if ("47".equals(DaeguGubun) && getYear < 2023 && "47720".equals(searchSigunguCd)) {
	        		shVO.setSearch_sigungu_cd("27720");
	        	}
	        } else if (getYear < 2023 && svo.getMber_cls().equals("SYS")) {
	        	if (getYear < 2023 && "47720".equals(searchSigunguCd)) {
	        		shVO.setSearch_sigungu_cd("47720");
	        	}
	        }

			totCnt = FpisSvc.selectOmissionCount(shVO);
			totCnt = FpisSvc.selectStatTransUnitCnt(shVO);
			shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
			shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
			shVO.setTot_page(Util.calcurateTPage(totCnt));
			// PAGING END ------------------

			resultList = FpisSvc.selectStatTransUnitList(shVO);
			shVO.setSearch_comp_bsns_num(org_comp_bsns_num); // 사업자번호 검색 "-" 기호
																// 제거 사용자가 입력한 값
																// 그대로 반환

		}

		// 시도 관리자 기능 추가
		if(svo.getMber_cls().equals("ADM")) {
			model.addAttribute("hid_sido_code", svo.getAdm_area_code().substring(0, 2));
			if(svo.getAdm_area_code().length() == 2) { // 2014.12.01 mgkim 시도
														// 관리자 검색조건 확인
				searchSidoCd = svo.getAdm_area_code();
				DaeguGubun = svo.getAdm_area_code(); // 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리.
				model.addAttribute("hid_sido_code", svo.getAdm_area_code());
				model.addAttribute("hid_sigungu_code", searchSigunguCd);
			}else {
				model.addAttribute("hid_sigungu_code", svo.getAdm_area_code());
				model.addAttribute("hid_sigungu_name", svo.getAdm_area_name());
				searchSidoCd = svo.getAdm_area_code().substring(0, 2);
			}
		}else {
			model.addAttribute("hid_sido_code", searchSidoCd);
			model.addAttribute("hid_sigungu_code", searchSigunguCd);
		}

		List<SigunguVO> sidoList = mberManageService.selectSido2016(new SigunguVO());
		model.addAttribute("SIDOLIST", sidoList);

		SigunguVO vo = new SigunguVO();
		List<SigunguVO> sigunList = null;
		if(searchSidoCd != null && !searchSidoCd.equals("")) {
			vo.setSidoCd(searchSidoCd);
			/* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
			if ("27".equals(DaeguGubun) || "47".equals(DaeguGubun)) {
				sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
				//sigunList = mberManageService.selectSigungu2016(sigunguVO);
			} else {
				sigunList = mberManageService.selectSigungu2016(vo);
			}
		}
		/* 2023.10.27 jwchoi 경북 군위군 -> 대구 군위군 편입 반영. 실적년도와 함께 검색 시 처리. */
		if(svo.getMber_cls().equals("SYS") && getYear < 2023) {
			sigunList = mberManageService.selectSigunguDaegu(vo, getYear);
		}
		model.addAttribute("SIGUNLIST", sigunList);
		

		// 현재 년도가 마감 됬는지 안됬는지 확인 후 마감 안됬으면 endYear를 작년으로 변경
		if(!Util.modifyEndDecide(Integer.toString(endYear)))
			endYear = endYear - 1;
		
		
		int progress = FpisSvc.getProgress();

		model.addAttribute("VO", shVO); // PAGING VO
		model.addAttribute("TOTCNT", totCnt); // PAGING VO
		model.addAttribute("resultList", resultList);

		model.addAttribute("strYear", strYear);
		model.addAttribute("endYear", endYear);

		model.addAttribute("rcode", "R13");
		model.addAttribute("bcode", "R13-11");
		
		model.addAttribute("progress", progress);

		return "/fpis/admin/obeySystem/trans/FpisAdminStatTransUnit_OLD";
	}


    /*2020.01.21 jws 제도통계 실적신고 상세정보*/
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTransUniOmissionDetail.do")
    public void FpisAdminStatTransUniOmissionDetail(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model
    		,HttpServletResponse res ) throws SQLException,UnknownHostException,JSONException,IOException {

    	SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");
        FpisAdminStatTrans7VO vo = new FpisAdminStatTrans7VO();
        vo.setSearch_comp_bsns_num(shVO.getUsr_mst_key());
        vo.setUsr_mst_key(shVO.getUsr_mst_key());
        vo.setSearch_year(shVO.getSearch_year());
        //vo.setBase_year(shVO.getBase_year());
        vo.setBase_year(shVO.getSearch_year());
        vo.setQuarter(shVO.getQuarter());
        vo.setDisposition_type("OMISSION");

        /*2020.11.11 ysw 정보노출에 따른 마스킹 처리*/
        String masked_info_status = req.getParameter("masked_info_status");
//        if(Integer.parseInt(vo.getBase_year()) < 2020){
        	//실적신고 상세정보(업체정보) 불러오기
            List<FpisAdminStatTrans7VO> omissionSutakList = FpisSvc.selectOmissionSutakList(vo);

            if("Y".equals(masked_info_status) && omissionSutakList.size() > 0) {
            	List<FpisAccessLogVO> accessLogVOList = new ArrayList<FpisAccessLogVO>();
            	/*이력 삽입*/
            	for(int i = 0; i < omissionSutakList.size(); i++) {
            		FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
            		accessLogVO.setRcode(req.getParameter("rcode"));
            		accessLogVO.setBcode(req.getParameter("bcode"));
            		accessLogVO.setComp_mst_key(omissionSutakList.get(i).getUsr_mst_key().replaceAll("-", ""));
            		accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
            		accessLogVO.setJob_cls("SE"); //목록조회
            		accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
            		accessLogVOList.add(accessLogVO);
            	}
            	accessLogService.insertAccessLogByList(accessLogVOList);
    		}

            JSONObject json = new JSONObject();
            JSONArray jsonArr = new JSONArray();

            //실적신고 상세정보 데이터 파라미터 셋
            for(int i=0; i<omissionSutakList.size(); i++){
            	JSONObject obj = new JSONObject();
        		obj.put("comp_nm", omissionSutakList.get(i).getComp_nm());
        		if("Y".equals(masked_info_status)) {
        			obj.put("addr1", omissionSutakList.get(i).getAddr1());
        		}else {
        			obj.put("addr1", omissionSutakList.get(i).getMasked_addr1());
        		}

        		obj.put("usr_mst_key", omissionSutakList.get(i).getUsr_mst_key());
        		obj.put("tel", omissionSutakList.get(i).getTel());
        		obj.put("comp_cls", omissionSutakList.get(i).getComp_cls());
        		obj.put("sutak_sum_charge", omissionSutakList.get(i).getSutak_sum_charge());
        		obj.put("sutak_charge1", omissionSutakList.get(i).getSutak_charge1());
        		obj.put("sutak_charge2", omissionSutakList.get(i).getSutak_charge2());
        		obj.put("sutak_charge3", omissionSutakList.get(i).getSutak_charge3());
        		obj.put("sutak_charge4", omissionSutakList.get(i).getSutak_charge4());
        		obj.put("sutak_charge5", omissionSutakList.get(i).getSutak_charge5());
        		obj.put("sutak_charge6", omissionSutakList.get(i).getSutak_charge6());
        		obj.put("sutak_charge7", omissionSutakList.get(i).getSutak_charge7());
        		obj.put("sutak_charge8", omissionSutakList.get(i).getSutak_charge8());
        		obj.put("sutak_charge9", omissionSutakList.get(i).getSutak_charge9());
        		obj.put("sutak_charge10", omissionSutakList.get(i).getSutak_charge10());
        		obj.put("sutak_charge11", omissionSutakList.get(i).getSutak_charge11());
        		obj.put("sutak_charge12", omissionSutakList.get(i).getSutak_charge12());
        		obj.put("sutak_car1", omissionSutakList.get(i).getSutak_car1());
        		obj.put("sutak_car2", omissionSutakList.get(i).getSutak_car2());
        		obj.put("sutak_car3", omissionSutakList.get(i).getSutak_car3());
        		obj.put("sutak_car4", omissionSutakList.get(i).getSutak_car4());
        		obj.put("sutak_car5", omissionSutakList.get(i).getSutak_car5());
        		obj.put("sutak_car6", omissionSutakList.get(i).getSutak_car6());
        		obj.put("sutak_car7", omissionSutakList.get(i).getSutak_car7());
        		obj.put("sutak_car8", omissionSutakList.get(i).getSutak_car8());
        		obj.put("sutak_car9", omissionSutakList.get(i).getSutak_car9());
        		obj.put("sutak_car10", omissionSutakList.get(i).getSutak_car10());
        		obj.put("sutak_car11", omissionSutakList.get(i).getSutak_car11());
        		obj.put("sutak_car12", omissionSutakList.get(i).getSutak_car12());
        		obj.put("sutak_charge0_1", omissionSutakList.get(i).getSutak_charge0_1());
        		obj.put("sutak_charge0_2", omissionSutakList.get(i).getSutak_charge0_2());
        		obj.put("comp_cls_detail", omissionSutakList.get(i).getComp_cls_detail());

        		jsonArr.put(obj);
            }
            json.put("omissionSutakList",jsonArr);
    		res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            PrintWriter out = res.getWriter();
            out.write(json.toString());
            out.close();
//        }
        /* 2022.10.18 jwchoi 허위의심 반영 취소되어 주석처리*/
//        else{
//        	FpisAdminStatTrans7VO fallacyResult = FpisSvc.selectFallacyResult(vo);
//        	List<FpisAdminStatTrans7VO> omissionDivisionList_WE = FpisSvc.selectOmissionDetailList_WE(vo);
//        	List<FpisAdminStatTrans7VO> omissionDivisionList_SU = FpisSvc.selectOmissionDetailList_SU(vo);
//        	List<FpisAdminStatTrans7VO> fallacyDivisionList_WE = FpisSvc.selectFallacyDetailList_WE(vo);
//        	List<FpisAdminStatTrans7VO> fallacyDivisionList_SU = FpisSvc.selectFallacyDetailList_SU(vo);
//
//        	JSONObject json = new JSONObject();
//            JSONArray jsonArr = new JSONArray();
//
//            //허위의심 결과조회
//            JSONObject obj = new JSONObject();
//            if(fallacyResult != null){
//            	obj.put("ok_charge",fallacyResult.getOk_charge());
//            	obj.put("result_fal",fallacyResult.getResult_fal());
//            }
//            json.put("fallacyResult",obj);
//
//            //누락의심 위탁계약정보 확인
//            for(int i=0; i<omissionDivisionList_WE.size(); i++){
//            	JSONObject obj_1 = new JSONObject();
//
//            	obj_1.put("is_target_reg",omissionDivisionList_WE.get(i).getIs_target_reg());
//            	obj_1.put("target_charge",omissionDivisionList_WE.get(i).getTarget_charge());
//            	obj_1.put("reg_nurak_rate",omissionDivisionList_WE.get(i).getReg_nurak_rate());
//            	obj_1.put("usr_sum_charge",omissionDivisionList_WE.get(i).getUsr_sum_charge());
//            	obj_1.put("usr_charge1",omissionDivisionList_WE.get(i).getUsr_charge1());
//            	obj_1.put("usr_charge2",omissionDivisionList_WE.get(i).getUsr_charge2());
//            	obj_1.put("usr_charge3",omissionDivisionList_WE.get(i).getUsr_charge3());
//            	obj_1.put("usr_charge4",omissionDivisionList_WE.get(i).getUsr_charge4());
//            	obj_1.put("usr_charge5",omissionDivisionList_WE.get(i).getUsr_charge5());
//            	obj_1.put("usr_charge6",omissionDivisionList_WE.get(i).getUsr_charge6());
//            	obj_1.put("usr_charge7",omissionDivisionList_WE.get(i).getUsr_charge7());
//            	obj_1.put("usr_charge8",omissionDivisionList_WE.get(i).getUsr_charge8());
//            	obj_1.put("usr_charge9",omissionDivisionList_WE.get(i).getUsr_charge9());
//            	obj_1.put("usr_charge10",omissionDivisionList_WE.get(i).getUsr_charge10());
//            	obj_1.put("usr_charge11",omissionDivisionList_WE.get(i).getUsr_charge11	());
//            	obj_1.put("usr_charge12",omissionDivisionList_WE.get(i).getUsr_charge12());
//            	obj_1.put("contractor_nm",omissionDivisionList_WE.get(i).getContractor_nm());
//            	obj_1.put("contractor_addr",omissionDivisionList_WE.get(i).getContractor_addr());
//            	obj_1.put("contractor_num",omissionDivisionList_WE.get(i).getContractor_num());
//            	obj_1.put("contractor_cls",omissionDivisionList_WE.get(i).getContractor_cls());
//            	obj_1.put("contractor_sum_charge",omissionDivisionList_WE.get(i).getContractor_sum_charge());
//            	obj_1.put("contractor_charge1",omissionDivisionList_WE.get(i).getContractor_charge1());
//            	obj_1.put("contractor_charge2",omissionDivisionList_WE.get(i).getContractor_charge2());
//            	obj_1.put("contractor_charge3",omissionDivisionList_WE.get(i).getContractor_charge3());
//            	obj_1.put("contractor_charge4",omissionDivisionList_WE.get(i).getContractor_charge4());
//            	obj_1.put("contractor_charge5",omissionDivisionList_WE.get(i).getContractor_charge5());
//            	obj_1.put("contractor_charge6",omissionDivisionList_WE.get(i).getContractor_charge6());
//            	obj_1.put("contractor_charge7",omissionDivisionList_WE.get(i).getContractor_charge7());
//            	obj_1.put("contractor_charge8",omissionDivisionList_WE.get(i).getContractor_charge8());
//            	obj_1.put("contractor_charge9",omissionDivisionList_WE.get(i).getContractor_charge9());
//            	obj_1.put("contractor_charge10",omissionDivisionList_WE.get(i).getContractor_charge10());
//            	obj_1.put("contractor_charge11",omissionDivisionList_WE.get(i).getContractor_charge11());
//            	obj_1.put("contractor_charge12",omissionDivisionList_WE.get(i).getContractor_charge12());
//            	jsonArr.put(obj_1);
//            }
//
//            json.put("omi_we",jsonArr);
//            jsonArr = new JSONArray();
//
//            //누락의심 수탁계약정보 확인
//            for(int i=0; i<omissionDivisionList_SU.size(); i++){
//            	JSONObject obj_2 = new JSONObject();
//
//            	obj_2.put("is_target_reg",omissionDivisionList_SU.get(i).getIs_target_reg());
//            	obj_2.put("target_charge",omissionDivisionList_SU.get(i).getTarget_charge());
//            	obj_2.put("reg_nurak_rate",omissionDivisionList_SU.get(i).getReg_nurak_rate());
//            	obj_2.put("usr_sum_charge",omissionDivisionList_SU.get(i).getUsr_sum_charge());
//            	obj_2.put("usr_charge1",omissionDivisionList_SU.get(i).getUsr_charge1());
//            	obj_2.put("usr_charge2",omissionDivisionList_SU.get(i).getUsr_charge2());
//            	obj_2.put("usr_charge3",omissionDivisionList_SU.get(i).getUsr_charge3());
//            	obj_2.put("usr_charge4",omissionDivisionList_SU.get(i).getUsr_charge4());
//            	obj_2.put("usr_charge5",omissionDivisionList_SU.get(i).getUsr_charge5());
//            	obj_2.put("usr_charge6",omissionDivisionList_SU.get(i).getUsr_charge6());
//            	obj_2.put("usr_charge7",omissionDivisionList_SU.get(i).getUsr_charge7());
//            	obj_2.put("usr_charge8",omissionDivisionList_SU.get(i).getUsr_charge8());
//            	obj_2.put("usr_charge9",omissionDivisionList_SU.get(i).getUsr_charge9());
//            	obj_2.put("usr_charge10",omissionDivisionList_SU.get(i).getUsr_charge10());
//            	obj_2.put("usr_charge11",omissionDivisionList_SU.get(i).getUsr_charge11	());
//            	obj_2.put("usr_charge12",omissionDivisionList_SU.get(i).getUsr_charge12());
//            	obj_2.put("contractor_nm",omissionDivisionList_SU.get(i).getContractor_nm());
//            	obj_2.put("contractor_addr",omissionDivisionList_SU.get(i).getContractor_addr());
//            	obj_2.put("contractor_num",omissionDivisionList_SU.get(i).getContractor_num());
//            	obj_2.put("contractor_cls",omissionDivisionList_SU.get(i).getContractor_cls());
//            	obj_2.put("contractor_sum_charge",omissionDivisionList_SU.get(i).getContractor_sum_charge());
//            	obj_2.put("contractor_charge1",omissionDivisionList_SU.get(i).getContractor_charge1());
//            	obj_2.put("contractor_charge2",omissionDivisionList_SU.get(i).getContractor_charge2());
//            	obj_2.put("contractor_charge3",omissionDivisionList_SU.get(i).getContractor_charge3());
//            	obj_2.put("contractor_charge4",omissionDivisionList_SU.get(i).getContractor_charge4());
//            	obj_2.put("contractor_charge5",omissionDivisionList_SU.get(i).getContractor_charge5());
//            	obj_2.put("contractor_charge6",omissionDivisionList_SU.get(i).getContractor_charge6());
//            	obj_2.put("contractor_charge7",omissionDivisionList_SU.get(i).getContractor_charge7());
//            	obj_2.put("contractor_charge8",omissionDivisionList_SU.get(i).getContractor_charge8());
//            	obj_2.put("contractor_charge9",omissionDivisionList_SU.get(i).getContractor_charge9());
//            	obj_2.put("contractor_charge10",omissionDivisionList_SU.get(i).getContractor_charge10());
//            	obj_2.put("contractor_charge11",omissionDivisionList_SU.get(i).getContractor_charge11());
//            	obj_2.put("contractor_charge12",omissionDivisionList_SU.get(i).getContractor_charge12());
//            	jsonArr.put(obj_2);
//            }
//            json.put("omi_su",jsonArr);
//            jsonArr = new JSONArray();
//
//            //허위의심 위탁계약정보 확인
//            for(int i=0; i<fallacyDivisionList_WE.size(); i++){
//            	JSONObject obj_3 = new JSONObject();
//
//            	obj_3.put("contractor_nm",fallacyDivisionList_WE.get(i).getContractor_nm());
//            	obj_3.put("contractor_addr",fallacyDivisionList_WE.get(i).getContractor_addr());
//            	obj_3.put("contractor_num",fallacyDivisionList_WE.get(i).getContractor_num());
//            	obj_3.put("contractor_cls",fallacyDivisionList_WE.get(i).getContractor_cls());
//            	obj_3.put("target_charge",fallacyDivisionList_WE.get(i).getTarget_charge());
//            	obj_3.put("reg_untruth_rate",fallacyDivisionList_WE.get(i).getReg_untruth_rate());
//            	obj_3.put("usr_sum_charge",fallacyDivisionList_WE.get(i).getUsr_sum_charge());
//            	obj_3.put("usr_charge1",fallacyDivisionList_WE.get(i).getUsr_charge1());
//            	obj_3.put("usr_charge2",fallacyDivisionList_WE.get(i).getUsr_charge2());
//            	obj_3.put("usr_charge3",fallacyDivisionList_WE.get(i).getUsr_charge3());
//            	obj_3.put("usr_charge4",fallacyDivisionList_WE.get(i).getUsr_charge4());
//            	obj_3.put("usr_charge5",fallacyDivisionList_WE.get(i).getUsr_charge5());
//            	obj_3.put("usr_charge6",fallacyDivisionList_WE.get(i).getUsr_charge6());
//            	obj_3.put("usr_charge7",fallacyDivisionList_WE.get(i).getUsr_charge7());
//            	obj_3.put("usr_charge8",fallacyDivisionList_WE.get(i).getUsr_charge8());
//            	obj_3.put("usr_charge9",fallacyDivisionList_WE.get(i).getUsr_charge9());
//            	obj_3.put("usr_charge10",fallacyDivisionList_WE.get(i).getUsr_charge10());
//            	obj_3.put("usr_charge11",fallacyDivisionList_WE.get(i).getUsr_charge11	());
//            	obj_3.put("usr_charge12",fallacyDivisionList_WE.get(i).getUsr_charge12());
//            	obj_3.put("contractor_sum_charge",fallacyDivisionList_WE.get(i).getContractor_sum_charge());
//            	obj_3.put("contractor_charge1",fallacyDivisionList_WE.get(i).getContractor_charge1());
//            	obj_3.put("contractor_charge2",fallacyDivisionList_WE.get(i).getContractor_charge2());
//            	obj_3.put("contractor_charge3",fallacyDivisionList_WE.get(i).getContractor_charge3());
//            	obj_3.put("contractor_charge4",fallacyDivisionList_WE.get(i).getContractor_charge4());
//            	obj_3.put("contractor_charge5",fallacyDivisionList_WE.get(i).getContractor_charge5());
//            	obj_3.put("contractor_charge6",fallacyDivisionList_WE.get(i).getContractor_charge6());
//            	obj_3.put("contractor_charge7",fallacyDivisionList_WE.get(i).getContractor_charge7());
//            	obj_3.put("contractor_charge8",fallacyDivisionList_WE.get(i).getContractor_charge8());
//            	obj_3.put("contractor_charge9",fallacyDivisionList_WE.get(i).getContractor_charge9());
//            	obj_3.put("contractor_charge10",fallacyDivisionList_WE.get(i).getContractor_charge10());
//            	obj_3.put("contractor_charge11",fallacyDivisionList_WE.get(i).getContractor_charge11());
//            	obj_3.put("contractor_charge12",fallacyDivisionList_WE.get(i).getContractor_charge12());
//                jsonArr.put(obj_3);
//            }
//
//            json.put("fal_we",jsonArr);
//            jsonArr = new JSONArray();
//
//            //허위의심 수탁계약정보 확인
//            for(int i=0; i<fallacyDivisionList_SU.size(); i++){
//            	JSONObject obj_4 = new JSONObject();
//
//            	obj_4.put("contractor_nm",fallacyDivisionList_SU.get(i).getContractor_nm());
//            	obj_4.put("contractor_addr",fallacyDivisionList_SU.get(i).getContractor_addr());
//            	obj_4.put("contractor_num",fallacyDivisionList_SU.get(i).getContractor_num());
//            	obj_4.put("contractor_cls",fallacyDivisionList_SU.get(i).getContractor_cls());
//            	obj_4.put("target_charge",fallacyDivisionList_SU.get(i).getTarget_charge());
//            	obj_4.put("reg_untruth_rate",fallacyDivisionList_SU.get(i).getReg_untruth_rate());
//            	obj_4.put("usr_sum_charge",fallacyDivisionList_SU.get(i).getUsr_sum_charge());
//            	obj_4.put("usr_charge1",fallacyDivisionList_SU.get(i).getUsr_charge1());
//            	obj_4.put("usr_charge2",fallacyDivisionList_SU.get(i).getUsr_charge2());
//            	obj_4.put("usr_charge3",fallacyDivisionList_SU.get(i).getUsr_charge3());
//            	obj_4.put("usr_charge4",fallacyDivisionList_SU.get(i).getUsr_charge4());
//            	obj_4.put("usr_charge5",fallacyDivisionList_SU.get(i).getUsr_charge5());
//            	obj_4.put("usr_charge6",fallacyDivisionList_SU.get(i).getUsr_charge6());
//            	obj_4.put("usr_charge7",fallacyDivisionList_SU.get(i).getUsr_charge7());
//            	obj_4.put("usr_charge8",fallacyDivisionList_SU.get(i).getUsr_charge8());
//            	obj_4.put("usr_charge9",fallacyDivisionList_SU.get(i).getUsr_charge9());
//            	obj_4.put("usr_charge10",fallacyDivisionList_SU.get(i).getUsr_charge10());
//            	obj_4.put("usr_charge11",fallacyDivisionList_SU.get(i).getUsr_charge11	());
//            	obj_4.put("usr_charge12",fallacyDivisionList_SU.get(i).getUsr_charge12());
//            	obj_4.put("contractor_sum_charge",fallacyDivisionList_SU.get(i).getContractor_sum_charge());
//            	obj_4.put("contractor_charge1",fallacyDivisionList_SU.get(i).getContractor_charge1());
//            	obj_4.put("contractor_charge2",fallacyDivisionList_SU.get(i).getContractor_charge2());
//            	obj_4.put("contractor_charge3",fallacyDivisionList_SU.get(i).getContractor_charge3());
//            	obj_4.put("contractor_charge4",fallacyDivisionList_SU.get(i).getContractor_charge4());
//            	obj_4.put("contractor_charge5",fallacyDivisionList_SU.get(i).getContractor_charge5());
//            	obj_4.put("contractor_charge6",fallacyDivisionList_SU.get(i).getContractor_charge6());
//            	obj_4.put("contractor_charge7",fallacyDivisionList_SU.get(i).getContractor_charge7());
//            	obj_4.put("contractor_charge8",fallacyDivisionList_SU.get(i).getContractor_charge8());
//            	obj_4.put("contractor_charge9",fallacyDivisionList_SU.get(i).getContractor_charge9());
//            	obj_4.put("contractor_charge10",fallacyDivisionList_SU.get(i).getContractor_charge10());
//            	obj_4.put("contractor_charge11",fallacyDivisionList_SU.get(i).getContractor_charge11());
//            	obj_4.put("contractor_charge12",fallacyDivisionList_SU.get(i).getContractor_charge12());
//                jsonArr.put(obj_4);
//            }
//            json.put("fal_su",jsonArr);
//            jsonArr = new JSONArray();
//
//            res.setContentType("application/json");
//            res.setCharacterEncoding("UTF-8");
//            PrintWriter out = res.getWriter();
//            out.write(json.toString());
//            out.close();
//        }


    }

    /*2019.12.02 jws 제도통계 행정처분*/
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTransUniHangjeong.do")
    public void FpisAdminMinCharge(HttpServletRequest req, ModelMap model,FpisAdminStatTrans7VO shVO
    		, HttpServletResponse res) throws SQLException,JSONException,IOException{
    	new ArrayList<Map<String, Object>>();

		List<FpisAdminStatTrans7VO> result = FpisSvc.selectHangjeongResultList(shVO);

    	JSONObject json = new JSONObject();
		//JSONArray json = new JSONArray();

    	if(result.size() > 0 ){
    		json.put("search_reg", result.get(0).getSearch_reg());
    		json.put("status", result.get(0).getStatus());
    		json.put("disposition_result", result.get(0).getDisposition_result());
    		json.put("content_yn", result.get(0).getContent_yn());
    	}

		PrintWriter out = res.getWriter();
        out.write(json.toString());
        out.close();
	}

    /*2019.12.02 jws 제도통계 행정처분 등록/수정 팝업 */
    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTransUniHangjeong_detail.do")
    public String FpisAdminStatTransUniHangjeong_detail(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,Exception {
        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        FpisAdminStatTrans7VO vo = new FpisAdminStatTrans7VO();

        vo.setSearch_comp_bsns_num(shVO.getUsr_mst_key());
        vo.setUsr_mst_key(shVO.getUsr_mst_key());
        vo.setSearch_year(shVO.getSearch_year());
        vo.setBase_year(shVO.getSearch_year());
        vo.setIs_reg("Y");	//미인정 결과만 불러오기
        vo.setE_row(2);

        List<FpisAdminStatTrans4VO> minimumList = FpisSvc.selectUsrInfoMartCarminList_renewal(vo);  //최소 결과분석
        List<FpisAdminStatTrans4VO> directList = FpisSvc.selectUsrInfoMartDirectList_renewal(vo);   //직접 결과분석
        List<FpisAdminStatTrans4VO> trustList = FpisSvc.selectUsrInfoMartDirectList_Na_renewal(vo);   //직접(나항) 결과분석

        vo.setDisposition_type("DIRECT");
        int dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0) FpisSvc.insertDisposition(vo);
        vo.setDisposition_type("MINIMUM");
        dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0) FpisSvc.insertDisposition(vo);
        vo.setDisposition_type("TRUST");
        dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0) FpisSvc.insertDisposition(vo);

        FpisAdminStatTrans7VO disposition =  FpisSvc.selectDispositionDetail(vo); //디테일 정보 가져오기

        //최소 결과등록 정보 가져오기
        if(disposition.getMinimum_result() == null){
        	vo.setDisposition_type("MINIMUM");
        	vo.setDis_seq(disposition.getMinimum_seq());

        	if(disposition.getMinimum_result() != null){
        		FpisAdminStatTrans7VO select_minimum_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
	        	String minimum_cancel_type = select_minimum_cancel.getCancel_type();
	        	String minimum_cancel_period = select_minimum_cancel.getCancel_period();
	        	String minimum_start_date = select_minimum_cancel.getStart_date();
	        	model.addAttribute("minimum_cancel_type"  , minimum_cancel_type);
	        	model.addAttribute("minimum_from_date"  , minimum_start_date);
	        	model.addAttribute("minimum_cancel_period"  , minimum_cancel_period);
        	}
        }
        else if("D".equals(disposition.getMinimum_result()) ){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("MINIMUM");
        	vo.setDis_seq(disposition.getMinimum_seq());

        	if(disposition.getMinimum_result() != null){
        		FpisAdminStatTrans7VO select_minimum_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
	        	String minimum_cancel_type = select_minimum_cancel.getCancel_type();
	        	String minimum_cancel_period = select_minimum_cancel.getCancel_period();
	        	String minimum_start_date = select_minimum_cancel.getStart_date();
	        	model.addAttribute("minimum_cancel_type"  , minimum_cancel_type);
	        	model.addAttribute("minimum_from_date"  , minimum_start_date);
	        	model.addAttribute("minimum_cancel_period"  , minimum_cancel_period);
        	}

        }else if("P".equals(disposition.getMinimum_result())){  // 과징금 시
        	vo.setDisposition_type("MINIMUM");
        	vo.setDis_seq(disposition.getMinimum_seq());

        	FpisAdminStatTrans7VO select_minimum_fee = FpisSvc.selectDispositionFee(vo);
        	String minimum_fee = select_minimum_fee.getFee();
        	model.addAttribute("minimum_fee"  , minimum_fee);
        }
        //차량추출기간추가
    	shVO.setS_date(shVO.getSearch_year()+"0101");
    	int e_date = Integer.parseInt(shVO.getSearch_year());
    	shVO.setE_date((e_date+1)+"0101");

        int minimum_totCnt = FpisSvc.selectCancelCarCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(minimum_totCnt));
    	shVO.setDis_seq(disposition.getMinimum_seq());
    	shVO.setDisposition_type("MINIMUM");
    	shVO.setBase_year(shVO.getSearch_year());
    	List<FpisAdminStatTrans7VO> minimum_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	model.addAttribute("MINIMUM_TOTCNT", minimum_totCnt);
    	model.addAttribute("MinimumCancelCar", minimum_cancelCar);




        //직접 결과등록 정보 가져오기
        if("D".equals(disposition.getDirect_result()) || disposition.getDirect_result() == null){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("DIRECT");
        	vo.setDis_seq(disposition.getDirect_seq());

        	if(disposition.getDirect_result() != null){
        		FpisAdminStatTrans7VO select_direct_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
        		String direct_cancel_type = select_direct_cancel.getCancel_type();
        		String direct_cancel_period = select_direct_cancel.getCancel_period();
        		String direct_start_date = select_direct_cancel.getStart_date();
        		model.addAttribute("direct_cancel_type"  , direct_cancel_type);
        		model.addAttribute("direct_from_date"  , direct_start_date);
        		model.addAttribute("direct_cancel_period"  , direct_cancel_period);
        	}

        }else if("P".equals(disposition.getDirect_result())){  // 과징금 시
        	vo.setDisposition_type("DIRECT");
        	vo.setDis_seq(disposition.getDirect_seq());

        	FpisAdminStatTrans7VO select_direct_fee = FpisSvc.selectDispositionFee(vo);
        	String direct_fee = select_direct_fee.getFee();
        	model.addAttribute("direct_fee"  , direct_fee);
        }
        int direct_totCnt = FpisSvc.selectCancelCarCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(direct_totCnt));
    	shVO.setDis_seq(disposition.getDirect_seq());
    	shVO.setDisposition_type("DIRECT");
    	shVO.setBase_year(shVO.getSearch_year());
    	List<FpisAdminStatTrans7VO> direct_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	model.addAttribute("DIRECT_TOTCNT", direct_totCnt);
    	model.addAttribute("DirectCancelCar", direct_cancelCar);



    	 //직접(나항) 결과등록 정보 가져오기
        if("D".equals(disposition.getDirect_trust_result()) || disposition.getDirect_trust_result() == null){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("TRUST");
        	vo.setDis_seq(disposition.getDirect_trust_seq());

        	if(disposition.getDirect_trust_result() != null){
        		FpisAdminStatTrans7VO select_direct_trust_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
        		String direct_trust_cancel_type = select_direct_trust_cancel.getCancel_type();
        		String direct_trust_cancel_period = select_direct_trust_cancel.getCancel_period();
        		String direct_trust_start_date = select_direct_trust_cancel.getStart_date();
        		model.addAttribute("direct_trust_cancel_type"  , direct_trust_cancel_type);
        		model.addAttribute("direct_trust_from_date"  , direct_trust_start_date);
        		model.addAttribute("direct_trust_cancel_period"  , direct_trust_cancel_period);
        	}

        }else if("P".equals(disposition.getDirect_trust_result())){  // 과징금 시
        	vo.setDisposition_type("TRUST");
        	vo.setDis_seq(disposition.getDirect_trust_seq());

        	FpisAdminStatTrans7VO select_direct_trust_fee = FpisSvc.selectDispositionFee(vo);
        	String direct_trust_fee = select_direct_trust_fee.getFee();
        	model.addAttribute("direct_trust_fee"  , direct_trust_fee);
        }
        int direct_trust_totCnt = FpisSvc.selectCancelCarCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(direct_trust_totCnt));
    	shVO.setDis_seq(disposition.getDirect_trust_seq());
    	shVO.setDisposition_type("TRUST");
    	shVO.setBase_year(shVO.getSearch_year());
    	List<FpisAdminStatTrans7VO> direct_trust_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	model.addAttribute("DIRECT_TRUST_TOTCNT", direct_trust_totCnt);
    	model.addAttribute("DirectTrustCancelCar", direct_trust_cancelCar);


        //행정처분 최대일 계산(최소)
        if(minimumList.size() != 0){
        	if(!minimumList.get(0).getM_no_perform().equals("-")){
        		double m_no_perform = Double.parseDouble(minimumList.get(0).getM_no_perform()) * 0.01;
	            double m_p1 = m_no_perform * 30; //1차 행정처분 최대일
	            double m_p2 = m_no_perform * 60; //2차 행정처분 최대일
	            double m_p3 = m_no_perform * minimum_totCnt; //3차 행정처분 최대대수

	            if(m_p1 <= 0 || (0 < m_p1 && m_p1 <= 1)) m_p1 = 1;
	            //else if(0 < m_p1 && m_p1 <= 1) m_p1 = 1;
	            else m_p1 = Math.floor(m_p1);

	            if(m_p2 <= 0 || (0 < m_p2 && m_p2 <= 1)) m_p2 = 1;
	            //else if(0 < m_p2 && m_p2 <= 1) m_p2 = 1;
	            else m_p2 = Math.floor(m_p2);

	            if(m_p3 <= 0 || (0 < m_p3 && m_p3 <= 1)) m_p3 = 1;
	            //else if(0 < m_p3 && m_p3 <= 1) m_p3 = 1;
	            else m_p3 = Math.floor(m_p3);

	            model.addAttribute("m_p1", (int)m_p1);
	            model.addAttribute("m_p2", (int)m_p2);
	            model.addAttribute("m_p3", (int)m_p3);
	            model.addAttribute("m_no_perform_val", m_no_perform);
        	}
        }

        //행정처분 최대일 계산(직접)
        if(directList.size() != 0){
        	if(!directList.get(0).getD_no_perform().equals("-")){
        		double d_no_perform = Double.parseDouble(directList.get(0).getD_no_perform()) * 0.01;
	            double d_p1 = d_no_perform * 30; //1차 행정처분 최대일
	            double d_p2 = d_no_perform * 60; //2차 행정처분 최대일
	            double d_p3 = d_no_perform * direct_totCnt; //3차 행정처분 최대대수

	            if(d_p1 <= 0 || (0 < d_p1 && d_p1 <= 1)) d_p1 = 1;
	            //else if(0 < d_p1 && d_p1 <= 1) d_p1 = 1;
	            else d_p1 = Math.floor(d_p1);

	            if(d_p2 <= 0 || (0 < d_p2 && d_p2 <= 1)) d_p2 = 1;
	            //else if(0 < d_p2 && d_p2 <= 1) d_p2 = 1;
	            else d_p2 = Math.floor(d_p2);

	            if(d_p3 <= 0 || (0 < d_p3 && d_p3 <= 1)) d_p3 = 1;
	            //else if(0 < d_p3 && d_p3 <= 1) d_p3 = 1;
	            else d_p3 = Math.floor(d_p3);

	            model.addAttribute("d_p1", (int)d_p1);
	            model.addAttribute("d_p2", (int)d_p2);
	            model.addAttribute("d_p3", (int)d_p3);
	            model.addAttribute("d_no_perform_val", d_no_perform);
        	}
        }

        model.addAttribute("base_year",shVO.getBase_year());

        model.addAttribute("minimumList", minimumList);
        model.addAttribute("directList", directList);
        model.addAttribute("trustList", trustList);
        model.addAttribute("disposition", disposition);
        model.addAttribute("list_cur_page", req.getParameter("list_cur_page"));
        model.addAttribute("VO", shVO);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTransUnitPop";
    }


    @RequestMapping("/admin/obeySystem/trans/FpisAdminStatTransUniHangjeongOmi_detail.do")
    public String FpisAdminStatTransUniHangjeongOmi_detail(FpisAdminStatTrans7VO shVO, HttpServletRequest req, ModelMap model) throws SQLException,UnknownHostException,Exception {

        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }

        String searchSidoCd = shVO.getHid_sido_code();
        String searchSigunguCd = shVO.getHid_sigungu_code();

        if(svo.getMber_cls().equals("ADM")){
            model.addAttribute("hid_sido_code" , svo.getAdm_area_code().substring(0, 2));
            if(svo.getAdm_area_code().length() == 2){  // 2014.12.01 mgkim 시도 관리자 검색조건 확인
                searchSidoCd = svo.getAdm_area_code();
                model.addAttribute("hid_sido_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_code" , searchSigunguCd);
            }else{
                model.addAttribute("hid_sigungu_code" , svo.getAdm_area_code());
                model.addAttribute("hid_sigungu_name" , svo.getAdm_area_name());
            }
        }else{
            model.addAttribute("hid_sido_code" , searchSidoCd);
            model.addAttribute("hid_sigungu_code" , searchSigunguCd);
        }

        shVO.setQuarter("60");	//20.02.12 jws 분기 셋팅 shVO:는 미이행률 카운트 vo는 미이행률 리스트 가져오게 되어 있음
        FpisAdminStatTrans7VO vo = new FpisAdminStatTrans7VO();
        vo.setSearch_comp_bsns_num(shVO.getUsr_mst_key());
        vo.setUsr_mst_key(shVO.getUsr_mst_key());
        vo.setSearch_year(shVO.getSearch_year());
        vo.setBase_year(shVO.getBase_year());
        vo.setQuarter(shVO.getQuarter());
        vo.setDisposition_type("OMISSION");


        List<FpisAdminStatTrans7VO> omissionSutakList = FpisSvc.selectOmissionSutakList(vo);
        List<FpisAdminStatTrans7VO> noPerformList = FpisSvc.selectNoPerformList(vo); //2018.08.28 PES 미이행율 추가

        int dispositionCNT = FpisSvc.selectDispositionYN(vo);
        if(dispositionCNT == 0){
        	vo.setDisposition_type("OMISSION");
        	FpisSvc.insertDisposition(vo);
        }

        FpisAdminStatTrans7VO disposition =  FpisSvc.selectDispositionDetail_omission(vo); //디테일 정보 가져오기

        /*2020.11.11 ysw 정보노출 처리 및 이력삽입*/
        String masked_info_status = req.getParameter("masked_info_status");
        if("N".equals(masked_info_status)) {
        	disposition.setTel(disposition.getMasked_tel());
        }else {
        	FpisAccessLogVO accessLogVO = new FpisAccessLogVO();
        	accessLogVO.setRcode(req.getParameter("rcode"));
        	accessLogVO.setBcode(req.getParameter("bcode"));
        	accessLogVO.setComp_mst_key(shVO.getUsr_mst_key());
        	accessLogVO.setMber_usr_mst_key(svo.getUsr_mst_key()); //유저마스터키
        	accessLogVO.setJob_cls("DE"); //상세정보보기
        	accessLogVO.setMber_ip(InetAddress.getLocalHost().getHostAddress());
        	accessLogService.insertAccessLogByUsrMstKey(accessLogVO);
        }
        model.addAttribute("masked_info_status", masked_info_status);



        //누락의심 결과등록 정보 가져오기
        if("D".equals(disposition.getDisposition_result()) || disposition.getDisposition_result() == null){  //허가 취소 등 행정처분 시
        	vo.setDisposition_type("OMISSION");
        	vo.setDis_seq(disposition.getDis_seq());

        	if(disposition.getDisposition_result() != null){
        		FpisAdminStatTrans7VO select_cancel = FpisSvc.selectDispositionCancel(vo);  // 허가취소 등 행정처분 정보 가져오기
	        	String cancel_type = select_cancel.getCancel_type();
	        	String cancel_period = select_cancel.getCancel_period();
	        	String start_date = select_cancel.getStart_date();
	        	model.addAttribute("cancel_type"  , cancel_type);
	        	model.addAttribute("cancel_period"  , cancel_period);
	        	model.addAttribute("from_date"  , start_date);
        	}

        }else if("P".equals(disposition.getDisposition_result())){  // 과징금 시
        	vo.setDisposition_type("OMISSION");
        	vo.setDis_seq(disposition.getDis_seq());

        	FpisAdminStatTrans7VO select_fee = FpisSvc.selectDispositionFee(vo);
        	String omission_fee = select_fee.getFee();
        	model.addAttribute("omission_fee"  , omission_fee);
        }
        //2018.08.29 PES 차량추출기간추가
    	shVO.setS_date(shVO.getBase_year()+"0101");
    	int e_date = Integer.parseInt(shVO.getBase_year());
    	shVO.setE_date((e_date+1)+"0101");

        int cancel_totCnt = FpisSvc.selectCancelCarCount(shVO);
        int noperform_totCnt = FpisSvc.selectNoPerformCount(shVO);
    	shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
    	shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
    	shVO.setTot_page(Util.calcurateTPage(cancel_totCnt));
    	shVO.setDis_seq(disposition.getMinimum_seq());
    	shVO.setDisposition_type("OMISSION");
    	shVO.setDis_seq(disposition.getDis_seq());


    	List<FpisAdminStatTrans7VO> omission_cancelCar = FpisSvc.selectCancelCarList(shVO);
    	//2018.08.29 PES 행정처분 최대일 계산
    	if(noPerformList.size() != 0){
    		double no_perform = Double.parseDouble(noPerformList.get(0).getNo_perform()) * 0.01;
	    	double p1 = no_perform * 10; //1차 행정처분 최대일
	    	double p2 = no_perform * 20; //2차 행정처분 최대일
	    	double p3 = no_perform * 30; //3차 행정처분 최대일

	    	if(p1 <= 0 || (0 < p1 && p1 <= 1)) p1 = 1;
	    	else p1 = Math.floor(p1);

	    	if(p2 <= 0 || (0 < p2 && p2 <= 1)) p2 = 1;
	    	//else if(0 < p2 && p2 <= 1) p2 = 1;
	    	else p2 = Math.floor(p2);

	    	if(p3 <= 0 || (0 < p3 && p3 <= 1)) p3 = 1;
	    	//else if(0 < p3 && p3 <= 1) p3 = 1;
	    	else p3 = Math.floor(p3);

	    	model.addAttribute("p1", (int)p1);
	    	model.addAttribute("p2", (int)p2);
	    	model.addAttribute("p3", (int)p3);
	    	model.addAttribute("no_perform", no_perform);
    	}
    	model.addAttribute("base_year",shVO.getBase_year());
    	model.addAttribute("OMISSION_TOTCNT", cancel_totCnt);
    	model.addAttribute("OmissionCancelCar", omission_cancelCar);
    	model.addAttribute("noperform_totCnt",noperform_totCnt);
    	model.addAttribute("noPerformList", noPerformList);
    	model.addAttribute("omissionSutakList", omissionSutakList);
        model.addAttribute("disposition", disposition);
        model.addAttribute("list_cur_page", req.getParameter("list_cur_page"));
        model.addAttribute("VO", shVO);

        return "/fpis/admin/obeySystem/trans/FpisAdminStatTransUnitOmiPop";
    }
    
    @RequestMapping(value = "/admin/obeySystem/trans/UpdateProgressNext.do")
    public void updateProgressNext(HttpServletRequest req, HttpServletResponse res, @RequestParam(value="progress") int progress)
    		throws SQLException,JSONException,IOException{
    	
    	if(progress != 100) {
    		FpisSvc.updateProgressNext(progress); //100%면 업데이트 안함
    	}
    	
    	progress = FpisSvc.getProgress();
    	
    	JSONObject json = new JSONObject();
    	
    	json.put("progress", progress);
    	
    	PrintWriter out = res.getWriter();
        out.write(json.toString());
        out.close();
    }
    
    @RequestMapping(value = "/admin/obeySystem/trans/UpdateProgressPre.do")
    public void updateProgressPre(HttpServletRequest req, HttpServletResponse res, @RequestParam(value="progress") int progress)
    		throws SQLException,JSONException,IOException{
    	
    	if(progress!=0) { 
    			FpisSvc.updateProgressPre(progress); //0%면 업데이트 안함
    	}
    	
    	if(progress > 5) {
    		progress = FpisSvc.getProgress();
    	}else {
    		progress = 0; //5% 혹은 0% 에서 이전 버튼 눌렀을 시 0으로 표시
    	}
    	
    	JSONObject json = new JSONObject();
    	
    	json.put("progress", progress);
    	
    	PrintWriter out = res.getWriter();
        out.write(json.toString());
        out.close();
    }
    
    @RequestMapping(value = "/admin/obeySystem/trans/FpisProgressStatusList.do")
    public String FpisProgressStatusList(HttpServletRequest req, HttpServletResponse res, ModelMap model) {
    	
    	List<FpisProgressStatusVO> progressList = FpisSvc.selectProgressStatusList();
    	model.addAttribute("PROGRESSLIST", progressList);
    	
    	return "/fpis/admin/obeySystem/trans/FpisProgressStatusList";
    }
    
    
}
