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
   */
  public static Object constructInstance(Class<?> theClass, Object arg) {
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
          throw new IllegalArgumentException(theClass.getName() + "(" + arg.getClass().getName() + " " + arg + ")", e);
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
}
