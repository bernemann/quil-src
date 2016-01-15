package org.quil.server.Tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quil.JSON.Document;
import org.quil.interpreter.Interpreter;
import org.quil.server.DocumentCache;

public class PricePortfolio extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PricePortfolio(String taskName, String taskXML) {
		super(taskName, taskXML);
	}

	@Override
	public void run() throws Exception {
		
		JSONParser parser = new JSONParser();

		final JSONObject taskDescription = (JSONObject) parser.parse(_taskDescription);

		String trades = (String) taskDescription.get("Trades");
		String predicate = (String) taskDescription.get("Where");

		List<Document> tradeList = DocumentCache.getOrCreate(trades).filter(predicate);

		
		ArrayList<IgniteCallable<JSONObject>> jobs = new ArrayList<IgniteCallable<JSONObject>> ();

		for (final Document trade : tradeList) {

			jobs.add (new IgniteCallable<JSONObject>() {
				@Override
				public JSONObject call() throws Exception {
					Interpreter interpreter = (Interpreter) Class.forName((String) taskDescription.get("Interpreter")).newInstance();

					JSONObject subTask = new JSONObject();
					subTask.put("Template", (String)taskDescription.get("Template"));
					subTask.put("Repository", (String)taskDescription.get("Repository"));
					subTask.put("MarketData", taskDescription.get("MarketData"));
					JSONObject tradeData = (JSONObject) (new JSONParser()).parse(trade.toString());
					subTask.put("TradeData", tradeData);

					interpreter.setData(subTask);
					interpreter.interpret();
					
					return interpreter.getResult();
				}
			});
		}
		
		Ignite ignite = Ignition.ignite();
        Collection<JSONObject> results = ignite.compute().call(jobs);
        
        Vector<String> resultsStr = new Vector<String>();
        for (JSONObject r : results) {
        	resultsStr.add(r.toJSONString());
        }

		Task.updateResult(_taskName, "[" + org.springframework.util.StringUtils.collectionToDelimitedString(results, ",") + "]");
	}

}