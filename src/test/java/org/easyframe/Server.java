package org.easyframe;

import jef.tools.ThreadUtils;

import org.easyframe.cxfplus.CXFPlusServlet;
import org.easyframe.cxfplus.support.SimpleServiceLookup;
import org.easyframe.jaxrs.PeopleService;
import org.easyframe.jaxrs.PeopleServiceImpl;
import org.easyframe.jaxrs.PeopleServiceXml;
import org.easyframe.jaxws.HelloService;
import org.easyframe.jaxws.HelloServiceImpl;
import org.junit.Test;

public class Server {
	@Test
	public void start(){
		
		SimpleServiceLookup data=new SimpleServiceLookup();
		PeopleServiceImpl p=new PeopleServiceImpl();
		data.addService(PeopleServiceXml.class, p);
		data.addService(HelloService.class, new HelloServiceImpl());
		data.addService(PeopleService.class, p);
		CXFPlusServlet servlet=new CXFPlusServlet();
		servlet.setRSlookup(data);
		servlet.setWSlookup(data);
		servlet.initParam();
		servlet.setHttpPrefix("http://localhost:80/services");
		servlet.processWebservice();
		servlet.processJaxRs();
		
		

		
		ThreadUtils.doWait(this);
	}
}
