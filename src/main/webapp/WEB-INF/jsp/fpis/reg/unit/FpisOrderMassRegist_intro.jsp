<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page language="java" import="java.util.*,fpis.common.vo.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" uri="http://egovframework.gov/ctl/ui"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page language="java" import="fpis.common.vo.SessionVO,
                                 java.io.*,
                                 java.text.*,
                                 java.util.*" %>

<!-- 
	실적 등록 :: 연도선택 페이지
	@author : ysw
	@hitsory : 2020.08.18 생성
-->

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/ TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<title>:: 화물운송관리 시스템 ::등록</title>
	<script type = "text/javascript">
		history.replaceState({}, null, location.pathname);
	</script>
	<link href="${pageContext.request.contextPath }/css/fpis/userstyle2.css" rel="stylesheet" type="text/css" ></link>	
	<link href="${pageContext.request.contextPath }/css/jquery/jquery-ui-1.8.23.custom.css" rel="stylesheet" type="text/css"></link>
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
</head>
<!--   CONTENT    -->
<body>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/g.util/gnt.utils.js"></script>
<!-- 신규 소스(연 1회 신고로 변경) -->
<script language="javaScript">
function yearValue(currentYear, year){
	//회원사 실적 대행 체크 합니다.
	if(document.getElementById("agency_yn_radio_1") && $('input[name="agency_yn_radio"]:checked').val() == 'Y'){
		//대행시 회원사 실적 대행 등록이면서 사업자 번호가 없는경우 체크 
		if($("#agency_comp_bsns_num").val()  == "") {
			simpleAlertDiv("대행 실적주체 사업자번호을 입력해 주세요..");
			$('#selectedQuarter').val('');
			return ;
		}
		
		if($("#agency_comp_bsns_num").val()  != ""){
	        if($("#agency_comp_bsns_num").val().length != 10){
	        	simpleAlertDiv("대행 실적주체 사업자번호 10자리를 모두 입력해 주십시요.");
	            $('#selectedQuarter').val('');
	            return;
	        }
	    }
		
		//회원인지 대행사 있는지부터 체크
		$.ajax({
	        type : "POST",
	        asyn : true,
	        url : "${pageContext.request.contextPath }/reg/unit/selectChkUsrAndAgencyByUsrMstKey.do" ,
	        data : {
        		usr_mst_key : $("#agency_comp_bsns_num").val(),
	        },
	        dataType: "json",
	        error: function (xhr, ajaxOptions, thrownError){
	            $('#loadingImage').hide();
	            simpleAlertDiv(xhr  + " : " + thrownError);
	        },                
	        success: function(jsonData) {
	        	if(jsonData.usr_cnt < 2){
	        		simpleAlertDiv("입력한 실적 대행 사업자 번호는 fpis에 가입되어있지 않은 사업자번호입니다.");
	        		$('#selectedQuarter').val('');
		            return;
	        	}else if(jsonData.agency_cnt < 1){
	        		//ts2020아이디는 넘어가도록...
	        		if($("#user_id").val() != 'ts2020'){
	        			simpleAlertDiv("등록되지않은 대행 사업자 번호 입니다.  정보관리 - 회원관리에서 먼저 등록해주세요.");
	        			$('#selectedQuarter').val('');
	        			return;
	        		}   
	        	}
	        	if(year != null && year !=""){
	                record_year = year;
	                $("#record_year").val(record_year);
	                var from_date_unit = record_year + "-" + "01" + "-" + "01";
	                var to_date_unit = record_year + "-" + "12" + "-" + "31";   
	                //해당연도 실적 체크
	                checkYearRecord(record_year, from_date_unit, to_date_unit,year);            
	            }else{
	                $("#from_date_unit").val("");
	                $("#to_date_unit").val("");
	                $("#currnetYear").val("");
	                $("#record_year").val("");
	                $("#record_falg").val("");
	            }
	        }
	    });
		
	}else{
		if(year != null && year !=""){
	        record_year = year;
	        $("#record_year").val(record_year);
	        var from_date_unit = record_year + "-" + "01" + "-" + "01";
	        var to_date_unit = record_year + "-" + "12" + "-" + "31";   
	        //해당연도 실적 체크
	        checkYearRecord(record_year, from_date_unit, to_date_unit,year);            
	    }else{
	        $("#from_date_unit").val("");
	        $("#to_date_unit").val("");
	        $("#currnetYear").val("");
	        $("#record_year").val("");
	        $("#record_falg").val("");
	    }
	}
}

