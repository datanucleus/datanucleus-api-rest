/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.api.rest.jsonobject;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.api.rest.RESTUtils;
import org.datanucleus.api.rest.RestServlet;
import org.datanucleus.api.rest.orgjson.JSONObject;
import org.datanucleus.util.ClassUtils;

/**
 * Handler to convert a JSON object to Google AppEngine Key object.
 */
public class GoogleAppEngineKeyHandler implements UserTypeJSONHandler
{
    public Object fromJSON(final JSONObject jsonobj, NucleusContext nucCtx)
    {
        ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
        Class cls = clr.classForName("com.google.appengine.api.datastore.Key");
        try
        {
            Object parent = null;
            if (jsonobj.has("parent") && !jsonobj.isNull("parent"))
            {
                //if it's a JSONObject
                JSONObject parentobj = jsonobj.getJSONObject("parent");
                parent = RESTUtils.getNonPersistableObjectFromJSONObject(parentobj, clr.classForName(jsonobj.getString("class")), nucCtx);
            }
            if (jsonobj.has("appId"))
            {
                String appId = jsonobj.getString("appId");
                String kind = jsonobj.getString("kind");
                Class keyFactory = clr.classForName("com.google.appengine.api.datastore.KeyFactory", false);
                if (parent != null)
                {
                    return ClassUtils.getMethodForClass(keyFactory, "createKey", new Class[]{cls, String.class,String.class}).invoke(null, new Object[]{parent, kind, appId});
                }

                return ClassUtils.getMethodForClass(keyFactory, "createKey", new Class[]{String.class,String.class}).invoke(null, new Object[]{kind, appId});
            }

            long id = jsonobj.getLong("id");
            String kind = jsonobj.getString("kind");
            Class keyFactory = clr.classForName("com.google.appengine.api.datastore.KeyFactory", false);
            if (parent != null)
            {
                return ClassUtils.getMethodForClass(keyFactory, "createKey", new Class[]{cls,String.class,long.class}).invoke(null, new Object[]{parent,kind,Long.valueOf(id)});
            }

            return ClassUtils.getMethodForClass(keyFactory, "createKey", new Class[]{String.class,long.class}).invoke(null, new Object[]{kind,Long.valueOf(id)});
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
