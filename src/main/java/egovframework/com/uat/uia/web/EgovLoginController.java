package egovframework.com.uat.uia.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.EgovWebUtil;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.com.cmm.service.EgovProperties;
import egovframework.com.cmm.util.EgovUserDetailsHelper;
import egovframework.com.uat.uia.service.EgovLoginService;
import egovframework.com.uss.umt.service.EgovMberManageService;
import egovframework.com.uss.umt.service.MberManageVO;
import egovframework.com.utl.sim.service.EgovClntInfo;
import fpis.common.listener.FpisSessionListener;
import fpis.common.utils.AESCrypto;
import fpis.common.utils.FpisConstants;
import fpis.common.utils.Util;
import fpis.common.vo.FpisLoginLogVO;
import fpis.common.vo.SessionVO;
import twitter4j.internal.org.json.JSONObject;


/**
 * 일반 로그인, 인증서 로그인을 처리하는 컨트롤러 클래스
 * @author 공통서비스 개발팀 박지욱
 * @since 2009.03.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자          수정내용
 *  -------    --------    ---------------------------
 *  2009.03.06  박지욱          최초 생성
 *  2011.8.26    정진오            IncludedInfo annotation 추가
 *  2011.09.07  서준식          스프링 시큐리티 로그인 및 SSO 인증 로직을 필터로 분리
 *  2011.09.25  서준식          사용자 관리 컴포넌트 미포함에 대한 점검 로직 추가
 *  2011.09.27  서준식          인증서 로그인시 스프링 시큐리티 사용에 대한 체크 로직 추가
 *  2011.10.27  서준식          아이디 찾기 기능에서 사용자 리름 공백 제거 기능 추가
 *  </pre>
 */


@Controller
public class EgovLoginController {


	/** 2014.04.10 swyang  로그인시 USRDN값 자동 업데이트 전용 서비스  삭제예정. */
	@Resource(name = "mberManageService")
	private EgovMberManageService mberManageService;

	/** EgovLoginService */
	@Resource(name = "loginService")
	private EgovLoginService loginService;

	/** EgovCmmUseService */
	@Resource(name="EgovCmmUseService")
	private EgovCmmUseService cmmUseService;


	/** EgovMessageSource */
	@Resource(name="egovMessageSource")
	public EgovMessageSource egovMessageSource;

	@Value(value="#{fpis['FPIS.domain']}")
	private String program_domain;

    @Value(value="#{globals['Globals.MainPage']}")
    private String main_page;

	/** log */
	protected static final Log LOG = LogFactory.getLog(EgovLoginController.class);


