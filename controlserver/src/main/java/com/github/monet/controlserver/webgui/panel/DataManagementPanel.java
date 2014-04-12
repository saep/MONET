package com.github.monet.controlserver.webgui.panel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;

import com.github.monet.controlserver.BundleManager;
import com.github.monet.controlserver.ControlServer;
import com.github.monet.controlserver.webgui.MonetMainPage;

public class DataManagementPanel extends ContentPanel {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -3227230933800914523L;

	/**
	 * A default error map which indicates the success of the form.
	 */
	private HashMap<String, String> successMap;

	/**
	 * A default error map key which indicates the success of the form.
	 */
	private String successKey = "Success";

	/**
	 * Default constructor with empty former error map.
	 */
	public DataManagementPanel() {
		this(new HashMap<String, String>());
	}

	public DataManagementPanel(Map<String, String> lastErrorMap) {
		super();

		// Default error map which indicates the success of the form
		successMap = new HashMap<String, String>();

		// Create feedback box
		String errors = "";
		String cssclassDefault = "errorPanel";
		String cssclass = cssclassDefault;
		if (lastErrorMap != null && !lastErrorMap.isEmpty()) {
			for (Entry<String, String> entry : lastErrorMap.entrySet()) {
				if (entry.getKey().equals(successKey)) {
					// On success take only the message, not the key
					cssclass = "successPanel";
					errors = entry.getValue();
					continue;
				}
				if (!errors.isEmpty()) {
					errors += "; ";
				}
				errors += entry.getKey() + ": " + entry.getValue();
			}
		}

		Label feedbackLabel = new Label("label.feedback", errors);
		feedbackLabel.add(new AttributeAppender("class", new Model<String>(
				cssclass), " "));

		// Hide feedback box if there is no feedback
		if (errors.isEmpty() && cssclass.equals(cssclassDefault)) {
			feedbackLabel.add(new AttributeAppender("style", new Model<String>(
					"visibility:hidden"), ";"));
		}

		// Add feedback box
		add(feedbackLabel);

		final FileUploadField bundleUploadField = new FileUploadField(
				"uploadfield.bundle");

		// Bundle upload form
		Form<File> bundleUploadForm = new Form<File>("form.bundle") {
			/**
			 *
			 */
			private static final long serialVersionUID = 2219614181443056687L;

			@Override
			protected void onSubmit() {
				super.onSubmit();
				Map<String, String> errorMap = new TreeMap<>();
				FileUpload bundleUpload = bundleUploadField.getFileUpload();
				if (bundleUpload == null) {
					errorMap.put("Warning",
							"Please select a bundle file before upload.");
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							errorMap), new DataManagementSidebarPanel());
					return;
				}
				java.io.File bundleFile;
				try {
					bundleFile = File.createTempFile("Bundle", ".jar");
					FileOutputStream os = new FileOutputStream(bundleFile);
					int c = 0;
					byte[] buf = new byte[2048];
					InputStream is = bundleUpload.getInputStream();
					while ((c = is.read(buf)) > 0) {
						os.write(buf, 0, c);
					}
					os.close();
					successMap.put(successKey, "Bundle successfully uploaded!");
					bundleFile.deleteOnExit();
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							successMap), new DataManagementSidebarPanel());
				} catch (IOException e) {
					errorMap.put("IOException",
							"The file to upload could not be opened.");
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							errorMap), new DataManagementSidebarPanel());
					return;
				}
				errorMap = BundleManager.getInstance().upload(bundleFile);
				if (!errorMap.isEmpty()) {
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							errorMap), new DataManagementSidebarPanel());
				}
			}
		};

		bundleUploadForm.setMultiPart(true);
		bundleUploadForm.add(bundleUploadField);

		add(bundleUploadForm);

		final FileUploadField graphUploadField = new FileUploadField(
				"uploadfield.graph");

		final TextField<String> graphName = new TextField<String>(
				"text.graphname", Model.of(""));

		Form<File> graphUploadForm = new Form<File>("form.graph") {
			/**
			 *
			 */
			private static final long serialVersionUID = 875169738312512366L;

			@Override
			protected void onSubmit() {
				Map<String, String> errorMap = new HashMap<String, String>();
				super.onSubmit();
				FileUpload graphUpload = graphUploadField.getFileUpload();
				if (graphUpload == null) {
					errorMap.put("Warning",
							"Please select a graph instance file before upload.");
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							errorMap), new DataManagementSidebarPanel());
					return;
				}
				if (graphName.getValue().isEmpty()) {
					errorMap.put("Warning",
							"Please enter a name for the graph instance.");
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							errorMap), new DataManagementSidebarPanel());
					return;
				}
				File graphFile = new File(System.getProperty("java.io.tmpdir")
						+ "/" + graphUpload.getClientFileName());
				try {
					graphUpload.writeTo(graphFile);
					ControlServer.getInstance().uploadGraphInstance(
							graphFile.getPath(), graphName.getValue());
					successMap.put(successKey,
							"Graph instance successfully uploaded!");
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							successMap), new DataManagementSidebarPanel());
				} catch (Exception e) {
					errorMap.put(e.getClass().getName(),
							e.getLocalizedMessage());
					MonetMainPage.renderWithPanel(new DataManagementPanel(
							errorMap), new DataManagementSidebarPanel());
				}
			}
		};

		graphUploadForm.setMultiPart(true);
		graphUploadForm.add(graphName);
		graphUploadForm.add(graphUploadField);

		add(graphUploadForm);
	}
}
