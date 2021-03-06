/*
 * Copyright 2016, Stuart Douglas, and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.fakereplace.runtime;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.fakereplace.core.Constants;
import org.fakereplace.core.Fakereplace;

public class VirtualDelegator {

    /**
     * stores information about which methods need to be delegated to. This data
     * is not needed to actually call the new method, as we can just look up the
     * method no from the MethodIdentifierStore
     */
    private static final Set<VirtualDelegatorData> delegatingMethods = new CopyOnWriteArraySet<>();

    public static void add(ClassLoader loader, String className, String methodName, String methodDesc) {
        delegatingMethods.add(new VirtualDelegatorData(loader, className, methodName, methodDesc));
    }

    public static void clear(ClassLoader classLoader, String className) {
        delegatingMethods.removeIf(i -> i.getLoader() == classLoader && className.equals(i.getClassName()));
    }

    public static boolean contains(Object val, String callingClassName, String methodName, String methodDesc) {
        if (!Fakereplace.isClassReplaceable(val.getClass().getName(), val.getClass().getClassLoader())) {
            return false;
        }
        Class<?> c = val.getClass();
        while (true) {
            if (c.getName().equals(callingClassName)) {
                return false;
            }
            VirtualDelegatorData i = new VirtualDelegatorData(c.getClassLoader(), c.getName(), methodName, methodDesc);
            if (delegatingMethods.contains(i)) {
                return true;
            }
            c = c.getSuperclass();
        }
    }

    public static Object run(Object val, String methodName, String methodDesc, Object[] params) {
        try {
            Method meth = val.getClass().getMethod(Constants.ADDED_METHOD_NAME, int.class, Object[].class);
            int methodIdentifier = MethodIdentifierStore.instance().getMethodNumber(methodName, methodDesc);
            return meth.invoke(val, methodIdentifier, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class VirtualDelegatorData {
        private final ClassLoader loader;
        private final String className;
        private final String methodName;
        private final String methodDesc;

        public ClassLoader getLoader() {
            return loader;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getMethodDesc() {
            return methodDesc;
        }

        public VirtualDelegatorData(ClassLoader loader, String className, String methodName, String methodDesc) {
            super();
            this.loader = loader;
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((methodDesc == null) ? 0 : methodDesc.hashCode());
            result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            VirtualDelegatorData other = (VirtualDelegatorData) obj;
            if (className == null) {
                if (other.className != null)
                    return false;
            } else if (!className.equals(other.className))
                return false;
            if (loader == null) {
                if (other.loader != null)
                    return false;
            } else if (!loader.equals(other.loader))
                return false;
            if (methodDesc == null) {
                if (other.methodDesc != null)
                    return false;
            } else if (!methodDesc.equals(other.methodDesc))
                return false;
            if (methodName == null) {
                if (other.methodName != null)
                    return false;
            } else if (!methodName.equals(other.methodName))
                return false;
            return true;
        }

    }
}
