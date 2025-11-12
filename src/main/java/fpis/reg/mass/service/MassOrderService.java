package fpis.reg.mass.service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
import fpis.common.vo.mod.ErrorVO;
import fpis.reg.mass.set.ImportDataSet;

/**
 * @method_desc	대량실적신고 검증
 * @returns RegVO
 *
 * @HISTORY
 * DATE			 	AUTHOR			NOTE
 * -------------	---------		------------------------
 * 2022. 08. 18.	최정원			최초생성
 *
 */
public interface MassOrderService {
	
	public void saveUsrInfo(String usrmstkey);
	
	public void saveUsrCond(String usrCond, boolean isDeahangCheck, boolean isOnly_0201);
	
	public ImportDataSet chkSheetCnt(MultipartFile file);
	
	public ImportDataSet chkPreData(MultipartFile file);
	
	public ImportDataSet getDataForSmallingBefore (MultipartFile excelFile, ImportDataSet importDataSet, List<String[]> listDataParam);
	
	public ImportDataSet getDataForSmallingAfter (String fname, ImportDataSet importDataSet, List<String[]> listDataParam);

	public ImportDataSet chkRowLimit(String fname, ImportDataSet importDataSet, List<String[]> listDataParam);

//	public ImportDataSet getRowCnt(ImportDataSet importDataSet);
	
	public ImportDataSet importCode(ImportDataSet importDataSet, String fname, String code);

	public List<String[]> dataEmptyArrayRemove(String fname, ImportDataSet importDataSet, List<String[]> listDataParam);
	
	public void chkNomerge(String fname, ImportDataSet importDataSet, List<String[]> listDataParam);

	public ImportDataSet makeOrderCnt(ImportDataSet importDataSet, final int type);

	public List<String[]> dataSupplement(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String typeSmallingDaehangDataSupplement);

	public Set<String> checkAgencyUsrMstKey(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, int type);

	public List<String[]> transDateString(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String supplement);
	
	public boolean checkAgencyCnt(String oriFileName, ImportDataSet importDataSet, List<String[]> listDataParam, int type);
	
	public boolean checkEmptyData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type);

	public ErrorVO checkErrCode();

	public boolean checkBasicFormData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type, boolean isOnly_0201);

	public boolean checkIdentifyData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type);

	public boolean checkContfromRangeData(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type);

	public boolean checkNoRecordInfo(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type);

	public boolean checkUsrGov(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type);

	public String getImportDate(List<String[]> listDataParam, String checkBit, int type);

	public boolean checkPreReg(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type, String importYear);

	public boolean checkRegModifyAllow(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type);

	public boolean checkRegLimit(String fname, ImportDataSet importDataSet, List<String[]> listDataParam, String checkBit, int type, String importYear);

	public ImportDataSet transport(ImportDataSet importDataSet, List<String[]> listDataParam, int type);

	public ImportDataSet contGroupGenerate(ImportDataSet importDataSet, List<String[]> listDataParam, int type);

	public ImportDataSet getDataForSpd(MultipartFile file);

	public List<Map<String, Object>> reSetErrCodeList();
	
	public List<Map<String, Object>> reSetErrDataList();
	
	public List<Map<String, Object>> getErrCodeList();
	
	public List<Map<String, Object>> removeDupliDataList(String oriFileName);

	public String makeErrorDirectory(String fname, String errorFilePath);

	public List<Map<String, Object>> makeFinalList(String oriFileName, List<Map<String, Object>> finalDataList, int type);

	public Map<String, Object> fpisFileUploadForSmalling(File[] fpisFiles, String type);

	public List<Map<String, Object>> removeNull(int cnt);

	public Map<String, Object> fpisFileUploadForSmallingTB(File[] fpisFiles, String string);

	public boolean getUploadFlag(String usr_mst_key);

	public int getUploadFileCnt(String usr_mst_key);
	
	public  List<Map<String, Object>> fpisFileUploadList(int fileCnt, int uploadCnt, List<FpisResultCommon> fpisResultCommonList, String usr_mst_key);

	public void insertRegUploadResult(List<FpisResultCommon> fpisResultCommonList,
			String usr_mst_key);

	public void updateUploadResultY(String usr_mst_key);

	public String getBCODE(String chkCond);

	public boolean checkNoRecordInfoDae();

	public boolean checkUsrGovDae();
	
	public boolean checkRegLimitDae();

	public boolean checkRegModifyAllowDae();
	
}
