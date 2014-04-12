package com.github.monet.controlserver.webgui.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;

import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.BundleValidationException;
import com.github.monet.common.DependencyManager;
import com.github.monet.common.Tuple;
import com.github.monet.controlserver.CSJob;
import com.github.monet.controlserver.CheckBoxConverter;
import com.github.monet.controlserver.ControlServer;
import com.github.monet.controlserver.ControlServerException;
import com.github.monet.controlserver.Experiment;
import com.github.monet.controlserver.Parameter;
import com.github.monet.controlserver.ParameterException;
import com.github.monet.controlserver.ParameterList;
import com.github.monet.controlserver.ParserParameterList;
import com.github.monet.controlserver.Scheduler;
import com.github.monet.controlserver.WorkerDescriptor;
import com.github.monet.controlserver.webgui.MonetMainPage;
import com.github.monet.worker.Job;
import com.github.monet.worker.WorkerJob;

/**
 * Overview about the experiment given by its {@code id}:
 * <ul>
 * <li>Name, state and priority</li>
 * <li>Worker list (assign / unassign)</li>
 * <li>Job list (link to {@link JobDetailsPanel} / add / remove)</li>
 * <li>Summary (job count, running, failed, successful, ...)</li>
 * </ul>
 *
 * @author Marco Kuhnke
 */
public class ExperimentDetailsPanel extends ContentPanel {
	private static final long serialVersionUID = 6987921836406978033L;

	private static Logger LOG = LogManager
			.getLogger(ExperimentDetailsPanel.class);

	private WorkerDescriptor selectedWorker;
	private WebMarkupContainer parameterContainer;
	private ParameterList parameterList;
	private ListView<Parameter> parameterListView;

