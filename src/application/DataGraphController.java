package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.hildan.fxgson.FxGson;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import application.ExperimentController.Experiment;
import application.ExperimentController.RB;
import application.ExperimentController.SL;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DataGraphController {
	
	@FXML private VBox menuPane;
	@FXML private VBox videoView;
	@FXML private Button backButton;
	@FXML LineChart<Number, Number> graph;
	@FXML private VBox screenshotBox;
	@FXML private AnchorPane paginationPane;
	@FXML private HBox commentBox;
	@FXML private HBox comboView;
	@FXML private ComboBox<String> graphComboBox;
	
	private FXMLLoader fxmlLoader;
	private String expName;
	private String partName;
	private Gson fxGson;
	private List<String> allDataFiles;
	private MediaController mediaCtr;
	private MediaPlayer mediaPlayer;
	private Pagination pagination;
	private List<File> snapshots;
	private String snapshotPath;
	private List<String> snapshotNames;
	private ComboBox<String> sourceComboBox;
	private List<String> sourceList;
	private TextArea commentField;
	private Button saveButton;
	private NumberAxis yAxis;
	private boolean showsAllData = false;
	// number of participants that can be displayed in the legend
	private int legendDisplayLimit = 5;
	// folders on the experiment level that are to be ignored
	private List<String> ignoreFolders = Arrays.asList("Snapshots", "Export");
	
	public DataGraphController(String exp, String part) {
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
        String mediaFilePath = RecorderController.findVideoFile(expName);
        try {
			String media_url = (new File(mediaFilePath)).toURI().toURL().toString();
	        
	        Media media = new Media(media_url);
	        mediaPlayer = new MediaPlayer(media);
	        mediaPlayer.setAutoPlay(true);
	        
	        mediaCtr = new MediaController(mediaPlayer);
	        mediaCtr.setViewMode(true);
	        
	        TitledPane titledPane = new TitledPane(mediaFilePath, mediaCtr);
	        titledPane.setCollapsible(false);
	        
	        videoView.getChildren().clear();
	        videoView.getChildren().add(titledPane);
	        
        } catch (MalformedURLException e) {
			e.printStackTrace();
		}
        
        allDataFiles = new ArrayList<>();
        if (partName.equals("")) {
        	showsAllData = true;
        	File expFolder = new File("Experiments/" + expName);
        	File[] expFiles = expFolder.listFiles();
	        for (File f : expFiles) {
	        	if (f.isDirectory() && !(ignoreFolders.contains(f.getName()))) {
	            	String dataFileName = "Experiments/" + expName + "/" + f.getName() + "/data.csv";
	            	allDataFiles.add(dataFileName);
            	}
            }
        }
        else {
        	allDataFiles.add("Experiments/" + expName + "/" + partName + "/data.csv");
        }
        
        // Pass experiment and participant params for snapshot functionality
        mediaCtr.setShowAllData(showsAllData);
        mediaCtr.setExpName(expName);
        mediaCtr.setPartName(partName);
        
        // Display combo box only if all data (for all participants) shown
        if (showsAllData) {
        	comboView.setVisible(true);
        	
        	graphComboBox.getItems().clear();
        	graphComboBox.getItems().addAll("All", "Mean", "Median", "Standard Deviation");
        	graphComboBox.getSelectionModel().select("All");
        	
        	// Change the graph data w.r.t. the selected mode
        	graphComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
    	    	graph.getData().clear();
    	    	if (newValue.equals("Mean")) {
    	    		meanMode();
    	    	}
    	    	else if (newValue.equals("Median")) {
    	    		medianMode();
    	    	}
    	    	else if (newValue.equals("Standard Deviation")) {
    	    		standardDeviationMode();
    	    	}
    	    	else {
    	    		allMode();
    	    	}
    	    });
        } else {
        	comboView.getChildren().clear();
        }
        
        // Resets the upper bound and intervals of the Y-Axis
        yAxis = (NumberAxis) graph.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setUpperBound(getMaxValue());
        yAxis.setTickUnit(1);
        
        // Display all data on the graph as default
        allMode();
        
    	// Pagination control
    	createPaginator();
    	
	}
	
	public void allMode() {
		int totalPartNum = allDataFiles.size();
		if (totalPartNum > legendDisplayLimit) {
			// Hides the legend if the number of the participants involved
			// is more than the display limit
			graph.setLegendVisible(false);
		}
		
		for (String dataFile : allDataFiles) {
    		// Extract data from an Excel file
    		XYChart.Series<Number, Number> data = getDataFromExcelFile(dataFile);
    		String partName = dataFile.split("/")[2];
    		data.setName(partName);
    		graph.getData().add(data);
    		
    		setDataNodesOnClick(data);	// Add mouse click listeners to nodes
		}
	}
	
	public void meanMode() {
		// Legend is always visible in this mode
		graph.setLegendVisible(true);
		
		Map<Number, List<Number>> dataListMap = getDataListMap();
		XYChart.Series<Number, Number> meanData = new XYChart.Series<>();
		
		for (Number key : dataListMap.keySet()) {
			List<Number> valueList = dataListMap.get(key);
			double sum = 0;
			double length = valueList.size();
			for (int i = 0; i < valueList.size(); i++) {
				Number value = valueList.get(i);
				if (value instanceof Integer) {
					String temp = value.toString();
					sum += Double.parseDouble(temp);
				}
				else {
					sum += (double) value;
				}
			}
			
			double mean = sum/length;
			Data<Number, Number> data_entry = new Data<>(key, mean);
			meanData.getData().add(data_entry);
		}
		
		meanData.setName("Mean");
		graph.getData().add(meanData);
		setDataNodesOnClick(meanData);	// Add mouse click listeners to nodes
	}
	
	public void medianMode() {
		// Legend is always visible in this mode
		graph.setLegendVisible(true);
		
		Map<Number, List<Number>> dataListMap = getDataListMap();
		XYChart.Series<Number, Number> medianData = new XYChart.Series<>();
		
		for (Number key : dataListMap.keySet()) {
			List<Number> valueList = dataListMap.get(key);
			
			double median = 0.0;
			int middleIndex = valueList.size() / 2;
			
			Number middleValue = valueList.get(middleIndex);
	    	if (middleValue instanceof Integer) {
				String temp = middleValue.toString();
				median = Double.parseDouble(temp);
			}
			else {
				median = (double) middleValue;
			}
			
		    if ((valueList.size() % 2) == 0) {
		    	double smallerMedian = 0.0;
		    	Number smallerMiddleValue = valueList.get(middleIndex-1);
		    	
		    	if (smallerMiddleValue instanceof Integer) {
					String temp = smallerMiddleValue.toString();
					smallerMedian = Double.parseDouble(temp);
				}
				else {
					smallerMedian = (double) smallerMiddleValue;
				}
		    	
		    	median = (double) (smallerMedian + median)/2.0;
		    }
			
			Data<Number, Number> data_entry = new Data<>(key, median);
			medianData.getData().add(data_entry);
		}
		
		medianData.setName("Median");
		graph.getData().add(medianData);
		setDataNodesOnClick(medianData);	// Add mouse click listeners to nodes
	}
	
	public void standardDeviationMode() {
		// Legend is always visible in this mode
		graph.setLegendVisible(true);
				
		Map<Number, List<Number>> dataListMap = getDataListMap();
		XYChart.Series<Number, Number> stdDevData = new XYChart.Series<>();
		
		XYChart.Series<Number, Number> plusData = new XYChart.Series<>();
		XYChart.Series<Number, Number> minusData = new XYChart.Series<>();
		
		double maxValue = getMaxValue();
		
		for (Number key : dataListMap.keySet()) {
			List<Number> valueList = dataListMap.get(key);
			double sum = 0;
			double stdDev = 0.0;
			double length = valueList.size();
			for (int i = 0; i < valueList.size(); i++) {
				Number value = valueList.get(i);
				if (value instanceof Integer) {
					String temp = value.toString();
					sum += Double.parseDouble(temp);
				}
				else {
					sum += (double) value;
				}
			}
			
			double mean = sum/length;
			for (int i = 0; i < valueList.size(); i++) {
				double num = 0.0;
				Number value = valueList.get(i);
				if (value instanceof Integer) {
					String temp = value.toString();
					num = Double.parseDouble(temp);
				}
				else {
					num = (double) num;
				}
				stdDev += Math.pow(num - mean, 2);
			}
			
			stdDev = Math.sqrt(stdDev/length);
			
			Data<Number, Number> data_entry_plus = new Data<>(key, ((mean + stdDev) < maxValue) ? (mean + stdDev) : maxValue);
			Data<Number, Number> data_entry_mean = new Data<>(key, mean);
			Data<Number, Number> data_entry_minus = new Data<>(key, (mean > stdDev) ? (mean - stdDev) : 0.0);

			plusData.getData().add(data_entry_plus);
			stdDevData.getData().add(data_entry_mean);
			minusData.getData().add(data_entry_minus);
		}
		
		plusData.setName("+ S.D.");
		stdDevData.setName("Mean");
		minusData.setName("- S.D.");
		graph.getData().add(plusData);
		graph.getData().add(stdDevData);
		graph.getData().add(minusData);
		setDataNodesOnClick(plusData); // Add mouse click listeners to nodes
		setDataNodesOnClick(stdDevData);
		setDataNodesOnClick(minusData);
	}
	
	public Map<Number, List<Number>> getDataListMap() {
		Map<Number, List<Number>> dataListMap = new HashMap<>();
		for (String dataFile : allDataFiles) {
    		// Extract data from an Excel file
			XYChart.Series<Number, Number> data = getDataFromExcelFile(dataFile);
			
			ListIterator<Data<Number, Number>> li = data.getData().listIterator();
			while (li.hasNext()) {
				Data<Number, Number> data_elem = li.next();
				Number key = data_elem.getXValue();
				Number value = data_elem.getYValue();
				
				List<Number> valueList = new ArrayList<>();
				if (dataListMap.containsKey(key)) {
					valueList = dataListMap.get(key);
				}
				valueList.add(value);
				
				dataListMap.put(key, valueList);
			}
		}
		
		return dataListMap;
	}
	
	public void setDataNodesOnClick(XYChart.Series<Number, Number> data) {
		// Add on mouse clicked listeners to all nodes
	    for (final XYChart.Data<Number, Number> data_entry : data.getData()) {
	    	data_entry.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
	    		
	    		@Override
	    		public void handle(MouseEvent event) {
	    			int x = (int) data_entry.getXValue();
	    			
	    			// restart the video
	    			boolean metEoM = false;
	    			if (mediaCtr.isAtEndOfMedia()) {
	    				mediaCtr.setAtEndOfMedia(false);
	    				metEoM = true;
	    			}
	    			mediaPlayer.seek(Duration.seconds(x));
	    			if (metEoM) {
	    				mediaPlayer.pause();
	    				mediaCtr.setStopRequested(false);
	    			}
	    		}
	    	});
	    }
	}
 
    public VBox createPage(int pageIndex) {        
        VBox box = new VBox(5);
        
        if (snapshots.size() > pageIndex) {
	        String filePath = snapshots.get(pageIndex).getPath();

	        // Gets the JSON data from the comments file (at the given snapshot path)
			JsonArray commentJsonArray = getCommentsJsonData(snapshotPath);
	        // Iterates through the array to find and display the comment for the active snapshot
			String activeSnapshotName = snapshotNames.get(pageIndex);
			String activeComment = "";
			String activeSource = sourceList.get(0);
			for (int i = 0; i < commentJsonArray.size(); i++) {
				JsonObject tempJsonObject = commentJsonArray.get(i).getAsJsonObject();
				String tempSnapshotName = tempJsonObject.get("snapshot").getAsString();
				String tempSource = tempJsonObject.get("source").getAsString();
				
				if (tempSnapshotName.equals(activeSnapshotName) && tempSource.equals(activeSource)) {
					activeComment = tempJsonObject.get("comment").getAsString();
					break;
				}
			}
			
			// Sets the active comment to the comment field below the active snapshot
			commentField.setText(activeComment);
			// Sets the active source to the combo box below the active snapshot
			sourceComboBox.getSelectionModel().select(activeSource);

			try {
				FileInputStream input = new FileInputStream(filePath);
				Image img = new Image(input);
		        ImageView imgView = new ImageView(img);
		        
		        // Get primary screen bounds
		        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
		        
		        // Determine the size of the pagination control
		        double imgHeight = screenBounds.getHeight() / 3.0;
		        double imgWidth = screenBounds.getWidth() / 3.0;
		        
		        // Resize video
		        imgView.setFitHeight(imgHeight);	// Safe value:- 720
		        imgView.setFitWidth(imgWidth);	// Safe value:- 1200
		        
		        box.getChildren().add(imgView);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
        }
        
        return box;
    }
    
	public void createPaginator() {
		// Get list of saved snapshots
		snapshots = new ArrayList<>();
		// Create Snapshots folder if not present
		snapshotPath = "";
		if (showsAllData) {
			snapshotPath = "Experiments/" + expName + "/Snapshots";
		}
		else {
			snapshotPath = "Experiments/" + expName + "/" + partName + "/Snapshots";
		}
		
		File file = new File(snapshotPath);
		if (!file.exists()) {
			file.mkdir();
		}
		else {
			// extract all snapshots from folder
			for (File snapshot : file.listFiles()) {
				if (snapshot.getName().endsWith(".png")) {
					snapshots.add(snapshot);
				}
		    }
		}
        
		int pageNum = (snapshots.isEmpty()) ? 1 : snapshots.size();
        pagination = new Pagination(pageNum, 0);
        pagination.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
        pagination.setPageFactory((Integer pageIndex) -> createPage(pageIndex));
        
        AnchorPane.setTopAnchor(pagination, 10.0);
        AnchorPane.setRightAnchor(pagination, 10.0);
        AnchorPane.setBottomAnchor(pagination, 10.0);
        AnchorPane.setLeftAnchor(pagination, 10.0);
        
        if (snapshots.size() > 0) {
        	paginationPane.getChildren().addAll(pagination);
        	
        	// Create a custom FX-GSON builder
			fxGson = FxGson.fullBuilder()
	                .setPrettyPrinting()
	                .create();
        	
        	// Screenshot Comment functionality: comments can be added to active screenshots
        	
        	// Comment box design
			sourceComboBox = new ComboBox<>();
			sourceComboBox.setTooltip(new Tooltip("Comment Source"));
			sourceComboBox.setVisibleRowCount(5);
			sourceComboBox.getItems().clear();
			// Prepares the list of possible sources for the source combo box
			sourceList = new ArrayList<>();
			if (showsAllData) {	// Experimenter Level
				// Sets 'Experimenter' as the first option (selected by default)
				sourceList.add("Experimenter");
				// Sets the individual participant titles (for this experiment) as the remaining options
				for (String dataFile : allDataFiles) {
		    		String tempPartName = dataFile.split("/")[2];
		    		sourceList.add(tempPartName);
				}
			} else {	// Participant Level
				// Sets the active participant name as the first option (selected by default)
				sourceList.add(partName);
				// Sets 'Experimenter' as the second option
				sourceList.add("Experimenter");
			}
			sourceComboBox.getItems().addAll(sourceList);
			sourceComboBox.getSelectionModel().select(sourceList.get(0));
			
			// Implementing Selection Change Listener for Source Combo Box to update comment
			sourceComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
				String newComment = "";
				
				int currentPageIndex = pagination.getCurrentPageIndex();
    			String activeSnapshotName = snapshotNames.get(currentPageIndex);
    			String newSource = newValue;
        		
        		// Gets the JSON data from the comments file (at the given snapshot path)
    			JsonArray commentJsonArray = getCommentsJsonData(snapshotPath);
    			
    			// Iterates through the stored comments to find the comment corresponding 
    			// to the changed data 
    			for (int i = 0; i < commentJsonArray.size(); i++) {
    				JsonObject tempJsonObject = commentJsonArray.get(i).getAsJsonObject();
    				String tempSnapshotName = tempJsonObject.get("snapshot").getAsString();
    				String tempSource = tempJsonObject.get("source").getAsString();
    				
    				// Gets the comment for the active snapshot and the changed source from the JSON array
    				if (tempSnapshotName.equals(activeSnapshotName) && tempSource.equals(newSource)) {
    					newComment = tempJsonObject.get("comment").getAsString();
    					break;
    				}
    			}
						
				// Sets the newly retrieved comment in its field	
				commentField.setText(newComment);
			});
			
        	commentField = new TextArea();
        	commentField.setPromptText("Enter comment here for the active screenshot..");
        	commentField.setPrefRowCount(1);
        	HBox.setHgrow(commentField, Priority.ALWAYS);
        	saveButton = new Button("Save Comment");
        	// Listener to update modified comments in their respective JSON entries (upon button click)
        	saveButton.setOnAction(e -> {
        		String inputComment = commentField.getText();
        		String inputSource = sourceComboBox.getValue();
        		int currentPageIndex = pagination.getCurrentPageIndex();
        		
        		// Gets the JSON data from the comments file (at the given snapshot path)
    			JsonArray commentJsonArray = getCommentsJsonData(snapshotPath);
    			
    			// Iterates through the array to update the comment for the active snapshot
    			String activeSnapshotName = snapshotNames.get(currentPageIndex);
    			for (int i = 0; i < commentJsonArray.size(); i++) {
    				JsonObject tempJsonObject = commentJsonArray.get(i).getAsJsonObject();
    				String tempSnapshotName = tempJsonObject.get("snapshot").getAsString();
    				String tempSource = tempJsonObject.get("source").getAsString();
    				
    				// Updates the comment for the active snapshot and source in the JSON array
    				if (tempSnapshotName.equals(activeSnapshotName) && tempSource.equals(inputSource)) {
    					commentJsonArray.get(i).getAsJsonObject().addProperty("comment", inputComment);
    					break;
    				}
    			}
    			
    			// Writes the modified JSON Array data to the comments JSON file
				try {
					// Extracts the data from the JSON array
					String updatedCommentJson = fxGson.toJson(commentJsonArray);
					// Writes the JSON data into the comments file
					FileWriter fw = new FileWriter(new File(snapshotPath + "/comments.json"));
					fw.write(updatedCommentJson);
					fw.flush();
					fw.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
        	});
        	commentBox.getChildren().addAll(sourceComboBox, commentField, saveButton);
        	commentBox.setSpacing(10);
        	
        	// Create/Load comments.csv file
        	String commentFilePath = snapshotPath + "/comments.json";
            File commentFile = new File(commentFilePath);
            
			// Gets list of all snapshot base names (without extension)
			snapshotNames = new ArrayList<>();
			for (File snapshot : snapshots) {
				String snapshotFileName = snapshot.getName();
				String snapshotName = snapshotFileName.substring(0, snapshotFileName.lastIndexOf('.'));
				
				snapshotNames.add(snapshotName);
			}
            
            if (!commentFile.exists()) {	// Comments JSON File not found
            	try {
            		// Creates new file if not already present
					commentFile.createNewFile();
					
					// Iterates through the snapshots present and
					// initializes their respective comment entries in the array
					JsonArray jsonArray = new JsonArray();
					for (String snapshotName : snapshotNames) {
						// Getting time-stamp from snapshot name
						String timestamp = snapshotName.split("_")[2];
						timestamp = timestamp.replace('.', ':');
						
						// Adding multiple comments per snapshot (i.e. for each source)
						for (String source : sourceList) {
							JsonObject tempJsonObject = new JsonObject();
							
							tempJsonObject.addProperty("source", source);
							tempJsonObject.addProperty("snapshot", snapshotName);
							tempJsonObject.addProperty("timestamp", timestamp);
							tempJsonObject.addProperty("comment", "");
							
							jsonArray.add(tempJsonObject);
						}
					}
					
					// Extracts the data from the JSON array
					String commentJson = fxGson.toJson(jsonArray);
					
					// Writes the JSON data into the comments file
					FileWriter fw = new FileWriter(commentFile);
					fw.write(commentJson);
					fw.flush();
					fw.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
    		} else {	// Comments JSON File present
				String activeSnapshotName = snapshotNames.get(0);
				String activeComment = "";
				String activeSource = sourceList.get(0);
				
				// Read the JSON file as a string
				try {
					// Gets the comments data from the JSON file (in array form)
					JsonArray commentJsonArray = getCommentsJsonData(snapshotPath);
					
					// Gets list of all JSON objects corresponding to missing snapshots and expired sources
					List<JsonObject> removeSnapshotList = new ArrayList<>();
					for (int i = 0; i < commentJsonArray.size(); i++) {
						JsonObject tempJsonObject = commentJsonArray.get(i).getAsJsonObject();
						String tempSnapshotName = tempJsonObject.get("snapshot").getAsString();
						String tempSource = tempJsonObject.get("source").getAsString();
						
						// Adds comment entry into the remove list if neither snapshot nor source found
						if (!(snapshotNames.contains(tempSnapshotName)) || !(sourceList.contains(tempSource))) {
							removeSnapshotList.add(tempJsonObject);
						}
					}
					
					// Deletes entries from JSON file where the corresponding snapshots are missing
					for (int i = 0; i < removeSnapshotList.size(); i++) {
						JsonObject tempJsonObject = removeSnapshotList.get(i).getAsJsonObject();
						
						commentJsonArray.remove(tempJsonObject);
					}
					
					// Iterates through all snapshot names and initializes entries 
					// for newly added snapshots within the JSON file
					for (String snapshotName: snapshotNames) {
						boolean isNewSnapshot = true;
						
						// Iterates through the JSON array to check if the snapshot entry is to be found
						Iterator<JsonElement> iter = commentJsonArray.iterator();
						while (iter.hasNext()) {
							JsonObject tempJsonObject = iter.next().getAsJsonObject();
							String tempSnapshotName = tempJsonObject.get("snapshot").getAsString();
							
							if (tempSnapshotName.equals(snapshotName)) {
								isNewSnapshot = false;
								break;
							}
						}
						
						// Creates a new entry in the JSON file if snapshot not already present
						if (isNewSnapshot) {
							// Getting time-stamp from snapshot name
							String timestamp = snapshotName.split("_")[2];
							timestamp = timestamp.replace('.', ':');
							
							// Adding multiple comments per snapshot (i.e. for each source)
							for (String source : sourceList) {
								JsonObject newJsonObject = new JsonObject();
								newJsonObject.addProperty("source", source);
								newJsonObject.addProperty("snapshot", snapshotName);
								newJsonObject.addProperty("timestamp", timestamp);
								newJsonObject.addProperty("comment", "");
								
								commentJsonArray.add(newJsonObject);
							}
						}
					}
					
					// Iterates through the array to find and display the comment for the active snapshot
					for (int i = 0; i < commentJsonArray.size(); i++) {
						JsonObject tempJsonObject = commentJsonArray.get(i).getAsJsonObject();
						String tempSnapshotName = tempJsonObject.get("snapshot").getAsString();
						String tempSource = tempJsonObject.get("source").getAsString();
						if (tempSnapshotName.equals(activeSnapshotName) && tempSource.equals(sourceList.get(0))) {
							activeComment = tempJsonObject.get("comment").getAsString();
							activeSource = tempSource;
							break;
						}
					}
					
					// Writes the modified JSON Array data to the comments JSON file
					// Extracts the data from the JSON array
					String updatedCommentJson = fxGson.toJson(commentJsonArray);
					// Writes the JSON data into the comments file
					FileWriter fw = new FileWriter(commentFile);
					fw.write(updatedCommentJson);
					fw.flush();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Sets the active comment to the comment field below the active snapshot
				commentField.setText(activeComment);
				// Sets the active source to the combo box below the active snapshot
				sourceComboBox.getSelectionModel().select(activeSource);
    		}
        }
	}
	
	public XYChart.Series<Number, Number> getDataFromExcelFile(String filename) {
		XYChart.Series<Number, Number> data = new XYChart.Series<>();
		
		// Explains the Y-axis label appropriately if Radio Buttons type
		Experiment exp = getExperiment();
        String displayType = exp.getDisplay();
        List<String> labels = new ArrayList<>();
		if (displayType.equals("Radio Buttons")) {
            labels = getRbLabels();
            String new_ylabel = labels.get(0) + " -> " + labels.get(labels.size()-1);
            graph.getYAxis().setLabel(new_ylabel);
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    int cnt = 0;
		    while ((line = br.readLine()) != null) {
		    	if (cnt > 0) {
			        String[] values = line.split(",");
			        int time = Integer.parseInt(values[0]);
			        String value_str = values[1];
			        
			        Number value;
			        if (displayType.equals("Radio Buttons")) {
			            value = Integer.parseInt(value_str);
			        }
			        else {
			        	value = Double.parseDouble(values[1]);
			        }
			        
			        XYChart.Data<Number, Number> data_entry = new XYChart.Data<>(time, value);
			        
			        data.getData().add(data_entry);
		    	}
		        cnt++;
		    }
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
	@FXML
	private void backButtonClicked() {
		// dispose video player before exiting window
		mediaPlayer.dispose();
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
	
	public Experiment getExperiment() {
		// parse data from the respective JSON file and set variables
		
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()  
                .setPrettyPrinting()
                .create();
		// the saved .JSON file from which the information is to be parsed
		File filePath = new File("Experiments/" + expName + "/settings.json");	
		
		// read the JSON file as a string
		String json;
		try {
			json = new String(Files.readAllBytes(Paths.get(filePath.getPath())));
		
			// parse the string into JsonObjects
			JsonObject exp_obj = JsonParser.parseString(json).getAsJsonObject();
			
			Experiment exp = fxGson.fromJson(exp_obj, Experiment.class);
			return exp;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public List<String> getRbLabels() {
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()  
                .setPrettyPrinting()
                .create();
		// the saved .JSON file from which the information is to be parsed
		File filePath = new File("Experiments/" + expName + "/settings.json");	
		
		// read the JSON file as a string
		String json;
		try {
			json = new String(Files.readAllBytes(Paths.get(filePath.getPath())));
		
			// parse the string into JsonObjects
			JsonObject exp_obj = JsonParser.parseString(json).getAsJsonObject();
			JsonObject set_obj = exp_obj.getAsJsonObject("settings");
			
			RB rb = fxGson.fromJson(set_obj, RB.class);
			return Arrays.asList(rb.getLabels());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public double getMaxValue() {
		double maxValue = -1;
		
		// create a custom FX-GSON builder
		Gson fxGson = FxGson.fullBuilder()  
                .setPrettyPrinting()
                .create();
		// the saved .JSON file from which the information is to be parsed
		File filePath = new File("Experiments/" + expName + "/settings.json");	
		
		// read the JSON file as a string
		String json;
		try {
			json = new String(Files.readAllBytes(Paths.get(filePath.getPath())));
		
			// parse the string into JsonObjects
			JsonObject exp_obj = JsonParser.parseString(json).getAsJsonObject();
			JsonObject set_obj = exp_obj.getAsJsonObject("settings");
			
			Experiment exp = fxGson.fromJson(exp_obj, Experiment.class);
			String displayType = exp.getDisplay();
			
			if (displayType.equals("Slider")) {
				// Gets the registered max value if display type is slider
				SL sl = fxGson.fromJson(set_obj, SL.class);
				maxValue = Double.parseDouble(sl.getMaxValue());
			}
			else if (displayType.equals("Radio Buttons")) {
				// Gets the total number of radio buttons if display type is radio buttons
				RB rb = fxGson.fromJson(set_obj, RB.class);
				maxValue = Double.parseDouble(rb.getButtonNum());
			} else {
				// Error: -1 is returned as error value
				System.out.println("ERROR: Display type not recognizable (" + displayType + ").");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return maxValue;
	}
	
	public JsonArray getCommentsJsonData(String snapshotPath) {
		try {
			String commentFilePath = snapshotPath + "/comments.json";
	        File commentFile = new File(commentFilePath);
	        // Gets the JSON data in string format from the comments JSON file
			String commentJson = new String(Files.readAllBytes(Paths.get(commentFile.getPath())));
			
			// Reads and parses the string into a JsonArray
			JsonReader jsonReader = new JsonReader(new StringReader(commentJson));
			jsonReader.setLenient(true);
			
			JsonArray commentJsonArray = JsonParser.parseReader(jsonReader).getAsJsonArray();
			
			return commentJsonArray;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
