package fpis.online.stdinfo.client.service.impl;

import java.sql.SQLException;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import egovframework.rte.fdl.cmmn.EgovAbstractServiceImpl;
import fpis.common.vo.usr.UsrInfoVO;
import fpis.online.stdinfo.client.service.FpisClientManageService;
import fpis.online.stdinfo.client.service.FpisNewJoinVO;
import fpis.online.stdinfo.client.service.FpisSysCompanyVO;
import fpis.online.stdinfo.client.service.FpisUsrCompanyMasterVO;
import fpis.online.stdinfo.client.service.FpisUsrCompanyVO;

@Service("FpisClientManageService")
public class FpisClientManageServiceImpl  extends EgovAbstractServiceImpl  implements FpisClientManageService {

    @Resource(name = "FpisClientManagerDAO")
    private FpisClientManagerDAO dao;
    
    @Override
    public int selectUsrCompanyCount(FpisUsrCompanyVO vo) throws Exception {
        return dao.selectUsrCompanyCount(vo);
    }
    @Override
    public List<FpisUsrCompanyVO> selectUsrCompanyList(FpisUsrCompanyVO vo) throws Exception {
        return dao.selectUsrCompanyList(vo);
    }
    
    @Override
    public int selectSysCompanyCnt(FpisSysCompanyVO vo) throws Exception {
        return dao.selectSysCompanyCnt(vo);
    }
    @Override
    public List<FpisSysCompanyVO> selectSysCompanyList(FpisSysCompanyVO vo) throws Exception {
        return dao.selectSysCompanyList(vo);
    }
    
    @Override
    public int insertMemberComp(List<FpisSysCompanyVO> vo) throws Exception {
        return dao.insertUsrComp(vo);
    }
    @Override
    public int deleteMemberComp(List<FpisSysCompanyVO> vo) throws Exception {
        return dao.deleteUsrComp(vo);
    }
    
    /*******************************************************************************
     * 자  성  일   : 2013. 9. 26.
     * 작  성  자   : mgkim
     * 변경  이력 : 2013. 9. 26. - 최초 생성
     *******************************************************************************/
    @Override
    public int isExistUsrCompanyInfo(FpisUsrCompanyVO compVO) throws SQLException {
        return dao.isExistUsrCompanyInfo(compVO);
    }
    
    /*******************************************************************************
     * 자  성  일   : 2013. 9. 26.
     * 작  성  자   : mgkim
     * 변경  이력 : 2013. 9. 26. - 최초 생성
     *******************************************************************************/
    @Override
    public int insertUsrCompanyInfo(FpisUsrCompanyVO compVO) throws SQLException {
        return dao.insertUsrCompanyInfo(compVO);
    }
    
