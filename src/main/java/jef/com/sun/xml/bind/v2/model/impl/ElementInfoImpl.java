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

package jef.com.sun.xml.bind.v2.model.impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.activation.MimeType;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlInlineBinaryData;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import jef.com.sun.istack.FinalArrayList;
import jef.com.sun.xml.bind.v2.TODO;
import jef.com.sun.xml.bind.v2.model.annotation.AnnotationSource;
import jef.com.sun.xml.bind.v2.model.annotation.Locatable;
import jef.com.sun.xml.bind.v2.model.core.Adapter;
import jef.com.sun.xml.bind.v2.model.core.ClassInfo;
import jef.com.sun.xml.bind.v2.model.core.ElementInfo;
import jef.com.sun.xml.bind.v2.model.core.ElementPropertyInfo;
import jef.com.sun.xml.bind.v2.model.core.ID;
import jef.com.sun.xml.bind.v2.model.core.NonElement;
import jef.com.sun.xml.bind.v2.model.core.PropertyInfo;
import jef.com.sun.xml.bind.v2.model.core.PropertyKind;
import jef.com.sun.xml.bind.v2.model.core.TypeInfo;
import jef.com.sun.xml.bind.v2.model.core.TypeRef;
import jef.com.sun.xml.bind.v2.runtime.IllegalAnnotationException;
import jef.com.sun.xml.bind.v2.runtime.Location;
import jef.com.sun.xml.bind.v2.runtime.SwaRefAdapter;

/**
 * {@link ElementInfo} implementation.
 *
 * @author Kohsuke Kawaguchi
 */
class ElementInfoImpl<T,C,F,M> extends TypeInfoImpl<T,C,F,M> implements ElementInfo<T,C> {

    private final QName tagName;

    private final NonElement<T,C> contentType;

    private final T tOfJAXBElementT;

    private final T elementType;

    private final ClassInfo<T,C> scope;

    /**
     * Annotation that controls the binding.
     */
    private final XmlElementDecl anno;

    /**
     * If this element can substitute another element, the element name.
     * @see #link()
     */
    private ElementInfoImpl<T,C,F,M> substitutionHead;

    /**
     * Lazily constructed list of {@link ElementInfo}s that can substitute this element.
     * This could be null.
     * @see #link()
     */
    private FinalArrayList<ElementInfoImpl<T,C,F,M>> substitutionMembers;

    /**
     * The factory method from which this mapping was created.
     */
    private final M method;

    /**
     * If the content type is adapter, return that adapter.
     */
    private final Adapter<T,C> adapter;

    private final boolean isCollection;

    private final ID id;

    private final PropertyImpl property;
    private final MimeType expectedMimeType;
    private final boolean inlineBinary;
    private final QName schemaType;

