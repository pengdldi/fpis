package fpis.online.stdinfo.client.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import fpis.common.service.CommonService;
import fpis.common.utils.FpisConstants;
import fpis.common.utils.Util;
import fpis.common.vo.SessionVO;
import fpis.common.vo.sys.SysCodeVO;
import fpis.online.order.readonly.service.FpisConsignmentReadOnlyService;
import fpis.online.order.service.FpisOrderContractVO;
import fpis.online.stdinfo.client.service.FpisClientManageService;
import fpis.online.stdinfo.client.service.FpisSysCompanyVO;
import fpis.online.stdinfo.client.service.FpisUsrCompanyVO;
import net.sf.json.JSONObject;

@Controller
public class FpisClientManageController {

	private static final Logger logger = Logger.getLogger(FpisClientManageController.class);

	@Value(value="#{fpis['FPIS.domain']}")
    private String program_domain;

	@Value(value="#{globals['Globals.ServerState']}")
    private String serverState;

	@Value(value="#{globals['Globals.fpisFilePath']}")
    private String fpisFilePath;

	@Value(value="#{fpis['FPIS.upload_path_usrComp']}")
    private String upload_path_usrComp;

    // Service Area
    @Resource(name = "CommonService")
    private CommonService commonService;

    // Service Area
    @Resource(name = "FpisClientManageService")
    private FpisClientManageService FpisSvc;

    @Resource(name = "FpisConsignmentReadOnlyService")
	private FpisConsignmentReadOnlyService FpisCsmSvc;
    /**
     * 온라인 실적관리->기준정보관리 ->거래처관리
     *
     * @param boardMasterVO
     * @param model
     * @return
     * @throws Exception
     */


    /*
     * 2013.09.13 mgkim 거래처관리 현황 메뉴명 변경됨.
     * 2013.09.13 mgkim 검색 오류 수정
     * 2015.02.10 mgkim 시스템간소화 - 필수항목외 모두 삭제처리
     * 2015.02.10 mgkim 리스트 등록일 역순 정렬
     * 2015.02.13 mgkim 검색기능 보완 - 구분, 등록일 검색
     */
    @RequestMapping("/online/FpisClientManagerList.do")
    public String FpisClientManagerList(FpisUsrCompanyVO shVO,HttpServletRequest req, ModelMap model) throws Exception {
    	/*2021.01.15 ysw 본문 매개변수가 조회에서 허용됨 - 파라메터 조작 방지를 위한 필터링을 걸어놓는다. (특수문자)*/
    	String rcode = req.getParameter("rcode");
    	if(!(rcode == null || rcode.equals(""))) {
    		req.setAttribute("rcode", Xsite_secure(rcode));
    	}
    	String bcode = req.getParameter("bcode");
    	if(!(bcode == null || bcode.equals(""))) {
    		req.setAttribute("bcode", Xsite_secure(bcode));
    	}


    	/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

        SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

        /* 2015.03.10 mgkim 거래처 삭제 검색조건 유지 시작 */
        String state = req.getParameter("state");
        if(state != null && state.equals("del_mode")){
            String del_search_name = "";
            //String serverState = EgovProperties.getProperty("Globals.ServerState");
            if(serverState != null && ( serverState.equals("fpis-owas") || serverState.equals("fpis-iwas")) ){  // 운영서버 was
                del_search_name = new String (req.getParameter("del_search_name").getBytes("KSC5601"),"EUC-KR");    // 2013.12.27 mgkim 리다이렉트로 파라메타 전달시 한글 깨짐.
            }else{  // 개발서버
                del_search_name = new String (req.getParameter("del_search_name").getBytes("8859_1"),"UTF-8");
            }
            shVO.setSearch_name(del_search_name);
            shVO.setSearch_comp_bsns_num(req.getParameter("del_search_comp_bsns_num"));
            shVO.setSearch_cls(req.getParameter("del_search_cls"));
            shVO.setReg_date(req.getParameter("del_reg_date"));
        }
        /* 2015.03.10 mgkim 거래처 삭제 검색조건 유지 종료 */


        List<FpisUsrCompanyVO> compList = null;
        int modelCnt = 0;
        int totCnt   = 0;

        //search_comp_bsns_num
        String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
        if(org_comp_bsns_num != null){
            shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
        }

        try {
            // PAGING START ------------------
            if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }
            shVO.setUsr_mst_key(svo.getUsr_mst_key());
            totCnt = FpisSvc.selectUsrCompanyCount(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ------------------

            compList = FpisSvc.selectUsrCompanyList(shVO);

        } catch (SQLException e) {
            logger.error("[ERROR] - SQLException : ", e);
        }

        shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

        modelCnt  = compList.size();
        model.addAttribute("VO"       , shVO);  // PAGING VO
        model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO
        model.addAttribute("modelCnt" , modelCnt);
        model.addAttribute("compList" , compList);

        List<SysCodeVO> codeFMS001   = commonService.commonCode("FMS001", null);
        model.addAttribute("codeFMS001", codeFMS001);

        return "/fpis/online/stdinfo/client/FpisClientManagerList";
    }


