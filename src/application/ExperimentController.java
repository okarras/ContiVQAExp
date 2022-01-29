package application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hildan.fxgson.FxGson;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ExperimentController {
	
	@FXML private VBox menuPane;
	@FXML private ComboBox<String> displayComboBox;
	@FXML private VBox displayView;
	@FXML private VBox extraSettingsView;
	@FXML private TextField expNameField;
	@FXML private TextField chooseFileField;
	@FXML private Button fileChooserButton;
	@FXML private CheckBox assessCheckBox;
	@FXML private ScrollPane demoPane;
	@FXML private VBox demoBox;
	@FXML private HBox addFeatureBox;
	@FXML private Button addDemoButton;
	@FXML private Button nextButton;
	@FXML private Button backButton;
	
	private FXMLLoader fxmlLoader;
	private SliderController sliderCtr;
	private RadioButtonsController rbCtr;
	private List<TextField> demoFields;
	private String[] newRbLabels;
	private String emptyFieldsMessage = "\nPlease ensure that there are no empty fields before proceeding further.";
	
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
		
		// set default file name
		File exp = new File("Experiments");
		Integer file_num;
		if (exp.listFiles() == null)
			file_num = 1;
		else
			file_num = exp.listFiles().length + 1;
		expNameField.setText("Experiment_" + file_num.toString());
		
		// set the video path text field as non-editable
		chooseFileField.setEditable(false);
		
		// initialize combo box properties
	    displayComboBox.getItems().clear();
		displayComboBox.getItems().addAll("Slider", "Radio Buttons");
		displayComboBox.setPromptText("Select type...");
	    
		// change the display view w.r.t. the selected Combo Box text
	    displayComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
	        try {
	        	// removes all whitespace from the string
	        	String displayType = newValue.replaceAll("\\s+","");
	        	
	        	fxmlLoader = new FXMLLoader(getClass().getResource(displayType + ".fxml"));
	        	Parent root = fxmlLoader.load();
	        	
	        	if (newValue.equals("Slider")) {
	        		sliderCtr = (SliderController) fxmlLoader.getController();
	        	}
	        	else if (newValue.equals("Radio Buttons")) {
	        		rbCtr = (RadioButtonsController) fxmlLoader.getController();
	        	}
	        	displayView.getChildren().clear();
	        	displayView.getChildren().add(root);
	        	
			} catch (Exception e) {
				e.printStackTrace();
			}
	    });
	    
	    // Pass the participant ID as the first text field
	    // and make it non-editable as it as a key attribute
	    TextField partNameField = new TextField("ID");
	    partNameField.setEditable(false);
		demoBox.getChildren().add(partNameField);
		demoFields = new ArrayList<>();
		demoFields.add(partNameField);
	}
	
	@FXML
	private void fileChooserButtonClicked() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Video File");
		fileChooser.getExtensionFilters().addAll(
		        new ExtensionFilter("Video Files", "*.mp4", "*.flv"),
		        new ExtensionFilter("All Files", "*.*"));
		Stage stage = (Stage) fileChooserButton.getScene().getWindow();
		File selectedFile = fileChooser.showOpenDialog(stage);
		if (selectedFile != null) {
			chooseFileField.setText(selectedFile.toString());
		}
	}
	
	@FXML
	private void nextButtonClicked() {
		// create new directory with the experiment name
		File exp = new File("Experiments/" + expNameField.getText());
		if (!exp.exists()) {
			exp.mkdir();
		}
		// if directory already exists, alert user (optional)
		else {
			// open alert box to confirm overwriting
			Alert alert = new Alert(AlertType.CONFIRMATION, 
					"An experiment already exists with the name '" + exp.getName() 
					+ "'. Are you sure you want to overwrite this experiment?", 
					ButtonType.YES, ButtonType.NO);
			alert.showAndWait();

			if (alert.getResult() == ButtonType.YES) {
			    // delete folder and all its contents
				MainWindowController.deleteFolder(exp);
				// create new folder with the same name
				exp = new File("Experiments/" + expNameField.getText());
				exp.mkdir();
			}
			else {
				return;
			}
		}
		
		// copy video file to directory
		File src = new File(chooseFileField.getText());
		File target = new File("Experiments/" + expNameField.getText() + "/" + src.getName());
		try {
			Files.copy(Paths.get(src.getPath()), Paths.get(target.getPath()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// save experiment and settings info as a JSON file
		saveInfoAsJson();
		
		// switch scene to MWC
		switchToMainWindow();
	}
	
	@FXML
	private void addNewDemoFeature() {
		TextField demoField = new TextField();
		demoBox.getChildren().add(demoField);
		demoFields.add(demoField);
	}
	
	public void changeToViewMode(String exp_name) {
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()  
                .setPrettyPrinting()
                .create();
		// the saved .JSON file from which the information is to be parsed
		File filePath = new File("Experiments/" + exp_name + "/settings.json");
		
		File extraSettingsFile = new File("Experiments/" + exp_name + "/extra_settings.json");
		
		try {
			// parse data from the respective JSON file and set variables
			
			// read the JSON file as a string
			String json = new String(Files.readAllBytes(Paths.get(filePath.getPath())));
			String extraSettingsJson = new String(Files.readAllBytes(Paths.get(extraSettingsFile.getPath())));
			
			// parse the string into JsonObjects
			JsonObject exp_obj = JsonParser.parseString(json).getAsJsonObject();
			JsonObject set_obj = exp_obj.getAsJsonObject("settings");
			JsonObject extra_set_obj = JsonParser.parseString(extraSettingsJson).getAsJsonObject();
			
			Experiment exp = fxGson.fromJson(exp_obj, Experiment.class);
			ExtraSettings extraSettings = fxGson.fromJson(extra_set_obj, ExtraSettings.class);
			
			// extract other experiment parameters
			String name = exp.getName();
			String video = exp.getVideo();
			String display = exp.getDisplay();
			boolean hasAssessForm = extraSettings.hasAssessForm();
			String[] demoLabels = extraSettings.getDemoLabels();
			
			// set the values of the respective parameters
		    expNameField.setText(name);
		    chooseFileField.setText(video);
			displayComboBox.getSelectionModel().select(display);
			nextButton.setText("Save");
			assessCheckBox.setSelected(hasAssessForm);
			demoBox.getChildren().clear();
			for (String demoLabel: demoLabels) {
				demoBox.getChildren().add(new Label(demoLabel));
			}
			
			// save button clicked
			nextButton.setOnAction(e -> {
				String newName = expNameField.getText();
				boolean isNameEmpty = (newName.equals(""));  // empty string
				boolean diffFolderExists = false;	// folder with the same name exists
				File exp_file = new File("Experiments/" + newName);
				if (exp_file.exists() && !newName.equals(exp_name)) {
					diffFolderExists = true;
				}
				
				if (isNameEmpty || diffFolderExists) {
					// create custom message
					String message = "";
					if (isNameEmpty) {
						message += "The given name is empty and hence not valid.";
					}
					else if (diffFolderExists) {
						message += "An experiment already exists with the given name '" + newName + "'.";
					}
					message += "\nPlease provide another name to the experiment.";
					
					// show alert (info/message) dialog
					Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
					alert.showAndWait();

					if (alert.getResult() == ButtonType.OK) {
					    // do nothing
						return;
					}
				}
				else {
					// rename experiment (both folder and the entry in the JSON file)
					
					// Extract selected data from all editable labels
					boolean emptyFieldsPresent = false;
					// implement switch case: depending upon the selected display type
					if (display.equals("Slider")) {
						String minLabel = sliderCtr.getMinLabelField().getText();
						String maxLabel = sliderCtr.getMaxLabelField().getText();
						String intervalType = sliderCtr.getIntervalType();
						
						if (minLabel.equals("") || maxLabel.equals("")) {
							// show alert (info/message) dialog
							Alert alert = new Alert(AlertType.INFORMATION, emptyFieldsMessage, ButtonType.OK);
							alert.showAndWait();
							if (alert.getResult() == ButtonType.OK) {
							    // do nothing
								return;
							}
						}
						else {
							set_obj.addProperty("minLabel", minLabel);
							set_obj.addProperty("maxLabel", maxLabel);
							set_obj.addProperty("intervalType", intervalType);
						}
					}
					else if (display.equals("Radio Buttons")) {
						String minLabel = rbCtr.getMinLabelField().getText();
						String maxLabel = rbCtr.getMaxLabelField().getText();
						String[] rbLabels = rbCtr.getLabels();
						
						for (String rbLabel : rbLabels) {
							if (rbLabel.equals("")) {
								emptyFieldsPresent = true;
								break;
							}
						}
						
						if (emptyFieldsPresent || minLabel.equals("") || maxLabel.equals("")) {
							// show alert (info/message) dialog
							Alert alert = new Alert(AlertType.INFORMATION, emptyFieldsMessage, ButtonType.OK);
							alert.showAndWait();
							if (alert.getResult() == ButtonType.OK) {
							    // do nothing
								return;
							}
						} else {
							set_obj.addProperty("minLabel", minLabel);
							set_obj.addProperty("maxLabel", maxLabel);
							newRbLabels = rbLabels;
						}
					}
					else {
						System.out.println("ERROR: Display type not recognizable.");
					}
					
					// rename experiment folder name
					File oldDir = new File("Experiments/" + exp_name);
					File newDir = new File("Experiments/" + newName);
					oldDir.renameTo(newDir);
					
					// rename experiment name in the settings JSON file
					exp_obj.addProperty("name", newName);
					
					File origVideoPath = new File(video);
					File newVideoPath = new File("Experiments/" + newName + "/" + origVideoPath.getName());
					exp_obj.addProperty("video", newVideoPath.getPath());
					
					String new_json = fxGson.toJson(exp_obj);
					if (display.equals("Slider")) {
						SL sl = fxGson.fromJson(set_obj, SL.class);
						Experiment new_exp = new Experiment(newName, newVideoPath.getPath(), display, sl);
						new_json = fxGson.toJson(new_exp);
					}
					else if (display.equals("Radio Buttons")) {
						RB rb = fxGson.fromJson(set_obj, RB.class);
						rb = new RB(rb.getMinLabel(), rb.getMaxLabel(), rb.getButtonNum(), newRbLabels);
						Experiment new_exp = new Experiment(newName, newVideoPath.getPath(), display, rb);
						new_json = fxGson.toJson(new_exp);
					}
					else {
						System.out.println("ERROR: Display type not recognizable.");
					}
					
					try {
						FileWriter fw = new FileWriter(new File(newDir.getPath() + "/settings.json"), false);
						fw.write(new_json);
						fw.flush();
						fw.close();
					} catch (Exception e_new) {
						e_new.printStackTrace();
					}
					
					// switch scene to MWC
					switchToMainWindow();
				}
			});
		    
			// implement switch case: depending upon the selected display type
			if (display.equals("Slider")) {
				SL sl = fxGson.fromJson(set_obj, SL.class);
		    	sliderCtr.changeToViewMode(sl.getMinLabel(), sl.getMaxLabel(), 
		    			sl.getMinValue(), sl.getMaxValue(), sl.getIntervalType());
			}
			else if (display.equals("Radio Buttons")) {
				RB rb = fxGson.fromJson(set_obj, RB.class);
				rbCtr.changeToViewMode(rb.getMinLabel(), rb.getMaxLabel(), 
						rb.getButtonNum(), rb.labels);
			}
			else {
				System.out.println("ERROR: Display type not recognizable.");
			}
			
			// disable portions of the window that should be non-editable
			chooseFileField.setEditable(false);
			fileChooserButton.setDisable(true);
			displayComboBox.setDisable(true);
			assessCheckBox.setDisable(true);
			addFeatureBox.getChildren().clear();
			addFeatureBox.getChildren().add(new Label("Attributes:"));
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void switchToMainWindow() {
		// switch scene to MWC
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
	
	@FXML
	private void backButtonClicked() {
		switchToMainWindow();
	}
	
	private void saveInfoAsJson() {
		// the new .JSON file in which the information is to be stored
		File jsonFile = new File("Experiments/" + expNameField.getText() + "/settings.json");
		File extraSettingsFile = new File("Experiments/" + expNameField.getText() + "/extra_settings.json");
		
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()
                .setPrettyPrinting()
                .create();
		
		String name = expNameField.getText();
		File orig_video = new File(chooseFileField.getText());
		File copied_video = new File("Experiments/" + expNameField.getText() + "/" + orig_video.getName());
	    String video = copied_video.getPath();
	    String display = displayComboBox.getValue();
	    Settings settings = null;
	    
	    // plug the parameters into the constructors of the static classes
	    if (display.equals("Slider")) {
	    	settings = new SL(sliderCtr.getMinLabelField().getText(), sliderCtr.getMaxLabelField().getText(), sliderCtr.getMinValueField().getText(), sliderCtr.getMaxValueField().getText(), sliderCtr.getIntervalType());
	    }
	    else if (display.equals("Radio Buttons")) {
	    	settings = new RB(rbCtr.getMinLabelField().getText(), rbCtr.getMaxLabelField().getText(), rbCtr.getButtonNumField().getText(), rbCtr.getLabels());
	    }
	    else {
	    	System.out.println("Error: Display type: " + display);
	    }
		
	    List<String> demoLabels = new ArrayList<>();
	    for (TextField tf : demoFields) {
	    	String text = tf.getText();
	    	if (!text.equals("") && !demoLabels.contains(text)) {
	    		demoLabels.add(tf.getText());
	    	}
	    }
	    
		Experiment exp = new Experiment(name, video, display, settings);
		ExtraSettings extraSettings = new ExtraSettings(assessCheckBox.isSelected(), demoLabels.toArray(new String[0]));
		
		// generate JSON output and write it to a .JSON file
		try {
			String json = fxGson.toJson(exp);
			String extraSettingsJson = fxGson.toJson(extraSettings);
			
			FileWriter fw = new FileWriter(jsonFile);
			fw.write(json);
			fw.flush();
			fw.close();
			
			fw = new FileWriter(extraSettingsFile);
			fw.write(extraSettingsJson);
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Static classes
	
	static class Experiment {
	    private String name;
	    private String video;
	    private String display;
	    private Settings settings;
	    
	    private Experiment(String name, String video, String display, Settings settings) {
	    	this.name = name;
	    	this.video = video;
	    	this.display = display;
	    	this.settings = settings;
	    }
	    
	    public String getName() {
			return name;
		}
	    
	    public String getVideo() {
			return video;
		}
	    
	    public String getDisplay() {
			return display;
		}
	    
	    public Settings getSettings() {
			return settings;
		}
	    
	    @Override
	    public String toString() {
	    	return String.format("{"
	      						+ "\n\t\"name\" : \"%s\","
	      						+ "\n\t\"video\" : \"%s\","
	      						+ "\n\t\"display\" : \"%s\","
	      						+ "\n\t\"settings\" : \"%s\",\n"
	      						+ "}", name, video, display, settings);
	    }
	}
	
	static class Settings {
	    private String minLabel;
	    private String maxLabel;
	    
	    private Settings(String minLabel, String maxLabel) {
	    	this.minLabel = minLabel;
	    	this.maxLabel = maxLabel;
	    }
	    
	    public String getMinLabel() {
			return minLabel;
		}
	    
	    public String getMaxLabel() {
			return maxLabel;
		}
	    
	    @Override
	    public String toString() {
	    	return String.format("{"
								+ "\n\t\"minLabel\" : \"%s\","
								+ "\n\t\"maxLabel\" : \"%s\",\n"
								+ "}", minLabel, maxLabel);
	    }
	}
	
	static class SL extends Settings {
	    private String minValue;
	    private String maxValue;
	    private String intervalType;
	    
	    private SL(String minLabel, String maxLabel, String minValue, String maxValue, String intervalType) {
	    	super(minLabel, maxLabel);
	    	this.minValue = minValue;
	    	this.maxValue = maxValue;
	    	this.intervalType = intervalType;
	    }
	    
	    public String getMinLabel() {
			return super.getMinLabel();
		}
	    
	    public String getMaxLabel() {
			return super.getMaxLabel();
		}
	    
	    public String getMinValue() {
			return minValue;
		}
	    
	    public String getMaxValue() {
			return maxValue;
		}
	    
	    public String getIntervalType() {
			return intervalType;
		}
	    
	    @Override
	    public String toString() {
	    	return String.format("{"
								+ "\n\t\"minLabel\" : \"%s\","
								+ "\n\t\"maxLabel\" : \"%s\","
								+ "\n\t\"minValue\" : \"%s\","
								+ "\n\t\"maxValue\" : \"%s\","
								+ "\n\t\"intervalType\" : \"%s\",\n"
								+ "}", super.minLabel, super.maxLabel, minValue, maxValue, intervalType);
	    }
	}
	
	static class RB extends Settings {
	    private String buttonNum;
	    private String[] labels;
	    
	    private RB(String minLabel, String maxLabel, String buttonNum, String[] labels) {
	    	super(minLabel, maxLabel);
	    	this.buttonNum = buttonNum;
	    	this.labels = labels;
	    }
	    
	    public String getMinLabel() {
			return super.getMinLabel();
		}
	    
	    public String getMaxLabel() {
			return super.getMaxLabel();
		}
	    
	    public String getButtonNum() {
			return buttonNum;
		}
	    
	    public String[] getLabels() {
			return labels;
		}
	    
	    @Override
	    public String toString() {
	    	return String.format("{"
								+ "\n\t\"minLabel\" : \"%s\","
								+ "\n\t\"maxLabel\" : \"%s\","
								+ "\n\t\"buttonNum\" : \"%s\","
								+ "\n\t\"labels\" : \"%s\",\n"
								+ "}", super.minLabel, super.maxLabel, buttonNum, Arrays.toString(labels));
	    }
	}
	
	static class ExtraSettings {
	    private boolean assessFormState = true;
	    private String[] demoLabels;
	    
	    private ExtraSettings(boolean assessFormState, String[] labels) {
	    	this.assessFormState = assessFormState;
	    	this.demoLabels = labels;
	    }
	    
	    public boolean hasAssessForm() {
			return assessFormState;
		}
	    
	    public String[] getDemoLabels() {
			return demoLabels;
		}
	    
	    @Override
	    public String toString() {
	    	return String.format("{"
								+ "\n\t\"assessFormState\" : \"%s\","
								+ "\n\t\"demoLabels\" : \"%s\",\n"
								+ "}", assessFormState, Arrays.toString(demoLabels));
	    }
	}
}