	/**
	 * 일반(세션) 로그인을 처리한다
	 * @param vo - 아이디, 비밀번호가 담긴 LoginVO
	 * @param request - 세션처리를 위한 HttpServletRequest
	 * @return result - 로그인결과(세션정보)
	 * @exception Exception
	 * 2013.10.16 mgkim 로그인 처리
	 * 2015.10.12 dyahn 로그인 처리시 RSA 암호화/부호화 알고리즘 적용
	 * 2019.11.13 pes 로그인 성공시 휴면계정 -> 정상으로 변경 적용
	 * 2021.01.11 ysw 보안취약점 METHOD POST 만 가능하도록 수정
	 */
	@RequestMapping(value="/uat/uia/actionLogin.do", method=RequestMethod.POST)
	public String actionLogin(@ModelAttribute("loginVO") LoginVO loginVO,
			@RequestParam(value="isPop", required=false) String isPop,
			HttpServletRequest request, HttpServletResponse res,
			ModelMap model) throws Exception,ServletException, IOException
			{

		//2015.10. 12 written by dyahn 모의해킹 보안조치 계정 데이터 평문 RSA 암호화로직 추가
		String securedId = request.getParameter("securedId");
		String securedPassword = request.getParameter("securedPassword");
		String securedPassword2 = request.getParameter("securedPassword2");
		String securedUserSe = request.getParameter("securedUserSe");

		HttpSession session = request.getSession();
		PrivateKey privateKey = (PrivateKey) session.getAttribute("__rsaPrivateKey__");
		//"테스트!!!! 개인키 : " + privateKey
		//privateKey = null;
		session.removeAttribute("__rsaPrivateKey__"); // 키의 재사용을 막는다. 항상 새로운 키를 받도록 강제.

		if (privateKey == null) {
			//throw new RuntimeException("암호화 비밀키 정보를 찾을 수 없습니다.");
			//"암호화 비밀키 정보를 찾을 수 없습니다."
			model.addAttribute("usrStt", "E");
			return "redirect:/userMain.do";
		}

		String username = decryptRsa(privateKey, securedId);
		String password = decryptRsa(privateKey, securedPassword);
		String password2 = decryptRsa(privateKey, securedPassword2);
		String userSe = decryptRsa(privateKey, securedUserSe);


		loginVO.setId(username);
		loginVO.setPassword(password);
		loginVO.setPassword2(password2);
		loginVO.setUserSe(userSe);

		//"---------------LOGIN ID:"+loginVO.getId()
		//"---------------LOGIN SE:"+loginVO.getUserSe()
		// 1. 일반 로그인 처리
		LoginVO resultVO = loginService.actionLogin(loginVO);

        // 2017. 05. 26 written by dyahn
        // admin계정으로 관리자페이지 접속시 허용된 IP주소상에서의 접근이 아닐 경우 접속을 차단.
        String request_ip = Util.getClientIpAddr(request);
        resultVO.setIp(request_ip);


        int isres3 = 2;        // 2017. 06. 05. written by dyahn 허용된 IP접근여부  - 2:접금유무판단필요없음 (기본값 2), 1:일치 , 0:불일치

        String loginYn = "N";

        if(resultVO != null && resultVO.getId() != null && !resultVO.getId().equals("")){

            if(resultVO.getUserSe().equals("GNR") || resultVO.getMber_cls().equals("ADM")){
            //일반사용자 또는 지자체관리자
                loginYn = "Y";

            }else if(resultVO.getMber_cls().equals("SYS")){
            //시스템 관리자의 경우 IP체크
                isres3 = loginService.selectUserPermitCheck(resultVO);

                if(isres3 > 0){
                    loginYn = "Y";

                }else{
                    loginYn = "N";

                }
            }

        }

		FpisLoginLogVO  logVO = new FpisLoginLogVO();
		if (loginYn == "Y") {
			// 2013.10.16 mgkim 로그인 성공
			// 2-1. 로그인 정보를 세션에 저장
			SessionVO SessionVO = null;
			SessionVO = loginService.getSession(loginVO);


			/*2021.01.13 ysw 영구적 쿠키에 대한 중요 세션 정보 포함 처리 : 문제가되는 쿠키삭제*/
			Cookie cookie2 = new Cookie("elevisor_for_j2ee_uid", null);
			cookie2.setMaxAge(0);
			cookie2.setPath("/");
			res.addCookie(cookie2);

			/*로그인 뒤 세션을 초기화 해줍니다.*/
			request.getSession().invalidate();

            SessionVO.setLoginSe(resultVO.getLoginSe());
            SessionVO.setHabbit(resultVO.getIp());
            SessionVO.setEnvnt_date(resultVO.getEnvnt_date());

			request.getSession().setAttribute("loginVO"  , resultVO);
			request.getSession().setAttribute("SessionVO", SessionVO);
			request.getSession().setAttribute("tempUserSe", loginVO.getUserSe()); // 2014.11.12 mgkim 관리자 외부망 로그인 임시기능
			// 로그인 로그 기록...
			//SessionVO SVO = (SessionVO )request.getSession().getAttribute("SessionVO");
			SessionVO SVO = SessionVO;
			logVO.setMber_id(SVO.getUser_id());
			logVO.setJob_cls("LI");
			logVO.setMber_cls(SVO.getMber_cls());
			logVO.setMber_nm(SVO.getUser_name());
			logVO.setUsr_mst_key(SVO.getUsr_mst_key());
			logVO.setOrd_cnt(0);
			// 2013.10.07 mgkim
			//2015. 10. 16 written by dyahn 모의해킹진단결과 중요정보 관련 로그 삭제


			//2017. 05. 30 written by dyahn 로그인 실패횟수 초기화
			loginVO.setLogin_fail_cnt(0);
            loginService.updateLoginFailCount(loginVO);


			// TODO 로그인 로그
			loginService.InsertLoginLog(logVO); // 2013.11.28 mgkim 로그인시 에러나는데 여기인지 테스트
			//loginService.recordLoginSession(logVO.getMber_id()); //로그인/비로그인 모두 세션관리함으로 해당부분은 Filter로 이동
			//Session Filter Login User Add
			FpisSessionListener.getInstance().setLoginSession(request.getSession());

			//2015. 09. 07 written by dyahn 차량관리작성등록으로 분기
			String car_regFlag = "N";
			if(request.getParameter("car_regFlag") != null && request.getParameter("car_regFlag") != ""){
				car_regFlag = request.getParameter("car_regFlag");
			}



			if("Y".equals(car_regFlag) || car_regFlag == "Y"){
				return "redirect:/online/FpisCarManagerRegist2.do";
			}
			else{
				return "redirect:/uat/uia/actionMain.do";
			}

		} else {                                                                                     // 2013.10.16 mgkim 로그인 실패
			LoginVO tempLoginVO = new LoginVO();
			int isres1     = loginService.actionLoginIsUser(loginVO);
			int isres2     = loginService.actionLoginIsUserPwd(loginVO);

			//"로그인 실패   isres1 : " + isres1
			//"로그인 실패   isres2 : " + isres2
			//"로그인 실패   isres3 : " + isres3

            //2017. 05. 30 written by dyahn 지정시간 로그인실패 5회시 계정잠금기능구현
			int loginFailCount = 0;

            /*	로그인 실패시 resulVO에 값 없음..그래서 로그인 실패시에 정상동작 안함.
             * if( resultVO != null && resultVO.getId() != null && !resultVO.getId().equals("") &&
                !resultVO.getUserSe().equals("GNRADM") && !resultVO.getUserSe().equals("ADM")){*/
        	if( loginVO != null && loginVO.getId() != null && !loginVO.getId().equals("") &&
                    !loginVO.getUserSe().equals("GNRADM") && !loginVO.getUserSe().equals("ADM")){

                if(isres1 != 0 && isres2 == 0 && (isres3 == 2 || isres3 == 1)){
                    //로그인실패횟수 카운트쿼리분기
                    loginFailCount = loginService.selectLoginFailCount(loginVO);

                    if(loginFailCount >= 5){
                        //5이상이면 상태값 변경(A:회원가입신청상태, C:업태변경신청상태, D:시스템사용정지상태, F:회원가입반려상태, P:회원가입승인상태, R:회원가입재신청상태)
                        loginService.updateMber_sttus(loginVO);
                        //"Login try count is over 5 And Your Account was loacked!!!"
                    }else{
                    	//로그인 실패 횟수 0회 문제로 수정( 다른 부분은 문제가 없으나 selectLoginFailCount에서 결과값-1로 리턴줘서 최초 1회는 실패횟수 0으로 출력되는문제 수정)
                    	if(loginFailCount == -1){
                    		loginFailCount++;
                    	}
                        //5이하면 카운트
                    	if(loginFailCount >= 0 && loginFailCount < 2147483647){
                    		loginFailCount++;
                    	}
                        loginVO.setLogin_fail_cnt(loginFailCount);
                        loginService.updateLoginFailCount(loginVO);
                        //"Increase in Your Login fail Count " + loginVO.getLogin_fail_cnt() +"/5"
                    }
                }
            }

            String usrStt  = loginService.actionLoginGetStt(loginVO);
            String UniqID  = loginService.actionLoginGetUniqID(loginVO);
            tempLoginVO.setId(loginVO.getId());
            tempLoginVO.setUniqId(UniqID);


            model.addAttribute("try", "fail");
            model.addAttribute("isres1", isres1);
            model.addAttribute("isres2", isres2);
            model.addAttribute("isres3", isres3);    //2017. 06. 05 추가

            model.addAttribute("usrStt", usrStt);
            model.addAttribute("loginFailCount", loginFailCount);
            request.getSession().setAttribute("tempLoginVO"  , tempLoginVO);

			SessionVO tempSessionVO = null;
			tempSessionVO = loginService.getSession(loginVO);
			request.getSession().setAttribute("tempSessionVO", tempSessionVO);  // 회원정보수정 및 재가입 요청을 위한 임시 세션


			if(loginVO.getUserSe().equals("GNR")){
				// 2013.11.20 mgkim 경로 변경(2013.11.5 별도 로그인 페이지 제거 요청)
				return "redirect:/userMain.do";
			} else {
				return "redirect:/chownmolitfpmsonlyfpisLogin.do";
			}
		}
	}



