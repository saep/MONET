package com.github.monet.controlserver.webgui.panel;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;

import com.github.monet.controlserver.MeasuredData;
import com.mongodb.BasicDBObject;

/**
 * @author Jakob Bossek
 */
public class JobResultsPanel extends ContentPanel {
  private static final long serialVersionUID = 6987921836406978033L;

  /**
   * Constructor that expects the {@code id} of the experiment as argument.
   * @param id
   */
  public JobResultsPanel(String id) {
    super();

    BasicDBObject query = new BasicDBObject("parentExperiment", id);
    MeasuredData data = new MeasuredData(query);
    List<Long> runtimes = data.getRuntimes();

    Label jsRuntimes = new Label("jsRuntimes", this.generateJavaScriptForRuntimeBoxplots(runtimes));
    jsRuntimes.setEscapeModelStrings(false);

    add(jsRuntimes);
  }

  private String generateJavaScriptForRuntimeBoxplots(List<Long> runtimes) {
    String factor = "";
    String sample = "";
    String data = "";

    int n = runtimes.size();

    for (int i = 0; i < n; i++) {
      factor += "'Algorithm'";
      sample += "'s" + (i+1) + "'";
      data += runtimes.get(i) / (1000000000f * 60f);
      if (i < (n-1)) {
        factor += ", ";
        sample += ", ";
        data   += ", ";
      }
    }

    String js = "var showGraph = function() {";
    js += "var boxplot = new CanvasXpress('jsRuntimes',{ 'z' : { 'Description' : ['Boxplot of algorithm runtime'] },";
        js += "'x' : { 'Factor' : [" + factor + "]";
        js += "}, 'y' : { 'vars' : ['runtime'],";
        js += "'smps' : [" + sample + "],";
        js += "'data' : [[" + data + "]],";
        js += "'desc' : ['runtime']";
        js += "}, 'm' : { 'Name' : 'Algorithm runtime', 'Description' : 'Distribution of algorithm runtimes for the experiment.' } },{";
        js += "'axisTitleFontStyle': 'normal', 'graphType': 'Boxplot', 'showLegend': false, 'showShadow': false, 'smpLabelScaleFontFactor': 0.8, 'title': 'Boxplot of runtime distribution', 'xAxisTitle': 'Runtime (in minutes)', 'showBoxplotOriginalData': true, 'boxplotDataPointRatio': 3, 'jitter': true, 'imageDir': 'assets/images/canvasExpress/'});";
        js += "boxplot.groupSamples(['Factor']); }; window.setTimeout('showGraph()', 1000);";
    return(js);
  }

}
