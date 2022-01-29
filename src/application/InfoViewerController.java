package application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class InfoViewerController {

	@FXML private VBox menuPane;
	@FXML private TabPane tabPane;
	@FXML private Tab demoTab;
	@FXML private VBox demoPane;
	@FXML private Tab assessTab;
	@FXML private VBox assessPane;
	@FXML private Button backButton;
	
	private FXMLLoader fxmlLoader;
	private String expName;
	private String partName;
	private boolean assessFormState;
	private List<String> featureList, descrList, secDescrList, scaleLabels, scaleLabelsList;
	private Map<String, ToggleGroup> map;
	private List<RadioButton> overallRadioButtons;
	private List<String> overallRbLabels;
	private HBox overallRadioBox;
	
	public InfoViewerController(String exp, String part) {
		this.expName = exp;
		this.partName = part;
	}
	
	@FXML
	public void initialize() {
		displayPartInfo();
	}
	
	public void displayPartInfo() {
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
		
		VBox assessVBox = new VBox();

		overallRadioBox = new HBox();
		overallRadioBox.setSpacing(8);
		overallRadioBox.setAlignment(Pos.CENTER_LEFT);
		
		overallRadioButtons = new ArrayList<>();
		overallRbLabels = Arrays.asList("very good", "good", "neutral", "bad", "very bad");
		for(int rbInd = 0; rbInd < 5; rbInd++) {
			RadioButton rb = new RadioButton(overallRbLabels.get(rbInd));
			rb.setDisable(true);
			rb.setStyle("-fx-font-weight: bold;");
			overallRadioButtons.add(rb);
			overallRadioBox.getChildren().add(rb);
		}
		
		ScrollPane scrollPane = new ScrollPane();
		GridPane questionBox = new GridPane();
		
		featureList = QuestionFormController.getFeatureList();
		// Removes the first feature (i.e. "Overall video quality"), as it is already displayed
		featureList.remove(0);
		
		// Gets the other label lists (do not contain values for 'Overall video quality')
		descrList = QuestionFormController.getFeatureDescriptions();
		secDescrList = QuestionFormController.getSecondaryDescriptions();
		scaleLabels = QuestionFormController.getScaleLabels();
		
		map = new HashMap<String, ToggleGroup>();
		
		// Sets toggle group for the overall video quality attribute
		ToggleGroup overallToggleGrp = new ToggleGroup();
		for (RadioButton rb : overallRadioButtons) {
			rb.setToggleGroup(overallToggleGrp);
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
				rb.setDisable(true);
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
		
		scaleLabelsList = QuestionFormController.getScaleLabels();
		
		// Define Border Style for the panes
		String cssBorderLayout = "-fx-border-color: black;\n" +
                   "-fx-border-insets: 5;\n" +
                   "-fx-border-width: 1;\n" +
                   "-fx-border-style: solid;\n";
		
		// The saved .JSON file from which the information is to be parsed
		File demoFile = new File("Experiments/" + expName + "/" + partName + "/demographics.json");
		File extraSetFile = new File("Experiments/" + expName + "/extra_settings.json");
		
		try {
			// Parse data from the respective JSON file and set variables
			
			// Read the JSON file as a string
			String demoJson = new String(Files.readAllBytes(Paths.get(demoFile.getPath())));
			String extraSetJson = new String(Files.readAllBytes(Paths.get(extraSetFile.getPath())));

			// Parse the string into JsonObjects
			JsonObject demo_obj = JsonParser.parseString(demoJson).getAsJsonObject();
			JsonObject extraSet_obj = JsonParser.parseString(extraSetJson).getAsJsonObject();
			
			assessFormState = extraSet_obj.get("assessFormState").getAsBoolean();
			
			if (assessFormState) {
				File assessFile = new File("Experiments/" + expName + "/" + partName + "/assessment.json");
				String assessJson = new String(Files.readAllBytes(Paths.get(assessFile.getPath())));
				JsonObject assess_obj = JsonParser.parseString(assessJson).getAsJsonObject();
				
				// Display assessment info (in the preserved order)
				int featureCount = 0;
				for (String key : featureList) {
					String value = assess_obj.get(key).toString();
					String valueString = value.split("\"")[1];
					
					// Read the feature-values and mark them within the radio buttons
					
					if (!(key.equals("Overall video quality"))) {
						String scaleLabelString = scaleLabelsList.get(featureCount);
						List<String> scaleLabels = Arrays.asList(scaleLabelString.split("\\|"));
						
						int val = Integer.parseInt(valueString);
						
						// Finds the specific scale index corresponding to the selected value
						int valIndex = 0, initVal = 2;
						while (initVal >= -2) {
							if (initVal == val) {
								break;
							}
							initVal--; valIndex++;
						}
						
						String selScaleLabel = scaleLabels.get(valIndex);
						
						ToggleGroup tg = map.get(key);
						int tgInd = 0;
						for(Toggle toggle : tg.getToggles()) {
							// Selects the read value as the new toggle
							if (tgInd == valIndex) {
								tg.selectToggle(toggle);
							}
							tgInd++;
						}
						
						// Sets the value string (with corresponding scale label) for output
						valueString = selScaleLabel + " (" + Integer.toString(val) + ")";
					}
					
					featureCount++;
				}
				
				// Set layout for the elements of assess pane
				HBox ovqEntry = new HBox();
				ovqEntry.setSpacing(8);
				ovqEntry.setAlignment(Pos.CENTER_LEFT);
				
				String value = assess_obj.get("Overall video quality").toString();
				String valueString = value.split("\"")[1];
				
				for (RadioButton rb : overallRadioButtons) {
					String rbLabel = rb.getText();
					if (valueString.equals(rbLabel)) {
						rb.setSelected(true);
					}
				}
				
				Label ovqLabel = new Label("Overall video quality: ");
				ovqLabel.setStyle("-fx-font-weight: bold;");
				Label ovqDescrLabel = new Label("I perceive the overall video quality of the presented video as: ");
				ovqEntry.getChildren().addAll(ovqLabel, ovqDescrLabel, overallRadioBox);
				
				scrollPane.setContent(questionBox);
				assessVBox.setSpacing(8);
				assessVBox.getChildren().addAll(ovqEntry, scrollPane);
				
				assessPane.getChildren().add(assessVBox);
				assessPane.setStyle(cssBorderLayout);
			}
			else {
				assessTab.setDisable(true);
			}
			
			// Display demographics
			SortedSet<String> keys = new TreeSet<>(demo_obj.keySet());
			for (String key : keys) {
				String value = demo_obj.get(key).toString();
				String valueString = value.split("\"")[1];
				
				HBox demoEntry = new HBox();
				demoEntry.setSpacing(8);
				demoEntry.setAlignment(Pos.CENTER_LEFT);
				
				Label keyLabel = new Label(key + ": ");
				keyLabel.setStyle("-fx-font-weight: bold");
				Label valueLabel = new Label(valueString);
				demoEntry.getChildren().addAll(keyLabel, valueLabel);
				demoPane.getChildren().add(demoEntry);
			}
			
			demoPane.setStyle(cssBorderLayout);
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void backButtonClicked() {
		switchToMainWindow();
	}
	
	public void switchToMainWindow() {
		// switch scene to MWC
		Stage stage = (Stage) backButton.getScene().getWindow();
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
}