//2018.12.13 pes 실적신고 제한 함수
function reg_limit(usr_mst_key, year){
    $('#loadingImage').show();
    $.ajax({
        type : "POST",
        asyn : true,
        url : "${pageContext.request.contextPath }/reg/unit/FpisOrderRegist_getRegLimit.do" ,
        data : {usr_mst_key : usr_mst_key,
                year : year},
        dataType: "json",
        error: function (xhr, ajaxOptions, thrownError){
            $('#loadingImage').hide();
            simpleAlertDiv(xhr  + " : " + thrownError);
        },                
        success: function(jsonData) {
            var compCls01 = jsonData.compCls01;
            var compCls02 = jsonData.compCls02;
            var compCls03 = jsonData.compCls03;
            var result = jsonData.result;
            var list = jsonData.list;
            var strCls01 = compCls01.substring(0,compCls01.length-1);
            var strArray01 = strCls01.split(',');
            var rstCls01 = "";
            
            for(var i = 0; i < strArray01.length; i++){
                strArray01[i] = strArray01[i].substring(0,2);
                rstCls01 += strArray01[i];
                if(i != strArray01.length-1) rstCls01 += '/';
            }
            $('#compCls01').text("운 송 유 형 : "+strCls01);
            $('#compCls02').text("주 선 유 형 : "+compCls02);
            $('#compCls03').text("망사업유형 : "+compCls03);
            if(compCls01.length == 0) $('#compDiv01').hide();
            if(compCls02.length == 0) $('#compDiv02').hide();
            if(compCls03.length == 0) $('#compDiv03').hide();
            $('#car_empty').hide();
            
            $('#ctrl_tb tbody tr').remove();
            
            if(result == '01'){
                for(var i = 0; i < list.length; i++){
                    $('#ctrl_tb tbody').append('<tr><td>'+list[i].from_ctrl_date+'</td><td>'+list[i].to_ctrl_date+'</td><td>'+list[i].ctrl_day+'</td><td>'+list[i].ctrl_time+'</td></tr>');
                }
            }
            else if(result == '02') result = '※ 운송사업자(일반/용달/개별)가 아닌 경우 차량을 등록할 수 없습니다.';
            else if(result == '03') result = '※ 개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.('+year+'년 기준 직영 또는 지입차량 0대)';
            else if(result == '04') result = '※ 개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.('+year+'년 기준 직영 또는 지입차량 2대 이상)';
            else if(result == '05') result = '※ 개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.('+year+'년 기준 장기용차 보유)';
            else if(result == '06') result = '※'+year+'년 기준 직영 및 지입 차량 수가 운송유형('+rstCls01+') 보유 수보다 적습니다.';
            else if(result == '07' || result == '09' || result == '10' || result == '11' || result == '17'){
                result = '※차량 필수 정보가 누락되었습니다.';
                $('#regDialog_carlimit').attr('title','차량 필수정보 누락 알림');
                $('#car_empty').show();
            }
            else if(result == '08') result = '※ 운송사업자유형('+rstCls01+') 보유 시, 직영 또는 지입차량을 최소 2대 이상 보유해야 장기용차를 보유할 수 있습니다.('+year+'년 기준 장기용차 보유)';
            else if(result == '12') result = '※ 개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.('+year+'년 기준 직영/지입차량 0대, 장기용차 보유)';
            else if(result == '13') result = '※ 개별운송사업자는 직영 또는 지입차량 1대만 보유할 수 있으며, 장기용차는 보유할 수 없습니다.('+year+'년 기준 직영/지입차량 2대 이상, 장기용차 보유)';
            else if(result == '14') result = '※ 운송사업자유형('+rstCls01+') 보유 시, 직영 또는 지입차량을 최소 3대 이상 보유해야 장기용차를 보유할 수 있습니다.('+year+'년 기준 장기용차 보유)';
            else if(result == '15') result = '※ 운송사업자유형('+rstCls01+') 보유 시, 직영 또는 지입차량을 최소 4대 이상 보유해야 장기용차를 보유할 수 있습니다.('+year+'년 기준 장기용차 보유)';
            else if(result == '16') result = '※ 운송사업자유형('+rstCls01+') 보유 시, 직영 또는 지입차량을 최소 5대 이상 보유해야 장기용차를 보유할 수 있습니다.('+year+'년 기준 장기용차 보유)';
            else if(result == 'ERR') result = '사업자유형 및 차량 보유수 검증 진행단계에 문제가 발생하였습니다. 콜센터(1577-0990)로 문의부탁드립니다.';
            
            $('#limit_detail').text(result);
            
            if(result.length != 0 & result != '99' & result != 'ONE' & result != '01'){  
                btn_disable();
                $('#regDialog_carlimit').dialog({
                    resizable: false,
                    width:600,
                    modal: true,
                    show: {
                        effect: "blind",
                        duration: 500
                    },
                    hide: {
                        effect: "clip",
                        duration: 800
                    },
                    buttons: {
                        "MY정보 수정" : function(){
                            var f = document.moveFrm;
                            f.rcode.value = "R4";
                            f.bcode.value = "R4-01";
                            f.action =  "${pageContext.request.contextPath}/uss/myi/EgovMberSelectUpdtViewUser.do";
                            f.method = "POST"; 
                            f.submit(); 
                        },
                        "차량 관리": function() {
                            var f = document.moveFrm;
                            f.rcode.value = "R8";
                            f.bcode.value = "R8-02-01";
                            f.action =  "${pageContext.request.contextPath}/online/FpisCarManagerList.do";
                            f.method = "POST"; 
                            f.submit();                        
                        },
                        "나중에 등록": function() {
                            $(this).dialog("close");
                        }
                    }
                });                    
            }else if(result == '01'){ //분산신고대상자 알림
                btn_disable();
                $('#regDialog_control').dialog({
                    resizable: false,
                    width:600,
                    modal: true,
                    show: {
                        effect: "blind",
                        duration: 500
                    },
                    hide: {
                        effect: "clip",
                        duration: 800
                    },
                    buttons: {
                        "나가기(로그아웃)": function() {
                            goURL('${pageContext.request.contextPath }/uat/uia/actionLogout.do');
                       },
                        "나중에 등록": function() {
                            $(this).dialog("close");
                        }
                    }
                });                 
            }else if(result == 'ONE'){ //1대사업자 안내
                $('#btnRecordEmpty_confirm').attr('href',"javaScript:goRecordEmpty('empty_confirm');");
                $('#btnRecordRegist').attr('href',"javaScript:goRegistOrder();");
                $('#btnRecordEmpty_cancel').attr('href',"javaScript:goRecordEmpty('empty_cancel');");
                $("#oneCarExceptionInfo").dialog({
                    resizable: false,
                    width:680,
                    modal: true,
                    show: {
                        effect: "blind",
                        duration: 500
                    },
                    hide: {
                        effect: "clip",
                        duration: 1000
                    },
                    buttons: {
                        "나가기(로그아웃)": function() {
                            goURL('${pageContext.request.contextPath }/uat/uia/actionLogout.do');
                       },
                       "MY정보로 이동" : function(){
                           var f = document.moveFrm;
                           f.rcode.value = "R4";
                           f.bcode.value = "R4-01";
                           f.action =  "${pageContext.request.contextPath}/uss/myi/EgovMberSelectUpdtViewUser.do";
                           f.method = "POST"; 
                           f.submit(); 
                       },
                        "차량등록 현황으로 이동": function() {
                            var f = document.moveFrm;
                            f.rcode.value = "R8";
                            f.bcode.value = "R8-02-01";
                            f.action =  "${pageContext.request.contextPath}/online/FpisCarManagerList.do";
                            f.method = "POST"; 
                            f.submit();
                            
                        },
                        "닫기": function() {
                            $(this).dialog("close");
                        }
                    }
                });
            }else{
                $('#btnRecordEmpty_confirm').attr('href',"javaScript:goRecordEmpty('empty_confirm');");
                $('#btnRecordRegist').attr('href',"javaScript:goRegistOrder();");
                $('#btnRecordEmpty_cancel').attr('href',"javaScript:goRecordEmpty('empty_cancel');");
            }
        }
    });
}



