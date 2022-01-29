package application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.hildan.fxgson.FxGson;

import com.google.gson.Gson;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class QuestionFormController {
	
	@FXML private VBox menuPane;
	@FXML private HBox overallRadioBox;
	@FXML private ScrollPane scrollPane;
	@FXML private GridPane questionBox;
	@FXML private Button nextButton;
	@FXML private Button cancelButton;
	
	private FXMLLoader fxmlLoader;
	private String expName;
	private String partName;
	List<String> featureList, descrList, secDescrList, scaleLabels;
	Map<String, ToggleGroup> map;
	
	public QuestionFormController(String exp, String part) {
		this.expName = exp;
		this.partName = part;
	}
	
	@FXML
	public void initialize() {
		createAssessmentForm();
	}
	
	public void createAssessmentForm() {
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
		
		featureList = getFeatureList();
		// Removes the first feature (i.e. "Overall video quality"), as it is already displayed
		featureList.remove(0);
		
		// Gets the other label lists (do not contain values for 'Overall video quality')
		descrList = getFeatureDescriptions();
		secDescrList = getSecondaryDescriptions();
		scaleLabels = getScaleLabels();
		
		map = new HashMap<String, ToggleGroup>();
		
		// Sets toggle group for the overall video quality attribute
		ToggleGroup overallToggleGrp = new ToggleGroup();
		List<String> overallRbLabels = Arrays.asList("very good", "good", "neutral", "bad", "very bad");
		for(int rbInd = 0; rbInd < 5; rbInd++) {
			RadioButton rb = new RadioButton(overallRbLabels.get(rbInd));
			rb.setStyle("-fx-font-weight: bold;");
			rb.setToggleGroup(overallToggleGrp);
			overallRadioBox.getChildren().add(rb);
		}
		
		map.put("Overall video quality", overallToggleGrp);
		
		int rowIndex = 0, count = 0;
		ListIterator<String> listIter = featureList.listIterator();
		while(listIter.hasNext()) {
			String feature = listIter.next();
			String description = descrList.get(count);
			
			String secDescription = secDescrList.get(count);
			// Splits the secondary description into three parts so that the middle part 
			// can be updated with the new value, when the radio buttons selection is changed
			List<String> secDescParts = Arrays.asList(secDescription.split("_____"));
			Label secDescPreLabel = new Label(secDescParts.get(0));
			Label secDescValueLabel = new Label("     ");
			Label secDescPostLabel = new Label(secDescParts.get(1));
			// Sets special style (bold, underlined) to the middle string
			secDescValueLabel.setStyle("-fx-font-weight: bold; -fx-underline: true;");
			// Joins the label parts together into a single box
			HBox secDescLabelBox = new HBox();
			secDescLabelBox.getChildren().addAll(secDescPreLabel, secDescValueLabel, secDescPostLabel);
			
			TextFlow featureTextFlow = new TextFlow();
			Text featureName = new Text(feature + ": ");
			featureName.setStyle("-fx-font-weight: bold");
			Text featureDescr = new Text(description);
			featureTextFlow.getChildren().addAll(featureName, featureDescr);
			
			Label featureLabel = new Label(null, featureTextFlow);
			
			// Sets the grid positions of the feature labels
			GridPane.setConstraints(featureLabel, 0, rowIndex);	// Parameters:- (var, col, row)
			// Sets the grid positions of the feature (secondary) descriptions
			GridPane.setConstraints(secDescLabelBox, 0, (rowIndex + 1));	// Parameters:- (var, col, row)

			// Sets the paddings for the individual elements of the Grid Pane
			GridPane.setMargin(featureLabel, new Insets(5, 5, 5, 5));
			GridPane.setMargin(secDescLabelBox, new Insets(5, 5, 5, 5));
			
			// Adds the feature labels and secondary descriptions to the grid pane at every interval
			questionBox.getChildren().addAll(featureLabel, secDescLabelBox);
			
			// Creates radio button box
			ToggleGroup toggleGrp = new ToggleGroup();
			
			String scaleLabelString = scaleLabels.get(count);
			List<String> scaleLabelList = Arrays.asList(scaleLabelString.split("\\|"));
			int scaleIndex = 0;
			for (int scaleVal = 2; scaleVal >= -2; scaleVal--) {
				
				VBox scaleHeaderBox = new VBox();
				Label scaleHeaderLabel = new Label(scaleLabelList.get(scaleIndex));
				Label scaleValueLabel = new Label("(" + Integer.toString(scaleVal) + ")");
				scaleHeaderLabel.setStyle("-fx-font-weight: bold");
				scaleValueLabel.setStyle("-fx-font-weight: bold");
				scaleHeaderBox.getChildren().addAll(scaleHeaderLabel, scaleValueLabel);
				scaleHeaderBox.setAlignment(Pos.CENTER);
				
				RadioButton rb = new RadioButton();
				rb.setId(Integer.toString(scaleVal));
				rb.setToggleGroup(toggleGrp);
				HBox rbContainer = new HBox();
				rbContainer.getChildren().add(rb);
				rbContainer.setAlignment(Pos.CENTER);
				
				// Sets the grid positions of the individual scale header labels
				GridPane.setConstraints(scaleHeaderBox, (scaleIndex + 1), rowIndex);	// Parameters:- (var, col, row)
				// Sets the grid positions of the individual radio buttons
				GridPane.setConstraints(rbContainer, (scaleIndex + 1), (rowIndex + 1));	// Parameters:- (var, col, row)
				
				// Aligns the scale headers and radio buttons horizontally to the center
				GridPane.setHalignment(scaleHeaderBox, HPos.CENTER);
				GridPane.setHalignment(rbContainer, HPos.CENTER);
				
				// Sets the paddings for the individual elements of the Grid Pane
				GridPane.setMargin(scaleHeaderBox, new Insets(5, 5, 5, 5));
				GridPane.setMargin(rbContainer, new Insets(5, 5, 5, 5));
				
				// Adds the individual scale header labels and radio buttons to the grid pane at every interval
				questionBox.getChildren().addAll(scaleHeaderBox, rbContainer);
				
				scaleIndex++;
			}
			
			// Listens each of the radio button groups to update the secondary description label
			// whenever the radio button selection is changed
			toggleGrp.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
				RadioButton selectedRB = (RadioButton) toggleGrp.getSelectedToggle();
				String selectedValue = selectedRB.getId();
				
				// Finds the specific scale index corresponding to the selected radio button id
				int ind = 0, val = 2;
				while (val >= -2) {
					if (val == Integer.parseInt(selectedValue)) {
						break;
					}
					val--; ind++;
				}
				
				// Updates the middle part of the secondary description to the newly selected value
				String newScaleLabelString = scaleLabelList.get(ind);
				secDescValueLabel.setText(newScaleLabelString);
			});
			
			map.put(feature, toggleGrp);
			
			rowIndex += 2;
			count++;
		}
		
		questionBox.setPadding(new Insets(2, 2, 2, 2));	// Sets padding all around the grid
		questionBox.setGridLinesVisible(true);	// Table design can be seen
	}
	
	public void nextButtonClicked() {
		try {
			boolean allBtnsSelected = true;
			Set<String> keySet = map.keySet();
			for (String key : keySet) {
				ToggleGroup toggleGrp = map.get(key);
				if (toggleGrp.getSelectedToggle().equals(null)) {
					allBtnsSelected = false;
					break;
				}
			}
			
			if (allBtnsSelected) {
				// save assessment info to JSON file
				saveInfoAsJson();
				
				switchToMainWindow();
			}
		}
		catch (NullPointerException e) {
			String message = "Please complete the survey to proceed.";
			
			// show alert (info/message) dialog
			Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
			alert.showAndWait();

			if (alert.getResult() == ButtonType.OK) {
			    // do nothing
			}
		}
	}
	
	public void cancelButtonClicked() {
		MainWindowController.deleteFolder(new File("Experiments/" + expName + "/" + partName));
		switchToMainWindow();
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
		String jsonFilePath = "Experiments/" + expName + "/" + partName + "/assessment.json";
		File jsonFile = new File(jsonFilePath);
		
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()
                .setPrettyPrinting()
                .create();
		
		// map the features to their respective selected radio button values
		Map<String, String> selectionMap = new HashMap<>();
		for (String key : map.keySet()) {
			ToggleGroup toggleGrp = map.get(key);
			RadioButton selectedRB = (RadioButton) toggleGrp.getSelectedToggle();
			
			String value = selectedRB.getId();
			if (key.equals("Overall video quality")) {	// Exception
				value = selectedRB.getText();
			}
			selectionMap.put(key, value);
		}
		
		// generate JSON output from the new map and write it to a .JSON file
		try {
			String json = fxGson.toJson(selectionMap);
			
			FileWriter fw = new FileWriter(jsonFile);
			fw.write(json);
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static List<String> getFeatureList() {
		List<String> featureList = new ArrayList<>(
				Arrays.asList("Overall video quality", "Image quality", "Sound quality", "Video length", 
						"Focus", "Plot", "Prior knowledge", "Essence", "Clutter", "Clarity",
						"Completeness", "Pleasure", "Intention", "Sense of responsibility", 
						"Support", "Stability")
		);
		
		return featureList;
	}
	
	public static List<String> getFeatureDescriptions() {
		// Note:- Does not include the description for 'Overall video quality'
		List<String> descriptions = new ArrayList<>(
				Arrays.asList("considers the visual quality of the image of a video.",
						"considers the auditory quality of the sound of a video.",
						"considers the duration of a video.",
						"considers the compact representation of a vision.",
						"considers the structured presentation of the content of a video.",
						"considers the presupposed prior knowledge to understand the content of a video.",
						"considers the important core elements, e.g., persons, locations, and entities, which are to be presented in a video.",
						"considers the disrupting and distracting elements, e.g., background actions and noises, that can be inadvertently recorded in a video.",
						"considers the intelligibility of the aspired goals of a vision by all parties involved.",
						"considers the coverage of the three contents of a vision, i.e., problem, solution, and improvement.",
						"considers the enjoyment of watching a video.",
						"considers the intended purpose of a video.",
						"considers the compliance of a video with the legal regulations.",
						"considers the level of acceptance of a vision, i.e., whether all parties involved share the vision.",
						"considers the consistency of a vision over time."
						)
		);
		
		return descriptions;
	}
	
	public static List<String> getSecondaryDescriptions() {
		// Note:- Does not include the secondary description for 'Overall video quality'
		List<String> secDescriptions = new ArrayList<>(
				Arrays.asList("The image of the vision video has a _____ visual quality.",
						"The sound of the vision video has _____ auditory quality.",
						"The length of the vision video feels _____.",
						"The vision video represents the vision in a _____ way.",
						"The vision video has a _____ plot.",
						"Prior knowledge is _____ to understand the vision video.",
						"The vision video contains _____ important core elements.",
						"The vision video contains _____ disrupting and distracting elements.",
						"The vision video presents a vision with _____ aspired goals.",
						"The vision video presents a _____ vision in terms of the considered problem, the proposed solution, and the improvement of the problem due to the solution.",
						"The vision video is _____ to watch.",
						"The vision video is _____ for the intended purpose of the scenario.",
						"The vision video is _____ with the legal regulations.",
						"I _____ that I accept and share the vision presented in the vision video.",
						"The vision video presents a _____ vision."
						)
		);
		
		return secDescriptions;
	}
	
	public static List<String> getScaleLabels() {
		// Note:- Does not include labels 'Overall video quality' (as the values are just 'good'/'bad')
		List<String> scaleLabels = new ArrayList<>(
				Arrays.asList("very good|good|neutral|bad|very bad",
						"very good|good|neutral|bad|very bad",
						"very long|long|neutral|short|very short",
						"very compact|compact|neutral|non-compact|very non-compact",
						"very good|good|neutral|bad|very bad",
						"very necessary|necessary|neutral|unnecessary|very unnecessary",
						"very much|much|neutral|little|very little",
						"very much|much|neutral|little|very little",
						"very intelligible|intelligible|neutral|unintelligible|very unintelligible",
						"very complete|complete|neutral|incomplete|very incomplete",
						"very enjoyable|enjoyable|neutral|unenjoyable|very unenjoyable",
						"very suitable|suitable|neutral|unsuitable|very unsuitable",
						"very compliant|compliant|neutral|non-compliant|very non-compliant",
						"totally agree|agree|neutral|disagree|totally disagree",
						"very stable|stable|neutral|unstable|very unstable"
						)
		);
		
		return scaleLabels;
	}
}
