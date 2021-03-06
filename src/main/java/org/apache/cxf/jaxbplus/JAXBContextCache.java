/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxbplus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.transform.dom.DOMSource;

import jef.tools.reflect.ClassEx;
import jef.tools.reflect.MethodEx;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.CacheMap;
import org.apache.cxf.common.util.CachedClass;
import org.apache.cxf.common.util.StringUtils;

/**
 * 
 */
public final class JAXBContextCache {
    public static final class CachedContextAndSchemas {
        private WeakReference<JAXBContext> context;
        private WeakReference<Set<Type>> classes;
        private Collection<DOMSource> schemas;

        CachedContextAndSchemas(JAXBContext context, Set<Type> classes) {
            this.context = new WeakReference<JAXBContext>(context);
            this.classes = new WeakReference<Set<Type>>(classes);
        }

        public JAXBContext getContext() {
            return context.get();
        }
        public Set<Type> getClasses() {
            return classes.get();
        }
        public void setClasses(Set<Type> cls) {
            classes = new WeakReference<Set<Type>>(cls);
        }
        
        public Collection<DOMSource> getSchemas() {
            return schemas;
        }

        public void setSchemas(Collection<DOMSource> schemas) {
            this.schemas = schemas;
        }
    } 
    
    private static final Map<Set<Type>, CachedContextAndSchemas> JAXBCONTEXT_CACHE
        = new CacheMap<Set<Type>, CachedContextAndSchemas>();

    private static final Map<Package, CachedClass> OBJECT_FACTORY_CACHE
        = new CacheMap<Package, CachedClass>(); 
    
    private JAXBContextCache() {
        //utility class
    }
    
    /**
     * Clear any caches to make sure new contexts are created
     */
    public static void clearCaches() {
        synchronized (JAXBCONTEXT_CACHE) {
            JAXBCONTEXT_CACHE.clear();
        }
        synchronized (OBJECT_FACTORY_CACHE) {
            OBJECT_FACTORY_CACHE.clear();
        }
    }

    public static void scanPackages(Set<Type> classes) {
        JAXBUtils.scanPackages(classes, OBJECT_FACTORY_CACHE);
    }
    
    public static CachedContextAndSchemas getCachedContextAndSchemas(Class<?> cls) throws JAXBException {
        Set<Type> classes = new HashSet<Type>();
        classes.add(cls);
        scanPackages(classes);
        return JAXBContextCache.getCachedContextAndSchemas(classes, null, null, null, false);
    }
    
    public static CachedContextAndSchemas getCachedContextAndSchemas(String pkg,
                                                                     Map<String, Object> props,
                                                                     ClassLoader loader) 
        throws JAXBException {
        Set<Type> classes = new HashSet<Type>();
        addPackage(classes, pkg, loader);
        return getCachedContextAndSchemas(classes, null, props, null, true);
    }
    
