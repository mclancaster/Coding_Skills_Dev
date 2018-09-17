package views;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

import application.ReqEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Class: MainController Purpose: Controls functionality of mainC.fxml, and
 * processing of mail-server processes
 * 
 * @author mclancaster
 *
 */
public class MainController {

	// FX elements
	private Stage stageLog;
	private ObservableList<ReqEntry> messList;

	// Mail elements
	private Store st;
	private PasswordAuthentication pwAuth;
	private LoginController lc;
	private Folder emailFolder;
	private Session emailSession;

	// standard elements
	private boolean goodLogin;
	private int messIndex, totalMess = 0;
	private String hostName, mailTag;
	private ReqEntry sel;

	// FXML Elements
	private FXMLLoader paneLoad;
	@FXML
	private ListView<ReqEntry> eMailList;
	@FXML
	private TextField textMailTag;
	@FXML
	private Label numMess;
	@FXML
	private TextArea textMailInfo;
	@FXML
	private TextArea textMessCont;
	@FXML
	private TextArea textReply;
	@FXML
	private Label lblRep_to;
	@FXML
	private TextField textRep_to;
	@FXML
	private Label lblRep_sub;
	@FXML
	private TextField textRep_sub;
	@FXML
	private Button btnRep_send;
	@FXML
	private Button btnReply;
	@FXML
	private Button btnNewMail;
	@FXML
	private Button btnCancel;
	@FXML
	private ProgressBar progBar;

	/**
	 * CONSTRUCTOR
	 */
	public MainController() {
		stageLog = new Stage();
		lc = null;
		goodLogin = false;

	}

	// FXML METHODS

