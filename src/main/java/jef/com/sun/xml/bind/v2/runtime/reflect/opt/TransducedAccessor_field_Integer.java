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

package jef.com.sun.xml.bind.v2.runtime.reflect.opt;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import jef.com.sun.xml.bind.DatatypeConverterImpl;
import jef.com.sun.xml.bind.api.AccessorException;
import jef.com.sun.xml.bind.v2.runtime.Name;
import jef.com.sun.xml.bind.v2.runtime.XMLSerializer;
import jef.com.sun.xml.bind.v2.runtime.reflect.TransducedAccessor;
import jef.com.sun.xml.bind.v2.runtime.reflect.DefaultTransducedAccessor;

import org.xml.sax.SAXException;

/**
 * Template {@link TransducedAccessor} for a byte field.
 *
 * <p>
 * All the TransducedAccessor_field are generated from <code>TransducedAccessor_field_B y t e</code>
 *
 * @author Kohsuke Kawaguchi
 *
 * @see TransducedAccessor#get
 */
public final class TransducedAccessor_field_Integer extends DefaultTransducedAccessor {
    public String print(Object o) {
        return DatatypeConverterImpl._printInt( ((Bean)o).f_int );
    }

    public void parse(Object o, CharSequence lexical) {
        ((Bean)o).f_int=DatatypeConverterImpl._parseInt(lexical);
    }

    public boolean hasValue(Object o) {
        return true;
    }

    @Override
    public void writeLeafElement(XMLSerializer w, Name tagName, Object o, String fieldName) throws SAXException, AccessorException, IOException, XMLStreamException {
        w.leafElement(tagName, ((Bean)o).f_int, fieldName );
    }
}
