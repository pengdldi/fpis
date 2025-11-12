/**
 *  대량실적 등록 검증 및 결과
 *  jwchoi
 */
 var fileNum = 0;
 var filesArr = new Array();
 const fileStatus = '검증 대기';
 var SUC_CNT = 0;
 var CONFIRM_CNT;
 var FAIL_CNT;
 var WARN_CNT;
 
 //2022.08.05 jwchoi 택배실적 버튼 유무
 $(document).ready(function() {
	//var tbYN = '';
	var usrCond = '';
	if (typeof document.option_form != "undefined") {
		 //tbYN = $('#tbYN').val();
		 usrCond = $('#usrCond').val();
	}

 });
 

 /* 2022.08.08 jwchoi 첨부파일 목록*/
 function fileList(obj) {
	var maxFileCnt = 11; //첨부파일 최대 개수
	var attFileCnt = document.querySelectorAll('.filebox').length //기존
	var remainFileCnt = maxFileCnt-attFileCnt-1; //추가로 첨부가능한 개수
	var curFileCnt = obj.files.length; // 현재 선택된 첨부파일 개수
//	alert ("fileList : "+obj.value);
	//첨부파일 개수 확인
	if (curFileCnt > remainFileCnt) {
		simpleAlertDiv("파일은 최대 10개 까지 첨부 가능합니다");
	}
	for(var i=0; i<Math.min(curFileCnt, remainFileCnt); i++) {
		const file = obj.files[i];
		var overFlag = false;
		//첨부파일 검증
		if (checkFile(file, overFlag)) {
			//중복파일 검증
			var reader = new FileReader();
			if (filesArr.length != 0) {
				for (var j=0; j<filesArr.length; j++){
					if (file.name==filesArr[j].name && file.size==filesArr[j].size) {
						simpleAlertDiv("중복된 파일이 존재합니다.");
						overFlag = true;
					}					
				}	
			}
		} else {
			continue;
		}
		if (overFlag) {
			break;
		} else if (!overFlag) {	
			reader.onload=function() {
				filesArr.push(file);
			};
			$('#noFile').hide();
			reader.readAsDataURL(file);
			var fname = file.name;
			//fname = fname[0];
			let htmlTD = '';
			htmlTD += '<tr id="fileNum'+fileNum+'" class="filebox" name="'+fname+'">';
			htmlTD += '<td class="noline">'+file.name+'</td>';
			htmlTD += '<td id="fileStt'+fileNum+'">'+fileStatus+'</td>';
			htmlTD += '<td><a id="deleteNum'+fileNum+'"onclick="deleteFile('+fileNum+');"><img src="/images/fpis/btn_del.gif" alt="삭제"></a></td>';
			htmlTD += '</tr>';
			if (fileNum == 0) {
				$('#fileList').after(htmlTD);
				fileNum++;				
			} else {
				$('#fileList').next().last().after(htmlTD);
				fileNum++;
			}
			emptyFile();
		} else {
			continue;
		}			
						
	}	
	document.querySelector("input[name=regFile]").value = "";
}

/* 2022.08.08 jwchoi 첨부파일 검증 */
function checkFile(obj, _overFlag) {
	const fileTypes = ['application/vnd.ms-excel','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet','application/haansoftxlsx','image/png'];
	if (obj.name.length > 100){
		alert("파일명이 100자 이상인 파일은 제외되었습니다.");
		return false;
	} else if (obj.name.lastIndexOf('.') == -1) {
		alert("확장자가 없는 파일은 제외되었습니다.");
		return false;
	} else if (!fileTypes.includes(obj.type)) {
		alert("첨부가 불가능한 파일은 제외되었습니다.");
		return false;
	} else {
		return true;
	}
}

/* 2022.08.08 jwchoi 첨부파일 삭제 */ 
function deleteFile(num) {
	document.querySelector("#fileNum"+num).remove();
	if (document.getElementById('fail_tr'+num)) {
		document.querySelector("#fail_tr"+num).remove();
	}
	filesArr.splice(num,1);
	for(var i=1; i<fileNum-num; i++) {
		var tmp = i+num;
		var result = tmp - 1;

		document.getElementById("fileNum"+tmp).setAttribute("id","fileNum"+result);
		document.getElementById("fileStt"+tmp).setAttribute("id","fileStt"+result);
		document.getElementById("fail_tr"+tmp).setAttribute("id","fail_tr"+result);
		document.getElementById("menu2_"+tmp).setAttribute("id","menu2_"+result);
		document.getElementById("menu1_"+tmp).setAttribute("onclick","failToggleView('"+result+"');");
		document.getElementById("menu1_"+tmp).setAttribute("id","menu1_"+result);
		document.getElementById("div_dfail"+tmp).setAttribute("id","div_dfail"+result);
		document.getElementById("deleteNum"+tmp).setAttribute("onclick","deleteFile("+result+");");
		document.getElementById("deleteNum"+tmp).setAttribute("id","deleteNum"+result);
	}
	fileNum--;
	emptyFile();
	
}

