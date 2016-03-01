/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Parameter;

public final class Reflective
{
    public final static Object NO_MATCH = new Object();
    public final static String MAIN = "_main";
    public final static Set<String> KEYWORDS = new HashSet<String>(
        Arrays.asList(new String[] { "abstract", "continue", "for", "new", "switch",
                "assert", "default", "goto", "package", "synchronized", "boolean", "do",
                "if", "private", "this", "break", "double", "implements", "protected",
                "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short",
                "try", "char", "final", "interface", "static", "void", "class",
                "finally", "long", "strictfp", "volatile", "const", "float", "native",
                "super", "while" }));

    /**
     * invokes the named method on the given target using the supplied args,
     * which are converted if necessary.
     * @param session
     * @param target
     * @param name
     * @param args
     * @return the result of the invoked method
     * @throws Exception
     */
    public static Object invoke(CommandSession session, Object target, String name,
        List<Object> args) throws Exception
    {
        Method[] methods = target.getClass().getMethods();
        name = name.toLowerCase();

        String get = "get" + name;
        String is = "is" + name;
        String set = "set" + name;

        if (KEYWORDS.contains(name))
        {
            name = "_" + name;
        }

        if (target instanceof Class<?>)
        {
            Method[] staticMethods = ((Class<?>) target).getMethods();
            for (Method m : staticMethods)
            {
                String mname = m.getName().toLowerCase();
                if (mname.equals(name) || mname.equals(get) || mname.equals(set)
                    || mname.equals(is) || mname.equals(MAIN))
                {
                    methods = staticMethods;
                    break;
                }
            }
        }

        Method bestMethod = null;
        Object[] bestArgs = null;
        int lowestMatch = Integer.MAX_VALUE;
        ArrayList<Class<?>[]> possibleTypes = new ArrayList<Class<?>[]>();

        for (Method m : methods)
        {
            String mname = m.getName().toLowerCase();
            if (mname.equals(name) || mname.equals(get) || mname.equals(set)
                || mname.equals(is) || mname.equals(MAIN))
            {
                Class<?>[] types = m.getParameterTypes();
                ArrayList<Object> xargs = new ArrayList<Object>(args);

                // pass command name as argv[0] to main, so it can handle
                // multiple commands
                if (mname.equals(MAIN))
                {
                    xargs.add(0, name);
                }

                Object[] parms = new Object[types.length];
                int match = coerce(session, target, m, types, parms, xargs);

                if (match < 0)
                {
                    // coerce failed
                    possibleTypes.add(types);
                }
                else
                {
                    if (match < lowestMatch)
                    {
                        lowestMatch = match;
                        bestMethod = m;
                        bestArgs = parms;
                    }

                    if (match == 0)
                        break; // can't get better score
                }
            }
        }

        if (bestMethod != null)
        {
            bestMethod.setAccessible(true);
            try
            {
                return bestMethod.invoke(target, bestArgs);
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof Exception)
                {
                    throw (Exception) cause;
                }
                throw e;
            }
        }
        else
        {
            ArrayList<String> list = new ArrayList<String>();
            for (Class<?>[] types : possibleTypes)
            {
                StringBuilder buf = new StringBuilder();
                buf.append('(');
                for (Class<?> type : types)
                {
                    if (buf.length() > 1)
                    {
                        buf.append(", ");
                    }
                    buf.append(type.getSimpleName());
                }
                buf.append(')');
                list.add(buf.toString());
            }

            StringBuilder params = new StringBuilder();
            for (Object arg : args)
            {
                if (params.length() > 1)
                {
                    params.append(", ");
                }
                params.append(arg == null ? "null" : arg.getClass().getSimpleName());
            }

            throw new IllegalArgumentException(String.format(
                "Cannot coerce %s(%s) to any of %s", name, params, list));
        }
    }

    /**
     * transform name/value parameters into ordered argument list.
     * params: --param2, value2, --flag1, arg3
     * args: true, value2, arg3
     * @param method
     * @param params
     * @return new ordered list of args.
     */
    private static List<Object> transformParameters(Method method, List<Object> in)
    {
        Annotation[][] pas = method.getParameterAnnotations();
        ArrayList<Object> out = new ArrayList<Object>();
        ArrayList<Object> parms = new ArrayList<Object>(in);

        for (Annotation as[] : pas)
        {
            for (Annotation a : as)
            {
                if (a instanceof Parameter)
                {
                    int i = -1;
                    Parameter p = (Parameter) a;
                    for (String name : p.names())
                    {
                        i = parms.indexOf(name);
                        if (i >= 0)
                            break;
                    }

                    if (i >= 0)
                    {
                        // parameter present
                        parms.remove(i);
                        Object value = p.presentValue();
                        if (Parameter.UNSPECIFIED.equals(value))
                        {
                            if (i >= parms.size())
                                return null; // missing parameter, so try other methods
                            value = parms.remove(i);
                        }
                        out.add(value);
                    }
                    else
                    {
                        out.add(p.absentValue());
                    }

                }
            }
        }

        out.addAll(parms);

        return out;
    }

    /**
     * Complex routein to convert the arguments given from the command line to
     * the arguments of the method call. First, an attempt is made to convert
     * each argument. If this fails, a check is made to see if varargs can be
     * applied. This happens when the last method argument is an array.
     *
     * @param session
     * @param target
     * @param m
     * @param types
     * @param out
     * @param in
     * @return -1 if arguments can't be coerced; 0 if no coercion was necessary; > 0 if coercion was needed.
     */
    private static int coerce(CommandSession session, Object target, Method m,
        Class<?> types[], Object out[], List<Object> in)
    {
        in = transformParameters(m, in);
        if (in == null)
        {
            // missing parameter argument?
            return -1;
        }

        int[] convert = { 0 };

        // Check if the command takes a session
        if ((types.length > 0) && types[0].isInterface()
            && types[0].isAssignableFrom(session.getClass()))
        {
            in.add(0, session);
        }

        int i = 0;
        while (i < out.length)
        {
            out[i] = null;

            // Try to convert one argument
            if (in.size() == 0)
            {
                out[i] = NO_MATCH;
            }
            else
            {
                out[i] = coerce(session, types[i], in.get(0), convert);

                if (out[i] == null && types[i].isArray() && in.size() > 0)
                {
                    // don't coerce null to array FELIX-2432
                    out[i] = NO_MATCH;
                }

                if (out[i] != NO_MATCH)
                {
                    in.remove(0);
                }
            }

            if (out[i] == NO_MATCH)
            {
                // No match, check for varargs
                if (types[i].isArray() && (i == types.length - 1))
                {
                    // Try to parse the remaining arguments in an array
                    Class<?> ctype = types[i].getComponentType();
                    int asize = in.size();
                    Object array = Array.newInstance(ctype, asize);
                    int n = i;
                    while (in.size() > 0)
                    {
                        Object t = coerce(session, ctype, in.remove(0), convert);
                        if (t == NO_MATCH)
                        {
                            return -1;
                        }
                        Array.set(array, i - n, t);
                        i++;
                    }
                    out[n] = array;

                    /*
                     * 1. prefer f() to f(T[]) with empty array
                     * 2. prefer f(T) to f(T[1])
                     * 3. prefer f(T) to f(Object[1]) even if there is a conversion cost for T
                     * 
                     * 1 & 2 require to add 1 to conversion cost, but 3 also needs to match
                     * the conversion cost for T.
                     */
                    return convert[0] + 1 + (asize * 2);
                }
                return -1;
            }
            i++;
        }

        if (in.isEmpty())
            return convert[0];
        return -1;
    }

    /**
     * converts given argument to specified type and increments convert[0] if any conversion was needed.
     * @param session
     * @param type
     * @param arg
     * @param convert convert[0] is incremented according to the conversion needed,
     * to allow the "best" conversion to be determined.
     * @return converted arg or NO_MATCH if no conversion possible.
     */
    private static Object coerce(CommandSession session, Class<?> type, Object arg,
        int[] convert)
    {
        if (arg == null)
        {
            return null;
        }

        if (type.isAssignableFrom(arg.getClass()))
        {
            return arg;
        }

        if (type.isArray())
        {
            return NO_MATCH;
        }

        if (type.isPrimitive() && arg instanceof Long)
        {
            // no-cost conversions between integer types
            Number num = (Number) arg;

            if (type == short.class)
            {
                return num.shortValue();
            }
            if (type == int.class)
            {
                return num.intValue();
            }
            if (type == long.class)
            {
                return num.longValue();
            }
        }

        // all following conversions cost 2 points
        convert[0] += 2;

        Object converted = session.convert(type, arg);
        if (converted != null)
        {
            return converted;
        }

        String string = arg.toString();

        if (type.isAssignableFrom(String.class))
        {
            return string;
        }

        if (type.isPrimitive())
        {
            type = primitiveToObject(type);
        }

        try
        {
            return type.getConstructor(String.class).newInstance(string);
        }
        catch (Exception e)
        {
        }

        if (type == Character.class && string.length() == 1)
        {
            return string.charAt(0);
        }

        return NO_MATCH;
    }

    private static Class<?> primitiveToObject(Class<?> type)
    {
        if (type == boolean.class)
        {
            return Boolean.class;
        }
        if (type == byte.class)
        {
            return Byte.class;
        }
        if (type == char.class)
        {
            return Character.class;
        }
        if (type == short.class)
        {
            return Short.class;
        }
        if (type == int.class)
        {
            return Integer.class;
        }
        if (type == float.class)
        {
            return Float.class;
        }
        if (type == double.class)
        {
            return Double.class;
        }
        if (type == long.class)
        {
            return Long.class;
        }
        return null;
    }

}
