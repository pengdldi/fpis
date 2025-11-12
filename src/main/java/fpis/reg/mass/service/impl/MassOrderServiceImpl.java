package fpis.reg.mass.service.impl;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import java.nio.charset.Charset;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import egovframework.rte.fdl.cmmn.EgovAbstractServiceImpl;
import fpis.reg.mass.set.ImportDataSet;
import fpis.reg.mass.set.ImportStatus;
import fpis.reg.mass.set.WorkFileType;
import fpis.reg.mass.web.MassOrderController;
import fpis.common.vo.mod.CheckRegModifyAllowVO;
import fpis.common.vo.mod.ErrorVO;
import fpis.mod.upload.service.impl.FpisModUploadDAO;
import fpis.reg.mass.error.ErrorType;
import fpis.reg.mass.res.RESOURCE_VAR;
import fpis.reg.mass.service.MassOrderService;
import fpis.reg.mass.service.Utils;
import fpis.reg.mass.service.ContGroupVO;
import fpis.reg.mass.service.ContractInfoForSmalling;
import fpis.reg.mass.service.ConvertVO;
import fpis.reg.mass.service.CustomHashMapStartWithKey;
import fpis.reg.mass.service.DataVerifyResult;
import fpis.reg.mass.service.DateUtil;
import fpis.reg.mass.service.ExcelExportManager;
import fpis.reg.mass.service.FpisResultCommon;
import fpis.reg.mass.service.ValidateBasic;
import fpis.reg.mass.service.MassOrderVO;


/**
 * @class_desc 대량실적신고 검증 fpis.reg.mass.service.imple MassOrderServiceImpl.java
 *
 * @DATE 2022. 08. 18.
 * @AUTHOR GnT 최정원
 * @HISTORY DATE AUTHOR NOTE ------------- -------- -------------------- 2022. 08. 18. 최정원 최초생성
 */

@Service("MassOrderService")
public class MassOrderServiceImpl extends EgovAbstractServiceImpl implements MassOrderService {

	private static final Logger logger = Logger.getLogger(MassOrderController.class);

	@Resource(name = "MassOrderDAO")
	private MassOrderDAO massorderDAO;

	@Resource(name = "FpisModUploadDAO")
	private FpisModUploadDAO uploadDAO;
	
	
	private ErrorVO error = new ErrorVO(null, null, null);
	private String usr_mst_key;
	private String usr_cond;
	private boolean is_Deahang;
	private boolean is_0201;
	private boolean noReCordDeaList = true;
	private boolean noUsrGovDae= true;
	private boolean isRegLimitDae= true;
	private boolean isRegModifyAlloweDae= true;
	private String[] noRegistUsrList;
	private List<Map<String,Object>> errCodeList; //양식검증 실패 에러코드 리스트에 삽입 ErrorVO code,message,detailMessage
	private List<Map<String,Object>> errDataList = new ArrayList<Map<String,Object>>(); //데이터검증 실패 에러 데이터 리스트에 삽입 사업자정보, 위탁정보, 배차정보

	public static boolean fpisUploadRunning = false;
	
	@Override
	public void saveUsrInfo(String usrmstkey) {
		this.usr_mst_key = usrmstkey;

	}
	
	@Override
	public void saveUsrCond(String usrCond, boolean isDeahangCheck, boolean isOnly_0201) {
		this.usr_cond = usrCond;
		this.is_Deahang = isDeahangCheck;
		this.is_0201 = isOnly_0201;
	}
	
	@Override
	public List<Map<String, Object>> reSetErrCodeList() {		
		return errCodeList =  new ArrayList<Map<String,Object>>();
	}
	
	@Override
	public List<Map<String, Object>> reSetErrDataList() {		
		return errDataList =  new ArrayList<Map<String,Object>>();
	}
	
	public void setErrCodeList(Map<String,Object> eMap) {
		Map<String,Object> errMap = new HashMap<String,Object>();
		errMap.putAll(eMap);
		errCodeList.add(errMap);
	}
	
	@Override
	public List<Map<String, Object>> getErrCodeList() {
		return errCodeList;
	}
	

	public void setErrDataList(Map<String,Object> eMap) {
		Map<String,Object> errMap = new HashMap<String,Object>();
		errMap.putAll(eMap);
		errDataList.add(errMap);
	}
	
	@Override
	public List<Map<String, Object>> removeNull(int cnt) {
		for (int i=0; i<errDataList.size(); i++) {
			for (int j=0; j<cnt; j++) {
				if ("".equals(errDataList.get(i).get("data"+j))) {
					errDataList.get(i).replace("data"+j, "입력없음");
				}
				
			}
		}
		// TODO Auto-generated method stub
		return errDataList;
	}
	
	@Override
	public List<Map<String, Object>> removeDupliDataList(String oriFileName) {
		List<Map<String,Object>> tmpDataList = new ArrayList<Map<String,Object>>();
		List<Map<String,Object>> finalDataList = new ArrayList<Map<String,Object>>();
		if (errDataList.size() >= 1) {
			for (int i=0; i<errDataList.size(); i++) {
				if (oriFileName == errDataList.get(i).get("fname")) {
					tmpDataList.add(errDataList.get(i));
				}
			}
			
			for (int j=0; j<tmpDataList.size(); j++) {
				if (!finalDataList.contains(tmpDataList.get(j))) {
					finalDataList.add(tmpDataList.get(j));
				}
			}
		}
		return finalDataList;
	}

	public ErrorVO checkErrCode() {
		return error;
	}
	
