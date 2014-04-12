package com.github.monet.controlserver;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * Converter to use CheckBoxes with Strings instead of Booleans.
 * Used by {@link ExperimentDetailsPanel} and {@link ExperimentNewPanel}.
 * 
 * @author Marco Kuhnke
 */
public class CheckBoxConverter extends Model<Boolean> {
	private static final long serialVersionUID = 1460140336658273522L;

	PropertyModel<String> wrappedModel = null;

	public CheckBoxConverter(PropertyModel<String> wrappedPropertyModel) {
		wrappedModel = wrappedPropertyModel;
	}

	@Override
	public Boolean getObject() {
		return Boolean.parseBoolean((String) wrappedModel.getObject());
	}

	@Override
	public void setObject(Boolean object) {
		wrappedModel.setObject(object.toString());
	}
}
