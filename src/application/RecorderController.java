package application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hildan.fxgson.FxGson;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import application.ExperimentController.Experiment;
import application.ExperimentController.ExtraSettings;
import application.ExperimentController.RB;
import application.ExperimentController.SL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class RecorderController {
	
	@FXML private VBox menuPane;
	@FXML private VBox videoView;
	@FXML private VBox recorderView;
	@FXML private Button nextButton;
	@FXML private Button cancelButton;
	
	private String expName;
	private String partName;
	private String recorderType;
	private SliderController sliderCtr;
	private RadioButtonsController rbCtr;
	private QuestionFormController questionFormCtr;
	private ExtraSettings extraSettings;
	private MediaController mediaCtr;
	private MediaPlayer mediaPlayer;
	private FXMLLoader fxmlLoader;
	private Map<String, String> recordedData;
	
	public RecorderController(String exp, String part) {
		this.expName = exp;
		this.partName = part;
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
		
		// open video player

        // create media player
        String mediaFilePath = findVideoFile(expName);
        try {
			String media_url = (new File(mediaFilePath)).toURI().toURL().toString();
	        
	        Media media = new Media(media_url);
	        mediaPlayer = new MediaPlayer(media);
	        mediaPlayer.setAutoPlay(true);
	        
	        mediaCtr = new MediaController(mediaPlayer);
	        mediaCtr.setNextButton(nextButton);
	        
	        TitledPane titledPane = new TitledPane(mediaFilePath, mediaCtr);
	        titledPane.setCollapsible(false);
	        
	        videoView.getChildren().clear();
	        videoView.getChildren().add(titledPane);
	        
	        loadRecorder();
	        
	        // stores the value on the recorder with time 
	        mediaPlayer.currentTimeProperty().addListener((observableValue, oldDuration, newDuration) -> {
	        	double seconds = newDuration.toSeconds();
        	    // integer type
        		String time = Double.toString(seconds);		
	        	String selected = "-";
	        	if (recorderType.equals("Slider")) {
	        		Slider slider = sliderCtr.getSlider();
	        		String intervalType = sliderCtr.getIntervalType();
	        		
	        		selected = String.format("%.2f", slider.getValue());	// continuous, by default
	        		if (intervalType != null) {
	    				if (intervalType.equals("discrete")) {	// discrete
	    					selected = String.format("%d", Math.round(slider.getValue()));
	    				}
	    			}
	        		
	        		if (mediaPlayer.getCurrentTime().equals(mediaPlayer.getStartTime())) {
	        			// select the middle slider value by default
	        			sliderCtr.selectMidByDefault();
	        		}
	        	}
	        	else if (recorderType.equals("Radio Buttons")) {
	        		ToggleGroup rbToggleGrp = rbCtr.getToggleGroup();
	        		RadioButton rb = (RadioButton) rbToggleGrp.getSelectedToggle();
	        		
	        		selected = rb.getId();
	        		
	        		if (mediaPlayer.getCurrentTime().equals(mediaPlayer.getStartTime())) {
	        			// select the middle radio button by default
		        		rbCtr.selectMidByDefault();
	        		}
	        	}
	        	
	        	// Update the recorded data for every change in time
	        	Map<String, String> tempData = mediaCtr.getRecordedData();
	        	tempData.put(time, selected);
	        	mediaCtr.setRecordedData(tempData);
	        });
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void nextButtonClicked() {
		recordedData = mediaCtr.getRecordedData();
		mediaPlayer.dispose();
		saveMapToExcelFile();	// save map (i.e. the recorder data) to an excel file
		
		if (extraSettings.hasAssessForm()) {
			switchToQuestionFormWindow();
		}
		else {
			switchToMainWindow();
		}
	}
	
	@FXML
	private void cancelButtonClicked() {
		mediaPlayer.dispose();
		MainWindowController.deleteFolder(new File("Experiments/" + expName + "/" + partName));
		switchToMainWindow();
	}
	
	@FXML
	private void switchToQuestionFormWindow() {
	    Stage stage = (Stage) nextButton.getScene().getWindow();
	    
		Parent root;
		try {
			questionFormCtr = new QuestionFormController(expName, partName);
			
			fxmlLoader = new FXMLLoader(getClass().getResource("QuestionFormWindow.fxml"));
			fxmlLoader.setController(questionFormCtr);
        	root = fxmlLoader.load();
        	
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
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
	
	public void saveMapToExcelFile() {
		Map<String, String> condensedData = condenseMap(recordedData);
		
		try {
			FileWriter csvWriter = new FileWriter("Experiments/" + expName + "/" + partName + "/data.csv");  
			csvWriter.append("Time");  
			csvWriter.append(",");
			csvWriter.append("Value");
			csvWriter.append("\n");
			
			// Sorts the keyspace of the map
			SortedSet<String> keys = new TreeSet<>(condensedData.keySet());
			for (String key : keys) {
				// Condenses the recorded values such that they are mapped only for each second.
				String value = recordedData.get(condensedData.get(key));
				value = value.replace(',', '.');	// so that double values are not interpreted as two different row values
				
				List<String> row = Arrays.asList(key, value);
				// Writes the row (time, value) to the Excel file
				csvWriter.append(String.join(",", row));
			    csvWriter.append("\n");
			}
			
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, String> condenseMap(Map<String, String> orig_map) {
		Map<String, String> cond_map = new HashMap<>();
		
		// Stores the nearest key for each second into the condensed map
		for (String key : orig_map.keySet()) {
			double key_double = Double.parseDouble(key);
			int key_floor = (int) Math.floor(key_double);
			double new_diff = key_double - key_floor;
			
			String key_floor_str = Integer.toString(key_floor);
			if (cond_map.containsKey(key_floor_str)) {
				double curr_diff = Double.parseDouble(cond_map.get(key_floor_str)) - key_floor;
				
				if (new_diff < curr_diff) {
					cond_map.put(key_floor_str, key);
				}
			}
			else {
				cond_map.put(key_floor_str, key);
			}
		}
		
		return cond_map;
	}
	
	public static String findVideoFile(String expName) {
		File expFolder = new File("Experiments/" + expName);
		File videoFile = null;
		
		// identify video file based on file extension
		for (File file : expFolder.listFiles()) {
	        if (file.isFile()) {
	        	String filename = file.getName();
	            int i = filename.lastIndexOf('.');
	            String ext = i > 0 ? filename.substring(i + 1) : "";
	            if (ext.equals("mp4") || ext.equals("flv")) {
	            	videoFile = file;
	            	break;
	            }
	        }
	    }

		return videoFile.getPath();
	}
	
	public void loadRecorder() {
		// parse data from the respective JSON file to find and load the display type
		
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()  
                .setPrettyPrinting()
                .create();
		
		try {
			// read the JSON file as a string
			File filePath = new File("Experiments/" + expName + "/settings.json");
			String json = new String(Files.readAllBytes(Paths.get(filePath.getPath())));
			
			File extraSettingsFile = new File("Experiments/" + expName + "/extra_settings.json");
			String extraSettingsJson = new String(Files.readAllBytes(Paths.get(extraSettingsFile.getPath())));
			
			// parse the string into JsonObjects
			JsonObject exp_obj = JsonParser.parseString(json).getAsJsonObject();
			JsonObject set_obj = exp_obj.getAsJsonObject("settings");
			JsonObject extra_set_obj = JsonParser.parseString(extraSettingsJson).getAsJsonObject();
			
			Experiment exp = fxGson.fromJson(exp_obj, Experiment.class);
			extraSettings = fxGson.fromJson(extra_set_obj, ExtraSettings.class);
			
			// extract parameters
			String display = exp.getDisplay();
			recorderType = display;
				
        	// removes all whitespace from the string
        	String displayType = display.replaceAll("\\s+","");
        	
        	fxmlLoader = new FXMLLoader(getClass().getResource(displayType + ".fxml"));
        	Parent root = fxmlLoader.load();
        	
        	// implement switch case: depending upon the selected display type
        	if (display.equals("Slider")) {
        		SL sl = fxGson.fromJson(set_obj, SL.class);
        		sliderCtr = (SliderController) fxmlLoader.getController();
        		
        		// set the values of the respective parameters
        		sliderCtr.changeToViewMode(sl.getMinLabel(), sl.getMaxLabel(), sl.getMinValue(), sl.getMaxValue(), sl.getIntervalType());
        		// remove the settings box
        		sliderCtr.removeSettings();
        		// select the middle slider value by default
        		sliderCtr.selectMidByDefault();
        	}
        	else if (display.equals("Radio Buttons")) {
        		RB rb = fxGson.fromJson(set_obj, RB.class);
        		rbCtr = (RadioButtonsController) fxmlLoader.getController();
        		
        		// set the values of the respective parameters
        		rbCtr.changeToViewMode(rb.getMinLabel(), rb.getMaxLabel(), rb.getButtonNum(), rb.getLabels());
        		// remove the settings box
        		rbCtr.removeSettings();
        		// select the middle radio button by default
        		rbCtr.selectMidByDefault();
        	}
        	else {
    			System.out.println("ERROR: Display type not recognizable.");
    		}
        	recorderView.getChildren().clear();
        	recorderView.getChildren().add(root);
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