    @RequestMapping("/online/FpisClientManagerList_exportExcel.do")
    public String FpisClientManagerList_exportExcel(FpisUsrCompanyVO shVO,HttpServletRequest req, ModelMap model) throws Exception {
    	SessionVO svo = (SessionVO)req.getSession().getAttribute("SessionVO");

    	List<FpisUsrCompanyVO> compList = null;
    	int totCnt   = 0;

    	//search_comp_bsns_num
    	String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
    	if(org_comp_bsns_num != null){
    		shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
    	}

    	try {
    		// PAGING START ------------------
            // 엑셀다운로드 어느 페이지에서 사용하던지 전체 목록 다운로드로 수정 - 2021.12.10 suhyun
            //if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }
    		shVO.setCur_page(1);
    		shVO.setUsr_mst_key(svo.getUsr_mst_key());
    		totCnt = FpisSvc.selectUsrCompanyCount(shVO);
    		shVO.setS_row(0);
    		shVO.setE_row(totCnt+1);
    		shVO.setTot_page(Util.calcurateTPage(totCnt));
    		// PAGING END ------------------

    		compList = FpisSvc.selectUsrCompanyList(shVO);

    	} catch (SQLException e) {
    		logger.error("[ERROR] - SQLException : ", e);
    	}

    	shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환

    	model.addAttribute("VO"       , shVO);  // PAGING VO
    	model.addAttribute("compList" , compList);
    	model.addAttribute("TOTCNT"   , totCnt);  // PAGING VO

		//tiles 이슈로 6depth경로로 이동. - 2021.11.03 suhyun
    	return "/fpis/online/stdinfo/client/excelJsp/fileList/FpisClientManagerList_exportExcel";
    }

    /* 2018.10.01 pes 거래처 위수탁확인 */
    @ResponseBody
    @RequestMapping("/online/FpisClientManagerWesutak.do")
    public String FpisClientManagerWesutak(FpisOrderContractVO vo,HttpServletRequest req, ModelMap model) throws Exception {
		req.getSession().getAttribute(FpisConstants.SESSION_KEY);

		String usr_mst_key = ((SessionVO)req.getSession().getAttribute("SessionVO")).getUsr_mst_key();
		vo.setUsr_mst_key(usr_mst_key);
		vo.setClient_num(req.getParameter("client_num"));
		vo.setSearch_date(req.getParameter("search_date"));
		vo.setSearch_kind(req.getParameter("search_kind"));
		String BCODE = req.getParameter("bcode");
		String RCODE = req.getParameter("rcode");
		model.addAttribute("BCODE", BCODE);
		model.addAttribute("RCODE", RCODE);

		JSONObject json = new JSONObject();
		if(vo.getSearch_kind() != null){
			List<FpisOrderContractVO> list = null;
			if("WE".equals(vo.getSearch_kind())){  //위탁거래정보확인
				list = FpisCsmSvc.selectClientWeList(vo);
			}else if("SU".equals(vo.getSearch_kind())){  //수탁 거래정보 확인
				list = FpisCsmSvc.selectClientSuList(vo);
			}

			model.addAttribute("LISTS", list);
			json.put("list",list);
			return json.toString();
		}
		model.addAttribute("VO", vo);

		return null;
    }

