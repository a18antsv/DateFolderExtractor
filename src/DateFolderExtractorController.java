import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

public class DateFolderExtractorController {

    @FXML private RadioButton directoryRadio;
    @FXML private RadioButton fileRadio;
    @FXML private TextField pathTextField;
    @FXML private CheckBox checkboxFileName;
    @FXML private CheckBox checkboxFileMetaData;
    @FXML private CheckBox checkboxLastModified;
    @FXML private TextArea logTextArea;

    @FXML
    public void run() {
        String pathTextFieldText = pathTextField.getText();
        if(pathTextFieldText.isEmpty()) {
            logTextArea.appendText("Path text field cannot be empty.\n\n");
            return;
        }

        List<String> pathList = getPathList(pathTextFieldText);
        if(pathList.isEmpty()) {
            logTextArea.appendText("No files to process...\n\n");
            return;
        }

        logTextArea.appendText("Running...\n");

        pathList.forEach(path -> logTextArea.appendText(new File(path).getName() + "\n"));

        pathList.forEach(path -> {
            File file = new File(path);
            File baseDirectory = file.getParentFile();
            String fileName = file.getName();
            LocalDate date = null;

            if(checkboxFileMetaData.isSelected()) {
                logTextArea.appendText("Extracting date from metadata of: " + fileName + "\n");
                date = getMetadataDate(file);
            }
            if(checkboxFileName.isSelected() && date == null) {
                logTextArea.appendText("Extracting date from file name of: " + fileName + "\n");
                date = getDateByFileName(fileName);
            }
            if(checkboxLastModified.isSelected() && date == null) {
                logTextArea.appendText("Extracting date from last modified date of: " + fileName + "\n");
                date = getLastModifiedFileDate(file);
            }

            if(date != null) {
                File newDirectory = new File(baseDirectory + "/" + date.getYear() + "/" + String.format("%02d", date.getMonthValue()) + "/" + String.format("%02d", date.getDayOfMonth()));

                if(newDirectory.mkdirs()) {
                    logTextArea.appendText("Created new directory: " + newDirectory + "\n");
                }

                if(file.renameTo(new File(newDirectory + "/" + fileName))) {
                    logTextArea.appendText("Moved " + fileName + " to " + newDirectory + "\n");
                } else {
                    logTextArea.appendText("Could not move " + fileName + "\n");
                }
            }
        });
        logTextArea.appendText("Finished.\n\n");
    }

    @FXML
    public void selectPath() {
        if(directoryRadio.isSelected()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select directory");
            File directory = chooser.showDialog(DateFolderExtractor.getStage());
            if(directory != null) {
                pathTextField.setText(directory.toString());
                logTextArea.appendText("Selected folder: " + directory.toString() + "\n");
            }
        } else if(fileRadio.isSelected()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select files");
            List<File> files = fileChooser.showOpenMultipleDialog(DateFolderExtractor.getStage());
            if(files != null) {
                clearPath();
                logTextArea.appendText("Selected files:\n");
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    pathTextField.appendText(file.toString());
                    if(i != files.size() - 1) {
                        pathTextField.appendText(",");
                    }
                    logTextArea.appendText(files.get(i) + "\n");
                }
            }
        }
    }

    @FXML
    public void clearLog() {
        logTextArea.clear();
    }

    @FXML
    public void clearPath() {
        pathTextField.clear();
    }

    private List<String> getPathList(String pathTextFieldText) {
        List<String> pathList = new ArrayList<>();
        if(directoryRadio.isSelected()) {
            logTextArea.appendText("Folder " + pathTextFieldText + " contains:\n");
            File directory = new File(pathTextFieldText);
            for(File file : Objects.requireNonNull(directory.listFiles())) {
                if(file.isFile()) {
                    pathList.add(file.toString());
                }
            }
        } else if(fileRadio.isSelected()) {
            logTextArea.appendText("Selected files:\n");
            pathList = Arrays.asList(pathTextFieldText.split(","));
        }
        return pathList;
    }

    private LocalDate getMetadataDate(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if(directory == null) {
                logTextArea.appendText(file.getName() + " has no date metadata. Ignoring...\n");
                return null;
            };
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            return convertToLocalDate(date);
        } catch (IOException | ImageProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private LocalDate getDateByFileName(String fileName) {
        if(fileName.length() >= 8) {
            String y = fileName.substring(0, 4);
            String m = fileName.substring(4, 6);
            String d = fileName.substring(6, 8);
            try {
                return LocalDate.parse(y + "-" + m + "-" + d);
            } catch(DateTimeParseException e) {
                e.printStackTrace();
                logTextArea.appendText(fileName + " does not start with a date. Ignoring...\n");
            }
            return null;
        }
        return null;
    }

    private LocalDate getLastModifiedFileDate(File file) {
        return Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate convertToLocalDate(Date dateToConvert) {
        if(dateToConvert == null) return null;
        return LocalDate.ofInstant(dateToConvert.toInstant(), ZoneId.systemDefault());
    }
}
