package fpis.reg.mass.service.impl;


import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import org.apache.poi.util.SystemOutLogger;
import org.springframework.stereotype.Service;

import fpis.reg.mass.service.ExtractObjectForSmalling;
import fpis.reg.mass.service.ExtractObjectForSpd;
import fpis.reg.mass.service.ExtractObjectInterface;
import fpis.reg.mass.service.MassOrderExtractService;
import fpis.reg.mass.res.RESOURCE_VAR;
import fpis.reg.mass.service.ContractInfoForSmalling;
import fpis.reg.mass.service.ContractInfoForSpd;
import fpis.reg.mass.service.CompanyInfoForSmalling;
import fpis.reg.mass.service.OperateInfoForSmalling;
import fpis.reg.mass.service.OperateInfoForSpd;
import fpis.reg.mass.service.TrustInfoForSmalling;
import fpis.reg.mass.service.TrustInfoForSpd;
import fpis.reg.mass.service.MassOrderVO;


/**
 * @class_desc 대량실적신고 파일변환 
 * fpis.reg.mass.service.impl
 * MassOrderExtractServiceImpl.java
 *
 * @DATE 2022. 09. 01.
 * @AUTHOR GnT 최정원
 * @HISTORY 
 * 
 * DATE			 	AUTHOR			NOTE
 * -------------	--------		--------------------
 * 2022. 09. 01.	최정원			최초생성
 */

@Service("MassOrderExtractService")
public class MassOrderExtractServiceImpl implements MassOrderExtractService {
	//jwchoi 수정할 곳
	private String user_id;
	private String usr_mst_key;
	
