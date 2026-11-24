package com.sevtinge.hyperceiler.xposedcompat;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class XposedHelpers {
    private static final ConcurrentHashMap<MemberCacheKey, Optional<Field>> fieldCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MemberCacheKey, Optional<Method>> exactMethodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MemberCacheKey, Optional<Method>> bestMethodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MemberCacheKey, Optional<Constructor<?>>> exactConstructorCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MemberCacheKey, Optional<Constructor<?>>> bestConstructorCache = new ConcurrentHashMap<>();
    private static final WeakHashMap<Object, HashMap<String, Object>> additionalFields = new WeakHashMap<>();

    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new HashMap<>();
    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES = {
            byte.class, short.class, char.class, int.class, long.class, float.class, double.class
    };

    static {
        PRIMITIVES.put("boolean", boolean.class);
        PRIMITIVES.put("byte", byte.class);
        PRIMITIVES.put("char", char.class);
        PRIMITIVES.put("short", short.class);
        PRIMITIVES.put("int", int.class);
        PRIMITIVES.put("long", long.class);
        PRIMITIVES.put("float", float.class);
        PRIMITIVES.put("double", double.class);
        PRIMITIVES.put("void", void.class);

        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.class);

        for (Map.Entry<Class<?>, Class<?>> entry : PRIMITIVE_TO_WRAPPER.entrySet()) {
            WRAPPER_TO_PRIMITIVE.put(entry.getValue(), entry.getKey());
        }
    }

    private XposedHelpers() {
    }

    public static final class ClassNotFoundError extends Error {
        public ClassNotFoundError(Throwable cause) {
            super(cause);
        }

        public ClassNotFoundError(String detailMessage, Throwable cause) {
            super(detailMessage, cause);
        }
    }

    public static final class InvocationTargetError extends Error {
        public InvocationTargetError(Throwable cause) {
            super(cause);
        }

        public InvocationTargetError(String detailMessage, Throwable cause) {
            super(detailMessage, cause);
        }
    }

    private static final class MemberCacheKey {
        private final Class<?> clazz;
        private final String name;
        private final Class<?>[] parameters;
        private final int hashCode;

        MemberCacheKey(Class<?> clazz, String name, Class<?>[] parameters) {
            this.clazz = clazz;
            this.name = name;
            this.parameters = parameters;
            int h = Objects.hash(clazz, name);
            h = 31 * h + Arrays.hashCode(parameters);
            this.hashCode = h;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MemberCacheKey that)) return false;
            return clazz == that.clazz
                    && Objects.equals(name, that.name)
                    && Arrays.equals(parameters, that.parameters);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        if (className == null) {
            throw new ClassNotFoundError(new NullPointerException("className must not be null"));
        }
        String baseName = className.trim();
        int arrayDims = 0;
        while (baseName.endsWith("[]")) {
            arrayDims++;
            baseName = baseName.substring(0, baseName.length() - 2).trim();
        }

        Class<?> resolved = null;
        if (arrayDims > 0) {
            resolved = PRIMITIVES.get(baseName);
        } else {
            resolved = PRIMITIVES.get(baseName);
            if (resolved != null) {
                return resolved;
            }
        }

        ClassNotFoundException lastError = null;
        if (resolved == null) {
            if (classLoader != null) {
                try {
                    resolved = loadClassWithInnerFallback(baseName, classLoader);
                } catch (ClassNotFoundException e) {
                    lastError = e;
                }
            } else {
                ClassLoader[] candidates = new ClassLoader[]{
                        XposedBridge.BOOTCLASSLOADER,
                        safeSystemClassLoader(),
                        XposedHelpers.class.getClassLoader(),
                        null
                };
                for (ClassLoader candidate : candidates) {
                    try {
                        resolved = loadClassWithInnerFallback(baseName, candidate);
                        break;
                    } catch (ClassNotFoundException e) {
                        lastError = e;
                    }
                }
            }
        }

        if (resolved == null) {
            throw new ClassNotFoundError(lastError != null ? lastError : new ClassNotFoundException(className));
        }

        for (int i = 0; i < arrayDims; i++) {
            resolved = Array.newInstance(resolved, 0).getClass();
        }
        return resolved;
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        if (className == null) {
            return null;
        }
        try {
            return findClass(className, classLoader);
        } catch (ClassNotFoundError e) {
            return null;
        }
    }

    private static ClassLoader safeSystemClassLoader() {
        try {
            return ClassLoader.getSystemClassLoader();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Class<?> loadClassWithInnerFallback(String name, ClassLoader cl) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, cl);
        } catch (ClassNotFoundException firstError) {
            String candidate = name;
            while (true) {
                int lastDot = candidate.lastIndexOf('.');
                if (lastDot < 0) {
                    throw firstError;
                }
                candidate = candidate.substring(0, lastDot) + '$' + candidate.substring(lastDot + 1);
                try {
                    return Class.forName(candidate, false, cl);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must not be null");
        }
        Field field = findFieldIfExists(clazz, fieldName);
        if (field == null) {
            throw new NoSuchFieldError(clazz.getName() + '#' + fieldName);
        }
        return field;
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) {
            return null;
        }
        MemberCacheKey key = new MemberCacheKey(clazz, fieldName, null);
        Optional<Field> cached = fieldCache.get(key);
        if (cached != null) {
            return cached.orElse(null);
        }
        Field found = findFieldRecursiveImpl(clazz, fieldName);
        fieldCache.putIfAbsent(key, Optional.ofNullable(found));
        return found;
    }

    private static Field findFieldRecursiveImpl(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        Class<?> current = clazz;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == type) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldError("Field of type " + type.getName() + " in class " + clazz.getName());
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0
                || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook callback)) {
            throw new IllegalArgumentException("no callback defined");
        }
        Method m = findMethodExact(clazz, methodName,
                getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback, parameterTypesAndCallback.length - 1));
        return XposedBridge.hookMethod(m, callback);
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        return findAndHookMethod(findClass(className, classLoader), methodName, parameterTypesAndCallback);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0
                || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook callback)) {
            throw new IllegalArgumentException("no callback defined");
        }
        Constructor<?> c = findConstructorExact(clazz,
                getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback, parameterTypesAndCallback.length - 1));
        return XposedBridge.hookMethod(c, callback);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        return findAndHookConstructor(findClass(className, classLoader), parameterTypesAndCallback);
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        int len = parameterTypes != null ? parameterTypes.length : 0;
        return findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypes, len));
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        Class<?> clazz = findClass(className, classLoader);
        int len = parameterTypes != null ? parameterTypes.length : 0;
        return findMethodExact(clazz, methodName, getParameterClasses(classLoader, parameterTypes, len));
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("methodName must not be null");
        }
        Class<?>[] params = parameterTypes != null ? parameterTypes : new Class<?>[0];
        Method method = findMethodExactImpl(clazz, methodName, params);
        if (method == null) {
            throw new NoSuchMethodError(clazz.getName() + '#' + methodName + getParametersString(params) + "#exact");
        }
        return method;
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
        if (clazz == null || methodName == null) {
            return null;
        }
        try {
            int len = parameterTypes != null ? parameterTypes.length : 0;
            Class<?>[] params = getParameterClasses(clazz.getClassLoader(), parameterTypes, len);
            return findMethodExactImpl(clazz, methodName, params);
        } catch (ClassNotFoundError e) {
            return null;
        }
    }

    public static Method findMethodExactIfExists(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null || methodName == null) {
            return null;
        }
        try {
            int len = parameterTypes != null ? parameterTypes.length : 0;
            Class<?>[] params = getParameterClasses(classLoader, parameterTypes, len);
            return findMethodExactImpl(clazz, methodName, params);
        } catch (ClassNotFoundError e) {
            return null;
        }
    }

    private static Method findMethodExactImpl(Class<?> clazz, String methodName, Class<?>[] params) {
        MemberCacheKey key = new MemberCacheKey(clazz, methodName, params);
        Optional<Method> cached = exactMethodCache.get(key);
        if (cached != null) {
            return cached.orElse(null);
        }
        Method found = null;
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && Arrays.equals(method.getParameterTypes(), params)) {
                    method.setAccessible(true);
                    found = method;
                    break;
                }
            }
            if (found != null) {
                break;
            }
            current = current.getSuperclass();
        }
        exactMethodCache.putIfAbsent(key, Optional.ofNullable(found));
        return found;
    }

    public static Method[] findMethodsByExactParameters(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        Class<?>[] params = parameterTypes != null ? parameterTypes : new Class<?>[0];
        List<Method> result = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getReturnType() != returnType) {
                continue;
            }
            if (Arrays.equals(method.getParameterTypes(), params)) {
                method.setAccessible(true);
                result.add(method);
            }
        }
        return result.toArray(new Method[0]);
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("methodName must not be null");
        }
        Class<?>[] params = parameterTypes != null ? parameterTypes : new Class<?>[0];
        MemberCacheKey key = new MemberCacheKey(clazz, methodName, params);
        Optional<Method> cached = bestMethodCache.get(key);
        if (cached != null) {
            return cached.orElseThrow(() ->
                    new NoSuchMethodError(clazz.getName() + '#' + methodName + getParametersString(params) + "#bestmatch"));
        }

        Method bestMatch = null;
        float bestCost = Float.MAX_VALUE;
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] methodParams = method.getParameterTypes();
                if (methodParams.length != params.length) {
                    continue;
                }
                float cost = computeMatchCost(params, methodParams);
                if (cost < 0f) {
                    continue;
                }
                if (method.isBridge()) {
                    cost += 0.05f;
                }
                if (bestMatch == null || cost < bestCost) {
                    bestMatch = method;
                    bestCost = cost;
                }
            }
            current = current.getSuperclass();
        }

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            bestMethodCache.putIfAbsent(key, Optional.of(bestMatch));
            return bestMatch;
        }
        bestMethodCache.putIfAbsent(key, Optional.empty());
        throw new NoSuchMethodError(clazz.getName() + '#' + methodName + getParametersString(params) + "#bestmatch");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        return findMethodBestMatch(clazz, methodName, getParameterTypes(args));
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) {
        Class<?>[] merged = mergeParameterTypes(parameterTypes, args);
        return findMethodBestMatch(clazz, methodName, merged);
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        int len = parameterTypes != null ? parameterTypes.length : 0;
        return findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypes, len));
    }

    public static Constructor<?> findConstructorExact(String className, ClassLoader classLoader, Object... parameterTypes) {
        Class<?> clazz = findClass(className, classLoader);
        int len = parameterTypes != null ? parameterTypes.length : 0;
        return findConstructorExact(clazz, getParameterClasses(classLoader, parameterTypes, len));
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        Class<?>[] params = parameterTypes != null ? parameterTypes : new Class<?>[0];
        Constructor<?> ctor = findConstructorExactImpl(clazz, params);
        if (ctor == null) {
            throw new NoSuchMethodError(clazz.getName() + getParametersString(params) + "#exact");
        }
        return ctor;
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Object... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            int len = parameterTypes != null ? parameterTypes.length : 0;
            return findConstructorExactImpl(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypes, len));
        } catch (ClassNotFoundError e) {
            return null;
        }
    }

    public static Constructor<?> findConstructorExactIfExists(String className, ClassLoader classLoader, Object... parameterTypes) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            return null;
        }
        try {
            int len = parameterTypes != null ? parameterTypes.length : 0;
            return findConstructorExactImpl(clazz, getParameterClasses(classLoader, parameterTypes, len));
        } catch (ClassNotFoundError e) {
            return null;
        }
    }

    private static Constructor<?> findConstructorExactImpl(Class<?> clazz, Class<?>[] params) {
        MemberCacheKey key = new MemberCacheKey(clazz, "<init>", params);
        Optional<Constructor<?>> cached = exactConstructorCache.get(key);
        if (cached != null) {
            return cached.orElse(null);
        }
        Constructor<?> found = null;
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (Arrays.equals(ctor.getParameterTypes(), params)) {
                ctor.setAccessible(true);
                found = ctor;
                break;
            }
        }
        exactConstructorCache.putIfAbsent(key, Optional.ofNullable(found));
        return found;
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        Class<?>[] params = parameterTypes != null ? parameterTypes : new Class<?>[0];
        MemberCacheKey key = new MemberCacheKey(clazz, "<init>", params);
        Optional<Constructor<?>> cached = bestConstructorCache.get(key);
        if (cached != null) {
            return cached.orElseThrow(() ->
                    new NoSuchMethodError(clazz.getName() + getParametersString(params) + "#bestmatch"));
        }

        Constructor<?> bestMatch = null;
        float bestCost = Float.MAX_VALUE;
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            Class<?>[] ctorParams = ctor.getParameterTypes();
            if (ctorParams.length != params.length) {
                continue;
            }
            float cost = computeMatchCost(params, ctorParams);
            if (cost < 0f) {
                continue;
            }
            if (bestMatch == null || cost < bestCost) {
                bestMatch = ctor;
                bestCost = cost;
            }
        }

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            bestConstructorCache.putIfAbsent(key, Optional.of(bestMatch));
            return bestMatch;
        }
        bestConstructorCache.putIfAbsent(key, Optional.empty());
        throw new NoSuchMethodError(clazz.getName() + getParametersString(params) + "#bestmatch");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        return findConstructorBestMatch(clazz, getParameterTypes(args));
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>[] parameterTypes, Object[] args) {
        return findConstructorBestMatch(clazz, mergeParameterTypes(parameterTypes, args));
    }

    public static Class<?>[] getParameterTypes(Object... args) {
        if (args == null) {
            return new Class<?>[0];
        }
        Class<?>[] clazzes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            clazzes[i] = (args[i] != null) ? args[i].getClass() : null;
        }
        return clazzes;
    }

    private static Class<?>[] mergeParameterTypes(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes == null) {
            return getParameterTypes(args);
        }
        Class<?>[] argTypes = getParameterTypes(args);
        Class<?>[] result = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] != null) {
                result[i] = parameterTypes[i];
            } else if (i < argTypes.length) {
                result[i] = argTypes[i];
            }
        }
        return result;
    }

    private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback, int count) {
        Class<?>[] parameterClasses = new Class<?>[count];
        for (int i = 0; i < count; i++) {
            Object type = parameterTypesAndCallback[i];
            if (type instanceof Class<?>) {
                parameterClasses[i] = (Class<?>) type;
            } else if (type instanceof String) {
                parameterClasses[i] = findClass((String) type, classLoader);
            } else {
                throw new ClassNotFoundError("parameter type must either be specified as Class or String", null);
            }
        }
        return parameterClasses;
    }

    private static float computeMatchCost(Class<?>[] argTypes, Class<?>[] paramTypes) {
        float totalCost = 0f;
        for (int i = 0; i < paramTypes.length; i++) {
            float c = getTypeCost(argTypes[i], paramTypes[i]);
            if (c < 0f) {
                return -1f;
            }
            totalCost += c;
        }
        return totalCost;
    }

    private static float getTypeCost(Class<?> src, Class<?> dst) {
        if (src == dst) {
            return 0f;
        }
        if (src == null) {
            if (dst.isPrimitive()) {
                return -1f;
            }
            if (dst == Object.class) {
                return 0.9f;
            }
            return 0.5f;
        }
        if (dst.isPrimitive()) {
            Class<?> srcPrimitive = src.isPrimitive() ? src : WRAPPER_TO_PRIMITIVE.get(src);
            if (srcPrimitive == null) {
                return -1f;
            }
            if (srcPrimitive == dst) {
                return 0.1f;
            }
            float w = getPrimitiveWideningCost(srcPrimitive, dst);
            if (w < 0f) {
                return -1f;
            }
            return (src.isPrimitive() ? 0f : 0.1f) + w;
        }
        Class<?> effectiveSrc = src.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(src) : src;
        if (effectiveSrc == dst) {
            return 0.1f;
        }
        if (!dst.isAssignableFrom(effectiveSrc)) {
            return -1f;
        }
        float cost = src.isPrimitive() ? 0.2f : 0f;
        if (dst.isInterface()) {
            return cost + 1.5f;
        }
        int depth = 0;
        Class<?> cur = effectiveSrc;
        while (cur != null && cur != dst) {
            depth++;
            cur = cur.getSuperclass();
        }
        return cost + (cur == dst ? depth : 10f);
    }

    private static float getPrimitiveWideningCost(Class<?> src, Class<?> dst) {
        if (src == boolean.class || dst == boolean.class || src == void.class || dst == void.class) {
            return -1f;
        }
        if (src == dst) {
            return 0f;
        }
        int steps = 0;
        Class<?> cur = src;
        while (cur != null && cur != dst) {
            steps++;
            if (cur == byte.class) {
                cur = short.class;
            } else if (cur == short.class || cur == char.class) {
                cur = int.class;
            } else if (cur == int.class) {
                cur = long.class;
            } else if (cur == long.class) {
                cur = float.class;
            } else if (cur == float.class) {
                cur = double.class;
            } else {
                cur = null;
            }
        }
        return cur == dst ? steps * 0.2f : -1f;
    }

    private static String getParametersString(Class<?>... clazzes) {
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Class<?> clazz : clazzes) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(clazz != null ? clazz.getCanonicalName() : "null");
        }
        sb.append(")");
        return sb.toString();
    }

    // Object / Primitive instance field accessors

    public static void setObjectField(Object obj, String fieldName, Object value) {
        try {
            findField(obj.getClass(), fieldName).set(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        try {
            findField(obj.getClass(), fieldName).setBoolean(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setByteField(Object obj, String fieldName, byte value) {
        try {
            findField(obj.getClass(), fieldName).setByte(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setCharField(Object obj, String fieldName, char value) {
        try {
            findField(obj.getClass(), fieldName).setChar(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setDoubleField(Object obj, String fieldName, double value) {
        try {
            findField(obj.getClass(), fieldName).setDouble(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setFloatField(Object obj, String fieldName, float value) {
        try {
            findField(obj.getClass(), fieldName).setFloat(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        try {
            findField(obj.getClass(), fieldName).setInt(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setLongField(Object obj, String fieldName, long value) {
        try {
            findField(obj.getClass(), fieldName).setLong(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setShortField(Object obj, String fieldName, short value) {
        try {
            findField(obj.getClass(), fieldName).setShort(obj, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getObjectField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).get(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getSurroundingThis(Object obj) {
        return getObjectField(obj, "this$0");
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getBoolean(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static byte getByteField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getByte(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static char getCharField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getChar(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static double getDoubleField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getDouble(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static float getFloatField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getFloat(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static int getIntField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getInt(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static long getLongField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getLong(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static short getShortField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getShort(obj);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    // Static field accessors

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        try {
            findField(clazz, fieldName).set(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        try {
            findField(clazz, fieldName).setBoolean(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticByteField(Class<?> clazz, String fieldName, byte value) {
        try {
            findField(clazz, fieldName).setByte(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticCharField(Class<?> clazz, String fieldName, char value) {
        try {
            findField(clazz, fieldName).setChar(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticDoubleField(Class<?> clazz, String fieldName, double value) {
        try {
            findField(clazz, fieldName).setDouble(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticFloatField(Class<?> clazz, String fieldName, float value) {
        try {
            findField(clazz, fieldName).setFloat(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
        try {
            findField(clazz, fieldName).setInt(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticLongField(Class<?> clazz, String fieldName, long value) {
        try {
            findField(clazz, fieldName).setLong(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticShortField(Class<?> clazz, String fieldName, short value) {
        try {
            findField(clazz, fieldName).setShort(null, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).get(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getBoolean(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static byte getStaticByteField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getByte(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static char getStaticCharField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getChar(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static double getStaticDoubleField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getDouble(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static float getStaticFloatField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getFloat(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static int getStaticIntField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getInt(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static long getStaticLongField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getLong(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static short getStaticShortField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getShort(null);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    // Method and constructor invocation

    public static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            return findMethodBestMatch(obj.getClass(), methodName, args).invoke(obj, args);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        }
    }

    public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return findMethodBestMatch(obj.getClass(), methodName, parameterTypes, args).invoke(obj, args);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try {
            return findMethodBestMatch(clazz, methodName, args).invoke(null, args);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return findMethodBestMatch(clazz, methodName, parameterTypes, args).invoke(null, args);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        }
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        try {
            return findConstructorBestMatch(clazz, args).newInstance(args);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        }
    }

    public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... args) {
        try {
            return findConstructorBestMatch(clazz, parameterTypes, args).newInstance(args);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        }
    }

    // Additional instance & static fields

    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        if (obj == null) {
            throw new NullPointerException("object must not be null");
        }
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        synchronized (additionalFields) {
            HashMap<String, Object> objectFields = additionalFields.get(obj);
            if (objectFields == null) {
                objectFields = new HashMap<>();
                additionalFields.put(obj, objectFields);
            }
            return objectFields.put(key, value);
        }
    }

    public static Object getAdditionalInstanceField(Object obj, String key) {
        if (obj == null) {
            throw new NullPointerException("object must not be null");
        }
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        synchronized (additionalFields) {
            HashMap<String, Object> objectFields = additionalFields.get(obj);
            if (objectFields == null) {
                return null;
            }
            return objectFields.get(key);
        }
    }

    public static Object removeAdditionalInstanceField(Object obj, String key) {
        if (obj == null) {
            throw new NullPointerException("object must not be null");
        }
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        synchronized (additionalFields) {
            HashMap<String, Object> objectFields = additionalFields.get(obj);
            if (objectFields == null) {
                return null;
            }
            return objectFields.remove(key);
        }
    }

    public static Object setAdditionalStaticField(Object obj, String key, Object value) {
        return setAdditionalInstanceField(obj.getClass(), key, value);
    }

    public static Object getAdditionalStaticField(Object obj, String key) {
        return getAdditionalInstanceField(obj.getClass(), key);
    }

    public static Object removeAdditionalStaticField(Object obj, String key) {
        return removeAdditionalInstanceField(obj.getClass(), key);
    }

    public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) {
        return setAdditionalInstanceField(clazz, key, value);
    }

    public static Object getAdditionalStaticField(Class<?> clazz, String key) {
        return getAdditionalInstanceField(clazz, key);
    }

    public static Object removeAdditionalStaticField(Class<?> clazz, String key) {
        return removeAdditionalInstanceField(clazz, key);
    }
}
