package m;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import com.github.sarxos.webcam.Webcam;
//import com.github.sarxos.webcam.WebcamUtils;

public class WebcamTest extends Application {

	ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
	
	private FlowPane bottomCameraControlPane;
	private FlowPane topPane;
	private FlowPane testPane;
	private BorderPane root;
	private BorderPane root2;
	private String cameraListPromptText = "Choose Camera";
	private ImageView imgWebCamCapturedImage;
	private Webcam webCam = null;
	private BufferedImage grabbedImage;
	private BorderPane webCamPane;
	private Button btnCameraStartAndStop;
	private Button btnCameraPicture;
	private Button cancel;
	private Button approve;
	private boolean stopCamera = false;
	private String filePath = getPath(); // this will be get path
	private Scene camera;

	@Override
	public void start(Stage primaryStage) {

		primaryStage.setTitle("Connecting WebCam Using Sarxos API");
		root = new BorderPane();
		root2 = new BorderPane();
		topPane = new FlowPane();
		topPane.setAlignment(Pos.CENTER);
		topPane.setHgap(20);
		topPane.setOrientation(Orientation.HORIZONTAL);
		topPane.setPrefHeight(40);
		root.setTop(topPane);
		webCamPane = new BorderPane();
		webCamPane.setStyle("-fx-background-color: #ccc;");
		imgWebCamCapturedImage = new ImageView();
		webCamPane.setCenter(imgWebCamCapturedImage);
		root.setCenter(webCamPane);
		createTopPanel();
		bottomCameraControlPane = new FlowPane();
		bottomCameraControlPane.setOrientation(Orientation.HORIZONTAL);
		bottomCameraControlPane.setAlignment(Pos.CENTER);
		bottomCameraControlPane.setHgap(20);
		bottomCameraControlPane.setVgap(10);
		bottomCameraControlPane.setPrefHeight(40);
		bottomCameraControlPane.setDisable(true);
		root.setBottom(bottomCameraControlPane);
		camera = new Scene(root);
		
		testPane = new FlowPane();
		cancel = new Button();
		cancel.setText("Cancel");
		approve = new Button();
		approve.setText("Approve");
		
		approve.setOnAction(e -> primaryStage.setScene(camera));
		testPane.getChildren().addAll(cancel, approve);
		testPane.setOrientation(Orientation.HORIZONTAL);
		testPane.setAlignment(Pos.CENTER);
		testPane.setHgap(20);
		testPane.setVgap(10);
		testPane.setPrefHeight(40);
		root2.setBottom(testPane);
		
		
		Scene approvePhoto = new Scene(root2, 300, 400);
		createCameraControls(primaryStage, approvePhoto);

		primaryStage.setScene(camera);
		primaryStage.setHeight(500);
		primaryStage.setWidth(400);
		primaryStage.centerOnScreen();
		primaryStage.show();

		Platform.runLater(new Runnable() {

			@Override
			public void run() {

				setImageViewSize();
			}
		});

	}

	class WebCamInfo
	{
		private String webCamName ;
		private int webCamIndex ;

		public String getWebCamName() {
			return webCamName;
		}
		public void setWebCamName(String webCamName) {
			this.webCamName = webCamName;
		}
		public int getWebCamIndex() {
			return webCamIndex;
		}
		public void setWebCamIndex(int webCamIndex) {
			this.webCamIndex = webCamIndex;
		}

		@Override
		public String toString() {
			return webCamName;
		}
	}

	protected void setImageViewSize() {

		double height = webCamPane.getHeight();
		double width  = webCamPane.getWidth();
		imgWebCamCapturedImage.setFitHeight(height);
		imgWebCamCapturedImage.setFitWidth(width);
		imgWebCamCapturedImage.prefHeight(height);
		imgWebCamCapturedImage.prefWidth(width);
		imgWebCamCapturedImage.setPreserveRatio(true);

	}


