/*
 * Copyright Gert Wijns gert.wijns@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.shad.tsqb.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.shad.tsqb.data.TypeSafeQueryProxyData;
import be.shad.tsqb.helper.TypeSafeQueryHelper;
import be.shad.tsqb.hql.HqlQuery;
import be.shad.tsqb.proxy.TypeSafeQueryProxy;
import be.shad.tsqb.query.copy.CopyContext;
import be.shad.tsqb.query.copy.Copyable;
import be.shad.tsqb.selection.SelectionValueTransformer;
import be.shad.tsqb.selection.group.TypeSafeQuerySelectionGroup;
import be.shad.tsqb.selection.group.TypeSafeQuerySelectionGroupImpl;
import be.shad.tsqb.selection.parallel.ParallelSelectionMerger;
import be.shad.tsqb.selection.parallel.ParallelSelectionMerger1;
import be.shad.tsqb.selection.parallel.ParallelSelectionMerger2;
import be.shad.tsqb.selection.parallel.ParallelSelectionMerger3;
import be.shad.tsqb.selection.parallel.SelectPair;
import be.shad.tsqb.selection.parallel.SelectTriplet;
import be.shad.tsqb.selection.parallel.SelectValue;
import be.shad.tsqb.values.HqlQueryBuilderParamsImpl;
import be.shad.tsqb.values.NamedValueEnabled;
import be.shad.tsqb.values.TypeSafeValue;

/**
 * Maintains the invocationQueue, provides the entity aliases and buffers the last selected value.
 */
public class TypeSafeRootQueryImpl extends AbstractTypeSafeQuery implements TypeSafeRootQuery, TypeSafeRootQueryInternal {
    
    private List<TypeSafeQueryProxyData> invocationQueue = new LinkedList<>();
    private Map<String, TypeSafeQueryProxy> customAliasedProxies = new HashMap<>();
    private Map<String, NamedValueEnabled> aliasedValues = new HashMap<>();
    private TypeSafeValue<?> lastSelectedValue;
    private String lastInvokedProjectionPath;
    private int entityAliasCount = 1;
    private int selectionGroupAliasCount = 1;
    private int firstResult = -1;
    private int maxResults = -1;
    
    @Override
    public TypeSafeRootQuery copy() {
        return new CopyContext().get(this);
    }
    
    @Override
    public Copyable copy(CopyContext context) {
        return new TypeSafeRootQueryImpl(context, this);
    }

    /**
     * Copy constructor
     */
    protected TypeSafeRootQueryImpl(CopyContext context, TypeSafeRootQueryImpl original) {
        super(context, original);
        for(TypeSafeQueryProxyData data: original.invocationQueue) {
            invocationQueue.add(context.get(data));
        }
        for(Entry<String, NamedValueEnabled> aliasedValue: original.aliasedValues.entrySet()) {
            aliasedValues.put(aliasedValue.getKey(), context.get(aliasedValue.getValue()));
        }
        for(Entry<String, TypeSafeQueryProxy> customAliasedProxy: original.customAliasedProxies.entrySet()) {
            customAliasedProxies.put(customAliasedProxy.getKey(), context.get(customAliasedProxy.getValue()));
        }
        lastSelectedValue = context.get(original.lastSelectedValue);
        lastInvokedProjectionPath = original.lastInvokedProjectionPath;
        entityAliasCount = original.entityAliasCount;
        selectionGroupAliasCount = original.selectionGroupAliasCount;
        firstResult = original.firstResult;
        maxResults = original.maxResults;
    }

