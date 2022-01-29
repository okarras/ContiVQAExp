package application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class SliderController {
	
	public TextField getMinLabelField() {
		return minLabelField;
	}

	public void setMinLabelField(TextField minLabelField) {
		this.minLabelField = minLabelField;
	}

	public TextField getMaxLabelField() {
		return maxLabelField;
	}

	public void setMaxLabelField(TextField maxLabelField) {
		this.maxLabelField = maxLabelField;
	}

	public TextField getMinValueField() {
		return minValueField;
	}

	public void setMinValueField(TextField minValueField) {
		this.minValueField = minValueField;
	}

	public TextField getMaxValueField() {
		return maxValueField;
	}

	public void setMaxValueField(TextField maxValueField) {
		this.maxValueField = maxValueField;
	}
	
	public Slider getSlider() {
		return slider;
	}
	
	public void selectMidByDefault() {
		double innerMid = (slider.getMax() - slider.getMin())/2;
		double mid = slider.getMin() + innerMid;
		slider.setValue(mid);
	}
	
	public void removeSettings() {
		settingsBox.getChildren().clear();
	}

	public void setAsContinuous() {
		this.intervalContButton.setSelected(true);
	}
	
	public void setAsDiscrete() {
		this.intervalDiscButton.setSelected(true);
	}
	
	public String getIntervalType() {
		RadioButton selectedRadioButton = (RadioButton) intervalGroup.getSelectedToggle();
		String intervalType = selectedRadioButton.getText();
		return intervalType;
	}
	
	@FXML private VBox settingsBox;
	@FXML private TextField minLabelField;
	@FXML private TextField maxLabelField;
	@FXML private TextField minValueField;
	@FXML private TextField maxValueField;
	@FXML private RadioButton intervalContButton;
	@FXML private RadioButton intervalDiscButton;
	@FXML private Label previewLabel;
	@FXML private Slider slider;
	@FXML private Label minLabel;
	@FXML private Label maxLabel;
	private ToggleGroup intervalGroup;
	
	@FXML
	public void initialize() {
		
		minLabelField.setText("Low");
		maxLabelField.setText("High");
		minValueField.setText("0");
		maxValueField.setText("100");
		
		intervalGroup = new ToggleGroup();
		
		intervalContButton.setToggleGroup(intervalGroup);
		intervalDiscButton.setToggleGroup(intervalGroup);

		intervalContButton.setSelected(true);
 		
		minLabelField.textProperty().addListener((observable, oldValue, newValue) -> {
			minLabel.setText(newValue.toString());
	    });
		
		maxLabelField.textProperty().addListener((observable, oldValue, newValue) -> {
			maxLabel.setText(newValue.toString());
	    });
		
		minValueField.textProperty().addListener((observable, oldValue, newValue) -> {
			double min;
			if (newValue.isEmpty()) {
				min = 0.0;
			}
			else {
				min = (double) Math.round(Double.parseDouble(newValue));
				
				if (min > slider.getMax()) {
					int max = (int) Math.round(min + 1);
					maxValueField.setText(Integer.toString(max));
				}
			}
			slider.setMin(min);
	    });
		
		maxValueField.textProperty().addListener((observable, oldValue, newValue) -> {
			double max;
			if (newValue.isEmpty()) {
				max = slider.getMin() + 1;
			}
			else {
				max = (double) Math.round(Double.parseDouble(newValue));
				
				if (max < slider.getMin()) {
					int min = (int) Math.round(max - 1);
					minValueField.setText(Integer.toString(min));
				}
			}
			slider.setMax(max);
	    });
		
		slider.valueProperty().addListener((observable, oldValue, newValue) -> {
			String intervalType = this.getIntervalType();
			
			newValue = Math.round(newValue.doubleValue() * 100.0) / 100.0;	// continuous, by default
			if (intervalType != null) {
				if (intervalType.equals("discrete")) {	// discrete
					newValue = Math.round(newValue.doubleValue());
				}
			}
			
			previewLabel.setText(newValue.toString());
	    });
	}
	
	public void changeToViewMode(String minLabel, String maxLabel, String minValue, String maxValue, String intervalType) {
		// set the values of the respective parameters
		minLabelField.setText(minLabel);
		maxLabelField.setText(maxLabel);
		minValueField.setText(minValue);
		maxValueField.setText(maxValue);
		
		// set if interval type is discrete or continuous
		this.setAsContinuous();	// continuous, by default
		if (intervalType != null) {
			if (intervalType.equals("discrete")) {	// discrete
				this.setAsDiscrete();
			}
		}
		
		// disable portions of the window that should be non-editable
		minValueField.setEditable(false);
		maxValueField.setEditable(false);
		intervalContButton.setDisable(true);
		intervalDiscButton.setDisable(true);
	}

}
