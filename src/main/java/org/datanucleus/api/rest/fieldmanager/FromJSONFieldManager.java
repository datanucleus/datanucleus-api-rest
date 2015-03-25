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
package org.datanucleus.api.rest.fieldmanager;

import java.lang.reflect.Array;
import java.util.Collection;

import org.datanucleus.ExecutionContext;
import org.datanucleus.api.rest.RESTUtils;
import org.datanucleus.api.rest.orgjson.JSONArray;
import org.datanucleus.api.rest.orgjson.JSONException;
import org.datanucleus.api.rest.orgjson.JSONObject;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.TypeConversionHelper;

/**
 * FieldManager responsible for accessing the values from a JSONObject, and putting into a POJO.
 */
public class FromJSONFieldManager extends AbstractFieldManager
{
    JSONObject jsonobj;
    AbstractClassMetaData cmd;
    ExecutionContext ec;
    ObjectProvider op;

    /**
     * @param jsonobj The JSON Object that we are processing the values for.
     * @param cmd Metadata for the class
     * @param ec ExecutionContext
     */
    public FromJSONFieldManager(JSONObject jsonobj, AbstractClassMetaData cmd, ExecutionContext ec)
    {
        this.jsonobj = jsonobj;
        this.cmd = cmd;
        this.ec = ec;
    }

    public FromJSONFieldManager(JSONObject jsonobj, AbstractClassMetaData cmd, ObjectProvider op)
    {
        this.jsonobj = jsonobj;
        this.cmd = cmd;
        this.op = op;
        this.ec = op.getExecutionContext();
    }

    public boolean fetchBooleanField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return false;
        }
        try
        {
            boolean val = jsonobj.getBoolean(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            return val;
        }
        catch (JSONException e)
        {
            NucleusLogger.DATASTORE_RETRIEVE.warn("Exception in fetch of boolean field", e);
        }
        return false;
    }

    public byte fetchByteField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return 0;
        }
        try
        {
            String str = jsonobj.getString(fieldName);
            byte value = 0;
            if (str != null && str.length() > 0)
            {
                value = str.getBytes()[0];
            }
            if (op != null)
            {
                op.makeDirty(position);
            }
            return value;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return 0;
    }

    public char fetchCharField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return 0;
        }
        try
        {
            String str = jsonobj.getString(fieldName);
            char value = 0;
            if (str != null && str.length() > 0)
            {
                value = str.charAt(0);
            }
            if (op != null)
            {
                op.makeDirty(position);
            }
            return value;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return 0;
    }

    public double fetchDoubleField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return 0;
        }
        try
        {
            double val = jsonobj.getDouble(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            return val;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return 0;
    }

    public float fetchFloatField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return 0;
        }
        try
        {
            float val = (float) jsonobj.getDouble(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            return val;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return 0;
    }

    public int fetchIntField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return 0;
        }
        try
        {
            int val = jsonobj.getInt(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            return val;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return 0;
    }

    public long fetchLongField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return 0;
        }
        try
        {
            long val = jsonobj.getLong(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            return val;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return 0;
    }

    public short fetchShortField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return 0;
        }
        try
        {
            short val = (short) jsonobj.getInt(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            return val;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return 0;
    }

    public String fetchStringField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return null;
        }
        try
        {
            String val = jsonobj.getString(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            return val;
        }
        catch (JSONException e)
        {
            // should not happen
        }
        return null;
    }

    public Object fetchObjectField(int position)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position);
        if (!jsonobj.has(mmd.getName()))
        {
            return null;
        }

        try
        {
            if (jsonobj.isNull(mmd.getName()))
            {
                return null;
            }

            Object value = jsonobj.get(mmd.getName());
            if (op != null)
            {
                op.makeDirty(position);
            }

            if (mmd.hasCollection())
            {
                JSONArray array = (JSONArray)value;
                Collection<Object> coll;
                try
                {
                    Class instanceType = SCOUtils.getContainerInstanceType(mmd.getType(), mmd.getOrderMetaData() != null);
                    coll = (Collection<Object>) instanceType.newInstance();
                }
                catch (Exception e)
                {
                    throw new NucleusDataStoreException("Exception creating container for field " + mmd.getFullFieldName(), e);
                }

                for (int i=0; i<array.length(); i++)
                {
                    if (array.isNull(i))
                    {
                        // TODO Are nulls allowed for this field?
                        coll.add(null);
                    }
                    else
                    {
                        Object elemValue = array.get(i);
                        if (elemValue instanceof JSONObject)
                        {
                            coll.add(RESTUtils.getObjectFromJSONObject((JSONObject)elemValue, mmd.getCollection().getElementType(), ec));
                        }
                        else
                        {
                            coll.add(TypeConversionHelper.convertTo(elemValue, mmd.getType()));
                        }
                    }
                }
                return coll;
            }
            else if (mmd.hasArray())
            {
                JSONArray array = (JSONArray)value;
                String elementType = mmd.getArray().getElementType();

                Object arr = Array.newInstance(mmd.getType().getComponentType(), array.length());

                for (int i=0; i<array.length(); i++)
                {
                    if (array.isNull(i))
                    {
                        // TODO Are nulls allowed?
                        Array.set(arr, i, null);
                    }
                    else
                    {
                        Object elemValue = array.get(i);
                        if (elemValue instanceof JSONObject)
                        {
                            Array.set(arr, i, RESTUtils.getObjectFromJSONObject((JSONObject)elemValue, elementType, ec));
                        }
                        else
                        {
                            Array.set(arr, i, TypeConversionHelper.convertTo(elemValue, mmd.getType()));
                        }
                    }
                }
                return arr;
            }
            else if (mmd.hasMap())
            {
                // TODO Implement support for Maps
                throw new NucleusException("Dont currently support persist of Map field at " + mmd.getFullFieldName());
            }

            if (RelationType.isRelationSingleValued(mmd.getRelationType(ec.getClassLoaderResolver())))
            {
                return RESTUtils.getObjectFromJSONObject((JSONObject)value, mmd.getTypeName(), ec);
            }

            String fieldType = mmd.getTypeName();
            try
            {
                // Use "class" if provided
                fieldType = ((JSONObject)value).getString("class");
            }
            catch (JSONException jsone)
            {
                NucleusLogger.GENERAL.info("Persistent field " + mmd.getFullFieldName() + " has indeterminate type. Specify 'class' JSON attribute to workaround this");
            }

            if (value instanceof JSONObject)
            {
                return RESTUtils.getObjectFromJSONObject((JSONObject)value, fieldType, ec);
            }
            return TypeConversionHelper.convertTo(value, cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getType());
        }
        catch (JSONException ex)
        {
            NucleusLogger.GENERAL.error("Exception thrown processing field " + mmd.getFullFieldName(), ex);
            throw new NucleusException("Exception thrown during persist", ex);
        }
    }
}