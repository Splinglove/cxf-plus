/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package jef.com.sun.xml.bind.v2.runtime.property;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import jef.com.sun.xml.bind.api.AccessorException;
import jef.com.sun.xml.bind.v2.model.core.PropertyKind;
import jef.com.sun.xml.bind.v2.model.runtime.RuntimeElementPropertyInfo;
import jef.com.sun.xml.bind.v2.model.runtime.RuntimeTypeRef;
import jef.com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import jef.com.sun.xml.bind.v2.runtime.Name;
import jef.com.sun.xml.bind.v2.runtime.XMLSerializer;
import jef.com.sun.xml.bind.v2.runtime.reflect.Accessor;
import jef.com.sun.xml.bind.v2.runtime.reflect.TransducedAccessor;
import jef.com.sun.xml.bind.v2.runtime.unmarshaller.ChildLoader;
import jef.com.sun.xml.bind.v2.runtime.unmarshaller.DefaultValueLoaderDecorator;
import jef.com.sun.xml.bind.v2.runtime.unmarshaller.LeafPropertyLoader;
import jef.com.sun.xml.bind.v2.runtime.unmarshaller.LeafPropertyXsiLoader;
import jef.com.sun.xml.bind.v2.runtime.unmarshaller.Loader;
import jef.com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import jef.com.sun.xml.bind.v2.util.QNameMap;

import org.xml.sax.SAXException;

/**
 * {@link Property} that contains a leaf value.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
final class SingleElementLeafProperty<BeanT> extends PropertyImpl<BeanT> {

    private final Name tagName;
    private final boolean nillable;
    private final Accessor acc;
    private final String defaultValue;
    private final TransducedAccessor<BeanT> xacc;

    public SingleElementLeafProperty(JAXBContextImpl context, RuntimeElementPropertyInfo prop) {
        super(context, prop);
        RuntimeTypeRef ref = prop.getTypes().get(0);
        tagName = context.nameBuilder.createElementName(ref.getTagName());
        assert tagName != null;
        nillable = ref.isNillable();
        defaultValue = ref.getDefaultValue();
        this.acc = prop.getAccessor().optimize(context);

        xacc = TransducedAccessor.get(context, ref);
        assert xacc != null;
    }

    public void reset(BeanT o) throws AccessorException {
        acc.set(o, null);
    }

    public String getIdValue(BeanT bean) throws AccessorException, SAXException {
        return xacc.print(bean).toString();
    }

    @Override
    public void serializeBody(BeanT o, XMLSerializer w, Object outerPeer) throws SAXException, AccessorException, IOException, XMLStreamException {
        boolean hasValue = xacc.hasValue(o);

        Object obj = null;

        try {
            obj = acc.getUnadapted(o);
        } catch (AccessorException ae) {
            ; // noop
        }

        Class valueType = acc.getValueType();

        // check for different type than expected. If found, add xsi:type declaration
        if (!valueType.isPrimitive() && acc.isValueTypeAbstractable() &&
                ((obj != null) && !obj.getClass().equals(valueType))) {

            w.startElement(tagName, outerPeer);
            w.childAsXsiType(obj, fieldName, w.grammar.getBeanInfo(valueType), nillable);
            w.endElement();

        } else {  // current type is expected

            if (hasValue) {
                xacc.writeLeafElement(w, tagName, o, fieldName);
            } else if (nillable) {
                w.startElement(tagName, null);
                w.writeXsiNilTrue();
                w.endElement();
            }
        }
    }

    public void buildChildElementUnmarshallers(UnmarshallerChain chain, QNameMap<ChildLoader> handlers) {
        Loader l = new LeafPropertyLoader(xacc);
        if (defaultValue != null)
            l = new DefaultValueLoaderDecorator(l, defaultValue);
        if (nillable || chain.context.allNillable)
            l = new XsiNilLoader.Single(l, acc);

        // LeafPropertyXsiLoader doesn't work well with nillable elements
        if (!nillable)
            l = new LeafPropertyXsiLoader(l, xacc, acc);

        handlers.put(tagName, new ChildLoader(l, null));
    }


    public PropertyKind getKind() {
        return PropertyKind.ELEMENT;
    }

    @Override
    public Accessor getElementPropertyAccessor(String nsUri, String localName) {
        if (tagName.equals(nsUri, localName))
            return acc;
        else
            return null;
    }
}