function checkYearRecord(record_year, from_date_unit, to_date_unit,year){
	//대행시 usr_mst_key 확인.
	if(document.getElementById("agency_yn_radio_1")){
		if($('input[name="agency_yn_radio"]:checked').val() == 'Y'){
			var usr_mst_key = $("#agency_comp_bsns_num").val();
		}else{
			var usr_mst_key = $("#usr_mst_key").val();	
		}
	}else{
		var usr_mst_key = $("#usr_mst_key").val();
	}
	

	$("#record_flag").val(""); //레코드플래그 초기화
    $("#record_year").val(record_year);
    $("#from_date_unit").val(from_date_unit);
    $("#to_date_unit").val(to_date_unit);
    
    $('#loadingImage').show();
    $.ajax({
        type : "POST",
        asyn : true,
        url : "${pageContext.request.contextPath }/reg/unit/FpisOrderRegist_getCountQuarterRecord.do" ,
        data : {
         		usr_mst_key : usr_mst_key,
                 record_year : record_year,
                 from_date_unit : from_date_unit,
                 to_date_unit : to_date_unit,
                 base_year : record_year
                },
        dataType: "json",
        error: function (xhr, ajaxOptions, thrownError){
            $('#loadingImage').hide();
            simpleAlertDiv(xhr  + " : " + thrownError);
        },                
        success: function(jsonData) {
            var record_cnt = jsonData.record_cnt;
            var record_flagCnt = jsonData.record_flagCnt;
            var reg_date = jsonData.reg_date;
            var upd_date = jsonData.upd_date;
            var record_seq = "";                
            //2018.03.13 pes 수정허가 여부 추가                
            var updPermission = jsonData.updPermission.substring(5,6);
            var cur_year = jsonData.updPermissionYear;        
            //alert(record_year+"/"+cur_year);
            if(record_year == cur_year && updPermission == 'N'){//(현재연도 -1)=실적등록연도 이면서 수정허가N일때
                $('#permission_msg').show();
                $('#permission_before_msg').hide();
                btn_disable();
            }else if(record_year < cur_year || (record_year == cur_year && jsonData.mm > 6)){//(현재연도-1)>실적등록연도 이거나 같은데 현재날짜가 7월 이상일때
                if(record_year < cur_year) $('#permission_before_msg').text('* '+cur_year+'년도 이전 실적은 변경이 불가합니다.');
                if(record_year == cur_year && jsonData.mm > 6) $('#permission_before_msg').text('* '+(cur_year+1)+'년도 이전 실적은 변경이 불가합니다.');
                $('#permission_before_msg').show();
                $('#permission_msg').hide();
                btn_disable();
            }else{//수정허가가 Y일때, (현재연도-1)<실적등록연도 일때
                $('#permission_msg').hide();
                $('#permission_before_msg').hide();
                reg_limit(usr_mst_key,year);//2018.12.13 pes 실적신고 제한 추가
            }
            $('#loadingImage').hide();
    
            
            
            
            if(record_flagCnt > 0){
                var record_flag = jsonData.record_flag;
                $("#record_flag").val(record_flag);
                $("#record_year_text_emptyConfirmValid").text($("#record_year").val());
                
                if(record_flag == "empty_confirm"){
                    $("#btnRecordEmpty_confirm").hide();
                    $("#btnRecordEmpty_cancel").show();
                    $("#record_year_text").text($("#record_year").val());
                    $("#reg_date_text").text(reg_date);
                    $("#upd_date_text").text(upd_date);
                }else if(record_flag == "empty_cancel"){
                    $("#btnRecordEmpty_confirm").show();
                    $("#btnRecordEmpty_cancel").hide();
                }
                
                record_seq = jsonData.record_seq;
                $("#record_seq").val(record_seq);
                
            }else{
                $("#record_year_text").text($("#record_year").val());
				$("#btnRecordEmpty_confirm").show();
				$("#btnRecordEmpty_cancel").hide();					
            }
            
            
            if(record_cnt == 0){
            	$("#quarterRecordedContract").val("N");
                $("#record_year_text_confirm").text($("#record_year").val());
            }else{
            	$("#quarterRecordedContract").val("Y");
                $("#record_year_text_exist").text($("#record_year").val());
                $("#record_flag").val("");
            }
            
        }
    });
};

