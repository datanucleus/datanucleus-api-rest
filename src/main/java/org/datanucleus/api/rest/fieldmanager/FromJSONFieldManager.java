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

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.ExecutionContext;
import org.datanucleus.api.rest.RESTUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.TypeConversionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public Object fetchObjectField(int position)
    {
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getName();
        if (!jsonobj.has(fieldName))
        {
            return null;
        }
        try
        {
            if (jsonobj.isNull(fieldName))
            {
                return null;
            }

            Object value = jsonobj.get(fieldName);
            if (op != null)
            {
                op.makeDirty(position);
            }
            if (value instanceof JSONObject)
            {
                return RESTUtils.getObjectFromJSONObject((JSONObject)value, ((JSONObject)value).getString("class"), ec);
            }
            if (value instanceof JSONArray)
            {
                return fetchJSONArray((JSONArray)value,position);
            }
            return TypeConversionHelper.convertTo(value, cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getType());
        }
        catch(JSONException ex)
        {
            throw new RuntimeException(ex);
        }
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

    private List fetchJSONArray(JSONArray array,  int position) throws JSONException
    {
        List elements = new ArrayList();
        for (int i=0; i<array.length(); i++)
        {
            if (array.isNull(i))
            {
                elements.add(null);
            }
            else
            {
                Object value = array.get(i);
                
                if (value instanceof JSONObject)
                {
                    elements.add(RESTUtils.getObjectFromJSONObject((JSONObject)value, ((JSONObject)value).getString("class"), ec));
                }
                else if (value instanceof JSONArray)
                {
                    elements.add(fetchJSONArray((JSONArray)value,position));
                }
                else
                {
                    elements.add(TypeConversionHelper.convertTo(value, cmd.getMetaDataForManagedMemberAtAbsolutePosition(position).getType()));
                }
            }
        }
        return elements;
    }
}