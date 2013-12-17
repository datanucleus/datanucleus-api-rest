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

import java.lang.reflect.Array;
import java.util.Collection;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * FieldManager to allow population of a JSONObject with values from fields of a persistable object.
 */
public class ToJSONFieldManager extends AbstractFieldManager
{
    JSONObject jsonobj;
    AbstractClassMetaData cmd;
    ExecutionContext ec;

    public ToJSONFieldManager(JSONObject jsonobj, AbstractClassMetaData cmd, ExecutionContext ec)
    {
        this.jsonobj = jsonobj;
        this.ec = ec;
        this.cmd = cmd;
    }

    public void storeBooleanField(int fieldNumber, boolean value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeCharField(int fieldNumber, char value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeByteField(int fieldNumber, byte value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeShortField(int fieldNumber, short value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeIntField(int fieldNumber, int value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeLongField(int fieldNumber, long value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeFloatField(int fieldNumber, float value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeDoubleField(int fieldNumber, double value)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeStringField(int fieldNumber, String value)
    {
        if (value == null)
        {
            return;
        }

        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        try
        {
            jsonobj.put(mmd.getName(), value);
        }
        catch (JSONException e)
        {
        }
    }

    public void storeObjectField(int fieldNumber, Object value)
    {
        if (value == null)
        {
            return;
        }

        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (RelationType.isRelationSingleValued(relationType))
        {
            // 1-1/N-1
            // TODO Prevent recursion
            JSONObject obj = RESTUtils.getJSONObjectFromPOJO(value, ec);
            try
            {
                jsonobj.put(mmd.getName(), obj);
            }
            catch (JSONException e)
            {
            }
        }
        else if (RelationType.isRelationMultiValued(relationType))
        {
            // 1-N/M-N
            // TODO Prevent recursion
            if (mmd.hasCollection())
            {
                JSONArray arr = new JSONArray();
                Collection collVal = (Collection)value;
                try
                {
                    int i = 0;
                    for (Object elem : collVal)
                    {
                        JSONObject obj = RESTUtils.getJSONObjectFromPOJO(elem, ec);
                        arr.put(i++, obj);
                    }
                    jsonobj.put(mmd.getName(), arr);
                }
                catch (JSONException e)
                {
                }
            }
            else if (mmd.hasArray())
            {
                JSONArray arr = new JSONArray();
                try
                {
                    for (int i=0;i<Array.getLength(value);i++)
                    {
                        JSONObject obj = RESTUtils.getJSONObjectFromPOJO(Array.get(value, i), ec);
                        arr.put(i++, obj);
                    }
                    jsonobj.put(mmd.getName(), arr);
                }
                catch (JSONException e)
                {
                }
            }
            // TODO Support maps
        }
        else
        {
            try
            {
                jsonobj.put(mmd.getName(), value);
            }
            catch (JSONException e)
            {
            }
        }
    }
}