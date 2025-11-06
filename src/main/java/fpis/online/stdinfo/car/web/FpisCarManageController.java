package fpis.online.stdinfo.car.web;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import com.opencsv.CSVWriter;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import fpis.common.service.CommonService;
import fpis.common.utils.FpisConstants;
import fpis.common.utils.Util;
import fpis.common.utils.Util_poi;
import fpis.common.vo.SessionVO;
import fpis.common.vo.sys.SysCodeVO;
import fpis.common.vo.usr.UsrInfoVO;
import fpis.online.stdinfo.car.service.FpisCarHistoryVO;
import fpis.online.stdinfo.car.service.FpisCarManageService;
import fpis.online.stdinfo.car.service.FpisCarManageVO;
import fpis.online.stdinfo.car.service.FpisUsrCarsFileInfoManageVO;
import fpis.online.stdinfo.client.service.FpisClientManageService;
import fpis.reg.RegVO;
import twitter4j.internal.org.json.JSONObject;


@SuppressWarnings({"rawtypes"})
@Controller
public class FpisCarManageController {

	private static final Logger logger = Logger.getLogger(FpisCarManageController.class);

	// Service Area
	@Resource(name = "CommonService")
	private CommonService commonService;

	@Resource(name = "FpisCarManageService")
	private FpisCarManageService CarManageService;

	// Service Area
	@Resource(name = "FpisClientManageService")
	private FpisClientManageService FpisCompSvc;

    // 2015. 12. 28 added by dyahn 업체정보 가져오기 Service
    @Resource(name = "FpisClientManageService")
    private FpisClientManageService FpisSvc;

    @Value(value="#{fpis['FPIS.upload_path_usrCar']}")
	private String upload_path_usrCar;

    @Value(value="#{fpis['FPIS.upload_path_usrCarZip']}")
	private String upload_path_usrCarZip;

    @Value(value="#{fpis['FPIS.domain']}")
	private String program_domain;

    @Value(value="#{globals['Globals.ServerState']}")
	private String serverState;

    @Value(value="#{globals['Globals.fpisFilePath']}")
	private String fpisFilePath;

	//private String upload_path_usrCar = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.upload_path_usrCar");       // 차량 파일등록 첨부파일 저장경로
	//private String upload_path_usrCarZip = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.upload_path_usrCarZip"); // 차량계약 근거자료 첨부파일 저장경로
	private String file_db_path = "/"; // 실제 파일등록 경로는 File.separator; DB에 경로저장시 운영:File.separator[확인필요] , 개발:/[확인완료]
	/*
	 * 기초정보 관리 -> 차량정보관리
	 *  - FpisCarManagerList : 차량정보 조회
	 *  2013.09.11 mgkim 차량관리 메뉴 수정
	 *  2016.05.10 dyahn 차량관리현황 UI 수정(상세정보화면 추가, 이용여부 검색조건 추가)
	 */
	@RequestMapping("/online/FpisCarManagerList.do")
	public String FpisCarManagerList(@ModelAttribute("frmThis") FpisCarManageVO shVO , BindingResult bindingResult ,
			HttpServletRequest req, ModelMap model) throws Exception {

		/*2021.01.15 ysw 본문 매개변수가 조회에서 허용됨 - 파라메터 조작 방지를 위한 필터링을 걸어놓는다. (특수문자)*/
    	String rcode = req.getParameter("rcode");
    	if(!(rcode == null || rcode.equals(""))) {
    		req.setAttribute("rcode", Xsite_secure(rcode));
    	}
    	String bcode = req.getParameter("bcode");
    	if(!(bcode == null || bcode.equals(""))) {
    		req.setAttribute("bcode", Xsite_secure(bcode));
    	}
    	String mUrl = req.getParameter("mUrl");
    	if(!(mUrl == null || mUrl.equals(""))) {
    		req.setAttribute("mUrl", Xsite_secure(mUrl));
    	}

		/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");
		//String program_domain = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.FpisConfPath"), "FPIS.domain");
		if(!refer_domain.contains(program_domain)) {
			return "redirect:/";
		}

		String contextPath = req.getContextPath();
		List<FpisCarManageVO> carVOS = null;
		SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

		/* 2015.03.13 swyang 차량 삭제 검색조건 유지 시작 */
		String state = req.getParameter("state");
		if(state != null && state.equals("del_mode")){
			String del_search_car_num = "";
			//String serverState = EgovProperties.getProperty("Globals.ServerState");
			if(serverState != null && ( serverState.equals("fpis-owas") || serverState.equals("fpis-iwas")) ){  // 운영서버 was
				del_search_car_num = new String (req.getParameter("del_search_car_num").getBytes("KSC5601"),"EUC-KR");    // 2013.12.27 mgkim 리다이렉트로 파라메타 전달시 한글 깨짐.
			}else{  // 개발서버
				del_search_car_num = new String (req.getParameter("del_search_car_num").getBytes("8859_1"),"UTF-8");
			}

			shVO.setSearch_car_num(del_search_car_num);
			shVO.setSearch_sort1(req.getParameter("del_search_sort1"));
			// sort2 오탈자 수정..... - 2021.12.18 suhyun
			shVO.setSearch_sort2(req.getParameter("del_search_sort2"));
			shVO.setSearch_car_cls(req.getParameter("del_search_car_cls"));
			shVO.setSearch_car_num(req.getParameter("del_search_car_num"));
			shVO.setSearch_car_kind(req.getParameter("del_search_car_kind"));
			shVO.setSearch_date_option(req.getParameter("del_search_date_option"));
			shVO.setSearch_s_date(req.getParameter("del_search_s_date"));
			shVO.setSearch_e_date(req.getParameter("del_search_e_date"));
		}
		/* 2015.03.13 swyang 차량 삭제 검색조건 유지 종료 */

		int modelCnt = 0;
		int totCnt   = 0;
		// PAGING...
		if (shVO.getCur_page() <= 0) {    shVO.setCur_page(1);    }
		if(shVO.getSearch_sort1() == null){shVO.setSearch_sort1("sort1_1");}
		if(shVO.getSearch_sort2() == null){shVO.setSearch_sort2("ASC");}
		if (shVO.getReturn_page() != null) {
				shVO.setCur_page(Integer.parseInt(shVO.getReturn_page()));
		}

		shVO.setPage_cls("USR");
		shVO.setUsr_mst_key(sVO.getUsr_mst_key());
		shVO.setComp_bsns_num(sVO.getUsr_bsns_num()); // 2013.10.14 mgkim 차량계약 근거자료 조회 쿼리 추가 사업자번호 값 추가

		totCnt = CarManageService.getCarCount(shVO);
		//direct_totCnt = (Integer)CarManageService.CarManageFirstChkCnt(shVO);   //직영, 지입차량 대수 가져오기

		List<SysCodeVO> codeFMS003 = commonService.commonCode("FMS003", null);
		List<SysCodeVO> codeFMS002 = commonService.commonCode("FMS002", null);

		shVO.setS_row(Util.getPagingStart(shVO.getCur_page()));
		shVO.setE_row(Util.getPagingEnd(shVO.getCur_page()));
		shVO.setTot_page(Util.calcurateTPage(totCnt));

		carVOS = CarManageService.searchCar(shVO);

		if(carVOS != null){
			modelCnt  = carVOS.size();
		}

		/*2021.01.11 ysw 크로스사이트 스크립트 필터 적용*/
		if(shVO.getSearch_s_date() != null) {
			shVO.setSearch_s_date(Xsite_secure(shVO.getSearch_s_date()));
		}
		if(shVO.getSearch_e_date() != null) {
			shVO.setSearch_e_date(Xsite_secure(shVO.getSearch_e_date()));
		}
		if(shVO.getSearch_car_num() != null) {
			shVO.setSearch_car_num(Xsite_secure(shVO.getSearch_car_num()));
		}
		bcode = Xsite_secure(req.getParameter("bcode"));
		rcode = Xsite_secure(req.getParameter("rcode"));

		// 페이지 네비  및 디폴트 검색조건 VO
		model.addAttribute("VO", shVO);
		model.addAttribute("codeFMS003", codeFMS003);
		model.addAttribute("codeFMS002", codeFMS002);

		// 페이지 리스트 뷰 Model
		model.addAttribute("modelCnt", modelCnt);
		model.addAttribute("carList", carVOS);
		model.addAttribute("TOTCNT", totCnt);
		//model.addAttribute("DIRECT_TOTCNT"   , direct_totCnt);
		model.addAttribute("contextPath", contextPath);

		//2016. 04. 27 written by dyahn BCODE, RCODE
		model.addAttribute("BCODE", bcode);
		model.addAttribute("RCODE", rcode);
		model.addAttribute("RES", req.getParameter("RES")); // 2013.11.18 mgkim 직영차량 수정기능후 상태알림
		model.addAttribute("MSG", req.getParameter("MSG"));

		//2016. 05. 10. written by dyahn 오늘날짜 가져오기
		SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat ( "yyyy.MM.dd", Locale.KOREA );
		Date currentTime = new Date ( );
		String toDay = mSimpleDateFormat.format ( currentTime );
		model.addAttribute("toDay", toDay);

		return "/fpis/online/stdinfo/car/FpisCarManageList";
	}

