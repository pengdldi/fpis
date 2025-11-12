package fpis.reg.mass.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import egovframework.com.cmm.service.FileVO;
import egovframework.com.cmm.service.impl.EgovComAbstractDAO;
import egovframework.com.uss.umt.service.MberManageVO;
import fpis.reg.mass.service.ContractInfoForSmalling;
import fpis.reg.mass.service.FpisResultCommon;
import fpis.reg.mass.service.MassOrderUploadVO;

/**
 * @class_desc	
 * fpis.reg.mass.service.impl
 * MassOrderDAO.java
 *
 * @DATE	2022. 09. 05.
 * @AUTHOR	GnT 최정원
 * @HISTORY
 * DATE			 	AUTHOR			NOTE
 * -------------	--------		--------------------
 * 2022. 09. 05.	최정원			최초생성
 */
@SuppressWarnings({"deprecation", "unchecked"})
@Repository("MassOrderDAO")    
public class MassOrderDAO extends EgovComAbstractDAO {

	private static final Logger logger = Logger.getLogger(MassOrderDAO.class);
	
	public int checkAgencyUsrMstKey(String aumk) throws Exception {
		MberManageVO vo = new MberManageVO();
		vo.setUsr_mst_key(aumk);
		int cnt = 0;
		String usr_mst_key = vo.getUsr_mst_key();
		try {
			cnt = (Integer) getSqlMapClient().queryForObject("MassOrderDAO.checkAgencyUsrMstKey", usr_mst_key);
		} catch (SQLException e) {
            logger.error("[ERROR] - SQLException : ", e);
        }
		return cnt;
		
	}

	public int chkNoRecord(String usrMstKey, String recordYear, String quarter) {
		Map<String, String> param = new HashMap<String, String>();
		param.put("usrMstKey", usrMstKey);
		param.put("recordYear", recordYear);
		param.put("quarter", quarter);
		int cnt = 0;
		//queryForObject("MassOrderDAO.chkNoRecord",param)
		try {
			cnt = (Integer) getSqlMapClient().queryForObject("MassOrderDAO.chkNoRecord",param);
		} catch (SQLException e) {
			logger.error("[ERROR] - SQLException : ", e);
		}
		return cnt;
	}

	/*2022.11.09 jwchoi 웹연계 대량실적 업로드 upload_flag [확인]여부 */
	public int getUploadFlag(String usrmstkey) {
		int cnt = 0;
		try {
			cnt = (Integer) getSqlMapClient().queryForObject("MassOrderDAO.getUploadFlag",usrmstkey);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cnt;
	}

	public int getUploadFileCnt(String usrmstkey) {
		int cnt = 0;
		try {
			cnt = (Integer) getSqlMapClient().queryForObject("MassOrderDAO.getUploadFileCnt",usrmstkey);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cnt;
	}

	public void insertRegUploadResult(List<FpisResultCommon> fpisResultCommonList, String usrmstkey, int cnt) {
		Map<String, String> param = new HashMap<String, String>();
		for (int i=0; i<cnt; i++) {
			param.put("usrmstkey", usrmstkey);
			param.put("filename", fpisResultCommonList.get(i).getCsvFileName());
			param.put("modmatchkey", fpisResultCommonList.get(i).getConnectKey());
			try {
				getSqlMapClient().insert("MassOrderDAO.insertRegUploadResult", param);
			}catch(SQLException e)  {
				logger.error("[ERROR] - SQLException : ", e);
			}
		}
		
	}

	public List<Map<String, Object>> getUploadFileList(List<Map<String, Object>> list, String usrmstkey) {
		//List<MassOrderUploadVO> fileList = new ArrayList<MassOrderUploadVO>();
		try {
			list = getSqlMapClient().queryForList("MassOrderDAO.getUploadFileList", usrmstkey);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	public void updateUploadResultY(String usrmstkey) {
		try {
			getSqlMapClient().update("MassOrderDAO.updateUploadResultY", usrmstkey);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*2023.01.17 jwchoi 화물우수인증정보망 개수 가져오기 */
	public int getMangMaxValue() {
		int result = 0;
		String tmp = "";
		try {
			tmp = (String)getSqlMapClient().queryForObject("MassOrderDAO.getMangMaxValue");
			result = Integer.parseInt(tmp);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	/*2023.04.10 jwchoi 화물우수인증정보망 유효한지 확인 */
	public int getVstring(String vString) {
		int result = 0;
		try {
			result = (Integer)getSqlMapClient().queryForObject("MassOrderDAO.getVstring", vString);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return result;
	}
	
	/* 2023.06.07 jwchoi 계약고유번호 검증 */
	public void insertIdentifyData(List<ContractInfoForSmalling> paramList) {
		Map<String, String> param = new HashMap<String, String>();
		for (int i=0; i<paramList.size(); i++) {
			param.put("uuid", paramList.get(i).getRegID());
			param.put("excel_row", paramList.get(i).getContractCount());
			param.put("agency_usr_mst_key", paramList.get(i).getAgencyUsrMstKey());
			param.put("contract_gubun", paramList.get(i).getCont_m_key());
			param.put("cont_from", paramList.get(i).getContStart());
			param.put("charge", paramList.get(i).getCharge());
			param.put("del_type", paramList.get(i).getDeliveryType());
			param.put("trans_type", paramList.get(i).getAnotherOper());
			try {
				getSqlMapClient().insert("MassOrderDAO.insertIdentifyData",param);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.error("ERROR : ", e);
			}
		}
	}
	
	/* 2023.06.07 jwchoi 계약고유번호 검증 */
	public List<ContractInfoForSmalling> selectIdentifyData(String uuid) {
		return (List<ContractInfoForSmalling>) list("MassOrderDAO.selectIdentifyData", uuid);
	}

}
