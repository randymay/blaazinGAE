package com.blaazinsoftware.centaur.data.dao;

import com.blaazinsoftware.centaur.search.ListResults;
import com.blaazinsoftware.centaur.search.QuerySearchOptions;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Randy May
 *         Date: 15-09-28
 */
public class BasicDAO {
    public <T> long saveForId(T entity) {
        return ofy().save().entity(entity).now().getId();
    }

    public <T> String saveForKey(T entity) {
        return ofy().save().entity(entity).now().getString();
    }

    public <T> void delete(T entity) {
        ofy().delete().entity(entity).now();
    }

    public <T> void delete(String keyString) {
        Key<T> key = getKey(keyString);
        ofy().delete().key(key).now();
    }

    public <T> Map<Key<T>, T> saveAll(List<T> entities) {
        return ofy().save().entities(entities).now();
    }

    public <T> T load(long id, Class<T> entityClass) {
        return ofy().load().type(entityClass).id(id).now();
    }

    public <T> T load(String keyString) {
        Key<T> key = getKey(keyString);
        return ofy().load().key(key).now();
    }

    private <T> Key<T> getKey(String keyString) {
        return Key.create(keyString);
    }

    public <T> void cacheEntity(T entity) {
        ofy().cache(true).save().entity(entity).now();
    }

    public <T> T loadFromCache(String keyString) {
        Key<T> key = getKey(keyString);
        return ofy().cache(true).load().key(key).now();
    }

    public <T> Map<Long, T> loadByIds(List<Long> ids, Class<T> entityClass) {
        return ofy().load().type(entityClass).ids(ids);
    }

    public <T> Map<String, T> loadByKeys(List<String> keys, Class<T> entityClass) {
        return ofy().load().type(entityClass).ids(keys);
    }

    public <T> T loadByGroup(long id, Class<T> entityClass, Class<?>... groupClass) {
        return ofy().load().group(groupClass).type(entityClass).id(id).now();
    }

    public <T> T loadByGroup(Long id, Class<T> entityClass, Class<?>... groupClass) {
        return ofy().load().group(groupClass).type(entityClass).id(id).now();
    }

    /*public <T> List<T> findEntitiesByFilter(Class<T> entityClass, String fieldName, Object filterObject) {
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put(fieldName, filterObject);
        return findEntitiesByFilter(entityClass, filterMap);
    }

    public <T> List<T> findEntitiesByFilter(Class<T> entityClass, Map<String, Object> filterMap) {
        Query<T> query = ofy().load().type(entityClass);
        for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
            query = query.filter(entry.getKey(), entry.getValue());
        }
        return query.list();
    }

    public <T> List<T> findEntities(QuerySearchOptions<T> searchOptions) {
        Query<T> query = ofy().load().type(searchOptions.getReturnType());
        for (Map.Entry<String, Object> entry : searchOptions.getFilter().entrySet()) {
            query = query.filter(entry.getKey(), entry.getValue());
        }
        query.offset(searchOptions.getOffset());
        if (searchOptions.getLimit() > 0) {
            query.limit(searchOptions.getLimit());
        }
        if (null != searchOptions.getClass()) {
            query.endAt(searchOptions.getCursor());
        }
        if (StringUtils.isNotEmpty(searchOptions.getOrderByField())) {
            query.order(searchOptions.getOrderByField());
            query.orderKey(searchOptions.isDescending());
        }
        return query.list();
    }*/

    public <T, P> T loadChild(Long id, Class<T> entityClass, P parentClass) {
        return ofy().load().type(entityClass).parent(parentClass).id(id).now();
    }

    public <T, P> T loadChild(String key, Class<T> entityClass, P parentClass) {
        return ofy().load().type(entityClass).parent(parentClass).id(key).now();
    }

    public <T, P> List<T> loadChildren(Class<T> entityClass, P parent) {
        return ofy().load().type(entityClass).ancestor(parent).list();
    }

    public <T, P> List<T> findChildrenByFilter(Class<T> entityClass, P parentClass, String fieldName, Object filterObject) {
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put(fieldName, filterObject);
        return findChildrenByFilter(entityClass, parentClass, filterMap);
    }

    public <T, P> List<T> findChildrenByFilter(Class<T> entityClass, P parentClass, Map<String, Object> filterMap) {
        Query<T> query = ofy().load().type(entityClass).ancestor(parentClass);
        for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
            query = query.filter(entry.getKey(), entry.getValue());
        }
        return query.list();
    }

    public <T, P> T loadFirstChild(Class<T> entityClass, P parentClass) {
        List<T> results = loadChildren(entityClass, parentClass);

        if (null == results || results.size() == 0) {
            return null;
        }

        return results.get(0);
    }

    public <T> T executeWorkInTransaction(Work<T> work) {
        return ofy().transact(work);
    }

    public <T> ListResults<T> getPagedList(QuerySearchOptions<T> searchOptions) {

        Query<T> query = ofy().load().type(searchOptions.getReturnType());

        // Apply Filter
        if (null != searchOptions.getFilter()) {
            query = query.filter(searchOptions.getFilter());
        }

        // Set Order By Field
        if (StringUtils.isNotEmpty(searchOptions.getOrderByField())) {
            String order = searchOptions.getOrderByField();
            if (!searchOptions.isDescending()){
                order = "-" + order;
            }
            query = query.order(order);
        }

        // Apply Cursor
        final Cursor cursor = searchOptions.getCursor();
        if (cursor != null) {
            query = query.startAt(cursor);
        } else {
            // Apply Offset
            if (searchOptions.getOffset() > 0) {
                query = query.offset(searchOptions.getOffset());
            }
        }
        // Apply Limit
        if (searchOptions.getLimit() > 0) {
            query = query.limit(searchOptions.getLimit());
        }

        ListResults<T> results = new ListResults<>();

        QueryResultIterator<T> iterator = query.iterator();

        while (iterator.hasNext()) {
            results.getResults().add(iterator.next());
        }

        results.setCountReturned(results.getResults().size());
        if (results.getCountReturned() >= searchOptions.getLimit()) {
            // Only return the cursor if more records are available
            results.setCursor(iterator.getCursor());
        }

        // Execute a second query to determine the total number of records in this query
        Query<T> countQuery = ofy().load().type(searchOptions.getReturnType());
        if (null != searchOptions.getFilter()) {
            countQuery = countQuery.filter(searchOptions.getFilter());
        }
        results.setCountFound(countQuery.count());

        return results;
    }
}