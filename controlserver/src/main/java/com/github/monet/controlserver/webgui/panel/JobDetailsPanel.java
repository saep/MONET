package com.github.monet.controlserver.webgui.panel;

import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.link.Link;

import com.github.monet.common.logging.LogEvent;
import com.github.monet.controlserver.CSJob;
import com.github.monet.controlserver.WorkerDescriptor;
import com.github.monet.controlserver.webgui.MonetMainPage;

/**
 * Job details:
 * <ul>
 * <li>ID, state</li>
 * <li>Worker (link to {@link WorkerDetailsPanel})</li>
 * <li>Algorithm, parameters</li>
 * <li>Graph, parser</li>
 * </ul>
 *
 * @author Marco Kuhnke
 */
public class JobDetailsPanel extends ContentPanelWithLog {
	private static final long serialVersionUID = 6168177550908141237L;

	private CSJob job;

	@SuppressWarnings("rawtypes")
	public JobDetailsPanel(final CSJob csjob) {
		super();

		this.job = csjob;

		add(new WebMarkupContainer("notFound") {
			private static final long serialVersionUID = 1L;
			@Override public boolean isVisible() {
				return job == null;
			}
		});
		WebMarkupContainer container = new WebMarkupContainer("container") {
			private static final long serialVersionUID = -6170491097947727715L;
			@Override public boolean isVisible() {
				return job != null;
			}
		};
		add(container);

		container.add(new Label("id", job.getID()));
		container.add(new Label("state", job.getState().getName()));
		container.add(new Label("dateStarted",
				job.getStartedDate() != null
						? job.getStartedDate().toString() : ""));
		container.add(new Label("dateFinished",
				job.getFinishedDate() != null
						? job.getFinishedDate().toString() : ""));

		final WorkerDescriptor worker = job.getWorker();
		Link workerLink = new Link("workerLink") {
			private static final long serialVersionUID = 3683453900472721026L;
			@Override public boolean isVisible() {
				return worker != null;
			}
			@Override public void onClick() {
				MonetMainPage.renderWithPanel(new WorkerDetailsPanel(
						worker), null);
			}
		};
		workerLink.add(new Label("workerLabel",
				worker == null ? "" : worker.getName()));
		container.add(workerLink);

		container.add(new Label("graph",
				job == null ? "" : job.getGraphDescriptor()));
		container.add(new Label("parser",
				job == null ? "" : job.getParserDescriptor()));

		String paramsString = "";
		Map<String, Object> params =
				job.getParameters((String) CSJob.KEY_PARSER_PARAMETERS);
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			paramsString += entry.getKey() + ":" + " "
					+ entry.getValue() + "\n";
		}
		container.add(new MultiLineLabel("parameters", paramsString));

		Link linkback = new Link("linkback") {
			private static final long serialVersionUID = 877401436832763342L;
			@Override public void onClick() {
				MonetMainPage.renderWithPanel(
						job == null
							? new ExperimentListPanel()
							: new ExperimentDetailsPanel(
								job.getParentExperiment().getName()),
						null);
			}
		};
		linkback.add(new Label("linkbackExperiment",
				job == null ? "the list of experiments" :
				job.getParentExperiment().getName()));
		add(linkback);

		// Find & add log
		LogViewer logViewer = new LogViewer(this, job.getLog().iterator());
		logViewer.addLogListView();
	}

	public Iterator<LogEvent> getLogEventIterator() {
		return job.getLog().iterator();
	}
}
