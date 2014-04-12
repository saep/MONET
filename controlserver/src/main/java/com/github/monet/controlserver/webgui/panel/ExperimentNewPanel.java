package com.github.monet.controlserver.webgui.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;

import com.github.monet.common.AlgorithmBundleDescriptor;
import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.BundleValidationException;
import com.github.monet.common.DependencyManager;
import com.github.monet.controlserver.AlgorithmParameterList;
import com.github.monet.controlserver.CheckBoxConverter;
import com.github.monet.controlserver.ControlServerException;
import com.github.monet.controlserver.Experiment;
import com.github.monet.controlserver.Parameter;
import com.github.monet.controlserver.ParameterException;
import com.github.monet.controlserver.webgui.MonetMainPage;
import com.github.monet.worker.WorkerJob;

/**
 * Form that collects all data that is needed to create a new experiment:
 * <ul>
 * <li>Name</li>
 * <li>Description</li>
 * <li>Priority</li>
 * <li>Algorithm</li>
 * <li>Parameters</li>
 * </ul>
 *
 * @author Marco Kuhnke
 */
public class ExperimentNewPanel extends ContentPanel {
	private static final long serialVersionUID = 4327864536053963333L;

	private AlgorithmParameterList parameterList;
	private ListView<Parameter> parameterListView;

	/**
	 * Constructor.
	 */
	public ExperimentNewPanel() {
		super();

		final ExperimentModel expModel = new ExperimentModel();

		// algorithm list
		ArrayList<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>(
				DependencyManager.getInstance().getAllBundles());
		final List<String> algorithmList = new ArrayList<String>(bundles.size());
		for (int p = 0; p < bundles.size(); p++) {
			if (bundles.get(p) instanceof AlgorithmBundleDescriptor)
				algorithmList.add(bundles.get(p).getDescriptor());
		}

		add(new Label(
				"expNewNoAlgorithm",
				!algorithmList.isEmpty() ? "Use the following form to add a new experiment."
						: "Please upload an algorithm first."));

		// form
		Form<ExperimentModel> form = new Form<ExperimentModel>("expNewForm") {
			private static final long serialVersionUID = -9164456511219367154L;

			@Override
			public boolean isVisible() {
				return !algorithmList.isEmpty();
			}

			@Override
			protected void onSubmit() {
				expModel.id = expModel.id.trim();
				if (expModel.id.isEmpty())
					error("You must enter a name.");
				else {
					Experiment exp;
					Map<String, Object> map;
					List<Parameter> params = parameterList.getParameterList();
					for (Parameter faulty : params) {
						faulty.setValue(expModel.params.get(faulty.getName()));
					}
					try {
						parameterList.validateParameters();
						map = new HashMap<String, Object>(2);
						map.put(WorkerJob.KEY_ALGORITHM, expModel.algo);
						map.put(WorkerJob.KEY_PARAMETERS,
								parameterList.getParameterMap());
						try {
							exp = new Experiment(expModel.id, expModel.descr);
							exp.setMap(map);
							exp.setPriority(expModel.priority);
							MonetMainPage.renderWithPanel(
									new ExperimentDetailsPanel(expModel.id),
									null);
						} catch (ControlServerException cse) {
							// An experiment with that name already exists
							error(cse.getMessage());
						}
					} catch (ParameterException e) {
						for (Parameter faultyParam : e.getFaultyParameters()) {
							error("Please enter a valid value for the "
									+ "parameter '" + faultyParam.getName()
									+ "'.");
						}
					}
				}
			}
		};

		form.add(new RequiredTextField<String>("expNewInputId",
				new PropertyModel<String>(expModel, "id"))
				.setLabel(new Model<String>("name")));

		form.add(new TextArea<String>("expNewInputDescr",
				new PropertyModel<String>(expModel, "descr")).setRequired(true)
				.setLabel(new Model<String>("description")));

		form.add(new RequiredTextField<Integer>("expNewInputPriority",
				new PropertyModel<Integer>(expModel, "priority")).setLabel(
				new Model<String>("priority")).add(
				new RangeValidator<Integer>(0, Integer.MAX_VALUE)));

		final WebMarkupContainer parameterContainer = new WebMarkupContainer(
				"expNewParameterContainer");
		parameterContainer.setOutputMarkupId(true);
		DropDownChoice<String> ddcAlgorithm = new DropDownChoice<String>(
				"expNewInputAlgorithm", new PropertyModel<String>(expModel,
						"algo"), algorithmList);
		ddcAlgorithm.setRequired(true).setLabel(new Model<String>("algorithm"));
		ddcAlgorithm.add(new AjaxFormComponentUpdatingBehavior("onchange") {
			private static final long serialVersionUID = -3405663949803885984L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				@SuppressWarnings("unchecked")
				String selectedAlgorithm = ((DropDownChoice<String>) getComponent())
						.getModelObject();
				if (selectedAlgorithm != null && !selectedAlgorithm.isEmpty()) {
					try {
						parameterList = new AlgorithmParameterList(
								selectedAlgorithm);
						parameterListView.setList(parameterList
								.getParameterList());
					} catch (BundleValidationException bve) {
						parameterListView.setList(null);
					}
				}
				target.add(parameterContainer);
			}

			@Override
			protected void onError(AjaxRequestTarget target, RuntimeException e) {
				parameterList = null;
				parameterListView.setList(null);
				target.add(parameterContainer);
			}
		});
		form.add(ddcAlgorithm);