	//2016. 01. 22 written by dyahn 차량등록현황 엑셀출력양식 콘트롤러
    @RequestMapping(value="/online/FpisCarManagerList_exportExcel.do")
    public ModelAndView FpisCarManagerList_exportExcel(@ModelAttribute("command") FpisCarManageVO command
        ,BindingResult bindingResult , HttpServletRequest req) throws Exception{

        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

        // PAGING...
        // 엑셀다운로드 어느 페이지에서 사용하던지 전체 목록 다운로드로 수정 - 2021.12.10 suhyun
        //if (command.getCur_page() <= 0) {    command.setCur_page(1);    }
        command.setCur_page(0);
        if(command.getSearch_sort1() == null){command.setSearch_sort1("sort1_1");}
        if(command.getSearch_sort2() == null){command.setSearch_sort2("ASC");}

        command.setPage_cls("USR");
        command.setUsr_mst_key(sVO.getUsr_mst_key());
        command.setComp_bsns_num(sVO.getUsr_bsns_num()); // 2013.10.14 mgkim 차량계약 근거자료 조회 쿼리 추가 사업자번호 값 추가

        int totCnt = CarManageService.getCarCount(command);

		command.setS_row(Util.getPagingStart(command.getCur_page()));
		command.setTot_page(Util.calcurateTPage(totCnt));  //끝페이지 계산
		command.setE_row(Util.getPagingEnd(command.getTot_page()));  //마지막 로우는 마지막페이지까지

        //2016. 01. 20. written by dyahn excel 다운로드 하는 객체
		//tiles 이슈로 6depth경로로 이동. - 2021.11.03 suhyun
        ModelAndView mav = new ModelAndView("/fpis/online/stdinfo/car/excelJsp/fileList/FpisCarManagerList_exportExcel");

        List<FpisCarManageVO> carVOS =  CarManageService.searchCar(command);

        mav.addObject( "VO" , command );
        mav.addObject( "carList" , carVOS );
        mav.addObject( "TOTCNT" , totCnt );
        return mav;
    }