function emptyFile() {
	if (fileNum == 0) {
		$('#noFile').show();
		$('#btn_a_2').show();
		$('#btn_a_3').hide()
	} else {
		for(var i=0; i<fileNum; i++) {
			if ($('#fileStt'+i).text().includes('검증 실패') ||  $('#fileStt'+i).text() == '검증 대기') {
				$('#fileStatus').val('N');
				$('#btn_a_3').hide();
				$('#btn_a_2').show();
				break;
			} else {
				$('#fileStatus').val('Y');
				$('#btn_a_2').hide();
				$('#btn_a_3').show();
			}
		}
	}
}

/* 2022.08.09 jwchoi 검증 */
function goFileValidation() {
	CONFIRM_CNT = 0;
	SUC_CNT = 0;
	FAIL_CNT = 0;
	WARN_CNT = 0;
	
	var formData = new FormData();
	var chkOp = $('input[name=reg_option]:checked').val();
	const loadingImg = '<img src="/images/fpis2016/load.gif" width="30" height="30" id="loadBar" title="처리중입니다." alt="처리중입니다." />';
	var len = filesArr.length;
	
	formData.append("chkOp", chkOp);

	if (len == 0) {
		alert('파일을 추가해주세요.');
	} else {
		$('.fail_tr_class').remove();
		if (chkOp == 'reg_tb') {
			for (var i=0; i<len; i++){
				formData.set("regFile", filesArr[i]);
				$('#fileStt'+i).html(loadingImg);
				
				$.ajax({
					type : 'POST',
					enctype: 'multipart/form-data',
					url : '/reg/mass/FpisOrderTBFileList.do',
					asyn: false,
					data : formData,
					processData : false,
					contentType : false,
					success: function(result) {
						result = JSON.parse(result);
						goResultFile(result);
					},			
					error : function(xhr, ajaxOptions, thrownError) {
						alert("error : "+xhr+"  |  "+ajaxOptions+'  |  '+thrownError);
					}
				});
			}
		} else {
			for (var i=0; i<len; i++){
				formData.set("regFile", filesArr[i]);
				$('#fileStt'+i).html(loadingImg);
				
				$.ajax({
					type : 'POST',
					enctype: 'multipart/form-data',
					url : '/reg/mass/FpisOrderFileList.do',
					asyn: false,
					data : formData,
					processData : false,
					contentType : false,
					success: function(result) {
						//alert("1111");
						result = JSON.parse(result);
						goResultFile(result);
					},			
					error : function(xhr, ajaxOptions, thrownError) {
						alert("error html : "+xhr+"//"+ajaxOptions+'//'+thrownError);
					}
				});
			}
		}

	}
}

function goResultFile(result){
	CONFIRM_CNT++;
	for (var i=0; i<filesArr.length; i++) {
		if($('#fileNum'+i).attr('name') == result.fname) {
			if (result.res == "SUC") {
				$('#fileStt'+i).html('검증 성공');
				$('#fileStt'+i).css("color", "blue");
				$('#fileStt'+i).css("font-weight", "bold");
				SUC_CNT++;

			} else if (result.res == "ERR") {
				FAIL_CNT++;
				if(result.yangError == 'Y') {
					goYangfail(result,i);
				} else if (result.dataError == 'Y') {
					goDatafail(result,i);
				}
				/*if (i == filesArr.length)
				simpleAlertDiv('[검증 실패] 파일이 존재합니다. 파일 확인 및 [실적 등록] 버튼을 눌러 재검증을 시도하시기 바랍니다.');*/
			} else if (result.res == "WARN") {
				WARN_CNT++;
				/*simpleAlertDiv('실적 검증 중 오류가 발생하였습니다. 파일 확인 및 [실적 등록] 버튼을 눌러 재검증을 시도하시기 바랍니다. 지속적인 [검증 오류]가 발생할 경우 콜센터 문의 부탁드립니다.');*/
				$('#fileStt'+i).html('검증 오류');
				$('#fileStt'+i).css("color", "red");
				$('#fileStt'+i).css("font-weight", "bold");
			}
		}
	}
	
	if (CONFIRM_CNT == filesArr.length) {
		if (SUC_CNT == filesArr.length) {
			$('#fileStatus').val('Y');
			$('#btn_a_2').hide();
			$('#btn_a_3').show();
			simpleAlertDiv('모든 파일의 검증이 완료됐습니다. [다음] 버튼을 누르신 후 진행하셔야 실적등록이 완료됩니다.');
		}
	}

}