    public static CachedContextAndSchemas getCachedContextAndSchemas(Set<Type> classes,
                                                                     String defaultNs,
                                                                     Map<String, Object> props,
                                                                     Collection<Object> typeRefs,
                                                                     boolean exact)
        throws JAXBException {
        for (Type type : classes) {
        	ClassEx clz=new ClassEx(type);
            if (clz.getName().endsWith("ObjectFactory")
                && checkObjectFactoryNamespaces(clz)) {
                // kind of a hack, but ObjectFactories may be created with empty
                // namespaces
                defaultNs = null;
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        if (defaultNs != null) {
            map.put("com.sun.xml.bind.defaultNamespaceRemap", defaultNs);
        }
        if (props != null) {
            map.putAll(props);
        }
        CachedContextAndSchemas cachedContextAndSchemas = null;
        JAXBContext context = null;
        if (typeRefs == null || typeRefs.isEmpty()) {
            synchronized (JAXBCONTEXT_CACHE) {
                if (exact) {
                    cachedContextAndSchemas = JAXBCONTEXT_CACHE.get(classes);
                } else {
                    for (Map.Entry<Set<Type>, CachedContextAndSchemas> k : JAXBCONTEXT_CACHE.entrySet()) {
                        Set<Type> key = k.getKey();
                        if (key != null && key.containsAll(classes)) {
                            cachedContextAndSchemas = k.getValue();
                            break;
                        }
                    }
                }
            }
        }
        if (cachedContextAndSchemas != null) {
            context = cachedContextAndSchemas.getContext();
            if (context ==  null) {
                JAXBCONTEXT_CACHE.remove(cachedContextAndSchemas.getClasses());
                cachedContextAndSchemas = null;
            }
        }
        if (context == null) {
            try {
                context = createContext(classes, map, typeRefs);
            } catch (JAXBException ex) {
                // load jaxb needed class and try to create jaxb context 
                boolean added = addJaxbObjectFactory(ex, classes);
                while (cachedContextAndSchemas == null && added) {
                    try {
                        context = JAXBContext.newInstance(classes
                                                      .toArray(new Class[classes.size()]), null);
                    } catch (JAXBException e) {
                        //second attempt failed as well, rethrow the original exception
                        throw ex;
                    }
                }
                if (context == null) {
                    throw ex;
                }
            }
            cachedContextAndSchemas = new CachedContextAndSchemas(context, classes);
            synchronized (JAXBCONTEXT_CACHE) {
                if (typeRefs == null || typeRefs.isEmpty()) {
                    JAXBCONTEXT_CACHE.put(classes, cachedContextAndSchemas);
                }
            }
        }

        return cachedContextAndSchemas;
    }
    
    private static boolean checkObjectFactoryNamespaces(ClassEx clz) {
        for (MethodEx meth : clz.getMethods()) {
            XmlElementDecl decl = meth.getAnnotation(XmlElementDecl.class);
            if (decl != null
                && XmlElementDecl.GLOBAL.class.equals(decl.scope())
                && StringUtils.isEmpty(decl.namespace())) {
                return true;
            }
        }

        return false;
    }
    
    
    private static JAXBContext createContext(Set<Type> classes,
                                      Map<String, Object> map,
                                      Collection<Object> typeRefs)
        throws JAXBException {
        JAXBContext ctx;
        if (typeRefs != null && !typeRefs.isEmpty()) {
            Class<?> fact = null;
            String pfx = "com.sun.xml.bind.";
            try {
                fact = ClassLoaderUtils.loadClass("com.sun.xml.bind.v2.ContextFactory",
                                                  JAXBContextCache.class);
            } catch (Throwable t) {
                try {
                    fact = ClassLoaderUtils.loadClass("com.sun.xml.internal.bind.v2.ContextFactory",
                                                      JAXBContextCache.class);
                    pfx = "com.sun.xml.internal.bind.";
                } catch (Throwable t2) {
                    //ignore
                }
            }
            if (fact != null) {
                for (Method m : fact.getMethods()) {
                    if ("createContext".equals(m.getName())
                        && m.getParameterTypes().length == 9) {
                        try {
                            return (JAXBContext)m.invoke(null,
                                     classes.toArray(new Class[classes.size()]),
                                     typeRefs,
                                     map.get(pfx + "subclassReplacements"),
                                     map.get(pfx + "defaultNamespaceRemap"),
                                     map.get(pfx + "c14n") == null
                                         ? Boolean.FALSE
                                             : map.get(pfx + "c14n"),
                                     map.get(pfx + "v2.model.annotation.RuntimeAnnotationReader"),
                                     map.get(pfx + "XmlAccessorFactory") == null
                                         ? Boolean.FALSE
                                             : map.get(pfx + "XmlAccessorFactory"),
                                     map.get(pfx + "treatEverythingNillable") == null
                                         ? Boolean.FALSE : map.get(pfx + "treatEverythingNillable"),
                                     map.get("retainReferenceToInfo") == null
                                         ? Boolean.FALSE : map.get("retainReferenceToInfo"));
                        } catch (Throwable e) {
                            //ignore
                        }
                    }
                }
            }
        }
        try {
            ctx = jef.com.sun.xml.bind.v2.ContextFactory.createContext(classes.toArray(new Type[classes.size()]), map);
        } catch (JAXBException ex) {
            if (map.containsKey("com.sun.xml.bind.defaultNamespaceRemap")
                && ex.getMessage() != null
                && ex.getMessage().contains("com.sun.xml.bind.defaultNamespaceRemap")) {
                map.put("com.sun.xml.internal.bind.defaultNamespaceRemap",
                        map.remove("com.sun.xml.bind.defaultNamespaceRemap"));
                ctx = JAXBContext.newInstance(classes.toArray(new Class[classes.size()]), map);
            } else {
                throw ex;
            }
        }
        return ctx;
    }
    // Now we can not add all the classes that Jaxb needed into JaxbContext,
    // especially when
    // an ObjectFactory is pointed to by an jaxb @XmlElementDecl annotation
    // added this workaround method to load the jaxb needed ObjectFactory class
    private static boolean addJaxbObjectFactory(JAXBException e1, Set<Type> classes) {
        boolean added = false;
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        java.io.PrintStream pout = new java.io.PrintStream(bout);
        e1.printStackTrace(pout);
        String str = new String(bout.toByteArray());
        Pattern pattern = Pattern.compile("(?<=There's\\sno\\sObjectFactory\\swith\\san\\s"
                                          + "@XmlElementDecl\\sfor\\sthe\\selement\\s\\{)\\S*(?=\\})");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String pkgName = JAXBUtils.namespaceURIToPackage(matcher.group());
            try {
                Class clz = JAXBContextCache.class.getClassLoader()
                    .loadClass(pkgName + "." + "ObjectFactory");

                if (!classes.contains(clz)) {
                    classes.add(clz);
                    added = true;
                }
            } catch (ClassNotFoundException e) {
                // do nothing
            }

        }
        return added;
    }

    public static void addPackage(Set<Type>  classes, String pkg, ClassLoader loader) {
        try {
            classes.add(Class.forName(pkg + ".ObjectFactory", false, loader));
        } catch (Exception ex) {
            //ignore
        }
        try {
            InputStream ins = loader.getResourceAsStream("/" + pkg.replace('.', '/') + "/jaxb.index");
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
            if (!StringUtils.isEmpty(pkg)) {
                pkg += ".";
            }
    
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.indexOf("#") != -1) {
                    line = line.substring(0, line.indexOf("#"));
                }
                if (!StringUtils.isEmpty(line)) {
                    try {
                        Class<?> ncls = Class.forName(pkg + line, false, loader);
                        classes.add(ncls);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                line = reader.readLine();
            }
        } catch (Exception ex) {
            //ignore
        }
    }

}
