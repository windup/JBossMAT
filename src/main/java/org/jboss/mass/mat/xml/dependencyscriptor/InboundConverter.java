package org.jboss.mass.mat.xml.dependencyscriptor;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class InboundConverter implements Converter {

	public void marshal(Object arg0, HierarchicalStreamWriter arg1,
			MarshallingContext arg2) {
		// TODO Auto-generated method stub
	}

	public Object unmarshal(HierarchicalStreamReader arg0,
			UnmarshallingContext arg1) {
		// TODO Auto-generated method stub
		String type = arg0.getAttribute("type");
		String confirmed = arg0.getAttribute("confirmed");
		Inbound in = new Inbound(type, confirmed);
//		System.out.println("Inbound value=" + arg0.getValue());
		in.setValue(arg0.getValue());
		
		return (Object)in;
	}

	public boolean canConvert(java.lang.Class arg0) {
		// TODO Auto-generated method stub
		return arg0.equals(Inbound.class);
	}

}