	private void createTopPanel() {

		int webCamCounter = 0;
		Label lbInfoLabel = new Label("Select Your WebCam Camera");
		ObservableList<WebCamInfo> options = FXCollections.observableArrayList();

		for(Webcam webcam:Webcam.getWebcams())
		{
			WebCamInfo webCamInfo = new WebCamInfo();
			webCamInfo.setWebCamIndex(webCamCounter);
			webCamInfo.setWebCamName(webcam.getName());
			options.add(webCamInfo);
			webCamCounter++;
		}
		
		ComboBox<WebCamInfo> cameraOptions = new ComboBox<WebCamInfo>();
		cameraOptions.setItems(options);
		cameraOptions.setPromptText(cameraListPromptText);
		cameraOptions.getSelectionModel().selectedItemProperty().addListener(new  ChangeListener<WebCamInfo>() {

			@Override
			public void changed(ObservableValue<? extends WebCamInfo> arg0, WebCamInfo arg1, WebCamInfo arg2) {
				if (arg2 != null) {

					System.out.println("WebCam Index: " + arg2.getWebCamIndex()+": WebCam Name:"+ arg2.getWebCamName());
					initializeWebCam(arg2.getWebCamIndex());
				}
			}
		});
		topPane.getChildren().addAll(lbInfoLabel, cameraOptions);
	}

	protected void initializeWebCam(final int webCamIndex) {

		Task<Void> webCamTask = new Task<Void>() {

			@Override
			protected Void call() throws Exception {

				if(webCam != null)
				{
					disposeWebCamCamera();
					webCam = Webcam.getWebcams().get(webCamIndex);
					webCam.open();
				}else
				{
					webCam = Webcam.getWebcams().get(webCamIndex);
					webCam.open();
				}

				startWebCamStream();
				return null;
			}
		};

		Thread webCamThread = new Thread(webCamTask);
		webCamThread.setDaemon(true);
		webCamThread.start();
		bottomCameraControlPane.setDisable(false);
	}

	protected void startWebCamStream() {

		stopCamera  = false;
		Task<Void> task = new Task<Void>() {


			@Override
			protected Void call() throws Exception {

				while (!stopCamera) {
					try {
						if ((grabbedImage = webCam.getImage()) != null) {

							//								System.out.println("Captured Image height*width:"+grabbedImage.getWidth()+"*"+grabbedImage.getHeight());
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									final Image mainiamge = SwingFXUtils
											.toFXImage(grabbedImage, null);
									imageProperty.set(mainiamge);
								}
							});

							grabbedImage.flush();

						}
					} catch (Exception e) {
					} finally {

					}

				}

				return null;

			}

		};
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
		imgWebCamCapturedImage.imageProperty().bind(imageProperty);

	}

	private void createCameraControls(Stage primaryStage, Scene approvePhoto) {
		btnCameraStartAndStop = new Button ();
		btnCameraStartAndStop.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if(stopCamera) {
					startWebCamCamera();
					btnCameraStartAndStop.setText("Stop Camera");
				} else {
					stopWebCamCamera();
					btnCameraStartAndStop.setText("Start Camera");
				}
			}
		});
		btnCameraStartAndStop.setText("Stop Camera");

		btnCameraPicture = new Button();
		btnCameraPicture.setText("Take Picture");
		btnCameraPicture.setOnAction(e -> approve(primaryStage, approvePhoto));
		
		bottomCameraControlPane.getChildren().add(btnCameraPicture);
		bottomCameraControlPane.getChildren().add(btnCameraStartAndStop);
	}

	protected void approve(Stage primaryStage, Scene approvePhoto) {
		File tempfile = takePictureFromWebCam();
		Image image = new Image(tempfile.toURI().toString());
	    ImageView iv = new ImageView(image);
	    root2.setCenter(iv);
		primaryStage.setScene(approvePhoto);
		cancel.setOnAction(e -> canclePhoto(primaryStage, tempfile));
	}	
	
	protected void canclePhoto(Stage primaryStage, File tempfile) {
		tempfile.delete();
		primaryStage.setScene(camera);
	}
	
	protected File takePictureFromWebCam() {
		try {
			String timeStamp = new SimpleDateFormat("MMM dd, yyyy - [HH.mm.ss.SSS]").format(new Timestamp(System.currentTimeMillis()));
			File file = new File(filePath + timeStamp + ".png");
			OutputStream out = new FileOutputStream(file);
			ImageIO.write(grabbedImage, "PNG", file);
			out.close();
			return file;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void disposeWebCamCamera() {

		stopCamera = true;
		webCam.close();
	}

	protected void startWebCamCamera() {

		stopCamera = false;
		startWebCamStream();
	}

	protected void stopWebCamCamera() {

		stopCamera = true;
	}

	public String getPath() {
		return "";
	}
	
	public static void main(String[] args) {
		launch(args);
	}	
	
}