	/* 2022.08.25 jwchoi 시트 수 0 혹은 2개 이상 확인 */
	public ImportDataSet chkSheetCnt(MultipartFile file) {

		ImportDataSet importDataSet = new ImportDataSet();

		Workbook wb = null;
		String fname = file.getOriginalFilename();

		try {

			wb = WorkbookFactory.create(file.getInputStream());
			Map<String,Object> errMap = new HashMap<String,Object>();

			int sheetCnt = wb.getNumberOfSheets();
			//Sheet sheet = wb.getSheetAt(sheetCnt);
			
			if (sheetCnt == 0) {
				//importDataSet = getErrorDataSet(wb, csvErrorData);
				importDataSet.setImportStatus(ImportStatus.EMPTY_SHEET);
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL015.getCode());
				errMap.put("eMsg", ErrorType.UPL015.getMessage());
				errMap.put("eDetail", ErrorType.UPL015.getDetailMessage());
				errCodeList.add(errMap);
			} else if (sheetCnt > 1) {
				importDataSet.setImportStatus(ImportStatus.EXTRA_SHEET);			
				errMap.put("fname", file.getOriginalFilename());
				errMap.put("eCode", ErrorType.UPL013.getCode());
				errMap.put("eMsg", ErrorType.UPL013.getMessage());
				errMap.put("eDetail", ErrorType.UPL013.getDetailMessage());
				errCodeList.add(errMap);
			} else {
				importDataSet.setImportStatus(ImportStatus.IS_NOT_OLD);
			}
			return importDataSet;
		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "UPL014");
			return importDataSet;
		}
	}

	/* 2022.08.25 jwchoi 이전양식 vs 현재 간소화양식 확인 */
	public ImportDataSet chkPreData(MultipartFile file) {
		
		ImportDataSet importDataSet = new ImportDataSet();
		
		String fname = file.getOriginalFilename();
		
		//Map<Integer, Character> preHeaderLineMap = new HashMap<Integer, Character>();
		Workbook wb = null;
		
		List<String[]> csvErrorData = new ArrayList<String[]>();
		List<String[]> csvErrorHeader = new ArrayList<String[]>();
		List<String[]> csvSucData = new ArrayList<String[]>();

		try {

			wb = WorkbookFactory.create(file.getInputStream());
			
			Sheet sheet = wb.getSheetAt(0);
			Row row = sheet.getRow(1);
			int columnCnt = row.getPhysicalNumberOfCells();
			Cell cell = row.getCell(0);

			int rows = sheet.getPhysicalNumberOfRows();
			String fCell = cell.getStringCellValue();
			

			if (columnCnt == RESOURCE_VAR.TYPE_ONE || columnCnt == RESOURCE_VAR.TYPE_ONE2
					|| columnCnt == RESOURCE_VAR.TYPE_TWO || columnCnt == RESOURCE_VAR.TYPE_TWO2
					|| fCell.contains("상호")) {

				for (int i=0; i<rows; i++) {
					row = sheet.getRow(i);
					String tmp[] = new String[columnCnt];
					String tmp2 = "";
					for (int j=0; j<columnCnt; j++) {
						cell = row.getCell(j);
						try {
							if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
								tmp2 = NumberToTextConverter.toText(cell.getNumericCellValue());
							} else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) { //2023.02.13 jwchoi 수식값일 때 분기처리 추가
								FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
								DataFormatter dataFormatter = new DataFormatter();
								tmp2 = dataFormatter.formatCellValue(evaluator.evaluateInCell(cell));
							} else {
								tmp2 = cell.getStringCellValue();
							}						
						} catch (NullPointerException e) {
							tmp2 = "";
						}
						tmp[j] = tmp2;
						if(j == columnCnt-1) {
							if (i < 2) {
								csvErrorHeader.add(tmp);
							} else {		
								csvErrorData.add(tmp);
							}
						}
					}
				}

				//dvr.setValid(false);
				ExcelExportManager.setFlag_yang(false);
				importDataSet.setImportData(csvErrorData);
				importCode(importDataSet, fname, "UPL006");

				//String _dateStr = DateUtil.getToDayTimeStrForErrorFileMake();
				//String _filename = saveFileName+"_E_"+_dateStr;
				//cer = ExcelExportManager.explodePreHeaderData(rows, columnCnt,_errorFilePath+_filename, csvErrorData, csvErrorHeader, false);	
				return importDataSet;

			} else {
				for (int i=0; i<rows; i++) {
					row = sheet.getRow(i);
					if (i > 1) {			
						String tmp[] = new String[columnCnt];
						String tmp2 = "";
						for (int j=0; j<columnCnt; j++) {
							cell = row.getCell(j);
							try {
								if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
									tmp2 = NumberToTextConverter.toText(cell.getNumericCellValue());
								} else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) { //2023.02.13 jwchoi 수식값일 때 분기처리 추가
									FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
									DataFormatter dataFormatter = new DataFormatter();
									tmp2 = dataFormatter.formatCellValue(evaluator.evaluateInCell(cell));
								} else {
									tmp2 = cell.getStringCellValue();
								}						
							} catch (NullPointerException e) {
								tmp2 = "";
							}
							tmp[j] = tmp2;
							if(j == columnCnt-1) {
								csvSucData.add(tmp);
							}
						}
					}
				}
				ExcelExportManager.setFlag_yang(true);
				importDataSet.setImportData(csvSucData);
				importDataSet.setImportStatus(ImportStatus.IS_NOT_OLD);
				importCode(importDataSet, fname, "success");
				return importDataSet;
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "UPL014");
			return importDataSet;
		}
		
	}


	/**
	 * @method_desc 대량실적신고 검증
	 * @returns RegVO 
	 * xls 엑셀데이터를 시스템으로 가져오기(간소화양식)_양식 무결성 체크
	 * @HISTORY DATE 		  AUTHOR 		NOTE 
	 * 			------------- --------- 	------------------------ 
	 * 			2022. 08. 18. 최정원			최초생성
	 *          2022. 08. 24. 최정원
	 *          2022. 09. 27. 최정원			이전양식도 피드백 엑셀생성
	 */
	public ImportDataSet getDataForSmallingBefore(MultipartFile excelFile, ImportDataSet importDataSet, List<String[]> listDataParam) {
		
		String fname = excelFile.getOriginalFilename();

		List<String[]> csvErrorHeader = new ArrayList<String[]>();
		List<String[]> csvErrorData = new ArrayList<String[]>();
		List<String[]> listData = new ArrayList<String[]>();
		Workbook wb = null;

		try {

			wb = WorkbookFactory.create(excelFile.getInputStream());

			Sheet sheet = wb.getSheetAt(0);
			// 양식 헤더
			Row row = sheet.getRow(0);
			Cell cell = row.getCell(0);
			
			boolean chkHeader = false;
			int columnCnt = row.getPhysicalNumberOfCells();
			int rows = sheet.getPhysicalNumberOfRows();
			int rowCnt = sheet.getLastRowNum();
			if ((rowCnt > 0) || (sheet.getPhysicalNumberOfRows() > 0)) {
				rowCnt++;
			}

			for (int i = 0; i < rowCnt; i++) {
				Row cells = sheet.getRow(i);

				if (i == 0) {
					if (!cells.getCell(0).getStringCellValue().contains("운송의뢰자")) { // 헤더 불일치
						importCode(importDataSet, fname, "UPL006");
						//return importDataSet;
					}
				} else if (i == 1) {
					if (!cells.getCell(0).getStringCellValue().contains("사업자등록번호")
							|| !cells.getCell(1).getStringCellValue().contains("의뢰자구분")) { // 헤더 불일치
						importCode(importDataSet, fname, "UPL006");
						//return importDataSet;
					} else {
						// 양식 헤더로 대행양식 여부 판단
						if (cells.getCell(3).getStringCellValue().startsWith("대행시")
								&& cells.getCell(3).getStringCellValue().endsWith("사업자등록번호")) {

							if ("배차횟수".equals(cells.getCell(10).getStringCellValue())) {
								importDataSet.setWorkFileType(WorkFileType.G2_TYPE_D); // 2차 간소화
							} else {
								importDataSet.setWorkFileType(WorkFileType.G1_TYPE_D); // 1차 간소화
							}
						} else {
							// 미대행 양식
							if ("배차횟수".equals(cells.getCell(9).getStringCellValue())) {
								importDataSet.setWorkFileType(WorkFileType.G2_TYPE_N);// 2차 간소화
																						// 미대행양식
																						// (일반)
							} else if ("위탁계약금액".equals(cells.getCell(9).getStringCellValue())) {
								importDataSet.setWorkFileType(WorkFileType.G2_TYPE_N_FORWARDONLY);// 2차
																									// 간소화
																									// 미대행양식
																									// (순수주선)
							} else {
								importDataSet.setWorkFileType(WorkFileType.G1_TYPE_N);// 1차 간소화
																						// 미대행양식
							}
						}
					}
				} else { // 간소화 양식 데이터 가져오기

					// public static String getStringValue(Cell cell) {    String rtnValue = "";    try {        rtnValue = cell.getStringCellValue();    } catch(IllegalStateException e) {        rtnValue = Integer.toString((int)cell.getNumericCellValue());                }        return rtnValue;}

					if (columnCnt == RESOURCE_VAR.TYPE_SMALLING_CONVERT // 간소화 통합양식(운송)
							|| columnCnt == RESOURCE_VAR.TYPE_SMALLING // 간소화 통합양식 (운송 최신)
							|| columnCnt == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY // 간소화 통합양식(순수주선)
							|| columnCnt == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) { // 간소화 통합양식(대행)
						
						String tmp[] = new String[columnCnt];
						String tmp2 = "";
						for (int j = 0; j < tmp.length; j++) {
							tmp[j] = "";
						}
						for (int k = 0; k < tmp.length; k++) {
							try {
								if (cells.getCell(k).getCellType() == Cell.CELL_TYPE_NUMERIC) {
									tmp2 = NumberToTextConverter
											.toText(cells.getCell(k).getNumericCellValue());
								} else if (cells.getCell(k).getCellType() == Cell.CELL_TYPE_FORMULA) { //2023.02.13 jwchoi 수식값일 때 분기처리 추가
									FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
									DataFormatter dataFormatter = new DataFormatter();
									tmp2 = dataFormatter.formatCellValue(evaluator.evaluateInCell(cells.getCell(k)));
								} else {
									tmp2 = cells.getCell(k).getStringCellValue();
								}
							} catch (IllegalStateException e) {
								importCode(importDataSet, fname, "UPL014");
								return importDataSet;
							} catch (NullPointerException e) {
								tmp2 = "";
							}

							if (tmp2 != null) {
								if (tmp2.length() > 0) {
									String format = tmp2;
									tmp2 = formatingFilter(format, tmp2);
								}
							}
							if (tmp2.indexOf(",") != -1) {
								tmp2 = tmp2.replaceAll(",", "").trim();
							}
							if (tmp2.indexOf("-") != -1) {
								tmp2 = tmp2.replaceAll("-", "").trim();
							}

							if (tmp.length - 1 >= k)
								tmp[k] = tmp2;
						}
						listData.add(tmp);
						importCode(importDataSet, fname, "success");
					} else {
						importCode(importDataSet, fname, "UPL008");
						//return importDataSet;
					}
				}
			}
			
			if (listData.size() == 0) {
				importCode(importDataSet, fname, "UPL009");
				if (columnCnt == RESOURCE_VAR.TYPE_SMALLING_CONVERT // 간소화 통합양식(운송)
							|| columnCnt == RESOURCE_VAR.TYPE_SMALLING // 간소화 통합양식 (운송 최신)
							|| columnCnt == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY // 간소화 통합양식(순수주선)
							|| columnCnt == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
					chkHeader = true;
				}
			}
			
			if(!importDataSet.isSuccess()) {
				ExcelExportManager.setFlag_yang(false);
				for (int i=0; i<rows; i++) {
					row = sheet.getRow(i);
					String tmp[] = new String[columnCnt];
					String tmp2 = "";
					for (int j=0; j<columnCnt; j++) {
						cell = row.getCell(j);
						try {
							if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
								tmp2 = NumberToTextConverter.toText(cell.getNumericCellValue());
							} else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) { //2023.02.13 jwchoi 수식값일 때 분기처리 추가
								FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
								DataFormatter dataFormatter = new DataFormatter();
								tmp2 = dataFormatter.formatCellValue(evaluator.evaluateInCell(cell));
							} else {
								tmp2 = cell.getStringCellValue();
							}						
						} catch (NullPointerException e) {
							tmp2 = "";
						}
						tmp[j] = tmp2;
						if(j == columnCnt-1) {
							if (i < 2) {
								csvErrorHeader.add(tmp);
							} else {
								csvErrorData.add(tmp);
							}
						}
					}
				}

				importDataSet.setImportData(csvErrorData);
				
			} else {
				ExcelExportManager.setFlag_yang(true);
				importDataSet.setImportData(listData);
			}

		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "UPL014");
			return importDataSet;
		}
		return importDataSet;
	}

	private static String formatingFilter(String format, String d) {
		try {
			if (Pattern.matches("^[y]{4}[\"][-][\"][m]{1,}[\"][-][\"][d]{1,}[\\D]+$", format)) {
				String[] s_str = d.split("[\"][-][\"]");
				for (int l = 0; l < s_str.length; l++) {
					s_str[l] = s_str[l].replaceAll("[\\D]", "");
				}
				String ch_str =
						s_str[0] + "-" + ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
								+ ((s_str[2].length() > 1) ? s_str[2] : "0" + s_str[2]);
				d = ch_str;
			}

			if (Pattern.matches("^[d]{2}[/][m]{2}[/][y]{4}$", format)) {
				String[] s_str = d.split("[\\.][\\s]");
				String ch_str = "20" + s_str[0] + "-"
						+ ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
						+ ((s_str[2].length() > 1) ? s_str[2] : "0" + s_str[2]);
				d = ch_str;
			}

			if (Pattern.matches(
					"^.{8}[d]{4}[\\\\][\\,][\\\\][\\s][m]{4}[\\\\][\\s][d]{2}[\\\\][\\,][\\\\][\\s][y]{4}$",
					format)) {
				String[] s_str = d.split("[\\,][\\s]");
				s_str[1] = s_str[1].replaceAll("[\\s]", "").replaceAll("[가-힣]", " ");
				s_str[2] = s_str[2].replaceAll("[\\s]", "");
				d = s_str[2] + "-"
						+ ((s_str[1].split("[\\s]")[0].length() > 1) ? s_str[1].split("[\\s]")[0]
								: "0" + s_str[1].split("[\\s]")[0])
						+ "-" + s_str[1].split("[\\s]")[1];
			}

			if (Pattern.matches(
					"^[y]{4}[\"][년][\"][\\\\][\\s][m][\"][월][\"][\\\\][\\s][d][\"][일][\"][\\D]+$",
					format)) {
				d = d.replaceAll("[\"][가-힣][\"]", "");
				String[] s_str = d.split("[\\s]");
				String ch_str =
						s_str[0] + "-" + ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
								+ ((s_str[2].length() > 1) ? s_str[2] : "0" + s_str[2]);
				d = ch_str;
			}

			if (Pattern.matches(
					"^[y]{2}[\"][年][\"][\\\\][\\s][m][\"][月][\"][\\\\][\\s][d][\"][日][\"][\\D]+$",
					format)) {
				d = d.replaceAll("[\"][年][\"]", "").replaceAll("[\"][月][\"]", "")
						.replaceAll("[\"][日][\"]", "");
				String[] s_str = d.split("[\\s]");
				String ch_str = "20" + s_str[0] + "-"
						+ ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
						+ ((s_str[2].length() > 1) ? s_str[2] : "0" + s_str[2]);
				d = ch_str;
			}

			if (Pattern.matches("^[y]{2}[\"][-][\"][m][\"][-][\"][d][\\D]+$", format)) {
				String[] s_str = d.split("[\"][-][\"]");
				String ch_str = "20" + s_str[0] + "-"
						+ ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
						+ ((s_str[2].length() > 1) ? s_str[2] : "0" + s_str[2]);
				d = ch_str;
			}

			if (Pattern.matches("^[y]{2}[\"][/][\"][m][\"][/][\"][d][\\D]+$", format)) {
				String[] s_str = d.split("[\"][/][\"]");
				String ch_str = "20" + s_str[0] + "-"
						+ ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
						+ ((s_str[2].length() > 1) ? s_str[2] : "0" + s_str[2]);
				d = ch_str;
			}

			if (Pattern.matches("^[y]{4}[\"][/][\"][m][\"][/][\"][d][\\D]+$", format)) {
				String[] s_str = d.split("[\"][/][\"]");
				String ch_str =
						s_str[0] + "-" + ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
								+ ((s_str[2].length() > 1) ? s_str[2] : "0" + s_str[2]);
				d = ch_str;
			}

			if (Pattern.matches("^[m][\"][/][\"][d][\"][/][\"][y]{2}[\\D]+$", format)) {
				String[] s_str = d.split("[\"][/][\"]");
				String ch_str = "20" + s_str[2] + "-"
						+ ((s_str[0].length() > 1) ? s_str[0] : "0" + s_str[0]) + "-"
						+ ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]);
				d = ch_str;
			}

			if (Pattern.matches("^[m]{2}[\"][/][\"][d]{2}[\"][/][\"][y]{2}[\\D]+$", format)) {
				String[] s_str = d.split("[\"][/][\"]");
				String ch_str = "20" + s_str[2] + "-" + s_str[0] + "-" + s_str[1];
				d = ch_str;
			}

			if (Pattern.matches("^.{7}[d][\"][-][\"][m]{3}[\"][-][\"][y]{2}[\\D]+$", format)) {
				String[] s_str = d.split("[\"][-][\"]");
				s_str[1] = s_str[1].replaceAll("[가-힣]", "");
				String ch_str = "20" + s_str[2] + "-"
						+ ((s_str[1].length() > 1) ? s_str[1] : "0" + s_str[1]) + "-"
						+ ((s_str[0].length() > 1) ? s_str[0] : "0" + s_str[0]);
				d = ch_str;
			}

			if (Pattern.compile("[0][\\.][0][0]").matcher(d).find()) {
				int commaIndex = d.indexOf(".");
				d = d.substring(0, commaIndex).replaceAll("[\\D]", "");
			}

			/*
			 * if(Pattern.matches("^[\\#][\\,][\\#][\\#][0][\\.][0][0][\\w]*$", format)){ int
			 * commaIndex = d.indexOf("."); d = d.substring(0,commaIndex); }
			 */
		} catch (Exception e) {
			return d;
		}
		return d;
	}

	@Override
	public ImportDataSet getDataForSmallingAfter(String fname, ImportDataSet importDataSet, List<String[]> listDataParam) {
		int COL_CNT = importDataSet.getImportData().get(0).length;
		Map<String,Object> errMap = new HashMap<String,Object>();
		
		if(is_Deahang) {//대행계정
			if (COL_CNT != RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {//대행양식X
				importCode(importDataSet, fname, "UPL003");
			} else if (importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_N || importDataSet.getWorkFileType() == WorkFileType.G2_TYPE_N) {//대행양식X
				importCode(importDataSet, fname, "UPL011");
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL011.getCode());
				errMap.put("eMsg", ErrorType.UPL011.getMessage());
				errMap.put("eDetail", ErrorType.UPL011.getDetailMessage());
				setErrCodeList(errMap);
			}
		} else {
			if(is_0201) {//주선사업자
				if (COL_CNT != RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {//주선양식X
					importCode(importDataSet, fname, "UPL004");
					errMap.put("fname", fname);
					errMap.put("eCode", ErrorType.UPL004.getCode());
					errMap.put("eMsg", ErrorType.UPL004.getMessage());
					errMap.put("eDetail", ErrorType.UPL004.getDetailMessage());
					//errCodeList.add(errMap);
					setErrCodeList(errMap);
				}
			} else {//운송사업자
				if (COL_CNT != RESOURCE_VAR.TYPE_SMALLING) {//운송양식X 
					importCode(importDataSet, fname, "UPL005");
					errMap.put("fname", fname);
					errMap.put("eCode", ErrorType.UPL005.getCode());
					errMap.put("eMsg", ErrorType.UPL005.getMessage());
					errMap.put("eDetail", ErrorType.UPL005.getDetailMessage());
					setErrCodeList(errMap);
				}
			}
			//미대행계정 && 대행양식일 때 에러
			if (importDataSet.getWorkFileType() == WorkFileType.G1_TYPE_D || importDataSet.getWorkFileType() == WorkFileType.G2_TYPE_D) { 
				importCode(importDataSet, fname, "UPL010");
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL010.getCode());
				errMap.put("eMsg", ErrorType.UPL010.getMessage());
				errMap.put("eDetail", ErrorType.UPL010.getDetailMessage());
				setErrCodeList(errMap);
			}
		}
		
		if(!importDataSet.isSuccess()) {
			ExcelExportManager.setFlag_yang(false);
		}
		return importDataSet;
	}


	@Override
	public ImportDataSet chkRowLimit(String fname, ImportDataSet importDataSet, List<String[]> listDataParam) {
		
		Map<String,Object> errMap = new HashMap<String,Object>();
		
		int rowCnt = importDataSet.getImportData().size();
		int COL_CNT = importDataSet.getImportData().get(0).length;
		
		if (rowCnt > 10000) {
			ExcelExportManager.setFlag_rowLimit(false);
			
			for (int i=0; i<COL_CNT; i++) {
				errMap.put("data"+i, listDataParam.get(10001)[i]);
			}
			errMap.put("fname", fname);
			errMap.put("row", 10001);
			errMap.put("eCode", ErrorType.UPL002.getCode());
			errMap.put("eMsg", ErrorType.UPL002.getMessage());
			errMap.put("eDetail", ErrorType.UPL002.getDetailMessage());
			setErrDataList(errMap);
		} else {
			ExcelExportManager.setFlag_rowLimit(true);
		}
		
		return importDataSet;
	}
	
	@Override
	public void chkNomerge(String fname, ImportDataSet importDataSet, List<String[]> listDataParam) {
		
		Map<String,Object> errMap = new HashMap<String,Object>();
		
		ExcelExportManager.setFlag_yang(false);
		errMap.put("fname", fname);
		errMap.put("eCode", ErrorType.UPL016.getCode());
		errMap.put("eMsg", ErrorType.UPL016.getMessage());
		errMap.put("eDetail", ErrorType.UPL016.getDetailMessage());
		setErrCodeList(errMap);
	}

	@Override
	public List<String[]> dataEmptyArrayRemove(String fname, ImportDataSet importDataSet,
			List<String[]> listDataParam) {
		List<String[]> listData = new ArrayList<String[]>();
		boolean isExistData = false;

		try {
			for (int i = 0; i < listDataParam.size(); i++) {
				String[] data = listDataParam.get(i);
				for (int j = 0; j < data.length; j++) {
					if (data[j] != null) {
						if (data[j].trim().length() > 0) {
							isExistData = true;
							break;
						}
					}
				}
				if (isExistData) {
					listData.add(data);
					isExistData = false;
				} else {

				}
			}
		} catch (Exception e) {
			importCode(importDataSet, fname, "UPL014");
		}
		return listData;
	}

	@Override
	public ImportDataSet makeOrderCnt(ImportDataSet importDataSet, int type) {
		List<String[]> list = importDataSet.getImportData();
		List<String[]> resultList = new ArrayList<String[]>();
		String[] temp = null;
		for (int i = 0; i < list.size(); i++) {
			String[] source = list.get(i);
			temp = new String[source.length + 1];

			if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT) {
				if (source[6] == null || source[6].equals("")) {
					// 배차정보가 없는 경우
					System.arraycopy(source, 0, temp, 0, 8);
					temp[8] = "";
					System.arraycopy(source, 8, temp, 9, source.length - 8);
				} else {
					// 배차정보가 없는 경우
					System.arraycopy(source, 0, temp, 0, 8);
					temp[8] = "1";
					System.arraycopy(source, 8, temp, 9, source.length - 8);
				}
			} else {
				if (source[7] == null || source[7].equals("")) {
					System.arraycopy(source, 0, temp, 0, 9);
					temp[9] = "";
					System.arraycopy(source, 9, temp, 10, source.length - 9);
				} else {
					System.arraycopy(source, 0, temp, 0, 9);
					temp[9] = "1";
					System.arraycopy(source, 9, temp, 10, source.length - 9);
				}
			}

			resultList.add(temp);

		}

		for (String[] a : resultList) {
			Utils.printArray(a);
		}

		importDataSet.setImportData(resultList);
		
		return importDataSet;

	}

	@Override
	public List<String[]> dataSupplement(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String supplement) {
		
		List<String[]> listDataFinal = new ArrayList<String[]>();
		int trustBnsNumPosition = 0;

		try {
			for (int i = 0; i < listDataParam.size(); i++) {
				String[] data = listDataParam.get(i);
				for (int j = 0; j < supplement.length(); j++) {
					data[j] = (data[j] != null) ? data[j].trim() : data[j];
					data[j] = (data[j] != null) ? data[j].replaceAll("\\p{Z}","") : data[j]; //2023.02.20 jwchoi 유니코드 공백 없애기. trim()으로 안 지워짐.
					data[j] = (data[j] != null) ? data[j].replaceAll("[-]", "") : data[j];

					// 1 : 자릿수가 한자리일 경우 앞에 0을 붙여 0x 타입으로 치환
					if (String.valueOf(supplement.charAt(j)).equals("1")) {
						if (data[j] != null && data[j].length() == 1) {
							data[j] = "0" + data[j];
						}
					}

					// 2 : null이거나 공백일 경우 0으로 치환
					if (String.valueOf(supplement.charAt(j)).equals("2")) {
						if (data[j] == null || data[j].equals("")) {
							data[j] = "0";
						}
					}

					/* 이사화물/동일항만내 이송 데이터보정 */
					// 8 : null이거나 공백일 경우 01로 아닐 경우 0x로 치환
					if (String.valueOf(supplement.charAt(j)).equals("8")) {
						data[j] = data[j].replaceAll("0", "");
						if (data[j] == null || data[j].equals("")) {
							data[j] = "01";
						} else {
							if (data[j].length() == 1) {
								data[j] = "0" + data[j];
							}
						}
					}

					/* 타운송수간 이용여부 데이터보정 */
					// 9 : null이거나 공백일 경우 01로 아닐 경우 0x로 치환
					if (String.valueOf(supplement.charAt(j)).equals("9")) {
						data[j] = data[j].replaceAll("0", "");
						if (data[j] == null || data[j].equals("")) {
							data[j] = "01";
						} else {
							if (data[j].length() == 1) {
								data[j] = "0" + data[j];
							}
						}
					}

					/* 의뢰자 구분 데이터보정 */
					// 7 : null이거나 공백일 경우 01로 아닐 경우 0x로 치환
					if (String.valueOf(supplement.charAt(j)).equals("7")) {
						String orijin = data[j];
						data[j] = data[j].replaceAll("0", "");
						if (data[j] == null || data[j].equals("")) {
							data[j] = orijin;
						} else {
							if (data[j].length() == 1) {
								if (Pattern.matches("^[0-9]*$", data[j])) {
									data[j] = "0" + data[j];
								} else {
									data[j] = orijin;
								}
							}
						}
					}

					if (String.valueOf(supplement.charAt(j)).equals("b")) {
						if (data[j] != null && "".equals(data[j])) {
							data[j] = "N";
						}
					}

					/** 대행정보 */
					// 4 : 공백일 경우 접속계정의 사업자번호로 치환
					if (String.valueOf(supplement.charAt(j)).equals("4")) {
						if (data[j] != null && "".equals(data[j])) {
							data[j] = usr_mst_key;;
						}
					}

					if (String.valueOf(supplement.charAt(j)).equals("2")) {
						trustBnsNumPosition = j;
					}


					if (String.valueOf(supplement.charAt(j)).equals("a")) {
						if (data[j] != null && data[j].length() == 1) {
							data[j] = "0" + data[j];
						}

					}

					/* 화물정보망이용여부 초기화 2017. 09. 27. written by dyahn */
					// 9 : null이거나 공백일 경우 01로 아닐 경우 0x로 치환
					if (String.valueOf(supplement.charAt(j)).equals("y")) {
						if ("N".equals(data[j]) || "n".equals(data[j])) {
							data[j] = "99";
						}
						if ("Y".equals(data[j]) || "y".equals(data[j])) {
							data[j] = "00";
						} else {
							if (data[j].length() == 1) {
								data[j] = "0" + data[j];
							}
						}
					}
				}
				listDataFinal.add(data);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			importCode(importDataSet, fname, "UPL014");
			
		}
		return listDataFinal;
	}
	
	@Override
	public boolean checkAgencyCnt(String fname, ImportDataSet importDataSet,
			List<String[]> listDataParam, int type) {
		
		Map<Integer, Character> agencyCntLineMap = new HashMap<Integer, Character>();
		Set<String> agencyUsrMstKey = new HashSet<String>();
		
		boolean isAgencyCntValid = true;
		//int agencyCnt = 0;
		
		try {
			for (int i = 0; i < listDataParam.size(); i++) {
				agencyUsrMstKey.add(listDataParam.get(i)[3]);
			}
			
			List<String> newAgencyList = new ArrayList<String>(agencyUsrMstKey);
			//newAgencyList.get(1001)
			if (newAgencyList.size() > 1000) {
				isAgencyCntValid = false;
				ExcelExportManager.setFlag_agencyCnt(false);
				for (int j = 1001; j < newAgencyList.size(); j++) {
					for (int i = 0; i < listDataParam.size(); i++) {
						if (newAgencyList.get(j).contains(listDataParam.get(i)[3])) {
							Map<String,Object> errMap = new HashMap<String,Object>();
							agencyCntLineMap.put(i, '0');
							errMap.put("fname", fname);
							errMap.put("row", i+3);
							errMap.put("eCode", ErrorType.LIM003.getCode());
							errMap.put("eMsg", ErrorType.LIM003.getMessage());
							errMap.put("eDetail", ErrorType.LIM003.getDetailMessage());
							for(int k=0; k<type; k++) {
								errMap.put("data"+k, listDataParam.get(i)[k]);
							}
							errMap.put("data3", listDataParam.get(i)[3]+"##FAIL##");
							setErrDataList(errMap);
						}
					}	
				}
				
				ExcelExportManager.setAgencyCntLineMap(agencyCntLineMap);
				//importCode(importDataSet, fname, "LIM003");
			}	
		} catch (Exception e) {
			isAgencyCntValid = false;
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		
		return isAgencyCntValid;
	}

	@Override
	public Set<String> checkAgencyUsrMstKey(String fname, ImportDataSet importDataSet,
			List<String[]> listDataParam, int type) {

		Map<Integer, Character> noUsrLineMap = new HashMap<Integer, Character>();
		
		Set<String> agencyUsrMstKey = new HashSet<String>();
		
		int noRegist = 0;

		for (int i = 0; i < listDataParam.size(); i++) {
			agencyUsrMstKey.add(listDataParam.get(i)[3]);
		}
		
		Set<String> notRegistUsr = new HashSet<String>();
		Iterator<String> iter = agencyUsrMstKey.iterator();
		while (iter.hasNext()) {
			String aumk = iter.next();
			try {
				noRegist = massorderDAO.checkAgencyUsrMstKey(aumk);
				if (noRegist == 0) {
					notRegistUsr.add(aumk);
				} else {
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// String usrListStr = getRegistUsrList(aumk);
		}
		
		String noRegistUsrAll = notRegistUsr.toString().replace("[", "").replace("]", "").replace(" ", "");
		String[] tempList = noRegistUsrAll.split(",");
		noRegistUsrList = new String[tempList.length];
		
		for(int i = 0; i < tempList.length; i++) {
			noRegistUsrList[i] = tempList[i];
		}
		
		if (notRegistUsr.size() > 0) {
			ExcelExportManager.setFlag_noUsr(false);
			for (int i = 0; i < listDataParam.size(); i++) {
				if ( notRegistUsr.contains(listDataParam.get(i)[3]) ) {
					Map<String,Object> errMap = new HashMap<String,Object>();
					noUsrLineMap.put(i, '0');
					errMap.put("fname", fname);
					errMap.put("row", i+3);
					errMap.put("eCode", ErrorType.COR008.getCode());
					errMap.put("eMsg", ErrorType.COR008.getMessage());
					errMap.put("eDetail", ErrorType.COR008.getDetailMessage());
					for(int k=0; k<type; k++) {
						errMap.put("data"+k, listDataParam.get(i)[k]);
					}
					errMap.put("data3", listDataParam.get(i)[3]+"##FAIL##");
					setErrDataList(errMap);
				}
			}

			ExcelExportManager.setNoUsrLineMap(noUsrLineMap);
			importCode(importDataSet, fname, "COR008");
		} else {
			ExcelExportManager.setFlag_noUsr(true);
		}
		return notRegistUsr;
	}

	@Override
	public List<String[]> transDateString(String fname, ImportDataSet importDataSet, List<String[]> listDataParam,
			String supplement) {
		
		List<String[]> dataFinal = new ArrayList<String[]>();

		try {
			for (int i = 0; i < listDataParam.size(); i++) {
				String[] data = listDataParam.get(i);
				for (int j = 0; j < supplement.length(); j++) {
					if (String.valueOf(supplement.charAt(j)).equals("1")) {
						if (data[j] != null && data[j].length() > 0) {
							if (data[j].length() > 6) {
								data[j] = data[j].substring(0, 6);
							}
						}
					}
				}
				dataFinal.add(data);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		return dataFinal;
	}

	public boolean checkEmptyData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type) {
		
		DataVerifyResult dvr = chkDataVerifyResult(fname, importDataSet, listDataParam, checkBit, type);

		ExcelExportManager.setEmptyBit(checkBit);
		
		if (!dvr.isValid()) {
			ExcelExportManager.setFlag_emptyData(false);
		} else {
			ExcelExportManager.setFlag_emptyData(true);
		}
		
		return dvr.isValid();

	}

	private DataVerifyResult chkDataVerifyResult(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			int type) {
		
		DataVerifyResult dvr = new DataVerifyResult();
		
		try {
			Map<Integer, Character> emptyLineMap = new HashMap<Integer, Character>();
			boolean isEmptyValid = true;
			boolean spdTrustExist = false;
			boolean spdOperExist = false;
			boolean isOperExist = false;
			boolean isTruExist = false;

			int checkingCount = 0;
			for (int k = 0; k < listDataParam.size(); k++) {
				Map<String,Object> errMap = new HashMap<String,Object>();
				String[] data = listDataParam.get(k);
				if (type == RESOURCE_VAR.TYPE_SPD) {
					// 위탁실적 판단
					if (data[6].equals("") && data[7].equals("") && data[8].equals("")) {
						spdTrustExist = false;
					} else {
						spdTrustExist = true;
					}

					// 배차실적 판단
					if (data[2].equals("") && data[3].equals("") && data[4].equals("")
							&& data[5].equals("")) {
						spdOperExist = false;
					} else {
						spdOperExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING) {
					if (data[RESOURCE_VAR.OPER_P1].equals("")
							&& data[RESOURCE_VAR.OPER_P2].equals("")
							&& data[RESOURCE_VAR.OPER_P3].equals("")
							&& data[RESOURCE_VAR.OPER_P4].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_P1].equals("")
							&& data[RESOURCE_VAR.TRUST_P2].equals("")
							&& data[RESOURCE_VAR.TRUST_P3].equals("")
							&& data[RESOURCE_VAR.TRUST_P4].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) { // 2017. 09. 19 written
																				// by dyahn 순수주선
																				// 간소화양식 공백데이터 검증 분기
																				// 추가

					isOperExist = false; // 순수주선 간호화양식의 배차계약정보는 입력하지 않으므로 무조건 false

					if (data[RESOURCE_VAR.TRUST_P1_FORWARDONLY].equals("")
							&& data[RESOURCE_VAR.TRUST_P2_FORWARDONLY].equals("")
							&& data[RESOURCE_VAR.TRUST_P3_FORWARDONLY].equals("")
							&& data[RESOURCE_VAR.TRUST_P4_FORWARDONLY].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}

				} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {


					if (data[RESOURCE_VAR.OPER_D_P1].equals("")
							&& data[RESOURCE_VAR.OPER_D_P2].equals("")
							&& data[RESOURCE_VAR.OPER_D_P3].equals("")
							&& data[RESOURCE_VAR.OPER_D_P4].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_D_P1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P2].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P3].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P4].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT) {
					if (data[RESOURCE_VAR.OPER_P1].equals("")
							&& data[RESOURCE_VAR.OPER_P2].equals("")
							&& data[RESOURCE_VAR.OPER_P3].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_P1 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_P2 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_P3 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_P4 - 1].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT_DAEHANG) {


					if (data[RESOURCE_VAR.OPER_D_P1].equals("")
							&& data[RESOURCE_VAR.OPER_D_P2].equals("")
							&& data[RESOURCE_VAR.OPER_D_P3].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_D_P1 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P2 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P3 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P4 - 1].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				}

				for (int i = 0; i < data.length; i++) {
					if (type == RESOURCE_VAR.TYPE_SPD) {
						if (String.valueOf(checkBit.charAt(i)).equals("1")) {
							if (data[i] == null || data[i].length() == 0) {
								checkingCount++;
								if (!emptyLineMap.containsKey(k)) {
									emptyLineMap.put(k, '0');
								}
								isEmptyValid = false;
								errMap.put("fname", fname);
								errMap.put("row", k+3);
								errMap.put("eCode", ErrorType.EMP002.getCode());
								errMap.put("eMsg", ErrorType.EMP002.getMessage());
								errMap.put("eDetail", ErrorType.EMP002.getDetailMessage());
								errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								
							}
						}

						if (spdOperExist && spdTrustExist) {
							if (String.valueOf(checkBit.charAt(i)).equals("2")
									|| String.valueOf(checkBit.charAt(i)).equals("3")
									|| String.valueOf(checkBit.charAt(i)).equals("4")) {
								if (data[i] == null || data[i].length() == 0) {
									checkingCount++;
									if (!emptyLineMap.containsKey(k)) {
										emptyLineMap.put(k, '0');
									}
									isEmptyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.EMP004.getCode());
									errMap.put("eMsg", ErrorType.EMP004.getMessage());
									errMap.put("eDetail", ErrorType.EMP004.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						}

						if (!spdOperExist && spdTrustExist) {
							if (String.valueOf(checkBit.charAt(i)).equals("2")
									|| String.valueOf(checkBit.charAt(i)).equals("4")) {
								if (data[i] == null || data[i].length() == 0) {
									checkingCount++;
									if (!emptyLineMap.containsKey(k)) {
										emptyLineMap.put(k, '0');
									}
									isEmptyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.EMP001.getCode());
									errMap.put("eMsg", ErrorType.EMP001.getMessage());
									errMap.put("eDetail", ErrorType.EMP001.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						}

						if (spdOperExist && !spdTrustExist) {
							if (String.valueOf(checkBit.charAt(i)).equals("3")) {
								if (data[i] == null || data[i].length() == 0) {
									checkingCount++;
									if (!emptyLineMap.containsKey(k)) {
										emptyLineMap.put(k, '0');
									}
									isEmptyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.EMP002.getCode());
									errMap.put("eMsg", ErrorType.EMP002.getMessage());
									errMap.put("eDetail", ErrorType.EMP002.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						}
					}

					if (type == RESOURCE_VAR.TYPE_SMALLING
							|| type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY
							|| type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG
							|| type == RESOURCE_VAR.TYPE_SMALLING_CONVERT
							|| type == RESOURCE_VAR.TYPE_SMALLING_CONVERT_DAEHANG) {

						if (String.valueOf(checkBit.charAt(i)).equals("1")) {
							if (data[i] == null || data[i].length() == 0) {
								checkingCount++;
								if (!emptyLineMap.containsKey(k)) {
									emptyLineMap.put(k, '0');
								}
								isEmptyValid = false;
								errMap.put("fname", fname);
								errMap.put("row", k+3);
								errMap.put("eCode", ErrorType.EMP003.getCode());
								errMap.put("eMsg", ErrorType.EMP003.getMessage());
								errMap.put("eDetail", ErrorType.EMP003.getDetailMessage());
								errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
							}
						} /* 2023.02.20 jwchoi 배차정보, 위탁정보 동시 입력 불가 검증 추가. 콜센터 요청사항. 공단 협의 완료 */
						if ((isOperExist && isTruExist)) {
							if (String.valueOf(checkBit.charAt(i)).equals("2")
									|| String.valueOf(checkBit.charAt(i)).equals("3")) {
								if (data[i] != null || data[i].length() > 0) {
									checkingCount++;
									if (!emptyLineMap.containsKey(k)) {
										emptyLineMap.put(k, '0');
									}
									isEmptyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.EMP006.getCode());
									errMap.put("eMsg", ErrorType.EMP006.getMessage());
									errMap.put("eDetail", ErrorType.EMP006.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						} else if (!isOperExist && !isTruExist) {
							if (String.valueOf(checkBit.charAt(i)).equals("2")
									|| String.valueOf(checkBit.charAt(i)).equals("3")) {
								if (data[i] == null || data[i].length() == 0) {
									checkingCount++;
									if (!emptyLineMap.containsKey(k)) {
										emptyLineMap.put(k, '0');
									}
									isEmptyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.EMP004.getCode());
									errMap.put("eMsg", ErrorType.EMP004.getMessage());
									errMap.put("eDetail", ErrorType.EMP004.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						} else if (isOperExist && !isTruExist) {
							if (String.valueOf(checkBit.charAt(i)).equals("2")) {
								if (data[i] == null || data[i].length() == 0) {
									checkingCount++;
									if (!emptyLineMap.containsKey(k)) {
										emptyLineMap.put(k, '0');
									}
									isEmptyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.EMP002.getCode());
									errMap.put("eMsg", ErrorType.EMP002.getMessage());
									errMap.put("eDetail", ErrorType.EMP002.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						} else if (!isOperExist && isTruExist) {
							if (String.valueOf(checkBit.charAt(i)).equals("3")) {
								if (data[i] == null || data[i].length() == 0) {
									checkingCount++;
									if (!emptyLineMap.containsKey(k)) {
										emptyLineMap.put(k, '0');
									}
									isEmptyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.EMP001.getCode());
									errMap.put("eMsg", ErrorType.EMP001.getMessage());
									errMap.put("eDetail", ErrorType.EMP001.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						}
					}
				}
				if (!errMap.isEmpty()) {
					int getKey = (Integer)errMap.get("row");
					if (k+3 == getKey) {
						for(int m=0; m<data.length; m++) {
							if (!errMap.containsKey("data"+m)) {
								errMap.put("data"+m, listDataParam.get(k)[m]);
							}
						}
						setErrDataList(errMap);
					}
				}
			}
			ExcelExportManager.setEmptyLineMap(emptyLineMap);
			dvr.setEmptyLineMap(emptyLineMap);
			dvr.setUnvalidCount(checkingCount);
			dvr.setValid(isEmptyValid);
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		return dvr;
	}

	public boolean checkBasicFormData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type,
			boolean isOnly_0201) {

		DataVerifyResult dvr = chkBasicFormData(fname, importDataSet, listDataParam, checkBit, type, isOnly_0201);

		if (!dvr.isValid()) {
			ExcelExportManager.setFlag_basicData(false);
			ExcelExportManager.setBasicBit(checkBit);
		} else {
			ExcelExportManager.setFlag_basicData(true);
		}

		return dvr.isValid();
	}

	/*
	 * 18.08.07 JJH 기본양식 검증 공통 함수 사용
	 * 
	 * 19.05.29 JJH 날짜검증 추가 작업
	 */
	private DataVerifyResult chkBasicFormData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			int type, boolean isOnly_0201) {
		// System.out.println("[M]checkBasicFormData 시작");
		
		DataVerifyResult dvr = new DataVerifyResult();
		try {
			SortedMap<Integer, List<Integer>> basicFormLineMap =
					new TreeMap<Integer, List<Integer>>();
			boolean basicCheck = false;
			boolean isValid = true;
			boolean spdTrustExist = false;
			boolean spdOperExist = false;
			boolean isOperExist = false;
			boolean isTruExist = false;

			int checkingCount = 0;
			
			int mangCnt = massorderDAO.getMangMaxValue();

			for (int k = 0; k < listDataParam.size(); k++) {
				Map<String,Object> errMap = new HashMap<String,Object>();
				String[] data = listDataParam.get(k);
				// System.out.println("data size : " + data.length);
				String mangCode = "";
				int mangCodeYN = 0;
				
				if (type == RESOURCE_VAR.TYPE_SPD || type == RESOURCE_VAR.TYPE_SPD_CONVERT) {
					mangCodeYN = 0;
				} else if (type == RESOURCE_VAR.TYPE_SMALLING || type == RESOURCE_VAR.TYPE_SMALLING_CONVERT_DAEHANG) {
					mangCode = data[14].toString();
					mangCodeYN = massorderDAO.getVstring(mangCode);
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
					mangCode = data[10].toString();
					mangCodeYN = massorderDAO.getVstring(mangCode);
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
					mangCode = data[15].toString();
					mangCodeYN = massorderDAO.getVstring(mangCode);
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT) {
					mangCode = data[13].toString();
					mangCodeYN = massorderDAO.getVstring(mangCode);
				}
				
				if (type == RESOURCE_VAR.TYPE_SPD) {

					if (data[6].equals("") && data[7].equals("") && data[8].equals("")) {
						spdTrustExist = false;
					} else {
						spdTrustExist = true;
					}

					if (data[2].equals("") && data[3].equals("") && data[4].equals("")
							&& data[5].equals("")) {
						spdOperExist = false;
					} else {
						spdOperExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING) {

					// 배차정보가 비어있는지
					// 위탁계약정보가 비어있는지 확인하여 true false 분기 검증??

					if (data[RESOURCE_VAR.OPER_P1].equals("")
							&& data[RESOURCE_VAR.OPER_P2].equals("")
							&& data[RESOURCE_VAR.OPER_P3].equals("")
							&& data[RESOURCE_VAR.OPER_P4].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_P1].equals("")
							&& data[RESOURCE_VAR.TRUST_P2].equals("")
							&& data[RESOURCE_VAR.TRUST_P3].equals("")
							&& data[RESOURCE_VAR.TRUST_P4].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}

				} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {

					// 2017. 09. 20. written by dyahn 순수주선양식 위탁/배차 입력항목 검증

					isOperExist = false; // 배차계약은 입력하지 않기 때문에 false로 고정
					if (data[RESOURCE_VAR.TRUST_P1_FORWARDONLY].equals("")
							&& data[RESOURCE_VAR.TRUST_P2_FORWARDONLY].equals("")
							&& data[RESOURCE_VAR.TRUST_P3_FORWARDONLY].equals("")
							&& data[RESOURCE_VAR.TRUST_P4_FORWARDONLY].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
					if (data[RESOURCE_VAR.OPER_D_P1].equals("")
							&& data[RESOURCE_VAR.OPER_D_P2].equals("")
							&& data[RESOURCE_VAR.OPER_D_P3].equals("")
							&& data[RESOURCE_VAR.OPER_D_P4].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_D_P1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P2].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P3].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P4].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT) {
					if (data[RESOURCE_VAR.OPER_P1].equals("")
							&& data[RESOURCE_VAR.OPER_P2].equals("")
							&& data[RESOURCE_VAR.OPER_P3].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_P1 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_P2 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_P3 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_P4 - 1].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT_DAEHANG) {
					if (data[RESOURCE_VAR.OPER_D_P1].equals("")
							&& data[RESOURCE_VAR.OPER_D_P2].equals("")
							&& data[RESOURCE_VAR.OPER_D_P3].equals("")) {
						isOperExist = false;
					} else {
						isOperExist = true;
					}

					if (data[RESOURCE_VAR.TRUST_D_P1 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P2 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P3 - 1].equals("")
							&& data[RESOURCE_VAR.TRUST_D_P4 - 1].equals("")) {
						isTruExist = false;
					} else {
						isTruExist = true;
					}
				}

				// System.out.println("checkBit : " + checkBit);
				for (int i = 0; i < data.length; i++) {
					String bitStr = String.valueOf(checkBit.charAt(i));
					// System.out.println("bitStr : " + bitStr);
					if (!bitStr.equals("0")) {
						if (type == RESOURCE_VAR.TYPE_SPD) {
							// 택배
							if (spdOperExist && spdTrustExist) {// 배차 위탁 모두 값이 존재할때
								if (String
										.valueOf(RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("1")
										|| String
												.valueOf(RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("2")
										|| String.valueOf(RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING.charAt(i)).equals("3")) {
									if (ValidateBasic.getInstance(false, data[0], data[i], bitStr,
											type, i, false, mangCnt, 0).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								} else if (String.valueOf(
										RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING.charAt(i))
										.equals("4")) {
									if (ValidateBasic.getInstance(false, data[0], data[i], "v",
											type, i, false, mangCnt, 0).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								} else if (String.valueOf(
										RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING.charAt(i))
										.equals("5")) {
									if (ValidateBasic.getInstance(false, data[0], data[i], bitStr,
											type, i, false, mangCnt, 0).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}

							if (!spdOperExist && spdTrustExist) {
								if (String
										.valueOf(RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("2")
										|| String
												.valueOf(RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("4")) {
									if (ValidateBasic.getInstance(false, data[0], data[i], bitStr,
											type, i, false, mangCnt, 0).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								} else if (String.valueOf(
										RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING.charAt(i))
										.equals("5")) {
									if (ValidateBasic.getInstance(false, data[0], data[i], "l",
											type, i, false, mangCnt, 0).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}

							if (spdOperExist && !spdTrustExist) {// 뒤만 없을때
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING.charAt(i))
										.equals("1") 
										|| String.valueOf(RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING.charAt(i)).equals("3")) {
									if (ValidateBasic.getInstance(false, data[0], data[i], bitStr,
											type, i, false, mangCnt, 0).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								} else if (String.valueOf(
										RESOURCE_VAR.TYPE_SPD_EMPTY_VERIFYBITSTRING.charAt(i))
										.equals("5")) {
									if (ValidateBasic.getInstance(false, data[0], data[i], "l",
											type, i, false, mangCnt, 0).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}
						} else if (type == RESOURCE_VAR.TYPE_SMALLING) {
							// 일반
							boolean isTransCustomer = !(data[1].equals("01"));
							if ((isOperExist && isTruExist) || (!isOperExist && !isTruExist)) { // 배차와
																								// 위탁이
																								// 모두
																								// 비어있는
																								// 경우는
																								// 없겠지만
																								// 코드는
																								// 유지

								if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i], bitStr,
										type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
									// 순수주선 여부 확인
									checkingCount++;
									basicCheck = true;
									isValid = false;
									error = ValidateBasic.checkErrCode();
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", error.getCode());
									errMap.put("eMsg", error.getMessage());
									errMap.put("eDetail", error.getDetail());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							} else if (isOperExist && !isTruExist) {
								if (String
										.valueOf(RESOURCE_VAR.TYPE_SMALLING_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("2")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										// 순수주선 여부 확인
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							} else if (!isOperExist && isTruExist) {
								if (String
										.valueOf(RESOURCE_VAR.TYPE_SMALLING_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("3")) {


									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										// 순수주선 여부 확인
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}

						} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
							// 순수주선
							boolean isTransCustomer = !(data[1].equals("01"));
							if ((isOperExist && isTruExist) || (!isOperExist && !isTruExist)) { // 배차와
																								// 위탁이
																								// 모두
																								// 비어있는
																								// 경우는
																								// 없겠지만
																								// 코드는
																								// 유지
								if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i], bitStr,
										type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
									checkingCount++;
									basicCheck = true;
									isValid = false;
									error = ValidateBasic.checkErrCode();
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", error.getCode());
									errMap.put("eMsg", error.getMessage());
									errMap.put("eDetail", error.getDetail());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							} else if (!isOperExist && isTruExist) {
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("3")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}
						} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
							// 대행
							boolean isTransCustomer = !(data[1].equals("01"));
							if ((isOperExist && isTruExist) || (!isOperExist && !isTruExist)) { // 배차와
																								// 위탁이
																								// 모두
																								// 비어있는
																								// 경우는
																								// 없겠지만
																								// 코드는
																								// 유지
								if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i], bitStr,
										type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
									checkingCount++;
									basicCheck = true;
									isValid = false;
									error = ValidateBasic.checkErrCode();
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", error.getCode());
									errMap.put("eMsg", error.getMessage());
									errMap.put("eDetail", error.getDetail());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							} else if (isOperExist && !isTruExist) {
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SMALLING_DAEHANG_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("2")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							} else if (!isOperExist && isTruExist) {
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SMALLING_DAEHANG_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("3")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}
						} else if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT) {
							boolean isTransCustomer = !(data[1].equals("01"));
							if ((isOperExist && isTruExist) || (!isOperExist && !isTruExist)) { // 배차와
																								// 위탁이
																								// 모두
																								// 비어있는
																								// 경우는
																								// 없겠지만
																								// 코드는
																								// 유지
								if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i], bitStr,
										type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
									checkingCount++;
									basicCheck = true;
									isValid = false;
									error = ValidateBasic.checkErrCode();
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", error.getCode());
									errMap.put("eMsg", error.getMessage());
									errMap.put("eDetail", error.getDetail());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							} else if (isOperExist && !isTruExist) {
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SMALLING_CONVERT_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("2")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							} else if (!isOperExist && isTruExist) {
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SMALLING_CONVERT_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("3")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}
						} else if (type == RESOURCE_VAR.TYPE_SMALLING_CONVERT_DAEHANG) {
							boolean isTransCustomer = !(data[1].equals("01"));
							if ((isOperExist && isTruExist) || (!isOperExist && !isTruExist)) { // 배차와
																								// 위탁이
																								// 모두
																								// 비어있는
																								// 경우는
																								// 없겠지만
																								// 코드는
																								// 유지
								if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i], bitStr,
										type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
									checkingCount++;
									basicCheck = true;
									isValid = false;
									error = ValidateBasic.checkErrCode();
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", error.getCode());
									errMap.put("eMsg", error.getMessage());
									errMap.put("eDetail", error.getDetail());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							} else if (isOperExist && !isTruExist) {
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("2")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							} else if (!isOperExist && isTruExist) {
								if (String.valueOf(
										RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_EMPTY_VERIFYBITSTRING
												.charAt(i))
										.equals("0")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("1")
										|| String.valueOf(
												RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_EMPTY_VERIFYBITSTRING
														.charAt(i))
												.equals("3")) {
									if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i],
											bitStr, type, i, isTransCustomer, mangCnt, mangCodeYN).validate()) {
										checkingCount++;
										basicCheck = true;
										isValid = false;
										error = ValidateBasic.checkErrCode();
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", error.getCode());
										errMap.put("eMsg", error.getMessage());
										errMap.put("eDetail", error.getDetail());
										errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
									}
								}
							}
						} else {
							boolean isTransCustomer = !(data[1].equals("01"));
							if (ValidateBasic.getInstance(isOnly_0201, data[0], data[i], bitStr,
									type, i, isTransCustomer, mangCnt, 0).validate()) {
								checkingCount++;
								basicCheck = true;
								isValid = false;
								error = ValidateBasic.checkErrCode();
								errMap.put("fname", fname);
								errMap.put("row", k+3);
								errMap.put("eCode", error.getCode());
								errMap.put("eMsg", error.getMessage());
								errMap.put("eDetail", error.getDetail());
								errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
							}
						}
					}
				}
				if (!errMap.isEmpty()) {
					int getKey = (Integer)errMap.get("row");
					if (k+3 == getKey) {
						for(int m=0; m<data.length; m++) {
							if (!errMap.containsKey("data"+m)) {
								errMap.put("data"+m, listDataParam.get(k)[m]);
							}
						}
						setErrDataList(errMap);
					}
				}

				// 계약년월, 위탁계약년월, 배차년월 날짜 체크 라인단위로 체크
				if (type == RESOURCE_VAR.TYPE_SPD) {
					// 택배
					//System.out.println(k + "|" + " 택배");
					//System.out.println(data[1]);
					String master_date = data[1];
					String car_date = data[3];
					String sub_date = data[7];

					if (master_date.equals(""))
						master_date = "0";
					if (car_date.equals(""))
						car_date = "0";
					if (sub_date.equals(""))
						sub_date = "0";
					
					int data1 = Integer.parseInt(master_date);
					int data3 = Integer.parseInt(car_date);
					int data7 = Integer.parseInt(sub_date);
					// System.out.println("date : " + master_date + " | " + car_date + " | " +
					// sub_date);
					if (ValidateBasic
							.getInstance(true, "", master_date + "/" + car_date + "/" + sub_date,
									"dateCheck", type, 1, true, mangCnt, mangCodeYN)
							.validate()) {
						checkingCount++;
						basicCheck = true;
						isValid = false;
						//error = ValidateBasic.checkErrCode();
						errMap.put("fname", fname);
						errMap.put("row", k+3); 
						errMap.put("eCode", ErrorType.DAT005.getCode());
						errMap.put("eMsg", ErrorType.DAT005.getMessage());
						errMap.put("eDetail", ErrorType.DAT005.getDetailMessage());
						for(int m=0; m<data.length; m++) {
							errMap.put("data"+m, listDataParam.get(k)[m]);
						}
						errMap.put("data1", listDataParam.get(k)[1]+"##FAIL##");
						if(data1 < data3) {
							errMap.put("data3", listDataParam.get(k)[3]+"##FAIL##");
						}
						if(data1 < data7) {
							errMap.put("data7", listDataParam.get(k)[7]+"##FAIL##");
						}
						setErrDataList(errMap);
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING) {
					// 일반
					//System.out.println(k + "|" + " 일반");

					String master_date = data[3];
					String car_date = data[8];
					String sub_date = data[12];

					if (master_date.equals(""))
						master_date = "0";
					if (car_date.equals(""))
						car_date = "0";
					if (sub_date.equals(""))
						sub_date = "0";

					int data3 = Integer.parseInt(master_date);
					int data8 = Integer.parseInt(car_date);
					int data12 = Integer.parseInt(sub_date);
					// System.out.println("date : " + master_date + " | " + car_date + " | " +
					// sub_date);

					if (ValidateBasic
							.getInstance(true, "", master_date + "/" + car_date + "/" + sub_date,
									"dateCheck", type, 3, true, mangCnt, mangCodeYN)
							.validate()) {
						checkingCount++;
						basicCheck = true;
						isValid = false;
						//error = ValidateBasic.checkErrCode();
						errMap.put("fname", fname);
						errMap.put("row", k+3);
						errMap.put("eCode", ErrorType.DAT005.getCode());
						errMap.put("eMsg", ErrorType.DAT005.getMessage());
						errMap.put("eDetail", ErrorType.DAT005.getDetailMessage());
						for(int m=0; m<data.length; m++) {
							errMap.put("data"+m, listDataParam.get(k)[m]);
						}
						errMap.put("data3", listDataParam.get(k)[3]+"##FAIL##");
						if(data3 < data8) {
							errMap.put("data8", listDataParam.get(k)[8]+"##FAIL##");
						}
						if(data3 < data12) {
							errMap.put("data12", listDataParam.get(k)[12]+"##FAIL##");
						}
						setErrDataList(errMap);
					}

				} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
					// 순수주선
					//System.out.println(k + "|" + " 순수주선");
					String master_date = data[3];
					String car_date = data[8];
					String sub_date = "";

					if (master_date.equals(""))
						master_date = "0";
					if (car_date.equals(""))
						car_date = "0";
					if (sub_date.equals(""))
						sub_date = "0";
					
					int data3 = Integer.parseInt(master_date);
					int data8 = Integer.parseInt(car_date);


					// System.out.println("date : " + master_date + " | " + car_date + " | " +
					// sub_date);

					if (ValidateBasic
							.getInstance(true, "", master_date + "/" + car_date + "/" + sub_date,
									"dateCheck", type, 3, true, mangCnt, mangCodeYN)
							.validate()) {
						checkingCount++;
						basicCheck = true;
						isValid = false;
						//error = ValidateBasic.checkErrCode();
						errMap.put("fname", fname);
						errMap.put("row", k+3);
						errMap.put("eCode", ErrorType.DAT005.getCode());
						errMap.put("eMsg", ErrorType.DAT005.getMessage());
						errMap.put("eDetail", ErrorType.DAT005.getDetailMessage());
						for(int m=0; m<data.length; m++) {
							errMap.put("data"+m, listDataParam.get(k)[m]);
						}
						errMap.put("data3", listDataParam.get(k)[3]+"##FAIL##");
						if(data3 < data8) {
							errMap.put("data8", listDataParam.get(k)[8]+"##FAIL##");
						}
						setErrDataList(errMap);
					}

				} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
					// 대행
					//System.out.println(k + "|" + " 대행");
					String master_date = data[4];
					String car_date = data[9];
					String sub_date = data[13];

					if (master_date.equals(""))
						master_date = "0";
					if (car_date.equals(""))
						car_date = "0";
					if (sub_date.equals(""))
						sub_date = "0";

					int data4 = Integer.parseInt(master_date);
					int data9 = Integer.parseInt(car_date);
					int data13 = Integer.parseInt(sub_date);
					// System.out.println("date : " + master_date + " | " + car_date + " | " +
					// sub_date);

					if (ValidateBasic
							.getInstance(true, "", master_date + "/" + car_date + "/" + sub_date,
									"dateCheck", type, 4, true, mangCnt, mangCodeYN)
							.validate()) {
						checkingCount++;
						basicCheck = true;
						isValid = false;
						//error = ValidateBasic.checkErrCode();
						errMap.put("fname", fname);
						errMap.put("row", k+3);
						errMap.put("eCode", ErrorType.DAT005.getCode());
						errMap.put("eMsg", ErrorType.DAT005.getMessage());
						errMap.put("eDetail", ErrorType.DAT005.getDetailMessage());
						for(int m=0; m<data.length; m++) {
							errMap.put("data"+m, listDataParam.get(k)[m]);
						}
						errMap.put("data4", listDataParam.get(k)[4]+"##FAIL##");
						if(data4 < data9) {
							errMap.put("data9", listDataParam.get(k)[9]+"##FAIL##");
						}
						if(data4 < data13) {
							errMap.put("data13", listDataParam.get(k)[13]+"##FAIL##");
						}
						setErrDataList(errMap);
					}
				}
				if (basicCheck)
					basicFormLineMap.put(k, ValidateBasic.getInstance().getColList());

				basicCheck = false;
			}
			ExcelExportManager.setBasicFormLineMap(basicFormLineMap);
			dvr.setBasicFormLineMap(basicFormLineMap);
			dvr.setUnvalidCount(checkingCount);
			dvr.setValid(isValid);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		return dvr;
	}

	@Override
	public boolean checkIdentifyData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type) {

		DataVerifyResult dvr = chkIdentifyData(fname, importDataSet, listDataParam, checkBit, type);

		if(!dvr.isValid()){
			ExcelExportManager.setFlag_identifyData(false);
		} else {
			ExcelExportManager.setFlag_identifyData(true);
		}
		return dvr.isValid();
	}

	private DataVerifyResult chkIdentifyData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			int type) {

		DataVerifyResult dvr = new DataVerifyResult();
		CustomHashMapStartWithKey<String, String> gubunKeyMap =
				new CustomHashMapStartWithKey<String, String>();
		int insertCount = 0;

		/* Collections.sort(listDataParam, new DataComparatorForContractGubun()); */

		try {

			if (type == RESOURCE_VAR.TYPE_SMALLING) {
				boolean isIdentifyValid = true;
				Map<String, Character> errorKey = new HashMap<String, Character>();
				int checkingCount = 0;
				//String currentKey = "";
				//String[] currentDatas = new String[4];
				String uuid = UUID.randomUUID().toString();
				
				List<ContractInfoForSmalling> paramList = new ArrayList<ContractInfoForSmalling>();
				List<ContractInfoForSmalling> result = new ArrayList<ContractInfoForSmalling>();
				for (int k = 0; k < listDataParam.size(); k++) {
					ContractInfoForSmalling contractInfoVO = new ContractInfoForSmalling();
					String[] data = listDataParam.get(k);
					
					contractInfoVO.setRegID(uuid);
					contractInfoVO.setContractCount(Integer.toString(k));
					contractInfoVO.setAgencyUsrMstKey("");
					contractInfoVO.setCont_m_key(data[2]);
					if(data[3].length()>6) {
						contractInfoVO.setContStart(data[3].substring(0, 6)); //20231019 chbaek 계약고유번호 검증이라 일단 자름 계약년월 검증은 다른데서 하는듯
					}else {
						contractInfoVO.setContStart(data[3]);
					}
					contractInfoVO.setCharge(data[4]);
					contractInfoVO.setDeliveryType(data[5]);
					contractInfoVO.setAnotherOper(data[6]);
					
					paramList.add(contractInfoVO);
				}
				
				massorderDAO.insertIdentifyData(paramList);
				result = massorderDAO.selectIdentifyData(uuid);
				
				if (result.size() > 0) {
					for (int i=0; i<result.size(); i++) {
						Map<String,Object> errMap = new HashMap<String,Object>();
						
						String tmprow = result.get(i).getContractCount();
						int row = Integer.parseInt(tmprow);
						String[] data = listDataParam.get(row);
						
						errorKey.put(data[2], '0');
						checkingCount++;
						isIdentifyValid = false;
						errMap.put("fname", fname);
						errMap.put("row", row+3);
						errMap.put("eCode", ErrorType.OVE001.getCode());
						errMap.put("eMsg", ErrorType.OVE001.getMessage());
						errMap.put("eDetail", ErrorType.OVE001.getDetailMessage());
						for(int m=0; m<data.length; m++) {
							errMap.put("data"+m, listDataParam.get(row)[m]);
						}
						errMap.put("data2", listDataParam.get(row)[2]+"##FAIL##");
						setErrDataList(errMap);
					}
				}
				
/*				for (int k = 0; k < listDataParam.size(); k++) {
					Map<String,Object> errMap = new HashMap<String,Object>();
					String[] data = listDataParam.get(k);

					if (k == 0) {

						currentKey = data[2];
						currentDatas[0] = data[3];
						currentDatas[1] = data[4];
						currentDatas[2] = data[5];
						currentDatas[3] = data[6];
					} else {
						if (data[2].equals("")) {
							currentKey = "";
							currentDatas[0] = "";
							currentDatas[1] = "";
							currentDatas[2] = "";
							currentDatas[3] = "";
						} else {
							if (!errorKey.containsKey(data[2])) {
								if (currentKey.equals(data[2])) {
									if (currentDatas[0].equals(data[3])
											&& currentDatas[1].equals(data[4])
											&& currentDatas[2].equals(data[5])
											&& currentDatas[3].equals(data[6])) {
									} else {
										errorKey.put(data[2], '0');
										checkingCount++;
										isIdentifyValid = false;
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", ErrorType.OVE001.getCode());
										errMap.put("eMsg", ErrorType.OVE001.getMessage());
										errMap.put("eDetail", ErrorType.OVE001.getDetailMessage());
										for(int m=0; m<data.length; m++) {
											errMap.put("data"+m, listDataParam.get(k)[m]);
										}
										errMap.put("data2", listDataParam.get(k)[2]+"##FAIL##");
										setErrDataList(errMap);
									}
								} else {
									currentKey = data[2];
									currentDatas[0] = data[3];
									currentDatas[1] = data[4];
									currentDatas[2] = data[5];
									currentDatas[3] = data[6];
								}
							}
						}
					}

				} */
				ExcelExportManager.setIdentifyLineMap(errorKey);
				dvr.setIdentifyLineMap(errorKey);
				dvr.setUnvalidCount(checkingCount);
				dvr.setValid(isIdentifyValid);
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
				boolean isIdentifyValid = true;
				Map<String, Character> errorKey = new HashMap<String, Character>();
				int checkingCount = 0;
				String currentKey = "";
				String[] currentDatas = new String[4];
				String uuid = UUID.randomUUID().toString();
				
				List<ContractInfoForSmalling> paramList = new ArrayList<ContractInfoForSmalling>();
				List<ContractInfoForSmalling> result = new ArrayList<ContractInfoForSmalling>();
				for (int k = 0; k < listDataParam.size(); k++) {
					ContractInfoForSmalling contractInfoVO = new ContractInfoForSmalling();
					String[] data = listDataParam.get(k);
					
					contractInfoVO.setRegID(uuid);
					contractInfoVO.setContractCount(Integer.toString(k));
					contractInfoVO.setAgencyUsrMstKey("");
					contractInfoVO.setCont_m_key(data[2]);
					contractInfoVO.setContStart(data[3]);
					contractInfoVO.setCharge(data[4]);
					contractInfoVO.setDeliveryType(data[5]);
					contractInfoVO.setAnotherOper(data[6]);
					
					paramList.add(contractInfoVO);
				}
				
				massorderDAO.insertIdentifyData(paramList);
				result = massorderDAO.selectIdentifyData(uuid);
				
				if (result.size() > 0) {
					for (int i=0; i<result.size(); i++) {
						Map<String,Object> errMap = new HashMap<String,Object>();
						
						String tmprow = result.get(i).getContractCount();
						int row = Integer.parseInt(tmprow);
						String[] data = listDataParam.get(row);
						
						errorKey.put(data[2], '0');
						checkingCount++;
						isIdentifyValid = false;
						errMap.put("fname", fname);
						errMap.put("row", row+3);
						errMap.put("eCode", ErrorType.OVE002.getCode());
						errMap.put("eMsg", ErrorType.OVE002.getMessage());
						errMap.put("eDetail", ErrorType.OVE002.getDetailMessage());
						for(int m=0; m<data.length; m++) {
							errMap.put("data"+m, listDataParam.get(row)[m]);
						}
						errMap.put("data2", listDataParam.get(row)[2]+"##FAIL##");
						setErrDataList(errMap);
					}
				}
				/* for (int k = 0; k < listDataParam.size(); k++) {
					Map<String,Object> errMap = new HashMap<String,Object>();
					String[] data = listDataParam.get(k);

					if (k == 0) {
						currentKey = data[2];
						currentDatas[0] = data[3];
						currentDatas[1] = data[4];
						currentDatas[2] = data[5];
						currentDatas[3] = data[6];
					} else {
						if (data[2].equals("")) {
							currentKey = "";
							currentDatas[0] = "";
							currentDatas[1] = "";
							currentDatas[2] = "";
							currentDatas[3] = "";
						} else {
							if (!errorKey.containsKey(data[2])) {
								if (currentKey.equals(data[2])) {
									if (currentDatas[0].equals(data[3])
											&& currentDatas[1].equals(data[4])
											&& currentDatas[2].equals(data[5])
											&& currentDatas[3].equals(data[6])) {
									} else {
										errorKey.put(data[2], '0');
										checkingCount++;
										isIdentifyValid = false;
										errMap.put("fname", fname);
										errMap.put("row", k+3);
										errMap.put("eCode", ErrorType.OVE002.getCode());
										errMap.put("eMsg", ErrorType.OVE002.getMessage());
										errMap.put("eDetail", ErrorType.OVE002.getDetailMessage());
										for(int m=0; m<data.length; m++) {
											errMap.put("data"+m, listDataParam.get(k)[m]);
										}
										errMap.put("data2", listDataParam.get(k)[2]+"##FAIL##");
										setErrDataList(errMap);
									}
								} else {
									currentKey = data[2];
									currentDatas[0] = data[3];
									currentDatas[1] = data[4];
									currentDatas[2] = data[5];
									currentDatas[3] = data[6];
								}
							}
						}
					}

				} */
				ExcelExportManager.setIdentifyLineMap(errorKey);
				dvr.setIdentifyLineMap(errorKey);
				dvr.setUnvalidCount(checkingCount);
				dvr.setValid(isIdentifyValid);
				if (!isIdentifyValid) {
//					error = err;
				}
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				boolean isIdentifyValid = true;
				Map<String, Character> errorKey = new HashMap<String, Character>();
				int checkingCount = 0;
				String currentKey = "";
				String[] currentDatas = new String[5];
				String uuid = UUID.randomUUID().toString();
				
				List<ContractInfoForSmalling> paramList = new ArrayList<ContractInfoForSmalling>();
				List<ContractInfoForSmalling> result = new ArrayList<ContractInfoForSmalling>();
				for (int k = 0; k < listDataParam.size(); k++) {
					ContractInfoForSmalling contractInfoVO = new ContractInfoForSmalling();
					String[] data = listDataParam.get(k);
					
					contractInfoVO.setRegID(uuid);
					contractInfoVO.setContractCount(Integer.toString(k));
					contractInfoVO.setCont_m_key(data[2]);
					contractInfoVO.setAgencyUsrMstKey(data[3]);
					contractInfoVO.setContStart(data[4]);
					contractInfoVO.setCharge(data[5]);
					contractInfoVO.setDeliveryType(data[6]);
					contractInfoVO.setAnotherOper(data[7]);
					
					paramList.add(contractInfoVO);
				}
				
				massorderDAO.insertIdentifyData(paramList);
				result = massorderDAO.selectIdentifyData(uuid);
				
				if (result.size() > 0) {
					for (int i=0; i<result.size(); i++) {
						Map<String,Object> errMap = new HashMap<String,Object>();
						
						String tmprow = result.get(i).getContractCount();
						int row = Integer.parseInt(tmprow);
						String[] data = listDataParam.get(row);
						
						errorKey.put(data[2], '0');
						checkingCount++;
						isIdentifyValid = false;
						errMap.put("fname", fname);
						errMap.put("row", row+3);
						errMap.put("eCode", ErrorType.OVE003.getCode());
						errMap.put("eMsg", ErrorType.OVE003.getMessage());
						errMap.put("eDetail", ErrorType.OVE003.getDetailMessage());
						for(int m=0; m<data.length; m++) {
							errMap.put("data"+m, listDataParam.get(row)[m]);
						}
						errMap.put("data2", listDataParam.get(row)[2]+"##FAIL##");
						setErrDataList(errMap);
					}
				}
				/* for (int k = 0; k < listDataParam.size(); k++) {
					Map<String,Object> errMap = new HashMap<String,Object>();
					String[] data = listDataParam.get(k);

					if (data[2].equals("")) {
						currentKey = "";
						currentDatas[0] = "";
						currentDatas[1] = "";
						currentDatas[2] = "";
						currentDatas[3] = "";
						currentDatas[4] = "";
					} else {
						if (!errorKey.containsKey(data[2])) {
							if (currentKey.equals(data[2])) {
								if (currentDatas[0].equals(data[3])
										&& currentDatas[1].equals(data[4])
										&& currentDatas[2].equals(data[5])
										&& currentDatas[3].equals(data[6])
										&& currentDatas[4].equals(data[7])) {
								} else {
									errorKey.put(data[2], '0');
									checkingCount++;
									isIdentifyValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.OVE003.getCode());
									errMap.put("eMsg", ErrorType.OVE003.getMessage());
									errMap.put("eDetail", ErrorType.OVE003.getDetailMessage());
									for(int m=0; m<data.length; m++) {
										errMap.put("data"+m, listDataParam.get(k)[m]);
									}
									errMap.put("data2", listDataParam.get(k)[2]+"##FAIL##");
									setErrDataList(errMap);
								}
							} else {
								currentKey = data[2];
								currentDatas[0] = data[3];
								currentDatas[1] = data[4];
								currentDatas[2] = data[5];
								currentDatas[3] = data[6];
								currentDatas[4] = data[7];
							}
						}
					}
				} */
				ExcelExportManager.setIdentifyLineMap(errorKey);
				dvr.setIdentifyLineMap(errorKey);
				dvr.setUnvalidCount(checkingCount);
				dvr.setValid(isIdentifyValid);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		return dvr;
	}

	@Override
	public boolean checkContfromRangeData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type) {

		DataVerifyResult dvr = chkContfromRange(fname, importDataSet, listDataParam, checkBit, type);

		if (!dvr.isValid()) {
			ExcelExportManager.setFlag_contData(false);
			ExcelExportManager.setcontBit(checkBit);
		} else {
			ExcelExportManager.setFlag_contData(true);
		}
		return dvr.isValid();
	}

	/**
	 * 계약년월이 분기를 넘어가는 경우를 체크
	 */
	private DataVerifyResult chkContfromRange(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type) {

		
		DataVerifyResult dvr = new DataVerifyResult();
		int currentBungi = 0;
		int base_year = 0;

		try {
			Map<Integer, Character> errorLineMap = new HashMap<Integer, Character>();
			boolean isErrorValid = true;
			int c_year = Calendar.getInstance().get(Calendar.YEAR);
			int checkingCount = 0;

			for (int k = 0; k < listDataParam.size(); k++) {
				Map<String,Object> errMap = new HashMap<String,Object>();
				String[] data = listDataParam.get(k);

				for (int i = 0; i < data.length; i++) {
					// 2018. 01. 02. blocked by dyahn 분기단위-> 연단위로 변경 (주석된 부분은 분기단위 검증프로세스)
					if (String.valueOf(checkBit.charAt(i)).equals("1")) {
						if (!data[i].equals("")) {

							int d_year = Integer.parseInt(data[i].substring(0, 4));
							// //System.out.println("첫번째 i는 : " + i);
							// //System.out.println("체크된 계약년월 : " + data[i]);
							// //System.out.println("계약년(d_year) : " + d_year);
							// //System.out.println("현재년(c_year) : " + c_year);
							// //System.out.println("base_year(base_year) : " + base_year);
							// //System.out.println("currentBungi : " + currentBungi);

							if (currentBungi == 0) {
								currentBungi = c_year;
								base_year = d_year;
							} else {
								if (base_year != d_year) {
									checkingCount++;

									if (!errorLineMap.containsKey(k)) {
										errorLineMap.put(k, '0');
									}
									isErrorValid = false;
									errMap.put("fname", fname);
									errMap.put("row", k+3);
									errMap.put("eCode", ErrorType.RAN001.getCode());
									errMap.put("eMsg", ErrorType.RAN001.getMessage());
									errMap.put("eDetail", ErrorType.RAN001.getDetailMessage());
									errMap.put("data"+i, listDataParam.get(k)[i]+"##FAIL##");
								}
							}
						}
					}
				}
				if (!errMap.isEmpty()) {
					int getKey = (Integer)errMap.get("row");
					if (k+3 == getKey) {
						for(int m=0; m<data.length; m++) {
							if (!errMap.containsKey("data"+m)) {
								errMap.put("data"+m, listDataParam.get(k)[m]);
							}
						}
						setErrDataList(errMap);
					}
				}
			}
			ExcelExportManager.setContLineMap(errorLineMap);
			dvr.setEmptyLineMap(errorLineMap);
			dvr.setUnvalidCount(checkingCount);
			dvr.setValid(isErrorValid);

		} catch (Exception e) {
			e.printStackTrace();

			importCode(importDataSet, fname, "COR001");
		}
		return dvr;
	}

	/*
	 * ============================================================== 
	 * [실적신고 없음] 검증 시작
	 * ==============================================================
	 */
	@Override
	public boolean checkNoRecordInfo(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type) {

		DataVerifyResult dvr = chkNoRecordInfo(fname, importDataSet, listDataParam, checkBit, type);
		
		Map<Integer, Character> noRecordLineMap = new HashMap<Integer, Character>();

		if (!dvr.isValid()) {
			if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				if (dvr.getArrMst_usr_key() != null) {
					String noReCordAll =
							dvr.getArrMst_usr_key().replace("[", "").replace("]", "").replace(" ", "");
					String[] tempList = noReCordAll.split(",");
					String[] noReCordList = new String[tempList.length];
					for (int i = 0; i < tempList.length; i++) {
						for (int j = i + 1; j < tempList.length; j++) {
							if (tempList[i].equals(tempList[j])) {
								tempList[j] = "";
							}
						}
					}
					
					for (int i = 0; i < tempList.length; i++) {
						if (!tempList[i].equals("")) {
							noReCordList[i] = tempList[i];
						}
						
					}
					
					if (noReCordList.length > 0) {
						ExcelExportManager.setFlag_noRecordDae(false);
						for (int i = 0; i < listDataParam.size(); i++) {
							if(noReCordList[i].equals(listDataParam.get(i)[3])) {
								Map<String,Object> errMap = new HashMap<String,Object>();
								String[] data = listDataParam.get(i);
								noRecordLineMap.put(i, '0');
								errMap.put("fname", fname);
								errMap.put("row", i+3);
								errMap.put("eCode", ErrorType.NOR002.getCode());
								errMap.put("eMsg", ErrorType.NOR002.getMessage());
								errMap.put("eDetail", ErrorType.NOR002.getDetailMessage());
								for(int k=0; k<data.length; k++) {
									errMap.put("data"+k, listDataParam.get(i)[k]);
								}
								errMap.put("data3", listDataParam.get(i)[3]+"##FAIL##");
								setErrDataList(errMap);
							}
						}
					}
					ExcelExportManager.setNoRecordtLineMap(noRecordLineMap);
					setNoReCordDeaList(false);
				} else {
					Map<String,Object> errMap = new HashMap<String,Object>();
					ExcelExportManager.setFlag_noRecord(false);
					errMap.put("fname", fname);
					errMap.put("eCode", ErrorType.NOR003.getCode());
					errMap.put("eMsg", ErrorType.NOR003.getMessage());
					errMap.put("eDetail", ErrorType.NOR003.getDetailMessage());
					setErrCodeList(errMap);
				}
			} else {
				Map<String,Object> errMap = new HashMap<String,Object>();
				ExcelExportManager.setFlag_noRecord(false);
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.NOR001.getCode());
				errMap.put("eMsg", ErrorType.NOR001.getMessage());
				errMap.put("eDetail", ErrorType.NOR001.getDetailMessage());
				setErrCodeList(errMap);
			}
		} else {
			ExcelExportManager.setFlag_noRecord(true);
		}

		// 22.03.03 jwchoi 실적신고제한 사업자번호 표출 추가
		return dvr.isValid();
	}

	private void setNoReCordDeaList(boolean flag) {
		noReCordDeaList = flag;
	}

	@Override
	public boolean checkNoRecordInfoDae() {
		return noReCordDeaList;
	}

	private DataVerifyResult chkNoRecordInfo(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			int type) {

		DataVerifyResult dvr = new DataVerifyResult();

		try {
			Map<String, String> recordQuarter = getRecordQuarter(fname, importDataSet, listDataParam, checkBit);
			// 기존
			// Map<String, Object> result = WebConnection.checkNoRecord(usr_mst_key, recordQuarter);

			// 개선
			// Map<String, Object> result = new HashMap<String, Object>();
			Map<String, Object> result = new HashMap<String, Object>();
			// result = WebConnection.checkNoRecord(usr_mst_key, recordQuarter);

			// 대행이면 실적주체 사업자번호로 실적없음 조회
			if (checkBit == RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTFROM_RANGE_VERIFYBITSTRING) {

				/* Collections.sort(listDataParam, new DataComparatorForContractGubun()); */

				String data[];
				String saveMstKeyList = "";
				String preMstKey = "";
				for (int i = 0; i < listDataParam.size(); i++) {
					data = listDataParam.get(i);
					for (int j = 0; j < data.length; j++) {
						if (j == 3) {
							if ("".equals(preMstKey)) {
								preMstKey = data[j];
								saveMstKeyList = data[j];
							} else {
								if (!data[j].equals(preMstKey)) {
									saveMstKeyList += ("," + data[j]);
									preMstKey = data[j];
								}
							}
						}
					}
				}

				// 대행 실적주체 실적없음
				result = checkNoRecordDeahang(fname, importDataSet, saveMstKeyList, recordQuarter);

				// result.get("DATA_LIST") -> 실적없음 신고자 배열
				if (result != null && "TRUE".equals((String) result.get("DATA"))) {
					dvr.setValid(false);
					if (result.get("DATA_LIST") != null) {
						dvr.setArrMst_usr_key(result.get("DATA_LIST").toString());						
					}
				} else if (result != null && "ERROR".equals((String) result.get("DATA"))) {
					dvr.setValid(false);
				} else {
					dvr.setValid(true);
				}

			} else {
				result = checkNoRecord(usr_mst_key, recordQuarter);
				// "TRUE".equals(result.get("DATA")) -> 실적없음 신고자
				if (result != null && "TRUE".equals((String) result.get("DATA"))) {
					// 여기로 들어옴
					dvr.setValid(false);

				} else if (result != null && "ERROR".equals((String) result.get("DATA"))) {
					dvr.setValid(false);
					if (!dvr.isValid()) {
						importCode(importDataSet, fname, "COR001");
					}
				} else {
					dvr.setValid(true);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		return dvr;
	}

	private Map<String, Object> checkNoRecord(String usrmstkey, Map<String, String> recordQuarter)
			throws DataAccessException {

		Map<String, Object> fpisAppStatusResult = new HashMap<String, Object>();
		String usrMstKey = usrmstkey;
		String recordYear = recordQuarter.get("year");
		String quarter = "0";
		int chkResult = 0;

		chkResult = massorderDAO.chkNoRecord(usrMstKey, recordYear, quarter);

		fpisAppStatusResult.put("DATA", (chkResult > 0) ? "TRUE" : "FALSE");
		fpisAppStatusResult.put("RESULT", "TRUE");

		return fpisAppStatusResult;
	}

	private Map<String, Object> checkNoRecordDeahang(String fname, ImportDataSet importDataSet, String saveMstKeyList,
			Map<String, String> recordQuarter) {

		Map<String, Object> fpisAppStatusResult = null;
		String usrMstKey = saveMstKeyList;
		String recordYear = recordQuarter.get("year");
		String quarter = "0";

		try {
			List<String> chkResult =
					uploadDAO.FpisChkNoRecordDeahang(usrMstKey, recordYear, quarter);
			fpisAppStatusResult = new HashMap<String, Object>();
			fpisAppStatusResult.put("DATA_LIST", chkResult.toString());
			fpisAppStatusResult.put("DATA", (chkResult.size() > 0) ? "TRUE" : "FALSE");
			fpisAppStatusResult.put("RESULT", "TRUE");
		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
			fpisAppStatusResult = new HashMap<String, Object>();
			fpisAppStatusResult.put("DATA", "ERROR");
			fpisAppStatusResult.put("RESULT", "TRUE");
		}

		return fpisAppStatusResult;
	}

	private Map<String, String> getRecordQuarter(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit) {
		Map<String, String> result = null;

		String[] data = listDataParam.get(0);

		for (int i = 0; i < data.length; i++) {
			if (String.valueOf(checkBit.charAt(i)).equals("1")) {
				// System.out.println("==============> data[i] : "+ data[i]);
				if (!data[i].equals(""))
					result = getBungiForQuarter(fname, importDataSet, data[i]);
			}
		}
		// System.out.println("==============> result : "+ result);
		return result;
	}

	private Map<String, String> getBungiForQuarter(String fname, ImportDataSet importDataSet, String contFrom) {
		//// System.out.println("======> contFrom : " + contFrom);
		Map<String, String> result = new HashMap<String, String>();
		int year = -9999;
		int month = -9999;
		try {
			year = Integer.parseInt(contFrom.substring(0, 4));
			month = Integer.parseInt(contFrom.substring(4, 6)); //20231019 chbaek 분기 체크하는 함수같은데 날짜까지 쓰면 여기서 오류나서 수정 현재 안쓰이는 기능인듯

			if (year == 2015 && month > 9) {
				month = 8;
			} /* 2015년도 3,4분기는 동일 마감일자로 판단됨에 따른 임시코드(2016년 3월에 삭제예정) */
		} catch (Exception e) {
			importCode(importDataSet, fname, "COR001");
			return null;
		}


		result.put("year", contFrom.substring(0, 4));

		switch (month) {
			case 1:
				result.put("bungi", "1Q");
				break;
			case 2:
				result.put("bungi", "1Q");
				break;
			case 3:
				result.put("bungi", "1Q");
				break;
			case 4:
				result.put("bungi", "2Q");
				break;
			case 5:
				result.put("bungi", "2Q");
				break;
			case 6:
				result.put("bungi", "2Q");
				break;
			case 7:
				result.put("bungi", "3Q");
				break;
			case 8:
				result.put("bungi", "3Q");
				break;
			case 9:
				result.put("bungi", "3Q");
				break;
			case 10:
				result.put("bungi", "4Q");
				break;
			case 11:
				result.put("bungi", "4Q");
				break;
			case 12:
				result.put("bungi", "4Q");
				break;
			default:
				result = null;
				break;
		}


		return result;
	}
	/*
	 * ============================================================== 
	 * [실적신고 없음] 검증 종료
	 * ==============================================================
	 */

	/*
	 * ============================================================== 
	 * 관할관청 등록 여부 검증 시작
	 * ==============================================================
	 */
	@Override
	public boolean checkUsrGov(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type) {
		// type :15 -> 일반,택배 간소화, type :16 -> 대행 (TYPE_SMALLING_DAEHANG)
		
		DataVerifyResult dvr = null;

		if (type < RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
			// System.out.println("일반 관할관청 ");
			dvr = _checkUsrGov(fname, importDataSet, listDataParam, checkBit);
			if (!dvr.isValid()) {
				ExcelExportManager.setFlag_usrGov(false);
			}
		} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
			Set<String> agencyUsrMstKey = getAgencyMemberUsrMstKey(listDataParam);
			dvr = _checkUsrGovDae(fname, importDataSet, listDataParam, checkBit, agencyUsrMstKey);
			if (!dvr.isValid()) {
				ExcelExportManager.setFlag_usrGovDae(false);
			}
		}
		// System.out.println("dvr.isValid() : " + dvr.isValid());

		if (!dvr.isValid()) {

		} else {
			ExcelExportManager.setFlag_usrGov(true);
			ExcelExportManager.setFlag_usrGovDae(true);
		}
		return dvr.isValid();
	}

	/* 관할관청 여부 검증(일반) */
	private DataVerifyResult _checkUsrGov(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit) {
		
		Map<String,Object> errMap = new HashMap<String,Object>();
		DataVerifyResult dvr = new DataVerifyResult();

		try {
			Map<String, Object> result = new HashMap<String, Object>();
			//List<String[]> errlistData = new ArrayList<String[]>();
			boolean errFlag = true;

			result = chkUsrGov(fname, importDataSet, usr_mst_key);

			if (result.size() == 0) {
				errFlag = false;
			} else {
				if ("fail".equals((String) result.get("status_result"))) {
					errFlag = false;
				}
			}

			if (!errFlag) {
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.DIS001.getCode());
				errMap.put("eMsg", ErrorType.DIS001.getMessage());
				errMap.put("eDetail", ErrorType.DIS001.getDetailMessage());
				setErrCodeList(errMap);
			}

			dvr.setValid(errFlag);
		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		return dvr;
	}

	private Map<String, Object> chkUsrGov(String fname, ImportDataSet importDataSet, String mstKeyList) {
		Map<String, Object> result = new HashMap<String, Object>();

		List<CheckRegModifyAllowVO> resultList = null;
		CheckRegModifyAllowVO checkRegModifyAllowVO = new CheckRegModifyAllowVO();
		String[] keyList = mstKeyList.split(",");

		String regTermination = "";
		// 신고 사업자 만큼 반복
		for (int i = 0; i < keyList.length; i++) {

			checkRegModifyAllowVO.setUsr_mst_key(keyList[i]);
		}
		try {
			resultList = uploadDAO.checkUsrGov(checkRegModifyAllowVO);

			for (int j = 0; j < resultList.size(); j++) {
				if ("U".equals(resultList.get(j).getStatus())
						|| "N".equals(resultList.get(j).getStatus())) {
					regTermination = "fail";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}

		result.put("status_result", regTermination);
		return result;
	}

	/* 대행 실적주체 사업자번호 */
	private Set<String> getAgencyMemberUsrMstKey(List<String[]> listDataParam) {
		Set<String> agencyUsrMstKey = getAgencyUsrMstKey(listDataParam);
		return agencyUsrMstKey;
	}

	/* 관할관청 여부 검증(대행) */
	private Set<String> getAgencyUsrMstKey(List<String[]> lists) {
		Set<String> agencyUsrMstKey = new HashSet<String>();
		boolean flag = true;
		
		for (int i = 0; i < lists.size(); i++) {
			for (int j = 0; j< noRegistUsrList.length;j++) {
				if(lists.get(i)[3].equals(noRegistUsrList[j])) {
					flag = false;
				}
			}
			if (flag) {
				agencyUsrMstKey.add(lists.get(i)[3]);				
			}
			flag = true;
		}
		return agencyUsrMstKey;
	}

	/* 관할관청 여부 검증(대행) */
	private DataVerifyResult _checkUsrGovDae(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			Set<String> agencyUsrMstKey) {

		Map<String, Object> result = new HashMap<String, Object>();
		Map<Integer, Character> usrGovDaeLineMap = new HashMap<Integer, Character>();
		
		DataVerifyResult dvr = new DataVerifyResult();
		List<String[]> suclistData = new ArrayList<String[]>();

		/* Collections.sort(listDataParam, new DataComparatorForContractGubun()); */

		// System.out.println("대행 관할관청 검증");

		String data[];
		String saveMstKeyList = "";
		String preMstKey = "";
		for (int i = 0; i < listDataParam.size(); i++) {
			data = listDataParam.get(i);
			for (int j = 0; j < data.length; j++) {
				if (j == 3) {
					if ("".equals(preMstKey)) {
						preMstKey = data[j];
						saveMstKeyList = data[j];
					} else {
						if (!data[j].equals(preMstKey)) {
							saveMstKeyList += ("," + data[j]);
							preMstKey = data[j];
						}
					}
				}
			}
		}
		// System.out.println(saveMstKeyList);
		try {
			result = chkUsrGovDaeList(fname, importDataSet, saveMstKeyList);
		} catch (Exception e) {
			e.printStackTrace();

			importCode(importDataSet, fname, "COR001");
		}

		boolean errorFlag = true;

		String failMstKeyList = "";
		String sucMstKeyList = "";
		int failCnt = 0;
		for (int i = 0; i < saveMstKeyList.split(",").length; i++) {
			if ("fail".equals(result.get("status_result").toString().split(",")[i])) {
				failMstKeyList += saveMstKeyList.split(",")[i] + ",";
				failCnt++;
			} else {
				sucMstKeyList += saveMstKeyList.split(",")[i] + ",";
			}
		}

		for (int j = 0; j < listDataParam.size(); j++) {
			if (failMstKeyList.indexOf(listDataParam.get(j)[3]) != -1) {
				Map<String,Object> errMap = new HashMap<String,Object>();
				String[] _data = listDataParam.get(j);
				usrGovDaeLineMap.put(j, '0');
				errMap.put("fname", fname);
				errMap.put("row", j+3);
				errMap.put("eCode", ErrorType.DIS002.getCode());
				errMap.put("eMsg", ErrorType.DIS002.getMessage());
				errMap.put("eDetail", ErrorType.DIS002.getDetailMessage());
				for(int k=0; k<_data.length; k++) {
					errMap.put("data"+k, listDataParam.get(j)[k]);
				}
				errMap.put("data3", listDataParam.get(j)[3]+"##FAIL##");
				setErrDataList(errMap);
			} else {
				suclistData.add(listDataParam.get(j));
			}
		}

		if (failMstKeyList.length() > 0) {
			errorFlag = false;
			setUsrGovDae(false);
		}

		// System.out.println("errlistData : " + errlistData.size());
		// System.out.println("suclistData : " + suclistData.size());
		ExcelExportManager.setUsrGovDae(usrGovDaeLineMap);
		dvr.setValid(errorFlag);
		dvr.setSucData(suclistData);

		return dvr;
	}

	/* 관할관청 여부 검증(대행) */
	private Map<String, Object> chkUsrGovDaeList(String fname, ImportDataSet importDataSet, String mstKeyList) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<CheckRegModifyAllowVO> resultList = null;
		CheckRegModifyAllowVO checkRegModifyAllowVO = new CheckRegModifyAllowVO();

		String[] keyList = mstKeyList.split(",");
		String regTermination = "";
		String statusList = "";

		for (int i = 0; i < keyList.length; i++) {

			checkRegModifyAllowVO.setUsr_mst_key(keyList[i]);
			try {
				resultList = uploadDAO.checkUsrGov(checkRegModifyAllowVO);

				for (int j = 0; j < resultList.size(); j++) {
					if ("U".equals(resultList.get(j).getStatus())
							|| "N".equals(resultList.get(j).getStatus())) {
						regTermination = "fail";
					}
				}

				if (!"fail".equals(regTermination)) {
					regTermination = "suc";
				}
				if (i == 0) {
					statusList = regTermination;
				} else {
					statusList += "," + regTermination;
				}
				regTermination = "";
			} catch (Exception e) {
				e.printStackTrace();
				importCode(importDataSet, fname, "COR001");
			}

		}
		result.put("status_result", statusList);
		return result;
	}
	
	private void setUsrGovDae(boolean flag) {
		noUsrGovDae = flag;
	}
	
	@Override
	public boolean checkUsrGovDae() {
		return noUsrGovDae;
	}

	/*
	 * ============================================================== 
	 * 관할관청 등록 여부 검증 종료
	 * ==============================================================
	 */
	@Override
	public String getImportDate(List<String[]> listDataParam, String checkBit, int type) {
		
		int yyyymm = 0;
		String[] data = null;

		for (int i = 0; i < listDataParam.size(); i++) {
			data = listDataParam.get(i);
			for (int j = 0; j < data.length; j++) {
				if (String.valueOf(checkBit.charAt(j)).equals("1")) {
					if (!data[j].equals("")) {

						if (yyyymm == 0)
							yyyymm = Integer.parseInt(data[j]);
						else if (yyyymm > Integer.parseInt(data[j]))
							yyyymm = Integer.parseInt(data[j]);
					}
				}
			}
		}
		String regMinDate = Integer.toString(yyyymm);
		return regMinDate;
	}

	@Override
	public boolean checkPreReg(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int COL_CNT, String importYear) {
		
		DataVerifyResult dvr = chkPreReg(fname, importDataSet, listDataParam, checkBit, importYear);
		
		if (!dvr.isValid()) {
			ExcelExportManager.setFlag_preReg(false);
			ExcelExportManager.setPreYear(importYear);
			 
		} else {
			ExcelExportManager.setFlag_preReg(true);
		}

		return dvr.isValid();
	}

	private DataVerifyResult chkPreReg(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, String importYear) {

		DataVerifyResult dvr = new DataVerifyResult();
		Map<Integer, Character> preRegLineMap = new HashMap<Integer, Character>();
		List<String[]> errlistData = new ArrayList<String[]>();
		List<String[]> suclistData = new ArrayList<String[]>();

		try {
			boolean isErrorValid = false;
			for (int i = 0; i < listDataParam.size(); i++) {
				Map<String,Object> errMap = new HashMap<String,Object>();
				String[] data = listDataParam.get(i);
				for (int j =0; j < data.length; j++) {
					if (String.valueOf(checkBit.charAt(j)).equals("1")) {
						if (data[j].contains(importYear)) { //2023.04.11 jwchoi 계약일자 완전일치>포함으로 수정
							preRegLineMap.put(i, '0');			
							errMap.put("fname", fname);
							errMap.put("row", i+3);
							errMap.put("eCode", ErrorType.PRE003.getCode());
							errMap.put("eMsg", ErrorType.PRE003.getMessage());
							errMap.put("eDetail", ErrorType.PRE003.getDetailMessage());
							errMap.put("data"+j, listDataParam.get(i)[j]+"##FAIL##");
						}
					}
					
				}
				if (!errMap.isEmpty()) {
					int getKey = (Integer)errMap.get("row");
					if (i+3 == getKey) {
						for(int m=0; m<data.length; m++) {
							if (!errMap.containsKey("data"+m)) {
								errMap.put("data"+m, listDataParam.get(i)[m]);
							}
						}
						setErrDataList(errMap);
					}
				}
			}
			ExcelExportManager.setPreReg(preRegLineMap);
			dvr.setErrData(errlistData);
			dvr.setSucData(suclistData);
			dvr.setValid(isErrorValid);
		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}

		return dvr;
	}

	private void setRegModifyAllowDae(boolean flag) {
		isRegModifyAlloweDae = flag;
	}

	@Override
	public boolean checkRegModifyAllowDae() {
		return isRegModifyAlloweDae;
	}
	
	@Override
	/* 수정허가여부 */
	public boolean checkRegModifyAllow(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type) {

		// type :15 -> 일반,택배 간소화, type :16 -> 대행
		DataVerifyResult dvr = null;
		Map<Integer, Character> regAllowLineMap = new HashMap<Integer, Character>();
		//String[] permitUsrList = null;
		
		if (type < RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
			dvr = chkRegModifyAllow(fname, importDataSet, listDataParam, checkBit);
		} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
			Set<String> agencyUsrMstKey = getAgencyMemberUsrMstKey(listDataParam);
			dvr = chkRegModifyAllow(fname, importDataSet, listDataParam, checkBit, agencyUsrMstKey);
		}

		if (!dvr.isValid()) {
			if (type < RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				if (!"".equals(usr_mst_key)) {
					Map<String,Object> errMap = new HashMap<String,Object>();
					ExcelExportManager.setFlag_regAllow(false);				
					errMap.put("fname", fname);
					errMap.put("eCode", ErrorType.PRE001.getCode());
					errMap.put("eMsg", ErrorType.PRE001.getMessage());
					errMap.put("eDetail", ErrorType.PRE001.getDetailMessage());
					setErrCodeList(errMap);
				}
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				if (dvr.getArrMst_usr_key() != null) {
					String noAllowAll =
							dvr.getArrMst_usr_key().replace("[", "").replace("]", "").replace(" ", "");
					String[] tempList = noAllowAll.split(",");
					String[] noAllowList = new String[tempList.length];
					for (int i = 0; i < tempList.length; i++) {
						for (int j = i + 1; j < tempList.length; j++) {
							if (tempList[i].equals(tempList[j])) {
								tempList[j] = "";
							}
						}
					}
					for (int i = 0; i < tempList.length; i++) {
						if (!tempList[i].equals("")) {
							noAllowList[i] = tempList[i];
						}
					}
					
					for (int i=0; i<noAllowList.length; i++) {
						if(usr_mst_key.equals(noAllowList[i])) {
							Map<String,Object> errMap = new HashMap<String,Object>();
							ExcelExportManager.setFlag_regAllow(false);
							errMap.put("fname", fname);
							errMap.put("eCode", ErrorType.PRE001.getCode());
							errMap.put("eMsg", ErrorType.PRE001.getMessage());
							errMap.put("eDetail", ErrorType.PRE001.getDetailMessage());
							setErrCodeList(errMap);
						}
					}
					if (ExcelExportManager.getFlag_regAllow()) {
						for (int i = 0; i < listDataParam.size(); i++) {
							Map<String,Object> errMap = new HashMap<String,Object>();
							for (int j = 0; j < noAllowList.length; j++) {
								if (listDataParam.get(i)[3].equals(noAllowList[j])) {
									regAllowLineMap.put(i, '0');
									errMap.put("fname", fname);
									errMap.put("row", i+3);
									errMap.put("eCode", ErrorType.PRE002.getCode());
									errMap.put("eMsg", ErrorType.PRE002.getMessage());
									errMap.put("eDetail", ErrorType.PRE002.getDetailMessage());
									for(int m=0; m<type; m++) {
										errMap.put("data"+m, listDataParam.get(i)[m]);
									}
									errMap.put("data3", listDataParam.get(i)[3]);
									setErrDataList(errMap);
								}
							}
						}
						ExcelExportManager.setFlag_regAllowDae(false);
						ExcelExportManager.setRegAllow(regAllowLineMap);
						setRegModifyAllowDae(false);
					}
				}
			}
		} else {
			ExcelExportManager.setFlag_regAllow(true);
			ExcelExportManager.setFlag_regAllowDae(true);
		}
		return dvr.isValid();
	}

	/* 수정허가여부(미대행 계정) */
	private DataVerifyResult chkRegModifyAllow(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit) {

		DataVerifyResult dvr = new DataVerifyResult();
		try {
			String regMinDate = getRecordMinDate(listDataParam, checkBit); // 실적일자중 최소날짜 가져오기

			Map<String, Object> result = new HashMap<String, Object>();
			List<String[]> errlistData = new ArrayList<String[]>();
			boolean errFlag = true;

			result = chkUser(fname, importDataSet, usr_mst_key, regMinDate);

			if (result.size() == 0) {
				errFlag = false;
			} else {
				if ("N".equals((String) result.get("permission_yn"))) {
					errFlag = false;
				} else if ("null".equals((String) result.get("permission_yn"))) {
					errFlag = false;
				}
			}
			dvr.setErrData(errlistData);
			dvr.setValid(errFlag);
		} catch (Exception e) {
			e.printStackTrace();

			importCode(importDataSet, fname, "COR001");
		}

		return dvr;
	}

	private Map<String, Object> chkUser(String fname, ImportDataSet importDataSet, String usrmstkey, String minDate) {
		Map<String, Object> result = new HashMap<String, Object>();

		CheckRegModifyAllowVO checkRegModifyAllowVO = new CheckRegModifyAllowVO();
		checkRegModifyAllowVO.setUsr_mst_key(usrmstkey);
		checkRegModifyAllowVO.setRegMinDate(minDate);
		String regMinDate = checkRegModifyAllowVO.getRegMinDate();

		Calendar c = Calendar.getInstance();
		String cur_year = String.valueOf(c.get(Calendar.YEAR));
		String cur_month = String.valueOf(c.get(Calendar.MONTH) + 1);

		String regMinYear = regMinDate.substring(0, 4);
		String regMinMonth = regMinDate.substring(4, 6);

		String regTermination = "N";
		String permission_yn = "N";

		// 현재시점이 4월 이후인 경우
		if (Integer.parseInt(cur_month) >= 4) {
			if (Integer.parseInt(regMinYear) < Integer.parseInt(cur_year)) {
				regTermination = "Y";
			} else {
				regTermination = "N";
			}
		}

		if (regTermination == "Y") {
			checkRegModifyAllowVO.setBase_year(regMinDate.substring(0, 4));
			try {
				permission_yn = uploadDAO.checkRegModifyAllow(checkRegModifyAllowVO);
			} catch (Exception e) {
				e.printStackTrace();
				importCode(importDataSet, fname, "COR001");
			}
			if (permission_yn == null || permission_yn.equals("N"))
				permission_yn = "N";
		} else {
			permission_yn = "Y";
		}
		result.put("permission_yn", permission_yn);
		result.put("reg_year", regMinYear);
		result.put("reg_month", regMinMonth);
		result.put("cur_year", cur_year);
		result.put("cur_month", ((Integer.parseInt(cur_month) < 10) ? "0" + cur_month : cur_month));
//		error = err;
		return result;
	}

	/* 수정허가여부(대행 계정) */
	private DataVerifyResult chkRegModifyAllow(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			Set<String> agencyUsrMstKey) {

		Map<String, Object> result = new HashMap<String, Object>();
		DataVerifyResult dvr = new DataVerifyResult();
		List<String[]> errlistData = new ArrayList<String[]>();

		/* Collections.sort(listDataParam, new DataComparatorForContractGubun()); */

		String regMinDate = getRecordMinDate(listDataParam, checkBit);

		String data[];
		String saveMstKeyList = "";
		String preMstKey = "";
		for (int i = 0; i < listDataParam.size(); i++) {
			data = listDataParam.get(i);
			for (int j = 0; j < data.length; j++) {
				if (j == 3) {
					if ("".equals(preMstKey)) {
						preMstKey = data[j];
						saveMstKeyList = data[j];
					} else {
						if (!data[j].equals(preMstKey)) {
							saveMstKeyList += ("," + data[j]);
							preMstKey = data[j];
						}
					}
				}
			}
		}
		try {
			result = server_jsonList(fname, importDataSet, saveMstKeyList, regMinDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
		dvr.setArrPermission_yn(result.get("permission_yn").toString().split(","));
		String arrMst_usr_key = "";
		boolean errorFlag = true;
		int failCnt = 0;

		for (int i = 0; i < result.get("permission_yn").toString().split(",").length; i++) {
			if ("N".equals(result.get("permission_yn").toString().split(",")[i])) {
				arrMst_usr_key += " " + saveMstKeyList.split(",")[i];
				errorFlag = false;
				//_failList[failCnt] = saveMstKeyList.split(",")[i];
				failCnt++;
				// permitUsrList[i] = saveMstKeyList.split(",")[i];
			}
		}
		
		dvr.setArrMst_usr_key(arrMst_usr_key); // agencyUsrMstKey[6668802621, 9058759962]
		dvr.setValid(errorFlag);
		dvr.setErrData(errlistData);

		return dvr;
	}

	private Map<String, Object> server_jsonList(String fname, ImportDataSet importDataSet, String saveMstKeyList, String regMinDate) {
		Map<String, Object> result = new HashMap<String, Object>();

		CheckRegModifyAllowVO checkRegModifyAllowVO = new CheckRegModifyAllowVO();

		Calendar c = Calendar.getInstance();
		String.valueOf(c.get(Calendar.MONTH) + 1);

		String regMinYear = regMinDate.substring(0, 4);
		regMinDate.substring(4, 6);

		String regTermination = "";
		String permission_yn = "";

		checkRegModifyAllowVO.setBase_year(regMinYear);

		String[] keyList = saveMstKeyList.split(",");

		// 신고 사업자 만큼 반복
		for (int i = 0; i < keyList.length; i++) {

			checkRegModifyAllowVO.setUsr_mst_key(keyList[i]);

			if (i == 0) {
				try {
					regTermination = uploadDAO.checkRegModifyAllow(checkRegModifyAllowVO);
					if (regTermination == null || regTermination.equals("N"))
						permission_yn = "N";
					else
						permission_yn = regTermination;
				} catch (Exception e) {
					e.printStackTrace();
					importCode(importDataSet, fname, "COR001");
				}
			} else {
				try {
					regTermination = uploadDAO.checkRegModifyAllow(checkRegModifyAllowVO);
					if (regTermination == null || regTermination.equals("N"))
						permission_yn += ",N";
					else
						permission_yn += "," + regTermination;
				} catch (Exception e) {
					e.printStackTrace();
					importCode(importDataSet, fname, "COR001");
				}
			}
		}

		result.put("permission_yn", permission_yn);
//		error = err;
		return result;
	}

	private String getRecordMinDate(List<String[]> listDataParam, String checkBit) {
		int yyyymm = 0;
		String[] data = null;

		// System.out.println("listDataParam.size() : " + listDataParam.size());
		for (int i = 0; i < listDataParam.size(); i++) {
			data = listDataParam.get(i);
			for (int j = 0; j < data.length; j++) {
				if (String.valueOf(checkBit.charAt(j)).equals("1")) {
					if (!data[j].equals("")) {

						if (yyyymm == 0)
							yyyymm = Integer.parseInt(data[j]);
						else if (yyyymm > Integer.parseInt(data[j]))
							yyyymm = Integer.parseInt(data[j]);
						// System.out.println("---- yyyymm : " + yyyymm );
					}
				}
			}
		}
		String regMinDate = Integer.toString(yyyymm);
		// System.out.println("==============> minDate : "+ regMinDate);
		return regMinDate;
	}
	
	private void setRegLimitDae(boolean flag) {
		isRegLimitDae = flag;
	}

	@Override
	public boolean checkRegLimitDae() {
		return isRegLimitDae;
	}

	/* 실적신고 제한 검증 */
	@Override
	public boolean checkRegLimit(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type, String importYear) {
		// type :15 -> 일반,택배 간소화, type :16 -> 대행
		error = null;
		String saveFileName = MassOrderVO.getSaveFileName();
		
		DataVerifyResult dvr = null;
		
		String arrMst_Usr_key = "";
		
		if (type < 16) {
			dvr = chkRegLimit(fname, importDataSet, listDataParam, checkBit, importYear);

			arrMst_Usr_key = dvr.getArrMst_usr_key();
		} else if (type == 16) {
			Set<String> agencyUsrMstKey = getAgencyMemberUsrMstKey(listDataParam);
			dvr = chkRegLimit(fname, importDataSet, listDataParam, checkBit, agencyUsrMstKey, importYear);
			arrMst_Usr_key = dvr.getArrMst_usr_key();
		}

		String errMessage01 = "※운송사업자(일반/용달/개별)가 아닌 경우 차량을 등록할 수 없습니다.";
		String errMessage02_1 = "※개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.("
				+ importYear + "년 기준, 직영 또는 지입차량 0대/ 장기용차 보유)";
		String errMessage02_2 = "※개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.("
				+ importYear + "년 기준, 직영 또는 지입차량 0대)";
		String errMessage03_1 = "※개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.("
				+ importYear + "년 기준, 직영 또는 지입차량 2대 이상/ 장기용차 보유)";
		String errMessage03_2 = "※개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.("
				+ importYear + "년 기준, 직영 또는 지입차량 2대 이상)";
		String errMessage04 = "※개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.(" + importYear
				+ "년 기준, 장기용차 보유)";
		String errMessage05 =
				"※" + importYear + "년 현재 직영 및 지입 차량 수가 운송사업자유형(일반/용달/개별) 보유 수보다 적습니다.";
		String errMessage06_1 = "※각 운송유형(일반/용달/개별) 당 직영 또는 지입차량을 최소 2대 이상 보유해야만 장기용차를 보유할 수 있습니다.";
		String errMessage06_2 = "※각 운송유형(일반/용달/개별) 당 직영 또는 지입차량을 최소 3대 이상 보유해야만 장기용차를 보유할 수 있습니다.";
		String errMessage06_3 = "※각 운송유형(일반/용달/개별) 당 직영 또는 지입차량을 최소 4대 이상 보유해야만 장기용차를 보유할 수 있습니다.";
		String errMessage06_4 = "※각 운송유형(일반/용달/개별) 당 직영 또는 지입차량을 최소 5대 이상 보유해야만 장기용차를 보유할 수 있습니다.";
		String errMessage07 = "※차량 필수 정보가 누락되었습니다.";
		String errMessage_Err = "※사업자유형 및 차량 보유수 검증 진행단계에 문제가 발생하였습니다. 콜센터(1899-2793)로 문의부탁드립니다.";

		if (!dvr.isValid()) {
			String _dateStr = DateUtil.getToDayTimeStrForErrorFileMake();
			String _filename = saveFileName+"_E_"+_dateStr;

			if (type < 16) {
				Map<String,Object> errMap = new HashMap<String,Object>();
				ExcelExportManager.setFlag_regLimit(false);
				String[] arrMstUsrkey = arrMst_Usr_key.split(",");
				String[] arrResult = dvr.getArrResult().split(",");
				String msg = "";
				for (int i = 0; i < arrMstUsrkey.length; i++) {
					if("02".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage01;
					}else if("03".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage02_2;
					}else if("04".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage03_2;
					}else if("05".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage04;
					}else if("06".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage05;
					}else if("08".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage06_1;
					}else if("07".equals(arrResult[i]) || "09".equals(arrResult[i]) ||
							"10".equals(arrResult[i]) || "11".equals(arrResult[i])||
							"17".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage07;
					}else if("12".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage02_1;
					}else if("13".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage03_1;
					}else if("14".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage06_2;
					}else if("15".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage06_3;
					}else if("16".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage06_4;
					}else if("ERR".equals(arrResult[i])){
						msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
						msg += " " +"제한 사유 : " + errMessage_Err;
					}
				}
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.LIM001.getCode());
				errMap.put("eMsg", ErrorType.LIM001.getMessage());
				errMap.put("eDetail", msg);
				setErrCodeList(errMap);
			} else if (type == 16) {
				String[] arrMstUsrkey = arrMst_Usr_key.split(",");
				String[] arrResult = dvr.getArrResult().split(",");
				String msg = "";
				Map<String,Object> errMap = new HashMap<String,Object>();
				for (int i = 0; i < arrMstUsrkey.length; i++) {
					if (usr_mst_key.equals(arrMstUsrkey[i])) {
						ExcelExportManager.setFlag_regLimit(false);
						errMap.put("fname", fname);
						errMap.put("eCode", ErrorType.LIM001.getCode());
						errMap.put("eMsg", ErrorType.LIM001.getMessage());
						errMap.put("eDetail", msg);
						setErrCodeList(errMap);
					} 
				}
				if (ExcelExportManager.getFlag_regLimit()) {
					ExcelExportManager.setFlag_regLimitDae(false);
					setRegLimitDae(false);
					for (int i = 0; i < arrMstUsrkey.length; i++) {
						Map<String,Object> errMap2 = new HashMap<String,Object>();
						if("02".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage01;
						}else if("03".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage02_2;
						}else if("04".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage03_2;
						}else if("05".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage04;
						}else if("06".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage05;
						}else if("08".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage06_1;
						}else if("07".equals(arrResult[i]) || "09".equals(arrResult[i]) ||
								"10".equals(arrResult[i]) || "11".equals(arrResult[i])||
								"17".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage07;
						}else if("12".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage02_1;
						}else if("13".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage03_1;
						}else if("14".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage06_2;
						}else if("15".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage06_3;
						}else if("16".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage06_4;
						}else if("ERR".equals(arrResult[i])){
							msg += " " +"신고가 제한된 사업자는 [" + arrMstUsrkey[i] + "] 입니다.";
							msg += " " +"제한 사유 : " + errMessage_Err;
						}
						for (int j = 0; j < listDataParam.size(); j++) {
							if (arrMstUsrkey[i].indexOf(listDataParam.get(j)[3]) != -1) {
								errMap2.put("fname", fname);
								errMap2.put("row", j+3);
								errMap2.put("eCode", ErrorType.LIM002.getCode());
								errMap2.put("eMsg", ErrorType.LIM002.getMessage());
								errMap2.put("eDetail", msg);
								for (int k =0; k < type; k++) {
									errMap2.put("data"+k, listDataParam.get(j)[k]);
								}
								errMap2.put("data3", listDataParam.get(j)[3]+"##FAIL##");
								setErrDataList(errMap2);
							}
						}
					}
				}
			}
		} else {
			ExcelExportManager.setFlag_regLimit(true);
			ExcelExportManager.setFlag_regLimitDae(true);
		}
		return dvr.isValid();
	}

	/* 실적신고 제한 미대행 */
	private DataVerifyResult chkRegLimit(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			String importYear) {

		DataVerifyResult dvr = new DataVerifyResult();

		try {
			Map<String, Object> result = new HashMap<String, Object>();
			List<String[]> errlistData = new ArrayList<String[]>();
			boolean errFlag = true;

			result = chkRegLimitList(fname, importDataSet, usr_mst_key, importYear);
			// System.out.println("---- result : " + result);
			// System.out.println("---- result size : " + result.size());
			// System.out.println("---- (String)result.get(status_result) : " +
			// (String)result.get("status_result"));

			if (result.size() == 0) {
				errFlag = false;
			} else {
				if (!"99".equals((String) result.get("status_result"))) {
					if (!"ONE".equals((String) result.get("status_result"))) {
						errFlag = false;
					}
				}
			}

			dvr.setArrResult((String) result.get("status_result"));
			dvr.setArrMst_usr_key(usr_mst_key);
			dvr.setErrData(errlistData);
			dvr.setValid(errFlag);
		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		return dvr;
	}

	private Map<String, Object> chkRegLimitList(String fname, ImportDataSet importDataSet, String mstkeyList, String importYear) {

		Map<String, Object> result = new HashMap<String, Object>();

		CheckRegModifyAllowVO checkRegModifyAllowVO = new CheckRegModifyAllowVO();
		checkRegModifyAllowVO.setReg_date(importYear);

		String[] keyList = mstkeyList.split(",");

		String regTermination = "";
		String statusList = "";

		// 신고 사업자 만큼 반복
		for (int i = 0; i < keyList.length; i++) {

			checkRegModifyAllowVO.setUsr_mst_key(keyList[i]);

			try {

				String comp_cls_detail = uploadDAO.getCompClsDetail(checkRegModifyAllowVO);

				if ("02-01".equals(comp_cls_detail) || "02-01,02-02".equals(comp_cls_detail)
						|| "02-01,04-01".equals(comp_cls_detail)
						|| "02-01,05".equals(comp_cls_detail) || "04-01".equals(comp_cls_detail)
						|| "02-01,04-01".equals(comp_cls_detail)) {
					regTermination = uploadDAO.checkRegLimit02(checkRegModifyAllowVO);
				} else if ("01-02".equals(comp_cls_detail)) {
					regTermination = uploadDAO.checkRegLimit03(checkRegModifyAllowVO);
				} else if ("05".equals(comp_cls_detail) || "06".equals(comp_cls_detail)
						|| "07".equals(comp_cls_detail)) {
					regTermination = "99";
				} else {
					regTermination = uploadDAO.checkRegLimit04(checkRegModifyAllowVO);
				}

				if (i == 0) {
					statusList = regTermination;
				} else {
					statusList += "," + regTermination;
				}
				regTermination = "";
			} catch (Exception e) {
				e.printStackTrace();
				importCode(importDataSet, fname, "COR001");
			}
		}

		result.put("status_result", statusList);
		return result;
	}

	/* 실적신고 제한 대행 계정 */
	private DataVerifyResult chkRegLimit(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit,
			Set<String> agencyUsrMstKey, String importYear) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		Map<Integer, Character> regLimitDaeLineMap = new HashMap<Integer, Character>();
		DataVerifyResult dvr = new DataVerifyResult();
		List<String[]> errlistData = new ArrayList<String[]>();
		List<String[]> suclistData = new ArrayList<String[]>();

		/* Collections.sort(listDataParam, new DataComparatorForContractGubun()); */

		String data[];
		String saveMstKeyList = "";
		String preMstKey = "";
		for (int i = 0; i < listDataParam.size(); i++) {
			data = listDataParam.get(i);
			for (int j = 0; j < data.length; j++) {
				if (j == 3) {
					if ("".equals(preMstKey)) {
						preMstKey = data[j];
						saveMstKeyList = data[j];
					} else {
						if (!data[j].equals(preMstKey)) {
							saveMstKeyList += ("," + data[j]);
							preMstKey = data[j];
						}
					}
				}
			}
		}
		// System.out.println("대행 신고자 사업자번호 : " + saveMstKeyList);

		try {
			result = chkRegLimitList(fname, importDataSet, saveMstKeyList, importYear);
		} catch (Exception e) {
			e.printStackTrace();
			importCode(importDataSet, fname, "COR001");
		}
		// System.out.println("--- result : " +saveMstKeyList);
		// System.out.println("--- result : " +result.get("status_result").toString());

		boolean errorFlag = true;
		String arrMst_usr_key = "";
		String arrResult = "";
		int failCnt = 0;

		for (int i = 0; i < saveMstKeyList.split(",").length; i++) {
			if (result.get("status_result") == null) {
				//미가입자일 경우 신고제한 대상자에 속하지 않음
				errorFlag = true;
			} else if (!"99".equals(result.get("status_result").toString().split(",")[i])) {
				if (!"ONE".equals((String) result.get("status_result").toString().split(",")[i])) {
					errorFlag = false;

					if ("".equals(arrMst_usr_key)) {
						arrMst_usr_key += saveMstKeyList.split(",")[i];
						arrResult += result.get("status_result").toString().split(",")[i];
					} else {
						arrMst_usr_key += "," + saveMstKeyList.split(",")[i];
						arrResult += "," + result.get("status_result").toString().split(",")[i];
					}
				}
			}
		}

//		String[] failUsrList = new String[failCnt];
//		String[] failResultList = new String[failCnt];
		for (int j = 0; j < listDataParam.size(); j++) {
			if (arrMst_usr_key.indexOf(listDataParam.get(j)[3]) != -1) {
				regLimitDaeLineMap.put(j, '0');
			} else {
				suclistData.add(listDataParam.get(j));
			}
		}
		
		ExcelExportManager.setRegLimitDae(regLimitDaeLineMap);
		dvr.setArrResult(arrResult);
		dvr.setArrMst_usr_key(arrMst_usr_key);


		dvr.setValid(errorFlag);

		dvr.setErrData(errlistData);
		dvr.setSucData(suclistData);

		return dvr;
	}

	@Override
	public ImportDataSet transport(ImportDataSet importDataSet, List<String[]> listDataParam,
			int type) {
		List<String[]> listDataFinal = new ArrayList<String[]>();
		Map<String, ConvertVO> convertMap = new HashMap<String, ConvertVO>();
		Map<String, ContGroupVO> originalContGroupSet = new HashMap<String, ContGroupVO>();

		ConvertVO convertVO = null;
		boolean isOperExist = false;
		boolean isTruExist = false;

		List<String[]> temp = new ArrayList<String[]>();
		int contGroupIndex = 0;
		int contChargeIndex = 0;

		for (int i = 0; i < listDataParam.size(); i++) {
			String[] data = listDataParam.get(i);
			if (type == RESOURCE_VAR.TYPE_SMALLING) {
				contGroupIndex = 2;
				contChargeIndex = 4;
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
				// 순수주선 양식 추가
				contGroupIndex = 2;
				contChargeIndex = 4;
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				contGroupIndex = 2;
				contChargeIndex = 5;
			}

			if (!data[contGroupIndex].equals("")) {
				if (originalContGroupSet.containsKey(data[contGroupIndex])) {
					originalContGroupSet.get(data[contGroupIndex]).upCount();
				} else {
					originalContGroupSet.put(data[contGroupIndex], new ContGroupVO(
							data[contGroupIndex], Long.parseLong(data[contChargeIndex])));
				}
			}
		}

		for (int i = 0; i < listDataParam.size(); i++) {
			String[] data = listDataParam.get(i);
			if (type == RESOURCE_VAR.TYPE_SMALLING) {
				contGroupIndex = 2;
				contChargeIndex = 4;
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
				contGroupIndex = 2;
				contChargeIndex = 4;
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				contGroupIndex = 2;
				contChargeIndex = 5;
			}

			if (!data[contGroupIndex].equals("")) {
				data[contChargeIndex] =
						originalContGroupSet.get(data[contGroupIndex]).getDivideCharge();
			}
			temp.add(data);
		}

		for (int i = 0; i < temp.size(); i++) {
			String[] data = temp.get(i);
			if (type == RESOURCE_VAR.TYPE_SMALLING) {
				if (data[RESOURCE_VAR.OPER_P1].equals("") && data[RESOURCE_VAR.OPER_P2].equals("")
						&& data[RESOURCE_VAR.OPER_P3].equals("")
						&& data[RESOURCE_VAR.OPER_P4].equals("")) {
					isOperExist = false;
				} else {
					isOperExist = true;
				}

				if (data[RESOURCE_VAR.TRUST_P1].equals("") && data[RESOURCE_VAR.TRUST_P2].equals("")
						&& data[RESOURCE_VAR.TRUST_P3].equals("")
						&& data[RESOURCE_VAR.TRUST_P4].equals("")) {
					isTruExist = false;
				} else {
					isTruExist = true;
				}
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {

				isOperExist = false; // 2017.09 20 written by dyahn 순수주선은 배차를 할수 없기 때문에 false로 고정

				if (data[RESOURCE_VAR.TRUST_P1_FORWARDONLY].equals("")
						&& data[RESOURCE_VAR.TRUST_P2_FORWARDONLY].equals("")
						&& data[RESOURCE_VAR.TRUST_P3_FORWARDONLY].equals("")
						&& data[RESOURCE_VAR.TRUST_P4_FORWARDONLY].equals("")) {
					isTruExist = false;
				} else {
					isTruExist = true;
				}
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				if (data[RESOURCE_VAR.OPER_D_P1].equals("")
						&& data[RESOURCE_VAR.OPER_D_P2].equals("")
						&& data[RESOURCE_VAR.OPER_D_P3].equals("")
						&& data[RESOURCE_VAR.OPER_D_P4].equals("")) {
					isOperExist = false;
				} else {
					isOperExist = true;
				}

				if (data[RESOURCE_VAR.TRUST_D_P1].equals("")
						&& data[RESOURCE_VAR.TRUST_D_P2].equals("")
						&& data[RESOURCE_VAR.TRUST_D_P3].equals("")
						&& data[RESOURCE_VAR.TRUST_D_P4].equals("")) {
					isTruExist = false;
				} else {
					isTruExist = true;
				}
			}

			System.out.println(String.format("배차 : %s, 위탁 : %s", isOperExist, isTruExist));

			if (isOperExist) {
				// 둘다 존재할 경우 배차부터 처리
				if (type == RESOURCE_VAR.TYPE_SMALLING) {
					convertVO = new ConvertVO(1, data, this);
					String operKey = getKeyByOper(data, RESOURCE_VAR.TYPE_SMALLING_KEY_COLUMNS);

					if (convertMap.containsKey(operKey)) {
						convertMap.put(operKey, convertMap.get(operKey).mergeObject(convertVO));
					} else {
						convertMap.put(operKey, convertVO);
					}
				} else {
					convertVO = new ConvertVO(1, data, this);
					String operKey =
							getKeyByOper(data, RESOURCE_VAR.TYPE_SMALLING_DAEHANG_KEY_COLUMNS);
					if (convertMap.containsKey(operKey)) {
						convertMap.put(operKey, convertMap.get(operKey).mergeObject(convertVO));
					} else {
						convertMap.put(operKey, convertVO);
					}
				}
			}

			if (isTruExist) {
				if (type == RESOURCE_VAR.TYPE_SMALLING) {
					convertVO = new ConvertVO(2, data, this);
					String trustKey = getKeyByTrust(data, RESOURCE_VAR.TYPE_SMALLING_KEY_COLUMNS);

					if (convertMap.containsKey(trustKey)) {
						convertMap.put(trustKey, convertMap.get(trustKey).mergeObject(convertVO));
					} else {
						convertMap.put(trustKey, convertVO);
					}
				} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
					// 2017. 09 20 written by dyahn 순수주선 병합

					convertVO = new ConvertVO(2, data, this);
					String trustKey =
							getKeyByTrust(data, RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_KEY_COLUMNS);

					if (convertMap.containsKey(trustKey)) {
						convertMap.put(trustKey, convertMap.get(trustKey).mergeObject(convertVO));
					} else {
						convertMap.put(trustKey, convertVO);
					}

				} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
					convertVO = new ConvertVO(2, data, this);
					String trustKey =
							getKeyByTrust(data, RESOURCE_VAR.TYPE_SMALLING_DAEHANG_KEY_COLUMNS);

					if (convertMap.containsKey(trustKey)) {
						convertMap.put(trustKey, convertMap.get(trustKey).mergeObject(convertVO));
					} else {
						convertMap.put(trustKey, convertVO);
					}
				}
			}

		}

		// 1) KEY 데이터를 생성하여 Map에 추가
		Set<String> keys = convertMap.keySet();
		Iterator<String> i = keys.iterator();

		while (i.hasNext()) {
			if (type == RESOURCE_VAR.TYPE_SMALLING) {
				listDataFinal
						.add(convertMap.get(i.next()).getStringData(RESOURCE_VAR.TYPE_SMALLING));
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
				listDataFinal.add(convertMap.get(i.next())
						.getStringData(RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY));
			} else {
				listDataFinal.add(
						convertMap.get(i.next()).getStringData(RESOURCE_VAR.TYPE_SMALLING_DAEHANG));
			}
		}

		for (int j = 0; j < listDataFinal.size(); j++) {
			Utils.printArray(listDataFinal.get(j));
		}
		importDataSet.setImportData(listDataFinal);
		return importDataSet;
	}

	private String getKeyByOper(String[] data, String typeSmallingConvertKeyColumns) {
		String key = "";
		for (int i = 0; i < data.length; i++) {
			if (String.valueOf(typeSmallingConvertKeyColumns.charAt(i)).equals("1")
					|| String.valueOf(typeSmallingConvertKeyColumns.charAt(i)).equals("2")) {
				key += data[i];
			}
		}
		return key;
	}

	private String getKeyByTrust(String[] data, String typeSmallingConvertKeyColumns) {
		String key = "";
		for (int i = 0; i < data.length; i++) {
			if (String.valueOf(typeSmallingConvertKeyColumns.charAt(i)).equals("1")
					|| String.valueOf(typeSmallingConvertKeyColumns.charAt(i)).equals("3")) {
				key += data[i];
			}
		}
		return key;
	}

	@Override
	public ImportDataSet contGroupGenerate(ImportDataSet importDataSet,
			List<String[]> listDataParam, int type) {
		List<String[]> tempData = new ArrayList<String[]>();
		List<String[]> resultData = new ArrayList<String[]>();
		Map<String, Long> saveContGroupMap = new HashMap<String, Long>();
		Map<String, String> keyMap = new HashMap<String, String>();
		for (String[] data : listDataParam) {


			if (type == RESOURCE_VAR.TYPE_SMALLING) {
				String contGroup = getContGroupKey(data,
						RESOURCE_VAR.TYPE_SMALLING_CONVERT_CONT_GROUP_COLUMNS);
				String uuid = UUID.randomUUID().toString();
				if (keyMap.containsKey(contGroup)) {
					data[2] = keyMap.get(contGroup);
				} else {
					keyMap.put(contGroup, uuid);
					data[2] = uuid;
				}

				if (saveContGroupMap.containsKey(contGroup)) {
					saveContGroupMap.put(contGroup,
							saveContGroupMap.get(contGroup) + Long.parseLong(data[4]));
				} else {
					saveContGroupMap.put(contGroup, Long.parseLong(data[4]));
				}


			} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
				// 순수주선 자동 계약고유번호 그룹화
				String contGroup = getContGroupKey(data,
						RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONVERT_CONT_GROUP_COLUMNS);
				String uuid = UUID.randomUUID().toString();
				if (keyMap.containsKey(contGroup)) {
					data[2] = keyMap.get(contGroup);
				} else {
					keyMap.put(contGroup, uuid);
					data[2] = uuid;
				}


				if (saveContGroupMap.containsKey(contGroup)) {
					saveContGroupMap.put(contGroup,
							saveContGroupMap.get(contGroup) + Long.parseLong(data[4]));
				} else {
					saveContGroupMap.put(contGroup, Long.parseLong(data[4]));
				}
			} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				String contGroup = getContGroupKey(data,
						RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_CONT_GROUP_COLUMNS);
				String uuid = UUID.randomUUID().toString();
				if (keyMap.containsKey(contGroup)) {
					data[2] = keyMap.get(contGroup);
				} else {
					keyMap.put(contGroup, uuid);
					data[2] = uuid;
				}
				if (saveContGroupMap.containsKey(contGroup)) {
					saveContGroupMap.put(contGroup,
							saveContGroupMap.get(contGroup) + Long.parseLong(data[5]));
				} else {
					saveContGroupMap.put(contGroup, Long.parseLong(data[5]));
				}
			}
			tempData.add(data);
		}

		for (String[] data : tempData) {
			if (type == RESOURCE_VAR.TYPE_SMALLING) {
				data[4] = String.valueOf(saveContGroupMap.get(getContGroupKey(data,
						RESOURCE_VAR.TYPE_SMALLING_CONVERT_CONT_GROUP_COLUMNS)));

			} else if (type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY) {
				data[4] = String.valueOf(saveContGroupMap.get(getContGroupKey(data,
						RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONVERT_CONT_GROUP_COLUMNS)));

			} else if (type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG) {
				data[5] = String.valueOf(saveContGroupMap.get(getContGroupKey(data,
						RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONVERT_CONT_GROUP_COLUMNS)));
			}
			resultData.add(data);
		}

		importDataSet.setImportData(resultData);
		
		return importDataSet;
	}
	
	@Override
	public ImportDataSet getDataForSpd(MultipartFile file) {
		ImportDataSet importDataSet = new ImportDataSet();
		
		List<String[]> listData = new ArrayList<String[]>();
		Workbook wb = null;
		
		String fname = file.getOriginalFilename();
		
		try {
				
			wb = WorkbookFactory.create(file.getInputStream());
	
			Sheet sheet = wb.getSheetAt(0);
			// 양식 헤더
			Row row = sheet.getRow(0);
			Cell cell = row.getCell(0);
			int columnCnt = row.getPhysicalNumberOfCells();
	
			int rowCnt = sheet.getLastRowNum();
			if ((rowCnt > 0) || (sheet.getPhysicalNumberOfRows() > 0)) {
				rowCnt++;
			}
			
			for (int i = 0; i < rowCnt; i++) {
				
				Row cells = sheet.getRow(i);
				
				if(i == 0){
					if(!cells.getCell(0).getStringCellValue().startsWith("계약정보")){
						importCode(importDataSet, fname, "UPL006");

					}
				}else if(i == 1){
					if(!cells.getCell(0).getStringCellValue().startsWith("계약고유번호")){
						importCode(importDataSet, fname, "UPL006");

					}
				} else {
					if (columnCnt == RESOURCE_VAR.TYPE_SPD) {
						String tmp[] = new String[columnCnt];
						String tmp2 = "";
						for (int j = 0; j < tmp.length; j++) {
							tmp[j] = "";
						}
						for (int j = 0; j < tmp.length; j++) {
							try {
								if (cells.getCell(j).getCellType() == Cell.CELL_TYPE_NUMERIC) {
									tmp2 = NumberToTextConverter
											.toText(cells.getCell(j).getNumericCellValue());
								} else if (cells.getCell(j).getCellType() == Cell.CELL_TYPE_FORMULA) { //2023.02.13 jwchoi 수식값일 때 분기처리 추가
									FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
									DataFormatter dataFormatter = new DataFormatter();
									tmp2 = dataFormatter.formatCellValue(evaluator.evaluateInCell(cells.getCell(j)));
								} else {
									tmp2 = cells.getCell(j).getStringCellValue();
								}							
							} catch (IllegalStateException e) {
								importCode(importDataSet, fname, "UPL014");
								return importDataSet;
							} catch (NullPointerException e) {
								tmp2 = "";
							}
							
							if(tmp2.indexOf(",") != -1){
								tmp2 = tmp2.replaceAll(",","").trim();
							}
							if(tmp2.indexOf("-") != -1){
								tmp2 = tmp2.replaceAll("-","").trim();
							}
							if(tmp.length-1 >= j)
								tmp[j] = tmp2;
						}
						listData.add(tmp);
					} else {
						importCode(importDataSet, fname, "UPL009");
						ExcelExportManager.setFlag_yang(false);
						return importDataSet;
					}
				}
			}
			if (listData.size() == 0) {
				importCode(importDataSet, fname, "UPL008");
				ExcelExportManager.setFlag_yang(false);
				return importDataSet;

			} else {
				ExcelExportManager.setFlag_yang(true);
				importDataSet.setImportData(listData);
				importDataSet.setSuccess(true);
				importDataSet.setImportStatus(ImportStatus.SUCCESS);	
				return importDataSet;
				
			}
		} catch (Exception e) {
				e.printStackTrace();
				importCode(importDataSet, fname, "UPL014");
				return importDataSet;
		}
	}
	
	private String getContGroupKey(String [] data, String typeSmallingConvertContGroupColumns){
		String key = "";
		for (int i = 0; i < data.length; i++) {
			if(String.valueOf(typeSmallingConvertContGroupColumns.charAt(i)).equals("1")){
				key += data[i];
			}
		}
		return key;
	}

	public String makeErrorDirectory(String saveFileName, String errorFilePath) {
		int endCount = 0;
		String target;
		File f = new File(errorFilePath); //System.getProperty ("user.home")+File.seRparator+LOCAL_PATH_CONST.GROUP_PATH;
		target = errorFilePath+saveFileName.replaceAll(" ", "");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		return f.getAbsolutePath()+File.separator;
	}
	
	public ImportDataSet importCode(ImportDataSet importDataSet, String fname, String code) {
		Map<String,Object> errMap = new HashMap<String,Object>();
		
		if (code.contains("UPL") || code.contains("COR")) {
			//importDataSet.setImportData(null);
			importDataSet.setSuccess(false);
			
			if ("UPL001".equals(code)) {
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL001.getCode());
				errMap.put("eMsg", ErrorType.UPL001.getMessage());
				errMap.put("eDetail", ErrorType.UPL001.getDetailMessage());
				setErrCodeList(errMap);
			} else if ("UPL003".equals(code)) {
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL003.getCode());
				errMap.put("eMsg", ErrorType.UPL003.getMessage());
				errMap.put("eDetail", ErrorType.UPL003.getDetailMessage());
				setErrCodeList(errMap);
			} else if ("UPL006".equals(code)) {
				importDataSet.setImportStatus(ImportStatus.HEADER_MIS_MATCH);
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL006.getCode());
				errMap.put("eMsg", ErrorType.UPL006.getMessage());
				errMap.put("eDetail", ErrorType.UPL006.getDetailMessage());
				setErrCodeList(errMap);
			} else if ("UPL008".equals(code)) {
				importDataSet.setImportStatus(ImportStatus.EMPTY_DATA);
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL008.getCode());
				errMap.put("eMsg", ErrorType.UPL008.getMessage());
				errMap.put("eDetail", ErrorType.UPL008.getDetailMessage());
				setErrCodeList(errMap);
			} else if ("UPL009".equals(code)) {
				importDataSet.setImportStatus(ImportStatus.COL_COUNT_MIS_MATCH);
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL009.getCode());
				errMap.put("eMsg", ErrorType.UPL009.getMessage());
				errMap.put("eDetail", ErrorType.UPL009.getDetailMessage());
				setErrCodeList(errMap);
			} else if ("UPL014".equals(code)) {
				importDataSet.setImportStatus(ImportStatus.ILLEGAL_ERROR);
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.UPL014.getCode());
				errMap.put("eMsg", ErrorType.UPL014.getMessage());
				errMap.put("eDetail", ErrorType.UPL014.getDetailMessage());
				setErrCodeList(errMap);
			} else if ("COR001".equals(code)) {
				errMap.put("fname", fname);
				errMap.put("eCode", ErrorType.COR001.getCode());
				errMap.put("eMsg", ErrorType.COR001.getMessage());
				errMap.put("eDetail", ErrorType.COR001.getDetailMessage());
				setErrCodeList(errMap);
			}

		} else if ("success".equals(code)) {
			importDataSet.setSuccess(true);
			importDataSet.setImportStatus(ImportStatus.SUCCESS);
			return importDataSet;
		}
		return importDataSet;
	}

	@Override
	public List<Map<String, Object>> makeFinalList(String oriFileName,
			List<Map<String, Object>> finalDataList, int type) {
		List<Map<String,Object>> finalCodeList = new ArrayList<Map<String,Object>>();
		if (finalDataList.size() >= 1) {
			for (int j=0; j<finalDataList.size(); j++) {
				Map<String,Object> codeMap = new HashMap<String,Object>();
				codeMap.put("eCode", finalDataList.get(j).get("eCode").toString());
				codeMap.put("eMsg", finalDataList.get(j).get("eMsg").toString());
				codeMap.put("eDetail", finalDataList.get(j).get("eDetail").toString());
				codeMap.put("fname_c", finalDataList.get(j).get("fname").toString());
				finalCodeList.add(codeMap);
			}
			
			for (int j=0; j<finalCodeList.size(); j++) {
				String ctmp = finalCodeList.get(j).get("eCode").toString();
				String ftmp = finalCodeList.get(j).get("fname_c").toString();

				for (int k=0; k<finalCodeList.size(); k++) {
					if (k != j) {
						if (ctmp == finalCodeList.get(k).get("eCode").toString() && ftmp == finalCodeList.get(k).get("fname_c").toString()) {
							finalCodeList.remove(k);
						}	
					}
				}
			}
			
		}
		return finalCodeList;
	}

	@Override
	public Map<String, Object> fpisFileUploadForSmalling(File[] fpisFiles, String type) {
		
		Map<String, Object> fpisUploadResultList = null;
		JSONManager jsonManager = new JSONManager();
		
		//HttpPost post = new HttpPost("http://115.68.163.11:3793/fpiseai/mod/FpisModUploadForSmalling.do"); //개발용
		HttpPost post = new HttpPost("http://10.176.34.118:64000/fpiseai/mod/FpisModUploadForSmalling.do"); //운영용
		HttpClient httpclient = HttpClientBuilder.create().build();
		
		fpisUploadRunning = true;
		
		try {
			post.setHeader("Connection", "Keep-Alive");
			post.setHeader("Accept-Charset", "UTF-8");
			post.setHeader("ENCTYPE", "multipart/form-data");
			
			MultipartEntity mEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, Charset.forName("UTF-8"));
			for (int i = 0; i < fpisFiles.length; i++) {
				//System.out.println(fpisFiles[i].getName());
				ContentBody cbFile = new FileBody(fpisFiles[i]);
				mEntity.addPart("userfile"+i, cbFile);
			}
			
			mEntity.addPart("type", new StringBody(type));
			post.setEntity(mEntity);
			
			HttpResponse res = httpclient.execute(post);
			HttpEntity resEntity = res.getEntity();

			if(resEntity != null) {

			}else {
				//System.out.println("response is error : " + res.getStatusLine().getStatusCode());
			}
			
			fpisUploadResultList = jsonManager.convertJsonToHashMapInList(convertStreamToString(resEntity.getContent()));

		}catch(Exception e){
			e.printStackTrace();
		}
		fpisUploadRunning = false;
		
		while (fpisUploadRunning) {
		}
		
		return fpisUploadResultList;
	}
	
	@Override
	public Map<String, Object> fpisFileUploadForSmallingTB(File[] fpisFiles, String type) {
		Map<String, Object> fpisUploadResultList = null;
		JSONManager jsonManager = new JSONManager();
		
		//HttpPost post = new HttpPost("http://115.68.163.11:3793/fpiseai/mod/FpisModUploadForTb.do"); //개발용
		HttpPost post = new HttpPost("http://10.176.34.118:64000/fpiseai/mod/FpisModUploadForTb.do"); //운영용
		HttpClient httpclient = HttpClientBuilder.create().build();
		
		fpisUploadRunning = true;
		
		try {
			post.setHeader("Connection", "Keep-Alive");
			post.setHeader("Accept-Charset", "UTF-8");
			post.setHeader("ENCTYPE", "multipart/form-data");
			
			MultipartEntity mEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, Charset.forName("UTF-8"));
			for (int i = 0; i < fpisFiles.length; i++) {
				System.out.println(fpisFiles[i].getName());
				ContentBody cbFile = new FileBody(fpisFiles[i]);
				mEntity.addPart("userfile"+i, cbFile);
			}
			
			mEntity.addPart("type", new StringBody(type));
			post.setEntity(mEntity);
			
			HttpResponse res = httpclient.execute(post);
			HttpEntity resEntity = res.getEntity();

			if(resEntity != null) {

			}else {
				System.out.println("response is error : " + res.getStatusLine().getStatusCode());
			}
			
			fpisUploadResultList = jsonManager.convertJsonToHashMapInList(convertStreamToString(resEntity.getContent()));

		}catch(Exception e){
			e.printStackTrace();
		}
		fpisUploadRunning = false;
		
		while (fpisUploadRunning) {

		}
		return fpisUploadResultList;
	}
	
	public static String convertStreamToString(InputStream is) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
			StringBuilder sb = new StringBuilder();

			String line = null;

			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}

			return sb.toString();
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				is.close();
			}catch(Exception ex) {
			}
		}

		return null;
	}

	@Override
	/*2022.11.09 jwchoi 웹연계 대량실적 업로드 upload_flag [확인]여부 */
	public boolean getUploadFlag(String usr_mst_key) {
		int cnt = massorderDAO.getUploadFlag(usr_mst_key); //upload_flag = 'X'
		boolean upload_flag;
		if (cnt > 0) {
			upload_flag = false;
		} else {
			upload_flag = true;
		}
		return upload_flag;
	}


	@Override
	public int getUploadFileCnt(String usr_mst_key) {
		return massorderDAO.getUploadFileCnt(usr_mst_key);
	}

	@Override
	public void insertRegUploadResult(List<FpisResultCommon> fpisResultCommonList, String usr_mst_key) {
		int cnt = fpisResultCommonList.size();
		
		massorderDAO.insertRegUploadResult(fpisResultCommonList, usr_mst_key, cnt);
		
	}

	@Override
	public List<Map<String, Object>> fpisFileUploadList(int fileCnt, int uploadCnt, List<FpisResultCommon> fileList, String usr_mst_key) {
		List<Map<String, Object>> uploadFileList = new ArrayList<Map<String,Object>>();

		if (uploadCnt < 1) {

			for (int i = 0; i < fileCnt; i++) {
				Map<String,Object> tmpmap = new HashMap<String,Object>();
				Map<String,Object> map = new HashMap<String,Object>();
				tmpmap.put("FILENAME", fileList.get(i).getCsvFileName());
				tmpmap.put("UPLOAD_CODE", "0");
				map.putAll(tmpmap);
				
				uploadFileList.add(map);
			}
			return uploadFileList;
		} else {
			uploadFileList =  massorderDAO.getUploadFileList(uploadFileList, usr_mst_key);
			return uploadFileList;
		}
		
	}

	@Override
	public void updateUploadResultY(String usr_mst_key) {
		massorderDAO.updateUploadResultY(usr_mst_key);
	}

	@Override
	public String getBCODE(String chkCond) {
		String tmp = "";
		if ("02".equals(chkCond) || "03".equals(chkCond)) {
			tmp = "R1-08-02";
		} else if ("01".equals(chkCond)) {
			tmp = "R1-11-02";
		} else if ("04".equals(chkCond)) {
			tmp = "R1-14-02";
		} else if ("05".equals(chkCond) || "07".equals(chkCond))
		{
			tmp = "R1-17-02";
		}
		return tmp;
	}
}