	/**
	 * Constructor that expects the {@code id} of the experiment as argument.
	 *
	 * @param id
	 */
	@SuppressWarnings("unchecked")
	public ExperimentDetailsPanel(String id) {
		super();
		final Scheduler scheduler = ControlServer.getScheduler();
		final Experiment experiment = Experiment.getExperiment(id);

		WebMarkupContainer container = new WebMarkupContainer("container");
		WebMarkupContainer notFound = new WebMarkupContainer("notFound");
		add(container);
		add(notFound);
		add(new Label("header.exp.name", new PropertyModel<String>(
				experiment, "name")));

		if (experiment == null) {

			notFound.add(new Label("notFoundId", id));
			container.setVisible(false);

		} else {

			notFound.setVisible(false);

			add(new Label("header.state", new PropertyModel<String>(
					experiment, "state")));

			setDefaultModel(new CompoundPropertyModel<Experiment>(experiment));
			container.add(new Label("name"));
			container.add(new Label("description"));
			container.add(new Label("state"));
			container.add(new Label("dateCreated",
					experiment.getCreatedDate() != null
							? experiment.getCreatedDate().toString() : ""));
			container.add(new Label("dateStarted",
					experiment.getStartedDate() != null
							? experiment.getStartedDate().toString() : ""));
			container.add(new Label("dateFinished",
							experiment.getFinishedDate() != null
							? experiment.getFinishedDate().toString() : ""));
			container.add(new Label("priority"));

			Map<String, Object> map;
			Map<String, Object> params;
			map = experiment.getHashmap();
			container.add(new Label("algorithm", (String) map
					.get(WorkerJob.KEY_ALGORITHM)));

			String paramsString = "";
			if (map.get(WorkerJob.KEY_PARAMETERS) instanceof Map) {
				params = (Map<String, Object>) map
						.get(WorkerJob.KEY_PARAMETERS);
				if (!params.isEmpty()) {
					for (Map.Entry<String, Object> entry : params.entrySet()) {
						paramsString += entry.getKey() + ":" + " "
								+ entry.getValue() + "\n";
					}
				}
			}
			container.add(new MultiLineLabel("parameters", paramsString));

			// worker list
			final WebMarkupContainer workerAnchor = new WebMarkupContainer(
					"workerAnchor");
			workerAnchor.setOutputMarkupId(true);
			container.add(workerAnchor);
			final List<WorkerDescriptor> workerList = experiment
					.getAssignedWorkers();
			final ListView<WorkerDescriptor> assignedListView = new ListView<WorkerDescriptor>(
					"assignedListView", workerList) {
				private static final long serialVersionUID = 1L;

				@Override
				protected void populateItem(
						final ListItem<WorkerDescriptor> item) {
					Link<WorkerDescriptor> assignedLink = new Link<WorkerDescriptor>(
							"assignedLink") {
						private static final long serialVersionUID = -5368042822746332021L;

						@Override
						public void onClick() {
							MonetMainPage.renderWithPanel(
									new WorkerDetailsPanel(item
											.getModelObject()),
									null);
						}
					};
					assignedLink.add(new Label("assignedLabel", item
							.getModelObject().getName()));
					item.add(assignedLink);
					Link<WorkerDescriptor> assignedUnassignLink = new Link<WorkerDescriptor>(
							"assignedUnassign") {
						private static final long serialVersionUID = -2055466713422852795L;

						@Override
						public void onClick() {
							experiment.unassignWorker(item.getModelObject());
						}
					};
					assignedUnassignLink.setAnchor(workerAnchor);
					item.add(assignedUnassignLink);
				}
			};
			WebMarkupContainer assignedListViewTable = new WebMarkupContainer(
					"assignedListViewTable") {
				private static final long serialVersionUID = -5251106108374868975L;

				@Override
				public boolean isVisible() {
					return !assignedListView.getList().isEmpty();
				}
			};
			assignedListViewTable.add(assignedListView);
			container.add(assignedListViewTable);
			container.add(new WebMarkupContainer("assignedNotFound") {
				private static final long serialVersionUID = -5251106108374858975L;

				@Override
				public boolean isVisible() {
					return assignedListView.getList().isEmpty();
				}
			});

			// assign worker form
			final List<WorkerDescriptor> workerListRegistered = new ArrayList<WorkerDescriptor>(
					scheduler.getWorkers());
			workerListRegistered.removeAll(workerList);
			Form<Experiment> assignedForm = new Form<Experiment>("assignedForm") {
				private static final long serialVersionUID = -2967515099481830194L;

				@Override
				public boolean isVisible() {
					String[] finalStates = new String[] {
							Experiment.STATE_CANCELLING,
							Experiment.STATE_CANCELLED,
							Experiment.STATE_FAILED,
							Experiment.STATE_PARTIAL_SUCCESS,
							Experiment.STATE_SUCCESS };
					if (Arrays.asList(finalStates).contains(
							experiment.getState())) {
						return false;
					}
					workerListRegistered.clear();
					workerListRegistered
							.addAll(new ArrayList<WorkerDescriptor>(
									ControlServer.getScheduler().getWorkers()));
					workerListRegistered.removeAll(workerList);
					return !workerListRegistered.isEmpty();
				}

				@Override
				protected void onSubmit() {
					if (ExperimentDetailsPanel.this.selectedWorker != null) {
						experiment
								.assignWorker(ExperimentDetailsPanel.this.selectedWorker);
					}
				}
			};
			assignedForm.add(new AttributeAppender("action", "#"
					+ workerAnchor.getMarkupId()));
			container.add(assignedForm);
			DropDownChoice<WorkerDescriptor> workerListDropdown = new DropDownChoice<WorkerDescriptor>(
					"assignedDropdown", new PropertyModel<WorkerDescriptor>(
							this, "selectedWorker"), workerListRegistered,
					new ChoiceRenderer<WorkerDescriptor>() {
						private static final long serialVersionUID = -1014193251164368134L;

						@Override
						public Object getDisplayValue(WorkerDescriptor wd) {
							return wd.getName();
						}
					});
			assignedForm.add(workerListDropdown);
			assignedForm.add(new Button("assignedButtonNew"));

			// job list
			final WebMarkupContainer jobListAnchor = new WebMarkupContainer(
					"jobListAnchor");
			jobListAnchor.setOutputMarkupId(true);
			container.add(jobListAnchor);
			final ListView<CSJob> jobListView = new ListView<CSJob>(
					"jobListView", experiment.getJobs()) {
				private static final long serialVersionUID = 1L;

				@Override
				protected void populateItem(final ListItem<CSJob> item) {
					Link<CSJob> jobLink = new Link<CSJob>("jobLink") {
						private static final long serialVersionUID = 4919349844168396043L;

						@Override
						public void onClick() {
							MonetMainPage.renderWithPanel(new JobDetailsPanel(
									item.getModelObject()), null);
						}
					};
					jobLink.add(new Label("jobLabel", item.getModelObject()
							.getID()));
					item.add(jobLink);

					item.add(new Label("jobState", new PropertyModel<String>(
							item.getModel(), "state")));

					Link<CSJob> jobRemoveLink = new Link<CSJob>("jobLinkRemove") {
						private static final long serialVersionUID = 8439121904597866518L;

						@Override
						public boolean isVisible() {
							return experiment.getState().equalsIgnoreCase(
									Experiment.STATE_NEW);
						}

						@Override
						public void onClick() {
							item.getModelObject().cancel();
							try {
								experiment.removeJob(item.getModelObject());
							} catch (ControlServerException e) {
								error("Could not remove this job!");
								System.out.println("ControlServerException "
										+ "while trying to remove a job: "
										+ e.getMessage());
							}
						}
					};
					jobRemoveLink.setAnchor(jobListAnchor);
					item.add(jobRemoveLink);

					Link<CSJob> jobCancelLink = new Link<CSJob>("jobCancel") {
						private static final long serialVersionUID = 5982000987961294731L;

						@Override
						public boolean isVisible() {
							return !(item.getModelObject().getState().isFinal() || experiment
									.getState().equalsIgnoreCase(
											Experiment.STATE_NEW));
						};

						@Override
						public void onClick() {
							ControlServer.getInstance().killJob(
									item.getModelObject());
						};
					};

					jobCancelLink.setAnchor(jobListAnchor);
					item.add(jobCancelLink);

					Link<CSJob> jobLinkResults = new Link<CSJob>(
							"jobLinkResults") {
						private static final long serialVersionUID = 5982000987961294731L;

						@Override
						public boolean isVisible() {
							return (item.getModelObject().getState()
									.equals(CSJob.State.SUCCESS));
						}

						@Override
						public void onClick() {
							MonetMainPage.renderWithPanel(
									new JobSingleResultPanel(item
											.getModelObject()),
									new ExperimentSidebarPanel());
						}
					};

					jobLinkResults.setAnchor(jobListAnchor);
					item.add(jobLinkResults);
				}
			};
			WebMarkupContainer jobListViewTable = new WebMarkupContainer(
					"jobListViewTable") {
				private static final long serialVersionUID = 1407639995658429999L;

				@Override
				public boolean isVisible() {
					return experiment.getJobCount() > 0;
				}
			};
			jobListViewTable.add(jobListView);
			container.add(jobListViewTable);
			container.add(new WebMarkupContainer("jobNotFound") {
				private static final long serialVersionUID = 7072863681611979182L;

				@Override
				public boolean isVisible() {
					return jobListView.getList().isEmpty();
				}
			});

			// "add job" form
			List<Tuple<String, String>> graphs = ControlServer.getInstance()
					.getUploadedGraphInstances();
			final List<String> graphFileList = new ArrayList<String>(
					graphs.size());
			for (Tuple<String, String> g : ControlServer.getInstance()
					.getUploadedGraphInstances()) {
				graphFileList.add(g.getSecond());
			}

			ArrayList<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>(
					DependencyManager.getInstance().getAllBundles());
			final List<String> graphParserList = new ArrayList<String>(
					bundles.size());
			for (int p = 0; p < bundles.size(); p++) {
				if (bundles.get(p).kind().equals(BundleDescriptor.Kind.PARSER))
					graphParserList.add(bundles.get(p).getDescriptor());
			}

			final JobModel jobModel = new JobModel();
			final Form<Experiment> jobForm = new Form<Experiment>("jobForm") {
				private static final long serialVersionUID = -7341241821151037600L;

				@Override
				public boolean isVisible() {
					return experiment.getState().equalsIgnoreCase(Experiment.STATE_NEW)
							&& !graphFileList.isEmpty()
							&& !graphParserList.isEmpty();
				}

				@Override
				protected void onSubmit() {
					HashMap<String, Object> hashmap = new HashMap<String, Object>();
					hashmap.putAll(experiment.getHashmap());
					hashmap.put(WorkerJob.KEY_GRAPHFILE, jobModel.graphFile);
					hashmap.put(WorkerJob.KEY_GRAPHPARSER, jobModel.graphParser);
					CSJob job = new CSJob(experiment, hashmap);

					if (jobModel.params != null) {
						List<Parameter> params = parameterList
								.getParameterList();
						for (Parameter check : params) {
							check.setValue(jobModel.params.get(check.getName()));
						}
						try {
							parameterList.validateParameters();
							job.putParameter(
									(String) CSJob.KEY_PARSER_PARAMETERS,
									parameterList.getParameterMap());
							try {
								experiment.addJobs(job, jobModel.times);
							} catch (ControlServerException e) {
								error("Could not add the new job: "
										+ e.getMessage());
							}
						} catch (ParameterException e) {
							for (Parameter faultyParam : e
									.getFaultyParameters()) {
								error("Please enter a valid value for the "
										+ "parameter '" + faultyParam.getName()
										+ "'.");
							}
						}
					}
				}
			};
			jobForm.add(new AttributeAppender("action", "#"
					+ jobListAnchor.getMarkupId()));

			jobForm.add(new DropDownChoice<String>("jobInputGraphFile",
					new PropertyModel<String>(jobModel, "graphFile"),
					graphFileList).setRequired(true).setLabel(
					new Model<String>("graph file")));

			parameterContainer = new WebMarkupContainer("parameterContainer");
			parameterContainer.setOutputMarkupId(true);
			final Label parameterException = new Label("parameterException",
					new Model<String>("")) {
				private static final long serialVersionUID = 1731943006954333501L;

				@Override
				public boolean isVisible() {
					return true;
				}
			};
			parameterContainer.add(parameterException);

			DropDownChoice<String> ddcGraphParser = new DropDownChoice<String>(
					"jobInputGraphParser", new PropertyModel<String>(jobModel,
							"graphParser"), graphParserList);
			ddcGraphParser.setRequired(true);
			ddcGraphParser.setLabel(new Model<String>("graph parser"));
			ddcGraphParser
					.add(new AjaxFormComponentUpdatingBehavior("onchange") {
						private static final long serialVersionUID = 5516528572657206557L;

						@Override
						protected void onUpdate(AjaxRequestTarget target) {
							String selectedGraph = ((DropDownChoice<String>) getComponent())
									.getModelObject();
							Model<String> parameterExceptionModel = new Model<String>(
									"");
							if (selectedGraph != null
									&& !selectedGraph.isEmpty()) {
								try {
									parameterList = new ParserParameterList(
											selectedGraph);
									parameterListView.setList(parameterList
											.getParameterList());
								} catch (BundleValidationException e) {
									parameterListView.setList(null);
									parameterExceptionModel = new Model<String>(
											"Error: " + e.getMessage());
								}
							}
							parameterException
									.setDefaultModel(parameterExceptionModel);
							target.add(parameterContainer);
						}
					});
			jobForm.add(ddcGraphParser);

			jobForm.add(new RequiredTextField<Integer>("jobInputTimes",
					new PropertyModel<Integer>(jobModel, "times")).add(
					new RangeValidator<Integer>(1, Integer.MAX_VALUE))
					.setLabel(new Model<String>("copies")));

			// Parameters
			parameterListView = new ListView<Parameter>("parameterContent",
					new ArrayList<Parameter>()) {

				private static final long serialVersionUID = -8936530684586005888L;

				@Override
				public boolean isVisible() {
					return !this.getList().isEmpty();
				}

				@Override
				protected void populateItem(ListItem<Parameter> item) {
					final Parameter param = item.getModelObject();
					item.add(new Label("parameterLabel", param.getName()));
					item.add(new Label("parameterDesc", param.getDescription()));

					final PropertyModel<String> paramValueModel = new PropertyModel<String>(
							jobModel, "params." + param.getName());
					if (paramValueModel.getObject() == null) {
						paramValueModel
								.setObject((String) param.defaultValue());
					}

					FormComponent<String> paramValueChoice = new FormComponent<String>(
							"parameterChoice") {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean isVisible() {
							return false;
						}
					};
					FormComponent<Boolean> paramValueCheckbox = new FormComponent<Boolean>(
							"parameterCheckbox") {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean isVisible() {
							return false;
						}
					};
					FormComponent<String> paramValueText = new FormComponent<String>(
							"parameterValue") {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean isVisible() {
							return false;
						}
					};
					if (param instanceof Parameter.ChoiceParameter) {
						// Choice -> Radio Buttons
						paramValueChoice = new RadioChoice<String>(
								"parameterChoice", paramValueModel,
								((Parameter.ChoiceParameter) param)
										.getChoices());
						paramValueChoice
								.add(new AjaxFormChoiceComponentUpdatingBehavior() {
									private static final long serialVersionUID = -1791974121454706797L;

									@Override
									protected void onUpdate(
											AjaxRequestTarget target) {

										List<Parameter> faultyParameters = parameterList
												.getParameterList();

										for (Parameter faulty : faultyParameters) {
											try {
												faulty.setValue(jobModel.params
														.get(faulty.getName()));
											} catch (IllegalArgumentException e) {
												faulty.setValue(null);
											}
										}

										parameterListView.setList(parameterList
												.getParameterList());
										target.add(parameterContainer);

									}
								});
					} else if (param instanceof Parameter.BooleanParameter) {
						// Boolean -> CheckBox
						paramValueCheckbox = new CheckBox("parameterCheckbox",
								new CheckBoxConverter(paramValueModel));
						paramValueCheckbox
								.add(new AjaxFormComponentUpdatingBehavior(
										"onchange") {
									private static final long serialVersionUID = 1L;

									@Override
									protected void onUpdate(
											AjaxRequestTarget target) {
										paramValueModel
												.setObject(getFormComponent()
														.getValue());
									}
								});
					} else {
						// String -> TextField
						paramValueText = new TextField<String>(
								"parameterValue", paramValueModel);
						paramValueText
								.add(new AjaxFormComponentUpdatingBehavior(
										"onchange") {
									private static final long serialVersionUID = 1L;

									@Override
									protected void onUpdate(
											AjaxRequestTarget target) {
										paramValueModel
												.setObject(getFormComponent()
														.getValue());
									}
								});
					}
					item.add(paramValueChoice);
					item.add(paramValueCheckbox);
					item.add(paramValueText);

					item.add(new Label("parameterDefault", "Default: "
							+ param.defaultValue()) {
						private static final long serialVersionUID = -8086591890038034828L;

						@Override
						public boolean isVisible() {
							return param.defaultValue() != null
									&& !param.defaultValue().isEmpty();
						}
					});
				}
			};
			parameterContainer.add(parameterListView);
			jobForm.add(parameterContainer);
			// End of Parameters

			jobForm.add(new FeedbackPanel("jobFormError"));
			jobForm.add(new Button("jobButtonNew"));
			container.add(jobForm);

			container
					.add(new Label(
							"jobFormInvisible",
							"No Job can be added"
									+ (!experiment.getState().equalsIgnoreCase(
											Experiment.STATE_NEW) ? " because the experiment is not in state NEW anymore."
											: (graphFileList.isEmpty() ? " because no graph instance has been uploaded yet."
													: (graphParserList
															.isEmpty() ? " because no graph parser has been uploaded yet."
															: ". Try reloading this page.")))) {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean isVisible() {
							return !jobForm.isVisible();
						}
					});

			container.add(new Link<Scheduler>("scheduleButton") {
				private static final long serialVersionUID = -3370585139611206236L;

				@Override
				public boolean isVisible() {
					if (scheduler.getExperiment(experiment.getName()) == null) {
						return experiment.getState().equalsIgnoreCase(
								Experiment.STATE_NEW)
								&& experiment.getJobCount() > 0;
					} else {
						return false;
					}
				}

				@Override
				public void onClick() {
					try {
						scheduler.addExperiment(experiment);
						this.setVisible(false);
					} catch (ControlServerException e) {
						error("Could not start this experiment!");
						LOG.error(e.getMessage());
					}
				}

			});

			// summary
			container.add(new Label("sumCount", experiment.getJobCount()));
			container
					.add(new Label("sumRunning", experiment.getRunningCount()));
			container.add(new Label("sumInitialized", experiment
					.getInitializedCount()));
			container.add(new Label("sumProcessed", experiment
					.getProcessedCount()));
			container
					.add(new Label("sumSuccess", experiment.getSuccessCount()));
			container.add(new Label("sumFailed", experiment.getFailedCount()));
			container.add(new Label("sumCancelled", experiment
					.getCancelledCount()));
			container
					.add(new Label("sumAborted", experiment.getAbortedCount()));

		}

//		container.add(new Link<Void>("linkbackTop") {
//			private static final long serialVersionUID = 877401436832763341L;
//
//			@Override
//			public void onClick() {
//				MonetMainPage.renderWithPanel(new ExperimentListPanel(),
//						null);
//			}
//		});
		container.add(new Link<Void>("linkbackTop") {
			private static final long serialVersionUID = 877401436832763342L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new ExperimentListPanel(), null);
			}
		});

		container.add(new Link<Void>("linkResults") {
			private static final long serialVersionUID = 877301436832763342L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new JobResultsPanel(experiment.getName()),
						null);
			}

			@Override
			public boolean isVisible() {
				for (CSJob job : experiment.getJobs()) {
					if (!job.getState().equals(Job.State.SUCCESS)) {
						return false;
					}
				}
				return true;
			}
		});
		add(new FeedbackPanel("expDetailsError"));
	}

	/**
	 * Model to temporarily store user input.
	 */
	private class JobModel implements Serializable {
		private static final long serialVersionUID = -7379016136772085074L;
		public int times = 1;
		public String graphFile, graphParser;
		public HashMap<String, String> params = new HashMap<String, String>();
	}

}
