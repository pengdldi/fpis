package fpis.reg.mass.service;

import java.util.List;
import java.util.Map;

/**
 * @method_desc	대량실적신고 업로드 파일 변환
 * @HISTORY
 * DATE			 	AUTHOR			NOTE
 * -------------	---------		------------------------
 * 2022. 09. 01.	최정원			최초생성
 *
 */
public interface MassOrderExtractService {
	
	public ExtractObjectInterface extractData(List<String[]> importData, int COL_CNT, String oriFileName);

	public int getDeahangBsnsNumCount(Map<String, ContractInfoForSmalling> contractData);

}
