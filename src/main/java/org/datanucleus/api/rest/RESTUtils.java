/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
   ...
**********************************************************************/
package org.datanucleus.api.rest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jdo.JDOFatalUserException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.NucleusContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.api.rest.fieldmanager.FromJSONFieldManager;
import org.datanucleus.api.rest.fieldmanager.ToJSONFieldManager;
import org.datanucleus.api.rest.jsonobject.GoogleAppEngineKeyHandler;
import org.datanucleus.api.rest.jsonobject.GoogleAppEngineUserHandler;
import org.datanucleus.api.rest.jsonobject.UserTypeJSONHandler;
import org.datanucleus.api.rest.orgjson.JSONArray;
import org.datanucleus.api.rest.orgjson.JSONException;
import org.datanucleus.api.rest.orgjson.JSONObject;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.TypeConversionHelper;

/**
 * Series of convenience methods for manipulating JSONObject objects.
 */
public class RESTUtils
{
    /**
     * Method to convert the provided POJO into its equivalent JSONObject.
     * @param coll Collection of POJOs
     * @param ec ExecutionContext
     * @return The JSONObject
     */
    public static JSONArray getJSONArrayFromCollection(final Collection coll, ExecutionContext ec)
    {
        JSONArray arr = new JSONArray();
        int i = 0;
        for (Object elem : coll)
        {
            try
            {
                arr.put(i++, getJSONObjectFromPOJO(elem, ec));
            }
            catch (JSONException e)
            {
            }
        }
        return arr;
    }

    /**
     * Method to convert the provided POJO into its equivalent JSONObject.
     * @param obj The object
     * @param ec ExecutionContext
     * @return The JSONObject
     */
    public static JSONObject getJSONObjectFromPOJO(final Object obj, ExecutionContext ec)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(obj.getClass(), clr);

        // Create JSONObject
        JSONObject jsonobj = new JSONObject();
        try
        {
            jsonobj.put("class", cmd.getFullClassName());
            if (cmd.getIdentityType() == IdentityType.DATASTORE)
            {
                jsonobj.put("_id", IdentityUtils.getTargetKeyForDatastoreIdentity(ec.getApiAdapter().getIdForObject(obj)));
            }
            if (ec.getApiAdapter().getVersionForObject(obj) != null)
            {
                jsonobj.put("_version", ec.getApiAdapter().getVersionForObject(obj));
            }
        }
        catch (JSONException e)
        {
        }

        // Copy all FetchPlan fields into the object
        ObjectProvider op = ec.findObjectProvider(obj);
        FieldManager fm = new ToJSONFieldManager(jsonobj, cmd, ec);
        op.provideFields(ec.getFetchPlan().getFetchPlanForClass(cmd).getMemberNumbers(), fm);

