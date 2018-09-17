package views;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.mail.*;

/**
 * Class: LoginController
 * Purpose: Controls functionality of Login Screen
 * @author mclancaster
 *
 */
public class LoginController implements Initializable{
	
	// FX elements
	private ObservableList<String> servList;
	private PasswordAuthentication pwAuth;
	
	// Standard elements
	private String[] servOpts = new String[] {"Gmail"};
	private boolean btnPush;
	
	// FXML elements
	@FXML
	private ChoiceBox<String> servChoice;
	@FXML
	private TextField textEmail;
	@FXML
	private PasswordField pw;
		
	/**
	 * INITIALIZE
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		servList = FXCollections.observableArrayList();
		servList.addAll(servOpts);
		servChoice.setItems(servList);
		servChoice.setValue(servChoice.getItems().get(0));
		pwAuth = null;
		btnPush = false;
	}
	
	//FXML METHODS
	
	@FXML
	/**
	 * Function: processLogin
	 * Purpose: verifies user has entered all form-data, then returns control to TicketMainConsole
	 * @author mclancaster
	 */
	private void processLogin() {
		
		// if user/pw are blank, do NOT process
		if (textEmail.getText().length() == 0 || pw.getText().length() == 0) {
			System.out.println("Must have valid email/pw");
		}
		else {			
			
			pwAuth = new PasswordAuthentication(textEmail.getText().trim(), pw.getText().trim()); // <-- create new authentication object			
			
			// reset text fields
			textEmail.setText("");
			pw.setText("");
			btnPush = true;
			Stage me = (Stage)pw.getScene().getWindow();
			me.close(); // <-- close window (triggers event in mainController)
		}
	}
	
	// NON-FXML METHODS
	
	/**
	 * Function: getPwAuth
	 * @author mclancaster
	 * @return pwAuth object
	 */
	public PasswordAuthentication getPwAuth() {
		return pwAuth;
	}
	
	/**
	 * Function: getHostName
	 * Purpose: process host-name based on choice-box selection
	 * @return String - HostName based on selection
	 */
	public String getHostName() {
		if (servChoice.getValue() == servOpts[0]) {
			return "imap.gmail.com";
		}
		return ""; // <-- should never reach this, as return value is determined by a choice-box
	}
	
	/**
	 * Function: getBtnPush
	 * Purpose: Used to determine if user pressed "submit" button, or manually closed the window
	 * @return btnPush - pushed "submit" (true), else (false)
	 */
	public boolean getBtnPush() {
		return btnPush;
	}
}