    /*******************************************************************************
     * 자  성  일   : 2015. 2. 16.
     * 작  성  자   : mgkim
     * 변경  이력 : 2015. 2. 16. - 최초 생성
     *******************************************************************************/
    @Override
    public int deleteUsrCompanyAll(FpisUsrCompanyVO shVO) {
        return dao.deleteUsrCompanyAll(shVO);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    /******************************************************************************************************************
     * 2015.02.12 mgkim 하단 거래처관리 서비스 아님..
     * 1. 회원가입 관련 서비스 소스 분리작업
     * 2. 실적신고 관련 거래처 조회 서비스
     *******************************************************************************************************************/
    
    
    
    /*******************************************************************************
     * 자  성  일   : 2013. 10. 14.
     * 작  성  자   : jhoh
     * 설         명 : 등록되어 있는 사용자 정보 가져오기(USR_INFO)
     * 변경  이력 : 2013. 10. 14. - 초최 생성
     *******************************************************************************/
    @Override
    public List<UsrInfoVO> joinUsrCompList(FpisNewJoinVO shVO) throws Exception {
        return dao.joinUsrCompList(shVO);
    }
    @Override
    public List<FpisSysCompanyVO> joinSysCompList(FpisNewJoinVO vo) throws Exception {
        return dao.joinSysCompList(vo);
    }
    /*******************************************************************************
     * 자  성  일   : 2013. 10. 10.
     * 작  성  자   : mgkim
     * 변경  이력 : 2013. 10. 10. - 초최 생성
     *******************************************************************************/
//    @Override
//    public List<UsrInfoVO> selectUsrInfoNetList(UsrInfoVO vo) throws Exception {
//        return (List<UsrInfoVO>)dao.selectUsrInfoNetList(vo);
//    }
    /*******************************************************************************
     * 자  성  일   : 2013. 10. 10.
     * 작  성  자   : mgkim
     * 변경  이력 : 2013. 10. 10. - 초최 생성
     *******************************************************************************/
    @Override
    public UsrInfoVO selectUsrInfo(String usr_mst_key) throws Exception {
        return dao.selectUsrInfo(usr_mst_key);
    }
    /*******************************************************************************
     * 자  성  일   : 2013. 10. 10.
     * 작  성  자   : mgkim
     * 변경  이력 : 2013. 10. 10. - 초최 생성
     *******************************************************************************/
    @Override
    public int updateUsrInfo(UsrInfoVO usrInfoVO) throws Exception {
        return dao.updateUsrInfo(usrInfoVO);
    }
    
    
    @Override
    public FpisSysCompanyVO getSysCompanyPk(String  comp_key) throws Exception {
        return dao.getSysCompanyPk(comp_key);
    }
    @Override
	public FpisSysCompanyVO getSysCompanyPk2(String  comp_key) throws Exception {
        return dao.getSysCompanyPk2(comp_key);
    }
    
    
    @Override
    public FpisUsrCompanyMasterVO getUserCompInfoByPk(String usr_mst_key,String comp_mst_key) throws Exception {
        return dao.getUserCompInfoByPk(usr_mst_key,comp_mst_key);
    }
    /*******************************************************************************
     * 자  성  일   : 2012. 12. 7.
     * 작  성  자   : Administrator
     * 변경  이력 : 2012. 12. 7. - 초최 생성
     *******************************************************************************/
    @Override
    public List<FpisSysCompanyVO> searchSysCompLis(FpisSysCompanyVO vo) throws Exception {
        return dao.searchSysCompLis(vo); 
    }
    /**
     * 기초정보 관리 -> 운송의뢰자(화주관리) -> 상세보기 
     *            :  운송의뢰자(화주)리스트중 선택하여 상세보기 정보 가져오기     
     * 리턴                : 운송의뢰자(화주)정보 VO                 
     */
    @Override
    public FpisUsrCompanyMasterVO getUserCompMasterByPk(String usr_mst_key,String comp_mst_key) throws Exception {
        return dao.getUserCompMasterByPk(usr_mst_key,comp_mst_key);
    }
    
    
//  @Override
//  public List<FpisUsrCompanyVO> searchMemberCompAll(FpisUsrCompanyVO vo) throws Exception {
//      return (List<FpisUsrCompanyVO>)dao.searchMemberCompAll(vo);
//  }
    // 2013.09.09 mgkim 사업자번호 시스템 존재 확인 
    @Override
    public int isSysCompanyInfoCount(String comp_bsns_num) throws Exception {
        return dao.isSysCompanyInfoCount(comp_bsns_num);
    }
    @Override
    public int isUsrInfoCount(String comp_bsns_num) throws Exception {
        return dao.isUsrInfoCount(comp_bsns_num);
    }
    /*******************************************************************************
     * 자  성  일   : 2013. 12. 13.
     * 작  성  자   : mgkim
     * 변경  이력 : 2013. 12. 13. - 최초 생성
     *******************************************************************************/
    @Override
    public String getCompNmUsrInfoOrSysInfo(String comp_bsns_num) throws Exception {
        return dao.getCompNmUsrInfoOrSysInfo(comp_bsns_num);
    }
	/*******************************************************************************
	 * 자  성  일   : 2015. 11. 19.
	 * 작  성  자   : Administrator
	 * 경       로   : 
	 * 메  소  드   : isSeolCount
	 * 파 라 메 터:
	 * 리         턴 :
	 * 변경  이력 : 2015. 11. 19. - 초최 생성
	 *******************************************************************************/
	@Override
	public int isSeolCount(String comp_bsns_num) throws Exception {
		return dao.isSeolCount(comp_bsns_num);
	}
	
	@Override
	public int updateSysCompInfo(FpisNewJoinVO shVO) throws Exception {
		return dao.updateSysCompInfo(shVO);
	}
	
	/* 2023.02.27 jwchoi 웹취약점 조치 - 사업자등록번호 검증 서버단 처리
	 * fpis_com.js > fpis_CompBsnsNum 참고 */
	/*******************************************************************************
	 * bizID 는 10자리 숫자이다. [ 000 - 00 - 00000 ] 숫자만 입력받아옴.
	 * 3자리 : 국세청과 세무서별 코드
	 * 2자리 : 개인,법인 구분
	 * 5자리 : 앞4자리는 과세사업자,면세사업자,법인사업자 번호  / 마지막1자리는 오류검증 번호
	 *******************************************************************************/
	@Override
	public String chkCompBsnsNum(String comp_bsns_num) {
		String regex = "[0-9]+";
		if (comp_bsns_num.matches(regex) && comp_bsns_num.length() == 10) {
			//숫자만 있음
			int[] checkID = {1,3,7,1,3,7,1,3,5,1};
			int chkSum = 0;
			int tmpBizID = 0;
			int remander = 0;
			String c2 = "";
			for (int i=0; i<=7; i++) {
				chkSum += checkID[i] * Character.getNumericValue(comp_bsns_num.charAt(i));
			}
			tmpBizID = checkID[8] * Character.getNumericValue(comp_bsns_num.charAt(8));
			c2 = Integer.toString(tmpBizID);
			c2 = "0"+c2;
			c2 = c2.substring(c2.length()-2, c2.length());
			chkSum += Math.floor(Character.getNumericValue(c2.charAt(0))) +
					  Math.floor(Character.getNumericValue(c2.charAt(1)));
			remander = (10-(chkSum % 10)) % 10;
			if(Math.floor(Character.getNumericValue(comp_bsns_num.charAt(9))) == remander) {
				return "SUC";
			} else {
				return "ERR";
			}
		} else {
			//숫자 외 값 존재
			return "ERR";
		}
	}
    
}