        return jsonobj;
    }

    /**
     * Method to convert the provided JSONObject into its equivalent object.
     * If it represents a POJO and the POJO is persistent then retrieves it and superimposes the JSONObject values.
     * If it represents a POJO and the POJO is not yet persistent then creates it with the JSONObject values.
     * Also allows some specific non-persistable object types.
     * Throws {@link ClassNotResolvedException} when the class is not found.
     * Throws {@link JDOFatalUserException} if other error occurred.
     * @param jsonobj JSONObject
     * @param className Name of the class
     * @param ec ExecutionContext
     * @return The Object being represented
     */
    public static Object getObjectFromJSONObject(final JSONObject jsonobj, String className, ExecutionContext ec)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForEntityName(className);
        Class cls = null;
        if (cmd != null)
        {
            cls = clr.classForName(cmd.getFullClassName(), true);
        }
        else
        {
            cls = clr.classForName(className, true);
            cmd = ec.getMetaDataManager().getMetaDataForClass(cls, clr);
        }

        if (cmd == null)
        {
            // Non-persistable object (special cases)
            return getNonPersistableObjectFromJSONObject(jsonobj, cls, ec.getNucleusContext());
        }

        // Get the identity of the object, then find the object copying the field values in
        Object id = null;
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            final FieldManager fm = new FromJSONFieldManager(jsonobj, cmd, ec);
            try
            {
                id = IdentityUtils.getApplicationIdentityForResultSetRow(ec, cmd, cls, false, fm);
            }
            catch (NucleusException ne)
            {
                // Not set
            }
        }
        else if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            // Datastore identity - assumed to be in a property "_id"
            try
            {
                if (MetaDataUtils.getTypeOfDatastoreIdentity(cmd.getBaseIdentityMetaData()) == String.class)
                {
                    id = ec.getNucleusContext().getIdentityManager().getDatastoreId(className, jsonobj.getString("_id"));
                }
                else
                {
                    id = ec.getNucleusContext().getIdentityManager().getDatastoreId(className, jsonobj.getLong("_id"));
                }
            }
            catch (JSONException e)
            {
                // Not set
            }
        }

        if (id != null)
        {
            try
            {
                Object pc = ec.findObject(id, true, false, cmd.getFullClassName());
                ObjectProvider pcOP = ec.findObjectProvider(pc);
                FieldManager fm2 = new FromJSONFieldManager(jsonobj, cmd, pcOP);
                pcOP.replaceFields(cmd.getAllMemberPositions(), fm2);
                return pc;
            }
            catch (NucleusException ne)
            {
                // Not yet persistent so needs a transient object
                // TODO Remove DummyStateManager and find better way
                final FieldManager fm = new FromJSONFieldManager(jsonobj, cmd, ec);
                DummyStateManager dummySM = new DummyStateManager(cls);
                dummySM.replaceFields(cmd.getAllMemberPositions(), fm);
                Object pc = dummySM.getObject();
                dummySM.disconnect();
                return pc;
            }
        }

        // TODO Remove DummyStateManager and find better way
        final FieldManager fm = new FromJSONFieldManager(jsonobj, cmd, ec);
        DummyStateManager dummySM = new DummyStateManager(cls);
        dummySM.replaceFields(cmd.getAllMemberPositions(), fm);
        Object pc = dummySM.getObject();
        dummySM.disconnect();
        return pc;
    }

    public static Object getIdentityForURLToken(AbstractClassMetaData cmd, String token, PersistenceNucleusContext nucCtx)
    {
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
            if (cmd.usesSingleFieldIdentityClass())
            {
                Object value = TypeConversionHelper.convertTo(token, cmd.getMetaDataForManagedMemberAtAbsolutePosition(cmd.getPKMemberPositions()[0]).getType());
                return nucCtx.getIdentityManager().getSingleFieldId(clr.classForName(cmd.getObjectidClass()), clr.classForName(cmd.getFullClassName()), value);
            }
            return nucCtx.getIdentityManager().getApplicationId(clr, cmd, token);
        }
        else if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            Object value = TypeConversionHelper.convertTo(token, MetaDataUtils.getTypeOfDatastoreIdentity(cmd.getBaseIdentityMetaData()));
            return nucCtx.getIdentityManager().getDatastoreId(cmd.getFullClassName(), value);
        }
        return null;
    }

    private static Map<String, UserTypeJSONHandler> userClassHandlers = new ConcurrentHashMap();

    /**
     * Deserialise from JSON to an object. Used for non-persistable classes.
     * @param jsonobj JSONObject
     * @param cls The class
     * @param nucCtx NucleusContext
     * @return The object of the specified class
     */
    public static Object getNonPersistableObjectFromJSONObject(final JSONObject jsonobj, final Class cls, NucleusContext nucCtx)
    {
        UserTypeJSONHandler handler = userClassHandlers.get(cls.getName());

        // TODO Make this a plugin point
        if (cls.getName().equals("com.google.appengine.api.users.User"))
        {
            handler = new GoogleAppEngineUserHandler();
            userClassHandlers.put(cls.getName(), handler);
        }
        else if (cls.getName().equals("com.google.appengine.api.datastore.Key"))
        {
            handler = new GoogleAppEngineKeyHandler();
            userClassHandlers.put(cls.getName(), handler);
        }

        if (handler != null)
        {
            return handler.fromJSON(jsonobj, nucCtx);
        }

        try
        {
            return AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        Constructor c = ClassUtils.getConstructorWithArguments(cls, new Class[]{});
                        c.setAccessible(true);
                        Object obj = c.newInstance(new Object[]{});
                        String[] fieldNames = JSONObject.getNames(jsonobj);
                        for (int i = 0; i < jsonobj.length(); i++)
                        {
                            //ignore class field
                            if (!fieldNames[i].equals("class"))
                            {
                                Field field = cls.getField(fieldNames[i]);
                                field.setAccessible(true);
                                field.set(obj, jsonobj.get(fieldNames[i]));
                            }
                        }
                        return obj;
                    }
                    catch (Exception e)
                    {
                        NucleusLogger.GENERAL.error("Exception in conversion from JSONObject field to value", e);
                    }
                    return null;
                }
            });
        }
        catch (SecurityException ex)
        {
            NucleusLogger.DATASTORE_RETRIEVE.warn("Exception in construction of object from JSON", ex);
        }
        return null;
    }
}