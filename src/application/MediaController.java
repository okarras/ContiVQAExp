/*
 * Copyright (c) 2012, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MediaController extends BorderPane {

    private MediaPlayer mp;
    private MediaView mediaView;
    private final boolean repeat = false;
    private boolean stopRequested = false;
    private boolean atEndOfMedia = false;
    private boolean timeSliderDragged = false;
    private boolean viewMode = false;
    private boolean showsAllData = false;
    private String expName;
    private String partName;
    private Duration duration;
    private Slider timeSlider;
    private Label playTime;
    private Slider volumeSlider;
    private HBox mediaBar;
    private Button nextButton;
    private Button rewindButton;
    private Button snapshotButton;
    private Map<String, String> recordedData;
    private String snapPath = "";

    public MediaController(final MediaPlayer mp) {
        this.mp = mp;
        setStyle("-fx-background-color: #bfc2c7;");
        mediaView = new MediaView(mp);
        recordedData = new HashMap<>();

        mediaBar = new HBox();
        mediaBar.setAlignment(Pos.CENTER);
        mediaBar.setPadding(new Insets(5, 10, 5, 10));
        mediaBar.setSpacing(5);
        
        VBox videoPane = new VBox();
        videoPane.getChildren().add(mediaView);
        videoPane.setStyle("-fx-background-color: black;");
        videoPane.setAlignment(Pos.CENTER);
        videoPane.getChildren().add(mediaBar);
        setCenter(videoPane);
        
        // Get primary screen bounds
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        
        // Determine the size of the media player (video)
        double mediaHeight = screenBounds.getHeight() / 3.0;
        double mediaWidth = screenBounds.getWidth() / 2.0;
        
        // Resize video
        mediaView.setFitHeight(mediaHeight);	// Safe value:- 720
        mediaView.setFitWidth(mediaWidth);	// Safe value:- ~250
        
        final Button playButton = new Button(">");

        playButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                Status status = mp.getStatus();

                if (status == Status.UNKNOWN || status == Status.HALTED) {
                    // don't do anything in these states
                    return;
                }

                // rewind the movie if we're sitting at the end
                if (atEndOfMedia) {
                	
                	if (viewMode) {
                		// restart the video
    					mp.seek(mp.getStartTime());
                        atEndOfMedia = false;
                        
                        mp.pause();
                        stopRequested = false;
                	}
                	else {
                		// open alert box to confirm restart and overwrite
                    	String message = "Would you like to restart (and overwrite) the recording process?";
        				Alert alert = new Alert(AlertType.CONFIRMATION, message,
        						ButtonType.YES, ButtonType.NO);
        				alert.showAndWait();

        				if (alert.getResult() == ButtonType.YES) {
        					// restart (overwrite) the recording
        					mp.seek(mp.getStartTime());
                            atEndOfMedia = false;
                            
                            mp.pause();
                            stopRequested = false;
                            
                            recordedData.clear();
        				}
                	}
    				
                    return;
                }
                
                if (status == Status.PAUSED
                        || status == Status.READY
                        || status == Status.STOPPED) {
                    mp.play();
                } else {
                    mp.pause();
                }
            }
        });
        mp.currentTimeProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                updateValues();
            }
        });

        mp.setOnPlaying(new Runnable() {
            public void run() {
            	playButton.setText("||");
                if (stopRequested) {
                    mp.pause();
                    stopRequested = false;
                } else {
                    if (!viewMode) {
                    	timeSlider.setDisable(false);
                    }
                }
            }
        });

        mp.setOnPaused(new Runnable() {
            public void run() {
            	if (!viewMode) {
            		if (!isAtEndOfMedia())
                		nextButton.setDisable(true);
            		timeSlider.setDisable(true);
            	}
            	
                playButton.setText(">");
            }
        });

        mp.setOnReady(new Runnable() {
            public void run() {
            	if (!viewMode) {
            		if (!isAtEndOfMedia())
            			timeSlider.setDisable(false);
            		nextButton.setDisable(true);
            		snapshotButton.setDisable(true);
            	}
            	else {
            		rewindButton.setDisable(true);
            		
            		snapshotButton.setOnAction(e -> {
            			captureScreenshot();
            		});
            	}
            		
            	mp.pause();
                duration = mp.getMedia().getDuration();
                updateValues();
            }
        });

        mp.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);
        mp.setOnEndOfMedia(new Runnable() {
            public void run() {
            	if (!repeat) {
                    playButton.setText(">");
                    stopRequested = true;
                    atEndOfMedia = true;
                    
                    if (!viewMode) {
                    	timeSlider.setDisable(true);
	                    nextButton.setDisable(false);
                    }
                }
            }
        });

        mediaBar.getChildren().add(playButton);
        
        // Add rewind button and functionality
        rewindButton = new Button("<");
        
        rewindButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                Status status = mp.getStatus();

                if (status == Status.UNKNOWN || status == Status.HALTED) {
                    // don't do anything in these states
                    return;
                }
                
                mp.pause();
                
                // Open a dialog in which the new time is to be selected
                Stage rewindStage = new Stage();
                BorderPane rewindPane = new BorderPane();
                Scene scene = new Scene(rewindPane, 300, 150);
                
                VBox centerBox = new VBox();
                centerBox.setAlignment(Pos.CENTER);
                centerBox.setPadding(new Insets(10));
                centerBox.setSpacing(15);
                HBox bottomBox = new HBox();
                bottomBox.setAlignment(Pos.CENTER_RIGHT);
                bottomBox.setPadding(new Insets(10));
                bottomBox.setSpacing(10);
                
                Slider rewindSlider = new Slider();
                rewindSlider.setMin(mp.getStartTime().toSeconds());
                rewindSlider.setMax(mp.getCurrentTime().toSeconds());
                
                HBox previewBox = new HBox();
                Label previewLabel = new Label("Rewind from: ");
                Label rewindLabel = new Label("0,00");
                rewindSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                	double currRewindTime = (double) newValue;
                	rewindLabel.setText(String.format ("%.2f", currRewindTime));
                });
                previewBox.getChildren().addAll(previewLabel, rewindLabel);
                
                Region bottomLeftRegion = new Region();
                HBox.setHgrow(bottomLeftRegion, Priority.ALWAYS);
                Button okButton = new Button("Rewind");
                okButton.setOnAction(e1 -> {
                	double rewindTime = rewindSlider.getValue();
                    
                	// Remove mappings for all entries after the new rewind time
                	List<String> keysToDelete = new ArrayList<>();
                    for (String key: recordedData.keySet()) {
                    	double comparedTime = Double.parseDouble(key);
                    	if (comparedTime >= rewindTime) {
                    		keysToDelete.add(key);
                    	}
                    }
                    for (String delKey : keysToDelete) {
                    	recordedData.remove(delKey);
                    }
                	
                	// Rewind (overwrite) the recording
    				mp.seek(Duration.seconds(rewindTime));
                	if(atEndOfMedia && (rewindTime < mp.getStopTime().toSeconds())) {
                		atEndOfMedia = false;
                	}
                    mp.pause();
                    stopRequested = false;
                	
                	rewindStage.close();
                });
                Button cancelButton = new Button("Cancel");
                cancelButton.setOnAction(e2 -> {
                	rewindStage.close();
                });
                
                centerBox.getChildren().addAll(rewindSlider, previewBox);
                bottomBox.getChildren().addAll(bottomLeftRegion, okButton, cancelButton);
                
                rewindPane.setCenter(centerBox);
                rewindPane.setBottom(bottomBox);
                
                rewindStage.setTitle("Rewind Settings");
                rewindStage.setScene(scene);
                rewindStage.setResizable(false);
                rewindStage.initModality(Modality.APPLICATION_MODAL);
                rewindStage.show();
            }
        });
        mediaBar.getChildren().add(rewindButton);

        // Add spacer
        Label spacer = new Label("   ");
        mediaBar.getChildren().add(spacer);
        
        // Add Time label
        Label timeLabel = new Label("Time: ");
        mediaBar.getChildren().add(timeLabel);

        // Add time slider
        timeSlider = new Slider();
        
        // Enable/Disable Time slider Drag
        if (!viewMode) {
	        timeSlider.setOnDragDetected(value -> {
	        	timeSliderDragged = true;
	        });
	        timeSlider.setOnDragExited(value -> {
	        	timeSliderDragged = false;
	        });
        }
        
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        timeSlider.setMinWidth(50);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
        timeSlider.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (timeSlider.isValueChanging()) {
                	if  (viewMode || (!timeSliderDragged)) {
	                    // multiply duration by percentage calculated by slider position
	                    mp.seek(duration.multiply(timeSlider.getValue() / 100.0));
                	}
                }
            }
        });
        mediaBar.getChildren().add(timeSlider);

        // Add Play label
        playTime = new Label();
        playTime.setPrefWidth(130);
        playTime.setMinWidth(50);
        mediaBar.getChildren().add(playTime);

        // Add snapshot button and functionality
        snapshotButton = new Button();
        
        try {
			FileInputStream input = new FileInputStream("src/img/photo-99136_1280.png");
			Image cameraIcon = new Image(input);
	        ImageView cameraIconView = new ImageView(cameraIcon);
	        cameraIconView.setFitHeight(15);
	        cameraIconView.setFitWidth(15);
	        
	        snapshotButton.setGraphic(cameraIconView);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			snapshotButton = new Button("|o|");
		}
        
        mediaBar.getChildren().add(snapshotButton);
        
        // Add the volume label
        Label volumeLabel = new Label("Vol: ");
        mediaBar.getChildren().add(volumeLabel);

        // Add Volume slider
        volumeSlider = new Slider();
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.setMinWidth(30);
        volumeSlider.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (volumeSlider.isValueChanging()) {
                    mp.setVolume(volumeSlider.getValue() / 100.0);
                }
            }
        });
        mediaBar.getChildren().add(volumeSlider);
        
        setBottom(mediaBar);
    }
    
    private void captureScreenshot() {
    	if (mp.getStatus().equals(Status.PLAYING)) {
    		mp.pause();
    	}
    	
    	WritableImage image = mediaView.snapshot(new SnapshotParameters(), null);
        
    	String snapFileName = "";
    	Date date = new Date();
    	String timestamp = playTime.getText();
    	timestamp = timestamp.split("/")[0];
    	timestamp = timestamp.replace(':', '.');
    	if (showsAllData) {	// Edited by Experimenter
    		snapFileName = "EXP_" + date.getTime() + "_" + timestamp + ".png";
    		snapPath = "Experiments/" + expName + "/Snapshots";
    	}
    	else {	// Edited by Participant
    		snapFileName = "PART_" + date.getTime() + "_" + timestamp + ".png";
    		snapPath = "Experiments/" + expName + "/" + partName + "/Snapshots"; 
    	}
    	snapPath += "/" + snapFileName;
    	
        File file = new File(snapPath);
        
        try {
    		// Save image file
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            
	        Stage prim_stage = (Stage) mediaView.getScene().getWindow();
	        // Minimize and Maximize screen to resize window
	        prim_stage.setIconified(true);
	        prim_stage.setIconified(false);
		    
	        // Refresh window
			Parent root;
			try {
				DataGraphController dataGraphCtr = new DataGraphController(expName, partName);
				
				FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DataGraphWindow.fxml"));
	        	fxmlLoader.setController(dataGraphCtr);
	        	root = fxmlLoader.load();
	        	
				Scene scene = new Scene(root);
				scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
				prim_stage.setScene(scene);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
        } catch (IOException e1) {
			e1.printStackTrace();
		}
    }

    protected void updateValues() {
        if (playTime != null && timeSlider != null && volumeSlider != null) {
            Platform.runLater(new Runnable() {
                public void run() {
                    Duration currentTime = mp.getCurrentTime();
                    playTime.setText(formatTime(currentTime, duration));
                    timeSlider.setDisable(duration.isUnknown());
                    if (!timeSlider.isDisabled()
                            && duration.greaterThan(Duration.ZERO)
                            && !timeSlider.isValueChanging()) {
                        timeSlider.setValue(currentTime.divide(duration.toMillis()).toMillis()
                                * 100.0);
                    }
                    if (!volumeSlider.isValueChanging()) {
                        volumeSlider.setValue((int) Math.round(mp.getVolume()
                                * 100));
                    }
                }
            });
        }
    }

    private static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60
                - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d/%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d/%02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }
    
    
    // custom helper methods
    
    public boolean isAtEndOfMedia() {
    	return atEndOfMedia;
    }
    
    public void setAtEndOfMedia(boolean eom) {
    	this.atEndOfMedia = eom;
    }
    
    public void setStopRequested(boolean stopReq) {
    	this.stopRequested = stopReq;
    }
    
    public void setViewMode(boolean value) {
    	this.viewMode = value;
    }
    
    public void setShowAllData(boolean value) {
    	this.showsAllData = value;
    }
    
    public void setExpName(String value) {
    	this.expName = value;
    }
    
    public void setPartName(String value) {
    	this.partName = value;
    }
    
    public void setNextButton(Button button) {
    	this.nextButton = button;
    }
    
    public Slider getTimeSlider() {
    	return this.timeSlider;
    }
    
    public void setRecordedData(Map<String, String> data) {
    	this.recordedData = data;
    }
    
    public Map<String, String> getRecordedData() {
    	return this.recordedData;
    }
    
    public double getVideoHeight() {
    	double height = mediaView.getFitHeight();
    	return height;
    }
    
    public double getVideoWidth() {
    	double width = mediaView.getFitWidth();
    	return width;
    }
}
