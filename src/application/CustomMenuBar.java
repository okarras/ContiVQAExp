package application;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CustomMenuBar {
	
	@FXML private MenuBar menuBar;
	
	public CustomMenuBar() {
		
	}
	
	@FXML
	void initialize() {
		
	}
	
	@FXML
	public void closeProgram() {
		Stage stage = (Stage) menuBar.getScene().getWindow();
	    stage.close();
	}
	
	@FXML
	public void aboutWindow() {
		// Open a dialog that displays information about the application
        Stage aboutStage = new Stage();
        BorderPane aboutPane = new BorderPane();
        Scene aboutScene = new Scene(aboutPane, 600, 420);
        
        
        // Top Box
        VBox topBox = new VBox();
        topBox.setAlignment(Pos.TOP_LEFT);
        topBox.setPadding(new Insets(10));
        topBox.setSpacing(5);
        
        HBox imageBox = new HBox();
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(5));
        imageBox.setSpacing(15);
        
        try {
        	// Set LUH Logo
	        FileInputStream input = new FileInputStream("src/img/luh_logo.png");
	        ImageView luhImgView = new ImageView(new Image(input));
	        luhImgView.setFitHeight(100);
	        luhImgView.setFitWidth(300);
			
	        // Set SE Logo
	        input = new FileInputStream("src/img/luh_se_logo.png");
	        ImageView seImgView = new ImageView(new Image(input));
	        seImgView.setFitHeight(220);
	        seImgView.setFitWidth(250);
	        
	        imageBox.getChildren().addAll(luhImgView, seImgView);
        
        } catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
        
        
        // Center Box
        VBox centerBox = new VBox();
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));
        centerBox.setSpacing(5);
        
        HBox uniBox = new HBox();
        uniBox.setAlignment(Pos.CENTER_LEFT);
        uniBox.setPadding(new Insets(5));
        uniBox.setSpacing(3);
        Label uniLabel = new Label("University: ");
        uniLabel.setStyle("-fx-font-weight:700");
        Label uniValue = new Label("Leibniz Universität Hannover");
        uniBox.getChildren().addAll(uniLabel, uniValue);
        
        HBox instBox = new HBox();
        instBox.setAlignment(Pos.CENTER_LEFT);
        instBox.setPadding(new Insets(5));
        instBox.setSpacing(3);
        Label instLabel = new Label("Research group: ");
        instLabel.setStyle("-fx-font-weight:700");
        Label instValue = new Label("Software Engineering Institute (SE)");
        instBox.getChildren().addAll(instLabel, instValue);
        
        HBox projectBox = new HBox();
        projectBox.setAlignment(Pos.CENTER_LEFT);
        projectBox.setPadding(new Insets(5));
        projectBox.setSpacing(3);
        Label projectLabel = new Label("Project: ");
        projectLabel.setStyle("-fx-font-weight:700");
        Label projectValue = new Label("ViViReq - ContiVQAExp");
        projectBox.getChildren().addAll(projectLabel, projectValue);
        
        
        // Bottom Box
        VBox bottomBox = new VBox();
        bottomBox.setAlignment(Pos.BASELINE_RIGHT);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setSpacing(5);
        
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(5));
        buttonBox.setSpacing(3);
		
		Button cancelButton = new Button("Close");
        cancelButton.setOnAction(e -> {
        	aboutStage.close();
        });
        
        buttonBox.getChildren().add(cancelButton);
        
        
        // General setup
        topBox.getChildren().addAll(imageBox, new Separator());
        centerBox.getChildren().addAll(uniBox, instBox, projectBox);
        bottomBox.getChildren().addAll(new Separator(), buttonBox);
        
        aboutPane.setTop(topBox);
        aboutPane.setCenter(centerBox);
        aboutPane.setBottom(bottomBox);
        
        aboutStage.setTitle("About");
        aboutStage.setScene(aboutScene);
        aboutStage.setResizable(false);
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        try {	// Sets the icon of the SE institute to the about stage
        	FileInputStream logoInput = new FileInputStream("src/img/luh_se_logo.png");
        	aboutStage.getIcons().add(new Image(logoInput));
        } catch(IOException e) {
			e.printStackTrace();
		}
        aboutStage.show();
	}
}
