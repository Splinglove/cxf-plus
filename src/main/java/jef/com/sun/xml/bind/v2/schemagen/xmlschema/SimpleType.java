
package jef.com.sun.xml.bind.v2.schemagen.xmlschema;

import jef.com.sun.xml.txw2.TypedXmlWriter;
import jef.com.sun.xml.txw2.annotation.XmlAttribute;
import jef.com.sun.xml.txw2.annotation.XmlElement;

@XmlElement("simpleType")
public interface SimpleType
    extends Annotated, SimpleDerivation, TypedXmlWriter
{


    @XmlAttribute("final")
    public SimpleType _final(String[] value);

    @XmlAttribute("final")
    public SimpleType _final(String value);

    @XmlAttribute
    public SimpleType name(String value);

}
