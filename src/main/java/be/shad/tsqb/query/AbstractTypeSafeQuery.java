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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import be.shad.tsqb.data.TypeSafeQueryProxyData;
import be.shad.tsqb.data.TypeSafeQueryProxyDataTree;
import be.shad.tsqb.exceptions.JoinException;
import be.shad.tsqb.exceptions.ValueNotInScopeException;
import be.shad.tsqb.grouping.OnGoingGroupBy;
import be.shad.tsqb.grouping.TypeSafeQueryGroupBys;
import be.shad.tsqb.helper.TypeSafeQueryHelper;
import be.shad.tsqb.hql.HqlQuery;
import be.shad.tsqb.joins.TypeSafeQueryJoin;
import be.shad.tsqb.ordering.OnGoingOrderBy;
import be.shad.tsqb.ordering.TypeSafeQueryOrderBys;
import be.shad.tsqb.proxy.TypeSafeQueryProxy;
import be.shad.tsqb.restrictions.OnGoingBooleanRestriction;
import be.shad.tsqb.restrictions.OnGoingDateRestriction;
import be.shad.tsqb.restrictions.OnGoingEnumRestriction;
import be.shad.tsqb.restrictions.OnGoingNumberRestriction;
import be.shad.tsqb.restrictions.OnGoingTextRestriction;
import be.shad.tsqb.restrictions.Restriction;
import be.shad.tsqb.restrictions.RestrictionChainable;
import be.shad.tsqb.restrictions.RestrictionsGroup;
import be.shad.tsqb.restrictions.RestrictionsGroupImpl;
import be.shad.tsqb.restrictions.RestrictionsGroupInternal;
import be.shad.tsqb.selection.TypeSafeQueryProjections;
import be.shad.tsqb.values.DirectTypeSafeValue;
import be.shad.tsqb.values.HqlQueryValue;
import be.shad.tsqb.values.ReferenceTypeSafeValue;
import be.shad.tsqb.values.TypeSafeValue;
import be.shad.tsqb.values.TypeSafeValueFunctions;

/**
 * Collects the data and creates the hqlQuery based on this data.
 */
public abstract class AbstractTypeSafeQuery implements TypeSafeQuery, TypeSafeQueryInternal {
    protected final TypeSafeQueryHelper helper;
    private TypeSafeRootQueryInternal rootQuery;

    private final TypeSafeQueryProxyDataTree dataTree;
    private final TypeSafeQueryProjections projections = new TypeSafeQueryProjections(this); 
    private final RestrictionsGroupInternal restrictions = new RestrictionsGroupImpl(this, null); 
    private final TypeSafeQueryGroupBys groupBys = new TypeSafeQueryGroupBys(this);
    private final TypeSafeQueryOrderBys orderBys = new TypeSafeQueryOrderBys(this);
    
