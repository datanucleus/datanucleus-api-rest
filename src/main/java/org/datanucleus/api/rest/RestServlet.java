/**********************************************************************
Copyright (c) 2009 Erik Bengtson and others. All rights reserved. 
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
2013-2015 Andy Jefferson - updated to use persistence.xml, GZIP, JDOQL, JPQL, FetchGroups
    ...
 **********************************************************************/
package org.datanucleus.api.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import javax.jdo.JDOException;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.api.rest.orgjson.JSONArray;
import org.datanucleus.api.rest.orgjson.JSONException;
import org.datanucleus.api.rest.orgjson.JSONObject;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.util.NucleusLogger;

/**
 * This servlet exposes persistent class via RESTful HTTP requests.
 * Supports the following
 * <ul>
 * <li>GET (retrieve/query), supporting GZIP compression on the response</li>
 * <li>POST (update/insert)</li>
 * <li>PUT (update/insert)</li>
 * <li>DELETE (delete)</li>
 * <li>HEAD (validate)</li>
 * </ul>
 */
public class RestServlet extends HttpServlet
{
    private static final long serialVersionUID = -4445182084242929362L;

    public static final NucleusLogger LOGGER_REST = NucleusLogger.getLoggerInstance("DataNucleus.REST");

    PersistenceManagerFactory pmf;
    PersistenceNucleusContext nucCtx;

    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#destroy()
     */
    public void destroy()
    {
        if (pmf != null && !pmf.isClosed())
        {
            LOGGER_REST.info("REST : Closing PMF");
            pmf.close();
        }
        super.destroy();
    }

