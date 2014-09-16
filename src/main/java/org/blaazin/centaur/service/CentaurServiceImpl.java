package org.blaazin.centaur.service;

import org.blaazin.centaur.CentaurException;
import org.blaazin.centaur.data.dto.CentaurEntity;
import org.blaazin.centaur.data.util.EntityTranslator;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CentaurServiceImpl implements CentaurService {

    @Autowired
    private CentaurDAO dao;

    @Override
    public <T extends CentaurEntity> Key save(T object) throws CentaurException {
        return save(object, null);
    }

    @Override
    public <T extends CentaurEntity> Key save(T object, Transaction transaction) throws CentaurException {
        if (null == object.getAppEngineKey()) {
            initKey(object);
        }
        Key key = dao.save(transaction, new EntityTranslator().toEntity(object));
        if (null == object.getAppEngineKey()) {
            object.setAppEngineKey(key);
        }

        return key;
    }

    private <T extends CentaurEntity> void initKey(T object) throws CentaurException {
        if (null == object.getAppEngineKey()) {
            Key key = createKey(object);
            object.setAppEngineKey(key);
        }
    }

    @Override
    public <T extends CentaurEntity, X extends CentaurEntity> Key saveChild(X parent, T object) throws CentaurException {
        Key childKey = this.saveChild(parent, object, null);
        if (null == object.getAppEngineKey()) {
            object.setAppEngineKey(childKey);
        }

        return childKey;
    }

    @Override
    public <T extends CentaurEntity, X extends CentaurEntity> Key saveChild(X parent, T object, Transaction transaction) throws CentaurException {
        if (null == parent || null == parent.getAppEngineKey()) {
            throw new CentaurException("Parent Entity is required");
        }

        if (null == object.getAppEngineKey()) {
            Key key = createKey(parent, object);
            object.setAppEngineKey(key);
            if (null == key) {
                return dao.save(transaction, new EntityTranslator().toEntity(object, parent.getAppEngineKey()));
            }
        }
        return dao.save(transaction, new EntityTranslator().toEntity(object));
    }

    @Override
    public <T extends CentaurEntity> T getObject(Key key, Class<T> klass) throws CentaurException {
        return new EntityTranslator().fromEntity(dao.getByKey(key), klass);
    }

    @Override
    public <T extends CentaurEntity> T getObject(String kind, String name, Class<T> klass) throws CentaurException {
        Key key = KeyFactory.createKey(kind, name);

        return new EntityTranslator().fromEntity(dao.getByKey(key), klass);
    }

    @Override
    public <T extends CentaurEntity> T getObject(String kind, long id, Class<T> klass) throws CentaurException {
        Key key = KeyFactory.createKey(kind, id);

        return new EntityTranslator().fromEntity(dao.getByKey(key), klass);
    }

    @Override
    public <T extends CentaurEntity> T getObject(String name, Class<T> klass) throws CentaurException {
        Key key = KeyFactory.createKey(klass.getSimpleName(), name);

        return new EntityTranslator().fromEntity(dao.getByKey(key), klass);
    }

    @Override
    public <T extends CentaurEntity> T getObject(String propertyName, Object value, Class<T> klass) throws CentaurException {
        return this.getObject(klass.getSimpleName(), propertyName, value, klass);
    }

    @Override
    public <T extends CentaurEntity> T getObjectByUserId(String userId, Class<T> klass) throws CentaurException {
        return getObject(klass.getSimpleName(), "userId", userId, klass);
    }

    @Override
    public <T extends CentaurEntity> T getObjectByUserId(String kind, String userId, Class<T> klass) throws CentaurException {
        return getObject(kind, "userId", userId, klass);
    }

    @Override
    public <T extends CentaurEntity> T getObject(String kind, String propertyName, Object value, Class<T> klass) throws CentaurException {
        Entity entity = dao.getSingleEntityByPropertyValue(kind, propertyName, value);
        if (entity == null) {
            return null;
        }

        return new EntityTranslator().fromEntity(entity, klass);
    }

    @Override
    public <T extends CentaurEntity> void deleteObject(T object) throws CentaurException {
        this.deleteObject(object, null);
    }

    @Override
    public <T extends CentaurEntity> void deleteObject(T object, Transaction transaction) throws CentaurException {
        dao.delete(transaction, new EntityTranslator().toEntity(object));
    }

    @Override
    public <T extends CentaurEntity> Key createKey(T object) throws CentaurException {
        if (object != null && !StringUtils.isEmpty(object.getKind()) && !StringUtils.isEmpty(object.getName())) {
            return KeyFactory.createKey(object.getKind(), object.getName());
        }

        return null;
    }

    @Override
    public <T extends CentaurEntity, X extends CentaurEntity> Key createKey(X parent, T object) throws CentaurException {
        if (object != null && !StringUtils.isEmpty(object.getKind()) && !StringUtils.isEmpty(object.getName())) {
            Entity parentEntity = new EntityTranslator().toEntity(parent);
            return KeyFactory.createKey(parentEntity.getKey(), object.getKind(), object.getName());
        }

        return null;
    }

    @Override
    public <T extends CentaurEntity, X extends CentaurEntity> List<T> getChildren(String kind, X parent, Class<T> klass) throws CentaurException {
        Entity parentEntity = new EntityTranslator().toEntity(parent);
        List<Entity> entities = dao.getChildren(kind, parentEntity);

        List<T> results = new ArrayList<>();
        if (null == entities) {
            return null;
        }

        for (Entity entity : entities) {
            T object = new EntityTranslator().fromEntity(entity, klass);
            results.add(object);
        }

        return results;
    }

    @Override
    public <T extends CentaurEntity> List<T> getObjects(String kind, Class<T> klass) throws CentaurException {
        List<T> entityList = new ArrayList<>();
        List<Entity> entities = dao.getEntitiesByKind(kind);
        if (entities == null) {
            return null;
        }

        for (Entity entity : entities) {
            T object = new EntityTranslator().fromEntity(entity, klass);
            entityList.add(object);
        }

        return entityList;
    }

    @Override
    public <T extends CentaurEntity> List<T> getObjects(String kind, String propertyName, Object value, Class<T> klass) throws CentaurException {
        List<Entity> entities = dao.getEntitiesByPropertyValue(kind, propertyName, value);
        return getObjectListFromEntities(klass, entities);
    }

    private <T extends CentaurEntity> List<T> getObjectListFromEntities(Class<T> klass, List<Entity> entities) {
        if (entities == null) {
            return null;
        }

        List<T> objectList = new ArrayList<>();
        for (Entity entity : entities) {
            T object = new EntityTranslator().fromEntity(entity, klass);
            objectList.add(object);
        }

        return objectList;
    }

    @Override
    public <T extends CentaurEntity> List<T> getObjects(String kind, Map<String, Object> keyValues, Class<T> klass) throws CentaurException {
        List<Entity> entities = dao.getEntitiesByPropertyValues(kind, keyValues);
        return getObjectListFromEntities(klass, entities);
    }

    public Transaction beginTransaction() {
        return dao.beginTransaction();
    }

    public void rollback(Transaction transaction) {
        dao.rollbackTransaction(transaction);
    }

    public void commit(Transaction transaction) {
        dao.commitTransaction(transaction);
    }

    protected void setDao(CentaurDAO dao) {
        this.dao = dao;
    }
}
