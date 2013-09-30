/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.util;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Utility reflection methods.
 * <p>
 * @author Spence Koehler
 */
public class ReflectUtil {
  
  /**
   * Build and instance of an object based on a string of the form:
   * <ol><li>classpath -- constructs a new instance</li>
   * <li>classpath:buildMethod -- calls a static properties-arg or no-arg factory method</li></ol>
   *
   * @param instanceString  string of the form classpath[:method]
   * @param properties      properties to feed to the constructor or method
   *                        if accepted.
   *
   * @return the designated object.
   */
  public static Object buildInstance(String instanceString, Properties properties) {
    Object result = null;

    String[] pieces = instanceString.split(":");
    if (pieces.length == 1) {
      result = newInstance(pieces[0], properties);
    }
    else if (pieces.length == 2) {
      result = getInstance(pieces[0], pieces[1], properties);
    }
    
    return result;
  }

  /**
   * Build and instance of an object based on a string of the form:
   * <ol><li>classpath -- constructs a new instance</li>
   * <li>classpath:buildMethod -- calls a static no-arg factory method</li></ol>
   *
   * @param instanceString  string of the form classpath[:method]
   *
   * @return the designated object.
   */
  public static Object buildInstance(String instanceString) {
    Object result = null;

    String[] pieces = instanceString.split(":");
    if (pieces.length == 1) {
      result = newInstance(pieces[0]);
    }
    else if (pieces.length == 2) {
      result = getInstance(pieces[0], pieces[1]);
    }
    
    return result;
  }

