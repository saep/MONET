package com.github.monet.controlserver.webgui.panel;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.github.monet.controlserver.Experiment;
import com.github.monet.controlserver.webgui.MonetMainPage;

/**
 * Provides a list of all experiments with associated information:
 * <ul>
 * 	<li>Name (with link to corresponding {@link ExperimentDetailsPanel})</li>
 * 	<li>State</li>
 * 	<li>Priority</li>
 * </ul>
 *
 * @author Marco Kuhnke
 */
public class ExperimentListPanel extends ContentPanel {
	private static final long serialVersionUID = 6707426634880480434L;

	/**
	 * Constructor.
	 */
	public ExperimentListPanel() {
		super();

		final List<Experiment> expList = new ArrayList<Experiment>(
				Experiment.getExperiments());

		WebMarkupContainer container = new WebMarkupContainer("expListViewTable") {
			private static final long serialVersionUID = -6218494464934332643L;
			@Override public boolean isVisible() {
				return !expList.isEmpty();
			}
		};
		add(container);
		container.add(new ListView<Experiment>("expListView", expList) {
			private static final long serialVersionUID = -4268993790733599752L;

			@Override
			protected void populateItem(final ListItem<Experiment> item) {
				Link<String> expLink =
						new Link<String>("expLink", new PropertyModel<String>(
								item.getModel(), "name")) {
					private static final long serialVersionUID = 3932928456409937465L;
					@Override public void onClick() {
						MonetMainPage.renderWithPanel(
								new ExperimentDetailsPanel(item.getModel()
										.getObject().getName()),
								null);
					}

				};
				expLink.add(new Label("expName", new PropertyModel<String>(item
						.getModel(), "name")));
				item.add(expLink);
				item.add(new Label("expNumberOfJobs", item.getModelObject().getJobCount()));
				item.add(new Label("expNumberOfAssignedWorkers", item.getModelObject().getAssignedWorkers().size()));
				item.add(new Label("expDescription", new PropertyModel<String>(item.
						getModel(), "description")));
				item.add(new Label("expState", new PropertyModel<String>(item
						.getModel(), "state")));
				item.add(new Label("expPriority", new PropertyModel<Integer>(
						item.getModel(), "priority")));
				Link<Experiment> expCancelLink = new Link<Experiment>("expCancel") {
					private static final long serialVersionUID = 4398346284270472458L;

					@Override
					public void onClick() {
						item.getModelObject().cancel();
					}

					@Override
					public boolean isEnabled() {
						return
								!Experiment.FINISH_STATES.contains(
										item.getModelObject().getState()) &&
								!Experiment.STATE_CANCELLING.equalsIgnoreCase(
										item.getModelObject().getState());
					}
				};

				/*
				 *  links get special class to let them appear disabled if job state is in
				 *  one of the finish states
				 */
				if (Experiment.FINISH_STATES.contains(item.getModelObject().getState())) {
					expCancelLink.add(new AttributeAppender("class", new Model<String>("disabled"), " "));
				}
				item.add(expCancelLink);

				// Repeat Button
				Link<Experiment> expRepeat = new Link<Experiment>("expRepeat") {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick() {
						item.getModelObject().repeat();
						MonetMainPage.renderWithPanel(new ExperimentListPanel(),
								null);
					}

					@Override
					public boolean isEnabled() {
						return true; // change if button should only appear if experiment has been run
					}
				};
				item.add(expRepeat);
			}

		});

		add(new Label("expCount", expList.size()));
		add(new Link<Void>("expNew") {
			private static final long serialVersionUID = 8771854695814233379L;
			@Override public void onClick() {
				MonetMainPage.renderWithPanel(
						new ExperimentNewPanel(),
						null);
			}
		});

	}

}