    public AbstractTypeSafeQuery(TypeSafeQueryHelper helper) {
        this.helper = helper;
        this.dataTree = new TypeSafeQueryProxyDataTree(helper, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeQueryHelper getHelper() {
        return helper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeQueryProxyDataTree getDataTree() {
        return dataTree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeRootQueryInternal getRootQuery() {
        return rootQuery;
    }
    
    /**
     * Allow subclasses to set the rootQuery.
     */
    protected void setRootQuery(TypeSafeRootQueryInternal rootQuery) {
        this.rootQuery = rootQuery;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T from(Class<T> fromClass) {
        return helper.createTypeSafeFromProxy(this, fromClass);
    }
    
    /**
     * {@inheritDoc}
     */
    public <T> T join(Collection<T> anyCollection) {
        return handleJoin(null, null, false);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T join(T anyObject) {
        return handleJoin(anyObject, null, false);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T join(Collection<T> anyCollection, JoinType joinType) {
        return handleJoin((T) null, joinType, false);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T join(T anyObject, JoinType joinType) {
        return handleJoin(anyObject, joinType, false);
    }


    /**
     * {@inheritDoc}
     */
    public <T> T join(Collection<T> anyCollection, JoinType joinType, boolean createAdditionalJoin) {
        return handleJoin((T) null, joinType, createAdditionalJoin);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T join(T anyObject, JoinType joinType, boolean createAdditionalJoin) {
        return handleJoin(anyObject, joinType, createAdditionalJoin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> TypeSafeQueryJoin<T> getJoin(T obj) {
        if(!(obj instanceof TypeSafeQueryProxy)) {
            throw new IllegalArgumentException("Can only get the join using a TypeSafeQueryProxy instance.");
        }
        return dataTree.getJoin(((TypeSafeQueryProxy) obj).getTypeSafeProxyData());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    private <T> T handleJoin(T obj, JoinType joinType, boolean createAdditionalJoin) {
        TypeSafeQueryProxyData data = rootQuery.dequeueInvocation();
        if( obj instanceof TypeSafeQueryProxy ) {
            data = ((TypeSafeQueryProxy) obj).getTypeSafeProxyData();
        }
        if( !data.getProxyType().isEntity() ) {
            throw new JoinException(String.format("Attempting to join an object "
                    + "which does not represent an entity. ", data.getAlias()));
        }
        if( createAdditionalJoin ) {
            data = helper.createTypeSafeJoinProxy(this, data.getParent(), 
                    data.getPropertyPath(), data.getPropertyType());
        }
        data.setJoinType(joinType == null ? JoinType.Default: joinType);
        return (T) data.getProxy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestrictionChainable where() {
        return restrictions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestrictionChainable where(HqlQueryValue restriction) {
        return restrictions.and(restriction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestrictionChainable where(RestrictionsGroup group) {
        return restrictions.and(group.getRestrictions());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestrictionChainable where(Restriction restriction) {
        return restrictions.and(restriction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestrictionsGroup whereGroup() {
        return new RestrictionsGroupImpl(this, null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public RestrictionChainable whereExists(TypeSafeSubQuery<?> subquery) {
        return restrictions.andExists(subquery);
    }
    
    /**
     * Delegate to restrictions.
     */
    @Override
    public <E extends Enum<E>> OnGoingEnumRestriction<E> where(E value) {
        return restrictions.and(value);
    }

    /**
     * Delegate to restrictions.
     */
    @Override
    public <E extends Enum<E>> OnGoingEnumRestriction<E> whereEnum(TypeSafeValue<E> value) {
        return restrictions.andEnum(value);
    }
    
    /**
     * Delegate to restrictions.
     */
    @Override
    public OnGoingBooleanRestriction where(Boolean value) {
        return restrictions.and(value);
    }

    /**
     * Delegate to restrictions.
     */
    @Override
    public OnGoingBooleanRestriction whereBoolean(TypeSafeValue<Boolean> value) {
        return restrictions.andBoolean(value);
    }
    
    /**
     * Delegate to restrictions.
     */
    @Override
    public OnGoingNumberRestriction where(Number value) {
        return restrictions.and(value);
    }

    /**
     * Delegate to restrictions.
     */
    @Override
    public OnGoingTextRestriction where(String value) {
        return restrictions.and(value);
    }

    /**
     * Delegate to restrictions.
     */
    @Override
    public <N extends Number> OnGoingNumberRestriction whereNumber(TypeSafeValue<N> value) {
        return restrictions.andNumber(value);
    }

    /**
     * Delegate to restrictions.
     */
    @Override
    public OnGoingTextRestriction whereString(TypeSafeValue<String> value) {
        return restrictions.andString(value);
    }
    
    /**
     * Delegate to restrictions.
     */
    @Override
    public OnGoingDateRestriction where(Date value) {
        return restrictions.and(value);
    }

    /**
     * Delegate to restrictions.
     */
    @Override
    public OnGoingDateRestriction whereDate(TypeSafeValue<Date> value) {
        return restrictions.andDate(value);
    }
    
    /**
     * Kicks off order by's. Use desc/asc afterwards to order by something.
     */
    public OnGoingOrderBy orderBy() {
        return orderBys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnGoingGroupBy groupBy(Number val) {
        return groupBys.and(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnGoingGroupBy groupBy(String val) {
        return groupBys.and(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnGoingGroupBy groupBy(Enum<?> val) {
        return groupBys.and(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnGoingGroupBy groupBy(Boolean val) {
        return groupBys.and(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnGoingGroupBy groupBy(Date val) {
        return groupBys.and(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnGoingGroupBy groupBy(TypeSafeValue<?> val) {
        return groupBys.and(val);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <VAL> TypeSafeValue<VAL> toValue(VAL value) {
        List<TypeSafeQueryProxyData> invocations = dequeueInvocations();
        if( invocations.isEmpty() ) {
            // direct selection
            return new DirectTypeSafeValue<VAL>(this, value);
        } else if( invocations.size() == 1 ) {
            // invoked with proxy
            return new ReferenceTypeSafeValue<VAL>(this, invocations.get(0));
        } else {
            // invalid call, only expected one invocation
            throw new IllegalStateException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Delegates to the dataTree.
     */
    @Override
    public boolean isInScope(TypeSafeQueryProxyData data, TypeSafeQueryProxyData join) {
        return dataTree.isInScope(data, join);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateInScope(TypeSafeQueryProxyData data, TypeSafeQueryProxyData join) {
        if( !isInScope(data, join) ) {
            throw new ValueNotInScopeException("Attempting to use data which is not in scope. " + data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateInScope(TypeSafeValue<?> value, TypeSafeQueryProxyData join) {
        new TypeSafeQueryScopeValidatorImpl(this, join).validateInScope(value);
    }
    
    /**
     * The projections, can be called to add extra projections.
     */
    public TypeSafeQueryProjections getProjections() {
        return projections;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestrictionsGroup getRestrictions() {
        return restrictions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeQueryGroupBys getGroupBys() {
        return groupBys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeQueryOrderBys getOrderBys() {
        return orderBys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> TypeSafeSubQuery<T> subquery(Class<T> clazz) {
        return new TypeSafeSubQueryImpl<>(clazz, helper, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeSafeValueFunctions function() {
        return new TypeSafeValueFunctions(this);
    }

    /**
     * Compose a query object with the selections, from, wheres, group bys and order bys.
     */
    public HqlQuery toHqlQuery() {
        HqlQuery query = new HqlQuery();
        
        // append select part:
        projections.appendTo(query);
        
        // append from part + their joins:
        dataTree.appendTo(query);
        
        // append where part:
        HqlQueryValue hqlRestrictions = restrictions.toHqlQueryValue();
        query.appendWhere(hqlRestrictions.getHql());
        query.addParams(hqlRestrictions.getParams());
        
        // append group part:
        groupBys.appendTo(query);
        
        // append order part:
        orderBys.appendTo(query);
        
        return query;
    }
    
}