function goYangfail(result,i) {
	let idNum = i;
	

	var ftmp = $('#fileNum'+i).attr('name');
	//양식검증 실패 메세지 불러오기
	//1. 양식검증 실패 수 가져오기
	var errCnt = result.codeCnt;
	//2. 실패 수 만큼 tr 및 메세지 추가
	for (var j = 0; j < errCnt; j++) {
		if (result.codeList[j].fname_c == ftmp) {		
			let failDetail = '';
			failDetail += '<tr>';
			failDetail += '<td>'+result.codeList[j].eCode+'</td>';
			failDetail += '<td>'+result.codeList[j].eMsg+'</td>';
			failDetail += '<td>'+result.codeList[j].eDetail+'</td>';
			failDetail += '</tr>';
			$('#div_yfail .yangErrorTR').after(failDetail);
		}
	}
	
	var _fileNum = $('#fileNum'+i).children().eq(1)
	
	let failStt = '';
	failStt += '<span style="color:red;"><b>검증 실패</b></span><br>';
	failStt += '<div class="list_mini_btn" style="width: 90px;margin: auto;display: inline-block;margin-top: 5px;margin-right: 5px;" onclick="failExcelDown(\'' + result.excel_name + '\');">엑셀다운</div>';
	failStt += '<div class="list_mini_btn"';
	failStt += 'id="menu1_'+i+'"';
	failStt += ' style="width: 90px;margin: auto;display: inline-block;margin-left: 5px;" onclick="failToggleView(\'' + i + '\');">상세보기</div>';
	
	const fail_tr = document.createElement("tr");
	fail_tr.setAttribute("id", "fail_tr"+i);
	fail_tr.setAttribute("class", "fail_tr_class");
	
	const fail_td = document.createElement("td");
	fail_td.setAttribute("id", "menu2_"+i);
	fail_td.setAttribute("colspan", 3);
	fail_td.setAttribute("style", "display:none;");
	fail_td.setAttribute("class", "noline");
	
	//양식검증 실패 테이블 노드 복사
	const copy_div = document.getElementById("div_yfail");
	
	const newNode = copy_div.cloneNode(true);
	//복사된 노드 id 변경
	newNode.id = 'div_yfail'+idNum;
	
	_fileNum.html(failStt);
	$('#fileNum'+i).after(fail_tr);
	$('#fail_tr'+i).prepend(fail_td);
	$('#menu2_'+i).prepend(newNode);
	
	//양식검증 실패 메세지 전부 삭제
	$('#div_yfail .yangErrorTR').nextAll().remove();
	
	if (CONFIRM_CNT == filesArr.length) {
		if (FAIL_CNT > 0) {
			simpleAlertDiv('[검증 실패] 파일이 존재합니다. 파일 확인 및 [실적 등록] 버튼을 눌러 재검증을 시도하시기 바랍니다.');
		} else if (WARN_CNT > 0) {
			simpleAlertDiv('실적 검증 중 오류가 발생하여 [검증 오류]파일이 존재합니다. 파일 확인 및 [실적 등록] 버튼을 눌러 재검증을 시도하시기 바랍니다. 지속적인 [검증 오류]가 발생할 경우 콜센터 문의 부탁드립니다.');
		}
	}
}

