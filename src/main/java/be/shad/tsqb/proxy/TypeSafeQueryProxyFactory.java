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
            switch( m.getName() ) {
                case "finalize":
                case "hashCode":
                case "equals":
                    return false;
                default: 
                    return true;
            }
        }
    };
    private final Map<Class<?>, Class<?>> proxyClasses = new HashMap<>();

    public <T> T getProxyInstance(Class<T> fromClass) {
        try {
            return (T) getProxyClass(fromClass).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> Class<T> getProxyClass(Class<T> fromClass) {
        synchronized ( TypeSafeQueryProxyFactory.class) { 
            Class<?> proxyClass = proxyClasses.get(fromClass);
            if( proxyClass == null ) {
                ProxyFactory f = new ProxyFactory();
                f.setSuperclass(fromClass); // what if the super class is final?? guess it will give an exception..
                f.setInterfaces(new Class[] { TypeSafeQueryProxy.class });
                f.setFilter(METHOD_FILTER);
                proxyClass = f.createClass();
                proxyClasses.put(fromClass, proxyClass);
            }
            return (Class<T>) proxyClass;
        }
    }
    
}