function btn_disable(){
    $('#btnRecordEmpty_confirm').attr('href',"javascript:disable_alt('empty_confirm');");
    $('#btnRecordRegist').attr('href',"javascript:disable_alt('regist');");
    $('#btnRecordEmpty_cancel').attr('href','javascript:void(0);');
}

function disable_alt(flag){
     if(flag == 'empty_confirm') simpleAlertDiv('실적없음 등록이 불가능합니다.');
     else if(flag == 'regist') simpleAlertDiv('실적 등록이 불가능합니다.');
}

function goRegistOrder(){
	
    if($("#selectedQuarter option:selected").val() == ""){
    	simpleAlertDiv("등록할 실적신고연도를 선택하여 주십시오.");
        return;
    }
    
    $("#selectedQuarterValue").val($("#selectedQuarter option:selected").val());
    
    if($("#record_flag").val() == "empty_confirm"){
        $( "#regDialog_noRecordInfo_emptyConfirmValid" ).dialog({
            resizable: false,
            width:550,
            modal: true,
            show: {
                effect: "blind",
                duration: 500
            },
            hide: {
                effect: "clip",
                duration: 1000
            },
            buttons: {
                "확인": function() {
                    $(this).dialog("close");
                }
            }
	    });
	    return;
    }
    var f = document.frmThis;
    f.action = '${pageContext.request.contextPath}/reg/mass/FpisOrderVerify.do';
    f.submit();
    
}


