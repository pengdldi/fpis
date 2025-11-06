package egovframework.com.cmm.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import egovframework.com.cmm.service.EgovFileMngService;
import egovframework.com.cmm.service.FileVO;
import egovframework.com.cmm.util.EgovUserDetailsHelper;
import egovframework.com.cop.bbs.service.BoardVO;
import egovframework.com.cop.bbs.service.EgovBBSManageService;
import fpis.common.service.CommonGetInfoService;
import fpis.common.utils.FpisConstants;
import fpis.common.vo.SessionVO;
import fpis.reg.RegVO;


/**
 * 파일 다운로드를 위한 컨트롤러 클래스
 * 
 * @author 공통서비스개발팀 이삼섭
 * @since 2009.06.01
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.3.25  이삼섭          최초 생성
 *
 * Copyright (C) 2009 by MOPAS  All right reserved.
 *      </pre>
 */
@Controller
public class EgovFileDownloadController {

	private static final Logger logger = Logger.getLogger(EgovFileDownloadController.class);



	@Value(value = "#{fpis['FPIS.fpis_file']}")
	private String fpis_file_path;

	@Value(value = "#{fpis['FPIS.upload_path_comStatCarMinData']}")
	private String upload_path_comStatCarMinData;

	@Value(value = "#{fpis['FPIS.upload_path_sysCompTempFile']}")
	private String upload_path_sysCompTempFile;
	
	@Value(value = "#{fpis['FPIS.upload_path_sysCompNewFile']}")
	private String upload_path_sysCompNewFile; //운수사업자 관리 샘플 파일 추가

	@Value(value = "#{fpis['FPIS.upload_path_usrCarZip']}")
	private String upload_path_usrCarZip;

	@Value(value = "#{fpis['FPIS.upload_path_modify']}")
	private String upload_path_modify;


	@Value(value = "#{globals['Globals.fpisFilePath']}")
	private String defaultFpisFilePath;

	@Value(value = "#{globals['Globals.majarStatFilePath']}")
	private String majarStatFilePath;

	@Value(value = "#{globals['Globals.fpisBaseFilePath']}")
	private String fpisBaseFilePath;

	@Resource(name = "EgovFileMngService")
	private EgovFileMngService fileService;

	// 사용자 정보 서비스
	@Resource(name = "CommonGetInfoService")
	private CommonGetInfoService commonGetInfoService;


	/* 2021.03.10 ysw 게시판 서비스 추가 */
	@Resource(name = "EgovBBSManageService")
	private EgovBBSManageService bbsMngService;
	
	//대량실적신고 에러파일 경로
	@Value(value="#{globals['Globals.orderErrorFilePath']}")
	private String errorFilePath;

	/*
	 * private static final Logger LOG =
	 * Logger.getLogger(EgovFileDownloadController.class.getName());
	 */

	/**
	 * 브라우저 구분 얻기.
	 *
	 * @param request
	 * @return
	 */
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

	/**
	 * Disposition 지정하기.
	 *
	 * @param filename
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void setDisposition(String filename, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String browser = getBrowser(request);

		String dispositionPrefix = "attachment; filename=";
		String encodedFilename = null;

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
			// throw new RuntimeException("Not supported browser");
			throw new IOException("Not supported browser");
		}

		response.setHeader("Content-Disposition", dispositionPrefix + encodedFilename);

		if ("Opera".equals(browser)) {
			response.setContentType("application/octet-stream;charset=UTF-8");
		}
	}

	/**
	 * 첨부파일로 등록된 파일에 대하여 다운로드를 제공한다.
	 *
	 * @param commandMap
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping(value = "/cmm/fms/FileDown.do")
	public void cvplFileDownload(Map<String, Object> commandMap, HttpServletRequest request,
			HttpServletResponse response, BoardVO boardVO) throws Exception {

		String atchFileId = (String) commandMap.get("atchFileId");
		String fileSn = (String) commandMap.get("fileSn");

		EgovUserDetailsHelper.isAuthenticated();

		// if(isAuthenticated) {

		/* 2021.03.10 ysw 게시기간이 지났으면 다운로드 불가처리. */
		BoardVO vo = bbsMngService.selectBoardArticle(boardVO);
		if ("N".equals(vo.getNtce_yn())) {
			return;
		}
		// 가져온 atchFileId와 전송된 atchFileId가 같은지 검사 ...
		// 이미 지난것인지 검사... bbs 셀렉트 쿼리에 게시기간 지났는지 판별하는 함수 추가 .
		