function goDatafail(result,i) {
	let idNum = i;
	
	//빈값일 경우 입력없음 표시
	
	var _fileNum = $('#fileNum'+i).children().eq(1)
	
	let failStt = '';
	failStt += '<span style="color:red;"><b>검증 실패</b></span><br>';
	failStt += '<div class="list_mini_btn" style="width: 90px;margin: auto;display: inline-block;margin-top: 5px;margin-right: 5px;" onclick="failExcelDown(\'' + result.excel_name + '\');">엑셀다운</div>';
	failStt += '<div class="list_mini_btn"';
	failStt += 'id="menu1_'+i+'"';
	failStt += ' style="width: 90px;margin: auto;display: inline-block;margin-left: 5px;" onclick="failToggleView(\'' + i + '\');">상세보기</div>';
	
	const fail_tr = document.createElement("tr");
	fail_tr.setAttribute("id", "fail_tr"+i);
	fail_tr.setAttribute("class", "fail_tr_class");
	
	const fail_td = document.createElement("td");
	fail_td.setAttribute("id", "menu2_"+i);
	fail_td.setAttribute("colspan", 3);
	fail_td.setAttribute("style", "display:none;");
	fail_td.setAttribute("class", "noline");
	
	//데이터 검증 실패 div(div_dfail) 노드 복사
	const copy_div = document.getElementById("div_dfail");
	
	const newNode = copy_div.cloneNode(true);
	//복사된 노드 id 변경
	newNode.id = 'div_dfail'+idNum;
	
	_fileNum.html(failStt);
	$('#fileNum'+i).after(fail_tr);
	$('#fail_tr'+i).prepend(fail_td);
	$('#menu2_'+i).prepend(newNode);
	
	
	/* 데이터 검증 실패 메세지 불러오기 */
	//1. 데이터 검증 실패코드 개수 가져오기
	var codeCnt = result.codeCnt;

	
	let ecodeTable = '<table width="90%" border="0" cellspacing="0" id="ecode" cellpadding="0" class="listype03 errinfo">';
	ecodeTable += '	<tr>';
	ecodeTable += '		<th width="10%">상태코드</th>';
	ecodeTable += '		<th width="20%">원인</th>';
	ecodeTable += '		<th width="60%">점검요망</th>';
	ecodeTable += '	</tr>';
	ecodeTable += '</table>';

	
	var ftmp = $('#fileNum'+i).attr('name');

	for (var j = 0; j < codeCnt; j++) {
		//2.파일이름과 일치하는 칸 하단에 위치
		if (result.codeList[j].fname_c == ftmp) {
		//3. div_dfail 하위 테이블 복사
		//4. 실패코드 개수 만큼 표출
			$('#div_dfail'+idNum).children().eq(j).after(ecodeTable);
			let ecodeDetail = '';
			ecodeDetail += '<tr>';
			ecodeDetail += '<td>'+result.codeList[j].eCode+'</td>';
			ecodeDetail += '<td>'+result.codeList[j].eMsg+'</td>';
			ecodeDetail += '<td>'+result.codeList[j].eDetail+'</td>';
			ecodeDetail += '</tr>';
			$('#ecode').append(ecodeDetail);
			$('#ecode').attr("id", "ecode"+i+j); //"e_"+ftmp+j
		}	
	}
	
	var usrCond = $('#usrCond').val();
	/* 데이터 검증 실패 엑셀 데이터 불러오기 */
	var errCnt = result.errCnt;
	var seq = 0;
	for (var j = 0; j < codeCnt; j++) {
		var tmp = result.codeList[j].eCode;
		for (var k = errCnt-1; k >= 0; k--) {
			if (tmp == result.errDataList[k].eCode) {
				const newDetailNode = $('#temp').clone();
	        	$('#ecode'+i+j).after(newDetailNode);
				newDetailNode.attr("id", "row_"+seq);
				
	        	if (usrCond == 'U') {
					let detailString1 = '';
					let detailString2 = '';
					let detailString3 = '';
					let detailString4 = '';
					detailString1 += '<tr class="cliInfo">';
					detailString1 += '<td>'+result.errDataList[k].row+'</td>';
					detailString1 += '<td>'+result.errDataList[k].data0+'</td>';
					detailString1 += '<td>'+result.errDataList[k].data1+'</td>';
					detailString1 += '</tr>';
					
					detailString2 += '<tr class="conInfo">';
					detailString2 += '<td>'+result.errDataList[k].data2+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data3+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data4+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data5+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data6+'</td>';
					detailString2 += '</tr>'; 
					
					detailString3 += '<tr class="opeInfo">';
					detailString3 += '<td>'+result.errDataList[k].data7+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data8+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data9+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data10+'</td>';
					detailString3 += '</tr>';
					
					detailString4 += '<tr class="truInfo">';
					detailString4 += '<td>'+result.errDataList[k].data11+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data12+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data13+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data14+'</td>';
					detailString4 += '</tr>';
					
					$('#row_'+seq).find('.dataErrorTR1').after(detailString1);
					$('#row_'+seq).find('.dataErrorTR2').after(detailString2);
					$('#row_'+seq).find('.dataErrorTR3').after(detailString3);
					$('#row_'+seq).find('.dataErrorTR4').after(detailString4);
					$('#row_'+seq).attr("id", "r_"+i+"_"+seq);
					var row_seq = "r_"+i+"_"+seq;
					//노란색 클래스 넣는 함수를 여기에 넣기, 여기에 row_seq 넘길 것
					errDataToYellow(row_seq, 15);
				} else if (usrCond == 'J') {
					let detailString1 = '';
					let detailString2 = '';
					let detailString4 = '';
					detailString1 += '<tr>';
					detailString1 += '<td>'+result.errDataList[k].row+'</td>';
					detailString1 += '<td>'+result.errDataList[k].data0+'</td>';
					detailString1 += '<td>'+result.errDataList[k].data1+'</td>';
					detailString1 += '</tr>';
					
					detailString2 += '<tr>';
					detailString2 += '<td>'+result.errDataList[k].data2+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data3+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data4+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data5+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data6+'</td>';
					detailString2 += '</tr>'; 
						
					detailString4 += '<tr>';
					detailString4 += '<td>'+result.errDataList[k].data7+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data8+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data9+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data10+'</td>';
					detailString4 += '</tr>';
					
					$('#row_'+seq).find('.dataErrorTR1').after(detailString1);
					$('#row_'+seq).find('.dataErrorTR2').after(detailString2);
					$('#row_'+seq).find('.dataErrorTR4').after(detailString4);
					$('#row_'+seq).attr("id", "r_"+i+"_"+seq);
					var row_seq = "r_"+i+"_"+seq;
					errDataToYellow(row_seq, 11);
				} else if (usrCond == 'D') {
					var detailString1 = '';
					var detailString2 = '';
					var detailString3 = '';
					var detailString4 = '';
					detailString1 += '<tr>';
					detailString1 += '<td>'+result.errDataList[k].row+'</td>';
					detailString1 += '<td>'+result.errDataList[k].data0+'</td>';
					detailString1 += '<td>'+result.errDataList[k].data1+'</td>';
					detailString1 += '</tr>';
					
					detailString2 += '<tr>';
					detailString2 += '<td>'+result.errDataList[k].data2+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data3+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data4+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data5+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data6+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data7+'</td>';
					detailString2 += '</tr>'; 
					
					detailString3 += '<tr>';
					detailString3 += '<td>'+result.errDataList[k].data8+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data9+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data10+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data11+'</td>';
					detailString3 += '</tr>';
					
					detailString4 += '<tr>';
					detailString4 += '<td>'+result.errDataList[k].data12+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data13+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data14+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data15+'</td>';
					detailString4 += '</tr>';

					$('#row_'+seq).find('.dataErrorTR1').after(detailString1);
					$('#row_'+seq).find('.dataErrorTR2').after(detailString2);
					$('#row_'+seq).find('.dataErrorTR3').after(detailString3);
					$('#row_'+seq).find('.dataErrorTR4').after(detailString4);	
					$('#row_'+seq).attr("id", "r_"+i+"_"+seq);
					var row_seq = "r_"+i+"_"+seq;
					errDataToYellow(row_seq, 16);
				} else if (usrCond == 'T') {
					let detailString2 = '';
					let detailString3 = '';
					let detailString4 = '';
					
					detailString2 += '<tr>';
					detailString2 += '<td>'+result.errDataList[k].row+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data0+'</td>';
					detailString2 += '<td>'+result.errDataList[k].data1+'</td>';
					detailString2 += '</tr>'; 
					
					detailString3 += '<tr>';
					detailString3 += '<td>'+result.errDataList[k].data2+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data3+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data4+'</td>';
					detailString3 += '<td>'+result.errDataList[k].data5+'</td>';
					detailString3 += '</tr>';
					
					detailString4 += '<tr>';
					detailString4 += '<td>'+result.errDataList[k].data6+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data7+'</td>';
					detailString4 += '<td>'+result.errDataList[k].data8+'</td>';
					detailString4 += '</tr>';
					$('#row_'+seq).find('.dataErrorTR2').after(detailString2);
					$('#row_'+seq).find('.dataErrorTR3').after(detailString3);
					$('#row_'+seq).find('.dataErrorTR4').after(detailString4);
					$('#row_'+seq).attr("id", "r_"+i+"_"+seq);
					var row_seq = "r_"+i+"_"+seq;
					errDataToYellow(row_seq, 9);
				}
	        	let contentString = '';
				contentString += '<div class="OnOffBar">';
				contentString += '    <table width="100%" border="0" cellpadding="0" cellspacing="0">';
	        	contentString += '        <tr>';
	        	contentString += '            <td style="height: 30px; width: 70%"  onclick="contToggleView(\'r_'+i+'_'+seq+'\');">&nbsp;';
	        	contentString += '            <img src="/images/fpis/arrow.png" width="15" height="10"/><label class="f_white b f_14px">'+result.errDataList[k].row+' 행</label></td>';
	        	contentString += '            <div id="status_open" style="display:none;float: right;margin-right: 15px;font-size: 16px;color: white;">+ 열기</div>';
	        	contentString += '        </tr">';
	        	contentString += '    </table>';
	        	contentString += '</div>';    	
	        	$('#ecode'+i+j).after(contentString);
	        	seq++;
			}
		}
	}
	
	if (CONFIRM_CNT == filesArr.length) {
		if (FAIL_CNT > 0) {
			simpleAlertDiv('[검증 실패] 파일이 존재합니다. 파일 확인 및 [실적 등록] 버튼을 눌러 재검증을 시도하시기 바랍니다.');
		} else if (WARN_CNT > 0) {
			simpleAlertDiv('실적 검증 중 오류가 발생하여 [검증 오류]파일이 존재합니다. 파일 확인 및 [실적 등록] 버튼을 눌러 재검증을 시도하시기 바랍니다. 지속적인 [검증 오류]가 발생할 경우 콜센터 문의 부탁드립니다.');
		}
	}
}

