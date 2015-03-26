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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.api.rest.RESTUtils;
import org.datanucleus.api.rest.orgjson.JSONArray;
import org.datanucleus.api.rest.orgjson.JSONException;
import org.datanucleus.api.rest.orgjson.JSONObject;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;

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

        // TODO Support embedded 1-1, 1-N
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relationType = mmd.getRelationType(clr);
        try
        {
            if (RelationType.isRelationSingleValued(relationType))
            {
                // 1-1/N-1 TODO Prevent recursion
                JSONObject obj = RESTUtils.getJSONObjectFromPOJO(value, ec);
                jsonobj.put(mmd.getName(), obj);
            }
            else if (mmd.hasCollection())
            {
                AbstractClassMetaData elemCmd = mmd.getCollection().getElementClassMetaData(clr, ec.getMetaDataManager());
                JSONArray arr = new JSONArray();
                Collection collVal = (Collection)value;
                int i = 0;
                for (Object elem : collVal)
                {
                    if (elemCmd != null)
                    {
                        arr.put(i++, RESTUtils.getJSONObjectFromPOJO(elem, ec)); // TODO Prevent recursion
                    }
                    else
                    {
                        arr.put(i++, elem);
                    }
                }
                jsonobj.put(mmd.getName(), arr);
            }
            else if (mmd.hasArray())
            {
                AbstractClassMetaData elemCmd = mmd.getArray().getElementClassMetaData(clr, ec.getMetaDataManager());
                JSONArray arr = new JSONArray();
                for (int i=0;i<Array.getLength(value);i++)
                {
                    Object elem = Array.get(value, i);
                    if (elemCmd != null)
                    {
                        arr.put(i++, RESTUtils.getJSONObjectFromPOJO(elem, ec)); // TODO Prevent recursion
                    }
                    else
                    {
                        arr.put(i++, elem);
                    }
                }
                jsonobj.put(mmd.getName(), arr);
            }
            else if (mmd.hasMap())
            {
                AbstractClassMetaData keyCmd = mmd.getMap().getKeyClassMetaData(clr, ec.getMetaDataManager());
                AbstractClassMetaData valCmd = mmd.getMap().getValueClassMetaData(clr, ec.getMetaDataManager());
                Map jsonMap = new HashMap();
                Iterator<Map.Entry> mapIter = ((Map)value).entrySet().iterator();
                while (mapIter.hasNext())
                {
                    Map.Entry entry = mapIter.next();
                    Object key = null;
                    Object val = null;
                    if (keyCmd != null)
                    {
                        key = RESTUtils.getJSONObjectFromPOJO(entry.getKey(), ec); // TODO Prevent recursion
                    }
                    else
                    {
                        key = entry.getKey();
                    }

                    if (valCmd != null)
                    {
                        val = RESTUtils.getJSONObjectFromPOJO(entry.getValue(), ec); // TODO Prevent recursion
                    }
                    else
                    {
                        val = entry.getValue();
                    }

                    jsonMap.put(key, val);
                }
                jsonobj.put(mmd.getName(), jsonMap);
            }
            else
            {
                jsonobj.put(mmd.getName(), value);
            }
        }
        catch (JSONException jsone)
        {
            throw new NucleusException("Exception converting value of field " + mmd.getFullFieldName() + " to JSON", jsone);
        }
    }
}