function goRecordEmpty(record_flag){

	if($("#selectedQuarter option:selected").val() == ""){
		simpleAlertDiv("등록할 실적신고연도를 선택하여 주십시오.");
        return;
    }else{
        $("#selectedQuarterValue").val($("#selectedQuarter option:selected").val());
    }
	
    if(record_flag == "empty_confirm"){
        if($("#quarterRecordedContract").val() == "Y"){
            $( "#regDialog_noRecordInfo_exist" ).dialog({
                resizable: false,
                width:550,
                modal: true,
                show: {
                    effect: "blind",
                    duration: 500
                },
                hide: {
                    effect: "clip",
                    duration: 1000
                },
                buttons: {
                    "실적 조회/수정으로 이동": function() {
                        var f = document.frmThis;
                        f.action = '${pageContext.request.contextPath}/reg/unit/FpisOrderList_unit.do';
                        f.submit();
                    },

                    "취소": function() {
                    	$("#agency_yn").val('N');
                        $(this).dialog("close");
                    }
                }
            });
            return;
        }else if($("#quarterRecordedContract").val() == "N"){

            $( "#regDialog_noRecordInfo_confirm" ).dialog({
                resizable: false,
                width:550,
                modal: true,
                show: {
                    effect: "blind",
                    duration: 500
                },
                hide: {
                    effect: "clip",
                    duration: 1000
                },
                buttons: {
                    "실적없음으로 신고": function() {
                        var f = document.frmThis;
                        f.record_flag.value = record_flag;
                        f.action = "${pageContext.request.contextPath}/reg/unit/FpisOrderRegist_quarterRecordEmpty.do";
                        f.submit();
                    },

                    "취소": function() {
                    	$("#agency_yn").val('N');
                        $(this).dialog("close");
                    }
                }
        });
        return;
        }
        
    }else if(record_flag == "empty_cancel"){
        $( "#regDialog_noRecordInfo_cancel" ).dialog({
            resizable: false,
            width:550,
            modal: true,
            show: {
                effect: "blind",
                duration: 500
            },
            hide: {
                effect: "clip",
                duration: 1000
            },
            buttons: {
                "실적없음 해제": function() {
                    var f = document.frmThis;
                    f.record_flag.value = record_flag;
                    f.action = "${pageContext.request.contextPath}/reg/unit/FpisOrderRegist_quarterRecordEmpty.do";
                    f.submit();
                },

                "취소": function() {
                	$("#agency_yn").val('N');
                    $(this).dialog("close");
                }
            }
    });
    return;
    }
}