	@FXML
	/**
	 * Function: loadLogin Purpose: display login screen for user sign-in
	 * 
	 * @author mclancaster
	 * @throws IOException
	 */
	private void loadLogin() throws IOException {

		// if login stage is NOT showing...
		if (!stageLog.isShowing()) {

			// load FXML sheet into new pane
			paneLoad = new FXMLLoader();
			Pane logPane = paneLoad.load(getClass().getResource("LoginConsole.fxml").openStream());
			lc = paneLoad.getController();

			stageLog = new Stage(); // <-- create new stage
			stageLog.setScene(new Scene(logPane)); // <-- set scene with history console
			stageLog.setTitle("New Session");
			stageLog.show();

			// Event handler when pane is closed...
			stageLog.setOnHidden((WindowEvent e) -> {

				if (lc.getBtnPush()) {
					// get user/pw information
					pwAuth = lc.getPwAuth();
					hostName = lc.getHostName();

					// attempt to login if user entered a PW
					if (pwAuth != null) {

						goodLogin = attemptConnection(); // <-- attempt to connect to mail server for email/pw

						// if login successful...
						if (goodLogin) {

							// Notify user of active connection
							Alert goodLog = new Alert(AlertType.INFORMATION);
							goodLog.setTitle("New Login");
							goodLog.setHeaderText("Login Successful!");
							goodLog.setContentText(
									"Please enter a desired mailbox below, and press \"Refresh\" to load information");
							goodLog.showAndWait();

							btnReply.setDisable(false);
							btnNewMail.setDisable(false);
							// set title so user can see they're logged in under an account
							Stage s = (Stage) eMailList.getScene().getWindow();
							s.setTitle("MailClient: Logged in as - " + pwAuth.getUserName());
						}
						// if login FAILED...
						else {

							// re-load login screen (error thrown in LoginController
							try {
								lc = null;
								loadLogin();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
			});

		}
		// if Login pane IS visible, bring to front
		else {
			stageLog.toFront();
		}
	}

	@FXML
	/**
	 * Function: logOutUser Purpose: Clears all personal data from variables, and
	 * reset GUI
	 * 
	 * @throws MessagingException
	 * @throws IOException
	 */
	public void logOutUser() throws MessagingException, IOException {

		// only reset if the user was actually logged in
		if (goodLogin) {
			goodLogin = false;

			lc = null;
			if (st.isConnected())
				st.close();
			pwAuth = null;
			if (emailFolder.isOpen())
				emailFolder.close();
			emailSession = null;
			messList.clear();
			sel = null;
			textMailTag.setText("");
			textMailInfo.setText("");
			textMessCont.setText("");
			Stage s = (Stage) eMailList.getScene().getWindow();
			s.setTitle("MailClient: NOT LOGGED IN");
			numMess.setText("()");
			messCancel();
			btnReply.setDisable(true);
			btnNewMail.setDisable(true);
			loadLogin();
		}
	}

	@FXML
	/**
	 * Function: refresh Purpose: connects with mail-server, checking for new
	 * e-mails that need to be 'fetched'
	 * 
	 * @author mclancaster
	 */
	private void refresh() throws MessagingException {

		if (goodLogin) {
			mailTag = "";

			// if user left mailTag blank, default to inbox
			if (textMailTag.getText().trim().length() == 0) {
				mailTag = "inbox";
			} else {
				mailTag = textMailTag.getText().trim(); // save mail-tag
			}

			// attempt to open desired folder
			try {
				emailFolder = st.getFolder(mailTag); // <-- get folder by "tag"

				emailFolder.open(Folder.READ_WRITE); // <-- open folder for reading/writing

				// set message count, and where to start in reading e-mails (NOTE: newest
				// e-mails are last)
				totalMess = emailFolder.getMessageCount() - 1; // <-- JavaMail Folder-indexing starts @ 1
				messIndex = totalMess - 20;

				listMessages(); // <-- display messages in ListView

			} catch (MessagingException e) {

				// alert user that the folder they wish to open could not be found
				Alert badFold = new Alert(AlertType.INFORMATION);
				badFold.setTitle("I can't find it :(");
				badFold.setHeaderText("Unable to locate \"" + mailTag + "\"");
				badFold.setContentText("Please re-enter a mailbox/mailTag, and press refresh");
				badFold.showAndWait();
				e.printStackTrace();
			}
		}
	}

	@FXML
	/**
	 * Function: listMessages Purpose: read-in messages from folder, and display in
	 * ListView (currently 20 at a time) Listings are shown from NEWEST(top) to
	 * OLDEST(bottom)
	 * 
	 * @author mclancaster
	 * @throws MessagingException
	 */
	private void listMessages() throws MessagingException {
		int viewSize = 20;
		messList = FXCollections.observableArrayList(); // <-- create Observable List to hold message objects

		// if user attempts to load messages when no folder exists...
		if (emailFolder == null) {
			System.out.println("NULL FOLDER");
			return; // <-- skip function as nothing can be loaded
		}
		if (messIndex == 1) {
			return;
		}
		numMess.setText(
				"(" + (totalMess - (messIndex + 19)) + "-" + (totalMess - messIndex) + " of " + totalMess + ")"); // <--
																													// update
																													// message
																													// range,
																													// and
																													// total
																													// messages

		// if there were less than 20 messages, update accordingly
		if (totalMess < 20) {
			messIndex = 1;
		}

		// Separate Task to read-in Messages (Groups of 20 - to prevent Thread freezing)
		Task<Void> loadMessages = new Task<Void>() {

			@Override
			protected Void call() throws Exception {

				int i;
				// for each message in the folder...
				for (i = messIndex; i <= totalMess; i++) {
					String body = "";

					// get the body from the message
					try {
						body = getBodyText(emailFolder.getMessage(i));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// create message object
					ReqEntry re = new ReqEntry(emailFolder.getMessage(i).getSubject(),
							emailFolder.getMessage(i).getFrom()[0].toString(),
							emailFolder.getMessage(i).getReceivedDate(), body);

					messList.add(re); // <-- add object to Observable List
					// if we've read 20 messages, save position and stop indexing
					if (i == (messIndex + 20)) {
						messIndex -= viewSize;
						break; // <-- leave loop
					}
				}
				return null;
			}

		};

		// Bind a progress bar to Task, to notify user that program is "thinking"
		progBar.progressProperty().bind(loadMessages.progressProperty());
		new Thread(loadMessages).start(); // <-- start Task

		// When the task is complete...
		loadMessages.setOnSucceeded(e -> {

			// un-bind and reset Progress bar
			progBar.progressProperty().unbind();
			progBar.setProgress(0);
			Collections.reverse(messList); // <-- reverse current listing for proper order
			eMailList.setItems(messList); // <-- set items in ListView
		});
	}

	@FXML
	/**
	 * Function: listPrevPage Purpose: Used for moving between pages of entries in
	 * ListView; view the 20 entries prior to the current ones
	 * 
	 * @author mclancaster
	 */
	private void listPrevPage() {

		// if user has moved forward at least one page...
		if (messIndex < totalMess - 20) {
			messIndex += 40; // <-- move back one page (20 entries to get to beginning of current list, 20 to
								// move to 20 prior)

			// call function to list messages with updates start index
			try {
				listMessages();
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	/**
	 * Function: closeConsole Purpose: exits application, logging user off if
	 * necessary
	 * 
	 * @author mclancaster
	 */
	private void closeConsole() throws MessagingException, IOException {
		logOutUser();
		System.exit(0);
	}

	@FXML
	/**
	 * Function: displaySelEmail Purpose: Display dissected information regarding
	 * currently selected email in ListView
	 * 
	 * @author mclancaster
	 */
	private void displaySelEmail() {

		sel = eMailList.getSelectionModel().getSelectedItem(); // <-- get Request object currently selected

		if (sel == null) {
			return;
		}

		textMailInfo.setText("Sender: " + sel.getFromEmail() + "\n" + "Date: " + sel.getRecDate() + "\n" + "Subject: "
				+ sel.getSubject());
		textMessCont.setText(sel.getBody());

	}

	// NON-FXML METHODS

	/**
	 * Function: setRepMess Purpose: Triggered when user presses 'reply' button,
	 * pre-loads 'to' and 'subject' fields before showing all reply options
	 */
	public void setRepMess() {
		// if client is replying to an e-mail, pre-load information
		if (sel != null) {
			textRep_to.setText(sel.getFromEmail());
			textRep_sub.setText("RE: " + sel.getSubject());
		}
		messReply(); // <-- show reply fields
	}

	/**
	 * Function: messReply Purpose: shows all fields related to sending an e-mail
	 * (Reply text, To, Subject, and Send button)
	 */
	public void messReply() {
		boolean setVal = true;

		// disable the 'reply' and 'new' buttons, as we're currently working on a
		// message
		btnReply.setDisable(setVal);
		btnNewMail.setDisable(setVal);

		// show text-fields / labels for user to enter information
		textReply.setVisible(setVal);
		lblRep_to.setVisible(setVal);
		textRep_to.setVisible(setVal);
		lblRep_sub.setVisible(setVal);
		textRep_sub.setVisible(setVal);
		btnRep_send.setVisible(setVal);

		// enable 'cancel' button
		btnCancel.setDisable(false);
	}

	/**
	 * Function: messCancel Purpose: Triggered when user presses 'cancel' button,
	 * removes any text in reply-related fields, hides reply information, and
	 * enables 'reply' and 'new' buttons
	 */
	public void messCancel() {
		boolean setVal = false;

		// enable buttons
		btnReply.setDisable(setVal);
		btnNewMail.setDisable(setVal);

		// remove any written text, and hide all fields
		textReply.setText("");
		textReply.setVisible(setVal);
		lblRep_to.setVisible(setVal);
		textRep_to.setText("");
		textRep_to.setVisible(setVal);
		lblRep_sub.setVisible(setVal);
		textRep_sub.setText("");
		textRep_sub.setVisible(setVal);
		btnRep_send.setVisible(setVal);

		btnCancel.setDisable(true); // <-- disable cancel button
	}

	/**
	 * Function: prepReply Purpose: Triggered when user presses 'send'. Verifies
	 * user has entered minimum information to send an email, and prompts
	 * confirmation before attempting to send Minimum e-mail: At least 1 character
	 * in the reply-text, and an e-mail address that is 3+ characters long, with an
	 * '@' symbol
	 */
	public void prepReply() {

		Alert conf;

		String toAdd = textRep_to.getText(); // <-- get text for reply-body

		// if user has entered minimum required info, prompt for confirmation of e-mail
		// sending
		if ((toAdd.contains("@") && toAdd.length() >= 3) && textReply.getText().length() > 0) { // <-- EMail addresses
																								// must be 3 or more
																								// characters, with a
																								// '@' between local
																								// part and domain part
			conf = new Alert(AlertType.CONFIRMATION);
			conf.setTitle("Send E-Mail");
			conf.setHeaderText("Are you sure?");
			conf.setContentText("Are you certain you want to send this message to " + toAdd + "?");
			Optional<ButtonType> result = conf.showAndWait();

			// if user presses 'OK', send the email
			if (result.get() == ButtonType.OK) {
				sendEmail();
			}
		}
		// if minimum info needed is NOT present, alert user
		else {
			conf = new Alert(AlertType.INFORMATION);
			conf.setTitle("Something went wrong...");
			conf.setHeaderText("Unable to send E-mMil");
			conf.setContentText(
					"E-Mail address must contain 3+ characters, including a '@' symbol, and reply section cannot be empty. Please try again");
			conf.showAndWait();
		}

	}

	/**
	 * Function: sendEmail Purpose: Creates new Message object with user-provided
	 * information, and attempts to send through smtp mail server
	 */
	public void sendEmail() {
		String mailTo = textRep_to.getText(), mailFrom = pwAuth.getUserName(), sub = textRep_sub.getText(),
				body = textReply.getText();

		// Set properties for send-session
		Properties props = new Properties();
		props.put("mail.smtp.host", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");

		// Establishing a session with required user details
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return pwAuth;
			}
		});

		// Attempt to send message
		try {
			Message mess = new MimeMessage(session); // <-- make new Message with current session

			// Set From, To, Subject, and Message Body
			mess.setFrom(new InternetAddress(mailFrom));
			mess.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo));
			mess.setSubject(sub);
			mess.setText(body);

			Transport.send(mess); // <-- send the E-mail

			// If we've reached here, Message was properly sent. Notify user
			Alert conf = new Alert(AlertType.INFORMATION);
			conf.setTitle("Send E-Mail");
			conf.setHeaderText("Away it goes!");
			conf.setContentText("Your message to " + mailFrom + " was sent successfully!");
			conf.showAndWait();

			messCancel(); // <-- erase contents and hide, as it's no longer needed

			// Notify user if E-Mail was unable to be send
		} catch (MessagingException e) {
			Alert conf = new Alert(AlertType.INFORMATION);
			conf.setTitle("Send E-Mail");
			conf.setHeaderText("Something went wrong :'(");
			conf.setContentText("Your message to " + mailFrom
					+ " was NOT able to be sent. Please verify proper information and try again, or contact your administrator");
			conf.showAndWait();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Function: getBodyText Purpose: Return the primary text content of the message
	 * (Recursive).
	 * 
	 * @author From JSoup website - provided code
	 * @throws IOException
	 */
	private String getBodyText(Part p) throws MessagingException, IOException {

		// determine MimeType for Part
		if (p.isMimeType("text/x-watch-html")) {
			return "";
		}
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			return s;
		}

		// if MultiPart, recurse based on type
		else if (p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount() / 2; i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getBodyText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					Document d = Jsoup.parse(getBodyText(bp));
					d.outputSettings(new Document.OutputSettings().prettyPrint(false));
					d.select("br").append("\\n");
					d.select("p").prepend("\\n\\n");
					String s = d.html().replaceAll("\\\\n", "\n");
					s = Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
					if (s != null && s.trim().length() > 0)
						return s;
				} else {
					return getBodyText(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount() / 2; i++) {
				String s = getBodyText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}

		return null;
	}

	/**
	 * Function: attemptConnection Purpose: attempts to connect to host, given a
	 * UserName, PW, and HostName
	 * 
	 * @author mclancaster
	 * @return Boolean: True (successful) or False(failed to connect)
	 */
	private boolean attemptConnection() {

		try {
			// Create Properties for connections
			Properties props = new Properties();
			props.put("mail.store.protocol", "imaps");
			props.put("mail.imaps.host", hostName);
			props.put("mail.imaps.port", "993");

			emailSession = Session.getDefaultInstance(props); // <-- get Session instance for properties

			st = emailSession.getStore("imaps"); // <-- get Store-session type

			st.connect(pwAuth.getUserName(), pwAuth.getPassword()); // <-- attempt to connect to store with User-login
																	// credentials

			// if authentication failed...
		} catch (AuthenticationFailedException e) {

			// alert user of failed connection
			Alert badLog = new Alert(AlertType.INFORMATION);
			badLog.setTitle("ERROR");
			badLog.setHeaderText("Invalid Login");
			badLog.setContentText("Unable to connect to server with credentials for email: " + pwAuth.getUserName());
			badLog.showAndWait();
			emailSession = null;
			try {
				st.close();
			} catch (MessagingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			st = null;
			return false; // <-- return

			// if provider could NOT be found
		} catch (NoSuchProviderException e) {
			// alert user that provider did not exist
			System.out.println("Provider does not exist");
			return false; // <-- return

			// if Folder could not be accessed, throw error
		} catch (MessagingException e) {
			System.out.println("MESSAGE EXCEPTION");
			return false;
		}

		// if we've reached here, connection was successful\
		return true;
	}
}