  /**
   * Get a new instance of an object based on its classpath.
   *
   * @param classname  the name of a class.
   * @param properties      properties to feed to the constructor or method
   *                        if accepted.
   *
   * @return an instance of the class created through its default (no-arg) constructor.
   */
  public static Object newInstance(String classname, Properties properties) {
    Object result = null;
    Class<?> theClass = null;

    try {
      theClass = Class.forName(classname);
      final Constructor constructor = theClass.getConstructor(Class.forName("java.util.Properties"));
      result = constructor.newInstance(properties);
    }
    catch (NoSuchMethodException e) {
      // when can't find a constructor that takes properties, use default constructor.
      result = newInstance(theClass);
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException("Can't instantiate '" + classname + "' ...", e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Can't access '" + classname + "' ...", e);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Can't find '" + classname + "' ...", e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException("Can't invoke '" + classname + ".newInstance(properties)' ...", e);
    }
    catch (NullPointerException e) {
      throw new IllegalArgumentException("Null instance '" + classname + "' ...", e);
    }

    return result;
  }

  /**
   * Get a new instance of an object based on its classpath.
   *
   * @param classname  the name of a class.
   *
   * @return an instance of the class created through its default (no-arg) constructor.
   */
  public static Object newInstance(String classname) {
    Object result = null;

    try {
      final Class theClass = Class.forName(classname);
      result = newInstance(theClass);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Can't find '" + classname + "' ...", e);
    }
    catch (NullPointerException e) {
      throw new IllegalArgumentException("Null instance '" + classname + "' ...", e);
    }

    return result;
  }

  /**
   * Get a new instance of an object based on its class.
   *
   * @param theClass  the class.
   *
   * @return an instance of the class created through its default (no-arg) constructor.
   */
  public static Object newInstance(Class theClass) {
    Object result = null;

    try {
      result = theClass.newInstance();
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException("Can't instantiate '" + theClass + "' ...", e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Can't access '" + theClass + "' ...", e);
    }
    catch (NullPointerException e) {
      throw new IllegalArgumentException("Null instance '" + theClass + "' ...", e);
    }

    return result;
  }

  /**
   * Get an instance of an object based on a static factory method.
   *
   * @param classname        the name of a class.
   * @param buildMethodName  the static factory method in the class.
   * @param properties      properties to feed to the constructor or method
   *                        if accepted.
   *
   * @return an object created through the factory method that accepts properties
   *         or the no-arg method if properties aren't accepted.
   */
  public static Object getInstance(String classname, String buildMethodName, Properties properties) {
    Object result = null;
    Class<?> theClass = null;

    try {
      theClass = Class.forName(classname);
      Method buildMethod = theClass.getMethod(buildMethodName, Class.forName("java.util.Properties"));
      result = buildMethod.invoke(null, properties);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Can't find class '" + classname + "' ...", e);
    }
    catch (NoSuchMethodException e) {
      // couldn't find a method accepting properties. Use no-arg method.
      result = getInstance(theClass, buildMethodName);
    }
    catch (SecurityException e) {
      throw new IllegalArgumentException("Can't access '" + classname + "." + buildMethodName + "' ...", e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Can't access '" + classname + "." + buildMethodName + "' ...", e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException("Can't invoke '" + classname + "." + buildMethodName + "' ...", e);
    }
    catch (NullPointerException e) {
      throw new IllegalArgumentException("Null instance '" + classname + "." + buildMethodName + "' ...", e);
    }

    return result;
  }

  /**
   * Get an instance of an object based on a static factory method.
   *
   * @param classname        the name of a class.
   * @param buildMethodName  the static factory method in the class.
   *
   * @return an object created through the (no-arg) factory method.
   */
  public static Object getInstance(String classname, String buildMethodName) {
    Object result = null;

    try {
      Class theClass = Class.forName(classname);
      result = getInstance(theClass, buildMethodName);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Can't find class '" + classname + "' ...", e);
    }
    catch (NullPointerException e) {
      throw new IllegalArgumentException("Null instance '" + classname + "." + buildMethodName + "' ...", e);
    }

    return result;
  }

  /**
   * Get an instance of an object based on a static factory method.
   *
   * @param theClass         the class.
   * @param buildMethodName  the static factory method in the class.
   *
   * @return an object created through the (no-arg) factory method.
   */
  public static Object getInstance(Class<?> theClass, String buildMethodName) {
    Object result = null;

    try {
      Method buildMethod = theClass.getMethod(buildMethodName);
      result = buildMethod.invoke(null);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Can't find method '" + buildMethodName + "' ...", e);
    }
    catch (SecurityException e) {
      throw new IllegalArgumentException("Can't access '" + theClass + "." + buildMethodName + "' ...", e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Can't access '" + theClass + "." + buildMethodName + "' ...", e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException("Can't invoke '" + theClass + "." + buildMethodName + "' ...", e);
    }
    catch (NullPointerException e) {
      throw new IllegalArgumentException("Null instance '" + theClass + "." + buildMethodName + "' ...", e);
    }

    return result;
  }

  /**
   * Get the class with the given classname;
   */
  public static Class getClass(String classname) {
    Class result = null;

    try {
      result = Class.forName(classname);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }

  /**
   * Get an instance of the class from its buildMethodName taking the single arg.
   */
  public static Object getInstance(Class<?> theClass, String buildMethodName, Object arg) {
    Object result = null;

    try {
      Method buildMethod = theClass.getMethod(buildMethodName, arg.getClass());
      result = buildMethod.invoke(null, arg);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(theClass.getName() + "." + buildMethodName + "(" + arg.getClass().getName() + " " + arg + ")", e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException(theClass.getName() + "." + buildMethodName + "(" + arg.getClass().getName() + " " + arg + ")", e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException(theClass.getName() + "." + buildMethodName + "(" + arg.getClass().getName() + " " + arg + ")", e);
    }

    return result;
  }

  /**
   * Construct an instance of the class from its constructor taking a single
   * argument of the object parameter's type.
   * <p>
   * NOTES:
   * - Superclasses of the arg are considered when searching for a constructor.
   * - If a constructor taking the arg is not found, empty constructors are ignored.
   */
  public static Object constructInstance(Class<?> theClass, Object arg) {
    return constructInstance(theClass, arg, false);
  }

  /**
   * Construct an instance of the class from its constructor taking a single
   * argument of the object parameter's type.
   * <p>
   * NOTES:
   * - Superclasses of the arg are considered when searching for a constructor.
   * - If a constructor taking the arg is not found, the empty constructor will
   *   be attempted if fallbackToEmptyConstructor is true.
   */
  public static Object constructInstance(Class<?> theClass, Object arg, boolean fallbackToEmptyConstructor) {
    Object result = null;

    Class argClass = arg.getClass();

    while (argClass != null && result == null) {
      try {
        final Constructor constructor = theClass.getConstructor(argClass);
        result = constructor.newInstance(arg);
      }
      catch (NoSuchMethodException e) {
        argClass = argClass.getSuperclass();

        if (argClass == null) {
          if (fallbackToEmptyConstructor) {
            result = newInstance(theClass);
          }
          else {
            throw new IllegalArgumentException(theClass.getName() + "(" + arg.getClass().getName() + " " + arg + ")", e);
          }
        }
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(theClass.getName() + "(" + arg.getClass().getName() + " " + arg + ")", e);
      }
      catch (InvocationTargetException e) {
        throw new IllegalArgumentException(theClass.getName() + "(" + arg.getClass().getName() + " " + arg + ")", e);
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(theClass.getName() + "(" + arg.getClass().getName() + " " + arg + ")", e);
      }
    }

    return result;
  }


  /**
   * Construct an instance of the class from its constructor taking a single
   * argument of the object parameter's type.
   */
  public static Object constructInstance(Class<?> theClass, Class<?>[] theArgTypes, Object[] args) {
    Object result = null;

    try {
      final Constructor<?> constructor = theClass.getConstructor(theArgTypes);
      result = constructor.newInstance(args);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(theClass.getName() + "(" + theArgTypes.length + " args)", e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException(theClass.getName() + "(" + theArgTypes.length + " args)", e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException(theClass.getName() + "(" + theArgTypes.length + " args)", e);
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException(theClass.getName() + "(" + theArgTypes.length + " args)", e);
    }

    return result;
  }

/*
  public static Object constructInstance(Class<?> theClass, Object[] args) {
    Object result = null;

    final List<Collection<Class<?>>> classCollections = new ArrayList<Collection<Class<?>>>();

    for (Object arg : args) {
      final List<Class<?>> classList = new ArrayList<Class<?>>();
      Class<?> argClass = arg.getClass();
      while (argClass != null) {
        classList.add(argClass);
        argClass = argClass.getSuperclass();
      }
      classCollections.add(classList);
    }

    final List<Collection<Class<?>>> combos = GeneralUtil.combine(classCollections);
    NoSuchMethodException nsme = null;

    for (Collection<Class<?>> classList : combos) {
      final Class<?>[] argTypes = classList.toArray(new Class<?>[classList.size()]);

      try {
        final Constructor<?> constructor = theClass.getConstructor(argTypes);
        result = constructor.newInstance(args);
      }
      catch (NoSuchMethodException e) {
        // ignore and try next combo
        nsme = e;
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args)", e);
      }
      catch (InvocationTargetException e) {
        throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args)", e);
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args)", e);
      }

      if (result != null) {
        break;
      }
    }

    if (result == null && nsme != null) {
      throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args) " + combos, nsme);
    }

    return result;
  }
*/

  public static Object constructInstance(Class<?> theClass, Object[] args) {
    Object result = null;

    try {
      Constructor<?> constructor = null;
      
      final Constructor<?>[] constructors = theClass.getConstructors();
      for (Constructor<?> c : constructors) {
        final Class<?>[] params = c.getParameterTypes();
        if (params.length == args.length) {
          boolean paramsMatch = true;
          for (int i = 0; i < params.length; ++i) {
            if (params[i] != null && args[i] != null && !params[i].isAssignableFrom(args[i].getClass())) {
              paramsMatch = false;
            }
          }
          if (paramsMatch) {
            constructor = c;
            break;
          }
        }
      }

      if (constructor == null) {
        throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args) constructor not found!");
      }

      result = constructor.newInstance(args);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args)", e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args)", e);
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException(theClass.getName() + "(" + args.length + " args)", e);
    }

    return result;
  }

  /**
   * Find and invoke the method on the object if it exists.
   * <p>
   * If result is an array of size 1, then return the return value from
   * invoking the method in result[0]. Note that if the method returns
   * "void", the result object (result[0]) will be null.
   *
   * @return true if method was found and invoked; otherwise, false.
   */
  public static boolean invokeMethod(Object object, String methodName, Object[] params, Object[] result) {
    if (object == null) return false;

    boolean retval = false;

    Method method = null;
    Class<?>[] paramTypes = params == null ? null : new Class<?>[params.length];

    if (params != null) {
      for (int i = 0; i < params.length; ++i) {
        paramTypes[i] = params[i] == null ? null : params[i].getClass();
      }
    }

    final Class<?> theClass = object.getClass();

    try {
      method = theClass.getMethod(methodName, paramTypes);
    }
    catch (NoSuchMethodException nsme) {
      // just return false
    }
    catch (SecurityException se) {
      // rethrow
      throw new IllegalArgumentException(theClass.getName() + "." + methodName, se);
    }

    // invoke
    if (method != null) {
      try {
        final Object invocationResult = method.invoke(object, params);
        if (result != null && result.length > 0) result[0] = invocationResult;
        retval = true;
      }
      catch (IllegalAccessException iae) {
        throw new IllegalArgumentException(theClass.getName() + "." + methodName, iae);
      }
      catch (InvocationTargetException ite) {
        throw new IllegalArgumentException(theClass.getName() + "." + methodName, ite);
      }
      catch (ExceptionInInitializerError eiie) {
        throw new IllegalArgumentException(theClass.getName() + "." + methodName, eiie);
      }
    }

    return retval;
  }
}
