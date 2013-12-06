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

import javax.jdo.PersistenceManager;
import javax.jdo.spi.Detachable;
import javax.jdo.spi.JDOImplHelper;
import javax.jdo.spi.PersistenceCapable;
import javax.jdo.spi.StateManager;

import org.datanucleus.store.fieldmanager.FieldManager;

/**
 * 
 */
public class DummyStateManager implements StateManager
{
    PersistenceCapable myPC;

    FieldManager fm;

    public DummyStateManager(Class cls)
    {
        myPC = JDOImplHelper.getInstance().newInstance(cls, this);
    }

    public boolean getBooleanField(PersistenceCapable arg0, int arg1, boolean arg2)
    {
        return false;
    }

    public byte getByteField(PersistenceCapable arg0, int arg1, byte arg2)
    {
        return 0;
    }

    public char getCharField(PersistenceCapable arg0, int arg1, char arg2)
    {
        return 0;
    }

    public double getDoubleField(PersistenceCapable arg0, int arg1, double arg2)
    {
        return 0;
    }

    public float getFloatField(PersistenceCapable arg0, int arg1, float arg2)
    {
        return 0;
    }

    public int getIntField(PersistenceCapable arg0, int arg1, int arg2)
    {
        return 0;
    }

    public long getLongField(PersistenceCapable arg0, int arg1, long arg2)
    {
        return 0;
    }

    public Object getObjectField(PersistenceCapable arg0, int arg1, Object arg2)
    {
        return null;
    }

    public Object getObjectId(PersistenceCapable arg0)
    {
        return null;
    }

    public PersistenceManager getPersistenceManager(PersistenceCapable arg0)
    {
        return null;
    }

    public short getShortField(PersistenceCapable arg0, int arg1, short arg2)
    {
        return 0;
    }

    public String getStringField(PersistenceCapable arg0, int arg1, String arg2)
    {
        return null;
    }

    public Object getTransactionalObjectId(PersistenceCapable arg0)
    {
        return null;
    }

    public Object getVersion(PersistenceCapable arg0)
    {
        return null;
    }

    public boolean isDeleted(PersistenceCapable arg0)
    {
        return false;
    }

    public boolean isDirty(PersistenceCapable arg0)
    {
        return false;
    }

    public boolean isLoaded(PersistenceCapable arg0, int arg1)
    {
        return false;
    }

    public boolean isNew(PersistenceCapable arg0)
    {
        return false;
    }

    public boolean isPersistent(PersistenceCapable arg0)
    {
        return false;
    }

    public boolean isTransactional(PersistenceCapable arg0)
    {
        return false;
    }

    public void makeDirty(PersistenceCapable arg0, String arg1)
    {
    }

    public void preSerialize(PersistenceCapable arg0)
    {
    }

    public void providedBooleanField(PersistenceCapable arg0, int arg1, boolean arg2)
    {
    }

    public void providedByteField(PersistenceCapable arg0, int arg1, byte arg2)
    {
    }

    public void providedCharField(PersistenceCapable arg0, int arg1, char arg2)
    {
    }

    public void providedDoubleField(PersistenceCapable arg0, int arg1, double arg2)
    {
    }

    public void providedFloatField(PersistenceCapable arg0, int arg1, float arg2)
    {
    }

    public void providedIntField(PersistenceCapable arg0, int arg1, int arg2)
    {
    }

    public void providedLongField(PersistenceCapable arg0, int arg1, long arg2)
    {
    }

    public void providedObjectField(PersistenceCapable arg0, int arg1, Object arg2)
    {
    }

    public void providedShortField(PersistenceCapable arg0, int arg1, short arg2)
    {
    }

    public void providedStringField(PersistenceCapable arg0, int arg1, String arg2)
    {
    }

    public boolean replacingBooleanField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchBooleanField(arg1);
    }

    public byte replacingByteField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchByteField(arg1);
    }

    public char replacingCharField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchCharField(arg1);
    }

    public Object[] replacingDetachedState(Detachable arg0, Object[] arg1)
    {
        return null;
    }

    public double replacingDoubleField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchDoubleField(arg1);
    }

    public byte replacingFlags(PersistenceCapable arg0)
    {
        return 0;
    }

    public float replacingFloatField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchFloatField(arg1);
    }

    public int replacingIntField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchIntField(arg1);
    }

    public long replacingLongField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchLongField(arg1);
    }

    public Object replacingObjectField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchObjectField(arg1);
    }

    public short replacingShortField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchShortField(arg1);
    }

    public javax.jdo.spi.StateManager replacingStateManager(PersistenceCapable arg0, javax.jdo.spi.StateManager arg1)
    {
        return null;
    }

    public String replacingStringField(PersistenceCapable arg0, int arg1)
    {
        return fm.fetchStringField(arg1);
    }

    public void setBooleanField(PersistenceCapable arg0, int arg1, boolean arg2, boolean arg3)
    {
    }

    public void setByteField(PersistenceCapable arg0, int arg1, byte arg2, byte arg3)
    {
    }

    public void setCharField(PersistenceCapable arg0, int arg1, char arg2, char arg3)
    {
    }

    public void setDoubleField(PersistenceCapable arg0, int arg1, double arg2, double arg3)
    {
    }

    public void setFloatField(PersistenceCapable arg0, int arg1, float arg2, float arg3)
    {
    }

    public void setIntField(PersistenceCapable arg0, int arg1, int arg2, int arg3)
    {
    }

    public void setLongField(PersistenceCapable arg0, int arg1, long arg2, long arg3)
    {
    }

    public void setObjectField(PersistenceCapable arg0, int arg1, Object arg2, Object arg3)
    {
    }

    public void setShortField(PersistenceCapable arg0, int arg1, short arg2, short arg3)
    {
    }

    public void setStringField(PersistenceCapable arg0, int arg1, String arg2, String arg3)
    {
    }

    void replaceFields(int[] fieldNumbers, FieldManager fm)
    {
        this.fm = fm;
        myPC.jdoReplaceFields(fieldNumbers);
    }

    public Object getObject()
    {
        return myPC;
    }

    public void disconnect()
    {
        try
        {
            // Calls to pc.jdoReplaceStateManager must be run privileged
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    myPC.jdoReplaceStateManager(null);
                    return null;
                }
            });
        }
        catch (SecurityException e)
        {
        }
    }
}