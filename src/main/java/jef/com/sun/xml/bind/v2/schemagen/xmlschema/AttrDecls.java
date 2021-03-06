
package jef.com.sun.xml.bind.v2.schemagen.xmlschema;

import jef.com.sun.xml.txw2.TypedXmlWriter;
import jef.com.sun.xml.txw2.annotation.XmlElement;

public interface AttrDecls
    extends TypedXmlWriter
{


    @XmlElement
    public LocalAttribute attribute();

    @XmlElement
    public Wildcard anyAttribute();

}
