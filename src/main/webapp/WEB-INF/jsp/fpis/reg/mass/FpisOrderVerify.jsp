<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page language="java" import="java.util.*,fpis.common.vo.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" uri="http://egovframework.gov/ctl/ui"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<%
	String contextPath = request.getContextPath();

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/ TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>:: 화물운송관리 시스템 :: 대량실적 등록</title>
	<!-- 2021.01.11 ysw 정보노출 보안처리 -->
	<%  
	    response.setHeader("Cache-Control","no-store");  
	    response.setHeader("Pragma","no-cache");  
	    response.setDateHeader("Expires",0);  
	    if (request.getProtocol().equals("HTTP/1.1")){
		    response.setHeader("Cache-Control", "no-cache");
	    }
    %>
    <!-- 2021.01.11 ysw 정보노출 보안처리끝 --> 
	<link href="<%= request.getContextPath() %>/css/fpis/userstyle2.css" rel="stylesheet" type="text/css" />	
	<link href="<%= request.getContextPath() %>/css/jquery/jquery-ui-1.8.23.custom.css" rel="stylesheet" type="text/css" />
</head>
<body>
<form name="failfile_form" method="post">
        <input type="hidden" name="fileCls"/>
        <input type="hidden" name="fileName"/>
        <input type="hidden" name="fileImportData"/>