function errDataToYellow(rowseqID, size) {
	for(var i=1; i<=size; i++) {
		var value = $('#'+rowseqID).find("td:eq("+i+")").text();
		if (value.includes("##FAIL##")) {
			$('#'+rowseqID).find("td:eq("+i+")").attr("class","td_yellow");
			value = value.replace(/##FAIL##/gi,"");
			$('#'+rowseqID).find("td:eq("+i+")").html(value);
		}
	}
}

function contToggleView (detailId) {
	if(document.getElementById(detailId).style.display == 'none'){
		document.getElementById(detailId).style.height = '430px';
		document.getElementById(detailId).style.margin = '15px 20px 15px 20px';
            $('#'+detailId).show(600);
        }else{
            $('#'+detailId).hide(600);
        }
}

function failToggleView(num) {

	if(document.getElementById('menu2_'+num).style.display == 'none'){
		document.getElementById('menu2_'+num).style.display = '';
		$('#div_yfail'+num).show(500);
		$('#div_dfail'+num).show(500);
    }else{
        document.getElementById('menu2_'+num).style.display = 'none';
        $('#div_yfail'+num).hide(800);
        $('#div_dfail'+num).hide(800);
    }
}

function failExcelDown(excel_name) {
	var elem = {
		title: "알림",
		content: "엑셀 출력은 많은시간이 소요될 수 있습니다. 계속 하시겠습니까?",
		yFunc: "yFuncExceldown",
		param : {
			excel_name : excel_name,
		}
	};
	setTimeout(function(){ confirmDiv(elem); }, 500);
	
}

function yFuncExceldown(param) {
	$("#loadingImage").show();
	var f = document.failfile_form;
    f.fileCls.value = "6";
    f.fileName.value = param.excel_name+".xls";
    f.action = "/cmm/fms/FpisFileDown_sw.do";
    f.submit();
    $("#loadingImage").hide();
}

function goUploadPage() {

	let fileNameList = '';
	fileNameList += '<input type="hidden" name="fileCnt" value="'+filesArr.length+'"/>';
	for (var j=0; j<filesArr.length; j++) {
		fileNameList += '<input type="hidden" name="file_'+j+'" value="'+filesArr[j].name+'"/>';
		$('#option_form').prepend(fileNameList);
	}
	
	var f = document.option_form;
     //f.bcode.value = bcode;
	f.action = "/reg/mass/MassOrderUploadPage.do";
	f.submit();	
}


function noUploadPage() {
	simpleAlertDiv("모든 파일의 검증결과가 [검증 성공] 이어야 진행이 가능합니다. 실적 검증을 완료해주시기 바랍니다.");
}

function showCodePopup() {
	//window.name = "errCodePopup";
	var path = getContextPath();
	window.open(path + "/mass/ErrCodePopup.do", "pop", "width=600,height=420, scrollbars=yes, resizable=yes");
}
