
package jef.com.sun.xml.bind.v2.schemagen.xmlschema;

import jef.com.sun.xml.txw2.TypedXmlWriter;
import jef.com.sun.xml.txw2.annotation.XmlElement;

@XmlElement("simpleContent")
public interface SimpleContent
    extends Annotated, TypedXmlWriter
{


    @XmlElement
    public SimpleExtension extension();

    @XmlElement
    public SimpleRestriction restriction();

}