    /*
     * 2013.09.17 mgkim 거래처관리 현황 - 거래처관리 삭제기능 처리 프로세스
     * 2015.02.10 mgkim DEL_YN 처리에서 DELETE 로 변경함
     */
    @RequestMapping(value="/online/FpisClientManagerDelete.do", method = { RequestMethod.POST, RequestMethod.GET })
    public String FpisClientManagerDelete(@ModelAttribute("frmThis") FpisUsrCompanyVO frmVO,
                                          HttpServletRequest req, ModelMap model) throws Exception {
        List<FpisSysCompanyVO>    compList = new ArrayList<FpisSysCompanyVO>();
        FpisSysCompanyVO           attVO   = null;
        SessionVO ssVO = (SessionVO)req.getSession().getAttribute("SessionVO");

        if(frmVO.getCompchk() != null && !frmVO.getCompchk().equals(""))
        {
            String [] tokArry;
            tokArry = frmVO.getCompchk().split(",");
            for(int i = 0 ; i < tokArry.length ; i++)
            {
                if(tokArry[i] != null && !tokArry[i].equals(""))
                {
                    attVO = new FpisSysCompanyVO(req);
                    attVO.setUsr_mst_key(ssVO.getUsr_mst_key());
                    attVO.setSys_comp_mst_key(tokArry[i]);
                    attVO.setUser_id(ssVO.getUser_id());
                    compList.add(attVO);
                } // end of if()
            } // end of for()
        }// end of if ()

        FpisSvc.deleteMemberComp(compList);
        model.addAttribute("state", "del_mode");
        model.addAttribute("del_search_name", frmVO.getSearch_name());                    // 2015.03.10 mgkim 삭제후 검색조건 유지
        model.addAttribute("del_search_comp_bsns_num", frmVO.getSearch_comp_bsns_num());  // 2015.03.10 mgkim 삭제후 검색조건 유지
        model.addAttribute("del_search_cls", frmVO.getSearch_cls());                      // 2015.03.10 mgkim 삭제후 검색조건 유지
        model.addAttribute("del_reg_date", frmVO.getReg_date());                          // 2015.03.10 mgkim 삭제후 검색조건 유지

        return "redirect:/online/FpisClientManagerList.do";
    }

    /*
     * 2015.02.16 mgkim 거래처 검색삭제 기능
     */
    @RequestMapping("/online/FpisClientManagerDeleteAll.do")
    public String FpisClientManagerDeleteAll(FpisUsrCompanyVO shVO, HttpServletRequest req, ModelMap model) throws Exception {
        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

        shVO.setUsr_mst_key(sVO.getUsr_mst_key());
        FpisSvc.deleteUsrCompanyAll(shVO);

        return "redirect:/online/FpisClientManagerList.do";
    }