    public TypeSafeRootQueryImpl(TypeSafeQueryHelper helper) {
        super(helper);
        setRootQuery(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFirstResult() {
        return firstResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeQueryInternal getParentQuery() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queueInvokedProjectionPath(String lastInvokedProjectionPath) {
        this.lastInvokedProjectionPath = lastInvokedProjectionPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String dequeueInvokedProjectionPath() {
        String lastInvokedProjectionPath = this.lastInvokedProjectionPath;
        this.lastInvokedProjectionPath = null;
        return lastInvokedProjectionPath;
    }
    
    /**
     * {@inheritDoc}
     */
    public void invocationWasMade(TypeSafeQueryProxyData data) {
        invocationQueue.add(data);
        // reset the invoked projection path, the getter was called without reason?
        lastInvokedProjectionPath = null;
    }

    /**
     * {@inheritDoc}
     */
    public List<TypeSafeQueryProxyData> dequeueInvocations() {
        List<TypeSafeQueryProxyData> old = invocationQueue;
        invocationQueue = new LinkedList<>();
        return old;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeQueryProxyData dequeueInvocation() {
        List<TypeSafeQueryProxyData> invocations = dequeueInvocations();
        if( invocations.isEmpty() ) {
            return null;
        }
        if( invocations.size() > 1 ) {
            throw new IllegalStateException(String.format("There are %d invocations pending. Only 1 should be pending. "
                    + "The one that was used to call join(value, joinType).", invocations.size()));
        }
        return invocations.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerCustomAliasForProxy(Object value, String customAlias) {
        TypeSafeQueryProxyData queuedData = dequeueInvocation();
        if (value == null && queuedData != null) {
            value = queuedData.getProxy();
        }
        if (!(value instanceof TypeSafeQueryProxy)) {
            throw new IllegalArgumentException(String.format("Value [%s] is not a TypeSafeQueryProxy", value));
        }
        TypeSafeQueryProxy current = (TypeSafeQueryProxy) value;
        TypeSafeQueryProxy previous = customAliasedProxies.put(customAlias, current);
        if (previous != null) {
            String previousAlias = previous.getTypeSafeProxyData().getAlias();
            String currentAlias = current.getTypeSafeProxyData().getAlias();
            if (!previousAlias.equals(currentAlias)) {
                throw new IllegalArgumentException(String.format("A different proxy [%s] was already "
                        + "registered for alias [%s]. Cannot register proxy [%s]", 
                        previousAlias, customAlias, currentAlias));
            }
        }
        current.getTypeSafeProxyData().setCustomAlias(customAlias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxyByCustomEntityAlias(String alias) {
        return (T) customAliasedProxies.get(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createEntityAlias() {
        return "hobj"+ entityAliasCount++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAlias(TypeSafeValue<?> param, String alias) {
        if (param instanceof NamedValueEnabled) {
            NamedValueEnabled previous = aliasedValues.put(alias, (NamedValueEnabled) param);
            if (previous != null) {
                throw new IllegalStateException(String.format(
                        "Attempting to bind alias [%s], but it was already bound.", 
                        alias));
            }
        } else {
            throw new IllegalArgumentException("Aliasing is only allowed if the value is NamedValueEnabled");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createSelectGroupAlias() {
        return "g" + selectionGroupAliasCount++;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void selectValue(Object value) {
        getProjections().project(value, null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T select(Class<T> resultClass) {
        TypeSafeQuerySelectionGroup resultGroup = new TypeSafeQuerySelectionGroupImpl(
                createSelectGroupAlias(), resultClass, true, null);
        return helper.createTypeSafeSelectProxy(this, resultClass, resultGroup);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <T, V> V select(Class<V> transformedClass, T value, SelectionValueTransformer<T, V> transformer) {
        if (value instanceof TypeSafeQueryProxy) {
            // invocation was not added because it is not a leaf (to support method chaining).
            invocationWasMade(((TypeSafeQueryProxy) value).getTypeSafeProxyData());
        }
        getProjections().setTransformerForNextProjection(transformer);
        return helper.getDummyValue(transformedClass);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <VAL> VAL distinct(VAL value) {
        return distinct(toValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <VAL> VAL distinct(TypeSafeValue<VAL> value) {
        return function().distinct(value).select();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T queueValueSelected(TypeSafeValue<T> value) {
        lastSelectedValue = value;
        return helper.getDummyValue(value.getValueClass());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeValue<?> dequeueSelectedValue() {
        TypeSafeValue<?> value = lastSelectedValue;
        lastSelectedValue = null;
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, SUB> SUB selectParallel(T resultDto, Class<SUB> subselectClass, ParallelSelectionMerger<T, SUB> merger) {
        return getHelper().createTypeSafeSelectProxy(this, subselectClass, new TypeSafeQuerySelectionGroupImpl(
                createSelectGroupAlias(), subselectClass, false, merger));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T, A> SelectValue<A> selectParallel(T resultDto, ParallelSelectionMerger1<T, A> merger) {
        return selectParallel(resultDto, SelectValue.class, (ParallelSelectionMerger) merger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T, A, B> SelectPair<A, B> selectParallel(T resultDto, ParallelSelectionMerger2<T, A, B> merger) {
        return selectParallel(resultDto, SelectPair.class, (ParallelSelectionMerger) merger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T, A, B, C> SelectTriplet<A, B, C> selectParallel(T resultDto, ParallelSelectionMerger3<T, A, B, C> merger) {
        return selectParallel(resultDto, SelectTriplet.class, (ParallelSelectionMerger) merger);
    }
    
    @Override
    public HqlQuery toHqlQuery() {
        return super.toHqlQuery(new HqlQueryBuilderParamsImpl());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void namedValue(String alias, Object value) {
        NamedValueEnabled param = aliasedValues.get(alias);
        if (param == null) {
            throw new IllegalArgumentException(String.format(
                    "Attempting to set value for parameter with alias [%s]. "
                    + "But no parameter exists with this alias.", alias));
        }
        ((NamedValueEnabled) param).setNamedValue(value);
    }

}
