package ortus.boxlang.ftp;

import org.apache.commons.net.ftp.FTPReply;

public class FTPResult {

	private FTPConnection	conn;
	private Object			returnValue;

	public FTPResult() {
		super();
	}

	public FTPResult( FTPConnection conn ) {
		super();
		this.conn = conn;
	}

	public Object getReturnValue() {
		return returnValue;
	}

	public void setReturnValue( Object returnValue ) {
		this.returnValue = returnValue;
	}

	public int getStatusCode() {
		return conn.getStatusCode();
	}

	public String getStatusText() {
		return conn.getStatusText().trim();
	}

	public Boolean isSuccessful() {
		int statusCode = getStatusCode();
		return FTPReply.isPositiveCompletion( statusCode );
	}

}