	@Override
	public ExtractObjectInterface extractData(List<String[]> dataParam, int type, String filename) {
		
		if(type == RESOURCE_VAR.TYPE_SPD){
			try{
				ExtractObjectForSpd extractObjectForSpd = new ExtractObjectForSpd();
				Map<String, ContractInfoForSpd> contractData = new HashMap<String, ContractInfoForSpd>();
				Map<Integer,OperateInfoForSpd> operateData = new HashMap<Integer,OperateInfoForSpd>();
				Map<Integer,TrustInfoForSpd> trustData = new HashMap<Integer,TrustInfoForSpd>();
				Map<String,String> keySaving = new HashMap<String,String>();
				Map<String,String> trustKeySaving = new HashMap<String,String>();
				Map<String,Long> contractCharge = new HashMap<String,Long>();
				Map<String,Long> trustCharge = new HashMap<String,Long>();

				ContractInfoForSpd contractInfoForSpd = null;
				OperateInfoForSpd operateInfoForSpd = null;
				TrustInfoForSpd trustInfoForSpd = null;

				String regID = "";
				String trustID = "";
				int operSeq = 0;
				int updateSeq = 1;
				int truSeq = 0;
				int seq = 1000000;
				int tseq = 1000000;
				for (int i = 0; i < dataParam.size(); i++) {
					String [] data = dataParam.get(i);
					contractInfoForSpd = new ContractInfoForSpd();
					operateInfoForSpd = new OperateInfoForSpd();
					trustInfoForSpd = new TrustInfoForSpd();

					for (int j = 0; j < data.length; j++) {
						if(j == 0) contractInfoForSpd.setContractCount(data[j]);
						if(j == 1) contractInfoForSpd.setContStart(data[j]);
						//if(j == 2) operateInfoForSpd.setCarUniqueID(data[j]);
						if(j == 2) operateInfoForSpd.setCarsMstKey(data[j]);
						if(j == 3) operateInfoForSpd.setOperEnd(data[j]);
						if(j == 4) operateInfoForSpd.setOperCnt(data[j]);
						if(j == 5) operateInfoForSpd.setCharge(data[j]);
						//if(j == 6) trustInfoForSpd.setTruUniqueID(data[j]);
						if(j == 6) trustInfoForSpd.setCompMstKey(data[j]);
						if(j == 7) trustInfoForSpd.setContStart(data[j]);
						if(j == 8) trustInfoForSpd.setCharge(data[j]);
					}

					boolean isExistKey = false;
					String cPrimaryKey = contractInfoForSpd.getContractCount()+contractInfoForSpd.getContStart();
					if(contractInfoForSpd.getContractCount() == null || "".equals(contractInfoForSpd.getContractCount())){
						//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
						regID = makeKeyNew(seq++);
						updateSeq = 1;
					}else{
						if(keySaving.keySet().size() > 0){
							if(keySaving.containsKey(cPrimaryKey)){ //같은키가 존재하는지 검사
								//GrobalLoggingManager.getLogger(DataVerify.class).debug("중복발생 계약구분값  : "+contractInfoForSpd.getContractCount());

								isExistKey = true;
								regID = keySaving.get(cPrimaryKey);
								updateSeq++;
							}else{
								//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
								regID = makeKeyNew(seq++);
								updateSeq = 1;
							}
						}else{
							//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
							regID = makeKeyNew(seq++);
							updateSeq = 1;
						}
					}

					contractInfoForSpd.setRegID(regID);
					operateInfoForSpd.setRegID(regID);

					trustID = "";

					//GrobalLoggingManager.getLogger(DataVerify.class).debug("trustInfoForSpd.isEmpty() = " + trustInfoForSpd.isEmpty());

					if(!trustInfoForSpd.isEmpty()){
						trustInfoForSpd.setRegID(regID);
						String tPrimaryKey = contractInfoForSpd.getContractCount()+ trustInfoForSpd.getCompMstKey()+trustInfoForSpd.getContStart()+trustInfoForSpd.getCharge();
						//GrobalLoggingManager.getLogger(DataVerify.class).debug("tPrimaryKey = " + tPrimaryKey);
						if(isExistKey){ //신규계약인지 판단
							boolean isTrustExistKey = false;
							if(trustKeySaving.keySet().size() > 0){ //위탁키저장소가 비어있는지 판단
								if(trustKeySaving.containsKey(tPrimaryKey)){ //위탁키저장소에 같은키가 존재하는지 검사
									//GrobalLoggingManager.getLogger(DataVerify.class).debug(tPrimaryKey+"와 같은키가 존재");
									isTrustExistKey = true;
									trustID = trustKeySaving.get(tPrimaryKey);
								}else{
									//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 위탁");
									trustID = trustMakeKeyNew(tseq++);
								}
							}else{
								//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 위탁");
								trustID = trustMakeKeyNew(tseq++);
							}

							operateInfoForSpd.setTrustID(trustID);
							if(!isTrustExistKey){
								//GrobalLoggingManager.getLogger(DataVerify.class).debug(tPrimaryKey+"와 같은키가 존재하지 않아서 신규로 추가");
								trustInfoForSpd.setRegID(regID);
								trustInfoForSpd.setTrustID(trustID);
								trustData.put(truSeq++, trustInfoForSpd);
								trustKeySaving.put(tPrimaryKey, trustID);
							}
						}else{
							//GrobalLoggingManager.getLogger(DataVerify.class).debug(isExistKey+"이전 계약으로 판단");
							trustID = trustMakeKeyNew(tseq++);
							operateInfoForSpd.setTrustID(trustID);
							trustInfoForSpd.setRegID(regID);
							trustInfoForSpd.setTrustID(trustID);
							trustData.put(truSeq++, trustInfoForSpd);
							trustKeySaving.put(tPrimaryKey, trustID);
						}
					}else{
						//GrobalLoggingManager.getLogger(DataVerify.class).debug("위탁정보 비어있음");
						operateInfoForSpd.setTrustID("");
					}

					if(!isExistKey){
						contractData.put(regID, contractInfoForSpd);
						keySaving.put(cPrimaryKey,regID);
					}

					if(contractCharge.containsKey(regID)){
						if(operateInfoForSpd.getCharge().equals("")){
							contractCharge.put(regID, (Long.parseLong(trustInfoForSpd.getCharge())+contractCharge.get(regID)));
						}else{
							contractCharge.put(regID, (Long.parseLong(operateInfoForSpd.getCharge())+contractCharge.get(regID)));
						}
					}else{
						if(operateInfoForSpd.getCharge().equals("")){
							contractCharge.put(regID, Long.parseLong(trustInfoForSpd.getCharge()));
						}else{
							contractCharge.put(regID, Long.parseLong(operateInfoForSpd.getCharge()));	
						}
					}

					if(!trustInfoForSpd.isEmpty()){
						if(!trustCharge.containsKey(regID+trustID)){
							/*trustCharge.put(regID+trustID, (Long.parseLong(trustInfoForSpd.getCharge())+trustCharge.get(regID+trustID)));
						}else{*/
							trustCharge.put(regID+trustID, Long.parseLong(trustInfoForSpd.getCharge()));
						}
					}

					operateInfoForSpd.setUpdateSeq(updateSeq);
					if(!operateInfoForSpd.getCharge().equals("")){
						operateData.put(operSeq++,operateInfoForSpd);
					}
				}
				//GrobalLoggingManager.getLogger(DataVerify.class).debug("[M]extractData 종료");

				extractObjectForSpd.setContractData(contractData);
				extractObjectForSpd.setOperateData(operateData);
				extractObjectForSpd.setTrustData(trustData);
				extractObjectForSpd.setContractCharge(contractCharge);
				extractObjectForSpd.setTrustCharge(trustCharge);

				extractObjectForSpd.updatePrice();
				return extractObjectForSpd;
			}catch(Exception e){
				e.printStackTrace();
			}
		} else if(type == RESOURCE_VAR.TYPE_SMALLING){
			ExtractObjectForSmalling extractData = new ExtractObjectForSmalling();
			Map<String, ContractInfoForSmalling> contractData = null;
			Map<String, CompanyInfoForSmalling> companyData = null;
			Map<Integer,OperateInfoForSmalling> operateData = null;
			Map<String,TrustInfoForSmalling> trustData = null;
			Map<String,String> keySaving = null;
			Map<String,String> t_keySaving = null;
			int agencyCount = 0;
			contractData = new HashMap<String, ContractInfoForSmalling>();
			companyData = new HashMap<String, CompanyInfoForSmalling>();
			operateData = new HashMap<Integer,OperateInfoForSmalling>();
			trustData = new HashMap<String,TrustInfoForSmalling>();
			keySaving = new HashMap<String,String>();
			t_keySaving = new HashMap<String,String>();

			extractData.setOrijinData(dataParam);
			String primaryKeyPattern = "";
			String cPrimaryKey = "";
			String tPrimaryKey = "";
			String trustKeyPattern = "";

			trustKeyPattern = RESOURCE_VAR.TYPE_SMALLING_TRUSTKEYMAKE_VERIFYBITSTRING;
			primaryKeyPattern = RESOURCE_VAR.TYPE_SMALLING_CONTRACTCHECK_VERIFYBITSTRING;

			ContractInfoForSmalling contractInfo = null;
			OperateInfoForSmalling operateInfo = null;
			TrustInfoForSmalling trustInfo = null;
			String compBsnsNum = "";
			String compGubun = "";
			String contractCount = "";

			String regID = "";
			String trustID = "";
			int seq = 1;
			int t_seq = 1;

			int operSeq = 1;
			int operKeySeq = 1;
			int trustKeySeq = 1;
			for (int i = 0; i < dataParam.size(); i++) {
				//GrobalLogger.getLogger(DataVerify.class).debug("데이터 분해 "+i+"번째 작업중...");
				contractInfo = new ContractInfoForSmalling();
				operateInfo = new OperateInfoForSmalling();
				trustInfo = new TrustInfoForSmalling();
				String [] data = dataParam.get(i);

				cPrimaryKey = makeMixKey(data, primaryKeyPattern);
				tPrimaryKey = makeMixKey(data, trustKeyPattern);

				for (int j = 0; j < data.length; j++) {
					contractInfo.setCont_m_key(cPrimaryKey);
					if(j == 0) compBsnsNum = data[j];
					if(j == 1) compGubun = data[j];

					/*계약정보*/
					if(j == 2) {
						contractInfo.setContractCount(data[j]);
						operateInfo.setContractCount(data[j]);
						trustInfo.setContractCount(data[j]);
						contractCount = data[j];
						contractInfo.setAgencyUsrMstKey("0");
					}
					if(j == 3) contractInfo.setContStart(data[j]);
					if(j == 4) contractInfo.setCharge(data[j]);
					if(j == 5) contractInfo.setDeliveryType(data[j]);
					if(j == 6) contractInfo.setAnotherOper(data[j]);
					/*계약정보*/

					/*배차정보*/
					if(j == 7) operateInfo.setCarsMstKey(data[j]);
					if(j == 8) operateInfo.setOperEnd(data[j]);
					if(j == 9) operateInfo.setOperCnt(data[j]);
					if(j == 10) operateInfo.setCharge(data[j]);
					/*배차정보*/

					if(j == 11) trustInfo.setCompMstKey(data[j]);
					if(j == 12) trustInfo.setContStart(data[j]);
					if(j == 13) trustInfo.setCharge(data[j]);
					if(j == 14) trustInfo.setUseNetwork(data[j]);
				}

				boolean isExistKey = false;
				if(contractInfo.getContractCount() == null || "".equals(contractInfo.getContractCount())){
					//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
					regID = makeKeyNew(seq++);
					operSeq = 1;
				}else{
					if(keySaving.keySet().size() > 0){
						if(keySaving.containsKey(cPrimaryKey)){ //같은키가 존재하는지 검사
							//GrobalLoggingManager.getLogger(DataVerify.class).debug("중복발생 계약구분값  : "+contractInfo.getContractCount());

							isExistKey = true;
							regID = keySaving.get(cPrimaryKey);
						}else{
							//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
							regID = makeKeyNew(seq++);
							operSeq = 1;
						}
					}else{
						//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
						regID = makeKeyNew(seq++);
						operSeq = 1;
					}
				}

				//GrobalLoggingManager.getLogger(DataVerify.class).debug(cPrimaryKey+"  ,  "+regID);
				CompanyInfoForSmalling c = new CompanyInfoForSmalling(compBsnsNum, compGubun, compGubun, contractCount);    //2018.01.30 written by dyahn
				
				c.setRegID(regID);
				if(!isExistKey){
					companyData.put(regID, c);
				}

				contractInfo.setRegID(regID);
				operateInfo.setRegID(regID);
				trustInfo.setRegID(regID);
				contractInfo.setAgencyYn("N");
				if(contractInfo.getCharge() != null){
					if(!isExistKey){
						contractData.put(regID, contractInfo);
					}
				}

				operateInfo.setUpdateSeq(operSeq++);
				if(operateInfo.exist()){
					operateData.put(operKeySeq++,operateInfo);
				}

				if(trustInfo.exist()){
					trustID = trustMakeKeyNew(t_seq++);
					trustInfo.setTrustID(trustID);
					trustData.put(trustID, trustInfo);
				}

				if(!isExistKey) keySaving.put(cPrimaryKey,regID);


			}

			extractData.setContractData(contractData);
			extractData.setOperateData(operateData);
			extractData.setTrustData(trustData);
			extractData.setCompanyData(companyData);
			extractData.setAgencyCount(0);

			return extractData;
			
			
		}else if(type == RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY){
			
			//2017. 09. 20 written by dyahn 순수주선 데이터 병합
			ExtractObjectForSmalling extractData = new ExtractObjectForSmalling();
			Map<String, ContractInfoForSmalling> contractData = null;
			Map<String, CompanyInfoForSmalling> companyData = null;
			Map<Integer,OperateInfoForSmalling> operateData = null;
			Map<String,TrustInfoForSmalling> trustData = null;
			Map<String,String> keySaving = null;
			Map<String,String> t_keySaving = null;
			int agencyCount = 0;
			contractData = new HashMap<String, ContractInfoForSmalling>();
			companyData = new HashMap<String, CompanyInfoForSmalling>();
			operateData = new HashMap<Integer,OperateInfoForSmalling>();
			trustData = new HashMap<String,TrustInfoForSmalling>();
			keySaving = new HashMap<String,String>();
			t_keySaving = new HashMap<String,String>();

			extractData.setOrijinData(dataParam);
			String primaryKeyPattern = "";
			String cPrimaryKey = "";
			String tPrimaryKey = "";
			String trustKeyPattern = "";

			trustKeyPattern = RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_TRUSTKEYMAKE_VERIFYBITSTRING;
			primaryKeyPattern = RESOURCE_VAR.TYPE_SMALLING_FORWARDONLY_CONTRACTCHECK_VERIFYBITSTRING;

			ContractInfoForSmalling contractInfo = null;
			OperateInfoForSmalling operateInfo = null;
			TrustInfoForSmalling trustInfo = null;
			String compBsnsNum = "";
			String compGubun = "";
			String contractCount = "";

			String regID = "";
			String trustID = "";
			int seq = 1;
			int t_seq = 1;

			int operSeq = 1;
			int operKeySeq = 1;
			int trustKeySeq = 1;
			for (int i = 0; i < dataParam.size(); i++) {
				//GrobalLogger.getLogger(DataVerify.class).debug("데이터 분해 "+i+"번째 작업중...");
				contractInfo = new ContractInfoForSmalling();
				operateInfo = new OperateInfoForSmalling();
				trustInfo = new TrustInfoForSmalling();
				String [] data = dataParam.get(i);
				//regID = makeKey(data, keyPattern);


				cPrimaryKey = makeMixKey(data, primaryKeyPattern);
				tPrimaryKey = makeMixKey(data, trustKeyPattern);

				for (int j = 0; j < data.length; j++) {
					contractInfo.setCont_m_key(cPrimaryKey);
					if(j == 0) compBsnsNum = data[j];
					if(j == 1) compGubun = data[j];

					/*계약정보*/
					if(j == 2) {
						contractInfo.setContractCount(data[j]);
						operateInfo.setContractCount(data[j]);
						trustInfo.setContractCount(data[j]);
						contractCount = data[j];
						contractInfo.setAgencyUsrMstKey("0");
					}
					if(j == 3) contractInfo.setContStart(data[j]);
					if(j == 4) contractInfo.setCharge(data[j]);
					if(j == 5) contractInfo.setDeliveryType(data[j]);
					if(j == 6) contractInfo.setAnotherOper(data[j]);
					/*계약정보*/

					/*위탁정보*/
					if(j == 7) trustInfo.setCompMstKey(data[j]);
					if(j == 8) trustInfo.setContStart(data[j]);
					if(j == 9) trustInfo.setCharge(data[j]);
					if(j == 10) trustInfo.setUseNetwork(data[j]);
					/*위탁정보*/
				}

				boolean isExistKey = false;
				if(contractInfo.getContractCount() == null || "".equals(contractInfo.getContractCount())){
					//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
					regID = makeKeyNew(seq++);
					operSeq = 1;
				}else{
					if(keySaving.keySet().size() > 0){
						if(keySaving.containsKey(cPrimaryKey)){ //같은키가 존재하는지 검사
							//GrobalLoggingManager.getLogger(DataVerify.class).debug("중복발생 계약구분값  : "+contractInfo.getContractCount());

							isExistKey = true;
							regID = keySaving.get(cPrimaryKey);
						}else{
							//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
							regID = makeKeyNew(seq++);
							operSeq = 1;
						}
					}else{
						//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
						regID = makeKeyNew(seq++);
						operSeq = 1;
					}
				}

				//GrobalLoggingManager.getLogger(DataVerify.class).debug(cPrimaryKey+"  ,  "+regID);

				//CompanyInfoForSmalling c = new CompanyInfoForSmalling(compBsnsNum, compGubun, "99", contractCount);
				CompanyInfoForSmalling c = new CompanyInfoForSmalling(compBsnsNum, compGubun, compGubun, contractCount);    //2018.01.30 written by dyahn
				c.setRegID(regID);
				if(!isExistKey){
					companyData.put(regID, c);
				}

				contractInfo.setRegID(regID);
				operateInfo.setRegID(regID);
				trustInfo.setRegID(regID);
				contractInfo.setAgencyYn("N");
				if(contractInfo.getCharge() != null){
					if(!isExistKey){
						contractData.put(regID, contractInfo);
					}
				}

				operateInfo.setUpdateSeq(operSeq++);
				if(operateInfo.exist()){
					operateData.put(operKeySeq++,operateInfo);
				}

				if(trustInfo.exist()){
					trustID = trustMakeKeyNew(t_seq++);
					trustInfo.setTrustID(trustID);
					trustData.put(trustID, trustInfo);
				}

				if(!isExistKey) keySaving.put(cPrimaryKey,regID);


			}

			extractData.setContractData(contractData);
			extractData.setOperateData(operateData);
			extractData.setTrustData(trustData);
			extractData.setCompanyData(companyData);
			extractData.setAgencyCount(0);

			return extractData;
			
			
		}else if(type == RESOURCE_VAR.TYPE_SMALLING_DAEHANG){
			ExtractObjectForSmalling extractData = new ExtractObjectForSmalling();
			Map<String, ContractInfoForSmalling> contractData = null;
			Map<String, CompanyInfoForSmalling> companyData = null;
			Map<Integer,OperateInfoForSmalling> operateData = null;
			Map<String,TrustInfoForSmalling> trustData = null;
			Map<String,String> keySaving = null;
			Map<String,String> t_keySaving = null;
			int agencyCount = 0;
			contractData = new HashMap<String, ContractInfoForSmalling>();
			companyData = new HashMap<String, CompanyInfoForSmalling>();
			operateData = new HashMap<Integer,OperateInfoForSmalling>();
			trustData = new HashMap<String,TrustInfoForSmalling>();
			keySaving = new HashMap<String,String>();
			t_keySaving = new HashMap<String,String>();

			String primaryKeyPattern = "";
			String cPrimaryKey = "";
			String tPrimaryKey = "";
			String trustKeyPattern = "";

			trustKeyPattern = RESOURCE_VAR.TYPE_SMALLING_DAEHANG_TRUSTKEYMAKE_VERIFYBITSTRING;
			primaryKeyPattern = RESOURCE_VAR.TYPE_SMALLING_DAEHANG_CONTRACTCHECK_VERIFYBITSTRING;

			ContractInfoForSmalling contractInfo = null;
			OperateInfoForSmalling operateInfo = null;
			TrustInfoForSmalling trustInfo = null;
			String compBsnsNum = "";
			String compGubun = "";
			String contractCount = "";
			extractData.setOrijinData(dataParam);

			String regID = "";
			String trustID = "";
			int seq = 1;
			int t_seq = 1;

			int operSeq = 1;
			int operKeySeq = 1;
			int trustKeySeq = 1;
			for (int i = 0; i < dataParam.size(); i++) {
				//GrobalLogger.getLogger(DataVerify.class).debug("데이터 분해 "+i+"번째 작업중...");
				contractInfo = new ContractInfoForSmalling();
				operateInfo = new OperateInfoForSmalling();
				trustInfo = new TrustInfoForSmalling();
				String [] data = dataParam.get(i);
				//regID = makeKey(data, keyPattern);


				cPrimaryKey = makeMixKey(data, primaryKeyPattern);
				trustInfo.setTrustID(makeMixKey(data, trustKeyPattern));

				for (int j = 0; j < data.length; j++) {
					contractInfo.setCont_m_key(cPrimaryKey);
					if(j == 0) compBsnsNum = data[j];
					if(j == 1) compGubun = data[j];

					/*계약정보*/
					if(j == 2) {
						contractInfo.setContractCount(data[j]);
						operateInfo.setContractCount(data[j]);
						trustInfo.setContractCount(data[j]);
						contractCount = data[j];
					}
					if(j == 3) contractInfo.setAgencyUsrMstKey(data[j]);
					if(j == 4) contractInfo.setContStart(data[j]);
					if(j == 5) contractInfo.setCharge(data[j]);
					if(j == 6) contractInfo.setDeliveryType(data[j]);
					if(j == 7) contractInfo.setAnotherOper(data[j]);
					/*계약정보*/

					/*배차정보*/
					if(j == 8) operateInfo.setCarsMstKey(data[j]);
					if(j == 9) operateInfo.setOperEnd(data[j]);
					if(j == 10) operateInfo.setOperCnt(data[j]);
					if(j == 11) operateInfo.setCharge(data[j]);
					/*배차정보*/

					if(j == 12) trustInfo.setCompMstKey(data[j]);
					if(j == 13) trustInfo.setContStart(data[j]);
					if(j == 14) trustInfo.setCharge(data[j]);
					if(j == 15) trustInfo.setUseNetwork(data[j]);
				}

				boolean isExistKey = false;
				if(contractInfo.getContractCount() == null || "".equals(contractInfo.getContractCount())){
					//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
					regID = makeKeyNew(seq++);
					operSeq = 1;
				}else{
					if(keySaving.keySet().size() > 0){
						if(keySaving.containsKey(cPrimaryKey)){ //같은키가 존재하는지 검사
							//GrobalLoggingManager.getLogger(DataVerify.class).debug("중복발생 계약구분값  : "+contractInfo.getContractCount());

							isExistKey = true;
							regID = keySaving.get(cPrimaryKey);
						}else{
							//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
							regID = makeKeyNew(seq++);
							operSeq = 1;
						}
					}else{
						//GrobalLoggingManager.getLogger(DataVerify.class).debug(i+"번째 신규 계약");
						regID = makeKeyNew(seq++);
						operSeq = 1;
					}
				}

				CompanyInfoForSmalling c = new CompanyInfoForSmalling(compBsnsNum, compGubun, compGubun, contractCount);    //2018.01.30 written by dyahn
				c.setRegID(regID);
				if(!isExistKey){
					companyData.put(regID, c);
				}

				contractInfo.setRegID(regID);
				operateInfo.setRegID(regID);
				trustInfo.setRegID(regID);

				if(contractInfo.getAgencyUsrMstKey().equals("0")){
					contractInfo.setAgencyYn("N");
				}else{
					contractInfo.setAgencyYn("Y");
					agencyCount++;
				}
				if(contractInfo.getCharge() != null){
					if(!isExistKey){
						contractData.put(regID, contractInfo);
					}
				}

				operateInfo.setUpdateSeq(operSeq++);
				if(operateInfo.exist()){
					operateData.put(operKeySeq++,operateInfo);
				}

				if(trustInfo.exist()){
					trustInfo.printObj();
					trustID = trustMakeKeyNew(t_seq++);
					trustInfo.setTrustID(trustID);
					trustData.put(regID+trustID, trustInfo);
				}

				if(!isExistKey) keySaving.put(cPrimaryKey,regID);
			}


			////System.out.println("trustData.size = " + trustData.size());
			extractData.setContractData(contractData);
			extractData.setOperateData(operateData);
			extractData.setTrustData(trustData);
			extractData.setCompanyData(companyData);
			extractData.setAgencyCount(agencyCount);

			return extractData;
		}
		return null;
	}
	