    public void init(ServletConfig config) throws ServletException
    {
        String factory = config.getInitParameter("persistence-context");
        if (factory == null)
        {
            throw new ServletException("You haven't specified \"persistence-context\" property defining the persistence unit");
        }

        try
        {
            LOGGER_REST.info("REST : Creating PMF for factory=" + factory);
            pmf = JDOHelper.getPersistenceManagerFactory(factory);
            this.nucCtx = ((JDOPersistenceManagerFactory)pmf).getNucleusContext();
        }
        catch (Exception e)
        {
            LOGGER_REST.error("Exception creating PMF", e);
            throw new ServletException("Could not create internal PMF. See nested exception for details", e);
        }

        super.init(config);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        // Retrieve any fetch group specification that need applying to the fetch
        String fetchGroup = req.getParameter("fetchGroup");
        if (fetchGroup == null)
        {
            fetchGroup = req.getParameter("fetch");
        }
        String maxFetchDepthStr = req.getParameter("maxFetchDepth");
        Integer maxFetchDepth = null;
        if (maxFetchDepthStr != null)
        {
            maxFetchDepth = Integer.valueOf(maxFetchDepthStr);
        }
        boolean compress = requestAllowsGZIPCompression(req);

        try
        {
            String token = getNextTokenAfterSlash(req);
            if (token.equalsIgnoreCase("jdoql"))
            {
                // GET "/jdoql?query=the_query_details" where "the_query_details" is (encoded) "SELECT FROM ... WHERE ... ORDER BY ..."
                String jdoqlStr = req.getParameter("query");
                if (jdoqlStr == null)
                {
                    // No query provided
                    JSONObject error = new JSONObject();
                    error.put("exception", "If using '/jdoql' GET request, must provide query parameter with encoded form of JDOQL query");
                    resp.getWriter().write(error.toString());
                    resp.setStatus(404);
                    resp.setHeader("Content-Type", "application/json");
                    return;
                }

                PersistenceManager pm = pmf.getPersistenceManager();
                try
                {
                    pm.currentTransaction().begin();

                    String queryString = URLDecoder.decode(jdoqlStr, "UTF-8");
                    Query query = pm.newQuery("JDOQL", queryString);
                    if (fetchGroup != null)
                    {
                        query.getFetchPlan().addGroup(fetchGroup);
                    }
                    if (maxFetchDepth != null)
                    {
                        query.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
                    }
                    Object result = query.execute();
                    if (result instanceof Collection)
                    {
                        JSONArray jsonobj = RESTUtils.getJSONArrayFromCollection((Collection)result, ((JDOPersistenceManager)pm).getExecutionContext());
                        writeResponse(resp, jsonobj.toString(), compress);
                    }
                    else
                    {
                        JSONObject jsonobj = RESTUtils.getJSONObjectFromPOJO(result, ((JDOPersistenceManager)pm).getExecutionContext());
                        writeResponse(resp, jsonobj.toString(), compress);
                    }
                    resp.setHeader("Content-Type", "application/json");
                    resp.setStatus(200);

                    pm.currentTransaction().commit();
                }
                finally
                {
                    if (pm.currentTransaction().isActive())
                    {
                        pm.currentTransaction().rollback();
                    }
                    pm.close();
                }
                return;
            }
            else if (token.equalsIgnoreCase("jpql"))
            {
                // GET "/jpql?query=the_query_details" where "the_query_details" is (encoded) "SELECT p FROM ... p WHERE ... ORDER BY ..."
                String jpqlStr = req.getParameter("query");
                if (jpqlStr == null)
                {
                    // No query provided
                    JSONObject error = new JSONObject();
                    error.put("exception", "If using '/jpql' GET request, must provide query parameter with encoded form of JPQL query");
                    resp.getWriter().write(error.toString());
                    resp.setStatus(404);
                    resp.setHeader("Content-Type", "application/json");
                    return;
                }

                // GET "/jpql?the_query_details" where "the_query_details" is "SELECT ... FROM ... WHERE ... ORDER BY ..."
                PersistenceManager pm = pmf.getPersistenceManager();
                try
                {
                    pm.currentTransaction().begin();

                    String queryString = URLDecoder.decode(jpqlStr, "UTF-8");
                    Query query = pm.newQuery("JPQL", queryString);
                    if (fetchGroup != null)
                    {
                        query.getFetchPlan().addGroup(fetchGroup);
                    }
                    if (maxFetchDepth != null)
                    {
                        query.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
                    }
                    Object result = query.execute();
                    if (result instanceof Collection)
                    {
                        JSONArray jsonobj = RESTUtils.getJSONArrayFromCollection((Collection)result, ((JDOPersistenceManager)pm).getExecutionContext());
                        writeResponse(resp, jsonobj.toString(), compress);
                    }
                    else
                    {
                        JSONObject jsonobj = RESTUtils.getJSONObjectFromPOJO(result, ((JDOPersistenceManager)pm).getExecutionContext());
                        writeResponse(resp, jsonobj.toString(), compress);
                    }
                    resp.setHeader("Content-Type", "application/json");
                    resp.setStatus(200);

                    pm.currentTransaction().commit();
                }
                finally
                {
                    if (pm.currentTransaction().isActive())
                    {
                        pm.currentTransaction().rollback();
                    }
                    pm.close();
                }
                return;
            }
            else if (token.equalsIgnoreCase("query"))
            {
                // GET "/query no longer supported
                JSONObject error = new JSONObject();
                error.put("exception", "If using '/query' GET request is not supported. Use '/jdoql' or '/jpql'");
                resp.getWriter().write(error.toString());
                resp.setStatus(404);
                resp.setHeader("Content-Type", "application/json");
                return;
            }
            else
            {
                // GET "/{candidateclass}..."
                String className = token;
                ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
                AbstractClassMetaData cmd = nucCtx.getMetaDataManager().getMetaDataForEntityName(className);
                try
                {
                    if (cmd == null)
                    {
                        cmd = nucCtx.getMetaDataManager().getMetaDataForClass(className, clr);
                    }
                }
                catch (ClassNotResolvedException ex)
                {
                    JSONObject error = new JSONObject();
                    error.put("exception", ex.getMessage());
                    resp.getWriter().write(error.toString());
                    resp.setStatus(404);
                    resp.setHeader("Content-Type", "application/json");
                    return;
                }

                Object id = getId(req);
                if (id == null)
                {
                    // GET "/{candidateclass}[?filter={the_filter}]" where "the_filter" is (encoded) "paramX == val1 && paramY == val2 ..."
                    try
                    {
                        PersistenceManager pm = pmf.getPersistenceManager();
                        if (fetchGroup != null)
                        {
                            pm.getFetchPlan().addGroup(fetchGroup);
                        }
                        if (maxFetchDepth != null)
                        {
                            pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
                        }

                        try
                        {
                            pm.currentTransaction().begin();

                            // get the whole extent for this candidate
                            String jdoqlStr = "SELECT FROM " + cmd.getFullClassName();
                            String filterStr = req.getParameter("filter");
                            if (filterStr != null)
                            {
                                // Optional filter
                                jdoqlStr += " WHERE " + URLDecoder.decode(filterStr, "UTF-8");
                            }

                            Query query = pm.newQuery("JDOQL", jdoqlStr);
                            List result = (List)query.execute();
                            JSONArray jsonobj = RESTUtils.getJSONArrayFromCollection(result, ((JDOPersistenceManager)pm).getExecutionContext());
                            writeResponse(resp, jsonobj.toString(), compress);
                            resp.setHeader("Content-Type", "application/json");
                            resp.setStatus(200);

                            pm.currentTransaction().commit();
                        }
                        finally
                        {
                            if (pm.currentTransaction().isActive())
                            {
                                pm.currentTransaction().rollback();
                            }
                            pm.close();
                        }
                        return;
                    }
                    catch (JDOUserException e)
                    {
                        JSONObject error = new JSONObject();
                        error.put("exception", e.getMessage());
                        resp.getWriter().write(error.toString());
                        resp.setStatus(400);
                        resp.setHeader("Content-Type", "application/json");
                        return;
                    }
                    catch (JDOException ex)
                    {
                        JSONObject error = new JSONObject();
                        error.put("exception", ex.getMessage());
                        resp.getWriter().write(error.toString());
                        resp.setStatus(404);
                        resp.setHeader("Content-Type", "application/json");
                        return;
                    }
                    catch (RuntimeException ex)
                    {
                        // errors from the google appengine may be raised when running queries TODO Remove appengine specific stuff, this should be general
                        JSONObject error = new JSONObject();
                        error.put("exception", ex.getMessage());
                        resp.getWriter().write(error.toString());
                        resp.setStatus(404);
                        resp.setHeader("Content-Type", "application/json");
                        return;
                    }
                }

                // GET "/{candidateclass}/id" - Find object by id
                PersistenceManager pm = pmf.getPersistenceManager();
                if (fetchGroup != null)
                {
                    pm.getFetchPlan().addGroup(fetchGroup);
                }
                if (maxFetchDepth != null)
                {
                    pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
                }

                try
                {
                    pm.currentTransaction().begin();
                    Object result = pm.getObjectById(id);
                    pm.retrieve(result); // Make sure all fields in FetchPlan are loaded before converting to JSON

                    JSONObject jsonobj = RESTUtils.getJSONObjectFromPOJO(result, ((JDOPersistenceManager)pm).getExecutionContext());
                    writeResponse(resp, jsonobj.toString(), compress);
                    resp.setHeader("Content-Type","application/json");
                    pm.currentTransaction().commit();
                    return;
                }
                catch (JDOObjectNotFoundException ex)
                {
                    resp.setContentLength(0);
                    resp.setStatus(404);
                    return;
                }
                catch (JDOException ex)
                {
                    JSONObject error = new JSONObject();
                    error.put("exception", ex.getMessage());
                    resp.getWriter().write(error.toString());
                    resp.setStatus(404);
                    resp.setHeader("Content-Type", "application/json");
                    return;
                }
                finally
                {
                    if (pm.currentTransaction().isActive())
                    {
                        pm.currentTransaction().rollback();
                    }
                    pm.close();
                }
            }
        }
        catch (JSONException e)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", e.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(404);
                resp.setHeader("Content-Type", "application/json");
            }
            catch (JSONException e1)
            {
                // ignore
            }
        }
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        doPost(req, resp);
    }

    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.addHeader("Allow", " GET, HEAD, POST, PUT, TRACE, OPTIONS");
        resp.setContentLength(0);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
    throws ServletException, IOException
    {
        if (req.getContentLength() < 1)
        {
            resp.setContentLength(0);
            resp.setStatus(400);// bad request
            return;
        }

        char[] buffer = new char[req.getContentLength()];
        req.getReader().read(buffer);
        String str = new String(buffer);
        JSONObject jsonobj;
        PersistenceManager pm = pmf.getPersistenceManager();
        ExecutionContext ec = ((JDOPersistenceManager)pm).getExecutionContext();
        try
        {
            pm.currentTransaction().begin();
            jsonobj = new JSONObject(str);
            String className = getNextTokenAfterSlash(req);
            jsonobj.put("class", className);

            // Process any id info provided in the URL
            AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(className, ec.getClassLoaderResolver());
            String path = req.getRequestURI().substring(req.getContextPath().length() + req.getServletPath().length());
            StringTokenizer tokenizer = new StringTokenizer(path, "/");
            tokenizer.nextToken(); // className
            if (tokenizer.hasMoreTokens())
            {
                String idToken = tokenizer.nextToken();
                Object id = RESTUtils.getIdentityForURLToken(cmd, idToken, nucCtx);
                if (id != null)
                {
                    if (cmd.getIdentityType() == IdentityType.APPLICATION)
                    {
                        if (cmd.usesSingleFieldIdentityClass())
                        {
                            jsonobj.put(cmd.getPrimaryKeyMemberNames()[0], IdentityUtils.getTargetKeyForSingleFieldIdentity(id));
                        }
                    }
                    else if (cmd.getIdentityType() == IdentityType.DATASTORE)
                    {
                        jsonobj.put("_id", IdentityUtils.getTargetKeyForDatastoreIdentity(id));
                    }
                }
            }

            // Convert to an object from JSON
            Object pc = RESTUtils.getObjectFromJSONObject(jsonobj, className, ec);

            // Persist
            Object obj = pm.makePersistent(pc);

            // Return as JSON
            JSONObject jsonobj2 = RESTUtils.getJSONObjectFromPOJO(obj, ec);
            resp.getWriter().write(jsonobj2.toString());
            resp.setHeader("Content-Type", "application/json");
            pm.currentTransaction().commit();
        }
        catch (ClassNotResolvedException e)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", e.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(500);
                resp.setHeader("Content-Type", "application/json");
                LOGGER_REST.error(e.getMessage(), e);
            }
            catch (JSONException e1)
            {
                throw new RuntimeException(e1);
            }
        }
        catch (JDOUserException e)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", e.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(400);
                resp.setHeader("Content-Type", "application/json");
                LOGGER_REST.error(e.getMessage(), e);
            }
            catch (JSONException e1)
            {
                throw new RuntimeException(e1);
            }
        }
        catch (JDOException e)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", e.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(500);
                resp.setHeader("Content-Type", "application/json");
                LOGGER_REST.error(e.getMessage(), e);
            }
            catch (JSONException e1)
            {
                throw new RuntimeException(e1);
            }
        }
        catch (JSONException e)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", e.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(500);
                resp.setHeader("Content-Type", "application/json");
                LOGGER_REST.error(e.getMessage(), e);
            }
            catch (JSONException e1)
            {
                throw new RuntimeException(e1);
            }
        }
        finally
        {
            if (pm.currentTransaction().isActive())
            {
                pm.currentTransaction().rollback();
            }
            pm.close();
        }
        resp.setStatus(201);// created
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) 
    throws ServletException, IOException
    {
        PersistenceManager pm = pmf.getPersistenceManager();
        try
        {
            String className = getNextTokenAfterSlash(req);
            ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
            AbstractClassMetaData cmd = nucCtx.getMetaDataManager().getMetaDataForEntityName(className);
            try
            {
                if (cmd == null)
                {
                    cmd = nucCtx.getMetaDataManager().getMetaDataForClass(className, clr);
                }
            }
            catch (ClassNotResolvedException ex)
            {
                try
                {
                    JSONObject error = new JSONObject();
                    error.put("exception", ex.getMessage());
                    resp.getWriter().write(error.toString());
                    resp.setStatus(404);
                    resp.setHeader("Content-Type", "application/json");
                }
                catch (JSONException e)
                {
                    // will not happen
                }
                return;
            }

            Object id = getId(req);
            if (id == null)
            {
                // Delete all objects of this type
                pm.currentTransaction().begin();
                Query q = pm.newQuery("SELECT FROM " + cmd.getFullClassName());
                q.deletePersistentAll();
                pm.currentTransaction().commit();
            }
            else
            {
                // Delete the object with the supplied id
                pm.currentTransaction().begin();
                Object obj = pm.getObjectById(id);
                pm.deletePersistent(obj);
                pm.currentTransaction().commit();
            }
        }
        catch (JDOObjectNotFoundException ex)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", ex.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(400);
                resp.setHeader("Content-Type", "application/json");
                LOGGER_REST.error("DELETE returned that object didn't exist : " + ex.getMessage(), ex);
                return;
            }
            catch (JSONException e)
            {
                // will not happen
            }
        }
        catch (JDOUserException e)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", e.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(400);
                resp.setHeader("Content-Type", "application/json");
                return;
            }
            catch (JSONException e1)
            {
                // ignore
            }
        }
        catch (JDOException e)
        {
            try
            {
                JSONObject error = new JSONObject();
                error.put("exception", e.getMessage());
                resp.getWriter().write(error.toString());
                resp.setStatus(500);
                resp.setHeader("Content-Type", "application/json");
                LOGGER_REST.error("Exception on attempted DELETE : " + e.getMessage(), e);
            }
            catch (JSONException e1)
            {
                // ignore
            }
        }
        finally
        {
            if (pm.currentTransaction().isActive())
            {
                pm.currentTransaction().rollback();
            }
            pm.close();
        }
        resp.setContentLength(0);
        resp.setStatus(204);// created
    }

    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String className = getNextTokenAfterSlash(req);
        ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
        AbstractClassMetaData cmd = nucCtx.getMetaDataManager().getMetaDataForEntityName(className);
        try
        {
            if (cmd == null)
            {
                cmd = nucCtx.getMetaDataManager().getMetaDataForClass(className, clr);
            }
        }
        catch (ClassNotResolvedException ex)
        {
            resp.setStatus(404);
            return;
        }

        Object id = getId(req);
        if (id == null)
        {
            // no id provided!
            try
            {
                // get the whole extent
                String queryString = "SELECT FROM " + cmd.getFullClassName();

                String rawQuery = req.getQueryString();
                if (rawQuery != null)
                {
                    // Query by filter
                    if (rawQuery.indexOf('&') >= 0)
                    {
                        queryString += " WHERE " + URLDecoder.decode(rawQuery.substring(0, rawQuery.indexOf('&')), "UTF-8");
                    }
                    else
                    {
                        queryString += " WHERE " + URLDecoder.decode(rawQuery, "UTF-8");
                    }
                }

                PersistenceManager pm = pmf.getPersistenceManager();
                try
                {
                    pm.currentTransaction().begin();
                    Query query = pm.newQuery("JDOQL", queryString);
                    query.execute();
                    resp.setStatus(200);
                    pm.currentTransaction().commit();
                }
                finally
                {
                    if (pm.currentTransaction().isActive())
                    {
                        pm.currentTransaction().rollback();
                    }
                    pm.close();
                }
                return;
            }
            catch (JDOUserException e)
            {
                resp.setStatus(400);
                return;
            }
            catch (JDOException ex)
            {
                resp.setStatus(404);
                return;
            }
            catch (RuntimeException ex)
            {
                resp.setStatus(404);
                return;
            }
        }

        PersistenceManager pm = pmf.getPersistenceManager();
        try
        {
            pm.currentTransaction().begin();
            pm.getObjectById(id);
            resp.setStatus(200);
            pm.currentTransaction().commit();
            return;
        }
        catch (JDOException ex)
        {
            resp.setStatus(404);
            return;
        }
        finally
        {
            if (pm.currentTransaction().isActive())
            {
                pm.currentTransaction().rollback();
            }
            pm.close();
        }
    }

    /**
     * Convenience accessor to get the persistable id, following a "/" or in the content of the request.
     * @param req The request
     * @return The id (or null if not available)
     */
    private Object getId(HttpServletRequest req)
    {
        ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(RestServlet.class.getClassLoader());
        String path = req.getRequestURI().substring(req.getContextPath().length() + req.getServletPath().length());
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        String className = tokenizer.nextToken();
        AbstractClassMetaData cmd = nucCtx.getMetaDataManager().getMetaDataForClass(className, clr);

        String id = null;
        if (tokenizer.hasMoreTokens())
        {
            // "id" single-field specified in URL
            id = tokenizer.nextToken();
            if (id == null || cmd == null)
            {
                return null;
            }

            Object identity = RESTUtils.getIdentityForURLToken(cmd, id, nucCtx);
            if (identity != null)
            {
                return identity;
            }
        }

        // "id" must have been specified in the content of the request
        try
        {
            if (id == null && req.getContentLength() > 0)
            {
                char[] buffer = new char[req.getContentLength()];
                req.getReader().read(buffer);
                id = new String(buffer);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if (id == null || cmd == null)
        {
            return null;
        }

        try
        {
            // assume it's a JSONObject
            id = URLDecoder.decode(id, "UTF-8");
            JSONObject jsonobj = new JSONObject(id);
            return RESTUtils.getNonPersistableObjectFromJSONObject(jsonobj, clr.classForName(cmd.getObjectidClass()), nucCtx);
        }
        catch (JSONException ex)
        {
            // not JSON syntax
        }
        catch (UnsupportedEncodingException e)
        {
            LOGGER_REST.error("Exception caught when trying to determine id", e);
        }

        return id;
    }

    /**
     * Accessor for whether the servlet request allows (GZIP) compression.
     * @param req The request
     * @return Whether we can do GZIP compression
     */
    private boolean requestAllowsGZIPCompression(HttpServletRequest req)
    {
        String encodings = req.getHeader("Accept-Encoding");
        return ((encodings != null) && (encodings.indexOf("gzip") > -1));
    }

    /**
     * Method to write the response, using (GZIP) compression if available.
     * @param resp The response
     * @param s The message to write
     * @param useCompression Whether to use compression
     * @throws IOException If an error occurs
     */
    private void writeResponse(HttpServletResponse resp, String s, boolean useCompression) throws IOException
    {
        if (useCompression && s.length() > 3000)
        {
            resp.setHeader("Content-Encoding", "gzip");
            OutputStream o = resp.getOutputStream();
            GZIPOutputStream gz = new GZIPOutputStream(o);
            gz.write(s.getBytes());
            gz.flush();
            gz.close();
            o.close();
        }
        else
        {
            // Just write normally
            resp.getWriter().write(s);
        }
    }

    /**
     * Convenience method to get the next token after a "/".
     * @param req The request
     * @return The next token
     */
    private String getNextTokenAfterSlash(HttpServletRequest req)
    {
        String path = req.getRequestURI().substring(req.getContextPath().length() + req.getServletPath().length());
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        return tokenizer.nextToken();
    }
}