</form> 
<div id="contwrap">
	<div class="location">
		<table border="0" cellspacing="0" cellpadding="0">
		  <tr>
			<td>홈</td>
			<td>실적신고</td>
			<th>대량실적 등록::계약검증</th>				 				
		  </tr>
		</table>
	</div>

	<div class="subtitle">
		<p>대량실적&nbsp;&nbsp;등록&nbsp;&nbsp;::&nbsp;&nbsp;계약검증</p>				
	</div>	
	    <!-- CONTENT NOTICE -->
    <div class="contnotice">
        <table>
            <tr>
                <th><img src="${contextPath}/images/fpis2013/body_content/contents_notice.png" /></th>
                <td>
                    <br/>1. [양식 내려받기]를 통해 엑셀 양식을 다운로드 받으실 수 있습니다.<br/>
                    <br/>2. 엑셀 파일은 최대 10개까지 선택 가능합니다. 엑셀 1개 당 입력 가능한 계약은 10000건 입니다.<br/>
                         * 계약고유번호는 같은 엑셀 파일 내 있는 계약건 에만 적용됩니다.<br/>
                           (예시) 예제A.xlsx 파일의 계약고유번호 [ABC123] 과 예제B.xlsx 파일의 계약고유번호 [ABC123] 은 다른 계약으로 간주합니다.<br/>
                    <br/>3. 실적 대행등록의 경우 최대 1000업체 까지 대행등록이 가능합니다.<br/>
                    <br/>4. [의뢰자정보] - [사업자등록번호]에서 '화주'가 아닌 '운수사’ 로부터 위탁 받은 실적의 재위탁은 거래 위반입니다.<br/>
                    <br/>5. [배차정보] - [차량등록번호] 입력은 상단에 있는[정보관리] - [차량관리] 메뉴에서 등록하신 차량정보의 차량번호를 <br/>정확히 등록해 주셔야 합니다.<br/>
                            * 만약 직영, 위수탁(지입), 장기용차의 차량정보를 정상등록하지 않을 시 직접운송 거래로 인정되지 않습니다.<br/>
                    <br/>6. 옵션 설정<br/>
                            - 병합 및 계약고유번호 자동부여 : 병합 후 데이터의 계약고유번호를 재부여합니다.<br/>
                            - 원본 유지 : 이미 월단위로 병합한 데이터인 경우 선택합니다. 계약고유번호가 없는 경우 개별 계약으로 처리합니다.<br/>
                            - 택배 실적 :  택배 실적을 등록할 경우 선택합니다.<br/>
                         * 데이터 병합 발생시 기존 계약고유번호는 병합 후 모두 사라집니다.<br/>
                    <br/>7. 파일 선택 후 [검증 시작] 버튼을 누르시면 실적 검증이 이뤄집니다.<br/>
                    <br/>8. 모든 파일의 상태가 [검증 성공]이 되면 하단에 [다음] 버튼을 선택합니다.<br/>

                </td>
            </tr>
        </table>
    </div>

    <div class="brdwrap">
    	<div class="brd_head">
	    	<div class="contitle"><img src="<%= contextPath %>/images/fpis/icon_title.gif" /> 옵션 설정</div>
		</div>
		<form class="brd_bottom_left"  id ="option_form" name="option_form" method="post" enctype="multipart/form-data">
			<input type="hidden" name="fileCls"/>
			<input type="hidden" name ="usrCond" id="usrCond"  value= "${usrCond}" />
			<input type="hidden" id="bcode" name="bcode" value="${bcode}" />
        	<input type="hidden" id="rcode" name="rcode" value="${rcode}" />
        	<input type="hidden" id="hid_contextPath" name="hid_contextPath" value="<%= contextPath %>" />
			<input type="hidden" name="fileStatus" id="fileStatus" value="N"/>
			
			<label class="cont_group"><input type="radio" name="reg_option" value="cont_group" class="brdbtn_radio" /><b> 병합 및 계약고유번호 자동부여</b></label>
			<label class="no_merge"><input type="radio" name="reg_option" value="no_merge" class="brdbtn_radio" checked /><b> 원본 유지</b></label>
			<c:choose>
				<c:when test = "${usrCond eq 'T'}">
					<label><input type="radio" name="reg_option" value="reg_tb" class="brdbtn_radio" checked /><b> 택배 실적</b></label>				
				</c:when>
			</c:choose>

		</form>
		<div class="brd_head">
	    	<div class="contitle">
	    		<img src="<%= contextPath %>/images/fpis/icon_title.gif" /> 대량실적등록 - 파일선택
		    	<a href="javascript:showCodePopup();" class="btnErrorCode"><img src="<%= contextPath %>/images/fpis2016/btn_detail_color.png" />상세코드 참고</a>
	    	</div>
		</div>
		<table width ="100%" border="0" cellspacing="0" cellpadding="0" class="contype01">
            <tr style="height: 50px;">
                <th width="40%">*등록 할 실적신고 파일을 선택하여 주십시오</th>
                <td width="60%"> 
                <form id="fileForm" method="post" enctype="multipart/form-data">
                    <input type="file" name="regFile" id="regFile" size="0" style="width:400px;" onchange="fileList(this)" multiple = "multiple" />
                </form>
                    &nbsp;&nbsp;
                </td>
            </tr>
        </table>
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="listype03_renewal">
			<colgroup>
				<col style="width:50%;">
				<col style="width:30%;">
				<col style="width:20%;">
			</colgroup>
        	<tr class="noline" id="fileList">
        		<th class="noline">파일명</th>
        		<th>검증 결과</th>
        		<th>삭제 선택</th>
       		</tr>
        	<tbody id="fileTable">
	        	<tr id="fail_tr">
				</tr>
				<tr id="noFile">
					<td colspan="3" class="noline"><br /><br /><b>선택된 파일이 없습니다.</b><br /><br /><br /></td>
				</tr>
        	</tbody>
        </table>
	<div class="brd_bottom">
		<img class="img_link" id="bottom_img" style="margin-right: 51%;" src="${pageContext.request.contextPath }/images/fpis2013/body_content/down_off.png" onmouseover="on_png(this)" onmouseout="off_png(this)" title="간소화 양식 다운로드"  onclick="fpis_downFile_reg('1')"/>
		<a id="btn_a_1" href="javascript:goFileValidation();"><img src="${pageContext.request.contextPath }/images/fpis/btn_validate.gif"  id="btn1" style="margin-bottom: 10px; margin-right: 12px;"/></a>	
		<a id="btn_a_2" href="javascript:noUploadPage();"><img src="${pageContext.request.contextPath }/images/fpis/btn_next_off.gif" style="margin-bottom: 10px;"/></a>
		<a id="btn_a_3" href="javascript:goUploadPage();" style="display: none;"><img src="${pageContext.request.contextPath }/images/fpis/btn_next.gif" style="margin-bottom: 10px;"/></a>
	</div>
	</div>

	<%-- 2022.09.20 jwchoi 양식검증 실패 시 표출테이블 --%>
	
	<div id="div_yfail" class="brd_head" style="display:none;height: auto;">
		<div class="contitle text_left">
			<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
			검증실패 양식 정보 확인
		</div>
		<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 errinfo">
			<tr class='yangErrorTR'>
				<th width="10%">상태코드</th>
				<th width="20%">원인</th>
				<th width="60%">점검요망</th>
			</tr>
		</table>
	</div>
	<%-- 2022.09.20 jwchoi 데이터검증 실패 시 표출테이블 --%>
	<div id="div_dfail" class="brd_head" style="display:none;height: auto;">
		<div class="contitle text_left">
			<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
			검증실패 입력 정보 확인
		</div>
	</div>
		<c:choose>
			<c:when test = "${usrCond eq 'U'}">
			<div id="temp" style="display:none;">
			<div id="div_dfail_detail" class="brd_head div_dfail_row">
				<div class="contitle text_left">
					<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
					상세정보 - 운송의뢰자 정보
				</div>
				<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 coninfo">
					<tr class='dataErrorTR1'>
						<th>엑셀 행</th>
						<th>사업자등록번호</th>
						<th>의뢰자구분</th>
					</tr>
				</table>
				<div class="contitle text_left">
					<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
					상세정보 - 계약정보
				</div>
				<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 opeinfo">
					<tr class='dataErrorTR2'>
						<th>계약고유번호</th>
						<th>계약년월</th>
						<th>계약금액</th>
						<th>이사화물/동일항만내 이송</th>
						<th>타운송수단 이용여부</th>
					</tr>
				</table>
				<div class="contitle text_left">
					<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
					상세정보 - 배차정보
				</div>
				<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 opeinfo">
					<tr class='dataErrorTR3'>
						<th>차량등록번호</th>
						<th>운송완료년월</th>
						<th>배차횟수</th>
						<th>운송료</th>
					</tr>
				</table>
				<div class="contitle text_left">
					<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
					상세정보 - 위탁계약정보
				</div>
				<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 truinfo">
					<tr class='dataErrorTR4'>
						<th>사업자등록번호</th>
						<th>위탁계약년월</th>
						<th>위탁계약금액</th>
						<th>화물정보망 이용여부</th>
					</tr>
				</table>
			</div>
			</div>
			</c:when>
			
			<c:when test = "${usrCond eq 'J'}">
			<div id="temp" style="display:none;">
			<div id="div_dfail_detail" class="brd_head div_dfail_row">
				<div class="contitle text_left">
						<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
						상세정보 - 운송의뢰자 정보
					</div>
					<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 coninfo">
						<tr class='dataErrorTR1'>
							<th>row</th>
							<th>사업자등록번호</th>
							<th>의뢰자구분</th>
						</tr>
					</table>
					<div class="contitle text_left">
					<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
					상세정보 - 계약정보
				</div>
				<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 opeinfo">
					<tr class='dataErrorTR2'>
						<th>계약고유번호</th>
						<th>계약년월</th>
						<th>계약금액</th>
						<th>이사화물/동일항만내 이송</th>
						<th>타운송수단 이용여부</th>
					</tr>
				</table>
					<div class="contitle text_left">
						<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
						상세정보 - 위탁계약정보
					</div>
					<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 truinfo">
						<tr class='dataErrorTR4'>
							<th>사업자등록번호</th>
							<th>위탁계약년월</th>
							<th>위탁계약금액</th>
							<th>화물정보망 이용여부</th>
						</tr>
					</table>
				</div>
				</div>
			</c:when>
			
			<c:when test = "${usrCond eq 'D'}">
			<div id="temp" style="display:none;">
			<div id="div_dfail_detail" class="brd_head div_dfail_row">
				<div class="contitle text_left">
						<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
						상세정보 - 운송의뢰자 정보
					</div>
					<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 coninfo">
						<tr class='dataErrorTR1'>
							<th>row</th>
							<th>사업자등록번호</th>
							<th>의뢰자구분</th>
						</tr>
					</table>
					<div class="contitle text_left">
					<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
					상세정보 - 계약정보
				</div>
				<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 opeinfo">
					<tr class='dataErrorTR2'>
						<th>계약고유번호</th>
						<th>대행시 실적주체 사업자번호</th>
						<th>계약년월</th>
						<th>계약금액</th>
						<th>이사화물/동일항만내 이송</th>
						<th>타운송수단 이용여부</th>
					</tr>
				</table>
					<div class="contitle text_left">
						<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
						상세정보 - 배차정보
					</div>
					<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 opeinfo">
						<tr class='dataErrorTR3'>
							<th>차량등록번호</th>
							<th>운송완료년월</th>
							<th>배차횟수</th>
							<th>운송료</th>
						</tr>
					</table>
					<div class="contitle text_left">
						<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
						상세정보 - 위탁계약정보
					</div>
					<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 truinfo">
						<tr class='dataErrorTR4'>
							<th>사업자등록번호</th>
							<th>위탁계약년월</th>
							<th>위탁계약금액</th>
							<th>화물정보망 이용여부</th>
						</tr>
					</table>
				</div>
				</div>
			</c:when>
			
			<c:when test = "${usrCond eq 'T'}">
			<div id="temp" style="display:none;">
			<div id="div_dfail_detail" class="brd_head div_dfail_row">
					<div class="contitle text_left">
					<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
					상세정보 - 계약정보
				</div>
				<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 opeinfo">
					<tr class='dataErrorTR2'>
						<th>row</th>
						<th>계약고유번호</th>
						<th>계약년월</th>
					</tr>
				</table>
					<div class="contitle text_left">
						<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
						상세정보 - 배차정보
					</div>
					<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 opeinfo">
						<tr class='dataErrorTR3'>
							<th>차량등록번호</th>
							<th>운송완료년월</th>
							<th>배차횟수</th>
							<th>운송료</th>
						</tr>
					</table>
					<div class="contitle text_left">
						<img src="${pageContext.request.contextPath }/images/fpis/left_pnt02.gif" />
						상세정보 - 위탁계약정보
					</div>
					<table width="90%" border="0" cellspacing="0" cellpadding="0" class="listype03 truinfo">
						<tr class='dataErrorTR4'>
							<th>사업자등록번호</th>
							<th>위탁계약년월</th>
							<th>위탁계약금액</th>
						</tr>
					</table>
				</div>
				</div>
			</c:when>
		</c:choose>
</div>
<div id="loadingImage" class="div_img_loading">
    <img src="${pageContext.request.contextPath }/images/fpis2013/body_content/loading.gif" id="loadingImg" title="처리중입니다." alt="처리중입니다." />
</div>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/fpis_mass_order_func.js"></script>
</body>
</html>