    /**
     * @Method Name : FpisClientManagerRegist
     * @자성일   : 2012. 10. 3.
     * @작성자   : limtg
     * @변경이력 :
     * 2013.09.13 mgkim 거래처관리 - 선택등록 메뉴명 변경
     * 2015.02.13 mgkim 거래처 선택등록 사업자번호 검색 2건 오류 수정
     * 2015.02.13 mgkim 시스템간소화 - 필수항목외 모두 삭제처리
     */
    @RequestMapping("/online/FpisClientManagerRegist.do")
    public String FpisClientManagerRegist(FpisSysCompanyVO shVO,HttpServletRequest req, ModelMap model) throws Exception {

    	/*2021.01.15 ysw 본문 매개변수가 조회에서 허용됨 - 파라메터 조작 방지를 위한 필터링을 걸어놓는다. (특수문자)*/
    	String rcode = req.getParameter("rcode");
    	if(!(rcode == null || rcode.equals(""))) {
    		req.setAttribute("rcode", Xsite_secure(rcode));
    	}
    	String bcode = req.getParameter("bcode");
    	if(!(bcode == null || bcode.equals(""))) {
    		req.setAttribute("bcode", Xsite_secure(bcode));
    	}

    	/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

    	SessionVO             svo = (SessionVO)req.getSession().getAttribute("SessionVO");
        FpisSysCompanyVO       vo = new FpisSysCompanyVO(req);
        List<FpisSysCompanyVO> compList = null;
        int modelCnt = 0;
        int totCnt   = 0;

        // PAGING START ------------------
        if (shVO.getCur_page() <= 0) {  shVO.setCur_page(1); }


        List<String> testList = new ArrayList<String>();
        List<String> testList2 = new ArrayList<String>();
        // 한글 깨짐 인코딩 확인하기.
        if (req.getParameter("in_search_name") != null && req.getParameter("in_search_name").toString() != ""){
            String s = req.getParameter("in_search_name").toString();
            String charset[] = {"KSC5601","8859_1", "ascii", "UTF-8", "EUC-KR", "MS949"};

            for(int i=0; i<charset.length ; i++){
                for(int j=0 ; j<charset.length ; j++){
                    if(i==j){ continue;}
                    else{
                    	testList.add(charset[i]+" : "+charset[j]);
                    	testList2.add(new String (s.getBytes(charset[i]),charset[j]));
                    }
                }
            }
        }

        model.addAttribute("testList", testList);
        model.addAttribute("testList2", testList2);

        String in_search_name = "";
        if(req.getParameter("in_search_name") != null){
            //in_search_name = new String (req.getParameter("in_search_name").getBytes("8859_1"),"UTF-8");      // 2013.09.05 mgkim 리다이렉트로 파라메타 전달시 한글 깨짐.
            //in_search_name = new String (req.getParameter("in_search_name").getBytes("KSC5601"),"EUC-KR");    // 2013.12.27 mgkim 리다이렉트로 파라메타 전달시 한글 깨짐.

            // 2014.12.01 mgkim 내/외부망 관리자 분리 globals 설정 시작
            // 2015.02.13 mgkim 운영서버/개발서버 인코딩 구분 활용.
            //String serverState = EgovProperties.getProperty("Globals.ServerState");
        	// 인코딩 문제 해결! - 2021.11.03 suhyun
            /*if(serverState != null && ( serverState.equals("fpis-owas") || serverState.equals("fpis-iwas")) ){  // 운영서버 was
                in_search_name = new String (req.getParameter("in_search_name").getBytes("KSC5601"),"EUC-KR");    // 2013.12.27 mgkim 리다이렉트로 파라메타 전달시 한글 깨짐.
            }else{  // 개발서버
                in_search_name = new String (req.getParameter("in_search_name").getBytes("8859_1"),"UTF-8");
            }*/
        	in_search_name = req.getParameter("in_search_name");
        }

        if( (shVO.getSearch_name() == null || shVO.getSearch_name().equals(""))
                && (shVO.getSearch_comp_bsns_num() == null || shVO.getSearch_comp_bsns_num().equals("")) ){
            shVO.setSearch_name(in_search_name);
            shVO.setSearch_comp_bsns_num(req.getParameter("in_search_comp_bsns_num"));
        }

        if((shVO.getSearch_name() != null && !shVO.getSearch_name().equals(""))
        || (shVO.getSearch_comp_bsns_num() != null && !shVO.getSearch_comp_bsns_num().equals("")) )
        {
        	//search_comp_bsns_num
            String org_comp_bsns_num = shVO.getSearch_comp_bsns_num();
            if(org_comp_bsns_num != null){
                shVO.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num().replaceAll("-", ""));    // 2013.10.18 사업자번호 검색 "-" 기호 제거
            }

            shVO.setUsr_mst_key(svo.getUsr_mst_key());
            shVO.setUsr_comp_mst_key(svo.getComp_mst_key());
            totCnt = FpisSvc.selectSysCompanyCnt(shVO);
            shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
            shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
            shVO.setTot_page(Util.calcurateTPage(totCnt));
            // PAGING END ---------------
            vo.setUsr_mst_key(svo.getUsr_mst_key());
            vo.setSearch_name(shVO.getSearch_name());
            vo.setSearch_comp_bsns_num(shVO.getSearch_comp_bsns_num());    // 2013.09.16 mgkim 사업자번호 검색기능 추가
            vo.setUsr_comp_mst_key(svo.getComp_mst_key());

            vo.setS_row(shVO.getS_row());
            vo.setE_row(shVO.getE_row());

            compList = FpisSvc.selectSysCompanyList(vo);
            modelCnt  = (compList == null) ? 0 : compList.size();
            shVO.setSearch_comp_bsns_num(org_comp_bsns_num);    // 2013.10.18 사업자번호 검색 "-" 기호 제거 사용자가 입력한 값 그대로 반환
        }
        String session_ubn = svo.getUsr_bsns_num().substring(0, 3)
                           + "-" + svo.getUsr_bsns_num().substring(3, 5)
                           + "-" + svo.getUsr_bsns_num().substring(5, 10);
        model.addAttribute("VO"       , shVO);
        model.addAttribute("TOTCNT"   , totCnt);
        model.addAttribute("modelCnt" , modelCnt);
        model.addAttribute("compList" , compList);
        model.addAttribute("session_ubn", session_ubn);  // 2013.10.18 mgkim 세션 사업자번호 값 사용
        return "/fpis/online/stdinfo/client/FpisClientManagerRegist";
    }

    /*
     * 2013.09.17 mgkim 거래처관리 - 작성등록 메뉴 신규생성
     * 2015.02.10 mgkim 시스템간소화 - 필수항목외 모두 삭제처리
     */
    @RequestMapping("/online/FpisClientManagerRegist2.do")
    public String FpisClientManagerRegist2(FpisSysCompanyVO shVO,HttpServletRequest req, ModelMap model) throws Exception {
        List<SysCodeVO> codeFMS001   = commonService.commonCode("FMS001", null);
        model.addAttribute("codeFMS001", codeFMS001);
        return "/fpis/online/stdinfo/client/FpisClientManagerRegist2";
    }

    /*
     * 2013.09.17 mgkim 거래처관리 - 파일등록 메뉴 신규생성
     * 2015.02.10 mgkim 시스템간소화 - 필수항목외 모두 삭제처리
     */
    @RequestMapping("/online/FpisClientManagerRegist3.do")
    public String FpisClientManagerRegist3(FpisSysCompanyVO shVO,HttpServletRequest req, ModelMap model) throws Exception {
        //String fpisFilePath = EgovProperties.getProperty("Globals.fpisFilePath");
        model.addAttribute("fpisFilePath",fpisFilePath); // 2013.09.12 mgkim FPIS 시스템의 파일다운시 사용할 절대경로값 설정

        model.addAttribute("mode", req.getParameter("mode"));
		model.addAttribute("f_name", req.getParameter("f_name"));
        return "/fpis/online/stdinfo/client/FpisClientManagerRegist3";
    }



    /**
     * @Method Name : FpisClientManagerInsert
     * @자성일   : 2012. 10. 3.
     * @작성자   : limtg
     * @변경이력 :
     * 2013.09.17 mgkim 거래처관리 - 선택등록 메뉴 선택 저장 처리 프로세스
     * 2015.02.10 mgkim 시스템간소화 - 필수항목외 모두 삭제처리
     */
    @RequestMapping("/online/FpisClientManagerInsert.do")
    public String FpisClientManagerInsert(FpisSysCompanyVO shVO ,HttpServletRequest req, ModelMap model) throws Exception {
        List<FpisSysCompanyVO>     compList = new ArrayList<FpisSysCompanyVO>();
        FpisSysCompanyVO          attVO   = null;
        SessionVO ssVO = (SessionVO)req.getSession().getAttribute("SessionVO");

        if(shVO.getCompchk() != null && !shVO.getCompchk().equals(""))
        {
            String [] tokArry;
            tokArry = shVO.getCompchk().split(",");
            for(int i = 0 ; i < tokArry.length ; i++)
            {
                if(tokArry[i] != null && !tokArry[i].equals(""))
                {
                    attVO = new FpisSysCompanyVO(req);
                    attVO.setUsr_mst_key(ssVO.getUsr_mst_key());
                    attVO.setUser_id(ssVO.getUser_id());
                    /*attVO.setSys_comp_mst_key(tokArry[i]);*/
                    String comp_bsns_num = tokArry[i].replaceAll("-", "");
                    attVO.setComp_bsns_num(comp_bsns_num);
                    attVO.setUsr_comp_mst_key(ssVO.getUsr_mst_key());
                    compList.add(attVO);
                } // end of if()
            } // end of for()
        }// end of if ()

        FpisSvc.insertMemberComp(compList);

        model.addAttribute("in_search_name"         , shVO.getSearch_name());                    // 2013.09.17 mgkim 검색 파라메터 유지
        model.addAttribute("in_search_comp_bsns_num"         , shVO.getSearch_comp_bsns_num());  // 2013.09.17 mgkim 검색 파라메터 유지

        return "redirect:/online/FpisClientManagerRegist.do";
    }


    /*
     * 2013.09.25 mgkim 거래처관리 - 작성등록 처리 프로세스  (신규)
     * 2015.02.10 mgkim 사업단회의 반영 - 선택사항 제거 작업
     */
    @RequestMapping("/online/FpisClientManagerInsert2.do")
    public String FpisClientManagerInsert2(@ModelAttribute("frmThis")FpisUsrCompanyVO usrCompVO ,
                                           BindingResult bindingResult ,
                                           HttpServletRequest req,
                                           ModelMap model
                                           ) throws Exception {

        SessionVO                   ssVO = (SessionVO)req.getSession().getAttribute("SessionVO"); // session VO

        usrCompVO.setComp_bsns_num(usrCompVO.getComp_bsns_num().replaceAll("-", ""));
        usrCompVO.setUsr_mst_key(ssVO.getUsr_mst_key());
        usrCompVO.setUser(ssVO.getUser_id());
        usrCompVO.setUsr_comp_mst_key(ssVO.getComp_mst_key());

        /*if(usrCompVO.getComp_bsns_num() == null || usrCompVO.getComp_bsns_num().equals("")){
        	usrCompVO.setComp_bsns_num(usrCompVO.getTel().replaceAll("-", "")); // 사업자번호가 없는 개인화주는 전화번호를 넣는다.
        }*/
        
        /* 2023.02.23 jwchoi 웹취약점 조치 - 사업자등록번호 검증 서버단 처리 */
        String res = "NULL";
        if ("ERR".equals(FpisSvc.chkCompBsnsNum(usrCompVO.getComp_bsns_num()))) {
        	res = "ERR";
        } else {
        	if(FpisSvc.isExistUsrCompanyInfo(usrCompVO) > 0) {
        		res = "EX";
        	}else{
        		int resRtn = -1;
        		resRtn    = FpisSvc.insertUsrCompanyInfo(usrCompVO);
        		if(resRtn < 0) {
        			res = "FAL";
        		} else {
        			res = "SUC";
        		}
        	}
        }

        model.addAttribute("RES"       , res);
        model.addAttribute("VO"       , usrCompVO);

        return "/fpis/online/stdinfo/client/FpisClientManagerRegist2";
    }

    /*
     * 2013.09.27 mgkim 거래처관리 -파일등록 (신규)
     * 2015.02.10 mgkim 사업단회의 반영 - 선택사항 제거 작업
     */
    @RequestMapping("/online/FpisClientManagerInsert3.do")
    public String FpisClientManagerInsert3(@RequestParam(value="source", required=false)String source,
                                final MultipartHttpServletRequest request, Model model) throws Exception {
        SessionVO ssVO = (SessionVO)request.getSession().getAttribute("SessionVO");

        final MultipartHttpServletRequest multiRequest = request;
        final Map<String, MultipartFile> files = multiRequest.getFileMap();
        InputStream fis = null;

        Iterator<Entry<String, MultipartFile>> itr = files.entrySet().iterator();
        MultipartFile file;
        String f_name = null;
        while(itr.hasNext()) {
            Entry<String, MultipartFile> entry = itr.next();

            file = entry.getValue();

            // 거래처관리 파일등록 기능구현
            if(!"".equals(file.getOriginalFilename()))
            {// 확장자 체크는 jsp 에서 처리
                try
                {
                    fis = file.getInputStream();
                    f_name = saveUsrCompFile(Util.getDateFormat4()+"_"+ssVO.getUsr_mst_key()+"_client.csv", fis);

                }catch(IOException e) {
                	logger.error("[ERROR] - IOException : ", e);
                    throw e;
                }finally {
                    if(fis != null) fis.close();
                }
            }
        }

        return "redirect:/online/FpisClientManagerRegist3.do?mode=progress&f_name="+f_name;
    }


    /* 2013.09.27 mgkim 거래처관리 파일등록 - 파일저장 프로세스 */
    private String saveUsrCompFile(String fname, InputStream fis) {
      //String r_path = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.upload_path_usrComp");

      // 일자별 저장...
      File _sFile = new File(upload_path_usrComp + File.separator + Util.getDateFormat() + File.separator + fname);
      if(!_sFile.getParentFile().exists()) {_sFile.getParentFile().mkdirs();}
      FileOutputStream fw = null;
      try {
          byte[] buf = new byte[1024];
          int cnt = 0;
          if(!_sFile.getParentFile().exists()) {_sFile.getParentFile().mkdirs();}
          fw = new FileOutputStream(_sFile);
          while((cnt = fis.read(buf)) > 0) {
              fw.write(buf, 0, cnt);
              fw.flush();
          }
          return _sFile.getName();
      }catch(IOException e) {
    	  logger.error("[ERROR] - IOException : ", e);
          return null;
      }finally {
          _sFile = null;
          if(fw != null)
              try {
            	  fw.close();
              }catch(IOException ex) {
            	  logger.error("[ERROR] - IOException : ", ex);
              }
          fw = null;
      }
    }

    //2019.06.19 pch : 보안취약점(크로스스크립트)
  	public String Xsite_secure(String param){
  		String cont = param;
  		String cont_low = cont.toLowerCase();

  		if(cont_low.contains("javascript") || cont_low.contains("script") ||cont_low.contains("iframe") || cont_low.contains("document") ||
  	            cont_low.contains("vbscript") || cont_low.contains("applet") ||cont_low.contains("embed") || cont_low.contains("object") ||
  	            cont_low.contains("frame") || cont_low.contains("grameset") ||cont_low.contains("layer") || cont_low.contains("bgsound") ||
  	            cont_low.contains("alert") || cont_low.contains("onblur") ||cont_low.contains("onchange") || cont_low.contains("onclick") ||
  	            cont_low.contains("ondblclick") || cont_low.contains("enerror") ||cont_low.contains("onfocus") || cont_low.contains("onload") ||
  	            cont_low.contains("onmouse") || cont_low.contains("onscroll") ||cont_low.contains("onsubmit") || cont_low.contains("onunload")||
  	  	        cont_low.contains("onerror") || cont_low.contains("confirm")) {
			cont = cont_low;
			cont = cont.replaceAll("&","&amp");
			cont = cont.replaceAll("<","&lt");
			cont = cont.replaceAll(">","&gt");

			//2021.01.11 xss 필터 내용 보완
			cont = cont.replaceAll("\"","&quot");
			cont = cont.replaceAll("'","&#039");
			cont = cont.replaceAll("\\\\","&apos");

		    cont = cont.replaceAll("javascript", "x-javascript");
		    cont = cont.replaceAll("img", "x-img");
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
			cont = cont.replaceAll("ondblclick","x-ondblclick");
			cont = cont.replaceAll("enerror", "x-enerror");
			cont = cont.replaceAll("onfocus", "x-onfocus");
			cont = cont.replaceAll("onload", "x-onload");
			cont = cont.replaceAll("onmouse", "x-onmouse");
			cont = cont.replaceAll("onscroll", "x-onscroll");
			cont = cont.replaceAll("onsubmit", "x-onsubmit");
			cont = cont.replaceAll("onunload", "x-onunload");
			cont = cont.replaceAll("onerror", "x-onerror");
			cont = cont.replaceAll("confirm", "x-confirm");
			param = cont;
  		}
  		return param;
  	}

}