package com.github.monet.controlserver.webgui.panel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;

import com.github.monet.controlserver.CSJob;
import com.github.monet.controlserver.ControlServer;
import com.github.monet.controlserver.Experiment;
import com.github.monet.controlserver.Scheduler;
import com.github.monet.controlserver.WorkerDescriptor;
import com.github.monet.controlserver.webgui.MonetMainPage;

public class WorkerListPanel extends ContentPanel {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 7760398775832508705L;

	public WorkerListPanel() {
		super();

		Scheduler scheduler = ControlServer.getScheduler();
		Collection<WorkerDescriptor> workers = scheduler.getWorkers();
		// Note: Final can be used here because panels are reconstructed every
		// time
		// the corresponding page is refreshed or called
		final List<WorkerDescriptor> workerList = new ArrayList<WorkerDescriptor>(
				workers);

		WebMarkupContainer container = new WebMarkupContainer("container.list") {
			private static final long serialVersionUID = 8617491931805371628L;

			@Override
			public boolean isVisible() {
				return !workerList.isEmpty();
			}
		};
		add(container);

		container.add(new Label("label.workercount", workers.size()));

		container
				.add(new ListView<WorkerDescriptor>("list.worker", workerList) {
					private static final long serialVersionUID = 1L;

					protected void populateItem(
							final ListItem<WorkerDescriptor> item) {

						Link<WorkerDescriptor> linkW = new Link<WorkerDescriptor>(
								"link.workerdetails", item.getModel()) {
							private static final long serialVersionUID = -5368042822746332021L;

							@Override
							public void onClick() {
								WorkerDescriptor wd = getModelObject();
								MonetMainPage.renderWithPanel(
										new WorkerDetailsPanel(wd),
										null);
							}
						};
						linkW.add(new Label("label.name",
								new PropertyModel<String>(item.getModel(),
										"name")));
						item.add(linkW);
						item.add(new Label("label.ip",
								new PropertyModel<String>(item.getModel(),
										"ip")));
						item.add(new Label("label.cpu",
								new PropertyModel<String>(item.getModel(),
										"cpu")));
						item.add(new Label("label.ram",
								new PropertyModel<String>(item.getModel(),
										"ram")));
						item.add(new Label("label.state",
								new PropertyModel<String>(item.getModel(),
										"state")));

						final CSJob job = item.getModel().getObject().getJob();

						Link<WorkerDescriptor> linkE = new Link<WorkerDescriptor>(
								"link.expdetails", item.getModel()) {
							private static final long serialVersionUID = -8421848153729370079L;

							@Override
							public void onClick() {
								Experiment exp = job.getParentExperiment();
								MonetMainPage.renderWithPanel(
										new ExperimentDetailsPanel(exp
												.getName()),
										null);
							}

							@Override
							public boolean isEnabled() {
								return (job != null);
							};
						};
						if (job != null) {
							linkE.add(new Label("label.exp.name",
									new PropertyModel<String>(job
											.getParentExperiment(), "name")));
						} else {
							linkE.add(new Label("label.exp.name", "None"));
						}
						item.add(linkE);

						Link<WorkerDescriptor> linkT = new Link<WorkerDescriptor>(
								"link.terminate", item.getModel()) {
							private static final long serialVersionUID = -5026628961205469074L;

							@Override
							public void onClick() {
								ControlServer.getInstance().killWorker(
										getModelObject());
								MonetMainPage.renderWithPanel(
										new WorkerListPanel(), null);
							};
						};
						item.add(linkT);
						Link<WorkerDescriptor> linkJ = new Link<WorkerDescriptor>(
								"link.endjob", item.getModel()) {
							private static final long serialVersionUID = -5026628961205469074L;

							@Override
							public void onClick() {
								ControlServer.getInstance().killJob(
										getModelObject());
								MonetMainPage.renderWithPanel(
										new WorkerListPanel(),
										null);
							};

							public boolean isVisible() {
								return getModelObject().getJob() != null;
							};
						};
						item.add(linkJ);
					}
				});

		WebMarkupContainer containerNoList = new WebMarkupContainer(
				"container.listempty") {
			private static final long serialVersionUID = 318481708107316837L;

			@Override
			public boolean isVisible() {
				return workerList.isEmpty();
			}
		};
		add(containerNoList);
	}
}
