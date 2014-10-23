/*
 * Copyright Gert Wijns gert.wijns@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.shad.tsqb.proxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;

/**
 * Provides the proxies using javaassist.
 * <p>
 * the proxied classes are cached for faster proxy creation and
 * to prevent extra class creations everytime a proxy is requested.
 */
public final class TypeSafeQueryProxyFactory {

    private static final MethodFilter METHOD_FILTER = new MethodFilter() {
        public boolean isHandled(Method m) {
            if (m.getName().equals("finalize") || m.getName().equals("hashCode") || m.getName().equals("equals")) {
                return false;
            } else {
                return true;
            }
        }
    };
    
    private final Map<Class<?>, Class<?>>[] proxyClasses;
    
    @SuppressWarnings("unchecked")
    public TypeSafeQueryProxyFactory() {
        proxyClasses = new HashMap[TypeSafeQueryProxyType.values().length];
        for (int i = 0, n = TypeSafeQueryProxyType.values().length; i < n; i++) {
            proxyClasses[i] = new HashMap<Class<?>, Class<?>>();
        }
    }

    public <T> T getProxy(Class<T> fromClass, TypeSafeQueryProxyType type) {
        try {
            return (T) getProxyClass(fromClass, type).newInstance();
        } catch (InstantiationException  e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e1 ) {
            throw new RuntimeException(e1);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> Class<T> getProxyClass(Class<T> fromClass, TypeSafeQueryProxyType type) {
        synchronized ( proxyClasses ) { 
            Class<?> proxyClass = proxyClasses[type.ordinal()].get(fromClass);
            if( proxyClass == null ) {
                ProxyFactory f = new ProxyFactory();
                f.setSuperclass(fromClass); // what if the super class is final?? guess it will give an exception..
                if( type.isEntity() || type.isComposite() ) {
                    f.setInterfaces(new Class[] { TypeSafeQueryProxy.class });
                } else {
                    f.setInterfaces(new Class[] { TypeSafeQuerySelectionProxy.class });
                }
                f.setFilter(METHOD_FILTER);
                proxyClass = f.createClass();
                proxyClasses[type.ordinal()].put(fromClass, proxyClass);
            }
            return (Class<T>) proxyClass;
        }
    }
    
}
