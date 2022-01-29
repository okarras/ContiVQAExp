package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.hildan.fxgson.FxGson;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import application.ExperimentController.Experiment;
import application.ExperimentController.ExtraSettings;
import application.ExperimentController.RB;
import application.ExperimentController.SL;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindowController {
	
	@FXML private VBox menuPane;
	@FXML private Button addExperimentButton;
	@FXML private Button addParticipantButton;
	@FXML private Button exportButton;
	@FXML private Button closeButton;
	@FXML private ListView<String> expListView;
	@FXML private ListView<String> partListView;
	
	private FXMLLoader fxmlLoader;
	private ExperimentController expCtr;
	private UserInfoController userInfoCtr;
	// folders on the experiment level that are to be ignored
	private List<String> ignoreFolders = Arrays.asList("Snapshots", "Export");
	
	@FXML
	public void initialize() {
		// Set MenuBar
		try {
			CustomMenuBar customMenuBar = new CustomMenuBar();
			fxmlLoader = new FXMLLoader(getClass().getResource("MenuBar.fxml"));
        	fxmlLoader.setController(customMenuBar);
        	Parent root = (Parent) fxmlLoader.load();
        	menuPane.getChildren().clear();
        	menuPane.getChildren().add(root);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Create experiments folder if not present
		File exp = new File("Experiments");
		if (!exp.exists()) {
			exp.mkdir();
		}
		else {
			// display all added experiments in the list view
			for (File exp_file : exp.listFiles()) {
		        if (exp_file.isDirectory()) {
		        	expListView.getItems().add(exp_file.getName());
		        }
		    }
		}
		
		// customize the individual cells of the Experiment List View
		expListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem view = new MenuItem("View");
            MenuItem showAllData = new MenuItem("Show All Data");
            MenuItem export = new MenuItem("Export");
            MenuItem delete = new MenuItem("Delete");
    		
            // define the context menu properties (for ex. 'view', 'delete')
            
            view.setOnAction(event -> {
            	viewExperiment(cell.getText());
            });
            
            showAllData.setOnAction(event -> {
            	int part_num = partListView.getItems().size();
            	if (part_num > 0) {
            		showData(cell.getText(), "");
            	}
            });
            
            export.setOnAction(event -> {
            	exportData(cell.getText());
            });
            
    		delete.setOnAction(event -> {
    			String exp_name = cell.getText();
    			for (File exp_file : exp.listFiles()) {
    				if (exp_file.isDirectory() && exp_file.getName().equals(exp_name)) {
    					// open alert box to confirm deletion
    					Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to delete '" + exp_file.getName() + "'?", 
    							ButtonType.YES, ButtonType.NO);
    					alert.showAndWait();

    					if (alert.getResult() == ButtonType.YES) {
    					    // delete folder and all its contents
    						deleteFolder(exp_file);
        					expListView.getItems().remove(exp_file.getName());
        					reloadMainWindow();
    					}
    					break;
    				}
    			}
    		});
    		
    		contextMenu.getItems().add(view);
    		contextMenu.getItems().add(showAllData);
    		contextMenu.getItems().add(export);
    		contextMenu.getItems().add(delete);
    		cell.textProperty().bind(cell.itemProperty());
            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            
            return cell;
        });
		
		// change to view mode upon double-clicking an experiment
		expListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent event) {
		    	if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
		    		try {
		    			String selectedExp = expListView.getSelectionModel().getSelectedItem();
			    		if (!selectedExp.equals(null))
			    			viewExperiment(selectedExp);
		    		} catch (NullPointerException e) {
		    			// handle null pointer exception by throwing a warning
		    			System.out.println("Warning: double-click on existing experiments to view info (and not on empty cells).");
		    		}
		    	}
		    }
		});
		
		// disable the add participant button on default
		addParticipantButton.setDisable(true);
		exportButton.setDisable(true);
		
		// when an experiment is selected
		expListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (!expListView.getSelectionModel().isEmpty()) {
				// enable the add participant button if any of the experiments are selected
				addParticipantButton.setDisable(false);
				exportButton.setDisable(false);
				
				// displays the participants of the selected experiment
				String selectedExp = expListView.getSelectionModel().getSelectedItem();
				File expFolder = new File("Experiments/" + selectedExp);
				partListView.getItems().clear();
				if (expFolder.exists()) {
					// display all added participants in the list view
					for (File partFolder : expFolder.listFiles()) {
				        if (partFolder.isDirectory() && !(ignoreFolders.contains(partFolder.getName()))) {
				        	partListView.getItems().add(partFolder.getName());
				        }
				    }
				}
			}
	    });
		
		// customize the individual cells of the Participant List View
		partListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem viewInfo = new MenuItem("View Info");
            MenuItem showData = new MenuItem("Show Data");
            MenuItem delete = new MenuItem("Delete");
            
            viewInfo.setOnAction(event -> {
            	String selectedExp = expListView.getSelectionModel().getSelectedItem();
            	viewParticipant(selectedExp, cell.getText());
            });
            
            showData.setOnAction(event -> {
            	String selectedExp = expListView.getSelectionModel().getSelectedItem();
            	showData(selectedExp, cell.getText());
            });
            
            // define the context menu properties (for ex. 'view', 'delete')           
    		delete.setOnAction(event -> {
    			String part_name = cell.getText();
    			File partPath = new File("Experiments/" + 
    					expListView.getSelectionModel().getSelectedItem() +
    					"/" + part_name);
    			
    			// open alert box to confirm deletion
				Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to delete '" + part_name + "'?", 
						ButtonType.YES, ButtonType.NO);
				alert.showAndWait();

				if (alert.getResult() == ButtonType.YES) {
				    // delete folder and all its contents
					deleteFolder(partPath);
					partListView.getItems().remove(part_name);
				}
    		});
    		
    		contextMenu.getItems().add(viewInfo);
    		contextMenu.getItems().add(showData);
    		contextMenu.getItems().add(delete);
    		cell.textProperty().bind(cell.itemProperty());
            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            
            return cell;
        });
		
		// change to view mode upon double-clicking a participant
		partListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent event) {
		    	if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
		    		try {
		    			String selectedExp = expListView.getSelectionModel().getSelectedItem();
		    			String selectedPart = partListView.getSelectionModel().getSelectedItem();
			    		if (!selectedPart.equals(null))
			    			viewParticipant(selectedExp, selectedPart);
		    		} catch (NullPointerException e) {
		    			// handle null pointer exception by throwing a warning
		    			System.out.println("Warning: double-click on existing participants to view info (and not on empty cells).");
		    		}
		    	}
		    }
		});
	}
	
	@FXML
	private void addExperiment() {
	    Stage stage = (Stage) addExperimentButton.getScene().getWindow();
	    
		Parent root;
		try {
			fxmlLoader = new FXMLLoader(getClass().getResource("ExperimentWindow.fxml"));
        	root = fxmlLoader.load();
			
        	expCtr = (ExperimentController) fxmlLoader.getController();
        	
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// displays the settings of the already existing experiment in a non-editable format
	private void viewExperiment(String exp_name) {
		addExperiment();
		expCtr.changeToViewMode(exp_name);
	}
	
	@FXML
	private void addParticipant() {
	    Stage stage = (Stage) addParticipantButton.getScene().getWindow();
	    
		Parent root;
		try {
			userInfoCtr = new UserInfoController(expListView.getSelectionModel().getSelectedItem());
			
			fxmlLoader = new FXMLLoader(getClass().getResource("UserInfoWindow.fxml"));
			fxmlLoader.setController(userInfoCtr);
        	root = fxmlLoader.load();
        	
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// displays the settings of the already existing participant in a non-editable format
	private void viewParticipant(String exp_name, String part_name) {
		Stage stage = (Stage) addParticipantButton.getScene().getWindow();
		Parent root;
		try {
        	String exp = expListView.getSelectionModel().getSelectedItem();
        	String part = partListView.getSelectionModel().getSelectedItem();
        	
        	InfoViewerController infoViewerCtr = new InfoViewerController(exp, part);
        	fxmlLoader = new FXMLLoader(getClass().getResource("InfoViewerWindow.fxml"));
        	fxmlLoader.setController(infoViewerCtr);
        	root = fxmlLoader.load();
        	
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void exportData(String exp_name) {
		// export recorded data of all participants of an experiment as a .csv file
		
		try {
			String exportPath = "Experiments/" + exp_name + "/Export";
			File file = new File(exportPath);
			if (!file.exists()) {
				file.mkdir();
			}
			
			
			// PART 1 (SETTINGS)
			
			FileWriter csvWriter = new FileWriter(exportPath + "/settings.csv");
			
			// create a custom FX-GSON builder
			Gson fxGson = FxGson.fullBuilder()  
	                .setPrettyPrinting()
	                .create();
			// the saved .JSON file from which the information is to be parsed
			File settingsFile = new File("Experiments/" + exp_name + "/settings.json");
			File extraSettingsFile = new File("Experiments/" + exp_name + "/extra_settings.json");
			
			// parse data from the respective JSON file and set variables
			// read the JSON file as a string
			String json = new String(Files.readAllBytes(Paths.get(settingsFile.getPath())));
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
			
			// Write attributes to the Excel file
			csvWriter.append("Name," + name + "\n");
			csvWriter.append("Video," + video + "\n");
			csvWriter.append("Display," + display + "\n");
			
			// implement switch case: depending upon the selected display type
			if (display.equals("Slider")) {
				SL sl = fxGson.fromJson(set_obj, SL.class);
				csvWriter.append("Min Value," + sl.getMinValue() + "\n");
				csvWriter.append("Max Value," + sl.getMaxValue() + "\n");
				csvWriter.append("Min Label," + sl.getMinLabel() + "\n");
				csvWriter.append("Max Label," + sl.getMaxLabel() + "\n");
				csvWriter.append("Interval Type," + sl.getIntervalType() + "\n");
			}
			else if (display.equals("Radio Buttons")) {
				RB rb = fxGson.fromJson(set_obj, RB.class);
				
				String rbString = "";
				int cnt = 1;
				for (String rbLabel: rb.getLabels()) {
					rbString += rbLabel + " (" + cnt + ")";
					if (cnt < rb.getLabels().length)
						rbString += " | ";
					cnt++;
				}
				csvWriter.append("Labels," + rbString + "\n");
				csvWriter.append("Min Label," + rb.getMinLabel() + "\n");
				csvWriter.append("Max Label," + rb.getMaxLabel() + "\n");
			}
			else {
				System.out.println("ERROR: Display type not recognizable.");
			}
			
			csvWriter.append("Assessment," + (hasAssessForm ? "Present" : "Not Present") + "\n");
			
			String demoString = "";
			int cnt = 1;
			for (String demoLabel: demoLabels) {
				demoString += demoLabel;
				if (cnt < demoLabels.length)
					demoString += " | ";
				cnt++;
			}
			csvWriter.append("Demo Labels," + demoString + "\n\n");
			
			csvWriter.flush();
			csvWriter.close();
			
			
			// PART 2 (Demographics)
			
			csvWriter = new FileWriter(exportPath + "/demographics.csv");
			
			List<String> partIDs = new ArrayList<>();
        	File expFolder = new File("Experiments/" + exp_name);
        	
	        csvWriter.append("ID");
	        demoString = "";
			for (String demoLabel: demoLabels) {
				if (!demoLabel.equals("ID")) {
					demoString += "," + demoLabel;
				}
			}
			csvWriter.append(demoString + "\n");
        	
        	File[] expFiles = expFolder.listFiles();
	        for (File f : expFiles) {
		        if (f.isDirectory() && !(ignoreFolders.contains(f.getName()))) {
	            	String partID = f.getName();
	            	// Add the participant ID into the list for later use
	            	partIDs.add(partID);
	            	
	            	String demoFileName = "Experiments/" + exp_name + "/" + partID + "/demographics.json";
	            	
	            	// the saved .JSON file from which the information is to be parsed
					File demoFile = new File(demoFileName);
					
					// parse data from the respective JSON file and set variables
					// read the JSON file as a string
					String demoJson = new String(Files.readAllBytes(Paths.get(demoFile.getPath())));
					// parse the string into JsonObjects
					JsonObject demo_obj = JsonParser.parseString(demoJson).getAsJsonObject();
					
					csvWriter.append(partID);
					demoString = "";
					for (String demoLabel: demoLabels) {
						if (!demoLabel.equals("ID")) {
							String value = demo_obj.get(demoLabel).getAsString();
							demoString += "," + value;
						}
					}
					csvWriter.append(demoString + "\n");
            	}
            }
	        csvWriter.append("\n");
	        
	        csvWriter.flush();
	        csvWriter.close();
	        
			
			// PART 3 (Data)
	        
	        csvWriter = new FileWriter(exportPath + "/data.csv");
	        csvWriter.append("ID,Time(in s),Value ");
	        if (display.equals("Radio Buttons")) {
	        	RB rb = fxGson.fromJson(set_obj, RB.class);
				String rbString = "";
				cnt = 1;
				for (String rbLabel: rb.getLabels()) {
					rbString += cnt + "=" + rbLabel;
					if (cnt < rb.getLabels().length)
						rbString += " | ";
					cnt++;
				}
	        	csvWriter.append("(" + rbString + ")");
	        }
	        csvWriter.append("\n");
	        
			for (String partID : partIDs) {
				String dataFile = "Experiments/" + exp_name + "/" + partID + "/data.csv";
				@SuppressWarnings("resource")
				BufferedReader br = new BufferedReader(new FileReader(dataFile));
				String line;
			    cnt = 0;
			    while ((line = br.readLine()) != null) {
			    	if (cnt > 0) {
				        String[] values = line.split(",");
				        int time = Integer.parseInt(values[0]);
				        String value_str = values[1];
				        
				        Number value;
				        if (display.equals("Radio Buttons")) {
				            value = Integer.parseInt(value_str);
				        }
				        else {
				        	value = Double.parseDouble(values[1]);
				        }
				        
				        csvWriter.append(partID + "," + time + "," + value + "\n");
			    	}
			        cnt++;
			    }
			}
			csvWriter.append("\n");
			
			csvWriter.flush();
			csvWriter.close();
			
			
			// PART 4 (Assessment)
			
			csvWriter = new FileWriter(exportPath + "/assessment.csv");
			
			if (hasAssessForm) {
				List<String> featureList = QuestionFormController.getFeatureList();
				// Removes the first feature (i.e. "Overall video quality"), as it is already displayed
				featureList.remove(0);
				
		        csvWriter.append("ID,Characteristic,Value\n");
				
				for (String partID : partIDs) {
					String assessFileName = "Experiments/" + exp_name + "/" + partID + "/assessment.json";
					// the saved .JSON file from which the information is to be parsed
					File assessFile = new File(assessFileName);
					
					// parse data from the respective JSON file and set variables
					// read the JSON file as a string
					String assessJson = new String(Files.readAllBytes(Paths.get(assessFile.getPath())));
					// parse the string into JsonObjects
					JsonObject assess_obj = JsonParser.parseString(assessJson).getAsJsonObject();
					
					String ovq = assess_obj.get("Overall video quality").getAsString();
					csvWriter.append(partID + ",Overall video quality," + ovq + "\n");
					
					ListIterator<String> listIter = featureList.listIterator();
					while(listIter.hasNext()) {
						String feature = listIter.next();
						String value = assess_obj.get(feature).getAsString();
						csvWriter.append(partID + "," + feature + "," + value + "\n");
					}
				}
				csvWriter.append("\n");
			}
			
			csvWriter.flush();
			csvWriter.close();
			
			
			// PART 5 (Comments)
			
			csvWriter = new FileWriter(exportPath + "/comments.csv");
	        csvWriter.append("Source,Snapshot,Timestamp,Comment\n");
	        
	        String commentFileName = "/Snapshots/comments.json";
	        
	        // List of all JSON files containing comments
	        List<File> commentFiles = new ArrayList<>();
	        
	        // Gets all comment files from the Global (Experimenter) Level
	        File globalCommentsFile = new File("Experiments/" + exp_name + commentFileName);
	        if (globalCommentsFile.exists()) {
	        	commentFiles.add(globalCommentsFile);
	        }
			
	        // Gets all comment files from the Local (Participant) Levels
	        for (String partID : partIDs) {
	        	File localCommentsFile = new File("Experiments/" + exp_name + "/" + partID + commentFileName);
		        if (localCommentsFile.exists()) {
		        	commentFiles.add(localCommentsFile);
		        }
	        }
	        
	        // Iterate through all comment files to get the stored comments
	        for (File commentFile: commentFiles) {
	        	// Gets the JSON data in string format from the comments JSON file
				String commentJson = new String(Files.readAllBytes(Paths.get(commentFile.getPath())));
				
				// Reads and parses the string into a JsonArray
				JsonReader jsonReader = new JsonReader(new StringReader(commentJson));
				jsonReader.setLenient(true);
				
				JsonArray commentJsonArray = JsonParser.parseReader(jsonReader).getAsJsonArray();
				
				// Iterate through all comment entries within a file
				for (int i = 0; i < commentJsonArray.size(); i++) {
					JsonObject commentJsonObject = commentJsonArray.get(i).getAsJsonObject();
					
					String sourceValue = commentJsonObject.get("source").getAsString();
					String snapshotValue = commentJsonObject.get("snapshot").getAsString();
					String timestampValue = commentJsonObject.get("timestamp").getAsString();
					String commentValue = commentJsonObject.get("comment").getAsString();
					
					// Writes the extracted comment attributes to the export file
					csvWriter.append(sourceValue + "," + snapshotValue + "," + timestampValue + "," + commentValue + "\n");
				}
	        }
	        
			
			csvWriter.flush();
			csvWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void showData(String exp_name, String part_name) {
		switchToDataGraphWindow(exp_name, part_name);
	}
	
	private void switchToDataGraphWindow(String exp_name, String part_name) {
	    Stage stage = (Stage) addExperimentButton.getScene().getWindow();
	    
		Parent root;
		try {
			DataGraphController dataGraphCtr = new DataGraphController(exp_name, part_name);
			
			fxmlLoader = new FXMLLoader(getClass().getResource("DataGraphWindow.fxml"));
        	fxmlLoader.setController(dataGraphCtr);
        	root = fxmlLoader.load();
        	
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void reloadMainWindow() {
		// switch scene to MWC
		Stage stage = (Stage) closeButton.getScene().getWindow();
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
	private void exportButtonClicked() {
		String expName = expListView.getSelectionModel().getSelectedItem();
		exportData(expName);
	}
	
	@FXML
	private void quitButtonClicked() {
	    Stage stage = (Stage) closeButton.getScene().getWindow();
	    stage.close();
	}
	
	// delete a folder with contents
	public static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if (files != null) {
	        for (File f : files) {
	            if (f.isDirectory()) 
	            	deleteFolder(f);
	            else 
	            	f.delete();
	        }
	    }
	    folder.delete();
	}
}
