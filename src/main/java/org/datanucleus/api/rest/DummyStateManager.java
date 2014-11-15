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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.datanucleus.ExecutionContext;
import org.datanucleus.enhancement.Detachable;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.enhancement.StateManager;
import org.datanucleus.enhancer.EnhancementHelper;
import org.datanucleus.store.fieldmanager.FieldManager;

/**
 * 
 */
public class DummyStateManager implements StateManager
{
    Persistable myPC;

    FieldManager fm;

    public DummyStateManager(Class cls)
    {
        myPC = EnhancementHelper.getInstance().newInstance(cls, this);
    }

    public boolean getBooleanField(Persistable arg0, int arg1, boolean arg2)
    {
        return false;
    }

    public byte getByteField(Persistable arg0, int arg1, byte arg2)
    {
        return 0;
    }

    public char getCharField(Persistable arg0, int arg1, char arg2)
    {
        return 0;
    }

    public double getDoubleField(Persistable arg0, int arg1, double arg2)
    {
        return 0;
    }

    public float getFloatField(Persistable arg0, int arg1, float arg2)
    {
        return 0;
    }

    public int getIntField(Persistable arg0, int arg1, int arg2)
    {
        return 0;
    }

    public long getLongField(Persistable arg0, int arg1, long arg2)
    {
        return 0;
    }

    public Object getObjectField(Persistable arg0, int arg1, Object arg2)
    {
        return null;
    }

    public Object getObjectId(Persistable arg0)
    {
        return null;
    }

    public ExecutionContext getExecutionContext(Persistable arg0)
    {
        return null;
    }

    public short getShortField(Persistable arg0, int arg1, short arg2)
    {
        return 0;
    }

    public String getStringField(Persistable arg0, int arg1, String arg2)
    {
        return null;
    }

    public Object getTransactionalObjectId(Persistable arg0)
    {
        return null;
    }

    public Object getVersion(Persistable arg0)
    {
        return null;
    }

    public boolean isDeleted(Persistable arg0)
    {
        return false;
    }

    public boolean isDirty(Persistable arg0)
    {
        return false;
    }

    public boolean isLoaded(Persistable arg0, int arg1)
    {
        return false;
    }

    public boolean isNew(Persistable arg0)
    {
        return false;
    }

    public boolean isPersistent(Persistable arg0)
    {
        return false;
    }

    public boolean isTransactional(Persistable arg0)
    {
        return false;
    }

    public void makeDirty(Persistable arg0, String arg1)
    {
    }

    public void preSerialize(Persistable arg0)
    {
    }

    public void providedBooleanField(Persistable arg0, int arg1, boolean arg2)
    {
    }

    public void providedByteField(Persistable arg0, int arg1, byte arg2)
    {
    }

    public void providedCharField(Persistable arg0, int arg1, char arg2)
    {
    }

    public void providedDoubleField(Persistable arg0, int arg1, double arg2)
    {
    }

    public void providedFloatField(Persistable arg0, int arg1, float arg2)
    {
    }

    public void providedIntField(Persistable arg0, int arg1, int arg2)
    {
    }

    public void providedLongField(Persistable arg0, int arg1, long arg2)
    {
    }

    public void providedObjectField(Persistable arg0, int arg1, Object arg2)
    {
    }

    public void providedShortField(Persistable arg0, int arg1, short arg2)
    {
    }

    public void providedStringField(Persistable arg0, int arg1, String arg2)
    {
    }

    public boolean replacingBooleanField(Persistable arg0, int arg1)
    {
        return fm.fetchBooleanField(arg1);
    }

    public byte replacingByteField(Persistable arg0, int arg1)
    {
        return fm.fetchByteField(arg1);
    }

    public char replacingCharField(Persistable arg0, int arg1)
    {
        return fm.fetchCharField(arg1);
    }

    public Object[] replacingDetachedState(Detachable arg0, Object[] arg1)
    {
        return null;
    }

    public double replacingDoubleField(Persistable arg0, int arg1)
    {
        return fm.fetchDoubleField(arg1);
    }

    public byte replacingFlags(Persistable arg0)
    {
        return 0;
    }

    public float replacingFloatField(Persistable arg0, int arg1)
    {
        return fm.fetchFloatField(arg1);
    }

    public int replacingIntField(Persistable arg0, int arg1)
    {
        return fm.fetchIntField(arg1);
    }

    public long replacingLongField(Persistable arg0, int arg1)
    {
        return fm.fetchLongField(arg1);
    }

    public Object replacingObjectField(Persistable arg0, int arg1)
    {
        return fm.fetchObjectField(arg1);
    }

    public short replacingShortField(Persistable arg0, int arg1)
    {
        return fm.fetchShortField(arg1);
    }

    public StateManager replacingStateManager(Persistable arg0, StateManager arg1)
    {
        return null;
    }

    public String replacingStringField(Persistable arg0, int arg1)
    {
        return fm.fetchStringField(arg1);
    }

    public void setBooleanField(Persistable arg0, int arg1, boolean arg2, boolean arg3)
    {
    }

    public void setByteField(Persistable arg0, int arg1, byte arg2, byte arg3)
    {
    }

    public void setCharField(Persistable arg0, int arg1, char arg2, char arg3)
    {
    }

    public void setDoubleField(Persistable arg0, int arg1, double arg2, double arg3)
    {
    }

    public void setFloatField(Persistable arg0, int arg1, float arg2, float arg3)
    {
    }

    public void setIntField(Persistable arg0, int arg1, int arg2, int arg3)
    {
    }

    public void setLongField(Persistable arg0, int arg1, long arg2, long arg3)
    {
    }

    public void setObjectField(Persistable arg0, int arg1, Object arg2, Object arg3)
    {
    }

    public void setShortField(Persistable arg0, int arg1, short arg2, short arg3)
    {
    }

    public void setStringField(Persistable arg0, int arg1, String arg2, String arg3)
    {
    }

    void replaceFields(int[] fieldNumbers, FieldManager fm)
    {
        this.fm = fm;
        myPC.dnReplaceFields(fieldNumbers);
    }

    public Object getObject()
    {
        return myPC;
    }

    public void disconnect()
    {
        try
        {
            // Calls to pc.dnReplaceStateManager must be run privileged
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    myPC.dnReplaceStateManager(null);
                    return null;
                }
            });
        }
        catch (SecurityException e)
        {
        }
    }
}