$(function() {
	//keypress 는 영어,숫자만 가능하여 keydown으로 변경함.
	/* $("#agency_comp_bsns_num").keypress(function(event){ */
	$("#agency_comp_bsns_num").keydown(function(event){
		//키 누르는순간 실적신고연도 초기화...
		$('#selectedQuarter').val('');
		$('#permission_before_msg').text('');
		if(event.which && ( (event.which  > 47 && event.which  < 58) || (event.which  > 95 && event.which  < 106) || event.which == 8 || event.which == 45 || event.ctrlKey)) {
		}else {
			if(event.which != 0)
				event.preventDefault();
		}
	});
});
</script>

<div id="contwrap">
    <div class="location"> 
        <table border="0" cellspacing="0" cellpadding="0">
        <tr>
            <td>홈</td>
            <td>실적신고</td>
            <th>대량실적 등록::연도선택</th>
        </tr>
        </table>
    </div>
    
    <div class="subtitle">
        <p>대량&nbsp;&nbsp;실적&nbsp;&nbsp;등록&nbsp;&nbsp;::&nbsp;&nbsp;연도선택</p>
    </div>
    
    
    <!-- CONTENT NOTICE -->
    <div class="contnotice">
        <table>
            <tr>
                <th><img src="${contextPath}/images/fpis2013/body_content/contents_notice.png" /></th>
                <td>
                    <br/>1. 연도선택항목에서 실적등록할 신고연도를 선택하여 주십시오.<br/><br/>
                    <span style="font-size: 1.0em;">* '17.12.15 화물운송실적신고제 시행지침 변경으로 차년 3월말까지 입력</span>        
                    <br/><br/>
                    <br/>2. 신고연도를 선택하신 후 하단에 나타난 버튼 중 <span style="color: blue; font-weight: bold;">[실적등록]</span>을 누르시면 <span style="font-weight: bold;">실적등록::계약정보</span> 화면으로이동합니다.<br/>
                    <br/>&nbsp;&nbsp;&nbsp;&nbsp;또는 선택하신 신고연도에 실적이 전혀 없는 경우에는 <span style="color: red; font-weight: bold;">[실적없음]</span>버튼을 눌러주십시오.<br/><br/>
                    <br/>3. <span style="color: red; font-weight: bold;">[실적없음]</span>으로 선택하신 연도는 더이상 실적등록을 하실 수 없습니다.<br/>
                    <br/>&nbsp;&nbsp;&nbsp;&nbsp;만약 <span style="color: red; font-weight: bold;">[실적없음]</span>으로 선택된 연도를 취소하시고 실적을 등록하고자 할 경우 <span style="font-weight: bold;">[실적없음 해제]</span>버튼을 누르신 후<br/>
                    <br/>&nbsp;&nbsp;&nbsp;&nbsp;다시 <span style="color: blue; font-weight: bold;">[실적등록]</span>을 누르시면 <span style="font-weight: bold;">실적등록::계약정보</span> 화면으로 이동이 가능합니다.<br/><br/>
                </td>
            </tr>
        </table>
    </div>
    
    
    <div class="brdwrap">
	    <div class="brd_head">
	        <div class="contitle"><img src="${pageContext.request.contextPath}/images/fpis/icon_title.gif" /> 실적등록 연도선택</div>
	    </div>
	    
	    
	    
	    <form id="frmThis" name="frmThis" method="post">
	    	<input type="hidden" id="bcode" name="bcode" value="${bcode}" />
        	<input type="hidden" id="rcode" name="rcode" value="${rcode}" />
	        <input type="hidden" id="usr_mst_key" name="usr_mst_key" value="${usr_mst_key}" />
	        <input type="hidden" id="from_date_unit" name="from_date_unit" value="" />
	        <input type="hidden" id="to_date_unit" name="to_date_unit" value="" />
	        <input type="hidden" id="quarterRecordedContract" name="quarterRecordedContract" value="" />
	        <input type="hidden" id="record_year" name="record_year" value="" />
	        <input type="hidden" id="record_quarter" name="record_quarter" value="" />
	        <input type="hidden" id="record_flag" name="record_flag" value="" />
	        <input type="hidden" id="record_seq" name="record_seq" value="" />
	        <input type="hidden" id="selectedQuarterValue" name="selectedQuarterValue"/>
	        <input type="hidden" id="currentYear" name="currentYear"/>
	        <input type="hidden" id="chk_mass_page" name="chk_mass_page" value ="Y" />
	        
	        <!-- 180313 smoh 수정허가기능 변수 추가 -->
	        <input type="hidden" id="regUpdPermissionFlag" name="regUpdPermissionFlag" value="Y"/>
	    
	    <table width="100%" border="0" cellspacing="0" cellpadding="0" class="contype01">
		    <tr >
		    <th width="330px">
		        <span style="color:red;">*</span>&nbsp;<span style="font-size: 1.2em; ">등록 할 <b>실적신고연도</b>를 선택하여 주십시오.</span>
		    </th>
		    <td>
		    &nbsp;&nbsp;
		        <select id ="selectedQuarter" style= "width:298px; height:24px; font-size: 1.15em; font-weight:bolder; background-color: #FAF4C0;" 
		            onchange="javaScript:yearValue('${currentYear}', this.value)" >
		            <!-- 171220 smoh 연 1회신고로 변경 -->
		            <option value="" >선택</option>
		            <c:forEach begin="${currentYear-1}" end="${currentYear}" varStatus="status">
		                <option value="${status.index}" >${status.index}년 실적</option>
		            </c:forEach>
		        </select>
		    </td>
		    </tr>    
	    </table>
	      
	    <div id="divRecordBtn" align="right">
	        <span id="permission_before_msg" style="display:none; float:left; margin-left:50px;"></span>
	        <span id="permission_msg" style="display:none; float:left; margin-left:50px;"><span style="color:red;">*</span>실적 수정을 하시려면 지자체 담당자의 <span style="font-weight: bold; color: red;">수정요청 승인</span>이 필요합니다. 해당 지자체로 문의바랍니다. (수정기간:4월~6월)</span>
	        <a href="javaScript:goRecordEmpty('empty_cancel');"    id="btnRecordEmpty_cancel"  style="display: none;">  
	        	<br/><br/>
	        		<span style="color:red;">*</span> 사용자께서 과거 <span id="record_year_text" style="font-weight: bold;"></span>년 실적을
	        		<span  style="font-weight: bold; color: red;">[실적없음]</span>으로 신고하셨습니다. (최초등록일 : <span id="reg_date_text"></span>)&nbsp;&nbsp; (수정일 : <span id="upd_date_text"></span>)
	        		<br/>
	        	<img src="${pageContext.request.contextPath }/images/fpis/btn_empty_cancel.gif" alt="실적없음해제" />
	        </a>
	        <a href="javaScript:goRecordEmpty('empty_confirm');" id="btnRecordEmpty_confirm"><img src="${pageContext.request.contextPath }/images/fpis/btn_empty_confirm.gif" alt="실적없음" style="margin-top:10px;"/></a>
	        <a href="javaScript:goRegistOrder();"  id="btnRecordRegist" ><img src="${pageContext.request.contextPath }/images/fpis/btn_register.gif" alt="실적등록" style="margin-top:10px;"/></a>
	    </div>
	    
	    </form>
	</div>
