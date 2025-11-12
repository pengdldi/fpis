package fpis.reg.mass.service;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import fpis.reg.mass.web.MassOrderController;

/**
 * @class_desc	
 * fpis.reg.mass.service
 * MassOrderUploadVO.java
 *
 * @DATE	2022. 11. 09.
 * @AUTHOR	GnT 최정원
 * @HISTORY
 * DATE			 	AUTHOR			NOTE
 * -------------	--------		--------------------
 * 2022. 11. 09.	최정원			최초생성
 */

@Repository("MassOrderUploadVO")  
public class MassOrderUploadVO {
	
	private static final Logger	logger	= Logger.getLogger(MassOrderController.class);
	
	private String filename;
	private  String uploadcode;
	
	public MassOrderUploadVO() {}
	public MassOrderUploadVO(String filename, String uploadcode) {
		this.filename = filename;
		this.uploadcode = uploadcode;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getUploadcode() {
		return uploadcode;
	}
	public void setUploadcode(String uploadcode) {
		this.uploadcode = uploadcode;
	}

}