	//2016. 11. 01 written by dyahn 차량등록현황 CSV 파일형식 출력
    @RequestMapping(value="/online/FpisCarManagerList_exportCsv.do")
    public  @ResponseBody ResponseEntity FpisCarManagerList_exportCsv(@ModelAttribute("command") FpisCarManageVO command
        ,BindingResult bindingResult , HttpServletRequest req, HttpServletResponse res) throws Exception{

        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

        // PAGING...
        if (command.getCur_page() <= 0) {    command.setCur_page(1);    }
        if(command.getSearch_sort1() == null){command.setSearch_sort1("sort1_1");}
        if(command.getSearch_sort2() == null){command.setSearch_sort2("ASC");}

        command.setPage_cls("USR");
        command.setUsr_mst_key(sVO.getUsr_mst_key());
        command.setComp_bsns_num(sVO.getUsr_bsns_num()); // 2013.10.14 mgkim 차량계약 근거자료 조회 쿼리 추가 사업자번호 값 추가

        int totCnt = CarManageService.getCarCount(command);

		command.setS_row(Util.getPagingStart(command.getCur_page()));
		command.setTot_page(Util.calcurateTPage(totCnt));  //끝페이지 계산
		command.setE_row(Util.getPagingEnd(command.getTot_page()));  //마지막 로우는 마지막페이지까지

        List<FpisCarManageVO> carVOS =  CarManageService.searchCar(command);

        //String fpisFilePath = "C:\\eGovFrame-2.0\\workspace.fpis\\fpis\\src\\main\\webapp";
        //String fpisFilePath = EgovProperties.getProperty("Globals.fpisFilePath");

        String fileName= "/data/fpis_network_"+ "test___"  +"_"+System.currentTimeMillis()+".csv";
        //String fileName= "test___"  +"_"+System.currentTimeMillis()+".csv";

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fpisFilePath+fileName), "EUC-KR"));

        int k = 1;
        out.write(String.valueOf("순번") + "," + String.valueOf( "차량번호" )  + "," + String.valueOf( "소유자" )  + "," + String.valueOf( "소속사업자번호")  + "," + String.valueOf( "차량구분" )  + "," +
                  String.valueOf( "차량종류" )  + "," + String.valueOf( "차량크기(톤)" )  + "," + String.valueOf( "계약시작일")  + "," + String.valueOf( "계약종료일" )  + "\r\n");
    	for(FpisCarManageVO car : carVOS) {

    		if(car.getCars_reg_num().equals(null)){car.setCars_reg_num("");  }
    		if(car.getComp_nm().equals(null)){car.setComp_nm("");  }
    		if(car.getComp_bsns_num_dec().equals(null)){car.setComp_bsns_num_dec("");  }
    		if(car.getCars_cls_nm().equals(null)){car.setCars_cls("");  }
    		if(car.getCars_kind_nm().equals(null)){car.setCars_kind_nm("");  }
    		if(car.getCars_size().equals(null)){car.setCars_size("");  }
    		if(car.getS_date() == null || car.getS_date().equals(null)){car.setS_date("");  }
    		if(car.getE_date() == null || car.getE_date().equals(null)){car.setE_date("");  }

            out.write(String.valueOf(k++) + "," + String.valueOf( car.getCars_reg_num() )  + "," + String.valueOf( car.getComp_nm() )  + "," + String.valueOf( car.getComp_bsns_num_dec() )  + "," + String.valueOf( car.getCars_cls_nm())  + "," +
                      String.valueOf( car.getCars_kind_nm() )  + "," + String.valueOf( car.getCars_size() )  + "," + String.valueOf( car.getS_date())  + "," + String.valueOf( car.getE_date() )  + "\r\n");
        }
		out.close();

		//2016. 11. 01 writen by dyahn 생성된 CSV파일 다운로드
		String filePath = fpisFilePath+fileName;
		File file = new File(filePath);
    	String extension = filePath.substring(filePath.length()-3, filePath.length());

        if("csv".equals(extension) || "CSV".equals(extension)){
            res.setContentType("text/csv");
            //res.setCharacterEncoding("UTF-8");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            setDisposition(  new String("차량관리현황_".getBytes(), "EUC-KR") + sdf.format(new Date()) + "." + extension, req, res  );

        }else{
        }

        res.setContentLength((int)file.length());
        FileCopyUtils.copy(new BufferedInputStream(new FileInputStream(file)), res.getOutputStream());
        file.delete();

        return new ResponseEntity(HttpStatus.OK);
    }


    //2016. 11. 01 written by dyahn 파일헤더설정(파일다운로드시 파일의 속성 체크)
	private void setDisposition(String filename, HttpServletRequest request, HttpServletResponse response) {
		String browser = getBrowser(request);

		String dispositionPrefix = "attachment; filename=";
		String encodedFilename = null;

		try {
			if ("MSIE".equals(browser)) {
				encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
			}
			else if ("Firefox".equals(browser)) {
				encodedFilename = "\"" + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
			}
			else if ("Opera".equals(browser)) {
				encodedFilename = "\"" + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
			}
			else if ("Chrome".equals(browser)) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < filename.length(); i++) {
					char c = filename.charAt(i);
					if (c > '~') {
						sb.append(URLEncoder.encode("" + c, "UTF-8"));
					}
					else {
						sb.append(c);
					}
				}
				encodedFilename = sb.toString();
			}
			else {
				encodedFilename = filename;
			}

		}
		catch ( IOException e ) {
			encodedFilename = filename;
		}

		response.setHeader("Content-Disposition", dispositionPrefix + encodedFilename);
		response.setHeader("Content-Transfer-Encoding", "binary");

		if ("Opera".equals(browser)) {
			response.setContentType("application/octet-stream;charset=UTF-8");
		}

	}

	//2016. 11. 01. written by dyahn 현재 접속 브라우저 체크
	private String getBrowser(HttpServletRequest request) {
		String header = request.getHeader("User-Agent");
		if (header.indexOf("MSIE") > -1) {
			return "MSIE";
		}
		else if (header.indexOf("Chrome") > -1) {
			return "Chrome";
		}
		else if (header.indexOf("Opera") > -1) {
			return "Opera";
		}
		return "Firefox";
	}

	/* 2013.09.05 mgkim 차량등록 - 작성등록 으로 메뉴 변경 */
	@RequestMapping("/online/FpisCarManagerRegist2.do")
	public String FpisCarManagerRegist2(@ModelAttribute("frmThis") FpisCarManageVO carsVO , BindingResult bindingResult ,
			HttpServletRequest req, ModelMap model) throws Exception {

		SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

		UsrInfoVO userInfoVO = FpisSvc.selectUsrInfo(sVO.getUsr_mst_key());

		//2015.01.16 mgkim 업체정보 세부 사업자유형 확인
        String strCompClsDetail = userInfoVO.getComp_cls_detail();

        //test!!
        model.addAttribute("strCompClsDetail", strCompClsDetail);

        String compCls_01_01 = "N";
        String compCls_01_02 = "N";
        String compCls_01_03 = "N";
        String compCls_01_04 = "N";
        String compCls_02_01 = "N";
        String compCls_02_02 = "N";
        String compCls_04_01 = "N";
        String[] strCCD = strCompClsDetail.split(",");
        for(int i=0; i<strCCD.length; i++){
                 if(strCCD[i].equals("01-01")){ compCls_01_01 = "Y"; }
            else if(strCCD[i].equals("01-02")){ compCls_01_02 = "Y"; }
            else if(strCCD[i].equals("01-03")){ compCls_01_03 = "Y"; }
            else if(strCCD[i].equals("01-04")){ compCls_01_04 = "Y"; }
            else if(strCCD[i].equals("02-01")){ compCls_02_01 = "Y"; }
            else if(strCCD[i].equals("02-02")){ compCls_02_02 = "Y"; }
            else if(strCCD[i].equals("04-01")){ compCls_04_01 = "Y"; }
        }

        model.addAttribute("compCls_01_01", compCls_01_01);
        model.addAttribute("compCls_01_02", compCls_01_02);
        model.addAttribute("compCls_01_03", compCls_01_03);
        model.addAttribute("compCls_01_04", compCls_01_04);
        model.addAttribute("compCls_02_01", compCls_02_01);
        model.addAttribute("compCls_02_02", compCls_02_02);
        model.addAttribute("compCls_04_01", compCls_04_01);
        
        if (req.getParameter("DURES") != null) {
        	String test = req.getParameter("DURES");
        	model.addAttribute("DURES", test);
        }

		List<SysCodeVO> codeFMS003 = commonService.commonCode("FMS003", null);
		List<SysCodeVO> codeFMS002   = commonService.commonCode("FMS002", null);

		carsVO.setUsr_mst_key(sVO.getUsr_mst_key());

		model.addAttribute("VO", carsVO); // Navi
		model.addAttribute("codeFMS002", codeFMS002);
		model.addAttribute("codeFMS003", codeFMS003);

		/*2014.11.26 양상완 사용자 업태 추가  1대사업자 확인용*/
		model.addAttribute("comp_cls_detail", sVO.getComp_cls_detail());
		model.addAttribute("comp_cls",sVO.getCond());

		model.addAttribute("BCODE", req.getParameter("bcode"));
		model.addAttribute("RCODE", req.getParameter("rcode"));

		return "/fpis/online/stdinfo/car/FpisCarManageRegist2";
	}

	/*
	 * 2013.09.03 mgkim 차량등록 - 파일등록 메뉴 신규생성
	 */
	@RequestMapping("/online/FpisCarManagerRegist3.do")
	public String FpisCarManagerRegist(HttpServletRequest req, ModelMap model) throws Exception {

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

		SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");
		model.addAttribute("comp_cls",sVO.getCond());

		model.addAttribute("mode", req.getParameter("mode"));
		model.addAttribute("f_name", req.getParameter("f_name"));
		return "/fpis/online/stdinfo/car/FpisCarManageRegist3";
	}
	
	//20220804 장기용차 대량 수정 메뉴 화면(페이지)
	@RequestMapping("/online/FpisCarManagerUpdate.do")
	public String FpisCarManagerUpdate(HttpServletRequest req, ModelMap model) throws Exception {

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
		
		String save = req.getParameter("save");		
		

		SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");
		model.addAttribute("comp_cls",sVO.getCond());

		model.addAttribute("mode", req.getParameter("mode"));
		model.addAttribute("f_name", req.getParameter("f_name"));
		
		
		model.addAttribute("save", save);
		
		/*
		 * if(save != null) { return
		 * "redirect:/online/FpisCarManageUpdate.do?mode=progress"; }
		 */
		
		
		return "/fpis/online/stdinfo/car/FpisCarManageUpdate";
	}
	
	//20220804 장기용차 대량 수정 등록
	
	@RequestMapping(value="/online/FpisCarManagerUpdate2.do")
	public String FpisCarManagerUpdate2(@RequestParam(value="source", required=false)String source,
			final MultipartHttpServletRequest req, Model model) throws Exception {
        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

		final Map<String, MultipartFile> files = req.getFileMap();
		InputStream fis = null;

		String f_name = null;
		Iterator<Entry<String, MultipartFile>> itr = files.entrySet().iterator();
		MultipartFile file;

		
		
		//String save = req.getParameter("save");
		
		
		
		/* 2021.01.11 pch 파일업로드 시 확장자 서버단체크 추가(csv파일아니면 파일등록페이지로 redirect) */
		String redirect_url = "";
		/*
		 * if(save != null) { redirect_url
		 * ="redirect:/online/FpisCarManagerUpdate.do?mode=progress"; } else
		 * redirect_url =
		 * "redirect:/online/FpisCarManagerUpdate.do?mode=typeErr";
		 */

		
		while(itr.hasNext()) {
			Entry<String, MultipartFile> entry = itr.next();

			file = entry.getValue();

			if(file.getOriginalFilename().toLowerCase().contains(".csv")){
				if(!"".equals(file.getOriginalFilename())) {// 확장자 체크는 jsp 에서 처리
					try {
						fis = file.getInputStream();

						f_name = saveUsrCarFile(Util.getDateFormat4()+"_"+sVO.getUsr_mst_key()+"_car.csv", fis);
					}catch(IOException e) {
						logger.error("[ERROR] - IOException : ", e);
						throw e;
					}finally {
						if(fis != null) fis.close();
					}
				}
				redirect_url = "redirect:/online/FpisCarManagerUpdate.do?mode=progress&f_name="+f_name;
			}else{
				redirect_url = "redirect:/online/FpisCarManagerUpdate.do?mode=typeErr";
			}
		}
		 
		
		
		return redirect_url;
	}
	
	//20220816 사용자에 따른 장기용차 엑셀 양식 다운로드
	@RequestMapping(value = "/online/FpisCarManagerListExportExcel.do")
	public void FpisCarManagerListExportExcel(FpisCarManageVO carVO, ModelMap model, HttpServletRequest req, HttpServletResponse res)
	throws Exception ,UnsupportedEncodingException ,IOException{
		
		SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");
		carVO.setUsr_mst_key(sVO.getUsr_mst_key());		
		
		List<FpisCarManageVO> exportList = CarManageService.selCarHisInfForExcel(carVO);
		
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int date = cal.get(Calendar.DATE);		
		
		//res.setContentType("ms-vnd/excel; charset=euc-kr");
		res.setContentType("text/csv; charset=euc-kr");		
		
		String fileName = "장기용차수정양식_" + year + "년" + month + "월" + date + "일" + ".csv";
		
		fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");		
		res.setHeader("Content-Disposition", "ATTachment; Filename=" + fileName);
		
		//workbook.write(res.getOutputStream());		
		
		BufferedWriter buff = new BufferedWriter(new OutputStreamWriter(res.getOutputStream(),Charset.forName("EUC-KR")));
        CSVWriter writer = new CSVWriter(buff);
        
        List<String[]> records = new ArrayList<String[]>();
        // adding header record
 		records.add(new String[] { "번호", "차량번호", "기존상태", "상태변경", "기존시작일" ,"기존종료일", "변경시작일", "변경종료일", "변경 소유자명"
 				,"변경 사업자번호", "변경 차량번호", "변경 차량종류", "변경 차량크기(톤)"});
 		Iterator<FpisCarManageVO> it = exportList.iterator();
 		int i = 1;
		while (it.hasNext()) {
			FpisCarManageVO exp = it.next();
			
			String remark = exp.getRemark();
			if(remark.equals("01")) { remark = "신규등록";}				
			else if(remark.equals("02")) { remark = "기간변경";}
			else if(remark.equals("03")) { remark = "휴업";}
			else if(remark.equals("04")) { remark = "용도변경";}
			else if(remark.equals("05")) { remark = "계약해지";}
			else if(remark.equals("06")) { remark = "이용";}
			else if(remark.equals("07")) { remark = "차량소속변경";}
			else if(remark.equals("08")) { remark = "차량번호변경";}
			else remark = exp.getRemark();
			
			records.add(new String[] { Integer.toString(i), exp.getCars_reg_num(), remark, null, exp.getS_date(), exp.getE_date(), null ,null , null,
					null, null, null, null});
			
			i++;
		}		
        
        List<String[]> data = records;
        
        writer.writeAll(data);
        writer.close();
		
	}
	

	/*
	 * 기초정보 관리 -> 차량정보관리
	 *  - FpisCarManagerList : 차량정보 조회
	 *  2013.09.11 mgkim 차량관리 메뉴 수정
	 */
	@RequestMapping("/online/FpisCarManagerDetail_renewal.do") //
	public String FpisCarManagerDetail_renewal(@ModelAttribute("frmThis") FpisCarManageVO carVO ,
			BindingResult bindingResult ,  HttpServletRequest req, ModelMap model) throws Exception {

		SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");
		carVO.setUsr_mst_key(sVO.getUsr_mst_key());

		//수정이후 redirect 분기
		if(req.getParameter("redirect_yn") != null && req.getParameter("redirect_yn").equals("Y")){
			carVO.setUsr_mst_key(req.getParameter("re_usr_mst_key"));    //사업자번호
			carVO.setCar_reg_seq(req.getParameter("re_car_reg_seq"));      //차량등록 시퀀스
			carVO.setCars_cls(req.getParameter("re_cars_cls"));      //차량구분
			carVO.setCur_page(Integer.parseInt(req.getParameter("re_cur_page")));      //페이징정보

			carVO.setSearch_sort1(req.getParameter("re_search_sort1"));
			carVO.setSearch_sort2(req.getParameter("re_search_sort2"));
			carVO.setSearch_car_cls(req.getParameter("re_search_car_cls"));
			carVO.setSearch_car_num(req.getParameter("re_search_car_num"));
			carVO.setSearch_car_kind(req.getParameter("re_search_car_kind"));
			carVO.setSearch_date_option(req.getParameter("re_search_date_option"));
			carVO.setSearch_s_date(req.getParameter("re_search_s_date"));
			carVO.setSearch_e_date(req.getParameter("re_search_e_date"));
			carVO.setSearch_status(req.getParameter("re_search_status"));

			//한글깨짐 방지
		    String re_cars_reg_num = req.getParameter("re_cars_reg_num");
		    re_cars_reg_num = new String(re_cars_reg_num.getBytes("8859_1"),"UTF-8");
		    carVO.setCars_reg_num(re_cars_reg_num);                                //차량번호
		}

		carVO.setReturn_page(String.valueOf(carVO.getCur_page()));

		FpisCarManageVO detailVO = new FpisCarManageVO();
		//차량 상세정보 가져오기
		detailVO = CarManageService.selectUsr_cars_info_bySeq(carVO);

		//장기용차인 경우
		if(detailVO.getCars_cls().equals("03")){

			FpisCarHistoryVO carHistoryVO = new FpisCarHistoryVO();
	        carHistoryVO.setUsr_mst_key(carVO.getUsr_mst_key());
	        carHistoryVO.setCar_reg_seq(carVO.getCar_reg_seq());


			if(req.getParameter("carHistory_cur_page") != null){
				int cur_page = Integer.parseInt(req.getParameter("carHistory_cur_page"));
				carHistoryVO.setCur_page(cur_page);
			}

			List<FpisCarHistoryVO> carHistoryVOS =  null;
			int totalPeriod = 0;

	        //이력정보 가져오기
			if (carHistoryVO.getCur_page() <= 0) {    carHistoryVO.setCur_page(1);    }
			int totCnt = CarManageService.selectUsr_history_count(carVO);     //이력정보 갯수 가져오기
			carHistoryVO.setS_row(Util.getPagingStart(carHistoryVO.getCur_page()));
			carHistoryVO.setE_row(Util.getPagingEnd(carHistoryVO.getCur_page()));
			carHistoryVO.setTot_page(Util.calcurateTPage(totCnt));

			if(totCnt < 1){
				//이력테이블에 차량이 없을 경우 이력테이블 삽입
				detailVO.setRemark("01");
                CarManageService.updateUsrCar(detailVO, "Y");
                totCnt++;
			}


	        carHistoryVOS = 	CarManageService.selectUsr_history_list(carHistoryVO);     //이력정보 리스트 가져오기
	        //2015. 05.19 written by dyahn
	        for(int i = 0 ; i <  carHistoryVOS.size(); i++) {
	            if(carHistoryVOS.get(i).getRemark().equals("01") || carHistoryVOS.get(i).getRemark().equals("06")){
	                totalPeriod = totalPeriod + Integer.parseInt(carHistoryVOS.get(i).getPeriod());
	            }
	        }

            model.addAttribute("totalPeriod", totalPeriod);
            model.addAttribute("HISTORY_TOTCNT", totCnt);
            model.addAttribute("carHistoryVO", carHistoryVO);
            model.addAttribute("carHistoryVOS", carHistoryVOS);
		}


		List<SysCodeVO> codeFMS003 = commonService.commonCode("FMS003", null);
		List<SysCodeVO> codeFMS002 = commonService.commonCode("FMS002", null);

		model.addAttribute("codeFMS003", codeFMS003);
		model.addAttribute("codeFMS002", codeFMS002);
		model.addAttribute("VO", carVO);

		model.addAttribute("DetailVO", detailVO);

		//2016. 04. 27 written by dyahn BCODE, RCODE
		model.addAttribute("bcode", req.getParameter("bcode"));
		model.addAttribute("rcode", req.getParameter("rcode"));
		model.addAttribute("BCODE", req.getParameter("bcode"));
		model.addAttribute("RCODE", req.getParameter("rcode"));
		model.addAttribute("RES", req.getParameter("res"));

		return "/fpis/online/stdinfo/car/FpisCarManageDetail_renewal";
	}

	/**
	 * @Method Name : FpisCarManagerInsert
	 * @자성일   : 2012. 10. 3.
	 * @작성자   : limtg
	 * @변경이력 :
	 * 2013.09.05 GNT-mgkim 차량등록 - 선택등록 메뉴명 변경 , 저장기능 수정 보완
	 * @Method 설명: 시스템 관리-차량정보 테이블에서 선택적으로 사용자 차량관리 테이블로 이관
	 * @param shVO
	 * @param req
	 * @param model
	 * @return
	 * @throws Exception
	 * 2015.01.29 양상완 차량 작성등록 삭제함...
	 */
	@RequestMapping("/online/FpisCarManagerInsert.do")
	public String FpisCarManagerInsert(@ModelAttribute("searchForm") FpisCarManageVO frmVO ,
			BindingResult bindingResult ,
			HttpServletRequest req, ModelMap model) throws Exception {
		req.getSession().getAttribute("SessionVO");
		List<String> ErrorMSG  = new ArrayList<String>();

		String in_search_car_num = "";
		if(frmVO.getSearch_car_num() != null){
			//in_search_car_num = new String (frmVO.getSearch_car_num().getBytes("8859_1"),"UTF-8");		// 2013.09.05 mgkim 리다이렉트로 파라메타 전달시 한글 깨짐. // 개발중일때
			in_search_car_num = new String (frmVO.getSearch_car_num().getBytes("KSC5601"),"EUC-KR");		// 2013.12.27 실서버용
		}

		model.addAttribute("in_search_car_num"         , in_search_car_num);            // 2013.09.05 mgkim 검색 파라메터 유지
		model.addAttribute("in_search_comp_mst_key"         , frmVO.getSearch_comp_mst_key());  // 2013.09.05 mgkim 검색 파라메터 유지

		FpisCarManageVO  _attVO   = null;
		SessionVO ssVO = (SessionVO)req.getSession().getAttribute("SessionVO");
		if(frmVO.getCarchk() != null && !frmVO.getCarchk().equals(""))
		{
			String [] tokArry;
			String [] carsArry;
			tokArry = frmVO.getCarchk().split(",");
			for(int i = 0 ; i < tokArry.length ; i++)
			{
				if(tokArry[i] != null && !tokArry[i].equals(""))
				{
					carsArry = tokArry[i].split("_");
					if(carsArry.length > 0)
					{
						_attVO = new FpisCarManageVO();
						_attVO.setUsr_mst_key(ssVO.getUsr_mst_key());
						_attVO.setSys_comp_mst_key(carsArry[0]);
						_attVO.setSys_cars_mst_key(carsArry[1]);

						//                        if(frmVO.getChk_sys().equals("0")){
							_attVO.setS_date(frmVO.getS_date());
							_attVO.setE_date(frmVO.getE_date());
							//                        	_attVO.setChk_sys(frmVO.getChk_sys());
							//                        }

							/*2015.01.23 양상완 차량근거 파일 기능 제거.*/
							//                    	MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) req;
							//                		MultipartFile file;
							//                		file = multiRequest.getFile("upload_file");
							//                		FpisUsrCarsFileInfoManageVO usrCarsFileVO = new FpisUsrCarsFileInfoManageVO();
							//                		String fileName =null ;
							//                		String fileType =null ;
							//                		if(file.getSize()>0){
							//                			_attVO.setUsr_car_file("Y");
							//                			fileName = file.getOriginalFilename(); //오리지날 파일네임
							//                	        fileType = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length()); // 파일확장자
							//                	        usrCarsFileVO.setOrg_file_name(fileName);
							//                	        usrCarsFileVO.setFile_dir(upload_path_usrCarZip+ Util.getDateFormat().substring(0, 4) + file_db_path);
							//                	        usrCarsFileVO.setUsr_mst_key(sVO.getUsr_mst_key());
							//                	        usrCarsFileVO.setCar_reg_seq(frmVO.getCar_reg_seq());
							//                	        usrCarsFileVO.setUsr_comp_bsns_num(sVO.getUsr_mst_key());
							//                	        usrCarsFileVO.setUsr_cars_reg_num(frmVO.getCars_reg_num());
							//                	        usrCarsFileVO.setNew_yn("Y");
							//                		}


							_attVO.setCars_cls(frmVO.getCars_cls());
							if(frmVO.getCars_cls().equals("01") || frmVO.getCars_cls().equals("02")){
								_attVO.setPerfect_cls2(frmVO.getPerfect_cls2());
								/*2014.12.06 직영차량 선택등록시 무조건 승인처리.*/
								if(_attVO.getPerfect_cls2().equals("Y")){  // 바로승인 처리 아님.
								}else{// 바로승인 처리
									_attVO.setPerfect_cls("Y");
								}
							}else if(frmVO.getCars_cls().equals("04")){
								_attVO.setPerfect_cls("Y");
							}
							_attVO.setUpdate_flag_code("0");
							if(CarManageService.InsertSysCar(_attVO) == 0){
								/*2015.01.23 양상완 차량근거 파일 기능 제거.*/
								//                        	InputStream fis = null;
								//                        	String file_rename = usrCarsFileVO.getUsr_cars_reg_num() +"_"+ usrCarsFileVO.getUsr_mst_key() +"_"+ _attVO.getS_date()+"_"+_attVO.getE_date()+"."+fileType;
								//                        	usrCarsFileVO.setFile_name(file_rename);
								//                        	if(file.getSize()>0){
								//                        		CarManageService.InsertUsrCarsFileInfo(usrCarsFileVO);
								//                           		fis = file.getInputStream();
								//                        		saveUsrCarZipFile(file_rename, fis); // 저장되었습니다.
								//                        	}
							}else{
							}
					}
				} // end of if()
			} // end of for()
		}// end of if ()

		String MSG= "";
		for(String msg : ErrorMSG ){
			MSG = MSG + msg + "\n";
		}

		int errCnt = 0;
		errCnt = ErrorMSG.size();
		model.addAttribute("errCnt", errCnt);
		model.addAttribute("MSG", MSG);
		return "redirect:/online/FpisCarManagerRegist.do";

	}

	/**
	 * 차량 수동등록 하기 저장.
	 * 2013.09.05 mgkim 차량등록 - 작성등록 으로 메뉴명 변경
	 */
	@RequestMapping("/online/FpisCarManagerInsert2.do")
	public String FpisCarManagerInsert2(@ModelAttribute("frmThis") FpisCarManageVO carsVO ,
			BindingResult bindingResult ,
			final HttpServletRequest req,
			ModelMap model) throws Exception {
		String res = "";
		String duRes = "";
		SessionVO ssVO = (SessionVO)req.getSession().getAttribute("SessionVO");
		carsVO.setUsr_mst_key(ssVO.getUsr_mst_key());
		
		/* 2022.08.23 jwchoi 웹취약점 조치 차량중복 검증 서버에서 처리 */
		int chkCnt = CarManageService.chkUsrCarInfoCnt(carsVO);
		if (chkCnt > 0) {
			duRes = "DUPLE";
			model.addAttribute("DURES"  , duRes);
			return "redirect:/online/FpisCarManagerRegist2.do";
		}
		carsVO.setComp_bsns_num(carsVO.getComp_bsns_num().replaceAll("-", ""));// 2013.10.24 mgkim 차량관리 작성등록 사업자번호 기호 제거
		///////////////////////////////////////////////////////////
		// 입력작업
		///////////////////////////////////////////////////////////

		if(carsVO.getPage_cls() != null && !carsVO.getPage_cls().equals("")) {
			// 입력
			if(carsVO.getPage_cls().equals("INS")) {
				if(carsVO.getCars_reg_num() == null || carsVO.getCars_reg_num().equals("")) {
					res  = "NULL";
				} else {
					/*2015.01.28 양상완 시스템에 존재하는 차량인지 검사하는 부분 삭제*/


					if(!ChkFpisCarNum(carsVO.getCars_reg_num())){ // 2013.09.27 mgkim 올바르지 않은 화물차량 번호[아,바,사,자    배]
						res = "ERRNUM";
					}else{
						/*2014.09.15 양상완 등록시 이력 추가.*/
						carsVO.setUpdate_flag_code("0"); //등록
						/*2015.01.23 양상완 차량근거자료 기능 삭제.*/
						/*장기용차를 제외한 나머지  바로 승인*/
						carsVO.setUpdate_flag_code("0");
						if( CarManageService.InsertTmpUsrCar(carsVO, "Y") < 0 ) {
							res = "FAIL";
						}else{
							res = "SUC";
							/*2015.01.23 양상완 차량근거자료 기능 삭제.*/
						}
					}
				}
			}
		}

		List<SysCodeVO> cKinds = commonService.commonCode("FMS003", null);
		List<SysCodeVO> cCls   = commonService.commonCode("FMS002", null);

		model.addAttribute("VO"         , carsVO  ); // Navi
		model.addAttribute("CARS_CLS"   , cCls         );
		model.addAttribute("CARS_KIND"  , cKinds       );
		model.addAttribute("RES"  , res);

		model.addAttribute("BCODE"  , req.getParameter("bcode"));
		model.addAttribute("RCODE"  , req.getParameter("rcode"));


		return "/fpis/online/stdinfo/car/FpisCarManageRegist2";
	}

	/*
	 * 2013.09.27 mgkim 차량번호 체크 기능구현
	 * 화물운수차량은 화물차[아,바,사,자] / 택배[배] 패턴을 가진다. 이외는 운수차량이 아님.
	 * 화물 : 80~97 , 특수 : 98,99
	 * -------------------------------
	 * 화물차 인터넷 정보
	 * [자가용] : 80 ~ 89. 90 ~ 97
	 * [화물] : 90, 91 아
	 * [개별화물] : 90 바
	 * [용달화물] : 90 자
	 * [트레일러] : 90 사
	 * -------------------------------
	 */
	private boolean ChkFpisCarNum(String strCarNum){
		// 테스트 패턴 1 : 서울80아1234
		// 테스트 패턴 2 : 80아1234
		int lenCarNum = strCarNum.length();
		String carNum_Kor = "";
		String temp = "";
		int carNum_Num = 0;
		if(lenCarNum == 9){        // 테스트 패턴 1 : 서울80아1234
			temp = strCarNum.substring(2, 4);
			carNum_Num = Integer.parseInt(temp);
			carNum_Kor = strCarNum.substring(4, 5);
		}else if(lenCarNum == 7){
			temp = strCarNum.substring(0, 2);
			carNum_Num = Integer.parseInt(temp);
			carNum_Kor = strCarNum.substring(2, 3);
		}
		if(carNum_Num >= 80 && carNum_Num <= 99){
			if("아".equals(carNum_Kor) || "바".equals(carNum_Kor) || "사".equals(carNum_Kor) || "자".equals(carNum_Kor) || "배".equals(carNum_Kor)){
				return true;
			}
		}
		return false;
	}

	/*
	 * 2013.09.06 mgkim 차량등록 - 작성등록
	 * 사업자번호 유효성 검사 ajax 모듈
	 */
	@RequestMapping("/online/FpisCarManagerInsert2_chkComBsnsNum.do")
	public void FpisCarManagerInsert2_chkComBsnsNum(@RequestParam(value="comp_bsns_num", required=false)String comp_bsns_num,
			HttpServletResponse res,
			HttpServletRequest req,
			Model model) throws Exception {

		JSONObject json = new JSONObject();
		//FpisCompSvc
		int isCompBsnsNum = FpisCompSvc.isSysCompanyInfoCount(comp_bsns_num);
		if(isCompBsnsNum < 1){
			isCompBsnsNum = FpisCompSvc.isUsrInfoCount(comp_bsns_num);
		}
		String comp_nm = "";
		if(isCompBsnsNum > 0){ // 2013.12.13 mgkim 사업자 있으면 해당 업체명 가져오기
			comp_nm = FpisCompSvc.getCompNmUsrInfoOrSysInfo(comp_bsns_num);
		}
		json.put("isCompBsnsNum",isCompBsnsNum);
		json.put("comp_nm",comp_nm);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");;
		PrintWriter out = res.getWriter();

		out.write(json.toString());
		out.close();
	}

	/*
	 * 2013.09.12 mgkim 차량등록 -파일등록
	 * 2013.09.12 mgkim 파일등록 기능 수정 - 소속사업자번호 항목 추가
	 */
	@RequestMapping(value="/online/FpisCarManagerInsert3.do")
	public String FpisCarManagerInsert3(@RequestParam(value="source", required=false)String source,
			final MultipartHttpServletRequest req, Model model) throws Exception {
        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

		final Map<String, MultipartFile> files = req.getFileMap();
		InputStream fis = null;

		String f_name = null;
		Iterator<Entry<String, MultipartFile>> itr = files.entrySet().iterator();
		MultipartFile file;

		/* 2021.01.11 pch 파일업로드 시 확장자 서버단체크 추가(csv파일아니면 파일등록페이지로 redirect) */
		String redirect_url = "";

		while(itr.hasNext()) {
			Entry<String, MultipartFile> entry = itr.next();

			file = entry.getValue();

			if(file.getOriginalFilename().toLowerCase().contains(".csv")){
				if(!"".equals(file.getOriginalFilename())) {// 확장자 체크는 jsp 에서 처리
					try {
						fis = file.getInputStream();

						f_name = saveUsrCarFile(Util.getDateFormat4()+"_"+sVO.getUsr_mst_key()+"_car.csv", fis);
					}catch(IOException e) {
						logger.error("[ERROR] - IOException : ", e);
						throw e;
					}finally {
						if(fis != null) fis.close();
					}
				}
				redirect_url = "redirect:/online/FpisCarManagerRegist3.do?mode=progress&f_name="+f_name;
			}else{
				redirect_url = "redirect:/online/FpisCarManagerRegist3.do?mode=typeErr";
			}
		}
		return redirect_url;
	}


	/* 2013.09.27 mgkim 파일저장 경로변경 */
	private String saveUsrCarFile(String fname, InputStream fis) {
		//        File _sFile = new File("/var/" + System.currentTimeMillis());

		// 일자별 저장...
		File _sFile = new File(upload_path_usrCar + File.separator + Util.getDateFormat() + File.separator + fname);
		if(!_sFile.getParentFile().exists()) {
			_sFile.getParentFile().mkdirs();
		}
		FileOutputStream fw = null;
		try {
			byte[] buf = new byte[1024];
			int cnt = 0;
			if(!_sFile.getParentFile().exists()) {
				_sFile.getParentFile().mkdirs();
			}
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

	/*
	 * 2013.10.02 mgkim 차량계약 근거자료 파일저장 모듈(신규)
	 * 근거자료 파일명 : 2013_사업자번호_차량번호.zip
	 * 2013.10.15 mgkim 근거자료 파일명명 변경, DB 등록기능 추가
	 * 근거자료 파일명 : 차량번호_사업자번호_제출년월일.zip
	 * 2013.12.12 mgkim 확장자 zip 아닌것도 허용
	 */
	@RequestMapping("/online/FpisCarManagerUsrCarZipUpload.do")
	public String FpisCarManagerUsrCarZipUpload(@RequestParam(value="source", required=false)String source,
			final HttpServletRequest req, Map commandMap, ModelMap model) throws Exception {
		// TODO 2013.10.02 mgkim 근거파일 저장기능

		SessionVO ssVO = (SessionVO)req.getSession().getAttribute("SessionVO"); // 2013.10.15 기능수정으로 사업자번호와, 차량번호 필요.
		//String usr_cars_reg_num = (String)req.getParameter("hidCarRegNum");
		String usr_cars_reg_num = new String (req.getParameter("hidCarRegNum").getBytes("KSC5601"),"EUC-KR");        //2013.12.31 mgkim 운영서버 인코딩


		// 2013.10.02 mgkim 차량계약 근거자료 파일저장 시작
		final MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) req;
		final Map<String, MultipartFile> files = multiRequest.getFileMap();
		InputStream fis = null;
		Iterator<Entry<String, MultipartFile>> itr = files.entrySet().iterator();
		MultipartFile file;
		while(itr.hasNext()) {
			Entry<String, MultipartFile> entry = itr.next();
			file = entry.getValue();
			String fileName = file.getOriginalFilename();
			String fileType = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
			if(!"".equals(file.getOriginalFilename())) {// 확장자 체크는 jsp 에서 처리
				try {
					fis = file.getInputStream();
					//saveUsrCarZipFile(file.getOriginalFilename(), fis); // 저장되었습니다.
					// 2013.10.15 mgkim 근거자료 등록 기능 수정
					String file_rename = usr_cars_reg_num +"_"+ ssVO.getUsr_bsns_num() +"_"+ Util.getDateFormat()+"."+fileType;
					/* 2014.03.20 mgkim 차량근거자료 파일명 암호화 시작 */
					// TODO 차량계약 근거자료




					/* 2014.03.20 mgkim 차량근거자료 파일명 암호화 끝 */
					saveUsrCarZipFile(file_rename, fis); // 저장되었습니다.
					FpisUsrCarsFileInfoManageVO usrCarsFileVO = new FpisUsrCarsFileInfoManageVO();
					usrCarsFileVO.setUsr_comp_bsns_num(ssVO.getUsr_bsns_num());
					usrCarsFileVO.setUsr_cars_reg_num(usr_cars_reg_num);
					usrCarsFileVO.setFile_dir(upload_path_usrCarZip+ Util.getDateFormat().substring(0, 4) + file_db_path);
					usrCarsFileVO.setFile_name(file_rename);
					usrCarsFileVO.setOrg_file_name(file.getOriginalFilename());
					usrCarsFileVO.setUsr_mst_key(ssVO.getUsr_mst_key()); // 2013.10.31 mgkim 사업자번호만으로는 한번에 조인 안되므로 마스터키를 사용함
					try{
						int isExistUsrCarsFile = CarManageService.isExistUsrCarsFile(usrCarsFileVO);
						if(isExistUsrCarsFile < 1){
							CarManageService.InsertUsrCarsFileInfo(usrCarsFileVO);
						}
					}catch(SQLException e){
						logger.error("[ERROR] - SQLException : ", e);
					}
				}catch(IOException e) {
					logger.error("[ERROR] - IOException : ", e);
					throw e;
				}finally {
					if(fis != null) fis.close();
				}
			}
		}
		// 2013.10.02 mgkim 차량계약 근거자료 파일저장 종료
		return "redirect:/online/FpisCarManagerList.do";
	}

	/* 2013.10.02 mgkim 차량관리 차량계약 근거자료 파일 저장 */
	private String saveUsrCarZipFile(String fname, InputStream fis) {
		String fname_new = fname;


		/* // 2014.01.02 mgkim 한글 파일명 깨짐현상 수정 테스트
    	try {
    		fname_new = new String (fname.getBytes("KSC5601"),"EUC-KR");
		}catch(UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
        }*/

		// 2014.01.15 mgkim 한글 파일명 깨짐현상 수정 테스트
		byte ptext[] = fname.getBytes();
		try {
			//			fname_new = new String(ptext, "EUC-KR");
			fname_new = new String(ptext, "UTF-8");
		}catch(UnsupportedEncodingException e1) {
			logger.error("[ERROR] - UnsupportedEncodingException : ", e1);
		}


		// 년도별 디렉토리 저장...
		File _sFile = new File(upload_path_usrCarZip + File.separator + Util.getDateFormat().substring(0, 4) + File.separator + fname_new);

		if(!_sFile.getParentFile().exists()) {
			_sFile.getParentFile().mkdirs();
		}
		FileOutputStream fw = null;
		try {
			byte[] buf = new byte[1024];
			int cnt = 0;
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

	public static boolean isNumber(String number){
		boolean flag = true;
		if ( number == null  ||    "".equals( number )  ) return false;

		int size = number.length();
		int st_no= 0;

		if ( number.charAt(0)  ==  45 )//음수인지 아닌지 판별 . 음수면 시작위치를 1부터
			st_no = 1;


		for ( int i = st_no ; i < size ; ++i ){
			if ( !( 48   <=  (number.charAt(i))   && 57>= ( number.charAt(i) )  )  ){
				flag = false;
				break;
			}

		}

		return flag;
	}


	/*
	 * 차량관리 현황 - 차량삭제 프로세스
	 * 2013.12.12 mgkim 차대번호 안씀 ==> 신고주체 사업자번호와 차량번호로 처리
	 * 2015.02.10 양상완 차번호 -> 시퀀스로 수정
	 */
	@RequestMapping("/online/FpisCarManagerDelete.do")
	public String FpisCarManagerDelete(@ModelAttribute("frmThis") FpisCarManageVO frmVO ,
			BindingResult bindingResult ,
			HttpServletRequest req, ModelMap model) throws Exception {
		List<FpisCarManageVO> _carVOS  = new ArrayList<FpisCarManageVO>();
		FpisCarManageVO  _attVO   = null;
		int              delCnt   = -1;

		SessionVO ssVO = (SessionVO)req.getSession().getAttribute("SessionVO");

		frmVO.setCarchk(req.getParameter("checkedList"));
		if(frmVO.getCarchk() != null && !frmVO.getCarchk().equals(""))
		{
			String [] tokArry;
			//--------------------------------------------------------------
			// 1차량마스터에서 선택된 차량을  차량소유업체와  차량을 분리하여 List에 담는다.
			// List에 담긴 차량정보,업체정보,USR_MST_KEY를  신고주체 차량관리테이블에 저장하기 위함.
			//--------------------------------------------------------------
			tokArry = frmVO.getCarchk().split(",");
			for(int i = 0 ; i < tokArry.length ; i++)
			{
				if(tokArry[i] != null && !tokArry[i].equals(""))
				{
					_attVO = new FpisCarManageVO();
					_attVO.setUsr_mst_key(ssVO.getUsr_mst_key());
					_attVO.setCar_reg_seq(tokArry[i]);
					_carVOS.add(_attVO);
				} // end of if()
			} // end of for()
		}


        String checkedList = req.getParameter("checkedList");

        new StringTokenizer(checkedList,",");
        delCnt = CarManageService.deleteCar(_carVOS, ssVO.getUser_id());
		if(delCnt < 0 ) {
		}
		else {
			model.addAttribute("MSG", "del");
			model.addAttribute("state", "del_mode");
			model.addAttribute("del_search_sort1", frmVO.getSearch_sort1());                      // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_sort2", frmVO.getSearch_sort2());                      // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_cls", frmVO.getSearch_car_cls());                  // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_num", frmVO.getSearch_car_num());                  // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_kind", frmVO.getSearch_car_kind());                // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_date_option", frmVO.getSearch_date_option());          // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_s_date", frmVO.getSearch_s_date());                    // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_s_date", frmVO.getSearch_e_date());                    // 2015.03.13 swyang 삭제후 검색조건 유지
		}

		//2016. 05. 02 written by dyahn
		model.addAttribute("bcode", req.getParameter("bcode"));
		model.addAttribute("rcode", req.getParameter("rcode"));

		return "redirect:/online/FpisCarManagerList.do";
	}



    //2016. 05. 02 written by dyahn 차량이력 목록 선택삭제 콘트롤러
	@RequestMapping("/online/FpisCarManagerDelete_history.do")
	public String FpisCarManagerDelete_history(@ModelAttribute("frmThis") FpisCarManageVO frmVO ,
			BindingResult bindingResult ,
			HttpServletRequest req, ModelMap model) throws Exception {
		//List<FpisCarManageVO> _carVOS  = new ArrayList<FpisCarManageVO>();
		List<FpisCarHistoryVO> _historyVOS = new ArrayList<FpisCarHistoryVO>();
		FpisCarHistoryVO _attVO   = null;

		int              delCnt   = -1;
		SessionVO ssVO = (SessionVO)req.getSession().getAttribute("SessionVO");
		String checkedList = req.getParameter("checkedList");
		if(checkedList != null && !checkedList.equals("")) {
			StringTokenizer token = new StringTokenizer(checkedList,",");
			String order_key ="";
			String [] itemArry  = null;



		while(token.hasMoreTokens()) {
			order_key = token.nextToken();

			if(order_key != null && !order_key.equals("") && order_key.length() > 1) {
				itemArry = order_key.split("\\|");

				_attVO = new FpisCarHistoryVO();
				_attVO.setUsr_mst_key(ssVO.getUsr_mst_key());
				_attVO.setSeq(itemArry[0]);
				_attVO.setCar_reg_seq(itemArry[1]);
				_attVO.setRemark(itemArry[2]);
				_historyVOS.add(_attVO);

			}
		}


		}

		delCnt = CarManageService.deleteUsr_cars_history_selected(_historyVOS);


		if(delCnt < 0 ) {
		}
		else {
			model.addAttribute("MSG", "del");

			model.addAttribute("state", "del_mode");
			model.addAttribute("del_search_sort1", frmVO.getSearch_sort1());                      // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_sort2", frmVO.getSearch_sort2());                      // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_cls", frmVO.getSearch_car_cls());                  // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_num", frmVO.getSearch_car_num());                  // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_kind", frmVO.getSearch_car_kind());                // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_date_option", frmVO.getSearch_date_option());          // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_s_date", frmVO.getSearch_s_date());                    // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_s_date", frmVO.getSearch_e_date());                    // 2015.03.13 swyang 삭제후 검색조건 유지

		}

		//2016. 005. 02 written by dyahn 리다이렉트 파라미터
		model.addAttribute("redirect_yn", "Y");
		model.addAttribute("re_usr_mst_key", frmVO.getUsr_mst_key());
		model.addAttribute("re_car_reg_seq", frmVO.getCar_reg_seq());
		model.addAttribute("re_cars_reg_num", frmVO.getCars_reg_num());

		model.addAttribute("re_cars_cls", frmVO.getCars_cls());
		model.addAttribute("re_cur_page", frmVO.getCur_page());

		//2016. 05. 02 written by dyahn
		model.addAttribute("bcode", req.getParameter("bcode"));
		model.addAttribute("rcode", req.getParameter("rcode"));

		return "redirect:/online/FpisCarManagerDetail_renewal.do";
	}








	/*2015.02.10 양상완 검색목록 삭제 */
	@RequestMapping("/online/FpisCarManagerDeleteAll.do")
	public String FpisCarManagerDeleteAll(@ModelAttribute("frmThis") FpisCarManageVO frmVO ,
			BindingResult bindingResult ,
			HttpServletRequest req, ModelMap model) throws Exception {
		SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");

        // written by dyahn 삭제주체 아이피주소 저장


		String request_ip = Util.getClientIpAddr(req);

		frmVO.setUsr_mst_key(sVO.getUsr_mst_key());
		frmVO.setComp_bsns_num(sVO.getUsr_bsns_num());
		frmVO.setDel_user(sVO.getUser_id());
		frmVO.setHabbit(request_ip);

		int delCnt = CarManageService.deleteAllUsrCar(frmVO);

		if(delCnt>0){
			model.addAttribute("MSG", "del");
			model.addAttribute("state", "del_mode");
			model.addAttribute("del_search_sort1", frmVO.getSearch_sort1());                      // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_sort2", frmVO.getSearch_sort2());                      // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_cls", frmVO.getSearch_car_cls());                  // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_num", frmVO.getSearch_car_num());                  // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_car_kind", frmVO.getSearch_car_kind());                // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_date_option", frmVO.getSearch_date_option());          // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_s_date", frmVO.getSearch_s_date());                    // 2015.03.13 swyang 삭제후 검색조건 유지
			model.addAttribute("del_search_s_date", frmVO.getSearch_e_date());                    // 2015.03.13 swyang 삭제후 검색조건 유지

		}

		//2016. 05. 02 written by dyahn
		model.addAttribute("bcode", req.getParameter("bcode"));
		model.addAttribute("rcode", req.getParameter("rcode"));

		return "redirect:/online/FpisCarManagerList.do";
	}


	/*해당 Str이 실수인지 검사*/
	public boolean isDouble(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}



	/**
     * 2017.09.18 mgkim 미등록차량 알림기능 추가
     * [ 운송완료년월 기준 해당 차량번호가 차량등록에 되어 있는지 확인 및 알림기능 ]
     * */
    @RequestMapping(value="/online/isUsrCarsInfo_ajax.do")
    public void isUsrCarsInfo_ajax(@RequestParam(value="cars_reg_num", required=false)String cars_reg_num,
                                   @RequestParam(value="s_yyyymm", required=false)String s_yyyymm,
                                   @RequestParam(value="usr_mst_key_ag", required=false)String usr_mst_key_ag,
                                   HttpServletResponse res, HttpServletRequest req, Model model) throws Exception {
        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");
        FpisCarManageVO carVO = new FpisCarManageVO();
        if(usr_mst_key_ag == null || "".equals(usr_mst_key_ag)) {
            carVO.setUsr_mst_key(sVO.getUsr_mst_key());
        }else{
            carVO.setUsr_mst_key(usr_mst_key_ag);
        }


        carVO.setCars_reg_num(cars_reg_num);
        carVO.setS_yyyymm(s_yyyymm);
        int car_cnt = CarManageService.isUsrCarsInfo_RegOperate(carVO);

        res.setContentType("application/text");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out;
        try {
            out = res.getWriter();
            out.write(""+car_cnt);
            out.close();
        } catch (IOException e) {
            System.err.println("/online/isUsrCarsInfo_ajax.do ERROR!!");
        }
    }

    /**
     * 2017.09.18 mgkim 미등록차량 알림기능 추가
     * [ 운송완료년월 기준 해당 차량번호가 차량등록에 되어 있는지 확인 및 알림기능 ]
     * */
    @RequestMapping(value="/online/isUsrCarsInfo_cls03_ajax.do")
    public void isUsrCarsInfo_cls03_ajax(@RequestParam(value="cars_reg_num", required=false)String cars_reg_num,
                                             @RequestParam(value="s_yyyymm", required=false)String s_yyyymm,
                                             @RequestParam(value="usr_mst_key_ag", required=false)String usr_mst_key_ag,
                                             HttpServletResponse res, HttpServletRequest req, Model model) throws Exception {
        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");
        FpisCarManageVO carVO = new FpisCarManageVO();
        if(usr_mst_key_ag == null || "".equals(usr_mst_key_ag)) {
            carVO.setUsr_mst_key(sVO.getUsr_mst_key());
        }else{
            carVO.setUsr_mst_key(usr_mst_key_ag);
        }

        carVO.setCars_reg_num(cars_reg_num);
        carVO.setS_yyyymm(s_yyyymm);

        // 2017.10.13 이력없는 장기용차 이력 생성기능 안도영대리 로직 반영 시작
        // 2017.10.18 이력없는 SEQ 찾기 로직 추가.
        List<String> car_reg_seq = CarManageService.selectUsrInfo_notHistory(carVO);
        for(int i=0; i<car_reg_seq.size(); i++){
        	carVO.setCar_reg_seq(car_reg_seq.get(i));
        	carVO.setCars_cls("03");
        	int totCnt = CarManageService.selectUsr_history_count(carVO);     //이력정보 갯수 가져오기
            if(totCnt < 1){
                //이력테이블에 차량이 없을 경우 이력테이블 삽입
                FpisCarManageVO detailVO = new FpisCarManageVO();
                //차량 상세정보 가져오기
                detailVO = CarManageService.selectUsr_cars_info_bySeq(carVO);
                detailVO.setRemark("01");
                CarManageService.updateUsrCar(detailVO, "Y");
            }
        }

        // 2017.10.13 이력없는 장기용차 이력 생성기능 안도영대리 로직 반영 끝


        int car_cnt = CarManageService.isUsrCarsInfo_cls03_RegOperate(carVO);

        res.setContentType("application/text");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out;
        try {
            out = res.getWriter();
            out.write(""+car_cnt);
            out.close();
        } catch (IOException e) {
            System.err.println("/online/isUsrCarsInfo_ajax.do ERROR!!");
        }
    }




    /**
     * 2017.09.25 mgkim 장기용차 배차횟수 산정 추가
     * 2017.10.13 mgkim 장기용차 이력없는 경우 이력생성 로직 추가
     * */
    @RequestMapping(value="/online/FpisCarManager_getRegOperateCntYear_ajax.do")
    public void FpisCarManager_getRegOperateCntYear_ajax(@RequestParam(value="cars_reg_num", required=false)String cars_reg_num,
                                             @RequestParam(value="car_reg_seq", required=false)String car_reg_seq,
                                             @RequestParam(value="base_year", required=false)String base_year,
                                             HttpServletResponse res, HttpServletRequest req, Model model) throws Exception {

        SessionVO sVO = (SessionVO)req.getSession().getAttribute("SessionVO");
        FpisCarManageVO carVO = new FpisCarManageVO();
        carVO.setUsr_mst_key(sVO.getUsr_mst_key());
        carVO.setCars_reg_num(cars_reg_num);
        carVO.setCar_reg_seq(car_reg_seq);

        // 2017.10.13 이력없는 장기용차 이력 생성기능 안도영대리 로직 반영 시작
        int totCnt = CarManageService.selectUsr_history_count(carVO);     //이력정보 갯수 가져오기
        if(totCnt < 1){
            //이력테이블에 차량이 없을 경우 이력테이블 삽입
            FpisCarManageVO detailVO = new FpisCarManageVO();
            //차량 상세정보 가져오기
            detailVO = CarManageService.selectUsr_cars_info_bySeq(carVO);
            detailVO.setRemark("01");
            CarManageService.updateUsrCar(detailVO, "Y");
        }
        // 2017.10.13 이력없는 장기용차 이력 생성기능 안도영대리 로직 반영 끝

        // 해당 장기용차가 계약 유효기간이 365일 이상인지 확인한다. CAR_REG_SEQ
        int period = CarManageService.selectUsrCarsHistory_period(carVO);

        String result = "";
        if(period < 365) { result = "cls04"; }
        else {
            carVO.setBase_year(base_year);
            int orderCntYear = CarManageService.selectRegOperate_orderCntYear(carVO);
            result = ""+orderCntYear;
        }

        res.setContentType("application/text");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out;
        try {
            out = res.getWriter();
            out.write(result);
            out.close();
        } catch (IOException e) {
            System.err.println("/FpisCarManager_getRegOperateCntYear_ajax.do ERROR!!");
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
			cont = cont.replaceAll("\'","&#039");
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