		/* 2023.02.23 jwchoi 웹취약점 조치 - 파일 다운로드 시 파일이름 비교 */
		if (!atchFileId.equals(vo.getAtchFileId())) {
			return;
		}

		FileVO fileVO = new FileVO();
		fileVO.setAtchFileId(atchFileId);
		fileVO.setFileSn(fileSn);
		FileVO fvo = fileService.selectFileInf(fileVO);

		File uFile = new File(fvo.getFileStreCours(), fvo.getStreFileNm());
		int fSize = (int) uFile.length();

		if (fSize > 0) {
			String mimetype = "application/x-msdownload";

			// response.setBufferSize(fSize); // OutOfMemeory 발생
			response.setContentType(mimetype);
			// response.setHeader("Content-Disposition",
			// "attachment; filename=\"" +
			// URLEncoder.encode(fvo.getOrignlFileNm(), "utf-8") + "\"");
			setDisposition(fvo.getOrignlFileNm(), request, response);
			response.setContentLength(fSize);

			/* 2021.01.11 ysw 정보노출 보안처리 */
			response.setHeader("Cache-Control", "no-store");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
			if (request.getProtocol().equals("HTTP/1.1")) {
				response.setHeader("Cache-Control", "no-cache");
			}


			/*
			 * FileCopyUtils.copy(in, response.getOutputStream()); in.close();
			 * response.getOutputStream().flush(); response.getOutputStream().close();
			 */
			BufferedInputStream in = null;
			BufferedOutputStream out = null;

			try {
				in = new BufferedInputStream(new FileInputStream(uFile));
				out = new BufferedOutputStream(response.getOutputStream());

				FileCopyUtils.copy(in, out);
				out.flush();
			} catch (FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			}
		}
	}

	/*
	 * 2013.09.12 GNT-mgkim 시스템의 양식파일등의 일반 다운로드 기능 2013.10.02 mgkim 한글경로 / 한글파일 에러 수정 fileDir : 파일의
	 * 디렉토리 경로 fileName : 파일명 2014.11.06 컨트롤러 주석처리.
	 */
	// @RequestMapping(value = "/cmm/fms/FpisFileDown.do")
	public void FpisFileDownload(Map<String, Object> commandMap, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String fileDir = (String) commandMap.get("fileDir");
		String fileName = (String) commandMap.get("fileName");

		// fileDir = new String (fileDir.getBytes("8859_1"),"UTF-8"); // 2013.10.02 mgkim 한글 깨짐.
		// 개발서버용
		// fileName = new String (fileName.getBytes("8859_1"),"UTF-8"); // 2013.10.02 mgkim 한글 깨짐.
		// 개발서버
		fileDir = new String(fileDir.getBytes("KSC5601"), "EUC-KR"); // 2013.12.30 mgkim 운영서버 인코딩
		fileName = new String(fileName.getBytes("KSC5601"), "EUC-KR"); // 2013.12.30 mgkim 운영서버 인코딩

		File uFile = new File(fileDir, fileName);
		int fSize = (int) uFile.length();

		if (fSize > 0) {
			String mimetype = "application/x-msdownload";

			// response.setBufferSize(fSize); // OutOfMemeory 발생
			response.setContentType(mimetype);
			setDisposition(fileName, request, response);
			response.setContentLength(fSize);

			BufferedInputStream in = null;
			BufferedOutputStream out = null;

			try {
				in = new BufferedInputStream(new FileInputStream(uFile));
				out = new BufferedOutputStream(response.getOutputStream());

				FileCopyUtils.copy(in, out);
				out.flush();
			} catch (FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			}
		}
	}


	/*
	 * 2014.07.28 양상완 파일이름만 post방식으로 넘기고 나머지는 여기서 cls로 한다. fileName : 파일명
	 */
	@RequestMapping(value = "/cmm/fms/FpisFileDown_sw.do")
	public void FpisFileDownload_sw(Map<String, Object> commandMap, @ModelAttribute RegVO vo,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		String fileName = (String) commandMap.get("fileName");
		String fileCls = request.getParameter("fileCls");
		
		// fileName = new String (fileName.getBytes("8859_1"),"UTF-8"); // 2013.10.02 mgkim 한글 깨짐.
		// 개발서버
		fileName = new String(fileName.getBytes("KSC5601"), "EUC-KR"); // 2013.12.30 mgkim 운영서버 인코딩
		/* 2014.11.06 양상완 파일네임의 상위폴더로 가는 특수문자 제거 */
		fileName = fileName.replace("/", "");
		fileName = fileName.replace("..", "");
		fileName = fileName.replace("\\", "");
		fileName = fileName.replace("&", "");
		fileName = fileName.replace("%", "");
		String fileDir = "";

		if ("1".equals(fileCls)) {// 각종 샘플파일
			fileDir = defaultFpisFilePath + "resources/";
		} else if ("2".equals(fileCls)) {
			fileDir = upload_path_comStatCarMinData;
		} else if ("3".equals(fileCls)) {// 지자체의 시스템업체관리 파일등록시 저장되는 임시파일, 리턴파일 경로
			fileDir = upload_path_sysCompTempFile;
		} else if ("4".equals(fileCls)) { // 2015.03.06 양상완 처음이세요 메뉴의 파일들.
			// 경로변경 기존운영서버에서는 resources 안에 파일들을 넣어서 관리했음. - 2021.11.25 suhyun
			fileDir = fpisBaseFilePath + "movie/";
		} else if ("5".equals(fileCls)) { // 2015.03.06 양상완 처음이세요 메뉴의 파일들.
			fileDir = defaultFpisFilePath + "program/";
		} else if ("6".equals(fileCls)) { // 2022.10.11 jwchoi 대량실적신고 피드백 엑셀 다운
			fileDir = errorFilePath;
		} else if ("7".equals(fileCls)) { // 2023.11.23 chbaek 미가입자 등록 FPIS 양식(운송/주선) 다운
			fileDir = upload_path_sysCompNewFile;
		} else if ("99".equals(fileCls)) { // 2015.03.06 양상완 처음이세요 메뉴의 파일들.
			fileDir = majarStatFilePath + File.separator;
		} else if ("00".equals(fileCls)) {
			// 2021.11.17 jwchoi exe파일 예외처리
			fileDir = fpisBaseFilePath + "program/version/" + fileCls + "/";
		} else if ("01".equals(fileCls)) {
			// 2021.11.17 jwchoi exe파일 예외처리
			fileDir = fpisBaseFilePath + "program/version/" + fileCls + "/";
		}
		File uFile = new File(fileDir, fileName);
		int fSize = (int) uFile.length();
		
		if (fSize > 0) {
			String mimetype = "application/x-msdownload";

			// response.setBufferSize(fSize); // OutOfMemeory 발생
			response.setContentType(mimetype);
			setDisposition(fileName, request, response);
			response.setContentLength(fSize);

			BufferedInputStream in = null;
			BufferedOutputStream out = null;

			try {
				in = new BufferedInputStream(new FileInputStream(uFile));
				out = new BufferedOutputStream(response.getOutputStream());

				FileCopyUtils.copy(in, out);
				out.flush();
			} catch (FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			}

			// 170626 smoh 직접/최소 파일 다운로드 후 삭제
			if ("99".equals(fileCls) && uFile.exists()) {
				uFile.delete();
			}

		}
	}

	/*
	 * 2014.07.28 양상완 파일이름만 post방식으로 넘기고 년도도 받는다. 한다. fileName : 파일명 fileYear : 파일년도
	 */
	@RequestMapping(value = "/cmm/fms/FpisFileDown_sw2.do")
	public void FpisFileDownload_sw2(Map<String, Object> commandMap, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String fileName = (String) commandMap.get("fileName");
		String fileYear = request.getParameter("fileYear");
		String fileCls = request.getParameter("fileCls");
		// fileName = new String (fileName.getBytes("8859_1"),"UTF-8"); // 2013.10.02 mgkim 한글 깨짐.
		// 개발서버

		fileName = new String(fileName.getBytes("KSC5601"), "EUC-KR"); // 2013.12.30 mgkim 운영서버 인코딩
		/* 2014.11.06 양상완 파일네임의 상위폴더로 가는 특수문자 제거 */
		fileName = fileName.replace("/", "");
		fileName = fileName.replace("..", "");
		fileName = fileName.replace("\\", "");
		fileName = fileName.replace("&", "");
		fileName = fileName.replace("%", "");

		String fileDir = "";

		if ("1".equals(fileCls)) {
			fileDir = upload_path_usrCarZip;
			fileDir = fileDir + fileYear + "/";
		} else if ("2".equals(fileCls)) {
			fileDir = upload_path_modify;
			/* 2015.04.01 양상완 정정요청 파일 경로 수정 */
			fileDir = fileDir + "newRegCorrect/" + fileYear + "/";
		}

		File uFile = new File(fileDir, fileName);
		int fSize = (int) uFile.length();

		if (fSize > 0) {
			String mimetype = "application/x-msdownload";

			// response.setBufferSize(fSize); // OutOfMemeory 발생
			response.setContentType(mimetype);
			setDisposition(fileName, request, response);
			response.setContentLength(fSize);

			BufferedInputStream in = null;
			BufferedOutputStream out = null;

			try {
				in = new BufferedInputStream(new FileInputStream(uFile));
				out = new BufferedOutputStream(response.getOutputStream());

				FileCopyUtils.copy(in, out);
				out.flush();
			} catch (FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			}
		}
	}
	
	/*
	 * 2022.08.05 jwchoi 대량실적 양식 다운로드 fileCls 전달. 나머지는 여기서 처리
	 */
	@RequestMapping(value = "/cmm/fms/FpisFileDown_reg.do")
	public void FpisFileDownload_reg(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String fileName = "";
		String fileDir = "";
		String fileCls = request.getParameter("fileCls");
		String chkTb = request.getParameter("reg_option");
		SessionVO svo = (SessionVO) request.getSession().getAttribute(FpisConstants.SESSION_KEY);
		String usrCls = svo.getComp_cls_detail();
		String usrCond = svo.getCond();
		
		if ("1".equals(fileCls)) {
			fileDir = fpisBaseFilePath + "down/";
			if ("04".equals(usrCond) || "05".equals(usrCond) || "06".equals(usrCond) || "07".equals(usrCond)) {
				fileName = "FPIS_ORDER_SAMPLE_G.xls";
			}
			if ("reg_tb".equals(chkTb)) {
				fileName = "FPIS_ORDER_SAMPLE_T.xls";
			} else if ("01".equals(usrCond) || "03".equals(usrCond)) {
				fileName = "FPIS_ORDER_SAMPLE_U.xls";
			} else if ("02".equals(usrCond)) {
				fileName = "FPIS_ORDER_SAMPLE_J.xls";
			}
		}

		fileName = new String(fileName.getBytes("KSC5601"), "EUC-KR"); // 2013.12.30 mgkim 운영서버 인코딩
		/* 2014.11.06 양상완 파일네임의 상위폴더로 가는 특수문자 제거 */
		fileName = fileName.replace("/", "");
		fileName = fileName.replace("..", "");
		fileName = fileName.replace("\\", "");
		fileName = fileName.replace("&", "");
		fileName = fileName.replace("%", "");

		File uFile = new File(fileDir, fileName);
		int fSize = (int) uFile.length();

		if (fSize > 0) {
			String mimetype = "application/x-msdownload";

			response.setContentType(mimetype);
			setDisposition(fileName, request, response);
			response.setContentLength(fSize);

			BufferedInputStream in = null;
			BufferedOutputStream out = null;

			try {
				in = new BufferedInputStream(new FileInputStream(uFile));
				out = new BufferedOutputStream(response.getOutputStream());

				FileCopyUtils.copy(in, out);
				out.flush();
			} catch (FileNotFoundException e) {
				logger.error("[ERROR] - FileNotFoundException : ", e);
			} catch (IOException e) {
				logger.error("[ERROR] - IOException : ", e);
			} finally {
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					logger.error("[ERROR] - IOException : ", e);
				}
			}
		}
	}
}
