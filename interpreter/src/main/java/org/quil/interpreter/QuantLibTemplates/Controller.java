package org.quil.interpreter.QuantLibTemplates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public abstract class Controller implements ApplicationContextAware 
{
	final static Logger logger = LoggerFactory.getLogger(Controller.class);

    protected  ApplicationContext _context; 
    protected boolean _error = false;
    
    public boolean getError() {
    	return _error;
    }
    
    public void setError(boolean error) {
    	_error = error;
    }

	public Controller()
	{
	}
	
	public Controller(String bean, String method)
	{
	}
	
	public  ApplicationContext getApplicationContext() {
	    return _context;
	}

	public void setApplicationContext(ApplicationContext ac) throws BeansException {
	       logger.debug("PricingController.setApplicationContext :   " + ac.toString()  );
	       _context = ac;
	}
	
	abstract public Parameters run();
}