		// Parameters
		parameterListView = new ListView<Parameter>("expNewInputParameters",
				new ArrayList<Parameter>(0)) {
			private static final long serialVersionUID = 2851230925250283773L;

			@Override
			protected void populateItem(ListItem<Parameter> item) {

				final Parameter param = item.getModelObject();
				item.add(new Label("expNewInputParameterName", param.getName()));
				item.add(new Label("expNewInputParameterDesc", param
						.getDescription()));

				final PropertyModel<String> paramValueModel = new PropertyModel<String>(
						expModel, "params." + param.getName());
				if (paramValueModel.getObject() == null) {
					paramValueModel.setObject((String) param.defaultValue());
				}

				FormComponent<String> paramValueChoice = new FormComponent<String>(
						"expNewInputParameterChoice") {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						return false;
					}
				};
				FormComponent<Boolean> paramValueCheckbox = new FormComponent<Boolean>(
						"expNewInputParameterCheckbox") {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						return false;
					}
				};
				FormComponent<String> paramValueText = new FormComponent<String>(
						"expNewInputParameterValue") {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						return false;
					}
				};
				if (param instanceof Parameter.ChoiceParameter) {
					// Choice -> Radio Buttons
					paramValueChoice = new RadioChoice<String>(
							"expNewInputParameterChoice", paramValueModel,
							((Parameter.ChoiceParameter) param).getChoices());
					paramValueChoice
							.add(new AjaxFormChoiceComponentUpdatingBehavior() {
								private static final long serialVersionUID = -1791974121454706797L;

								@Override
								protected void onUpdate(AjaxRequestTarget target) {

									List<Parameter> faultyParameters = parameterList
											.getParameterList();

									for (Parameter faulty : faultyParameters) {
										try {
											faulty.setValue(expModel.params
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
					paramValueCheckbox = new CheckBox(
							"expNewInputParameterCheckbox",
							new CheckBoxConverter(paramValueModel));
					paramValueCheckbox
							.add(new AjaxFormComponentUpdatingBehavior(
									"onchange") {
								private static final long serialVersionUID = 1L;

								@Override
								protected void onUpdate(AjaxRequestTarget target) {
									paramValueModel
											.setObject(getFormComponent()
													.getValue());
								}
							});
				} else {
					// String -> TextField
					paramValueText = new TextField<String>(
							"expNewInputParameterValue", paramValueModel);
					paramValueText.add(new AjaxFormComponentUpdatingBehavior(
							"onchange") {
						private static final long serialVersionUID = 1L;

						@Override
						protected void onUpdate(AjaxRequestTarget target) {
							paramValueModel.setObject(getFormComponent()
									.getValue());
						}
					});
				}
				item.add(paramValueChoice);
				item.add(paramValueCheckbox);
				item.add(paramValueText);

				item.add(new Label("expNewInputParameterDefault", "Default: "
						+ (String) param.defaultValue()) {
					private static final long serialVersionUID = 411120042052210649L;

					@Override
					public boolean isVisible() {
						return param.defaultValue() != null
								&& !((String) param.defaultValue()).isEmpty();
					}
				});
			}

			@Override
			public boolean isVisible() {
				return !this.getList().isEmpty();
			}

		};
		parameterContainer.add(parameterListView);
		form.add(parameterContainer);

		form.add(new Button("expNewSubmit"));
		add(form);
		add(new FeedbackPanel("expNewError"));

	}

	/**
	 * Model to temporarily store user input.
	 */
	private class ExperimentModel implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 868300393071393609L;
		public String id, descr, algo;
		public HashMap<String, String> params = new HashMap<String, String>();
		public int priority;
	}

}