</div>

<!--  
[이미지 호출]           $('#loadingImage').show();
[이미지 강제로 숨기기]   $('#loadingImage').hide();   
-->
<div id="loadingImage" class="div_img_loading">
    <img src="${pageContext.request.contextPath }/images/fpis2013/body_content/loading.gif" id="loadingImg" title="처리중입니다." alt="처리중입니다." />
</div>


<c:choose>
    <c:when test="${RESULT != null and RESULT eq 0}">
        <script>
        simpleAlertDiv("정상적으로 처리되었습니다.");
        </script>
    </c:when>
    <c:when test="${RESULT != null and RESULT eq -1}">
        <script>
        simpleAlertDiv("일시적인 오류로 처리에 실패하였습니다.");
        </script>
    </c:when>
</c:choose>



<!-- 실적없음 등록안내_해당분기 실적존재 -->
<div id="regDialog_noRecordInfo_exist" title="실적없음 등록안내" style="display: none;">
    <p>
        <b>
        선택하신 <span id="record_year_text_exist" style="font-size:1.3em; font-weight: bold; color:blue;"></span> 년에 이미 등록한 실적이 존재합니다.<br/><br/>
        </b>
        <span id="regDialogMsg">
            <b>[실적신고] - [실적조회/수정]</b> 메뉴로 이동하여 해당연도의 실적을 삭제하신 이후<br/>  <span style="font-weight: bold; color:red;">[실적없음]</span> 등록이 가능합니다.<br/><br/>
            <b>[실적조회/수정]</b> 메뉴로 이동하시겠습니까?
        </span>
    </p>