    /**
     * Singleton instance of {@link ElementPropertyInfo} for this element.
     */
    protected class PropertyImpl implements
            ElementPropertyInfo<T,C>,
            TypeRef<T,C>,
            AnnotationSource {
        //
        // TypeRef impl
        //
        public NonElement<T,C> getTarget() {
            return contentType;
        }
        public QName getTagName() {
            return tagName;
        }

        public List<? extends TypeRef<T,C>> getTypes() {
            return Collections.singletonList(this);
        }

        public List<? extends NonElement<T,C>> ref() {
            return Collections.singletonList(contentType);
        }

        public QName getXmlName() {
            return tagName;
        }

        public boolean isCollectionRequired() {
            return false;
        }

        public boolean isCollectionNillable() {
            return true;
        }

        public boolean isNillable() {
            return true;
        }

        public String getDefaultValue() {
            String v = anno.defaultValue();
            if(v.equals("\u0000"))
                return null;
            else
                return v;
        }

        public ElementInfoImpl<T,C,F,M> parent() {
            return ElementInfoImpl.this;
        }

        public String getName() {
            return "value";
        }

        public String displayName() {
            return "JAXBElement#value";
        }

        public boolean isCollection() {
            return isCollection;
        }

        /**
         * For {@link ElementInfo}s, a collection always means a list of values.
         */
        public boolean isValueList() {
            return isCollection;
        }

        public boolean isRequired() {
            return true;
        }

        public PropertyKind kind() {
            return PropertyKind.ELEMENT;
        }

        public Adapter<T,C> getAdapter() {
            return adapter;
        }

        public ID id() {
            return id;
        }

        public MimeType getExpectedMimeType() {
            return expectedMimeType;
        }

        public QName getSchemaType() {
            return schemaType;
        }

        public boolean inlineBinaryData() {
            return inlineBinary;
        }

        public PropertyInfo<T,C> getSource() {
            return this;
        }

        //
        //
        // AnnotationSource impl
        //
        //
        public <A extends Annotation> A readAnnotation(Class<A> annotationType) {
            return reader().getMethodAnnotation(annotationType,method,ElementInfoImpl.this);
        }

        public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
            return reader().hasMethodAnnotation(annotationType,method);
        }
    }

    /**
     * @param m
     *      The factory method on ObjectFactory that comes with {@link XmlElementDecl}.
     */
    public ElementInfoImpl(ModelBuilder<T,C,F,M> builder,
                           RegistryInfoImpl<T,C,F,M> registry, M m ) throws IllegalAnnotationException {
        super(builder,registry);

        this.method = m;
        anno = reader().getMethodAnnotation( XmlElementDecl.class, m, this );
        assert anno!=null;  // the caller should check this
        assert anno instanceof Locatable;

        elementType = nav().getReturnType(m);
        T baseClass = nav().getBaseClass(elementType,nav().asDecl(JAXBElement.class));
        if(baseClass==null)
            throw new IllegalAnnotationException(
                Messages.XML_ELEMENT_MAPPING_ON_NON_IXMLELEMENT_METHOD.format(nav().getMethodName(m)),
                anno );

        tagName = parseElementName(anno);
        T[] methodParams = nav().getMethodParameters(m);

        // adapter
        Adapter<T,C> a = null;
        if(methodParams.length>0) {
            XmlJavaTypeAdapter adapter = reader().getMethodAnnotation(XmlJavaTypeAdapter.class,m,this);
            if(adapter!=null)
                a = new Adapter<T,C>(adapter,reader(),nav());
            else {
                XmlAttachmentRef xsa = reader().getMethodAnnotation(XmlAttachmentRef.class,m,this);
                if(xsa!=null) {
                    TODO.prototype("in APT swaRefAdapter isn't avaialble, so this returns null");
                    a = new Adapter<T,C>(owner.nav.asDecl(SwaRefAdapter.class),owner.nav);
                }
            }
        }
        this.adapter = a;

        // T of JAXBElement<T>
        tOfJAXBElementT =
            methodParams.length>0 ? methodParams[0] // this is more reliable, as it works even for ObjectFactory that sometimes have to return public types
            : nav().getTypeArgument(baseClass,0); // fall back to infer from the return type if no parameter.
        
        if(adapter==null) {
            T list = nav().getBaseClass(tOfJAXBElementT,nav().asDecl(List.class));
            if(list==null) {
                isCollection = false;
                contentType = builder.getTypeInfo(tOfJAXBElementT,this);  // suck this type into the current set.
            } else {
                isCollection = true;
                contentType = builder.getTypeInfo(nav().getTypeArgument(list,0),this);
            }
        } else {
            // but if adapted, use the adapted type
            contentType = builder.getTypeInfo(this.adapter.defaultType,this);
            isCollection = false;
        }

        // scope
        T s = reader().getClassValue(anno,"scope");
        if(s.equals(nav().ref(XmlElementDecl.GLOBAL.class)))
            scope = null;
        else {
            // TODO: what happens if there's an error?
            NonElement<T,C> scp = builder.getClassInfo(nav().asDecl(s),this);
            if(!(scp instanceof ClassInfo)) {
                throw new IllegalAnnotationException(
                    Messages.SCOPE_IS_NOT_COMPLEXTYPE.format(nav().getTypeName(s)),
                    anno );
            }
            scope = (ClassInfo<T,C>)scp;
        }

        id = calcId();

        property = createPropertyImpl();

        this.expectedMimeType = Util.calcExpectedMediaType(property,builder);
        this.inlineBinary = reader().hasMethodAnnotation(XmlInlineBinaryData.class,method);
        this.schemaType = Util.calcSchemaType(reader(),property,registry.registryClass,
                getContentInMemoryType(),this);
    }

    final QName parseElementName(XmlElementDecl e) {
        String local = e.name();
        String nsUri = e.namespace();
        if(nsUri.equals("##default")) {
            // if defaulted ...
            XmlSchema xs = reader().getPackageAnnotation(XmlSchema.class,
                nav().getDeclaringClassForMethod(method),this);
            if(xs!=null)
                nsUri = xs.namespace();
            else {
                nsUri = builder.defaultNsUri;
            }
        }

        return new QName(nsUri.intern(),local.intern());
    }

    protected PropertyImpl createPropertyImpl() {
        return new PropertyImpl();
    }

    public ElementPropertyInfo<T,C> getProperty() {
        return property;
    }

    public NonElement<T,C> getContentType() {
        return contentType;
    }

    public T getContentInMemoryType() {
        if(adapter==null) {
            return tOfJAXBElementT;
        } else {
            return adapter.customType;
        }
    }

    public QName getElementName() {
        return tagName;
    }

    public T getType() {
        return elementType;
    }

    /**
     * Leaf-type cannot be referenced from IDREF.
     *
     * @deprecated
     *      why are you calling a method whose return value is always known?
     */
    public final boolean canBeReferencedByIDREF() {
        return false;
    }

    private ID calcId() {
        // TODO: share code with PropertyInfoImpl
        if(reader().hasMethodAnnotation(XmlID.class,method)) {
            return ID.ID;
        } else
        if(reader().hasMethodAnnotation(XmlIDREF.class,method)) {
            return ID.IDREF;
        } else {
            return ID.NONE;
        }
    }

    public ClassInfo<T, C> getScope() {
        return scope;
    }

    public ElementInfo<T,C> getSubstitutionHead() {
        return substitutionHead;
    }

    public Collection<? extends ElementInfoImpl<T,C,F,M>> getSubstitutionMembers() {
        if(substitutionMembers==null)
            return Collections.emptyList();
        else
            return substitutionMembers;
    }

    /**
     * Called after all the {@link TypeInfo}s are collected into the {@link #owner}.
     */
    /*package*/ void link() {
        // substitution head
        if(anno.substitutionHeadName().length()!=0) {
            QName name = new QName(
                anno.substitutionHeadNamespace(), anno.substitutionHeadName() );
            substitutionHead = owner.getElementInfo(null,name);
            if(substitutionHead==null) {
                builder.reportError(
                    new IllegalAnnotationException(Messages.NON_EXISTENT_ELEMENT_MAPPING.format(
                        name.getNamespaceURI(),name.getLocalPart()), anno));
                // recover by ignoring this substitution declaration
            } else
                substitutionHead.addSubstitutionMember(this);
        } else
            substitutionHead = null;
        super.link();
    }

    private void addSubstitutionMember(ElementInfoImpl<T,C,F,M> child) {
        if(substitutionMembers==null)
            substitutionMembers = new FinalArrayList<ElementInfoImpl<T,C,F,M>>();
        substitutionMembers.add(child);
    }

    public Location getLocation() {
        return nav().getMethodLocation(method);
    }
}
