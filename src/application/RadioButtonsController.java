package application;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RadioButtonsController {
	
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

	public TextField getButtonNumField() {
		return buttonNumField;
	}

	public void setButtonNumField(TextField buttonNumField) {
		this.buttonNumField = buttonNumField;
	}

	public String[] getLabels() {
		return this.labels.toArray(new String[0]);
	}
	
	public void setLabels(List<String> labels) {
		this.labels = labels;
	}
	
	public ToggleGroup getToggleGroup() {
		return toggleGroup;
	}
	
	public void selectMidByDefault() {
		ListIterator<Node> li = radioButtonBox.getChildren().listIterator();
		int num_rb = radioButtonBox.getChildren().size();
		
		int middle = (int) Math.ceil(num_rb / 2.0);
		String middle_id = Integer.toString(middle);
		
		while (li.hasNext()) {
			RadioButton rb = (RadioButton) li.next();
			if (rb.getId().equals(middle_id)) {
				rb.setSelected(true);
				break;
			}
		}
	}
	
	public void removeSettings() {
		settingsBox.getChildren().clear();
	}

	@FXML private VBox settingsBox;
	@FXML private TextField minLabelField;
	@FXML private TextField maxLabelField;
	@FXML private TextField buttonNumField;
	@FXML private HBox labelsBox;
	@FXML private Label minLabel;
	@FXML private HBox radioButtonBox;
	@FXML private Label maxLabel;
	
	private ToggleGroup toggleGroup;
	private List<String> labels;
	
	@FXML
	public void initialize() {
		
		minLabelField.setText("Low");
		maxLabelField.setText("High");
		buttonNumField.setText("2");
		
		updateRadioButtons(Integer.parseInt(buttonNumField.getText()));
		updateLabels(Integer.parseInt(buttonNumField.getText()));
		
		minLabelField.textProperty().addListener((observable, oldValue, newValue) -> {
			minLabel.setText(newValue.toString());
	    });
		
		maxLabelField.textProperty().addListener((observable, oldValue, newValue) -> {
			maxLabel.setText(newValue.toString());
	    });
		
		buttonNumField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.isEmpty()) {
				int num = (int) Integer.parseInt(newValue);
				
				if (num < 2) {
					num = 2;
					buttonNumField.setText(Integer.toString(num));
				}
				else if (num > 10) {
					num = 10;
					buttonNumField.setText(Integer.toString(num));
				}
				else {
					updateRadioButtons(num);
					updateLabels(num);
				}
			}
	    });
	}
	
	public void changeToViewMode(String minLabel, String maxLabel, String buttonNum, String[] labels) {
		// set the values of the respective parameters
		minLabelField.setText(minLabel);
		maxLabelField.setText(maxLabel);
		buttonNumField.setText(buttonNum);
		
		// disable portions of the window that should be non-editable
		buttonNumField.setEditable(false);
		
		int j = 0;	// label_num
		for (int i = 0; i < labelsBox.getChildren().size(); i++) {
			Node node = labelsBox.getChildren().get(i);
			if (node instanceof TextField) {
				TextField label = (TextField) node;
				label.setText(labels[j]);  // set text of individual labels
				labelsBox.getChildren().set(i, label);
				j++;  // increment label_num by 1
			}
		}
	}
	
	public void updateRadioButtons(int num_buttons) {
		radioButtonBox.getChildren().clear();
		toggleGroup = new ToggleGroup();
		
		for (int i = 1; i <= num_buttons; i++) {
			RadioButton button = new RadioButton(Integer.toString(i));
			button.setId(Integer.toString(i));
			button.setToggleGroup(toggleGroup);
			radioButtonBox.getChildren().add(button);
		}
	}
	
	public void updateLabels(int num_labels) {
		labelsBox.getChildren().clear();
		labelsBox.getChildren().add(new Label("Labels:"));
		
		labels = new ArrayList<>();
		
		for (int i = 1; i <= num_labels; i++) {
			Label label_index = new Label(" " + Integer.toString(i) + ". ");
			TextField label = new TextField();
			
			label.setId(Integer.toString(i));
			label.setText(Integer.toString(i));
			
			labels.add(i - 1, Integer.toString(i));
			
			label.textProperty().addListener((observable, oldValue, newValue) -> {
				for (int x = 0; x < radioButtonBox.getChildren().size(); x++) {
					RadioButton rb = (RadioButton) radioButtonBox.getChildren().get(x);
					
					boolean idCheck = rb.getId().equals(label.getId());					
					if (idCheck) {
						rb.setText(newValue);
						radioButtonBox.getChildren().set(x, rb);
						
						int label_id = Integer.parseInt(label.getId());
						labels.set(label_id - 1, label.getText());
					}
				}
			});
			
			label.setMaxWidth(80);
			labelsBox.getChildren().addAll(label_index, label);
		}
	}
	
}