	/**
	 * 2015. 10. 20 written by dyahn 로그인 창 출력 후 암호화 개인키 생성 통신
	 */
	@RequestMapping(value="/uat/uia/genRsaKey_ajax.do")
	public void genRsaKey_ajax(HttpServletRequest req, HttpServletResponse res, ModelMap model) throws Exception {

		//2015.10. 12 written by dyahn 모의해킹 보안조치 계정 데이터 평문 RSA 암호화 로직 시작
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

		//2015.10. 12 written by dyahn 모의해킹 보안조치 계정 데이터 평문 RSA 암호화 로직 끝

		JSONObject json = new JSONObject();
		json.put("publicKeyModulus",publicKeyModulus);
		json.put("publicKeyExponent",publicKeyExponent);
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}


	private String decryptRsa(PrivateKey privateKey, String securedValue) throws Exception {

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		//Cipher cipher = Cipher.getInstance("RSA");
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");


		byte[] encryptedBytes = hexToByteArray(securedValue);
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
		String decryptedValue = new String(decryptedBytes, "utf-8"); // 문자 인코딩 주의.
		return decryptedValue;
	}


	/**
	 * 16진 문자열을 byte 배열로 변환한다.
	 */
	public static byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() % 2 != 0) {
			return new byte[]{};
		}

		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < hex.length(); i += 2) {
			byte value = (byte)Integer.parseInt(hex.substring(i, i + 2), 16);
			bytes[(int) Math.floor(i / 2)] = value;
		}
		return bytes;
	}

	/**
	 * 공인인증서 로그인을 처리한다
	 * @param vo - 아이디, 비밀번호가 담긴 LoginVO
	 * @param request - 세션처리를 위한 HttpServletRequest
	 * @return result - 로그인결과(세션정보)
	 * @exception Exception
	 * 2013.12.20 mgkim 사용자공인인증 확인전용 조건에 권한구분 추가.
	 */
	@RequestMapping(value="/uat/uia/actionNPkiLogin.do")
	public String actionNPkiLogin(HttpServletRequest request,
			ModelMap model) throws Exception {

		LoginVO         loginVO = new LoginVO();
		SessionVO     SessionVO = null;
		FpisLoginLogVO    logVO = new FpisLoginLogVO();
		LoginVO       resultVO1 =  null;
		LoginVO       resultVO2 =  null;


		String aResult = request.getParameter("aResult");
		String aVidMsg = request.getParameter("aVidMsg");

		loginVO = Util.getParsePKIData(aResult, aVidMsg);

		if(loginVO.getUsrdn() == null || loginVO.getUsrvid() == null){
			//"VID 값으로 사용자정보 미확인"
			model.addAttribute("try", "npki_fail");
			return "redirect:/userMain.do";
		}

		String userSe = request.getParameter("userSe");

		loginVO.setChkcls("DN");
		//"공인인증 로그인 mgkim userSe : " + userSe
		loginVO.setUserSe(userSe);  // 2014.12.16 mgkim 오류수정 // 2014.11.12 mgkim 관리자 외부망 로그인 임시기능

		//        int inVidCnt = loginService.loginNPKICheckVID(loginVO); //기존에 USRDN으로 검사
		/*2014.04.10 로그인시 VID값으로 검사 및 USRDN 자동 업데이트 한달 뒤 삭제예정*/
		int inVidCnt = loginService.loginNPKICheckVID_swyang(loginVO);
		if(inVidCnt > 0){
			MberManageVO mberVO     =  new MberManageVO();
			mberVO.setUsrDN(loginVO.getUsrdn()); // NPKI
			mberVO.setUsrVID(loginVO.getUsrvid()); // NPKI
			mberManageService.updateUsrVID(mberVO);
		}

		/*2014.04.10 로그인시 VID값으로 검사 및 USRDN 자동 업데이트 한달 뒤 삭제예정  끝*/

		if(inVidCnt > 0) {
			resultVO1 = loginService.getUsrIdByNPKI(loginVO);
			resultVO1.setUserSe(userSe);

			// 1. 일반 로그인 처리
			resultVO2 = loginService.actionLogin(resultVO1);

			if (resultVO2 != null && resultVO2.getId() != null && !resultVO2.getId().equals("")) {
				// 2-1. 로그인 정보를 세션에 저장
				request.getSession().setAttribute("loginVO", resultVO2);
				request.getSession().setAttribute("SessionVO", loginService.getSession(resultVO2));
				request.getSession().setAttribute(FpisConstants.SESSION_LOGIN_BY_POP, FpisConstants.VALUE_LOGIN_BY_POP);
				request.getSession().setAttribute("tempUserSe", loginVO.getUserSe()); // 2014.11.12 mgkim 관리자 외부망 로그인 임시기능

				// 로그인 로그 기록...
				SessionVO = (SessionVO )request.getSession().getAttribute("SessionVO");

				logVO.setMber_id(SessionVO.getUser_id());
				logVO.setJob_cls("LI");
				logVO.setMber_cls(SessionVO.getMber_cls());
				logVO.setMber_nm(SessionVO.getUser_name());
				logVO.setUsr_mst_key(SessionVO.getUsr_mst_key());
				logVO.setOrd_cnt(0);
				// 2013.10.07 mgkim

				// TODO 공인인증 로그인 로그
				loginService.InsertLoginLog(logVO);  // 2013.11.28 mgkim 로그인시 에러나는데 여기인지 테스트
				loginService.recordLoginSession(logVO.getMber_id());
				return "redirect:/uat/uia/actionMain.do";

			} // 로그인 성공
		}
		// 암호화딘 vid가 실패한 경우 , 원본vid로 로그인 다시 시도..

		// TODO NPKI 로그인
		model.addAttribute("try", "npki_fail");
		return "redirect:/userMain.do";
	}

	/**
	 * 2014.01.13 mgkim 사용자 공인인증 회원가입 (전용기능으로 분리작업) (ajax 모듈)
	 * 2014.01.14 mgkim 이미가입된 공인인증서 중복가입 방지 처리
	 */
	@RequestMapping(value="/uat/uia/actionNPkiJoin.do")
	public void actionNPkiJoin(HttpServletResponse res, HttpServletRequest req,
			@RequestParam(value="aResult") String aResult,
			@RequestParam(value="aVidMsg") String aVidMsg,
			@RequestParam(value="id") String id) throws Exception {
		//"usr_sys npki join action - 사용자 공인인증 회원가입 액션"
		LOG.debug("usr_sys npki join action - 사용자 공인인증 회원가입 액션");

		LoginVO loginVO = Util.getParsePKIData(aResult, aVidMsg);

		if(loginVO.getUsrdn() == null || loginVO.getUsrvid() == null || id == null){
			JSONObject json = new JSONObject();
			json.put("result",-1);

			res.setContentType("application/json");
			res.setCharacterEncoding("UTF-8");;
			PrintWriter out = res.getWriter();

			out.write(json.toString());
			out.close();
		}else{

			loginVO.setChkcls("DN");
			loginVO.setUserSe("GNR");
			loginVO.setId(id);

			//"공인인증 회원가입(ajax) usrVID : " + loginVO.getUsrvid()
			//"공인인증 회원가입(ajax) usrDN : " + loginVO.getUsrdn()
			//"공인인증 회원가입(ajax) id : " + id
			//        int inVidCnt = loginService.loginNPKICheckVID(loginVO);
			int inVidCnt = loginService.loginNPKICheckVID_swyang(loginVO);

			if(inVidCnt > 0) { // 가입된 공인인증서가 있는경우(갱신처리)
				loginService.updateUsrVid(loginVO);
			}else{ // 가입된 공인인증서가 없는경우(등록처리)
				loginService.updateUsrVid(loginVO);
			}


			JSONObject json = new JSONObject();
			json.put("result",1);

			res.setContentType("application/json");
			res.setCharacterEncoding("UTF-8");;
			PrintWriter out = res.getWriter();

			out.write(json.toString());
			out.close();
		}
	}


	/**
	 * 2013.12.09 mgkim
	 * 관리자 공인인증서 로그인을 처리한다
	 */
	@RequestMapping(value="/uat/uia/actionNPkiLogin_adm.do")
	public String actionNPkiLogin_adm(HttpServletRequest request,
			ModelMap model) throws Exception {
		//"/uat/uia/actionNPkiLogin_adm.do ===> 관리자 공인인증 로그인 처리"

		LoginVO         loginVO = new LoginVO();
		//SessionVO     SessionVO  = null;

		LoginVO       resultVO1 =  null;
		LoginVO       resultVO2 =  null;


		String aResult = request.getParameter("aResult");
		String aVidMsg = request.getParameter("aVidMsg");
		String aUsrVid = "";		
		loginVO = Util.getParsePKIData(aResult, aVidMsg);		
		aUsrVid = loginVO.getUsrvid();
		//aUsrVid = AESCrypto.getInstance().encrypt("6609071094019");//개발용.

		/*
		 * 2022.06.11 jwchoi 공인인증서 로그인 시 vid를 변환시킴
		 * RequestWrapper의 xss_secure 함수로 돌렸으나, 잘못 변환되어 따로 생성
		 */		
		aUsrVid = XSS_secure(aUsrVid);		
		loginVO.setUsrvid(aUsrVid);
		//loginVO.setUsrdn("");//개발용. 

		if(loginVO.getUsrdn() == null || loginVO.getUsrvid() == null){
		//"VID 값으로 사용자정보 미확인"
			model.addAttribute("try", "npki_fail");
			return "redirect:/userMain.do";
		}

		//"---------------  NOR vidNum : " + usrVID
		//

		//"---------------  ENC vidNum : " + usrVID

		loginVO.setChkcls("DN");
		loginVO.setUserSe("ADM");
		//"공인인증 로그인 usrVID : " + loginVO.getUsrvid()
		//"공인인증 로그인 usrDN : " + loginVO.getUsrdn()
		int inVidCnt = loginService.loginNPKICheckVID(loginVO);
		if(inVidCnt > 0){
			MberManageVO mberVO     =  new MberManageVO();
			mberVO.setUsrDN(loginVO.getUsrdn()); // NPKI
			mberVO.setUsrVID(loginVO.getUsrvid()); // NPKI
			mberManageService.updateUsrVID(mberVO);
		}

		if(inVidCnt > 0) {

			resultVO1 = loginService.getUsrIdByNPKI(loginVO);

			// 1. 일반 로그인 처리
			//jwchoi 공인인증서 로그인인 용 password2 임시값 지정
			resultVO1.setPassword2("1234");
			resultVO2 = loginService.actionLogin(resultVO1);
			
			if (resultVO2 != null && resultVO2.getId() != null && !resultVO2.getId().equals("")) {
				// 2-1. 로그인 정보를 세션에 저장
				request.getSession().setAttribute("loginVO", resultVO2);
				request.getSession().setAttribute("SessionVO", loginService.getSession(resultVO2));
				request.getSession().setAttribute(FpisConstants.SESSION_LOGIN_BY_POP, FpisConstants.VALUE_LOGIN_BY_POP);

				return "redirect:/uat/uia/actionMain.do";
			} // 로그인 성공
		}

		// TODO NPKI 로그인
		//"VID 값으로 사용자정보 미확인"
		if(resultVO1!=null){
			model.addAttribute("try", "npki_fail2");
		}else{
			model.addAttribute("try", "npki_fail");
		}

		return "redirect:/chownmolitfpmsonlyfpisLogin.do";
	}



	/**
	 * 로그인 후 메인화면으로 들어간다
	 * @param
	 * @return 로그인 페이지
	 * @exception Exception
	 */
	@RequestMapping(value="/uat/uia/actionMain.do")
	public String actionMain(
			HttpServletRequest request,
			HttpServletResponse response,
			ModelMap model)
					throws Exception {
		// 1. Spring Security 사용자권한 처리
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		if(!isAuthenticated) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "egovframework/com/uat/uia/EgovLoginUsr";
		}

		// 3. 메인 페이지 이동
		String str_MAIN_PAGE = EgovWebUtil.removeCRLF(main_page);
		String str_main_page = EgovWebUtil.removeCRLF(main_page);
		LOG.debug("Globals.MAIN_PAGE > " + str_MAIN_PAGE);
		LOG.debug("main_page > " + str_main_page);
		if (main_page.startsWith("/")) {
			return "forward:" + main_page;
		} else {
			return main_page;
		}
	}

	/**
	 * 로그아웃한다.
	 * @return String
	 * @exception Exception
	 */
	@RequestMapping(value="/uat/uia/actionLogout.do")
	public String actionLogout(HttpServletRequest request, HttpServletResponse response, ModelMap model)
			throws Exception {
		/*String userIp = EgovClntInfo.getClntIP(request);

        // 1. Security 연동
        return "redirect:/j_spring_security_logout";*/

		String returnURL="";
		LoginVO   loginVO  = (LoginVO)request.getSession().getAttribute("loginVO");
		SessionVO SessionVO = (SessionVO )request.getSession().getAttribute("SessionVO");

		if(loginVO.getUserSe().equals("GNR"))
			returnURL = "redirect:/userMain.do";
		else
			returnURL = "redirect:/chownmolitfpmsonlyfpisLogin.do";


		// 로그아웃 로그 기록...
		/*FpisLoginLogVO  logVO = new FpisLoginLogVO();

		logVO.setMber_id(SessionVO.getUser_id());
		logVO.setJob_cls("LO");
		logVO.setMber_cls(SessionVO.getMber_cls());
		logVO.setMber_nm(SessionVO.getUser_name());
		logVO.setUsr_mst_key(SessionVO.getUsr_mst_key());
		logVO.setOrd_cnt(0);

		loginService.InsertLogOutLog(logVO);  // 2013.11.28 mgkim 로그인시 에러나는데 여기인지 테스트
		 */
		insertLogoutRecord(SessionVO.getUser_id(), "LO", SessionVO.getMber_cls(), SessionVO.getUser_name(), SessionVO.getUsr_mst_key());

		loginService.deleteLoginSession(SessionVO.getUser_id());

		request.getSession().setAttribute("loginVO"    , null);
		request.getSession().setAttribute("SessionVO"  , null);
		request.getSession().setAttribute("tempUserSe"  , null);   // 2014.12.16 mgkim 오류수정 // 2014.11.12 mgkim 관리자 외부망 로그인 임시기능

		request.getSession().removeAttribute(FpisConstants.SESSION_KEY);
		request.getSession().removeAttribute(FpisConstants.SESSION_LOGIN_BY_POP);
		request.getSession().removeAttribute("tempUserSe");   // 2014.12.16 mgkim 오류수정 // 2014.11.12 mgkim 관리자 외부망 로그인 임시기능

		//"-----------> user logout url:"+returnURL

		return returnURL;
	}


	/**
	 * 일정시간뒤 자동로그아웃 (15분) - 신고주체(GNR) 전용기능
	 * 2015.02.09 mgkim 최초구현
	 */
	@RequestMapping(value="/uat/uia/autoLogout.do")
	public String autoLogout(HttpServletRequest request, ModelMap model) throws Exception {
		//LoginVO   loginVO  = (LoginVO)request.getSession().getAttribute("loginVO");
		SessionVO SessionVO = (SessionVO )request.getSession().getAttribute("SessionVO");

		// 로그아웃 로그 기록...
		/*FpisLoginLogVO  logVO = new FpisLoginLogVO();
		logVO.setMber_id(SessionVO.getUser_id());
		logVO.setJob_cls("LO");
		logVO.setMber_cls(SessionVO.getMber_cls());
		logVO.setMber_nm(SessionVO.getUser_name());
		logVO.setUsr_mst_key(SessionVO.getUsr_mst_key());
		//logVO.setOrd_cnt(0);

		FpisSessionListener.getInstance().setLogoutSession(request.getSession());
		loginService.InsertLogOutLog(logVO);  // 2013.11.28 mgkim 로그인시 에러나는데 여기인지 테스트
		 */
		insertLogoutRecord(SessionVO.getUser_id(), "LO", SessionVO.getMber_cls(), SessionVO.getUser_name(), SessionVO.getUsr_mst_key());


		request.getSession().setAttribute("loginVO"    , null);
		request.getSession().setAttribute("SessionVO"  , null);
		request.getSession().setAttribute("tempUserSe"  , null);   // 2014.12.16 mgkim 오류수정 // 2014.11.12 mgkim 관리자 외부망 로그인 임시기능

		request.getSession().removeAttribute(FpisConstants.SESSION_KEY);
		request.getSession().removeAttribute(FpisConstants.SESSION_LOGIN_BY_POP);
		request.getSession().removeAttribute("tempUserSe");   // 2014.12.16 mgkim 오류수정 // 2014.11.12 mgkim 관리자 외부망 로그인 임시기능

		loginService.deleteLoginSession(SessionVO.getUser_id());

		model.addAttribute("loginState", "autoLogout");

		return "redirect:/userMain.do";
	}

	/**
	 * 세션시간 연장기능 - 신고주체(GNR) 전용기능
	 * 2015.02.09 mgkim 최초구현
	 */
	@RequestMapping(value="/uat/uia/sessionDefault_ajax.do")
	public void sessionDefault(HttpServletRequest req, HttpServletResponse res, ModelMap model) throws Exception {

		/*2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사*/
		String refer_domain = req.getHeader("referer");

		if(!refer_domain.contains(program_domain)) {
			return ;
		}

		/*Enumeration se = req.getSession().getAttributeNames();
        while(se.hasMoreElements()){
            String getse = se.nextElement()+"";
        }*/
		//"remain session time : "+req.getSession().getMaxInactiveInterval()
		

		//"reset and remain session time : "+req.getSession().getMaxInactiveInterval()
		JSONObject json = new JSONObject();
		json.put("state","ok");
		
		/* 2022.08.23 jwchoi 웹취약점 조치 로그인 유효시간 서버에서 가져오기 */
		int defaultmin = 60 * 10; // 10분
		req.getSession().setMaxInactiveInterval(defaultmin) ;
		int minute = req.getSession().getMaxInactiveInterval();
		json.put("minute", minute / 60);
		json.put("second", 0);

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	/**
	 * 세션시간 24시간 세팅 - 신고주체(GNR) 전용기능
	 * 2015.07.10 mgkim 최초구현
	 */
	@RequestMapping(value="/uat/uia/sessionOneDay_ajax.do")
	public void sessionOneDay(HttpServletRequest req, HttpServletResponse res, ModelMap model) throws Exception {

		/*Enumeration se = req.getSession().getAttributeNames();
        while(se.hasMoreElements()){
            String getse = se.nextElement()+"";
        }*/
		// 2015.02.09 mgkim 세션 기본시간 60분 : 3600
		int defaultSec = 60 * 60 * 24; // 세션의 시간을 24시간으로 늘려준다.
		req.getSession().setMaxInactiveInterval(defaultSec);
		JSONObject json = new JSONObject();
		json.put("state","ok");

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	/**
	 * 콜센터 및 운영자 테스트 계정에 대한 세션연장 타이머 기능 해제
	 *
	 */

	@RequestMapping(value="/uat/uia/isManager_ajax.do")
	public void isManager(HttpServletRequest req, HttpServletResponse res, ModelMap model) throws Exception {

		//2021.01.12 ysw CSRF 방지를 위한 http 헤더 검사
		String refer_domain = req.getHeader("referer");

		if(!refer_domain.contains(program_domain)) {
			JSONObject json = new JSONObject();
			json.put("state","f");
			res.setContentType("application/json");
			res.setCharacterEncoding("UTF-8");;
			PrintWriter out = res.getWriter();
			out.write(json.toString());
			out.close();
		}

		JSONObject json = new JSONObject();
		SessionVO session = (SessionVO)req.getSession().getAttribute("SessionVO");
		
		/* 2022.08.23 jwchoi 웹취약점 조치 로그인 유효시간 서버에서 가져오기 */
		int defaultmin = 60 * 10; // 10분
		req.getSession().setMaxInactiveInterval(defaultmin) ;
		int minute = req.getSession().getMaxInactiveInterval();
		json.put("minute", minute / 60);
		json.put("second", 0);

		String currentUserId = session.getUser_id();

		if(currentUserId == null || currentUserId.equals("")){
			json.put("state","f");
		}

		String managerList = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.ManagerConfPath"), "FPIS.manager.users");

		if(managerList == null || managerList.equals("")){
			json.put("state","f");
		}else{
			String [] managers = managerList.split("[,]");
			if(managers == null || managers.length == 0){
				json.put("state","f");
			}else{
				for(int i = 0 ; i < managers.length ; i++) {
					if(managers[i].equals(currentUserId)){
						json.put("state","t");
						break;
					}else{
						json.put("state","f");
					}
				}
			}
		}

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");;
		PrintWriter out = res.getWriter();
		out.write(json.toString());
		out.close();
	}

	/**
	 * 개발 시스템 구축 시 발급된 GPKI 서버용인증서에 대한 암호화데이터를 구한다.
	 * 최초 한번만 실행하여, 암호화데이터를 EgovGpkiVariables.js의 ServerCert에 넣는다.
	 * @return String
	 * @exception Exception
	 */
	@RequestMapping(value="/uat/uia/getEncodingData.do")
	public void getEncodingData()
			throws Exception {

		/*
        X509Certificate x509Cert = null;
        byte[] cert = null;
        String base64cert = null;
        try {
            x509Cert = Disk.readCert("/product/jeus/egovProps/gpkisecureweb/certs/SVR1311000011_env.cer");
            cert = x509Cert.getCert();
            Base64 base64 = new Base64();
            base64cert = base64.encode(cert);
            log.info("+++ Base64로 변환된 인증서는 : " + base64cert);

        } catch (GpkiApiException e) {
            e.printStackTrace();
        }
		 */
	}

	/**
	 * 로그아웃을 기록한다.(브라우저 종료시 로그아웃 및 일반로그아웃을 기록하여 신뢰도를 측정한다)
	 * @return String
	 * @exception Exception
	 */
	@RequestMapping(value="/uat/uia/actionLogoutRecord.do")
	public void actionLogoutRecord(HttpServletRequest request, ModelMap model)
			throws Exception {

		/*String userIp = EgovClntInfo.getClntIP(request);

        // 1. Security 연동
        return "redirect:/j_spring_security_logout";*/

		SessionVO SessionVO = (SessionVO )request.getSession().getAttribute("SessionVO");

		if(SessionVO == null){
			//"SessionVO is null.... 브라우저 종료체크..로그인 사용자 아님."
		}else{
			// 로그아웃 로그 기록...
			insertLogoutRecord(SessionVO.getUser_id(), "BS", SessionVO.getMber_cls(), SessionVO.getUser_name(), SessionVO.getUsr_mst_key());
		}
	}

	private void insertLogoutRecord(String user_id, String mode, String mber_cls, String mber_nm, String usr_mst_key) throws Exception{
		FpisLoginLogVO  logVO = new FpisLoginLogVO();

		logVO.setMber_id(user_id);
		logVO.setJob_cls(mode);
		logVO.setMber_cls(mber_cls);
		logVO.setMber_nm(mber_nm);
		logVO.setUsr_mst_key(usr_mst_key);
		logVO.setOrd_cnt(0);

		loginService.InsertLogOutLog(logVO);  // 2013.11.28 mgkim 로그인시 에러나는데 여기인지 테스트
	}
	
	/*
	 * 2022.06.11 jwchoi
	 * GPKI 공인인증서 로그인시 vid(주민번호)를 불러올 때 db에 이스케이프문자로 저장된 데이터와 맞지 않은 오류 발생
	 * RequestWrapper 의 xss_secure 함수로 둘렸으나, 잘못 변환되어 여기에 따로 생성
	 */
	private String XSS_secure (String param) {		
        if(param.indexOf("\'") != -1){ param= param.replaceAll("\'", "&apos;"); }
  		if(param.indexOf("<") != -1){ param= param.replaceAll("<", "&lt;"); }
        if(param.indexOf(">") != -1){ param= param.replaceAll(">", "&gt;"); }
        if(param.indexOf("&") != -1){ param= param.replaceAll("&", "&amp;"); }
        if(param.indexOf("\'") != -1){ param= param.replaceAll("\'", "&apos;"); }
  		if(param.indexOf("<") != -1){ param= param.replaceAll("<", "&lt;"); }
        if(param.indexOf(">") != -1){ param= param.replaceAll(">", "&gt;"); }
        if(param.indexOf("&") != -1){ param= param.replaceAll("&", "&amp;"); }       
        return param;
	}
	 
}