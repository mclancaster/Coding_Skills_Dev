package application;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;

/**
 * Class: ReqEntry
 * Purpose: maintains request-specific information from each JavaMail Message object
 * @author mclancaster
 *
 */
public class ReqEntry {
	
	private String subject, fromEmail, body;
	private Date recDate;
	
	/**
	 * CONSTRUCTOR
	 * @param subject - from e-mail's subject line
	 * @param fromEmail - email address of person sending message
	 * @param body - Text-body of e-mail, to be parsed here
	 * @param date - Date e-mail was received
	 */
	public ReqEntry(String subject, String email, Date recDate, String body) {
		
		// set subject if it exists
		if (subject == null) {
			this.subject = "NO SUBJECT";
		}
		else {
			this.subject = subject;
		}

		if (body != null) {
			 //create Jsoup document from HTML
	        Document jsoupDoc = Jsoup.parse(body);
	        
	        //set pretty print to false, so \n is not removed
	        jsoupDoc.outputSettings(new OutputSettings().prettyPrint(false));
	        
	        //select all <br> tags and append \n after that
	        jsoupDoc.select("br").after("\\n");
	        
	        //select all <p> tags and prepend \n before that
	        jsoupDoc.select("p").before("\\n");
	                
	        //get the HTML from the document, and retaining original new lines
	        String str = jsoupDoc.html().replaceAll("\\\\n", "\n");
	        
	        // save modified text as message body
	        this.body = Jsoup.clean(str, "", Whitelist.none(), new OutputSettings().prettyPrint(false));
		}
		else {
			this.body = "N/A";
		}
		
		this.fromEmail = email;
		this.recDate = recDate;
	}
	
	/**
	 * Function: getSubject
	 * @return Message subject(String)
	 */
	public String getSubject() {
		return subject;
	}
	/**
	 * Function: getFromEmail
	 * @return email address of person who submit request (String)
	 */
	public String getFromEmail() {
		return fromEmail;
	}		
	
	/**
	 * Function: getBody
	 * @return body - String representation of message contents
	 */
	public String getBody() {
		return body;
	}
	
	/**
	 * Function: getRecDate
	 * @return recDate - string representation of date e-mail was received
	 */
	public String getRecDate() {
		return recDate.toString();
	}
	/**
	 * Function: toString
	 * Purpose: Primary use in ListView, to show subject
	 */
	@Override
	public String toString() {
		return subject;
	}
}
