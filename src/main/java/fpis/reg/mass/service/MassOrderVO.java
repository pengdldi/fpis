package fpis.reg.mass.service;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import fpis.reg.mass.web.MassOrderController;

/**
 * @class_desc	
 * fpis.reg.mass.service
 * MassOrderVO.java
 *
 * @DATE	2022. 09. 05.
 * @AUTHOR	GnT 최정원
 * @HISTORY
 * DATE			 	AUTHOR			NOTE
 * -------------	--------		--------------------
 * 2022. 09. 05.	최정원			최초생성
 */

@Repository("MassOrderVO")  
public class MassOrderVO {
	
	private static final Logger	logger	= Logger.getLogger(MassOrderController.class);
	
	private static String usr_id;
	private static String usr_mst_key;
	private static String cond;
	private static String condDetail;
	private static String savefilename;
	
	public static String getUserIDAtSystem() {
		return usr_id;
	}
	
	public String setUserIDAtSystem(String usr_id) {
		return MassOrderVO.usr_id = usr_id;
	}
	
	public static String getUsrMstKeyAtSystem(){
		return usr_mst_key;
	}
	
	public String setUsrMstKeyAtSystem(String usr_mst_key){
		return MassOrderVO.usr_mst_key = usr_mst_key;
	}
	
	public static String getCond() {
		return cond;
	}
	
	public String setCond(String cond) {
		return MassOrderVO.cond = cond;
	}
	
	public static HashMapAddFunc<String, Character> getCondDetail() {
		HashMapAddFunc<String,Character> detailMap = new HashMapAddFunc<String, Character>();
		String [] cdArr = condDetail.split("[,]");
		for (int i = 0; i < cdArr.length; i++) {
			detailMap.put(cdArr[i], '1');
		}
		return detailMap;
	}
	
	public String setCondDetail(String condDetail) {
		return MassOrderVO.condDetail = condDetail;
	}
	
	public static String getSaveFileName() {
		return savefilename;
	}
	
	public String setSaveFileName(String cond) {
		return MassOrderVO.savefilename = cond;
	}

}
