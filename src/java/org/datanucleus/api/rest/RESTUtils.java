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

import javax.jdo.JDOFatalUserException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.NucleusContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.identity.OID;
import org.datanucleus.identity.OIDFactory;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.TypeConversionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                OID oid = (OID)ec.getApiAdapter().getIdForObject(obj);
                jsonobj.put("_id", oid.getKeyValue());
            }
            if (ec.getApiAdapter().getVersionForObject(obj) != null)
            {
                Object ver = ec.getApiAdapter().getVersionForObject(obj);
                jsonobj.put("_version", ver);
            }
        }
        catch (JSONException e)
        {
        }

        // Copy all FetchPlan fields into the object
        ObjectProvider op = ec.findObjectProvider(obj);
        int[] fpMembers = ec.getFetchPlan().getFetchPlanForClass(cmd).getMemberNumbers();
        FieldManager fm = new ToJSONFieldManager(jsonobj, cmd, ec);
        op.provideFields(fpMembers, fm);

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
                    String idVal = jsonobj.getString("_id");
                    id = OIDFactory.getInstance(ec.getNucleusContext(), className, idVal);
                }
                else
                {
                    long idVal = jsonobj.getLong("_id");
                    id = OIDFactory.getInstance(ec.getNucleusContext(), className, idVal);
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
                final FieldManager fm = new FromJSONFieldManager(jsonobj, cmd, ec);
                DummyStateManager dummySM = new DummyStateManager(cls);
                int[] fieldNumbers = cmd.getAllMemberPositions();
                dummySM.replaceFields(fieldNumbers, fm);
                Object obj = dummySM.getObject();
                dummySM.disconnect();
                return obj;
            }
        }

        final FieldManager fm = new FromJSONFieldManager(jsonobj, cmd, ec);
        DummyStateManager dummySM = new DummyStateManager(cls);
        int[] fieldNumbers = cmd.getAllMemberPositions();
        dummySM.replaceFields(fieldNumbers, fm);
        Object obj = dummySM.getObject();
        dummySM.disconnect();
        return obj;
    }

    public static Object getIdentityForURLToken(AbstractClassMetaData cmd, String token, PersistenceNucleusContext nucCtx)
    {
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            if (cmd.usesSingleFieldIdentityClass())
            {
                ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
                Object value = TypeConversionHelper.convertTo(token,
                    cmd.getMetaDataForManagedMemberAtAbsolutePosition(cmd.getPKMemberPositions()[0]).getType());
                return nucCtx.getApiAdapter().getNewSingleFieldIdentity(clr.classForName(cmd.getObjectidClass()), 
                    clr.classForName(cmd.getFullClassName()), value);
            }
            // TODO Composite PK?
        }
        else if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            Class type = MetaDataUtils.getTypeOfDatastoreIdentity(cmd.getBaseIdentityMetaData());
            Object value = TypeConversionHelper.convertTo(token, type);
            return OIDFactory.getInstance(nucCtx, cmd.getFullClassName(), value);
        }
        return null;
    }

    /**
     * Deserialise from JSON to an object. Used for non-persistable classes.
     * @param jsonobj JSONObject
     * @param cls The class
     * @param nucCtx NucleusContext
     * @return The object of the specified class
     */
    public static Object getNonPersistableObjectFromJSONObject(final JSONObject jsonobj, final Class cls,
            NucleusContext nucCtx)
    {
        ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
        if (cls.getName().equals("com.google.appengine.api.users.User"))
        {
            String email = null;
            String authDomain = null;
            try
            {
                email = jsonobj.getString("email");
            }
            catch (JSONException e)
            {
                // should not happen if the field exists
            }
            try
            {
                authDomain = jsonobj.getString("authDomain");
            }
            catch (JSONException e)
            {
                // should not happen if the field exists
            }
            return ClassUtils.newInstance(cls, new Class[]{String.class, String.class}, new String[]{email, authDomain});
        }
        else if (cls.getName().equals("com.google.appengine.api.datastore.Key"))
        {
            try
            {
                Object parent = null;
                if (jsonobj.has("parent") && !jsonobj.isNull("parent"))
                {
                    //if it's a JSONObject
                    JSONObject parentobj = jsonobj.getJSONObject("parent");
                    parent = RESTUtils.getNonPersistableObjectFromJSONObject(parentobj, 
                        clr.classForName(jsonobj.getString("class")), nucCtx);
                }
                if (jsonobj.has("appId"))
                {
                    String appId = jsonobj.getString("appId");
                    String kind = jsonobj.getString("kind");
                    Class keyFactory = clr.classForName("com.google.appengine.api.datastore.KeyFactory", false);
                    if (parent != null)
                    {
                        return ClassUtils.getMethodForClass(keyFactory, "createKey",
                            new Class[]{cls, String.class,String.class}).invoke(null, new Object[]{parent, kind, appId});
                    }
                    else
                    {
                        return ClassUtils.getMethodForClass(keyFactory, "createKey",
                            new Class[]{String.class,String.class}).invoke(null, new Object[]{kind, appId});
                    }
                }
                else
                {
                    long id = jsonobj.getLong("id");
                    String kind = jsonobj.getString("kind");
                    Class keyFactory = clr.classForName("com.google.appengine.api.datastore.KeyFactory", false);
                    if (parent != null)
                    {
                        return ClassUtils.getMethodForClass(keyFactory, "createKey",
                            new Class[]{cls,String.class,long.class}).invoke(null, new Object[]{parent,kind,Long.valueOf(id)});
                    }
                    else
                    {
                        return ClassUtils.getMethodForClass(keyFactory, "createKey",
                            new Class[]{String.class,long.class}).invoke(null, new Object[]{kind,Long.valueOf(id)});
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
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

        }
        return null;
    }
}