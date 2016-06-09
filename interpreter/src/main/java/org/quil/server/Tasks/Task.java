package org.quil.server.Tasks;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.IgniteExceptionRegistry;
import org.apache.ignite.resources.LoggerResource;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.cache.Cache.Entry;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.UUID;

public abstract class Task implements Serializable {
	
	@LoggerResource
    private static IgniteLogger logger;

	@QuerySqlField(index=true)
	protected String _taskName = "";
	
	@QuerySqlField
	protected String _taskDescription = "";
	
	@QuerySqlField
	protected String _taskResult = "";
	
	@QuerySqlField
	protected String _taskTag = "";
	
	@QuerySqlField
	protected int _taskStatus = 0;
	
	static class Status {
		final static int PENDING = 0;
		final static int RUNNING = 1;
		final static int FINISHED = 2;
		final static int ERROR = 3;
	}
	
	
	public static Task fromString(String taskDescription) {
		String taskName = UUID.randomUUID().toString();
		
		Ignite ignite = Ignition.ignite();
		CacheConfiguration<String, Task> cfg = new CacheConfiguration<String, Task>();
		
        cfg.setCacheMode(CacheMode.REPLICATED);
        cfg.setName("Tasks");

		try {
			cfg.setIndexedTypes(String.class, Task.class,
					String.class, PriceTrade.class,
					String.class, PricePortfolio.class,
					String.class, ScriptedTask.class,
					String.class, Class.forName("org.quil.server.Tasks.RunQLObjectsApplication"));
		}catch (Exception e) {
			logger.info("Failed to set indexed types");
		}
        
        IgniteCache<String,Task> tasks = ignite.getOrCreateCache(cfg);     
        
        try {
			JSONObject taskObj = (JSONObject) (new JSONParser()).parse(taskDescription);
			
			Constructor c =  Class.forName("org.quil.server.Tasks." + (String) taskObj.get("Task")).getConstructor(String.class, String.class);
			Task task = (Task) c.newInstance(taskName, taskDescription);
			tasks.put(taskName,task);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return tasks.get(taskName);
	}
	
	static Task fromJSONObject(JSONObject taskDescription) {
		return fromString(taskDescription.toJSONString());
	}
	
	public static Task get(String taskName) {
		
		Ignite ignite = Ignition.ignite();
        IgniteCache<String,Task> tasks = ignite.getOrCreateCache("Tasks");
        
		return tasks.get(taskName);
	}
	
	public static void updateStatus(String taskName, int status) {
		
		Ignite ignite = Ignition.ignite();
        IgniteCache<String,Task> tasks = ignite.getOrCreateCache("Tasks");
        Task task = tasks.get(taskName); 
        task.setStatus(status);
        tasks.put(taskName, task);
	}
	
	public static void updateResult(String taskName, String result) {
		Ignite ignite = Ignition.ignite();
        IgniteCache<String,Task> tasks = ignite.getOrCreateCache("Tasks");
        Task task = tasks.get(taskName); 
        task.setResult(result);
        
        tasks.put(taskName, task);
	}
	
	public static HashMap<String, Task> allTasks() {
		Ignite ignite = Ignition.ignite();
		CacheConfiguration<String, Task> cfg = new CacheConfiguration<String, Task>();
		
        cfg.setCacheMode(CacheMode.REPLICATED);
        cfg.setName("Tasks");
		try {
			cfg.setIndexedTypes(String.class, Task.class,
					String.class, PriceTrade.class,
					String.class, PricePortfolio.class,
					String.class, ScriptedTask.class,
					String.class, Class.forName("org.quil.server.Tasks.RunQLObjectsApplication"));
		}catch (Exception e) {
			logger.info("Failed to set indexed types");
		}
        IgniteCache<String,Task> tasks = ignite.getOrCreateCache(cfg);     
        
        HashMap<String, Task> all = new HashMap<String, Task>();
        
        for (Entry<String, Task> entry : tasks) {
        	all.put(entry.getKey(),entry.getValue());
        }
        
		return all;
	}
	
	public Task(String taskName, String taskXML) {
		_taskStatus = Status.PENDING;
		_taskDescription = taskXML;
		_taskName = taskName;
		
		JSONParser parser = new JSONParser();

		try {
			final JSONObject taskDescription = (JSONObject) parser.parse(_taskDescription);
			
			if (taskDescription.containsKey("Tag"))
				_taskTag = (String)taskDescription.get("Tag");
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void setStatus(int status) {
		_taskStatus = status;
	}
	
	public String getDescription() {
		return _taskDescription;
	}
	
	public void setResult(String result) {
		 _taskResult = result;
	}
	
	public String getResult() {
		return _taskResult;
	}
	
	public String getName() {
		return _taskName;
	}
	
	public int getStatus() {
		return _taskStatus;
	}
	
	public String toJSONString()  {
		return toJSONObj().toJSONString();
	}
	
	public JSONObject toJSONObj()  {
		
		JSONObject obj = new JSONObject();
		obj.put("name", _taskName);
		obj.put("status", _taskStatus);
		obj.put("result", _taskResult);
		obj.put("tag", _taskTag);
	
		return obj;
	}
	
	abstract public void run() throws Exception;
}