	private String makeMixKey(String [] data, String pattern){
		//GrobalLoggingManager.getLogger(DataVerify.class).debug(String.format("pattern = %s", pattern));
		String keyPreFix = "";
		String key = "";
		for (int i = 0; i < pattern.length(); i++) {
			if(String.valueOf(pattern.charAt(i)).equals("1")){
				key += data[i];
			}
			if(String.valueOf(pattern.charAt(i)).equals("2")){
				if(data[i].equals("0")){
					keyPreFix = MassOrderVO.getUsrMstKeyAtSystem();
				}else{
					keyPreFix = data[i];
				}
			}
		}
		////System.out.println("keyPreFix+key = " + keyPreFix+key);
		return keyPreFix+key;
	}
	
	/**
	 * RegID를 생성한다.
	 * U(입력방식에 대한 구분) + System.currentTimeMillis() + "_" + usr_mst_key + "_" + seq
	 * @return
	 */
	private String makeKeyNew(int seq){
		//return "U"+System.currentTimeMillis()+"_"+Activator.usr_mst_key+"_"+seq;
		Random r = new Random();
		return seq+"_R"+"_"+MassOrderVO.getUserIDAtSystem()+System.currentTimeMillis()+r.nextInt(10);
	}
	
	private String trustMakeKeyNew(int seq){
		//return "U"+System.currentTimeMillis()+"_"+Activator.usr_mst_key+"_"+seq;
		Random r = new Random();
		return seq+"_T"+"_"+MassOrderVO.getUserIDAtSystem()+System.currentTimeMillis()+r.nextInt(10);
	}

	@Override
	public int getDeahangBsnsNumCount(Map<String, ContractInfoForSmalling> contractMaps) {
		List<String> resultList = new ArrayList<String>();
		int result = 0;
		Set<String> conSet = contractMaps.keySet();
		Iterator<String> conIter = conSet.iterator();

		while (conIter.hasNext()) {
			ContractInfoForSmalling cifs = contractMaps.get(conIter.next());
			if(!resultList.contains(cifs.getAgencyUsrMstKey())){
				result++;
				resultList.add(cifs.getAgencyUsrMstKey());
			}
		}

		resultList = null;

		return result;
	}


}
