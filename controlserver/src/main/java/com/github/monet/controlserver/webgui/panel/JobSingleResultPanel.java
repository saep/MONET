package com.github.monet.controlserver.webgui.panel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;

import com.github.monet.controlserver.CSJob;
import com.github.monet.controlserver.MeasuredData;
import com.github.monet.controlserver.MeasuredParetoFront;

/**
 * @author Jakob Bossek
 */
public class JobSingleResultPanel extends ContentPanel {
	private static final long serialVersionUID = 6987921836406978033L;

	/**
	 * Constructor that expects the {@code id} of the job as argument.
	 *
	 * @param id
	 */
	public JobSingleResultPanel(CSJob job) {
		super();

		// load pareto front
		MeasuredData data = job.getMeasuredData();
		List<MeasuredParetoFront> paretoFrontList = data.getParetoFronts();

		/*
		 * Set up canvasExpress plot. Only applicable if pareto front was
		 * computed successfully and the corresponding problem is 2-dimensional.
		 */
		Label jsParetoFront = null;
		if (paretoFrontList.isEmpty()) {
			System.out
					.println("RESULTS: no pareto front available or problem more than two dimensions.");
			jsParetoFront = new Label("jsParetoFront", "");
		} else {
			jsParetoFront = new Label("jsParetoFront",
					JobSingleResultPanel.generateScatterplot(paretoFrontList.get(0)
							.getParetoFrontPoints(), "jsParetoFront", null));
		}
		jsParetoFront.setEscapeModelStrings(false);
		add(jsParetoFront);
	}

	public static String generateScatterplot(List<List<Double>> points,
			String container, HashMap<String, String> params) {
		int n = points.size();

		String js = "var scatterplot = new CanvasXpress('";
		js += container + "',";
		js += "{ 'y' : {";
		js += "'smps' : [ 'x1', 'x2' ], 'vars' : [";
		for (int i = 1; i <= n; i++) {
			js += "'p" + i + "'";
			if (i < n) {
				js += ", ";
			}
		}
		js += "], 'data' : [";
		for (int i = 0; i < n; i++) {
			js += "[";
			List<Double> point = points.get(i);
			for (int j = 0; j < point.size(); j++) {
				js += point.get(j);
				if (j < (point.size() - 1)) {
					js += ", ";
				}
			}
			js += "]";
			if (i < n-1) {
				js += ", ";
			}
		}
		js += "] },";
		js += "'m' : {'Name' : 'Pareto-Front', 'Description' : 'Pareto-Front' },";
		js += "'x' : { 'Description' : ['Pareto-Front'] }}, {";
		js += "'graphType': 'Scatter2D', 'imageDir': 'assets/images/canvasExpress/', 'title': 'Pareto-Front', 'xAxis': ['x1'], 'yAxis': ['x2']});";
		return (js);
	}

	public static void main(String[] args) {
		// test the generating code this way, because currently not possible to
		// finish experiments successfully on my machine
		List<List<Double>> points = new LinkedList<List<Double>>();
		LinkedList<Double> p1 = new LinkedList<Double>();
		p1.add(3.4);
		p1.add(5.4);
		LinkedList<Double> p2 = new LinkedList<Double>();
		p2.add(2.3);
		p2.add(3.2);

		points.add(p1);
		points.add(p2);
		String pf = JobSingleResultPanel.generateScatterplot(points, "jsParetoFront", null);
		System.out.println(pf);
	}
}