</div>
   
<!-- 실적없음 등록안내_확인 -->
<div id="regDialog_noRecordInfo_confirm" title="실적없음 등록안내" style="display: none;">
    <p>
        <b>선택하신 <span id="record_year_text_confirm" style="font-size:1.3em; font-weight: bold; color:blue;"></span> 년을
            <span style="font-weight: bold; color:red;">실적없음</span>으로 신고합니다.<br/><br/>
        </b>
    <span id="regDialogMsg">
        해당분기에 실적이 발생되지 않는 경우에 한하여 <span style="font-weight: bold; color:red;">[실적없음]</span>으로 신고하실 수 있습니다.<br/>
        만약 <span style="font-weight: bold; color:red;">[실적없음]</span> 신고이후 해당연도에 실적신고를 하시고자 할 경우 해당연도의<br/>
        실적신고기간 이전에 <b>[실적없음 해제]</b>버튼을 누르신 후  <span style="font-weight: bold; color:blue;">[실적등록]</span>을 진행하여<br/>
        주시기 바랍니다. <br/><br/>
        선택하신 연도를 <span style="font-weight: bold; color:red;">[실적없음]</span>으로 신고하시겠습니까?<br/>
    </span>
    </p>
</div>
    
   
<!-- 실적없음 등록안내_실적없음 해제 -->
<div id="regDialog_noRecordInfo_cancel" title="실적없음 해제안내" style="display: none;">
<p>
    <b>
    [실적없음]을 해제합니다.<br/><br/>
    </b>
    <span id="regDialogMsg">
        선택한 연도에 신고된 <span style="font-weight: bold; color:red;">[실적없음]</span>을 해제하고자 합니다.<br/>
        <span style="font-weight: bold;">[실적없음 해제]</span> 이후에 다시 해당 분기의 <span style="font-weight: bold; color:blue;">[실적등록]</span>을 하실 수 있습니다.<br/><br/>
        계속 진행하시겠습니까?
    </span>
</p>
</div>


<!-- 실적없음 등록안내_해당분기 실적없음신고로 인하여 실적등록제한 메시지 -->
<div id="regDialog_noRecordInfo_emptyConfirmValid" title="실적등록 안내" style="display: none;">
<p>
    <b>
    선택하신 <span id="record_year_text_emptyConfirmValid" style="font-size:1.3em; font-weight: bold; color:blue;"></span> 년에
     <span style="font-weight: bold; color:red;">실적없음</span>으로 신고되어 있습니다.<br/><br/>
    </b>
    <span id="regDialogMsg">
        선택한 신고연도는 사용자께서 <span style="font-weight: bold; color:red;">[실적없음]</span>으로 신고한 상태입니다.<br/>
       해당분기의 <span style="font-weight: bold; color:blue;">[실적등록]</span>를 다시 진행하시고자 할 경우 <span style="font-weight: bold;">[실적없음 해제]</span>를 클릭하신 이후에<br/>
       해당 분기의 <span style="font-weight: bold; color:blue;">[실적등록]</span>을 하실 수 있습니다.<br/><br/>
    </span>
</p>
</div>
    
<form name="moveFrm" id="moveFrm" method="post">
    <input type="hidden" name="rcode" id="rcode" value="${rcode}"/>
    <input type="hidden" id="bcode" name="bcode" value="${bcode}" />
    <input type="hidden" name="mUrl" id="mUrl" value=""/>
</form>
<input type="hidden" id="user_id" value="${SessionVO.user_id}" />
</body>
</html>
