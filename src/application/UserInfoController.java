package application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hildan.fxgson.FxGson;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import application.ExperimentController.ExtraSettings;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UserInfoController {

	@FXML private VBox menuPane;
	@FXML private Button nextButton;
	@FXML private Button cancelButton;
	@FXML private VBox demoFieldBox;
	
	private String expName;
	private String partName;
	private TextField partNameField;
	private FXMLLoader fxmlLoader;
	private RecorderController recorderCtr;
	private List<String> demoFieldLabels;
	private List<TextField> demoFields;
	private Map<String, String> demoInfoMap;
	
	public UserInfoController(String exp) {
		this.expName = exp;
	}
	
	@FXML
	public void initialize() {
		// Set MenuBar
		try {
			CustomMenuBar customMenuBar = new CustomMenuBar();
			fxmlLoader = new FXMLLoader(getClass().getResource("MenuBar.fxml"));
        	fxmlLoader.setController(customMenuBar);
        	Parent root = fxmlLoader.load();
        	menuPane.getChildren().clear();
        	menuPane.getChildren().add(root);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// generate Text Fields from the stored demoLabels list in the settings
		generateDemoFields();
		
		// enable the next button only if all parameters are entered
	    nextButton.disableProperty().unbind();
	    nextButton.disableProperty().bind(
	    	    Bindings.length(partNameField.textProperty()).isEqualTo(0));
	}
	
	public void generateDemoFields() {
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()  
                .setPrettyPrinting()
                .create();
		
		// the saved .JSON file from which the information is to be parsed
		File extraSettingsFile = new File("Experiments/" + expName + "/extra_settings.json");
		
		demoInfoMap = new HashMap<>();
		
		try {
			// parse data from the respective JSON file and set variables
			
			// read the JSON file as a string
			String extraSettingsJson = new String(Files.readAllBytes(Paths.get(extraSettingsFile.getPath())));
			
			// parse the string into JsonObjects
			JsonObject extra_set_obj = JsonParser.parseString(extraSettingsJson).getAsJsonObject();
			
			ExtraSettings extraSettings = fxGson.fromJson(extra_set_obj, ExtraSettings.class);
			
			// extract other experiment parameters
			String[] demoLabels = extraSettings.getDemoLabels();
			demoFieldLabels = new ArrayList<>();
			demoFields = new ArrayList<>();
			for (String demoLabel: demoLabels) {
				// Create label-text field pairs for each demo feature
				HBox demoEntry = new HBox();
				demoEntry.setSpacing(8);
				demoEntry.setAlignment(Pos.CENTER_LEFT);
				
				TextField demoField = new TextField();
				demoField.setId(demoLabel);
				demoFields.add(demoField);
				demoFieldLabels.add(demoLabel);
				
				if (demoLabel.equals("ID")) {
					partNameField = demoField;
				}
				
				Label tempLabel = new Label(demoLabel + ": ");
				
				demoEntry.getChildren().addAll(tempLabel, demoField);
				demoFieldBox.getChildren().add(demoEntry);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void nextButtonClicked() {
		// create new directory with the participant name (ID)
		partName = partNameField.getText();
		String partPath = "Experiments/" + expName + "/" + partName;
		File part = new File(partPath);
		if (!part.exists()) {
			part.mkdir();
		}
		// if directory already exists, alert user
		else {
			// open alert box to confirm overwriting
			Alert alert = new Alert(AlertType.CONFIRMATION, 
					"A participant already exists with the name '" + part.getName() 
					+ "'. Are you sure you want to overwrite this participant?", 
					ButtonType.YES, ButtonType.NO);
			alert.showAndWait();

			if (alert.getResult() == ButtonType.YES) {
			    // delete folder and all its contents
				MainWindowController.deleteFolder(part);
				// create new folder with the same name
				part = new File(partPath);
				part.mkdir();
			}
			else {
				return;
			}
		}
		
		// save experiment and settings info as a JSON file
		saveInfoAsJson();
				
		switchToRecorderWindow();
	}
	
	@FXML
	private void cancelButtonClicked() {
		switchToMainWindow();
	}
	
	@FXML
	private void switchToRecorderWindow() {
	    Stage stage = (Stage) nextButton.getScene().getWindow();
	    
		Parent root;
		try {
			recorderCtr = new RecorderController(expName, partName);
			
			fxmlLoader = new FXMLLoader(getClass().getResource("RecorderWindow.fxml"));
        	fxmlLoader.setController(recorderCtr);
        	root = fxmlLoader.load();
        	
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void switchToMainWindow() {
		// switch scene to given window
		Stage stage = (Stage) nextButton.getScene().getWindow();
		Parent root;
		try {
			root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void saveInfoAsJson() {
		// the new .JSON file in which the information is to be stored
		String jsonFilePath = "Experiments/" + expName + "/" + partName + "/demographics.json";
		File jsonFile = new File(jsonFilePath);
		
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()
                .setPrettyPrinting()
                .create();
		
		// input all demo information into the map
		for (TextField tf : demoFields) {
			String key = tf.getId();
			String value = tf.getText();
			
			demoInfoMap.put(key, value);
		}
		
		// generate JSON output and write it to a .JSON file
		try {
			String json = fxGson.toJson(demoInfoMap);
			
			FileWriter fw = new FileWriter(jsonFile);
			fw.write(json);
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
